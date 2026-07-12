package com.scrumtools.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.domain.Page;

@Data
@AllArgsConstructor
public class ErrorGroupDetailResponse {

    private ErrorGroupResponse group;
    private Page<ErrorEventResponse> events;
}
