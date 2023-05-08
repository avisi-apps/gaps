(ns com.avisi-apps.gaps.log
  (:require-macros com.avisi-apps.gaps.log)
  (:require
    ["pino" :as pino]
    [cljs-bean.core :refer [->js bean]]
    [goog.object :as gobj]
    [hyperfiddle.rcf :refer [tests]]
    [clojure.string :as str]
    [clojure.set :as set]))

(declare debug info warn error spy)

;; You can override this name by using closure defines.
;; For more information see: https://shadow-cljs.github.io/docs/UsersGuide.html#closure-defines
(goog-define LOGGER_NAME "avisi-apps-logger")

(def kw->log-severity
  {:debug "DEBUG"
   :info "INFO"
   :notice "NOTICE"
   :warn "WARNING"
   :error "ERROR"
   :critical "CRITICAL"
   :alert "ALERT"
   :emergency "EMERGENCY"})

(def severity->kw (set/map-invert kw->log-severity))

(defonce ^{:dynamic true} *logger-config* {:name LOGGER_NAME
                                           :serializers {:err
                                                         (.wrapErrorSerializer
                                                           ^js (.-stdSerializers ^js pino)
                                                           (fn [serialized]
                                                             (when-let [data (gobj/get serialized "data")]
                                                               (gobj/set serialized "data" (clj->js data)))
                                                             serialized))}
                                           :messageKey "message"
                                           :level (if ^boolean goog/DEBUG "debug" "info")})

(defonce ^{:dynamic true} *logger* (pino. (clj->js *logger-config*)))

(defn update-log-config! [update-fn]
  (let [updated-config (update-fn *logger-config*)]
    (set! *logger-config* updated-config)
    (set! *logger* (pino. (clj->js updated-config)))))

(defn log!
  [{:keys [severity message]
    :as payload}]
  (let [logger* *logger*
        js-payload (->js (dissoc payload :message))
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

(defn request->log [{:keys [request-method original-url protocol] :as args}]
  (when (seq args)
    #js {"requestMethod" (and request-method (str/upper-case (name request-method))),
         "requestUrl" original-url,
         "protocol" protocol}))

(tests
  "Don't fail on empty request"
  (request->log {}) := nil
  "Don't fail on empty request-method"
  (js->clj (request->log {:original-url "/foo/bar"})) := {"requestMethod" nil, "requestUrl" "/foo/bar", "protocol" nil}
  "Uppercase request-method"
  (js->clj (request->log {:original-url "/foo/bar"
                          :request-method :get})) := {"requestMethod" "GET", "requestUrl" "/foo/bar", "protocol" nil})

(defn add-error-data [data exception]
  (let [{:keys [request response]} (bean exception)]
    (cond->
      (assoc
        data
        :err exception)
      ;; Request might be a Javascript object
      request (assoc :httpRequest (request->log (bean request)))
      response (assoc :response response))))

(defn log [{:keys [level data line ns file]}]
  (log!
    (->
      (dissoc data :error :request)
      (assoc
        :severity (get kw->log-severity level "INFO")
        ;; Based on https://cloud.google.com/logging/docs/reference/v2/rest/v2/LogEntry#LogEntrySourceLocation
        :sourceLocation
        {:ns ns
         :file file
         :line line})
      (cond->
        (:error data) (add-error-data (:error data))
        (:request data) (assoc :httpRequest (request->log (:request data)))))))
