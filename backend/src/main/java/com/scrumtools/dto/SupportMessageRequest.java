package com.scrumtools.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SupportMessageRequest {

    @NotBlank(message = "Mesaj boş olamaz")
    @Size(max = 10000, message = "Mesaj 10000 karakteri aşamaz")
    private String content;
}
