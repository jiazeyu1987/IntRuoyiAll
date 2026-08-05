package cn.iocoder.yudao.module.erp.job.kingdeeautosync;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.erp.service.kingdeeautosync.ErpKingdeeTableAutoSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("erpKingdeeTableAutoSyncJob")
@RequiredArgsConstructor
public class ErpKingdeeTableAutoSyncJob implements JobHandler {

    private final ErpKingdeeTableAutoSyncService tableAutoSyncService;

    @Override
    @TenantJob
    public String execute(String param) {
        return tableAutoSyncService.executeAutoForCurrentTenant();
    }
}
