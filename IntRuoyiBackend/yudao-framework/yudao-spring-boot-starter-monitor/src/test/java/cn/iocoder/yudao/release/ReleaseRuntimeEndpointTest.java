package cn.iocoder.yudao.release;
import cn.iocoder.yudao.framework.tracer.config.ReleaseRuntimeAutoConfiguration;
import cn.iocoder.yudao.framework.tracer.config.ReleaseRuntimeEndpoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.nio.file.Path;
import java.nio.file.Files;
import static org.assertj.core.api.Assertions.assertThat;
public class ReleaseRuntimeEndpointTest {
    @TempDir Path temp;
    private ApplicationContextRunner runner(Path info) {
        return new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(ReleaseRuntimeAutoConfiguration.class))
            .withPropertyValues("intruoyi.release.maintenance=true", "intruoyi.release.info-path=" + info);
    }
    @Test void missingMetadataNeverReportsSuccessAndDoesNotBlockContextStartup() {
        runner(temp.resolve("missing.json")).run(context -> {
            assertThat(context).hasNotFailed();
            var result = context.getBean(ReleaseRuntimeEndpoint.class).release();
            assertThat(result.get("status")).isEqualTo("unavailable");
            assertThat(result.get("releaseTag")).isNull();
        });
    }
    @Test void readsRealTagAndInspectsActualProcessorsRatherThanMaintenanceFlag() throws Exception {
        Path info = temp.resolve("release-info.json");
        Files.writeString(info, "{\"releaseTag\":\"2026-10-21-12-34-56\"}");
        runner(info).withBean("gxpAuditDevelopmentMode", ModeState.class, ModeState::new).run(context -> {
            var result = context.getBean(ReleaseRuntimeEndpoint.class).release();
            assertThat(result.get("status")).isEqualTo("available");
            assertThat(result.get("releaseTag")).isEqualTo("2026-10-21-12-34-56");
            assertThat(result.get("schedulingDisabled")).isEqualTo(true);
            assertThat(result.get("consumersDisabled")).isEqualTo(true);
            assertThat(result.get("gxpAuditMode")).isEqualTo("DEVELOPMENT_DISABLED");
            assertThat(result.get("configuredGxpAuditMode")).isEqualTo("FORMAL");
        });
        runner(info).run(context -> assertThat(context.getBean(ReleaseRuntimeEndpoint.class).release().get("status"))
                .isEqualTo("unavailable"));
        runner(info).withUserConfiguration(UnexpectedScheduling.class).run(context -> {
            var result = context.getBean(ReleaseRuntimeEndpoint.class).release();
            assertThat(result.get("schedulingDisabled")).isEqualTo(false);
        });
    }
    @Test void invalidMetadataNeverReportsSuccess() throws Exception {
        Path info = temp.resolve("bad.json");
        Files.writeString(info, "{\"releaseTag\":\"\"}");
        runner(info).run(context -> assertThat(context.getBean(ReleaseRuntimeEndpoint.class).release().get("status")).isEqualTo("unavailable"));
    }
    @Configuration(proxyBeanMethods = false) @EnableScheduling static class UnexpectedScheduling {}
    public static class ModeState {
        public java.util.Map<String, String> releaseState() {
            return java.util.Map.of("gxpAuditMode", "DEVELOPMENT_DISABLED", "configuredGxpAuditMode", "FORMAL");
        }
    }
}
