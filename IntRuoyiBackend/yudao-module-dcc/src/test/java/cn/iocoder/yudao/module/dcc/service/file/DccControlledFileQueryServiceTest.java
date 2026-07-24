package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledPreviewWatermarkOverlayRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledPreviewWatermarkRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePreviewMetadataRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileUploadDirectoryTreeRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileUploadRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileVersionHistoryRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccExternalFileReviewRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccCategoryDirectoryBindingDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccFileDirectoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileAccessLogDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDistributionDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDistributionRecipientDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRouteSnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTrainingAssignmentDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTrainingProgressDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTrainingDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccExternalFileReviewDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileAccessEventDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileDownloadRecordDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileWatermarkTraceDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccCategoryDirectoryBindingMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccFileDirectoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileAccessLogMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionRecipientMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileRouteSnapshotMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSignatureMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingAssignmentMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingProgressMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccExternalFileReviewMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileAccessEventMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileDownloadRecordMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileWatermarkTraceMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeAssignmentFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeAssignmentMapper;
import cn.iocoder.yudao.module.dcc.enums.DccAccessTypeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileProcessTypeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileTrainingStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccDistributionMediumEnum;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService;
import cn.iocoder.yudao.module.dcc.service.download.DccDownloadFileBinary;
import cn.iocoder.yudao.module.dcc.service.download.DccDownloadPolicyService;
import cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyAdminService;
import cn.iocoder.yudao.module.dcc.service.preview.DccControlledPreviewAccessService;
import cn.iocoder.yudao.module.dcc.service.preview.DccPreviewAccessRequest;
import cn.iocoder.yudao.module.dcc.service.preview.DccPreviewAccessResult;
import cn.iocoder.yudao.module.dcc.service.token.DccViewerTokenExpectedContext;
import cn.iocoder.yudao.module.dcc.service.token.DccViewerTokenPayload;
import cn.iocoder.yudao.module.dcc.service.token.DccViewerTokenService;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Objects;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACCESS_DENIED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_DOWNLOAD_WARNING_UNCONFIRMED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_DOWNLOAD_AUDIT_RECORD_FAILED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_DOWNLOAD_REQUEST_ID_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_DOWNLOAD_REQUEST_ID_REUSED;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@MockitoSettings(strictness = Strictness.LENIENT)
class DccControlledFileQueryServiceTest extends BaseMockitoUnitTest {

    private static final String VIEWER_TOKEN = "viewer-token";
    private static final String VIEWER_TOKEN_ID = "VT-20260528-0001";
    private static final String VIEWER_TOKEN_NONCE = "VN-20260528-0001";
    private static final String ACCESS_EVENT_CODE = "AE-20260528-0001";
    private static final String WATERMARK_TRACE_CODE = "WM-20260528-0001";
    private static final String DOWNLOAD_REQUEST_ID = "DR-20260528-0001";
    private static final String SOURCE_IP = "10.8.0.31";
    private static final String USER_AGENT = "Playwright-DCC-Audit/1.0";
    private static final String PREVIEW_REQUEST_ID = "REQ-PREVIEW-20260528-0001";

    private static final Set<String> ORDINARY_RESPONSE_FORBIDDEN_CAPABILITY_FIELDS = Set.of(
            "originalFileId", "sourceFileId", "drawingPdfFileId", "publishedFileId", "stampedFileId",
            "trainingRecordFileId", "outputFileId", "fileUrl", "configId", "path");

    @Mock
    private DccFileCategoryMapper categoryMapper;
    @Mock
    private DccCategoryDirectoryBindingMapper categoryDirectoryBindingMapper;
    @Mock
    private DccFileDirectoryMapper directoryMapper;
    @Mock
    private DccControlledFileMapper controlledFileMapper;
    @Mock
    private DccControlledFileRouteSnapshotMapper routeSnapshotMapper;
    @Mock
    private DccControlledFileDistributionMapper distributionMapper;
    @Mock
    private DccControlledFileDistributionRecipientMapper distributionRecipientMapper;
    @Mock
    private DccControlledFileTrainingMapper trainingMapper;
    @Mock
    private DccControlledFileTrainingAssignmentMapper trainingAssignmentMapper;
    @Mock
    private DccControlledFileTrainingProgressMapper trainingProgressMapper;
    @Mock
    private DccControlledFileSignatureMapper signatureMapper;
    @Mock
    private DccExternalFileReviewMapper externalReviewMapper;
    @Mock
    private DccControlledFileAccessLogMapper accessLogMapper;
    @Mock
    private DccControlledFileCategoryPermissionSupport permissionSupport;
    @Mock
    private DccControlledFileViewMatrixAccessService viewMatrixAccessService;
    @Mock
    private DccDirectoryAccessPermissionService directoryAccessPermissionService;
    @Mock
    private FileMapper fileMapper;
    @Mock
    private FileService fileService;
    @Mock
    private DccControlledPreviewWatermarkService watermarkService;
    @Mock
    private DccOnlyOfficePreviewTokenService onlyOfficePreviewTokenService;
    @Mock
    private DccControlledPreviewAccessService previewAccessService;
    @Mock
    private DccViewerTokenService viewerTokenService;
    @Mock
    private DccControlledFileAccessEventMapper accessEventMapper;
    @Mock
    private DccControlledFileDownloadRecordMapper downloadRecordMapper;
    @Mock
    private DccControlledFileWatermarkTraceMapper watermarkTraceMapper;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private DccControlledFileBrowserSettingsService browserSettingsService;
    @Mock
    private DccProjectCodeAssignmentMapper projectCodeAssignmentMapper;
    @Mock
    private DccProjectCodeAssignmentFileMapper projectCodeAssignmentFileMapper;
    @Mock
    private DccFileTypeTaxonomyAdminService fileTypeTaxonomyAdminService;
    @Mock
    private PermissionApi permissionApi;
    @Spy
    private DccDownloadPolicyService downloadPolicyService = new DccDownloadPolicyService();

    @InjectMocks
    private DccControlledFileQueryServiceImpl queryService;

    @BeforeEach
    void setUpDirectoryAccessDefaults() {
        TenantContextHolder.setTenantId(31L);
        lenient().when(directoryAccessPermissionService.getAuthorizedDirectoryIds(99L, DccAccessTypeEnum.QUERY))
                .thenReturn(Set.of(20L));
        lenient().when(directoryAccessPermissionService.getAuthorizedDirectoryIds(99L, DccAccessTypeEnum.PREVIEW))
                .thenReturn(Set.of(20L));
        lenient().when(directoryAccessPermissionService.getAuthorizedDirectoryIds(99L, DccAccessTypeEnum.DOWNLOAD))
                .thenReturn(Set.of(20L));
        lenient().when(browserSettingsService.getBlacklistedExtensionPatterns()).thenReturn(List.of());
        lenient().when(viewMatrixAccessService.canAccessCurrentViewMatrix(eq(99L), any(DccControlledFileDO.class)))
                .thenReturn(true);
        lenient().when(previewAccessService.prepareAccess(any(DccPreviewAccessRequest.class)))
                .thenReturn(previewAccessResult());
        lenient().when(accessEventMapper.selectOne(any())).thenAnswer(invocation -> accessEvent(900L, "1.0"));
        lenient().when(watermarkTraceMapper.selectOne(any())).thenAnswer(invocation -> watermarkTrace(900L, "1.0"));
        lenient().when(downloadRecordMapper.updateById(any(DccControlledFileDownloadRecordDO.class))).thenReturn(1);
        lenient().when(downloadRecordMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);
        lenient().when(accessLogMapper.insert(any(DccControlledFileAccessLogDO.class))).thenReturn(1);
        lenient().when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        lenient().when(viewerTokenService.verify(eq(VIEWER_TOKEN), any(DccViewerTokenExpectedContext.class)))
                .thenReturn(DccViewerTokenPayload.builder()
                        .tokenId(VIEWER_TOKEN_ID)
                        .nonce(VIEWER_TOKEN_NONCE)
                        .tenantId(31L)
                        .userId(99L)
                        .fileId(900L)
                        .versionId("1.0")
                        .accessEventId(88001L)
                        .purpose("CONTROLLED_PREVIEW")
                        .ttlSeconds(900L)
                        .issuedAtEpochSecond(1L)
                        .expiresAtEpochSecond(901L)
                        .build());
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void ordinaryResponseVoTypes_doNotExposeUnderlyingFileCapabilities() throws Exception {
        assertNoForbiddenProperties(DccControlledFileRespVO.class);
        assertNoForbiddenProperties(DccControlledFileVersionHistoryRespVO.class);
        assertNoForbiddenProperties(DccExternalFileReviewRespVO.class);
        assertNoForbiddenProperties(DccControlledFilePreviewMetadataRespVO.class);
        assertNoForbiddenProperties(DccControlledFileUploadRespVO.class);
    }

    @Test
    void identifyControlledFileScope_recognizesKnownDccArtifactsWithoutPathPrefix() {
        when(controlledFileMapper.selectList(org.mockito.ArgumentMatchers.<Wrapper<DccControlledFileDO>>any()))
                .thenReturn(List.of(DccControlledFileDO.builder()
                        .id(902L)
                        .tenantId(122L)
                        .sourceFileId(700L)
                        .originalFileId(700L)
                        .drawingPdfFileId(700L)
                        .trainingRecordFileId(700L)
                        .publishedFileId(700L)
                        .stampedFileId(700L)
                        .build()));
        when(externalReviewMapper.selectList(org.mockito.ArgumentMatchers.<Wrapper<DccExternalFileReviewDO>>any()))
                .thenReturn(List.of(DccExternalFileReviewDO.builder()
                        .tenantId(122L)
                        .controlledFileId(902L)
                        .outputFileId(700L)
                        .build()));

        DccControlledFileQueryService serviceContract = queryService;
        DccControlledFileScope scope = serviceContract.identifyControlledFileScope(700L);

        assertTrue(scope.controlled());
        assertEquals(700L, scope.infraFileId());
        assertEquals(Set.of(902L), scope.references().stream()
                .map(DccControlledFileArtifactReference::controlledFileId)
                .collect(Collectors.toSet()));
        assertEquals(Set.of(122L), scope.references().stream()
                .map(DccControlledFileArtifactReference::tenantId)
                .collect(Collectors.toSet()));
        assertEquals(Set.of(
                DccControlledFileArtifactRole.SOURCE,
                DccControlledFileArtifactRole.ORIGINAL,
                DccControlledFileArtifactRole.DRAWING_PDF,
                DccControlledFileArtifactRole.TRAINING_RECORD,
                DccControlledFileArtifactRole.PUBLISHED,
                DccControlledFileArtifactRole.STAMPED,
                DccControlledFileArtifactRole.EXTERNAL_REVIEW_OUTPUT),
                scope.references().stream()
                        .map(DccControlledFileArtifactReference::role)
                        .collect(Collectors.toSet()));
    }

    @Test
    void getUploadDirectoryTree_returnsBindingPathAndSubtree() {
        when(categoryMapper.selectById(10L)).thenReturn(DccFileCategoryDO.builder()
                .id(10L)
                .active(Boolean.TRUE)
                .build());
        when(categoryDirectoryBindingMapper.selectActiveByCategoryId(10L)).thenReturn(
                DccCategoryDirectoryBindingDO.builder().id(1L).categoryId(10L).directoryId(20L).active(Boolean.TRUE).build());
        when(directoryMapper.selectEnabledList()).thenReturn(List.of(
                directory(1L, null, "3.DMR"),
                directory(20L, 1L, "01.图纸"),
                directory(21L, 20L, "二级目录"),
                directory(22L, 21L, "三级叶子"),
                directory(30L, null, "其他根目录")));

        DccControlledFileUploadDirectoryTreeRespVO result = queryService.getUploadDirectoryTree(10L);

        assertEquals(20L, result.getBindingDirectoryId());
        assertEquals("3.DMR/01.图纸", result.getBindingDirectoryPath());
        assertFalse(Boolean.TRUE.equals(result.getLeafBinding()));
        assertEquals(1, result.getChildren().size());
        assertEquals(21L, result.getChildren().get(0).getId());
        assertEquals(1, result.getChildren().get(0).getChildren().size());
        assertEquals(22L, result.getChildren().get(0).getChildren().get(0).getId());
    }

    @Test
    void getPreviewMetadata_officeFileReturnsOnlyOfficeLink() {
        DccOnlyOfficePreviewProperties properties = new DccOnlyOfficePreviewProperties();
        properties.setBaseUrl("http://onlyoffice.local");
        properties.setJwtSecret("secret-demo");
        properties.setPublicFileBaseUrl("http://127.0.0.1:48081");
        ReflectionTestUtils.setField(queryService, "onlyOfficePreviewProperties", properties);

        when(controlledFileMapper.selectById(990L)).thenReturn(DccControlledFileDO.builder()
                .id(990L)
                .categoryId(10L)
                .directoryId(20L)
                .publishedFileId(7001L)
                .versionNo("1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build());
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.VIEW))
                .thenReturn(true);
        when(fileMapper.selectById(7001L)).thenReturn(FileDO.builder()
                .id(7001L)
                .configId(1L)
                .path("dcc/original/spec.docx")
                .name("Spec.docx")
                .type("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .build());
        DccOnlyOfficePreviewTokenService.PreviewTokenPayload officeTokenPayload =
                new DccOnlyOfficePreviewTokenService.PreviewTokenPayload();
        officeTokenPayload.setTokenId("OT-20260528-0001");
        officeTokenPayload.setNonce("ON-20260528-0001");
        when(onlyOfficePreviewTokenService.issueControlledFile(eq(31L), eq(99L), eq(990L), eq("1.0"),
                eq(88001L), eq("CONTROLLED_PREVIEW"), eq(900L)))
                .thenReturn(new DccOnlyOfficePreviewTokenService.IssuedPreviewToken("office-token", officeTokenPayload));
        when(watermarkService.build(99L, "preview", "Spec.docx"))
                .thenReturn(DccControlledPreviewWatermarkRespVO.builder()
                        .label("受控预览")
                        .text("受控预览 | Spec.docx")
                        .actorName("Quality User")
                        .actorAccount("quality.user")
                        .timestamp("2026-05-22 10:00:00")
                        .purpose("preview")
                        .overlay(DccControlledPreviewWatermarkOverlayRespVO.builder()
                                .textColor("#6b7280")
                                .opacity(0.18D)
                                .rotationDeg(-24)
                                .gapX(260)
                                .gapY(180)
                                .fontSize(18)
                                .build())
                        .build());

        DccControlledFilePreviewMetadataRespVO result = queryService.getPreviewMetadata(99L, 990L,
                auditContext(PREVIEW_REQUEST_ID));

        assertEquals("OFFICE", result.getPreviewKind());
        assertEquals("Spec.docx", result.getFileName());
        assertEquals("http://onlyoffice.local", result.getOnlyofficeBaseUrl());
        assertEquals("http://127.0.0.1:48081/admin-api/dcc/controlled-files/990/onlyoffice-file?token=office-token",
                result.getOnlyofficeDocumentUrl());
        assertEquals("preview", result.getWatermark().getPurpose());
    }

    @Test
    void getPreviewMetadata_passesRequestAuditContextToPreviewAccess() {
        when(controlledFileMapper.selectById(993L)).thenReturn(DccControlledFileDO.builder()
                .id(993L)
                .categoryId(10L)
                .directoryId(20L)
                .publishedFileId(7003L)
                .fileNumber("SOP-993")
                .versionNo("1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build());
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.VIEW))
                .thenReturn(true);
        when(fileMapper.selectById(7003L)).thenReturn(FileDO.builder()
                .id(7003L)
                .configId(1L)
                .path("dcc/published/audit.pdf")
                .name("audit.pdf")
                .type("application/pdf")
                .build());
        when(watermarkService.build(99L, "preview", "audit.pdf"))
                .thenReturn(DccControlledPreviewWatermarkRespVO.builder().purpose("preview").build());

        queryService.getPreviewMetadata(99L, 993L, auditContext(PREVIEW_REQUEST_ID));

        ArgumentCaptor<DccPreviewAccessRequest> requestCaptor =
                ArgumentCaptor.forClass(DccPreviewAccessRequest.class);
        verify(previewAccessService).prepareAccess(requestCaptor.capture());
        DccPreviewAccessRequest request = requestCaptor.getValue();
        assertEquals(SOURCE_IP, request.sourceIp());
        assertEquals(USER_AGENT, request.userAgent());
        assertEquals(PREVIEW_REQUEST_ID, request.requestId());
    }

    @Test
    void getPreviewMetadata_officeFileWithoutOnlyOfficeConfig_returnsUnavailableReason() {
        ReflectionTestUtils.setField(queryService, "onlyOfficePreviewProperties", new DccOnlyOfficePreviewProperties());

        when(controlledFileMapper.selectById(990L)).thenReturn(DccControlledFileDO.builder()
                .id(990L)
                .categoryId(10L)
                .directoryId(20L)
                .publishedFileId(7001L)
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build());
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.VIEW))
                .thenReturn(true);
        when(fileMapper.selectById(7001L)).thenReturn(FileDO.builder()
                .id(7001L)
                .configId(1L)
                .path("dcc/original/spec.docx")
                .name("Spec.docx")
                .type("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .build());
        when(watermarkService.build(99L, "preview", "Spec.docx"))
                .thenReturn(DccControlledPreviewWatermarkRespVO.builder().purpose("preview").build());

        DccControlledFilePreviewMetadataRespVO result = queryService.getPreviewMetadata(99L, 990L,
                auditContext(PREVIEW_REQUEST_ID));

        assertEquals("OFFICE", result.getPreviewKind());
        assertEquals("Spec.docx", result.getFileName());
        assertNull(result.getOnlyofficeBaseUrl());
        assertNull(result.getOnlyofficeDocumentUrl());
        assertEquals("OnlyOffice preview config is missing: yudao.dcc.preview.onlyoffice.base-url is missing",
                result.getPreviewUnavailableReason());
        assertEquals("preview", result.getWatermark().getPurpose());
    }

    @Test
    void getPreviewMetadata_videoFileReturnsVideoKind() {
        when(controlledFileMapper.selectById(991L)).thenReturn(DccControlledFileDO.builder()
                .id(991L)
                .categoryId(10L)
                .directoryId(20L)
                .publishedFileId(7002L)
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build());
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.VIEW))
                .thenReturn(true);
        when(fileMapper.selectById(7002L)).thenReturn(FileDO.builder()
                .id(7002L)
                .configId(1L)
                .path("dcc/original/training.mp4")
                .name("training.mp4")
                .type("video/mp4")
                .build());
        when(watermarkService.build(99L, "preview", "training.mp4"))
                .thenReturn(DccControlledPreviewWatermarkRespVO.builder().purpose("preview").build());

        DccControlledFilePreviewMetadataRespVO result = queryService.getPreviewMetadata(99L, 991L,
                auditContext(PREVIEW_REQUEST_ID));

        assertEquals("VIDEO", result.getPreviewKind());
        assertEquals("training.mp4", result.getFileName());
        assertEquals("video/mp4", result.getContentType());
        assertNull(result.getOnlyofficeBaseUrl());
        assertNull(result.getOnlyofficeDocumentUrl());
    }

    @Test
    void readPreviewFile_activeAndSupersededUseProtectedPublishedBinary() throws Exception {
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.VIEW))
                .thenReturn(true);
        when(fileMapper.selectById(501L)).thenReturn(FileDO.builder()
                .id(501L)
                .configId(1L)
                .path("dcc/published/sample.pdf")
                .name("sample.pdf")
                .type("application/pdf")
                .build());
        when(fileService.getFileContent(1L, "dcc/published/sample.pdf")).thenReturn("pdf".getBytes());
        when(watermarkService.build(99L, "preview", "sample.pdf"))
                .thenReturn(DccControlledPreviewWatermarkRespVO.builder()
                        .label("受控预览")
                        .text("受控预览 | sample.pdf")
                        .actorName("Quality User")
                        .actorAccount("quality.user")
                        .timestamp("2026-05-16 12:00:00")
                        .purpose("preview")
                        .overlay(DccControlledPreviewWatermarkOverlayRespVO.builder()
                                .textColor("#6b7280")
                                .opacity(0.18D)
                                .rotationDeg(-24)
                                .gapX(260)
                                .gapY(180)
                                .fontSize(18)
                                .build())
                        .build());

        for (String status : List.of(DccControlledFileStatusEnum.ACTIVE.getStatus(),
                DccControlledFileStatusEnum.SUPERSEDED.getStatus())) {
            when(controlledFileMapper.selectById(900L)).thenReturn(DccControlledFileDO.builder()
                    .id(900L)
                    .categoryId(10L)
                    .directoryId(20L)
                    .publishedFileId(501L)
                    .versionNo("1.0")
                    .status(status)
                    .build());
            when(accessEventMapper.selectOne(any())).thenReturn(accessEvent(900L, "1.0"));
            when(watermarkTraceMapper.selectOne(any())).thenReturn(watermarkTrace(900L, "1.0"));

            DccControlledFileBinary binary = queryService.readPreviewFile(99L, 900L,
                    VIEWER_TOKEN, ACCESS_EVENT_CODE, WATERMARK_TRACE_CODE, VIEWER_TOKEN_ID, VIEWER_TOKEN_NONCE,
                    auditContext(PREVIEW_REQUEST_ID));

            assertEquals("sample.pdf", binary.fileName());
            assertEquals("application/pdf", binary.contentType());
            assertArrayEquals("pdf".getBytes(), binary.bytes());
            assertEquals("preview", binary.watermark().getPurpose());
        }
    }

    @Test
    void readPreviewFile_pendingRevisionRequesterReadsOriginalBinary() throws Exception {
        when(fileMapper.selectById(601L)).thenReturn(FileDO.builder()
                .id(601L)
                .configId(1L)
                .path("dcc/original/pending.pdf")
                .name("pending.pdf")
                .type("application/pdf")
                .build());
        when(fileService.getFileContent(1L, "dcc/original/pending.pdf")).thenReturn("pending".getBytes());
        when(watermarkService.build(99L, "preview", "pending.pdf"))
                .thenReturn(DccControlledPreviewWatermarkRespVO.builder()
                        .label("受控预览")
                        .text("受控预览 | pending.pdf")
                        .actorName("Quality User")
                        .actorAccount("quality.user")
                        .timestamp("2026-05-16 12:10:00")
                        .purpose("preview")
                        .overlay(DccControlledPreviewWatermarkOverlayRespVO.builder()
                                .textColor("#6b7280")
                                .opacity(0.18D)
                                .rotationDeg(-24)
                                .gapX(260)
                                .gapY(180)
                                .fontSize(18)
                                .build())
                        .build());
        when(controlledFileMapper.selectById(905L)).thenReturn(DccControlledFileDO.builder()
                .id(905L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(99L)
                .originalFileId(601L)
                .versionNo("1.0")
                .status(DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus())
                .build());
        when(accessEventMapper.selectOne(any())).thenReturn(accessEvent(905L, "1.0"));
        when(watermarkTraceMapper.selectOne(any())).thenReturn(watermarkTrace(905L, "1.0"));

        DccControlledFileBinary binary = queryService.readPreviewFile(99L, 905L,
                VIEWER_TOKEN, ACCESS_EVENT_CODE, WATERMARK_TRACE_CODE, VIEWER_TOKEN_ID, VIEWER_TOKEN_NONCE,
                auditContext(PREVIEW_REQUEST_ID));

        assertEquals("pending.pdf", binary.fileName());
        assertArrayEquals("pending".getBytes(), binary.bytes());
        assertEquals("preview", binary.watermark().getPurpose());
    }

    @Test
    void readPreviewFile_pendingRevisionSnapshotParticipantReadsOriginalBinaryWithoutCategoryReviewApprovePermission() throws Exception {
        when(fileMapper.selectById(602L)).thenReturn(FileDO.builder()
                .id(602L)
                .configId(1L)
                .path("dcc/original/pending-snapshot.pdf")
                .name("pending-snapshot.pdf")
                .type("application/pdf")
                .build());
        when(fileService.getFileContent(1L, "dcc/original/pending-snapshot.pdf"))
                .thenReturn("pending-snapshot".getBytes());
        when(watermarkService.build(99L, "preview", "pending-snapshot.pdf"))
                .thenReturn(DccControlledPreviewWatermarkRespVO.builder()
                        .label("受控预览")
                        .text("受控预览 | pending-snapshot.pdf")
                        .actorName("Quality User")
                        .actorAccount("quality.user")
                        .timestamp("2026-06-22 11:30:00")
                        .purpose("preview")
                        .overlay(DccControlledPreviewWatermarkOverlayRespVO.builder()
                                .textColor("#6b7280")
                                .opacity(0.18D)
                                .rotationDeg(-24)
                                .gapX(260)
                                .gapY(180)
                                .fontSize(18)
                                .build())
                        .build());
        when(controlledFileMapper.selectById(906L)).thenReturn(DccControlledFileDO.builder()
                .id(906L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(88L)
                .originalFileId(602L)
                .versionNo("1.0")
                .status(DccControlledFileStatusEnum.PENDING_MATRIX_REVIEW.getStatus())
                .build());
        when(routeSnapshotMapper.selectListByControlledFileId(906L)).thenReturn(List.of(
                DccControlledFileRouteSnapshotDO.builder()
                        .id(1002L)
                        .controlledFileId(906L)
                        .stageNo(2)
                        .stageCode("MATRIX_REVIEW")
                        .resolvedUserIds("99,100")
                        .build()));
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.REVIEW))
                .thenReturn(false);
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.APPROVE))
                .thenReturn(false);
        when(accessEventMapper.selectOne(any())).thenReturn(accessEvent(906L, "1.0"));
        when(watermarkTraceMapper.selectOne(any())).thenReturn(watermarkTrace(906L, "1.0"));

        DccControlledFileBinary binary = queryService.readPreviewFile(99L, 906L,
                VIEWER_TOKEN, ACCESS_EVENT_CODE, WATERMARK_TRACE_CODE, VIEWER_TOKEN_ID, VIEWER_TOKEN_NONCE,
                auditContext(PREVIEW_REQUEST_ID));

        assertEquals("pending-snapshot.pdf", binary.fileName());
        assertArrayEquals("pending-snapshot".getBytes(), binary.bytes());
        assertEquals("preview", binary.watermark().getPurpose());
    }

    @Test
    void readPreviewFile_pendingRevisionFutureStageParticipantDenied() throws Exception {
        when(controlledFileMapper.selectById(906L)).thenReturn(DccControlledFileDO.builder()
                .id(906L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(88L)
                .originalFileId(602L)
                .versionNo("1.0")
                .status(DccControlledFileStatusEnum.PENDING_MATRIX_REVIEW.getStatus())
                .build());
        when(routeSnapshotMapper.selectListByControlledFileId(906L)).thenReturn(List.of(
                DccControlledFileRouteSnapshotDO.builder()
                        .id(1002L)
                        .controlledFileId(906L)
                        .stageNo(2)
                        .stageCode("MATRIX_REVIEW")
                        .resolvedUserIds("100")
                        .build(),
                DccControlledFileRouteSnapshotDO.builder()
                        .id(1003L)
                        .controlledFileId(906L)
                        .stageNo(3)
                        .stageCode("MATRIX_APPROVAL")
                        .resolvedUserIds("99")
                        .build()));

        assertServiceException(() -> queryService.readPreviewFile(99L, 906L,
                        VIEWER_TOKEN, ACCESS_EVENT_CODE, WATERMARK_TRACE_CODE, VIEWER_TOKEN_ID, VIEWER_TOKEN_NONCE,
                        auditContext(PREVIEW_REQUEST_ID)),
                CONTROLLED_FILE_ACCESS_DENIED);

        verify(fileMapper, never()).selectById(602L);
    }

    @Test
    void readDownloadFile_supersededRevisionDenied() {
        when(controlledFileMapper.selectById(901L)).thenReturn(DccControlledFileDO.builder()
                .id(901L)
                .categoryId(10L)
                .publishedFileId(502L)
                .status(DccControlledFileStatusEnum.SUPERSEDED.getStatus())
                .build());

        assertServiceException(() -> queryService.readDownloadFile(99L, 901L, true, DOWNLOAD_REQUEST_ID,
                        auditContext(DOWNLOAD_REQUEST_ID)),
                CONTROLLED_FILE_ACCESS_DENIED);

        verify(accessLogMapper).insert(org.mockito.ArgumentMatchers.any(DccControlledFileAccessLogDO.class));
    }

    @Test
    void readDownloadFile_withoutWarningConfirmationDeniedBeforeContentRead() {
        when(controlledFileMapper.selectById(904L)).thenReturn(DccControlledFileDO.builder()
                .id(904L)
                .categoryId(10L)
                .publishedFileId(505L)
                .fileNumber("SOP-004")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build());

        assertServiceException(() -> queryService.readDownloadFile(99L, 904L, false, DOWNLOAD_REQUEST_ID,
                        auditContext(DOWNLOAD_REQUEST_ID)),
                CONTROLLED_FILE_DOWNLOAD_WARNING_UNCONFIRMED);

        verify(accessLogMapper).insert(org.mockito.ArgumentMatchers.<DccControlledFileAccessLogDO>argThat(log ->
                "WARNING_UNCONFIRMED".equals(log.getReason())));
    }

    @Test
    void readDownloadFile_rejectsMissingDownloadRequestIdBeforeSourceRead() throws Exception {
        assertServiceException(() -> queryService.readDownloadFile(99L, 904L, true, " ",
                        auditContext(DOWNLOAD_REQUEST_ID)),
                DCC_DOWNLOAD_REQUEST_ID_REQUIRED);

        verify(downloadRecordMapper, never()).selectOne(any());
        verify(controlledFileMapper, never()).selectById(any());
        verify(accessEventMapper, never()).insert(any(DccControlledFileAccessEventDO.class));
        verify(downloadRecordMapper, never()).insert(any(DccControlledFileDownloadRecordDO.class));
        verify(fileService, never()).getFileContent(anyLong(), anyString());
    }

    @Test
    void readDownloadFile_rejectsReusedDownloadRequestIdBeforeSourceRead() throws Exception {
        when(downloadRecordMapper.selectOne(any())).thenReturn(DccControlledFileDownloadRecordDO.builder()
                .id(1001L)
                .downloadRequestId(DOWNLOAD_REQUEST_ID)
                .build());

        assertServiceException(() -> queryService.readDownloadFile(99L, 904L, true, DOWNLOAD_REQUEST_ID,
                        auditContext(DOWNLOAD_REQUEST_ID)),
                DCC_DOWNLOAD_REQUEST_ID_REUSED);

        verify(controlledFileMapper, never()).selectById(any());
        verify(accessEventMapper, never()).insert(any(DccControlledFileAccessEventDO.class));
        verify(downloadRecordMapper, never()).insert(any(DccControlledFileDownloadRecordDO.class));
        verify(fileService, never()).getFileContent(anyLong(), anyString());
    }

    @Test
    void readDownloadFile_failsClosedWhenAccessEventInsertReturnsZero() throws Exception {
        stubActiveDownloadFile(910L, 511L, "audit-event-zero.pdf");
        when(accessEventMapper.insert(any(DccControlledFileAccessEventDO.class))).thenReturn(0);

        assertServiceException(() -> queryService.readDownloadFile(99L, 910L, true, DOWNLOAD_REQUEST_ID,
                        auditContext(DOWNLOAD_REQUEST_ID)),
                DCC_DOWNLOAD_AUDIT_RECORD_FAILED);

        verify(downloadRecordMapper, never()).insert(any(DccControlledFileDownloadRecordDO.class));
        verify(fileService, never()).getFileContent(anyLong(), anyString());
    }

    @Test
    void readDownloadFile_failsClosedWhenAccessEventIdIsNotBackfilled() throws Exception {
        stubActiveDownloadFile(911L, 512L, "audit-event-id-null.pdf");
        when(accessEventMapper.insert(any(DccControlledFileAccessEventDO.class))).thenReturn(1);

        assertServiceException(() -> queryService.readDownloadFile(99L, 911L, true, DOWNLOAD_REQUEST_ID,
                        auditContext(DOWNLOAD_REQUEST_ID)),
                DCC_DOWNLOAD_AUDIT_RECORD_FAILED);

        verify(downloadRecordMapper, never()).insert(any(DccControlledFileDownloadRecordDO.class));
        verify(fileService, never()).getFileContent(anyLong(), anyString());
    }

    @Test
    void readDownloadFile_failsClosedWhenDownloadRecordInsertReturnsZero() throws Exception {
        stubActiveDownloadFile(912L, 513L, "download-record-zero.pdf");
        stubDownloadAccessEventInsert(88012L);
        when(downloadRecordMapper.insert(any(DccControlledFileDownloadRecordDO.class))).thenReturn(0);

        assertServiceException(() -> queryService.readDownloadFile(99L, 912L, true, DOWNLOAD_REQUEST_ID,
                        auditContext(DOWNLOAD_REQUEST_ID)),
                DCC_DOWNLOAD_AUDIT_RECORD_FAILED);

        verify(fileService, never()).getFileContent(anyLong(), anyString());
    }

    @Test
    void readDownloadFile_failsClosedWhenDownloadRecordIdIsNotBackfilled() throws Exception {
        stubActiveDownloadFile(913L, 514L, "download-record-id-null.pdf");
        stubDownloadAccessEventInsert(88013L);
        when(downloadRecordMapper.insert(any(DccControlledFileDownloadRecordDO.class))).thenReturn(1);

        assertServiceException(() -> queryService.readDownloadFile(99L, 913L, true, DOWNLOAD_REQUEST_ID,
                        auditContext(DOWNLOAD_REQUEST_ID)),
                DCC_DOWNLOAD_AUDIT_RECORD_FAILED);

        verify(fileService, never()).getFileContent(anyLong(), anyString());
    }

    @Test
    void readDownloadFile_concurrentDuplicateDownloadRecordInsertReturnsRequestIdReused() throws Exception {
        stubActiveDownloadFile(914L, 515L, "download-record-duplicate.pdf");
        stubDownloadAccessEventInsert(88014L);
        when(downloadRecordMapper.insert(any(DccControlledFileDownloadRecordDO.class)))
                .thenThrow(new DuplicateKeyException("duplicate download_request_id"));

        assertServiceException(() -> queryService.readDownloadFile(99L, 914L, true, DOWNLOAD_REQUEST_ID,
                        auditContext(DOWNLOAD_REQUEST_ID)),
                DCC_DOWNLOAD_REQUEST_ID_REUSED);

        verify(fileService, never()).getFileContent(anyLong(), anyString());
    }

    @Test
    void readDownloadFile_prefixRecordRequiresExplicitCategoryDownloadPermission() throws Exception {
        when(controlledFileMapper.selectById(905L)).thenReturn(DccControlledFileDO.builder()
                .id(905L)
                .categoryId(10L)
                .directoryId(20L)
                .publishedFileId(506L)
                .fileNumber("INT/RE-001")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build());
        assertServiceException(() -> queryService.readDownloadFile(99L, 905L, true, DOWNLOAD_REQUEST_ID,
                        auditContext(DOWNLOAD_REQUEST_ID)),
                CONTROLLED_FILE_ACCESS_DENIED);

        verify(permissionSupport).hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.DOWNLOAD);
        verify(fileService, never()).getFileContent(1L, "dcc/published/system-record.pdf");
        verify(accessLogMapper).insert(org.mockito.ArgumentMatchers.<DccControlledFileAccessLogDO>argThat(log ->
                "DOWNLOAD".equals(log.getActionType())
                        && "DENIED".equals(log.getResult())
                        && "ACCESS_DENIED".equals(log.getReason())));
    }

    @Test
    void readDownloadFile_returnsOpenableControlledPdfWithoutEncryptionPackage() throws Exception {
        byte[] pdfBytes = "%PDF-1.7 controlled copy".getBytes(StandardCharsets.UTF_8);
        String pdfSha256 = sha256Hex(pdfBytes);
        stubActiveDownloadFile(918L, 519L, "openable-controlled.pdf");
        stubDownloadAccessEventInsert(88018L);
        stubDownloadRecordInsert(98018L);
        when(fileService.getFileContent(1L, "dcc/published/openable-controlled.pdf")).thenReturn(pdfBytes);

        DccDownloadFileBinary result = queryService.readDownloadFile(99L, 918L, true, DOWNLOAD_REQUEST_ID,
                auditContext(DOWNLOAD_REQUEST_ID));

        assertEquals("openable-controlled.pdf", result.fileName());
        assertEquals("application/pdf", result.contentType());
        assertArrayEquals(pdfBytes, result.bytes());
        assertEquals(DOWNLOAD_REQUEST_ID, result.downloadRequestId());
        assertNull(result.encryptionPolicyVersion());
        assertNull(result.artifactId());
        assertEquals(pdfSha256, result.plainSha256());
        assertNull(result.cipherSha256());
        verify(fileService).getFileContent(1L, "dcc/published/openable-controlled.pdf");
        verify(downloadRecordMapper).updateById(org.mockito.ArgumentMatchers.<DccControlledFileDownloadRecordDO>argThat(record ->
                Long.valueOf(98018L).equals(record.getId())
                        && "READY".equals(record.getEncryptionStatus())
                        && pdfSha256.equals(record.getPlainSha256())
                        && record.getEncryptionPolicyVersion() == null
                        && record.getArtifactId() == null
                        && record.getCipherFileRef() == null
                        && record.getCipherSha256() == null
                        && record.getEncryptedAt() == null
                        && record.getReturnedAt() != null));
        verify(accessLogMapper).insert(org.mockito.ArgumentMatchers.<DccControlledFileAccessLogDO>argThat(log ->
                "DOWNLOAD".equals(log.getActionType())
                        && "ALLOWED".equals(log.getResult())
                        && "OK".equals(log.getReason())));
    }

    @Test
    void readDownloadFile_createsEventAndRecordBeforePdfReadAndReturnsOpenablePdf() throws Exception {
        byte[] pdfBytes = "%PDF-1.7 contract-source".getBytes(StandardCharsets.UTF_8);
        String pdfSha256 = sha256Hex(pdfBytes);
        stubActiveDownloadFile(908L, 509L, "contract-source.pdf");
        stubDownloadAccessEventInsert(88008L);
        stubDownloadRecordInsert(98008L);
        when(fileService.getFileContent(1L, "dcc/published/contract-source.pdf")).thenReturn(pdfBytes);

        DccDownloadFileBinary result = queryService.readDownloadFile(99L, 908L, true, DOWNLOAD_REQUEST_ID,
                auditContext(DOWNLOAD_REQUEST_ID));

        assertEquals("contract-source.pdf", result.fileName());
        assertEquals("application/pdf", result.contentType());
        assertArrayEquals(pdfBytes, result.bytes());
        assertEquals(DOWNLOAD_REQUEST_ID, result.downloadRequestId());
        assertNull(result.encryptionPolicyVersion());
        assertNull(result.artifactId());
        assertEquals(pdfSha256, result.plainSha256());
        assertNull(result.cipherSha256());

        ArgumentCaptor<DccControlledFileAccessEventDO> eventCaptor =
                ArgumentCaptor.forClass(DccControlledFileAccessEventDO.class);
        ArgumentCaptor<DccControlledFileDownloadRecordDO> insertedRecordCaptor =
                ArgumentCaptor.forClass(DccControlledFileDownloadRecordDO.class);
        ArgumentCaptor<DccControlledFileDownloadRecordDO> updatedRecordCaptor =
                ArgumentCaptor.forClass(DccControlledFileDownloadRecordDO.class);
        InOrder order = inOrder(accessEventMapper, downloadRecordMapper, fileService);
        order.verify(accessEventMapper).insert(eventCaptor.capture());
        order.verify(downloadRecordMapper).insert(insertedRecordCaptor.capture());
        order.verify(fileService).getFileContent(1L, "dcc/published/contract-source.pdf");
        order.verify(downloadRecordMapper).updateById(updatedRecordCaptor.capture());

        DccControlledFileAccessEventDO event = eventCaptor.getValue();
        assertEquals(908L, event.getControlledFileId());
        assertEquals("1.0", event.getFileVersionNo());
        assertEquals(99L, event.getUserId());
        assertEquals("DOWNLOAD", event.getAccessType());
        assertEquals("CONTROLLED_DOWNLOAD", event.getPurpose());
        assertEquals("SUCCESS", event.getResult());
        assertNotNull(event.getAccessEventCode());

        DccControlledFileDownloadRecordDO insertedRecord = insertedRecordCaptor.getValue();
        assertEquals(DOWNLOAD_REQUEST_ID, insertedRecord.getDownloadRequestId());
        assertEquals(88008L, insertedRecord.getAccessEventId());
        assertEquals(event.getAccessEventCode(), insertedRecord.getAccessEventCode());
        assertEquals(908L, insertedRecord.getControlledFileId());
        assertEquals("1.0", insertedRecord.getFileVersionNo());
        assertEquals(99L, insertedRecord.getUserId());
        assertEquals("dcc-download-policy-v1", insertedRecord.getPolicyVersion());
        assertEquals("REQUESTED", insertedRecord.getEncryptionStatus());
        assertNotNull(insertedRecord.getRequestedAt());
        assertNull(insertedRecord.getEncryptedAt());
        assertNull(insertedRecord.getReturnedAt());

        DccControlledFileDownloadRecordDO updatedRecord = updatedRecordCaptor.getValue();
        assertEquals(98008L, updatedRecord.getId());
        assertEquals("READY", updatedRecord.getEncryptionStatus());
        assertNull(updatedRecord.getEncryptionPolicyVersion());
        assertNull(updatedRecord.getArtifactId());
        assertNull(updatedRecord.getCipherFileRef());
        assertEquals(pdfSha256, updatedRecord.getPlainSha256());
        assertNull(updatedRecord.getCipherSha256());
        assertNull(updatedRecord.getEncryptedAt());
        assertNotNull(updatedRecord.getReturnedAt());
        assertNull(updatedRecord.getFailureCode());
        assertNull(updatedRecord.getFailureReason());
    }

    @Test
    void readDownloadFile_recordsRequestAuditContextOnEventAndAccessLog() throws Exception {
        stubActiveDownloadFile(917L, 518L, "audit-context.pdf");
        stubDownloadAccessEventInsert(88017L);
        stubDownloadRecordInsert(98017L);
        when(fileService.getFileContent(1L, "dcc/published/audit-context.pdf"))
                .thenReturn("%PDF audit-context".getBytes(StandardCharsets.UTF_8));

        queryService.readDownloadFile(99L, 917L, true, DOWNLOAD_REQUEST_ID,
                auditContext(DOWNLOAD_REQUEST_ID));

        ArgumentCaptor<DccControlledFileAccessEventDO> eventCaptor =
                ArgumentCaptor.forClass(DccControlledFileAccessEventDO.class);
        verify(accessEventMapper).insert(eventCaptor.capture());
        DccControlledFileAccessEventDO event = eventCaptor.getValue();
        assertEquals(SOURCE_IP, event.getSourceIp());
        assertEquals(USER_AGENT, event.getUserAgent());
        assertEquals(DOWNLOAD_REQUEST_ID, event.getRequestId());

        ArgumentCaptor<DccControlledFileAccessLogDO> logCaptor =
                ArgumentCaptor.forClass(DccControlledFileAccessLogDO.class);
        verify(accessLogMapper).insert(logCaptor.capture());
        DccControlledFileAccessLogDO log = logCaptor.getValue();
        assertEquals(SOURCE_IP, log.getSourceIp());
        assertEquals(USER_AGENT, log.getUserAgent());
        assertEquals(DOWNLOAD_REQUEST_ID, log.getRequestId());
    }

    @Test
    void readDownloadFile_failsClosedWhenAllowedAccessLogInsertReturnsZero() throws Exception {
        byte[] pdfBytes = "%PDF allowed-log-zero".getBytes(StandardCharsets.UTF_8);
        String pdfSha256 = sha256Hex(pdfBytes);
        stubActiveDownloadFile(916L, 517L, "allowed-log-zero.pdf");
        stubDownloadAccessEventInsert(88016L);
        stubDownloadRecordInsert(98016L);
        when(fileService.getFileContent(1L, "dcc/published/allowed-log-zero.pdf")).thenReturn(pdfBytes);
        when(accessLogMapper.insert(any(DccControlledFileAccessLogDO.class))).thenAnswer(invocation -> {
            DccControlledFileAccessLogDO log = invocation.getArgument(0);
            if ("DOWNLOAD".equals(log.getActionType()) && "ALLOWED".equals(log.getResult())) {
                return 0;
            }
            return 1;
        });

        assertServiceException(() -> queryService.readDownloadFile(99L, 916L, true, DOWNLOAD_REQUEST_ID,
                        auditContext(DOWNLOAD_REQUEST_ID)),
                DCC_DOWNLOAD_AUDIT_RECORD_FAILED);

        verify(downloadRecordMapper).updateById(org.mockito.ArgumentMatchers.<DccControlledFileDownloadRecordDO>argThat(record ->
                Long.valueOf(98016L).equals(record.getId())
                        && "READY".equals(record.getEncryptionStatus())
                        && pdfSha256.equals(record.getPlainSha256())
                        && record.getArtifactId() == null
                        && record.getCipherSha256() == null));
        assertFailureUpdateClearsReturnableEvidence("AUDIT_RECORD_FAILED");
        verify(transactionManager).rollback(any());
        verify(accessLogMapper).insert(org.mockito.ArgumentMatchers.<DccControlledFileAccessLogDO>argThat(log ->
                "DOWNLOAD".equals(log.getActionType())
                        && "ALLOWED".equals(log.getResult())
                        && "OK".equals(log.getReason())));
        verify(accessLogMapper).insert(org.mockito.ArgumentMatchers.<DccControlledFileAccessLogDO>argThat(log ->
                "DOWNLOAD".equals(log.getActionType())
                        && "DENIED".equals(log.getResult())
                        && "AUDIT_RECORD_FAILED".equals(log.getReason())));
    }

    @Test
    void readDownloadFile_failsClosedWhenReadyAuditUpdateReturnsZero() throws Exception {
        stubActiveDownloadFile(915L, 516L, "ready-update-zero.pdf");
        stubDownloadAccessEventInsert(88015L);
        stubDownloadRecordInsert(98015L);
        when(fileService.getFileContent(1L, "dcc/published/ready-update-zero.pdf"))
                .thenReturn("%PDF ready-update-zero".getBytes(StandardCharsets.UTF_8));
        when(downloadRecordMapper.updateById(any(DccControlledFileDownloadRecordDO.class))).thenAnswer(invocation -> {
            DccControlledFileDownloadRecordDO record = invocation.getArgument(0);
            if ("READY".equals(record.getEncryptionStatus())) {
                return 0;
            }
            return 1;
        });

        assertServiceException(() -> queryService.readDownloadFile(99L, 915L, true, DOWNLOAD_REQUEST_ID,
                        auditContext(DOWNLOAD_REQUEST_ID)),
                DCC_DOWNLOAD_AUDIT_RECORD_FAILED);

        verify(accessLogMapper).insert(org.mockito.ArgumentMatchers.<DccControlledFileAccessLogDO>argThat(log ->
                "DOWNLOAD".equals(log.getActionType())
                        && "DENIED".equals(log.getResult())
                        && "AUDIT_RECORD_FAILED".equals(log.getReason())));
        assertFailureUpdateClearsReturnableEvidence("AUDIT_RECORD_FAILED");
        verify(transactionManager).rollback(any());
    }

    @Test
    void readDownloadFile_categoryAndDirectoryAllowedReturnsPdfWithoutEncryptionGateway() throws Exception {
        byte[] pdfBytes = "%PDF allowed-download".getBytes(StandardCharsets.UTF_8);
        String pdfSha256 = sha256Hex(pdfBytes);
        when(controlledFileMapper.selectById(906L)).thenReturn(DccControlledFileDO.builder()
                .id(906L)
                .categoryId(10L)
                .directoryId(20L)
                .publishedFileId(507L)
                .fileNumber("SOP-906")
                .versionNo("1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build());
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.DOWNLOAD))
                .thenReturn(true);
        when(fileMapper.selectById(507L)).thenReturn(FileDO.builder()
                .id(507L)
                .configId(1L)
                .path("dcc/published/allowed-download.pdf")
                .name("allowed-download.pdf")
                .type("application/pdf")
                .build());
        stubDownloadAccessEventInsert(88006L);
        stubDownloadRecordInsert(98006L);
        when(fileService.getFileContent(1L, "dcc/published/allowed-download.pdf")).thenReturn(pdfBytes);

        DccDownloadFileBinary result = queryService.readDownloadFile(99L, 906L, true, DOWNLOAD_REQUEST_ID,
                auditContext(DOWNLOAD_REQUEST_ID));

        assertEquals("allowed-download.pdf", result.fileName());
        assertEquals("application/pdf", result.contentType());
        assertArrayEquals(pdfBytes, result.bytes());
        assertEquals(pdfSha256, result.plainSha256());
        assertNull(result.encryptionPolicyVersion());
        assertNull(result.artifactId());
        assertNull(result.cipherSha256());
        verify(accessLogMapper).insert(org.mockito.ArgumentMatchers.<DccControlledFileAccessLogDO>argThat(log ->
                "DOWNLOAD".equals(log.getActionType())
                        && "ALLOWED".equals(log.getResult())
                        && "OK".equals(log.getReason())));
    }

    @Test
    void readDownloadFile_sourceReadRuntimeExceptionMarksRecordFailedAndDoesNotReturnBytes() throws Exception {
        when(controlledFileMapper.selectById(907L)).thenReturn(DccControlledFileDO.builder()
                .id(907L)
                .categoryId(10L)
                .directoryId(20L)
                .publishedFileId(508L)
                .fileNumber("SOP-907")
                .versionNo("1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build());
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.DOWNLOAD))
                .thenReturn(true);
        when(fileMapper.selectById(508L)).thenReturn(FileDO.builder()
                .id(508L)
                .configId(1L)
                .path("dcc/published/invalid-evidence-download.pdf")
                .name("invalid-evidence-download.pdf")
                .type("application/pdf")
                .build());
        stubDownloadAccessEventInsert(88007L);
        stubDownloadRecordInsert(98007L);
        when(fileService.getFileContent(1L, "dcc/published/invalid-evidence-download.pdf"))
                .thenThrow(new IllegalStateException("file storage down"));

        assertServiceException(() -> queryService.readDownloadFile(99L, 907L, true, DOWNLOAD_REQUEST_ID,
                        auditContext(DOWNLOAD_REQUEST_ID)),
                CONTROLLED_FILE_ACCESS_DENIED);

        verify(fileService).getFileContent(1L, "dcc/published/invalid-evidence-download.pdf");
        verify(accessLogMapper).insert(org.mockito.ArgumentMatchers.<DccControlledFileAccessLogDO>argThat(log ->
                "DOWNLOAD".equals(log.getActionType())
                        && "DENIED".equals(log.getResult())
                        && "SOURCE_READ_FAILED".equals(log.getReason())));
        UpdateWrapper failureUpdate = assertFailureUpdateClearsReturnableEvidence("SOURCE_READ_FAILED");
        assertTrue(failureUpdate.getParamNameValuePairs().values().contains("file storage down"),
                failureUpdate.getParamNameValuePairs().values()::toString);
    }

    @Test
    void readDownloadFile_sourceReadServiceExceptionMarksRecordFailedAndDoesNotReturnBytes() throws Exception {
        stubActiveDownloadFile(909L, 510L, "source-read-exception.pdf");
        stubDownloadAccessEventInsert(88009L);
        stubDownloadRecordInsert(98009L);
        when(fileService.getFileContent(1L, "dcc/published/source-read-exception.pdf"))
                .thenThrow(new ServiceException(CONTROLLED_FILE_ACCESS_DENIED.getCode(),
                        CONTROLLED_FILE_ACCESS_DENIED.getMsg()));

        assertServiceException(() -> queryService.readDownloadFile(99L, 909L, true, DOWNLOAD_REQUEST_ID,
                        auditContext(DOWNLOAD_REQUEST_ID)),
                CONTROLLED_FILE_ACCESS_DENIED);

        verify(fileService).getFileContent(1L, "dcc/published/source-read-exception.pdf");
        verify(accessLogMapper).insert(org.mockito.ArgumentMatchers.<DccControlledFileAccessLogDO>argThat(log ->
                "DOWNLOAD".equals(log.getActionType())
                        && "DENIED".equals(log.getResult())
                        && "SOURCE_READ_FAILED".equals(log.getReason())));
        assertFailureUpdateClearsReturnableEvidence("SOURCE_READ_FAILED");
    }

    @Test
    void getControlledFile_deniedWhenUserIsOutsideViewMatrixEvenWithLegacyViewPermission() {
        when(controlledFileMapper.selectById(950L)).thenReturn(DccControlledFileDO.builder()
                .id(950L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(88L)
                .publishedFileId(510L)
                .title("Directory denied detail")
                .fileNumber("SOP-950")
                .versionNo("1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build());
        when(directoryAccessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(false);
        when(viewMatrixAccessService.canAccessCurrentViewMatrix(eq(99L), any(DccControlledFileDO.class)))
                .thenReturn(false);
        lenient().when(fileMapper.selectById(510L)).thenReturn(FileDO.builder()
                .id(510L)
                .name("directory-denied-detail.pdf")
                .type("application/pdf")
                .build());
        stubEmptyDetailRelations(950L);

        assertServiceException(() -> queryService.getControlledFile(99L, 950L), CONTROLLED_FILE_ACCESS_DENIED);
    }

    @Test
    void readPreviewFile_activeFileDeniedWhenUserIsOutsideViewMatrixEvenWithLegacyViewPermission() throws Exception {
        when(controlledFileMapper.selectById(951L)).thenReturn(DccControlledFileDO.builder()
                .id(951L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(88L)
                .publishedFileId(511L)
                .fileNumber("SOP-951")
                .versionNo("1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build());
        when(viewMatrixAccessService.canAccessCurrentViewMatrix(eq(99L), any(DccControlledFileDO.class)))
                .thenReturn(false);
        lenient().when(fileMapper.selectById(511L)).thenReturn(FileDO.builder()
                .id(511L)
                .configId(1L)
                .path("dcc/published/directory-denied-preview.pdf")
                .name("directory-denied-preview.pdf")
                .type("application/pdf")
                .build());
        lenient().when(fileService.getFileContent(1L, "dcc/published/directory-denied-preview.pdf"))
                .thenReturn("preview".getBytes());
        lenient().when(watermarkService.build(99L, "preview", "directory-denied-preview.pdf"))
                .thenReturn(DccControlledPreviewWatermarkRespVO.builder().purpose("preview").build());

        assertServiceException(() -> queryService.readPreviewFile(99L, 951L,
                VIEWER_TOKEN, ACCESS_EVENT_CODE, WATERMARK_TRACE_CODE, VIEWER_TOKEN_ID, VIEWER_TOKEN_NONCE,
                auditContext(PREVIEW_REQUEST_ID)),
                CONTROLLED_FILE_ACCESS_DENIED);

        verify(accessLogMapper).insert(org.mockito.ArgumentMatchers.<DccControlledFileAccessLogDO>argThat(log ->
                Long.valueOf(951L).equals(log.getControlledFileId())
                        && Long.valueOf(99L).equals(log.getUserId())
                        && "PREVIEW".equals(log.getActionType())
                        && "DENIED".equals(log.getResult())
                        && "ACCESS_DENIED".equals(log.getReason())
                        && SOURCE_IP.equals(log.getSourceIp())
                        && USER_AGENT.equals(log.getUserAgent())
                        && PREVIEW_REQUEST_ID.equals(log.getRequestId())));
    }

    @Test
    void readDownloadFile_activeFileDeniedWhenDirectoryCanDownloadFalseEvenWithCategoryDownload() throws Exception {
        when(controlledFileMapper.selectById(952L)).thenReturn(DccControlledFileDO.builder()
                .id(952L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(88L)
                .publishedFileId(512L)
                .fileNumber("SOP-952")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build());
        lenient().when(directoryAccessPermissionService.getAuthorizedDirectoryIds(99L, DccAccessTypeEnum.DOWNLOAD))
                .thenReturn(Set.of(21L));
        lenient().when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.DOWNLOAD))
                .thenReturn(true);
        lenient().when(fileMapper.selectById(512L)).thenReturn(FileDO.builder()
                .id(512L)
                .configId(1L)
                .path("dcc/published/directory-denied-download.pdf")
                .name("directory-denied-download.pdf")
                .type("application/pdf")
                .build());
        lenient().when(fileService.getFileContent(1L, "dcc/published/directory-denied-download.pdf"))
                .thenReturn("download".getBytes());

        assertServiceException(() -> queryService.readDownloadFile(99L, 952L, true, DOWNLOAD_REQUEST_ID,
                        auditContext(DOWNLOAD_REQUEST_ID)),
                CONTROLLED_FILE_ACCESS_DENIED);

        verify(accessLogMapper).insert(org.mockito.ArgumentMatchers.<DccControlledFileAccessLogDO>argThat(log ->
                Long.valueOf(952L).equals(log.getControlledFileId())
                        && Long.valueOf(99L).equals(log.getUserId())
                        && "DOWNLOAD".equals(log.getActionType())
                        && "DENIED".equals(log.getResult())
                        && "ACCESS_DENIED".equals(log.getReason())));
    }

    @Test
    void getControlledFile_allowsDetailAndPreviewFromViewMatrixButKeepsDownloadDeniedByDownloadRule() {
        when(controlledFileMapper.selectById(953L)).thenReturn(DccControlledFileDO.builder()
                .id(953L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(88L)
                .publishedFileId(513L)
                .title("Directory denied actions")
                .fileNumber("SOP-953")
                .versionNo("1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build());
        when(directoryAccessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(false);
        when(viewMatrixAccessService.canAccessCurrentViewMatrix(eq(99L), any(DccControlledFileDO.class)))
                .thenReturn(true);
        when(routeSnapshotMapper.selectListByControlledFileId(953L)).thenReturn(List.of(
                DccControlledFileRouteSnapshotDO.builder()
                        .id(1200L)
                        .controlledFileId(953L)
                        .stageNo(1)
                        .stageCode("DOC_CONTROL_REVIEW")
                        .resolvedUserIds("100")
                        .build()));
        lenient().when(directoryAccessPermissionService.getAuthorizedDirectoryIds(99L, DccAccessTypeEnum.DOWNLOAD))
                .thenReturn(Set.of(21L));
        lenient().when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.DOWNLOAD))
                .thenReturn(true);
        when(fileMapper.selectById(513L)).thenReturn(FileDO.builder()
                .id(513L)
                .name("directory-denied-actions.pdf")
                .type("application/pdf")
                .build());
        stubEmptyDetailRelations(953L);

        DccControlledFileRespVO respVO = queryService.getControlledFile(99L, 953L);

        assertTrue(Boolean.TRUE.equals(respVO.getCanPreview()));
        assertFalse(Boolean.TRUE.equals(respVO.getCanDownload()));
    }

    @Test
    void getControlledFilePage_allowsViewMatrixParticipantWithoutLegacyViewOrDirectoryQuery() {
        DccControlledFilePageReqVO reqVO = new DccControlledFilePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setDirectoryId(20L);
        DccControlledFileDO file = DccControlledFileDO.builder()
                .id(954L)
                .masterId(740L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(88L)
                .title("Review matrix visible")
                .fileNumber("SOP-954")
                .versionNo("1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .effectiveDate(LocalDate.of(2026, 6, 22))
                .build();
        when(directoryAccessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(false);
        when(controlledFileMapper.selectWorkflowList(any(DccControlledFilePageReqVO.class), eq(Set.of(20L))))
                .thenReturn(List.of(file));
        when(controlledFileMapper.selectListByMasterId(740L)).thenReturn(List.of(file));
        when(viewMatrixAccessService.canAccessCurrentViewMatrix(99L, file)).thenReturn(true);
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.DOWNLOAD))
                .thenReturn(false);

        PageResult<DccControlledFileRespVO> result = queryService.getControlledFilePage(99L, reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals(List.of(954L), result.getList().stream().map(DccControlledFileRespVO::getId).toList());
        verify(directoryAccessPermissionService, never()).getAuthorizedDirectoryIds(99L, DccAccessTypeEnum.QUERY);
    }

    @Test
    void getControlledFilePage_filtersBrowserExtensionBlacklistForProjectCodeAssociations() {
        DccControlledFilePageReqVO reqVO = new DccControlledFilePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setDccProjectCodeId(3000L);
        DccControlledFileDO drawing = DccControlledFileDO.builder()
                .id(955L)
                .masterId(741L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(88L)
                .fileName("220YCK300-CP-103 按压式Y型连接器Ⅲ型.pdf")
                .fileNumber("YCKPR")
                .title("成品图纸")
                .versionNo("1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .effectiveDate(LocalDate.of(2026, 6, 22))
                .build();
        DccControlledFileDO blacklisted = DccControlledFileDO.builder()
                .id(956L)
                .masterId(742L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(88L)
                .fileName("Thumbs.db")
                .fileNumber("Thumbs")
                .title("Thumbs")
                .versionNo("1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .effectiveDate(LocalDate.of(2026, 6, 22))
                .build();
        when(directoryAccessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(true);
        when(browserSettingsService.getBlacklistedExtensionPatterns()).thenReturn(List.of("*.db"));
        when(controlledFileMapper.selectWorkflowList(any(DccControlledFilePageReqVO.class)))
                .thenReturn(List.of(drawing, blacklisted));

        PageResult<DccControlledFileRespVO> result = queryService.getControlledFilePage(99L, reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals(List.of(955L), result.getList().stream().map(DccControlledFileRespVO::getId).toList());
    }

    @Test
    void getControlledFile_explainsCurrentViewMatrixAccessAndSeparateDownloadDecision() {
        DccControlledFileDO file = DccControlledFileDO.builder()
                .id(957L)
                .masterId(742L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(88L)
                .publishedFileId(517L)
                .title("Review matrix explained")
                .fileNumber("SOP-957")
                .versionNo("1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .effectiveDate(LocalDate.of(2026, 6, 22))
                .build();
        when(controlledFileMapper.selectById(957L)).thenReturn(file);
        when(directoryAccessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(false);
        when(viewMatrixAccessService.canAccessCurrentViewMatrix(99L, file)).thenReturn(true);
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.DOWNLOAD))
                .thenReturn(false);
        when(fileMapper.selectById(517L)).thenReturn(FileDO.builder()
                .id(517L)
                .name("review-matrix-explained.pdf")
                .type("application/pdf")
                .build());
        stubEmptyDetailRelations(957L);

        DccControlledFileRespVO respVO = queryService.getControlledFile(99L, 957L);

        assertEquals("CURRENT_VIEW_MATRIX", respVO.getAccessExplanation().getDetailSource());
        assertEquals("当前查看矩阵参与人", respVO.getAccessExplanation().getDetailReason());
        assertEquals("CURRENT_VIEW_MATRIX", respVO.getAccessExplanation().getPublishedPreviewSource());
        assertEquals("DOWNLOAD_POLICY", respVO.getAccessExplanation().getDownloadSource());
        assertEquals("CATEGORY_DOWNLOAD_DENIED", respVO.getAccessExplanation().getDownloadDeniedReason());
        assertTrue(Boolean.TRUE.equals(respVO.getCanPreview()));
        assertFalse(Boolean.TRUE.equals(respVO.getCanDownload()));
    }

    @Test
    void getControlledFile_readyToPublishWithApprovePermissionProjectsPublishAction() {
        DccControlledFileDO file = DccControlledFileDO.builder()
                .id(959L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(99L)
                .title("Ready revision")
                .fileNumber("SOP-959")
                .versionNo("2.0")
                .status(DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus())
                .build();
        when(controlledFileMapper.selectById(959L)).thenReturn(file);
        when(directoryAccessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(false);
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.APPROVE))
                .thenReturn(true);
        stubEmptyDetailRelations(959L);

        DccControlledFileRespVO respVO = queryService.getControlledFile(99L, 959L);

        assertTrue(Boolean.TRUE.equals(respVO.getCanPublish()));
        assertFalse(Boolean.TRUE.equals(respVO.getCanObsolete()));
    }

    @Test
    void explainControlledFileAccess_returnsDeniedReasonWhenUserOutsideCurrentViewMatrix() {
        DccControlledFileDO file = DccControlledFileDO.builder()
                .id(958L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(88L)
                .publishedFileId(518L)
                .title("Denied explained")
                .fileNumber("SOP-958")
                .versionNo("1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build();
        when(controlledFileMapper.selectById(958L)).thenReturn(file);
        when(directoryAccessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(false);
        when(viewMatrixAccessService.canAccessCurrentViewMatrix(99L, file)).thenReturn(false);

        var explanation = queryService.explainControlledFileAccess(99L, 958L);

        assertEquals("DENIED", explanation.getDetailSource());
        assertEquals("不在当前文件类型查看矩阵解析主体内", explanation.getDetailDeniedReason());
        assertEquals("DOWNLOAD_POLICY", explanation.getDownloadSource());
    }

    @Test
    void readPreviewFile_activeFileAllowedForViewMatrixParticipantWithoutLegacyPreviewDirectory() throws Exception {
        when(controlledFileMapper.selectById(955L)).thenReturn(DccControlledFileDO.builder()
                .id(955L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(88L)
                .publishedFileId(515L)
                .fileNumber("SOP-955")
                .versionNo("1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build());
        when(viewMatrixAccessService.canAccessCurrentViewMatrix(eq(99L), any(DccControlledFileDO.class)))
                .thenReturn(true);
        when(accessEventMapper.selectOne(any())).thenReturn(accessEvent(955L, "1.0"));
        when(watermarkTraceMapper.selectOne(any())).thenReturn(watermarkTrace(955L, "1.0"));
        when(fileMapper.selectById(515L)).thenReturn(FileDO.builder()
                .id(515L)
                .configId(1L)
                .path("dcc/published/review-matrix-preview.pdf")
                .name("review-matrix-preview.pdf")
                .type("application/pdf")
                .build());
        when(fileService.getFileContent(1L, "dcc/published/review-matrix-preview.pdf"))
                .thenReturn("preview".getBytes());
        when(watermarkService.build(99L, "preview", "review-matrix-preview.pdf"))
                .thenReturn(DccControlledPreviewWatermarkRespVO.builder().purpose("preview").build());

        DccControlledFileBinary binary = queryService.readPreviewFile(99L, 955L,
                VIEWER_TOKEN, ACCESS_EVENT_CODE, WATERMARK_TRACE_CODE, VIEWER_TOKEN_ID, VIEWER_TOKEN_NONCE,
                auditContext(PREVIEW_REQUEST_ID));

        assertArrayEquals("preview".getBytes(), binary.bytes());
    }

    @Test
    void getControlledFile_pendingFileAllowsSnapshotParticipantEvenWhenCurrentViewMatrixChanged() {
        when(controlledFileMapper.selectById(956L)).thenReturn(DccControlledFileDO.builder()
                .id(956L)
                .masterId(741L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(88L)
                .originalFileId(516L)
                .title("Pending snapshot visible")
                .fileNumber("SOP-956")
                .versionNo("2.0")
                .processInstanceId("pi-956")
                .status(DccControlledFileStatusEnum.PENDING_MATRIX_REVIEW.getStatus())
                .effectiveDate(LocalDate.of(2026, 6, 22))
                .build());
        when(directoryAccessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(false);
        when(viewMatrixAccessService.canAccessCurrentViewMatrix(eq(99L), any(DccControlledFileDO.class)))
                .thenReturn(false);
        when(routeSnapshotMapper.selectListByControlledFileId(956L)).thenReturn(List.of(
                DccControlledFileRouteSnapshotDO.builder()
                        .id(1201L)
                        .controlledFileId(956L)
                        .stageNo(2)
                        .stageCode("MATRIX_REVIEW")
                        .resolvedUserIds("99,100")
                        .build()));
        when(fileMapper.selectById(516L)).thenReturn(FileDO.builder()
                .id(516L)
                .name("pending-snapshot.docx")
                .type("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .build());

        DccControlledFileRespVO respVO = queryService.getControlledFile(99L, 956L);

        assertTrue(Boolean.TRUE.equals(respVO.getCanPreview()));
        assertFalse(Boolean.TRUE.equals(respVO.getCanDownload()));
    }

    @Test
    void getControlledFile_pendingFileDeniesFutureStageParticipant() {
        when(controlledFileMapper.selectById(956L)).thenReturn(DccControlledFileDO.builder()
                .id(956L)
                .masterId(741L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(88L)
                .originalFileId(516L)
                .title("Pending snapshot future")
                .fileNumber("SOP-956")
                .versionNo("2.0")
                .processInstanceId("pi-956")
                .status(DccControlledFileStatusEnum.PENDING_MATRIX_REVIEW.getStatus())
                .effectiveDate(LocalDate.of(2026, 6, 22))
                .build());
        when(directoryAccessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(false);
        when(viewMatrixAccessService.canAccessCurrentViewMatrix(eq(99L), any(DccControlledFileDO.class)))
                .thenReturn(false);
        when(routeSnapshotMapper.selectListByControlledFileId(956L)).thenReturn(List.of(
                DccControlledFileRouteSnapshotDO.builder()
                        .id(1201L)
                        .controlledFileId(956L)
                        .stageNo(2)
                        .stageCode("MATRIX_REVIEW")
                        .resolvedUserIds("100")
                        .build(),
                DccControlledFileRouteSnapshotDO.builder()
                        .id(1202L)
                        .controlledFileId(956L)
                        .stageNo(3)
                        .stageCode("MATRIX_APPROVAL")
                        .resolvedUserIds("99")
                        .build()));

        assertServiceException(() -> queryService.getControlledFile(99L, 956L),
                CONTROLLED_FILE_ACCESS_DENIED);
    }

    @Test
    void getControlledFile_activeCurrentVersionWithPendingNewVersionMarksModifying() {
        when(controlledFileMapper.selectById(906L)).thenReturn(DccControlledFileDO.builder()
                .id(906L)
                .masterId(720L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(88L)
                .publishedFileId(507L)
                .title("SOP-005")
                .fileNumber("SOP-005")
                .versionNo("1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .effectiveDate(LocalDate.of(2026, 5, 20))
                .build());
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.VIEW))
                .thenReturn(true);
        when(controlledFileMapper.selectListByMasterId(720L)).thenReturn(List.of(
                DccControlledFileDO.builder()
                        .id(906L)
                        .masterId(720L)
                        .categoryId(10L)
                        .directoryId(20L)
                        .publishedFileId(507L)
                        .title("SOP-005")
                        .fileNumber("SOP-005")
                        .versionNo("1.0")
                        .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                        .effectiveDate(LocalDate.of(2026, 5, 20))
                        .build(),
                DccControlledFileDO.builder()
                        .id(907L)
                        .masterId(720L)
                        .categoryId(10L)
                        .directoryId(20L)
                        .originalFileId(508L)
                        .title("SOP-005")
                        .fileNumber("SOP-005")
                        .versionNo("2.0")
                        .status(DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus())
                        .effectiveDate(LocalDate.of(2026, 6, 1))
                        .build()));

        DccControlledFileRespVO respVO = queryService.getControlledFile(99L, 906L);

        assertTrue(Boolean.TRUE.equals(respVO.getModifying()));
    }

    @Test
    void pendingApplicantProjectionShouldBeConsistentBetweenDetailAndPage() {
        DccControlledFileDO pendingFile = DccControlledFileDO.builder()
                .id(1001L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(99L)
                .title("Pending applicant projection")
                .versionNo("2.0")
                .processInstanceId("pi-1001")
                .status(DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus())
                .build();
        when(controlledFileMapper.selectById(1001L)).thenReturn(pendingFile);
        DccControlledFilePageReqVO reqVO = new DccControlledFilePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        when(controlledFileMapper.selectWorkflowList(reqVO)).thenReturn(List.of(pendingFile));

        DccControlledFileRespVO detail = queryService.getControlledFile(99L, 1001L);
        PageResult<DccControlledFileRespVO> page = queryService.getControlledFilePage(99L, reqVO);

        assertEquals(1L, page.getTotal());
        var detailProjection = detail.getActionProjection();
        var pageProjection = page.getList().get(0).getActionProjection();
        assertEquals(detailProjection, pageProjection);
        assertTrue(Boolean.TRUE.equals(detailProjection.getActionLocked()));
        assertNotNull(detailProjection.getActionLockReason());
        assertEquals(List.of("VIEW", "WITHDRAW"), detailProjection.getAllowedActions());
        assertTrue(Boolean.TRUE.equals(detailProjection.getCanWithdraw()));
        assertEquals(1001L, detailProjection.getPendingRequestId());
        assertEquals("2.0", detailProjection.getPendingVersionNo());
    }

    @Test
    void pendingNonApplicantProjectionShouldLockWithdraw() {
        DccControlledFileDO pendingFile = DccControlledFileDO.builder()
                .id(1002L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(88L)
                .title("Pending non applicant projection")
                .versionNo("2.0")
                .processInstanceId("pi-1002")
                .status(DccControlledFileStatusEnum.PENDING_MATRIX_REVIEW.getStatus())
                .build();
        when(controlledFileMapper.selectById(1002L)).thenReturn(pendingFile);
        when(directoryAccessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(true);
        DccControlledFilePageReqVO reqVO = pageReq();
        when(controlledFileMapper.selectWorkflowList(reqVO)).thenReturn(List.of(pendingFile));

        DccControlledFileRespVO detail = queryService.getControlledFile(99L, 1002L);

        var projection = detail.getActionProjection();
        assertPageProjectionMatches(99L, reqVO, detail);
        assertTrue(Boolean.TRUE.equals(projection.getActionLocked()));
        assertNotNull(projection.getActionLockReason());
        assertEquals(List.of("VIEW"), projection.getAllowedActions());
        assertFalse(Boolean.TRUE.equals(projection.getCanWithdraw()));
        assertEquals(1002L, projection.getPendingRequestId());
        assertEquals("2.0", projection.getPendingVersionNo());
    }

    @Test
    void pendingApplicantTrainingRecordProjectionShouldExposeUploadAction() {
        DccControlledFileDO pendingFile = DccControlledFileDO.builder()
                .id(1013L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(99L)
                .title("Pending training record projection")
                .versionNo("2.0")
                .processInstanceId("pi-1013")
                .needTraining(true)
                .status(DccControlledFileStatusEnum.PENDING_APPLICANT_TRAINING_RECORD.getStatus())
                .build();
        when(controlledFileMapper.selectById(1013L)).thenReturn(pendingFile);
        DccControlledFilePageReqVO reqVO = pageReq();
        when(controlledFileMapper.selectWorkflowList(reqVO)).thenReturn(List.of(pendingFile));

        DccControlledFileRespVO detail = queryService.getControlledFile(99L, 1013L);

        var projection = detail.getActionProjection();
        assertPageProjectionMatches(99L, reqVO, detail);
        assertTrue(Boolean.TRUE.equals(projection.getActionLocked()));
        assertEquals(List.of("VIEW", "WITHDRAW", "UPLOAD_TRAINING_RECORD"), projection.getAllowedActions());
        assertTrue(Boolean.TRUE.equals(projection.getCanWithdraw()));
        assertEquals(1013L, projection.getPendingRequestId());
        assertEquals("2.0", projection.getPendingVersionNo());
    }

    @Test
    void withdrawnApplicantProjectionShouldAllowOnlyWithdrawnFlowActions() {
        DccControlledFileDO reusableWithdrawnFile = DccControlledFileDO.builder()
                .id(1003L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(99L)
                .title("Withdrawn without successor")
                .versionNo("2.0")
                .status(DccControlledFileStatusEnum.WITHDRAWN.getStatus())
                .build();
        DccControlledFileDO succeededWithdrawnFile = DccControlledFileDO.builder()
                .id(1004L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(99L)
                .title("Withdrawn with successor")
                .versionNo("2.0")
                .status(DccControlledFileStatusEnum.WITHDRAWN.getStatus())
                .supersededByFileId(1005L)
                .build();
        when(controlledFileMapper.selectById(1003L)).thenReturn(reusableWithdrawnFile);
        when(controlledFileMapper.selectById(1004L)).thenReturn(succeededWithdrawnFile);
        DccControlledFilePageReqVO reqVO = pageReq();
        when(controlledFileMapper.selectWorkflowList(reqVO))
                .thenReturn(List.of(reusableWithdrawnFile, succeededWithdrawnFile));

        DccControlledFileRespVO reusableResp = queryService.getControlledFile(99L, 1003L);
        DccControlledFileRespVO succeededResp = queryService.getControlledFile(99L, 1004L);
        var reusableProjection = reusableResp.getActionProjection();
        var succeededProjection = succeededResp.getActionProjection();

        assertPageProjectionMatches(99L, reqVO, reusableResp);
        assertPageProjectionMatches(99L, reqVO, succeededResp);
        assertTrue(Boolean.TRUE.equals(reusableProjection.getActionLocked()));
        assertFalse(Boolean.TRUE.equals(reusableProjection.getCanWithdraw()));
        assertEquals(List.of("VIEW", "DELETE_WITHDRAWN_FLOW", "RESUBMIT_WITHDRAWN_FLOW"),
                reusableProjection.getAllowedActions());
        assertNull(reusableProjection.getPendingRequestId());
        assertEquals(List.of("VIEW"), succeededProjection.getAllowedActions());
        assertFalse(succeededProjection.getAllowedActions().contains("DELETE_WITHDRAWN_FLOW"));
        assertFalse(succeededProjection.getAllowedActions().contains("RESUBMIT_WITHDRAWN_FLOW"));
    }

    @Test
    void terminalProjectionShouldNotExposeOrdinaryActions() {
        when(directoryAccessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(true);
        DccControlledFileDO obsoleteFile = DccControlledFileDO.builder()
                .id(1006L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(99L)
                .title("Obsolete projection")
                .versionNo("1.0")
                .status(DccControlledFileStatusEnum.OBSOLETE.getStatus())
                .build();
        DccControlledFileDO supersededFile = DccControlledFileDO.builder()
                .id(1007L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(99L)
                .publishedFileId(7007L)
                .title("Superseded projection")
                .versionNo("1.0")
                .status(DccControlledFileStatusEnum.SUPERSEDED.getStatus())
                .build();
        when(controlledFileMapper.selectById(1006L)).thenReturn(obsoleteFile);
        when(controlledFileMapper.selectById(1007L)).thenReturn(supersededFile);
        DccControlledFilePageReqVO reqVO = pageReq();
        when(controlledFileMapper.selectWorkflowList(reqVO)).thenReturn(List.of(obsoleteFile, supersededFile));
        Set<String> ordinaryActions = Set.of("WITHDRAW", "OBSOLETE", "MANUAL_RELEASE",
                "DELETE_WITHDRAWN_FLOW", "RESUBMIT_WITHDRAWN_FLOW", "EDIT", "SUBMIT_RELEASE", "DISTRIBUTE");

        DccControlledFileRespVO obsoleteResp = queryService.getControlledFile(99L, 1006L);
        DccControlledFileRespVO supersededResp = queryService.getControlledFile(99L, 1007L);
        var obsoleteProjection = obsoleteResp.getActionProjection();
        var supersededProjection = supersededResp.getActionProjection();

        assertPageProjectionMatches(99L, reqVO, obsoleteResp);
        assertPageProjectionMatches(99L, reqVO, supersededResp);
        assertTrue(Boolean.TRUE.equals(obsoleteProjection.getActionLocked()));
        assertTrue(Boolean.TRUE.equals(supersededProjection.getActionLocked()));
        assertEquals(List.of("VIEW"), obsoleteProjection.getAllowedActions());
        assertEquals(List.of("VIEW", "PREVIEW"), supersededProjection.getAllowedActions());
        assertTrue(ordinaryActions.stream().noneMatch(obsoleteProjection.getAllowedActions()::contains));
        assertTrue(ordinaryActions.stream().noneMatch(supersededProjection.getAllowedActions()::contains));
    }

    @Test
    void activeProjectionShouldExposeObsoleteOnlyWhenAllowed() {
        DccControlledFileDO obsoleteAllowedFile = DccControlledFileDO.builder()
                .id(1008L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(99L)
                .publishedFileId(7008L)
                .title("Active obsolete allowed")
                .versionNo("3.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build();
        DccControlledFileDO obsoleteDeniedFile = DccControlledFileDO.builder()
                .id(1009L)
                .categoryId(11L)
                .directoryId(20L)
                .requesterId(99L)
                .publishedFileId(7009L)
                .title("Active obsolete denied")
                .versionNo("3.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build();
        when(controlledFileMapper.selectById(1008L)).thenReturn(obsoleteAllowedFile);
        when(controlledFileMapper.selectById(1009L)).thenReturn(obsoleteDeniedFile);
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.OBSOLETE))
                .thenReturn(true);
        when(permissionSupport.hasCategoryPermission(11L, 99L, DccFileCategoryPermissionActionEnum.OBSOLETE))
                .thenReturn(false);
        DccControlledFilePageReqVO reqVO = pageReq();
        when(controlledFileMapper.selectWorkflowList(reqVO))
                .thenReturn(List.of(obsoleteAllowedFile, obsoleteDeniedFile));

        DccControlledFileRespVO allowedResp = queryService.getControlledFile(99L, 1008L);
        DccControlledFileRespVO deniedResp = queryService.getControlledFile(99L, 1009L);

        assertPageProjectionMatches(99L, reqVO, allowedResp);
        assertPageProjectionMatches(99L, reqVO, deniedResp);
        assertFalse(Boolean.TRUE.equals(allowedResp.getActionProjection().getActionLocked()));
        assertEquals(List.of("VIEW", "PREVIEW", "OBSOLETE"), allowedResp.getActionProjection().getAllowedActions());
        assertTrue(Boolean.TRUE.equals(allowedResp.getCanObsolete()));
        assertFalse(Boolean.TRUE.equals(deniedResp.getActionProjection().getActionLocked()));
        assertEquals(List.of("VIEW", "PREVIEW"), deniedResp.getActionProjection().getAllowedActions());
        assertFalse(Boolean.TRUE.equals(deniedResp.getCanObsolete()));
    }

    @Test
    void pendingProjectionShouldFailFastWhenStableIdentityMissing() {
        when(controlledFileMapper.selectById(1010L)).thenReturn(DccControlledFileDO.builder()
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(99L)
                .title("Pending projection without file id")
                .versionNo("2.0")
                .processInstanceId("pi-1010")
                .status(DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus())
                .build());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> queryService.getControlledFile(99L, 1010L));

        assertTrue(ex.getMessage().contains("controlledFileId"));
    }

    @Test
    void pendingProjectionShouldFailFastWhenPendingProcessInstanceMissing() {
        when(controlledFileMapper.selectById(1011L)).thenReturn(DccControlledFileDO.builder()
                .id(1011L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(99L)
                .title("Pending projection without process instance")
                .versionNo("2.0")
                .status(DccControlledFileStatusEnum.PENDING_MATRIX_REVIEW.getStatus())
                .build());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> queryService.getControlledFile(99L, 1011L));

        assertTrue(ex.getMessage().contains("processInstanceId"));
    }

    @Test
    void activeProjectionShouldBeConsistentBetweenDetailPageAndBrowserPage() {
        DccControlledFileDO activeFile = DccControlledFileDO.builder()
                .id(1012L)
                .masterId(812L)
                .categoryId(12L)
                .directoryId(20L)
                .requesterId(99L)
                .publishedFileId(7012L)
                .title("Active browser projection")
                .versionNo("3.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build();
        when(controlledFileMapper.selectById(1012L)).thenReturn(activeFile);
        DccControlledFilePageReqVO pageReqVO = new DccControlledFilePageReqVO();
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(10);
        DccControlledFilePageReqVO browserReqVO = new DccControlledFilePageReqVO();
        browserReqVO.setPageNo(1);
        browserReqVO.setPageSize(10);
        when(directoryAccessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(true);
        when(controlledFileMapper.selectWorkflowList(pageReqVO)).thenReturn(List.of(activeFile));
        when(controlledFileMapper.selectBrowserSummaryList(browserReqVO)).thenReturn(List.of(activeFile));
        when(controlledFileMapper.selectListByMasterId(812L)).thenReturn(List.of(activeFile));
        when(permissionSupport.hasCategoryPermission(12L, 99L, DccFileCategoryPermissionActionEnum.OBSOLETE))
                .thenReturn(true);
        when(projectCodeAssignmentMapper.selectActiveProjectCodeIdsByAssigneeUserId(eq(99L), any(LocalDateTime.class)))
                .thenReturn(List.of());

        DccControlledFileRespVO detail = queryService.getControlledFile(99L, 1012L);
        DccControlledFileRespVO pageRow = queryService.getControlledFilePage(99L, pageReqVO).getList().get(0);
        DccControlledFileRespVO browserRow = queryService.getControlledFileBrowserPage(99L, browserReqVO)
                .getList().get(0);

        assertEquals(detail.getActionProjection(), pageRow.getActionProjection());
        assertEquals(detail.getActionProjection(), browserRow.getActionProjection());
        assertEquals(List.of("VIEW", "PREVIEW", "DOWNLOAD", "OBSOLETE"),
                detail.getActionProjection().getAllowedActions());
    }

    @Test
    void getControlledFilePage_excludesObsoleteRowsForOrdinaryUser() {
        DccControlledFilePageReqVO reqVO = new DccControlledFilePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setDirectoryId(20L);
        when(directoryAccessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(false);
        when(controlledFileMapper.selectWorkflowList(reqVO)).thenReturn(List.of(
                DccControlledFileDO.builder()
                        .id(900L)
                        .categoryId(10L)
                        .directoryId(20L)
                        .requesterId(11L)
                        .title("Visible")
                        .versionNo("2.0")
                        .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                        .effectiveDate(LocalDate.of(2026, 5, 1))
                        .build(),
                DccControlledFileDO.builder()
                        .id(901L)
                        .categoryId(10L)
                        .directoryId(20L)
                        .requesterId(12L)
                        .title("Obsolete")
                        .versionNo("1.0")
                        .status(DccControlledFileStatusEnum.OBSOLETE.getStatus())
                        .effectiveDate(LocalDate.of(2025, 5, 1))
                        .build()));
        when(controlledFileMapper.selectWorkflowList(any(DccControlledFilePageReqVO.class), eq(Set.of(20L))))
                .thenReturn(List.of(
                        DccControlledFileDO.builder()
                                .id(900L)
                                .categoryId(10L)
                                .directoryId(20L)
                                .requesterId(11L)
                                .title("Visible")
                                .versionNo("2.0")
                                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                                .effectiveDate(LocalDate.of(2026, 5, 1))
                                .build(),
                        DccControlledFileDO.builder()
                                .id(901L)
                                .categoryId(10L)
                                .directoryId(20L)
                                .requesterId(12L)
                                .title("Obsolete")
                                .versionNo("1.0")
                                .status(DccControlledFileStatusEnum.OBSOLETE.getStatus())
                                .effectiveDate(LocalDate.of(2025, 5, 1))
                                .build()));
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.OBSOLETE))
                .thenReturn(false);

        PageResult<DccControlledFileRespVO> result = queryService.getControlledFilePage(99L, reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getList().size());
        assertEquals(900L, result.getList().get(0).getId());
        assertFalse(Boolean.TRUE.equals(result.getList().get(0).getCanDownload()));
    }

    @Test
    void getControlledFilePage_includeDescendantDirectories_returnsFilesFromParentSubtree() {
        DccControlledFilePageReqVO reqVO = new DccControlledFilePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setDirectoryId(20L);
        reqVO.setIncludeDescendantDirectories(true);
        when(directoryAccessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(true);
        when(directoryMapper.selectEnabledList()).thenReturn(List.of(
                directory(20L, null, "父目录"),
                directory(21L, 20L, "子目录A"),
                directory(22L, 20L, "子目录B")));
        when(controlledFileMapper.selectWorkflowList(any(DccControlledFilePageReqVO.class), eq(Set.of(20L, 21L, 22L))))
                .thenReturn(List.of(
                        DccControlledFileDO.builder()
                                .id(910L)
                                .categoryId(10L)
                                .directoryId(21L)
                                .requesterId(11L)
                                .title("A")
                                .versionNo("1.0")
                                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                                .effectiveDate(LocalDate.of(2026, 5, 1))
                                .build(),
                        DccControlledFileDO.builder()
                                .id(911L)
                                .categoryId(10L)
                                .directoryId(22L)
                                .requesterId(12L)
                                .title("B")
                                .versionNo("1.0")
                                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                                .effectiveDate(LocalDate.of(2026, 5, 2))
                                .build()));

        PageResult<DccControlledFileRespVO> result = queryService.getControlledFilePage(99L, reqVO);

        assertEquals(2L, result.getTotal());
        assertEquals(List.of(910L, 911L), result.getList().stream().map(DccControlledFileRespVO::getId).toList());
    }

    @Test
    void getControlledFilePage_includeDescendantDirectories_preservesProcessTypeFilter() {
        DccControlledFilePageReqVO reqVO = new DccControlledFilePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setDirectoryId(20L);
        reqVO.setProcessType(DccControlledFileProcessTypeEnum.EXTERNAL_REVIEW.getCode());
        reqVO.setIncludeDescendantDirectories(true);
        when(directoryAccessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(true);
        when(directoryMapper.selectEnabledList()).thenReturn(List.of(
                directory(20L, null, "父目录"),
                directory(21L, 20L, "子目录A")));
        when(controlledFileMapper.selectWorkflowList(any(DccControlledFilePageReqVO.class), eq(Set.of(20L, 21L))))
                .thenReturn(List.of());

        queryService.getControlledFilePage(99L, reqVO);

        ArgumentCaptor<DccControlledFilePageReqVO> reqCaptor = ArgumentCaptor.forClass(DccControlledFilePageReqVO.class);
        verify(controlledFileMapper).selectWorkflowList(reqCaptor.capture(), eq(Set.of(20L, 21L)));
        assertNull(reqCaptor.getValue().getDirectoryId());
        assertEquals(DccControlledFileProcessTypeEnum.EXTERNAL_REVIEW.getCode(), reqCaptor.getValue().getProcessType());
    }

    @Test
    void getControlledFilePage_includeDescendantDirectories_forOrdinaryUserFiltersRequestedSubtreeByViewMatrix() {
        DccControlledFilePageReqVO reqVO = new DccControlledFilePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setDirectoryId(20L);
        reqVO.setIncludeDescendantDirectories(true);
        when(directoryAccessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(false);
        when(directoryMapper.selectEnabledList()).thenReturn(List.of(
                directory(20L, null, "父目录"),
                directory(21L, 20L, "授权目录"),
                directory(22L, 20L, "未授权目录")));
        when(controlledFileMapper.selectWorkflowList(any(DccControlledFilePageReqVO.class), eq(Set.of(20L, 21L, 22L))))
                .thenReturn(List.of(
                        DccControlledFileDO.builder()
                                .id(920L)
                                .categoryId(10L)
                                .directoryId(21L)
                                .requesterId(11L)
                                .title("授权文件")
                                .versionNo("1.0")
                                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                                .effectiveDate(LocalDate.of(2026, 5, 3))
                                .build(),
                        DccControlledFileDO.builder()
                                .id(921L)
                                .categoryId(10L)
                                .directoryId(22L)
                                .requesterId(12L)
                                .title("未授权文件")
                                .versionNo("1.0")
                                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                                .effectiveDate(LocalDate.of(2026, 5, 4))
                                .build()));
        when(viewMatrixAccessService.canAccessCurrentViewMatrix(eq(99L), argThat(file -> Long.valueOf(920L).equals(file.getId()))))
                .thenReturn(true);
        when(viewMatrixAccessService.canAccessCurrentViewMatrix(eq(99L), argThat(file -> Long.valueOf(921L).equals(file.getId()))))
                .thenReturn(false);

        PageResult<DccControlledFileRespVO> result = queryService.getControlledFilePage(99L, reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals(920L, result.getList().get(0).getId());
    }

    @Test
    void getControlledFileBrowserPage_withoutIncludeDescendantDirectories_onlyReturnsCurrentDirectoryRows() {
        DccControlledFilePageReqVO reqVO = new DccControlledFilePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setDirectoryId(20L);
        reqVO.setLatestVersionOnly(Boolean.TRUE);
        when(directoryAccessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(true);
        when(controlledFileMapper.selectBrowserSummaryList(any(DccControlledFilePageReqVO.class), eq(Set.of(20L))))
                .thenReturn(List.of(
                        DccControlledFileDO.builder()
                                .id(925L)
                                .masterId(725L)
                                .categoryId(10L)
                                .directoryId(20L)
                                .requesterId(11L)
                                .publishedFileId(510L)
                                .title("父目录文件")
                                .fileNumber("FI-925")
                                .versionNo("1.0")
                                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                                .effectiveDate(LocalDate.of(2026, 5, 5))
                                .build()));
        when(controlledFileMapper.selectListByMasterId(725L)).thenReturn(List.of(
                DccControlledFileDO.builder()
                        .id(925L)
                        .masterId(725L)
                        .categoryId(10L)
                        .directoryId(20L)
                        .requesterId(11L)
                        .publishedFileId(510L)
                        .title("父目录文件")
                        .fileNumber("FI-925")
                        .versionNo("1.0")
                        .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                        .effectiveDate(LocalDate.of(2026, 5, 5))
                        .build()));
        when(directoryMapper.selectEnabledList()).thenReturn(List.of(
                directory(20L, null, "父目录"),
                directory(21L, 20L, "子目录")));
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.DOWNLOAD))
                .thenReturn(true);

        PageResult<DccControlledFileRespVO> result = queryService.getControlledFileBrowserPage(99L, reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals(List.of(925L), result.getList().stream().map(DccControlledFileRespVO::getId).toList());
        ArgumentCaptor<DccControlledFilePageReqVO> reqCaptor = ArgumentCaptor.forClass(DccControlledFilePageReqVO.class);
        verify(controlledFileMapper).selectBrowserSummaryList(reqCaptor.capture(), eq(Set.of(20L)));
        assertNull(reqCaptor.getValue().getDirectoryId());
    }

    @Test
    void getControlledFileBrowserPage_includeDescendantDirectoriesTrue_returnsCurrentAndChildDirectoryRows() {
        DccControlledFilePageReqVO reqVO = new DccControlledFilePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setDirectoryId(20L);
        reqVO.setIncludeDescendantDirectories(Boolean.TRUE);
        reqVO.setLatestVersionOnly(Boolean.TRUE);
        when(directoryAccessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(true);
        when(directoryMapper.selectEnabledList()).thenReturn(List.of(
                directory(20L, null, "父目录"),
                directory(21L, 20L, "子目录")));
        when(controlledFileMapper.selectBrowserSummaryList(any(DccControlledFilePageReqVO.class), eq(Set.of(20L, 21L))))
                .thenReturn(List.of(
                        DccControlledFileDO.builder()
                                .id(925L)
                                .masterId(725L)
                                .categoryId(10L)
                                .directoryId(20L)
                                .requesterId(11L)
                                .publishedFileId(510L)
                                .title("父目录文件")
                                .fileNumber("FI-925")
                                .versionNo("1.0")
                                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                                .effectiveDate(LocalDate.of(2026, 5, 5))
                                .build(),
                        DccControlledFileDO.builder()
                                .id(926L)
                                .masterId(726L)
                                .categoryId(10L)
                                .directoryId(21L)
                                .requesterId(12L)
                                .publishedFileId(511L)
                                .title("子目录文件")
                                .fileNumber("FI-926")
                                .versionNo("1.0")
                                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                                .effectiveDate(LocalDate.of(2026, 5, 6))
                                .build()));
        when(controlledFileMapper.selectListByMasterId(725L)).thenReturn(List.of(
                DccControlledFileDO.builder()
                        .id(925L)
                        .masterId(725L)
                        .categoryId(10L)
                        .directoryId(20L)
                        .requesterId(11L)
                        .publishedFileId(510L)
                        .title("父目录文件")
                        .fileNumber("FI-925")
                        .versionNo("1.0")
                        .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                        .effectiveDate(LocalDate.of(2026, 5, 5))
                        .build()));
        when(controlledFileMapper.selectListByMasterId(726L)).thenReturn(List.of(
                DccControlledFileDO.builder()
                        .id(926L)
                        .masterId(726L)
                        .categoryId(10L)
                        .directoryId(21L)
                        .requesterId(12L)
                        .publishedFileId(511L)
                        .title("子目录文件")
                        .fileNumber("FI-926")
                        .versionNo("1.0")
                        .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                        .effectiveDate(LocalDate.of(2026, 5, 6))
                        .build()));
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.DOWNLOAD))
                .thenReturn(true);

        PageResult<DccControlledFileRespVO> result = queryService.getControlledFileBrowserPage(99L, reqVO);

        assertEquals(2L, result.getTotal());
        assertEquals(List.of(925L, 926L), result.getList().stream().map(DccControlledFileRespVO::getId).toList());
        ArgumentCaptor<DccControlledFilePageReqVO> reqCaptor = ArgumentCaptor.forClass(DccControlledFilePageReqVO.class);
        verify(controlledFileMapper).selectBrowserSummaryList(reqCaptor.capture(), eq(Set.of(20L, 21L)));
        assertNull(reqCaptor.getValue().getDirectoryId());
    }

    @Test
    void getControlledFileBrowserPage_filtersConfiguredExtensionBlacklistForEveryUser() {
        DccControlledFilePageReqVO reqVO = new DccControlledFilePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setDirectoryId(20L);
        reqVO.setLatestVersionOnly(Boolean.TRUE);
        DccControlledFileDO pdfFile = DccControlledFileDO.builder()
                .id(970L)
                .masterId(770L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(11L)
                .publishedFileId(570L)
                .title("公开文件.pdf")
                .fileName("公开文件.pdf")
                .fileNumber("FI-970")
                .versionNo("1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .effectiveDate(LocalDate.of(2026, 7, 11))
                .build();
        DccControlledFileDO dbFile = DccControlledFileDO.builder()
                .id(971L)
                .masterId(771L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(12L)
                .publishedFileId(571L)
                .title("数据库.db")
                .fileName("数据库.DB")
                .fileNumber("FI-971")
                .versionNo("1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .effectiveDate(LocalDate.of(2026, 7, 11))
                .build();
        DccControlledFileDO pycFile = DccControlledFileDO.builder()
                .id(972L)
                .masterId(772L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(13L)
                .publishedFileId(572L)
                .title("缓存.pyc")
                .fileName("缓存.pyc")
                .fileNumber("FI-972")
                .versionNo("1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .effectiveDate(LocalDate.of(2026, 7, 11))
                .build();
        when(browserSettingsService.getBlacklistedExtensionPatterns()).thenReturn(List.of("*.db", "*.pyc"));
        when(directoryAccessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(false);
        when(controlledFileMapper.selectBrowserSummaryList(any(DccControlledFilePageReqVO.class), eq(Set.of(20L))))
                .thenReturn(List.of(pdfFile, dbFile, pycFile));
        when(viewMatrixAccessService.canAccessCurrentViewMatrix(eq(99L), any(DccControlledFileDO.class))).thenReturn(true);
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.DOWNLOAD))
                .thenReturn(true);
        when(controlledFileMapper.selectListByMasterId(770L)).thenReturn(List.of(pdfFile));

        PageResult<DccControlledFileRespVO> result = queryService.getControlledFileBrowserPage(99L, reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals(List.of(970L), result.getList().stream().map(DccControlledFileRespVO::getId).toList());
        verify(browserSettingsService, atLeastOnce()).getBlacklistedExtensionPatterns();
    }

    @Test
    void getControlledFileBrowserPage_reusesViewMatrixAccessByCategoryWithinOneRequest() {
        DccControlledFilePageReqVO reqVO = new DccControlledFilePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setLatestVersionOnly(Boolean.TRUE);
        DccControlledFileDO firstFile = DccControlledFileDO.builder()
                .id(973L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(11L)
                .publishedFileId(573L)
                .title("同类文件 A")
                .fileName("同类文件A.pdf")
                .fileNumber("FI-973")
                .versionNo("1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .effectiveDate(LocalDate.of(2026, 7, 13))
                .build();
        DccControlledFileDO secondFile = DccControlledFileDO.builder()
                .id(974L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(12L)
                .publishedFileId(574L)
                .title("同类文件 B")
                .fileName("同类文件B.pdf")
                .fileNumber("FI-974")
                .versionNo("1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .effectiveDate(LocalDate.of(2026, 7, 13))
                .build();
        when(directoryAccessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(false);
        when(controlledFileMapper.selectWorkflowList(any(DccControlledFilePageReqVO.class)))
                .thenReturn(List.of(firstFile, secondFile));
        when(controlledFileMapper.selectBrowserSummaryList(any(DccControlledFilePageReqVO.class)))
                .thenReturn(List.of(firstFile, secondFile));
        when(viewMatrixAccessService.canAccessCurrentViewMatrix(eq(99L), any(DccControlledFileDO.class)))
                .thenReturn(true);
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.DOWNLOAD))
                .thenReturn(true);

        PageResult<DccControlledFileRespVO> result = queryService.getControlledFileBrowserPage(99L, reqVO);

        assertEquals(2L, result.getTotal());
        assertEquals(List.of(973L, 974L), result.getList().stream().map(DccControlledFileRespVO::getId).toList());
        verify(viewMatrixAccessService, times(1))
                .canAccessCurrentViewMatrix(eq(99L), any(DccControlledFileDO.class));
    }

    @Test
    void getControlledFileBrowserPage_activeProjectCodeAssignmentRestrictsCandidatesAndShowsAssignedRows() {
        DccControlledFilePageReqVO reqVO = new DccControlledFilePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setLatestVersionOnly(Boolean.TRUE);
        DccControlledFileDO assignedFile = DccControlledFileDO.builder()
                .id(981L)
                .masterId(781L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(11L)
                .publishedFileId(581L)
                .title("已分配项目代码文件")
                .fileName("assigned.pdf")
                .fileNumber("ASSIGNED-981")
                .versionNo("1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .effectiveDate(LocalDate.of(2026, 7, 13))
                .build();
        when(directoryAccessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(false);
        when(projectCodeAssignmentMapper.selectActiveProjectCodeIdsByAssigneeUserId(eq(99L), any(LocalDateTime.class)))
                .thenReturn(List.of(3001L));
        when(projectCodeAssignmentFileMapper.selectActiveControlledFileIdsByAssigneeUserId(eq(99L), any(LocalDateTime.class)))
                .thenReturn(List.of(981L));
        when(controlledFileMapper.selectBrowserSummaryList(any(DccControlledFilePageReqVO.class), isNull(), eq(Set.of(981L))))
                .thenReturn(List.of(assignedFile));
        when(controlledFileMapper.selectListByMasterId(781L)).thenReturn(List.of(assignedFile));
        when(viewMatrixAccessService.canAccessCurrentViewMatrix(eq(99L), any(DccControlledFileDO.class)))
                .thenReturn(false);

        PageResult<DccControlledFileRespVO> result = queryService.getControlledFileBrowserPage(99L, reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals(List.of(981L), result.getList().stream().map(DccControlledFileRespVO::getId).toList());
        verify(controlledFileMapper, never()).selectBrowserSummaryList(any(DccControlledFilePageReqVO.class));
        verify(controlledFileMapper).selectBrowserSummaryList(any(DccControlledFilePageReqVO.class), isNull(), eq(Set.of(981L)));
    }

    @Test
    void getControlledFileBrowserPage_activeProjectCodeAssignmentRestrictsDirectoryManagerWhenNotDocControl() {
        DccControlledFilePageReqVO reqVO = new DccControlledFilePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setLatestVersionOnly(Boolean.TRUE);
        DccControlledFileDO assignedFile = DccControlledFileDO.builder()
                .id(981L)
                .masterId(781L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(11L)
                .publishedFileId(581L)
                .title("已分配项目代码文件")
                .fileName("assigned.pdf")
                .fileNumber("ASSIGNED-981")
                .versionNo("1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .effectiveDate(LocalDate.of(2026, 7, 13))
                .build();
        DccControlledFileDO outsideFile = DccControlledFileDO.builder()
                .id(982L)
                .masterId(782L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(12L)
                .publishedFileId(582L)
                .title("范围外文件")
                .fileName("outside.pdf")
                .fileNumber("OUTSIDE-982")
                .versionNo("1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .effectiveDate(LocalDate.of(2026, 7, 13))
                .build();
        when(directoryAccessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(true);
        when(projectCodeAssignmentMapper.selectActiveProjectCodeIdsByAssigneeUserId(eq(99L), any(LocalDateTime.class)))
                .thenReturn(List.of(3001L));
        when(projectCodeAssignmentFileMapper.selectActiveControlledFileIdsByAssigneeUserId(eq(99L), any(LocalDateTime.class)))
                .thenReturn(List.of(981L));
        when(controlledFileMapper.selectBrowserSummaryList(any(DccControlledFilePageReqVO.class)))
                .thenReturn(List.of(assignedFile, outsideFile));
        when(controlledFileMapper.selectBrowserSummaryList(any(DccControlledFilePageReqVO.class), isNull(), eq(Set.of(981L))))
                .thenReturn(List.of(assignedFile));
        when(controlledFileMapper.selectListByMasterId(781L)).thenReturn(List.of(assignedFile));
        when(controlledFileMapper.selectListByMasterId(782L)).thenReturn(List.of(outsideFile));

        PageResult<DccControlledFileRespVO> result = queryService.getControlledFileBrowserPage(99L, reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals(List.of(981L), result.getList().stream().map(DccControlledFileRespVO::getId).toList());
        verify(controlledFileMapper).selectBrowserSummaryList(any(DccControlledFilePageReqVO.class), isNull(), eq(Set.of(981L)));
    }

    @Test
    void getControlledFileBrowserPage_activeProjectCodeAssignmentRestrictsDocControlRoleUser() {
        DccControlledFilePageReqVO reqVO = new DccControlledFilePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setLatestVersionOnly(Boolean.TRUE);
        DccControlledFileDO assignedFile = DccControlledFileDO.builder()
                .id(981L)
                .masterId(781L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(11L)
                .publishedFileId(581L)
                .title("已分配项目代码文件")
                .fileName("assigned.pdf")
                .fileNumber("ASSIGNED-981")
                .versionNo("1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .effectiveDate(LocalDate.of(2026, 7, 13))
                .build();
        DccControlledFileDO outsideFile = DccControlledFileDO.builder()
                .id(982L)
                .masterId(782L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(12L)
                .publishedFileId(582L)
                .title("范围外文件")
                .fileName("outside.pdf")
                .fileNumber("OUTSIDE-982")
                .versionNo("1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .effectiveDate(LocalDate.of(2026, 7, 13))
                .build();
        when(directoryAccessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(true);
        when(permissionApi.hasAnyRoles(99L, DccControlledFileMetadataUpdateService.DOC_CONTROL_ROLE_CODE))
                .thenReturn(true);
        when(projectCodeAssignmentMapper.selectActiveProjectCodeIdsByAssigneeUserId(eq(99L), any(LocalDateTime.class)))
                .thenReturn(List.of(3001L));
        when(projectCodeAssignmentFileMapper.selectActiveControlledFileIdsByAssigneeUserId(eq(99L), any(LocalDateTime.class)))
                .thenReturn(List.of(981L));
        when(controlledFileMapper.selectBrowserSummaryList(any(DccControlledFilePageReqVO.class), isNull(), eq(Set.of(981L))))
                .thenReturn(List.of(assignedFile));
        when(controlledFileMapper.selectListByMasterId(781L)).thenReturn(List.of(assignedFile));
        when(controlledFileMapper.selectListByMasterId(782L)).thenReturn(List.of(outsideFile));

        PageResult<DccControlledFileRespVO> result = queryService.getControlledFileBrowserPage(99L, reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals(List.of(981L), result.getList().stream().map(DccControlledFileRespVO::getId).toList());
        verify(controlledFileMapper).selectBrowserSummaryList(any(DccControlledFilePageReqVO.class), isNull(), eq(Set.of(981L)));
    }

    @Test
    void getControlledFilePage_latestVersionOnly_returnsLatestRowPerMasterForBrowserView() {
        DccControlledFilePageReqVO reqVO = new DccControlledFilePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setDirectoryId(20L);
        reqVO.setLatestVersionOnly(Boolean.TRUE);
        when(directoryAccessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(true);
        when(controlledFileMapper.selectWorkflowList(any(DccControlledFilePageReqVO.class), eq(Set.of(20L)))).thenReturn(List.of(
                DccControlledFileDO.builder()
                        .id(930L)
                        .masterId(700L)
                        .categoryId(10L)
                        .directoryId(20L)
                        .requesterId(11L)
                        .publishedFileId(503L)
                        .title("SOP-001")
                        .fileNumber("FI-001")
                        .versionNo("2.0")
                        .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                        .effectiveDate(LocalDate.of(2026, 5, 14))
                        .build(),
                DccControlledFileDO.builder()
                        .id(929L)
                        .masterId(700L)
                        .categoryId(10L)
                        .directoryId(20L)
                        .requesterId(11L)
                        .publishedFileId(502L)
                        .title("SOP-001")
                        .fileNumber("FI-001")
                        .versionNo("1.0")
                        .status(DccControlledFileStatusEnum.SUPERSEDED.getStatus())
                        .effectiveDate(LocalDate.of(2025, 5, 14))
                        .build(),
                DccControlledFileDO.builder()
                        .id(931L)
                        .masterId(701L)
                        .categoryId(10L)
                        .directoryId(20L)
                        .requesterId(12L)
                        .publishedFileId(504L)
                        .title("SOP-002")
                        .fileNumber("FI-002")
                        .versionNo("1.0")
                        .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                        .effectiveDate(LocalDate.of(2026, 5, 15))
                        .build()));
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.VIEW))
                .thenReturn(true);
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.DOWNLOAD))
                .thenReturn(true);
        when(controlledFileMapper.selectListByMasterId(700L)).thenReturn(List.of(
                DccControlledFileDO.builder()
                        .id(930L)
                        .masterId(700L)
                        .categoryId(10L)
                        .publishedFileId(503L)
                        .title("SOP-001")
                        .fileNumber("FI-001")
                        .versionNo("2.0")
                        .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                        .effectiveDate(LocalDate.of(2026, 5, 14))
                        .build(),
                DccControlledFileDO.builder()
                        .id(929L)
                        .masterId(700L)
                        .categoryId(10L)
                        .publishedFileId(502L)
                        .title("SOP-001")
                        .fileNumber("FI-001")
                        .versionNo("1.0")
                        .status(DccControlledFileStatusEnum.SUPERSEDED.getStatus())
                        .effectiveDate(LocalDate.of(2025, 5, 14))
                        .build()));
        when(controlledFileMapper.selectListByMasterId(701L)).thenReturn(List.of(
                DccControlledFileDO.builder()
                        .id(931L)
                        .masterId(701L)
                        .categoryId(10L)
                        .publishedFileId(504L)
                        .title("SOP-002")
                        .fileNumber("FI-002")
                        .versionNo("1.0")
                        .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                        .effectiveDate(LocalDate.of(2026, 5, 15))
                        .build()));

        PageResult<DccControlledFileRespVO> result = queryService.getControlledFilePage(99L, reqVO);

        assertEquals(2L, result.getTotal());
        assertEquals(List.of(930L, 931L), result.getList().stream().map(DccControlledFileRespVO::getId).toList());
        assertEquals(2, result.getList().get(0).getVersionHistory().size());
        assertEquals("2.0", result.getList().get(0).getVersionNo());
    }

    @Test
    void getControlledFilePage_slicesBeforeBuildingResponseRows() {
        DccControlledFilePageReqVO reqVO = new DccControlledFilePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(2);
        reqVO.setDirectoryId(20L);
        reqVO.setLatestVersionOnly(Boolean.TRUE);
        List<DccControlledFileDO> candidates = java.util.stream.IntStream.rangeClosed(1, 12)
                .mapToObj(index -> DccControlledFileDO.builder()
                        .id(1000L + index)
                        .masterId(2000L + index)
                        .categoryId(10L)
                        .directoryId(20L)
                        .requesterId(11L)
                        .title("NAS-" + index)
                        .fileNumber("NAS-" + index)
                        .versionNo("1.0")
                        .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                        .effectiveDate(LocalDate.of(2026, 6, 1))
                        .build())
                .toList();
        when(directoryAccessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(true);
        when(controlledFileMapper.selectWorkflowList(any(DccControlledFilePageReqVO.class), eq(Set.of(20L))))
                .thenReturn(candidates);
        candidates.stream().limit(2).forEach(file -> when(controlledFileMapper.selectListByMasterId(file.getMasterId()))
                .thenReturn(List.of(file)));

        PageResult<DccControlledFileRespVO> result = queryService.getControlledFilePage(99L, reqVO);

        assertEquals(12L, result.getTotal());
        assertEquals(List.of(1001L, 1002L), result.getList().stream().map(DccControlledFileRespVO::getId).toList());
        verify(controlledFileMapper, times(2)).selectListByMasterId(anyLong());
    }

    @Test
    void getControlledFilePage_pageSizeNoneReturnsAllVisibleRows() {
        DccControlledFilePageReqVO reqVO = new DccControlledFilePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        reqVO.setDirectoryId(20L);
        reqVO.setLatestVersionOnly(Boolean.FALSE);
        List<DccControlledFileDO> candidates = java.util.stream.IntStream.rangeClosed(1, 12)
                .mapToObj(index -> DccControlledFileDO.builder()
                        .id(1000L + index)
                        .masterId(2000L + index)
                        .categoryId(10L)
                        .directoryId(20L)
                        .requesterId(11L)
                        .title("NAS-ALL-" + index)
                        .fileNumber("NAS-ALL-" + index)
                        .versionNo("1.0")
                        .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                        .effectiveDate(LocalDate.of(2026, 6, 1))
                        .build())
                .toList();
        when(directoryAccessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(true);
        when(controlledFileMapper.selectWorkflowList(any(DccControlledFilePageReqVO.class), eq(Set.of(20L))))
                .thenReturn(candidates);
        candidates.forEach(file -> when(controlledFileMapper.selectListByMasterId(file.getMasterId()))
                .thenReturn(List.of(file)));

        PageResult<DccControlledFileRespVO> result = queryService.getControlledFilePage(99L, reqVO);

        assertEquals(12L, result.getTotal());
        assertEquals(candidates.stream().map(DccControlledFileDO::getId).toList(),
                result.getList().stream().map(DccControlledFileRespVO::getId).toList());
        verify(controlledFileMapper, times(12)).selectListByMasterId(anyLong());
    }

    @Test
    void getControlledFilePage_reusesDirectoryManagementPermissionDuringCandidateFiltering() {
        DccControlledFilePageReqVO reqVO = new DccControlledFilePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(2);
        reqVO.setDirectoryId(20L);
        List<DccControlledFileDO> candidates = java.util.stream.IntStream.rangeClosed(1, 12)
                .mapToObj(index -> DccControlledFileDO.builder()
                        .id(1100L + index)
                        .masterId(2100L + index)
                        .categoryId(10L)
                        .directoryId(20L)
                        .requesterId(11L)
                        .title("NAS-CACHED-" + index)
                        .fileNumber("NAS-CACHED-" + index)
                        .versionNo("1.0")
                        .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                        .effectiveDate(LocalDate.of(2026, 6, 1))
                        .build())
                .toList();
        when(directoryAccessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(true);
        when(controlledFileMapper.selectWorkflowList(any(DccControlledFilePageReqVO.class), eq(Set.of(20L))))
                .thenReturn(candidates);
        candidates.stream().limit(2).forEach(file -> when(controlledFileMapper.selectListByMasterId(file.getMasterId()))
                .thenReturn(List.of(file)));

        PageResult<DccControlledFileRespVO> result = queryService.getControlledFilePage(99L, reqVO);

        assertEquals(12L, result.getTotal());
        assertEquals(List.of(1101L, 1102L), result.getList().stream().map(DccControlledFileRespVO::getId).toList());
        verify(directoryAccessPermissionService, times(1)).hasDirectoryManagementPermission(99L);
    }

    @Test
    void getControlledFilePage_withoutLatestVersionOnly_keepsRequesterRecordRows() {
        DccControlledFilePageReqVO reqVO = new DccControlledFilePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setRequesterId(99L);
        when(directoryAccessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(false);
        when(controlledFileMapper.selectWorkflowList(reqVO)).thenReturn(List.of(
                DccControlledFileDO.builder()
                        .id(940L)
                        .masterId(710L)
                        .categoryId(10L)
                        .directoryId(20L)
                        .requesterId(99L)
                        .title("SOP-003")
                        .fileNumber("FI-003")
                        .versionNo("2.0")
                        .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                        .effectiveDate(LocalDate.of(2026, 5, 16))
                        .build(),
                DccControlledFileDO.builder()
                        .id(939L)
                        .masterId(710L)
                        .categoryId(10L)
                        .directoryId(20L)
                        .requesterId(99L)
                        .title("SOP-003")
                        .fileNumber("FI-003")
                        .versionNo("1.0")
                        .status(DccControlledFileStatusEnum.SUPERSEDED.getStatus())
                        .effectiveDate(LocalDate.of(2025, 5, 16))
                        .build()));
        when(controlledFileMapper.selectListByMasterId(710L)).thenReturn(List.of(
                DccControlledFileDO.builder()
                        .id(940L)
                        .masterId(710L)
                        .categoryId(10L)
                        .title("SOP-003")
                        .fileNumber("FI-003")
                        .versionNo("2.0")
                        .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                        .effectiveDate(LocalDate.of(2026, 5, 16))
                        .build(),
                DccControlledFileDO.builder()
                        .id(939L)
                        .masterId(710L)
                        .categoryId(10L)
                        .title("SOP-003")
                        .fileNumber("FI-003")
                        .versionNo("1.0")
                        .status(DccControlledFileStatusEnum.SUPERSEDED.getStatus())
                        .effectiveDate(LocalDate.of(2025, 5, 16))
                        .build()));

        PageResult<DccControlledFileRespVO> result = queryService.getControlledFilePage(99L, reqVO);

        assertEquals(2L, result.getTotal());
        assertEquals(List.of(940L, 939L), result.getList().stream().map(DccControlledFileRespVO::getId).toList());
    }

    @Test
    void getControlledFilePage_returnsProductName() {
        DccControlledFilePageReqVO reqVO = new DccControlledFilePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        DccControlledFileDO file = DccControlledFileDO.builder()
                .id(950L)
                .masterId(720L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(88L)
                .title("SOP-004")
                .fileName("SOP-004")
                .fileNumber("FI-004")
                .productName("离心泵")
                .dccProjectCodeId(3000L)
                .fileTypeTaxonomyId(8803L)
                .fileTypeLevel1("一级")
                .fileTypeLevel2("二级")
                .fileTypeLevel3("三级")
                .versionNo("1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .effectiveDate(LocalDate.of(2026, 6, 4))
                .build();
        when(directoryAccessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(true);
        when(controlledFileMapper.selectWorkflowList(reqVO)).thenReturn(List.of(file));
        when(controlledFileMapper.selectListByMasterId(720L)).thenReturn(List.of(file));

        PageResult<DccControlledFileRespVO> result = queryService.getControlledFilePage(99L, reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals("离心泵", result.getList().get(0).getProductName());
        assertEquals(3000L, result.getList().get(0).getDccProjectCodeId());
        assertEquals(8803L, result.getList().get(0).getFileTypeTaxonomyId());
        assertEquals("一级", result.getList().get(0).getFileTypeLevel1());
        assertEquals("二级", result.getList().get(0).getFileTypeLevel2());
        assertEquals("三级", result.getList().get(0).getFileTypeLevel3());
    }

    @Test
    void getControlledFilePage_resolvesLegacyFileTypeTaxonomyIdFromStoredNames() {
        DccControlledFilePageReqVO reqVO = new DccControlledFilePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        DccControlledFileDO file = DccControlledFileDO.builder()
                .id(960L)
                .masterId(721L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(88L)
                .title("SOP-LEGACY")
                .fileName("SOP-LEGACY")
                .fileNumber("FI-LEGACY")
                .fileTypeLevel1("一级")
                .fileTypeLevel2("二级")
                .fileTypeLevel3("三级")
                .versionNo("1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .effectiveDate(LocalDate.of(2026, 7, 20))
                .build();
        when(directoryAccessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(true);
        when(controlledFileMapper.selectWorkflowList(reqVO)).thenReturn(List.of(file));
        when(controlledFileMapper.selectListByMasterId(721L)).thenReturn(List.of(file));
        when(fileTypeTaxonomyAdminService.resolveActiveIdByPath("一级", "二级", "三级", null, null))
                .thenReturn(8803L);

        PageResult<DccControlledFileRespVO> result = queryService.getControlledFilePage(99L, reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals(8803L, result.getList().get(0).getFileTypeTaxonomyId());
        assertEquals("一级", result.getList().get(0).getFileTypeLevel1());
        assertEquals("二级", result.getList().get(0).getFileTypeLevel2());
        assertEquals("三级", result.getList().get(0).getFileTypeLevel3());
    }

    @Test
    void getControlledFilePage_withLatestVersionOnlyFiltersKeywordAfterLatestAggregation() {
        DccControlledFilePageReqVO reqVO = new DccControlledFilePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setLatestVersionOnly(Boolean.TRUE);
        reqVO.setKeyword("NEW-SOP");
        DccControlledFileDO latestMatching = DccControlledFileDO.builder()
                .id(970L)
                .masterId(730L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(88L)
                .title("NEW-SOP 最新版")
                .fileName("latest.pdf")
                .fileNumber("DCC-970")
                .versionNo("2.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .effectiveDate(LocalDate.of(2026, 6, 17))
                .build();
        DccControlledFileDO latestWithoutKeyword = DccControlledFileDO.builder()
                .id(971L)
                .masterId(731L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(88L)
                .title("当前有效工艺规程")
                .fileName("current.pdf")
                .fileNumber("DCC-971")
                .versionNo("2.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .effectiveDate(LocalDate.of(2026, 6, 17))
                .build();
        DccControlledFileDO historicalKeywordOnly = DccControlledFileDO.builder()
                .id(972L)
                .masterId(731L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(88L)
                .title("NEW-SOP 历史版")
                .fileName("old.pdf")
                .fileNumber("DCC-OLD")
                .versionNo("1.0")
                .status(DccControlledFileStatusEnum.SUPERSEDED.getStatus())
                .effectiveDate(LocalDate.of(2025, 6, 17))
                .build();
        when(directoryAccessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(true);
        when(controlledFileMapper.selectWorkflowList(any(DccControlledFilePageReqVO.class)))
                .thenReturn(List.of(latestMatching, latestWithoutKeyword, historicalKeywordOnly));
        when(controlledFileMapper.selectListByMasterId(730L)).thenReturn(List.of(latestMatching));

        PageResult<DccControlledFileRespVO> result = queryService.getControlledFilePage(99L, reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals(List.of(970L), result.getList().stream().map(DccControlledFileRespVO::getId).toList());
        ArgumentCaptor<DccControlledFilePageReqVO> queryCaptor = ArgumentCaptor.forClass(DccControlledFilePageReqVO.class);
        verify(controlledFileMapper).selectWorkflowList(queryCaptor.capture());
        assertNull(queryCaptor.getValue().getKeyword());
    }

    @Test
    void getControlledFileBrowserPage_returnsLightweightRowsWithoutDetailSummaries() {
        DccControlledFilePageReqVO reqVO = new DccControlledFilePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setDirectoryId(20L);
        reqVO.setLatestVersionOnly(Boolean.TRUE);
        DccControlledFileDO file = DccControlledFileDO.builder()
                .id(960L)
                .masterId(730L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(88L)
                .publishedFileId(560L)
                .title("受控浏览摘要")
                .fileName("受控浏览摘要.pdf")
                .fileNumber("FI-960")
                .productName("输注泵")
                .versionNo("3.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .effectiveDate(LocalDate.of(2026, 6, 17))
                .build();
        when(directoryAccessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(true);
        when(controlledFileMapper.selectBrowserSummaryList(any(DccControlledFilePageReqVO.class), eq(Set.of(20L))))
                .thenReturn(List.of(file));
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.DOWNLOAD))
                .thenReturn(true);
        when(controlledFileMapper.selectListByMasterId(730L)).thenReturn(List.of(file));

        PageResult<DccControlledFileRespVO> result = queryService.getControlledFileBrowserPage(99L, reqVO);

        assertEquals(1L, result.getTotal());
        DccControlledFileRespVO row = result.getList().get(0);
        assertEquals(960L, row.getId());
        assertEquals("输注泵", row.getProductName());
        assertEquals(1, row.getVersionHistory().size());
        assertTrue(Boolean.TRUE.equals(row.getCanPreview()));
        assertTrue(Boolean.TRUE.equals(row.getCanDownload()));
        assertNull(row.getDistributionStatuses());
        assertNull(row.getTrainingStatuses());
        assertNull(row.getSignatureSummaries());
        assertNull(row.getExternalReview());
        assertNull(row.getRouteSnapshots());
        verify(distributionMapper, never()).selectListByControlledFileId(anyLong());
        verify(trainingMapper, never()).selectListByControlledFileId(anyLong());
        verify(signatureMapper, never()).selectListByControlledFileId(anyLong());
        verify(externalReviewMapper, never()).selectByControlledFileId(anyLong());
    }

    @Test
    void getControlledFileBrowserPage_directoryManagerCanPreviewAndDownloadPublishedRowsWithoutProductOrCategoryRule() {
        DccControlledFilePageReqVO reqVO = new DccControlledFilePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setDirectoryId(20L);
        reqVO.setLatestVersionOnly(Boolean.TRUE);
        DccControlledFileDO file = DccControlledFileDO.builder()
                .id(961L)
                .masterId(731L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(88L)
                .publishedFileId(561L)
                .title("文控受控浏览")
                .fileName("文控受控浏览.pdf")
                .fileNumber("FI-961")
                .versionNo("1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build();
        when(directoryAccessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(true);
        when(controlledFileMapper.selectBrowserSummaryList(any(DccControlledFilePageReqVO.class), eq(Set.of(20L))))
                .thenReturn(List.of(file));
        when(viewMatrixAccessService.canAccessCurrentViewMatrix(eq(99L), any(DccControlledFileDO.class))).thenReturn(false);
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.DOWNLOAD))
                .thenReturn(false);
        when(controlledFileMapper.selectListByMasterId(731L)).thenReturn(List.of(file));

        PageResult<DccControlledFileRespVO> result = queryService.getControlledFileBrowserPage(99L, reqVO);

        DccControlledFileRespVO row = result.getList().get(0);
        assertTrue(Boolean.TRUE.equals(row.getCanPreview()));
        assertTrue(Boolean.TRUE.equals(row.getCanDownload()));
        assertEquals(1, row.getVersionHistory().size());
        assertTrue(Boolean.TRUE.equals(row.getVersionHistory().get(0).getCanPreview()));
        assertTrue(Boolean.TRUE.equals(row.getVersionHistory().get(0).getCanDownload()));
    }

    @Test
    void getControlledFile_includesVersionHistoryRouteSnapshotsAndDownstreamSummaries() {
        LocalDateTime signedAt = LocalDateTime.of(2026, 5, 14, 9, 0);
        when(controlledFileMapper.selectById(902L)).thenReturn(DccControlledFileDO.builder()
                .id(902L)
                .masterId(700L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(88L)
                .trainingRecordFileId(600L)
                .publishedFileId(503L)
                .title("SOP-001")
                .fileNumber("FI-001")
                .versionNo("2.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .effectiveDate(LocalDate.of(2026, 5, 14))
                .build());
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.VIEW))
                .thenReturn(true);
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.DOWNLOAD))
                .thenReturn(true);
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.OBSOLETE))
                .thenReturn(true);
        when(routeSnapshotMapper.selectListByControlledFileId(902L)).thenReturn(List.of(
                DccControlledFileRouteSnapshotDO.builder()
                        .id(1001L)
                        .controlledFileId(902L)
                        .stageNo(1)
                        .stageCode("DOC_CONTROL_REVIEW")
                        .stageName("Doc Control Review")
                        .resolvedUserIds("11,12")
                        .build()));
        when(controlledFileMapper.selectListByMasterId(700L)).thenReturn(List.of(
                DccControlledFileDO.builder()
                        .id(902L)
                        .masterId(700L)
                        .categoryId(10L)
                        .title("SOP-001")
                        .fileNumber("FI-001")
                        .versionNo("2.0")
                        .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                        .effectiveDate(LocalDate.of(2026, 5, 14))
                        .build(),
                DccControlledFileDO.builder()
                        .id(801L)
                        .masterId(700L)
                        .categoryId(10L)
                        .title("SOP-001")
                        .fileNumber("FI-001")
                        .versionNo("1.0")
                        .status(DccControlledFileStatusEnum.SUPERSEDED.getStatus())
                        .effectiveDate(LocalDate.of(2025, 5, 14))
                        .build()));
        when(distributionMapper.selectListByControlledFileId(902L)).thenReturn(List.of(
                DccControlledFileDistributionDO.builder()
                        .id(301L)
                        .controlledFileId(902L)
                        .departmentId(500L)
                        .distributionMedium(DccDistributionMediumEnum.PAPER.getCode())
                        .acknowledgedBy(88L)
                        .acknowledgedAt(signedAt)
                        .status("PENDING")
                        .build()));
        when(distributionRecipientMapper.selectListByDistributionId(301L)).thenReturn(List.of(
                DccControlledFileDistributionRecipientDO.builder()
                        .id(401L)
                        .distributionId(301L)
                        .userId(91L)
                        .build()));
        when(trainingMapper.selectListByControlledFileId(902L)).thenReturn(List.of(
                DccControlledFileTrainingDO.builder()
                        .id(302L)
                        .controlledFileId(902L)
                        .departmentId(600L)
                        .status(DccControlledFileTrainingStatusEnum.ACKNOWLEDGED.getCode())
                        .build()));
        when(trainingAssignmentMapper.selectListByTrainingId(302L)).thenReturn(List.of(
                DccControlledFileTrainingAssignmentDO.builder()
                        .id(402L)
                        .trainingId(302L)
                        .userId(99L)
                        .status(DccControlledFileTrainingStatusEnum.ACKNOWLEDGED.getCode())
                        .acknowledgedAt(signedAt)
                        .build()));
        when(trainingProgressMapper.selectByControlledFileIdAndUserId(902L, 99L)).thenReturn(
                DccControlledFileTrainingProgressDO.builder()
                        .id(9001L)
                        .controlledFileId(902L)
                        .userId(99L)
                        .requiredViewSeconds(600)
                        .accumulatedViewSeconds(600)
                        .acknowledgedAt(signedAt)
                        .build());
        when(signatureMapper.selectListByControlledFileId(902L)).thenReturn(List.of(
                DccControlledFileSignatureDO.builder()
                        .id(501L)
                        .controlledFileId(902L)
                        .actorId(77L)
                        .actionType("APPROVE")
                        .signatureMode("PASSWORD")
                        .signedAt(signedAt)
                        .comment("Approved")
                        .build()));

        DccControlledFileRespVO respVO = queryService.getControlledFile(99L, 902L);

        assertEquals(2, respVO.getVersionHistory().size());
        assertEquals(1, respVO.getRouteSnapshots().size());
        assertEquals(1, respVO.getDistributionStatuses().size());
        assertEquals(1, respVO.getTrainingStatuses().size());
        assertEquals(1, respVO.getSignatureSummaries().size());
        assertTrue(Boolean.TRUE.equals(respVO.getCanPreview()));
        assertTrue(Boolean.TRUE.equals(respVO.getCanDownload()));
        assertTrue(Boolean.TRUE.equals(respVO.getCanObsolete()));
        assertFalse(Boolean.TRUE.equals(respVO.getHasPendingTrainingAcknowledgement()));
        assertEquals(600, respVO.getTrainingStatuses().get(0).getAssignments().get(0).getAccumulatedViewSeconds());
        assertEquals(600, respVO.getTrainingStatuses().get(0).getAssignments().get(0).getRequiredViewSeconds());
        assertFalse(Boolean.TRUE.equals(respVO.getTrainingStatuses().get(0).getAssignments().get(0).getEligibleToAcknowledge()));
    }

    @Test
    void getControlledFile_pendingManualDistribution_exposesManualReleaseFlag() {
        when(controlledFileMapper.selectById(903L)).thenReturn(DccControlledFileDO.builder()
                .id(903L)
                .masterId(701L)
                .categoryId(10L)
                .directoryId(20L)
                .requesterId(88L)
                .publishedFileId(504L)
                .title("SOP-002")
                .fileNumber("FI-002")
                .versionNo("3.0")
                .status(DccControlledFileStatusEnum.PENDING_MANUAL_DISTRIBUTION.getStatus())
                .effectiveDate(LocalDate.of(2026, 5, 15))
                .build());
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.VIEW))
                .thenReturn(true);
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.DISTRIBUTE))
                .thenReturn(true);
        when(trainingMapper.selectListByControlledFileId(903L)).thenReturn(List.of(
                DccControlledFileTrainingDO.builder()
                        .id(303L)
                        .controlledFileId(903L)
                        .departmentId(600L)
                        .status(DccControlledFileTrainingStatusEnum.ACKNOWLEDGED.getCode())
                        .build()));
        when(trainingAssignmentMapper.selectListByTrainingId(303L)).thenReturn(List.of(
                DccControlledFileTrainingAssignmentDO.builder()
                        .id(403L)
                        .trainingId(303L)
                        .userId(99L)
                        .status(DccControlledFileTrainingStatusEnum.ACKNOWLEDGED.getCode())
                        .acknowledgedAt(LocalDateTime.of(2026, 5, 15, 9, 0))
                        .build()));
        when(trainingProgressMapper.selectByControlledFileIdAndUserId(903L, 99L)).thenReturn(
                DccControlledFileTrainingProgressDO.builder()
                        .id(9002L)
                        .controlledFileId(903L)
                        .userId(99L)
                        .requiredViewSeconds(600)
                        .accumulatedViewSeconds(600)
                        .acknowledgedAt(LocalDateTime.of(2026, 5, 15, 9, 0))
                        .build());

        DccControlledFileRespVO respVO = queryService.getControlledFile(99L, 903L);

        assertTrue(Boolean.TRUE.equals(respVO.getCanManualRelease()));
        assertFalse(Boolean.TRUE.equals(respVO.getCanDownload()));
        assertFalse(Boolean.TRUE.equals(respVO.getCanPreview()));
    }

    private DccFileDirectoryDO directory(Long id, Long parentId, String name) {
        return DccFileDirectoryDO.builder()
                .id(id)
                .parentId(parentId)
                .code("DIR-" + id)
                .name(name)
                .active(Boolean.TRUE)
                .sort(1)
                .build();
    }

    private DccControlledFilePageReqVO pageReq() {
        DccControlledFilePageReqVO reqVO = new DccControlledFilePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        return reqVO;
    }

    private void assertPageProjectionMatches(Long userId, DccControlledFilePageReqVO reqVO,
                                             DccControlledFileRespVO detail) {
        PageResult<DccControlledFileRespVO> page = queryService.getControlledFilePage(userId, reqVO);
        DccControlledFileRespVO pageRow = page.getList().stream()
                .filter(row -> detail.getId().equals(row.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals(detail.getActionProjection(), pageRow.getActionProjection());
    }

    private void stubEmptyDetailRelations(Long controlledFileId) {
        lenient().when(routeSnapshotMapper.selectListByControlledFileId(controlledFileId)).thenReturn(List.of());
        lenient().when(distributionMapper.selectListByControlledFileId(controlledFileId)).thenReturn(List.of());
        lenient().when(trainingMapper.selectListByControlledFileId(controlledFileId)).thenReturn(List.of());
        lenient().when(signatureMapper.selectListByControlledFileId(controlledFileId)).thenReturn(List.of());
    }

    private DccPreviewAccessResult previewAccessResult() {
        return new DccPreviewAccessResult(
                88001L,
                ACCESS_EVENT_CODE,
                99001L,
                WATERMARK_TRACE_CODE,
                VIEWER_TOKEN,
                VIEWER_TOKEN_ID,
                VIEWER_TOKEN_NONCE,
                LocalDateTime.of(2026, 5, 28, 3, 30),
                LocalDateTime.of(2026, 5, 28, 3, 45),
                "{\"traceCode\":\"" + WATERMARK_TRACE_CODE + "\"}");
    }

    private DccControlledFileAccessEventDO accessEvent(Long controlledFileId, String versionNo) {
        return DccControlledFileAccessEventDO.builder()
                .id(88001L)
                .accessEventCode(ACCESS_EVENT_CODE)
                .controlledFileId(controlledFileId)
                .fileVersionNo(versionNo)
                .userId(99L)
                .accessType("PREVIEW")
                .purpose("CONTROLLED_PREVIEW")
                .result("SUCCESS")
                .build();
    }

    private DccControlledFileWatermarkTraceDO watermarkTrace(Long controlledFileId, String versionNo) {
        return DccControlledFileWatermarkTraceDO.builder()
                .id(99001L)
                .traceCode(WATERMARK_TRACE_CODE)
                .accessEventId(88001L)
                .accessEventCode(ACCESS_EVENT_CODE)
                .controlledFileId(controlledFileId)
                .fileVersionNo(versionNo)
                .userId(99L)
                .build();
    }

    private void stubActiveDownloadFile(Long controlledFileId, Long publishedFileId, String fileName) {
        when(controlledFileMapper.selectById(controlledFileId)).thenReturn(DccControlledFileDO.builder()
                .id(controlledFileId)
                .categoryId(10L)
                .directoryId(20L)
                .publishedFileId(publishedFileId)
                .fileNumber("SOP-" + controlledFileId)
                .versionNo("1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build());
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.DOWNLOAD))
                .thenReturn(true);
        when(fileMapper.selectById(publishedFileId)).thenReturn(FileDO.builder()
                .id(publishedFileId)
                .configId(1L)
                .path("dcc/published/" + fileName)
                .name(fileName)
                .type("application/pdf")
                .build());
    }

    private void stubDownloadAccessEventInsert(Long accessEventId) {
        doAnswer(invocation -> {
            DccControlledFileAccessEventDO event = invocation.getArgument(0);
            event.setId(accessEventId);
            return 1;
        }).when(accessEventMapper).insert(any(DccControlledFileAccessEventDO.class));
    }

    private void stubDownloadRecordInsert(Long recordId) {
        doAnswer(invocation -> {
            DccControlledFileDownloadRecordDO record = invocation.getArgument(0);
            record.setId(recordId);
            return 1;
        }).when(downloadRecordMapper).insert(any(DccControlledFileDownloadRecordDO.class));
    }

    private UpdateWrapper assertFailureUpdateClearsReturnableEvidence(String failureCode) {
        ArgumentCaptor<UpdateWrapper> updateCaptor = ArgumentCaptor.forClass(UpdateWrapper.class);
        verify(downloadRecordMapper, atLeastOnce()).update(isNull(), updateCaptor.capture());
        UpdateWrapper failureUpdate = updateCaptor.getValue();
        String sqlSet = failureUpdate.getSqlSet();
        assertTrue(sqlSet.contains("encryption_policy_version="), sqlSet);
        assertTrue(sqlSet.contains("artifact_id="), sqlSet);
        assertTrue(sqlSet.contains("cipher_file_ref="), sqlSet);
        assertTrue(sqlSet.contains("plain_sha256="), sqlSet);
        assertTrue(sqlSet.contains("cipher_sha256="), sqlSet);
        assertTrue(sqlSet.contains("returned_at="), sqlSet);
        Collection<Object> values = failureUpdate.getParamNameValuePairs().values();
        assertTrue(values.stream().filter(Objects::isNull).count() >= 6, values::toString);
        assertTrue(values.contains("FAILED"), values::toString);
        assertTrue(values.contains(failureCode), values::toString);
        return failureUpdate;
    }

    private DccRequestAuditContext auditContext(String requestId) {
        return new DccRequestAuditContext(SOURCE_IP, USER_AGENT, requestId);
    }

    private String sha256Hex(byte[] bytes) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is unavailable", ex);
        }
    }

    private void assertNoForbiddenProperties(Class<?> responseType) throws IntrospectionException {
        Set<String> propertyNames = Arrays.stream(Introspector.getBeanInfo(responseType, Object.class).getPropertyDescriptors())
                .map(PropertyDescriptor::getName)
                .collect(Collectors.toSet());
        Set<String> exposedForbidden = ORDINARY_RESPONSE_FORBIDDEN_CAPABILITY_FIELDS.stream()
                .filter(propertyNames::contains)
                .collect(Collectors.toSet());
        assertTrue(exposedForbidden.isEmpty(),
                () -> responseType.getSimpleName() + " exposes forbidden file capability fields " + exposedForbidden);
    }
}
