package org.quantil.qprov.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.jetbrains.annotations.NotNull;
import org.quantil.qprov.core.entities.QPU;
import org.quantil.qprov.core.repositories.QPURepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "qpu")
@RestController
@CrossOrigin(allowedHeaders = "*", origins = "*")
@RepositoryRestResource(collectionResourceRel = "qpus", path = "qpus")
public interface IRestQPURepository extends QPURepository {

    QPU findByBackendName(@Param("backendName") String backendName);

    // disabled as currently not in use which makes the linter angry
    // QPU findById(@Param("id") long id);

    @NotNull Iterable<QPU> findAll();
}
