package com.org.service;

import com.org.model.BankTransaction;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for categorizing bank transactions based on narration, amount, and transaction type
 */
@Service
public class TransactionCategorizationService {
    
    private static final Logger logger = LoggerFactory.getLogger(TransactionCategorizationService.class);
    
    // Define main transaction categories
    public enum TransactionCategory {
        FOOD_AND_DINING("Food & Dining"),
        SHOPPING("Shopping"),
        ENTERTAINMENT("Entertainment"),
        BILLS_AND_UTILITIES("Bills & Utilities"),
        TRANSPORTATION("Transportation"),
        TRAVEL("Travel"),
        HEALTH_AND_PERSONAL_CARE("Health & Personal Care"),
        EDUCATION("Education"),
        GIFTS_AND_DONATIONS("Gifts & Donations"),
        INCOME("Income"),
        INVESTMENTS("Investments"),
        TRANSFERS("Transfers"),
        OTHER("Other");
        
        private final String displayName;
        
        TransactionCategory(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    // Category rules with keywords for matching
    private final Map<TransactionCategory, List<String>> categoryKeywords = new HashMap<>();
    
    // Patterns for specific transaction types
    private final Pattern upiPattern = Pattern.compile("UPI-", Pattern.CASE_INSENSITIVE);
    private final Pattern creditCardPattern = Pattern.compile("CREDIT\\s+CARD|CC\\s+PAYMENT", Pattern.CASE_INSENSITIVE);
    private final Pattern salaryPattern = Pattern.compile("SALARY|PAYROLL|WAGES", Pattern.CASE_INSENSITIVE);
    
    /**
     * Initialize the service with category keywords
     */
    public TransactionCategorizationService() {
        initializeCategoryKeywords();
    }
    
    /**
     * Set up the keywords for each category
     */
    private void initializeCategoryKeywords() {
        // Food & Dining keywords
        categoryKeywords.put(TransactionCategory.FOOD_AND_DINING, Arrays.asList(
            "restaurant", "cafe", "bakery", "food", "dining", "eat", "lunch", "dinner", 
            "breakfast", "coffee", "swiggy", "zomato", "snack", "grocery", "hotel", 
            "dhaba", "dunkin", "pizza", "mcdonald", "kfc", "burger", "dominoes", "starbucks",
            "cafeteria", "tea", "catering", "barbeque", "bbq", "kitchen", "chef", "groceries", 
            "provisions", "zepto", "bigbasket", "grofers", "blinkit", "instamart", "supermarket", "hypermarket", "bigbazar",
            "burger", "tacobell", "Amul", "Nestlé", "Britannia", "Parle", "Haldiram's", "McDonald's", "Domino's", "KFC", 
            "Burger King", "Pizza Hut", "Dabur", "Pepsi", "Coca-Cola", "Bikanervala", "Mother Dairy", 
            "Sunfeast", "Lays", "Maggi", "Bingo", "Amazon", "Flipkart", "Myntra", "Reliance Trends", "Ajio", "Big Bazaar", "DMart",
             "Tata Cliq", "Snapdeal", "Nykaa", "Decathlon", "Shoppers Stop", "Pantaloons", "Max Fashion", "Westside", 
             "Lifestyle", "Zara", "H&M", "Forever 21"

        ));
        
        // Shopping keywords
        categoryKeywords.put(TransactionCategory.SHOPPING, Arrays.asList(
            "shop", "store", "retail", "mall", "mart", "purchase", "buy", "market", 
            "amazon", "flipkart", "myntra", "ajio", "decathlon", "ikea", "lifestyle", 
            "clothing", "apparel", "fashion", "electronic", "gadget", "accessory", 
            "jewel", "furniture", "hardware", "homecenter", "supermarket", "hypermarket",
            "bigbasket", "grofers", "blinkit", "zepto", "instamart", "Amazon", "Flipkart", "Myntra",
             "Reliance Trends",  "Big Bazaar", "DMart", "Tata Cliq", "Snapdeal", "Nykaa", "Decathlon",
              "Shoppers Stop", "Pantaloons", "Max Fashion", "Westside", "Lifestyle", "Zara", "H&M", "Forever 21", 
              "Vero Moda"
        ));
        
        // Entertainment keywords
        categoryKeywords.put(TransactionCategory.ENTERTAINMENT, Arrays.asList(
            "movie", "cinema", "theatre", "concert", "show", "ticket", "game", "pvr", 
            "inox", "bookmyshow", "netflix", "amazon prime", "hotstar", "disney", 
            "spotify", "music", "event", "festival", "play", "park", "amusement", 
            "entertainment", "arcade", "bowling", "streaming", "subscription","Zee Entertainment", 
            "Sony Pictures Networks", "Star India", "Netflix", "Amazon Prime Video", "Disney+ Hotstar", 
            "ALTBalaji", "JioCinema", "Eros Now", "Voot", "MX Player", "PVR Cinemas", "INOX", "Carnival Cinemas",
             "BookMyShow", "Hungama", "Gaana", "Spotify", "Wynk Music", "Saavn"
        ));
        
        // Utilities keywords
        categoryKeywords.put(TransactionCategory.BILLS_AND_UTILITIES, Arrays.asList(
            "electric", "water", "gas", "sewage", "utility", "power", "energy", "bill", 
            "broadband", "internet", "wifi", "fiber", "connection", "telephone", "phone", 
            "mobile", "airtel", "jio", "vodafone", "bsnl", "tata", "reliance", "dish tv",
            "tatasky", "dth", "recharge", "Tata Power", "Reliance Power", "Adani Power", "BSES", 
            "Torrent Power", "NTPC", "HP Gas", "Indane", "Bharat Gas", "GAIL", "EESL", "CESC", "BEST", 
            "Mahanagar Gas", "IGL", "Tata Steel", "UltraTech Cement", "Ambuja Cement", "Berger Paints", "Asian Paints"
        ));
        
        // Transportation keywords
        categoryKeywords.put(TransactionCategory.TRANSPORTATION, Arrays.asList(
            "uber", "ola", "cab", "taxi", "auto", "rickshaw", "metro", "train", "bus", 
            "transport", "rapido", "petrol", "diesel", "fuel", "parking", "toll", "fare",
            "ride", "commute", "travel", "trip", "journey", "drive", "Ola", "Uber", "Rapido", "RedBus", "IRCTC",
            "VRL Logistics", "SRS Travels", "Blue Dart", "DHL", "FedEx", "Delhivery", "Shadowfax", "XpressBees", 
            "Indian Railways", "Tata Motors", "Mahindra", "Ashok Leyland", "Bajaj Auto", "Hero MotoCorp", "TVS Motors",
             "Maruti Suzuki"

        ));
        
        // Travel keywords
        categoryKeywords.put(TransactionCategory.TRAVEL, Arrays.asList(
            "flight", "airline", "hotel", "resort", "booking", "airbnb", "makemytrip", 
            "goibibo", "easemytrip", "cleartrip", "yatra", "oyo", "stay", "lodging", 
            "accommodation", "vacation", "holiday", "travel", "tourism", "tour", "trip",
            "cruise", "indigo", "spicejet", "air india", "vistara", "train", "ticket", "boat", "ferry", "airport", 
            "airport fees", "airport security", "airport parking", "airport terminal", "airport lounge", "airport lounge fees",
             "airport lounge access", "airport lounge pass", "airport lounge membership", "airport lounge access pass", 
             "airport lounge access membership",
            "air asia", "stays", "luggage","IRCTC"
        ));
        
        // Healthcare keywords
        categoryKeywords.put(TransactionCategory.HEALTH_AND_PERSONAL_CARE, Arrays.asList(
            "hospital", "clinic", "doctor", "medical", "health", "pharma", "medicine", 
            "healthcare", "dental", "pharmacy", "apollo", "medplus", "diagnostic", "lab", 
            "test", "scan", "consultation", "therapy", "treatment", "wellness", "fitness",
            "gym", "yoga", "meditation", "physiotherapy", "training", "Training Fees", "Apollo Hospitals", 
            "Fortis Healthcare", "Max Healthcare", "AIIMS", "Narayana Health", "Medanta", "Manipal Hospitals",
             "Cipla", "Sun Pharma", "Lupin", "Dr. Redd's", "Biocon", "Aurobindo Pharma", "Glenmark", "Torrent Pharma",
              "Patanjali", "Dabur", "Himalaya", "Colgate", "Oral-B", "medicines", "pharmacy", "meds", "spa", "salon", "beauty",
               "cosmetic", "makeup", "skincare", "haircut", "barbershop", "grooming", "personal care", 
               "hygiene", "parlour", "massage"

        ));
        
        // Education keywords
        categoryKeywords.put(TransactionCategory.EDUCATION, Arrays.asList(
            "school", "college", "university", "institute", "academy", "education", 
            "tuition", "course", "class", "workshop", "training", "tutorial", "lecture", 
            "seminar", "conference", "certification", "degree", "diploma", "learning",
            "byju", "unacademy", "coursera", "udemy", "upgrad", "books", "library", "study", "fees", "fee"
        ));
        
        
        // Gifts & Donations keywords
        categoryKeywords.put(TransactionCategory.GIFTS_AND_DONATIONS, Arrays.asList(
            "gift", "present", "donation", "charity", "donate", "fundraiser", "contribution", 
            "welfare", "ngo", "help", "support", "relief", "foundation", "trust",
            "birthday", "anniversary", "wedding", "celebration", "festival", "occasion"
        ));
        
        // Income keywords
        categoryKeywords.put(TransactionCategory.INCOME, Arrays.asList(
            "salary", "income", "payment received", "remuneration", "wage", "stipend", 
            "pension", "dividend", "interest received", "earned", "credit", "deposit", "cashback", 
            "refund", "return", "reimbursement", "settlement", "compensation"
        ));
        
        // Investments keywords
        categoryKeywords.put(TransactionCategory.INVESTMENTS, Arrays.asList(
            "investment", "mutual fund", "stock", "share", "equity", "demat", "zerodha", 
            "groww", "upstox", "etf", "bond", "fixed deposit", "fd", "ppf", "nps", 
            "retirement", "wealth", "capital", "portfolio", "sip", "asset", "security",
            "dividend", "profit", "Zerodha", "Upstox", "Groww", "Angel One", 
            "ICICI Direct", "HDFC Securities", "Motilal Oswal", "Kotak Securities", "5paisa", "Sharekhan",
             "Axis Direct", "SBI Securities", "Edelweiss", "Tata Capital", "Bajaj Finserv", "LIC Mutual Fund", 
             "SBI Mutual Fund", "HDFC Mutual Fund", "Nippon India Mutual Fund", "Aditya Birla Sun Life Mutual Fund"

        ));
        
        // Transfers keywords
        categoryKeywords.put(TransactionCategory.TRANSFERS, Arrays.asList(
            "transfer", "send money", "sent to", "received from", "imps", "rtgs", "neft", 
            "fund transfer", "account transfer", "bank transfer", "transaction", "payment", 
            "settle", "settlement", "repay", "repayment", "payback", "reimbursement",
            "credit", "debit", "paid to", "received from"
        ));
              
        
    }
    
    /**
     * Categorize a list of transactions
     * @param transactions List of transactions to categorize
     * @return The same list with categories populated
     */
    public List<BankTransaction> categorizeTransactions(List<BankTransaction> transactions) {
        for (BankTransaction transaction : transactions) {
            categorizeTransaction(transaction);
        }
        return transactions;
    }
    
    /**
     * Categorize a single transaction
     * @param transaction Transaction to categorize
     * @return The same transaction with category populated
     */
    public BankTransaction categorizeTransaction(BankTransaction transaction) {
        // Skip if already categorized
        if (transaction.getCategory() != null && !transaction.getCategory().isEmpty()) {
            return transaction;
        }
        
        // Get the narration and determine if it's a withdrawal or deposit
        String narration = transaction.getNarration().toUpperCase();
        boolean isWithdrawal = transaction.getWithdrawalAmt() != null && transaction.getWithdrawalAmt().compareTo(BigDecimal.ZERO) > 0;
        
        // Get matched categories
        List<TransactionCategory> matches = findMatchingCategories(narration, isWithdrawal);
        
        // Apply specialized rules
        TransactionCategory specialCategory = applySpecializedRules(transaction, narration, isWithdrawal);
        if (specialCategory != null) {
            matches.add(0, specialCategory); // Insert at the beginning as highest priority
        }
        
        // Store matched categories for reference
        if (!matches.isEmpty()) {
            transaction.setMatchedCategories(
                matches.stream()
                    .map(TransactionCategory::getDisplayName)
                    .collect(Collectors.toList())
            );
            
            // Set the primary category (first match)
            transaction.setCategory(matches.get(0).getDisplayName());
        } else {
            // Default to "Other" if no matches
            transaction.setCategory(TransactionCategory.OTHER.getDisplayName());
            transaction.setMatchedCategories(Collections.singletonList(TransactionCategory.OTHER.getDisplayName()));
        }
        
        return transaction;
    }
    
    /**
     * Find all categories that match the transaction narration
     */
    private List<TransactionCategory> findMatchingCategories(String narration, boolean isWithdrawal) {
        List<TransactionCategory> matches = new ArrayList<>();
        Map<TransactionCategory, Integer> categoryScores = new HashMap<>();
        
        // Process each category
        for (Map.Entry<TransactionCategory, List<String>> entry : categoryKeywords.entrySet()) {
            TransactionCategory category = entry.getKey();
            List<String> keywords = entry.getValue();
            
            // Skip Income category for withdrawals
            if (isWithdrawal && category == TransactionCategory.INCOME) {
                continue;
            }
            
            // Calculate match score for this category
            int score = 0;
            for (String keyword : keywords) {
                if (narration.contains(keyword.toUpperCase())) {
                    score += 1;
                    
                    // Bonus points for exact word matches (not part of other words)
                    if (narration.matches(".*\\b" + Pattern.quote(keyword.toUpperCase()) + "\\b.*")) {
                        score += 2;
                    }
                }
            }
            
            if (score > 0) {
                categoryScores.put(category, score);
            }
        }
        
        // Sort categories by score
        if (!categoryScores.isEmpty()) {
            matches = categoryScores.entrySet().stream()
                .sorted(Map.Entry.<TransactionCategory, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        }
        
        return matches;
    }
    
    /**
     * Apply specialized rules beyond keyword matching
     */
    private TransactionCategory applySpecializedRules(BankTransaction transaction, String narration, boolean isWithdrawal) {
        // UPI transaction rules
        if (upiPattern.matcher(narration).find()) {
            // If it contains clear food/grocery indicators, prioritize that
            if (narration.contains("ZOMATO") || narration.contains("SWIGGY") || 
                narration.contains("FOOD") || narration.contains("DINE") || 
                narration.contains("EAT") || narration.contains("RESTAURANT") ||
                narration.contains("BAKERY") || narration.contains("SNACK")) {
                return TransactionCategory.FOOD_AND_DINING;
            }
            
            // Online shopping
            if (narration.contains("AMAZON") || narration.contains("FLIPKART") || 
                narration.contains("MYNTRA") || narration.contains("AJIO")) {
                return TransactionCategory.SHOPPING;
            }
            
            // Entertainment
            if (narration.contains("MOVIE") || narration.contains("TICKET") || 
                narration.contains("INOX") || narration.contains("PVR") ||
                narration.contains("BOOKMYSHOW")) {
                return TransactionCategory.ENTERTAINMENT;
            }
            
            // Utilities/Bills
            if (narration.contains("BILL") || narration.contains("RECHARGE") || 
                narration.contains("AIRTEL") || narration.contains("JIO") ||
                narration.contains("ELECTRIC") || narration.contains("WATER") ||
                narration.contains("GAS")) {
                return TransactionCategory.BILLS_AND_UTILITIES;
            }
            
            // Transport
            if (narration.contains("UBER") || narration.contains("OLA") || 
                narration.contains("RAPIDO") || narration.contains("TAXI") ||
                narration.contains("METRO") || narration.contains("PETROL") ||
                narration.contains("FUEL")) {
                return TransactionCategory.TRANSPORTATION;
            }
            
            // Generic online shopping or small value transactions
            if (isWithdrawal && transaction.getWithdrawalAmt().compareTo(new BigDecimal("1000")) < 0) {
                return TransactionCategory.SHOPPING;
            }
        }
        
        // Salary/Income detection
        if (!isWithdrawal && salaryPattern.matcher(narration).find()) {
            return TransactionCategory.INCOME;
        }
        
        
        // More rules can be added here for specific transaction types
        
        return null; // No special rule matched
    }
    
    /**
     * Get all available categories
     * @return List of category names
     */
    public List<String> getAllCategories() {
        return Arrays.stream(TransactionCategory.values())
            .map(TransactionCategory::getDisplayName)
            .collect(Collectors.toList());
    }
} 