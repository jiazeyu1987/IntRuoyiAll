package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileApproveTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSubmitReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileTrainingRecordReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileUploadPreviewReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileUploadRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledPreviewWatermarkOverlayRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledPreviewWatermarkRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccExternalFileReviewApproveTaskReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryLifecycleStageEnum;
import cn.iocoder.yudao.module.dcc.service.audit.DccAccessBoundaryLogCreateCommand;
import cn.iocoder.yudao.module.dcc.service.audit.DccControlledFileAccessAuditService;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadSizePolicyMatch;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadSizePolicyScopeType;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadSizePolicyService;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketCreateCommand;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketCreated;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketService;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessDeniedException;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessOperation;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessReference;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessRequest;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessService;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import io.swagger.v3.oas.annotations.media.Schema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.beans.Introspector;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_CATEGORY_DISABLED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_DRAWING_PDF_FILE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SOURCE_FILE_TYPE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_UPLOAD_PURPOSE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_UPLOAD_PREVIEW_SINGLE_FILE_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_UPLOAD_SLOT_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_UPLOAD_SIZE_EXCEEDED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_UPLOAD_SIZE_POLICY_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_CATEGORY_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_CATEGORY_LIFECYCLE_STAGE_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccControlledFileUploadApiTest extends BaseMockitoUnitTest {

    private static final BusinessFileAccessReference TEMP_REFERENCE = new BusinessFileAccessReference(
            "dcc", "DCC_TEMPORARY_UPLOAD", 501L, "TEMP-501", 31L, null);

    @Mock
    private FileService fileService;
    @Mock
    private FileMapper fileMapper;
    @Mock
    private DccControlledPreviewWatermarkService watermarkService;
    @Mock
    private DccOnlyOfficePreviewTokenService onlyOfficePreviewTokenService;
    @Mock
    private DccUploadSizePolicyService uploadSizePolicyService;
    @Mock
    private DccUploadTicketService uploadTicketService;
    @Mock
    private DccControlledFileAccessAuditService accessAuditService;
    @Mock
    private DccControlledFileCategoryPermissionSupport permissionSupport;
    @Mock
    private DccFileCategoryMapper categoryMapper;
    @Mock
    private BusinessFileAccessService businessFileAccessService;

    @InjectMocks
    private DccControlledFileUploadServiceImpl uploadService;

    @BeforeEach
    void setUpCategory() {
        TenantContextHolder.setTenantId(31L);
        lenient().when(categoryMapper.selectById(10L)).thenReturn(DccFileCategoryDO.builder()
                .id(10L)
                .active(true)
                .lifecycleStage(DccFileCategoryLifecycleStageEnum.PLAN.getCode())
                .build());
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void uploadResponseContract_exposesUploadTicketAndSignedOnlyOfficeDocumentUrlWithoutFileId() throws Exception {
        assertTrue(hasBeanProperty(DccControlledFileUploadRespVO.class, "uploadTicket"));
        assertFalse(hasBeanProperty(DccControlledFileUploadRespVO.class, "fileId"));
        assertTrue(hasBeanProperty(DccControlledFileUploadRespVO.class, "onlyofficeDocumentUrl"));
    }

    @Test
    void submitRequestContract_hidesInternalRawFileIdsFromJsonAndOpenApi() throws Exception {
        assertInternalOnlyContractField(DccControlledFileSubmitReqVO.class, "originalFileId");
        assertInternalOnlyContractField(DccControlledFileSubmitReqVO.class, "sourceFileId");
        assertInternalOnlyContractField(DccControlledFileSubmitReqVO.class, "drawingPdfFileId");

        DccControlledFileSubmitReqVO reqVO = new ObjectMapper().readValue("""
                {
                  "originalFileId": 100,
                  "sourceFileId": 101,
                  "drawingPdfFileId": 102,
                  "originalUploadTicket": "UT-ORIGINAL",
                  "sourceUploadTicket": "UT-SOURCE",
                  "drawingPdfUploadTicket": "UT-DRAWING"
                }
                """, DccControlledFileSubmitReqVO.class);

        assertNull(reqVO.getOriginalFileId());
        assertNull(reqVO.getSourceFileId());
        assertNull(reqVO.getDrawingPdfFileId());
        assertEquals("UT-ORIGINAL", reqVO.getOriginalUploadTicket());
        assertEquals("UT-SOURCE", reqVO.getSourceUploadTicket());
        assertEquals("UT-DRAWING", reqVO.getDrawingPdfUploadTicket());
    }

    @Test
    void externalReviewApproveRequestContract_hidesInternalOutputFileIdFromJsonAndOpenApi() throws Exception {
        assertInternalOnlyContractField(DccExternalFileReviewApproveTaskReqVO.class, "outputFileId");

        DccExternalFileReviewApproveTaskReqVO reqVO = new ObjectMapper().readValue("""
                {
                  "outputFileId": 700,
                  "outputUploadTicket": "UT-OUTPUT"
                }
                """, DccExternalFileReviewApproveTaskReqVO.class);

        assertNull(reqVO.getOutputFileId());
        assertEquals("UT-OUTPUT", reqVO.getOutputUploadTicket());
    }

    @Test
    void approveRequestContract_hidesInternalStampedAndTrainingFileIdsFromJsonAndOpenApi() throws Exception {
        assertInternalOnlyContractField(DccControlledFileApproveTaskReqVO.class, "stampedPdfFileId");
        assertInternalOnlyContractField(DccControlledFileApproveTaskReqVO.class, "trainingRecordFileId");

        DccControlledFileApproveTaskReqVO reqVO = new ObjectMapper().readValue("""
                {
                  "stampedPdfFileId": 800,
                  "trainingRecordFileId": 801,
                  "sessionId": "session-stamped",
                  "stampedPdfUploadTicket": "UT-STAMPED"
                }
                """, DccControlledFileApproveTaskReqVO.class);

        assertNull(reqVO.getStampedPdfFileId());
        assertNull(reqVO.getTrainingRecordFileId());
        assertEquals("session-stamped", reqVO.getSessionId());
        assertEquals("UT-STAMPED", reqVO.getStampedPdfUploadTicket());
    }

    @Test
    void trainingRecordRequestContract_usesPurposeSpecificUploadTicket() throws Exception {
        assertFalse(hasBeanProperty(DccControlledFileTrainingRecordReqVO.class, "uploadTicket"));
        assertTrue(hasBeanProperty(DccControlledFileTrainingRecordReqVO.class, "trainingRecordUploadTicket"));

        ObjectMapper objectMapper = new ObjectMapper();
        assertThrows(UnrecognizedPropertyException.class, () -> objectMapper.readValue("""
                {
                  "sessionId": "session-training",
                  "uploadTicket": "UT-LEGACY"
                }
                """, DccControlledFileTrainingRecordReqVO.class));

        DccControlledFileTrainingRecordReqVO reqVO = objectMapper.readValue("""
                {
                  "sessionId": "session-training",
                  "trainingRecordUploadTicket": "UT-TRAINING"
                }
                """, DccControlledFileTrainingRecordReqVO.class);

        assertEquals("session-training", reqVO.getSessionId());
        assertEquals("UT-TRAINING", reqVO.getTrainingRecordUploadTicket());
    }

    @Test
    void uploadPreviewRequestContract_hidesSingleFileComputedPropertyFromJsonAndOpenApi() throws Exception {
        Method singleFile = DccControlledFileUploadPreviewReqVO.class.getMethod("isSingleFile");
        assertNotNull(singleFile.getAnnotation(JsonIgnore.class));
        Schema schema = singleFile.getAnnotation(Schema.class);
        assertNotNull(schema);
        assertTrue(schema.hidden());

        assertFalse(hasJsonProperty(DccControlledFileUploadPreviewReqVO.class, "singleFile"));
    }

    @Test
    void uploadPreviewFile_withoutCategoryUploadPermission_successCreatesTicketAndDoesNotExposeFileId() throws Exception {
        ReflectionTestUtils.setField(uploadService, "onlyOfficePreviewProperties", new DccOnlyOfficePreviewProperties());
        DccControlledFileUploadPreviewReqVO reqVO = uploadReq("SOURCE",
                new MockMultipartFile("files", "sample.docx",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "docx".getBytes()));
        mockSizePolicy("SOURCE", 4L);
        when(fileService.createFileAndReturnId(eq("docx".getBytes()), eq("sample.docx"), eq("dcc/original"),
                eq("application/vnd.openxmlformats-officedocument.wordprocessingml.document")))
                .thenReturn(100L);
        when(fileMapper.selectById(100L)).thenReturn(storedFile(100L, "sample.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        when(watermarkService.build(99L, "preview", "sample.docx"))
                .thenReturn(DccControlledPreviewWatermarkRespVO.builder()
                        .label("受控预览")
                        .text("受控预览 | sample.docx")
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
        when(uploadTicketService.createTicket(any())).thenReturn(new DccUploadTicketCreated(
                "UT-20260528-0001", "session-1", "SOURCE", "AVAILABLE",
                LocalDateTime.of(2026, 5, 28, 12, 30), 100L, "sample.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", 4L));

        DccControlledFileUploadRespVO respVO = uploadService.uploadPreviewFile(99L, reqVO,
                auditContext("REQ-UPLOAD-SUCCESS"));

        assertEquals("UT-20260528-0001", respVO.getUploadTicket());
        assertEquals("session-1", respVO.getSessionId());
        assertEquals("SOURCE", respVO.getPurpose());
        assertEquals("AVAILABLE", respVO.getStatus());
        assertEquals("sample.docx", respVO.getFileName());
        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document", respVO.getContentType());
        assertEquals("OFFICE", respVO.getPreviewKind());
        assertEquals(4L, respVO.getFileSize());
        assertEquals("preview", respVO.getWatermark().getPurpose());
        ArgumentCaptor<DccUploadTicketCreateCommand> ticketCaptor =
                ArgumentCaptor.forClass(DccUploadTicketCreateCommand.class);
        verify(uploadTicketService).createTicket(ticketCaptor.capture());
        assertEquals(99L, ticketCaptor.getValue().userId());
        assertEquals(10L, ticketCaptor.getValue().categoryId());
        assertEquals("session-1", ticketCaptor.getValue().sessionId());
        assertEquals("SOURCE", ticketCaptor.getValue().purpose());
        assertEquals(100L, ticketCaptor.getValue().storageFileId());
        assertEquals("REQ-UPLOAD-SUCCESS", ticketCaptor.getValue().requestId());
        ArgumentCaptor<DccAccessBoundaryLogCreateCommand> auditCaptor =
                ArgumentCaptor.forClass(DccAccessBoundaryLogCreateCommand.class);
        verify(accessAuditService).recordBoundaryLog(auditCaptor.capture());
        assertEquals(99L, auditCaptor.getValue().userId());
        assertEquals("UPLOAD", auditCaptor.getValue().actionType());
        assertEquals("SOURCE", auditCaptor.getValue().purpose());
        assertEquals("SUCCESS", auditCaptor.getValue().result());
        assertNull(auditCaptor.getValue().failureCode());
        assertEquals("10.0.0.9", auditCaptor.getValue().sourceIp());
        assertEquals("REQ-UPLOAD-SUCCESS", auditCaptor.getValue().requestId());
        assertEquals("JUnit", auditCaptor.getValue().userAgent());
        verify(permissionSupport, never()).hasCategoryPermission(anyLong(), anyLong(),
                any(DccFileCategoryPermissionActionEnum.class));
    }

    @Test
    void uploadPreviewFile_sourceXlsx_withOnlyOfficeConfigReturnsSignedDocumentUrl() throws Exception {
        DccOnlyOfficePreviewProperties properties = configuredOnlyOfficeProperties();
        ReflectionTestUtils.setField(uploadService, "onlyOfficePreviewProperties", properties);
        DccControlledFileUploadPreviewReqVO reqVO = uploadReq("SOURCE",
                new MockMultipartFile("files", "report.xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "xlsx".getBytes()));
        mockSizePolicy("SOURCE", 4L);
        when(fileService.createFileAndReturnId(eq("xlsx".getBytes()), eq("report.xlsx"), eq("dcc/original"),
                eq("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")))
                .thenReturn(104L);
        when(fileMapper.selectById(104L)).thenReturn(storedFile(104L, "report.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        when(watermarkService.build(99L, "preview", "report.xlsx"))
                .thenReturn(DccControlledPreviewWatermarkRespVO.builder().purpose("preview").build());
        when(uploadTicketService.createTicket(any())).thenReturn(new DccUploadTicketCreated(
                "UT-20260803-0001", "session-1", "SOURCE", "AVAILABLE",
                LocalDateTime.of(2026, 8, 3, 12, 30), 104L, "report.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 4L));
        when(onlyOfficePreviewTokenService.issueBusinessFile(
                DccOnlyOfficePreviewTokenService.AUDIENCE_UPLOAD_PREVIEW,
                BusinessFileAccessOperation.ONLYOFFICE_PREVIEW, 104L, 31L, 99L, null, TEMP_REFERENCE, 300L))
                .thenReturn(new DccOnlyOfficePreviewTokenService.IssuedPreviewToken(
                        "signed-upload-preview-token",
                        new DccOnlyOfficePreviewTokenService.PreviewTokenPayload()));
        when(businessFileAccessService.assertAllowed(any(BusinessFileAccessRequest.class)))
                .thenReturn(java.util.Optional.of(TEMP_REFERENCE));

        DccControlledFileUploadRespVO respVO = uploadService.uploadPreviewFile(99L, reqVO,
                auditContext("REQ-XLSX-ONLYOFFICE"));

        assertEquals("OFFICE", respVO.getPreviewKind());
        assertEquals("http://onlyoffice.local", respVO.getOnlyofficeBaseUrl());
        assertEquals("http://host.docker.internal:48081/admin-api/dcc/controlled-files/upload-preview/104"
                        + "/onlyoffice-file?token=signed-upload-preview-token",
                readBeanProperty(respVO, "onlyofficeDocumentUrl"));
        assertNull(respVO.getPreviewUnavailableReason());
        verify(onlyOfficePreviewTokenService).issueBusinessFile(
                DccOnlyOfficePreviewTokenService.AUDIENCE_UPLOAD_PREVIEW,
                BusinessFileAccessOperation.ONLYOFFICE_PREVIEW, 104L, 31L, 99L, null, TEMP_REFERENCE, 300L);
    }

    @Test
    void readUploadPreviewOnlyOfficeFile_rechecksConvertTokenBeforeFileLookupAndRead() throws Exception {
        ReflectionTestUtils.setField(uploadService, "onlyOfficePreviewProperties", configuredOnlyOfficeProperties());
        DccOnlyOfficePreviewTokenService.PreviewTokenPayload payload =
                new DccOnlyOfficePreviewTokenService.PreviewTokenPayload();
        payload.setTokenId("OT-CONVERT-1");
        payload.setTenantId(31L);
        payload.setServiceIdentity(DccOnlyOfficePreviewTokenService.SERVICE_DCC_PDF_CONVERSION);
        payload.setOperation(BusinessFileAccessOperation.CONVERT.name());
        payload.setInfraFileId(104L);
        when(onlyOfficePreviewTokenService.verifyBusinessFileToken("convert-token",
                DccOnlyOfficePreviewTokenService.AUDIENCE_UPLOAD_PREVIEW, 104L))
                .thenReturn(payload);
        when(businessFileAccessService.assertAllowed(any(BusinessFileAccessRequest.class)))
                .thenThrow(new BusinessFileAccessDeniedException("revoked",
                        BusinessFileAccessOperation.CONVERT, 104L, "dcc"));

        assertThrows(RuntimeException.class,
                () -> uploadService.readUploadPreviewOnlyOfficeFile(104L, "convert-token"));

        verify(fileMapper, never()).selectById(any());
        verify(fileService, never()).getFileContent(any(), any());
    }

    @Test
    void readUploadPreviewOnlyOfficeFile_runsInTokenTenantAndRestoresIgnoredCallerContext() throws Exception {
        TenantContextHolder.setTenantId(88L);
        TenantContextHolder.setIgnore(true);
        ReflectionTestUtils.setField(uploadService, "onlyOfficePreviewProperties", configuredOnlyOfficeProperties());
        DccOnlyOfficePreviewTokenService.PreviewTokenPayload payload =
                new DccOnlyOfficePreviewTokenService.PreviewTokenPayload();
        payload.setTokenId("OT-UPLOAD-1");
        payload.setTenantId(31L);
        payload.setUserId(99L);
        payload.setOperation(BusinessFileAccessOperation.ONLYOFFICE_PREVIEW.name());
        payload.setInfraFileId(104L);
        payload.setProviderId(TEMP_REFERENCE.providerId());
        payload.setBusinessType(TEMP_REFERENCE.businessType());
        payload.setBusinessId(TEMP_REFERENCE.businessId());
        payload.setVersionKey(TEMP_REFERENCE.versionKey());
        when(onlyOfficePreviewTokenService.verifyBusinessFileToken("upload-token",
                DccOnlyOfficePreviewTokenService.AUDIENCE_UPLOAD_PREVIEW, 104L)).thenReturn(payload);
        when(businessFileAccessService.assertAllowed(any(BusinessFileAccessRequest.class)))
                .thenAnswer(invocation -> {
                    assertEquals(31L, TenantContextHolder.getRequiredTenantId());
                    assertTrue(!TenantContextHolder.isIgnore());
                    BusinessFileAccessRequest request = invocation.getArgument(0);
                    assertTrue(request.tokenClaimRequired());
                    assertEquals(TEMP_REFERENCE, request.claim());
                    return java.util.Optional.of(TEMP_REFERENCE);
                });
        when(fileMapper.selectById(104L)).thenReturn(FileDO.builder()
                .id(104L).configId(1L).path("dcc/original/report.docx")
                .name("report.docx").type("application/docx").build());
        when(fileService.getFileContent(1L, "dcc/original/report.docx")).thenReturn("office".getBytes());

        uploadService.readUploadPreviewOnlyOfficeFile(104L, "upload-token");

        assertEquals(88L, TenantContextHolder.getRequiredTenantId());
        assertTrue(TenantContextHolder.isIgnore());
    }

    @Test
    void uploadPreviewFile_missingPurpose_throwsBeforePolicyOrStorage() {
        DccControlledFileUploadPreviewReqVO reqVO = uploadReq(null,
                new MockMultipartFile("files", "stamped.pdf", "application/pdf", "%PDF-1.4".getBytes()));

        assertServiceException(() -> uploadService.uploadPreviewFile(99L, reqVO, auditContext("REQ-MISSING-PURPOSE")),
                CONTROLLED_FILE_UPLOAD_PURPOSE_INVALID);

        verify(uploadSizePolicyService, never()).validateUploadSize(any(), any(), anyLong(), any());
        verify(fileService, never()).createFile(any(), any(), any(), any());
        verify(uploadTicketService, never()).createTicket(any());
    }

    @Test
    void uploadPreviewFile_missingCategory_throwsBeforePolicyOrStorage() {
        DccControlledFileUploadPreviewReqVO reqVO = uploadReq("SOURCE",
                new MockMultipartFile("files", "sample.docx",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "docx".getBytes()));
        reqVO.setCategoryId(999999999L);

        assertServiceException(() -> uploadService.uploadPreviewFile(99L, reqVO,
                auditContext("REQ-CATEGORY-MISSING")), FILE_CATEGORY_NOT_EXISTS);

        verify(uploadSizePolicyService, never()).validateUploadSize(any(), any(), anyLong(), any());
        verify(fileService, never()).createFile(any(), any(), any(), any());
        verify(fileService, never()).createFileAndReturnId(any(), any(), any(), any());
        verify(uploadTicketService, never()).createTicket(any());
    }

    @Test
    void uploadPreviewFile_disabledCategory_throwsBeforePolicyOrStorage() {
        when(categoryMapper.selectById(10L)).thenReturn(DccFileCategoryDO.builder()
                .id(10L)
                .active(false)
                .lifecycleStage(DccFileCategoryLifecycleStageEnum.PLAN.getCode())
                .build());
        DccControlledFileUploadPreviewReqVO reqVO = uploadReq("SOURCE",
                new MockMultipartFile("files", "sample.docx",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "docx".getBytes()));

        assertServiceException(() -> uploadService.uploadPreviewFile(99L, reqVO,
                auditContext("REQ-CATEGORY-DISABLED")), CONTROLLED_FILE_CATEGORY_DISABLED);

        verify(uploadSizePolicyService, never()).validateUploadSize(any(), any(), anyLong(), any());
        verify(fileService, never()).createFile(any(), any(), any(), any());
        verify(fileService, never()).createFileAndReturnId(any(), any(), any(), any());
        verify(uploadTicketService, never()).createTicket(any());
    }

    @Test
    void uploadPreviewFile_invalidCategoryLifecycleStage_throwsBeforePolicyOrStorage() {
        when(categoryMapper.selectById(10L)).thenReturn(DccFileCategoryDO.builder()
                .id(10L)
                .active(true)
                .lifecycleStage("ARCHIVED")
                .build());
        DccControlledFileUploadPreviewReqVO reqVO = uploadReq("SOURCE",
                new MockMultipartFile("files", "sample.docx",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "docx".getBytes()));

        assertServiceException(() -> uploadService.uploadPreviewFile(99L, reqVO,
                auditContext("REQ-CATEGORY-LIFECYCLE")), FILE_CATEGORY_LIFECYCLE_STAGE_INVALID, "ARCHIVED");

        verify(uploadSizePolicyService, never()).validateUploadSize(any(), any(), anyLong(), any());
        verify(fileService, never()).createFile(any(), any(), any(), any());
        verify(fileService, never()).createFileAndReturnId(any(), any(), any(), any());
        verify(uploadTicketService, never()).createTicket(any());
    }

    @Test
    void uploadPreviewFile_sameContentInActiveSlotReturnsOriginalTicketWithoutSecondStorageWrite() throws Exception {
        ReflectionTestUtils.setField(uploadService, "onlyOfficePreviewProperties", new DccOnlyOfficePreviewProperties());
        DccControlledFileUploadPreviewReqVO reqVO = uploadReq("SOURCE",
                new MockMultipartFile("files", "sample.docx",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "docx".getBytes()));
        mockSizePolicy("SOURCE", 4L);
        when(uploadTicketService.reuseActiveTicketOrReject(any())).thenReturn(new DccUploadTicketCreated(
                "UT-ORIGINAL", "session-1", "SOURCE", "AVAILABLE",
                LocalDateTime.of(2026, 8, 11, 12, 30), 100L, "sample.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", 4L));
        when(fileMapper.selectById(100L)).thenReturn(storedFile(100L, "sample.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        when(watermarkService.build(99L, "preview", "sample.docx"))
                .thenReturn(DccControlledPreviewWatermarkRespVO.builder().purpose("preview").build());

        DccControlledFileUploadRespVO respVO = uploadService.uploadPreviewFile(99L, reqVO,
                auditContext("REQ-IDEMPOTENT-RETRY"));

        assertEquals("UT-ORIGINAL", respVO.getUploadTicket());
        assertEquals(4L, respVO.getFileSize());
        verify(fileService, never()).createFileAndReturnId(any(), any(), any(), any());
        verify(uploadTicketService, never()).createTicket(any());
    }

    @Test
    void uploadPreviewFile_differentContentInActiveSlotRejectsBeforeStorageWrite() throws Exception {
        DccControlledFileUploadPreviewReqVO reqVO = uploadReq("SOURCE",
                new MockMultipartFile("files", "sample.docx",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "new-content".getBytes()));
        mockSizePolicy("SOURCE", 11L);
        when(uploadTicketService.reuseActiveTicketOrReject(any())).thenThrow(
                cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(
                        CONTROLLED_FILE_UPLOAD_SLOT_CONFLICT));

        assertServiceException(() -> uploadService.uploadPreviewFile(99L, reqVO,
                auditContext("REQ-SLOT-CONFLICT")), CONTROLLED_FILE_UPLOAD_SLOT_CONFLICT);

        verify(fileService, never()).createFileAndReturnId(any(), any(), any(), any());
        verify(uploadTicketService, never()).createTicket(any());
    }

    @Test
    void uploadPreviewFile_concurrentWinnerDeletesLosingStorageAndReturnsWinnerTicket() throws Exception {
        ReflectionTestUtils.setField(uploadService, "onlyOfficePreviewProperties", new DccOnlyOfficePreviewProperties());
        DccControlledFileUploadPreviewReqVO reqVO = uploadReq("SOURCE",
                new MockMultipartFile("files", "sample.docx",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "docx".getBytes()));
        mockSizePolicy("SOURCE", 4L);
        when(fileService.createFileAndReturnId(any(), eq("sample.docx"), eq("dcc/original"), any()))
                .thenReturn(105L);
        when(fileMapper.selectById(105L)).thenReturn(storedFile(105L, "sample.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        when(fileMapper.selectById(100L)).thenReturn(storedFile(100L, "sample.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        when(uploadTicketService.createTicket(any())).thenReturn(new DccUploadTicketCreated(
                "UT-WINNER", "session-1", "SOURCE", "AVAILABLE",
                LocalDateTime.of(2026, 8, 11, 12, 30), 100L, "sample.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", 4L));
        when(watermarkService.build(99L, "preview", "sample.docx"))
                .thenReturn(DccControlledPreviewWatermarkRespVO.builder().purpose("preview").build());

        DccControlledFileUploadRespVO respVO = uploadService.uploadPreviewFile(99L, reqVO,
                auditContext("REQ-CONCURRENT-WINNER"));

        assertEquals("UT-WINNER", respVO.getUploadTicket());
        verify(fileService).deleteFile(105L);
    }

    @Test
    void uploadPreviewFile_missingSizePolicy_throwsBeforeStorageOrTicket() throws Exception {
        DccControlledFileUploadPreviewReqVO reqVO = uploadReq("SOURCE",
                new MockMultipartFile("files", "sample.docx",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "docx".getBytes()));
        when(uploadSizePolicyService.validateUploadSize(eq(10L), eq("SOURCE"), eq(4L), nullable(LocalDateTime.class)))
                .thenThrow(cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil
                        .exception(DCC_UPLOAD_SIZE_POLICY_MISSING));

        assertServiceException(() -> uploadService.uploadPreviewFile(99L, reqVO, auditContext("REQ-POLICY-MISSING")),
                DCC_UPLOAD_SIZE_POLICY_MISSING);

        verify(fileService, never()).createFile(any(), any(), any(), any());
        verify(uploadTicketService, never()).createTicket(any());
        ArgumentCaptor<DccAccessBoundaryLogCreateCommand> auditCaptor =
                ArgumentCaptor.forClass(DccAccessBoundaryLogCreateCommand.class);
        verify(accessAuditService).recordBoundaryLog(auditCaptor.capture());
        assertEquals("UPLOAD", auditCaptor.getValue().actionType());
        assertEquals("SOURCE", auditCaptor.getValue().purpose());
        assertEquals("DENIED", auditCaptor.getValue().result());
        assertEquals("DCC_UPLOAD_SIZE_POLICY_MISSING", auditCaptor.getValue().failureCode());
        assertEquals("REQ-POLICY-MISSING", auditCaptor.getValue().requestId());
        assertEquals("10.0.0.9", auditCaptor.getValue().sourceIp());
        assertEquals("JUnit", auditCaptor.getValue().userAgent());
    }

    @Test
    void uploadPreviewFile_sizeExceeded_throwsBeforeReadingStreamOrStorageOrTicket() throws Exception {
        ReadFailingMultipartFile file = new ReadFailingMultipartFile("oversize.docx", 101L);
        DccControlledFileUploadPreviewReqVO reqVO = uploadReq("SOURCE", file);
        when(uploadSizePolicyService.validateUploadSize(eq(10L), eq("SOURCE"), eq(101L),
                nullable(LocalDateTime.class)))
                .thenThrow(cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil
                        .exception(DCC_UPLOAD_SIZE_EXCEEDED, 101L, 100L));

        assertServiceException(() -> uploadService.uploadPreviewFile(99L, reqVO, auditContext("REQ-SIZE-EXCEEDED")),
                DCC_UPLOAD_SIZE_EXCEEDED, 101L, 100L);

        assertFalse(file.inputStreamRequested);
        verify(fileService, never()).createFile(any(), any(), any(), any());
        verify(uploadTicketService, never()).createTicket(any());
        ArgumentCaptor<DccAccessBoundaryLogCreateCommand> auditCaptor =
                ArgumentCaptor.forClass(DccAccessBoundaryLogCreateCommand.class);
        verify(accessAuditService).recordBoundaryLog(auditCaptor.capture());
        assertEquals("UPLOAD", auditCaptor.getValue().actionType());
        assertEquals("SOURCE", auditCaptor.getValue().purpose());
        assertEquals("DENIED", auditCaptor.getValue().result());
        assertEquals("DCC_UPLOAD_SIZE_EXCEEDED", auditCaptor.getValue().failureCode());
        assertEquals("REQ-SIZE-EXCEEDED", auditCaptor.getValue().requestId());
    }

    @Test
    void uploadPreviewFile_sourceZip_throws() {
        DccControlledFileUploadPreviewReqVO reqVO = uploadReq("SOURCE",
                new MockMultipartFile("files", "archive.zip", "application/octet-stream", "zip".getBytes()));

        assertServiceException(() -> uploadService.uploadPreviewFile(99L, reqVO, auditContext("REQ-ZIP")),
                CONTROLLED_FILE_SOURCE_FILE_TYPE_INVALID);
    }

    @Test
    void uploadPreviewFile_drawingPdf_success() throws Exception {
        DccControlledFileUploadPreviewReqVO reqVO = uploadReq("DRAWING_PDF",
                new MockMultipartFile("files", "drawing.pdf", "application/pdf", "%PDF-1.4".getBytes()));
        mockSizePolicy("DRAWING_PDF", 8L);
        when(fileService.createFileAndReturnId(eq("%PDF-1.4".getBytes()), eq("drawing.pdf"), eq("dcc/original"),
                eq("application/pdf"))).thenReturn(102L);
        when(fileMapper.selectById(102L)).thenReturn(storedFile(102L, "drawing.pdf", "application/pdf"));
        when(watermarkService.build(99L, "preview", "drawing.pdf"))
                .thenReturn(DccControlledPreviewWatermarkRespVO.builder().purpose("preview").build());
        when(uploadTicketService.createTicket(any())).thenReturn(new DccUploadTicketCreated(
                "UT-20260528-0002", "session-1", "DRAWING_PDF", "AVAILABLE",
                LocalDateTime.of(2026, 5, 28, 12, 30), 102L, "drawing.pdf", "application/pdf", 8L));

        DccControlledFileUploadRespVO respVO = uploadService.uploadPreviewFile(99L, reqVO,
                auditContext("REQ-DRAWING"));

        assertEquals("UT-20260528-0002", respVO.getUploadTicket());
        assertEquals("drawing.pdf", respVO.getFileName());
        assertEquals("application/pdf", respVO.getContentType());
        assertEquals("PDF", respVO.getPreviewKind());
    }

    @Test
    void uploadPreviewFile_drawingPdfTxt_throws() {
        DccControlledFileUploadPreviewReqVO reqVO = uploadReq("DRAWING_PDF",
                new MockMultipartFile("files", "drawing.txt", "text/plain", "txt".getBytes()));

        assertServiceException(() -> uploadService.uploadPreviewFile(99L, reqVO, auditContext("REQ-DRAWING-TXT")),
                CONTROLLED_FILE_DRAWING_PDF_FILE_INVALID);
    }

    @Test
    void uploadPreviewFile_trainingRecord_allowsGenericPdfWithExplicitPurpose() throws Exception {
        ReflectionTestUtils.setField(uploadService, "onlyOfficePreviewProperties", new DccOnlyOfficePreviewProperties());
        DccControlledFileUploadPreviewReqVO reqVO = uploadReq("TRAINING_RECORD",
                new MockMultipartFile("files", "training.pdf", "application/pdf", "%PDF-1.4".getBytes()));
        mockSizePolicy("TRAINING_RECORD", 8L);
        when(fileService.createFileAndReturnId(eq("%PDF-1.4".getBytes()), eq("training.pdf"), eq("dcc/original"),
                eq("application/pdf"))).thenReturn(103L);
        when(fileMapper.selectById(103L)).thenReturn(storedFile(103L, "training.pdf", "application/pdf"));
        when(watermarkService.build(99L, "preview", "training.pdf"))
                .thenReturn(DccControlledPreviewWatermarkRespVO.builder().purpose("preview").build());
        when(uploadTicketService.createTicket(any())).thenReturn(new DccUploadTicketCreated(
                "UT-20260528-0003", "session-1", "TRAINING_RECORD", "AVAILABLE",
                LocalDateTime.of(2026, 5, 28, 12, 30), 103L, "training.pdf", "application/pdf", 8L));

        DccControlledFileUploadRespVO respVO = uploadService.uploadPreviewFile(99L, reqVO,
                auditContext("REQ-TRAINING"));

        assertEquals("UT-20260528-0003", respVO.getUploadTicket());
        assertEquals("TRAINING_RECORD", respVO.getPurpose());
        assertEquals("PDF", respVO.getPreviewKind());
    }

    @Test
    void uploadPreviewFile_docx_missingOnlyOfficeConfig_doesNotBlockUpload() throws Exception {
        ReflectionTestUtils.setField(uploadService, "onlyOfficePreviewProperties", new DccOnlyOfficePreviewProperties());
        DccControlledFileUploadPreviewReqVO reqVO = uploadReq("SOURCE",
                new MockMultipartFile("files", "sample.docx",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "docx".getBytes()));
        mockSizePolicy("SOURCE", 4L);
        when(fileService.createFileAndReturnId(eq("docx".getBytes()), eq("sample.docx"), eq("dcc/original"),
                eq("application/vnd.openxmlformats-officedocument.wordprocessingml.document")))
                .thenReturn(101L);
        when(fileMapper.selectById(101L)).thenReturn(storedFile(101L, "sample.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        when(watermarkService.build(99L, "preview", "sample.docx"))
                .thenReturn(DccControlledPreviewWatermarkRespVO.builder().purpose("preview").build());
        when(uploadTicketService.createTicket(any())).thenReturn(new DccUploadTicketCreated(
                "UT-20260528-0004", "session-1", "SOURCE", "AVAILABLE",
                LocalDateTime.of(2026, 5, 28, 12, 30), 101L, "sample.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", 4L));

        DccControlledFileUploadRespVO respVO = uploadService.uploadPreviewFile(99L, reqVO,
                auditContext("REQ-OFFICE-MISSING"));

        assertEquals("UT-20260528-0004", respVO.getUploadTicket());
        assertEquals("OFFICE", respVO.getPreviewKind());
        assertEquals("OnlyOffice preview config is missing: yudao.dcc.preview.onlyoffice.base-url is missing",
                respVO.getPreviewUnavailableReason());
        assertNull(respVO.getOnlyofficeBaseUrl());
    }

    @Test
    void uploadPreviewFile_multipleFiles_throws() {
        DccControlledFileUploadPreviewReqVO reqVO = new DccControlledFileUploadPreviewReqVO();
        reqVO.setCategoryId(10L);
        reqVO.setSessionId("session-1");
        reqVO.setPurpose("SOURCE");
        reqVO.setFiles(new MockMultipartFile[]{
                new MockMultipartFile("files", "sample-a.pdf", "application/pdf", "a".getBytes()),
                new MockMultipartFile("files", "sample-b.pdf", "application/pdf", "b".getBytes())
        });

        assertServiceException(() -> uploadService.uploadPreviewFile(99L, reqVO, auditContext("REQ-MULTI")),
                CONTROLLED_FILE_UPLOAD_PREVIEW_SINGLE_FILE_REQUIRED);
    }

    @Test
    void uploadPreviewFile_missingOriginalFilename_throws() {
        DccControlledFileUploadPreviewReqVO reqVO = uploadReq("SOURCE",
                new MockMultipartFile("files", "", "application/octet-stream", "docx".getBytes()));

        assertServiceException(() -> uploadService.uploadPreviewFile(99L, reqVO, auditContext("REQ-NO-NAME")),
                CONTROLLED_FILE_UPLOAD_PREVIEW_SINGLE_FILE_REQUIRED);
    }

    @Test
    void uploadPreviewFile_fileRecordMissing_throwsBeforeTicket() throws Exception {
        DccControlledFileUploadPreviewReqVO reqVO = uploadReq("SOURCE",
                new MockMultipartFile("files", "sample.docx",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "docx".getBytes()));
        mockSizePolicy("SOURCE", 4L);
        when(fileService.createFileAndReturnId(eq("docx".getBytes()), eq("sample.docx"), eq("dcc/original"),
                eq("application/vnd.openxmlformats-officedocument.wordprocessingml.document")))
                .thenReturn(106L);
        when(fileMapper.selectById(106L)).thenReturn(null);

        assertThrows(Exception.class, () -> uploadService.uploadPreviewFile(99L, reqVO,
                auditContext("REQ-FILE-MISSING")));
        verify(uploadTicketService, never()).createTicket(any());
    }

    private DccRequestAuditContext auditContext(String requestId) {
        return new DccRequestAuditContext("10.0.0.9", "JUnit", requestId);
    }

    private DccControlledFileUploadPreviewReqVO uploadReq(String purpose, MultipartFile file) {
        DccControlledFileUploadPreviewReqVO reqVO = new DccControlledFileUploadPreviewReqVO();
        reqVO.setCategoryId(10L);
        reqVO.setSessionId("session-1");
        reqVO.setPurpose(purpose);
        reqVO.setFiles(new MultipartFile[]{file});
        return reqVO;
    }

    private void mockSizePolicy(String purpose, long fileSize) {
        when(uploadSizePolicyService.validateUploadSize(eq(10L), eq(purpose), eq(fileSize), nullable(LocalDateTime.class)))
                .thenReturn(new DccUploadSizePolicyMatch(1L, "policy-1",
                        DccUploadSizePolicyScopeType.CATEGORY_PURPOSE, 10L, purpose, fileSize + 1024L,
                        "v1", 100, 3));
    }

    private FileDO storedFile(Long id, String name, String contentType) {
        return FileDO.builder().id(id).name(name).type(contentType).build();
    }

    private boolean hasBeanProperty(Class<?> clazz, String propertyName) throws Exception {
        return Arrays.stream(Introspector.getBeanInfo(clazz).getPropertyDescriptors())
                .anyMatch(descriptor -> propertyName.equals(descriptor.getName()));
    }

    private Object readBeanProperty(Object bean, String propertyName) throws Exception {
        return Arrays.stream(Introspector.getBeanInfo(bean.getClass()).getPropertyDescriptors())
                .filter(descriptor -> propertyName.equals(descriptor.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing bean property: " + propertyName))
                .getReadMethod()
                .invoke(bean);
    }

    private boolean hasJsonProperty(Class<?> clazz, String propertyName) {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.getSerializationConfig()
                .introspect(objectMapper.constructType(clazz))
                .findProperties()
                .stream()
                .anyMatch(property -> propertyName.equals(property.getName()));
    }

    private void assertInternalOnlyContractField(Class<?> clazz, String fieldName) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        assertNotNull(field.getAnnotation(JsonIgnore.class));
        Schema schema = field.getAnnotation(Schema.class);
        assertNotNull(schema);
        assertTrue(schema.hidden());
    }

    private DccOnlyOfficePreviewProperties configuredOnlyOfficeProperties() {
        DccOnlyOfficePreviewProperties properties = new DccOnlyOfficePreviewProperties();
        properties.setBaseUrl("http://onlyoffice.local/");
        properties.setJwtSecret("unit-test-secret");
        properties.setPublicFileBaseUrl("http://host.docker.internal:48081/");
        return properties;
    }

    private static final class ReadFailingMultipartFile implements MultipartFile {

        private final String originalFilename;
        private final long size;
        private boolean inputStreamRequested;

        private ReadFailingMultipartFile(String originalFilename, long size) {
            this.originalFilename = originalFilename;
            this.size = size;
        }

        @Override
        public String getName() {
            return "files";
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public long getSize() {
            return size;
        }

        @Override
        public byte[] getBytes() {
            throw new AssertionError("must validate DCC upload size before reading bytes");
        }

        @Override
        public InputStream getInputStream() {
            inputStreamRequested = true;
            throw new AssertionError("must validate DCC upload size before reading stream");
        }

        @Override
        public void transferTo(File dest) throws IOException {
            throw new AssertionError("must validate DCC upload size before transferring file");
        }
    }
}
