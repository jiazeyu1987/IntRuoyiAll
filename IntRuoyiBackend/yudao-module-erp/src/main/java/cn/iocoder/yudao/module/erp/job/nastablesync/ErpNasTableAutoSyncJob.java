package cn.iocoder.yudao.module.erp.job.nastablesync;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.erp.service.nastablesync.ErpNasTableSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("erpNasTableAutoSyncJob")
@RequiredArgsConstructor
public class ErpNasTableAutoSyncJob implements JobHandler {

    private final ErpNasTableSyncService nasTableSyncService;

    @Override
    @TenantJob
    public String execute(String param) {
        return nasTableSyncService.executeAutoForCurrentTenant();
    }
}
