# 2. Handling additional logging service data

Date: 06-01-2023

## Status

Accepted

## Context

In order te pass all the information needed for the Rollbar external logging service, we need to find a good solution to implement this in the existing code.

The solution decided on in this decision will affect the call all the projects that implement the logging library. 
It wil also affect the inner structure of the logging project.

Currently, the way a project uses the log function before implementing the additional date in order to send a debug log message:

```Clojure
(log/debug {:message "this is an example"})
```
We will modify this call in every option in order to create a better insight of how te solution looks like when implemented.

## Requirements

For our implementation we want to choose a method that is:

- Easy to expand in the future with additional variables.
- Has a clean implementation and allows to have a good maintainability.
- Does not break the existing library when implemented.

At the moment of writing the following two variables are expected to be added to the current library call: 

- Rollbar access token
- fulcro application

Fulcro application is an important value. This value allows us to look in the Fulcro state. With this state can achieve two goals:

- Allow us to see whether the user has given permission to log his data to an external service.
- Allows us the see, and export the data at the moment that the error occured, it also allows us to see the history of the data and changes to the state leading up to the error.

## Options

### Adding variables to existing logger calls

Adding variables is the most obvious option. 
This means that every time the library is used the two  values need to be added to each function call.

The new function call with this option wil look like:

```Clojure
(log/debug {:message "this is an example"} token fulcro-application)
```

**Advantages**

The advantages of this solution is that it is relative easy to implement from an architectural standpoint.

**Disadvantages**

this option has the following disadvantages:

- This option comes at the cost of expandability in the future, with this solution every variable added or removed has to be put through all the functions in the library as an argument.
- Since the library is already in use by one app and, the other two libraries in this project this update wil resort in a lot of breaking changes.

### Optional map

It is possible to introduce optional variables to a function. This wil allow the option of giving details about the external logging service.
It gives the developer the control to choose if he wants a error message to potentially go to Rollbar.
While not introducing breaking changes.

By adding a map to the function call will look like this:
```Clojure
(log/debug {:message "this is an example"} {:log/token token :app fulcro-application}  )
```
It is also possible to make call using a preconfigured map stored in a namespace, this could look something like:
```Clojure
(log/debug {:message "this is an example"}  (loggerConfig/get-preconfigered-rollbar-config))
```

**Advantages**

this option has the following advantages:

- When using a map it is easy to add variables later on in development, since the map already exists in most functions.
- Does not cause breaking changes when introduced to project already implementing this library. 
- This solution offers good maintainability. 
- Allows for the option to have the optional map in a preconfigured state where a part of the data is already filled in.

**Disadvantages**

this option has the following disadvantages:

- On implementation the optional map needs to be added as a parameter to most of the function currently in the log library.
- Needs to handle the additional case of the map not being supplied 
- Needs to handle the additional case of missing needed values.

### Separating the logging service from the existing log code

Keeping the implementation of the external logging service separate, by adding a second function for when the developer wants to go to the external logging service.

An example in code if this implementation wil look something like:

```Clojure
(let [payload {:message "this is an example"}]
(log/debug payload)
(log/debug-to-logging-service payload token fulcro-application))
```

**Advantages**

this option has the following advantages:

- This had the benefit of not introducing breaking changes to the projects that are currently implementing this library already.
- Allows for the option to introduce separate custom implementations for each aspect of the logging library
- The developer can choose what error message get potentially send to the external logging service.

**Disadvantages**

this option has the following disadvantages:

- The error payload need to be stored temporary in a let in order to give it to both function, this results in the need to update al the current uses in order to take advantage of this new feature.
- Makes adding logging more work since it requires more code to be written per logging event in the code base.
- introduce either dependencies between the two sides or introducing code duplication.
- lowers the maintainability of the library.

### Adding variables to the current map

Instead of adding a new argument to the function like in the previous options it is possible to add the new variables to the existing map.
Implementing this solution this would mean that the code looks te following:

```Clojure
(log/debug {:message "this is an example" :log/token token :app fulcro-application})
```

**Advantages**

this option has the following advantages:

- There is no need to modify the existing function within the log library, the variables will automatically be passed through the functions.
- This does not introduce breaking changes when updating the library in project that the library currently exists in.
- Allows for easy adding/removing of more variables later.

**Disadvantages**

this option has the following disadvantages:

- Makes one general implementation providing the variables harder to realise.
- Adds and addition case for when one or more of the variables are not provided.
- The variables or a general implementation of them must be added to al existing function calls to the library

### Creating an initialize function

Instead of adding the needed information to the log library call. 
It is possible to set the needed values upon start of the application, and saving it in the library for when it is needed.

The implementation of this option wil be a bit more difficult. It requires the following steps:

- Modify the mount function of the app to add a function to the rollbar package with the needed information.
- Create a new namespace in het rollbar package that can be called upon initialization, and add the functionality to store the needed information.
- create a function in core to read the stored variables in order to work with them.

**Advantages**

- The biggest advantage of this method is that we can set it upon initialization of the app, after that we don't have to provide the information again.
- No breaking changes are introduced when using this option.
- All existing uses of the logging library can use the external logging service without modifiation needed.  
- This implementation does not change the maintainability of the library.

**Disadvantages**

- When updating the logging library in existing project the initialisation of the app has to be modified.
- When introducing this library new to project the initialisation function has to be modified.


## Decision

Given the advantages and disadvantages the chosen solution is the option to create an initialize function.
This option has the best maintainability compared to the other options. Another strong reason is the fact that the variables only need to be set once, this is a by far better implementation over the other options which require constant input.
The disadvantage of being harder to update/integrate into project comes from a bit of needed additional knowledge of the app, however this can be easily explained through a readme.md or similar document, and only needs to be done once per project.

The other options have more disadvantages, and their main disadvantage that they all cause consistent need for writing more code for each call made to the library due to the constant need for the additional variables.
There is also the disadvantage for existing implementations that need to be updated individually to enable the feature to use the remote logging service.
