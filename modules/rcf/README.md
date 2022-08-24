# RCF [![Clojars Project](https://img.shields.io/clojars/v/com.avisi-apps.gaps/rcf.svg)](https://clojars.org/com.avisi-apps.gaps/rcf)

This is helper module to automate enabling [hyperfiddle/rcf](https://github.com/hyperfiddle/rcf) or running all of your 
rcf tests on CI.

# Usage

## Adding a dev preload

For enabling hyperfidde you can add the following preload to shadow-cljs `com.avisi-apps.gaps.rcf.preload`. This looks as 
follows:

```clojure
:devtools {:preloads [com.avisi-apps.gaps.rcf.preload]}
```

## Extracting tests for CI

When you write a lot of inline test it would be nice if those tests get ran on CI. There is hook for that 
`com.avisi-apps.gaps.rcf.shadow-cljs.hook` below is an example build for creating a build to run NodeJS tests:


```clojure
{:target :node-test
 :output-to "modules/test/index.js"
 :ns-regexp "-test$"
 :build-hooks [(com.avisi-apps.gaps.rcf.shadow-cljs.hook/hook)]}
```
