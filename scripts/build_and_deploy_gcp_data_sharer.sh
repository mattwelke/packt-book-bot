#/bin/bash

# Builds and deploys the GCP data sharer action as an IBM Cloud Functions function using the Java 17
# runtime from the extended runtimes project. Assumes triggers, sequences, etc
# that are related to this action have already been created.

# Script uses command line params instead of env vars to help with running it
# locally too, not just with CI/CD.

REGION="us-south"
RESOURCE_GROUP="book-bot"
FUNCTIONS_NAMESPACE="book-bot"
ACTION_NAME="gcp-data-sharer"

BUILD_DIR="gcp-data-sharer/build"
JAR_PATH="${BUILD_DIR}/libs/gcp-data-sharer-all.jar"

rm -r $BUILD_DIR 2> /dev/null

./gradlew gcp-data-sharer:shadowJar

ibmcloud login --apikey $1

ibmcloud target -r $REGION
ibmcloud target -g $RESOURCE_GROUP
ibmcloud fn namespace target $FUNCTIONS_NAMESPACE

# Do deploy using action update (aka create or update) command
ibmcloud fn action update $ACTION_NAME $JAR_PATH \
  --main "com.mattwelke.packtbookbot.GcpDataSharerAction" \
  --docker "mwelke/openwhisk-runtime-java-18:202204230659" \
  --param gcpCreds $2
