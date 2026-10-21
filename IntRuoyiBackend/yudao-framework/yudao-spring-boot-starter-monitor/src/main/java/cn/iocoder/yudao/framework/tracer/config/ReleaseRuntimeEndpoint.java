package cn.iocoder.yudao.framework.tracer.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.Lifecycle;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.util.ClassUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Attests actual runtime objects, never treats missing metadata or failed inspection as success. */
@Endpoint(id = "release")
public class ReleaseRuntimeEndpoint {
    private final ConfigurableApplicationContext context;

    public ReleaseRuntimeEndpoint(ConfigurableApplicationContext context) {
        this.context = context;
    }

    @ReadOperation
    public Map<String, Object> release() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "unavailable");
        result.put("releaseTag", null);
        result.put("maintenance", context.getEnvironment().getProperty("intruoyi.release.maintenance", Boolean.class, false));
        result.put("schedulingDisabled", false);
        result.put("consumersDisabled", false);
        try {
            Map<String, ScheduledAnnotationBeanPostProcessor> processors = context.getBeansOfType(ScheduledAnnotationBeanPostProcessor.class);
            int tasks = processors.values().stream().mapToInt(p -> p.getScheduledTasks().size()).sum();
            result.put("scheduledProcessors", processors.size());
            result.put("scheduledTaskCount", tasks);
            List<Map<String, Object>> schedulers = new ArrayList<>();
            for (Map.Entry<String, ?> entry : beans("org.quartz.Scheduler").entrySet()) {
                Object scheduler = entry.getValue();
                boolean started = (Boolean) scheduler.getClass().getMethod("isStarted").invoke(scheduler);
                boolean standby = (Boolean) scheduler.getClass().getMethod("isInStandbyMode").invoke(scheduler);
                schedulers.add(Map.of("bean", entry.getKey(), "started", started, "standby", standby));
            }
            result.put("quartz", schedulers);
            result.put("schedulingDisabled", processors.isEmpty() && tasks == 0
                    && schedulers.stream().noneMatch(s -> Boolean.TRUE.equals(s.get("started"))));
            List<Map<String, Object>> consumers = new ArrayList<>();
            for (String type : List.of(
                    "org.springframework.data.redis.listener.RedisMessageListenerContainer",
                    "org.springframework.data.redis.stream.StreamMessageListenerContainer",
                    "org.apache.rocketmq.spring.support.DefaultRocketMQListenerContainer")) {
                for (Map.Entry<String, ?> entry : beans(type).entrySet()) {
                    consumers.add(consumer(entry.getKey(), entry.getValue()));
                }
            }
            for (String type : List.of("org.springframework.kafka.config.KafkaListenerEndpointRegistry",
                    "org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry")) {
                for (Map.Entry<String, ?> entry : beans(type).entrySet()) {
                    Collection<?> containers = (Collection<?>) entry.getValue().getClass()
                            .getMethod("getListenerContainers").invoke(entry.getValue());
                    for (Object container : containers) consumers.add(consumer(entry.getKey(), container));
                }
            }
            result.put("consumerContainers", consumers);
            result.put("consumersDisabled", consumers.stream().noneMatch(c -> Boolean.TRUE.equals(c.get("running"))));
            Path infoPath = Path.of(context.getEnvironment().getProperty("intruoyi.release.info-path", "/yudao-server/release-info.json"));
            JsonNode info = new ObjectMapper().readTree(Files.readString(infoPath));
            JsonNode tag = info == null ? null : info.get("releaseTag");
            if (tag == null || !tag.isTextual() || !tag.textValue().matches("\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2}")) {
                throw new IllegalStateException("release-info.json contains no valid releaseTag");
            }
            result.put("releaseTag", tag.textValue());
            result.put("status", "available");
        } catch (Exception error) {
            result.put("status", "unavailable");
            result.put("error", error.getClass().getSimpleName() + ": " + error.getMessage());
        }
        return result;
    }

    private Map<String, ?> beans(String type) throws ClassNotFoundException {
        if (!ClassUtils.isPresent(type, context.getClassLoader())) return Map.of();
        return context.getBeansOfType(ClassUtils.forName(type, context.getClassLoader()));
    }

    private Map<String, Object> consumer(String name, Object container) throws ReflectiveOperationException {
        // Stream and RocketMQ interfaces differ across dependencies; inspect the real public running contract.
        boolean running = container instanceof Lifecycle ? ((Lifecycle) container).isRunning()
                : (Boolean) container.getClass().getMethod("isRunning").invoke(container);
        return Map.of("bean", name, "type", container.getClass().getName(), "running", running);
    }
}
