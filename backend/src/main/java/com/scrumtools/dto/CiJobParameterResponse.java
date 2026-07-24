package com.scrumtools.dto;

import com.scrumtools.service.ci.client.CiJobParameter;

import java.util.List;

/** Job'ın tanımlı parametreleri — eşleme ekranında şablonu ön doldurmak için. */
public record CiJobParameterResponse(
        String name,
        String type,
        String defaultValue,
        List<String> choices
) {
    public static CiJobParameterResponse from(CiJobParameter p) {
        return new CiJobParameterResponse(p.name(), p.type(), p.defaultValue(), p.choices());
    }
}
