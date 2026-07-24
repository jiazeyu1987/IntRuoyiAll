package cn.iocoder.yudao.module.dcc.job;

import cn.iocoder.yudao.framework.tenant.core.service.TenantFrameworkService;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.dcc.service.permission.DccNasPermissionRestoreExecutionService;
import jakarta.annotation.Resource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "yudao.local-job-control", name = "dcc-nas-permission-restore-enabled", havingValue = "true", matchIfMissing = true)
public class DccNasPermissionRestoreExecutionScheduler {

    @Resource
    private TenantFrameworkService tenantFrameworkService;

    @Resource
    private DccNasPermissionRestoreExecutionService restoreExecutionService;

    @Scheduled(cron = "0/30 * * * * ?")
    public void schedule() {
        for (Long tenantId : tenantFrameworkService.getTenantIds()) {
            TenantUtils.execute(tenantId, restoreExecutionService::processWaitingRestorePlans);
        }
    }

}
