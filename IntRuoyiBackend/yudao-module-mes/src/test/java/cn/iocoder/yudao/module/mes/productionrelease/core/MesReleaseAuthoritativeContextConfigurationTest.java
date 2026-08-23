package cn.iocoder.yudao.module.mes.productionrelease.core;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Test
    void preservesStructuredBlockerUntilAuthoritativeAdaptersAreWired() {
        MesReleaseAuthoritativeContextUnavailablePort port = new MesReleaseAuthoritativeContextUnavailablePort();

        MesReleaseFlowBlockerException exception = assertThrows(MesReleaseFlowBlockerException.class,
                () -> port.require(new MesReleaseFinalizationCommand().setReleaseTransactionId(42L)));

        assertEquals(MesReleaseFlowBlockerType.AUTHORITATIVE_RECEIPT_CONTEXT_REQUIRED,
                exception.getFailure().getBlockers().get(0).getBlockerType());
    }
}
