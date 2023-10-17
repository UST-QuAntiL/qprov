/*******************************************************************************
 * Copyright (c) 2023 the QProv contributors.
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

package org.quantil.qprov.collector.providers.ibmq;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AllArgsConstructor
public class IBMQRunnableCircuits implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(IBMQRunnableCircuits.class);

    private IBMQProvider ibmqProvider;

    @Override
    public void run() {
        logger.debug("Starting periodic collection by executing quantum circuits...");
        boolean result = false;
        try {
            result = ibmqProvider.collectThroughCircuits();
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.debug("Finished periodic collection by executing quantum circuits with result: {}", result);
    }
}
