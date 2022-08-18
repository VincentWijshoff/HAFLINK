FROM ubuntu:20.04

ENV JAVA_VERSION=11

RUN \
    apt-get update && \
    apt-get -y install openjdk-${JAVA_VERSION}-jdk python-is-python3 maven

ADD quickstart /quickstart
WORKDIR /quickstart
RUN mvn install

# we extend the flink docker image with our own jar file
FROM flink:1.15

COPY --from=0 /quickstart/target/quickstart-0.1.jar /vincent/workdir/flink/quickstart.jar
COPY --from=0 /quickstart/.env /opt/flink/.env

RUN mkdir ./plugins/s3-fs-presto
RUN cp ./opt/flink-s3-fs-presto-1.15.1.jar ./plugins/s3-fs-presto/flink-s3-fs-presto-1.15.1.jar

RUN mkdir /cache && chmod og+rwX /cache