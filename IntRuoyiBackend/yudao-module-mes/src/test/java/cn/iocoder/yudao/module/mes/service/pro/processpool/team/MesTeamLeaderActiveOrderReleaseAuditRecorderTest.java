package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowAuditCommand;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowAuditEventType;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrOperationAuditCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrOperationAuditService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrOperationAuditServiceImpl;
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
        MesTeamLeaderActiveOrderReleaseAuditRecorder recorder =
                new MesTeamLeaderActiveOrderReleaseAuditRecorder(auditService);
        MesReleaseFlowAuditCommand command = new MesReleaseFlowAuditCommand()
                .setEventType(MesReleaseFlowAuditEventType.PQC_PRODUCTION_RELEASE_APPLIED)
                .setStage("SP_1")
                .setRequestId("request-1")
                .setIdempotencyKey("request-1")
                .setTenantId(1L)
                .setApplicationId(7001L)
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
                () -> assertEquals(8001L, captor.getValue().getWorkTaskId()),
                () -> assertEquals(MesReleaseFlowAuditEventType.PQC_PRODUCTION_RELEASE_APPLIED,
                        captor.getValue().getOperationType()),
                () -> assertEquals("SUCCESS", captor.getValue().getResultStatus()));
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
