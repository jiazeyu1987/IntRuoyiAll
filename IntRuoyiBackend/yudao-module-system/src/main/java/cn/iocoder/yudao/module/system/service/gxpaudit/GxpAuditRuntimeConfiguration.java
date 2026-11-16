package cn.iocoder.yudao.module.system.service.gxpaudit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
public class GxpAuditRuntimeConfiguration {
    @Bean
    @Lazy(false)
    public GxpLocalDevelopmentPolicy gxpLocalDevelopmentPolicy(GxpAuditDevelopmentMode mode,
            Environment environment, GxpAuditPolicyBundleLoader loader) {
        // A null factory result deliberately means no local policy in formal or disabled mode.
        return mode.localPolicyEnabled() ? new GxpLocalDevelopmentPolicy(environment, loader) : null;
    }
}
