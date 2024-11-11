package com.example;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DataFetcher {

    public static void main(String[] args) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        // Schedule the data fetching task at fixed intervals
        scheduler.scheduleAtFixedRate(DataFetcher::fetchDataAndProduceToKafka, 0, 5, TimeUnit.MINUTES); // Adjust the interval as needed
    }

    private static void fetchDataAndProduceToKafka() {
        String url = "https://xmountwashington.appspot.com/csc.php?size=large&callback=jQuery32107568063095697357_1731267296790&_=1731267296791";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("accept", "text/javascript, application/javascript, application/ecmascript, application/x-ecmascript, */*; q=0.01")
                .header("x-requested-with", "XMLHttpRequest")
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(DataFetcher::extractJsonFromResponse)
                .thenAccept(DataFetcher::produceToKafka)
                .join();
    }

    private static String extractJsonFromResponse(String response) {
        // Extract JSON payload from the response (remove the callback wrapper)
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        return (start != -1 && end != -1) ? response.substring(start, end + 1) : "{}";
    }

    private static void produceToKafka(String jsonData) {
        // Configure Kafka producer properties
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        KafkaProducer<String, String> producer = new KafkaProducer<>(props);
        String topic = "weather-input";

        // Send the JSON data to Kafka
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, jsonData);
        producer.send(record, (metadata, exception) -> {
            if (exception == null) {
                System.out.println("Record sent to topic " + topic + " with offset " + metadata.offset());
            } else {
                exception.printStackTrace();
            }
        });

        producer.close();
    }
}
