# Provenance Collector

The provenance collector is intended to automatically and periodically collect provenance attributes about QPUs that are available via a quantum cloud offering, such as [IBMQ](https://quantum-computing.ibm.com/).
Therefore, the data can be gathered over a period of time, e.g., to gain insights about the timely variation in the decoherence times of qubits of different QPUs.
Furthermore, it aggregates the collected data, e.g., in the form of a calibration matrix to provide it to the user over the QProv API.

The provenance collector is located in [this sub-module](../../org.quantil.qprov.collector) and dockerized by [this Dockerfile](../../Dockerfile-Collector).
It is plug-in based in currently supports the collection of provenance attributes for QPUs from IBMQ, by accessing the corresponding API as well as executing calibration circuits to retrieve required data.
In the following, it is documented how the collector can be configured to enable the periodic data collection.

## Configuration

The provenance collector can either be configured using the [configuration file](../../org.quantil.qprov.collector/src/main/resources/application.yml) if it is build and executed locally or by setting the corresponding environment variables if a dockerized setup is used.
It defines the following properties:

* `QPROV_IBMQ_TOKEN`: 
The access token to enable accessing the API from IBMQ, as well as executing circuits on QPUs available over IBMQ.
Thus, this property has to be set for the successful provenance data collection.
The token can be retrieved in the account settings from the [IBMQ website](https://quantum-computing.ibm.com/).

* `QPROV_IBMQ_EXECUTE_CIRCUITS` (default: `false`): 
This property can be used to define if the provenance data should only be collected by accessing the APIs (set to `false`) or it should also execute calibration circuits to determine missing data (set to `true`).
However, if it is set to false, data that is not available via the provider API, such as the calibration matrix for the QPUs, can not be retrieved.

* `QPROV_IBMQ_AUTO_COLLECT` (default: `false`): 
If the provenance data should be collected periodically by the framework it is set to `true`.
Otherwise, data is only collected when triggered explicitly via the API (see below).
 
* `QPROV_IBMQ_AUTO_COLLECT_INTERVAL` (default: `60`): 
The interval in which the provenance data should be captured in minutes if the `QPROV_IBMQ_AUTO_COLLECT` property is set to `true`.

* `QPROV_IBMQ_AUTO_COLLECT_INTERVAL_CIRCUITS` (default: `180`):
The interval in minutes in which quantum circuits are executed on the QPUs to determine the provenance data if both properties `QPROV_IBMQ_EXECUTE_CIRCUITS` and `QPROV_IBMQ_AUTO_COLLECT` are set to `true`.
This interval should not be to small, as the execution of the calibration circuits can take some time depending on the current queue size of the QPUs.

## Collector API

In addition to the periodic provenance data collection that can be configured by the properties as described above, it is also possible to trigger the data collection over the collector API.
Therefore, the API provides two endpoints for the collection of data over the APIs and by executing calibration circuits:

* POST on `http://$IP:$COLLECTOR_PORT/qprov-collector/collect`: 
Trigger the collection of provenance data from the provider APIs.

* POST on `http://$IP:$COLLECTOR_PORT/qprov-collector/collectCircuit`:  
Trigger the collection of provenance data by executing calibration circuits on the QPUs.


Thereby, `$IP` is the IP address of your system if you run QProv locally, or the IP address of the docker engine if you use the dockerized setup.
Furthermore, `$COLLECTOR_PORT` is the port where the provenance collector runs and defaults to `5021`.

The execution of quantum circuits on IBMQ is done by utilizing the capabilities of the [qiskit-service](https://github.com/UST-QuAntiL/qiskit-service).
Thus, it has to be started when setting `QPROV_IBMQ_EXECUTE_CIRCUITS` to `true`.
The easiest way to get all required services up and running is using the [docker-compose setup](https://github.com/UST-QuAntiL/quantil-docker).
