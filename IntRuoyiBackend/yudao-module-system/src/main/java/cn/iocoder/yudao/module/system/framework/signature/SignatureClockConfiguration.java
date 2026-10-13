package cn.iocoder.yudao.module.system.framework.signature;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class SignatureClockConfiguration {
    @Bean public Clock signatureClock() { return Clock.systemUTC(); }
}
