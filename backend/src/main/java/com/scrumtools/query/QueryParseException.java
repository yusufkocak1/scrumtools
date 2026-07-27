package com.scrumtools.query;

import lombok.Getter;

/**
 * STQL sözdizimi/anlam hatası.
 * position ve length, hatanın kaynak metindeki yerini işaret eder; frontend
 * bu aralığın altını kırmızı çizerek gösterir.
 */
@Getter
public class QueryParseException extends RuntimeException {

    private final int position;
    private final int length;

    public QueryParseException(String message, int position, int length) {
        super(message);
        this.position = Math.max(0, position);
        this.length = Math.max(1, length);
    }

    public QueryParseException(String message, QueryToken token) {
        this(message, token.position(), token.length());
    }
}
