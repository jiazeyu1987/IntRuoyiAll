package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormFillLogDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormFillLogPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormFillLogPageRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionFieldAuditBatchDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionFieldAuditItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionFieldAuditBatchMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionFieldAuditItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(MesProEdhrFormFillLogServiceImpl.class)
class MesProEdhrFormFillLogServiceImplTest extends BaseDbUnitTest {

    private static final Long TENANT_ID = 122L;
    private static final String HASH_64 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    @Resource
    private MesProEdhrFormFillLogService formFillLogService;
    @Resource
    private MesProBatchRecordExecutionMapper executionMapper;
    @Resource
    private MesProBatchRecordExecutionFieldAuditBatchMapper auditBatchMapper;
    @Resource
    private MesProBatchRecordExecutionFieldAuditItemMapper auditItemMapper;
    @Resource
    private MesProEdhrBatchExecutionTaskMapper batchExecutionTaskMapper;

    @BeforeEach
    void setUpTenant() {
        TenantContextHolder.setTenantId(TENANT_ID);
        TenantContextHolder.setIgnore(false);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void getPage_filtersByManagerDimensionsAndReturnsClickableBatchContext() {
        MesProBatchRecordExecutionDO execution = insertExecution(2001L, "EXEC-001", "RPT-PUMP",
                "压力泵生产记录", "WO-001", "BATCH-001");
        insertBatchExecutionTask(3001L, execution.getId(), 9001L, "RPT-PUMP", "压力泵生产记录");
        insertAuditBatch(1001L, execution.getId(), 101L, "测试填写人",
                LocalDateTime.of(2026, 7, 13, 9, 30), 2);
        insertAuditItem(4001L, 1001L, execution.getId(), 1L, "temperature", "温度", "36.6", "37.5");
        insertAuditItem(4002L, 1001L, execution.getId(), 2L, "pressure", "压力", "1.1", "1.2");
        insertNonMatchingAuditData();

        PageResult<MesProEdhrFormFillLogPageRespVO> page = formFillLogService.getPage(new MesProEdhrFormFillLogPageReqVO()
                .setFormKeyword("压力泵")
                .setBatchCode("BATCH-001")
                .setWorkOrderCode("WO-001")
                .setActorId(101L)
                .setChangedAtStart(LocalDateTime.of(2026, 7, 13, 0, 0))
                .setChangedAtEnd(LocalDateTime.of(2026, 7, 13, 23, 59)));

        assertEquals(1L, page.getTotal());
        MesProEdhrFormFillLogPageRespVO row = page.getList().get(0);
        assertEquals(1001L, row.getAuditBatchId());
        assertEquals(execution.getId(), row.getExecutionId());
        assertEquals("EXEC-001", row.getExecutionCode());
        assertEquals("RPT-PUMP", row.getBatchRecordReportId());
        assertEquals("压力泵生产记录", row.getFormName());
        assertEquals(9001L, row.getBatchExecutionId());
        assertEquals("BATCH-001", row.getBatchCode());
        assertEquals("WO-001", row.getWorkOrderCode());
        assertEquals(101L, row.getActorId());
        assertEquals("测试填写人", row.getActorName());
        assertEquals(2, row.getFieldCount());
        assertEquals("COMPLETE", row.getContextStatus());
        assertEquals("VALID", row.getHashStatus());
        assertTrue(row.getCellSummary().contains("温度=37.5"));
        assertTrue(row.getCellSummary().contains("压力=1.2"));

        PageResult<MesProEdhrFormFillLogPageRespVO> reportIdKeywordPage = formFillLogService.getPage(
                new MesProEdhrFormFillLogPageReqVO().setFormKeyword("RPT-PUMP"));
        assertEquals(1L, reportIdKeywordPage.getTotal());
        assertEquals(1001L, reportIdKeywordPage.getList().get(0).getAuditBatchId());
    }

    @Test
    void getPage_missingBatchExecutionTaskMarksContextMissingWithoutGuessingRoute() {
        MesProBatchRecordExecutionDO execution = insertExecution(2101L, "EXEC-NO-TASK", "RPT-LOST",
                "历史表单", "WO-LOST", "BATCH-LOST");
        insertAuditBatch(1101L, execution.getId(), 102L, "历史填写人",
                LocalDateTime.of(2026, 7, 13, 10, 0), 1);
        insertAuditItem(4101L, 1101L, execution.getId(), 1L, "appearance", "外观", "", "合格");

        PageResult<MesProEdhrFormFillLogPageRespVO> page = formFillLogService.getPage(new MesProEdhrFormFillLogPageReqVO()
                .setExecutionCode("EXEC-NO-TASK"));

        assertEquals(1L, page.getTotal());
        MesProEdhrFormFillLogPageRespVO row = page.getList().get(0);
        assertEquals("BATCH_CONTEXT_MISSING", row.getContextStatus());
        assertNull(row.getBatchExecutionId());
        assertEquals("BATCH-LOST", row.getBatchCode());
        assertEquals("WO-LOST", row.getWorkOrderCode());
    }

    @Test
    void getDetail_returnsCellItemsForOneAuditBatchWithoutReasonRequirement() {
        MesProBatchRecordExecutionDO execution = insertExecution(2201L, "EXEC-DETAIL", "RPT-DETAIL",
                "明细表单", "WO-DETAIL", "BATCH-DETAIL");
        insertBatchExecutionTask(3201L, execution.getId(), 9201L, "RPT-DETAIL", "明细表单");
        insertAuditBatch(1201L, execution.getId(), 103L, "明细填写人",
                LocalDateTime.of(2026, 7, 13, 11, 0), 2);
        insertAuditItem(4201L, 1201L, execution.getId(), 1L, "temperature", "温度", "36.6", "37.5");
        insertAuditItem(4202L, 1201L, execution.getId(), 2L, "pressure", "压力", "1.1", "1.2");

        MesProEdhrFormFillLogDetailRespVO detail = formFillLogService.getDetail(1201L);

        assertEquals(1201L, detail.getAuditBatchId());
        assertEquals("BATCH-DETAIL", detail.getBatchCode());
        assertEquals("WO-DETAIL", detail.getWorkOrderCode());
        assertEquals(2, detail.getItems().size());
        assertEquals("温度", detail.getItems().get(0).getFieldLabel());
        assertEquals("37.5", detail.getItems().get(0).getNewValueDisplay());
        assertEquals("压力", detail.getItems().get(1).getFieldLabel());
    }

    private MesProBatchRecordExecutionDO insertExecution(Long id, String executionCode, String reportId,
                                                         String templateName, String workOrderCode,
                                                         String batchCode) {
        MesProBatchRecordExecutionDO execution = new MesProBatchRecordExecutionDO()
                .setId(id)
                .setExecutionCode(executionCode)
                .setTemplateId(id + 10)
                .setTemplateCode("TPL-" + id)
                .setTemplateName(templateName)
                .setWorkOrderId(id + 20)
                .setWorkOrderCode(workOrderCode)
                .setBatchRecordReportId(reportId)
                .setRouteProcessId(id + 30)
                .setFormSlotType("MAIN")
                .setRecordCategory("BATCH_RECORD")
                .setValidationProfile("CONTROLLED_BATCH")
                .setArchiveVisibility("FINAL_DHR")
                .setBatchCode(batchCode)
                .setStatus(1)
                .setSheetLayoutJson("[]")
                .setCellValuesJson("{}")
                .setCellValuesHash(HASH_64)
                .setFieldAuditRevision(2L)
                .setFieldAuditHeadHash(HASH_64);
        executionMapper.insert(execution);
        return execution;
    }

    private void insertBatchExecutionTask(Long id, Long executionId, Long batchExecutionId,
                                          String reportId, String reportName) {
        batchExecutionTaskMapper.insert(new MesProEdhrBatchExecutionTaskDO()
                .setId(id)
                .setBatchExecutionId(batchExecutionId)
                .setNodeType("FORM")
                .setRouteProcessId(id + 10)
                .setRouteProcessSort(1)
                .setProcessId(id + 20)
                .setProcessCode("PROC-" + id)
                .setProcessName("生产工序")
                .setBatchRecordReportId(reportId)
                .setBatchRecordReportName(reportName)
                .setBatchRecordSort(1)
                .setExecutionMode("SEQUENTIAL")
                .setFormSlotType("MAIN")
                .setRecordCategory("BATCH_RECORD")
                .setValidationProfile("CONTROLLED_BATCH")
                .setRequiredPolicy("REQUIRED")
                .setOwnerRoleKey("PRODUCTION")
                .setArchiveVisibility("FINAL_DHR")
                .setExecutionId(executionId)
                .setStatus(1)
                .setRequiredFlag(Boolean.TRUE));
    }

    private void insertAuditBatch(Long id, Long executionId, Long actorId, String actorName,
                                  LocalDateTime changedAt, int fieldCount) {
        auditBatchMapper.insert(new MesProBatchRecordExecutionFieldAuditBatchDO()
                .setId(id)
                .setExecutionId(executionId)
                .setIdempotencyKey("idem-" + id)
                .setRequestHash(HASH_64)
                .setActionType("FIELD_CHANGE")
                .setReasonCategory("OPERATOR_ENTRY")
                .setReasonText("测试填写")
                .setFieldCount(fieldCount)
                .setActorId(actorId)
                .setActorName(actorName)
                .setSignatureId(id + 5000)
                .setSignatureChallengeHash(HASH_64)
                .setSignatureProjectionHash(HASH_64)
                .setBaseCellValuesHash(HASH_64)
                .setBeforeCellValuesHash(HASH_64)
                .setAfterCellValuesHash(HASH_64)
                .setBaseFieldAuditRevision(0L)
                .setBeforeFieldAuditRevision(0L)
                .setAfterFieldAuditRevision((long) fieldCount)
                .setBaseFieldAuditHeadHash(HASH_64)
                .setPreviousHeadHash(HASH_64)
                .setNewHeadHash(HASH_64)
                .setHashVerificationJson("{\"status\":\"VALID\"}")
                .setChangedAt(changedAt)
                .setTenantId(TENANT_ID));
    }

    private void insertAuditItem(Long id, Long batchId, Long executionId, Long revision, String fieldKey,
                                 String fieldLabel, String oldValue, String newValue) {
        auditItemMapper.insert(new MesProBatchRecordExecutionFieldAuditItemDO()
                .setId(id)
                .setAuditBatchId(batchId)
                .setExecutionId(executionId)
                .setFieldAuditRevision(revision)
                .setBatchItemIndex(revision.intValue())
                .setFieldPath("sheet[0].rows[" + revision + "].cells[1]." + fieldKey)
                .setFieldKey(fieldKey)
                .setFieldLabel(fieldLabel)
                .setRowIndex(revision.intValue())
                .setColumnIndex(1)
                .setComponent("Input")
                .setValueType("STRING")
                .setOldValueJson("\"" + oldValue + "\"")
                .setOldValueDisplay(oldValue)
                .setOldValueHash(HASH_64)
                .setNewValueJson("\"" + newValue + "\"")
                .setNewValueDisplay(newValue)
                .setNewValueHash(HASH_64)
                .setReasonCategory("OPERATOR_ENTRY")
                .setReasonText("测试填写")
                .setActorId(101L)
                .setActorName("测试填写人")
                .setSignatureId(batchId + 5000)
                .setSignatureProjectionHash(HASH_64)
                .setPreviousHash(HASH_64)
                .setAuditHash(HASH_64.substring(0, 60) + String.format("%04d", id % 10000))
                .setBeforeCellValuesHash(HASH_64)
                .setAfterCellValuesHash(HASH_64)
                .setExecutionSnapshotHash(HASH_64)
                .setChangedAt(LocalDateTime.of(2026, 7, 13, 9, 30).plusMinutes(revision))
                .setTenantId(TENANT_ID));
    }

    private void insertNonMatchingAuditData() {
        MesProBatchRecordExecutionDO execution = insertExecution(2999L, "EXEC-OTHER", "RPT-OTHER",
                "其它表单", "WO-OTHER", "BATCH-OTHER");
        insertAuditBatch(1999L, execution.getId(), 999L, "其它填写人",
                LocalDateTime.of(2026, 7, 12, 9, 0), 1);
        insertAuditItem(4999L, 1999L, execution.getId(), 1L, "other", "其它", "0", "1");
    }
}
