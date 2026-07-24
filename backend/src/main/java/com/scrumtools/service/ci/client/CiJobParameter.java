package com.scrumtools.service.ci.client;

import java.util.List;

/**
 * Job'ın tanımlı bir build parametresi — eşleme ekranında şablonu otomatik
 * doldurmak için kullanılır.
 *
 * @param name         parametre adı (BRANCH, ENV ...)
 * @param type         sağlayıcı tipi (StringParameterDefinition, ChoiceParameterDefinition ...)
 * @param defaultValue tanımlı varsayılan, yoksa null
 * @param choices      ChoiceParameterDefinition ise seçenekler, değilse boş liste
 */
public record CiJobParameter(String name, String type, String defaultValue, List<String> choices) {}
