package com.scrumtools.query;

import java.util.Set;

import static com.scrumtools.query.QueryOperator.*;

/**
 * Alan veri tipi — hangi operatörlerin geçerli olduğunu ve değerin nasıl
 * çözümleneceğini belirler.
 */
public enum FieldType {

    /** Kısa metin (title, customId, environment...). */
    STRING(Set.of(EQ, NEQ, CONTAINS, NOT_CONTAINS, IN, NOT_IN, IS_EMPTY, IS_NOT_EMPTY)),

    /** Uzun metin (description) — eşitlik anlamsız, arama odaklı. */
    TEXT(Set.of(CONTAINS, NOT_CONTAINS, IS_EMPTY, IS_NOT_EMPTY)),

    /** Sınırlı değer kümesi (status, priority, issueType). */
    ENUM(Set.of(EQ, NEQ, IN, NOT_IN, IS_EMPTY, IS_NOT_EMPTY)),

    /** Kullanıcı e-postası tutan alan; currentUser() fonksiyonunu kabul eder. */
    USER(Set.of(EQ, NEQ, CONTAINS, IN, NOT_IN, IS_EMPTY, IS_NOT_EMPTY)),

    NUMBER(Set.of(EQ, NEQ, GT, GTE, LT, LTE, IN, NOT_IN, IS_EMPTY, IS_NOT_EMPTY)),

    DATE(Set.of(EQ, NEQ, GT, GTE, LT, LTE, IS_EMPTY, IS_NOT_EMPTY)),

    DATETIME(Set.of(EQ, NEQ, GT, GTE, LT, LTE, IS_EMPTY, IS_NOT_EMPTY)),

    /** @ElementCollection listesi (labels, watchers). */
    COLLECTION(Set.of(EQ, NEQ, CONTAINS, NOT_CONTAINS, IN, NOT_IN, IS_EMPTY, IS_NOT_EMPTY)),

    /** İlişkili entity (sprint, project, release, parent) — ad/key/UUID ile eşleşir. */
    ENTITY_REF(Set.of(EQ, NEQ, IN, NOT_IN, IS_EMPTY, IS_NOT_EMPTY)),

    /** customFields JSONB içindeki dinamik alan — cf[anahtar]. */
    CUSTOM_FIELD(Set.of(EQ, NEQ, CONTAINS, NOT_CONTAINS, IN, NOT_IN, IS_EMPTY, IS_NOT_EMPTY)),

    /**
     * Bir zengin filtrenin akıllı filtre sınıflandırması — smart["zengin filtre adı"].
     * Değerler akıllı filtre adlarıdır; IS EMPTY "hiçbirine uymayanlar" demektir.
     */
    SMART_FILTER(Set.of(EQ, NEQ, IN, NOT_IN, IS_EMPTY, IS_NOT_EMPTY));

    private final Set<QueryOperator> operators;

    FieldType(Set<QueryOperator> operators) {
        this.operators = operators;
    }

    public Set<QueryOperator> operators() {
        return operators;
    }

    public boolean supports(QueryOperator op) {
        return operators.contains(op);
    }

    /**
     * Alan bir grafiğin grup ekseni olabilir mi?
     *
     * Sınırlı sayıda ayrık değer üreten tipler gruplanabilir. Serbest metin
     * (başlık, açıklama) neredeyse her görevde farklı bir değer taşır — grafiği
     * anlamsız kılar; tarih alanları ise önce bir zaman kovasına indirilmelidir
     * (gün/hafta/ay), o da ayrı bir işin konusudur.
     */
    public boolean groupable() {
        return switch (this) {
            case ENUM, USER, ENTITY_REF, COLLECTION, CUSTOM_FIELD, SMART_FILTER -> true;
            case STRING, TEXT, NUMBER, DATE, DATETIME -> false;
        };
    }

    /** Alan bir ölçü (metrik) olarak toplanabilir mi — "kaç story point" gibi. */
    public boolean summable() {
        return this == NUMBER;
    }
}
