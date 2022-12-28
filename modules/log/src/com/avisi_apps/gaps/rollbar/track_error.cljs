(ns com.avisi-apps.gaps.rollbar.track-error
  (:require
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]))

(defmutation lookup-error-tracking [_]
             (action [{:keys [state]}]
                     (when [:error-tracking :track-error?]
                                         "true"
                                         "false")))

(defmutation toggle-error-tracking [{:keys [toggle]}]
             (action [{:keys [state]}] (swap! state assoc-in [:error-tracking :track-error?] toggle)))

(defsc error-tracking [this {:keys [:track-error?]}]
       {:query [:track-error?]
   :initial-state (fn [_] {:track-error? false})})
