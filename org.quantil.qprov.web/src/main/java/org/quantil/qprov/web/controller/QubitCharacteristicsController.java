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
import org.quantil.qprov.core.model.entities.Qubit;
import org.quantil.qprov.core.model.entities.QubitCharacteristics;
import org.quantil.qprov.core.repositories.ProviderRepository;
import org.quantil.qprov.core.repositories.QPURepository;
import org.quantil.qprov.core.repositories.QubitCharacteristicsRepository;
import org.quantil.qprov.core.repositories.QubitRepository;
import org.quantil.qprov.web.Constants;
import org.quantil.qprov.web.dtos.QubitCharacteristicsDto;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
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
@RequestMapping("/" + Constants.PATH_PROVIDERS + "/{providerId}/" + Constants.PATH_QPUS + "/{qpuId}/" + Constants.PATH_QUBITS + "/{qubitId}/" +
        Constants.PATH_CHARACTERISTICS)
@AllArgsConstructor
@Slf4j
public class QubitCharacteristicsController {

    private final ProviderRepository providerRepository;

    private final QPURepository qpuRepository;

    private final QubitRepository qubitRepository;

    private final QubitCharacteristicsRepository qubitCharacteristicsRepository;

    @Operation(responses = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "404", description = "No characteristics for this Qubit available.")
    }, description = "Retrieve the calibration characteristics from the given qubit. " +
            "By using the latest parameter only the latest data is retrieved, otherwise all available data.")
    @GetMapping
    public ResponseEntity<CollectionModel<EntityModel<QubitCharacteristicsDto>>> getQubitCharacterisitcs(@PathVariable UUID providerId,
                                                                                                         @PathVariable UUID qpuId,
                                                                                                         @PathVariable UUID qubitId,
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

        // check availability of qubit
        final Optional<Qubit> qubitOptional = qubitRepository.findById(qubitId);
        if (qubitOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        final Stream<QubitCharacteristics> qubitCharacteristicsStream;
        if (latest) {
            // retrieve characteristics with latest calibration time stamp
            qubitCharacteristicsStream = Stream.ofNullable(
                    qubitCharacteristicsRepository.findByQubitOrderByCalibrationTimeDesc(qubitOptional.get()).stream().findFirst().orElse(null));
        } else {
            // retrieve all characteristics
            qubitCharacteristicsStream = qubitCharacteristicsRepository.findByQubitOrderByCalibrationTimeDesc(qubitOptional.get()).stream();
        }

        final List<EntityModel<QubitCharacteristicsDto>> entities = new ArrayList<>();
        qubitCharacteristicsStream.forEach(qubitCharacteristics -> {
            entities.add(new EntityModel<QubitCharacteristicsDto>(QubitCharacteristicsDto.createDTO(qubitCharacteristics)));
        });

        return ResponseEntity.ok(new CollectionModel<>(entities));
    }
}
