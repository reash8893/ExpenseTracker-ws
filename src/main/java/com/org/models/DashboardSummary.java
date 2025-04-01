package com.org.models;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class DashboardSummary {
    // Overall Summary
    private BigDecimal totalExpenses;
    private BigDecimal monthlyExpenses;
    private BigDecimal weeklyExpenses;
    private BigDecimal dailyAverage;
    private BigDecimal monthlyAverage;
    
    // Trend Analysis
    private BigDecimal monthOverMonthChange; // Percentage
    private BigDecimal yearOverYearChange;   // Percentage
    
    // Category-wise Analysis
    private Map<String, BigDecimal> categoryWiseExpenses;
    private Map<String, Double> categoryWisePercentages;
    private String topSpendingCategory;
    
    // Monthly Breakdown
    private Map<String, BigDecimal> monthlyBreakdown; // Last 12 months
    
    // Budget Analysis
    private BigDecimal totalBudget;
    private BigDecimal remainingBudget;
    private Double budgetUtilizationPercentage;
    
    // Recent Activity
    private List<Expense> recentTransactions; // Last 5 transactions
    
    // Time-based Insights
    private Map<String, BigDecimal> weekdayWiseExpenses;
    private String highestSpendingDay;
    
    // Expense Patterns
    private Map<String, BigDecimal> recurringExpenses;
    private List<String> unusualSpending; // Categories with significant deviation from average
    
    // Savings Insights
    private BigDecimal potentialSavings;
    private List<String> savingsSuggestions;
} 