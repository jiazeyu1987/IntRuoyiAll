package cn.iocoder.yudao.module.dcc.service.file.listener;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileLifecycleService;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Register scheduling only in the manager-approved isolated runtime. */
@Component("dccControlledFileActivationJob")
@Slf4j
public class DccControlledFileActivationJob implements JobHandler {
    private final DccControlledFileLifecycleService lifecycleService;
    public DccControlledFileActivationJob(DccControlledFileLifecycleService lifecycleService) {
        this.lifecycleService = lifecycleService;
    }
    @Override
    @TenantJob(failOnError = true)
    public String execute(String param) {
        int activated = 0;
        List<Long> failedFileIds = new ArrayList<>();
        List<RuntimeException> failures = new ArrayList<>();
        for (var file : lifecycleService.dueVersions()) {
            try {
                if (lifecycleService.activateDue(file.getId())) activated++;
            } catch (RuntimeException failure) {
                failedFileIds.add(file.getId());
                failures.add(failure);
                log.error("DCC_ACTIVATION_FAILED tenantId={} fileId={} processDefinitionKey={}",
                        TenantContextHolder.getRequiredTenantId(), file.getId(), file.getProcessDefinitionKey(), failure);
            }
        }
        if (!failures.isEmpty()) {
            IllegalStateException summary = new IllegalStateException(
                    "DCC activation batch failed: activatedCount=" + activated + " failedFileIds=" + failedFileIds);
            failures.forEach(summary::addSuppressed);
            throw summary;
        }
        return "实际生效版本数：" + activated;
    }
}
