package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditDetailReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditExportReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditExportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditVerifyReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditVerifyRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilityExportReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilityExportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilitySummaryReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionFieldAuditBatchDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionFieldAuditItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionSignatureDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionFieldAuditBatchMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionFieldAuditItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionSignatureMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import jakarta.annotation.Resource;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_CHAIN_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_EXPORT_FAILED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionErrorCodeConstants.PRO_EDHR_OBJECT_PERMISSION_DENIED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Import({
        MesProBatchRecordExecutionFieldAuditServiceImpl.class,
        MesProBatchRecordExecutionFieldResponsibilityService.class
})
class MesProBatchRecordExecutionFieldAuditQueryExportServiceTest extends BaseDbUnitTest {

    private static final Long TENANT_ID = 122L;
    private static final String FIELD_PATH = "sheet[0].rows[1].cells[2].temperature";
    private static final String GENESIS = MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH;
    private static final LocalDateTime CHANGED_AT = LocalDateTime.of(2026, 5, 26, 13, 0);
    private static final List<String> RESPONSIBILITY_EXPORT_HEADERS = List.of(
            "Generated At",
            "Execution ID",
            "Execution Code",
            "Batch Record Definition ID",
            "Batch Record Version ID",
            "Batch Record Report ID",
            "Field Audit Revision",
            "Field Audit Head Hash",
            "Cell Values Hash",
            "Field Path",
            "Field Key",
            "Field Label",
            "Row Index",
            "Column Index",
            "Component",
            "Value Type",
            "Current Value JSON",
            "Current Value Display",
            "Current Value Hash",
            "Value Origin",
            "First Human Actor ID",
            "First Human Actor Name",
            "First Human Changed At",
            "First Signature ID",
            "First Signature Username",
            "First Signature Nickname",
            "First Signature Display At",
            "Current Value Actor ID",
            "Current Value Actor Name",
            "Current Value Changed At",
            "Current Signature ID",
            "Current Signature Username",
            "Current Signature Nickname",
            "Current Signature Display At",
            "Evidence Status",
            "Reason Codes",
            "History Count",
            "Latest Audit Item ID",
            "Context Warnings");
    private static final List<String> SENSITIVE_WORKBOOK_SENTINELS = List.of(
            "198.51.100.77",
            "Sensitive-UA/7.7",
            "password-sentinel-23a",
            "token-sentinel-31b",
            "auth-sentinel-47c",
            "authentication-sentinel-59d",
            "session-sentinel-61e",
            "credential-sentinel-73f",
            "secret-sentinel-89g");

    @Resource
    private MesProBatchRecordExecutionFieldAuditService fieldAuditService;
    @Resource
    private MesProBatchRecordExecutionMapper executionMapper;
    @MockitoSpyBean
    private MesProBatchRecordExecutionFieldAuditBatchMapper batchMapper;
    @MockitoSpyBean
    private MesProBatchRecordExecutionFieldAuditItemMapper itemMapper;
    @MockitoSpyBean
    private MesProBatchRecordExecutionSignatureMapper signatureMapper;
    @MockitoSpyBean
    private MesProEdhrWorkTaskMapper workTaskMapper;

    @MockitoBean
    private MesProBatchRecordExecutionSignatureService signatureService;
    @MockitoBean
    private MesProBatchRecordExecutionAttachmentService attachmentService;
    @MockitoBean
    private MesProEdhrWorkTaskService workTaskService;
    @MockitoBean
    private MesProEdhrOperationAuditService operationAuditService;
    @MockitoBean
    private MesProEdhrPermissionGateService permissionGateService;
    @MockitoBean
    private MesProEdhrPermissionScopeService permissionScopeService;

    @BeforeEach
    void setUpTenant() {
        TenantContextHolder.setTenantId(TENANT_ID);
        lenient().when(attachmentService.verifyAttachmentChain(anyLong()))
                .thenReturn(MesProBatchRecordExecutionAttachmentChainVerifyResult.builder()
                        .valid(true)
                        .checkedEventCount(0)
                        .headHash(null)
                        .build());
        lenient().when(permissionScopeService.evaluate(any()))
                .thenReturn(new MesProEdhrPermissionEvaluateResult()
                        .setDecisions(java.util.Map.of("AUDIT_VIEW", "ALLOW")));
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void pageDetailVerifyAndExport_returnFieldPathOldNewReasonActorSignatureAndValidHash() throws Exception {
        SeedData seed = insertValidAuditChain();

        MesProBatchRecordExecutionFieldAuditPageReqVO pageReqVO = new MesProBatchRecordExecutionFieldAuditPageReqVO()
                .setExecutionId(seed.executionId())
                .setFieldPath(FIELD_PATH);
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(10);
        PageResult<MesProBatchRecordExecutionFieldAuditItemRespVO> page = fieldAuditService.getPage(pageReqVO);

        assertEquals(1L, page.getTotal());
        MesProBatchRecordExecutionFieldAuditItemRespVO row = page.getList().get(0);
        assertEquals(FIELD_PATH, row.getFieldPath());
        assertEquals("36.6", row.getOldValueJson());
        assertEquals("37.5", row.getNewValueJson());
        assertEquals("operator correction", row.getReasonText());
        assertEquals(99L, row.getActorId());
        assertEquals(501L, row.getSignatureId());
        assertEquals(seed.auditHash(), row.getAuditHash());
        assertEquals(GENESIS, row.getPreviousHash());
        assertEquals("VALID", row.getHashVerification().getStatus());

        MesProBatchRecordExecutionFieldAuditDetailRespVO detail = fieldAuditService.getDetail(
                new MesProBatchRecordExecutionFieldAuditDetailReqVO()
                        .setExecutionId(seed.executionId())
                        .setAuditBatchId(seed.batchId()));
        assertEquals(seed.batchId(), detail.getAuditBatch().getId());
        assertEquals(FIELD_PATH, detail.getItems().get(0).getFieldPath());
        assertEquals(501L, detail.getSignature().getSignatureId());
        assertEquals("FIELD_CHANGE", detail.getSignature().getActionType());
        assertEquals("VALID", detail.getHashVerification().getStatus());
        assertEquals(seed.itemId(), detail.getItems().get(0).getId());
        assertEquals("36.6", detail.getItems().get(0).getOldValueJson());
        assertEquals("37.5", detail.getItems().get(0).getNewValueJson());

        MesProBatchRecordExecutionFieldAuditDetailRespVO itemDetail = fieldAuditService.getDetail(
                new MesProBatchRecordExecutionFieldAuditDetailReqVO()
                        .setExecutionId(seed.executionId())
                        .setAuditItemId(seed.itemId()));
        assertEquals(seed.batchId(), itemDetail.getAuditBatch().getId());
        assertEquals(seed.itemId(), itemDetail.getItems().get(0).getId());

        assertServiceException(() -> fieldAuditService.getDetail(
                        new MesProBatchRecordExecutionFieldAuditDetailReqVO()
                                .setExecutionId(seed.executionId())
                                .setAuditBatchId(7999L)
                                .setAuditItemId(seed.itemId())),
                PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_CHAIN_INVALID);

        MesProBatchRecordExecutionFieldAuditVerifyRespVO verify = fieldAuditService.verifyChain(
                new MesProBatchRecordExecutionFieldAuditVerifyReqVO().setExecutionId(seed.executionId()));
        assertEquals(seed.auditHash(), verify.getFieldAuditHeadHash());
        assertEquals(1L, verify.getVerifiedCount());
        assertEquals("VALID", verify.getHashVerification().getStatus());

        MesProBatchRecordExecutionFieldAuditExportReqVO jsonReqVO = new MesProBatchRecordExecutionFieldAuditExportReqVO()
                .setFormat("JSON");
        jsonReqVO.setExecutionId(seed.executionId());
        assertServiceException(() -> fieldAuditService.export(jsonReqVO),
                PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_EXPORT_FAILED);

        MesProBatchRecordExecutionFieldAuditExportReqVO exportReqVO = new MesProBatchRecordExecutionFieldAuditExportReqVO()
                .setFormat("PDF");
        exportReqVO.setExecutionId(seed.executionId());
        MesProBatchRecordExecutionFieldAuditExportRespVO export = fieldAuditService.export(exportReqVO);
        assertEquals("application/pdf", export.getContentType());
        assertEquals(1L, export.getRecordCount());
        assertEquals(seed.auditHash(), export.getFieldAuditHeadHash());
        assertEquals("VALID", export.getHashVerification().getStatus());
        assertNotNull(export.getSha256());
        assertTrue(new String(export.getContent(), 0, 5, StandardCharsets.US_ASCII).startsWith("%PDF-"));
        String body;
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(export.getContent()))) {
            body = new PDFTextStripper().getText(document);
        }
        assertTrue(body.contains("本系统认证并可校验"));
        assertTrue(body.contains(FIELD_PATH));
        assertTrue(body.contains("签名 ID: 501"));
        assertTrue(body.contains("签名挑战哈希: 8888888888888888888888888888888888888888888888888888888888888888"));
        assertTrue(body.contains("校验状态: VALID"));
    }

    @Test
    void detailAndExport_rejectBrokenChainInsteadOfReturningMockSuccess() {
        SeedData seed = insertValidAuditChain();
        itemMapper.updateById(new MesProBatchRecordExecutionFieldAuditItemDO()
                .setId(seed.itemId())
                .setReasonText("tampered"));

        assertServiceException(() -> fieldAuditService.getDetail(
                        new MesProBatchRecordExecutionFieldAuditDetailReqVO()
                                .setExecutionId(seed.executionId())
                                .setAuditBatchId(seed.batchId())),
                PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_CHAIN_INVALID);
        MesProBatchRecordExecutionFieldAuditExportReqVO exportReqVO = new MesProBatchRecordExecutionFieldAuditExportReqVO()
                .setFormat("PDF");
        exportReqVO.setExecutionId(seed.executionId());
        assertServiceException(() -> fieldAuditService.export(exportReqVO),
                PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_CHAIN_INVALID);
    }

    @Test
    void verifyChain_rejectsTamperedSignatureBindingAsSignatureMismatch() {
        SeedData seed = insertValidAuditChain();
        MesProBatchRecordExecutionFieldAuditHashVerification valid = fieldAuditService.verifyChain(seed.executionId());
        assertEquals(MesProBatchRecordExecutionFieldAuditHashVerificationStatus.VALID, valid.getStatus());

        signatureMapper.updateById(new MesProBatchRecordExecutionSignatureDO()
                .setId(501L)
                .setFieldAuditHeadHash("1111111111111111111111111111111111111111111111111111111111111111"));

        MesProBatchRecordExecutionFieldAuditHashVerification tampered = fieldAuditService.verifyChain(seed.executionId());
        assertEquals(MesProBatchRecordExecutionFieldAuditHashVerificationStatus.SIGNATURE_MISMATCH,
                tampered.getStatus());
        assertEquals(seed.batchId(), tampered.getBrokenBatchId());
    }

    @Test
    void responsibilityExportRejectsIncompleteOverallEvidence() {
        Long executionId = insertUnknownResponsibilityExecution();
        MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO summary =
                fieldAuditService.getResponsibilitySummary(
                        new MesProBatchRecordExecutionFieldResponsibilitySummaryReqVO()
                                .setExecutionId(executionId)
                                .setPageSize(200));

        assertEquals(MesProBatchRecordExecutionResponsibilityEvidenceStatus.EVIDENCE_MISSING,
                summary.getOverallEvidenceStatus());
        assertEquals(MesProBatchRecordExecutionResponsibilityValueOrigin.UNKNOWN,
                summary.getList().get(0).getValueOrigin());
        assertEquals("21", summary.getList().get(0).getCurrentValueJson());

        assertServiceException(() -> fieldAuditService.exportResponsibility(
                        new MesProBatchRecordExecutionFieldResponsibilityExportReqVO()
                                .setExecutionId(executionId)
                                .setFormat("XLSX")),
                PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_EXPORT_FAILED);
    }

    @Test
    void responsibilityExportCreatesCompleteSnapshotWorkbookWithoutSensitiveColumns() throws Exception {
        SeedData seed = insertCompleteResponsibilityEvidence();
        MesProBatchRecordExecutionDO execution = executionMapper.selectById(seed.executionId());
        String generatedAt;
        String currentNumberHash = MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                MesProBatchRecordExecutionFieldAuditValueType.NUMBER, 37.2);
        String batchCodeHash = MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                MesProBatchRecordExecutionFieldAuditValueType.STRING, "AUTO-01");
        String nullValueHash = MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                MesProBatchRecordExecutionFieldAuditValueType.NULL, null);

        MesProBatchRecordExecutionFieldResponsibilityExportRespVO export =
                fieldAuditService.exportResponsibility(
                        new MesProBatchRecordExecutionFieldResponsibilityExportReqVO()
                                .setExecutionId(seed.executionId())
                                .setFormat("XLSX"));

        assertEquals("field-responsibility-" + seed.executionId() + ".xlsx", export.getFileName());
        assertEquals("XLSX", export.getFormat());
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                export.getContentType());
        assertEquals(3L, export.getRecordCount());
        assertEquals(1L, export.getFieldAuditRevision());
        assertEquals(seed.auditHash(), export.getFieldAuditHeadHash());
        assertEquals(executionMapper.selectById(seed.executionId()).getCellValuesHash(),
                export.getCellValuesHash());
        assertEquals(MesProBatchRecordExecutionResponsibilityEvidenceStatus.COMPLETE,
                export.getEvidenceStatus());
        assertEquals(List.of(), export.getReasonCodes());
        assertEquals(List.of(), export.getContextWarnings());
        assertNotNull(export.getGeneratedAt());
        generatedAt = export.getGeneratedAt().toString();

        byte[] content = Base64.getDecoder().decode(export.getContentBase64());
        assertEquals(DigestUtil.sha256Hex(content), export.getSha256());
        assertTrue(content.length > 0);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheet("Responsibility");
            assertNotNull(sheet);
            Map<String, Integer> headers = headerIndexes(sheet.getRow(0));
            assertEquals(RESPONSIBILITY_EXPORT_HEADERS, new ArrayList<>(headers.keySet()));
            assertEquals(RESPONSIBILITY_EXPORT_HEADERS.size(), headers.size());
            assertEquals(export.getRecordCount().intValue(), sheet.getLastRowNum());
            assertEquals(export.getRecordCount().intValue() + 1, sheet.getPhysicalNumberOfRows());
            assertEquals(List.of(
                    generatedAt,
                    String.valueOf(seed.executionId()),
                    "EXE-EXPORT-8101",
                    "100",
                    "101",
                    "REPORT-EXPORT",
                    "1",
                    seed.auditHash(),
                    execution.getCellValuesHash(),
                    "sheet[0].rows[1].cells[1]",
                    "temperature",
                    "Temperature",
                    "1",
                    "1",
                    "input-number",
                    "NUMBER",
                    "37.2",
                    "37.2",
                    currentNumberHash,
                    "HUMAN",
                    "101",
                    "Alice",
                    "2026-07-10T09:30",
                    "501",
                    "alice",
                    "Alice QA",
                    "2026-07-10T09:30",
                    "101",
                    "Alice",
                    "2026-07-10T09:30",
                    "501",
                    "alice",
                    "Alice QA",
                    "2026-07-10T09:30",
                    "COMPLETE",
                    "",
                    "1",
                    String.valueOf(seed.itemId()),
                    ""), rowValues(sheet, 1));
            assertEquals(expectedUntouchedResponsibilityRow(
                    generatedAt,
                    seed,
                    execution.getCellValuesHash(),
                    "sheet[0].rows[1].cells[2]",
                    "batchCode",
                    "Batch Code",
                    "2",
                    "STRING",
                    "\"AUTO-01\"",
                    "AUTO-01",
                    batchCodeHash,
                    "SYSTEM_BASELINE",
                    ""), rowValues(sheet, 2));
            assertEquals(expectedUntouchedResponsibilityRow(
                    generatedAt,
                    seed,
                    execution.getCellValuesHash(),
                    "sheet[0].rows[1].cells[3]",
                    "notes",
                    "Notes",
                    "3",
                    "NULL",
                    "null",
                    "",
                    nullValueHash,
                    "EMPTY_UNTOUCHED",
                    ""), rowValues(sheet, 3));
            StringBuilder workbookText = new StringBuilder();
            sheet.forEach(row -> row.forEach(cell -> workbookText.append(cell.getStringCellValue()).append('\n')));
            SENSITIVE_WORKBOOK_SENTINELS.forEach(
                    value -> assertFalse(workbookText.toString().contains(value), value));
            assertFalse(headers.keySet().stream()
                    .map(name -> name.toLowerCase(java.util.Locale.ROOT))
                    .anyMatch(name -> name.contains("client ip")
                            || name.contains("user-agent")
                            || name.contains("user agent")
                            || name.contains("password")
                            || name.contains("token")
                            || name.contains("auth")
                            || name.contains("authentication")
                            || name.contains("session")
                            || name.contains("credential")
                            || name.contains("secret")));
        }
    }

    @Test
    void responsibilityExportIncludesVersionContextWarningWithoutChangingCompleteEvidence() throws Exception {
        SeedData seed = insertCompleteResponsibilityEvidence(true);

        MesProBatchRecordExecutionFieldResponsibilityExportRespVO export =
                fieldAuditService.exportResponsibility(
                        new MesProBatchRecordExecutionFieldResponsibilityExportReqVO()
                                .setExecutionId(seed.executionId())
                                .setFormat("XLSX"));

        assertEquals(MesProBatchRecordExecutionResponsibilityEvidenceStatus.COMPLETE, export.getEvidenceStatus());
        assertEquals(List.of(), export.getReasonCodes());
        assertEquals(List.of(MesProBatchRecordExecutionResponsibilityContextWarning.VERSION_CONTEXT_MISSING),
                export.getContextWarnings());
        byte[] content = Base64.getDecoder().decode(export.getContentBase64());
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheet("Responsibility");
            Map<String, Integer> headers = headerIndexes(sheet.getRow(0));
            assertEquals(RESPONSIBILITY_EXPORT_HEADERS, new ArrayList<>(headers.keySet()));
            assertEquals(List.of("HUMAN", "SYSTEM_BASELINE", "EMPTY_UNTOUCHED"), List.of(
                    cell(sheet, 1, headers, "Value Origin"),
                    cell(sheet, 2, headers, "Value Origin"),
                    cell(sheet, 3, headers, "Value Origin")));
            assertEquals(List.of("COMPLETE", "COMPLETE", "COMPLETE"), List.of(
                    cell(sheet, 1, headers, "Evidence Status"),
                    cell(sheet, 2, headers, "Evidence Status"),
                    cell(sheet, 3, headers, "Evidence Status")));
            assertEquals(List.of("", "", ""), List.of(
                    cell(sheet, 1, headers, "Reason Codes"),
                    cell(sheet, 2, headers, "Reason Codes"),
                    cell(sheet, 3, headers, "Reason Codes")));
            assertEquals(List.of("VERSION_CONTEXT_MISSING", "VERSION_CONTEXT_MISSING",
                    "VERSION_CONTEXT_MISSING"), List.of(
                    cell(sheet, 1, headers, "Context Warnings"),
                    cell(sheet, 2, headers, "Context Warnings"),
                    cell(sheet, 3, headers, "Context Warnings")));
            assertEquals("", cell(sheet, 1, headers, "Batch Record Version ID"));
            assertEquals(export.getRecordCount().intValue(), sheet.getLastRowNum());
        }
    }

    @Test
    void responsibilityExportRejectsBlockedEvidenceAndPreservesUnknownSummaryOrigin() {
        Long executionId = insertUnknownResponsibilityExecution(true);
        MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO summary =
                fieldAuditService.getResponsibilitySummary(
                        new MesProBatchRecordExecutionFieldResponsibilitySummaryReqVO()
                                .setExecutionId(executionId)
                                .setPageSize(200));

        assertEquals(MesProBatchRecordExecutionResponsibilityEvidenceStatus.BLOCKED,
                summary.getOverallEvidenceStatus());
        assertEquals(List.of(MesProBatchRecordExecutionResponsibilityReasonCode.CHAIN_INVALID),
                summary.getOverallReasonCodes());
        assertEquals(MesProBatchRecordExecutionResponsibilityValueOrigin.UNKNOWN,
                summary.getList().get(0).getValueOrigin());
        assertEquals("21", summary.getList().get(0).getCurrentValueJson());

        assertServiceException(() -> fieldAuditService.exportResponsibility(
                        new MesProBatchRecordExecutionFieldResponsibilityExportReqVO()
                                .setExecutionId(executionId)
                                .setFormat("XLSX")),
                PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_EXPORT_FAILED);
    }

    @Test
    void responsibilityExportRequiresAuditViewBeforeGeneratingWorkbook() {
        SeedData seed = insertCompleteResponsibilityEvidence();
        clearInvocations(itemMapper, batchMapper, signatureMapper, workTaskMapper);
        when(permissionScopeService.evaluate(any()))
                .thenReturn(new MesProEdhrPermissionEvaluateResult()
                        .setDecisions(Map.of("AUDIT_VIEW", "DENY")));

        cn.iocoder.yudao.framework.common.exception.ServiceException exception = assertThrows(
                cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> fieldAuditService.exportResponsibility(
                        new MesProBatchRecordExecutionFieldResponsibilityExportReqVO()
                                .setExecutionId(seed.executionId())
                                .setFormat("XLSX")));
        assertEquals(PRO_EDHR_OBJECT_PERMISSION_DENIED.getCode(), exception.getCode());
        verifyNoInteractions(itemMapper, batchMapper, signatureMapper, workTaskMapper);
    }

    private Long insertUnknownResponsibilityExecution() {
        return insertUnknownResponsibilityExecution(false);
    }

    private Long insertUnknownResponsibilityExecution(boolean invalidChain) {
        Long executionId = 8100L;
        String snapshotJson = """
                {"fields":[
                  {"fieldPath":"sheet[0].rows[1].cells[1]","fieldKey":"temperature","label":"Temperature","rowIndex":1,"columnIndex":1,"component":"input-number","valueType":"NUMBER","defaultValue":20}
                ]}
                """;
        String valueHash = MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                MesProBatchRecordExecutionFieldAuditValueType.NUMBER, 21);
        String cellValuesJson = """
                [{"fieldPath":"sheet[0].rows[1].cells[1]","fieldKey":"temperature","rowIndex":1,"columnIndex":1,"valueType":"NUMBER","value":21,"valueDisplay":"21","valueHash":"%s"}]
                """.formatted(valueHash);
        executionMapper.insert(MesProBatchRecordExecutionDO.builder()
                .id(executionId)
                .executionCode("EXE-UNKNOWN-8100")
                .workOrderId(1100L)
                .workOrderCode("WO-UNKNOWN-8100")
                .batchCode("BATCH-UNKNOWN-8100")
                .batchRecordDefinitionId(100L)
                .batchRecordVersionId(101L)
                .batchRecordReportId("REPORT-UNKNOWN")
                .permissionScopeId(801L)
                .status(0)
                .sheetLayoutJson("{}")
                .metaJson("{}")
                .executionSnapshotJson(snapshotJson)
                .cellValuesJson(cellValuesJson)
                .cellValuesHash(MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(cellValuesJson))
                .fieldAuditRevision(invalidChain ? 1L : 0L)
                .fieldAuditHeadHash(GENESIS)
                .build());
        return executionId;
    }

    private SeedData insertCompleteResponsibilityEvidence() {
        return insertCompleteResponsibilityEvidence(false);
    }

    private SeedData insertCompleteResponsibilityEvidence(boolean versionContextMissing) {
        Long executionId = 8101L;
        Long batchId = 7101L;
        Long itemId = 9101L;
        Long signatureId = 501L;
        String snapshotJson = """
                {"fields":[
                  {"fieldPath":"sheet[0].rows[1].cells[1]","fieldKey":"temperature","label":"Temperature","rowIndex":1,"columnIndex":1,"component":"input-number","valueType":"NUMBER","defaultValue":20},
                  {"fieldPath":"sheet[0].rows[1].cells[2]","fieldKey":"batchCode","label":"Batch Code","rowIndex":1,"columnIndex":2,"component":"input","valueType":"STRING","defaultValue":"AUTO-01"},
                  {"fieldPath":"sheet[0].rows[1].cells[3]","fieldKey":"notes","label":"Notes","rowIndex":1,"columnIndex":3,"component":"input","valueType":"NULL","defaultValue":null}
                ]}
                """;
        String currentValueHash = MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                MesProBatchRecordExecutionFieldAuditValueType.NUMBER, 37.2);
        String cellValuesJson = """
                [{"fieldPath":"sheet[0].rows[1].cells[1]","fieldKey":"temperature","rowIndex":1,"columnIndex":1,"valueType":"NUMBER","value":37.2,"valueDisplay":"37.2","valueHash":"%s"}]
                """.formatted(currentValueHash);
        String cellValuesHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(cellValuesJson);
        LocalDateTime changedAt = LocalDateTime.of(2026, 7, 10, 9, 30);
        MesProBatchRecordExecutionSignatureDO signature = MesProBatchRecordExecutionSignatureDO.builder()
                .id(signatureId)
                .executionId(executionId)
                .actorId(101L)
                .actorName("Alice")
                .actorUsernameSnapshot("alice")
                .actorNicknameSnapshot("Alice QA")
                .actionType("FIELD_CHANGE")
                .signatureMode(MesProBatchRecordExecutionSignatureService.SIGNATURE_MODE_PASSWORD)
                .passwordVerified(Boolean.TRUE)
                .authenticationMethod(SENSITIVE_WORKBOOK_SENTINELS.get(2))
                .clientIpSnapshot(SENSITIVE_WORKBOOK_SENTINELS.get(0))
                .userAgentSnapshot(SENSITIVE_WORKBOOK_SENTINELS.get(1))
                .signedAt(changedAt)
                .signatureDisplayAt(changedAt)
                .signatureTimeMode("SERVER")
                .selectedTimeZone(SENSITIVE_WORKBOOK_SENTINELS.get(3))
                .selectedTimeReason(SENSITIVE_WORKBOOK_SENTINELS.get(4))
                .selectedTimePolicyVersion(SENSITIVE_WORKBOOK_SENTINELS.get(5))
                .selectedTimeAuditHash(SENSITIVE_WORKBOOK_SENTINELS.get(6) + "|"
                        + SENSITIVE_WORKBOOK_SENTINELS.get(8))
                .reasonCategory("OPERATOR_ENTRY")
                .reason("entered")
                .auditBatchId(batchId)
                .signatureChallengeHash(SENSITIVE_WORKBOOK_SENTINELS.get(7))
                .fieldAuditRevision(1L)
                .cellValuesHash(cellValuesHash)
                .build();
        String signatureProjectionHash = MesProBatchRecordExecutionFieldAuditHasher.hashSignatureProjection(
                signatureProjection(signature));
        String oldValueHash = MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                MesProBatchRecordExecutionFieldAuditValueType.NUMBER, 20);
        String auditHash = MesProBatchRecordExecutionFieldAuditHasher.hashItem(
                MesProBatchRecordExecutionFieldAuditItemHashInput.builder()
                        .fieldPath("sheet[0].rows[1].cells[1]")
                        .fieldKey("temperature")
                        .rowIndex(1)
                        .columnIndex(1)
                        .valueType(MesProBatchRecordExecutionFieldAuditValueType.NUMBER)
                        .oldValueJson("20")
                        .oldValueDisplay("20")
                        .oldValueHash(oldValueHash)
                        .newValueJson("37.2")
                        .newValueDisplay("37.2")
                        .newValueHash(currentValueHash)
                        .reasonCategory("OPERATOR_ENTRY")
                        .reasonText("entered")
                        .actorId(101L)
                        .actorName("Alice")
                        .signatureProjectionHash(signatureProjectionHash)
                        .previousHash(GENESIS)
                        .changedAt(changedAt)
                        .build());
        signature.setFieldAuditHeadHash(auditHash);

        executionMapper.insert(MesProBatchRecordExecutionDO.builder()
                .id(executionId)
                .executionCode("EXE-EXPORT-8101")
                .workOrderId(1101L)
                .workOrderCode("WO-EXPORT-8101")
                .batchCode("BATCH-EXPORT-8101")
                .batchRecordDefinitionId(100L)
                .batchRecordVersionId(versionContextMissing ? null : 101L)
                .batchRecordReportId("REPORT-EXPORT")
                .permissionScopeId(801L)
                .status(0)
                .sheetLayoutJson("{}")
                .metaJson("{}")
                .executionSnapshotJson(snapshotJson)
                .cellValuesJson(cellValuesJson)
                .cellValuesHash(cellValuesHash)
                .fieldAuditRevision(1L)
                .fieldAuditHeadHash(auditHash)
                .fieldAuditLastBatchId(batchId)
                .build());
        signatureMapper.insert(signature);
        batchMapper.insert(MesProBatchRecordExecutionFieldAuditBatchDO.builder()
                .id(batchId)
                .executionId(executionId)
                .idempotencyKey("idem-responsibility-export")
                .requestHash("7777777777777777777777777777777777777777777777777777777777777777")
                .actionType("FIELD_CHANGE")
                .reasonCategory("OPERATOR_ENTRY")
                .reasonText("entered")
                .fieldCount(1)
                .actorId(101L)
                .actorName("Alice")
                .signatureId(signatureId)
                .signatureChallengeHash(SENSITIVE_WORKBOOK_SENTINELS.get(7))
                .signatureProjectionHash(signatureProjectionHash)
                .baseCellValuesHash("base-cell-values")
                .beforeCellValuesHash("before-cell-values")
                .afterCellValuesHash(cellValuesHash)
                .baseFieldAuditRevision(0L)
                .beforeFieldAuditRevision(0L)
                .afterFieldAuditRevision(1L)
                .baseFieldAuditHeadHash(GENESIS)
                .previousHeadHash(GENESIS)
                .newHeadHash(auditHash)
                .hashVerificationJson("{\"status\":\"VALID\"}")
                .changedAt(changedAt)
                .tenantId(TENANT_ID)
                .build());
        itemMapper.insert(MesProBatchRecordExecutionFieldAuditItemDO.builder()
                .id(itemId)
                .auditBatchId(batchId)
                .executionId(executionId)
                .fieldAuditRevision(1L)
                .batchItemIndex(0)
                .fieldPath("sheet[0].rows[1].cells[1]")
                .fieldKey("temperature")
                .fieldLabel("Temperature")
                .rowIndex(1)
                .columnIndex(1)
                .component("input-number")
                .valueType("NUMBER")
                .oldValueJson("20")
                .oldValueDisplay("20")
                .oldValueHash(oldValueHash)
                .newValueJson("37.2")
                .newValueDisplay("37.2")
                .newValueHash(currentValueHash)
                .reasonCategory("OPERATOR_ENTRY")
                .reasonText("entered")
                .actorId(101L)
                .actorName("Alice")
                .signatureId(signatureId)
                .signatureProjectionHash(signatureProjectionHash)
                .previousHash(GENESIS)
                .auditHash(auditHash)
                .beforeCellValuesHash("before-cell-values")
                .afterCellValuesHash(cellValuesHash)
                .executionSnapshotHash(
                        MesProBatchRecordExecutionFieldAuditHasher.hashExecutionSnapshot(snapshotJson))
                .changedAt(changedAt)
                .tenantId(TENANT_ID)
                .build());
        return new SeedData(executionId, batchId, itemId, auditHash);
    }

    private MesProBatchRecordExecutionFieldAuditSignatureProjection signatureProjection(
            MesProBatchRecordExecutionSignatureDO signature) {
        return new MesProBatchRecordExecutionFieldAuditSignatureProjection()
                .setId(signature.getId())
                .setExecutionId(signature.getExecutionId())
                .setActionType(signature.getActionType())
                .setActorId(signature.getActorId())
                .setActorName(signature.getActorName())
                .setSignatureMode(signature.getSignatureMode())
                .setPasswordVerified(signature.getPasswordVerified())
                .setSignedAt(signature.getSignedAt())
                .setSelectedSignedAt(signature.getSelectedSignedAt())
                .setSignatureDisplayAt(signature.getSignatureDisplayAt())
                .setSignatureTimeMode(signature.getSignatureTimeMode())
                .setSelectedTimeZone(signature.getSelectedTimeZone())
                .setSelectedTimeReason(signature.getSelectedTimeReason())
                .setSelectedTimePolicyVersion(signature.getSelectedTimePolicyVersion())
                .setSelectedTimeAuditHash(signature.getSelectedTimeAuditHash())
                .setReasonCategory(signature.getReasonCategory())
                .setReasonText(signature.getReason())
                .setAuditBatchId(signature.getAuditBatchId())
                .setSignatureChallengeHash(signature.getSignatureChallengeHash())
                .setFieldAuditRevision(signature.getFieldAuditRevision())
                .setFieldAuditHeadHash(signature.getFieldAuditHeadHash())
                .setCellValuesHash(signature.getCellValuesHash());
    }

    private Map<String, Integer> headerIndexes(Row header) {
        Map<String, Integer> result = new LinkedHashMap<>();
        header.forEach(cell -> result.put(cell.getStringCellValue(), cell.getColumnIndex()));
        return result;
    }

    private String cell(Sheet sheet, int rowIndex, Map<String, Integer> headers, String header) {
        return sheet.getRow(rowIndex).getCell(headers.get(header)).getStringCellValue();
    }

    private List<String> rowValues(Sheet sheet, int rowIndex) {
        List<String> result = new ArrayList<>();
        for (int columnIndex = 0; columnIndex < RESPONSIBILITY_EXPORT_HEADERS.size(); columnIndex++) {
            result.add(sheet.getRow(rowIndex).getCell(columnIndex).getStringCellValue());
        }
        return result;
    }

    private List<String> expectedUntouchedResponsibilityRow(
            String generatedAt,
            SeedData seed,
            String cellValuesHash,
            String fieldPath,
            String fieldKey,
            String fieldLabel,
            String columnIndex,
            String valueType,
            String currentValueJson,
            String currentValueDisplay,
            String currentValueHash,
            String valueOrigin,
            String contextWarnings) {
        List<String> result = new ArrayList<>(List.of(
                generatedAt,
                String.valueOf(seed.executionId()),
                "EXE-EXPORT-8101",
                "100",
                "101",
                "REPORT-EXPORT",
                "1",
                seed.auditHash(),
                cellValuesHash,
                fieldPath,
                fieldKey,
                fieldLabel,
                "1",
                columnIndex,
                "input",
                valueType,
                currentValueJson,
                currentValueDisplay,
                currentValueHash,
                valueOrigin));
        result.addAll(java.util.Collections.nCopies(14, ""));
        result.addAll(List.of("COMPLETE", "", "0", "", contextWarnings));
        return result;
    }

    private SeedData insertValidAuditChain() {
        Long executionId = 8001L;
        String beforeJson = "[{\"rowIndex\":1,\"columnIndex\":2,\"value\":\"36.6\"}]";
        String afterJson = "[{\"rowIndex\":1,\"columnIndex\":2,\"value\":\"37.5\"}]";
        String beforeHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(beforeJson);
        String afterHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(afterJson);
        MesProBatchRecordExecutionFieldAuditItemHashInput hashInput =
                MesProBatchRecordExecutionFieldAuditItemHashInput.builder()
                        .fieldPath(FIELD_PATH)
                        .fieldKey("temperature")
                        .rowIndex(1)
                        .columnIndex(2)
                        .valueType(MesProBatchRecordExecutionFieldAuditValueType.NUMBER)
                        .oldValueJson("36.6")
                        .oldValueDisplay("36.6")
                        .oldValueHash(MesProBatchRecordExecutionFieldAuditHasher.hashCanonicalTypedValue("36.6"))
                        .newValueJson("37.5")
                        .newValueDisplay("37.5")
                        .newValueHash(MesProBatchRecordExecutionFieldAuditHasher.hashCanonicalTypedValue("37.5"))
                        .reasonCategory("CORRECTION")
                        .reasonText("operator correction")
                        .actorId(99L)
                        .actorName("QA")
                        .signatureProjectionHash("9999999999999999999999999999999999999999999999999999999999999999")
                        .previousHash(GENESIS)
                        .changedAt(CHANGED_AT)
                        .build();
        String auditHash = MesProBatchRecordExecutionFieldAuditHasher.hashItem(hashInput);
        MesProBatchRecordExecutionDO execution = MesProBatchRecordExecutionDO.builder()
                .id(executionId)
                .executionCode("BRE-FIELD-AUDIT-QUERY")
                .workOrderId(1001L)
                .workOrderCode("WO-FIELD-AUDIT")
                .batchCode("BATCH-FIELD-AUDIT")
                .status(0)
                .sheetLayoutJson("{}")
                .metaJson("{}")
                .executionSnapshotJson("{\"fields\":[]}")
                .cellValuesJson(afterJson)
                .cellValuesHash(afterHash)
                .fieldAuditRevision(1L)
                .fieldAuditHeadHash(auditHash)
                .fieldAuditLastBatchId(7001L)
                .build();
        executionMapper.insert(execution);
        signatureMapper.insert(MesProBatchRecordExecutionSignatureDO.builder()
                .id(501L)
                .executionId(executionId)
                .actorId(99L)
                .actorName("QA")
                .actionType("FIELD_CHANGE")
                .signatureMode("PASSWORD")
                .passwordVerified(Boolean.TRUE)
                .signedAt(CHANGED_AT)
                .reasonCategory("CORRECTION")
                .reason("operator correction")
                .auditBatchId(7001L)
                .signatureChallengeHash("8888888888888888888888888888888888888888888888888888888888888888")
                .fieldAuditRevision(1L)
                .fieldAuditHeadHash(auditHash)
                .cellValuesHash(afterHash)
                .build());
        batchMapper.insert(MesProBatchRecordExecutionFieldAuditBatchDO.builder()
                .id(7001L)
                .executionId(executionId)
                .idempotencyKey("idem-query")
                .requestHash("7777777777777777777777777777777777777777777777777777777777777777")
                .actionType("FIELD_CHANGE")
                .reasonCategory("CORRECTION")
                .reasonText("operator correction")
                .fieldCount(1)
                .actorId(99L)
                .actorName("QA")
                .signatureId(501L)
                .signatureChallengeHash("8888888888888888888888888888888888888888888888888888888888888888")
                .signatureProjectionHash("9999999999999999999999999999999999999999999999999999999999999999")
                .baseCellValuesHash(beforeHash)
                .beforeCellValuesHash(beforeHash)
                .afterCellValuesHash(afterHash)
                .baseFieldAuditRevision(0L)
                .beforeFieldAuditRevision(0L)
                .afterFieldAuditRevision(1L)
                .baseFieldAuditHeadHash(GENESIS)
                .previousHeadHash(GENESIS)
                .newHeadHash(auditHash)
                .hashVerificationJson("{\"status\":\"VALID\"}")
                .changedAt(CHANGED_AT)
                .tenantId(TENANT_ID)
                .build());
        MesProBatchRecordExecutionFieldAuditItemDO item = MesProBatchRecordExecutionFieldAuditItemDO.builder()
                .id(9001L)
                .auditBatchId(7001L)
                .executionId(executionId)
                .fieldAuditRevision(1L)
                .batchItemIndex(1)
                .fieldPath(FIELD_PATH)
                .fieldKey("temperature")
                .fieldLabel("Temperature")
                .rowIndex(1)
                .columnIndex(2)
                .component("input-number")
                .valueType("NUMBER")
                .oldValueJson("36.6")
                .oldValueDisplay("36.6")
                .oldValueHash(MesProBatchRecordExecutionFieldAuditHasher.hashCanonicalTypedValue("36.6"))
                .newValueJson("37.5")
                .newValueDisplay("37.5")
                .newValueHash(MesProBatchRecordExecutionFieldAuditHasher.hashCanonicalTypedValue("37.5"))
                .reasonCategory("CORRECTION")
                .reasonText("operator correction")
                .actorId(99L)
                .actorName("QA")
                .signatureId(501L)
                .signatureProjectionHash("9999999999999999999999999999999999999999999999999999999999999999")
                .previousHash(GENESIS)
                .auditHash(auditHash)
                .beforeCellValuesHash(beforeHash)
                .afterCellValuesHash(afterHash)
                .executionSnapshotHash(MesProBatchRecordExecutionFieldAuditHasher.hashExecutionSnapshot("{\"fields\":[]}"))
                .changedAt(CHANGED_AT)
                .tenantId(TENANT_ID)
                .build();
        itemMapper.insert(item);
        return new SeedData(executionId, 7001L, 9001L, auditHash);
    }

    private record SeedData(Long executionId, Long batchId, Long itemId, String auditHash) {
    }
}
