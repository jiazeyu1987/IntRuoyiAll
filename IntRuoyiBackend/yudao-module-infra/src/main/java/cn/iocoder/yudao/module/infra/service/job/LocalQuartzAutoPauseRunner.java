package cn.iocoder.yudao.module.infra.service.job;

import cn.iocoder.yudao.framework.quartz.core.scheduler.SchedulerManager;
import cn.iocoder.yudao.module.infra.dal.dataobject.job.JobDO;
import cn.iocoder.yudao.module.infra.dal.mysql.job.JobMapper;
import cn.iocoder.yudao.module.infra.enums.job.JobStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.SchedulerException;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Keeps Quartz loaded in local profile while preventing non-allowlisted jobs
 * from auto-running in the developer environment.
 */
@Component
@Profile("local")
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class LocalQuartzAutoPauseRunner implements ApplicationRunner {

    private final JobMapper jobMapper;
    private final SchedulerManager schedulerManager;
    private final LocalJobControlProperties localJobControlProperties;

    @Override
    public void run(ApplicationArguments args) throws SchedulerException {
        if (!schedulerManager.isEnabled()) {
            log.info("[run][local Quartz 未启用，跳过本地自动任务收口]");
            return;
        }
        if (!localJobControlProperties.isEnabled()) {
            log.info("[run][local 自动任务收口已关闭，保持 Quartz 默认行为]");
            return;
        }

        List<JobDO> jobs = jobMapper.selectList();
        int pausedCount = 0;
        for (JobDO job : jobs) {
            if (!JobStatusEnum.NORMAL.getStatus().equals(job.getStatus())) {
                continue;
            }
            if (localJobControlProperties.allowsQuartzAutoRun(job.getHandlerName())) {
                continue;
            }
            schedulerManager.pauseJob(job.getHandlerName());
            pausedCount++;
            log.info("[run][local 自动暂停 Quartz 任务][jobId({}) handlerName({}) cron({})]",
                    job.getId(), job.getHandlerName(), job.getCronExpression());
        }
        log.info("[run][local Quartz 自动任务收口完成][pausedCount({}) allowlistedCount({})]",
                pausedCount, localJobControlProperties.getQuartzAutoRunHandlerWhitelist().size());
    }
}
