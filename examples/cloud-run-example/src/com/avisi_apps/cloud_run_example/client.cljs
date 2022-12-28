(ns com.avisi-apps.cloud-run-example.client
  (:require
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.avisi-apps.gaps.log :as log]
    [com.fulcrologic.fulcro.dom :as dom]))

(defonce app (app/fulcro-app))

(defn generate-exception
  "Creates and throws an error, sends it to the logging service."
  []
  (try
    (throw
      (ex-info "test error 23" {:value "value1" :foo "bar"}))
    (catch ExceptionInfo e
      (log/error e (ex-data e)))))

(defsc Root [this props]
  {:initial-state (fn [params] {:initialized? true})
   :query         [:initialized?]}
  (dom/div
    (dom/h1 "TODO")
    (dom/pre (str props))
    (dom/br)
    (dom/h1 "Buttons")
    (dom/p "Generate Error")
    (dom/button
      {:onClick #(generate-exception)}
      "Generate error")))

(defn ^:export init
  "Shadow-cljs sets this up to be our entry-point function. See shadow-cljs.edn `:init-fn` in the modules of the main build."
  []
  (app/mount! app Root "app")
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

  (log/debug {:message "This is not good"})

  )
