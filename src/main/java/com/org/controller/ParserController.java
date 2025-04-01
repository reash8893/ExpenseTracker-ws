package com.org.controller;

import com.org.model.BankTransaction;
import com.org.service.AIParserService;
import com.org.service.PDFParserService;
import com.org.service.TransactionCategorizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/expenseTracker/parser")
public class ParserController {
    private static final Logger logger = LoggerFactory.getLogger(ParserController.class);

    private final PDFParserService pdfParserService;
    private final AIParserService aiParserService;
    private final TransactionCategorizationService categorizationService;

    @Autowired
    public ParserController(PDFParserService pdfParserService, 
                           AIParserService aiParserService,
                           TransactionCategorizationService categorizationService) {
        this.pdfParserService = pdfParserService;
        this.aiParserService = aiParserService;
        this.categorizationService = categorizationService;
    }

    @PostMapping("/parse-pdf")
    public ResponseEntity<List<BankTransaction>> parsePDF(@RequestParam("file") MultipartFile file,
                                                         @RequestParam(value = "categorize", defaultValue = "true") boolean categorize) {
        try {
            logger.info("Received PDF file for parsing: {}", file.getOriginalFilename());
            List<BankTransaction> transactions = pdfParserService.parseBankStatement(file);
            
            // Categorize transactions if requested
            System.out.println("_______________________Categorizing transactions: " + transactions);
            if (categorize) {
                logger.info("Categorizing {} transactions", transactions.size());
                transactions = categorizationService.categorizeTransactions(transactions);
            }
            
            return ResponseEntity.ok(transactions);
        } catch (IOException e) {
            logger.error("Error parsing PDF", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/parse-pdf-ai")
    public ResponseEntity<?> parsePDFWithAI(@RequestParam("file") MultipartFile file,
                                           @RequestParam(value = "categorize", defaultValue = "true") boolean categorize) {
        try {
            logger.info("Received PDF file for AI parsing: {}", file.getOriginalFilename());
            
            // First try with AI parser
            List<BankTransaction> transactions = aiParserService.parseWithAI(file);
            boolean usedAI = true;
            
            // If AI parser fails or returns empty results, fall back to rule-based parser
            if (transactions == null || transactions.isEmpty()) {
                logger.info("AI parser returned no results, falling back to rule-based parser");
                transactions = pdfParserService.parseBankStatement(file);
                usedAI = false;
            }
            
            // Categorize transactions if requested
            if (categorize && transactions != null && !transactions.isEmpty()) {
                logger.info("Categorizing {} transactions", transactions.size());
                transactions = categorizationService.categorizeTransactions(transactions);
            }
            
            // Create a response with metadata about which parser was used
            Map<String, Object> response = new HashMap<>();
            response.put("transactions", transactions);
            response.put("parserUsed", usedAI ? "ai_parser" : "rule_based_parser");
            response.put("transactionCount", transactions.size());
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            logger.error("Error parsing PDF with AI", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
} 