package cn.iocoder.yudao.module.dcc.job;

import cn.iocoder.yudao.framework.tenant.core.service.TenantFrameworkService;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileNasTransferService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "yudao.local-job-control", name = "dcc-nas-transfer-enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class DccControlledFileNasTransferTaskScheduler {

    @Resource
    private TenantFrameworkService tenantFrameworkService;

    @Resource
    private DccControlledFileNasTransferService nasTransferService;

    @PostConstruct
    public void recoverInterruptedTasksOnStartup() {
        for (Long tenantId : tenantFrameworkService.getTenantIds()) {
            TenantUtils.execute(tenantId, nasTransferService::recoverInterruptedTasksOnStartup);
        }
    }

    @Scheduled(cron = "0/30 * * * * ?")
    public void schedule() {
        for (Long tenantId : tenantFrameworkService.getTenantIds()) {
            TenantUtils.execute(tenantId, () -> {
                try {
                    nasTransferService.processWaitingTasks();
                } catch (RuntimeException exception) {
                    log.error("[schedule][tenantId({}) DCC NAS transfer task execution failed]", tenantId, exception);
                }
            });
        }
    }
}
