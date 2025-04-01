package com.org.service;

import com.org.models.Expense;
import org.apache.coyote.Response;

import java.util.List;

public interface ExpenseService {
    List<Expense> getAllExpenses(Long userId);
    Response getExpenseById(Long id);
    Response addExpense(List<Expense> expenses);
    Response updateExpense(Long id, Expense updatedExpense);
    void deleteExpense(Long id);
}