package org.quantil.qprov.web;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.quantil.qprov.core.entities.QPU;
import org.quantil.qprov.core.entities.QPUProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "qraph")
@RestController
@CrossOrigin(allowedHeaders = "*", origins = "*")
@RequestMapping("/qraph")
public class QubitGrapher {

    private static final Logger logger = LoggerFactory.getLogger(QubitGrapher.class);

    final IRestQPURepository qpuRepository;

    @Autowired
    QubitGrapher(IRestQPURepository qpuRepository) {
        this.qpuRepository = qpuRepository;
    }

    @CrossOrigin(allowedHeaders = "*", origins = "*")
    @GetMapping("/qpus/{backendName}")
    public List<QGraphEdge> getQubitGraph(@PathVariable String backendName) {
        QPU qpu = this.qpuRepository.findByBackendName(backendName);
        QPUProperties qpuProperties = qpu.getProperties();

        // build qubit connections "graph" to be displayed with ngx-graph
        // for docs, node/edge format, ... see https://swimlane.github.io/ngx-graph/
        // TODO: think about removing this and do transformation in ui/typescript...
        ArrayList<QGraphEdge> graphEdges = new ArrayList<>();

        qpuProperties.getGates().forEach(gate -> {

            List<BigDecimal> qubits = gate.getQubits();
            Collections.sort(qubits);

            if (qubits.size() == 2) {

                // TODO: enhance graph with same values as shown at ibmq
                QGraphEdge qEdge = new QGraphEdge();
                qEdge.setId(gate.getName());
                qEdge.setLabel(gate.getName());
                qEdge.setSource(qubits.get(0).intValue());
                qEdge.setTarget(qubits.get(1).intValue());
                graphEdges.add(qEdge);

            }
        });

        logger.debug("generated edges for qraph of " + backendName);
        logger.info(String.valueOf(graphEdges));

        return graphEdges;
    }
}
