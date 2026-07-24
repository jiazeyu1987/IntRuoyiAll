package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccApprovalPrintTemplateSaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccApprovalPrintTemplateDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRouteSnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccApprovalPrintTemplateMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileRouteSnapshotMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSignatureMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.APPROVAL_PRINT_TEMPLATE_FILE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.APPROVAL_PRINT_TEMPLATE_PLACEHOLDER_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACCESS_DENIED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_NOT_EXISTS;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccApprovalPrintTemplateServiceTest extends BaseMockitoUnitTest {

    @Mock
    private DccApprovalPrintTemplateMapper templateMapper;
    @Mock
    private DccControlledFileMapper controlledFileMapper;
    @Mock
    private DccControlledFileRouteSnapshotMapper routeSnapshotMapper;
    @Mock
    private DccControlledFileSignatureMapper signatureMapper;
    @Mock
    private DccControlledFileQueryService queryService;
    @Mock
    private FileMapper fileMapper;
    @Mock
    private FileService fileService;

    @InjectMocks
    private DccApprovalPrintTemplateServiceImpl templateService;

    @Test
    void saveActiveTemplate_validDocx_insertsActiveTemplateConfig() throws Exception {
        when(templateMapper.selectActive()).thenReturn(null);
        when(fileMapper.selectById(501L)).thenReturn(templateFile());
        when(fileService.getFileContent(1L, "dcc/templates/approval.docx")).thenReturn(validTemplateDocx());

        var result = templateService.saveActiveTemplate(99L, saveReq(501L));

        assertEquals(501L, result.getTemplateFileId());
        assertEquals("approval.docx", result.getTemplateFileName());
        assertTrue(result.getActive());
        ArgumentCaptor<DccApprovalPrintTemplateDO> captor = ArgumentCaptor.forClass(DccApprovalPrintTemplateDO.class);
        verify(templateMapper).insert(captor.capture());
        assertEquals(501L, captor.getValue().getTemplateFileId());
        assertEquals("approval.docx", captor.getValue().getTemplateFileName());
        assertTrue(captor.getValue().getActive());
    }

    @Test
    void saveActiveTemplate_existingActiveTemplate_updatesConfigWithoutFallback() throws Exception {
        when(templateMapper.selectActive()).thenReturn(DccApprovalPrintTemplateDO.builder()
                .id(88L)
                .templateFileId(400L)
                .templateFileName("old.docx")
                .active(Boolean.TRUE)
                .build());
        when(fileMapper.selectById(501L)).thenReturn(templateFile());
        when(fileService.getFileContent(1L, "dcc/templates/approval.docx")).thenReturn(validTemplateDocx());

        var result = templateService.saveActiveTemplate(99L, saveReq(501L));

        assertEquals(88L, result.getId());
        assertEquals(501L, result.getTemplateFileId());
        ArgumentCaptor<DccApprovalPrintTemplateDO> captor = ArgumentCaptor.forClass(DccApprovalPrintTemplateDO.class);
        verify(templateMapper).updateById(captor.capture());
        assertEquals(88L, captor.getValue().getId());
        assertEquals(501L, captor.getValue().getTemplateFileId());
        assertEquals("approval.docx", captor.getValue().getTemplateFileName());
    }

    @Test
    void saveActiveTemplate_invalidDocx_throwsClearTemplateError() throws Exception {
        when(fileMapper.selectById(501L)).thenReturn(templateFile());
        when(fileService.getFileContent(1L, "dcc/templates/approval.docx"))
                .thenReturn("not-a-zip".getBytes(StandardCharsets.UTF_8));

        assertServiceException(() -> templateService.saveActiveTemplate(99L, saveReq(501L)),
                APPROVAL_PRINT_TEMPLATE_FILE_INVALID);
    }

    @Test
    void saveActiveTemplate_missingRequiredPlaceholder_throwsClearTemplateError() throws Exception {
        when(fileMapper.selectById(501L)).thenReturn(templateFile());
        when(fileService.getFileContent(1L, "dcc/templates/approval.docx"))
                .thenReturn(docx("""
                        <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                          <w:body><w:p><w:r><w:t>{{fileNumber}} {{fileName}} {{versionNo}}</w:t></w:r></w:p></w:body>
                        </w:document>
                        """));

        assertServiceException(() -> templateService.saveActiveTemplate(99L, saveReq(501L)),
                APPROVAL_PRINT_TEMPLATE_PLACEHOLDER_MISSING);
    }

    @Test
    void exportApprovalWord_activeTemplate_rendersControlledFileAndApprovalData() throws Exception {
        when(templateMapper.selectActive()).thenReturn(activeTemplate());
        when(queryService.getControlledFile(99L, 901L)).thenReturn(new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRespVO());
        when(fileMapper.selectById(501L)).thenReturn(templateFile());
        when(fileService.getFileContent(1L, "dcc/templates/approval.docx")).thenReturn(validTemplateDocx());
        when(controlledFileMapper.selectById(901L)).thenReturn(DccControlledFileDO.builder()
                .id(901L)
                .categoryId(10L)
                .requesterId(88L)
                .fileNumber("DCC-001")
                .fileName("Quality Manual")
                .title("质量手册")
                .versionNo("V1.0")
                .productCode("P0000000000001")
                .processInstanceId("pi-901")
                .status(DccControlledFileStatusEnum.PENDING_MATRIX_APPROVAL.getStatus())
                .submittedTime(LocalDateTime.of(2026, 5, 27, 9, 0))
                .effectiveDate(LocalDate.of(2026, 6, 1))
                .build());
        when(routeSnapshotMapper.selectListByControlledFileId(901L)).thenReturn(List.of(
                DccControlledFileRouteSnapshotDO.builder()
                        .stageNo(1)
                        .stageCode("DOC_CONTROL_REVIEW")
                        .stageName("文控审核")
                        .resolvedUserIds("99")
                        .build()));
        when(signatureMapper.selectListByControlledFileId(901L)).thenReturn(List.of(
                DccControlledFileSignatureDO.builder()
                        .taskId("task-1")
                        .actorId(99L)
                        .actionType("APPROVE")
                        .comment("同意")
                        .signedAt(LocalDateTime.of(2026, 5, 27, 10, 0))
                        .build()));

        DccApprovalPrintRenderedWord result = templateService.exportApprovalWord(99L, 901L);

        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document", result.contentType());
        assertTrue(result.fileName().contains("DCC-001"));
        String documentXml = readDocumentXml(result.bytes());
        assertFalse(documentXml.contains("{{"));
        assertTrue(documentXml.contains("DCC-001"));
        assertTrue(documentXml.contains("Quality Manual"));
        assertTrue(documentXml.contains("V1.0"));
        assertTrue(documentXml.contains("文控审核"));
        assertTrue(documentXml.contains("APPROVE"));
        assertTrue(documentXml.contains("同意"));
    }

    @Test
    void getApprovalPrintHtml_activeTemplate_returnsEscapedHtmlWithControlledFileAndApprovalData() throws Exception {
        when(templateMapper.selectActive()).thenReturn(activeTemplate());
        when(queryService.getControlledFile(99L, 901L)).thenReturn(new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRespVO());
        when(fileMapper.selectById(501L)).thenReturn(templateFile());
        when(fileService.getFileContent(1L, "dcc/templates/approval.docx")).thenReturn(validTemplateDocx());
        when(controlledFileMapper.selectById(901L)).thenReturn(DccControlledFileDO.builder()
                .id(901L)
                .categoryId(10L)
                .requesterId(88L)
                .fileNumber("DCC-HTML")
                .fileName("Quality <Manual>")
                .versionNo("V1.0")
                .productCode("P0000000000001")
                .processInstanceId("pi-901")
                .status(DccControlledFileStatusEnum.PENDING_MATRIX_APPROVAL.getStatus())
                .build());
        when(routeSnapshotMapper.selectListByControlledFileId(901L)).thenReturn(List.of(
                DccControlledFileRouteSnapshotDO.builder()
                        .stageNo(1)
                        .stageCode("DOC_CONTROL_REVIEW")
                        .stageName("文控审核")
                        .resolvedUserIds("99")
                        .build()));
        when(signatureMapper.selectListByControlledFileId(901L)).thenReturn(List.of(
                DccControlledFileSignatureDO.builder()
                        .taskId("task-1")
                        .actorId(99L)
                        .actionType("APPROVE")
                        .comment("同意 <OK>")
                        .signedAt(LocalDateTime.of(2026, 5, 27, 10, 0))
                        .build()));

        var result = templateService.getApprovalPrintHtml(99L, 901L);

        assertEquals(1L, result.getTemplateId());
        assertEquals("approval.docx", result.getTemplateFileName());
        assertTrue(result.getHtml().contains("width: 100%;"));
        assertTrue(result.getHtml().contains("DCC-HTML"));
        assertTrue(result.getHtml().contains("Quality &lt;Manual&gt;"));
        assertTrue(result.getHtml().contains("审批记录"));
        assertTrue(result.getHtml().contains("文控审核"));
        assertTrue(result.getHtml().contains("APPROVE"));
        assertTrue(result.getHtml().contains("同意 &lt;OK&gt;"));
        assertFalse(result.getHtml().contains("{{"));
    }

    @Test
    void exportApprovalWord_requesterCanExportWithoutCategoryViewPermission() throws Exception {
        when(templateMapper.selectActive()).thenReturn(activeTemplate());
        when(queryService.getControlledFile(99L, 901L)).thenReturn(new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRespVO());
        when(fileMapper.selectById(501L)).thenReturn(templateFile());
        when(fileService.getFileContent(1L, "dcc/templates/approval.docx")).thenReturn(validTemplateDocx());
        when(controlledFileMapper.selectById(901L)).thenReturn(DccControlledFileDO.builder()
                .id(901L)
                .categoryId(10L)
                .requesterId(99L)
                .fileNumber("DCC-REQ")
                .fileName("Requester File")
                .versionNo("V1.0")
                .status(DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus())
                .build());
        when(routeSnapshotMapper.selectListByControlledFileId(901L)).thenReturn(List.of());
        when(signatureMapper.selectListByControlledFileId(901L)).thenReturn(List.of());

        DccApprovalPrintRenderedWord result = templateService.exportApprovalWord(99L, 901L);

        String documentXml = readDocumentXml(result.bytes());
        assertTrue(documentXml.contains("DCC-REQ"));
    }

    @Test
    void exportApprovalWord_noCategoryPermission_throwsAccessDenied() {
        when(queryService.getControlledFile(99L, 901L)).thenThrow(exception(CONTROLLED_FILE_ACCESS_DENIED));

        assertServiceException(() -> templateService.exportApprovalWord(99L, 901L),
                CONTROLLED_FILE_ACCESS_DENIED);
    }

    @Test
    void exportApprovalWord_crossTenantFilteredFile_throwsNotExists() {
        when(queryService.getControlledFile(99L, 901L)).thenThrow(exception(CONTROLLED_FILE_NOT_EXISTS));

        assertServiceException(() -> templateService.exportApprovalWord(99L, 901L),
                CONTROLLED_FILE_NOT_EXISTS);
    }

    @Test
    void rendererPreservesNonXmlZipEntries() throws Exception {
        when(templateMapper.selectActive()).thenReturn(activeTemplate());
        when(queryService.getControlledFile(99L, 901L)).thenReturn(new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRespVO());
        when(fileMapper.selectById(501L)).thenReturn(templateFile());
        when(fileService.getFileContent(1L, "dcc/templates/approval.docx")).thenReturn(validTemplateDocx());
        when(controlledFileMapper.selectById(901L)).thenReturn(DccControlledFileDO.builder()
                .id(901L)
                .categoryId(10L)
                .requesterId(99L)
                .fileNumber("DCC-BIN")
                .fileName("Binary Preserved")
                .versionNo("V1.0")
                .status(DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus())
                .build());
        when(routeSnapshotMapper.selectListByControlledFileId(901L)).thenReturn(List.of());
        when(signatureMapper.selectListByControlledFileId(901L)).thenReturn(List.of());

        DccApprovalPrintRenderedWord result = templateService.exportApprovalWord(99L, 901L);

        assertArrayEquals(new byte[]{1, 2, 3}, readZipEntry(result.bytes(), "word/media/image1.bin"));
    }

    private DccApprovalPrintTemplateSaveReqVO saveReq(Long fileId) {
        DccApprovalPrintTemplateSaveReqVO reqVO = new DccApprovalPrintTemplateSaveReqVO();
        reqVO.setTemplateFileId(fileId);
        reqVO.setRemark("R12 template");
        return reqVO;
    }

    private DccApprovalPrintTemplateDO activeTemplate() {
        return DccApprovalPrintTemplateDO.builder()
                .id(1L)
                .templateFileId(501L)
                .templateFileName("approval.docx")
                .templateFileContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .active(Boolean.TRUE)
                .build();
    }

    private FileDO templateFile() {
        return FileDO.builder()
                .id(501L)
                .configId(1L)
                .path("dcc/templates/approval.docx")
                .name("approval.docx")
                .type("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .build();
    }

    private byte[] validTemplateDocx() throws Exception {
        return docx("""
                <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                  <w:body>
                    <w:p><w:r><w:t>{{fileNumber}}</w:t></w:r></w:p>
                    <w:p><w:r><w:t>{{fileName}}</w:t></w:r></w:p>
                    <w:p><w:r><w:t>{{versionNo}}</w:t></w:r></w:p>
                    <w:p><w:r><w:t>{{approvalRecords}}</w:t></w:r></w:p>
                  </w:body>
                </w:document>
                """);
    }

    private byte[] docx(String documentXml) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zip.write("""
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                      <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                      <Default Extension="xml" ContentType="application/xml"/>
                      <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
                    </Types>
                    """.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("_rels/.rels"));
            zip.write("""
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
                    </Relationships>
                    """.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write(documentXml.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("word/media/image1.bin"));
            zip.write(new byte[]{1, 2, 3});
            zip.closeEntry();
        }
        return output.toByteArray();
    }

    private String readDocumentXml(byte[] docx) throws Exception {
        return new String(readZipEntry(docx, "word/document.xml"), StandardCharsets.UTF_8);
    }

    private byte[] readZipEntry(byte[] docx, String name) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(docx), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (name.equals(entry.getName())) {
                    return zip.readAllBytes();
                }
            }
        }
        throw new AssertionError("missing zip entry: " + name);
    }
}
