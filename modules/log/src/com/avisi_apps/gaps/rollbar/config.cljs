(ns com.avisi-apps.gaps.rollbar.config)

(defonce configuration (atom {:log/token nil :app nil}))

(defn initialize-rollbar! [rollbar-configuration]
  (reset! configuration rollbar-configuration))

(defn get-rollbar-config []
 @configuration)
