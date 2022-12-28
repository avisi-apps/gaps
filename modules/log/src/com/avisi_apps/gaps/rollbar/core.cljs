(ns com.avisi-apps.gaps.rollbar.core
  (:require
    ["error-stack-parser" :as error-stack-parser]
    [hyperfiddle.rcf :refer [tests]]
    [com.avisi-apps.gaps.rollbar.api :as api]))

(defn cljs-error? [e]
  (instance? ExceptionInfo e))

(tests
  "TC5: Error is of type ExceptionInfo"
  (instance? ExceptionInfo (ex-info "Test error" {:test "true"})) := true
  "TC6: Error not is of type ExceptionInfo, it is a javascript error"
  (instance? ExceptionInfo (js/Error. "Test error")) := false)

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
       {:filename (.getFileName stack )
        :lineno (.getLineNumber stack)
        :colno (.getColumnNumber stack)
        :method (.getFunctionName stack)})
     stack-frames)]
  stack))

(defn build-notifier [VERSION]
  (let [notifier {:name "Gaps/log" :version VERSION} ]
    notifier))

(defn build-client [VERSION]
  (let [client {:javascript {:code_version VERSION
                             :source_map_enable true
                             :guess_uncaught_frames false}}]
    client))

(defn logAdditionalInformation [VERSION severity payload]
   (if (= severity "ERROR")
     (api/send-exception-to-rollbar (build-notifier VERSION) (build-client VERSION) severity payload (build-frames-for-exception (parse-error-stack payload)))
     (api/send-message-to-rollbar (build-notifier VERSION) severity (:message payload))))
