package cn.iocoder.yudao.module.mes.service.pro.route;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
@ConditionalOnProperty(prefix = "yudao.mes.route-snapshot-migration", name = "mode")
public class MesProRouteVersionSnapshotMigrationRunner implements ApplicationRunner {

    private final MesProRouteVersionSnapshotMigrationCommand command;
    private final ConfigurableApplicationContext applicationContext;
    private final String mode;
    private final Path reportFile;

    public MesProRouteVersionSnapshotMigrationRunner(
            MesProRouteVersionSnapshotMigrationCommand command,
            ConfigurableApplicationContext applicationContext,
            @Value("${yudao.mes.route-snapshot-migration.mode}") String mode,
            @Value("${yudao.mes.route-snapshot-migration.report-file}") String reportFile) {
        this.command = command;
        this.applicationContext = applicationContext;
        this.mode = mode;
        this.reportFile = Path.of(reportFile);
    }

    @Override
    public void run(ApplicationArguments args) {
        int exitCode = command.run(mode, reportFile);
        if (exitCode != MesProRouteVersionSnapshotMigrationCommand.EXIT_READY) {
            throw new IllegalStateException("route snapshot migration command failed, exitCode=" + exitCode
                    + ", reportFile=" + reportFile.toAbsolutePath().normalize());
        }
        SpringApplication.exit(applicationContext, () -> MesProRouteVersionSnapshotMigrationCommand.EXIT_READY);
    }
}
