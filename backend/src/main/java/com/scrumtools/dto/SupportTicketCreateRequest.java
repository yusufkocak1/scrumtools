package com.scrumtools.dto;

import com.scrumtools.entity.enums.SupportCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SupportTicketCreateRequest {

    @NotBlank(message = "Konu boş olamaz")
    @Size(max = 200, message = "Konu 200 karakteri aşamaz")
    private String subject;

    @NotBlank(message = "Açıklama boş olamaz")
    @Size(max = 10000, message = "Açıklama 10000 karakteri aşamaz")
    private String description;

    @NotNull(message = "Kategori seçilmelidir")
    private SupportCategory category;

    /** İlişkili hata takip numarası (opsiyonel), örn. ERR-7K3M2A9X */
    @Size(max = 16)
    private String errorTrackingCode;
}
