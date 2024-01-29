/*******************************************************************************
 * Copyright (c) 2024 the QProv contributors.
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

import org.quantil.qprov.core.model.prov.ProvAgent;
import org.quantil.qprov.core.model.prov.ProvDocument;
import org.quantil.qprov.core.model.prov.ProvQualifiedName;
import org.quantil.qprov.core.repositories.prov.ProvAgentRepository;
import org.quantil.qprov.core.repositories.prov.ProvDocumentRepository;
import org.quantil.qprov.core.repositories.prov.QualifiedNameRepository;
import org.quantil.qprov.web.Constants;
import org.quantil.qprov.web.dtos.ProvAgentDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.modelmapper.ModelMapper;
import org.openprovenance.prov.sql.Agent;
import org.openprovenance.prov.sql.ObjectFactory;
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
@RequestMapping("/" + Constants.PATH_PROV + "/{provDocumentId}/" + Constants.PATH_PROV_AGENTS)
@AllArgsConstructor
@Slf4j
public class ProvAgentController {

    protected static final Logger logger = LogManager.getLogger();

    private final ProvDocumentRepository provDocumentRepository;

    private final ProvAgentRepository provAgentRepository;

    private final QualifiedNameRepository qualifiedNameRepository;

    private final ObjectFactory factory = new ObjectFactory();

    private final ModelMapper modelMapper = new ModelMapper();

    @Operation(responses = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "404", description = "PROV document with the ID not available.")
    }, description = "Retrieve all PROV agents of the PROV document.")
    @GetMapping
    public ResponseEntity<CollectionModel<EntityModel<ProvAgentDto>>> getProvAgents(@PathVariable Long provDocumentId) {

        // check availability of PROV document
        final Optional<ProvDocument> provDocumentOptional = provDocumentRepository.findById(provDocumentId);
        if (provDocumentOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        final List<EntityModel<ProvAgentDto>> provAgentEntities = new ArrayList<>();
        final List<Link> provAgentLinks = new ArrayList<>();

        for (Agent agent : provAgentRepository.findAll()) {
            logger.debug("Found Prov agent with Id: {}", agent.getId());
            provAgentLinks.add(linkTo(methodOn(ProvAgentController.class).getProvAgent(provDocumentId, agent.getPk()))
                    .withRel(agent.getPk().toString()));
            provAgentEntities.add(createEntityModel(provDocumentId, agent));
        }

        final var collectionModel = CollectionModel.of(provAgentEntities);
        collectionModel.add(provAgentLinks);
        collectionModel.add(linkTo(methodOn(ProvAgentController.class).getProvAgents(provDocumentId)).withSelfRel());
        return ResponseEntity.ok(collectionModel);
    }

    @Operation(responses = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "404", description = "Not Found. PROV agent with given ID doesn't exist.")
    }, description = "Retrieve a specific PROV agent.")
    @GetMapping("/{provAgentId}")
    public ResponseEntity<EntityModel<ProvAgentDto>> getProvAgent(@PathVariable Long provDocumentId, @PathVariable Long provAgentId) {

        final Optional<ProvDocument> provDocumentOptional = provDocumentRepository.findById(provDocumentId);
        final Optional<ProvAgent> provAgentOptional = provAgentRepository.findById(provAgentId);
        if (provDocumentOptional.isEmpty() || provAgentOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(createEntityModel(provDocumentId, provAgentOptional.get()));
    }

    @Operation(responses = {
            @ApiResponse(responseCode = "204"),
            @ApiResponse(responseCode = "400"),
            @ApiResponse(responseCode = "404", description = "Not Found. PROV agent with given ID doesn't exist.")
    }, description = "Delete a PROV agent.")
    @DeleteMapping("/{provAgentId}")
    public ResponseEntity<Void> deleteProvAgent(@PathVariable Long provDocumentId, @PathVariable Long provAgentId) {

        final Optional<ProvDocument> provDocumentOptional = provDocumentRepository.findById(provDocumentId);
        final Optional<ProvAgent> provAgentOptional = provAgentRepository.findById(provAgentId);
        if (provDocumentOptional.isEmpty() || provAgentOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        provAgentRepository.delete(provAgentOptional.get());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Operation(responses = {@ApiResponse(responseCode = "201"),
            @ApiResponse(responseCode = "404", description = "Not Found. PROV document with given ID doesn't exist.")
    }, description = "Create a new PROV agent in the specified PROV document.")
    @PostMapping
    public ResponseEntity<EntityModel<ProvAgentDto>> addProvAgentToDocument(@PathVariable Long provDocumentId,
                                                                            @RequestBody ProvQualifiedName qualifiedName) {

        logger.debug("Adding new PROV agent to document with Id: {}", provDocumentId);

        // check availability of PROV document
        final Optional<ProvDocument> provDocumentOptional = provDocumentRepository.findById(provDocumentId);
        if (provDocumentOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        final ProvDocument provDocument = provDocumentOptional.get();

        final Agent agent = factory.createAgent();
        qualifiedNameRepository.save(qualifiedName);
        agent.setId(qualifiedName);

        provDocument.getStatementOrBundle().add(agent);
        provDocumentRepository.save(provDocument);
        return new ResponseEntity<>(EntityModel.of(ProvAgentDto.createDTO(agent)), HttpStatus.CREATED);
    }

    @Operation(responses = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "404", description = "Not Found. PROV document or agent with given ID doesn't exist.")
    }, description = "Update the agent in a specific PROV document.")
    @PutMapping("/{provAgentId}")
    public ResponseEntity<EntityModel<ProvAgentDto>> setProvAgent(@PathVariable Long provDocumentId,
                                                                  @PathVariable Long provAgentId,
                                                                  @RequestBody ProvAgentDto provAgentDto) {

        // check availability of PROV document and agent
        final Optional<ProvDocument> provDocumentOptional = provDocumentRepository.findById(provDocumentId);
        final Optional<ProvAgent> provAgentOptional = provAgentRepository.findById(provAgentId);
        if (provDocumentOptional.isEmpty() || provAgentOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // update agent with passed data
        ProvAgent newAgent = modelMapper.map(provAgentDto, ProvAgent.class);
        newAgent.setPk(provAgentId);
        newAgent = provAgentRepository.save(newAgent);

        return ResponseEntity.ok(createEntityModel(provDocumentId, newAgent));
    }

    private EntityModel<ProvAgentDto> createEntityModel(Long provDocumentId, Agent agent) {
        final EntityModel<ProvAgentDto> entityModel = EntityModel.of(ProvAgentDto.createDTO(agent));
        entityModel.add(linkTo(methodOn(ProvAgentController.class).getProvAgent(provDocumentId, agent.getPk())).withSelfRel());
        return entityModel;
    }
}
