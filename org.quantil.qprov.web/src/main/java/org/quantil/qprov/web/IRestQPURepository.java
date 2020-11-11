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

package org.quantil.qprov.web;

import org.jetbrains.annotations.NotNull;
import org.quantil.qprov.core.entities.QPU_old;
import org.quantil.qprov.core.repositories.QPURepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "qpu")
@RestController
@CrossOrigin(allowedHeaders = "*", origins = "*")
@RepositoryRestResource(collectionResourceRel = "qpus", path = "qpus")
public interface IRestQPURepository extends QPURepository {

    QPU_old findByBackendName(@Param("backendName") String backendName);

    @NotNull Iterable<QPU_old> findAll();
}
