package cn.iocoder.yudao.release;
import cn.iocoder.yudao.framework.quartz.config.YudaoQuartzAutoConfiguration;
import cn.iocoder.yudao.framework.quartz.core.scheduler.SchedulerManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.quartz.Scheduler;
import static org.assertj.core.api.Assertions.assertThat;
class ReleaseMaintenanceJobTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(YudaoQuartzAutoConfiguration.class, QuartzAutoConfiguration.class))
        .withBean(ScheduledProbe.class);
    static class ScheduledProbe { @Scheduled(fixedDelay = 600000) public void tick() {} }
    @Test void maintenanceCreatesManagerButRegistersNoScheduledTasksAndDoesNotStartQuartz() {
        runner.withPropertyValues("intruoyi.release.maintenance=true").run(context -> {
            assertThat(context).hasSingleBean(SchedulerManager.class);
            assertThat(context).doesNotHaveBean(ScheduledAnnotationBeanPostProcessor.class);
            assertThat(context.getBean(Scheduler.class).isStarted()).isFalse();
        });
    }
    @Test void absentFlagKeepsSchedulingAndQuartzNormal() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(ScheduledAnnotationBeanPostProcessor.class);
            assertThat(context.getBean(ScheduledAnnotationBeanPostProcessor.class).getScheduledTasks()).hasSize(1);
            assertThat(context.getBean(Scheduler.class).isStarted()).isTrue();
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableScheduling
    static class IndependentlyEnabledScheduling {
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void maintenancePreventsRealJimuAndIndependentSchedulingRegardlessOfRegistrationOrder(boolean jimuFirst) {
        Class<?>[] configurations = jimuFirst
            ? new Class<?>[]{org.jeecg.modules.jmreport.automate.conf.a.class, IndependentlyEnabledScheduling.class}
            : new Class<?>[]{IndependentlyEnabledScheduling.class, org.jeecg.modules.jmreport.automate.conf.a.class};
        runner.withUserConfiguration(configurations)
            .withPropertyValues("intruoyi.release.maintenance=true")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(org.jeecg.modules.jmreport.automate.conf.a.class);
                assertThat(context).hasSingleBean(IndependentlyEnabledScheduling.class);
                assertThat(context).hasSingleBean(ScheduledProbe.class);
                assertThat(context).doesNotHaveBean(ScheduledAnnotationBeanPostProcessor.class);
                assertThat(context.getBean(Scheduler.class).isStarted()).isFalse();
                assertThat(context.getBean(Scheduler.class).isInStandbyMode()).isTrue();
            });
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "intruoyi.release.maintenance=false"})
    void realJimuAndIndependentSchedulingRemainEnabledOutsideMaintenance(String property) {
        ApplicationContextRunner normal = runner.withUserConfiguration(
            org.jeecg.modules.jmreport.automate.conf.a.class, IndependentlyEnabledScheduling.class);
        if (!property.isEmpty()) {
            normal = normal.withPropertyValues(property);
        }
        normal.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(org.jeecg.modules.jmreport.automate.conf.a.class);
            assertThat(context).hasSingleBean(ScheduledAnnotationBeanPostProcessor.class);
            assertThat(context.getBean(ScheduledAnnotationBeanPostProcessor.class).getScheduledTasks()).hasSize(1);
            assertThat(context.getBean(Scheduler.class).isStarted()).isTrue();
            assertThat(context).doesNotHaveBean("releaseMaintenanceScheduledProcessorDisabler");
        });
    }
}
