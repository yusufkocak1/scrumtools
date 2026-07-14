package com.scrumtools.service.scm.client;

/** Token doğrulaması sırasında sağlayıcıdan dönen kimlik bilgisi. */
public record ScmUserInfo(String username, String email) {}
