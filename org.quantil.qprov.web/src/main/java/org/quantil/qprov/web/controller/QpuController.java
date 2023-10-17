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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quantil.qprov.core.model.agents.Provider;
import org.quantil.qprov.core.model.agents.QPU;
import org.quantil.qprov.core.repositories.ProviderRepository;
import org.quantil.qprov.core.repositories.QPURepository;
import org.quantil.qprov.web.Constants;
import org.quantil.qprov.web.dtos.QpuDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@io.swagger.v3.oas.annotations.tags.Tag(name = Constants.TAG_PROVIDER)
@RestController
@CrossOrigin(allowedHeaders = "*", origins = "*")
@RequestMapping("/" + Constants.PATH_PROVIDERS + "/{providerId}/" + Constants.PATH_QPUS)
@AllArgsConstructor
@Slf4j
public class QpuController {

    private static final Logger logger = LoggerFactory.getLogger(QpuController.class);

    private final ProviderRepository providerRepository;

    private final QPURepository qpuRepository;

    @Operation(responses = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "404", description = "Provider with the ID not available.")
    }, description = "Retrieve all QPUs of the provider.")
    @GetMapping
    public ResponseEntity<CollectionModel<EntityModel<QpuDto>>> getQPUs(@PathVariable UUID providerId) {

        // check availability of provider
        final Optional<Provider> provider = providerRepository.findById(providerId);
        if (provider.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        final List<EntityModel<QpuDto>> qpuEntities = new ArrayList<>();
        final List<Link> qpuLinks = new ArrayList<>();

        qpuRepository.findByProvider(provider.get()).forEach((QPU qpu) -> {
                    logger.debug("Found QPU with name: {}", qpu.getName());
                    qpuLinks.add(linkTo(methodOn(QpuController.class).getQPU(providerId, qpu.getDatabaseId()))
                            .withRel(qpu.getDatabaseId().toString()));
                    qpuEntities.add(createQpuDto(providerId, qpu));
                }
        );

        final var collectionModel = CollectionModel.of(qpuEntities);
        collectionModel.add(qpuLinks);
        collectionModel.add(linkTo(methodOn(QpuController.class).getQPUs(providerId)).withSelfRel());
        return ResponseEntity.ok(collectionModel);
    }

    @Operation(responses = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "400", description = "QPU belongs not to specified provider."),
            @ApiResponse(responseCode = "404", description = "Not Found. QPU with given ID doesn't exist.")
    }, description = "Retrieve a specific QPU and its basic properties.")
    @GetMapping("/{qpuId}")
    public ResponseEntity<EntityModel<QpuDto>> getQPU(
            @PathVariable UUID providerId, @PathVariable UUID qpuId) {

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
        if (!qpu.getProvider().getDatabaseId().equals(providerId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        return ResponseEntity.ok(createQpuDto(providerId, qpu));
    }

    private EntityModel<QpuDto> createQpuDto(UUID providerId, QPU qpu) {
        final EntityModel<QpuDto> qpuDto = EntityModel.of(QpuDto.createDTO(qpu));
        qpuDto.add(linkTo(methodOn(QpuController.class).getQPU(providerId, qpu.getDatabaseId()))
                .withSelfRel());
        if (!qpu.isSimulator()) {
            // calibration data about simulators is not available, thus do not add a link to the qubits
            qpuDto.add(linkTo(methodOn(QubitController.class).getQubits(providerId, qpu.getDatabaseId())).withRel(Constants.PATH_QUBITS));
        }
        qpuDto.add(linkTo(methodOn(AggregatedDataController.class).getLinksToAggregatedData(providerId, qpu.getDatabaseId()))
                .withRel(Constants.PATH_AGGREGATED_DATA));
        return qpuDto;
    }
}
