#!/bin/bash

echo "Configuring hosts file..."
sed -i -r "s/.+(0.hdpf.aws.mine)/$(ifconfig eth0 |grep 'inet ' |awk '{print $2}') \\1/" /etc/hosts

echo "Restarting ambari-agent..."
ambari-agent restart

echo "Restarting ambari-server..."
ambari-server restart


echo "Starting service via Ambari REST API..."

function waitAmbariServiceStart {
  AMBARI_SERVICE=$1

  COUNTER=0
  while [ $COUNTER -lt 30 ]
  do
    STATE=$(curl -u admin:admin -s "http://localhost:8080/api/v1/clusters/HDPF/services/${AMBARI_SERVICE}" |jq -r .ServiceInfo.state)
    if [ ${STATE} == "STARTED" ]
    then
      echo "Service ${AMBARI_SERVICE} has started."
      break;
    fi
    let COUNTER+=1
    echo "Service ${AMBARI_SERVICE} has not started. state=${STATE} counter=${COUNTER}"
    sleep 10
  done
}

function startAmbariService {
  AMBARI_SERVICE=$1

  COUNTER=0
  while [ $COUNTER -lt 10 ]
  do
    HTTP_RES=$(curl -u admin:admin -s -o /dev/null -H "X-Requested-By: ambari" -w "%{http_code}" -XPUT -d \
     "{\"RequestInfo\": {\"context\" :\"Start ${AMBARI_SERVICE} via REST\"}, \"Body\": {\"ServiceInfo\": {\"state\": \"STARTED\"}}}" \
     "http://localhost:8080/api/v1/clusters/HDPF/services/${AMBARI_SERVICE}")

    echo Ambari returned ${HTTP_RES}

    # If we send start request too soon after server started, Ambari returns 200 instead of 202 since it does not know service state yet (UNKOWN).
    # In that case, we need to resend a request.
    if [ ${HTTP_RES} -eq 202 ]
    then
      echo "Service ${AMBARI_SERVICE} has accepted to start."
      break;
    fi
    let COUNTER+=1
    echo "Service ${AMBARI_SERVICE} has not accepted to start. counter=${COUNTER}"
    sleep 10
  done
}

for s in ZOOKEEPER AMBARI_INFRA AMBARI_METRICS \
  HDFS YARN MAPREDUCE2 HBASE HIVE DRUID \
  REGISTRY KAFKA NIFI STORM STREAMLINE SMARTSENSE
do
  startAmbariService $s
  waitAmbariServiceStart $s
done

