package com.scrumtools.service.ci;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Şablon değişkenlerinin tetikleme anındaki değerleri. Bir bağlamda anlamlı olmayan
 * alanlar null bırakılır (ör. TASK_DEPLOY'da releaseName); resolver o değişken şablonda
 * kullanılırsa anlaşılır hata verir.
 *
 * @param branch       {{branch}}
 * @param taskKey      {{taskKey}}     — Task.customId
 * @param taskTitle    {{taskTitle}}
 * @param releaseName  {{releaseName}}
 * @param projectKey   {{projectKey}}  — Project.key
 * @param triggeredBy  {{triggeredBy}} — tetikleyen kullanıcı email
 */
public record CiParameterContext(
        String branch,
        String taskKey,
        String taskTitle,
        String releaseName,
        String projectKey,
        String triggeredBy
) {

    /** Değişken adı → değer eşlemesi (null değerler de dahil — "bilinen ama boş" ayrımı için). */
    public Map<String, String> asVariableMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("branch", branch);
        map.put("taskKey", taskKey);
        map.put("taskTitle", taskTitle);
        map.put("releaseName", releaseName);
        map.put("projectKey", projectKey);
        map.put("triggeredBy", triggeredBy);
        return map;
    }
}
