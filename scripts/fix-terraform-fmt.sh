#!/bin/bash

# This script downloads and runs terraform fmt on the infrastructure files
# since we don't have terraform installed locally

echo "Downloading terraform..."
cd /tmp
curl -o terraform.zip https://releases.hashicorp.com/terraform/1.6.0/terraform_1.6.0_darwin_amd64.zip
unzip -o terraform.zip
chmod +x terraform

echo "Running terraform fmt..."
cd /Users/addison/Desktop/Workspace/DeusExMachina
/tmp/terraform fmt -recursive infrastructure/

echo "Done! The following files were formatted:"
git status --porcelain | grep "^ M" | cut -c4-