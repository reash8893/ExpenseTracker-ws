package com.org.service;

import com.org.entities.ExpenseEntity;
import com.org.models.Expense;
import com.org.repositories.ExpenseRepository;
import org.apache.coyote.Response;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class for Expense
 */
@Service
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepository;

    public ExpenseServiceImpl(ExpenseRepository expenseRepository){
        this.expenseRepository = expenseRepository;
    }

    @Override
    @Transactional
    public List<Expense> getAllExpenses(Long userId) {
        System.out.println("get all expenses service implement");
        List<Expense> expensesOfUser = expenseRepository.findExpensesWithUserAndCategory(userId);
        List<Expense> expenses = new ArrayList<>();
        System.out.println("Expense size - "+expensesOfUser.size());
        for (Expense exp: expensesOfUser){
            Expense expense = new Expense();
            expense.setExpenseId(exp.getExpenseId());
            expense.setTitle(exp.getTitle());
            expense.setAmount(exp.getAmount());
            expense.setExpenseDate(exp.getExpenseDate());
            expense.setCategoryId(exp.getCategoryId());
            expense.setDescription(exp.getDescription());
            expense.setUserId(exp.getUserId());  // Getting user ID
            expense.setUserName(exp.getUserName());
            System.out.println("Expenses records ------"+expense);
            expenses.add(expense);
        }

        return expenses;
    }

    @Override
    @Transactional
    public Response getExpenseById(Long id) {
        return null;
    }

    @Override
    public Response addExpense(List<Expense> expenses) {
        try {
            int recordCount = 0;
            for (Expense expense : expenses) {
                ExpenseEntity expenseEntity = new ExpenseEntity();
                expenseEntity.setTitle(expense.getTitle());
                expenseEntity.setAmount(expense.getAmount());
                expenseEntity.setExpenseDate(expense.getExpenseDate());
                expenseEntity.getUserEntity().setUserId(expense.getUserId());
                expenseEntity.getCategoryEntity().setCategoryId(expense.getCategoryId());
                expenseRepository.save(expenseEntity);
                recordCount++;
            }
            
            Response response = new Response();
            response.setMessage(recordCount + " Expense record(s) added");
            response.setStatus(200);
            return response;
        } catch (Exception e) {
            Response response = new Response();
            response.setMessage("Failed to add expenses: " + e.getMessage());
            response.setStatus(500);
            return response;
        }
    }

    @Override
    public Response updateExpense(Long id, Expense updatedExpense) {
        return null;
    }

    @Override
    public void deleteExpense(Long id) {
    }
}
