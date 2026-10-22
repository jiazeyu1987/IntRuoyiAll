package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Periodically reconciles stale release workflows.
 *
 * <p>The scheduler is intentionally fail-closed: exceptions from workflow
 * recovery are not swallowed, so an unconfirmed low-level operation
 * termination remains visible to Spring scheduling logs and operators.</p>
 */
@Component
public class ReleaseWorkflowRecoveryScheduler {

    private final ReleaseWorkflowOrchestrator orchestrator;

    public ReleaseWorkflowRecoveryScheduler(ReleaseWorkflowOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @Scheduled(initialDelay = 60_000L, fixedDelay = 30_000L)
    public void recoverStaleWorkflows() {
        Instant now = Instant.now();
        orchestrator.reconcileActiveWorkflows(now);
        orchestrator.recoverStaleWorkflows(now);
    }
}
