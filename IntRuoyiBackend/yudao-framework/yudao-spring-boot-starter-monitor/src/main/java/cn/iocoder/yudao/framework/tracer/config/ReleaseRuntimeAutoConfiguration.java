package cn.iocoder.yudao.framework.tracer.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

/** Read-only evidence for the release executor; no startup dependency on release metadata. */
@AutoConfiguration
public class ReleaseRuntimeAutoConfiguration {
    @Bean
    public ReleaseRuntimeEndpoint releaseRuntimeEndpoint(ConfigurableApplicationContext context) {
        return new ReleaseRuntimeEndpoint(context);
    }
}
