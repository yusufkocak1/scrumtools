package com.scrumtools.entity.enums;

/**
 * Job eşlemesinin kullanım amacı. Task dev panelinde yalnız TASK_DEPLOY,
 * release ekranında yalnız RELEASE_PIPELINE eşlemeleri listelenir.
 */
public enum CiJobType {
    TASK_DEPLOY,
    RELEASE_PIPELINE
}
