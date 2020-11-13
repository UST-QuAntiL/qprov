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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.quantil.qprov.core.model.agents.Provider;
import org.quantil.qprov.core.model.agents.QPU;
import org.quantil.qprov.core.model.entities.Qubit;
import org.quantil.qprov.core.repositories.ProviderRepository;
import org.quantil.qprov.core.repositories.QPURepository;
import org.quantil.qprov.core.repositories.QubitRepository;
import org.quantil.qprov.web.Constants;
import org.quantil.qprov.web.dtos.QubitDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/" + Constants.PATH_PROVIDERS + "/{providerId}/" + Constants.PATH_QPUS + "/{qpuId}/" + Constants.PATH_QUBITS)
@AllArgsConstructor
@Slf4j
public class QubitController {

    private static final Logger logger = LoggerFactory.getLogger(QubitController.class);

    private final ProviderRepository providerRepository;

    private final QPURepository qpuRepository;

    private final QubitRepository qubitRepository;

    @Operation(responses = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "404", description = "Provider or QPU with the ID not available.")
    }, description = "Retrieve all Qubits of the QPU.")
    @GetMapping
    public ResponseEntity<CollectionModel<EntityModel<QubitDto>>> getQubits(@PathVariable UUID providerId, @PathVariable UUID qpuId) {

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

        final List<EntityModel<QubitDto>> qubitEntities = new ArrayList<>();
        final List<Link> qubitLinks = new ArrayList<>();

        qubitRepository.findByQpu(qpuOptional.get()).forEach((Qubit qubit) -> {
                    logger.debug("Found Qubit with name: {}", qubit.getName());

                    qubitEntities.add(createQubitDto(providerId, qpuId, qubit));
                    qubitLinks.add(linkTo(methodOn(QubitController.class).getQubit(providerId, qpuId, qubit.getDatabaseId()))
                            .withRel(qubit.getDatabaseId().toString()));
                }
        );

        final var collectionModel = new CollectionModel<>(qubitEntities);
        collectionModel.add(qubitLinks);
        collectionModel.add(linkTo(methodOn(QubitController.class).getQubits(providerId, qpuId)).withSelfRel());
        return ResponseEntity.ok(collectionModel);
    }

    @Operation(responses = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "400", description = "Qubit belongs not to specified provider and QPU."),
            @ApiResponse(responseCode = "404", description = "Not Found. Qubit with given ID doesn't exist.")
    }, description = "Retrieve a specific Qubit and its basic properties.")
    @GetMapping("/{qubitId}")
    public ResponseEntity<EntityModel<QubitDto>> getQubit(
            @PathVariable UUID providerId, @PathVariable UUID qpuId, @PathVariable UUID qubitId) {

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

        // check availability of qpu
        final Optional<Qubit> qubitOptional = qubitRepository.findById(qubitId);
        if (qubitOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(createQubitDto(providerId, qpuId, qubitOptional.get()));
    }

    private EntityModel<QubitDto> createQubitDto(UUID providerId, UUID qpuId, Qubit qubit) {
        final EntityModel<QubitDto> qpuDto = new EntityModel<QubitDto>(QubitDto.createDTO(qubit));
        qpuDto.add(linkTo(methodOn(QubitController.class).getQubit(providerId, qpuId, qubit.getDatabaseId()))
                .withSelfRel());
        qpuDto.add(linkTo(methodOn(QubitCharacteristicsController.class).getQubitCharacterisitcs(providerId, qpuId, qubit.getDatabaseId(), false))
                .withRel(Constants.PATH_CHARACTERISTICS));
        for (Qubit connectedQubit : qubit.getConnectedQubits()) {
            qpuDto.add(linkTo(methodOn(QubitController.class).getQubit(providerId, qpuId, connectedQubit.getDatabaseId()))
                    .withRel(Constants.PATH_QUBITS_CONNECTED + connectedQubit.getName()));
        }
        // TODO: add links to gates
        return qpuDto;
    }
}
