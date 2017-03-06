#!/bin/bash

AMBARI_SERVER=$1

if [ -z ${AMBARI_SERVER} ]
then
  echo "get-ambari-service-states.sh <ambari-server>"
  exit 1
fi

echo "{"
for AMBARI_SERVICE in ZOOKEEPER AMBARI_INFRA AMBARI_METRICS NIFI KAFKA STORM
do
  STATE=$(ambari/get-service-state.sh ${AMBARI_SERVER} ${AMBARI_SERVICE})
  echo -n "\"${AMBARI_SERVICE}\": \"${STATE}\""
  if [ ${AMBARI_SERVICE} != "STORM" ]; then echo ','; fi
done
echo 
echo "}"
