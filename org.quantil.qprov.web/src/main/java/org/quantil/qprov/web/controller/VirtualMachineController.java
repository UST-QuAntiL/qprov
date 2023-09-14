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

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.quantil.qprov.core.model.agents.VirtualMachine;
import org.quantil.qprov.core.repositories.VirtualMachineRepository;
import org.quantil.qprov.web.Constants;
import org.quantil.qprov.web.dtos.VirtualMachineDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
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

@io.swagger.v3.oas.annotations.tags.Tag(name = Constants.TAG_VIRTUAL_MACHINE)
@RestController
@CrossOrigin(allowedHeaders = "*", origins = "*")
@RequestMapping("/" + Constants.PATH_VIRTUAL_MACHINES)
@AllArgsConstructor
@Slf4j
public class VirtualMachineController {

    private static final Logger logger = LoggerFactory.getLogger(ProviderController.class);

    private final VirtualMachineRepository virtualMachineRepository;

    @Operation(responses = {@ApiResponse(responseCode = "200")}, description = "Retrieve all classical hardware virtual machines.")
    @GetMapping
    public ResponseEntity<CollectionModel<EntityModel<VirtualMachineDto>>> getVirtualMachines() {
        final List<EntityModel<VirtualMachineDto>> virtualMachineEntities = new ArrayList<>();

        final List<Link> virtualMachineLinks = new ArrayList<>();
        virtualMachineRepository.findAll().forEach((VirtualMachine virtualMachine) -> {
            logger.debug("Found VirtualMachine with name: {}", virtualMachine.getName());
            final EntityModel<VirtualMachineDto> virtualMachineDto = new EntityModel<VirtualMachineDto>(VirtualMachineDto.createDTO(virtualMachine));
            virtualMachineDto.add(linkTo(methodOn(VirtualMachineController.class).getVirtualMachine(virtualMachine.getDatabaseId())).withSelfRel());
            virtualMachineDto.add(linkTo(methodOn(HardwareCharacteristicsController.class).getHardwareCharacteristics(virtualMachine.getDatabaseId(),
                    false)).withRel(Constants.PATH_CHARACTERISTICS));
            virtualMachineLinks.add(linkTo(methodOn(VirtualMachineController.class).getVirtualMachine(virtualMachine.getDatabaseId())).withRel(
                    virtualMachine.getDatabaseId().toString()));
            virtualMachineEntities.add(virtualMachineDto);
        });

        final var collectionModel = new CollectionModel<>(virtualMachineEntities);
        collectionModel.add(virtualMachineLinks);
        collectionModel.add(linkTo(methodOn(VirtualMachineController.class).getVirtualMachines()).withSelfRel());
        return ResponseEntity.ok(collectionModel);
    }

    @Operation(responses = {@ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "404", description = "Not Found. VirtualMachine with given ID doesn't exist.")},
            description = "Retrieve a specific VirtualMachine and its basic properties.")
    @GetMapping("/{virtualMachineId}")
    public ResponseEntity<EntityModel<VirtualMachineDto>> getVirtualMachine(@PathVariable UUID virtualMachineId) {

        final Optional<VirtualMachine> virtualMachine = virtualMachineRepository.findById(virtualMachineId);
        if (virtualMachine.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        final EntityModel<VirtualMachineDto> virtualMachineDto =
                new EntityModel<VirtualMachineDto>(VirtualMachineDto.createDTO(virtualMachine.get()));
        virtualMachineDto.add(linkTo(methodOn(VirtualMachineController.class).getVirtualMachine(virtualMachineId)).withSelfRel());
        virtualMachineDto.add(
                linkTo(methodOn(HardwareCharacteristicsController.class).getHardwareCharacteristics(virtualMachine.get().getDatabaseId(),
                        false)).withRel(Constants.PATH_CHARACTERISTICS));
        return ResponseEntity.ok(virtualMachineDto);
    }

    @Operation(responses = {
            @ApiResponse(responseCode = "201"), },
            description = "Create a new VirtualMachine and return the link which can then be used to retrieve, update, and delete it.")
    @PostMapping
    public ResponseEntity<EntityModel<VirtualMachineDto>> createVirtualMachine(@RequestBody VirtualMachineDto virtualMachineDto) {

        final VirtualMachine virtualMachine = new VirtualMachine();
        virtualMachine.setName(virtualMachineDto.getName());
        virtualMachine.setCpu(virtualMachineDto.getCpu());
        virtualMachine.setCpuCores(virtualMachineDto.getCpuCores());
        virtualMachine.setRamSize(virtualMachineDto.getRamSize());
        virtualMachine.setDiskSize(virtualMachineDto.getDiskSize());
        virtualMachine.setHardwareCharacteristics(new HashSet<>());


        virtualMachineRepository.save(virtualMachine);

        return new ResponseEntity<>(createEntityModel(virtualMachine), HttpStatus.CREATED);
    }

    private EntityModel<VirtualMachineDto> createEntityModel(VirtualMachine virtualMachine) {
        final EntityModel<VirtualMachineDto> virtualMachineDto = new EntityModel<VirtualMachineDto>(VirtualMachineDto.createDTO(virtualMachine));
        virtualMachineDto.add(linkTo(methodOn(VirtualMachineController.class).getVirtualMachine(virtualMachine.getDatabaseId())).withSelfRel());
        return virtualMachineDto;
    }


}


