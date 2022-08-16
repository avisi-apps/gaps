(ns avisi-apps.gaps.log
  (:require-macros avisi-apps.gaps.log)
  (:require
    ["bunyan" :as bunyan]
    [cljs-bean.core :refer [->js bean]]
    [hyperfiddle.rcf :refer [tests]]
    [clojure.string :as str]
    [clojure.set :as set]))

(declare debug info warn error spy)

;; You can override this name by using closure defines.
;; For more information see: https://shadow-cljs.github.io/docs/UsersGuide.html#closure-defines
(goog-define LOGGER_NAME "avisi-apps-logger")

(def logger
  (.createLogger
    ^js bunyan
    (clj->js
      (merge
        {:name LOGGER_NAME
         :streams
         [{:stream js/process.stdout
           :level (if ^boolean goog/DEBUG "debug" "info")}]}))))

(def kw->log-severity
  {:debug "DEBUG"
   :info "INFO"
   :notice "NOTICE"
   :warn "WARNING"
   :error "ERROR"
   :critical "CRITICAL"
   :alert "ALERT"
   :emergency "EMERGENCY"})

(defn log!
  [{:keys [severity message]
    :as payload}]
  (let [logger* logger
        js-payload (->js payload)
        msg (or message "No message")]
    ;; Based on https://cloud.google.com/logging/docs/reference/v2/rest/v2/LogEntry#logseverity
    (case severity
      "DEBUG" (.debug ^js logger* js-payload msg)
      "INFO" (.info ^js logger* js-payload msg)
      "NOTICE" (.info ^js logger* js-payload msg)
      "WARNING" (.warn ^js logger* js-payload msg)
      "ERROR" (.error ^js logger* js-payload msg)
      "CRITICAL" (.fatal ^js logger* js-payload msg)
      "ALERT" (.fatal ^js logger* js-payload msg)
      "EMERGENCY" (.fatal ^js logger* js-payload msg))))

(defn request->log [{:keys [request-method url content-length remote-addr protocol] :as args}]
  (when (seq args)
    #js {"requestMethod" (and request-method (str/upper-case (name request-method))),
         "requestUrl" url,
         "requestSize" content-length,
         "remoteIp" remote-addr,
         "protocol" protocol}))

(tests
  "Don't fail on empty request"
  (request->log {}) := nil
  "Don't fail on empty request-method"
  (js->clj (request->log {:url "/foo/bar"})) := {"requestMethod" nil, "requestUrl" "/foo/bar", "requestSize" nil, "remoteIp" nil, "protocol" nil})

(defn add-error-data [data exception]
  (let [{:keys [stack request response]} (bean exception)]
    (cond->
      (merge
        data
        {:err exception
         :exception-message (ex-message exception)
         :exception-data (ex-data exception)})
      request (assoc :httpRequest (request->log request))
      response (assoc :response response)
      stack (update :message (fn [message] (str message "\n Error:\n" stack))))))

(defn log [{:keys [level data line ns file]}]
  (log!
    (->
      (dissoc data :error)
      (assoc
        :severity (get kw->log-severity level "INFO")
        ;; Based on https://cloud.google.com/logging/docs/reference/v2/rest/v2/LogEntry#LogEntrySourceLocation
        :sourceLocation
        {:ns ns
         :file file
         :line line})
      (cond-> (:error data) (add-error-data (:error data))))))
