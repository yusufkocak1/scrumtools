package com.scrumtools.dto;

import lombok.Data;

@Data
public class CreateRetroItemRequest {
    private String value;
    private String owner; // email or "Anonymous"
}

