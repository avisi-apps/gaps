# Log [![Clojars Project](https://img.shields.io/clojars/v/com.avisi-apps.gaps/log.svg)](https://clojars.org/com.avisi-apps.gaps/log)

Google cloud logging compatible structured logging.

See the below example for usage examples:

```clojure
(ns example
  ;; First make sure you add this require
  (:require [com.avisi-apps.gaps.log :as log]))

;; You need to log maps, make sure to always add a `:message`
(log/info {:message "My human readable message"
           :extra-information {:tenant-id "foo"}
           :answer 42})

;; You can log errors
(log/error error {:message "Something horrible happend"
                  :debug-information {:input 13}})

;; Make sure to only log something as an error when necessary you can also use warn

(log/warn {:message "Something has happend which does not need our attention immediately"
           :error error ;; You can still give it the error and it will extract information from it
           })

;; You can also log debug message (are by default only enabled in shadow-cljs dev mode)
;; Will be removed on release builds
(log/debug {:message "debug logging"})
```

## Pro tip

There are two preloads which you can use in `shadow-cljs`. You can enable this by adding it as
a preload in your shadow-cljs.edn

For backend services use:

```clojure
{:devtools {:preloads [com.avisi-apps.gaps.log.preload]}}
```

For client-side builds use:

```clojure
{:devtools {:preloads [com.avisi-apps.gaps.log.preload-browser]}}
```

### Production elision of debug calls

To remove debug calls from release calls make sure the add the following alias
to your `deps.edn`. This alias should be enabled on most CI configs within
Avisi.

```clojure
:ci {:jvm-opts ["-Dtaoensso.telemere.ct-min-level=:info" "-Dtaoensso.timbre.min-level.edn=:warn"]}
```
