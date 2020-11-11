# QProv

[![build images](https://github.com/UST-QuAntiL/QProv/workflows/build%20images/badge.svg)](https://github.com/orgs/UST-QuAntiL/packages?repo_name=QProv)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A provenance system for quantum computing.

Please refer to the [documentation](docs) for details of the possible usage of the system.

The underlying provenance data model of the QProv system can be found [here](docs/data-model).

## Build

1. Run `mvn package -DskipTests` inside the root folder.
2. When completed, the built product for the provenance system can be found in `org.quantil.qprov.web/target` and for the provenance collector in `org.quantil.qprov.collector/target`.

## Running via Docker

TODO

## Running on Tomcat

TODO

## notes

### api

openapi spec:

* openapi json: <http://127.0.0.1:1337/v3/api-docs>
* openapi yaml: <http://127.0.0.1:1337/v3/api-docs.yaml>

swagger ui & config

* swagger ui: <http://127.0.0.1:1337/swagger-ui.html>
* swagger config: <http://127.0.0.1:1337/v3/api-docs/swagger-config>

hal (??)

* explorer  
  <http://127.0.0.1:1337/explorer/index.html#uri=http://127.0.0.1:1337>

### collector

currently the collection can be triggered via a HTTP endpoint.  
example using [httpie](https://github.com/httpie/httpie) (you can also use the famous `curl`)

```bash
echo '[{"provider": "ibmq", "token": "<ibmq api key>"}]' | http POST http://127.0.0.1:7331/collect
```

```bash
http POST http://127.0.0.1:7331/collect  # if token is set via QPROV_IBMQ_TOKEN
```

## Haftungsausschluss

Dies ist ein Forschungsprototyp.
Die Haftung für entgangenen Gewinn, Produktionsausfall, Betriebsunterbrechung, entgangene Nutzungen, Verlust von Daten und Informationen, Finanzierungsaufwendungen sowie sonstige Vermögens- und Folgeschäden ist, außer in Fällen von grober Fahrlässigkeit, Vorsatz und Personenschäden, ausgeschlossen.

## Disclaimer of Warranty

Unless required by applicable law or agreed to in writing, Licensor provides the Work (and each Contributor provides its Contributions) on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied, including, without limitation, any warranties or conditions of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A PARTICULAR PURPOSE.
You are solely responsible for determining the appropriateness of using or redistributing the Work and assume any risks associated with Your exercise of permissions under this License.
