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

package org.quantil.qprov.web.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import javax.ws.rs.QueryParam;

import org.quantil.qprov.core.model.agents.Provider;
import org.quantil.qprov.core.model.agents.QPU;
import org.quantil.qprov.core.model.entities.CalibrationMatrix;
import org.quantil.qprov.core.repositories.CalibrationMatrixRepository;
import org.quantil.qprov.core.repositories.ProviderRepository;
import org.quantil.qprov.core.repositories.QPURepository;
import org.quantil.qprov.web.Constants;
import org.quantil.qprov.web.dtos.CalibrationMatrixDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@io.swagger.v3.oas.annotations.tags.Tag(name = Constants.TAG_PROVIDER)
@RestController
@CrossOrigin(allowedHeaders = "*", origins = "*")
@RequestMapping("/" + Constants.PATH_PROVIDERS + "/{providerId}/" + Constants.PATH_QPUS + "/{qpuId}/" + Constants.PATH_AGGREGATED_DATA)
@AllArgsConstructor
@Slf4j
public class AggregatedDataController {
    protected static final Logger logger = LogManager.getLogger();

    private final ProviderRepository providerRepository;

    private final QPURepository qpuRepository;

    private final CalibrationMatrixRepository calibrationMatrixRepository;

    @Operation(responses = {@ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "404", description = "Provider or QPU not found.")})
    @GetMapping
    public HttpEntity<RepresentationModel<?>> getLinksToAggregatedData(@PathVariable UUID providerId, @PathVariable UUID qpuId) {

        // check availability of provider
        final Optional<Provider> provider = providerRepository.findById(providerId);
        if (provider.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // check availability of qpu
        final Optional<QPU> qpuOptional = qpuRepository.findById(qpuId);
        if (qpuOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        final RepresentationModel<?> responseEntity = new RepresentationModel<>();

        // add self-link and links to routes returning aggregated data
        responseEntity.add(linkTo(methodOn(AggregatedDataController.class).getLinksToAggregatedData(providerId, qpuId)).withSelfRel());
        responseEntity.add(linkTo(methodOn(AggregatedDataController.class).getCalibrationMatrix(providerId, qpuId, false))
                .withRel(Constants.PATH_CALIBRATION_MATRIX));

        return ResponseEntity.ok(responseEntity);
    }

    @Operation(responses = {@ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "404", description = "Provider or QPU not found or no calibration matrix available for this QPU.")})
    @GetMapping("/" + Constants.PATH_CALIBRATION_MATRIX)
    public ResponseEntity<CollectionModel<EntityModel<CalibrationMatrixDto>>> getCalibrationMatrix(@PathVariable UUID providerId,
                                                                                                   @PathVariable UUID qpuId,
                                                                                                   @QueryParam("latest") boolean latest) {

        // check availability of provider
        final Optional<Provider> provider = providerRepository.findById(providerId);
        if (provider.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // check availability of qpu
        final Optional<QPU> qpuOptional = qpuRepository.findById(qpuId);
        if (qpuOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        final QPU qpu = qpuOptional.get();
        logger.debug("Retrieving calibration matrix for QPU with name: {}", qpu.getName());

        final Stream<CalibrationMatrix> calibrationMatrixStream;
        if (latest) {
            // retrieve characteristics with latest calibration time stamp
            calibrationMatrixStream = Stream.ofNullable(
                    calibrationMatrixRepository.findByQpuOrderByCalibrationTimeDesc(qpu).stream().findFirst().orElse(null));
        } else {
            // retrieve all characteristics
            calibrationMatrixStream = calibrationMatrixRepository.findByQpuOrderByCalibrationTimeDesc(qpu).stream();
        }

        final List<EntityModel<CalibrationMatrixDto>> entities = new ArrayList<>();
        calibrationMatrixStream.forEach(calibrationMatrix -> {
            entities.add(EntityModel.of(CalibrationMatrixDto.createDTO(calibrationMatrix)));
        });
        logger.debug("Retrieved {} calibration matrix records for QPU with name: {}", entities.size(), qpu.getName());

        if (entities.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(CollectionModel.of(entities));
    }
}
