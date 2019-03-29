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
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.amazonaws.services.lambda.runtime.Context;
import lombok.extern.slf4j.Slf4j;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 *  DataUploadHandler class contains the Lambda code for reading the data from CloudWatch
 *  and uploading that data to S3 in form of CSV file. Values and timestamps are extracted
 *  from data and uploaded as separate CSV files.
 *
 *  This is the entry point for both Train and Transform state machines described in README.
 */
@Slf4j
public class DataUploadHandler {

    private static final String VALUES = "values";
    private static final String TIMESTAMPS = "timestamps";

    private static final int NUMBER_OF_DATAPOINTS_PER_REQUEST = 1440;

    private final AmazonCloudWatch cloudWatch;
    private final S3FileManager s3FileManager;

    public DataUploadHandler() {
        cloudWatch = AmazonCloudWatchClientBuilder.standard().build();
        s3FileManager = new S3FileManager();
    }

    /**
     * Implementation of Lambda handleRequest interface. Reads data from CloudWatch, and uploads
     * obtained data to S3 bucket.
     *
     * @param input contains information on jobType and time range for which to retrieve data from
     *              CloudWatch
     * @param context Lambda execution related context.
     * @return DataUploadOutput object containing timestamp, and relevant S3 information
     */
    public DataUploadOutput handleRequest(DataUploadInput input, Context context) {
        List<Datapoint> datapoints = getDatapointsFromCloudWatch(input.getTimeRangeInDays());
        return uploadDatapointsToS3(datapoints, input.getJobType());
    }

    /**
     * Returns a list of datapoints from CloudWatch, for specified time range. Metric period is read
     * from environment variable CLOUDWATCH_METRIC_PERIOD_IN_SECONDS in lambda env.
     *
     * @param timeRangeInDays time range for which to retrieve CloudWatch datapoints.
     * @return List of datapoints containing values and timestamps for specified metric
     */
    private List<Datapoint> getDatapointsFromCloudWatch(int timeRangeInDays) {
        int periodInSeconds = Env.getCloudwatchMetricPeriodInSeconds();
        int numberOfRequests = calculateNumberOfRequests(periodInSeconds, timeRangeInDays);

        log.info("Calculated number of requests is: " + numberOfRequests);
        List<GetMetricStatisticsRequest> requests = createGetMetricStatisticsRequests(numberOfRequests, periodInSeconds);

        List<Datapoint> datapoints = retrieveDatapoints(requests);
        log.info("Retrieved total of: " + datapoints.size() + " datapoints.");

        return datapoints;
    }

    /**
     * Uploads datapoints retrieved from CloudWatch to S3. It extracts values and timestamps,
     * and converts them to CSV files before uploading to S3 bucket. Bucket name is read from
     * environment variable S3_BUCKET_NAME in lambda env.
     *
     * @param datapoints Datapoints to upload to S3
     * @param jobType Job type can be train or transform
     * @return Return DataUploadOutput object containing information that is going to be used
     * by next Lambda in the state machine (training or transform).
     */
    private DataUploadOutput uploadDatapointsToS3(List<Datapoint> datapoints, String jobType) {
        String timestamp = Long.toString(System.currentTimeMillis());
        String bucket = Env.getS3BucketName();

        String values = extractValuesFromDatapoints(datapoints);
        log.info("Extracted values content from datapoints.");

        String timestamps = extractTimestampsFromDatapoints(datapoints);
        log.info("Extracted timestamps content from datapoints.");

        DataUploadConfig valuesConfig = new DataUploadConfig(bucket, jobType, timestamp, VALUES);
        s3FileManager.uploadStringAsFile(valuesConfig.getBucket(), valuesConfig.getKey(), values);
        log.info("Uploaded CSV file with values to S3");

        DataUploadConfig timestampsConfig = new DataUploadConfig(bucket, jobType, timestamp, TIMESTAMPS);
        s3FileManager.uploadStringAsFile(timestampsConfig.getBucket(), timestampsConfig.getKey(), timestamps);
        log.info("Uploaded CSV file with timestamps to S3");

        return new DataUploadOutput(bucket, timestamp, timestampsConfig.getKey(),
            valuesConfig.getKeyPrefix(), valuesConfig.getFileName());
    }

    /**
     * Calculates the number of GetMetricStatisticsRequest to execute towards CloudWatch, based on total
     * number of datapoints to retrieve and maximum number of datapoints per request (1440).
     *
     * @param periodInSeconds metric period in seconds
     * @param timeRangeInDays time range for which we're making the request
     * @return total number of requests
     */
    private int calculateNumberOfRequests(int periodInSeconds, int timeRangeInDays) {
        int intervalInSeconds = (int) TimeUnit.DAYS.toSeconds(timeRangeInDays);
        int numberOfDataPoints = intervalInSeconds / periodInSeconds;
        return (numberOfDataPoints + NUMBER_OF_DATAPOINTS_PER_REQUEST - 1) / NUMBER_OF_DATAPOINTS_PER_REQUEST;
    }

    /**
     * Creates GetMetricStatisticsRequest based on calculated number of requests and metric period.
     * Multiple parameters are read from lambda environment, including CLOUDWATCH_NAMESPACE,
     * CLOUDWATCH_METRIC_NAME and CLOUDWATCH_METRIC_STATISTIC.
     *
     * @param numberOfRequests total number of requests to create.
     * @param periodInSeconds metric period in seconds.
     * @return List of GetMetricStatisticsRequests
     */
    private List<GetMetricStatisticsRequest> createGetMetricStatisticsRequests(int numberOfRequests, int periodInSeconds) {
        List<GetMetricStatisticsRequest> requests = new ArrayList<>();
        long periodInMillis = TimeUnit.SECONDS.toMillis(periodInSeconds);
        long deltaInMilliseconds = NUMBER_OF_DATAPOINTS_PER_REQUEST * periodInMillis;
        Date endDate = new Date();
        for (int i = 0; i < numberOfRequests; i++) {
            Date startDate = new Date(endDate.getTime() - deltaInMilliseconds);
            GetMetricStatisticsRequest request = new GetMetricStatisticsRequest()
                    .withMetricName(Env.getCloudwatchMetricName())
                    .withNamespace(Env.getCloudwatchNamespace())
                    .withPeriod(periodInSeconds)
                    .withStatistics(Env.getCloudwatchMetricStatistic())
                    .withStartTime(startDate)
                    .withEndTime(endDate);
            requests.add(request);
            endDate = startDate;
        }
        return requests;
    }

    /**
     * Execute all GetMetricStatisticsRequest and collect the returned datapoints. Datapoints
     * are sorted based on their timestamp before being returned to the caller.
     *
     * @param requests List of GetMetricStatisticsRequests
     * @return List of
     */
    private ArrayList<Datapoint> retrieveDatapoints(List<GetMetricStatisticsRequest> requests) {
        ArrayList<Datapoint> datapoints = new ArrayList<>();
        for (GetMetricStatisticsRequest request : requests) {
            GetMetricStatisticsResult result = cloudWatch.getMetricStatistics(request);
            datapoints.addAll(result.getDatapoints());
        }
        datapoints.sort(this::compareDataPoints);
        return datapoints;
    }

    private int compareDataPoints(Datapoint first, Datapoint second)
    {
        return first.getTimestamp().compareTo(second.getTimestamp());
    }

    /**
     * Extract timestamps from CloudWatch datapoints, and create a CSV string,
     * where each row will contain exactly one timestamp.
     *
     * @param datapoints CloudWatch datapoints.
     * @return CSV string containing timestamps.
     */
    private String extractTimestampsFromDatapoints(List<Datapoint> datapoints) {
        return datapoints.stream()
            .map(datapoint -> Long.toString(datapoint.getTimestamp().getTime()))
            .collect(Collectors.joining(System.lineSeparator()));
    }

    /**
     * Extract values from CloudWatch datapoints, and create a CSV string,
     * where each row will contain exactly one value.
     *
     * @param datapoints CloudWatch datapoints.
     * @return CSV string containing value.
     */
    private String extractValuesFromDatapoints(List<Datapoint> datapoints) {
        return datapoints.stream()
            .map(datapoint -> Double.toString(datapoint.getAverage()))
            .collect(Collectors.joining(System.lineSeparator()));
    }
}
