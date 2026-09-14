package cn.iocoder.yudao.module.dcc.job;

import cn.iocoder.yudao.framework.tenant.core.service.TenantFrameworkService;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.dcc.service.file.DccNasControlAuditService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "yudao.local-job-control", name = "dcc-nas-control-audit-enabled",
        havingValue = "true", matchIfMissing = true)
@Slf4j
public class DccNasControlAuditTaskScheduler {

    @Resource
    private TenantFrameworkService tenantFrameworkService;
    @Resource
    private DccNasControlAuditService auditService;

    @PostConstruct
    public void recoverInterruptedTasksOnStartup() {
        for (Long tenantId : tenantFrameworkService.getTenantIds()) {
            TenantUtils.execute(tenantId, auditService::recoverInterruptedTasksOnStartup);
        }
    }

    @Scheduled(cron = "0/30 * * * * ?")
    public void schedule() {
        for (Long tenantId : tenantFrameworkService.getTenantIds()) {
            TenantUtils.execute(tenantId, () -> {
                try {
                    auditService.processWaitingTasks();
                } catch (RuntimeException exception) {
                    log.error("[schedule][tenantId({}) DCC NAS control audit execution failed]", tenantId, exception);
                }
            });
        }
    }
}
