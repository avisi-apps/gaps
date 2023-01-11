(ns com.avisi-apps.gaps.rollbar.core
  (:require
    ["error-stack-parser" :as error-stack-parser]
    [hyperfiddle.rcf :refer [tests]]
    [com.fulcrologic.fulcro.application :as app]
    [com.avisi-apps.gaps.rollbar.api :as api]
    [com.avisi-apps.gaps.rollbar.config :as config]))


(defn check-permission? [configuration]
  (let [state (app/current-state (:app configuration))]
  (get-in state [:error-tracking :track-error?])))

(defn cljs-error? [e]
  (instance? ExceptionInfo e))

(tests
  "TC5: Error is of type ExceptionInfo"
  (cljs-error? (ex-info "Test error" {:test "true"})) := true
  "TC6: Error not of type ExceptionInfo, it is a javascript error"
  (cljs-error? (js/Error. "Test error")) := false)

(defn parse-error-stack [payload]
 (let [parsed-stack
       (if (cljs-error? (:err payload))
         (error-stack-parser/parse (clj->js (:err payload)))
         (error-stack-parser/parse payload))]
   parsed-stack))

(defn build-frames-for-exception [stack-frames]
(let
  [stack
   (mapv
     (fn [stack]
       {:filename (.getFileName stack)
        :lineno (.getLineNumber stack)
        :colno (.getColumnNumber stack)
        :method (.getFunctionName stack)})
     stack-frames)]
  stack))

(defn build-notifier [version]
  (let [notifier {:name "Gaps/log" :version version} ]
    notifier))

(defn build-client [version]
  (let [client {:javascript {:code_version version
                             :source_map_enable true
                             :guess_uncaught_frames false}}]
    client))

(defn validate-rollbar-config? [configuration]
  (if (and (contains? configuration :log/token) (string? (get configuration :log/token)))
    true
    false))

(tests
  "TC1: Should be valid config"
  (validate-rollbar-config? {:log/token "testToken"}) := true
  "TC2: Should be invalid config, token is wrong dataType"
  (validate-rollbar-config? {:log/token 123}) := false
  "TC3: Should be invalid config, missing the :log/token key instead :token was provided"
  (validate-rollbar-config? {:token "testValue"}) := false
  "TC4: Should be invalid config, the map is empty"
  (validate-rollbar-config? {}) := false)

(defn log-additional-information [version severity payload]
  (let [configuration (config/get-rollbar-config)]
    (when (and (validate-rollbar-config? configuration) (check-permission? configuration))
      (if (= severity "ERROR")
        (api/send-exception-to-rollbar configuration (build-notifier version) (build-client version) severity payload (build-frames-for-exception (parse-error-stack payload)))
        (api/send-message-to-rollbar configuration (build-notifier version) severity (:message payload))))))
