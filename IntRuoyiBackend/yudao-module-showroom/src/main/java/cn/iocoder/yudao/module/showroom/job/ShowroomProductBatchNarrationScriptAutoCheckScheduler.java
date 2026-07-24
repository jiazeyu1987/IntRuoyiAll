package cn.iocoder.yudao.module.showroom.job;

import cn.iocoder.yudao.framework.tenant.core.service.TenantFrameworkService;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.showroom.controller.ShowroomApiRuntime;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 展厅产品批量讲解任务调度器。
 */
@Component
@ConditionalOnProperty(prefix = "yudao.local-job-control", name = "showroom-product-batch-narration-script-auto-check-enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class ShowroomProductBatchNarrationScriptAutoCheckScheduler {

    @Resource
    private TenantFrameworkService tenantFrameworkService;

    @Resource
    private ShowroomApiRuntime runtime;

    @Scheduled(cron = "0 0/10 * * * ?")
    public void schedule() {
        for (Long tenantId : tenantFrameworkService.getTenantIds()) {
            TenantUtils.execute(tenantId, () -> {
                try {
                    runtime.runScheduledProductBatchNarrationScriptAutoCheck();
                } catch (RuntimeException exception) {
                    log.error("[schedule][tenantId({}) 展厅产品批量讲解任务执行失败]", tenantId, exception);
                }
            });
        }
    }
}
