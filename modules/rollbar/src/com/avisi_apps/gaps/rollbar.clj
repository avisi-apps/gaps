(ns com.avisi-apps.gaps.rollbar
  (:require
    [com.fulcrologic.fulcro.components :as comp]))

(defmacro error-boundary
  "Fulcro wrapper macro for Rollbar's Error Boundary

  Options:
    * :level, level at which Rollbar logs react error messages
    * :error-message, the message that will be sent along the error
    * :fallback-ui-fn, the UI components to be rendered when an error occurs

  The `:error-message` can also be a function with the signature: `(fn [error props] message-to-sent)`,
  see Rollbar's react documentation for more details on the props.

  The `:fallback-ui-fn` function takes function with the signature: `(fn [props] what-to-render)`,
  where the props is a bean with the key `:error`."
  [opts & body]
  `(error-boundary*
     (assoc ~opts
       :parent comp/*parent*
       :render #(comp/with-parent-context % (comp/fragment ~@body)))))
