package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Component
public class MesProRouteVersionSnapshotMigrationCommand {

    public static final String MODE_BACKFILL = "BACKFILL";
    public static final String MODE_READINESS = "READINESS";
    public static final int EXIT_READY = 0;
    public static final int EXIT_BLOCKED = 2;
    public static final int EXIT_EXECUTION_FAILED = 3;
    public static final int EXIT_USAGE = 64;

    private final MesProRouteVersionSnapshotHashMigrationService migrationService;
    private final ObjectMapper objectMapper = JsonUtils.getObjectMapper();

    public MesProRouteVersionSnapshotMigrationCommand(
            MesProRouteVersionSnapshotHashMigrationService migrationService) {
        this.migrationService = migrationService;
    }

    public int run(String requestedMode, Path reportFile) {
        String mode = requestedMode == null ? "" : requestedMode.trim().toUpperCase(Locale.ROOT);
        CommandReport report;
        int exitCode;
        try {
            MesProRouteVersionSnapshotHashMigrationService.MigrationResult result;
            if (MODE_BACKFILL.equals(mode)) {
                result = migrationService.backfillAll();
            } else if (MODE_READINESS.equals(mode)) {
                result = migrationService.readinessAllTenants();
            } else {
                report = failureReport(mode, "MODE_INVALID",
                        "mode must be BACKFILL or READINESS");
                writeReport(reportFile, report);
                return EXIT_USAGE;
            }
            boolean ready = result.blockers().isEmpty();
            report = new CommandReport("MES_ROUTE_SNAPSHOT_MIGRATION_REPORT_V1", mode,
                    ready ? "READY" : "BLOCKED", Instant.now().toString(),
                    result.scannedCount(), result.updatedCount(), result.blockers().size(), result.blockers());
            exitCode = ready ? EXIT_READY : EXIT_BLOCKED;
        } catch (MesProRouteVersionSnapshotHashMigrationService.MigrationExecutionException ex) {
            MesProRouteVersionSnapshotHashMigrationService.MigrationResult result = ex.getResult();
            report = new CommandReport("MES_ROUTE_SNAPSHOT_MIGRATION_REPORT_V1", mode,
                    "FAILED", Instant.now().toString(), result.scannedCount(), result.updatedCount(),
                    result.blockers().size(), result.blockers());
            exitCode = EXIT_EXECUTION_FAILED;
        } catch (RuntimeException ex) {
            report = failureReport(mode, "MIGRATION_EXECUTION_FAILED", safeMessage(ex));
            exitCode = EXIT_EXECUTION_FAILED;
        }
        writeReport(reportFile, report);
        return exitCode;
    }

    private CommandReport failureReport(String mode, String reasonCode, String detail) {
        MesProRouteVersionSnapshotHashMigrationService.MigrationBlocker blocker =
                new MesProRouteVersionSnapshotHashMigrationService.MigrationBlocker(
                        null, null, null, reasonCode, detail);
        return new CommandReport("MES_ROUTE_SNAPSHOT_MIGRATION_REPORT_V1", mode,
                "FAILED", Instant.now().toString(), 0, 0, 1, List.of(blocker));
    }

    private void writeReport(Path reportFile, CommandReport report) {
        if (reportFile == null) {
            throw new IllegalArgumentException("route snapshot migration report file is required");
        }
        Path absolute = reportFile.toAbsolutePath().normalize();
        Path parent = absolute.getParent();
        if (parent == null) {
            throw new IllegalArgumentException("route snapshot migration report parent is required");
        }
        try {
            Files.createDirectories(parent);
            Path temporary = Files.createTempFile(parent, absolute.getFileName().toString(), ".tmp");
            try {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), report);
                Files.move(temporary, absolute, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("failed to write route snapshot migration report: " + absolute, ex);
        }
    }

    private String safeMessage(RuntimeException ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? ex.getClass().getSimpleName() : message;
    }

    public record CommandReport(String schemaVersion, String mode, String status, String generatedAt,
                                int scannedCount, int updatedCount, int blockerCount,
                                List<MesProRouteVersionSnapshotHashMigrationService.MigrationBlocker> blockers) {
    }
}
