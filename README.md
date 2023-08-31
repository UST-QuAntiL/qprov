# QProv

![Java CI with Maven](https://github.com/UST-QuAntiL/qprov/workflows/Java%20CI%20with%20Maven/badge.svg)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A provenance system for quantum computing, which allows collecting and analyzing important provenance attributes about quantum computations.
The underlying provenance data model of the QProv system can be found [here](docs/data-model).
Therefore, it can be used to support different use cases, such as [selecting suitable quantum hardware](https://github.com/UST-QuAntiL/nisq-analyzer) for the execution of a quantum circuit, providing a basis for transpilers and optimizers, or finding the origins of errors in quantum computations.

Please refer to the [documentation](docs) for details about the possible usage of the system, as well as details about its configuration and how to set up the system for development.

## Build

1. Run `mvn package -DskipTests` inside the root folder.
2. When completed, the built product for the provenance system can be found in `org.quantil.qprov.web/target` and for the provenance collector in `org.quantil.qprov.collector/target`.

## Running via Docker

The easiest way to get started is using Docker-Compose: [quantil-docker](https://github.com/UST-QuAntiL/quantil-docker)

Alternatively you can build and run the QProv Docker images by your own:

1. Build the collector:
    `docker build -t collector -f Dockerfile-Collector .`
2. Build the QProv system:
    `docker build -t qprov -f Dockerfile-Web .`
3. Run the Docker containers: `docker run -p 5020:5020 qprov` and `docker run -p 5021:5021 collector`

Then, the QProv system can be accessed on <http://localhost:5020/qprov>. 

The collection of current data can either be triggered via the collector API (POST on <http://localhost:5021/qprov-collector/collect>) or it can be configured to run the collection periodically (please refer to the [documentation](docs)).

You can also use the pre-built images:

    docker run -p 5020:5020 planqk/qprov
    docker run -p 5021:5021 planqk/qprov-collector

## Running on embedded Tomcat

The built products can also be executed directly using the embedded Tomcat server:

    java -jar org.quantil.qprov.collector.jar
    java -jar org.quantil.qprov.web.jar

## API documentation

The QProv systems provides a Swagger UI, as well as an HAL browser, which can be accessed on the following URLs:

* Swagger UI: <http://localhost:5020/qprov/swagger-ui>
* HAL browser: <http://localhost:5020/qprov/explorer>
  
Furthermore, the OpenAPI specification can be found here:
  
* OpenAPI Json: <http://localhost:5020/qprov/v3/api-docs>
* OpenAPI Yaml: <http://localhost:5020/qprov/v3/api-docs.yaml>

## Haftungsausschluss

Dies ist ein Forschungsprototyp.
Die Haftung für entgangenen Gewinn, Produktionsausfall, Betriebsunterbrechung, entgangene Nutzungen, Verlust von Daten und Informationen, Finanzierungsaufwendungen sowie sonstige Vermögens- und Folgeschäden ist, außer in Fällen von grober Fahrlässigkeit, Vorsatz und Personenschäden, ausgeschlossen.

## Disclaimer of Warranty

Unless required by applicable law or agreed to in writing, Licensor provides the Work (and each Contributor provides its Contributions) on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied, including, without limitation, any warranties or conditions of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A PARTICULAR PURPOSE.
You are solely responsible for determining the appropriateness of using or redistributing the Work and assume any risks associated with Your exercise of permissions under this License.
