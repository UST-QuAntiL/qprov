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

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import javax.servlet.http.HttpServletResponse;

import org.openprovenance.prov.interop.InteropFramework;
import org.quantil.qprov.core.model.ProvDocument;
import org.quantil.qprov.core.repositories.ProvDocumentRepository;
import org.quantil.qprov.web.Constants;
import org.quantil.qprov.web.dtos.ProvDocumentDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller to access and create provenance graphs about quantum computations
 */
@io.swagger.v3.oas.annotations.tags.Tag(name = Constants.TAG_PROV)
@RestController
@CrossOrigin(allowedHeaders = "*", origins = "*")
@RequestMapping("/" + Constants.PATH_PROV)
@AllArgsConstructor
@Slf4j
public class ProvController {

    private static final Logger logger = LoggerFactory.getLogger(ProvController.class);

    private final ProvDocumentRepository provDocumentRepository;

    @Operation(responses = {
            @ApiResponse(responseCode = "200")
    }, description = "Retrieve all stored PROV documents.")
    @GetMapping
    public HttpEntity<RepresentationModel<?>> getProvenanceDocuments() {

        final RepresentationModel<?> responseEntity = new RepresentationModel<>();
        responseEntity.add(linkTo(methodOn(ProvController.class).getProvenanceDocuments()).withSelfRel());

        for (ProvDocument provDocument : provDocumentRepository.findAll()) {
            logger.debug("Found Prov document with Id: {}", provDocument.getDatabaseId());
            responseEntity
                    .add(linkTo(methodOn(ProvController.class).getProvDocument(provDocument.getDatabaseId())).withRel(Constants.PATH_PROV_DOCUMENT +
                            provDocument.getDatabaseId()));
        }

        return ResponseEntity.ok(responseEntity);
    }

    @Operation(responses = {@ApiResponse(responseCode = "201"),
    }, description = "Create a new PROV document and return the link which can then be used to retrieve, update, and delete it.")
    @PostMapping
    public ResponseEntity<EntityModel<ProvDocumentDto>> createProvDocument() {

        // create new PROV document and retrieve Id from database
        ProvDocument provDocument = new ProvDocument();
        provDocument = provDocumentRepository.save(provDocument);

        return ResponseEntity.ok(createEntityModel(provDocument));
    }

    @Operation(responses = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "404", description = "Not Found. PROV document with given ID doesn't exist.")
    }, description = "Retrieve a specific PROV document.")
    @GetMapping("/{provDocumentId}")
    public ResponseEntity<EntityModel<ProvDocumentDto>> getProvDocument(@PathVariable UUID provDocumentId) {

        final Optional<ProvDocument> provDocumentOptional = provDocumentRepository.findById(provDocumentId);
        if (provDocumentOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(createEntityModel(provDocumentOptional.get()));
    }

    @Operation(responses = {
            @ApiResponse(responseCode = "204"),
            @ApiResponse(responseCode = "400"),
            @ApiResponse(responseCode = "404", description = "Not Found. PROV document with given ID doesn't exist.")
    }, description = "Delete a PROV document.")
    @DeleteMapping("/{provDocumentId}")
    public ResponseEntity<Void> deleteProvDocument(@PathVariable UUID provDocumentId) {
        final Optional<ProvDocument> provDocumentOptional = provDocumentRepository.findById(provDocumentId);
        if (provDocumentOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        provDocumentRepository.delete(provDocumentOptional.get());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Operation(responses = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "400"),
            @ApiResponse(responseCode = "404", description = "Not Found. PROV document with given ID doesn't exist.")
    }, description = "Update the properties of a PROV document.")
    @PutMapping("/{provDocumentId}")
    public ResponseEntity<EntityModel<ProvDocumentDto>> updateProvDocument(@PathVariable UUID provDocumentId,
                                                                           @Validated @RequestBody ProvDocumentDto inputProvDocumentDto) {

        final Optional<ProvDocument> provDocumentOptional = provDocumentRepository.findById(provDocumentId);
        if (provDocumentOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // update the PROV document with the given Dto
        inputProvDocumentDto.setDatabaseId(provDocumentId);
        final ProvDocument updatedProvDocument = provDocumentRepository.save(ProvDocumentDto.createPROV(inputProvDocumentDto));

        return ResponseEntity.ok(createEntityModel(updatedProvDocument));
    }

    @Operation(responses = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "404", description = "Not Found. PROV document with given ID doesn't exist.")
    }, description = "Retrieve a specific PROV document and return it as serialized XML document.")
    @GetMapping("/{provDocumentId}/" + Constants.PATH_PROV_XML)
    public HttpEntity<RepresentationModel<?>> getProvDocumentXml(@PathVariable UUID provDocumentId, HttpServletResponse response) {

        logger.debug("Serializing PROV document with Id {} to XML!", provDocumentId);
        final Optional<ProvDocument> provDocumentOptional = provDocumentRepository.findById(provDocumentId);
        if (provDocumentOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        final InteropFramework intF = new InteropFramework();
        try {
            intF.writeDocument(response.getOutputStream(), InteropFramework.ProvFormat.XML, provDocumentOptional.get());
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(responses = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "404", description = "Not Found. PROV document with given ID doesn't exist.")
    }, description = "Retrieve a specific PROV document and return it as JPEG image.")
    @GetMapping("/{provDocumentId}/" + Constants.PATH_PROV_JPEG)
    public HttpEntity<RepresentationModel<?>> getProvDocumentJPEG(@PathVariable UUID provDocumentId, HttpServletResponse response) {

        logger.debug("Serializing PROV document with Id {} to JPEG!", provDocumentId);
        final Optional<ProvDocument> provDocumentOptional = provDocumentRepository.findById(provDocumentId);
        if (provDocumentOptional.isEmpty()) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        final InteropFramework intF = new InteropFramework();
        try {
            intF.writeDocument(response.getOutputStream(), InteropFramework.ProvFormat.JPEG, provDocumentOptional.get());
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private EntityModel<ProvDocumentDto> createEntityModel(ProvDocument provDocument) {
        final EntityModel<ProvDocumentDto> provDocumentDto = new EntityModel<ProvDocumentDto>(ProvDocumentDto.createDTO(provDocument));
        provDocumentDto.add(linkTo(methodOn(ProvController.class).getProvDocument(provDocument.getDatabaseId())).withSelfRel());
        provDocumentDto
                .add(linkTo(methodOn(ProvController.class).getProvDocumentXml(provDocument.getDatabaseId(), null))
                        .withRel(Constants.PATH_PROV_XML));
        provDocumentDto.add(linkTo(methodOn(ProvController.class).getProvDocumentJPEG(provDocument.getDatabaseId(), null))
                .withRel(Constants.PATH_PROV_JPEG));
        return provDocumentDto;
    }
}
