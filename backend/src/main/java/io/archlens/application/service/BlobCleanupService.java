package io.archlens.application.service;

import io.archlens.infrastructure.persistence.BlobCleanupTaskEntity;
import io.archlens.infrastructure.persistence.ReportSnapshotEntity;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class BlobCleanupService {

    private static final AtomicInteger forcedStorageFailures = new AtomicInteger();

    public static void failNextStorageAttempts(int count) {
        forcedStorageFailures.set(Math.max(0, count));
    }

    public int sweep() {
        List<UUID> pending = QuarkusTransaction.requiringNew().call(() ->
                BlobCleanupTaskEntity.listPending().stream().map(task -> task.id).toList());
        int completed = 0;
        for (UUID taskId : pending) {
            try {
                if (QuarkusTransaction.requiringNew().call(() -> completeTask(taskId))) {
                    completed++;
                }
            } catch (RuntimeException ex) {
                QuarkusTransaction.requiringNew().run(() -> markFailure(taskId, ex));
            }
        }
        return completed;
    }

    private boolean completeTask(UUID taskId) {
        if (forcedStorageFailures.get() > 0) {
            forcedStorageFailures.decrementAndGet();
            throw new IllegalStateException("falha de armazenamento");
        }
        BlobCleanupTaskEntity task = BlobCleanupTaskEntity.findById(taskId);
        if (task == null || "DONE".equals(task.status)) {
            return false;
        }
        ReportSnapshotEntity snapshot = ReportSnapshotEntity.findById(task.snapshotId);
        boolean orphan = snapshot != null && snapshot.assessmentId == null;
        BlobCleanupPlanner.Task next = BlobCleanupPlanner.afterAttempt(
                new BlobCleanupPlanner.Task(task.attempts, task.status),
                orphan,
                () -> {
                    if (snapshot != null) {
                        snapshot.delete();
                    }
                }
        );
        task.attempts = next.attempts();
        task.status = next.status();
        task.updatedAt = Instant.now();
        if ("DONE".equals(next.status())) {
            task.lastError = null;
        }
        return "DONE".equals(next.status());
    }

    private void markFailure(UUID taskId, RuntimeException error) {
        BlobCleanupTaskEntity task = BlobCleanupTaskEntity.findById(taskId);
        if (task == null || "DONE".equals(task.status)) {
            return;
        }
        task.attempts = task.attempts + 1;
        task.status = "PENDING";
        task.lastError = error.getClass().getSimpleName();
        task.updatedAt = Instant.now();
    }
}
