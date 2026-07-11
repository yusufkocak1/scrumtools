package com.scrumtools.exception;

import lombok.Getter;

/**
 * Organizasyonun paketi ilgili işleme izin vermediğinde fırlatılır.
 * GlobalExceptionHandler tarafından HTTP 402 + {code:"PLAN_LIMIT"} olarak dönülür;
 * frontend bu cevaba göre yükseltme (upgrade) akışını tetikler.
 */
@Getter
public class PlanLimitExceededException extends RuntimeException {

    /** MEMBERS | PROJECTS | FEATURE */
    private final String limitType;

    public PlanLimitExceededException(String limitType, String message) {
        super(message);
        this.limitType = limitType;
    }
}
