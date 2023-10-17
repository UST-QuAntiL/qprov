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

import org.quantil.qprov.core.model.agents.QPU;
import org.quantil.qprov.core.model.entities.Gate;
import org.quantil.qprov.core.model.entities.Qubit;
import org.quantil.qprov.core.repositories.GateRepository;
import org.quantil.qprov.core.repositories.QPURepository;
import org.quantil.qprov.core.repositories.QubitRepository;
import org.quantil.qprov.web.Constants;
import org.quantil.qprov.web.dtos.GateDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@io.swagger.v3.oas.annotations.tags.Tag(name = Constants.TAG_PROVIDER)
@RestController
@CrossOrigin(allowedHeaders = "*", origins = "*")
@RequestMapping("/" + Constants.PATH_PROVIDERS + "/{providerId}/" + Constants.PATH_QPUS + "/{qpuId}/" + Constants.PATH_QUBITS + "/{qubitId}/" +
        Constants.PATH_GATES)
@AllArgsConstructor
@Slf4j
public class GateController {

    private final QPURepository qpuRepository;

    private final QubitRepository qubitRepository;

    private final GateRepository gateRepository;

    @Operation(responses = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "404", description = "Qubit or QPU not available.")
    }, description = "Retrieve all Gates that can be executed on the Qubit.")
    @GetMapping
    public ResponseEntity<CollectionModel<EntityModel<GateDto>>> getGates(@PathVariable UUID providerId, @PathVariable UUID qpuId,
                                                                          @PathVariable UUID qubitId) {

        // check availability of qpu
        final Optional<QPU> qpuOptional = qpuRepository.findById(qpuId);
        if (qpuOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // check availability of Qubit
        final Optional<Qubit> qubitOptional = qubitRepository.findById(qubitId);
        if (qubitOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        final List<EntityModel<GateDto>> qubitEntities = new ArrayList<>();
        final List<Link> qubitLinks = new ArrayList<>();

        gateRepository.findByQpu(qpuOptional.get()).stream().filter(gate -> gate.getOperatingQubits().contains(qubitOptional.get())).forEach(gate -> {
                    qubitEntities.add(createGateDto(providerId, qpuId, qubitId, gate));
                }
        );

        final var collectionModel = CollectionModel.of(qubitEntities);
        collectionModel.add(qubitLinks);
        collectionModel.add(linkTo(methodOn(GateController.class).getGates(providerId, qpuId, qubitId)).withSelfRel());
        return ResponseEntity.ok(collectionModel);
    }

    @Operation(responses = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "404", description = "Not Found. Gate with given ID doesn't exist.")
    }, description = "Retrieve a specific Qubit and its basic properties.")
    @GetMapping("/{gateId}")
    public ResponseEntity<EntityModel<GateDto>> getGate(
            @PathVariable UUID providerId, @PathVariable UUID qpuId, @PathVariable UUID qubitId, @PathVariable UUID gateId) {

        // check availability of qpu
        final Optional<QPU> qpuOptional = qpuRepository.findById(qpuId);
        if (qpuOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // check availability of Qubit
        final Optional<Qubit> qubitOptional = qubitRepository.findById(qubitId);
        if (qubitOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // check availability of Gate
        final Optional<Gate> gateOptional = gateRepository.findById(gateId);
        if (gateOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(createGateDto(providerId, qpuId, qubitId, gateOptional.get()));
    }

    private EntityModel<GateDto> createGateDto(UUID providerId, UUID qpuId, UUID qubitId, Gate gate) {
        final EntityModel<GateDto> gateDto = EntityModel.of(GateDto.createDTO(gate));
        gateDto.add(linkTo(methodOn(GateController.class).getGate(providerId, qpuId, qubitId, gate.getDatabaseId())).withSelfRel());
        for (Qubit qubit : gate.getOperatingQubits()) {
            gateDto.add(linkTo(methodOn(QubitController.class).getQubit(providerId, qpuId, qubit.getDatabaseId()))
                    .withRel(Constants.PATH_QUBITS_OPERATING + qubit.getDatabaseId()));
        }
        gateDto.add(
                linkTo(methodOn(GateCharacteristicsController.class).getGateCharacterisitcs(providerId, qpuId, qubitId, gate.getDatabaseId(), false))
                        .withRel(Constants.PATH_CHARACTERISTICS));
        return gateDto;
    }
}
