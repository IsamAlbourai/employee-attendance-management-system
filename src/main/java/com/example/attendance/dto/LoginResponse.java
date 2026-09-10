package com.example.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {

    private Long id;
    private String name;
    private String email;
    private String role;
    private boolean active;

    private String token;
    private String tokenType;
}