package com.scrumtools.entity.enums;

/**
 * Build yaşam döngüsü. QUEUED ve RUNNING dışındakiler terminaldir; poller
 * yalnız terminal olmayanları sorgular.
 *
 * LOST: kuyruğa girdi ama makul sürede (30 dk) build numarası çözülemedi ya da
 * Jenkins kaydı artık bulunamıyor — temizlik adımı bu duruma çeker.
 */
public enum CiBuildStatus {
    QUEUED,
    RUNNING,
    SUCCESS,
    FAILURE,
    UNSTABLE,
    ABORTED,
    LOST;

    /** Poller'ın artık takip etmesi gerekmeyen durumlar. */
    public boolean isTerminal() {
        return this != QUEUED && this != RUNNING;
    }
}
