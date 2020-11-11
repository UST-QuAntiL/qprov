# Docs

## Developer information

[Here](dev) you can find developer information how to set-up IntelliJ for the development oft the QProv system.

TODO

## modules

* [core](org.quantil.qprov.core/)
  * provides common things like model, utils, ... to the other modules (`api` and `collector`)
  * generates the sources for the api clients of the collector

* [collector](org.quantil.qprov.collector/)
  * fetches data from cloud quantum resource providers like IBMQ or rigetti

* [API](org.quantil.qprov.api/)
  * provides access to the collected data
