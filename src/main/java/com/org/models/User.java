package com.org.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long userId;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNo;
    
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
}
