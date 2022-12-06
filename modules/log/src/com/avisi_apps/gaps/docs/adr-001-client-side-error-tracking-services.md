# 1. Client-side error tracking services

Date: 25-10-2022

## Status

Accepted

## Context

For our implementation of a client-side error tracking in our apps. 
We are looking if there is an already existing error tracking service available that meets as many requirements out of the box as possible, that we can integrate with.
For this we are looking at their feature set, alongside it would be nice to have some type of analytics available besides, these functionalities it is important that the service has good security features and is compliant with privacy laws.

## Options

There are many options when it comes to client side error tracking services. In this ADR we are going to look at the following 5:

- [Rollbar](https://rollbar.com/)
- [LogRocket](https://logrocket.com/for/how-to-log-client-side-javascript-errors/)
- [Elmah.io](https://elmah.io/features/clientside-logging/)
- [Sentry](https://sentry.io/welcome/)
- [Solarwinds Loggly](https://www.loggly.com/blog/best-practices-for-client-side-logging-and-error-handling-in-react/)

## Requirements

The services need to meet the following requirements.

**Security:**
 
- Compliant with EU privacy laws.
- Good security certificates such as SOC2, ISO-27001 & GDPR compliance.

**Functionality:**

- Ability to send direct error messages in order to avoid the console.
- Support for sourcemaps.
- Build-in support to include browserinformation with the log messages.
- Metrics and Notification. Would be nice to have, either in the product or an option to integrate with a service that provides that.

**Pricing:**

- We want to pay on a based on amount of messages per month. So we don't overpay for functionality we do not need or for unused bandwidth.

## Decision

### Chosen solution: Rollbar

Given the requirements Rollbar is the service that meets our requirements the best. 
It meets our must-have requirements:

- Security wise they have many standards and laws they are compliant with, including SOC2, ISO-27001 & GDPR.
- Good documentation API documentation available.
- Support for sourcemaps build-in.
- Support for browser information, however it is not clear if this data is collected for us or if we need to do that ourselves.
- They also have good pricing, the tiers they specify are mainly separated bij the amounts of events (messages/errors) a month. Going up in tiers from a feature standpoint is not needed since the lowest tier contains every feature we need.

And the nice to have requirement:
- Rollbar has good integration with the other tools witch we use such as: Slack, Github and Jira, for these integrations there are docs provided by Rollbar on how to set them up.

### Not chosen solutions

**Sentry**

Sentry is a good option it contains all the must-have features:

- Sentry has similar security certificates (SOC2 & ISO-27001).
- It has good support for the requirements we specified.
- It supports sourcemaps with the ability to send them straight tru to Sentry without making it public.
- It also offers a similar pricing structure compared to Rollbar.

However, it was not chosen due to:

- No listed GDPR compliance.

**Elmah.io**

Elmah.io was not chosen due to a lack of features compared to other services:

- It does not support sourcemaps.
- Has no SOC2 or ISO-27001 compliance listed, only GDPR compliance.

**Solarwinds Loggly**

Loggly was not chosen due to lack of required features such as:

- No support for sourcemaps in their documentation. 

Also their API documentation is not as comprehensive compared to the other options.

**LogRocket**

LogRocket works mainly based on listening to the already existing error calls from javascript this means that all error's have to be publicly thrown which makes them visible for our customers.
This is problematic for our wish to use sourcemaps because they have to public in order to end up in LogRocket.

The documentation confirms that sourcemaps do not work when they are kept private. Because of LogRockets implementation it would not be practical for us to implement. We want to log only when the user gives concent, so we need to disable this functionality from LogRocket.
LogRocket also collects a lot of data we don't want to collect that we need to disable since it seems to be active by default.

At last pricing on LogRocket is per session, This is because LogRocket has a strong focus on user monitoring and behavioural insight. This is not favorable for us.
