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

package org.quantil.qprov.core.model.agents;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.persistence.*;

import org.hibernate.annotations.GenericGenerator;
import org.openprovenance.prov.model.Statement;
import org.openprovenance.prov.xml.Agent;
import org.quantil.qprov.core.Constants;
import org.quantil.qprov.core.model.ProvExtension;
import org.quantil.qprov.core.model.entities.HardwareCharacteristics;
import org.quantil.qprov.core.utils.Utils;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
public class VirtualMachine extends org.openprovenance.prov.xml.Agent implements ProvExtension<VirtualMachine> {

    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "databaseId", updatable = false, nullable = false)
    private UUID databaseId;

    private String name;

    private String cpu;

    private int cpuCores;

    private int ramSize;

    private int diskSize;

    @OneToMany(mappedBy = "virtualMachine",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<HardwareCharacteristics> hardwareCharacteristics = new HashSet<>();

    @Override
    public Set<Statement> toStandardCompliantProv(VirtualMachine extensionStatement) {
        final Agent agent = new Agent();
        agent.setId(Utils.generateQualifiedName(name, null));
        agent.getType().add(Utils.createTypeElement(Constants.QPROV_TYPE_VIRTUAL_MACHINE));
        agent.getOther().add(Utils
                .createOtherElement(Constants.QPROV_TYPE_VIRTUAL_MACHINE_NAME, name,
                        Constants.QPROV_TYPE_VIRTUAL_MACHINE_NAME + Constants.QPROV_TYPE_SUFFIX));
        agent.getOther().add(Utils
                .createOtherElement(Constants.QPROV_TYPE_VIRTUAL_MACHINE_CPU_NAME, cpu,
                        Constants.QPROV_TYPE_VIRTUAL_MACHINE_CPU_NAME + Constants.QPROV_TYPE_SUFFIX));
        agent.getOther().add(Utils
                .createOtherElement(Constants.QPROV_TYPE_VIRTUAL_MACHINE_CPU_CORES_COUNT, String.valueOf(cpuCores),
                        Constants.QPROV_TYPE_VIRTUAL_MACHINE_CPU_CORES_COUNT + Constants.QPROV_TYPE_SUFFIX));
        agent.getOther().add(Utils
                .createOtherElement(Constants.QPROV_TYPE_VIRTUAL_MACHINE_RAM_SIZE, String.valueOf(ramSize),
                        Constants.QPROV_TYPE_VIRTUAL_MACHINE_RAM_SIZE + Constants.QPROV_TYPE_SUFFIX));
        agent.getOther().add(Utils
                .createOtherElement(Constants.QPROV_TYPE_VIRTUAL_MACHINE_DISK_SIZE, String.valueOf(diskSize),
                        Constants.QPROV_TYPE_VIRTUAL_MACHINE_DISK_SIZE + Constants.QPROV_TYPE_SUFFIX));

        // add latest usage data
        String recordingTime = Constants.QPROV_CHARACTERISTICS_NO_DATA;
        String cpuUsage = Constants.QPROV_CHARACTERISTICS_NO_DATA;
        String clockSpeed = Constants.QPROV_CHARACTERISTICS_NO_DATA;
        String ramUsage = Constants.QPROV_CHARACTERISTICS_NO_DATA;
        String diskUsage = Constants.QPROV_CHARACTERISTICS_NO_DATA;

        final Optional<HardwareCharacteristics> currentCharacteristicsOptional = extensionStatement.getHardwareCharacteristics().stream()
                .min(Comparator.comparing(HardwareCharacteristics::getRecordingTime));
        if (currentCharacteristicsOptional.isPresent()) {
            // update with the latest usage data
            final HardwareCharacteristics currentCharacteristics = currentCharacteristicsOptional.get();
            recordingTime = currentCharacteristics.getRecordingTime().toString();
            cpuUsage = String.valueOf(currentCharacteristics.getCpuUsage());
            clockSpeed = String.valueOf(currentCharacteristics.getClockSpeed());
            ramUsage = String.valueOf(currentCharacteristics.getRamUsage());
            diskUsage = String.valueOf(currentCharacteristics.getDiskUsage());
        }
        agent.getOther().add(Utils.createOtherElement(Constants.QPROV_TYPE_VIRTUAL_MACHINE_RECORDING_TIME,
                recordingTime, Constants.QPROV_TYPE_VIRTUAL_MACHINE_RECORDING_TIME + Constants.QPROV_TYPE_SUFFIX));
        agent.getOther().add(Utils.createOtherElement(Constants.QPROV_TYPE_VIRTUAL_MACHINE_CPU_USAGE,
                cpuUsage, Constants.QPROV_TYPE_VIRTUAL_MACHINE_CPU_USAGE + Constants.QPROV_TYPE_SUFFIX));
        agent.getOther().add(Utils.createOtherElement(Constants.QPROV_TYPE_VIRTUAL_MACHINE_CLOCK_SPEED,
                clockSpeed, Constants.QPROV_TYPE_VIRTUAL_MACHINE_CLOCK_SPEED + Constants.QPROV_TYPE_SUFFIX));
        agent.getOther().add(Utils.createOtherElement(Constants.QPROV_TYPE_VIRTUAL_MACHINE_RAM_USAGE,
                ramUsage, Constants.QPROV_TYPE_VIRTUAL_MACHINE_RAM_USAGE + Constants.QPROV_TYPE_SUFFIX));
        agent.getOther().add(Utils.createOtherElement(Constants.QPROV_TYPE_VIRTUAL_MACHINE_DISK_USAGE,
                diskUsage, Constants.QPROV_TYPE_VIRTUAL_MACHINE_DISK_USAGE + Constants.QPROV_TYPE_SUFFIX));
        return Stream.of(agent).collect(Collectors.toCollection(HashSet::new));
    }
}
