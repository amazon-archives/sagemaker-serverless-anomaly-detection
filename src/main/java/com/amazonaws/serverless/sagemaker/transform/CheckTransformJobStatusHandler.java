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
package com.amazonaws.serverless.sagemaker.transform;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.sagemaker.AmazonSageMaker;
import com.amazonaws.services.sagemaker.AmazonSageMakerClientBuilder;
import com.amazonaws.services.sagemaker.model.DescribeTransformJobRequest;
import com.amazonaws.services.sagemaker.model.DescribeTransformJobResult;

/**
 * CheckTransformJobStatusHandler class contains the logic required to
 * check the status of batch transform job. This step is executed every
 * 2 minutes in transform state machine, after the batch transform job
 * has been successfully started.
 */
public class CheckTransformJobStatusHandler {

    private final AmazonSageMaker sagemaker;

    public CheckTransformJobStatusHandler() {
        sagemaker = AmazonSageMakerClientBuilder.standard().build();
    }

    /**
     * Implementation of Lambda handleRequest method. It checks the status of the
     * batch transform job, and updates the status if it has changed.
     *
     * @param input StartTransformJobOutput contains the batch transform job name, required
     *              to check the status of the job.
     * @param context Lambda context.
     *
     * @return StartTransformJobOutput object with updated job status.
     */
    public StartTransformJobOutput handleRequest(StartTransformJobOutput input, Context context) {
        DescribeTransformJobRequest request = new DescribeTransformJobRequest()
            .withTransformJobName(input.getTransformJobName());
        DescribeTransformJobResult result = sagemaker.describeTransformJob(request);
        // Similar to training job, the output of previous lambda will be input for
        // the next one until the job is finished. That's why we update the input instead
        // of creating the new output.
        input.setTransformJobStatus(result.getTransformJobStatus());
        return input;
    }
}
