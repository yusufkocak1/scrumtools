package com.scrumtools.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class UpdateRetroBoardRequest {
    private String retroBoardName;
    /** Yeni kolon listesi (sıralı). null ise kolonlar değişmez. */
    private List<String> columns;
    /** Kolon yeniden adlandırma haritası: eski ad → yeni ad. Item'ların columnName alanı buna göre taşınır. */
    private Map<String, String> columnRenames;
}
