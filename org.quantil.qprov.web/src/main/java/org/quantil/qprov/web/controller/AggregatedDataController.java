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

package org.quantil.qprov.web.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.UUID;

import org.quantil.qprov.web.Constants;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@io.swagger.v3.oas.annotations.tags.Tag(name = Constants.TAG_PROVIDER)
@RestController
@CrossOrigin(allowedHeaders = "*", origins = "*")
@RequestMapping("/" + Constants.PATH_PROVIDERS + "/{providerId}/" + Constants.PATH_QPUS + "/{qpuId}/" + Constants.PATH_AGGREGATED_DATA)
@AllArgsConstructor
@Slf4j
public class AggregatedDataController {

    @Operation(responses = {@ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "404", description = "Provider or QPU not found.")})
    @GetMapping
    public HttpEntity<RepresentationModel<?>> getLinksToAggregatedData(@PathVariable UUID providerId, @PathVariable UUID qpuId) {

        final RepresentationModel<?> responseEntity = new RepresentationModel<>();

        // add self-link and links to sub-controllers
        responseEntity.add(linkTo(methodOn(AggregatedDataController.class).getLinksToAggregatedData(providerId, qpuId)).withSelfRel());
        // TODO: add link to calibration matrix

        return ResponseEntity.ok(responseEntity);
    }
}
