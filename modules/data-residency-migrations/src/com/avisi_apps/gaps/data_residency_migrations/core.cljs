(ns com.avisi-apps.gaps.data-residency-migrations.core
  (:require
    ["google-auth-library" :refer [GoogleAuth]]
    [com.avisi-apps.gaps.log :as log]
    [com.avisi-apps.gaps.http.core :as http]
    [com.avisi-apps.gaps.google-cloud.pubsub :as pubsub]
    [clojure.string :as str]
    [cljs-bean.core :refer [->clj]]
    [promesa.core :as p]
    [camel-snake-kebab.core :as csk]))

(defn ^:private get-project-id [] (.-env.GCLOUD_PROJECT js/process))

(defn ^:private log-success [installation migration]
  (log/info
    {:message "Successfully handled migration call"
     :clientKey (:clientKey installation)
     :baseUrl (:baseUrl installation)
     :migration migration}))

(defn ^:private catch-migration-error [respond installation]
  (fn [e]
    (log/error
      e
      {:message "Error while handling migration request"
       :clientKey (:clientKey installation)
       :baseUrl (:baseUrl installation)})
    (respond
      {:status 500
       :body "Unexpected error"})))

; for now migration leaders are hardcoded to be in the us region
(defn ^:private project-id->migrations-topic [project-id]
  (let [[app-name environment _] (->
                                   project-id
                                   (str/split #"-"))
        leader-project (str/join "-" [app-name environment "us"])]
    (str "projects/" leader-project "/topics/dare-migrations")))

(defn ^:private handle-migration-event [{:keys [installation migration-data tenant-ref]}]
  (let [tenant (:clientKey installation)
        {:keys [phase start-time end-time]} migration-data]
    (condp contains? (:phase migration-data)
      #{"schedule"}
        (let [source-project (get-project-id)
              [app-name environment _] (->
                                         source-project
                                         (str/split #"-"))
              destination-region-label (str/lower-case (:location migration-data))
              destination-project (str/join "-" [app-name environment destination-region-label])]
          (pubsub/publish-message!
            (project-id->migrations-topic source-project)
            {:attributes
               {:tenant tenant
                :source_project source-project
                :destination_project destination-project
                :start_time start-time
                :end_time end-time}}))
      #{"start" "commit" "rollback"}
        (p/let [token (->
                        ^js (GoogleAuth.)
                        (.getAccessToken))
                callback-url (p/->
                               tenant-ref
                               (.get)
                               (.data)
                               ->clj
                               :migrationCallback)]
          (http/request
            {::http/base-url callback-url
             ::http/method "POST"
             ::http/headers
               {"Content-Type" "application/json"
                "Authorization" (str "Bearer " token)}
             ::http/body {:phase phase}})))))

(defn ^:private migration-handler
  [{:keys [installation]
    {:keys [tenant-ref]} :dare-migration
    :as request}
   respond
   _]
  (let [phase (->
                request
                :path-params
                :phase)
        data (->
               request
               :body
               ->clj
               (update-keys csk/->kebab-case-keyword))
        migration-data (merge {:phase phase} data)]
    (->
      (p/do!
        (handle-migration-event
          {:migration-data migration-data
           :tenant-ref tenant-ref
           :installation installation})
        (log-success installation migration-data)
        (respond {:status 200}))
      (p/catch (catch-migration-error respond installation)))))

(defn ^:private get-migration-status
  [{:keys [installation]
    {:keys [tenant-ref]} :dare-migration}
   respond
   _]
  (->
    (p/let [migration-status (p/->
                               tenant-ref
                               (.get)
                               (.data)
                               ->clj
                               :migrationStatus
                               (or "not-ready"))]
      (->
        (p/do!
          (log-success installation {:status migration-status})
          (respond
            {:status 200
             :body {:status migration-status}}))))
    (p/catch (catch-migration-error respond installation))))

(defn endpoints
  "
  Data residency migration endpoints for integration with reitit-router.

  Registration in the descriptor takes the form of the path from the base-url to here plus '/migrations' (e.g. '/atlassian/lifecycle/migrations')
  See atlassians docs for more info: https://developer.atlassian.com/cloud/jira/platform/data-residency/

  Authentication and authorisation are expected to be handled by the calling application.

  The handlers for these endpoints expect the middleware of the calling application to place the following keys and associated values on the request
    - :dare-migration {:tenant-ref ...} a reference to the firestore-doc that needs to be migrated
    - :installation {:clientKey ... :baseUrl ...} data about the tenant-installation

  The route-config parameter of this function can be used to register any data and/or middleware to achieve the above points.
  "
  [route-config]
  ["/migrations" route-config ["/status" {:get get-migration-status}] ["/:phase" {:post migration-handler}]])
