package com.scrumtools.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Sprint kapatma sonucu — kullanıcıya "kaç iş taşındı/kapandı" özetini
 * gösterebilmek için sayılar sprintle birlikte döner.
 */
@Data
@Builder
public class CompleteSprintResponse {
    private SprintResponse sprint;
    /** Sprintte tamamlanmış (Done/Closed/Fixed/Verified) görev sayısı */
    private int completedCount;
    /** Kapatma anında tamamlanmamış olan görev sayısı */
    private int incompleteCount;
    /** Tamamlanmamış görevlere uygulanan aksiyon */
    private String incompleteAction;
    /** MOVE seçildiyse hedef sprintin adı */
    private String targetSprintName;
}
