package com.hortonworks.hdf.tutorial.storm;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.storm.Config;
import org.apache.storm.StormSubmitter;
import org.apache.storm.kafka.bolt.KafkaBolt;
import org.apache.storm.kafka.bolt.selector.DefaultTopicSelector;
import org.apache.storm.kafka.spout.KafkaSpout;
import org.apache.storm.kafka.spout.KafkaSpoutConfig;
import org.apache.storm.kafka.spout.KafkaSpoutRetryExponentialBackoff;
import org.apache.storm.kafka.spout.KafkaSpoutStreams;
import org.apache.storm.kafka.spout.KafkaSpoutTupleBuilder;
import org.apache.storm.kafka.spout.KafkaSpoutTuplesBuilder;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.topology.base.BaseWindowedBolt.Count;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.apache.storm.kafka.spout.KafkaSpoutConfig.FirstPollOffsetStrategy.LATEST;

public class ReportingTopology {

    public static void main(String[] args) throws Exception {

        final String brokerConnectionStr = args[0];
        final String inputTopic = args[1];
        final String outputTopic = args[2];
        final Integer windowSize = Integer.parseInt(args[3]);
        final Integer slidingInterval = Integer.parseInt(args[4]);
        final String inputConsumerGroupId = ReportingTopology.class.getSimpleName();

        final TopologyBuilder topologyBuilder = new TopologyBuilder();

        final String streamId = "reporting";

        final KafkaSpout<String, String> kafkaInput = getInputKafkaSpout(brokerConnectionStr, inputTopic, inputConsumerGroupId, streamId);
        topologyBuilder.setSpout("kafka-input", kafkaInput);

        topologyBuilder.setBolt("calc-average", new AverageBolt(streamId)
                .withWindow(new Count(windowSize), new Count(slidingInterval)))
                .shuffleGrouping("kafka-input", streamId);

        final KafkaBolt<String, String> kafkaReport = getOutputKafkaBolt(brokerConnectionStr, outputTopic);
        topologyBuilder.setBolt("kafka-report", kafkaReport).shuffleGrouping("calc-average", streamId);

        final Config stormConf = new Config();
        StormSubmitter.submitTopology(ReportingTopology.class.getSimpleName(), stormConf, topologyBuilder.createTopology());

    }

    private static KafkaSpout<String, String> getInputKafkaSpout(final String brokerConnectionStr,
                                                                 final String inputTopic,
                                                                 final String inputConsumerGroupId,
                                                                 final String streamId) {

        final Fields outputFields = new Fields("topic", "partition", "offset", "key", "value");
        final KafkaSpoutStreams spoutStreams = new KafkaSpoutStreams.Builder(outputFields, streamId, new String[]{inputTopic}).build();

        final KafkaSpoutTupleBuilder<String,String> tupleBuilder = new KafkaSpoutTupleBuilder<String, String>(inputTopic) {
            @Override
            public List<Object> buildTuple(ConsumerRecord<String, String> consumerRecord) {
                return new Values(consumerRecord.topic(),
                        consumerRecord.partition(),
                        consumerRecord.offset(),
                        consumerRecord.key(),
                        consumerRecord.value());
            }
        };

        final KafkaSpoutTuplesBuilder<String, String> tuplesBuilder = new KafkaSpoutTuplesBuilder.Builder<>(tupleBuilder).build();

        final KafkaSpoutRetryExponentialBackoff retryService = new KafkaSpoutRetryExponentialBackoff(
                KafkaSpoutRetryExponentialBackoff.TimeInterval.microSeconds(500),
                KafkaSpoutRetryExponentialBackoff.TimeInterval.milliSeconds(2), Integer.MAX_VALUE,
                KafkaSpoutRetryExponentialBackoff.TimeInterval.seconds(10));

        final Map<String, Object> consumerConfig = new HashMap<>();
        consumerConfig.put(KafkaSpoutConfig.Consumer.BOOTSTRAP_SERVERS, brokerConnectionStr);
        consumerConfig.put(KafkaSpoutConfig.Consumer.GROUP_ID, inputConsumerGroupId);
        consumerConfig.put(KafkaSpoutConfig.Consumer.KEY_DESERIALIZER, "org.apache.kafka.common.serialization.StringDeserializer");
        consumerConfig.put(KafkaSpoutConfig.Consumer.VALUE_DESERIALIZER, "org.apache.kafka.common.serialization.StringDeserializer");


        final KafkaSpoutConfig<String,String> kafkaInputConfig =
                new KafkaSpoutConfig.Builder<>(consumerConfig, spoutStreams, tuplesBuilder, retryService)
                .setOffsetCommitPeriodMs(10_000)
                .setFirstPollOffsetStrategy(LATEST)
                .setMaxUncommittedOffsets(250)
                .build();

        return new KafkaSpout<>(kafkaInputConfig);
    }

    private static KafkaBolt<String, String> getOutputKafkaBolt(final String brokerConnectionStr, final String outputTopic) {
        final Properties producerConfig = new Properties();
        producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerConnectionStr);
        producerConfig.put(ProducerConfig.ACKS_CONFIG, "1");
        producerConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        producerConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");

        return new KafkaBolt<String, String>()
                .withTopicSelector(new DefaultTopicSelector(outputTopic))
                .withProducerProperties(producerConfig);
    }

}
