package com.hzgc.cluster.peoman.worker.service;

import com.hzgc.common.collect.bean.FaceObject;
import com.hzgc.common.util.json.JacksonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Properties;

@Slf4j
@Component
public class FaceConsumer implements Runnable{
    @Autowired
    @SuppressWarnings("unused")
    private PeopleCompare peopleCompare;

    @Value("kafka.bootstrap.servers")
    @SuppressWarnings("unused")
    private String kafkaHost;

    @Value("kafka.face.topic")
    @SuppressWarnings("unused")
    private String faceTopic;

    @Value("kafka.inner.topic.polltime")
    @SuppressWarnings("unused")
    private Long pollTime;

    private KafkaConsumer<String, String> consumer;

    public void initFaceConsumer(String groupId) {
        Properties properties = new Properties();
        properties.put("group.id", groupId);
        properties.put("bootstrap.servers", kafkaHost);
        properties.put("key.deserializer", StringDeserializer.class.getName());
        properties.put("value.deserializer", StringDeserializer.class.getName());
        consumer = new KafkaConsumer<>(properties);
        consumer.subscribe(Collections.singletonList(faceTopic));
    }

    @Override
    public void run() {
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(pollTime);
            for (ConsumerRecord<String, String> record : records) {
                FaceObject faceObject = JacksonUtil.toObject(record.value(), FaceObject.class);
                peopleCompare.comparePeople(faceObject);
            }
        }
    }

}