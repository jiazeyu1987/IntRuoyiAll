package cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager;

import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionAttachmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionSignatureMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesOrderReleaseCompletenessCheck;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesOrderReleaseCompletenessService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrApprovalStatusMapping;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionServiceImpl;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProductionReleaseBusinessReadinessServiceTest {

    @Mock private MesProEdhrBatchExecutionTaskMapper batchExecutionTaskMapper;
    @Mock private MesProBatchRecordExecutionMapper executionMapper;
    @Mock private MesProBatchRecordExecutionSignatureMapper executionSignatureMapper;
    @Mock private MesProBatchRecordExecutionAttachmentMapper attachmentMapper;
    @Mock private FormActionInstanceMapper formActionInstanceMapper;
    @Mock private MesOrderReleaseCompletenessService releaseCompletenessService;

    private MesProductionReleaseBusinessReadinessService service;

    @BeforeEach
    void setUp() {
        service = new MesProductionReleaseBusinessReadinessService(
                batchExecutionTaskMapper,
                executionMapper,
                executionSignatureMapper,
                attachmentMapper,
                formActionInstanceMapper,
                releaseCompletenessService);
    }

    @Test
    void resolvesFormalBusinessChecksWithoutDefaultPassAndKeepsCountsAligned() {
        MesProEdhrBatchExecutionDO batch = batch();
        when(batchExecutionTaskMapper.selectListByBatchExecutionId(901L)).thenReturn(List.of());
        when(releaseCompletenessService.evaluateInspectionResult(batch))
                .thenReturn(check(MesProEdhrReleaseServiceImpl.CHECK_INSPECTION_RESULT,
                        MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS, "INFO"));
        when(releaseCompletenessService.evaluateDeviationClosed(batch))
                .thenReturn(check(MesProEdhrReleaseServiceImpl.CHECK_DEVIATION_CLOSED,
                        MesProEdhrReleaseServiceImpl.CHECK_RESULT_NOT_APPLICABLE, "INFO"));
        when(releaseCompletenessService.evaluateReworkClosed(batch))
                .thenReturn(check(MesProEdhrReleaseServiceImpl.CHECK_REWORK_CLOSED,
                        MesProEdhrReleaseServiceImpl.CHECK_RESULT_BLOCKER, "BLOCKER"));
        when(releaseCompletenessService.evaluateScrapRecorded(batch))
                .thenReturn(check(MesProEdhrReleaseServiceImpl.CHECK_SCRAP_RECORDED,
                        MesProEdhrReleaseServiceImpl.CHECK_RESULT_NOT_APPLICABLE, "INFO"));
        when(releaseCompletenessService.evaluateInventoryConsistency(batch))
                .thenReturn(check(MesProEdhrReleaseServiceImpl.CHECK_INVENTORY_CONSISTENCY,
                        MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS, "INFO"));

        MesProductionReleaseBusinessReadiness result = service.resolveBusinessReadinessChecks(batch);

        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_FAIL, result.dhrStatus());
        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS, result.inspectionStatus());
        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_NOT_APPLICABLE, result.deviationStatus());
        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_BLOCKER, result.reworkStatus());
        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_NOT_APPLICABLE, result.scrapStatus());
        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS, result.inventoryStatus());
        assertEquals(6, result.requiredCheckCount());
        assertEquals(2, result.failedCheckCount());
        assertEquals(2, result.blockingCheckCount());
        assertTrue(result.hasBlockingChecks());
        assertTrue(result.snapshotJson().contains(MesProEdhrReleaseServiceImpl.CHECK_REWORK_CLOSED));
        assertTrue(result.snapshotJson().contains(MesProEdhrReleaseServiceImpl.CHECK_RESULT_NOT_APPLICABLE));
        verify(releaseCompletenessService).evaluateInspectionResult(batch);
        verify(releaseCompletenessService).evaluateDeviationClosed(batch);
        verify(releaseCompletenessService).evaluateReworkClosed(batch);
        verify(releaseCompletenessService).evaluateScrapRecorded(batch);
        verify(releaseCompletenessService).evaluateInventoryConsistency(batch);
    }

    @Test
    void treatsPrecheckRequiredAsBlockingUnverifiedBusinessCheck() {
        MesProEdhrBatchExecutionDO batch = batch();
        stubDhrApproved(batch);
        when(releaseCompletenessService.evaluateInspectionResult(batch))
                .thenReturn(check(MesProEdhrReleaseServiceImpl.CHECK_INSPECTION_RESULT,
                        MesProEdhrReleaseServiceImpl.STATUS_PRECHECK_REQUIRED, "INFO"));
        when(releaseCompletenessService.evaluateDeviationClosed(batch))
                .thenReturn(check(MesProEdhrReleaseServiceImpl.CHECK_DEVIATION_CLOSED,
                        MesProEdhrReleaseServiceImpl.CHECK_RESULT_NOT_APPLICABLE, "INFO"));
        when(releaseCompletenessService.evaluateReworkClosed(batch))
                .thenReturn(check(MesProEdhrReleaseServiceImpl.CHECK_REWORK_CLOSED,
                        MesProEdhrReleaseServiceImpl.CHECK_RESULT_NOT_APPLICABLE, "INFO"));
        when(releaseCompletenessService.evaluateScrapRecorded(batch))
                .thenReturn(check(MesProEdhrReleaseServiceImpl.CHECK_SCRAP_RECORDED,
                        MesProEdhrReleaseServiceImpl.CHECK_RESULT_NOT_APPLICABLE, "INFO"));
        when(releaseCompletenessService.evaluateInventoryConsistency(batch))
                .thenReturn(check(MesProEdhrReleaseServiceImpl.CHECK_INVENTORY_CONSISTENCY,
                        MesProEdhrReleaseServiceImpl.CHECK_RESULT_NOT_APPLICABLE, "INFO"));

        MesProductionReleaseBusinessReadiness result = service.resolveBusinessReadinessChecks(batch);

        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS, result.dhrStatus());
        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_PRECHECK_REQUIRED, result.inspectionStatus());
        assertEquals(6, result.requiredCheckCount());
        assertEquals(1, result.failedCheckCount());
        assertEquals(1, result.blockingCheckCount());
        assertTrue(result.hasBlockingChecks());
    }

    @Test
    void rejectsUnknownFormalResultInsteadOfTreatingItAsPass() {
        MesProEdhrBatchExecutionDO batch = batch();
        when(batchExecutionTaskMapper.selectListByBatchExecutionId(901L)).thenReturn(List.of());
        when(releaseCompletenessService.evaluateInspectionResult(batch))
                .thenReturn(check(MesProEdhrReleaseServiceImpl.CHECK_INSPECTION_RESULT,
                        "UNKNOWN", "INFO"));
        when(releaseCompletenessService.evaluateDeviationClosed(batch))
                .thenReturn(check(MesProEdhrReleaseServiceImpl.CHECK_DEVIATION_CLOSED,
                        MesProEdhrReleaseServiceImpl.CHECK_RESULT_NOT_APPLICABLE, "INFO"));
        when(releaseCompletenessService.evaluateReworkClosed(batch))
                .thenReturn(check(MesProEdhrReleaseServiceImpl.CHECK_REWORK_CLOSED,
                        MesProEdhrReleaseServiceImpl.CHECK_RESULT_NOT_APPLICABLE, "INFO"));
        when(releaseCompletenessService.evaluateScrapRecorded(batch))
                .thenReturn(check(MesProEdhrReleaseServiceImpl.CHECK_SCRAP_RECORDED,
                        MesProEdhrReleaseServiceImpl.CHECK_RESULT_NOT_APPLICABLE, "INFO"));
        when(releaseCompletenessService.evaluateInventoryConsistency(batch))
                .thenReturn(check(MesProEdhrReleaseServiceImpl.CHECK_INVENTORY_CONSISTENCY,
                        MesProEdhrReleaseServiceImpl.CHECK_RESULT_NOT_APPLICABLE, "INFO"));

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> service.resolveBusinessReadinessChecks(batch));

        assertTrue(failure.getMessage().contains("unknown business readiness check result"));
    }

    private void stubDhrApproved(MesProEdhrBatchExecutionDO batch) {
        when(batchExecutionTaskMapper.selectListByBatchExecutionId(batch.getId()))
                .thenReturn(List.of(new MesProEdhrBatchExecutionTaskDO()
                        .setId(7001L)
                        .setBatchExecutionId(batch.getId())
                        .setNodeType(MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_ROUTE_FORM)
                        .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED)
                        .setRequiredFlag(true)
                        .setExecutionId(8001L)));
        when(executionMapper.selectById(8001L)).thenReturn(new MesProBatchRecordExecutionDO()
                .setId(8001L)
                .setStatus(MesProEdhrApprovalStatusMapping.EXECUTION_STATUS_FILL_COMPLETED)
                .setSubmittedAt(LocalDateTime.of(2026, 9, 13, 9, 0))
                .setClosedAt(LocalDateTime.of(2026, 9, 13, 10, 0))
                .setCellValuesHash("cell-values-hash")
                .setFieldAuditRevision(3L)
                .setFieldAuditHeadHash("field-audit-head-hash"));
        when(executionSignatureMapper.selectCount(any())).thenReturn(1L);
    }

    private static MesProEdhrBatchExecutionDO batch() {
        return new MesProEdhrBatchExecutionDO()
                .setId(901L)
                .setBatchExecutionCode("BE-901")
                .setWorkOrderId(301L)
                .setWorkOrderCode("WO-001")
                .setBatchCode("BATCH-001")
                .setProductCode("P-001");
    }

    private static MesOrderReleaseCompletenessCheck check(String code, String result, String severity) {
        return new MesOrderReleaseCompletenessCheck(
                code,
                code + " name",
                "BUSINESS",
                result,
                severity,
                "MES",
                "EDHR_BATCH_EXECUTION",
                "901",
                "BE-901",
                code + " reason",
                code + " suggestion");
    }
}
