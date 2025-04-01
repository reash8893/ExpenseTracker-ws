package com.org.controller;

import com.org.models.User;
import com.org.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("//expenseTracker/users")
public class UserAPI {

    private final UserService userService;

    public UserAPI(UserService userService) {
        this.userService = userService;
    }

    // ✅ User Registration
//    @PostMapping("/register")
//    public ResponseEntity<User> registerUser(@RequestBody User user) {
//        return ResponseEntity.ok(userService.registerUser(user));
//    }
//
//    // ✅ User Login
//    @PostMapping("/login")
//    public ResponseEntity<String> loginUser(@RequestParam String username, @RequestParam String password) {
//        return ResponseEntity.ok(userService.authenticateUser(username, password));
//    }
//
//    // ✅ Get User Profile
//    @GetMapping("/{id}")
//    public ResponseEntity<User> getUserById(@PathVariable Long id) {
//        return ResponseEntity.ok(userService.getUserById(id));
//    }
}
