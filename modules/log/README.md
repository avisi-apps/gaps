# Log [![Clojars Project](https://img.shields.io/clojars/v/com.avisi-apps.gaps/log.svg)](https://clojars.org/com.avisi-apps.gaps/log)

Google cloud logging compatible structured logging.

# Usage
This project uses pino (used to be bunyan but that is unmaintained) for it's logging make sure you added that to your dependencies by running:

```shell
yarn add --exact pino
```

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
If you want to make the output of logging more readable you can use `pino-pretty`. For example

```shell
yarn node index.js | yarn pino-pretty
```
