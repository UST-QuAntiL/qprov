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

import org.openprovenance.prov.sql.Document;
import org.openprovenance.prov.sql.Entity;
import org.openprovenance.prov.sql.ObjectFactory;
import org.openprovenance.prov.sql.QualifiedName;
import org.quantil.qprov.core.repositories.prov.ProvDocumentRepository;
import org.quantil.qprov.core.repositories.prov.ProvEntityRepository;
import org.quantil.qprov.core.repositories.prov.QualifiedNameRepository;
import org.quantil.qprov.web.Constants;
import org.quantil.qprov.web.dtos.ProvEntityDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@io.swagger.v3.oas.annotations.tags.Tag(name = Constants.TAG_PROV)
@RestController
@CrossOrigin(allowedHeaders = "*", origins = "*")
@RequestMapping("/" + Constants.PATH_PROV + "/{provDocumentId}/" + Constants.PATH_PROV_ENTITIES)
@AllArgsConstructor
@Slf4j
public class ProvEntityController {

    private static final Logger logger = LoggerFactory.getLogger(ProvEntityController.class);

    private final ProvDocumentRepository provDocumentRepository;

    private final ProvEntityRepository provEntityRepository;

    private final QualifiedNameRepository qualifiedNameRepository;

    private final ObjectFactory factory = new ObjectFactory();

    @Operation(responses = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "404", description = "PROV document with the ID not available.")
    }, description = "Retrieve all PROV entities of the PROV document.")
    @GetMapping
    public ResponseEntity<CollectionModel<EntityModel<ProvEntityDto>>> getProvEntities(@PathVariable Long provDocumentId) {

        // check availability of PROV document
        final Optional<Document> provDocumentOptional = provDocumentRepository.findById(provDocumentId);
        if (provDocumentOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        final List<EntityModel<ProvEntityDto>> provElementEntities = new ArrayList<>();
        final List<Link> provElementLinks = new ArrayList<>();

        for (Entity entity : provEntityRepository.findAll()) {
            logger.debug("Found Prov entity with Id: {}", entity.getId());
            provElementLinks.add(linkTo(methodOn(ProvEntityController.class).getProvEntity(provDocumentId, entity.getPk()))
                    .withRel(entity.getPk().toString()));
            provElementEntities.add(createEntityModel(provDocumentId, entity));
        }

        final var collectionModel = new CollectionModel<>(provElementEntities);
        collectionModel.add(provElementLinks);
        collectionModel.add(linkTo(methodOn(ProvEntityController.class).getProvEntities(provDocumentId)).withSelfRel());
        return ResponseEntity.ok(collectionModel);
    }

    @Operation(responses = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "404", description = "Not Found. PROV entity with given ID doesn't exist.")
    }, description = "Retrieve a specific PROV entity.")
    @GetMapping("/{provEntityId}")
    public ResponseEntity<EntityModel<ProvEntityDto>> getProvEntity(@PathVariable Long provDocumentId, @PathVariable Long provEntityId) {

        final Optional<Document> provDocumentOptional = provDocumentRepository.findById(provDocumentId);
        final Optional<Entity> provEntityOptional = provEntityRepository.findById(provEntityId);
        if (provDocumentOptional.isEmpty() || provEntityOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(createEntityModel(provDocumentId, provEntityOptional.get()));
    }

    @Operation(responses = {
            @ApiResponse(responseCode = "204"),
            @ApiResponse(responseCode = "400"),
            @ApiResponse(responseCode = "404", description = "Not Found. PROV entity with given ID doesn't exist.")
    }, description = "Delete a PROV entity.")
    @DeleteMapping("/{provEntityId}")
    public ResponseEntity<Void> deleteProvEntity(@PathVariable Long provDocumentId, @PathVariable Long provEntityId) {

        final Optional<Document> provDocumentOptional = provDocumentRepository.findById(provDocumentId);
        final Optional<Entity> provEntityOptional = provEntityRepository.findById(provEntityId);
        if (provDocumentOptional.isEmpty() || provEntityOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        provEntityRepository.delete(provEntityOptional.get());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Operation(responses = {@ApiResponse(responseCode = "201"),
            @ApiResponse(responseCode = "404", description = "Not Found. PROV document with given ID doesn't exist.")
    }, description = "Create a new PROV entity in the specified PROV document.")
    @PostMapping
    public ResponseEntity<EntityModel<ProvEntityDto>> addProvEntityToDocument(@PathVariable Long provDocumentId,
                                                                              @RequestBody QualifiedName qualifiedName) {

        logger.debug("Adding new PROV entity to document with Id: {}", provDocumentId);

        // check availability of PROV document
        final Optional<Document> provDocumentOptional = provDocumentRepository.findById(provDocumentId);
        if (provDocumentOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        final Document provDocument = provDocumentOptional.get();

        final Entity entity = factory.createEntity();
        qualifiedNameRepository.save(qualifiedName);
        entity.setId(qualifiedName);

        provDocument.getStatementOrBundle().add(entity);
        provDocumentRepository.save(provDocument);
        return new ResponseEntity<>(new EntityModel<>(ProvEntityDto.createDTO(entity)), HttpStatus.CREATED);
    }

    private EntityModel<ProvEntityDto> createEntityModel(Long provDocumentId, Entity entity) {
        final EntityModel<ProvEntityDto> entityModel = new EntityModel<ProvEntityDto>(ProvEntityDto.createDTO(entity));
        entityModel.add(linkTo(methodOn(ProvEntityController.class).getProvEntity(provDocumentId, entity.getPk())).withSelfRel());
        return entityModel;
    }
}
