package cn.iocoder.yudao.module.dcc.job;

import cn.iocoder.yudao.framework.tenant.core.service.TenantFrameworkService;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileBatchRecognitionService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Component
@ConditionalOnProperty(prefix = "yudao.local-job-control", name = "dcc-batch-recognition-enabled", havingValue = "true", matchIfMissing = true)
public class DccControlledFileBatchRecognitionStartupRecovery {

    @Resource
    private TenantFrameworkService tenantFrameworkService;

    @Resource
    private DccControlledFileBatchRecognitionService batchRecognitionService;

    @PostConstruct
    public void recoverInterruptedTasksOnStartup() {
        for (Long tenantId : tenantFrameworkService.getTenantIds()) {
            TenantUtils.execute(tenantId, batchRecognitionService::recoverInterruptedTasksOnStartup);
        }
    }
}
