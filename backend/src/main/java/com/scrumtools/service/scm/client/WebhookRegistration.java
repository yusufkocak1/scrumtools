package com.scrumtools.service.scm.client;

/** Sağlayıcıda oluşturulan webhook kaydı — externalId silme işlemi için saklanır. */
public record WebhookRegistration(String externalId) {}
