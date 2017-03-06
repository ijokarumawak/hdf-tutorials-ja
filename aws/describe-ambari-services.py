import sys
import subprocess
import json

if len(sys.argv) < 2:
  print "spotRequestId was not specified."
  quit()

spotRequestId = sys.argv[1]

p = subprocess.Popen(["aws/get-ec2-instances.sh", spotRequestId],
        stdout=subprocess.PIPE, stderr=subprocess.PIPE)
out, err = p.communicate()
ec2Instances = json.loads(out)

for ec2Instance in ec2Instances:

  p = subprocess.Popen(["ambari/get-service-states.sh", ec2Instance['PublicIpAddress']],
          stdout=subprocess.PIPE, stderr=subprocess.PIPE)
  out, err = p.communicate()
  ambari = {'Services': json.loads(out)}
  ec2Instance['Ambari'] = ambari

print json.dumps(ec2Instances, ensure_ascii=False)
