package org.myorg.quickstart;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.util.Collector;

public class WindowWordCount {

    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(5000);
        //env.getCheckpointConfig().setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);


        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers("yalii-cluster-kafka-bootstrap:9092")
                .setTopics("vincent-input")
                .setGroupId("my-group")
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        DataStream<Tuple2<String, Integer>> dataStream = env
                .fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source")
                .flatMap(new Splitter())
                .keyBy(value -> value)
                .process(new StatefulReduceFunc());

        dataStream.print();

        KafkaSink<String> sink = KafkaSink.<String>builder()
                .setBootstrapServers("yalii-cluster-kafka-bootstrap:9092")
                .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                        .setTopic("vincent-output")
                        .setValueSerializationSchema(new SimpleStringSchema())
                        .build()
                )
                .setDeliverGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
                .build();

        dataStream.flatMap(new Flattener()).sinkTo(sink);

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

    public static class Flattener implements FlatMapFunction<Tuple2<String, Integer>, String> {
        @Override
        public void flatMap(Tuple2<String, Integer> sentence, Collector<String> out) throws Exception {
            out.collect(sentence.toString());
        }
    }

    private static class StatefulReduceFunc extends KeyedProcessFunction<String, String, Tuple2<String, Integer>> {

        private transient ValueState<Integer> count;

        public void processElement(String s, Context context, Collector<Tuple2<String, Integer>> collector) throws Exception {
            int currentCnt = count.value() == null ? 1 : count.value() + 1;
            count.update(currentCnt);
            collector.collect(new Tuple2<String, Integer>(s, currentCnt));
        }

        public void open(Configuration parameters){
            ValueStateDescriptor<Integer> valueStateDescriptor = new ValueStateDescriptor<>("count", Integer.class);
            count = getRuntimeContext().getState(valueStateDescriptor);
        }

    }

}
