# 3. Implementation of source mapping

Date: 10-01-2023

## Status

Accepted

## Context

One of the desired features for the logging service to implement is source mapping. 
Source mapping allows the compiled code to be translated back towards the readable code a developer can read.

In order to implement sourcemapping, we need to implement the following points:

- How to make a sourcemap and keep it private.
- How to get the sourcemap to the external logging service.
- How to deconstruct en send the error for it can be translated.

In this ADR we are going to look at these three points and see how we can implement them and what the best option is.

## Options

### Creating the source map

For this section we are concerned about making the sourcemap, here we need to meet the following requirements:

- We need to be able to keep the sourcemap private, the user should not be able to see the sourcemap through the developer tools in their browser.

In shadow-cljs it is possible to [make a source-map](https://shadow-cljs.github.io/docs/UsersGuide.html#compiler-options/) during the compilation of a project.
This allows us to generate the sourcemap during the build pipeline of the project, with very little modification needed.

### Sending the sourcemap to Rollbar

In this section we are looking at how we are going to send our sourcemap to Rollbar.
Because we selected the Rollbar service we do not have to look at external libraries or other sources to find potential solutions.
The source for this section is the [Rollbar documentation](https://docs.rollbar.com/docs/source-maps/)

According to the documentation from Rollbar the following shell command must be used:

```shell
curl https://api.rollbar.com/api/1/sourcemap \
-F access_token={accessToken} \
-F version={code_version} \
-F minified_url={minified_url} \
-F source_map=@{source_map}
```

This command can be used local when developing or via the CI/CD pipeline for production builds. 
In addition to the ability to use the shell command it is possible to upload sourcemaps to Rollbar via the UI. This methode can also be used for testing purposes.

### Deconstructing error to external logging service

In order send the error to Rollbar to transform it into readable code, we must deconstruct the error into frames.
Frames are a term used by Rollbar in their api spec for a piece of the stacktrace. 

Deconstructing the stacktrace is possible via a library. 
There are multiple options available, however this [npm library](https://www.npmjs.com/package/error-stack-parser) is the one used by Rollbar themselves looking at the code of their [SDK](https://github.com/rollbar/rollbar.js/blob/master/src/errorParser.js#L21).

For this reason this library is chosen to be used for the implementation of this feature. 
Following this we need to evaluate each function whether it is a Javascript or ClojureScript error. 
If the error is in ClojureScript we have to convert it to Javascript before parsing it with the library.
After parsing, a map with frames is returned. 
These frames however contain the wrong keys and unnecessary values for our specific implementation.
We need to alter the keys and trim the frames. After that they are ready to be send to Rollbar.

