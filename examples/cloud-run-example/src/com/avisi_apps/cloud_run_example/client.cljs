(ns com.avisi-apps.cloud-run-example.client
  (:require-macros [com.avisi-apps.cloud-run-example.read-token :as token-reader])
  (:require
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom :as dom]
    [com.avisi-apps.gaps.log :as log]
    [com.avisi-apps.gaps.rollbar.interface :as et]
    [com.avisi-apps.gaps.rollbar.config :as rollbar-config]))

(def log-token (token-reader/read-token))
(defonce app (app/fulcro-app))

(defn generate-exception
  "Creates and throws an error, sends it to the logging service."
  []
  (try
    (throw
      (ex-info "test error 25" {:value "value1" :foo "bar"}))
    (catch ExceptionInfo e
      (log/error e (ex-data e)))))

(defsc Root [this props]
  {:initial-state (fn [params] {:initialized? true
                                :error-tracking (comp/get-initial-state et/error-tracking)})
   :query [:initialized?
           { :error-tracking (comp/get-query et/error-tracking)}]
   :initLocalState
   (fn [this props]
     {:enable-error-tracking (fn [] (comp/transact! this [(et/toggle-error-tracking {:toggle true})]))
      :disable-error-tracking (fn [] (comp/transact! this [(et/toggle-error-tracking {:toggle false})]))})}
  (let [enable-error-tracking (comp/get-state this :enable-error-tracking)
        disable-error-tracking (comp/get-state this :disable-error-tracking)]
  (dom/div
    (dom/h1 "TODO")
    (dom/pre (str props))
    (dom/h3 "Set logging service:")
    (dom/button
      {:onClick enable-error-tracking}
      "Enable tracking")
    (dom/button
      {:onClick disable-error-tracking}
      "Disable tracking")
    (dom/h3 "Generate error:")
    (dom/button
      {:onClick #(generate-exception)}
      "Generate error")
    (dom/button
      {:onClick #(log/info {:message "this is an information log."})}
      "Generate info"))))

(defn ^:export init
  "Shadow-cljs sets this up to be our entry-point function. See shadow-cljs.edn `:init-fn` in the modules of the main build."
  []
  (app/mount! app Root "app")
  (rollbar-config/initialize-rollbar! {:log/token log-token :app app})
  (log/debug {:message "Loaded"}))

(defn ^:export refresh
  "During development, shadow-cljs will call this on every hot reload of source. See shadow-cljs.edn"
  []
  ;; re-mounting will cause forced UI refresh, update internals, etc.
  (app/mount! app Root "app")
  ;; As of Fulcro 3.3.0, this addition will help with stale queries when using dynamic routing:
  (comp/refresh-dynamic-queries! app)
  (log/debug {:message "Hot reload"}))

(comment
  (log/info {:message "This is not good"})

  (log/debug {:message "This is not good"}))
