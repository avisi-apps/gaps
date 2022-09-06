# Reitit Express [![Clojars Project](https://img.shields.io/clojars/v/com.avisi-apps.gaps/reitit-express.svg)](https://clojars.org/com.avisi-apps.gaps/reitit-express)

Library to use reitit with clojurescript with [Express](http://expressjs.com). This makes it possibble to use reitit easily
for Google Cloud functions or Google Cloud run with minimal differences.


# Usage
This project uses a few javascript dependencies:

| npm dependency                                                | Usage                                                                            | required                                          |
|---------------------------------------------------------------|----------------------------------------------------------------------------------|---------------------------------------------------|
| [express](https://www.npmjs.com/package/express)              | The nodejs webserver                                                             | *                                                 |
| [cookie-parser](https://www.npmjs.com/package/cookie-parser) | This is the middleware that parses incoming cookies                              | *                                                 |
| [negotiator](https://www.npmjs.com/package/negotiator)       | This is used when you use the negotiation middleware, this parses accept headers | only when using the content-negotation middleware |

You can quickly add them as follows:

```shell
yarn add --exact express negotiator cookie-parser
```

# Getting started

There are currently two code examples on how to get started with this library:

* Getting started with [cloud run](../../examples/cloud-run-example)
* Getting started with a [firebase function](../../examples/firebase-example)
