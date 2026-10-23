package cn.iocoder.yudao.framework.quartz.config;

import cn.iocoder.yudao.framework.quartz.core.scheduler.SchedulerManager;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Scheduler;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.config.TaskManagementConfigUtils;

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
    public static BeanFactoryPostProcessor releaseMaintenanceScheduledProcessorDisabler() {
        // Run after all configuration/registry processing, but before BeanPostProcessors are instantiated.
        // Third-party @EnableScheduling (including JimuReport) can otherwise register tasks in maintenance.
        return beanFactory -> {
            String processorName = TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME;
            if (beanFactory.containsBeanDefinition(processorName)) {
                ((BeanDefinitionRegistry) beanFactory).removeBeanDefinition(processorName);
            }
        };
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
