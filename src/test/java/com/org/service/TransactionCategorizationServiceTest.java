package com.org.service;

import com.org.model.BankTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransactionCategorizationServiceTest {

    private TransactionCategorizationService categorizationService;

    @BeforeEach
    void setUp() {
        categorizationService = new TransactionCategorizationService();
    }

    @Test
    void testCategorizeTransactions() {
        List<BankTransaction> transactions = createSampleTransactions();
        
        // Categorize all transactions
        List<BankTransaction> categorizedTransactions = categorizationService.categorizeTransactions(transactions);
        
        // Check that all transactions have categories
        for (BankTransaction transaction : categorizedTransactions) {
            assertNotNull(transaction.getCategory(), "Transaction should have a category");
            assertNotNull(transaction.getMatchedCategories(), "Transaction should have matched categories");
            assertFalse(transaction.getMatchedCategories().isEmpty(), "Transaction should have at least one matched category");
        }
        
        // Check specific categorizations for representative transactions
        assertEquals("Food & Dining", findTransactionByNarration(categorizedTransactions, "UPI-ZOMATO").getCategory(), 
                     "Zomato transaction should be categorized as Food & Dining");
        
        assertEquals("Entertainment", findTransactionByNarration(categorizedTransactions, "UPI-BOOKMYSHOW").getCategory(), 
                     "BookMyShow transaction should be categorized as Entertainment");
        
        assertEquals("Shopping", findTransactionByNarration(categorizedTransactions, "UPI-AMAZON").getCategory(), 
                     "Amazon transaction should be categorized as Shopping");
        
        assertEquals("Utilities", findTransactionByNarration(categorizedTransactions, "AIRTEL BILL PAYMENT").getCategory(), 
                     "Airtel bill payment should be categorized as Utilities");
        
        assertEquals("Transportation", findTransactionByNarration(categorizedTransactions, "UPI-UBER").getCategory(), 
                     "Uber transaction should be categorized as Transportation");
        
        assertEquals("Income", findTransactionByNarration(categorizedTransactions, "SALARY CREDIT").getCategory(), 
                     "Salary transaction should be categorized as Income");
    }
    
    @Test
    void testGetAllCategories() {
        List<String> categories = categorizationService.getAllCategories();
        
        // Check that we have all expected categories
        assertTrue(categories.contains("Food & Dining"), "Should contain Food & Dining category");
        assertTrue(categories.contains("Shopping"), "Should contain Shopping category");
        assertTrue(categories.contains("Entertainment"), "Should contain Entertainment category");
        assertTrue(categories.contains("Utilities"), "Should contain Utilities category");
        assertTrue(categories.contains("Transportation"), "Should contain Transportation category");
        assertTrue(categories.contains("Income"), "Should contain Income category");
        
        // Check total number of categories (will need updating if categories are added/removed)
        assertEquals(18, categories.size(), "Should have the expected number of categories");
    }
    
    /**
     * Create sample transactions for testing
     */
    private List<BankTransaction> createSampleTransactions() {
        List<BankTransaction> transactions = new ArrayList<>();
        
        // Food & Dining transactions
        transactions.add(createTransaction("01/01/2023", "UPI-ZOMATO-1234567890@OKBIZ", "200.00", null));
        transactions.add(createTransaction("02/01/2023", "UPI-SWIGGY-9876543210@YBL", "350.50", null));
        transactions.add(createTransaction("03/01/2023", "UPI-DOMINOS-PIZZA-8765432109@ORBIT", "500.00", null));
        
        // Entertainment transactions
        transactions.add(createTransaction("04/01/2023", "UPI-BOOKMYSHOW-7654321098@UPAY", "1000.00", null));
        transactions.add(createTransaction("05/01/2023", "UPI-PVR CINEMAS-6543210987@PBB", "450.00", null));
        transactions.add(createTransaction("06/01/2023", "NETFLIX SUBSCRIPTION", "649.00", null));
        
        // Shopping transactions
        transactions.add(createTransaction("07/01/2023", "UPI-AMAZON-5432109876@APL", "2500.00", null));
        transactions.add(createTransaction("08/01/2023", "UPI-FLIPKART-4321098765@ICICI", "1800.00", null));
        transactions.add(createTransaction("09/01/2023", "UPI-MYNTRA-3210987654@YESB", "3000.00", null));
        
        // Utilities transactions
        transactions.add(createTransaction("10/01/2023", "AIRTEL BILL PAYMENT", "999.00", null));
        transactions.add(createTransaction("11/01/2023", "ELECTRICITY BILL PAYMENT", "1500.00", null));
        transactions.add(createTransaction("12/01/2023", "JIO RECHARGE", "555.00", null));
        
        // Transportation transactions
        transactions.add(createTransaction("13/01/2023", "UPI-UBER-2109876543@OKICICI", "350.00", null));
        transactions.add(createTransaction("14/01/2023", "UPI-OLA-1098765432@UPI", "420.00", null));
        transactions.add(createTransaction("15/01/2023", "PETROL PUMP PAYMENT", "2000.00", null));
        
        // Income transactions
        transactions.add(createTransaction("25/01/2023", "SALARY CREDIT", null, "50000.00"));
        transactions.add(createTransaction("26/01/2023", "INTEREST CREDIT", null, "1200.00"));
        transactions.add(createTransaction("27/01/2023", "REFUND FROM AMAZON", null, "500.00"));
        
        return transactions;
    }
    
    /**
     * Helper method to create a transaction
     */
    private BankTransaction createTransaction(String date, String narration, String withdrawalAmt, String depositAmt) {
        return BankTransaction.builder()
            .date(date)
            .narration(narration)
            .chqRefNo("12345678")
            .valueDate(date)
            .withdrawalAmt(withdrawalAmt != null ? new BigDecimal(withdrawalAmt) : null)
            .depositAmt(depositAmt != null ? new BigDecimal(depositAmt) : null)
            .closingBalance(new BigDecimal("10000.00"))
            .flagged(false)
            .source("test")
            .build();
    }
    
    /**
     * Helper method to find a transaction by narration containing
     */
    private BankTransaction findTransactionByNarration(List<BankTransaction> transactions, String narrationPart) {
        return transactions.stream()
            .filter(transaction -> transaction.getNarration().contains(narrationPart))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Transaction with narration containing '" + narrationPart + "' not found"));
    }
} 