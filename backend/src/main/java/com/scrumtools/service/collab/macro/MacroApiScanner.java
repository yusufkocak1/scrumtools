package com.scrumtools.service.collab.macro;

import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Makro kaynağında hangi API alanlarının kullanıldığını çıkarır (§9.2 — rıza diyaloğu).
 *
 * <p><b>Bu bir güvenlik sınırı değildir.</b> Düzenli ifadeyle yapılan statik tarama
 * {@code ScrumTools['ta' + 'sks']} gibi bir yazımı kaçırır. Gerçek sınır iki yerde:
 * makro Web Worker içinde ağ ve depolama erişimi olmadan koşar (K8), ve veri
 * çağrıları ana iş parçacığında <b>çalıştıran kullanıcının kendi oturumuyla</b>
 * yapılır — yani makro, kullanıcının zaten yapabileceğinden fazlasını yapamaz.
 *
 * <p>Taramanın işi kullanıcıya dürüst bir özet göstermek: "bu betik görevlerini
 * okuyacak ve Docs'a yazacak". Kaçırılan bir alan yetki kazandırmaz, yalnızca
 * diyaloğu eksik bırakır.
 */
@Component
public class MacroApiScanner {

    private static final Pattern SCOPE = Pattern.compile(
            "ScrumTools\\s*\\.\\s*(tasks|sprints|docs|http|ui|utils)\\b");

    /** Dokümana yazan çağrılar — rıza metninde "değiştirecek" ifadesini tetikler. */
    private static final Pattern DOCUMENT_WRITE = Pattern.compile(
            "\\.(setValues?|setFormula|setStyle|insertRows?|insertColumns?|"
                    + "deleteRows?|deleteColumns?|setText|replaceText|sort)\\s*\\(");

    private static final Pattern EXTERNAL_WRITE = Pattern.compile(
            "ScrumTools\\s*\\.\\s*docs\\s*\\.\\s*savePage\\s*\\(");

    public Set<String> scan(String source) {
        Set<String> scopes = new LinkedHashSet<>();
        if (source == null || source.isBlank()) return scopes;

        SCOPE.matcher(source).results()
                .map(match -> match.group(1).toLowerCase(Locale.ROOT))
                .forEach(scopes::add);

        if (DOCUMENT_WRITE.matcher(source).find()) scopes.add("document:write");
        if (EXTERNAL_WRITE.matcher(source).find()) scopes.add("docs:write");
        return scopes;
    }

    public String scanToString(String source) {
        return String.join(",", scan(source));
    }
}
