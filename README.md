# packt-book-bot

Twitter bot (https://twitter.com/PacktBookBot) that tweets on a schedule.

![Screenshot of free eBook of the day tweet](img/free_example.png)

## Current features:

- Free eBook of the day - every day at 12:01am UTC

## Future features:

- Monitoring for new titles, posting them as they are released (only if Packt Twitter handle doesn't do this already)

## Tech stack

Written using Java 17, deployed to IBM Cloud Functions using the custom Java 17 runtime from https://github.com/ow-extended-runtimes/java-17.

**Why Java?** I wanted to practice my Java.

**Why IBM Cloud?** They have a good free tier for FaaS functions and periodic scheduled events to trigger them.
