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
package com.amazonaws.serverless.sagemaker;

import com.amazonaws.regions.Regions;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class RandomCutForestConfig {

    private static final String ALGORITHM = "randomcutforest";

    private static final String TAG = "1";

    public static final String ALGORITHM_NAME = "random-cut-forest";

    private static final Map<Regions, String> regionToRegistryPath = new HashMap<>();

    static {
        regionToRegistryPath.put(Regions.US_WEST_1,"632365934929.dkr.ecr.us-west-1.amazonaws.com");
        regionToRegistryPath.put(Regions.US_WEST_2,"174872318107.dkr.ecr.us-west-2.amazonaws.com");
        regionToRegistryPath.put(Regions.US_EAST_1,"382416733822.dkr.ecr.us-east-1.amazonaws.com");
        regionToRegistryPath.put(Regions.US_EAST_2,"404615174143.dkr.ecr.us-east-2.amazonaws.com");
        regionToRegistryPath.put(Regions.AP_NORTHEAST_1,"351501993468.dkr.ecr.ap-northeast-1.amazonaws.com");
        regionToRegistryPath.put(Regions.AP_NORTHEAST_2,"835164637446.dkr.ecr.ap-northeast-2.amazonaws.com");
        regionToRegistryPath.put(Regions.AP_SOUTH_1,"991648021394.dkr.ecr.ap-south-1.amazonaws.com");
        regionToRegistryPath.put(Regions.AP_SOUTHEAST_1,"475088953585.dkr.ecr.ap-southeast-1.amazonaws.com");
        regionToRegistryPath.put(Regions.AP_SOUTHEAST_2,"712309505854.dkr.ecr.ap-southeast-2.amazonaws.com");
        regionToRegistryPath.put(Regions.CA_CENTRAL_1,"469771592824.dkr.ecr.ca-central-1.amazonaws.com");
        regionToRegistryPath.put(Regions.EU_CENTRAL_1,"664544806723.dkr.ecr.eu-central-1.amazonaws.com");
        regionToRegistryPath.put(Regions.EU_WEST_1,"438346466558.dkr.ecr.eu-west-1.amazonaws.com");
        regionToRegistryPath.put(Regions.EU_WEST_2,"644912444149.dkr.ecr.eu-west-2.amazonaws.com");
    }

    public static String getAlgorithmImage() {
        Regions currentRegion = Regions.fromName(System.getenv("AWS_DEFAULT_REGION"));
        log.info("Current region is: " + currentRegion.getName());
        String registryPath = regionToRegistryPath.get(currentRegion);
        log.info("Registry path is: " + registryPath);
        return registryPath + "/" + ALGORITHM + ":" + TAG;
    }

}
