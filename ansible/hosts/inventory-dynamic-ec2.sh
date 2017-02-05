#!/bin/bash
# Requires AWS environment variables to be defined

#echo "{}" && exit 0 # Disable this inventory script for now

basedir="$(dirname $0)"

# AWS Access Keys required unless running from an EC2 instance with assumed role
if [ -n "${AWS_ACCESS_KEY}" ] && [ -n "${AWS_SECRET_KEY}" ]
then
    export AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY}"
    export AWS_SECRET_ACCESS_KEY="${AWS_SECRET_KEY}"
else
    echo "Warning: AWS_ACCESS_KEY and AWS_SECRET_KEY undefined" >&2
fi

# Execute the actual EC2 inventory script
/usr/bin/env python ${basedir}/../hosts-common/ec2.py --refresh-cache
