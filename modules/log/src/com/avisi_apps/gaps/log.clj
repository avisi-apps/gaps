(ns com.avisi-apps.gaps.log)

(defn- log-expr
  [level data m]
  `(log
     ~{:level level
       :line (:line m)
       :file (:file m)
       :ns (str *ns*)
       :data data}))

(defmacro debug [data] (log-expr :debug data (meta &form)))

(defmacro info [data] (log-expr  :info data (meta &form)))

(defmacro warn [data] (log-expr  :warn data (meta &form)))

(defmacro error [error data] (log-expr :error `(assoc ~data :error ~error) (meta &form)))

(defmacro spy
  [form]
  (let [res (gensym)]
    `(let [~res ~form]
       ~(log-expr
          :debug
          `{:spy ~(str form)
            :=> (str ~res)}
          (meta &form))
       ~res)))
