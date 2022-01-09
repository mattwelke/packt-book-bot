# packt-book-bot

[![CodeQL](https://github.com/mattwelke/packt-book-bot/actions/workflows/codeql-analysis.yml/badge.svg?branch=main)](https://github.com/mattwelke/packt-book-bot/actions/workflows/codeql-analysis.yml)

Twitter bot (https://twitter.com/PacktBookBot) that tweets on a schedule.

![Screenshot of free eBook of the day tweet](img/free_example.png)

## Current features

- Free eBook of the day - every day at 12:01am UTC

## Future features

- Monitoring for new titles, posting them as they are released (only if Packt Twitter handle doesn't do this already)

## Tech stack

Written using Java 17, deployed to IBM Cloud Functions using the custom Java 17 runtime from https://github.com/ow-extended-runtimes/java-17.

**Why Java?**

* I wanted to practice my Java.
* For this use case, slow startup speed (about half a second in this case) is good enough for the use case.

**Why IBM Cloud?**

* They have a good free tier for FaaS functions.
  * This includes invocations.
  * This also includes periodic scheduled events to trigger them ([OpenWhisk alarms](https://github.com/apache/openwhisk-package-alarms/blob/master/provider/lib/cronAlarm.js)).
* Their FaaS service is powered by the open source FaaS project [Apache OpenWhisk](https://openwhisk.apache.org/)! 

