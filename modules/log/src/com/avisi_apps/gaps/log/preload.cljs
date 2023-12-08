(ns com.avisi-apps.gaps.log.preload
  (:require
    ["pino-pretty"]
    [com.avisi-apps.gaps.log :as log]))

(log/update-log-config!
  (fn [current-config]
    (assoc current-config
      :transport
        {:target "pino-pretty"
         :options
           {:sync true
            :messageKey "message"
            :colorize true}})))

(log/warn {:message "Enabled pino-pretty logger make sure this doesn't happen in production!"})
