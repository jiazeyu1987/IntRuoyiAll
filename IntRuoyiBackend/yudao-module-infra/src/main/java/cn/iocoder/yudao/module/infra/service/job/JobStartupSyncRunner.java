package cn.iocoder.yudao.module.infra.service.job;

import cn.iocoder.yudao.framework.quartz.core.scheduler.SchedulerManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.SchedulerException;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Syncs persisted job definitions to Quartz when the application starts.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
@RequiredArgsConstructor
@Slf4j
public class JobStartupSyncRunner implements ApplicationRunner {

    private final JobService jobService;
    private final SchedulerManager schedulerManager;

    @Override
    public void run(ApplicationArguments args) throws SchedulerException {
        if (!schedulerManager.isEnabled()) {
            log.info("[run][Quartz 已禁用，跳过启动阶段定时任务同步]");
            return;
        }
        jobService.syncJob();
        log.info("[run][定时任务已同步到 Quartz]");
    }

}
