package com.scrumtools.dto;

import lombok.Data;
import java.util.List;

@Data
public class CreateRetroBoardRequest {
    private String retroBoardName;
    private List<String> columns;
}

