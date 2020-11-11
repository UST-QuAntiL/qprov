FROM openjdk:16-alpine as builder
COPY . /tmp/qprov
WORKDIR /tmp/qprov
RUN apk update && apk add unzip && apk add maven
RUN mvn package -DskipTests && mkdir /build && unzip /tmp/qprov/org.quantil.qprov.web/target/org.quantil.qprov.web.war -d /build/qprov

FROM tomcat:9-jdk8
LABEL maintainer = "Benjamin Weder <benjamin.weder@iaas.uni-stuttgart.de>"

RUN rm -rf ${CATALINA_HOME}/webapps/*
COPY --from=builder /build/qprov ${CATALINA_HOME}/webapps/qprov

EXPOSE 8080

CMD ["catalina.sh", "run"]
