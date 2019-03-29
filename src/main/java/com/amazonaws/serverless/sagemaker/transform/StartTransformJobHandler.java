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
import com.amazonaws.services.sagemaker.model.*;

import lombok.extern.slf4j.Slf4j;

import static com.amazonaws.serverless.sagemaker.RandomCutForestConfig.ALGORITHM_NAME;

/**
 * StartTransformJobHandler class contains the logic required to start
 * batch transform job which will find anomaly scores for the previously
 * unseen datapoints. This is the second step in the transform state machine.
 */
@Slf4j
public class StartTransformJobHandler {

    private static final String TRANSFORM_JOB_STATUS = "InProgress";

    private static final int LIST_MODELS_MAX_RESULTS = 1;
    private static final int LATEST_MODEL_INDEX = 0;

    private final AmazonSageMaker sagemaker;

    public StartTransformJobHandler() {
        sagemaker = AmazonSageMakerClientBuilder.standard().build();
    }

    /**
     * Implementation of Lambda handleRequest. It obtains the latest created model
     * information, and then uses that model to start a transform job.
     *
     *  @param input StartTransformJobInput contains all necessary information (like s3
     *               bucket and keys) required to start transform job.
     * @param context Lambda context object.
     * @return StartTransformJobOutput object that contains all the necessary information
     * to find anomalies in the data.
     */
    public StartTransformJobOutput handleRequest(StartTransformJobInput input, Context context) {
        String modelName = getLatestModelName();
        log.info("Latest model name is: " + modelName);
        return createSageMakerTransformJob(input, modelName);
    }

    /**
     * Lists all the models created for the account with ALGORITHM_NAME, sorts them
     * based on timestamp and returns the name of the latest model.
     *
     * @return Latest model name.
     */
    private String getLatestModelName() {
        ListModelsRequest request = new ListModelsRequest()
                .withNameContains(ALGORITHM_NAME)
                .withMaxResults(LIST_MODELS_MAX_RESULTS)
                .withSortBy(ModelSortKey.CreationTime)
                .withSortOrder(OrderKey.Descending);
        ListModelsResult result = sagemaker.listModels(request);
        ModelSummary modelSummary = result.getModels().get(LATEST_MODEL_INDEX);
        return modelSummary.getModelName();
    }

    /**
     * Starts SageMaker batch transform job by using the provided model name, and data
     * that was uploaded to s3 bucket.
     *
     * @param input StartTransformJobInput object contains all the necessary s3 information.
     * @param modelName Name of the model to use with batch transform
     *
     * @return StartTransformJobOutput object that contains all the necessary information
     * to find anomalies in the data.
     */
    private StartTransformJobOutput createSageMakerTransformJob(StartTransformJobInput input, String modelName) {
        StartTransformJobConfig config = new StartTransformJobConfig(
            input.getTimestamp(), input.getBucket(), input.getValuesKey(), input.getValuesFile(), modelName);
        CreateTransformJobRequest request = config.getTransformJobRequest();
        log.info("Starting sagemaker transform job: " + request.getTransformJobName());
        log.info("Full transform tob request is: " + request);
        sagemaker.createTransformJob(request);
        return new StartTransformJobOutput(input.getBucket(), input.getTimestamp(),
            input.getTimestampsKey(), config.getAnomalyScoresKey(),
            request.getTransformJobName(), TRANSFORM_JOB_STATUS);
    }
}
