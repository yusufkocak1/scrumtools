package com.scrumtools.service.collab.macro;

import com.scrumtools.entity.enums.MacroRunStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.IOAccess;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Sunucu tarafı makro yürütmesi — GraalJS + bekçi iş parçacığı
 * (COLLAB_WORKSPACE_PLAN.md Faz 5, K8'in D3 notu).
 *
 * <p><b>Neden sunucuda çalıştırıyoruz:</b> webhook ve zamanlanmış tetikleyicilerde
 * ortada açık bir sekme yoktur. K8 makroların tarayıcıda koşmasını şart koşuyor
 * ama o kural "kullanıcı varken" geçerli; tetikleyici geldiğinde kullanıcı yok.
 *
 * <p><b>Neden ayrı bir yürütücü değil de aynı sıkı sınırlar:</b> tarayıcıdaki
 * worker sandbox'ı tutamakları silerek çalışıyordu; burada karşılığı
 * {@link HostAccess#NONE} ve kapalı tüm yetenekler. Betik ne dosya açabilir, ne
 * iş parçacığı yaratabilir, ne süreç başlatabilir, ne de bir Java sınıfına
 * ulaşabilir. Sınırdan yalnızca JSON geçer ({@link ServerMacroApi}).
 *
 * <p><b>Bekçi neden ayrı iş parçacığında:</b> tarayıcıdaki gerekçenin aynısı —
 * sonsuz döngüye girmiş bir betik kendi zamanlayıcısını çalıştıramaz.
 * {@code context.close(true)} yürütmeyi iptal eder ve {@code PolyglotException}
 * fırlatır.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ServerMacroRunner {

    private final ServerMacroApi api;

    /**
     * Bekçi zamanlayıcısı. Tek iş parçacığı yeterli: makrolar zaten tek tek
     * koşuyor ({@link CollabMacroQueue}), aynı anda en fazla bir bekçi gerekir.
     */
    private final ScheduledExecutorService watchdog = Executors.newSingleThreadScheduledExecutor(
            runnable -> {
                Thread thread = new Thread(runnable, "collab-macro-watchdog");
                thread.setDaemon(true);
                return thread;
            });

    public record Result(MacroRunStatus status, String log, String error, long durationMs) {
    }

    /**
     * Betiği çalıştırır.
     *
     * <p>Entity değil düz değerler alıyor: çağrı kuyruk iş parçacığından ve açık
     * bir işlem dışından geliyor (bkz. {@link CollabServerMacroService}).
     *
     * @param identityEmail makronun hangi kullanıcının yetkisiyle koşacağı —
     *                      onaylayan kişi
     */
    public Result run(String source, UUID projectId, String identityEmail,
                      Map<String, Object> triggerPayload, long timeoutMs) {
        long startedAt = System.currentTimeMillis();
        StringBuilder log = new StringBuilder();

        // Kimlik bu iş parçacığına yazılıyor: çağrılan servisler yetkiyi
        // SecurityContextHolder'dan okuyor, yani ayrı bir yetki yolu açılmıyor.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(identityEmail, null, List.of()));

        Context context = buildContext();
        ScheduledFuture<?> killer = watchdog.schedule(
                () -> closeQuietly(context), timeoutMs, TimeUnit.MILLISECONDS);

        try {
            api.install(context, new ServerMacroApi.RunContext(projectId, triggerPayload, log));
            Value outcome = context.eval("js", wrap(source));

            String error = readError(outcome);
            if (error != null) {
                return new Result(MacroRunStatus.FAILED, log.toString(), error, elapsed(startedAt));
            }
            return new Result(MacroRunStatus.SUCCESS, log.toString(), null, elapsed(startedAt));

        } catch (PolyglotException e) {
            if (e.isCancelled() || e.isInterrupted()) {
                return new Result(MacroRunStatus.TIMEOUT, log.toString(),
                        "Makro " + (timeoutMs / 1000) + " saniyede tamamlanmadı ve durduruldu.",
                        elapsed(startedAt));
            }
            return new Result(MacroRunStatus.FAILED, log.toString(),
                    e.getMessage(), elapsed(startedAt));
        } catch (Exception e) {
            return new Result(MacroRunStatus.FAILED, log.toString(),
                    e.getMessage() != null ? e.getMessage() : e.toString(), elapsed(startedAt));
        } finally {
            killer.cancel(false);
            closeQuietly(context);
            // İş parçacığı havuzda yeniden kullanılıyor; bağlam bırakılırsa bu
            // makronun kimliği sıradakine sızardı (bkz. CollabMacroQueue).
            SecurityContextHolder.clearContext();
        }
    }

    private Context buildContext() {
        return Context.newBuilder("js")
                .allowHostAccess(HostAccess.NONE)
                .allowHostClassLookup(className -> false)
                .allowIO(IOAccess.NONE)
                .allowCreateThread(false)
                .allowCreateProcess(false)
                .allowNativeAccess(false)
                .allowPolyglotAccess(org.graalvm.polyglot.PolyglotAccess.NONE)
                .allowEnvironmentAccess(org.graalvm.polyglot.EnvironmentAccess.NONE)
                // Stok JDK'da Truffle yorumlayıcı modda koşar; bu beklenen durum
                // (bkz. pom.xml notu), her çalıştırmada uyarı basmasın.
                .option("engine.WarnInterpreterOnly", "false")
                .build();
    }

    /**
     * Betiği bir {@code async} sarmalayıcıya alır ve sonucu global bir nesneye yazar.
     *
     * <p>{@code async}: kullanıcı {@code await} yazabilsin diye. Sunucu API'leri
     * senkron olduğundan mikro görev kuyruğu {@code eval} dönerken zaten
     * boşalıyor — bu yüzden sonucu okumak için ayrı bir bekleme gerekmiyor.
     * {@code done} yine de kontrol ediliyor: makro çözülmeyen bir söz beklerse
     * bunu sessiz bir başarı olarak raporlamak yanlış olurdu.
     *
     * <p><b>Birleştirme, {@code formatted()} değil:</b> kullanıcı kaynağı biçim
     * dizesi olarak yorumlanamaz. {@code i % 2} yazan ilk makro
     * {@code UnknownFormatConversionException} ile patlardı — üstelik hata JS'ten
     * değil Java'dan geleceği için mesajı kullanıcıya hiçbir şey anlatmazdı.
     */
    private String wrap(String source) {
        return "globalThis.__macroResult = { done: false, error: null };\n"
                + "(async () => {\n"
                + source
                + "\n})().then(\n"
                + "  () => { globalThis.__macroResult.done = true; },\n"
                + "  (e) => {\n"
                + "    globalThis.__macroResult.done = true;\n"
                + "    globalThis.__macroResult.error = String((e && (e.stack || e.message)) || e);\n"
                + "  }\n"
                + ");\n"
                + "globalThis.__macroResult;\n";
    }

    private String readError(Value outcome) {
        if (outcome == null || outcome.isNull()) return null;
        Value done = outcome.getMember("done");
        if (done != null && !done.asBoolean()) {
            return "Makro çözülmeyen bir söz (Promise) bekliyor; sunucu makrolarında "
                    + "dış olay beklemek mümkün değil.";
        }
        Value error = outcome.getMember("error");
        return (error == null || error.isNull()) ? null : error.asString();
    }

    private long elapsed(long startedAt) {
        return System.currentTimeMillis() - startedAt;
    }

    private void closeQuietly(Context context) {
        try {
            context.close(true);
        } catch (Exception e) {
            log.debug("Makro bağlamı kapatılamadı: {}", e.toString());
        }
    }
}
