#!/bin/bash

EC2CMD="aws ec2 --region us-west-1"
spotRequestId=$1

if [ -z ${spotRequestId} ]
then
  echo "get-ec2-instances.sh <spotRequestId>"
  exit 1
fi

instanceIds=$(${EC2CMD} describe-spot-fleet-instances --spot-fleet-request-id $1 \
  |jq -r .ActiveInstances[].InstanceId |xargs echo -n)


${EC2CMD} describe-instances --instance-ids ${instanceIds} \
  |jq '.Reservations[].Instances[] | {PublicIpAddress: .PublicIpAddress, PrivateIpAddress: .PrivateIpAddress}' \
  |jq -s .
