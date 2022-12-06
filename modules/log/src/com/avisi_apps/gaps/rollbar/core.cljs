(ns com.avisi-apps.gaps.rollbar.core
  (:require
    [com.avisi-apps.gaps.rollbar.api :as api]))

(defn logAdditionalInformation [severity message]
  (api/sendToRollbar severity message))
