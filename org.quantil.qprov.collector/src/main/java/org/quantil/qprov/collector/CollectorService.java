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

package org.quantil.qprov.collector;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController()
public class CollectorService {

    private static final Logger logger = LoggerFactory.getLogger(CollectorService.class);

    private final Set<IProvider> availableProviders;

    @Autowired
    public CollectorService(Set<IProvider> availableProviders) {
        this.availableProviders = availableProviders;
    }

    @Operation(responses = {
            @ApiResponse(responseCode = "202"),
            @ApiResponse(responseCode = "500", description = "Server error during provenance data collection"),
    }, description = "Retrieve the provenance data from all available quantum hardware providers.")
    @PostMapping("/collect")
    public HttpEntity<RepresentationModel<?>> collectProvenanceData() {

        this.availableProviders.forEach((IProvider provider) -> {
            logger.debug("Collecting provenance data for provider: {}", provider.getProviderId());
            final boolean success = provider.collectFromApi();
            logger.debug("Finished retrieval of data with success: {}", success);
        });

        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
