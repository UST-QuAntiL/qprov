package org.quantil.qprov.collector;

import java.util.List;

import org.quantil.qprov.core.entities.QPU;

public interface IProvider {

    String getProviderId();

    // void setApiToken(String apiToken);

    /**
     * check if we need to authenticate before the first query
     *
     * @return true or false
     */
    boolean preAuthenticationNeeded();

    /**
     * authenticate to provider api
     *
     * @return boolean result of authentication attempt
     */
    boolean authenticate(String token);

    /**
     * fetch all data from provider
     *
     * @return boolean result of fetch attempt
     */
    boolean collect();

    /**
     * fetch all data from provider with supplied token
     *
     * @param token api token
     * @return boolean result of fetch attempt
     */
    boolean collect(String token);

    /**
     * fetch qpu data from provider
     *
     * @return list of collected qpus
     */
    List<QPU> collectQPUs();

    /**
     * get available qpus
     * @return list of qpus
     */
    //Iterable<QPU> getQPUs();

    /**
     * get qubits of a qpu
     * @param qpuName name of the device
     */
    //Iterable<Qubit> getQubits(String qpuName);

    /**
     * get characteristics of a qubit
     * @param qubitId id of the qubit
     */
    //QubitCharacteristics getQubitCharacteristics(String qubitId);
}
