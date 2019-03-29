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

import com.amazonaws.serverless.sagemaker.RandomCutForestConfig;
import com.amazonaws.serverless.utils.Env;
import com.amazonaws.services.sagemaker.model.*;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * StartTrainingJobConfig is helper class containing all methods required
 * to create CreateTrainingJobRequest along with some other utilities methods
 * related to SageMaker training job.
 */
public class StartTrainingJobConfig {

    private static final String TRAINING_MODE = "File";

    private static final String FEATURE_DIM = "feature_dim";
    private static final String FEATURE_DIM_VALUE = "1";
    private static final String NUM_TREES = "num_trees";
    private static final String NUM_SAMPLES_PER_TREE = "num_samples_per_tree";

    private static final String S3_DISTRIBUTION_TYPE = "ShardedByS3Key";
    private static final String S3_DATA_TYPE = "S3Prefix";
    private static final String S3_PREFIX = "s3://";

    private static final String TRAINING_CHANNEL_NAME = "train";
    private static final String TRAINING_CHANNEL_CONTENT_TYPE = "text/csv;label_size=0";

    private static final String MODEL_PREFIX = "/models/";
    private static final String MODEL_SUFFIX = "/output/model.tar.gz";

    private static final int TRAINING_JOB_MAX_RUNTIME_IN_MINUTES = 20;

    private String timestamp;
    private String bucket;
    private String key;

    StartTrainingJobConfig(String timestamp, String bucket, String key) {
        this.timestamp = timestamp;
        this.bucket = bucket;
        this.key = key;
    }

    /**
     * Create CreateTrainingJobRequest required to start SageMaker training job.
     * SAGEMAKER_ROLE_ARN variable is read from Lambda environment.
     *
     * @return CreateTrainingJobRequest object.
     */
    public CreateTrainingJobRequest getTrainingJobRequest() {
        return new CreateTrainingJobRequest()
            .withAlgorithmSpecification(getAlgorithmSpecification())
            .withHyperParameters(getHyperparameters())
            .withInputDataConfig(getTrainingChannel())
            .withOutputDataConfig(getOutputDataConfig())
            .withResourceConfig(getResourceConfig())
            .withRoleArn(Env.getSagemakerRoleArn())
            .withStoppingCondition(getStoppingCondition())
            .withTrainingJobName(RandomCutForestConfig.ALGORITHM_NAME + "-" + timestamp);
    }

    /**
     * Helper method to create AlgorithmSpecification which contains information
     * on where to find algorithm image and what is the algorithm name.
     *
     * @return AlgorithmSpecification object.
     */
    private AlgorithmSpecification getAlgorithmSpecification() {
        return new AlgorithmSpecification()
            .withTrainingImage(RandomCutForestConfig.getAlgorithmImage())
            .withTrainingInputMode(TRAINING_MODE);
    }

    /**
     * Helper method to create hyperparameters. Feature dim HP is set to 1 since
     * we're working with one-dimensional data. NUM_TREES and NUM_SAMPLES_PER_TREE
     * HP's are read from Lambda environment.
     *
     * @return Hyperparameters map.
     */
    private HashMap<String, String> getHyperparameters() {
        HashMap<String, String> hyperParameters = new HashMap<>();
        hyperParameters.put(FEATURE_DIM, FEATURE_DIM_VALUE);
        hyperParameters.put(NUM_TREES, Env.getSagemakerTrainingNumTrees());
        hyperParameters.put(NUM_SAMPLES_PER_TREE, Env.getSagemakerTrainingNumSamplesPerTree());
        return hyperParameters;
    }

    /**
     * Helper method to define training channel. It basically tells SageMaker where to find
     * the data required for training. Information on s3 bucket and key was passed from previous
     * lambda which uploaded the data.
     *
     * @return Channel object.
     */
    private Channel getTrainingChannel() {
        S3DataSource s3DataSource = new S3DataSource()
            .withS3DataDistributionType(S3_DISTRIBUTION_TYPE)
            .withS3DataType(S3_DATA_TYPE)
            .withS3Uri(S3_PREFIX + bucket + "/" + key);
        DataSource dataSource = new DataSource()
            .withS3DataSource(s3DataSource);
        return new Channel()
            .withChannelName(TRAINING_CHANNEL_NAME)
            .withContentType(TRAINING_CHANNEL_CONTENT_TYPE)
            .withDataSource(dataSource);
    }

    /**
     * Helper method used to define how long a training job can last.
     *
     * @return StoppingCondition containing maxRuntimeInSeconds.
     */
    private StoppingCondition getStoppingCondition() {
        int maxRuntimeInSeconds = (int) TimeUnit.MINUTES.toSeconds(TRAINING_JOB_MAX_RUNTIME_IN_MINUTES);
        return new StoppingCondition()
            .withMaxRuntimeInSeconds(maxRuntimeInSeconds);
    }

    /**
     * Helper mehtod used to define the resource configuration. This is defining
     * the infrastructure used by SageMaker when starting the training job. Variables read
     * from Lambda env are: SAGEMAKER_TRAINING_INSTANCE_COUNT, SAGEMAKER_TRAINING_INSTANCE_TYPE,
     * and SAGEMAKER_TRAINING_VOLUME_SIZE.
     *
     * @return Resource config object specifying instance type, instance count and volume size
     */
    private ResourceConfig getResourceConfig() {
        return new ResourceConfig()
                .withInstanceCount(Env.getSagemakerTrainingInstanceCount())
                .withInstanceType(Env.getSagemakerTrainingInstanceType())
                .withVolumeSizeInGB(Env.getSagemakerTrainingVolumeSize());
    }

    /**
     * Helper method specifying where to output the model once training is completed.
     *
     * @return OutputDataConfig object containing model outuput path.
     */
    private OutputDataConfig getOutputDataConfig() {
        return new OutputDataConfig()
                .withS3OutputPath(getModelOutputPathPrefix());
    }

    /**
     * Helper method to generate model output path prefix. If bucket name is test-bucket,
     * this method will return: s3://test-bucket/models/
     * @return model output path prefix
     */
    private String getModelOutputPathPrefix() {
        return S3_PREFIX + bucket + MODEL_PREFIX;
    }

    /**
     * Helper method to return full model output path. If the model prefix is s3://test-bucket/models/
     * and timestamp is 123456789 this method will return:
     * s3://test-bucket/models/random-cut-forest-123456789/output/model.tar.gz
     *
     * @return model output path
     */
    public String getModelOutputPath() {
        return getModelOutputPathPrefix() + RandomCutForestConfig.ALGORITHM_NAME + "-" + timestamp + MODEL_SUFFIX;
    }
}
