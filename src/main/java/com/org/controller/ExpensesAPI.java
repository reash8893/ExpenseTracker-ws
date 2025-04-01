package com.org.controller;



import com.org.models.Expense;
import com.org.service.ExpenseService;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/expenseTracker/expenses")
public class ExpensesAPI {

    private final ExpenseService expenseService;

    @Autowired
    public ExpensesAPI(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }


    // Get all expenses of user
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Expense>> getAllExpenses(@PathVariable Long userId) {
        System.out.println("_____________________________controller class ");
        return ResponseEntity.ok(expenseService.getAllExpenses(userId));
    }

    // Get expense by ID
    @GetMapping("/{id}")
    public ResponseEntity<Response> getExpenseById(@PathVariable Long id) {
        return ResponseEntity.ok(expenseService.getExpenseById(id));
    }

    //Add a new expense
    @PostMapping
    public ResponseEntity<Response> addExpense(@RequestBody Expense expense) {
        return ResponseEntity.ok(expenseService.addExpense(List.of(expense)));
    }

    //Update an expense
    @PutMapping("/{id}")
    public ResponseEntity<Response> updateExpense(@PathVariable Long id, @RequestBody Expense expense) {
        return ResponseEntity.ok(expenseService.updateExpense(id, expense));
    }

    //Delete an expense
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteExpense(@PathVariable Long id) {
        expenseService.deleteExpense(id);
        return ResponseEntity.ok("Expense deleted successfully");
    }
}
