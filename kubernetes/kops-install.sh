#!/usr/bin/env bash

# Define environment variables
export KOPS_STATE_STORE=$(< ~/SECRETS/aws-kops-cfg-bucket.txt)
export KOPS_ROUTE53_ZONEID=$(< ~/SECRETS/aws-kops-cfg-zoneid.txt)
export KOPS_ROUTE53_DOMAIN=$(< ~/SECRETS/aws-kops-cfg-domain.txt)
export KOPS_TRUSTED_IPS=$(< ~/SECRETS/aws-kops-cfg-trusted-ips.txt)
export AWS_ACCESS_KEY_ID=$(< ~/SECRETS/aws-kops-key-access.txt)
export AWS_SECRET_ACCESS_KEY=$(< ~/SECRETS/aws-kops-key-secret.txt)

# Parameters
export AWS_REGION='eu-west-1'
export AWS_VPC_CIDR='10.77.0.0/16'
export KOPS_CLUSTER_NAME="${KOPS_ROUTE53_DOMAIN}"

# Print variables
echo "AWS_REGION='${AWS_REGION}'"
echo "KOPS_STATE_STORE='${KOPS_STATE_STORE}'"
echo "AWS_ACCESS_KEY_ID='${AWS_ACCESS_KEY_ID}'"
echo "KOPS_CLUSTER_NAME='${KOPS_CLUSTER_NAME}'"
echo "KOPS_TRUSTED_IPS='${KOPS_TRUSTED_IPS}'"

# Build the k8s cluster using kops
KOPS_DRY_RUN="--dry-run --output yaml"
/usr/bin/kops create cluster \
              --cloud aws \
              --name=${KOPS_CLUSTER_NAME} \
              --dns-zone=${KOPS_CLUSTER_NAME} \
              --state=${KOPS_STATE_STORE} \
              --master-count=1 --master-zones=${AWS_REGION}a --master-size=t2.micro --master-volume-size 8 \
              --node-count=2 --zones ${AWS_REGION}a,${AWS_REGION}b --node-size=t2.micro --node-volume-size 10 \
              --network-cidr "${AWS_VPC_CIDR}" \
              --admin-access "${AWS_VPC_CIDR},${KOPS_TRUSTED_IPS}" \
              --ssh-access "${KOPS_TRUSTED_IPS}" \
              --yes
