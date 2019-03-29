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
package com.amazonaws.serverless.cloudwatch;

import com.amazonaws.serverless.s3.S3FileManager;
import com.amazonaws.serverless.utils.Env;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * AnomalousDataUploadHandler class contains all the logic required to find and upload
 * anomalies in original metric back to CloudWatch. This lambda is the last step of the
 * transform state machine.
 */
@Slf4j
public class AnomalousDataUploadHandler {
    
    private static final String ANOMALY_INDICATOR_SUFFIX = "AnomalyIndicator";
    private static final int STORAGE_RESOLUTION_IN_SECONDS = 60;
    private static final int NUMBER_OF_METRIC_DATUMS_PER_REQUEST = 100;

    private final AmazonCloudWatch cloudWatch;
    private final S3FileManager s3FileManager;

    public AnomalousDataUploadHandler() {
        cloudWatch = AmazonCloudWatchClientBuilder.standard().build();
        s3FileManager = new S3FileManager();
    }

    /**
     * Implementation of Lambda handleRequest interface. Downloads anomaly scores and original
     * metric timestamps from S3, finds anomalies based on 2 Sigma cutoff and uploads anomaly
     * indicator as a new metric to CloudWatch, where each anomalous datapoint will have a
     * value of 1.
     *
     * @param input AnomalousDataUploadInput containing information needed to retrieve data from S3.
     * @param context Lambda context
     * @return AnomalousDataUploadOutput containing information on total number of anomalous datapoints.
     *
     * @throws IOException if unable to read from S3
     */
    public AnomalousDataUploadOutput handleRequest(AnomalousDataUploadInput input, Context context) throws IOException {
        List<Double> anomalyScores = getAnomalyScores(input.getBucket(), input.getAnomalyScoresKey());

        List<Integer> anomalyIndices = findAnomalousIndices(anomalyScores);

        List<Long> timestamps = getTimestamps(input.getBucket(), input.getTimestampsKey());

        return uploadAnomalousDataToCloudWatch(timestamps, anomalyIndices, anomalyScores.size());
    }

    /**
     * Get anomaly score lines file from S3 and convert to a list of anomaly scores.
     *
     * @param bucket S3 bucket containing the data
     * @param anomalyScoresKey S3 key (filename)
     * @return List of anomaly scores values
     * @throws IOException if unable to get file from S3
     */
    private List<Double> getAnomalyScores(String bucket, String anomalyScoresKey) throws IOException {
        List<String> anomalyScoreLines = s3FileManager.readFileLinesFromS3(bucket, anomalyScoresKey);
        log.info("Retrieved total of: " + anomalyScoreLines.size() + " anomaly score lines.");
        return getAnomalyScoresFromLines(anomalyScoreLines);
    }

    /**
     * Find anomalies from a list of anomaly scores based on 2 sigma cutoff. First we calculate the mean
     * and the standard deviation. Then we define cutoff threshold at mean + 2 * sigma. All datapoints with
     * anomaly score above that threshold are considered anomalous.
     *
     * @param anomalyScores list of anomaly scores
     * @return List of indices pointing to anomalous datapoints from original data.
     */
    private List<Integer> findAnomalousIndices(List<Double> anomalyScores) {
        double mean = getMean(anomalyScores);
        log.info("Anomaly score mean is: " + mean);

        double std = getStd(anomalyScores, mean);
        log.info("Anomaly score standard deviation is: " + std);

        double scoreCutoff = mean + 2 * std;
        log.info("Anomaly score cutoff  is: " + scoreCutoff);

        List<Integer> anomalousIndices = getAnomalousIndices(anomalyScores, scoreCutoff);
        log.info("Number of anomalous datapoints found is: " + anomalousIndices.size());
        return anomalousIndices;
    }

    /**
     * Get timestamps file from S3 and convert to a list of timestamps.
     *
     * @param bucket S3 bucket containing the data
     * @param timestampsKey S3 key (filename)
     * @return List of timestamps
     * @throws IOException if unable to read file from S3
     */
    private List<Long> getTimestamps(String bucket, String timestampsKey) throws IOException {
        List<String> timestampLines = s3FileManager.readFileLinesFromS3(bucket, timestampsKey);
        log.info("Retrieved total of: " + timestampLines.size() + " timestamp lines.");
        return getTimestampsFromLines(timestampLines);
    }

    /**
     * Uploads anomaly indicator metric to CloudWatch. Value of 1 indicates the datapoint is anomalous,
     * value of 0 indicates regular datapoint.
     *
     * @param timestamps list of timestamps
     * @param anomalyIndices list of anomaly indices
     * @param numScores total number of scores (datapoints)
     * @return AnomalousDataUploadOutput containing information on number of anomalous datapoints.
     */
    private AnomalousDataUploadOutput uploadAnomalousDataToCloudWatch(List<Long> timestamps, List<Integer> anomalyIndices, int numScores) {
        List<MetricDatum> metricData = getMetricData(timestamps, anomalyIndices, numScores);
        log.info("Created metric data with: " + metricData.size() + " metric datums");

        int numPartitions = metricData.size() / NUMBER_OF_METRIC_DATUMS_PER_REQUEST;
        List<List<MetricDatum>> metricDataPartitions = Lists.partition(metricData, numPartitions);
        log.info("Total number of requests is: " + metricDataPartitions.size());

        for (List<MetricDatum> metricDataPartition : metricDataPartitions) {
            PutMetricDataRequest request = new PutMetricDataRequest()
                .withMetricData(metricDataPartition)
                .withNamespace(Env.getCloudwatchNamespace());
            cloudWatch.putMetricData(request);
        }
        log.info("Successfully uploaded anomalous metrics.");
        return new AnomalousDataUploadOutput(anomalyIndices.size());
    }

    /**
     * Creates metric data from timestamps and anomaly indices.
     *
     * @param timestamps list of timestamps
     * @param anomalousIndices list of anomaly indices
     * @param numScores total number of scores (datapoints)
     * @return List of containing metric data
     */
    private List<MetricDatum> getMetricData(List<Long> timestamps, List<Integer> anomalousIndices, int numScores) {
        ArrayList<MetricDatum> metricData = new ArrayList<>();
        for (int i = 0; i < numScores; i++) {
            MetricDatum metricDatum = new MetricDatum()
                .withTimestamp(new Date(timestamps.get(i)))
                .withStorageResolution(STORAGE_RESOLUTION_IN_SECONDS)
                .withMetricName(Env.getCloudwatchMetricName() + ANOMALY_INDICATOR_SUFFIX)
                .withValue(0.0);
            metricData.add(metricDatum);
        }
        for (Integer anomalousIndex : anomalousIndices) {
            metricData.get(anomalousIndex).setValue(1.0);
        }
        return metricData;
    }

    /**
     * Helper method (java lambda) to extract all anomaly indices from a list of anomaly scores
     * based on score cutoff.
     *
     * @param anomalyScores list of anomaly scores
     * @param scoreCutoff score cutoff threshold
     * @return List of indices for anomalous datapoints
     */
    private List<Integer> getAnomalousIndices(List<Double> anomalyScores, double scoreCutoff) {
        return IntStream.range(0, anomalyScores.size())
            .filter(i -> anomalyScores.get(i) > scoreCutoff)
            .boxed().collect(Collectors.toList());
    }

    /**
     * Helper method to calculate standard deviation. We implement it here to avoid adding another
     * dependency to the project.
     * @param values list of values
     * @param mean mean of those values
     * @return standard deviation of values and mean
     */
    private double getStd(List<Double> values, double mean) {
        double sum = 0.0;
        for (Double value : values) {
            sum += Math.pow(value - mean, 2);
        }
        return Math.sqrt(sum/values.size());
    }

    /**
     * Helper method to calculate mean of list of values. We implement it here to avoid adding another
     * dependency to the project.
     *
     * @param values list of values
     * @return mean of all values
     */
    private double getMean(List<Double> values) {
        double sum = 0.0;
        for (Double value : values) {
            sum += value;
        }
        return sum / values.size();
    }

    /**
     * Converts timestamps from string to long
     *
     * @param timestampLines list of timestamps encoded as strings
     * @return list of timestamps
     */
    private List<Long> getTimestampsFromLines(List<String> timestampLines) {
        ArrayList<Long> timestamps = new ArrayList<>();
        for (String timestampLine : timestampLines) {
            timestamps.add(Long.parseLong(timestampLine));
        }
        return timestamps;
    }

    /**
     * Converts anomaly scores from JSON encoded strings to double values.
     *
     * @param valueLines List of values encoded in JSON strings
     * @return list of anomaly scores
     * @throws IOException if conversion from JSON to Double fails.
     */
    private List<Double> getAnomalyScoresFromLines(List<String> valueLines) throws IOException {
        ArrayList<Double> anomalyScores = new ArrayList<>();
        for (String valueLine : valueLines) {
            ObjectMapper objectMapper = new ObjectMapper();
            AnomalyScore anomalyScore = objectMapper.readValue(valueLine, AnomalyScore.class);
            anomalyScores.add(anomalyScore.getScore());
        }
        return anomalyScores;
    }

}
