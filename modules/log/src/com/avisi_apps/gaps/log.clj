(ns com.avisi-apps.gaps.log
  (:require
    [taoensso.telemere :as t]))

(defn create-log-statement [level data]
  `(taoensso.telemere/log!
     {:level ~level
      :data (dissoc ~data :message)}
     (:message ~data)))

(defmacro debug [data] (create-log-statement :debug data))
(defmacro info [data] (create-log-statement :info data))
(defmacro warn [data] (create-log-statement :warn data))
(defmacro error
  [error data]
  `(taoensso.telemere/log!
     {:level :error
      :error ~error
      :data (merge (dissoc ~data :message :error))}
     (:message ~data)))

(defmacro spy [body] `(taoensso.telemere/spy! :debug ~body))
