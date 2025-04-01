package com.org.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

/**
 * Model class representing a bank transaction from a statement
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankTransaction {
    private String date;
    private String narration;
    private String chqRefNo;
    private String valueDate;
    private BigDecimal withdrawalAmt;
    private BigDecimal depositAmt;
    private BigDecimal closingBalance;
    private String category;
    private List<String> matchedCategories;
    private Boolean flagged;
    private String source; // Indicates which parser extracted this transaction (ai_parser/rule_based_parser)
} 