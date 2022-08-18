(ns avisi-apps.gaps.rcf.shadow-cljs.hook
  (:require
    [shadow.build :as build]
    [shadow.build.targets.node-test :as node-test-target]
    [shadow.build.test-util :as tu]
    [shadow.build.classpath :as cp]
    [shadow.jvm-log :as log]))

(defn hook
  "This is a hook which runs before compiling test namespaces and finds all namespaces that use
  `hyperfiddle.rcf` because those namespaces contain test which we need to run on CI"
  {::build/stage :compile-prepare}
  [{::build/keys [config]
    :as state}
   &
   _]
  (let [test-namespaces (into
                          (tu/find-test-namespaces state config)
                          (->
                            (cp/find-resources-using-ns (:classpath state) 'hyperfiddle.rcf)
                            ;; Dev is a preload for running with firebase so exclude it
                            (disj 'dev 'avisi-apps.gaps.rcf.preload)))]
    (log/info
      ::hook
      {:test-namespaces test-namespaces
       :config config})
    (node-test-target/test-resolve (assoc-in state [::build/config :namespaces] test-namespaces))))
