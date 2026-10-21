package cn.iocoder.yudao.release;
import cn.iocoder.yudao.framework.mq.redis.config.YudaoRedisMQConsumerAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import static org.assertj.core.api.Assertions.assertThat;
class ReleaseMaintenanceRedisTest {
    @Test void maintenanceDoesNotInstantiateConsumerConfigurationOrContainers() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(YudaoRedisMQConsumerAutoConfiguration.class))
            .withPropertyValues("intruoyi.release.maintenance=true").run(context -> {
                assertThat(context).doesNotHaveBean(YudaoRedisMQConsumerAutoConfiguration.class);
                assertThat(context).doesNotHaveBean(RedisMessageListenerContainer.class);
                assertThat(context).doesNotHaveBean(StreamMessageListenerContainer.class);
                assertThat(context).doesNotHaveBean(ScheduledAnnotationBeanPostProcessor.class);
            });
    }
}
