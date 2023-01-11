(ns com.avisi-apps.gaps.rollbar.interface
  (:require
    [com.fulcrologic.fulcro.components :refer [defsc]]
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]))

(defmutation toggle-error-tracking [{:keys [toggle]}]
             (action [{:keys [state]}] (swap! state assoc-in [:error-tracking :track-error?] toggle)))

(defsc error-tracking [_ {:keys []}]
       {:query [:track-error?]
        :initial-state (fn [_] {:track-error? false})})
