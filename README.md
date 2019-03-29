## SageMaker Serverless Anomaly Detection

This repository contains the assets for system described in this [ML blog post](https://aws.amazon.com/blogs/machine-learning/build-a-serverless-anomaly-detection-tool-using-java-and-the-amazon-sagemaker-random-cut-forest-algorithm/).

It demonstrates how to build a [serverless](https://aws.amazon.com/serverless/#getstarted) anomaly detection system to find anomalies in [CloudWatch metrics](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/working_with_metrics.html) 
using [Amazon SageMaker](https://aws.amazon.com/sagemaker/), [AWS Step Functions](https://aws.amazon.com/step-functions/), [AWS Lambda](https://aws.amazon.com/lambda/), 
[Amazon CloudWatch Events](https://docs.aws.amazon.com/AmazonCloudWatch/latest/events/WhatIsCloudWatchEvents.html) and [Java](https://www.java.com/en/) programming language. 

The repository contains sample code for the Step Functions state machines and the Lambda functions,
as well as an [AWS CloudFormation](https://aws.amazon.com/cloudformation/) template for creating the functions and related resources.

## Walkthrough of Architecture

The anomaly detection system is composed of two main parts: training and transform state machines. Training state machine
is responsible for creating new, up to date model. Transform state machine is responsible for finding anomalies in the data
by using the model produced by training state machine. The steps are as follows:

1. CloudWatch Event triggers monthly execution of Training State Machine.
1. Training state machine pulls the metric data from CloudWatch, uploads the data to S3 and starts the training job.
1. It then waits and periodically checks the training job status until it is finished.
1. As the last step in training state machine, a model artifact is created from training output.
1. CloudWatch Event triggers weekly execution of Transform State Machine (this happens independently of Training State Machine).
1. Transform State Machine pulls a week worth of metric data from CloudWatch, uploads the data to S3 and starts the batch transform job.
1. It then waits and periodically checks the transform job status until it is finished.
1. The last step in Transform State Machine is to get the anomaly scores from S3, find anomalies based on 3-sigma threshold and upload the anomaly indicators back to CloudWatch.

More details on each of the state machines can be found in the mentioned blog post.

## Running the Example

Building and running the example locally requires you to have following dependencies installed on your machine:

1. [Java 8](https://www.java.com/en/) or later
1. [Maven](https://maven.apache.org/)
1. [AWS CLI](https://aws.amazon.com/cli/)

Start by setting up required environment variables for later commands to use (this is the minimum set of variables to set, for full set of variables check the CloudFormation template):

```bash
S3BUCKET=[REPLACE_WITH_BUCKET_TO_UPLOAD_TEMPLATE_ARTIFACTS_TO]
S3DATABUCKETNAME=[REPLACE_WITH_BUCKET_TO_UPLOAD_DATA_AND_MODELS_TO]
REGION=[REPLACE_WITH_REGION_YOU_WISH_TO_DEPLOY_TO]
STACKNAME=[REPLACE_WITH_DESIRED_STACK_NAME]
CLOUDWATCHMETRICNAME=[REPLACE_WITH_DESIRED_METRIC_NAME]
CLOUDWATCHNAMESPACE=[REPLACE_WITH_DESIRED_NAMESPACE]
SAGEMAKERROLEARN=[REPLACE_WITH_SAGEMAKER_EXECUTION_ROLE]
```

There are additional variables that can be defined and passed to CloudFormation. For the full list check provided template file.

Next step is to go to root folder of the project and run:

```bash
mvn clean install
```

Then go to the `cloudformation` folder and use the `aws cloudformation package` utility

```bash
cd cloudformation

aws cloudformation package --region $REGION --s3-bucket $S3BUCKET --template anomaly_detection.serverless.yaml --output-template-file anomaly_detection.serverless.output.yaml
```
Last, deploy the stack with the resulting yaml file through the CloudFormation Console or command line:

```bash
aws cloudformation deploy --region $REGION --template-file anomaly_detection.serverless.output.yaml --stack-name $STACKNAME --capabilities CAPABILITY_NAMED_IAM --parameter-overrides S3DataBucketName=$S3DATABUCKETNAME CloudWatchMetricName=$CLOUDWATCHMETRICNAME CloudWatchNamespace=$CLOUDWATCHNAMESPACE SageMakerRoleArn=$SAGEMAKERROLEARN 
```

## Testing the Example
Once you have deployed the template, the machines are ready to run the next time they are triggered by the CloudWatch Events rule. If you would like to test it manually follow these steps:
1. Create a JSON with jobType and timeRangeInDays parameters as described in CloudFormation template.
1. Navigate to the [Step Functions console](https://console.aws.amazon.com/states/home)
1. Select the training/transform machine by clicking it's name in the `State machines` table.
1. Scroll down to the `Executions` table and click `Start execution`.
1. In the `New execution` paste the JSON created earlier from your clipboard.
1. Click `Start execution` to invoke an execution of the state machine.

After starting the state machine execution you can watch as it steps through the states from the console.

## Cleaning Up the Stack Resources

To remove all resources created by this example, do the following:

1. [Manually delete the S3 Data bucket](https://docs.aws.amazon.com/AmazonS3/latest/user-guide/delete-bucket.html) (buckets that aren't empty cannot be deleted by CloudFormation)
1. Delete the CloudFormation stack.
1. Delete the CloudWatch log groups associated with each Lambda function created by the CloudFormation stack.

## CloudFormation Template Resources
The following sections explain all of the resources created by the CloudFormation template provided with this example.

### Amazon CloudWatch Events
- **RunTrainingMonthlyRule** - Scheduled expression rule configured to invoke Step Function training state machine execution monthly.
- **RunTransformWeeklyRule** - Scheduled expression rule configured to invoke Step Function transform state machine execution weekly.

### AWS Step Functions
- **SageMakerTrainingStateMachine** - Step Functions state machine that orchestrates training related operations.
- **SageMakerTransformStateMachine** - Step Functions state machine that orchestrates batch transform related operations.

### AWS Lambda
- **UploadMetricDataToS3** - Lambda function implementing task of **UploadMetricDataToS3** step of both training and transform machine
- **StartTrainingJob** - Lambda function implementing task of **StartTrainingJob** step of training state machine
- **CheckTrainingJobStatus** - Lambda function implementing task of **CheckTrainingJobStatus** step of training state machine
- **CreateModel** - Lambda function implementing task of **CreateModel** step of training state machine
- **StartTransformJob** - Lambda function implementing task of **StartTransformJob** step of transform state machine
- **CheckTransformJobStatus** - Lambda function implementing task of **CheckTransformJobStatus** step of transform state machine
- **UploadAnomalousMetricScores** - Lambda function implementing task of **UploadAnomalousMetricScores** step of transform state machine

### AWS IAM
- **UploadMetricDataToS3Role** - IAM Role with policy that allows Lambda function to get metric data from CloudWatch, and put objects to S3.
- **StartTrainingJobRole** - IAM Role with policy that allows Lambda function to create SageMaker model training jobs, pass a configured IAM role to SageMaker, and write log messages to CloudWatch Logs.
- **CheckTrainingJobStatusRole** - IAM Role with policy that allows Lambda function to check the status of SageMaker training jobs.
- **CreateModelRole** - IAM Role with policy that allows Lambda function to create SageMaker model and pass a configured execution role to SageMaker.
- **StartTransformJobRole** - IAM Role with policy that allows Lambda function to create SageMaker model training jobs, pass a configured IAM role to SageMaker, and write log messages to CloudWatch Logs.
- **CheckTransformJobStatusRole** - IAM Role with policy that allows Lambda function to check the status of SageMaker batch transform jobs.
- **UploadAnomalousMetricScoresRole** - IAM Role with policy that allows Lambda function to get data from S3 and put metric data to CloudWatch.

### Amazon S3
- **S3DataBucket** - S3 bucket to hold model training data and trained model artifacts.

## License Summary

This sample code is made available under a modified MIT license. See the LICENSE file.