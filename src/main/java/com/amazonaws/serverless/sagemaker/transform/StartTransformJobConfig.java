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

import com.amazonaws.serverless.utils.Env;
import com.amazonaws.services.sagemaker.model.*;

import static com.amazonaws.serverless.sagemaker.RandomCutForestConfig.ALGORITHM_NAME;

/**
 * StartTransformJobConfig class contains all the necessary configuration
 * and helper methods required to start batch transform job.
 */
public class StartTransformJobConfig {

    private static final String S3_DATA_TYPE = "S3Prefix";
    private static final String S3_PREFIX = "s3://";

    private static final String TRANSFORM_INPUT_COMPRESSION_TYPE = "None";
    private static final String TRANSFORM_INPUT_CONTENT_TYPE = "text/csv;label_size=0";
    private static final String TRANSFORM_INPUT_LINE = "Line";

    private static final String TRANSFORM_OUTPUT_ACCEPT = "application/jsonlines";
    private static final String TRANSFORM_OUTPUT_ASSEMBLE_WITH = "Line";
    private static final String TRANSFORM_OUTPUT_PATH_PREFIX = "data/transform/output/values/";
    private static final String TRANSFORM_OUTPUT_FILE_SUFFIX = ".out";

    private String timestamp;
    private String bucket;
    private String key;
    private String file;
    private String modelName;

    StartTransformJobConfig(String timestamp, String bucket, String key, String file, String modelName) {
        this.timestamp = timestamp;
        this.bucket = bucket;
        this.key = key;
        this.file = file;
        this.modelName = modelName;
    }

    /**
     * Creates CreateTransformJobRequest to be used for batch transform job.
     *
     * @return CreateTransformJobRequest object
     */
    public CreateTransformJobRequest getTransformJobRequest() {
        TransformResources transformResources = getTransformResources();
        TransformInput transformInput = getTransformInput();
        TransformOutput transformOutput = getTransformOutput();
        String transformJobName = ALGORITHM_NAME + "-" + timestamp;
        return new CreateTransformJobRequest()
            .withTransformJobName(transformJobName)
            .withModelName(modelName)
            .withTransformResources(transformResources)
            .withTransformInput(transformInput)
            .withTransformOutput(transformOutput);
    }

    /**
     * Helper method used to create transform resources required to setup the
     * SageMaker transform cluster. SAGEMAKER_TRANSFORM_INSTANCE_COUNT and
     * SAGEMAKER_TRANSFORM_INSTANCE_TYPE are read from Lambda env.
     *
     * @return TransformResources object containing instance type and instance
     * count to use in batch transform
     */
    private TransformResources getTransformResources() {
        return new TransformResources()
            .withInstanceCount(Env.getSagemakerTransformInstanceCount())
            .withInstanceType(Env.getSagemakerTransformInstanceType());
    }

    /**
     * Helper method to create TransformInput object that contains the bucket, key and
     * other useful information on how to obtain data from S3.
     *
     * @return TransformInput object.
     */
    private TransformInput getTransformInput() {
        TransformS3DataSource transformS3DataSource = new TransformS3DataSource()
            .withS3DataType(S3_DATA_TYPE)
            .withS3Uri(S3_PREFIX + bucket +  "/" + key);
        TransformDataSource transformDataSource = new TransformDataSource().withS3DataSource(transformS3DataSource);
        return new TransformInput()
            .withCompressionType(TRANSFORM_INPUT_COMPRESSION_TYPE)
            .withContentType(TRANSFORM_INPUT_CONTENT_TYPE)
            .withDataSource(transformDataSource)
            .withSplitType(TRANSFORM_INPUT_LINE);
    }

    /**
     * Helper method to create TransformOutput object which specifies where to output
     * the scores produced by batch transform.
     *
     * @return TransformOutput object
     */
    private TransformOutput getTransformOutput() {
        return new TransformOutput()
            .withS3OutputPath(S3_PREFIX + bucket + "/" + getAnomalyScoresKeyPrefix())
            .withAccept(TRANSFORM_OUTPUT_ACCEPT)
            .withAssembleWith(TRANSFORM_OUTPUT_ASSEMBLE_WITH);
    }

    /**
     * Helper method to define anomaly scores key prefix (used on s3). If timestamp is
     * 123456789, this method will return: data/transform/output/values/123456789/
     *
     * @return anomaly scores key prefix
     */
    private String getAnomalyScoresKeyPrefix() {
        return TRANSFORM_OUTPUT_PATH_PREFIX + timestamp + "/";
    }

    /**
     * Helper method to define the full anomaly scores key (used on s3). If file is: file-1,
     * and anomaly scores prefix is: data/transform/output/values/123456789/, this method
     * will return: data/transform/output/values/123456789/file-1.out
     * @return anomaly scores key
     */
    public String getAnomalyScoresKey() {
        return getAnomalyScoresKeyPrefix() + file + TRANSFORM_OUTPUT_FILE_SUFFIX;
    }
}
