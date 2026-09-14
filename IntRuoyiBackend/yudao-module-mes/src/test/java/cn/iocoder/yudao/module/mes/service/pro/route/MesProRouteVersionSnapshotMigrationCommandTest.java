package cn.iocoder.yudao.module.mes.service.pro.route;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MesProRouteVersionSnapshotMigrationCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void runner_shouldMarkProductionConstructorForSpringInjection() throws Exception {
        assertTrue(MesProRouteVersionSnapshotMigrationRunner.class
                .getConstructor(
                        MesProRouteVersionSnapshotMigrationCommand.class,
                        ConfigurableApplicationContext.class,
                        String.class,
                        String.class)
                .isAnnotationPresent(Autowired.class));
    }

    @Test
    void run_shouldWriteMachineReadableBlockerReportAndReturnNonZero() throws Exception {
        MesProRouteVersionSnapshotHashMigrationService service =
                mock(MesProRouteVersionSnapshotHashMigrationService.class);
        when(service.readinessAllTenants()).thenReturn(
                new MesProRouteVersionSnapshotHashMigrationService.MigrationResult(2, 0, List.of(
                        new MesProRouteVersionSnapshotHashMigrationService.MigrationBlocker(
                                11L, 9L, "V1", "ROUTE_PROCESS_ID_DUPLICATE", "routeProcessId=101"))));
        MesProRouteVersionSnapshotMigrationCommand command =
                new MesProRouteVersionSnapshotMigrationCommand(service);
        Path report = tempDir.resolve("readiness.json");

        int exitCode = command.run(MesProRouteVersionSnapshotMigrationCommand.MODE_READINESS, report);

        assertEquals(MesProRouteVersionSnapshotMigrationCommand.EXIT_BLOCKED, exitCode);
        String json = Files.readString(report, StandardCharsets.UTF_8);
        assertTrue(json.contains("\"status\" : \"BLOCKED\""));
        assertTrue(json.contains("\"blockerCount\" : 1"));
        assertTrue(json.contains("ROUTE_PROCESS_ID_DUPLICATE"));
    }

    @Test
    void run_shouldReturnExecutionFailureAndReportWriteDrift() throws Exception {
        MesProRouteVersionSnapshotHashMigrationService service =
                mock(MesProRouteVersionSnapshotHashMigrationService.class);
        MesProRouteVersionSnapshotHashMigrationService.MigrationResult failed =
                new MesProRouteVersionSnapshotHashMigrationService.MigrationResult(1, 1, List.of(
                        new MesProRouteVersionSnapshotHashMigrationService.MigrationBlocker(
                                11L, 9L, "V1", "SNAPSHOT_HASH_MISMATCH", "stored hash drifted")));
        when(service.backfillAll()).thenThrow(
                new MesProRouteVersionSnapshotHashMigrationService.MigrationExecutionException(failed));
        MesProRouteVersionSnapshotMigrationCommand command =
                new MesProRouteVersionSnapshotMigrationCommand(service);
        Path report = tempDir.resolve("failed.json");

        int exitCode = command.run(MesProRouteVersionSnapshotMigrationCommand.MODE_BACKFILL, report);

        assertEquals(MesProRouteVersionSnapshotMigrationCommand.EXIT_EXECUTION_FAILED, exitCode);
        assertTrue(Files.readString(report, StandardCharsets.UTF_8).contains("SNAPSHOT_HASH_MISMATCH"));
    }

    @Test
    void runner_shouldFailStartupForAnyNonZeroCommandResult() {
        MesProRouteVersionSnapshotMigrationCommand command =
                mock(MesProRouteVersionSnapshotMigrationCommand.class);
        when(command.run(MesProRouteVersionSnapshotMigrationCommand.MODE_READINESS,
                tempDir.resolve("blocked.json"))).thenReturn(MesProRouteVersionSnapshotMigrationCommand.EXIT_BLOCKED);
        MesProRouteVersionSnapshotMigrationRunner runner = new MesProRouteVersionSnapshotMigrationRunner(
                command, mock(ConfigurableApplicationContext.class),
                MesProRouteVersionSnapshotMigrationCommand.MODE_READINESS,
                tempDir.resolve("blocked.json").toString());

        assertThrows(IllegalStateException.class,
                () -> runner.run(new DefaultApplicationArguments(new String[0])));
    }

    @Test
    void runner_shouldExitJvmAfterSuccessfulCommandResult() throws Exception {
        MesProRouteVersionSnapshotMigrationCommand command =
                mock(MesProRouteVersionSnapshotMigrationCommand.class);
        when(command.run(MesProRouteVersionSnapshotMigrationCommand.MODE_BACKFILL,
                tempDir.resolve("ready.json"))).thenReturn(MesProRouteVersionSnapshotMigrationCommand.EXIT_READY);
        AtomicInteger capturedExitCode = new AtomicInteger(-1);
        MesProRouteVersionSnapshotMigrationRunner runner = new MesProRouteVersionSnapshotMigrationRunner(
                command,
                mock(ConfigurableApplicationContext.class),
                MesProRouteVersionSnapshotMigrationCommand.MODE_BACKFILL,
                tempDir.resolve("ready.json").toString(),
                capturedExitCode::set);

        runner.run(new DefaultApplicationArguments(new String[0]));

        assertEquals(MesProRouteVersionSnapshotMigrationCommand.EXIT_READY, capturedExitCode.get());
    }
}
