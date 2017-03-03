package com.hortonworks.hdf.tutorial.storm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseWindowedBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.apache.storm.windowing.TupleWindow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AverageBolt extends BaseWindowedBolt {

    private final String streamId;
    private OutputCollector collector;
    private ObjectMapper mapper;

    public AverageBolt(String streamId) {
        this.streamId = streamId;
    }

    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        this.collector = collector;
        this.mapper = new ObjectMapper();
    }


    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declareStream(streamId, new Fields("key", "message"));
    }

    @Override
    public void execute(TupleWindow inputWindow) {
        final List<Tuple> inputTuples = inputWindow.get();
        final Map<String, List<Integer>> groupedValues = new HashMap<>();
        for(Tuple tuple: inputTuples) {
            final String key = tuple.getStringByField("key");
            final Integer value = Integer.parseInt(tuple.getStringByField("value"));
            final List<Integer> values = groupedValues.computeIfAbsent(key, k -> new ArrayList<>());
            values.add(value);
        }

        final Map<String, Double> averages = groupedValues.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().stream().mapToInt(Integer::intValue).average()))
                .entrySet().stream().filter(entry -> entry.getValue().isPresent())
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getAsDouble()));

        try {
            final String averageJson = mapper.writeValueAsString(averages);
            collector.emit(streamId, new Values("key", averageJson));

            inputTuples.forEach(t -> collector.ack(t));
        } catch (JsonProcessingException e) {
            collector.reportError(e);
            inputTuples.forEach(t -> collector.fail(t));
        }


    }
}
