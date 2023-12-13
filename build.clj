(ns build
  (:require [clojure.core.server :as server]
            [clojure.string :as str]
            [clojure.tools.build.api :as b]
            [org.corfield.build :as bb]
            [clojure.java.io :as io]
            [clj-yaml.core :as yaml])
  (:import [java.io File]))

(defn name->module [n]
  (symbol "com.avisi-apps.gaps" n))

(def base-version "0.0")

(def scm-url "git@github.com:avisi-apps/gaps.git")

(def current-tag (b/git-process {:git-args "describe --tags --exact-match"}))
(println "Current tag = " current-tag)

(def version
  (if current-tag
    (subs current-tag 1)
    (format "%s.%s-SNAPSHOT" base-version (b/git-count-revs nil))))
(println "Version = " version)

(def modules-folder (b/resolve-path "modules"))

(def current-branch (b/git-process {:git-args "branch --show-current"}))
(println "Current branch = " current-branch)

(def release-branch "master")

(def modules (->>
               (.listFiles ^File modules-folder)
               (filter #(.isDirectory ^File %))
               (mapv (fn [^File file] (name->module (.getName file))))))

(println "Modules found = " modules)

(defn sha
  [{:keys [dir path] :or {dir "."}}]
  (some-> {:command-args (cond-> ["git" "rev-parse" "HEAD"]
                                 path (conj "--" path))
           :dir (.getPath (b/resolve-path dir))
           :out :capture}
          b/process
          :out
          str/trim))

(defn local-lib->mvn-lib [[k {:deps/keys [manifest] :as v}]]
  (if (= manifest :deps)
    [k {:mvn/version version
        :deps/manifest :mvn
        :parents #{[]}}]
    [k v]))

(defn module-dir [{:keys [lib]}]
  (format "./modules/%s" (name lib)))

(defn build-module [{:keys [lib] :as opts}]
  (let [dir (module-dir opts)
        module-sha (sha {})]
    (with-bindings {#'clojure.tools.build.api/*project-root* dir}
      (let [basis (-> (b/create-basis)
                      (update :libs #(into {} (map local-lib->mvn-lib) %)))
            class-dir (bb/default-class-dir)
            opts (assoc opts
                   :version version
                   :basis basis
                   :class-dir class-dir
                   :jar-file (bb/default-jar-file (bb/default-target) lib version)
                   :scm {:tag (or current-tag module-sha)
                         :connection (str "scm:git:" scm-url)
                         :developerConnection (str "scm:git:" scm-url)
                         :url scm-url}
                   :pom-data [[:licenses
                               [:license
                                [:name "MIT License"]
                                [:url "https://opensource.org/license/mit/"]]]])]
        (println "\nWriting pom.xml...")
        (b/write-pom opts)
        (println "\nCopying files...")
        (b/copy-dir {:src-dirs ["src" "resources"]
                     :target-dir class-dir})
        (println "\nBuilding jar...")
        (b/jar opts)
        (println "\nInstalling jar...")
        (b/install opts)
        opts))))

(defn build [opts]
  (run!
    (fn [module]
      (println "Building module " module)
      (build-module {:lib module})
      (println "Built module " module))
    modules)
  opts)

(defn clean [opts]
  (run!
    (fn [module]
      (println "Cleaning module " module)
      (b/delete {:path (str (module-dir {:lib module}) "/target")})
      (println "Cleaned module " module))
    modules)
  opts)

(defn release-module [{:keys [lib] :as opts}]
  (println "Releasing module...")
  (with-bindings {#'clojure.tools.build.api/*project-root* (module-dir opts)}
    (->
      (assoc opts
        :version version
        :artifact (str (module-dir opts) "/" (bb/default-jar-file lib version)))
      (bb/deploy))))

(defn release [opts]
  (clean opts)
  (run!
    (fn [module]
      (->
        opts
        (assoc
          :lib module)
        (build-module)
        (release-module))) modules))

(defn update-changelog! [new-version]
  (let [changelog (slurp "CHANGELOG.md")
        updated-changelog (-> changelog
                              (str/replace "## [Unreleased]"
                                           (str "## [Unreleased]
### Added

### Changed

### Fixed

## [" new-version "]"))
                              (str/replace #"(?m)^\[Unreleased\]:.*"
                                           (str
                                             (format "[Unreleased]: https://github.com/avisi-apps/gaps/compare/%s...HEAD\n" new-version)
                                             (format "[%s]: https://github.com/avisi-apps/gaps/releases/tag/%s" (subs new-version 1) new-version))))]
    (spit "CHANGELOG.md" updated-changelog)))

(defn generate-release-tag [_]
  (when (not= current-branch "master")
    (throw (ex-info (format "You can only release from the master branch not: %s" current-branch) {:current-branch current-branch})))

  (let [new-version (format "v%s.%s" base-version (b/git-count-revs nil))]
    (println (format "Updating changelog (moving everything from Unreleased to %s)" new-version))
    (update-changelog! new-version)
    (println "Updated changelog")

    (println "Add new changelog")
    (b/git-process
      {:git-args "add CHANGELOG.md"})
    (println "Commit new changelog")
    (b/git-process
      {:git-args ["commit" "-m" (format "Prepare release %s" new-version)]})
    (println "Creating new release tag: " new-version)
    (b/git-process
      {:git-args (str "tag " new-version)})
    (println "Created new release tag: " new-version)
    (println "Pushing master and new release tag: " new-version)
    (b/git-process {:git-args (str "push origin master")})
    (b/git-process {:git-args (str "push origin " new-version)})
    (println "Pushed new release tag: " new-version)))

(defn repl [{:keys [port] :or {port 5555}}]
  (server/start-server {:name "build repl"
                        :port port
                        :accept 'clojure.core.server/repl})
  (println "Started repl server on port: " port)
  @(promise))

(defn changed-files-from-release-branch []
  (when (and current-branch (not= release-branch current-branch))
    (->
      (b/git-process {:git-args "diff master --name-only"})
      (str/split-lines))))

(defn file-path->affected-module [p]
  (some->
    (re-find #"^modules\/([^\/]*)\/.*$" p)
    second))

(comment
  (file-path->affected-module "modules/log/README.md") := "log"
  (file-path->affected-module "README.md") := nil)


(defn gen-workflow-for-module [])

(defn generate-ci-config [opts]
  (let [changed-modules (into #{}
                              (keep file-path->affected-module)
                              (changed-files-from-release-branch))
        _ (println "loading base config")
        base-config (->
                      (io/file ".circleci/continue-config.yml")
                      (slurp)
                      (yaml/parse-string))

        _ (println "loaded base config")
        config-with-changed-modules (reduce
                                      (fn [config module]
                                        ;; Here we could possibly add extra build test for certain modules
                                        config)
                                      base-config
                                      changed-modules)]
    (println "Generate generated-config.yml")
    (spit (io/file "generated-config.yml")
          (yaml/generate-string config-with-changed-modules :dumper-options {:flow-style :block}))))

(comment
  (generate-ci-config {})

  version


  (def base-config (->
                     (io/file ".circleci/continue-config.yml")
                     (slurp)
                     (yaml/parse-string)))

  (assoc-in base-config [:workflows])


  (let [release-branch "master"
        current-branch (b/git-process {:git-args "branch --show-current"})]
    (when (not= release-branch current-branch)
      (->
        (b/git-process {:git-args "diff master --name-only"})
        (str/split-lines))))

  (into #{}
        (keep file-path->affected-module)
        (changed-files-from-release-branch))

  (build {})


  (release-module {:lib 'com.avisi-apps.gaps/rcf})


  (build-module {:lib 'com.avisi-apps.gaps/log})

  )
