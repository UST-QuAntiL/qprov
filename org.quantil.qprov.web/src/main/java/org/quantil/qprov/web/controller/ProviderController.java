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

import org.quantil.qprov.core.model.agents.Provider;
import org.quantil.qprov.core.repositories.ProviderRepository;
import org.quantil.qprov.web.Constants;
import org.quantil.qprov.web.dtos.ProviderDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
    protected static final Logger logger = LogManager.getLogger();

    private final ProviderRepository providerRepository;

    @Operation(responses = {
            @ApiResponse(responseCode = "200")
    }, description = "Retrieve all quantum hardware providers.")
    @GetMapping
    public ResponseEntity<CollectionModel<EntityModel<ProviderDto>>> getProviders() {
        final List<EntityModel<ProviderDto>> providerEntities = new ArrayList<>();

        final List<Link> providerLinks = new ArrayList<>();
        providerRepository.findAll().forEach((Provider provider) -> {
                    logger.debug("Found provider with name: {}", provider.getName());
                    final EntityModel<ProviderDto> providerDto = EntityModel.of(ProviderDto.createDTO(provider));
                    providerDto.add(linkTo(methodOn(ProviderController.class).getProvider(provider.getDatabaseId()))
                            .withSelfRel());
                    providerDto.add(linkTo(methodOn(QpuController.class).getQPUs(provider.getDatabaseId())).withRel(Constants.PATH_QPUS));
                    providerLinks.add(linkTo(methodOn(ProviderController.class).getProvider(provider.getDatabaseId()))
                            .withRel(provider.getDatabaseId().toString()));
                    providerEntities.add(providerDto);
                }
        );

        final var collectionModel = CollectionModel.of(providerEntities);
        collectionModel.add(providerLinks);
        collectionModel.add(linkTo(methodOn(ProviderController.class).getProviders()).withSelfRel());
        return ResponseEntity.ok(collectionModel);
    }

    @Operation(responses = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "404", description = "Not Found. Provider with given ID doesn't exist.")
    }, description = "Retrieve a specific provider and its basic properties.")
    @GetMapping("/{providerId}")
    public ResponseEntity<EntityModel<ProviderDto>> getProvider(
            @PathVariable UUID providerId) {

        final Optional<Provider> provider = providerRepository.findById(providerId);
        if (provider.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        final EntityModel<ProviderDto> providerDto = EntityModel.of(ProviderDto.createDTO(provider.get()));
        providerDto.add(linkTo(methodOn(ProviderController.class).getProvider(providerId)).withSelfRel());
        providerDto.add(linkTo(methodOn(QpuController.class).getQPUs(providerId)).withRel(Constants.PATH_QPUS));
        return ResponseEntity.ok(providerDto);
    }
}
