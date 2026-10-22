package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessOperation;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessReference;
import cn.iocoder.yudao.module.dcc.service.token.DccViewerTokenExpectedContext;
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

    @Test void g07_exactBodyContextIsSignedAndCannotBeReplayedWithAnotherPurposeOrFile() {
        var context=new DccControlledFileBodyReadContext(DccControlledFileBodyReadContext.Intent.APPROVAL_HISTORY,
                "990","V1.0","REVISION","old-round","old-task");
        var issued=tokenService.issueControlledFile(1L,99L,990L,"V1.0",91L,context.purpose(),300L,7001L,DCC_REFERENCE,context);
        var decoded=tokenService.verifyControlledFile(issued.token(),990L);
        assertEquals(context.wire(),decoded.getBodyReadContext());
        assertEquals(context.purpose(),decoded.getPurpose());
        assertThrows(RuntimeException.class,()->tokenService.verifyControlledFile(issued.token(),991L));
        var payloadPart=issued.token().substring(0,issued.token().indexOf('.'));
        var json=new String(java.util.Base64.getUrlDecoder().decode(payloadPart),java.nio.charset.StandardCharsets.UTF_8);
        var changed=java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(json.replace(context.wire(),DccControlledFileBodyReadContext.daily(990L).wire()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThrows(RuntimeException.class,()->tokenService.verifyControlledFile(changed+issued.token().substring(issued.token().indexOf('.')),990L));
        assertThrows(IllegalArgumentException.class,()->tokenService.issueControlledFile(1L,99L,990L,"V1.0",91L,"CONTROLLED_PREVIEW",300L,7001L,DCC_REFERENCE,context));
        assertThrows(IllegalArgumentException.class,()->tokenService.issueControlledFile(1L,99L,990L,"V1.0",91L,context.purpose(),300L,7001L,DCC_REFERENCE));
    }

    @Test void exactTaskPurposeUsesSignedFullIdentityAndRejectsAnotherTask() {
        var issued=tokenService.issueControlledFile(1L,99L,990L,"V1.0",30L,"DIST_PREVIEW:10:9007199254740993",300L,7001L,DCC_REFERENCE);
        var payload=tokenService.verifyControlledFile(issued.token(),990L);
        assertEquals("DIST_PREVIEW:10:9007199254740993",payload.getPurpose());
        assertEquals(DCC_REFERENCE,payload.toBusinessFileReference());
        var expected=new DccViewerTokenExpectedContext(1L,99L,990L,"V1.0",30L,payload.getPurpose(),300L,payload.getNonce(),payload.getTokenId());
        assertDoesNotThrow(()->tokenService.verifyControlledFile(issued.token(),expected));
        assertThrows(RuntimeException.class,()->tokenService.verifyControlledFile(issued.token(),new DccViewerTokenExpectedContext(
                1L,99L,990L,"V1.0",30L,"DIST_PREVIEW:10:11",300L,payload.getNonce(),payload.getTokenId())));
        TenantContextHolder.setTenantId(2L);
        assertThrows(RuntimeException.class,()->tokenService.verifyControlledFile(issued.token(),990L));
    }

    @Test void controlledIssuerRejectsArbitraryAndNoncanonicalTaskPurpose() {
        for(var purpose:java.util.List.of("AUTO","CONTROLLED_PREVIEW:10","DIST_PREVIEW:01:11","DIST_PREVIEW:10:0",
                "DIST_PREVIEW:10:11:12","DIST_PREVIEW:10:9223372036854775808")) {
            assertThrows(IllegalArgumentException.class,()->tokenService.issueControlledFile(1L,99L,990L,"V1.0",30L,purpose,300L,7001L,DCC_REFERENCE));
        }
        assertThrows(IllegalArgumentException.class,()->tokenService.issueControlledFile(1L,99L,990L,"V1.0",30L,"DIST_PREVIEW:10:11",300L,7001L,
                new BusinessFileAccessReference("other","DCC_CONTROLLED_FILE",990L,"V1.0",1L,null)));
    }

    @Test void normalControlledPurposeStillVerifiesAndCannotReuseBusinessOrUploadToken() {
        var issued=tokenService.issueControlledFile(1L,99L,990L,"V1.0",30L,DccOnlyOfficePreviewTokenService.PURPOSE_CONTROLLED_PREVIEW,300L,7001L,DCC_REFERENCE);
        assertEquals(DccOnlyOfficePreviewTokenService.PURPOSE_CONTROLLED_PREVIEW,tokenService.verifyControlledFile(issued.token(),990L).getPurpose());
        var business=tokenService.issueBusinessFile(DccOnlyOfficePreviewTokenService.AUDIENCE_UPLOAD_PREVIEW,
                BusinessFileAccessOperation.CONVERT,7001L,1L,null,DccOnlyOfficePreviewTokenService.SERVICE_DCC_PDF_CONVERSION,DCC_REFERENCE,300L);
        assertThrows(RuntimeException.class,()->tokenService.verifyControlledFile(business.token(),990L));
        assertThrows(RuntimeException.class,()->tokenService.verifyControlledFile("x"+issued.token().substring(1),990L));
    }
}
