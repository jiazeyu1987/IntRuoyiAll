package cn.iocoder.yudao.module.dcc.service.file.listener;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileLifecycleService;
import org.springframework.stereotype.Component;

/** Mandatory tenantId:fileId scope for an explicitly registered document activation job. */
@Component("dccControlledFileTargetedActivationJob")
public class DccControlledFileTargetedActivationJob implements JobHandler {
    private final DccControlledFileLifecycleService lifecycle;

    public DccControlledFileTargetedActivationJob(DccControlledFileLifecycleService lifecycle) {
        this.lifecycle = lifecycle;
    }

    @Override
    public String execute(String parameter) {
        if (parameter == null || !parameter.matches("[1-9][0-9]*:[1-9][0-9]*"))
            throw new IllegalArgumentException("生效作业必须指定租户ID:文件ID");
        String[] ids = parameter.split(":");
        long tenantId = Long.parseLong(ids[0]), fileId = Long.parseLong(ids[1]);
        int[] activated = {0};
        TenantUtils.execute(tenantId, () -> {
            if (lifecycle.activateDue(fileId)) activated[0] = 1;
        });
        return "实际生效版本数：" + activated[0];
    }
}
