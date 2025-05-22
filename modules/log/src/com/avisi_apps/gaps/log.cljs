(ns com.avisi-apps.gaps.log
  (:require-macros com.avisi-apps.gaps.log)
  (:require
    [clojure.string :as str]
    [taoensso.telemere :as t]
    [taoensso.telemere.utils :as tu]))

(declare debug info warn error spy)

(def production-handler
  (t/handler:console
    {:output-fn
       (t/pr-signal-fn
         {:clean-fn
            (comp
              (fn
                [{:keys [level msg_]
                  :as signal}]
                (->
                  (assoc
                    signal :severity
                    (str/upper-case (name level)) :message
                    msg_)
                  (dissoc :msg_)))
              (tu/clean-signal-fn))
          :pr-fn :json})}))

(when-not ^boolean goog/DEBUG
  (t/remove-handler! :default/console)
  (t/add-handler! :raw-console-handler production-handler))
