(ns com.avisi-apps.gaps.brevo.axios-client
  (:refer-clojure :exclude [get])
  (:require
    ["axios" :as axios]
    ["agentkeepalive" :refer [HttpsAgent]]
    [cljs-bean.core :refer [->clj ->js bean]]
    [goog.uri.utils :as gutils]
    [promesa.core :as p]
    [clojure.string :as string]))

(defn stringify-query-params [m] (gutils/buildQueryDataFromMap (->js m)))

(def https-agent
  (HttpsAgent.
    (clj->js
      {:keepAlive true
       :maxSockets 100
       :maxFreeSockets 10
       :timeout 30000
       :freeSocketTimeout 30000})))

(def axios-instance
  (axios/create
    (clj->js
      {;; cap the maximum content length we'll accept to 1MBs, just in case
       :maxContentLength (* 1 1000 1000)
       :maxRedirects 0
       :httpsAgent https-agent})))

(defn endpoint-with-qp [{:keys [endpoint query-params]}]
  (let [query-string (stringify-query-params query-params)]
    (cond-> endpoint (not (string/blank? query-string)) (str "?" query-string))))

(defn request [{:keys [method endpoint headers data query-params]}]
  (let [url (endpoint-with-qp
              {:endpoint endpoint
               :query-params query-params})]
    (->
      (.request
        ^js axios-instance
        (cond->
          {:url url
           :method (name method)
           :headers (merge {:content-type "application/json"} headers)}
          data (assoc :data data)
          :always ->js))
      (p/then ->clj)
      (p/catch
        (fn [err]
          (let [{:keys [response]} (bean err)
                {:keys [status]
                 :as res}
                  (some->
                    response
                    bean)]
            (throw
              (ex-info
                "Failed HTTP request"
                {:url url
                 :method method
                 :request-data data
                 :response-data (js->clj (:data res))
                 :status status}))))))))

(defn get [env] (request (assoc env :method :get)))

(defn put [env] (request (assoc env :method :put)))

(defn post [env] (request (assoc env :method :post)))

(defn delete [env] (request (assoc env :method :delete)))

(defn axios-error? [err] (:isAxiosError err false))

(defn axios-error->json [{:keys [response request]}]
  (cond
    response
      (->
        (bean response)
        (select-keys [:status :data :headers])
        (assoc :type-of-failure :axios-response-failure))
    request
      (->
        (bean request)
        (select-keys [:config])
        (assoc :type-of-failure :axios-request-failure))
    :else {:type-of-failure :axios-unexpected-error}))
