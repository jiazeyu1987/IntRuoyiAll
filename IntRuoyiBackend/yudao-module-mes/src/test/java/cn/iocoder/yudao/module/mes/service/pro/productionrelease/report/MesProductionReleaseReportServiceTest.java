package cn.iocoder.yudao.module.mes.service.pro.productionrelease.report;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskStatus;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowAuditRecorder;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerException;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerType;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowStatus;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowIdempotency;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrSpecialNodeAttachment;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProductionReleaseReportServiceTest {

    private static final Long TENANT_ID = 1L;
    private static final Long ACTOR_ID = 7101L;
    private static final Long APPLICATION_ID = 7001L;
    private static final Long BATCH_EXECUTION_ID = 9001L;
    private static final Long BATCH_TASK_ID = 9101L;
    private static final Long WORK_TASK_ID = 9201L;
    private static final int VERSION = 2;

    @Mock private MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper;
    @Mock private MesProEdhrWorkTaskMapper workTaskMapper;
    @Mock private MesProEdhrBatchExecutionTaskMapper batchTaskMapper;
    @Mock private MesProductionReleaseReportNodePort reportNodePort;
    @Mock private MesProductionReleaseManagerStageInitializer managerStageInitializer;
    @Mock private MesReleaseFlowAuditRecorder auditRecorder;

    private MesProductionReleaseReportService service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        service = new MesProductionReleaseReportServiceImpl(
                applicationMapper, workTaskMapper, batchTaskMapper, reportNodePort,
                managerStageInitializer, auditRecorder,
                Clock.fixed(Instant.parse("2026-08-16T00:00:00Z"), ZoneOffset.UTC));
        when(workTaskMapper.selectReleaseReportByBatchTaskId(BATCH_TASK_ID)).thenReturn(workTask());
        when(applicationMapper.selectByBatchExecutionIdForUpdate(BATCH_EXECUTION_ID)).thenReturn(application());
        when(batchTaskMapper.selectByIdForUpdate(BATCH_TASK_ID)).thenReturn(batchTask(
                BATCH_TASK_ID, "INCOMING_INSPECTION_REPORT", 0, null));
        lenient().when(batchTaskMapper.updateReleaseReportCompletionPayload(eq(BATCH_TASK_ID), anyString()))
                .thenReturn(1);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void completingOneOfFirstThreeReportsKeepsUploadStageAndAdvancesVersion() {
        when(reportNodePort.complete(any())).thenReturn(currentEvidence());
        when(workTaskMapper.completeReleaseReportTask(eq(WORK_TASK_ID), any())).thenReturn(1);
        when(batchTaskMapper.selectListByBatchExecutionId(BATCH_EXECUTION_ID)).thenReturn(List.of(
                batchTask(BATCH_TASK_ID, "INCOMING_INSPECTION_REPORT", 40, null),
                batchTask(9102L, "STERILIZATION_REPORT", 0, null),
                batchTask(9103L, "FINISHED_PRODUCT_INSPECTION_REPORT", 0, null),
                batchTask(9104L, "FINISHED_PRODUCT_INSPECTION_RECORD", 0, null)));
        when(applicationMapper.advanceReportVersion(APPLICATION_ID, VERSION)).thenReturn(1);

        MesProductionReleaseReportNodeCompleteResult result = service.complete(ACTOR_ID, command());

        assertEquals(MesReleaseFlowStatus.REPORT_UPLOAD_PENDING, result.getReportUploadStatus());
        assertEquals(VERSION + 1, result.getVersion());
        assertEquals(WORK_TASK_ID, result.getWorkTaskId());
        verify(managerStageInitializer, never()).initializeManagerReleaseStage(any());
        verify(applicationMapper, never()).handoffReportsToManager(any(), any(), any(), any(), any(), any());
    }

    @Test
    void prepareAttachmentReturnsCurrentVersionWithoutAdvancingApplication() {
        when(reportNodePort.prepareAttachment(any()))
                .thenReturn(new MesProductionReleaseReportAttachmentPrepareResult()
                        .setFileId(101L)
                        .setFileName("incoming.pdf")
                        .setContentType("application/pdf")
                        .setFileSize(3L)
                        .setSha256("a".repeat(64))
                        .setStorageRetentionHash("retention-hash"));

        MesProductionReleaseReportAttachmentPrepareResult result = service.prepareAttachment(
                ACTOR_ID, new MesProductionReleaseReportAttachmentPrepareCommand()
                        .setBatchTaskId(BATCH_TASK_ID)
                        .setExpectedVersion(VERSION)
                        .setIdempotencyKey("report-prepare-9101")
                        .setFileName("incoming.pdf")
                        .setContentType("application/pdf")
                        .setContent(new byte[]{1, 2, 3}));

        assertEquals(VERSION, result.getVersion());
        verify(applicationMapper, never()).advanceReportVersion(any(), any());
        verify(applicationMapper, never()).handoffReportsToManager(any(), any(), any(), any(), any(), any());
    }

    @Test
    void sameKeyAndPayloadReplayReturnsStoredReceiptWithoutDuplicateWrites() {
        MesProductionReleaseReportNodeCompleteResult stored = new MesProductionReleaseReportNodeCompleteResult()
                .setBatchExecutionId(BATCH_EXECUTION_ID)
                .setBatchTaskId(BATCH_TASK_ID)
                .setWorkTaskId(WORK_TASK_ID)
                .setNodeType("INCOMING_INSPECTION_REPORT")
                .setNodeStatus("COMPLETED")
                .setActiveAttachmentVersion(1)
                .setAttachmentIds(List.of(101L))
                .setAttachmentHashes(List.of("a".repeat(64)))
                .setReportUploadStatus(MesReleaseFlowStatus.REPORT_UPLOAD_PENDING)
                .setVersion(VERSION + 1);
        when(batchTaskMapper.selectByIdForUpdate(BATCH_TASK_ID)).thenReturn(batchTask(
                BATCH_TASK_ID, "INCOMING_INSPECTION_REPORT", 40, storedPayload(stored)));

        MesProductionReleaseReportNodeCompleteResult replay = service.complete(ACTOR_ID, command());

        assertEquals(VERSION + 1, replay.getVersion());
        verify(reportNodePort, never()).complete(any());
        verify(workTaskMapper, never()).completeReleaseReportTask(any(), any());
    }

    @Test
    void sameKeyWithDifferentPayloadReturnsConflict() {
        MesProductionReleaseReportNodeCompleteResult stored = new MesProductionReleaseReportNodeCompleteResult()
                .setBatchExecutionId(BATCH_EXECUTION_ID)
                .setBatchTaskId(BATCH_TASK_ID)
                .setWorkTaskId(WORK_TASK_ID)
                .setNodeType("INCOMING_INSPECTION_REPORT")
                .setNodeStatus("COMPLETED")
                .setReportUploadStatus(MesReleaseFlowStatus.REPORT_UPLOAD_PENDING)
                .setVersion(VERSION + 1);
        when(batchTaskMapper.selectByIdForUpdate(BATCH_TASK_ID)).thenReturn(batchTask(
                BATCH_TASK_ID, "INCOMING_INSPECTION_REPORT", 40, storedPayload(stored)));
        MesProductionReleaseReportNodeCompleteCommand changed = command()
                .setAttachments(List.of(new MesProEdhrSpecialNodeAttachment()
                        .setFileId(102L).setSha256("b".repeat(64))));

        MesReleaseFlowBlockerException failure = assertThrows(
                MesReleaseFlowBlockerException.class,
                () -> service.complete(ACTOR_ID, changed));

        assertEquals(MesReleaseFlowBlockerType.IDEMPOTENCY_PAYLOAD_CONFLICT,
                failure.getFailure().getBlockers().get(0).getBlockerType());
        verify(reportNodePort, never()).complete(any());
    }

    @Test
    void fourthReportAtomicallyInitializesManagerStage() {
        when(reportNodePort.complete(any())).thenReturn(currentEvidence());
        when(workTaskMapper.completeReleaseReportTask(eq(WORK_TASK_ID), any())).thenReturn(1);
        when(batchTaskMapper.selectListByBatchExecutionId(BATCH_EXECUTION_ID)).thenReturn(completedFourTasks());
        when(managerStageInitializer.initializeManagerReleaseStage(any()))
                .thenReturn(new MesProductionReleaseManagerStageInitializationResult()
                        .setReleaseTransactionId(9301L)
                        .setManagerReleaseWorkTaskId(9401L)
                        .setManagerCandidateSnapshotHash("manager-candidates-hash"));
        when(applicationMapper.handoffReportsToManager(
                eq(APPLICATION_ID), eq(VERSION), anyString(), eq(9301L), eq(9401L),
                eq("manager-candidates-hash")))
                .thenReturn(1);

        MesProductionReleaseReportNodeCompleteResult result = service.complete(ACTOR_ID, command());

        assertEquals(MesReleaseFlowStatus.MANAGER_RELEASE_PENDING, result.getReportUploadStatus());
        assertEquals(9301L, result.getReleaseTransactionId());
        assertEquals(9401L, result.getManagerReleaseWorkTaskId());
        assertEquals(VERSION + 1, result.getVersion());
    }

    @Test
    void managerStageFailureDoesNotAdvanceApplication() {
        when(reportNodePort.complete(any())).thenReturn(currentEvidence());
        when(workTaskMapper.completeReleaseReportTask(eq(WORK_TASK_ID), any())).thenReturn(1);
        when(batchTaskMapper.selectListByBatchExecutionId(BATCH_EXECUTION_ID)).thenReturn(completedFourTasks());
        when(managerStageInitializer.initializeManagerReleaseStage(any()))
                .thenThrow(new IllegalStateException("manager stage persistence failed"));

        assertThrows(IllegalStateException.class, () -> service.complete(ACTOR_ID, command()));

        verify(applicationMapper, never()).advanceReportVersion(any(), any());
        verify(applicationMapper, never()).handoffReportsToManager(any(), any(), any(), any(), any(), any());
    }

    @Test
    void userOutsideFrozenCandidateCannotCompleteReport() {
        MesReleaseFlowBlockerException failure = assertThrows(
                MesReleaseFlowBlockerException.class,
                () -> service.complete(7999L, command()));

        assertEquals(MesReleaseFlowBlockerType.WORK_TASK_NOT_PROCESSABLE,
                failure.getFailure().getBlockers().get(0).getBlockerType());
        verify(reportNodePort, never()).complete(any());
    }

    @Test
    void staleVersionFailsBeforeAttachmentWrite() {
        MesProductionReleaseReportNodeCompleteCommand stale = command().setExpectedVersion(VERSION - 1);

        MesReleaseFlowBlockerException failure = assertThrows(
                MesReleaseFlowBlockerException.class,
                () -> service.complete(ACTOR_ID, stale));

        assertEquals(MesReleaseFlowBlockerType.STATE_VERSION_CONFLICT,
                failure.getFailure().getBlockers().get(0).getBlockerType());
        verify(reportNodePort, never()).complete(any());
    }

    private MesProductionReleaseReportNodeCompleteCommand command() {
        return new MesProductionReleaseReportNodeCompleteCommand()
                .setBatchTaskId(BATCH_TASK_ID)
                .setExpectedVersion(VERSION)
                .setIdempotencyKey("report-complete-9101")
                .setAttachments(List.of(new MesProEdhrSpecialNodeAttachment()
                        .setFileId(101L)
                        .setSha256("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")));
    }

    private MesProcessPoolActiveOrderReleaseApplicationDO application() {
        return new MesProcessPoolActiveOrderReleaseApplicationDO()
                .setId(APPLICATION_ID)
                .setBatchExecutionId(BATCH_EXECUTION_ID)
                .setApplicationStatus(MesReleaseFlowStatus.REPORT_UPLOAD_PENDING)
                .setVersion(VERSION);
    }

    private MesProEdhrWorkTaskDO workTask() {
        return new MesProEdhrWorkTaskDO()
                .setId(WORK_TASK_ID)
                .setTaskType("FILL")
                .setBatchExecutionId(BATCH_EXECUTION_ID)
                .setBatchTaskId(BATCH_TASK_ID)
                .setBusinessScopeType("RELEASE_REPORT_NODE")
                .setBusinessScopeId(BATCH_TASK_ID)
                .setCandidateUserSnapshot(String.valueOf(ACTOR_ID))
                .setStatus(MesProEdhrWorkTaskStatus.TODO);
    }

    private MesProductionReleaseReportNodeEvidence currentEvidence() {
        return new MesProductionReleaseReportNodeEvidence()
                .setBatchExecutionId(BATCH_EXECUTION_ID)
                .setBatchTaskId(BATCH_TASK_ID)
                .setNodeType("INCOMING_INSPECTION_REPORT")
                .setActiveAttachmentVersion(1)
                .setAttachmentIds(List.of(101L))
                .setAttachmentHashes(List.of(
                        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"));
    }

    private String storedPayload(MesProductionReleaseReportNodeCompleteResult stored) {
        JSONObject payload = JSON.parseObject(currentEvidence().toPayloadJson());
        payload.put("releaseReportIdempotencyKey", "report-complete-9101");
        payload.put("releaseReportPayloadHash", MesReleaseFlowIdempotency.payloadHash(
                String.valueOf(BATCH_TASK_ID), "INCOMING_INSPECTION_REPORT", null,
                "101:" + "a".repeat(64)));
        payload.put("releaseReportReceipt", JSON.toJSON(stored));
        return payload.toJSONString();
    }

    private List<MesProEdhrBatchExecutionTaskDO> completedFourTasks() {
        return List.of(
                batchTask(BATCH_TASK_ID, "INCOMING_INSPECTION_REPORT", 40, currentEvidence().toPayloadJson()),
                batchTask(9102L, "STERILIZATION_REPORT", 40,
                        evidence(9102L, "STERILIZATION_REPORT", 102L, 'b').toPayloadJson()),
                batchTask(9103L, "FINISHED_PRODUCT_INSPECTION_REPORT", 40,
                        evidence(9103L, "FINISHED_PRODUCT_INSPECTION_REPORT", 103L, 'c').toPayloadJson()),
                batchTask(9104L, "FINISHED_PRODUCT_INSPECTION_RECORD", 40,
                        evidence(9104L, "FINISHED_PRODUCT_INSPECTION_RECORD", 104L, 'd').toPayloadJson()));
    }

    private MesProductionReleaseReportNodeEvidence evidence(
            Long batchTaskId, String nodeType, Long attachmentId, char hashCharacter) {
        return new MesProductionReleaseReportNodeEvidence()
                .setBatchExecutionId(BATCH_EXECUTION_ID)
                .setBatchTaskId(batchTaskId)
                .setNodeType(nodeType)
                .setSterilizationBatchNo("STERILIZATION_REPORT".equals(nodeType) ? "STER-BATCH-001" : null)
                .setActiveAttachmentVersion(1)
                .setAttachmentIds(List.of(attachmentId))
                .setAttachmentHashes(List.of(String.valueOf(hashCharacter).repeat(64)));
    }

    private MesProEdhrBatchExecutionTaskDO batchTask(
            Long id, String nodeType, Integer status, String payload) {
        return new MesProEdhrBatchExecutionTaskDO()
                .setId(id)
                .setBatchExecutionId(BATCH_EXECUTION_ID)
                .setNodeType(nodeType)
                .setProcessName(nodeType)
                .setStatus(status)
                .setSpecialPayloadJson(payload);
    }
}
