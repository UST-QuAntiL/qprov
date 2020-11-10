package org.quantil.qprov.core.repositories;


import org.jetbrains.annotations.NotNull;
import org.quantil.qprov.core.entities.QPU;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QPURepository extends CrudRepository<QPU, Long> {

    QPU findByBackendName(String backendName);

    // disabled as currently not in use which makes the linter angry
    // QPU findById(long id);

    @NotNull Iterable<QPU> findAll();
}
