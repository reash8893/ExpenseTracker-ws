package com.org.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/expenseTracker")
public class HealthCheckController {

    @GetMapping("/health")
    public ResponseEntity<String> checkHealth() {
        System.out.println("________Application health check________");
        return ResponseEntity.ok("Application is running!");
    }
}