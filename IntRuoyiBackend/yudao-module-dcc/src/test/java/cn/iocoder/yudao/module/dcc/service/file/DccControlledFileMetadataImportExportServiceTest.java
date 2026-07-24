package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileMetadataExportExcelVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileMetadataImportPreviewRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRecognitionMigrationImportPreviewRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePageReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccFileDirectoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRecognitionRecordDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccFileDirectoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileRecognitionRecordMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.mdm.api.product.MdmProductApi;
import cn.iocoder.yudao.module.mdm.api.product.dto.MdmProductRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_METADATA_UPDATE_NOT_ALLOWED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccControlledFileMetadataImportExportServiceTest extends BaseMockitoUnitTest {

    @Mock
    private DccControlledFileQueryService controlledFileQueryService;
    @Mock
    private DccControlledFileMapper controlledFileMapper;
    @Mock
    private DccControlledFileRecognitionRecordMapper recognitionRecordMapper;
    @Mock
    private DccFileDirectoryMapper directoryMapper;
    @Mock
    private DccProjectCodeMapper projectCodeMapper;
    @Mock
    private MdmProductApi productApi;
    @Mock
    private DccControlledFileMetadataUpdateService metadataUpdateService;
    @Mock
    private PermissionApi permissionApi;

    @InjectMocks
    private DccControlledFileMetadataImportExportServiceImpl metadataImportExportService;

    @Test
    void exportList_onlyReturnsRowsWithRecognizedFileNameAndFileNumber() {
        when(permissionApi.hasAnyRoles(99L, DccControlledFileMetadataUpdateService.DOC_CONTROL_ROLE_CODE))
                .thenReturn(true);
        DccControlledFilePageReqVO reqVO = new DccControlledFilePageReqVO();
        reqVO.setKeyword("SOP");
        when(controlledFileQueryService.listControlledFileBrowserCandidates(99L, reqVO)).thenReturn(List.of(
                controlledFile(900L, "SOP-A", "DOC-001"),
                controlledFile(901L, " ", "DOC-002"),
                controlledFile(902L, "SOP-C", null)));

        List<DccControlledFileMetadataExportExcelVO> exports = metadataImportExportService.getExportList(99L, reqVO);

        assertEquals(1, exports.size());
        assertEquals(900L, exports.get(0).getControlledFileId());
        assertEquals("SOP-A", exports.get(0).getFileName());
        assertEquals("DOC-001", exports.get(0).getFileNumber());
    }

    @Test
    void recognitionRecordExport_containsSharedLedgerRows() throws Exception {
        when(permissionApi.hasAnyRoles(99L, DccControlledFileMetadataUpdateService.DOC_CONTROL_ROLE_CODE))
                .thenReturn(true);
        DccControlledFilePageReqVO reqVO = new DccControlledFilePageReqVO();
        reqVO.setDirectoryId(31L);
        reqVO.setIncludeDescendantDirectories(true);
        when(controlledFileQueryService.listControlledFileBrowserCandidates(99L, reqVO)).thenReturn(List.of(
                controlledFile(900L, "SOP-A", "DOC-001"),
                controlledFile(901L, "SOP-B", "DOC-002")));
        when(directoryMapper.selectList()).thenReturn(List.of(
                DccFileDirectoryDO.builder().id(30L).name("父目录").build(),
                DccFileDirectoryDO.builder().id(31L).parentId(30L).name("子目录").build()));
        when(recognitionRecordMapper.selectListByFileIds(List.of(900L, 901L), null, null)).thenReturn(List.of(
                DccControlledFileRecognitionRecordDO.builder()
                        .controlledFileId(900L)
                        .status("SUCCESS")
                        .recognizedProductName("离心泵")
                        .recognizedProductCode("PRD20260604001")
                        .matchedProjectAliasId(901L)
                        .matchedProjectAliasText("离心泵别名")
                        .matchedProjectAliasSource("DIRECTORY")
                        .matchType("PROJECT_NAME")
                        .matchText("离心泵别名")
                        .recognitionVersion("project-code-v1")
                        .batchTaskId(300L)
                        .recognizedBy(99L)
                        .build(),
                DccControlledFileRecognitionRecordDO.builder()
                        .controlledFileId(901L)
                        .status("FAILED")
                        .failureMessage("no match")
                        .recognitionVersion("project-code-v1")
                        .batchTaskId(300L)
                        .recognizedBy(99L)
                        .build()));

        byte[] bytes = metadataImportExportService.buildRecognitionRecordExportExcel(99L, reqVO);

        try (org.apache.poi.ss.usermodel.Workbook workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            assertEquals("目录路径", formatter.formatCellValue(sheet.getRow(0).getCell(0)));
            assertEquals("识别状态", formatter.formatCellValue(sheet.getRow(0).getCell(3)));
            assertEquals("命中别名ID", formatter.formatCellValue(sheet.getRow(0).getCell(6)));
            assertEquals("命中别名文本", formatter.formatCellValue(sheet.getRow(0).getCell(7)));
            assertEquals("命中别名来源", formatter.formatCellValue(sheet.getRow(0).getCell(8)));
            assertEquals("批量任务ID", formatter.formatCellValue(sheet.getRow(0).getCell(18)));
            assertEquals("父目录/子目录", formatter.formatCellValue(sheet.getRow(1).getCell(0)));
            assertEquals("SUCCESS", formatter.formatCellValue(sheet.getRow(1).getCell(3)));
            assertEquals("离心泵", formatter.formatCellValue(sheet.getRow(1).getCell(4)));
            assertEquals("901", formatter.formatCellValue(sheet.getRow(1).getCell(6)));
            assertEquals("离心泵别名", formatter.formatCellValue(sheet.getRow(1).getCell(7)));
            assertEquals("DIRECTORY", formatter.formatCellValue(sheet.getRow(1).getCell(8)));
            assertEquals("300", formatter.formatCellValue(sheet.getRow(1).getCell(18)));
            assertEquals("FAILED", formatter.formatCellValue(sheet.getRow(2).getCell(3)));
            assertEquals("no match", formatter.formatCellValue(sheet.getRow(2).getCell(11)));
        }
    }

    @Test
    void recognitionRecordExport_batchTaskRowsDoNotDisappearWhenBrowserCandidatesAreEmpty() throws Exception {
        when(permissionApi.hasAnyRoles(99L, DccControlledFileMetadataUpdateService.DOC_CONTROL_ROLE_CODE))
                .thenReturn(true);
        DccControlledFilePageReqVO reqVO = new DccControlledFilePageReqVO();
        reqVO.setDirectoryId(31L);
        reqVO.setIncludeDescendantDirectories(true);
        reqVO.setBatchRecognitionTaskId(300L);
        when(controlledFileQueryService.listControlledFileBrowserCandidates(99L, reqVO)).thenReturn(List.of());
        when(controlledFileMapper.selectBatchIds(List.of(900L))).thenReturn(List.of(
                controlledFile(900L, "SOP-A", "DOC-001")));
        when(directoryMapper.selectList()).thenReturn(List.of(
                DccFileDirectoryDO.builder().id(30L).name("父目录").build(),
                DccFileDirectoryDO.builder().id(31L).parentId(30L).name("子目录").build()));
        when(recognitionRecordMapper.selectListByBatchTaskId(300L, null)).thenReturn(List.of(
                DccControlledFileRecognitionRecordDO.builder()
                        .controlledFileId(900L)
                        .status("SUCCESS")
                        .recognizedProductName("离心泵")
                        .recognizedProductCode("PRD20260604001")
                        .recognitionVersion("project-code-v1")
                        .batchTaskId(300L)
                        .recognizedBy(99L)
                        .build()));

        byte[] bytes = metadataImportExportService.buildRecognitionRecordExportExcel(99L, reqVO);

        try (org.apache.poi.ss.usermodel.Workbook workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            assertEquals("目录路径", formatter.formatCellValue(sheet.getRow(0).getCell(0)));
            assertNotNull(sheet.getRow(1), "batch task recognition record row must be exported");
            assertEquals("父目录/子目录", formatter.formatCellValue(sheet.getRow(1).getCell(0)));
            assertEquals("SOP-A", formatter.formatCellValue(sheet.getRow(1).getCell(1)));
            assertEquals("SUCCESS", formatter.formatCellValue(sheet.getRow(1).getCell(3)));
            assertEquals("离心泵", formatter.formatCellValue(sheet.getRow(1).getCell(4)));
            assertEquals("300", formatter.formatCellValue(sheet.getRow(1).getCell(18)));
        }
    }

    @Test
    void recognitionMigrationExport_containsStableKeysAndProjectFields() throws Exception {
        when(permissionApi.hasAnyRoles(99L, DccControlledFileMetadataUpdateService.DOC_CONTROL_ROLE_CODE))
                .thenReturn(true);
        DccControlledFilePageReqVO reqVO = new DccControlledFilePageReqVO();
        reqVO.setBatchRecognitionTaskId(300L);
        when(controlledFileQueryService.listControlledFileBrowserCandidates(99L, reqVO)).thenReturn(List.of());
        when(controlledFileMapper.selectBatchIds(List.of(900L, 901L))).thenReturn(List.of(
                controlledFile(900L, "SOP-A", "DOC-001"),
                controlledFile(901L, "SOP-B", "DOC-002")));
        when(directoryMapper.selectList()).thenReturn(List.of(
                DccFileDirectoryDO.builder().id(30L).name("父目录").build(),
                DccFileDirectoryDO.builder().id(31L).parentId(30L).name("子目录").build()));
        when(projectCodeMapper.selectBatchIds(List.of(600L))).thenReturn(List.of(projectCode(600L, "离心泵项目", "P-001")));
        when(productApi.getProduct(5000L)).thenReturn(product(7000L));
        when(recognitionRecordMapper.selectListByBatchTaskId(300L, null)).thenReturn(List.of(
                DccControlledFileRecognitionRecordDO.builder()
                        .controlledFileId(900L)
                        .status("SUCCESS")
                        .recognizedProductName("离心泵")
                        .recognizedProductCode("P-001")
                        .matchedProjectCodeId(600L)
                        .matchedProjectAliasText("离心泵别名")
                        .matchType("PROJECT_NAME")
                        .fileTypeLevel1("质量文件")
                        .fileTypeLevel2("SOP")
                        .recognitionVersion("project-code-v1")
                        .batchTaskId(300L)
                        .recognizedBy(99L)
                        .build(),
                DccControlledFileRecognitionRecordDO.builder()
                        .controlledFileId(901L)
                        .status("FAILED")
                        .failureMessage("未识别到项目")
                        .recognitionVersion("project-code-v1")
                        .batchTaskId(300L)
                        .recognizedBy(99L)
                        .build()));

        byte[] bytes = metadataImportExportService.buildRecognitionMigrationExportExcel(99L, reqVO);

        try (org.apache.poi.ss.usermodel.Workbook workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            assertEquals("目录路径", formatter.formatCellValue(sheet.getRow(0).getCell(0)));
            assertEquals("文件编号", formatter.formatCellValue(sheet.getRow(0).getCell(2)));
            assertEquals("测试服受控文件ID", formatter.formatCellValue(sheet.getRow(0).getCell(3)));
            assertEquals("项目名称", formatter.formatCellValue(sheet.getRow(0).getCell(7)));
            assertEquals("项目编码", formatter.formatCellValue(sheet.getRow(0).getCell(8)));
            assertEquals("父目录/子目录", formatter.formatCellValue(sheet.getRow(1).getCell(0)));
            assertEquals("DOC-001", formatter.formatCellValue(sheet.getRow(1).getCell(2)));
            assertEquals("900", formatter.formatCellValue(sheet.getRow(1).getCell(3)));
            assertEquals("SUCCESS", formatter.formatCellValue(sheet.getRow(1).getCell(4)));
            assertEquals("PRD20260604001", formatter.formatCellValue(sheet.getRow(1).getCell(6)));
            assertEquals("离心泵项目", formatter.formatCellValue(sheet.getRow(1).getCell(7)));
            assertEquals("P-001", formatter.formatCellValue(sheet.getRow(1).getCell(8)));
            assertEquals("质量文件", formatter.formatCellValue(sheet.getRow(1).getCell(16)));
            assertEquals("FAILED", formatter.formatCellValue(sheet.getRow(2).getCell(4)));
            assertEquals("未识别到项目", formatter.formatCellValue(sheet.getRow(2).getCell(15)));
        }
    }

    @Test
    void recognitionMigrationPreview_matchesByDirectoryAndFileNumberInsteadOfTestServerId() {
        when(permissionApi.hasAnyRoles(99L, DccControlledFileMetadataUpdateService.DOC_CONTROL_ROLE_CODE))
                .thenReturn(true);
        MockMultipartFile file = recognitionMigrationWorkbook(
                new String[] {"父目录/子目录", "SOP-A", "DOC-001", "900", "SUCCESS", "离心泵",
                        "PRD20260604001", "离心泵项目", "P-001", "600", "", "离心泵别名", "ALIAS",
                        "PROJECT_NAME", "离心泵", "", "质量文件", "SOP", "", "", "", "project-code-v1",
                        "300", "99", ""});
        when(directoryMapper.selectList()).thenReturn(List.of(
                DccFileDirectoryDO.builder().id(30L).name("父目录").build(),
                DccFileDirectoryDO.builder().id(31L).parentId(30L).name("子目录").build()));
        when(controlledFileMapper.selectList(any())).thenReturn(List.of(controlledFile(901L, "SOP-A", "DOC-001")));
        when(productApi.getEnabledDccProductByDccProductCode("PRD20260604001")).thenReturn(product(7000L));
        when(projectCodeMapper.selectByProjectNameAndProjectCode("离心泵项目", "P-001"))
                .thenReturn(projectCode(6000L, "离心泵项目", "P-001"));

        DccControlledFileRecognitionMigrationImportPreviewRespVO respVO =
                metadataImportExportService.previewRecognitionMigrationImport(99L, file);

        assertEquals(1, respVO.getTotalCount());
        assertEquals(1, respVO.getApplicableCount());
        assertEquals(0, respVO.getBlockedCount());
        assertEquals(901L, respVO.getRows().get(0).getTargetControlledFileId());
        assertEquals("APPLICABLE", respVO.getRows().get(0).getImportAction());
    }

    @Test
    void recognitionMigrationConfirm_appliesOnlyApplicableSuccessRows() {
        when(permissionApi.hasAnyRoles(99L, DccControlledFileMetadataUpdateService.DOC_CONTROL_ROLE_CODE))
                .thenReturn(true);
        MockMultipartFile file = recognitionMigrationWorkbook(
                new String[] {"父目录/子目录", "SOP-A", "DOC-001", "900", "SUCCESS", "离心泵",
                        "PRD20260604001", "离心泵项目", "P-001", "600", "", "离心泵别名", "ALIAS",
                        "PROJECT_NAME", "离心泵", "", "质量文件", "SOP", "", "", "", "project-code-v1",
                        "300", "99", ""},
                new String[] {"父目录/子目录", "SOP-B", "DOC-002", "901", "FAILED", "",
                        "", "", "", "", "", "", "", "", "", "未识别到项目", "", "", "", "", "",
                        "project-code-v1", "300", "99", ""});
        when(directoryMapper.selectList()).thenReturn(List.of(
                DccFileDirectoryDO.builder().id(30L).name("父目录").build(),
                DccFileDirectoryDO.builder().id(31L).parentId(30L).name("子目录").build()));
        when(controlledFileMapper.selectList(any()))
                .thenReturn(List.of(controlledFile(1900L, "SOP-A", "DOC-001")))
                .thenReturn(List.of(controlledFile(1901L, "SOP-B", "DOC-002")));
        when(productApi.getEnabledDccProductByDccProductCode("PRD20260604001")).thenReturn(product(7000L));
        when(projectCodeMapper.selectByProjectNameAndProjectCode("离心泵项目", "P-001"))
                .thenReturn(projectCode(6000L, "离心泵项目", "P-001"));

        DccControlledFileRecognitionMigrationImportPreviewRespVO respVO =
                metadataImportExportService.confirmRecognitionMigrationImport(99L, file);

        assertEquals(2, respVO.getTotalCount());
        assertEquals(1, respVO.getAppliedCount());
        assertEquals(1, respVO.getFailedRecognitionCount());
        ArgumentCaptor<cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileMetadataUpdateReqVO> captor =
                ArgumentCaptor.forClass(cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileMetadataUpdateReqVO.class);
        verify(metadataUpdateService).updateMetadata(any(), any(), captor.capture());
        assertEquals(7000L, captor.getValue().getProductMasterId());
        assertEquals(6000L, captor.getValue().getDccProjectCodeId());
        assertEquals("质量文件", captor.getValue().getFileTypeLevel1());
        assertEquals("SOP", captor.getValue().getFileTypeLevel2());
        assertEquals("DOC-001", captor.getValue().getFileNumber());
    }

    @Test
    void previewImport_returnsFailureForMissingFileAndBlankFields() {
        when(permissionApi.hasAnyRoles(99L, DccControlledFileMetadataUpdateService.DOC_CONTROL_ROLE_CODE))
                .thenReturn(true);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "metadata.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                workbookBytes(List.of(
                        new String[] {"受控文件ID", "文件名称", "文件编号"},
                        new String[] {"900", "新文件名", "DOC-001"},
                        new String[] {"999", "", "DOC-002"}
                )));
        when(controlledFileMapper.selectById(900L)).thenReturn(controlledFile(900L, "旧文件名", "OLD-001"));

        DccControlledFileMetadataImportPreviewRespVO respVO = metadataImportExportService.previewImport(99L, file);

        assertEquals(2, respVO.getTotalCount());
        assertEquals(1, respVO.getUpdateCount());
        assertEquals(1, respVO.getFailureCount());
        assertEquals(2, respVO.getRows().size());
        assertEquals("UPDATE", respVO.getRows().get(0).getImportAction());
        assertEquals("INVALID", respVO.getRows().get(1).getImportAction());
        assertTrue(respVO.getRows().get(1).getFailureReason().contains("受控文件不存在")
                || respVO.getRows().get(1).getFailureReason().contains("不能为空"));
    }

    @Test
    void confirmImport_reusesMetadataUpdateServiceForEachValidRow() {
        when(permissionApi.hasAnyRoles(99L, DccControlledFileMetadataUpdateService.DOC_CONTROL_ROLE_CODE))
                .thenReturn(true);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "metadata.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                workbookBytes(List.of(
                        new String[] {"受控文件ID", "文件名称", "文件编号"},
                        new String[] {"900", "新文件名", "DOC-001"},
                        new String[] {"901", "原文件名", "DOC-009"}
                )));
        when(controlledFileMapper.selectById(900L)).thenReturn(controlledFile(900L, "旧文件名", "OLD-001"));
        when(controlledFileMapper.selectById(901L)).thenReturn(controlledFile(901L, "原文件名", "DOC-009"));

        DccControlledFileMetadataImportPreviewRespVO respVO = metadataImportExportService.confirmImport(99L, file);

        assertEquals(2, respVO.getTotalCount());
        assertEquals(1, respVO.getUpdateCount());
        assertEquals(1, respVO.getUnchangedCount());
        ArgumentCaptor<cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileMetadataUpdateReqVO> captor =
                ArgumentCaptor.forClass(cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileMetadataUpdateReqVO.class);
        verify(metadataUpdateService).updateMetadata(any(), any(), captor.capture());
        assertEquals("新文件名", captor.getValue().getFileName());
        assertEquals("DOC-001", captor.getValue().getFileNumber());
    }

    @Test
    void confirmImport_failsFastWhenPreviewStillContainsInvalidRows() {
        when(permissionApi.hasAnyRoles(99L, DccControlledFileMetadataUpdateService.DOC_CONTROL_ROLE_CODE))
                .thenReturn(true);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "metadata.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                workbookBytes(List.of(
                        new String[] {"受控文件ID", "文件名称", "文件编号"},
                        new String[] {"999", "新文件名", "DOC-001"}
                )));
        when(controlledFileMapper.selectById(999L)).thenReturn(null);

        IllegalStateException ex = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> metadataImportExportService.confirmImport(99L, file));
        assertTrue(ex.getMessage().contains("失败"));
        verify(metadataUpdateService, never()).updateMetadata(any(), any(), any());
    }

    @Test
    void previewImport_nonDocControlFailsFast() {
        when(permissionApi.hasAnyRoles(99L, DccControlledFileMetadataUpdateService.DOC_CONTROL_ROLE_CODE))
                .thenReturn(false);

        assertServiceException(() -> metadataImportExportService.previewImport(99L, new MockMultipartFile(
                        "file", "metadata.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        new byte[] {1})),
                CONTROLLED_FILE_METADATA_UPDATE_NOT_ALLOWED);
    }

    private DccControlledFileDO controlledFile(Long id, String fileName, String fileNumber) {
        return DccControlledFileDO.builder()
                .id(id)
                .categoryId(11L)
                .directoryId(31L)
                .productMasterId(5000L)
                .productCode("PRD20260604001")
                .productName("离心泵")
                .fileName(fileName)
                .title(fileName)
                .fileNumber(fileNumber)
                .needTraining(false)
                .build();
    }

    private DccProjectCodeDO projectCode(Long id, String projectName, String projectCode) {
        return DccProjectCodeDO.builder()
                .id(id)
                .projectName(projectName)
                .projectCode(projectCode)
                .build();
    }

    private MdmProductRespDTO product(Long id) {
        return MdmProductRespDTO.builder()
                .id(id)
                .dccProductCode("PRD20260604001")
                .nameCn("离心泵")
                .build();
    }

    private MockMultipartFile recognitionMigrationWorkbook(String[]... dataRows) {
        List<String[]> rows = new java.util.ArrayList<>();
        rows.add(new String[] {"目录路径", "文件名称", "文件编号", "测试服受控文件ID", "识别状态",
                "产品名称", "产品编码", "项目名称", "项目编码", "测试服项目ID", "命中别名ID",
                "命中别名文本", "命中别名来源", "匹配方式", "匹配文本", "失败原因", "文件类型1",
                "文件类型2", "文件类型3", "文件类型4", "文件类型5", "识别版本", "批量任务ID",
                "识别人", "识别时间"});
        rows.addAll(List.of(dataRows));
        return new MockMultipartFile(
                "file",
                "recognition-migration.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                workbookBytes(rows));
    }

    private byte[] workbookBytes(List<String[]> rows) {
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("metadata");
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIndex);
                String[] values = rows.get(rowIndex);
                for (int columnIndex = 0; columnIndex < values.length; columnIndex++) {
                    row.createCell(columnIndex).setCellValue(values[columnIndex]);
                }
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
