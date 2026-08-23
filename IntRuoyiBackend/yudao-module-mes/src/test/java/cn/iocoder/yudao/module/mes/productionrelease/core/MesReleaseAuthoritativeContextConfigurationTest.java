package cn.iocoder.yudao.module.mes.productionrelease.core;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class MesReleaseAuthoritativeContextConfigurationTest {

    @Test
    void registersExactlyOneBlockerPortWhenNoAuthoritativeAdapterIsPresent() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                MesReleaseAuthoritativeContextConfiguration.class)) {
            String[] beanNames = context.getBeanNamesForType(MesReleaseAuthoritativeContextPort.class);

            assertEquals(1, beanNames.length);
            assertInstanceOf(MesReleaseAuthoritativeContextUnavailablePort.class,
                    context.getBean(MesReleaseAuthoritativeContextPort.class));
        }
    }
}
