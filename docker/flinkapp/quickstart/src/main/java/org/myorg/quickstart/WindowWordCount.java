package org.myorg.quickstart;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExactlyOnceOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;
import org.postgresql.xa.PGXADataSource;


public class WindowWordCount {

    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(60000); // 1 minute for testing the exactly once behaviour

        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers("yalii-cluster-kafka-bootstrap:9092")
                .setTopics("vincent-input")
                .setGroupId("my-group")
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        DataStream<String> dataStream = env
                .fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source")
                .flatMap(new Splitter())
                .keyBy(value -> value)
                .flatMap(new Counter())
                .flatMap(new Flattener());

        dataStream.print();

        KafkaSink<String> sink = KafkaSink.<String>builder()
                .setBootstrapServers("yalii-cluster-kafka-bootstrap:9092")
                .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                        .setTopic("vincent-output")
                        .setValueSerializationSchema(new SimpleStringSchema())
                        .build()
                )
                .setDeliverGuarantee(DeliveryGuarantee.EXACTLY_ONCE)
                .setTransactionalIdPrefix("vincent")
                .build();

        dataStream.sinkTo(sink);

//        postgresql
        env.fromElements("hey", "dit", "is", "een", "test").addSink(
                JdbcSink.sink(
                        "insert into flinklink (\"data\") values (?)",
                        (statement, book) -> {
                            statement.setString(1, (String) book);
                        },
                        JdbcExecutionOptions.builder()
                                .withBatchSize(1000)
                                .withBatchIntervalMs(2000)
                                .withMaxRetries(3)
                                .build(),
                        new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
                                .withUrl("jdbc:postgresql://kapernikov-pg-cluster:5432/postgres")
                                .withDriverName("org.postgresql.Driver")
                                .withUsername("testuser")
                                .withPassword("bSWQH2o0vHX8LyFy8blakVtZ6TkngDV8xrXNFqmbXTXG1c73oTJHy9xPkk3oMELu")
                                .build()
                )).uid("fink application").name("fink application");


//        env.fromElements("hey", "dit", "is", "een", "test").addSink(JdbcSink.exactlyOnceSink(
//           "insert into \"flink-postgres\" (\"data\") values (?)",
//                (statement, item) -> {
//                     statement.setString(1, item);
//                },
//                JdbcExecutionOptions.builder()
//                        .withMaxRetries(0) // needed for exactly once, otherwise duplicates can arise
//                        .build(),
//                JdbcExactlyOnceOptions.builder()
//                        .withTransactionPerConnection(true) // needed in postgres
//                        .build(),
//                () -> {
//                    PGXADataSource xaDataSource = new org.postgresql.xa.PGXADataSource();
//                    xaDataSource.setUrl("jdbc:postgresql://kapernikov-pg-cluster-1:5432/postgres");
//                    xaDataSource.setUser("testuser");
//                    xaDataSource.setPassword("FaHlwYPjNXwVaqZh98KUgk8lSYj26INZVoS4lJ0ll7ppkqKGiFXgCNaZRyJY2BvY");
//                    return xaDataSource;
//                }
//        ));

        env.execute("Window WordCount");
    }

    public static class Splitter implements FlatMapFunction<String, String> {
        @Override
        public void flatMap(String sentence, Collector<String> out) throws Exception {
            for (String word: sentence.split(" ")) {
                out.collect(word);
            }
        }
    }

    public static class Counter extends RichFlatMapFunction<String, Tuple2<String, Integer>> {

        private transient ValueState<Tuple2<String, Integer>> sum;

        @Override
        public void flatMap(String word, Collector<Tuple2<String, Integer>> out) throws Exception {
            Tuple2<String, Integer> current = sum.value();
            if(current.f0 == null){
                // new word
                sum.update(new Tuple2<>(word, 1));
            }
            else{
                // already existed
                current.f1 += 1;
                sum.update(current);
            }
            out.collect(sum.value());
        }

        @Override
        public void open(Configuration config) {
            ValueStateDescriptor<Tuple2<String, Integer>> descriptor =
                    new ValueStateDescriptor<>(
                            "count", // the state name
                            TypeInformation.of(new TypeHint<Tuple2<String, Integer>>() {}), // type information
                            Tuple2.of(null, 0)); // default value of the state, if nothing was set
            sum = getRuntimeContext().getState(descriptor);
        }
    }

    public static class Flattener implements FlatMapFunction<Tuple2<String, Integer>, String> {
        @Override
        public void flatMap(Tuple2<String, Integer> sentence, Collector<String> out) throws Exception {
            out.collect(sentence.toString());
        }
    }

}
