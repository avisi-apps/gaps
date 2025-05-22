(ns com.avisi-apps.gaps.log.preload-browser
  (:require
    [com.avisi-apps.gaps.log :as log]
    [taoensso.telemere :as t]))

(t/set-min-level! :debug)

(t/remove-handler! :default/console)

(t/add-handler! :raw-console-handler (t/handler:console-raw))

(log/warn
  {:message "Set min level to debug - Make sure this does not happen in production!"
   :level :debug})

