# Rollbar [![Clojars Project](https://img.shields.io/clojars/v/com.avisi-apps.gaps/rollbar.svg)](https://clojars.org/com.avisi-apps.gaps/rollbar)

[Fulcro](https://github.com/fulcrologic/fulcro) components to add [Rollbar](https://rollbar.com/#) in cljs projects.

# Usage
This project uses `rollbar` and `@rollbar/react` for creating the react components. Make sure you added these to your dependencies by running:

```shell
yarn add --exact rollbar @rollbar/react
```

See the below example for usage examples:
```clojure
(ns example
  ;; First make sure you add this require
  (:require
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.components :refer [defsc] :as comp]
    [com.avisi-apps.gaps.rollbar :as rollbar]))

(defonce app (atom (app/fulcro-app)))

(defsc Root [this {:keys [router]}]
  {:query [{:router (comp/get-query Router)}]
   :initial-state (fn [_] {})}
  ;; You need to wrap the main logic in the provider
  (rollbar/provider
    {:config @rollbar/config}

    ;; Then anywhere in the UI tree you can wrap an error boundary,
    ;; just like in React
    (rollbar/error-boundary
      {:level rollbar/error
       :error-message "Error in React render from Root"}
      ;; Content to be rendered normally
      (dom/div ...))))

(defn ^:export init []
  ;; Set the rollbar config inside the method,
  ;; that mounts your app
  (rollbar/set-config!
    {:accessToken "POST_CLIENT_ITEM_ACCESS_TOKEN"
     :captureUncaught true
     :captureUnhandledRejections true
     :payload {...}}

  ;; You can also instantiate a new Rollbar notifier to do manual logging.
  ;; This still requires the config to be set first
  (.log ^js (rollbar/notifier) "App initialized")

  (app/mount! @app Root "app"))
```

## Changing the fallback UI
By default this project comes with a very simple fallback UI that renders the string: `There was an error.`

You can customize the fallback UI by passing a fallback ui function to the error boundary. For example,

```clojure
(rollbar/error-boundary
  {...
   :fallback-ui-fn 
    (fn [{:keys [error]}]
     (dom/div
       (dom/h2 "Unexpected Error")
       (dom/p "An error occurred while rendering the app.")
       (dom/p (str error))))})
```

The `js-obj` includes the key "error" which holds some info about the error that occurred.

## Transforming the Rollbar event
Rollbar allows the user to transform the event right before it is send by supplying the `:transform` key in the configuration.
it takes a function with the signature `(fn [^js payload] ...)`. This project also includes a helper to compose multiple transformers together.

```clojure
(->
  (rollbar/set-config! {...})
  (rollbar/with-transformers
    ;; Note: the transformers will be executed right to left
    (fn [^js payload] ... payload)
    ...
    (fn [^js payload] ... payload)))
```

This way multiple smaller transformers can be chained together. Since we are modifying JS objects make sure it is returned, 
that way the next transformer receives the updated payload.

### Fulcro route as context
The project includes a transformer that sets the current Fulcro route as the context for Rollbar. It requires an atom that holds the current app.
This atom gets dereferenced inside the transformer to fetch the current route.

```clojure
(->
  (rollbar/set-config! {...})
  (assoc :transform (rollbar/fulcro-route-to-context-transformer app)))
```
## Timbre
Fulcro uses [Timbre](https://github.com/ptaoussanis/timbre) for it's logging. This project includes a rollbar appender for Timbre to be able to send Fulcro's error logging.

```clojure
(ns example
  (:require 
    [com.avisi-apps.gaps.rollbar.timbre :as timbre-rollbar]))

(timbre/merge-config!
  {:appenders
     {:rollbar 
      (timbre-rollbar/rollbar-appender
        ;; We need to pass a Rollbar notifier
        ;; This project has a way to create a new notifier based on the config set earlier
        (rollbar/notifier)
        ;; You can pass a namespace filter that gets passed to Timbre
        {:ns-filter #{"com.fulcrologic.fulcro.*"}})}})
```
