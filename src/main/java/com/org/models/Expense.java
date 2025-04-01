package com.org.models;

import lombok.*;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Expense {
    private Long expenseId;
    private String title;
    private BigDecimal  amount;
    private Date expenseDate;
    private Long userId;     // Instead of User entity, just store userId
    private Long categoryId; // Instead of Category entity, just store categoryId
    private String description;
    private String userName;


    public Expense(Long expenseId, String title, BigDecimal amount, Date expenseDate,
                   String categoryDescription, Long userId, String userName) {
        this.expenseId = expenseId;
        this.title = title;
        this.amount = amount;
        this.expenseDate = expenseDate;
        this.description = categoryDescription;
        this.userId = userId;
        this.userName = userName;
    }
}