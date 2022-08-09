# we extend the flink docker image with our own jar file
FROM flink:1.15

#COPY /target/flink-s3-fs-presto-1.15.1.jar ./plugins/s3-fs-presto/

COPY /target/quickstart-0.1.jar /vincent/workdir/flink/quickstart.jar

RUN mkdir ./plugins/s3-fs-presto
RUN cp ./opt/flink-s3-fs-presto-1.15.1.jar ./plugins/s3-fs-presto/flink-s3-fs-presto-1.15.1.jar

RUN echo $(ls /vincent/workdir/flink)
RUN echo $(ls /opt/flink/plugins)
