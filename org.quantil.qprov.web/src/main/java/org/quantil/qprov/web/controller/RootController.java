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

import org.quantil.qprov.web.Constants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Root controller to access all entities within Quality, trigger the hardware selection, and execution of quantum algorithms.
 */
@io.swagger.v3.oas.annotations.tags.Tag(name = Constants.TAG_ROOT)
@RestController
@CrossOrigin(allowedHeaders = "*", origins = "*")
@RequestMapping("/")
@Slf4j
public class RootController {

    @Operation(responses = {@ApiResponse(responseCode = "200")})
    @GetMapping("/")
    public HttpEntity<RepresentationModel<?>> root() {

        final RepresentationModel<?> responseEntity = new RepresentationModel<>();

        // add self-link and links to sub-controllers
        responseEntity.add(linkTo(methodOn(RootController.class).root()).withSelfRel());
        responseEntity.add(linkTo(methodOn(ProviderController.class).getProviders()).withRel(Constants.PATH_PROVIDERS));
        responseEntity.add(linkTo(methodOn(ProvTemplateController.class).getProvenanceTemplates()).withRel(Constants.PATH_PROV_TEMPLATE));
        responseEntity.add(linkTo(methodOn(ProvDocumentController.class).getProvenanceDocuments()).withRel(Constants.PATH_PROV));

        return ResponseEntity.ok(responseEntity);
    }
}
