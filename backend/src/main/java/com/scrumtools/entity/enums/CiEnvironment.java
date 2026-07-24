package com.scrumtools.entity.enums;

/**
 * Job eşlemesinin hedef ortamı. Serbest ortam tanımı ihtiyacı doğarsa
 * ayrı bir entity'ye taşınacak (bkz. plan bölüm 8/1).
 */
public enum CiEnvironment {
    TEST,
    STAGING,
    PROD
}
