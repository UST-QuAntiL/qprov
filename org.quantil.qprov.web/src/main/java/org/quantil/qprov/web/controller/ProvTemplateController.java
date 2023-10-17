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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletResponse;

import org.quantil.qprov.core.model.ProvTemplate;
import org.quantil.qprov.core.repositories.prov.ProvTemplateRepository;
import org.quantil.qprov.core.utils.ProvInteroperabilityUtils;
import org.quantil.qprov.core.utils.Utils;
import org.quantil.qprov.web.Constants;
import org.quantil.qprov.web.dtos.ProvDocumentDto;
import org.openprovenance.prov.interop.Formats;
import org.openprovenance.prov.interop.InteropFramework;
import org.openprovenance.prov.sql.Document;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Controller to access and create PROV templates for quantum computations
 */
@io.swagger.v3.oas.annotations.tags.Tag(name = Constants.TAG_PROV_TEMPLATE)
@RestController
@CrossOrigin(allowedHeaders = "*", origins = "*")
@RequestMapping("/" + Constants.PATH_PROV_TEMPLATE)
@AllArgsConstructor
@Slf4j
public class ProvTemplateController {

    private static final Logger logger = LoggerFactory.getLogger(ProvTemplateController.class);

    private final ProvTemplateRepository provTemplateRepository;

    private final ProvInteroperabilityUtils provInteroperabilityUtils;

    private final InteropFramework intF = new InteropFramework();

    @Operation(responses = {
            @ApiResponse(responseCode = "200")
    }, description = "Retrieve all stored PROV templates.")
    @GetMapping
    public ResponseEntity<CollectionModel<EntityModel<ProvDocumentDto>>> getProvenanceTemplates() {

        final List<EntityModel<ProvDocumentDto>> provDocumentEntities = new ArrayList<>();
        final List<Link> provDocumentLinks = new ArrayList<>();

        for (Document provDocument : provTemplateRepository.findAll()) {
            logger.debug("Found Prov document with Id: {}", provDocument.getPk());
            provDocumentLinks.add(linkTo(methodOn(ProvTemplateController.class).getProvTemplate(provDocument.getPk()))
                    .withRel(provDocument.getPk().toString()));
            provDocumentEntities.add(createEntityModel(provDocument));
        }

        final var collectionModel = CollectionModel.of(provDocumentEntities);
        collectionModel.add(provDocumentLinks);
        collectionModel.add(linkTo(methodOn(ProvTemplateController.class).getProvenanceTemplates()).withSelfRel());
        return ResponseEntity.ok(collectionModel);
    }

    @Operation(responses = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "404", description = "Not Found. PROV document with given ID doesn't exist.")
    }, description = "Retrieve a specific PROV template.")
    @GetMapping("/{provTemplateId}")
    public ResponseEntity<EntityModel<ProvDocumentDto>> getProvTemplate(@PathVariable Long provTemplateId) {

        final Optional<ProvTemplate> provTemplateOptional = provTemplateRepository.findById(provTemplateId);
        if (provTemplateOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(createEntityModel(provTemplateOptional.get()));
    }

    @Operation(responses = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "404", description = "Not Found. PROV template with given ID doesn't exist.")
    }, description = "Retrieve a specific PROV template and return it as serialized XML document.")
    @GetMapping("/{provTemplateId}/" + Constants.PATH_PROV_XML)
    public HttpEntity<RepresentationModel<?>> getProvTemplateXml(@PathVariable Long provTemplateId, HttpServletResponse response) {

        logger.debug("Serializing PROV template with Id {} to XML!", provTemplateId);
        final Optional<ProvTemplate> provTemplateOptional = provTemplateRepository.findById(provTemplateId);
        if (provTemplateOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        try {
            intF.writeDocument(response.getOutputStream(), Formats.ProvFormat.XML,
                    provInteroperabilityUtils.createProvXMLDocument(provTemplateOptional.get()));
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(responses = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "404", description = "Not Found. PROV template with given ID doesn't exist.")
    }, description = "Retrieve a specific PROV template and return it as JPEG image.")
    @GetMapping("/{provTemplateId}/" + Constants.PATH_PROV_JPEG)
    public HttpEntity<RepresentationModel<?>> getProvTemplateJPEG(@PathVariable Long provTemplateId, HttpServletResponse response) {

        logger.debug("Serializing PROV template with Id {} to JPEG!", provTemplateId);
        final Optional<ProvTemplate> provTemplateOptional = provTemplateRepository.findById(provTemplateId);
        if (provTemplateOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        try {
            intF.writeDocument(response.getOutputStream(), Formats.ProvFormat.JPEG,
                    provInteroperabilityUtils.createProvXMLDocument(provTemplateOptional.get()));
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(responses = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "404", description = "Not Found. PROV template with given ID doesn't exist.")
    }, description = "Retrieve a specific PROV template and return it as PDF.")
    @GetMapping("/{provTemplateId}/" + Constants.PATH_PROV_PDF)
    public HttpEntity<RepresentationModel<?>> getProvTemplatePDF(@PathVariable Long provTemplateId, HttpServletResponse response) {

        logger.debug("Serializing PROV template with Id {} to PDF!", provTemplateId);
        final Optional<ProvTemplate> provTemplateOptional = provTemplateRepository.findById(provTemplateId);
        if (provTemplateOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        try {
            intF.writeDocument(response.getOutputStream(), Formats.ProvFormat.PDF,
                    provInteroperabilityUtils.createProvXMLDocument(provTemplateOptional.get()));
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(responses = {
            @ApiResponse(responseCode = "204"),
            @ApiResponse(responseCode = "400"),
            @ApiResponse(responseCode = "404", description = "Not Found. PROV template with given ID doesn't exist.")
    }, description = "Delete a PROV template.")
    @DeleteMapping("/{provTemplateId}")
    public ResponseEntity<Void> deleteProvTemplate(@PathVariable Long provTemplateId) {
        final Optional<ProvTemplate> provTemplateOptional = provTemplateRepository.findById(provTemplateId);
        if (provTemplateOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        provTemplateRepository.delete(provTemplateOptional.get());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Operation(responses = {@ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "500", description = "Server error while processing file.")
    }, description = "Create a new PROV template by uploading the corresponding file.")
    @PostMapping
    public ResponseEntity<EntityModel<ProvDocumentDto>> handleProvTemplateUpload(@RequestParam("file") MultipartFile file,
                                                                                 @RequestParam("format") Formats.ProvFormat format) {

        try {
            final org.openprovenance.prov.model.Document template = intF.readDocument(file.getInputStream(), format, "");
            final Document templateSql = provInteroperabilityUtils.createProvSQLDocument(template);
            ProvTemplate mappedTemplate = Utils.createProvTemplate(templateSql);
            mappedTemplate = provTemplateRepository.save(mappedTemplate);
            return ResponseEntity.ok(createEntityModel(mappedTemplate));
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(responses = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "404", description = "Not Found. PROV template with given ID doesn't exist.")
    }, description = "Retrieve the parameters that are required to instantiate a PROV document from a PROV template.")
    @GetMapping("/{provTemplateId}/" + Constants.PATH_PROV_PARAMETERS)
    public HttpEntity<RepresentationModel<?>> getProvTemplateParameters(@PathVariable Long provTemplateId) {

        logger.debug("Getting parameters for PROV template with Id {}!", provTemplateId);
        final Optional<ProvTemplate> provTemplateOptional = provTemplateRepository.findById(provTemplateId);
        if (provTemplateOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    private EntityModel<ProvDocumentDto> createEntityModel(Document provDocument) {
        final EntityModel<ProvDocumentDto> provDocumentDto = EntityModel.of(ProvDocumentDto.createDTO(provDocument));
        provDocumentDto.add(linkTo(methodOn(ProvTemplateController.class).getProvTemplate(provDocument.getPk())).withSelfRel());
        provDocumentDto
                .add(linkTo(methodOn(ProvTemplateController.class).getProvTemplateXml(provDocument.getPk(), null))
                        .withRel(Constants.PATH_PROV_XML));
        provDocumentDto.add(linkTo(methodOn(ProvTemplateController.class).getProvTemplateJPEG(provDocument.getPk(), null))
                .withRel(Constants.PATH_PROV_JPEG));
        return provDocumentDto;
    }
}
