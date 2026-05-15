package com.scrumtools.dto;

import lombok.Data;

@Data
public class SprintRequest {
    private String name;
    private String goal;
    private String startDate;
    private String endDate;
}
