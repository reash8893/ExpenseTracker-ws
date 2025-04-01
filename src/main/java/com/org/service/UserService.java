package com.org.service;

import com.org.models.User;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface UserService extends UserDetailsService {
    User registerUser(User user);
    User getUserByUsername(String username);
    User getUserByEmail(String email);
}
