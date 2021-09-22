#!/bin/bash

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