(ns com.avisi-apps.gaps.rollbar.core
  (:require
    [com.avisi-apps.gaps.rollbar.api :as api]
    [com.avisi-apps.gaps.rollbar.track-error :as track-error]
    [hyperfiddle.rcf :refer [tests]]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]))

(defn check-error-tracking-permission []
  ;;Hier naar de configuration storage F22

  ;;(track-error/lookup-error-tracking)
  true)

(tests
  "TC5: Should return true"
  (check-error-tracking-permission) := true)

(defn logAdditionalInformation [severity message]
  (when (check-error-tracking-permission)
  (api/sendToRollbar severity message)))


