package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileApproveTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSubmitReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileTrainingRecordReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileUploadPreviewReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileUploadRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledPreviewWatermarkOverlayRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledPreviewWatermarkRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccExternalFileReviewApproveTaskReqVO;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import io.swagger.v3.oas.annotations.media.Schema;
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
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACCESS_DENIED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_DRAWING_PDF_FILE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SOURCE_FILE_TYPE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_UPLOAD_PURPOSE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_UPLOAD_PREVIEW_SINGLE_FILE_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_UPLOAD_SIZE_EXCEEDED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_UPLOAD_SIZE_POLICY_MISSING;
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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccControlledFileUploadApiTest extends BaseMockitoUnitTest {

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

    @InjectMocks
    private DccControlledFileUploadServiceImpl uploadService;

    @BeforeEach
    void setUpCategoryUploadPermission() {
        lenient().when(permissionSupport.hasCategoryPermission(anyLong(), anyLong(),
                any(DccFileCategoryPermissionActionEnum.class))).thenReturn(true);
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
    void uploadPreviewFile_sourceDocx_successCreatesTicketAndDoesNotExposeFileId() throws Exception {
        ReflectionTestUtils.setField(uploadService, "onlyOfficePreviewProperties", new DccOnlyOfficePreviewProperties());
        DccControlledFileUploadPreviewReqVO reqVO = uploadReq("SOURCE",
                new MockMultipartFile("files", "sample.docx",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "docx".getBytes()));
        mockSizePolicy("SOURCE", 4L);
        when(fileService.createFile(eq("docx".getBytes()), eq("sample.docx"), eq("dcc/original"),
                eq("application/vnd.openxmlformats-officedocument.wordprocessingml.document")))
                .thenReturn("http://test.yudao.iocoder.cn/dcc/original/sample.docx");
        doReturn(FileDO.builder().id(100L).name("sample.docx")
                .url("http://test.yudao.iocoder.cn/dcc/original/sample.docx").build())
                .when(fileMapper).selectFirstOne(org.mockito.ArgumentMatchers.<SFunction<FileDO, ?>>any(),
                        eq("http://test.yudao.iocoder.cn/dcc/original/sample.docx"));
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
                LocalDateTime.of(2026, 5, 28, 12, 30)));

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
        when(fileService.createFile(eq("xlsx".getBytes()), eq("report.xlsx"), eq("dcc/original"),
                eq("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")))
                .thenReturn("http://test.yudao.iocoder.cn/dcc/original/report.xlsx");
        doReturn(FileDO.builder().id(104L).name("report.xlsx")
                .url("http://test.yudao.iocoder.cn/dcc/original/report.xlsx").build())
                .when(fileMapper).selectFirstOne(org.mockito.ArgumentMatchers.<SFunction<FileDO, ?>>any(),
                        eq("http://test.yudao.iocoder.cn/dcc/original/report.xlsx"));
        when(watermarkService.build(99L, "preview", "report.xlsx"))
                .thenReturn(DccControlledPreviewWatermarkRespVO.builder().purpose("preview").build());
        when(uploadTicketService.createTicket(any())).thenReturn(new DccUploadTicketCreated(
                "UT-20260803-0001", "session-1", "SOURCE", "AVAILABLE",
                LocalDateTime.of(2026, 8, 3, 12, 30)));
        when(onlyOfficePreviewTokenService.issue(DccOnlyOfficePreviewTokenService.RESOURCE_UPLOAD_PREVIEW, 104L))
                .thenReturn("signed-upload-preview-token");

        DccControlledFileUploadRespVO respVO = uploadService.uploadPreviewFile(99L, reqVO,
                auditContext("REQ-XLSX-ONLYOFFICE"));

        assertEquals("OFFICE", respVO.getPreviewKind());
        assertEquals("http://onlyoffice.local", respVO.getOnlyofficeBaseUrl());
        assertEquals("http://host.docker.internal:48081/admin-api/dcc/controlled-files/upload-preview/104"
                        + "/onlyoffice-file?token=signed-upload-preview-token",
                readBeanProperty(respVO, "onlyofficeDocumentUrl"));
        assertNull(respVO.getPreviewUnavailableReason());
        verify(onlyOfficePreviewTokenService).issue(DccOnlyOfficePreviewTokenService.RESOURCE_UPLOAD_PREVIEW, 104L);
    }

    @Test
    void uploadPreviewFile_withoutCategoryUploadPermission_deniesBeforePolicyOrStorage() {
        DccControlledFileUploadPreviewReqVO reqVO = uploadReq("SOURCE",
                new MockMultipartFile("files", "SOP-001.docx",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "doc".getBytes()));
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.UPLOAD))
                .thenReturn(false);

        assertServiceException(() -> uploadService.uploadPreviewFile(99L, reqVO,
                        auditContext("REQ-UPLOAD-NO-CATEGORY-PERMISSION")),
                CONTROLLED_FILE_ACCESS_DENIED);

        verify(uploadSizePolicyService, never()).validateUploadSize(any(), any(), anyLong(), any());
        verify(fileService, never()).createFile(any(), any(), any(), any());
        verify(uploadTicketService, never()).createTicket(any());
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
        when(fileService.createFile(eq("%PDF-1.4".getBytes()), eq("drawing.pdf"), eq("dcc/original"), eq("application/pdf")))
                .thenReturn("http://test.yudao.iocoder.cn/dcc/original/drawing.pdf");
        doReturn(FileDO.builder().id(102L).name("drawing.pdf").url("http://test.yudao.iocoder.cn/dcc/original/drawing.pdf").build())
                .when(fileMapper).selectFirstOne(org.mockito.ArgumentMatchers.<SFunction<FileDO, ?>>any(),
                        eq("http://test.yudao.iocoder.cn/dcc/original/drawing.pdf"));
        when(watermarkService.build(99L, "preview", "drawing.pdf"))
                .thenReturn(DccControlledPreviewWatermarkRespVO.builder().purpose("preview").build());
        when(uploadTicketService.createTicket(any())).thenReturn(new DccUploadTicketCreated(
                "UT-20260528-0002", "session-1", "DRAWING_PDF", "AVAILABLE",
                LocalDateTime.of(2026, 5, 28, 12, 30)));

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
        when(fileService.createFile(eq("%PDF-1.4".getBytes()), eq("training.pdf"), eq("dcc/original"), eq("application/pdf")))
                .thenReturn("http://test.yudao.iocoder.cn/dcc/original/training.pdf");
        doReturn(FileDO.builder().id(103L).name("training.pdf").url("http://test.yudao.iocoder.cn/dcc/original/training.pdf").build())
                .when(fileMapper).selectFirstOne(org.mockito.ArgumentMatchers.<SFunction<FileDO, ?>>any(),
                        eq("http://test.yudao.iocoder.cn/dcc/original/training.pdf"));
        when(watermarkService.build(99L, "preview", "training.pdf"))
                .thenReturn(DccControlledPreviewWatermarkRespVO.builder().purpose("preview").build());
        when(uploadTicketService.createTicket(any())).thenReturn(new DccUploadTicketCreated(
                "UT-20260528-0003", "session-1", "TRAINING_RECORD", "AVAILABLE",
                LocalDateTime.of(2026, 5, 28, 12, 30)));

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
        when(fileService.createFile(eq("docx".getBytes()), eq("sample.docx"), eq("dcc/original"),
                eq("application/vnd.openxmlformats-officedocument.wordprocessingml.document")))
                .thenReturn("http://test.yudao.iocoder.cn/dcc/original/sample.docx");
        doReturn(FileDO.builder().id(101L).name("sample.docx").url("http://test.yudao.iocoder.cn/dcc/original/sample.docx").build())
                .when(fileMapper).selectFirstOne(org.mockito.ArgumentMatchers.<SFunction<FileDO, ?>>any(),
                        eq("http://test.yudao.iocoder.cn/dcc/original/sample.docx"));
        when(watermarkService.build(99L, "preview", "sample.docx"))
                .thenReturn(DccControlledPreviewWatermarkRespVO.builder().purpose("preview").build());
        when(uploadTicketService.createTicket(any())).thenReturn(new DccUploadTicketCreated(
                "UT-20260528-0004", "session-1", "SOURCE", "AVAILABLE",
                LocalDateTime.of(2026, 5, 28, 12, 30)));

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
        when(fileService.createFile(eq("docx".getBytes()), eq("sample.docx"), eq("dcc/original"),
                eq("application/vnd.openxmlformats-officedocument.wordprocessingml.document")))
                .thenReturn("http://test.yudao.iocoder.cn/dcc/original/sample.docx");
        doReturn(null).when(fileMapper).selectFirstOne(org.mockito.ArgumentMatchers.<SFunction<FileDO, ?>>any(),
                eq("http://test.yudao.iocoder.cn/dcc/original/sample.docx"));

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
