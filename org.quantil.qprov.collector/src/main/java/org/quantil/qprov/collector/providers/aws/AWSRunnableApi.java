/*******************************************************************************
 * Copyright (c) 2024 the QProv contributors.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package org.quantil.qprov.collector.providers.aws;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AllArgsConstructor
public class AWSRunnableApi implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(AWSRunnableApi.class);

    private AWSProvider awsProvider;

    @Override
    public void run() {
        logger.debug("Starting periodic collection from API...");
        boolean result = false;
        try {
            result = awsProvider.collectFromApi();
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.debug("Finished periodic collection from API with result: {}", result);
    }
}
