package io.archlens.application.service;

/**
 * Decide o próximo estado de uma limpeza de blob sem depender do banco.
 * Falha deixa a tarefa pendente para outra tentativa.
 */
public final class BlobCleanupPlanner {

    public record Task(int attempts, String status) {
    }

    private BlobCleanupPlanner() {
    }

    public static Task afterAttempt(Task current, boolean orphanExists, Runnable delete) {
        try {
            if (orphanExists) {
                delete.run();
            }
            return new Task(current.attempts(), "DONE");
        } catch (RuntimeException ex) {
            return new Task(current.attempts() + 1, "PENDING");
        }
    }
}
