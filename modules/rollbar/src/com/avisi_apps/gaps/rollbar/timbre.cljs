(ns com.avisi-apps.gaps.rollbar.timbre)

(defn ^:private set-context! [^js rollbar context] (.configure ^js rollbar #js {:payload #js {:context context}}))

(defn ^:private log! [^js rollbar level msg err ns]
  (case level
    :trace (.debug rollbar msg #js {:namespace ns})
    :debug (.debug rollbar msg #js {:namespace ns})
    :info (.info rollbar msg #js {:namespace ns})
    :warn (.warning rollbar msg #js {:namespace ns})
    :error (.error rollbar msg err #js {:namespace ns})
    :fatal (.critical rollbar msg #js {:namespace ns})
    :report (.info rollbar msg #js {:namespace ns})))

(defn rollbar-appender
  "Returns a Timbre Rollbar appender

  Requires a Rollbar notifier to be passed in,
  see Rollbar (Browser JS/Node.js) documentation for details.

  Options:
    * :ns-filter checks the namespace to determine if it should be sent.
      By default `#{\"*\"}` so everything is sent through.
    * :context sets the Rollbar context in the payload"
  ([^js rollbar] (rollbar-appender rollbar {}))
  ([^js rollbar opts]
   (let [{:keys [ns-filter context]} opts]
     {:enabled? true
      :async? true
      :min-level :error ;; Sane default for Rollbar
      :output-fn :inherit
      :ns-filter (or ns-filter #{"*"})
      :fn
        (fn [{:keys [level msg_ ?ns-str ?err]}]
          (when rollbar
            (when context (set-context! rollbar context))
            (log! rollbar level (force msg_) (clj->js ?err) ?ns-str)))})))
