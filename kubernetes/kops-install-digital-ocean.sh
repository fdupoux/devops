#!/usr/bin/env bash

# Define environment variables
export KOPS_STATE_STORE=$(< ~/SECRETS/digital-ocean-kops-cfg-bucket.txt)
export DIGITALOCEAN_ACCESS_TOKEN=$(< ~/SECRETS/digital-ocean-kops-general-token.txt)
export S3_ENDPOINT=$(< ~/SECRETS/digital-ocean-kops-s3-endpoint.txt)
export S3_ACCESS_KEY_ID=$(< ~/SECRETS/digital-ocean-kops-s3-key-access.txt)
export S3_SECRET_ACCESS_KEY=$(< ~/SECRETS/digital-ocean-kops-s3-key-secret.txt)
export KOPS_CLUSTER_NAME=$(< ~/SECRETS/digital-ocean-kops-cfg-cluster-name.txt)
export KOPS_TRUSTED_IPS=$(< ~/SECRETS/aws-kops-cfg-trusted-ips.txt)

# Parameters
export KOPS_DIGITAL_OCEAN_REGION='ams3'
export KOPS_DIGITAL_OCEAN_NODESPEC_MASTERS='s-1vcpu-2gb'
export KOPS_DIGITAL_OCEAN_NODESPEC_SLAVES='s-1vcpu-2gb'

# Print variables
echo "KOPS_STATE_STORE='${KOPS_STATE_STORE}'"
echo "KOPS_CLUSTER_NAME='${KOPS_CLUSTER_NAME}'"
echo "KOPS_TRUSTED_IPS='${KOPS_TRUSTED_IPS}'"

# DigitalOcean support is currently alpha
export KOPS_FEATURE_FLAGS=AlphaAllowDO

# Build the k8s cluster using kops
KOPS_DRY_RUN="--dry-run --output yaml"
/usr/bin/kops create cluster \
              --cloud digitalocean \
              --name=${KOPS_CLUSTER_NAME} \
              --dns-zone=${KOPS_CLUSTER_NAME} \
              --state=${KOPS_STATE_STORE} \
              --networking=flannel \
              --zones=${KOPS_DIGITAL_OCEAN_REGION} \
              --image=debian-9-x64 \
              --master-count=1 --master-size=${KOPS_DIGITAL_OCEAN_NODESPEC_MASTERS} \
              --node-count=2 --node-size=${KOPS_DIGITAL_OCEAN_NODESPEC_SLAVES} \
              --admin-access "${KOPS_TRUSTED_IPS}" \
              --ssh-access "${KOPS_TRUSTED_IPS}" \
              --ssh-public-key=~/.ssh/id_rsa.pub \
              --yes
