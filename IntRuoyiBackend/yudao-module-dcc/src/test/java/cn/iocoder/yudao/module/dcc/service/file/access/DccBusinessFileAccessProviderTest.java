package cn.iocoder.yudao.module.dcc.service.file.access;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileTemporaryFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileTemporaryFileMapper;
import cn.iocoder.yudao.module.dcc.service.audit.DccControlledFileAccessAuditService;
import cn.iocoder.yudao.module.dcc.service.audit.DccDirectLinkDeniedLogCreateCommand;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileArtifactReference;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileArtifactRole;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileQueryService;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileScope;
import cn.iocoder.yudao.module.dcc.service.file.DccOnlyOfficePreviewTokenService;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessOperation;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessReference;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccBusinessFileAccessProviderTest extends BaseMockitoUnitTest {

    private static final Long FILE_ID = 700L;
    private static final Long CONTROLLED_FILE_ID = 900L;
    private static final Long TENANT_ID = 122L;

    @Mock
    private DccControlledFileQueryService controlledFileQueryService;
    @Mock
    private DccControlledFileMapper controlledFileMapper;
    @Mock
    private DccControlledFileAccessAuditService accessAuditService;
    @Mock
    private DccControlledFileTemporaryFileMapper temporaryFileMapper;

    @InjectMocks
    private DccBusinessFileAccessProvider provider;

    @BeforeEach
    void defaultNoTemporaryReference() {
        lenient().when(temporaryFileMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void resolve_whenNoFormalReference_reportsOrdinary() {
        when(controlledFileQueryService.identifyControlledFileScope(FILE_ID))
                .thenReturn(new DccControlledFileScope(FILE_ID, List.of()));

        Optional<BusinessFileAccessReference> result = assertDoesNotThrow(() -> provider.resolve(FILE_ID));

        assertTrue(result.isEmpty());
        verify(controlledFileMapper, never()).selectById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void resolve_usesFormalReferenceAndPersistedVersion() {
        when(controlledFileQueryService.identifyControlledFileScope(FILE_ID))
                .thenReturn(new DccControlledFileScope(FILE_ID, List.of(
                        artifact(DccControlledFileArtifactRole.PUBLISHED),
                        artifact(DccControlledFileArtifactRole.STAMPED))));
        when(controlledFileMapper.selectById(CONTROLLED_FILE_ID)).thenReturn(controlledFile());

        BusinessFileAccessReference result = assertDoesNotThrow(() -> provider.resolve(FILE_ID)).orElseThrow();

        assertEquals("dcc", result.providerId());
        assertEquals("DCC_CONTROLLED_FILE", result.businessType());
        assertEquals(CONTROLLED_FILE_ID, result.businessId());
        assertEquals("V2.1", result.versionKey());
        assertEquals(TENANT_ID, result.tenantId());
    }

    @Test
    void resolve_usesTenantNeutralLookupAndRestoresCallerContext() {
        TenantContextHolder.setTenantId(999L);
        TenantContextHolder.setIgnore(false);
        when(controlledFileQueryService.identifyControlledFileScope(FILE_ID)).thenAnswer(invocation -> {
            assertTrue(TenantContextHolder.isIgnore());
            return new DccControlledFileScope(FILE_ID, List.of(
                    artifact(DccControlledFileArtifactRole.PUBLISHED)));
        });
        when(controlledFileMapper.selectById(CONTROLLED_FILE_ID)).thenAnswer(invocation -> {
            assertTrue(TenantContextHolder.isIgnore());
            return controlledFile();
        });

        BusinessFileAccessReference result = provider.resolve(FILE_ID).orElseThrow();

        assertEquals(CONTROLLED_FILE_ID, result.businessId());
        assertEquals(999L, TenantContextHolder.getRequiredTenantId());
        assertTrue(!TenantContextHolder.isIgnore());
    }

    @Test
    void resolve_whenNoControlledArtifact_usesFormalTemporaryUploadReference() {
        when(controlledFileQueryService.identifyControlledFileScope(FILE_ID))
                .thenReturn(new DccControlledFileScope(FILE_ID, List.of()));
        when(temporaryFileMapper.selectList(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(temporaryFile()));

        BusinessFileAccessReference result = provider.resolve(FILE_ID).orElseThrow();

        assertEquals("DCC_TEMPORARY_UPLOAD", result.businessType());
        assertEquals(501L, result.businessId());
        assertEquals("TEMP-501", result.versionKey());
        assertEquals(TENANT_ID, result.tenantId());
    }

    @Test
    void resolve_rejectsReferencesToDifferentBusinessObjects() {
        when(controlledFileQueryService.identifyControlledFileScope(FILE_ID))
                .thenReturn(new DccControlledFileScope(FILE_ID, List.of(
                        artifact(DccControlledFileArtifactRole.PUBLISHED),
                        new DccControlledFileArtifactReference(901L, TENANT_ID,
                                DccControlledFileArtifactRole.EXTERNAL_REVIEW_OUTPUT))));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> provider.resolve(FILE_ID));

        assertTrue(exception.getMessage().contains("ambiguous DCC formal references"), exception.getMessage());
        verify(controlledFileMapper, never()).selectById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void supportsExactlyTheUnifiedOperations() {
        Set<BusinessFileAccessOperation> supported = Arrays.stream(BusinessFileAccessOperation.values())
                .filter(provider::supports)
                .collect(java.util.stream.Collectors.toSet());

        assertEquals(Set.of(BusinessFileAccessOperation.DIRECT_LINK, BusinessFileAccessOperation.PREVIEW,
                BusinessFileAccessOperation.ONLYOFFICE_PREVIEW, BusinessFileAccessOperation.CONVERT,
                BusinessFileAccessOperation.PRINT, BusinessFileAccessOperation.DOWNLOAD), supported);
    }

    @Test
    void directLink_recordsSanitizedDenialForEveryFormalRole() {
        when(controlledFileQueryService.identifyControlledFileScope(FILE_ID))
                .thenReturn(new DccControlledFileScope(FILE_ID, List.of(
                        artifact(DccControlledFileArtifactRole.PUBLISHED))));
        BusinessFileAccessRequest request = BusinessFileAccessRequest.publicDirectLink(
                FILE_ID, "REQ-DIRECT-1", "10.0.0.7", "JUnit");

        assertDoesNotThrow(() -> provider.assertAllowed(request, businessReference()));

        verify(accessAuditService).recordDirectLinkDeniedLog(org.mockito.ArgumentMatchers.argThat(command ->
                TENANT_ID.equals(command.tenantId())
                        && CONTROLLED_FILE_ID.equals(command.controlledFileId())
                        && FILE_ID.equals(command.infraFileId())
                        && "PUBLISHED".equals(command.artifactRole())
                        && "DIRECT_LINK".equals(command.actionType())
                        && "DENIED".equals(command.result())
                        && "REQ-DIRECT-1".equals(command.requestId())
                        && "10.0.0.7".equals(command.sourceIp())
                        && "JUnit".equals(command.userAgent())
                        && !command.reason().contains("token")));
    }

    @Test
    void preview_delegatesObjectAuthorizationWithAuditContext() throws NoSuchMethodException {
        assertDoesNotThrow(() -> DccControlledFileQueryService.class.getMethod("assertBusinessFileAccess",
                Long.class, Long.class, BusinessFileAccessOperation.class,
                cn.iocoder.yudao.module.dcc.service.file.DccRequestAuditContext.class));
        BusinessFileAccessRequest request = new BusinessFileAccessRequest(BusinessFileAccessOperation.PREVIEW,
                FILE_ID, TENANT_ID, 99L, null, "REQ-PREVIEW-1", businessReference(), "10.0.0.8", "JUnit");

        assertDoesNotThrow(() -> provider.assertAllowed(request, businessReference()));

        assertTrue(mockingDetails(controlledFileQueryService).getInvocations().stream().anyMatch(invocation ->
                invocation.getMethod().getName().equals("assertBusinessFileAccess")
                        && Arrays.equals(invocation.getArguments(), new Object[]{99L, CONTROLLED_FILE_ID,
                        BusinessFileAccessOperation.PREVIEW,
                        new cn.iocoder.yudao.module.dcc.service.file.DccRequestAuditContext(
                                "10.0.0.8", "JUnit", "REQ-PREVIEW-1")})),
                "provider must delegate the exact object, operation, subject and audit context");
    }

    @Test
    void conversion_allowsOnlyTheFixedServiceIdentityWithoutUserAuthorizationFallback() {
        BusinessFileAccessRequest allowed = new BusinessFileAccessRequest(BusinessFileAccessOperation.CONVERT,
                FILE_ID, TENANT_ID, null, DccOnlyOfficePreviewTokenService.SERVICE_DCC_PDF_CONVERSION,
                "REQ-CONVERT-1", businessReference(), null, null);

        assertDoesNotThrow(() -> provider.assertAllowed(allowed, businessReference()));
        verify(controlledFileQueryService, never()).assertBusinessFileAccess(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

        BusinessFileAccessRequest wrongService = new BusinessFileAccessRequest(BusinessFileAccessOperation.CONVERT,
                FILE_ID, TENANT_ID, null, "OTHER_SERVICE", "REQ-CONVERT-2", businessReference(), null, null);
        assertThrows(IllegalArgumentException.class,
                () -> provider.assertAllowed(wrongService, businessReference()));
    }

    @Test
    void temporaryUpload_allowsOnlyTheOwnerWhileTheReferenceIsActive() {
        DccControlledFileTemporaryFileDO temporaryFile = temporaryFile();
        when(temporaryFileMapper.selectById(501L)).thenReturn(temporaryFile);
        BusinessFileAccessReference reference = temporaryReference();
        BusinessFileAccessRequest ownerRequest = new BusinessFileAccessRequest(
                BusinessFileAccessOperation.ONLYOFFICE_PREVIEW, FILE_ID, TENANT_ID, 99L, null,
                "REQ-TEMP-OWNER", reference, null, null);

        assertDoesNotThrow(() -> provider.assertAllowed(ownerRequest, reference));

        BusinessFileAccessRequest otherUserRequest = new BusinessFileAccessRequest(
                BusinessFileAccessOperation.ONLYOFFICE_PREVIEW, FILE_ID, TENANT_ID, 100L, null,
                "REQ-TEMP-OTHER", reference, null, null);
        assertThrows(IllegalArgumentException.class,
                () -> provider.assertAllowed(otherUserRequest, reference));

        temporaryFile.setExpireTime(LocalDateTime.now().minusSeconds(1));
        assertThrows(IllegalArgumentException.class,
                () -> provider.assertAllowed(ownerRequest, reference));
    }

    private DccControlledFileArtifactReference artifact(DccControlledFileArtifactRole role) {
        return new DccControlledFileArtifactReference(CONTROLLED_FILE_ID, TENANT_ID, role);
    }

    private DccControlledFileDO controlledFile() {
        return DccControlledFileDO.builder()
                .id(CONTROLLED_FILE_ID)
                .tenantId(TENANT_ID)
                .versionNo("V2.1")
                .build();
    }

    private BusinessFileAccessReference businessReference() {
        return new BusinessFileAccessReference("dcc", "DCC_CONTROLLED_FILE", CONTROLLED_FILE_ID,
                "V2.1", TENANT_ID, null);
    }

    private BusinessFileAccessReference temporaryReference() {
        return new BusinessFileAccessReference("dcc", "DCC_TEMPORARY_UPLOAD", 501L,
                "TEMP-501", TENANT_ID, null);
    }

    private DccControlledFileTemporaryFileDO temporaryFile() {
        DccControlledFileTemporaryFileDO temporaryFile = DccControlledFileTemporaryFileDO.builder()
                .id(501L)
                .uploaderId(99L)
                .storageFileId(FILE_ID)
                .status("AVAILABLE")
                .cleanupStatus("ACTIVE")
                .expireTime(LocalDateTime.now().plusMinutes(10))
                .build();
        temporaryFile.setTenantId(TENANT_ID);
        return temporaryFile;
    }
}
