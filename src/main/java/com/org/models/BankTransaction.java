package com.org.models;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BankTransaction {
    private LocalDate date;
    private LocalDate valueDate;
    private String description;
    private String referenceNumber;
    private BigDecimal withdrawalAmount;
    private BigDecimal depositAmount;
    private BigDecimal closingBalance;
    private String category; // Will be determined by the categorization logic
}