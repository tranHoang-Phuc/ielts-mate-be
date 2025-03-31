package com.fptu.sep490.commonlibrary.kafka.doc.config;

import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Deserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;


public abstract class BaseKafkaListenerConfig<K, V> {

    private final Class<K> keyType;
    private final Class<V> valueType;
    private final KafkaProperties kafkaProperties;

    public BaseKafkaListenerConfig(Class<K> keyType, Class<V> valueType, KafkaProperties kafkaProperties) {
        this.valueType = valueType;
        this.keyType = keyType;
        this.kafkaProperties = kafkaProperties;
    }

    public abstract ConcurrentKafkaListenerContainerFactory<K, V> listenerContainerFactory();

    public ConcurrentKafkaListenerContainerFactory<K, V> kafkaListenerContainerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<K, V>();
        factory.setConsumerFactory(typeConsumerFactory(keyType, valueType));
        return factory;
    }

    private ConsumerFactory<K, V> typeConsumerFactory(Class<K> keyClazz, Class<V> valueClazz) {
        Map<String, Object> props = buildConsumerProperties();
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        Deserializer<K> keyDeserializer = new ErrorHandlingDeserializer<>(new JsonDeserializer<>(keyClazz));
        Deserializer<V> valueDeserializer = new ErrorHandlingDeserializer<>(new JsonDeserializer<>(valueClazz));

        return new DefaultKafkaConsumerFactory<>(props, keyDeserializer, valueDeserializer);
    }

    private static <T> JsonDeserializer<T> getJsonDeserializer(Class<T> clazz) {
        var jsonDeserializer = new JsonDeserializer<>(clazz);
        jsonDeserializer.addTrustedPackages("*");
        return jsonDeserializer;
    }

    private Map<String, Object> buildConsumerProperties() {
        return kafkaProperties.buildConsumerProperties();
    }


}
