# RCF [![Clojars Project](https://img.shields.io/clojars/v/com.avisi-apps.gaps/rcf.svg)](https://clojars.org/com.avisi-apps.gaps/rcf)

This is helper module to automate enabling [hyperfiddle/rcf](https://github.com/hyperfiddle/rcf) or running all of your 
rcf tests on CI.

# Usage

## Adding a dev preload

For enabling hyperfidde you can add the following preload to shadow-cljs `avisi-apps.gaps.rcf.preload`. This looks as 
follows:

```clojure
:devtools {:preloads [avisi-apps.gaps.rcf.preload]}
```

## Extracting tests for CI

When you write a lot of inline test it would be nice if those tests get ran on CI. There is hook for that 
`avisi-apps.gaps.rcf.shadow-cljs.hook` below is a example build for creating a build to run NodeJS tests:


```clojure
{:target :node-test
 :js-options {:js-package-dirs ["modules/test/node_modules"]
              :resolve {"firebase" {:target :npm
                                    :require "firebase/compat/app"}}}
 :output-to "modules/test/index.js"
 :ns-regexp "-test$"
 :build-hooks [(avisi-apps.gaps.rcf.shadow-cljs.hook)]}
```
