package cn.iocoder.yudao.module.mes.productionrelease.core;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Explicitly registers the integration blocker until flow 4/6/8 provide
 * their persistent authoritative context adapter.
 */
@Configuration(proxyBeanMethods = false)
public class MesReleaseAuthoritativeContextConfiguration {

    @Bean
    @ConditionalOnMissingBean(MesReleaseAuthoritativeContextPort.class)
    public MesReleaseAuthoritativeContextPort mesReleaseAuthoritativeContextUnavailablePort() {
        return new MesReleaseAuthoritativeContextUnavailablePort();
    }
}
