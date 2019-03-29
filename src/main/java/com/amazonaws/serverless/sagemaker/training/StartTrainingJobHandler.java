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
package com.amazonaws.serverless.sagemaker.training;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.sagemaker.AmazonSageMaker;
import com.amazonaws.services.sagemaker.AmazonSageMakerClientBuilder;
import com.amazonaws.services.sagemaker.model.CreateTrainingJobRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * StartTrainingJobHandler class contains the logic required to start a
 * SageMaker training job by using AmazonSageMaker client. This code is
 * executed in train state machine, after the data has been uploaded to S3.
 */
@Slf4j
public class StartTrainingJobHandler {

    private static final String TRAINING_JOB_STATUS = "InProgress";

    private final AmazonSageMaker sagemaker;

    public StartTrainingJobHandler() {
        sagemaker = AmazonSageMakerClientBuilder.standard().build();
    }

    /**
     * Implementation of Lambda handleRequest interface. Starts SageMaker training job by creating
     * CreateTrainingJobRequest based on input from previous Lambda (uploading data to CloudWatch)
     * and environment variables defined in Lambda env.
     *
     * @param input StartTrainingJobInput containing information on S3 bucket, key etc.
     * @param context Lambda context.
     * @return StartTrainingJobOutput containing training job name, model output path etc.
     */
    public StartTrainingJobOutput handleRequest(StartTrainingJobInput input, Context context) {
        StartTrainingJobConfig config = new StartTrainingJobConfig(
            input.getTimestamp(), input.getBucket(), input.getValuesKey());

        CreateTrainingJobRequest request = config.getTrainingJobRequest();
        log.info("Starting sagemaker training job: " + request.getTrainingJobName());
        log.info("Full training job request is: " + request);
        sagemaker.createTrainingJob(request);

        return new StartTrainingJobOutput(
            input.getTimestamp(), request.getTrainingJobName(),
            TRAINING_JOB_STATUS, config.getModelOutputPath());
    }

}
