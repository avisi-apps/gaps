(ns com.avisi-apps.gaps.rollbar
  (:require-macros [com.avisi-apps.gaps.rollbar])
  (:require
    ["rollbar" :as Rollbar]
    ["@rollbar/react" :refer [LEVEL_CRITICAL LEVEL_DEBUG LEVEL_ERROR LEVEL_INFO LEVEL_WARN Provider ErrorBoundary]]
    [cljs-bean.core :refer [bean]]
    [clojure.string :as str]
    [com.fulcrologic.fulcro.algorithms.react-interop :as react-interop]
    [com.fulcrologic.fulcro.components :as comp]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]))

;;
;; Rollbar config
;;
(defonce config (atom nil))
(defn set-config! [m] (reset! config m))

(defn notifier
  "Creates a new Rollbar notifier using existing config.
  Can be merged with configurations in `m`"
  ([] (notifier {}))
  ([m]
   (when-let [c @config]
     (new ^js Rollbar (clj->js (merge c m))))))

;;
;; Rollbar error levels
;;
(def critical LEVEL_CRITICAL)
(def debug LEVEL_DEBUG)
(def error LEVEL_ERROR)
(def info LEVEL_INFO)
(def warn LEVEL_WARN)

;;
;; Fulcro UI components
;;
(defn fallback-ui-wrapper [fallback-ui-fn]
  (fn [^js js-obj]
    (let [fallback-ui-fn (or
                           (and (fn? fallback-ui-fn) fallback-ui-fn)
                           (fn [_] "An error has occurred. Try reloading the page."))]
      (fallback-ui-fn (bean js-obj)))))

(def ^:private ui-error-boundary (react-interop/react-factory ErrorBoundary))

(defn error-boundary*
  "Use the error-boundary macro instead"
  [{:keys [level error-message parent render fallback-ui-fn]}]
  (ui-error-boundary
    {:level (or level error)
     :errorMessage error-message
     :fallbackUI (fallback-ui-wrapper fallback-ui-fn)}
    (comp/with-parent-context parent
      (render))))

(def provider (react-interop/react-factory Provider))

;;
;; Utilities
;;
(defn with-transformers
  "Composes the functions `t` from right to left and assigns that to the Rollbar config"
  [c & t] (assoc c :transform (apply comp t)))

(defn fulcro-route-to-context-transformer
  "Given an app atom, returns a composable Rollbar transformer
  that sets the current route as the context"
  [app]
  (fn [^js payload]
    (set! (.-context payload) (str/join "/" (dr/current-route @app)))
    payload))
