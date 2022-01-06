#/bin/bash

# Deploys the function to IBM Cloud Functions using the Java 17 runtime
# from the extended runtimes project.
#
# Script uses command line params instead of env vars, to help with
# running it locally too, not just with CI/CD.

REGION="us-south"
RESOURCE_GROUP="book-bot"
FUNCTIONS_NAMESPACE="book-bot"
ACTION_NAME="book-bot"

BUILD_DIR="lib/build"
JAR_PATH="lib/build/libs/lib-all.jar"

rm -r $BUILD_DIR 2> /dev/null

./gradlew shadowJar

ibmcloud login --apikey $1

ibmcloud target -r $REGION
ibmcloud target -g $RESOURCE_GROUP
ibmcloud fn namespace target $FUNCTIONS_NAMESPACE

# Do deploy using action update (aka create or update) command
ibmcloud fn action update $ACTION_NAME $JAR_PATH \
  --main "com.mattwelke.packtbookbot.Function" \
  --docker "owextendedruntimes/java-17:experiment-abstract-class-impl-1634521420" \
  --param twitterConsumerKey $2 \
  --param twitterConsumerSecret $3 \
  --param twitterToken $4 \
  --param twitterTokenSecret $5
