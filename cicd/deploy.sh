#!/bin/bash

echo "Running on branch $CODEBUILD_WEBHOOK_TRIGGER"

if [[ $DEPLOYMENT_ENVIRONMENT == "production" ]]
then
  echo "Deploying to production..."
elif [[ $DEPLOYMENT_ENVIRONMENT == "staging" ]]
then
  echo "Deploying to staging..."
else
  echo "Undefined environment detected"
  exit 2
fi