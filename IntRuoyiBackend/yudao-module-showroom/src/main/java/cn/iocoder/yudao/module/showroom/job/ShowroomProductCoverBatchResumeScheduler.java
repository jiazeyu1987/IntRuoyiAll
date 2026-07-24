package cn.iocoder.yudao.module.showroom.job;

import cn.iocoder.yudao.framework.tenant.core.service.TenantFrameworkService;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.showroom.cover.ShowroomProductCoverBatchTaskService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "yudao.local-job-control", name = "showroom-product-cover-batch-resume-enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class ShowroomProductCoverBatchResumeScheduler {

    @Resource
    private TenantFrameworkService tenantFrameworkService;

    @Resource
    private ShowroomProductCoverBatchTaskService taskService;

    @PostConstruct
    public void recoverInterruptedTasksOnStartup() {
        taskService.recoverInterruptedTasksOnStartup();
    }

    @Scheduled(cron = "0 0/10 * * * ?")
    public void schedule() {
        for (Long tenantId : tenantFrameworkService.getTenantIds()) {
            TenantUtils.execute(tenantId, () -> {
                try {
                    taskService.processWaitingTasks();
                } catch (RuntimeException exception) {
                    log.error("[schedule][tenantId({}) 展厅产品批量封面后台续跑执行失败]", tenantId, exception);
                }
            });
        }
    }
}
