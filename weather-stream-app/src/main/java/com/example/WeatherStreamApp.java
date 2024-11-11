package com.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;

import java.util.Iterator;

public class WeatherStreamApp {
    public static void main(String[] args) {
        // Create a StreamsBuilder instance
        StreamsBuilder builder = new StreamsBuilder();

        // Define input and output Kafka topics
        String inputTopic = "weather-input";
        String outputTopic = "weather-output";

        // Stream from input topic
        KStream<String, String> weatherStream = builder.stream(inputTopic);

        // ObjectMapper for JSON parsing
        ObjectMapper objectMapper = new ObjectMapper();

        // Process and transform the JSON data
        weatherStream.mapValues(value -> {
            try {
                // Parse the input JSON
                JsonNode rootNode = objectMapper.readTree(value);
                JsonNode currentConditions = rootNode.path("currentSummitConditions").path("Imperial").path("CGVPTemperature");

                // Prepare output format
                StringBuilder processedData = new StringBuilder();
                processedData.append("Temperature Details:\\n");

                // Iterate over temperature data
                Iterator<JsonNode> elements = currentConditions.elements();
                while (elements.hasNext()) {
                    JsonNode node = elements.next();
                    String site = node.get(0).asText();
                    String temperature = node.get(1).asText();
                    String annotation = node.get(2).asText();
                    String style = node.get(3).asText();
                    String tooltip = node.get(4).asText();

                    processedData.append(String.format("Site: %s, Temperature: %s, Style: %s, Tooltip: %s\\n",
                            site, temperature, style, tooltip));
                }

                return processedData.toString();
            } catch (Exception e) {
                e.printStackTrace();
                return "Error processing data";
            }
        }).to(outputTopic, Produced.with(Serdes.String(), Serdes.String()));

        // Build the topology
        Topology topology = builder.build();

        // Set the properties for Kafka Streams
        java.util.Properties props = new java.util.Properties();
        props.put("application.id", "weather-stream-app");
        props.put("bootstrap.servers", "localhost:9092");
        props.put("default.key.serde", Serdes.String().getClass());
        props.put("default.value.serde", Serdes.String().getClass());

        // Start the Kafka Streams application
        KafkaStreams streams = new KafkaStreams(topology, props);
        streams.start();

        // Add shutdown hook to close the streams on exit
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }
}
