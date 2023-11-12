/*******************************************************************************
 * Copyright (c) 2020 the QProv contributors.
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

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public abstract class AWSConstants {

    public static final String PROVIDER_ID = "aws";

    public static final Map<String, String> OFFERINGS = Map.of(
            "ionq", "https://ionq.com/",
            "rigetti", "https://www.rigetti.com/",
            "aws", "https://aws.amazon.com/braket/"
    );

    // Checkout https://docs.aws.amazon.com/braket/latest/developerguide/braket-regions.html for regions
    public static final Map<String, String> PROVIDERS = Map.of(
            "ionq", "us-east-1",
            "rigetti", "us-west-1",
            "aws", "us-east-1"
    );

    public static final Map<String, Integer> QUBITS_PER_GATE = ImmutableMap.<String, Integer>builder()
            .put("x", 1)
            .put("y", 1)
            .put("z", 1)
            .put("rx", 1)
            .put("ry", 1)
            .put("rz", 1)
            .put("h", 1)
            .put("s", 1)
            .put("si", 1)
            .put("t", 1)
            .put("ti", 1)
            .put("v", 1)
            .put("vi", 1)
            .put("cnot", 2)
            .put("xx", 2)
            .put("yy", 2)
            .put("zz", 2)
            .put("swap", 2)
            .build();
}
