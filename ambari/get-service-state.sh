#!/bin/bash

AMBARI_SERVER=$1
AMBARI_SERVICE=$2

if [ -z ${AMBARI_SERVER} ] || [ -z ${AMBARI_SERVICE} ]
then
  echo "get-ambari-service-state.sh <ambari-server> <ambari-service>"
  exit 1
fi

curl -u admin:admin -s http://${AMBARI_SERVER}:8080/api/v1/clusters/hdf/services/${AMBARI_SERVICE} \
  |jq -r .ServiceInfo.state
