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

import java.util.ArrayList;
import java.util.List;

import org.quantil.qprov.core.model.agents.Provider;
import org.quantil.qprov.core.repositories.ProviderRepository;
import org.quantil.qprov.web.Constants;
import org.quantil.qprov.web.dtos.ProviderDto;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller to access the quantum hardware providers and the corresponding QPUs that were collected as provenance data
 */
@io.swagger.v3.oas.annotations.tags.Tag(name = Constants.TAG_PROVIDER)
@RestController
@CrossOrigin(allowedHeaders = "*", origins = "*")
@RequestMapping("/" + Constants.PATH_PROVIDERS)
@AllArgsConstructor
@Slf4j
public class ProviderController {

    private final ProviderRepository providerRepository;

    @Operation(responses = {
            @ApiResponse(responseCode = "200")
    }, description = "Retrieve all quantum hardware providers.")
    @GetMapping
    public ResponseEntity<CollectionModel<EntityModel<ProviderDto>>> getProviders() {
        final List<EntityModel<ProviderDto>> providerEntities = new ArrayList<>();
        final var collectionModel = new CollectionModel<>(providerEntities);

        final List<Provider> providers = providerRepository.findAll();
        //resource.add(getLinks().linkTo(methodOn(AlgorithmController.class).getAlgorithm(getId(resource))).withSelfRel());
        return ResponseEntity.ok(collectionModel);
    }
}
