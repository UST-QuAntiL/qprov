# QProv 💥 Quantum Provenance 💡 [![build images](https://github.com/UST-QuAntiL/QProv/workflows/build%20images/badge.svg)](https://github.com/orgs/UST-QuAntiL/packages?repo_name=QProv)

A Provenance System for Quantum Computing

## modules

* [core](org.quantil.qprov.core/)
  * provides common things like model, utils, ... to the other modules (`api` and `collector`)
  * generates the sources for the api clients of the collector

* [collector](org.quantil.qprov.collector/)
  * fetches data from cloud quantum resource providers like IBMQ or rigetti
  
  * [ibmq](org.quantil.qprov.core/target/generated-sources/org.quantil.qprov.ibmq) example api client used by the collector / IBMQ Provider
    * *generated* api client for IBMQ platform REST API
    * ***note:** this module will be generated at buildtime!*

* [API](org.quantil.qprov.api/)
  * provides access to the collected data

## manual build & run

* cleanup (double-check the paths before you run a `rm` command!)

    ```bash
    rm -rf ~/.m2/repository/org/quantil/qprov org.quantil.qprov.*/target
    ```

* build project & docker images

    ```bash
    mvn -U -T 1C clean install spring-boot:build-image
    ```

* run collector & api

    ```bash
    docker run --rm -it --name collector -p 7331:7331 docker.pkg.github.com/ust-quantil/qprov/collector:0.0.1-SNAPSHOT
    docker run --rm -it --name api -p 1337:1337 docker.pkg.github.com/ust-quantil/qprov/api:0.0.1-SNAPSHOT
    ```

    a profile can be set with the `SPRING_PROFILES_ACTIVE` env var. profiles are defined in the `../src/main/resource/application.yml` files of every module  
    > default profile uses h2 databases (for module testing and ci)
    > profile `development` uses postgresql database (for project testing)

    ```bash
    docker run --rm -it --name collector -p 7331:7331 -e "SPRING_PROFILES_ACTIVE=development" docker.pkg.github.com/ust-quantil/qprov/collector:0.0.1-SNAPSHOT
    docker run --rm -it --name api -p 1337:1337 -e "SPRING_PROFILES_ACTIVE=development" docker.pkg.github.com/ust-quantil/qprov/api:0.0.1-SNAPSHOT
    ```

> For development you can also use the provided [`devCleanBuild.sh`](devCleanBuild.sh)

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

## architecture (draft, outdated)

![QProv Architecture](.github/img/QProv_1.png "QProv Architecture")
