package cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.dal.dataobject.signature.BpmApprovalSignatureRecordDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.signature.BpmApprovalSignatureRecordMapper;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseApproveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskStatus;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowAuditRecorder;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerException;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerType;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowStatus;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowIdempotency;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseServiceImpl;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.report.MesProductionReleaseReportNodeEvidence;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.report.MesProductionReleaseReportSnapshots;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.MesProductionReleaseRequiredCandidateResolver;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.MesProductionReleaseRoleCandidates;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.MesProductionReleaseRoleCodes;
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
import com.alibaba.fastjson.JSONObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProductionReleaseManagerApprovalServiceTest {

    private static final Long ACTOR_USER_ID = 8101L;
    private static final String SIGNATURE_URL = "storage://signature/manager-8101.png";

    @Mock private MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper;
    @Mock private MesProEdhrReleaseTransactionMapper releaseTransactionMapper;
    @Mock private MesProEdhrReleaseTransactionEventMapper releaseEventMapper;
    @Mock private MesProEdhrWorkTaskMapper workTaskMapper;
    @Mock private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Mock private MesProEdhrBatchExecutionTaskMapper batchTaskMapper;
    @Mock private MesProductionReleaseRequiredCandidateResolver candidateResolver;
    @Mock private BpmApprovalSignatureRecordMapper approvalSignatureRecordMapper;
    @Mock private MesReleaseFlowAuditRecorder auditRecorder;

    private MesProductionReleaseManagerApprovalServiceImpl service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(1L);
        service = new MesProductionReleaseManagerApprovalServiceImpl(
                applicationMapper, releaseTransactionMapper, releaseEventMapper, workTaskMapper,
                batchExecutionMapper, batchTaskMapper, candidateResolver, approvalSignatureRecordMapper,
                auditRecorder, Clock.fixed(Instant.parse("2026-08-16T00:00:00Z"), ZoneOffset.UTC));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void approveRecomputesFrozenReportsAndAtomicallyReleasesApplicationAndTransaction() {
        Fixture fixture = fixture();
        stubApproval(fixture);
        when(applicationMapper.releaseFromManager(
                701L, 5, fixture.reportSnapshotHash(), 1001L, 2001L)).thenReturn(1);
        when(workTaskMapper.completeManagerReleaseTask(2001L, fixture.now(), "approved")).thenReturn(1);
        MesProductionReleaseManagerApprovalResult prepared = service.prepareForFinalization(
                ACTOR_USER_ID, fixture.command());
        MesProductionReleaseManagerApprovalResult result = service.completeAfterFinalization(
                ACTOR_USER_ID, fixture.command(), prepared, fixture.releasedTransaction());

        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_RELEASED,
                result.getReleaseTransaction().getReleaseStatus());
        assertEquals(MesReleaseFlowStatus.RELEASED, result.getApplicationStatus());
        verify(releaseTransactionMapper, never()).approveProductionRelease(
                any(), any(), any(), any(), any(), any(), any());
        verify(applicationMapper).releaseFromManager(
                701L, 5, fixture.reportSnapshotHash(), 1001L, 2001L);
        verify(workTaskMapper).completeManagerReleaseTask(2001L, fixture.now(), "approved");
        verify(auditRecorder).record(any());
    }

    @Test
    void changedReportSnapshotBlocksBeforeAnyReleaseWrite() {
        Fixture fixture = fixture();
        stubApproval(fixture);
        when(applicationMapper.selectByReleaseTransactionIdForUpdate(1001L))
                .thenReturn(fixture.application().setReportSnapshotHash("changed-snapshot"));

        MesReleaseFlowBlockerException failure = assertThrows(
                MesReleaseFlowBlockerException.class,
                () -> service.prepareForFinalization(ACTOR_USER_ID, fixture.command()));

        assertEquals(MesReleaseFlowBlockerType.REPORT_SNAPSHOT_CHANGED,
                failure.getFailure().getBlockers().get(0).getBlockerType());
        verify(releaseTransactionMapper, never()).approveProductionRelease(
                any(), any(), any(), any(), any(), any(), any());
        verify(applicationMapper, never()).releaseFromManager(any(), any(), any(), any(), any());
    }

    @Test
    void staleReleaseTransactionVersionBlocksBeforeTaskOrReleaseWrite() {
        Fixture fixture = fixture();
        fixture.command().setExpectedVersion(2);
        when(applicationMapper.selectByReleaseTransactionIdForUpdate(1001L)).thenReturn(fixture.application());
        when(releaseTransactionMapper.selectByIdForUpdate(1001L)).thenReturn(fixture.transaction());

        MesReleaseFlowBlockerException failure = assertThrows(
                MesReleaseFlowBlockerException.class,
                () -> service.prepareForFinalization(ACTOR_USER_ID, fixture.command()));

        assertEquals(MesReleaseFlowBlockerType.STATE_VERSION_CONFLICT,
                failure.getFailure().getBlockers().get(0).getBlockerType());
        verify(workTaskMapper, never()).selectByIdForUpdate(any());
        verify(releaseTransactionMapper, never()).approveProductionRelease(
                any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void currentRoleMembershipAndFrozenTaskCandidateAreBothRequired() {
        Fixture fixture = fixture();
        when(applicationMapper.selectByReleaseTransactionIdForUpdate(1001L)).thenReturn(fixture.application());
        when(releaseTransactionMapper.selectByIdForUpdate(1001L)).thenReturn(fixture.transaction());
        when(workTaskMapper.selectByIdForUpdate(2001L)).thenReturn(fixture.workTask());
        when(candidateResolver.resolveRequiredCandidates(1L,
                MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE))
                .thenReturn(new MesProductionReleaseRoleCandidates(
                        77L, MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE,
                        List.of(9999L), "changed-manager-candidate-hash"));

        MesReleaseFlowBlockerException failure = assertThrows(
                MesReleaseFlowBlockerException.class,
                () -> service.prepareForFinalization(ACTOR_USER_ID, fixture.command()));

        assertEquals(MesReleaseFlowBlockerType.WORK_TASK_NOT_PROCESSABLE,
                failure.getFailure().getBlockers().get(0).getBlockerType());
        verify(releaseTransactionMapper, never()).approveProductionRelease(
                any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void sameKeySamePayloadReplaysReleasedReceiptWithoutWrites() {
        Fixture fixture = fixture();
        fixture.application().setApplicationStatus(MesReleaseFlowStatus.RELEASED).setVersion(6);
        when(applicationMapper.selectByReleaseTransactionIdForUpdate(1001L)).thenReturn(fixture.application());
        when(releaseEventMapper.selectByReleaseTransactionIdAndEventTypeAndIdempotencyKey(
                1001L, "APPROVE", fixture.command().getIdempotencyKey()))
                .thenReturn(replayEvent(fixture));
        when(releaseTransactionMapper.selectById(1001L)).thenReturn(fixture.releasedTransaction());
        when(batchExecutionMapper.selectById(901L)).thenReturn(fixture.batch());

        MesProductionReleaseManagerApprovalResult result = service.prepareForFinalization(
                ACTOR_USER_ID, fixture.command());

        org.junit.jupiter.api.Assertions.assertTrue(result.isReplayed());
        verify(releaseTransactionMapper, never()).approveProductionRelease(
                any(), any(), any(), any(), any(), any(), any());
        verify(applicationMapper, never()).releaseFromManager(any(), any(), any(), any(), any());
    }

    @Test
    void sameKeyDifferentPayloadIsRejected() {
        Fixture fixture = fixture();
        fixture.application().setApplicationStatus(MesReleaseFlowStatus.RELEASED).setVersion(6);
        MesProEdhrReleaseTransactionEventDO event = replayEvent(fixture);
        fixture.command().setApprovalOpinion("changed opinion");
        when(applicationMapper.selectByReleaseTransactionIdForUpdate(1001L)).thenReturn(fixture.application());
        when(releaseEventMapper.selectByReleaseTransactionIdAndEventTypeAndIdempotencyKey(
                1001L, "APPROVE", fixture.command().getIdempotencyKey())).thenReturn(event);

        MesReleaseFlowBlockerException failure = assertThrows(
                MesReleaseFlowBlockerException.class,
                () -> service.prepareForFinalization(ACTOR_USER_ID, fixture.command()));

        assertEquals(MesReleaseFlowBlockerType.IDEMPOTENCY_PAYLOAD_CONFLICT,
                failure.getFailure().getBlockers().get(0).getBlockerType());
        verify(releaseTransactionMapper, never()).approveProductionRelease(
                any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void targetReleaseRejectAndWithdrawAreExplicitlyUnsupported() {
        when(applicationMapper.selectListByReleaseTransactionId(1001L)).thenReturn(List.of(fixture().application()));

        MesReleaseFlowBlockerException reject = assertThrows(MesReleaseFlowBlockerException.class,
                () -> service.assertActionSupported(1001L, "REJECT"));
        MesReleaseFlowBlockerException withdraw = assertThrows(MesReleaseFlowBlockerException.class,
                () -> service.assertActionSupported(1001L, "WITHDRAW"));

        assertEquals(MesReleaseFlowBlockerType.UNSUPPORTED_RELEASE_ACTION,
                reject.getFailure().getBlockers().get(0).getBlockerType());
        assertEquals(MesReleaseFlowBlockerType.UNSUPPORTED_RELEASE_ACTION,
                withdraw.getFailure().getBlockers().get(0).getBlockerType());
    }

    private void stubApproval(Fixture fixture) {
        when(applicationMapper.selectByReleaseTransactionIdForUpdate(1001L)).thenReturn(fixture.application());
        when(releaseTransactionMapper.selectByIdForUpdate(1001L)).thenReturn(fixture.transaction());
        when(workTaskMapper.selectByIdForUpdate(2001L)).thenReturn(fixture.workTask());
        when(batchExecutionMapper.selectById(901L)).thenReturn(fixture.batch());
        when(batchTaskMapper.selectListByBatchExecutionId(901L)).thenReturn(fixture.batchTasks());
        when(candidateResolver.resolveRequiredCandidates(1L,
                MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE))
                .thenReturn(new MesProductionReleaseRoleCandidates(
                        77L, MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE,
                        List.of(ACTOR_USER_ID), "manager-candidate-hash"));
        lenient().when(approvalSignatureRecordMapper.selectList(any())).thenReturn(List.of(
                new BpmApprovalSignatureRecordDO()
                        .setPasswordVerified(true)
                        .setSignatureImageFileUrl(SIGNATURE_URL)));
    }

    private Fixture fixture() {
        List<MesProductionReleaseReportNodeEvidence> evidences = List.of(
                evidence(911L, "FINISHED_PRODUCT_INSPECTION_RECORD", null, 1011L, '1'),
                evidence(912L, "FINISHED_PRODUCT_INSPECTION_REPORT", null, 1012L, '2'),
                evidence(913L, "INCOMING_INSPECTION_REPORT", null, 1013L, '3'),
                evidence(914L, "STERILIZATION_REPORT", "STER-001", 1014L, '4'));
        MesProcessPoolActiveOrderReleaseApplicationDO application = new MesProcessPoolActiveOrderReleaseApplicationDO()
                .setId(701L)
                .setBatchExecutionId(901L)
                .setReleaseTransactionId(1001L)
                .setReleaseApprovalWorkTaskId(2001L)
                .setApplicationStatus(MesReleaseFlowStatus.MANAGER_RELEASE_PENDING)
                .setVersion(5);
        String reportSnapshotHash = MesProductionReleaseReportSnapshots.hash(application, evidences);
        application.setReportSnapshotHash(reportSnapshotHash);
        List<MesProEdhrBatchExecutionTaskDO> batchTasks = evidences.stream()
                .map(evidence -> new MesProEdhrBatchExecutionTaskDO()
                        .setId(evidence.getBatchTaskId())
                        .setBatchExecutionId(901L)
                        .setNodeType(evidence.getNodeType())
                        .setStatus(40)
                        .setSpecialPayloadJson(evidence.toPayloadJson()))
                .toList();
        MesProEdhrReleaseApproveReqVO command = new MesProEdhrReleaseApproveReqVO()
                .setReleaseTransactionId(1001L)
                .setWorkTaskId(2001L)
                .setExpectedVersion(3)
                .setIdempotencyKey("manager-release-001")
                .setSignoffEvidenceHash(DigestUtil.sha256Hex(SIGNATURE_URL))
                .setApprovalOpinion("approved");
        return new Fixture(application, reportSnapshotHash, batchTasks, command);
    }

    private MesProEdhrReleaseTransactionEventDO replayEvent(Fixture fixture) {
        String payloadHash = MesReleaseFlowIdempotency.payloadHash(
                String.valueOf(fixture.command().getReleaseTransactionId()),
                String.valueOf(fixture.command().getWorkTaskId()),
                String.valueOf(fixture.command().getExpectedVersion()),
                String.valueOf(ACTOR_USER_ID),
                fixture.command().getSignoffEvidenceHash(),
                fixture.command().getApprovalOpinion(),
                fixture.reportSnapshotHash());
        JSONObject snapshot = new JSONObject(true);
        snapshot.put("reportSnapshotHash", fixture.reportSnapshotHash());
        snapshot.put("managerApprovalPayloadHash", payloadHash);
        return new MesProEdhrReleaseTransactionEventDO()
                .setReleaseTransactionId(1001L)
                .setEventType("APPROVE")
                .setIdempotencyKey(fixture.command().getIdempotencyKey())
                .setEventSnapshotJson(snapshot.toJSONString());
    }

    private MesProductionReleaseReportNodeEvidence evidence(
            Long batchTaskId, String nodeType, String sterilizationBatchNo, Long attachmentId, char hashDigit) {
        return new MesProductionReleaseReportNodeEvidence()
                .setBatchExecutionId(901L)
                .setBatchTaskId(batchTaskId)
                .setNodeType(nodeType)
                .setSterilizationBatchNo(sterilizationBatchNo)
                .setActiveAttachmentVersion(1)
                .setAttachmentIds(List.of(attachmentId))
                .setAttachmentHashes(List.of(String.valueOf(hashDigit).repeat(64)));
    }

    private record Fixture(
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            String reportSnapshotHash,
            List<MesProEdhrBatchExecutionTaskDO> batchTasks,
            MesProEdhrReleaseApproveReqVO command) {

        MesProEdhrReleaseTransactionDO transaction() {
            return new MesProEdhrReleaseTransactionDO()
                    .setId(1001L)
                    .setBatchExecutionId(901L)
                    .setReleaseStatus(MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL)
                    .setVersion(3);
        }

        MesProEdhrReleaseTransactionDO releasedTransaction() {
            return transaction().setReleaseStatus(MesProEdhrReleaseServiceImpl.STATUS_RELEASED).setVersion(4);
        }

        MesProEdhrWorkTaskDO workTask() {
            return new MesProEdhrWorkTaskDO()
                    .setId(2001L)
                    .setTaskType("RELEASE_APPROVE")
                    .setBatchExecutionId(901L)
                    .setBusinessScopeType("RELEASE_TRANSACTION")
                    .setBusinessScopeId(1001L)
                    .setCandidateSourceType("ROLE_GROUP")
                    .setCandidateSourceId(77L)
                    .setCandidateUserSnapshot(String.valueOf(ACTOR_USER_ID))
                    .setResponsibilitySourceKey(MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE)
                    .setStatus(MesProEdhrWorkTaskStatus.TODO);
        }

        MesProEdhrBatchExecutionDO batch() {
            return new MesProEdhrBatchExecutionDO().setId(901L).setRouteId(501L);
        }

        java.time.LocalDateTime now() {
            return java.time.LocalDateTime.ofInstant(Instant.parse("2026-08-16T00:00:00Z"), ZoneOffset.UTC);
        }
    }
}
