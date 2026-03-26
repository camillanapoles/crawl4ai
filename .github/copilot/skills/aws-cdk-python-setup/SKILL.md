---
name: aws-cdk-python-setup
description: Setup and initialization guide for developing AWS CDK (Cloud Development Kit) applications in Python.
---
# AWS CDK Python Setup Instructions

This skill provides setup guidance for working with **AWS CDK (Cloud Development Kit)** projects using **Python**.

## Prerequisites

- **Node.js** >= 14.15.0
- **Python** >= 3.7
- **AWS CLI**
- **Git**

## Installation Steps

```bash
npm install -g aws-cdk
cdk --version
aws configure
mkdir my-cdk-project && cd my-cdk-project
cdk init app --language python
source .venv/bin/activate
pip install -r requirements.txt
```

## Development Workflow

```bash
cdk synth    # Synthesize CloudFormation templates
cdk diff     # Preview changes
cdk deploy   # Deploy to AWS
cdk bootstrap # First deployment only
```
