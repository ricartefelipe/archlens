package io.archlens.infrastructure.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class ArchLensMetrics {

    @Inject
    MeterRegistry registry;

    private Counter importSuccess;
    private Counter importSchemaFailure;
    private Counter findingsGenerated;
    private Counter reportGenerated;
    private Counter reportPdfFailure;
    private Counter reportIntegrityFailure;
    private Counter importIntegrityFailure;
    private Counter oidcFailure;
    private Counter dbFailure;
    private Timer importDuration;
    private Timer reportDuration;

    @PostConstruct
    void init() {
        importSuccess = registry.counter("archlens_import_total", "result", "success");
        importSchemaFailure = registry.counter("archlens_import_schema_failure_total");
        findingsGenerated = registry.counter("archlens_findings_generated_total");
        reportGenerated = registry.counter("archlens_report_generated_total");
        reportPdfFailure = registry.counter("archlens_report_pdf_failure_total");
        reportIntegrityFailure = registry.counter("archlens_report_integrity_failure_total");
        importIntegrityFailure = registry.counter("archlens_import_integrity_failure_total");
        oidcFailure = registry.counter("archlens_oidc_failure_total");
        dbFailure = registry.counter("archlens_db_failure_total");
        importDuration = registry.timer("archlens_import_duration_seconds");
        reportDuration = registry.timer("archlens_report_duration_seconds");
    }

    public void recordImportSuccess(int findingsCount, long durationMs) {
        importSuccess.increment();
        findingsGenerated.increment(findingsCount);
        importDuration.record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordImportSchemaFailure() {
        importSchemaFailure.increment();
    }

    public void recordReportSuccess(long durationMs) {
        reportGenerated.increment();
        reportDuration.record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordReportPdfFailure() {
        reportPdfFailure.increment();
    }

    public void recordReportIntegrityFailure() {
        reportIntegrityFailure.increment();
    }

    public void recordImportIntegrityFailure() {
        importIntegrityFailure.increment();
    }

    public void recordOidcFailure() {
        oidcFailure.increment();
    }

    public void recordDbFailure() {
        dbFailure.increment();
    }
}
