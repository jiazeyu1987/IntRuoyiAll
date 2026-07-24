package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalContext;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalEffectResult;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequest;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequestStatus;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordDefinitionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordVersionApprovalEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordDefinitionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordVersionApprovalEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordVersionMigrationItemMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProBatchRecordVersionBusinessApprovalEffectExecutorTest {

    private MesProBatchRecordVersionBusinessApprovalEffectExecutor executor;

    @Mock
    private MesProBatchRecordVersionMapper versionMapper;
    @Mock
    private MesProBatchRecordDefinitionMapper definitionMapper;
    @Mock
    private MesProBatchRecordVersionMigrationItemMapper migrationItemMapper;
    @Mock
    private MesProBatchRecordVersionApprovalEventMapper approvalEventMapper;

    @BeforeEach
    void setUp() {
        executor = new MesProBatchRecordVersionBusinessApprovalEffectExecutor();
        ReflectionTestUtils.setField(executor, "versionMapper", versionMapper);
        ReflectionTestUtils.setField(executor, "definitionMapper", definitionMapper);
        ReflectionTestUtils.setField(executor, "migrationItemMapper", migrationItemMapper);
        ReflectionTestUtils.setField(executor, "approvalEventMapper", approvalEventMapper);
    }

    @Test
    void executorCodeIsStablePlatformCode() {
        assertEquals("MES_BATCH_RECORD_VERSION_PUBLISH", executor.getExecutorCode());
    }

    @Test
    void directExecutionApprovesPrecheckedVersionWithoutStartingBpm() {
        when(versionMapper.selectByIdForUpdate(2001L)).thenReturn(precheckVersion());
        when(definitionMapper.selectByIdForUpdate(10L)).thenReturn(definition());
        when(versionMapper.selectPendingApprovalByDefinitionIdForUpdate(10L)).thenReturn(null);
        when(migrationItemMapper.countBlockingItems(2001L)).thenReturn(0L);
        when(definitionMapper.updateCurrentVersionIfMatch(10L, 1001L, 2001L)).thenReturn(1);
        when(versionMapper.updateById(any(MesProBatchRecordVersionDO.class))).thenReturn(1);

        BusinessApprovalEffectResult result = executor.executeDirect(context(), request());

        assertEquals("APPROVED", result.getResultState());
        ArgumentCaptor<MesProBatchRecordVersionDO> updateCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordVersionDO.class);
        verify(versionMapper).updateById(updateCaptor.capture());
        assertEquals(2001L, updateCaptor.getValue().getId());
        assertEquals("APPROVED", updateCaptor.getValue().getStatus());
        assertEquals(501L, updateCaptor.getValue().getApprovedBy());
        ArgumentCaptor<MesProBatchRecordVersionApprovalEventDO> eventCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordVersionApprovalEventDO.class);
        verify(approvalEventMapper).insert(eventCaptor.capture());
        assertEquals(10L, eventCaptor.getValue().getDefinitionId());
        assertEquals(2001L, eventCaptor.getValue().getVersionId());
        assertNull(eventCaptor.getValue().getApprovalInstanceId());
        assertEquals("3001:DIRECT", eventCaptor.getValue().getApprovalEventId());
        assertEquals("DIRECT", eventCaptor.getValue().getApprovalResult());
        assertEquals("APPROVED", eventCaptor.getValue().getProcessedResult());
        assertEquals(501L, eventCaptor.getValue().getActorUserId());
    }

    @Test
    void markPendingPersistsBpmProcessInstanceAndDoesNotApprove() {
        when(versionMapper.selectByIdForUpdate(2001L)).thenReturn(precheckVersion());
        when(definitionMapper.selectByIdForUpdate(10L)).thenReturn(definition());
        when(versionMapper.selectPendingApprovalByDefinitionIdForUpdate(10L)).thenReturn(null);
        when(migrationItemMapper.countBlockingItems(2001L)).thenReturn(0L);
        when(versionMapper.updateById(any(MesProBatchRecordVersionDO.class))).thenReturn(1);

        BusinessApprovalEffectResult result = executor.markPending(context(), requestWithProcessInstance());

        assertEquals("PENDING_APPROVAL", result.getResultState());
        ArgumentCaptor<MesProBatchRecordVersionDO> updateCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordVersionDO.class);
        verify(versionMapper).updateById(updateCaptor.capture());
        assertEquals("PENDING_APPROVAL", updateCaptor.getValue().getStatus());
        assertEquals(501L, updateCaptor.getValue().getSubmittedBy());
        assertEquals("bpm-2001", updateCaptor.getValue().getApprovalInstanceId());
        verify(definitionMapper, never()).updateCurrentVersionIfMatch(any(), any(), any());
    }

    @Test
    void approvedExecutionSwitchesCurrentVersion() {
        when(versionMapper.selectByIdForUpdate(2001L)).thenReturn(pendingVersion());
        when(definitionMapper.updateCurrentVersionIfMatch(10L, 1001L, 2001L)).thenReturn(1);
        when(versionMapper.updateById(any(MesProBatchRecordVersionDO.class))).thenReturn(1);

        BusinessApprovalEffectResult result = executor.executeApproved(context(), requestWithProcessInstance(), 902L);

        assertEquals("APPROVED", result.getResultState());
        verify(definitionMapper).updateCurrentVersionIfMatch(10L, 1001L, 2001L);
        verify(versionMapper).obsoleteApprovedVersionsExcept(10L, 2001L);
        ArgumentCaptor<MesProBatchRecordVersionDO> updateCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordVersionDO.class);
        verify(versionMapper).updateById(updateCaptor.capture());
        assertEquals("APPROVED", updateCaptor.getValue().getStatus());
        assertEquals(902L, updateCaptor.getValue().getApprovedBy());
    }

    @Test
    void rejectedExecutionKeepsCurrentVersionAndRecordsReason() {
        when(versionMapper.selectByIdForUpdate(2001L)).thenReturn(pendingVersion());
        when(versionMapper.updateById(any(MesProBatchRecordVersionDO.class))).thenReturn(1);

        BusinessApprovalEffectResult result = executor.reject(context(), requestWithProcessInstance(), 903L,
                "资料不完整");

        assertEquals("REJECTED", result.getResultState());
        ArgumentCaptor<MesProBatchRecordVersionDO> updateCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordVersionDO.class);
        verify(versionMapper).updateById(updateCaptor.capture());
        assertEquals("REJECTED", updateCaptor.getValue().getStatus());
        assertEquals(903L, updateCaptor.getValue().getApprovedBy());
        assertEquals("资料不完整", updateCaptor.getValue().getRejectReason());
        verify(definitionMapper, never()).updateCurrentVersionIfMatch(any(), any(), any());
    }

    @Test
    void cancelledExecutionRejectsPendingVersionWithoutPublishing() {
        when(versionMapper.selectByIdForUpdate(2001L)).thenReturn(pendingVersion());
        when(versionMapper.updateById(any(MesProBatchRecordVersionDO.class))).thenReturn(1);

        BusinessApprovalEffectResult result = executor.cancel(context(), requestWithProcessInstance(), 904L,
                "撤回审批");

        assertEquals("REJECTED", result.getResultState());
        verify(definitionMapper, never()).updateCurrentVersionIfMatch(any(), any(), any());
    }

    private static BusinessApprovalContext context() {
        return BusinessApprovalContext.builder()
                .tenantId(122L)
                .dataDomain("MES")
                .systemCode("MES")
                .objectType("BATCH_RECORD_VERSION")
                .objectId("2001")
                .objectVersion("V2.0")
                .actionCode("PUBLISH")
                .objectState("PRECHECK_PASSED")
                .applicantUserId(501L)
                .reason("publish batch record version")
                .build();
    }

    private static BusinessApprovalRequest request() {
        return BusinessApprovalRequest.builder()
                .requestId(3001L)
                .tenantId(122L)
                .effectExecutorCode("MES_BATCH_RECORD_VERSION_PUBLISH")
                .status(BusinessApprovalRequestStatus.PENDING_BPM)
                .context(context())
                .build();
    }

    private static BusinessApprovalRequest requestWithProcessInstance() {
        return request().toBuilder()
                .processInstanceId("bpm-2001")
                .build();
    }

    private static MesProBatchRecordDefinitionDO definition() {
        return MesProBatchRecordDefinitionDO.builder()
                .id(10L)
                .batchRecordName("平台审批批记录")
                .currentVersionId(1001L)
                .build();
    }

    private static MesProBatchRecordVersionDO precheckVersion() {
        return MesProBatchRecordVersionDO.builder()
                .id(2001L)
                .definitionId(10L)
                .versionNo("V2.0")
                .status("PRECHECK_PASSED")
                .sourceVersionId(1001L)
                .build();
    }

    private static MesProBatchRecordVersionDO pendingVersion() {
        return precheckVersion().setStatus("PENDING_APPROVAL")
                .setApprovalInstanceId("bpm-2001");
    }

}
