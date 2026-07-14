package com.scrumtools.dto;

/** "Bağlantıyı Test Et" sonucu — başarısızlıkta da 200 döner, ok=false. */
public record ScmConnectionTestResponse(boolean ok, String message, String username) {}
