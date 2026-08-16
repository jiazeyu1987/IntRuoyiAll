package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessOperation;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DccOnlyOfficePreviewTokenServiceTest extends BaseMockitoUnitTest {

    private static final BusinessFileAccessReference DCC_REFERENCE = new BusinessFileAccessReference(
            "dcc", "DCC_CONTROLLED_FILE", 990L, "V1.0", 1L, null);

    @InjectMocks
    private DccOnlyOfficePreviewTokenService tokenService;

    @BeforeEach
    void configureTokenService() {
        DccOnlyOfficePreviewProperties properties = new DccOnlyOfficePreviewProperties();
        properties.setBaseUrl("http://onlyoffice.local");
        properties.setJwtSecret("secret-demo");
        properties.setPublicFileBaseUrl("http://127.0.0.1:48081");
        ReflectionTestUtils.setField(tokenService, "properties", properties);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void userTokenBindsTenantSubjectBusinessObjectInfraFileOperationAndExpiry() {
        DccOnlyOfficePreviewTokenService.IssuedPreviewToken issued = tokenService.issueBusinessFile(
                DccOnlyOfficePreviewTokenService.AUDIENCE_ONLINE_FILE_PREVIEW,
                BusinessFileAccessOperation.ONLYOFFICE_PREVIEW, 7001L, 1L, 99L, null,
                DCC_REFERENCE, 300L);

        DccOnlyOfficePreviewTokenService.PreviewTokenPayload payload = tokenService.verifyBusinessFile(
                issued.token(), DccOnlyOfficePreviewTokenService.AUDIENCE_ONLINE_FILE_PREVIEW,
                BusinessFileAccessOperation.ONLYOFFICE_PREVIEW, 7001L);

        assertEquals(1L, payload.getTenantId());
        assertEquals(99L, payload.getUserId());
        assertNull(payload.getServiceIdentity());
        assertEquals("ONLYOFFICE_PREVIEW", payload.getOperation());
        assertEquals(7001L, payload.getInfraFileId());
        assertEquals(DCC_REFERENCE, payload.toBusinessFileReference());
        assertEquals(300L, payload.getTtlSeconds());
        assertThrows(RuntimeException.class, () -> tokenService.verifyBusinessFile(
                issued.token(), DccOnlyOfficePreviewTokenService.AUDIENCE_ONLINE_FILE_PREVIEW,
                BusinessFileAccessOperation.CONVERT, 7001L));
        assertThrows(RuntimeException.class, () -> tokenService.verifyBusinessFile(
                issued.token(), DccOnlyOfficePreviewTokenService.AUDIENCE_ONLINE_FILE_PREVIEW,
                BusinessFileAccessOperation.ONLYOFFICE_PREVIEW, 7002L));
        assertThrows(RuntimeException.class, () -> tokenService.verifyBusinessFile(
                issued.token(), DccOnlyOfficePreviewTokenService.AUDIENCE_UPLOAD_PREVIEW,
                BusinessFileAccessOperation.ONLYOFFICE_PREVIEW, 7001L));

        TenantContextHolder.setTenantId(2L);
        assertThrows(RuntimeException.class, () -> tokenService.verifyBusinessFile(
                issued.token(), DccOnlyOfficePreviewTokenService.AUDIENCE_ONLINE_FILE_PREVIEW,
                BusinessFileAccessOperation.ONLYOFFICE_PREVIEW, 7001L));
    }

    @Test
    void serviceTokenBindsExactlyOneServiceSubject() {
        DccOnlyOfficePreviewTokenService.IssuedPreviewToken issued = tokenService.issueBusinessFile(
                DccOnlyOfficePreviewTokenService.AUDIENCE_UPLOAD_PREVIEW,
                BusinessFileAccessOperation.CONVERT, 7001L, 1L, null,
                DccOnlyOfficePreviewTokenService.SERVICE_DCC_PDF_CONVERSION, DCC_REFERENCE, 300L);

        DccOnlyOfficePreviewTokenService.PreviewTokenPayload payload = tokenService.verifyBusinessFile(
                issued.token(), DccOnlyOfficePreviewTokenService.AUDIENCE_UPLOAD_PREVIEW,
                BusinessFileAccessOperation.CONVERT, 7001L);

        assertNull(payload.getUserId());
        assertEquals(DccOnlyOfficePreviewTokenService.SERVICE_DCC_PDF_CONVERSION,
                payload.getServiceIdentity());
    }

    @Test
    void ignoredCallbackContextDoesNotOverrideTheSignedTenant() {
        DccOnlyOfficePreviewTokenService.IssuedPreviewToken issued = tokenService.issueBusinessFile(
                DccOnlyOfficePreviewTokenService.AUDIENCE_ONLINE_FILE_PREVIEW,
                BusinessFileAccessOperation.ONLYOFFICE_PREVIEW, 7001L, 1L, 99L, null,
                DCC_REFERENCE, 300L);
        TenantContextHolder.setTenantId(2L);
        TenantContextHolder.setIgnore(true);

        DccOnlyOfficePreviewTokenService.PreviewTokenPayload payload = assertDoesNotThrow(
                () -> tokenService.verifyBusinessFile(issued.token(),
                        DccOnlyOfficePreviewTokenService.AUDIENCE_ONLINE_FILE_PREVIEW,
                        BusinessFileAccessOperation.ONLYOFFICE_PREVIEW, 7001L));

        assertEquals(1L, payload.getTenantId());
        assertEquals(2L, TenantContextHolder.getRequiredTenantId());
        assertEquals(true, TenantContextHolder.isIgnore());
    }

    @Test
    void tokenIssueRejectsMissingOrAmbiguousSubjectAndTenantMismatchedClaim() {
        assertThrows(IllegalArgumentException.class, () -> tokenService.issueBusinessFile(
                DccOnlyOfficePreviewTokenService.AUDIENCE_ONLINE_FILE_PREVIEW,
                BusinessFileAccessOperation.ONLYOFFICE_PREVIEW, 7001L, 1L,
                null, null, DCC_REFERENCE, 300L));
        assertThrows(IllegalArgumentException.class, () -> tokenService.issueBusinessFile(
                DccOnlyOfficePreviewTokenService.AUDIENCE_ONLINE_FILE_PREVIEW,
                BusinessFileAccessOperation.ONLYOFFICE_PREVIEW, 7001L, 1L,
                99L, "service", DCC_REFERENCE, 300L));
        assertThrows(IllegalArgumentException.class, () -> tokenService.issueBusinessFile(
                DccOnlyOfficePreviewTokenService.AUDIENCE_ONLINE_FILE_PREVIEW,
                BusinessFileAccessOperation.ONLYOFFICE_PREVIEW, 7001L, 2L,
                99L, null, DCC_REFERENCE, 300L));
    }
}
