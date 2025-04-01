package com.org.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ApplicationConfig {

    /**
     * Configure the ObjectMapper for the application with appropriate serialization options
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // Configure serialization options
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        
        // Add custom serializers if needed
        SimpleModule module = new SimpleModule();
        // module.addSerializer(YourCustomClass.class, new YourCustomSerializer());
        objectMapper.registerModule(module);
        
        return objectMapper;
    }

    /**
     * Configure the RestTemplate with appropriate timeouts and error handling
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(60000); // 60 seconds
        factory.setReadTimeout(60000);    // 60 seconds
        
        return new RestTemplate(factory);
    }
} 