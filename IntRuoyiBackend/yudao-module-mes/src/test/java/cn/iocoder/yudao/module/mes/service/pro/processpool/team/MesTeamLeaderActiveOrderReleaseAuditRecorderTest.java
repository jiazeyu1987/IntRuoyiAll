package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowAuditCommand;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowAuditEventType;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrOperationAuditCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrOperationAuditService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrOperationAuditServiceImpl;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import cn.hutool.crypto.digest.DigestUtil;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class MesTeamLeaderActiveOrderReleaseAuditRecorderTest {

    @Test
    void adapterUsesCallerTransactionAuditEntryOnly() {
        MesProEdhrOperationAuditService auditService = mock(MesProEdhrOperationAuditService.class);
        AdminUserService adminUserService = mock(AdminUserService.class);
        verifyActor(adminUserService, 1001L);
        MesTeamLeaderActiveOrderReleaseAuditRecorder recorder =
                new MesTeamLeaderActiveOrderReleaseAuditRecorder(auditService, adminUserService);
        MesReleaseFlowAuditCommand command = new MesReleaseFlowAuditCommand()
                .setEventType(MesReleaseFlowAuditEventType.PQC_PRODUCTION_RELEASE_APPLIED)
                .setStage("SP_1")
                .setRequestId("request-1")
                .setIdempotencyKey("request-1")
                .setTenantId(1L)
                .setApplicationId(7001L)
                .setActiveOrderId(8101L)
                .setWorkTaskId(8001L)
                .setToStatus("PQC_RELEASE_PENDING")
                .setVersion(1)
                .setActorUserId(1001L)
                .setOccurredAt(LocalDateTime.of(2026, 8, 14, 12, 0))
                .setSourceSnapshotHash("source-hash")
                .setResultStatus("SUCCESS");

        recorder.record(command);

        ArgumentCaptor<MesProEdhrOperationAuditCommand> captor =
                ArgumentCaptor.forClass(MesProEdhrOperationAuditCommand.class);
        verify(auditService).recordInCallerTransaction(captor.capture());
        verify(auditService, never()).record(captor.getValue());
        assertAll(
                () -> assertEquals("request-1", captor.getValue().getRequestId()),
                () -> assertEquals("PRODUCTION_RELEASE_APPLICATION", captor.getValue().getObjectType()),
                () -> assertEquals("7001", captor.getValue().getObjectId()),
                () -> assertEquals(8101L,
                        ((com.alibaba.fastjson.JSONObject) com.alibaba.fastjson.JSON.parseObject(
                                captor.getValue().getMetadataJson())).getLong("activeOrderId")),
                () -> assertEquals(8001L, captor.getValue().getWorkTaskId()),
                () -> assertEquals(MesReleaseFlowAuditEventType.PQC_PRODUCTION_RELEASE_APPLIED,
                        captor.getValue().getOperationType()),
                () -> assertEquals("测试操作人", captor.getValue().getActorUsername()),
                () -> assertEquals("mes:pro-process-pool-team-leader:release-apply",
                        captor.getValue().getPermissionCode()),
                () -> assertEquals("SUCCESS", captor.getValue().getResultStatus()));
    }

    @Test
    void pqcDecisionUsesPqcApprovalPermissionCode() {
        MesProEdhrOperationAuditService auditService = mock(MesProEdhrOperationAuditService.class);
        AdminUserService adminUserService = mock(AdminUserService.class);
        verifyActor(adminUserService, 7101L);
        MesTeamLeaderActiveOrderReleaseAuditRecorder recorder =
                new MesTeamLeaderActiveOrderReleaseAuditRecorder(auditService, adminUserService);
        recorder.record(new MesReleaseFlowAuditCommand()
                .setEventType(MesReleaseFlowAuditEventType.PQC_PRODUCTION_RELEASE_APPROVED)
                .setStage("SP_2")
                .setActiveOrderId(8101L)
                .setApplicationId(7001L)
                .setWorkTaskId(8001L)
                .setBatchExecutionId(9001L)
                .setSignatureId(7701L)
                .setActorUserId(7101L)
                .setOccurredAt(LocalDateTime.of(2026, 8, 15, 12, 0))
                .setSourceSnapshotHash("source-hash")
                .setResultStatus("SUCCESS"));

        ArgumentCaptor<MesProEdhrOperationAuditCommand> captor =
                ArgumentCaptor.forClass(MesProEdhrOperationAuditCommand.class);
        verify(auditService).recordInCallerTransaction(captor.capture());
        assertEquals("mes:pro-production-release:pqc-approve", captor.getValue().getPermissionCode());
        var metadata = (com.alibaba.fastjson.JSONObject) com.alibaba.fastjson.JSON.parseObject(
                captor.getValue().getMetadataJson());
        assertAll(
                () -> assertEquals("PQC生产放行", captor.getValue().getActionName()),
                () -> assertEquals(9001L, captor.getValue().getBatchExecutionId()),
                () -> assertEquals("source-hash", captor.getValue().getAfterSummaryHash()),
                () -> assertEquals(8101L, metadata.getLong("activeOrderId")),
                () -> assertEquals(9001L, metadata.getLong("batchExecutionId")),
                () -> assertEquals(7701L, metadata.getLong("signatureId")));
    }

    @Test
    void activeOrderFormalFactSnapshotUsesDigestForAuditSummaryAndKeepsSourceInMetadata() {
        MesProEdhrOperationAuditService auditService = mock(MesProEdhrOperationAuditService.class);
        AdminUserService adminUserService = mock(AdminUserService.class);
        verifyActor(adminUserService, 7101L);
        MesTeamLeaderActiveOrderReleaseAuditRecorder recorder =
                new MesTeamLeaderActiveOrderReleaseAuditRecorder(auditService, adminUserService);
        String formalFactSnapshot = "ACTIVE_ORDER_FACTS:" + "a".repeat(64);

        recorder.record(new MesReleaseFlowAuditCommand()
                .setEventType(MesReleaseFlowAuditEventType.BATCH_RECORD_RELEASE_APPROVED)
                .setStage("SP_3")
                .setRequestId("market-release-1")
                .setActiveOrderId(8101L)
                .setApplicationId(7001L)
                .setBatchExecutionId(9001L)
                .setSignatureId(7701L)
                .setActorUserId(7101L)
                .setOccurredAt(LocalDateTime.of(2026, 9, 20, 13, 30))
                .setSourceSnapshotHash(formalFactSnapshot)
                .setResultStatus("SUCCESS"));

        ArgumentCaptor<MesProEdhrOperationAuditCommand> captor =
                ArgumentCaptor.forClass(MesProEdhrOperationAuditCommand.class);
        verify(auditService).recordInCallerTransaction(captor.capture());
        MesProEdhrOperationAuditCommand audit = captor.getValue();
        var metadata = (com.alibaba.fastjson.JSONObject) com.alibaba.fastjson.JSON.parseObject(audit.getMetadataJson());
        assertAll(
                () -> assertEquals(MesReleaseFlowAuditEventType.BATCH_RECORD_RELEASE_APPROVED,
                        audit.getOperationType()),
                () -> assertEquals(DigestUtil.sha256Hex(formalFactSnapshot), audit.getAfterSummaryHash()),
                () -> assertEquals(64, audit.getAfterSummaryHash().length()),
                () -> assertEquals(formalFactSnapshot, metadata.getString("sourceSnapshotHash")));
    }

    private static void verifyActor(AdminUserService adminUserService, Long actorUserId) {
        org.mockito.Mockito.when(adminUserService.getUser(actorUserId))
                .thenReturn(new AdminUserDO().setId(actorUserId).setNickname("测试操作人"));
    }

    @Test
    void callerTransactionAuditMethodUsesRequiredPropagation() throws Exception {
        Transactional transactional = MesProEdhrOperationAuditServiceImpl.class
                .getMethod("recordInCallerTransaction", MesProEdhrOperationAuditCommand.class)
                .getAnnotation(Transactional.class);

        assertNotNull(transactional);
        assertEquals(Propagation.REQUIRED, transactional.propagation());
    }
}
