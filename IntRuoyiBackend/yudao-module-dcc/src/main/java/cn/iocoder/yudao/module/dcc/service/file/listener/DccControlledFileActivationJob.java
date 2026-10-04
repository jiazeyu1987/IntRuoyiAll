package cn.iocoder.yudao.module.dcc.service.file.listener;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileLifecycleService;
import org.springframework.stereotype.Component;

/** Register scheduling only in the manager-approved isolated runtime. */
@Component("dccControlledFileActivationJob")
public class DccControlledFileActivationJob implements JobHandler {
    private final DccControlledFileLifecycleService lifecycleService;
    public DccControlledFileActivationJob(DccControlledFileLifecycleService lifecycleService) {
        this.lifecycleService = lifecycleService;
    }
    @Override
    @TenantJob
    public String execute(String param) {
        int activated = 0;
        for (var file : lifecycleService.dueVersions()) {
            if (lifecycleService.activateDue(file.getId())) activated++;
        }
        return "实际生效版本数：" + activated;
    }
}
