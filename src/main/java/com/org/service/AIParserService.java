package com.org.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.model.BankTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for interacting with the AI Parser microservice.
 * This service forwards PDF files to the Python-based AI parser
 * and processes the results.
 */
@Service
public class AIParserService {
    private static final Logger logger = LoggerFactory.getLogger(AIParserService.class);
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${ai.parser.url:http://localhost:5000/api/ai-parser/parse-pdf}")
    private String aiParserUrl;
    
    @Value("${ai.parser.enabled:true}")
    private boolean aiParserEnabled;
    
    @Autowired
    public AIParserService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        logger.info("AIParserService initialized with endpoint: {}", aiParserUrl);
    }
    
    /**
     * Parse a bank statement PDF using the AI parser service
     * @param file The PDF file to parse
     * @return List of extracted transactions
     */
    public List<BankTransaction> parseWithAI(MultipartFile file) throws IOException {
        if (!aiParserEnabled) {
            logger.info("AI parser is disabled. Skipping AI parsing.");
            return new ArrayList<>();
        }
        
        logger.info("Sending PDF to AI parser service: {}", aiParserUrl);
        
        try {
            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            // Prepare the parts (file)
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", file.getResource());
            
            // Create the request entity
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            // Send request to AI parser service
            ResponseEntity<String> response = restTemplate.postForEntity(
                    aiParserUrl, 
                    requestEntity, 
                    String.class);
            
            // Process response
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                logger.debug("AI parser response: {}", response.getBody());
                
                try {
                    // Parse JSON response to list of transactions
                    List<BankTransaction> transactions = objectMapper.readValue(
                            response.getBody(), 
                            new TypeReference<List<BankTransaction>>() {});
                    
                    // Mark transactions as coming from AI parser
                    for (BankTransaction transaction : transactions) {
                        transaction.setSource("ai_parser");
                    }
                    
                    logger.info("Successfully extracted {} transactions using AI parser", 
                            transactions.size());
                    
                    return transactions;
                } catch (Exception e) {
                    logger.error("Error deserializing AI parser response", e);
                    return new ArrayList<>();
                }
            } else {
                logger.error("Error response from AI parser: {}", response.getStatusCode());
                return new ArrayList<>();
            }
            
        } catch (ResourceAccessException e) {
            logger.error("Could not connect to AI parser service at {}. Make sure the service is running.", 
                    aiParserUrl, e);
            return new ArrayList<>();
        } catch (Exception e) {
            logger.error("Error communicating with AI parser service", e);
            // If AI parser fails, return empty list
            return new ArrayList<>();
        }
    }
} 