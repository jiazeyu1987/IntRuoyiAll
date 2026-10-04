package cn.iocoder.yudao.module.dcc.service.file;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.nio.file.Path;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Only explicit local-maintenance + enabled=true; absent on ordinary application startup.
 */
@Component
@Profile("local-maintenance & !prod & !production & !backup & !test & !unit-test")
@ConditionalOnProperty(prefix = "yudao.dcc.legacy-registration-maintenance", name = "enabled", havingValue = "true", matchIfMissing = false)
public class DccLegacyNameRegistrationMaintenanceRunner implements ApplicationRunner {

    final DccLegacyNameRegistrationMaintenanceCommand command;

    final Path protectedRoot, request;

    final String requestSha;

    final ConfigurableApplicationContext context;

    public DccLegacyNameRegistrationMaintenanceRunner(DccLegacyNameRegistrationMaintenanceCommand command, @Value("${yudao.dcc.legacy-registration-maintenance.protected-root}") String root, @Value("${yudao.dcc.legacy-registration-maintenance.request-file}") String request, @Value("${yudao.dcc.legacy-registration-maintenance.request-sha256}") String sha, ConfigurableApplicationContext context) {
        this.command = command;
        this.protectedRoot = Path.of(root);
        this.request = Path.of(request);
        this.requestSha = sha;
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) {
        var receipt = command.execute(protectedRoot, request, requestSha, System.in);
        if (receipt == null || receipt.scopeId() == null || receipt.scopeId() < 1 || receipt.eventId() == null || receipt.eventId() < 1 || receipt.sequence() == null || receipt.sequence() < 1 || receipt.eventHash() == null || !receipt.eventHash().matches("[0-9a-f]{64}"))
            throw DccLegacyMaintenanceGate.invalid("ACTUAL_RECEIPT_MISSING");
        // Only nonsecret stable receipt identities; no stdin, actor input, artifact or credential is echoed.
        System.out.println("{\"status\":\"LEGACY_REGISTRATION_ACTUAL_RECEIPT\",\"scopeId\":\"" + receipt.scopeId() + "\",\"eventId\":\"" + receipt.eventId() + "\",\"sequence\":\"" + receipt.sequence() + "\",\"eventHash\":\"" + receipt.eventHash() + "\",\"replay\":" + receipt.replay() + "}");
        context.close();
    }
}
