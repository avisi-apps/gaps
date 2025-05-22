(ns com.avisi-apps.gaps.log.preload
  (:require
    [taoensso.telemere :as t]
    [com.avisi-apps.gaps.log :as log]))

(t/set-min-level! :debug)

(log/warn
  {:message "Set min level to debug - Make sure this does not happen in production!"
   :level :debug})
