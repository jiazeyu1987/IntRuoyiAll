package cn.iocoder.yudao.framework.quartz.config;

import cn.iocoder.yudao.framework.quartz.core.scheduler.SchedulerManager;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Scheduler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Optional;

/**
 * 定时任务 Configuration
 */
@AutoConfiguration
@Slf4j
public class YudaoQuartzAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @EnableScheduling
    @ConditionalOnProperty(name = "intruoyi.release.maintenance", havingValue = "false", matchIfMissing = true)
    static class SchedulingConfiguration {
    }

    @Bean
    @ConditionalOnProperty(name = "intruoyi.release.maintenance", havingValue = "true")
    public SchedulerFactoryBeanCustomizer releaseMaintenanceQuartzCustomizer() {
        return factory -> factory.setAutoStartup(false);
    }

    @Bean
    public SchedulerManager schedulerManager(Optional<Scheduler> scheduler) {
        if (!scheduler.isPresent()) {
            log.info("[定时任务 - 已禁用][参考 https://doc.iocoder.cn/job/ 开启]");
            return new SchedulerManager(null);
        }
        return new SchedulerManager(scheduler.get());
    }

}
