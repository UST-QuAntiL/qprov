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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.servlet.http.HttpServletResponse;

import org.quantil.qprov.core.model.prov.ProvDocument;
import org.quantil.qprov.core.repositories.prov.ProvDocumentRepository;
import org.quantil.qprov.core.utils.ProvInteroperabilityUtils;
import org.quantil.qprov.web.Constants;
import org.quantil.qprov.web.dtos.ProvDocumentDto;
import org.quantil.qprov.web.dtos.ProvNamespaceDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openprovenance.prov.interop.Formats;
import org.openprovenance.prov.interop.InteropFramework;
import org.openprovenance.prov.sql.Document;
import org.openprovenance.prov.sql.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpEntity;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Controller to access and create provenance graphs about quantum computations
 */
@io.swagger.v3.oas.annotations.tags.Tag(name = Constants.TAG_PROV)
@RestController
@CrossOrigin(allowedHeaders = "*", origins = "*")
@RequestMapping("/" + Constants.PATH_PROV)
@AllArgsConstructor
@Slf4j
public class ProvDocumentController {

    private static final Logger logger = LoggerFactory.getLogger(ProvDocumentController.class);

    private final ProvDocumentRepository provDocumentRepository;

    private final ProvInteroperabilityUtils provInteroperabilityUtils;

    private final InteropFramework intF = new InteropFramework();

    @Operation(responses = {
            @ApiResponse(responseCode = "200")
    }, description = "Retrieve all stored PROV documents.")
    @GetMapping
    public ResponseEntity<CollectionModel<EntityModel<ProvDocumentDto>>> getProvenanceDocuments() {

        final List<EntityModel<ProvDocumentDto>> provDocumentEntities = new ArrayList<>();
        final List<Link> provDocumentLinks = new ArrayList<>();

        for (Document provDocument : provDocumentRepository.findAll()) {
            logger.debug("Found Prov document with Id: {}", provDocument.getPk());
            provDocumentLinks.add(linkTo(methodOn(ProvDocumentController.class).getProvDocument(provDocument.getPk()))
                    .withRel(provDocument.getPk().toString()));
            provDocumentEntities.add(createEntityModel(provDocument));
        }

        final var collectionModel = CollectionModel.of(provDocumentEntities);
        collectionModel.add(provDocumentLinks);
        collectionModel.add(linkTo(methodOn(ProvDocumentController.class).getProvenanceDocuments()).withSelfRel());
        return ResponseEntity.ok(collectionModel);
    }

    @Operation(responses = {@ApiResponse(responseCode = "201"),
    }, description = "Create a new PROV document and return the link which can then be used to retrieve, update, and delete it.")
    @PostMapping
    public ResponseEntity<EntityModel<ProvDocumentDto>> createProvDocument(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "format", required = false) Formats.ProvFormat format) {

        ProvDocument provDocument = new ProvDocument();
        if (Objects.nonNull(file) && Objects.nonNull(format)) {
            // create document that is passed as input
            try {
                final org.openprovenance.prov.model.Document inputDocument = intF.readDocument(file.getInputStream(), format);
                provDocument = (ProvDocument) provInteroperabilityUtils.createProvSQLDocument(inputDocument);
            } catch (IOException e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } else {
            // prepare namespace of empty PROV document
            final Namespace ns = new Namespace();
            ns.addKnownNamespaces();
            provDocument.setNamespace(ns);
        }

        provDocument = provDocumentRepository.save(provDocument);
        return new ResponseEntity<>(createEntityModel(provDocument), HttpStatus.CREATED);
    }

    @Operation(responses = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "404", description = "Not Found. PROV document with given ID doesn't exist.")
    }, description = "Retrieve a specific PROV document.")
    @GetMapping("/{provDocumentId}")
    public ResponseEntity<EntityModel<ProvDocumentDto>> getProvDocument(@PathVariable Long provDocumentId) {

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
    public ResponseEntity<Void> deleteProvDocument(@PathVariable Long provDocumentId) {
        final Optional<ProvDocument> provDocumentOptional = provDocumentRepository.findById(provDocumentId);
        if (provDocumentOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        provDocumentRepository.delete(provDocumentOptional.get());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Operation(responses = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "404", description = "Not Found. PROV document with given ID doesn't exist.")
    }, description = "Retrieve a specific PROV document and return it as serialized XML document.")
    @GetMapping("/{provDocumentId}/" + Constants.PATH_PROV_XML)
    public HttpEntity<RepresentationModel<?>> getProvDocumentXml(@PathVariable Long provDocumentId, HttpServletResponse response) {

        logger.debug("Serializing PROV document with Id {} to XML!", provDocumentId);
        final Optional<ProvDocument> provDocumentOptional = provDocumentRepository.findById(provDocumentId);
        if (provDocumentOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        final InteropFramework intF = new InteropFramework();
        try {
            intF.writeDocument(response.getOutputStream(), provInteroperabilityUtils.createProvXMLDocument(provDocumentOptional.get()),
                    Formats.ProvFormat.PROVX);
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
    public HttpEntity<RepresentationModel<?>> getProvDocumentJPEG(@PathVariable Long provDocumentId, HttpServletResponse response) {

        logger.debug("Serializing PROV document with Id {} to JPEG!", provDocumentId);
        final Optional<ProvDocument> provDocumentOptional = provDocumentRepository.findById(provDocumentId);
        if (provDocumentOptional.isEmpty()) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        final InteropFramework intF = new InteropFramework();
        try {
            intF.writeDocument(response.getOutputStream(), provInteroperabilityUtils.createProvXMLDocument(provDocumentOptional.get()),
                    Formats.ProvFormat.JPEG);
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(responses = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "404", description = "Not Found. PROV document with given ID doesn't exist.")
    }, description = "Retrieve a specific PROV document and return it as PDF.")
    @GetMapping("/{provDocumentId}/" + Constants.PATH_PROV_PDF)
    public HttpEntity<RepresentationModel<?>> getProvDocumentPDF(@PathVariable Long provDocumentId, HttpServletResponse response) {

        logger.debug("Serializing PROV document with Id {} to PDF!", provDocumentId);
        final Optional<ProvDocument> provDocumentOptional = provDocumentRepository.findById(provDocumentId);
        if (provDocumentOptional.isEmpty()) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        final InteropFramework intF = new InteropFramework();
        try {
            intF.writeDocument(response.getOutputStream(), provInteroperabilityUtils.createProvXMLDocument(provDocumentOptional.get()),
                    Formats.ProvFormat.PDF);
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(responses = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "404", description = "Not Found. PROV document with given ID doesn't exist.")
    }, description = "Retrieve the namespace of a specific PROV document.")
    @GetMapping("/{provDocumentId}/" + Constants.PATH_PROV_NAMESPACE)
    public ResponseEntity<EntityModel<ProvNamespaceDto>> getProvNamespace(@PathVariable Long provDocumentId) {

        final Optional<ProvDocument> provDocumentOptional = provDocumentRepository.findById(provDocumentId);
        if (provDocumentOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        final EntityModel<ProvNamespaceDto> provDocumentDto =
                EntityModel.of(ProvNamespaceDto.createDTO(provDocumentOptional.get().getNamespace()));
        provDocumentDto.add(linkTo(methodOn(ProvDocumentController.class).getProvNamespace(provDocumentId)).withSelfRel());
        return ResponseEntity.ok(provDocumentDto);
    }

    @Operation(responses = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "404", description = "Not Found. PROV document with given ID doesn't exist.")
    }, description = "Update the namespace of a specific PROV document.")
    @PutMapping("/{provDocumentId}/" + Constants.PATH_PROV_NAMESPACE)
    public ResponseEntity<EntityModel<ProvNamespaceDto>> setProvNamespace(@PathVariable Long provDocumentId,
                                                                          @RequestBody ProvNamespaceDto provNamespaceDto) {

        final Optional<ProvDocument> provDocumentOptional = provDocumentRepository.findById(provDocumentId);
        if (provDocumentOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        final ProvDocument provDocument = provDocumentOptional.get();

        // update the namespaces and prefixes
        final org.openprovenance.prov.model.Namespace namespace = provDocument.getNamespace();
        final Map<String, String> namespaceMap = namespace.getNamespaces();
        namespaceMap.clear();
        namespaceMap.putAll(provNamespaceDto.getNamespaces());

        final Map<String, String> prefixMap = namespace.getPrefixes();
        prefixMap.clear();
        prefixMap.putAll(provNamespaceDto.getPrefixes());

        provDocumentRepository.save(provDocument);

        final EntityModel<ProvNamespaceDto> provDocumentDto =
                EntityModel.of(ProvNamespaceDto.createDTO(provDocumentOptional.get().getNamespace()));
        provDocumentDto.add(linkTo(methodOn(ProvDocumentController.class).getProvNamespace(provDocumentId)).withSelfRel());
        return ResponseEntity.ok(provDocumentDto);
    }

    private EntityModel<ProvDocumentDto> createEntityModel(Document provDocument) {
        final EntityModel<ProvDocumentDto> provDocumentDto = EntityModel.of(ProvDocumentDto.createDTO(provDocument));
        provDocumentDto.add(linkTo(methodOn(ProvDocumentController.class).getProvDocument(provDocument.getPk())).withSelfRel());
        provDocumentDto.add(linkTo(methodOn(ProvDocumentController.class).getProvNamespace(provDocument.getPk()))
                .withRel(Constants.PATH_PROV_NAMESPACE));
        provDocumentDto.add(linkTo(methodOn(ProvEntityController.class).getProvEntities(provDocument.getPk()))
                .withRel(Constants.PATH_PROV_ENTITIES));
        provDocumentDto.add(linkTo(methodOn(ProvActivityController.class).getProvActivities(provDocument.getPk()))
                .withRel(Constants.PATH_PROV_ACTIVITIES));
        provDocumentDto.add(linkTo(methodOn(ProvAgentController.class).getProvAgents(provDocument.getPk()))
                .withRel(Constants.PATH_PROV_AGENTS));
        provDocumentDto
                .add(linkTo(methodOn(ProvDocumentController.class).getProvDocumentXml(provDocument.getPk(), null))
                        .withRel(Constants.PATH_PROV_XML));
        provDocumentDto.add(linkTo(methodOn(ProvDocumentController.class).getProvDocumentJPEG(provDocument.getPk(), null))
                .withRel(Constants.PATH_PROV_JPEG));
        return provDocumentDto;
    }
}
