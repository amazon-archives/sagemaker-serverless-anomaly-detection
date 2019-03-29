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
import com.amazonaws.services.sagemaker.model.DescribeTrainingJobRequest;
import com.amazonaws.services.sagemaker.model.DescribeTrainingJobResult;
import lombok.extern.slf4j.Slf4j;

/**
 * CheckTrainingJobStatusHandler class contains the logic required to check
 * if a SageMaker training job has finished successfully. This code is executed
 * continuously every two minutes until the job either fails or finishes successfully.
 */
@Slf4j
public class CheckTrainingJobStatusHandler {

    private final AmazonSageMaker sagemaker;

    public CheckTrainingJobStatusHandler() {
        sagemaker = AmazonSageMakerClientBuilder.standard().build();
    }

    /**
     * Implements Lambda handleRequest method. It uses the training job name provided by
     * previous lambda (the one that started training job) to check if the training job has
     * finished.
     *
     * @param input StartTrainingJobOutput object containing training job name.
     * @param context Lambda context object.
     *
     * @return Updated StartTrainingJobOutput containing latest status of a training job.
     */
    public StartTrainingJobOutput handleRequest(StartTrainingJobOutput input, Context context) {
        DescribeTrainingJobRequest request = new DescribeTrainingJobRequest()
            .withTrainingJobName(input.getTrainingJobName());
        log.info("Running describe training job for job: " + input.getTrainingJobName());
        DescribeTrainingJobResult result = sagemaker.describeTrainingJob(request);
        log.info("Describe training job result: " + result);
        // Since this lambda function is going to be executed continuously, it will receive
        // previous output as input for next invocation. This is why we're overriding and returning
        // the input object instead of creating a new one.
        input.setTrainingJobStatus(result.getTrainingJobStatus());
        return input;
    }

}
