(ns avisi-apps.gaps.rcf.preload
  (:require hyperfiddle.rcf
            [avisi-apps.gaps.log :as log]))

(log/warn {:message "ENABLING HYPERFIDDLE PRELOAD (Make sure this not happening in production)"})

; wait to enable tests until after app namespaces are loaded
(hyperfiddle.rcf/enable!)

; subsequent REPL interactions will run tests

; prevent test execution during cljs hot code reload
(defn ^:dev/before-load stop [] (hyperfiddle.rcf/enable! false))
(defn ^:dev/after-load start [] (hyperfiddle.rcf/enable!))
