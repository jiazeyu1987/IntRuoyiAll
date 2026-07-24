package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DccOnlyOfficePreviewTokenServiceTest extends BaseMockitoUnitTest {

    @InjectMocks
    private DccOnlyOfficePreviewTokenService tokenService;

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void issueShouldBindTokenToCurrentTenantAndRejectOtherTenantContext() {
        DccOnlyOfficePreviewProperties properties = new DccOnlyOfficePreviewProperties();
        properties.setBaseUrl("http://onlyoffice.local");
        properties.setJwtSecret("secret-demo");
        properties.setPublicFileBaseUrl("http://127.0.0.1:48081");
        ReflectionTestUtils.setField(tokenService, "properties", properties);

        TenantContextHolder.setTenantId(1L);
        String token = tokenService.issue(DccOnlyOfficePreviewTokenService.RESOURCE_CONTROLLED_FILE, 990L);

        TenantContextHolder.setTenantId(2L);
        assertThrows(IllegalStateException.class,
                () -> tokenService.verify(token, DccOnlyOfficePreviewTokenService.RESOURCE_CONTROLLED_FILE, 990L));

        TenantContextHolder.clear();
        DccOnlyOfficePreviewTokenService.PreviewTokenPayload payload =
                tokenService.verify(token, DccOnlyOfficePreviewTokenService.RESOURCE_CONTROLLED_FILE, 990L);
        assertEquals(1L, payload.getTenantId());
    }
}
