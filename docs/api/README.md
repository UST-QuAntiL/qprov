# Provenance API

The REST API of the QProv system consists of three major parts which will be discussed in the following and can also be
seen in the Swagger UI: <http://localhost:5020/qprov/swagger-ui>

### Provider

All data that is automatically gathered by the [provenance collector](../collector) can be retrieved under
the `provider` tag in the Swagger UI.
Therefore, data about all available QPUs, as well as their current characteristics, and further aggregated data, such as
calibration matrices is accessible.

### Provenance Documents

Under the `provenance-document` tag, all available PROV documents can be retrieved in XML format or as a graph
representation (JPEG).
The contained activities, entities, and agents can be modified and new elements can be added to documents.
Furthermore, new PROV documents can be created over the API.
For this, there is the possibility to create empty documents to fill them during collection, as well as by uploading a
PROV document, e.g., as XML or PROVN.
Finally, PROV documents can also be created by instantiation provenance templates, which will be discussed below.

### Provenance Templates

Under the `provenance-template` tag, available provenance templates can be handled and new templates can be created.
A [provenance template](https://lucmoreau.wordpress.com/2015/08/03/provtoolbox-tutorial-4-templates-for-provenance-part-2/)
defines the structure of a provenance document and provides placeholders for parameters that have to be passed to
instantiate the template and create a PROV document from it.
Therefore, new templates can be upload and exported, but additionally, it is possible to retrieve all parameters that
are required to instantiate a template.
Finally, there is an endpoint to perform the instantiation by passing in the parameters.
