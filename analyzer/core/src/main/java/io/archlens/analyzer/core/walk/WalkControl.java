package io.archlens.analyzer.core.walk;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Cancelamento cooperativo do caminhamento. Não interrompe threads à força.
 */
public final class WalkControl {

    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public void cancel() {
        cancelled.set(true);
    }

    public boolean isCancelled() {
        return cancelled.get();
    }
}
