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

import org.quantil.qprov.core.model.prov.ProvActivity;
import org.quantil.qprov.core.model.prov.ProvDocument;
import org.quantil.qprov.core.model.prov.ProvQualifiedName;
import org.quantil.qprov.core.repositories.prov.ProvActivityRepository;
import org.quantil.qprov.core.repositories.prov.ProvDocumentRepository;
import org.quantil.qprov.core.repositories.prov.QualifiedNameRepository;
import org.quantil.qprov.web.Constants;
import org.quantil.qprov.web.dtos.ProvActivityDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.openprovenance.prov.sql.Activity;
import org.openprovenance.prov.sql.Document;
import org.openprovenance.prov.sql.ObjectFactory;
import org.openprovenance.prov.sql.QualifiedName;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@io.swagger.v3.oas.annotations.tags.Tag(name = Constants.TAG_PROV)
@RestController
@CrossOrigin(allowedHeaders = "*", origins = "*")
@RequestMapping("/" + Constants.PATH_PROV + "/{provDocumentId}/" + Constants.PATH_PROV_ACTIVITIES)
@AllArgsConstructor
@Slf4j
public class ProvActivityController {

    private static final Logger logger = LoggerFactory.getLogger(ProvActivityController.class);

    private final ProvDocumentRepository provDocumentRepository;

    private final ProvActivityRepository provActivityRepository;

    private final QualifiedNameRepository qualifiedNameRepository;

    private final ObjectFactory factory = new ObjectFactory();

    private final ModelMapper modelMapper = new ModelMapper();

    @Operation(responses = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "404", description = "PROV document with the ID not available.")
    }, description = "Retrieve all PROV activities of the PROV document.")
    @GetMapping
    public ResponseEntity<CollectionModel<EntityModel<ProvActivityDto>>> getProvActivities(@PathVariable Long provDocumentId) {

        // check availability of PROV document
        final Optional<ProvDocument> provDocumentOptional = provDocumentRepository.findById(provDocumentId);
        if (provDocumentOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        final List<EntityModel<ProvActivityDto>> provActivityEntities = new ArrayList<>();
        final List<Link> provActivityLinks = new ArrayList<>();

        for (Activity activity : provActivityRepository.findAll()) {
            logger.debug("Found Prov activity with Id: {}", activity.getId());
            provActivityLinks.add(linkTo(methodOn(ProvActivityController.class).getProvActivity(provDocumentId, activity.getPk()))
                    .withRel(activity.getPk().toString()));
            provActivityEntities.add(createEntityModel(provDocumentId, activity));
        }

        final var collectionModel = CollectionModel.of(provActivityEntities);
        collectionModel.add(provActivityLinks);
        collectionModel.add(linkTo(methodOn(ProvActivityController.class).getProvActivities(provDocumentId)).withSelfRel());
        return ResponseEntity.ok(collectionModel);
    }

    @Operation(responses = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "404", description = "Not Found. PROV activity with given ID doesn't exist.")
    }, description = "Retrieve a specific PROV activity.")
    @GetMapping("/{provActitvityId}")
    public ResponseEntity<EntityModel<ProvActivityDto>> getProvActivity(@PathVariable Long provDocumentId, @PathVariable Long provActitvityId) {

        final Optional<ProvDocument> provDocumentOptional = provDocumentRepository.findById(provDocumentId);
        final Optional<ProvActivity> provActivityOptional = provActivityRepository.findById(provActitvityId);
        if (provDocumentOptional.isEmpty() || provActivityOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(createEntityModel(provDocumentId, provActivityOptional.get()));
    }

    @Operation(responses = {
            @ApiResponse(responseCode = "204"),
            @ApiResponse(responseCode = "400"),
            @ApiResponse(responseCode = "404", description = "Not Found. PROV activity with given ID doesn't exist.")
    }, description = "Delete a PROV activity.")
    @DeleteMapping("/{provActitvityId}")
    public ResponseEntity<Void> deleteProvActivity(@PathVariable Long provDocumentId, @PathVariable Long provActitvityId) {

        final Optional<ProvDocument> provDocumentOptional = provDocumentRepository.findById(provDocumentId);
        final Optional<ProvActivity> provActivityOptional = provActivityRepository.findById(provActitvityId);
        if (provDocumentOptional.isEmpty() || provActivityOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        provActivityRepository.delete(provActivityOptional.get());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Operation(responses = {@ApiResponse(responseCode = "201"),
            @ApiResponse(responseCode = "404", description = "Not Found. PROV document with given ID doesn't exist.")
    }, description = "Create a new PROV activity in the specified PROV document.")
    @PostMapping
    public ResponseEntity<EntityModel<ProvActivityDto>> addProvActivityToDocument(@PathVariable Long provDocumentId,
                                                                                  @RequestBody ProvQualifiedName qualifiedName) {

        logger.debug("Adding new PROV activity to document with Id: {}", provDocumentId);

        // check availability of PROV document
        final Optional<ProvDocument> provDocumentOptional = provDocumentRepository.findById(provDocumentId);
        if (provDocumentOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        final ProvDocument provDocument = provDocumentOptional.get();

        final Activity activity = factory.createActivity();
        qualifiedNameRepository.save(qualifiedName);
        activity.setId(qualifiedName);

        provDocument.getStatementOrBundle().add(activity);
        provDocumentRepository.save(provDocument);
        return new ResponseEntity<>(EntityModel.of(ProvActivityDto.createDTO(activity)), HttpStatus.CREATED);
    }

    @Operation(responses = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "404", description = "Not Found. PROV document or agent with given ID doesn't exist.")
    }, description = "Update the activity in a specific PROV document.")
    @PutMapping("/{provActivityId}")
    public ResponseEntity<EntityModel<ProvActivityDto>> setProvActivity(@PathVariable Long provDocumentId,
                                                                        @PathVariable Long provActivityId,
                                                                        @RequestBody ProvActivityDto provActivityDto) {

        // check availability of PROV document and activity
        final Optional<ProvDocument> provDocumentOptional = provDocumentRepository.findById(provDocumentId);
        final Optional<ProvActivity> provActivityOptional = provActivityRepository.findById(provActivityId);
        if (provDocumentOptional.isEmpty() || provActivityOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // update activity with passed data
        ProvActivity newActivity = modelMapper.map(provActivityDto, ProvActivity.class);
        newActivity.setPk(provActivityId);
        newActivity = provActivityRepository.save(newActivity);

        return ResponseEntity.ok(createEntityModel(provDocumentId, newActivity));
    }

    private EntityModel<ProvActivityDto> createEntityModel(Long provDocumentId, Activity activity) {
        final EntityModel<ProvActivityDto> entityModel = EntityModel.of(ProvActivityDto.createDTO(activity));
        entityModel.add(linkTo(methodOn(ProvActivityController.class).getProvActivity(provDocumentId, activity.getPk())).withSelfRel());
        return entityModel;
    }
}
