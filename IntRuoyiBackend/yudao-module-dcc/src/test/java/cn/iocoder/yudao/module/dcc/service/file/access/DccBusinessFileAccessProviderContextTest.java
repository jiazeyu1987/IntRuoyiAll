package cn.iocoder.yudao.module.dcc.service.file.access;

import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileAccessLogMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileAccessEventMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileTemporaryFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileWatermarkTraceMapper;
import cn.iocoder.yudao.module.dcc.service.audit.DccControlledFileAccessAuditService;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileQueryService;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.stereotype.Service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DccBusinessFileAccessProviderContextTest {

    private final ApplicationContextRunner providerContext = new ApplicationContextRunner()
            .withBean(DccControlledFileQueryService.class,
                    () -> mock(DccControlledFileQueryService.class))
            .withBean(DccControlledFileMapper.class,
                    () -> mock(DccControlledFileMapper.class))
            .withBean(DccControlledFileTemporaryFileMapper.class,
                    () -> mock(DccControlledFileTemporaryFileMapper.class))
            .withBean(DccControlledFileAccessLogMapper.class,
                    () -> mock(DccControlledFileAccessLogMapper.class))
            .withBean(DccControlledFileAccessEventMapper.class,
                    () -> mock(DccControlledFileAccessEventMapper.class))
            .withBean(DccControlledFileWatermarkTraceMapper.class,
                    () -> mock(DccControlledFileWatermarkTraceMapper.class))
            .withBean(DccControlledFileAccessAuditService.class,
                    () -> mock(DccControlledFileAccessAuditService.class));

    @Test
    void dccProviderIsARequiredSpringService() {
        Class<?> providerClass = assertDoesNotThrow(() -> Class.forName(
                "cn.iocoder.yudao.module.dcc.service.file.access.DccBusinessFileAccessProvider"));

        assertNotNull(providerClass.getAnnotation(Service.class));
    }

    @Test
    void springContextRegistersExactlyTheDccProvider() {
        providerContext
                .withUserConfiguration(DccBusinessFileAccessProvider.class,
                        DccBusinessFileAccessProviderPresenceGuard.class)
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    var providers = context.getBeansOfType(BusinessFileAccessProvider.class);
                    assertEquals(1, providers.size());
                    assertInstanceOf(DccBusinessFileAccessProvider.class,
                            providers.values().iterator().next());
                });
    }

    @Test
    void springContextFailsWhenDccProviderIsMissing() {
        providerContext
                .withUserConfiguration(DccBusinessFileAccessProviderPresenceGuard.class)
                .run(context -> {
                    assertNotNull(context.getStartupFailure());
                    assertTrue(rootMessage(context.getStartupFailure())
                            .contains("exactly one DCC business file access provider is required"));
                });
    }

    @Test
    void springContextFailsWhenDccProviderIsDuplicated() {
        BusinessFileAccessProvider first = mock(BusinessFileAccessProvider.class);
        BusinessFileAccessProvider second = mock(BusinessFileAccessProvider.class);
        when(first.providerId()).thenReturn(DccBusinessFileAccessProvider.PROVIDER_ID);
        when(second.providerId()).thenReturn(DccBusinessFileAccessProvider.PROVIDER_ID);

        providerContext
                .withBean("firstDccProvider", BusinessFileAccessProvider.class, () -> first)
                .withBean("secondDccProvider", BusinessFileAccessProvider.class, () -> second)
                .withUserConfiguration(DccBusinessFileAccessProviderPresenceGuard.class)
                .run(context -> {
                    assertNotNull(context.getStartupFailure());
                    assertTrue(rootMessage(context.getStartupFailure())
                            .contains("exactly one DCC business file access provider is required"));
                });
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return String.valueOf(current.getMessage());
    }
}
