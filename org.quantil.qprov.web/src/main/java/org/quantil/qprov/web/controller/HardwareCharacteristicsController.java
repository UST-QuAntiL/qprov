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
import java.util.UUID;
import java.util.stream.Stream;
import javax.ws.rs.QueryParam;

import org.quantil.qprov.core.model.agents.VirtualMachine;
import org.quantil.qprov.core.model.entities.HardwareCharacteristics;
import org.quantil.qprov.core.repositories.HardwareCharacteristicsRepository;
import org.quantil.qprov.core.repositories.VirtualMachineRepository;
import org.quantil.qprov.web.Constants;
import org.quantil.qprov.web.dtos.HardwareCharacteristicsDto;
import org.quantil.qprov.web.dtos.VirtualMachineDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@io.swagger.v3.oas.annotations.tags.Tag(name = Constants.TAG_VIRTUAL_MACHINE)
@RestController
@CrossOrigin(allowedHeaders = "*", origins = "*")
@RequestMapping("/" + Constants.PATH_VIRTUAL_MACHINES + "/{virtualMachineId}/" + Constants.PATH_CHARACTERISTICS)
@AllArgsConstructor
@Slf4j
public class HardwareCharacteristicsController {

    private final VirtualMachineRepository virtualMachineRepository;

    private final HardwareCharacteristicsRepository hardwareCharacteristicsRepository;

    @Operation(responses = {@ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "404", description = "No characteristics for this Gate available.")}, description =
            "Retrieve the calibration characteristics from the given gate. " +
                    "By using the latest parameter only the latest data is retrieved, otherwise all available data.")
    @GetMapping
    public ResponseEntity<CollectionModel<EntityModel<HardwareCharacteristicsDto>>> getHardwareCharacteristics(
            @PathVariable UUID virtualMachineId, @QueryParam("latest") boolean latest) {

        // check availability of virtual machine
        final Optional<VirtualMachine> virtualMachineOptional = virtualMachineRepository.findById(virtualMachineId);
        if (virtualMachineOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        final Stream<HardwareCharacteristics> hardwareCharacteristicsStream;
        if (latest) {
            // retrieve characteristics with the latest calibration time stamp
            hardwareCharacteristicsStream = Stream.ofNullable(
                    hardwareCharacteristicsRepository.findByVirtualMachineOrderByRecordingTimeDesc(virtualMachineOptional.get()).stream()
                            .findFirst().orElse(null));
        } else {
            // retrieve all characteristics
            hardwareCharacteristicsStream =
                    hardwareCharacteristicsRepository.findByVirtualMachineOrderByRecordingTimeDesc(virtualMachineOptional.get()).stream();
        }

        final List<EntityModel<HardwareCharacteristicsDto>> entities = new ArrayList<>();
        hardwareCharacteristicsStream.forEach(hardwareCharacteristic -> {
            entities.add(EntityModel.of(HardwareCharacteristicsDto.createDTO(hardwareCharacteristic)));
        });

        if (entities.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(CollectionModel.of(entities));
    }

    @Operation(responses = {@ApiResponse(responseCode = "201"),
    }, description = "Create a new VirtualMachine and return the link which can then be used to retrieve, update, and delete it.")
    @PutMapping()
    public ResponseEntity<EntityModel<VirtualMachineDto>> addHardwareCharacteristics(
            @RequestBody HardwareCharacteristicsDto hardwareCharacteristicsDto, @PathVariable UUID virtualMachineId) {

        final HardwareCharacteristics characteristics = new HardwareCharacteristics();
        final Optional<VirtualMachine> virtualMachineOptional = virtualMachineRepository.findById(virtualMachineId);

        if (virtualMachineOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        final VirtualMachine virtualMachine = virtualMachineOptional.get();

        characteristics.setCpuUsage(hardwareCharacteristicsDto.getCpuUsage());
        characteristics.setClockSpeed(hardwareCharacteristicsDto.getClockSpeed());
        characteristics.setDiskUsage(hardwareCharacteristicsDto.getDiskUsage());
        characteristics.setRamUsage(hardwareCharacteristicsDto.getRamUsage());
        characteristics.setRecordingTime(hardwareCharacteristicsDto.getRecordingTime());
        characteristics.setVirtualMachine(virtualMachine);
        virtualMachine.getHardwareCharacteristics().add(characteristics);

        virtualMachineRepository.save(virtualMachine);

        return new ResponseEntity<>(createEntityModel(virtualMachine), HttpStatus.CREATED);
    }

    private EntityModel<VirtualMachineDto> createEntityModel(VirtualMachine virtualMachine) {
        final EntityModel<VirtualMachineDto> virtualMachineDto = EntityModel.of(VirtualMachineDto.createDTO(virtualMachine));
        virtualMachineDto.add(linkTo(methodOn(VirtualMachineController.class).getVirtualMachine(virtualMachine.getDatabaseId())).withSelfRel());
        return virtualMachineDto;
    }
}
