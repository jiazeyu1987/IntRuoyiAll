package cn.iocoder.yudao.module.mes.productionrelease.core;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MesReleaseFlowCoreContractTest {

    private static final long UNSAFE_JAVASCRIPT_ID = 9_007_199_254_740_993L;

    @Test
    void lifecycleMustUseStatusAndVersionCasAndWriteAuditInCallerTransaction() {
        MesProcessPoolActiveOrderReleaseApplicationMapper mapper =
                mock(MesProcessPoolActiveOrderReleaseApplicationMapper.class);
        MesReleaseFlowAuditRecorder auditRecorder = mock(MesReleaseFlowAuditRecorder.class);
        MesProcessPoolActiveOrderReleaseApplicationDO application =
                new MesProcessPoolActiveOrderReleaseApplicationDO()
                        .setId(8101L)
                        .setApplicationStatus(MesReleaseFlowStatus.PQC_RELEASE_PENDING)
                        .setVersion(3)
                        .setSourceSnapshotHash("source-hash");
        when(mapper.selectByIdForUpdate(8101L)).thenReturn(application);
        when(mapper.compareAndSetStatus(8101L, 3,
                MesReleaseFlowStatus.PQC_RELEASE_PENDING,
                MesReleaseFlowStatus.REPORT_UPLOAD_PENDING)).thenReturn(1);
        MesReleaseFlowLifecycleServiceImpl service =
                new MesReleaseFlowLifecycleServiceImpl(mapper, auditRecorder);

        MesProcessPoolActiveOrderReleaseApplicationDO transitioned = service.transition(
                new MesReleaseFlowTransitionCommand()
                        .setApplicationId(8101L)
                        .setExpectedVersion(3)
                        .setExpectedStatus(MesReleaseFlowStatus.PQC_RELEASE_PENDING)
                        .setTargetStatus(MesReleaseFlowStatus.REPORT_UPLOAD_PENDING)
                        .setStage(MesReleaseFlowStage.SP_2)
                        .setAuditEventType(MesReleaseFlowAuditEventType.PQC_PRODUCTION_RELEASE_APPROVED)
                        .setRequestId("request-1")
                        .setIdempotencyKey("idempotency-1")
                        .setActorUserId(1001L));

        assertEquals(MesReleaseFlowStatus.REPORT_UPLOAD_PENDING, transitioned.getApplicationStatus());
        assertEquals(4, transitioned.getVersion());
        verify(mapper).compareAndSetStatus(8101L, 3,
                MesReleaseFlowStatus.PQC_RELEASE_PENDING,
                MesReleaseFlowStatus.REPORT_UPLOAD_PENDING);
        verify(auditRecorder).record(org.mockito.ArgumentMatchers.argThat(command ->
                MesReleaseFlowAuditEventType.PQC_PRODUCTION_RELEASE_APPROVED.equals(command.getEventType())
                        && "PQC_RELEASE_PENDING".equals(command.getFromStatus())
                        && "REPORT_UPLOAD_PENDING".equals(command.getToStatus())
                        && Integer.valueOf(4).equals(command.getVersion())));
    }

    @Test
    void lifecycleMustBlockLegacyApplicationInsteadOfInferringNewStatus() {
        MesProcessPoolActiveOrderReleaseApplicationMapper mapper =
                mock(MesProcessPoolActiveOrderReleaseApplicationMapper.class);
        MesProcessPoolActiveOrderReleaseApplicationDO legacy =
                new MesProcessPoolActiveOrderReleaseApplicationDO()
                        .setId(8102L).setApplicationStatus("PENDING_RELEASE_APPROVAL").setVersion(1);
        when(mapper.selectByIdForUpdate(8102L)).thenReturn(legacy);
        MesReleaseFlowLifecycleServiceImpl service =
                new MesReleaseFlowLifecycleServiceImpl(mapper, mock(MesReleaseFlowAuditRecorder.class));

        MesReleaseFlowBlockerException exception = assertThrows(MesReleaseFlowBlockerException.class,
                () -> service.lockAndValidate(8102L, 1,
                        MesReleaseFlowStatus.PQC_RELEASE_PENDING, MesReleaseFlowStage.SP_2));

        assertEquals(MesReleaseFlowBlockerType.LEGACY_RELEASE_APPLICATION_MIGRATION_REQUIRED,
                exception.getFailure().getBlockers().get(0).getBlockerType());
    }

    @Test
    void adviceMustPreserveStructuredFailureDataWithNonZeroCode() throws Exception {
        MesReleaseFlowFailureRespVO failure = new MesReleaseFlowFailureRespVO()
                .setStage(MesReleaseFlowStage.SP_2)
                .setCurrentStatus(MesReleaseFlowStatus.PQC_RELEASE_PENDING)
                .setBlockers(List.of(new MesReleaseFlowBlocker()
                        .setBlockerType(MesReleaseFlowBlockerType.PROCESS_INSPECTION_SOURCE_REQUIRED)
                        .setObjectType("ACTIVE_ORDER")
                        .setObjectId("8101")
                        .setRouteProcessId(UNSAFE_JAVASCRIPT_ID)
                        .setReason("missing formal source")
                        .setSuggestion("complete inspection")));
        MesReleaseFlowBlockerException exception =
                new MesReleaseFlowBlockerException("release precondition failed", failure);

        CommonResult<MesReleaseFlowFailureRespVO> result =
                new MesReleaseFlowExceptionAdvice().handleBlocker(exception);
        JsonNode json = JsonUtils.parseTree(JsonUtils.toJsonString(result));

        assertTrue(json.get("code").asInt() != 0);
        assertEquals("SP_2", json.at("/data/stage").asText());
        assertEquals("PROCESS_INSPECTION_SOURCE_REQUIRED",
                json.at("/data/blockers/0/blockerType").asText());
        assertTrue(json.at("/data/blockers/0/routeProcessId").isTextual());
        assertEquals(Long.toString(UNSAFE_JAVASCRIPT_ID),
                json.at("/data/blockers/0/routeProcessId").asText());
    }

    @Test
    void workTaskProjectionMustKeepLongIdsAsStringsAndExposeReportQueryFields() throws Exception {
        MesProEdhrWorkTaskRespVO response = new MesProEdhrWorkTaskRespVO()
                .setId(UNSAFE_JAVASCRIPT_ID)
                .setBatchExecutionId(UNSAFE_JAVASCRIPT_ID)
                .setBatchTaskId(UNSAFE_JAVASCRIPT_ID)
                .setBusinessScopeId(UNSAFE_JAVASCRIPT_ID)
                .setNodeType("STERILIZATION_REPORT")
                .setNodeName("Sterilization report")
                .setVersion(7);
        JsonNode json = JsonUtils.parseTree(JsonUtils.toJsonString(response));

        assertEquals(Long.toString(UNSAFE_JAVASCRIPT_ID), json.get("id").asText());
        assertTrue(json.get("id").isTextual());
        assertTrue(json.get("batchExecutionId").isTextual());
        assertTrue(json.get("batchTaskId").isTextual());
        assertTrue(json.get("businessScopeId").isTextual());
        assertEquals("STERILIZATION_REPORT", json.get("nodeType").asText());
        assertEquals(7, json.get("version").asInt());

        assertEquals(List.class,
                MesProEdhrWorkTaskPageReqVO.class.getDeclaredField("nodeTypes").getType());
        assertEquals(Long.class,
                MesProEdhrWorkTaskPageReqVO.class.getDeclaredField("batchExecutionId").getType());
    }

    @Test
    void idempotencyKeyMustBeAsciiAndPayloadHashMustBeStable() {
        assertEquals("release-key-1", MesReleaseFlowIdempotency.requireKey("release-key-1"));
        assertThrows(MesReleaseFlowBlockerException.class,
                () -> MesReleaseFlowIdempotency.requireKey("放行-key"));
        assertEquals(MesReleaseFlowIdempotency.payloadHash("A", "B"),
                MesReleaseFlowIdempotency.payloadHash("A", "B"));
        assertTrue(!MesReleaseFlowIdempotency.payloadHash("A", "B")
                .equals(MesReleaseFlowIdempotency.payloadHash("A", "C")));
    }
}
