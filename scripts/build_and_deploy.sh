#/bin/bash

REGION="us-south"
RESOURCE_GROUP="book-bot"
FUNCTIONS_NAMESPACE="book-bot"
ACTION_NAME="book-bot"

BUILD_DIR="lib/build"
JAR_PATH="lib/build/libs/lib-all.jar"

rm -r $BUILD_DIR 2> /dev/null

./gradlew shadowJar

ibmcloud target -r $REGION
ibmcloud target -g $RESOURCE_GROUP
ibmcloud fn namespace target $FUNCTIONS_NAMESPACE

# Do deploy using action update (aka create or update) command
ibmcloud fn action update $ACTION_NAME $JAR_PATH \
  --main "com.mattwelke.packtbookbot.Function" \
  --docker "owextendedruntimes/java-17:experiment-abstract-class-impl-1634521420"
