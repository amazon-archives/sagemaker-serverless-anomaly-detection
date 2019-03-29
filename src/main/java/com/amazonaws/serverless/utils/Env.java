/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.amazonaws.serverless.utils;

/**
 * Helper class for retrieving lambda environment values.
 */

public class Env {

    private static final String CLOUDWATCH_NAMESPACE = "CLOUDWATCH_NAMESPACE";
    private static final String CLOUDWATCH_METRIC_NAME = "CLOUDWATCH_METRIC_NAME";
    private static final String CLOUDWATCH_METRIC_STATISTIC = "CLOUDWATCH_METRIC_STATISTIC";
    private static final String CLOUDWATCH_METRIC_PERIOD_IN_SECONDS = "CLOUDWATCH_METRIC_PERIOD_IN_SECONDS";

    private static final String SAGEMAKER_ROLE_ARN = "SAGEMAKER_ROLE_ARN";

    private static final String SAGEMAKER_TRAINING_INSTANCE_COUNT = "SAGEMAKER_TRAINING_INSTANCE_COUNT";
    private static final String SAGEMAKER_TRAINING_INSTANCE_TYPE = "SAGEMAKER_TRAINING_INSTANCE_TYPE";
    private static final String SAGEMAKER_TRAINING_VOLUME_SIZE = "SAGEMAKER_TRAINING_VOLUME_SIZE";
    private static final String SAGEMAKER_TRAINING_NUM_TREES = "SAGEMAKER_TRAINING_NUM_TREES";
    private static final String SAGEMAKER_TRAINING_NUM_SAMPLES_PER_TREE = "SAGEMAKER_TRAINING_NUM_SAMPLES_PER_TREE";

    private static final String SAGEMAKER_TRANSFORM_INSTANCE_COUNT = "SAGEMAKER_TRANSFORM_INSTANCE_COUNT";
    private static final String SAGEMAKER_TRANSFORM_INSTANCE_TYPE = "SAGEMAKER_TRANSFORM_INSTANCE_TYPE";

    private static final String S3_BUCKET_NAME = "S3_BUCKET_NAME";

    public static String getCloudwatchNamespace() {
        return System.getenv(CLOUDWATCH_NAMESPACE);
    }

    public static String getCloudwatchMetricName() {
        return System.getenv(CLOUDWATCH_METRIC_NAME);
    }

    public static String getCloudwatchMetricStatistic() {
        return System.getenv(CLOUDWATCH_METRIC_STATISTIC);
    }

    public static int getCloudwatchMetricPeriodInSeconds() {
        return Integer.parseInt(System.getenv(CLOUDWATCH_METRIC_PERIOD_IN_SECONDS));
    }

    public static String getSagemakerRoleArn() {
        return System.getenv(SAGEMAKER_ROLE_ARN);
    }

    public static int getSagemakerTrainingInstanceCount() {
        return Integer.parseInt(System.getenv(SAGEMAKER_TRAINING_INSTANCE_COUNT));
    }

    public static String getSagemakerTrainingInstanceType() {
        return System.getenv(SAGEMAKER_TRAINING_INSTANCE_TYPE);
    }

    public static int getSagemakerTrainingVolumeSize() {
        return Integer.parseInt(System.getenv(SAGEMAKER_TRAINING_VOLUME_SIZE));
    }

    public static String getSagemakerTrainingNumTrees() {
        return System.getenv(SAGEMAKER_TRAINING_NUM_TREES);
    }

    public static String getSagemakerTrainingNumSamplesPerTree() {
        return System.getenv(SAGEMAKER_TRAINING_NUM_SAMPLES_PER_TREE);
    }

    public static int getSagemakerTransformInstanceCount() {
        return Integer.parseInt(System.getenv(SAGEMAKER_TRANSFORM_INSTANCE_COUNT));
    }

    public static String getSagemakerTransformInstanceType() {
        return System.getenv(SAGEMAKER_TRANSFORM_INSTANCE_TYPE);
    }

    public static String getS3BucketName() {
        return System.getenv(S3_BUCKET_NAME);
    }
}
