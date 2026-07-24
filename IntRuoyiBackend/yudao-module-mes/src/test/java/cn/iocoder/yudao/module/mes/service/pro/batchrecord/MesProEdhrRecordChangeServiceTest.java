package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalContext;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalException;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalPolicy;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalPolicyMode;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalPolicyResolution;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequest;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequestStatus;
import cn.iocoder.yudao.module.bpm.businessapproval.service.BusinessApprovalErrorCode;
import cn.iocoder.yudao.module.bpm.businessapproval.service.BusinessApprovalOrchestrator;
import cn.iocoder.yudao.module.bpm.businessapproval.service.BusinessApprovalPolicyResolveService;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchVoidApprovalResolutionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchVoidApprovalResolutionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordChangeApproveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordChangePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordChangeRequestReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordChangeRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionArchiveDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionArchiveDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionSignatureDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrRecordChangeEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionArchiveMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionArchiveMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionSignatureMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrRecordChangeEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.MockedStatic;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_CHANGE_REASON_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_RELEASE_STATUS_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import(MesProEdhrRecordChangeServiceImpl.class)
class MesProEdhrRecordChangeServiceTest extends BaseDbUnitTest {

    private static final Long ACTOR_ID = 101L;
    private static final String HASH_64 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    @Resource
    private MesProEdhrRecordChangeService changeService;
    @Resource
    private MesProBatchRecordExecutionMapper executionMapper;
    @Resource
    private MesProBatchRecordExecutionArchiveMapper archiveMapper;
    @Resource
    private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Resource
    private MesProEdhrBatchExecutionArchiveMapper batchArchiveMapper;
    @Resource
    private MesProEdhrBatchExecutionSignatureMapper batchSignatureMapper;
    @Resource
    private MesProEdhrRecordChangeEventMapper changeEventMapper;
    @Resource
    private MesProEdhrReleaseTransactionMapper releaseTransactionMapper;

    @MockitoBean
    private MesProBatchRecordExecutionSignatureService signatureService;
    @MockitoBean
    private BpmProcessInstanceApi processInstanceApi;
    @MockitoBean
    private AdminUserApi adminUserApi;
    @MockitoBean
    private BusinessApprovalOrchestrator businessApprovalOrchestrator;
    @MockitoBean
    private BusinessApprovalPolicyResolveService businessApprovalPolicyResolveService;
    @MockitoBean
    private MesProEdhrGoldenFingerPermissionService goldenFingerPermissionService;

    @BeforeEach
    void setUpBpm() {
        when(processInstanceApi.createProcessInstance(any(), any())).thenReturn("void-process-default");
        when(businessApprovalOrchestrator.submit(any(BusinessApprovalContext.class)))
                .thenAnswer(invocation -> submitBatchVoidBusinessApprovalInTest(invocation));
    }

    @Test
    void voidExecution_approvedExecution_createsSignedChangeEventAndMarksExecutionVoided() {
        MesProBatchRecordExecutionDO execution = insertApprovedExecution();
        when(signatureService.recordSubmitSignature(eq(execution.getId()), eq("request-pass"), eq("request comment")))
                .thenReturn(9001L);

        EdhrRecordChangeRespVO response;
        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            response = changeService.requestVoidExecution(new EdhrRecordChangeRequestReqVO()
                    .setExecutionId(execution.getId())
                    .setReasonCategory("DATA_ERROR")
                    .setReasonText("原始记录确认存在错误，需要受控作废。")
                    .setPassword("request-pass")
                    .setComment("request comment"));
        }

        assertEquals("VOID", response.getChangeType());
        assertEquals("SUBMITTED", response.getChangeStatus());
        assertEquals(9001L, response.getRequestSignatureId());
        MesProEdhrRecordChangeEventDO event = changeEventMapper.selectById(response.getId());
        assertEquals("EXECUTION", event.getTargetScope());
        assertEquals(execution.getId(), event.getExecutionId());
        assertEquals("3", event.getPreviousStatus());
        assertEquals("4", event.getNewStatus());
        assertEquals(HASH_64, event.getPreviousHeadHash());
        assertEquals(ACTOR_ID, event.getRequestedBy());
        assertNotNull(event.getRequestedAt());
    }

    @Test
    void voidExecution_archivedExecution_marksArchiveInvalidWithoutDeletingArchive() {
        MesProBatchRecordExecutionDO execution = insertApprovedExecution();
        MesProBatchRecordExecutionArchiveDO archive = insertSealedArchive(execution.getId());
        when(signatureService.recordSubmitSignature(eq(execution.getId()), eq("request-pass"), any()))
                .thenReturn(9001L);
        when(signatureService.recordApprovalSignature(any(MesProBatchRecordExecutionApprovalSignatureCommand.class)))
                .thenReturn(9002L);

        EdhrRecordChangeRespVO request;
        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            request = changeService.requestVoidExecution(new EdhrRecordChangeRequestReqVO()
                    .setExecutionId(execution.getId())
                    .setReasonCategory("DATA_ERROR")
                    .setReasonText("归档记录存在确认错误，需要受控作废。")
                    .setPassword("request-pass"));
            changeService.approveVoidExecution(new EdhrRecordChangeApproveReqVO()
                    .setChangeEventId(request.getId())
                    .setPassword("approve-pass")
                    .setComment("approve comment"));
        }

        MesProBatchRecordExecutionDO updatedExecution = executionMapper.selectById(execution.getId());
        assertEquals(4, updatedExecution.getStatus());
        assertEquals(request.getId(), updatedExecution.getVoidedByChangeEventId());
        MesProBatchRecordExecutionArchiveDO updatedArchive = archiveMapper.selectById(archive.getId());
        assertEquals(Boolean.FALSE, updatedArchive.getArchiveValidFlag());
        assertEquals("VOIDED", updatedArchive.getArchiveValidStatus());
        assertEquals(request.getId(), updatedArchive.getInvalidatedByChangeEventId());
        MesProEdhrRecordChangeEventDO event = changeEventMapper.selectById(request.getId());
        assertEquals("EFFECTIVE", event.getChangeStatus());
        assertEquals(9002L, event.getApprovalSignatureId());
        assertEquals(ACTOR_ID, event.getApprovedBy());
        assertNotNull(event.getEffectiveAt());
        assertEquals(archive.getSha256(), event.getPreviousArchiveHash());
        assertEquals(archive.getSha256(), event.getNewArchiveHash());
    }

    @Test
    void voidExecution_missingReasonOrSignature_failsFast() {
        MesProBatchRecordExecutionDO execution = insertApprovedExecution();

        assertServiceException(() -> changeService.requestVoidExecution(new EdhrRecordChangeRequestReqVO()
                        .setExecutionId(execution.getId())
                        .setReasonCategory("DATA_ERROR")
                        .setReasonText(" ")
                        .setPassword("request-pass")),
                PRO_BATCH_RECORD_EXECUTION_CHANGE_REASON_REQUIRED);
    }

    @Test
    void voidExecution_duplicateEffectiveChange_rejected() {
        MesProBatchRecordExecutionDO execution = insertApprovedExecution();
        when(signatureService.recordSubmitSignature(eq(execution.getId()), eq("request-pass"), any()))
                .thenReturn(9001L);
        when(signatureService.recordApprovalSignature(any(MesProBatchRecordExecutionApprovalSignatureCommand.class)))
                .thenReturn(9002L);

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            EdhrRecordChangeRespVO request = changeService.requestVoidExecution(new EdhrRecordChangeRequestReqVO()
                    .setExecutionId(execution.getId())
                    .setReasonCategory("DATA_ERROR")
                    .setReasonText("第一次受控作废申请。")
                    .setPassword("request-pass"));
            changeService.approveVoidExecution(new EdhrRecordChangeApproveReqVO()
                    .setChangeEventId(request.getId())
                    .setPassword("approve-pass"));

            assertServiceException(() -> changeService.requestVoidExecution(new EdhrRecordChangeRequestReqVO()
                            .setExecutionId(execution.getId())
                            .setReasonCategory("DATA_ERROR")
                            .setReasonText("重复作废申请。")
                            .setPassword("request-pass")),
                    PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID);
        }
    }

    @Test
    void getPage_returnsChangeEvents() {
        MesProBatchRecordExecutionDO execution = insertApprovedExecution();
        when(signatureService.recordSubmitSignature(eq(execution.getId()), eq("request-pass"), any()))
                .thenReturn(9001L);

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            changeService.requestVoidExecution(new EdhrRecordChangeRequestReqVO()
                    .setExecutionId(execution.getId())
                    .setReasonCategory("DATA_ERROR")
                    .setReasonText("查询用作废申请。")
                    .setPassword("request-pass"));
        }

        List<MesProEdhrRecordChangeEventDO> events = changeEventMapper.selectList(
                new LambdaQueryWrapperX<MesProEdhrRecordChangeEventDO>()
                        .eq(MesProEdhrRecordChangeEventDO::getExecutionId, execution.getId()));
        assertEquals(1, events.size());
    }

    @Test
    void reopenBatch_closedBatch_createsChangeEventAndPreservesSealedArchive() {
        MesProEdhrBatchExecutionDO batch = insertClosedBatchExecution();
        MesProEdhrBatchExecutionArchiveDO archive = insertSealedBatchArchive(batch.getId());
        when(signatureService.recordSubmitSignature(eq(0L), eq("request-pass"), any()))
                .thenReturn(9101L);
        when(signatureService.recordApprovalSignature(any(MesProBatchRecordExecutionApprovalSignatureCommand.class)))
                .thenReturn(9102L);

        EdhrRecordChangeRespVO request;
        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            request = changeService.requestReopenBatch(new EdhrRecordChangeRequestReqVO()
                    .setBatchExecutionId(batch.getId())
                    .setReasonCategory("PROCESS_DEVIATION")
                    .setReasonText("已关闭未归档批次需要按受控流程重开。")
                    .setPassword("request-pass"));
            changeService.approveReopenBatch(new EdhrRecordChangeApproveReqVO()
                    .setChangeEventId(request.getId())
                    .setPassword("approve-pass")
                    .setComment("approve reopen"));
        }

        MesProEdhrBatchExecutionDO updatedBatch = batchExecutionMapper.selectById(batch.getId());
        assertEquals(70, updatedBatch.getStatus());
        MesProEdhrBatchExecutionArchiveDO updatedArchive = batchArchiveMapper.selectById(archive.getId());
        assertEquals(Boolean.FALSE, updatedArchive.getArchiveValidFlag());
        assertEquals("SUPERSEDED", updatedArchive.getArchiveValidStatus());
        assertEquals(request.getId(), updatedArchive.getInvalidatedByChangeEventId());
        MesProEdhrRecordChangeEventDO event = changeEventMapper.selectById(request.getId());
        assertEquals("REOPEN", event.getChangeType());
        assertEquals("BATCH", event.getTargetScope());
        assertEquals("EFFECTIVE", event.getChangeStatus());
        assertEquals("30", event.getPreviousStatus());
        assertEquals("70", event.getNewStatus());
        assertEquals(archive.getContentHash(), event.getPreviousArchiveHash());
        assertEquals(archive.getContentHash(), event.getNewArchiveHash());
    }

    @Test
    void reopenBatch_qualityRejectedBatch_createsEffectiveReopenEventAndPreservesRejectEvidence() {
        MesProEdhrBatchExecutionDO batch = insertRejectedBatchExecution();
        MesProEdhrBatchExecutionArchiveDO archive = insertSealedBatchArchive(batch.getId());
        when(signatureService.recordSubmitSignature(eq(0L), eq("request-pass"), any()))
                .thenReturn(9201L);
        when(signatureService.recordApprovalSignature(any(MesProBatchRecordExecutionApprovalSignatureCommand.class)))
                .thenReturn(9202L);

        EdhrRecordChangeRespVO request;
        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            request = changeService.requestReopenBatch(new EdhrRecordChangeRequestReqVO()
                    .setBatchExecutionId(batch.getId())
                    .setReasonCategory("QUALITY_MISJUDGEMENT")
                    .setReasonText("质量终态拒收经复核属于误拒收，需要按受控流程重开原记录。")
                    .setPassword("request-pass"));
            changeService.approveReopenBatch(new EdhrRecordChangeApproveReqVO()
                    .setChangeEventId(request.getId())
                    .setPassword("approve-pass")
                    .setComment("approve quality terminal reopen"));
        }

        MesProEdhrBatchExecutionDO updatedBatch = batchExecutionMapper.selectById(batch.getId());
        assertEquals(70, updatedBatch.getStatus());
        assertEquals(batch.getRejectSignatureId(), updatedBatch.getRejectSignatureId());
        assertEquals(batch.getRejectedBy(), updatedBatch.getRejectedBy());
        assertEquals(batch.getRejectedAt(), updatedBatch.getRejectedAt());
        assertEquals(batch.getRejectReason(), updatedBatch.getRejectReason());
        MesProEdhrBatchExecutionArchiveDO updatedArchive = batchArchiveMapper.selectById(archive.getId());
        assertEquals(Boolean.FALSE, updatedArchive.getArchiveValidFlag());
        assertEquals("SUPERSEDED", updatedArchive.getArchiveValidStatus());
        assertEquals(request.getId(), updatedArchive.getInvalidatedByChangeEventId());
        MesProEdhrRecordChangeEventDO event = changeEventMapper.selectById(request.getId());
        assertEquals("REOPEN", event.getChangeType());
        assertEquals("BATCH", event.getTargetScope());
        assertEquals("EFFECTIVE", event.getChangeStatus());
        assertEquals("50", event.getPreviousStatus());
        assertEquals("70", event.getNewStatus());
        assertEquals(archive.getContentHash(), event.getPreviousArchiveHash());
        assertEquals(archive.getContentHash(), event.getNewArchiveHash());
    }

    @Test
    void reopenBatch_archivedBatch_rejected() {
        MesProEdhrBatchExecutionDO batch = insertArchivedBatchExecution();

        assertServiceException(() -> changeService.requestReopenBatch(new EdhrRecordChangeRequestReqVO()
                        .setBatchExecutionId(batch.getId())
                        .setReasonCategory("PROCESS_DEVIATION")
                        .setReasonText("已归档批次不得重开。")
                        .setPassword("request-pass")),
                PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID);
    }

    @Test
    void voidBatchExecution_rowRequestStartsBpmWithBatchSignatureAndWorkOrderContext() {
        MesProEdhrBatchExecutionDO batch = insertActiveBatchExecution();
        when(processInstanceApi.createProcessInstance(eq(ACTOR_ID), any(BpmProcessInstanceCreateReqDTO.class)))
                .thenReturn("void-process-1");

        EdhrRecordChangeRespVO response;
        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            response = changeService.requestVoidBatchExecution(new EdhrRecordChangeRequestReqVO()
                    .setBatchExecutionId(batch.getId())
                    .setReasonCategory("ORDER_CANCELLED")
                    .setReasonText("工单对应批次执行需要按 BPM 作废。")
                    .setPassword("request-pass")
                    .setComment("void comment")
                    .setStartUserSelectAssignees(Map.of("Activity_approve", List.of(201L))));
        }

        assertEquals("VOID", response.getChangeType());
        assertEquals("BATCH", response.getTargetScope());
        assertEquals("SUBMITTED", response.getChangeStatus());
        assertEquals(batch.getId(), response.getBatchExecutionId());
        assertNull(response.getExecutionId());
        assertEquals("void-process-1", response.getBpmProcessInstanceId());
        assertEquals(10, batchExecutionMapper.selectById(batch.getId()).getStatus());

        MesProEdhrRecordChangeEventDO event = changeEventMapper.selectById(response.getId());
        assertEquals("10", event.getPreviousStatus());
        assertEquals("60", event.getNewStatus());
        assertNotNull(event.getRequestSignatureId());
        assertEquals(ACTOR_ID, event.getRequestedBy());
        verify(adminUserApi).validatePassword(ACTOR_ID, "request-pass");
        verify(signatureService, never()).recordSubmitSignature(eq(0L), eq("request-pass"), any());

        MesProEdhrBatchExecutionSignatureDO signature =
                batchSignatureMapper.selectById(event.getRequestSignatureId());
        assertNotNull(signature);
        assertEquals(batch.getId(), signature.getBatchExecutionId());
        assertEquals(ACTOR_ID, signature.getActorId());
        assertEquals("BATCH_VOID_REQUEST", signature.getActionType());
        assertEquals("PASSWORD", signature.getSignatureMode());
        assertEquals("void comment", signature.getComment());
        assertEquals(batch.getAggregateHash(), signature.getAggregateHash());

        ArgumentCaptor<BpmProcessInstanceCreateReqDTO> bpmCaptor =
                ArgumentCaptor.forClass(BpmProcessInstanceCreateReqDTO.class);
        verify(processInstanceApi).createProcessInstance(eq(ACTOR_ID), bpmCaptor.capture());
        BpmProcessInstanceCreateReqDTO bpmReq = bpmCaptor.getValue();
        assertEquals("mes-edhr-batch-execution-void-v1", bpmReq.getProcessDefinitionKey());
        assertEquals("BUSINESS_APPROVAL:4101", bpmReq.getBusinessKey());
        assertEquals(Map.of("Activity_approve", List.of(201L)), bpmReq.getStartUserSelectAssignees());
        assertEquals("EDHR_BATCH_EXECUTION_VOID", bpmReq.getVariables().get("businessType"));
        assertEquals("EDHR_BATCH_EXECUTION", bpmReq.getVariables().get("objectType"));
        assertEquals(String.valueOf(batch.getId()), bpmReq.getVariables().get("objectId"));
        assertEquals("VOID", bpmReq.getVariables().get("actionCode"));
        assertEquals("10", bpmReq.getVariables().get("objectState"));
        assertEquals(batch.getId(), bpmReq.getVariables().get("batchExecutionId"));
        assertEquals(batch.getBatchExecutionCode(), bpmReq.getVariables().get("batchExecutionCode"));
        assertEquals(batch.getWorkOrderId(), bpmReq.getVariables().get("workOrderId"));
        assertEquals(batch.getWorkOrderCode(), bpmReq.getVariables().get("workOrderCode"));
        assertEquals("ORDER_CANCELLED", bpmReq.getVariables().get("reasonCategory"));
        assertNull(bpmReq.getVariables().get("password"));

        ArgumentCaptor<BusinessApprovalContext> contextCaptor =
                ArgumentCaptor.forClass(BusinessApprovalContext.class);
        verify(businessApprovalOrchestrator).submit(contextCaptor.capture());
        BusinessApprovalContext context = contextCaptor.getValue();
        assertEquals("MES", context.getSystemCode());
        assertEquals("EDHR_BATCH_EXECUTION", context.getObjectType());
        assertEquals("VOID", context.getActionCode());
        assertEquals("10", context.getObjectState());
        assertEquals("request-pass", context.getTransientVariables().get("password"));
        assertNull(context.getVariables().get("password"));
    }

    @Test
    void resolveVoidBatchExecutionApproval_usesBusinessPolicyWithoutSubmittingApproval() {
        MesProEdhrBatchExecutionDO batch = insertActiveBatchExecution();
        when(businessApprovalPolicyResolveService.resolve(any(BusinessApprovalContext.class)))
                .thenReturn(BusinessApprovalPolicyResolution.from(BusinessApprovalPolicy.builder()
                        .policyId(7101L)
                        .tenantId(1L)
                        .dataDomain("MES")
                        .systemCode("MES")
                        .objectType("EDHR_BATCH_EXECUTION")
                        .actionCode("VOID")
                        .objectState(BusinessApprovalPolicy.OBJECT_STATE_ALL)
                        .mode(BusinessApprovalPolicyMode.BPM_REQUIRED)
                        .processDefinitionKey("mes-edhr-batch-execution-void-v1")
                        .effectExecutorCode(MesProEdhrBatchVoidFormEffectExecutor.EXECUTOR_CODE)
                        .status(BusinessApprovalPolicy.STATUS_PUBLISHED)
                        .build()));

        EdhrBatchVoidApprovalResolutionRespVO response;
        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            response = changeService.resolveVoidBatchExecutionApproval(
                    new EdhrBatchVoidApprovalResolutionReqVO().setBatchExecutionId(batch.getId()));
        }

        assertEquals(7101L, response.getPolicyId());
        assertEquals("BPM_REQUIRED", response.getPolicyMode());
        assertEquals(Boolean.TRUE, response.getRequiresBpm());
        assertEquals("mes-edhr-batch-execution-void-v1", response.getBpmProcessKey());
        assertEquals(MesProEdhrBatchVoidFormEffectExecutor.EXECUTOR_CODE, response.getEffectExecutorCode());

        ArgumentCaptor<BusinessApprovalContext> contextCaptor =
                ArgumentCaptor.forClass(BusinessApprovalContext.class);
        verify(businessApprovalPolicyResolveService).resolve(contextCaptor.capture());
        BusinessApprovalContext context = contextCaptor.getValue();
        assertEquals("MES", context.getSystemCode());
        assertEquals("EDHR_BATCH_EXECUTION", context.getObjectType());
        assertEquals("VOID", context.getActionCode());
        assertEquals(String.valueOf(batch.getId()), context.getObjectId());
        assertEquals("10", context.getObjectState());
        verify(businessApprovalOrchestrator, never()).submit(any(BusinessApprovalContext.class));
        assertEquals(0, changeEventMapper.selectList(new LambdaQueryWrapperX<MesProEdhrRecordChangeEventDO>()
                .eq(MesProEdhrRecordChangeEventDO::getBatchExecutionId, batch.getId())
                .eq(MesProEdhrRecordChangeEventDO::getChangeType, "VOID")).size());
    }

    @Test
    void voidBatchExecution_approvedBpmCallbackMarksBatchVoidedAndArchiveInvalid() {
        MesProEdhrBatchExecutionDO batch = insertClosedBatchExecution();
        MesProEdhrBatchExecutionArchiveDO archive = insertSealedBatchArchive(batch.getId());
        when(signatureService.recordSubmitSignature(eq(0L), eq("request-pass"), any()))
                .thenReturn(9351L);
        when(processInstanceApi.createProcessInstance(eq(ACTOR_ID), any(BpmProcessInstanceCreateReqDTO.class)))
                .thenReturn("void-process-2");

        EdhrRecordChangeRespVO request;
        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            request = changeService.requestVoidBatchExecution(new EdhrRecordChangeRequestReqVO()
                    .setBatchExecutionId(batch.getId())
                    .setReasonCategory("ORDER_CANCELLED")
                    .setReasonText("关闭后的批次执行作废。")
                    .setPassword("request-pass"));
            changeService.handleVoidBatchExecutionApprovalCallback("void-process-2",
                    "event-approved", "APPROVED", "同意作废", ACTOR_ID);
        }

        MesProEdhrBatchExecutionDO updatedBatch = batchExecutionMapper.selectById(batch.getId());
        assertEquals(60, updatedBatch.getStatus());
        MesProEdhrBatchExecutionArchiveDO updatedArchive = batchArchiveMapper.selectById(archive.getId());
        assertEquals(Boolean.FALSE, updatedArchive.getArchiveValidFlag());
        assertEquals("VOIDED", updatedArchive.getArchiveValidStatus());
        assertEquals(request.getId(), updatedArchive.getInvalidatedByChangeEventId());
        MesProEdhrRecordChangeEventDO event = changeEventMapper.selectById(request.getId());
        assertEquals("EFFECTIVE", event.getChangeStatus());
        assertEquals("void-process-2", event.getBpmProcessInstanceId());
        assertEquals(ACTOR_ID, event.getApprovedBy());
        assertNotNull(event.getEffectiveAt());
    }

    @Test
    void voidBatchExecution_directPlatformExecutionVoidsBatchWithoutBpmProcess() {
        MesProEdhrBatchExecutionDO batch = insertClosedBatchExecution();
        MesProEdhrBatchExecutionArchiveDO archive = insertSealedBatchArchive(batch.getId());

        EdhrRecordChangeRespVO response = changeService.executeDirectPlatformVoidBatchExecution(
                new EdhrRecordChangeRequestReqVO()
                        .setBatchExecutionId(batch.getId())
                        .setReasonCategory("ORDER_CANCELLED")
                        .setReasonText("关闭后的批次执行按 DIRECT 策略作废。")
                        .setPassword("request-pass")
                        .setComment("direct void"),
                ACTOR_ID);

        MesProEdhrBatchExecutionDO updatedBatch = batchExecutionMapper.selectById(batch.getId());
        assertEquals(60, updatedBatch.getStatus());
        MesProEdhrBatchExecutionArchiveDO updatedArchive = batchArchiveMapper.selectById(archive.getId());
        assertEquals(Boolean.FALSE, updatedArchive.getArchiveValidFlag());
        assertEquals("VOIDED", updatedArchive.getArchiveValidStatus());
        assertEquals(response.getId(), updatedArchive.getInvalidatedByChangeEventId());
        MesProEdhrRecordChangeEventDO event = changeEventMapper.selectById(response.getId());
        assertEquals("EFFECTIVE", event.getChangeStatus());
        assertNull(event.getBpmProcessInstanceId());
        assertEquals(ACTOR_ID, event.getRequestedBy());
        assertEquals(ACTOR_ID, event.getApprovedBy());
        assertNotNull(event.getRequestSignatureId());
        assertNotNull(event.getEffectiveAt());
        verify(processInstanceApi, never()).createProcessInstance(any(), any());
        verify(adminUserApi).validatePassword(ACTOR_ID, "request-pass");
    }

    @Test
    void voidBatchExecution_formCenterApprovedCallbackDoesNotPersistIdempotencyKeyAsBpmTaskId() {
        MesProEdhrBatchExecutionDO batch = insertClosedBatchExecution();
        when(processInstanceApi.createProcessInstance(eq(ACTOR_ID), any(BpmProcessInstanceCreateReqDTO.class)))
                .thenReturn("void-process-form-center-approved");
        String idempotencyKey = "EDHR-BATCH-VOID-" + batch.getId()
                + "-ed6f1f27-dc60-424e-abae-27f66d5a9f82";

        EdhrRecordChangeRespVO request;
        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            request = changeService.requestVoidBatchExecution(new EdhrRecordChangeRequestReqVO()
                    .setBatchExecutionId(batch.getId())
                    .setReasonCategory("ORDER_CANCELLED")
                    .setReasonText("表单中心审批通过回调不得把幂等键写入 BPM 任务字段。")
                    .setPassword("request-pass"));
            changeService.handleVoidBatchExecutionApprovalCallback("void-process-form-center-approved",
                    idempotencyKey, "APPROVED", "同意作废", ACTOR_ID);
        }

        MesProEdhrRecordChangeEventDO event = changeEventMapper.selectById(request.getId());
        assertEquals("EFFECTIVE", event.getChangeStatus());
        assertNull(event.getBpmTaskId());
    }

    @Test
    void voidBatchExecution_formCenterCancelledCallbackDoesNotPersistIdempotencyKeyAsBpmTaskId() {
        MesProEdhrBatchExecutionDO batch = insertClosedBatchExecution();
        when(processInstanceApi.createProcessInstance(eq(ACTOR_ID), any(BpmProcessInstanceCreateReqDTO.class)))
                .thenReturn("void-process-form-center-cancelled");
        String idempotencyKey = "EDHR-BATCH-VOID-" + batch.getId()
                + "-2b60817b-8424-11f1-8af4-00155d44772c";

        EdhrRecordChangeRespVO request;
        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            request = changeService.requestVoidBatchExecution(new EdhrRecordChangeRequestReqVO()
                    .setBatchExecutionId(batch.getId())
                    .setReasonCategory("ORDER_CANCELLED")
                    .setReasonText("表单中心撤回回调不得把幂等键写入 BPM 任务字段。")
                    .setPassword("request-pass"));
            changeService.handleVoidBatchExecutionApprovalCallback("void-process-form-center-cancelled",
                    idempotencyKey, "CANCELLED", "申请人撤回作废申请", ACTOR_ID);
        }

        MesProEdhrRecordChangeEventDO event = changeEventMapper.selectById(request.getId());
        assertEquals("REJECTED", event.getChangeStatus());
        assertNull(event.getBpmTaskId());
    }

    @Test
    void voidBatchExecution_effectiveBatchMovesOutOfExecutionPageAndIntoChangePage() {
        MesProEdhrBatchExecutionDO batch = insertClosedBatchExecution();
        when(processInstanceApi.createProcessInstance(eq(ACTOR_ID), any(BpmProcessInstanceCreateReqDTO.class)))
                .thenReturn("void-process-list-routing");

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            changeService.requestVoidBatchExecution(new EdhrRecordChangeRequestReqVO()
                    .setBatchExecutionId(batch.getId())
                    .setReasonCategory("ORDER_CANCELLED")
                    .setReasonText("已作废批次应从批次执行列表迁移到变更与异常列表。")
                    .setPassword("request-pass"));
            changeService.handleVoidBatchExecutionApprovalCallback("void-process-list-routing",
                    "event-approved", "APPROVED", "同意作废", ACTOR_ID);
        }

        EdhrBatchExecutionPageReqVO batchPageReq = new EdhrBatchExecutionPageReqVO();
        batchPageReq.setPageNo(1);
        batchPageReq.setPageSize(10);
        PageResult<MesProEdhrBatchExecutionDO> batchPage = batchExecutionMapper.selectPage(batchPageReq);
        assertEquals(0, batchPage.getList().stream()
                .filter(row -> batch.getId().equals(row.getId()))
                .count());
        batchPageReq.setStatus(60);
        PageResult<MesProEdhrBatchExecutionDO> voidedStatusBatchPage = batchExecutionMapper.selectPage(batchPageReq);
        assertEquals(0, voidedStatusBatchPage.getList().stream()
                .filter(row -> batch.getId().equals(row.getId()))
                .count());

        EdhrRecordChangePageReqVO changePageReq = new EdhrRecordChangePageReqVO();
        changePageReq.setPageNo(1);
        changePageReq.setPageSize(10);
        changePageReq.setChangeType("VOID");
        changePageReq.setTargetScope("BATCH");
        PageResult<MesProEdhrRecordChangeEventDO> changePage = changeEventMapper.selectPage(changePageReq);
        assertEquals(1, changePage.getList().stream()
                .filter(row -> batch.getId().equals(row.getBatchExecutionId())
                        && "EFFECTIVE".equals(row.getChangeStatus())
                        && "60".equals(row.getNewStatus()))
                .count());
    }

    @Test
    void voidBatchExecution_pendingRequestCanBeWithdrawnByRequester() {
        MesProEdhrBatchExecutionDO batch = insertActiveBatchExecution();
        when(processInstanceApi.createProcessInstance(eq(ACTOR_ID), any(BpmProcessInstanceCreateReqDTO.class)))
                .thenReturn("void-process-withdraw");

        EdhrRecordChangeRespVO request;
        EdhrRecordChangeRespVO withdrawn;
        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            request = changeService.requestVoidBatchExecution(new EdhrRecordChangeRequestReqVO()
                    .setBatchExecutionId(batch.getId())
                    .setReasonCategory("ORDER_CANCELLED")
                    .setReasonText("提交后只允许撤回作废申请，不允许继续正常操作。")
                    .setPassword("request-pass"));
            withdrawn = changeService.withdrawVoidBatchExecution(new EdhrRecordChangeApproveReqVO()
                    .setChangeEventId(request.getId())
                    .setComment("申请人撤回作废申请"));
        }

        assertEquals("VOID", withdrawn.getChangeType());
        assertEquals("BATCH", withdrawn.getTargetScope());
        assertEquals("REJECTED", withdrawn.getChangeStatus());
        assertEquals(batch.getId(), withdrawn.getBatchExecutionId());
        assertEquals(10, batchExecutionMapper.selectById(batch.getId()).getStatus());
        verify(processInstanceApi).cancelProcessInstance(ACTOR_ID, "void-process-withdraw", "申请人撤回作废申请");

        MesProEdhrRecordChangeEventDO event = changeEventMapper.selectById(request.getId());
        assertEquals("REJECTED", event.getChangeStatus());
        assertEquals(ACTOR_ID, event.getApprovedBy());
        assertNotNull(event.getApprovedAt());
        assertNull(event.getEffectiveAt());
    }

    @Test
    void releasePendingApproval_blocksRecordChangeRequests() {
        MesProEdhrBatchExecutionDO activeBatch = insertActiveBatchExecution();
        insertPendingReleaseTransaction(activeBatch.getId());
        MesProEdhrBatchExecutionDO closedBatch = insertClosedBatchExecution();
        insertPendingReleaseTransaction(closedBatch.getId());
        MesProBatchRecordExecutionDO execution = insertApprovedExecution();

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            assertServiceException(() -> changeService.requestVoidBatchExecution(new EdhrRecordChangeRequestReqVO()
                            .setBatchExecutionId(activeBatch.getId())
                            .setReasonCategory("ORDER_CANCELLED")
                            .setReasonText("放行审批中不得再提交作废申请。")
                            .setPassword("request-pass")),
                    PRO_EDHR_RELEASE_STATUS_INVALID);
            assertServiceException(() -> changeService.requestReopenBatch(new EdhrRecordChangeRequestReqVO()
                            .setBatchExecutionId(closedBatch.getId())
                            .setReasonCategory("PROCESS_DEVIATION")
                            .setReasonText("放行审批中不得再提交重开申请。")
                            .setPassword("request-pass")),
                    PRO_EDHR_RELEASE_STATUS_INVALID);
            assertServiceException(() -> changeService.requestSupplement(new EdhrRecordChangeRequestReqVO()
                            .setBatchExecutionId(activeBatch.getId())
                            .setExecutionId(execution.getId())
                            .setReasonCategory("MISSING_NOTE")
                            .setReasonText("放行审批中不得再提交补录申请。")
                            .setPassword("request-pass")),
                    PRO_EDHR_RELEASE_STATUS_INVALID);
        }
    }

    @Test
    void releasePendingApproval_goldenFingerAllowsBatchVoidRequest() {
        MesProEdhrBatchExecutionDO activeBatch = insertActiveBatchExecution();
        insertPendingReleaseTransaction(activeBatch.getId());
        when(goldenFingerPermissionService.hasGoldenFingerPermission(ACTOR_ID)).thenReturn(true);
        when(processInstanceApi.createProcessInstance(eq(ACTOR_ID), any(BpmProcessInstanceCreateReqDTO.class)))
                .thenReturn("void-process-golden-finger");

        EdhrRecordChangeRespVO response;
        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            response = changeService.requestVoidBatchExecution(new EdhrRecordChangeRequestReqVO()
                    .setBatchExecutionId(activeBatch.getId())
                    .setReasonCategory("ORDER_CANCELLED")
                    .setReasonText("金手指测试审批中作废申请。")
                    .setPassword("request-pass")
                    .setComment("golden finger void"));
        }

        assertEquals("VOID", response.getChangeType());
        assertEquals("SUBMITTED", response.getChangeStatus());
        assertEquals("void-process-golden-finger", response.getBpmProcessInstanceId());
        MesProEdhrRecordChangeEventDO event = changeEventMapper.selectById(response.getId());
        assertEquals(activeBatch.getId(), event.getBatchExecutionId());
        assertEquals(ACTOR_ID, event.getRequestedBy());
        assertNotNull(event.getRequestSignatureId());
    }

    @Test
    void voidBatchExecution_bpmProcessNotStartedFailsWithoutPersistingEvent() {
        MesProEdhrBatchExecutionDO batch = insertActiveBatchExecution();
        when(signatureService.recordSubmitSignature(eq(0L), eq("request-pass"), any()))
                .thenReturn(9401L);
        when(processInstanceApi.createProcessInstance(eq(ACTOR_ID), any(BpmProcessInstanceCreateReqDTO.class)))
                .thenReturn(" ");

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            BusinessApprovalException exception = assertThrows(BusinessApprovalException.class,
                    () -> changeService.requestVoidBatchExecution(new EdhrRecordChangeRequestReqVO()
                                    .setBatchExecutionId(batch.getId())
                                    .setReasonCategory("ORDER_CANCELLED")
                                    .setReasonText("BPM 未启动时不能创建作废事件。")
                                    .setPassword("request-pass")));
            assertEquals(BusinessApprovalErrorCode.BUSINESS_APPROVAL_PROCESS_NOT_STARTED,
                    exception.getErrorCode());
        }

        Long count = changeEventMapper.selectCount(new LambdaQueryWrapperX<MesProEdhrRecordChangeEventDO>()
                .eq(MesProEdhrRecordChangeEventDO::getBatchExecutionId, batch.getId())
                .eq(MesProEdhrRecordChangeEventDO::getChangeType, "VOID"));
        assertEquals(0L, count);
    }

    @Test
    void reopenExecution_approvedExecution_marksExecutionReopenedAndArchiveSuperseded() {
        MesProBatchRecordExecutionDO execution = insertApprovedExecution();
        MesProBatchRecordExecutionArchiveDO archive = insertSealedArchive(execution.getId());
        when(signatureService.recordSubmitSignature(eq(execution.getId()), eq("request-pass"), any()))
                .thenReturn(9151L);
        when(signatureService.recordApprovalSignature(any(MesProBatchRecordExecutionApprovalSignatureCommand.class)))
                .thenReturn(9152L);

        EdhrRecordChangeRespVO request;
        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            request = changeService.requestReopenExecution(new EdhrRecordChangeRequestReqVO()
                    .setExecutionId(execution.getId())
                    .setReasonCategory("PROCESS_DEVIATION")
                    .setReasonText("已批准执行记录需要按受控流程重开。")
                    .setPassword("request-pass")
                    .setComment("request reopen execution"));
            changeService.approveReopenExecution(new EdhrRecordChangeApproveReqVO()
                    .setChangeEventId(request.getId())
                    .setPassword("approve-pass")
                    .setComment("approve reopen execution"));
        }

        MesProBatchRecordExecutionDO updatedExecution = executionMapper.selectById(execution.getId());
        assertEquals(5, updatedExecution.getStatus());
        assertEquals(request.getId(), updatedExecution.getReopenedByChangeEventId());
        MesProBatchRecordExecutionArchiveDO updatedArchive = archiveMapper.selectById(archive.getId());
        assertEquals(Boolean.FALSE, updatedArchive.getArchiveValidFlag());
        assertEquals("SUPERSEDED", updatedArchive.getArchiveValidStatus());
        assertEquals(request.getId(), updatedArchive.getInvalidatedByChangeEventId());
        MesProEdhrRecordChangeEventDO event = changeEventMapper.selectById(request.getId());
        assertEquals("REOPEN", event.getChangeType());
        assertEquals("EXECUTION", event.getTargetScope());
        assertEquals("EFFECTIVE", event.getChangeStatus());
        assertEquals("3", event.getPreviousStatus());
        assertEquals("5", event.getNewStatus());
        assertEquals(9151L, event.getRequestSignatureId());
        assertEquals(9152L, event.getApprovalSignatureId());
        assertEquals(archive.getSha256(), event.getPreviousArchiveHash());
        assertEquals(archive.getSha256(), event.getNewArchiveHash());
    }

    @Test
    void supplement_approvedExecution_createsSignedEffectiveEventWithoutChangingArchive() {
        MesProBatchRecordExecutionDO execution = insertApprovedExecution();
        MesProBatchRecordExecutionArchiveDO archive = insertSealedArchive(execution.getId());
        when(signatureService.recordSubmitSignature(eq(execution.getId()), eq("request-pass"), any()))
                .thenReturn(9201L);
        when(signatureService.recordApprovalSignature(any(MesProBatchRecordExecutionApprovalSignatureCommand.class)))
                .thenReturn(9202L);

        EdhrRecordChangeRespVO request;
        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            request = changeService.requestSupplement(new EdhrRecordChangeRequestReqVO()
                    .setExecutionId(execution.getId())
                    .setReasonCategory("MISSING_NOTE")
                    .setReasonText("批准后发现需要追加补录说明。")
                    .setPassword("request-pass")
                    .setComment("request supplement"));
            changeService.approveSupplement(new EdhrRecordChangeApproveReqVO()
                    .setChangeEventId(request.getId())
                    .setPassword("approve-pass")
                    .setComment("approve supplement"));
        }

        MesProBatchRecordExecutionDO updatedExecution = executionMapper.selectById(execution.getId());
        assertEquals(3, updatedExecution.getStatus());
        MesProBatchRecordExecutionArchiveDO updatedArchive = archiveMapper.selectById(archive.getId());
        assertEquals(Boolean.TRUE, updatedArchive.getArchiveValidFlag());
        assertEquals("VALID", updatedArchive.getArchiveValidStatus());
        MesProEdhrRecordChangeEventDO event = changeEventMapper.selectById(request.getId());
        assertEquals("SUPPLEMENT", event.getChangeType());
        assertEquals("EXECUTION", event.getTargetScope());
        assertEquals("EFFECTIVE", event.getChangeStatus());
        assertEquals("3", event.getPreviousStatus());
        assertEquals("3", event.getNewStatus());
        assertEquals(9201L, event.getRequestSignatureId());
        assertEquals(9202L, event.getApprovalSignatureId());
        assertEquals(archive.getSha256(), event.getPreviousArchiveHash());
        assertEquals(archive.getSha256(), event.getNewArchiveHash());
    }

    @Test
    void supplementDraft_submit_approve_recordsDraftSubmittedEffectiveFlow() {
        MesProBatchRecordExecutionDO execution = insertApprovedExecution();
        MesProBatchRecordExecutionArchiveDO archive = insertSealedArchive(execution.getId());
        when(signatureService.recordSubmitSignature(eq(execution.getId()), eq("submit-pass"), any()))
                .thenReturn(9251L);
        when(signatureService.recordApprovalSignature(any(MesProBatchRecordExecutionApprovalSignatureCommand.class)))
                .thenReturn(9252L);

        EdhrRecordChangeRespVO draft;
        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            draft = changeService.saveSupplementDraft(new EdhrRecordChangeRequestReqVO()
                    .setExecutionId(execution.getId())
                    .setReasonCategory("MISSING_NOTE")
                    .setReasonText("批准后发现需要先保存补录草稿。")
                    .setComment("draft supplement"));
            EdhrRecordChangeRespVO submitted = changeService.submitSupplement(new EdhrRecordChangeApproveReqVO()
                    .setChangeEventId(draft.getId())
                    .setPassword("submit-pass")
                    .setComment("submit supplement"));
            assertEquals("SUBMITTED", submitted.getChangeStatus());
            assertEquals(9251L, submitted.getRequestSignatureId());
            changeService.approveSupplement(new EdhrRecordChangeApproveReqVO()
                    .setChangeEventId(draft.getId())
                    .setPassword("approve-pass")
                    .setComment("approve supplement draft"));
        }

        MesProBatchRecordExecutionDO updatedExecution = executionMapper.selectById(execution.getId());
        assertEquals(3, updatedExecution.getStatus());
        MesProBatchRecordExecutionArchiveDO updatedArchive = archiveMapper.selectById(archive.getId());
        assertEquals(Boolean.TRUE, updatedArchive.getArchiveValidFlag());
        assertEquals("VALID", updatedArchive.getArchiveValidStatus());
        MesProEdhrRecordChangeEventDO event = changeEventMapper.selectById(draft.getId());
        assertEquals("SUPPLEMENT", event.getChangeType());
        assertEquals("EXECUTION", event.getTargetScope());
        assertEquals("EFFECTIVE", event.getChangeStatus());
        assertEquals("3", event.getPreviousStatus());
        assertEquals("3", event.getNewStatus());
        assertEquals(9251L, event.getRequestSignatureId());
        assertEquals(9252L, event.getApprovalSignatureId());
        assertEquals(archive.getSha256(), event.getPreviousArchiveHash());
        assertEquals(archive.getSha256(), event.getNewArchiveHash());
    }

    private MesProBatchRecordExecutionDO insertApprovedExecution() {
        MesProBatchRecordExecutionDO execution = MesProBatchRecordExecutionDO.builder()
                .executionCode("EXE-" + System.nanoTime())
                .templateId(10L)
                .templateCode("TPL-EDHR")
                .templateName("EDHR")
                .workOrderId(20L)
                .workOrderCode("MO-VOID")
                .batchCode("BATCH-VOID")
                .status(3)
                .sheetLayoutJson("{\"sheet\":\"main\"}")
                .executionSnapshotJson("{\"source\":\"approved\"}")
                .cellValuesJson("{\"source\":\"approved\"}")
                .cellValuesHash(HASH_64)
                .fieldAuditRevision(0L)
                .fieldAuditHeadHash(HASH_64)
                .submittedBy(ACTOR_ID)
                .submittedAt(LocalDateTime.now().minusHours(2))
                .approvedBy(ACTOR_ID)
                .approvedAt(LocalDateTime.now().minusHours(1))
                .build();
        executionMapper.insert(execution);
        return execution;
    }

    private MesProBatchRecordExecutionArchiveDO insertSealedArchive(Long executionId) {
        MesProBatchRecordExecutionArchiveDO archive = MesProBatchRecordExecutionArchiveDO.builder()
                .executionId(executionId)
                .archiveCode("ARCH-" + System.nanoTime())
                .archiveVersion(1)
                .artifactType("PDF")
                .archiveStatus("SEALED")
                .fileId(7001L)
                .fileName("edhr.pdf")
                .contentType("application/pdf")
                .fileSize(100L)
                .sha256(HASH_64)
                .generatedBy(ACTOR_ID)
                .generatedAt(LocalDateTime.now().minusMinutes(30))
                .sealedBy(ACTOR_ID)
                .sealedAt(LocalDateTime.now().minusMinutes(20))
                .archiveValidFlag(Boolean.TRUE)
                .archiveValidStatus("VALID")
                .build();
        archiveMapper.insert(archive);
        return archive;
    }

    private MesProEdhrBatchExecutionDO insertArchivedBatchExecution() {
        MesProEdhrBatchExecutionDO batch = MesProEdhrBatchExecutionDO.builder()
                .batchExecutionCode("BATCH-EXE-" + System.nanoTime())
                .workOrderId(30L)
                .workOrderCode("MO-REOPEN")
                .batchCode("BATCH-REOPEN")
                .routeId(40L)
                .routeCode("ROUTE-REOPEN")
                .status(40)
                .taskTotal(2)
                .taskApprovedCount(2)
                .blockedCount(0)
                .aggregateHash(HASH_64)
                .closedBy(ACTOR_ID)
                .closedAt(LocalDateTime.now().minusHours(1))
                .build();
        batchExecutionMapper.insert(batch);
        return batch;
    }

    private MesProEdhrBatchExecutionDO insertClosedBatchExecution() {
        MesProEdhrBatchExecutionDO batch = insertArchivedBatchExecution();
        batchExecutionMapper.updateById(new MesProEdhrBatchExecutionDO()
                .setId(batch.getId())
                .setStatus(30));
        return batchExecutionMapper.selectById(batch.getId());
    }

    private MesProEdhrBatchExecutionDO insertRejectedBatchExecution() {
        MesProEdhrBatchExecutionDO batch = insertClosedBatchExecution();
        MesProEdhrBatchExecutionSignatureDO signature = new MesProEdhrBatchExecutionSignatureDO()
                .setBatchExecutionId(batch.getId())
                .setActorId(ACTOR_ID)
                .setActorName(String.valueOf(ACTOR_ID))
                .setActionType("QUALITY_REJECT")
                .setSignatureMode("PASSWORD")
                .setPasswordVerified(Boolean.TRUE)
                .setComment("质量复核前终态拒收。")
                .setSignedAt(LocalDateTime.now().minusMinutes(40))
                .setSignatureTimeMode("SERVER_TIME")
                .setAggregateHash(HASH_64);
        batchSignatureMapper.insert(signature);
        LocalDateTime rejectedAt = LocalDateTime.now().minusMinutes(30);
        batchExecutionMapper.updateById(new MesProEdhrBatchExecutionDO()
                .setId(batch.getId())
                .setStatus(50)
                .setRejectSignatureId(signature.getId())
                .setRejectedBy(ACTOR_ID)
                .setRejectedAt(rejectedAt)
                .setRejectReason("质量复核前终态拒收。"));
        return batchExecutionMapper.selectById(batch.getId());
    }

    private MesProEdhrBatchExecutionDO insertActiveBatchExecution() {
        MesProEdhrBatchExecutionDO batch = insertArchivedBatchExecution();
        batchExecutionMapper.updateById(new MesProEdhrBatchExecutionDO()
                .setId(batch.getId())
                .setStatus(10));
        return batchExecutionMapper.selectById(batch.getId());
    }

    private MesProEdhrBatchExecutionArchiveDO insertSealedBatchArchive(Long batchExecutionId) {
        MesProEdhrBatchExecutionArchiveDO archive = MesProEdhrBatchExecutionArchiveDO.builder()
                .batchExecutionId(batchExecutionId)
                .artifactType("FINAL_PDF")
                .archiveVersion(1)
                .archiveStatus("SEALED")
                .fileName("batch-edhr.pdf")
                .contentType("application/pdf")
                .fileSize(200L)
                .filePath("mes/edhr/batch.pdf")
                .contentHash(HASH_64)
                .sourceManifestJson("{\"batchExecutionId\":" + batchExecutionId + "}")
                .generatedBy(ACTOR_ID)
                .generatedAt(LocalDateTime.now().minusMinutes(30))
                .sealedSignatureId(8801L)
                .archiveValidFlag(Boolean.TRUE)
                .archiveValidStatus("VALID")
                .build();
        batchArchiveMapper.insert(archive);
        return archive;
    }

    private void insertPendingReleaseTransaction(Long batchExecutionId) {
        releaseTransactionMapper.insert(new MesProEdhrReleaseTransactionDO()
                .setBatchExecutionId(batchExecutionId)
                .setReleaseCode("REL-PENDING-" + batchExecutionId)
                .setReleaseStatus(MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL)
                .setBlockingCheckCount(0)
                .setFailedCheckCount(0)
                .setRequiredCheckCount(6));
    }

    private BusinessApprovalRequest submitBatchVoidBusinessApprovalInTest(InvocationOnMock invocation) {
        BusinessApprovalContext context = invocation.getArgument(0);
        Long requestId = 4101L;
        BpmProcessInstanceCreateReqDTO bpmReq = new BpmProcessInstanceCreateReqDTO();
        bpmReq.setProcessDefinitionKey(MesProEdhrRecordChangeServiceImpl.BATCH_EXECUTION_VOID_PROCESS_DEFINITION_KEY);
        bpmReq.setBusinessKey("BUSINESS_APPROVAL:" + requestId);
        bpmReq.setVariables(buildBusinessApprovalBpmVariables(context, requestId));
        bpmReq.setStartUserSelectAssignees(context.getStartUserSelectAssignees());
        String processInstanceId = processInstanceApi.createProcessInstance(context.getApplicantUserId(), bpmReq);
        if (processInstanceId == null || processInstanceId.isBlank()) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_PROCESS_NOT_STARTED,
                    "BPM process instance was not started for eDHR batch void test");
        }
        BusinessApprovalRequest request = BusinessApprovalRequest.builder()
                .requestId(requestId)
                .tenantId(context.getTenantId())
                .policyMode(BusinessApprovalPolicyMode.BPM_REQUIRED)
                .processDefinitionKey(MesProEdhrRecordChangeServiceImpl.BATCH_EXECUTION_VOID_PROCESS_DEFINITION_KEY)
                .effectExecutorCode(MesProEdhrBatchVoidFormEffectExecutor.EXECUTOR_CODE)
                .status(BusinessApprovalRequestStatus.PENDING_BPM)
                .context(context)
                .processInstanceId(processInstanceId)
                .build();
        changeService.requestPlatformVoidBatchExecution(toBatchVoidRequest(context), processInstanceId);
        return request;
    }

    private Map<String, Object> buildBusinessApprovalBpmVariables(BusinessApprovalContext context, Long requestId) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("tenantId", context.getTenantId());
        variables.put("businessType", context.getVariables().get("businessType"));
        variables.put("approvalRequestId", requestId);
        variables.put("dataDomain", context.getDataDomain());
        variables.put("systemCode", context.getSystemCode());
        variables.put("objectType", context.getObjectType());
        variables.put("objectId", context.getObjectId());
        variables.put("objectVersion", context.getObjectVersion());
        variables.put("actionCode", context.getActionCode());
        variables.put("objectState", context.getObjectState());
        variables.put("businessKey", context.getObjectType() + ":" + context.getObjectId() + ":"
                + context.getActionCode());
        variables.put("reason", context.getReason());
        context.getVariables().forEach(variables::putIfAbsent);
        return variables;
    }

    private EdhrRecordChangeRequestReqVO toBatchVoidRequest(BusinessApprovalContext context) {
        return new EdhrRecordChangeRequestReqVO()
                .setBatchExecutionId(Long.valueOf(context.getObjectId()))
                .setReasonCategory(String.valueOf(context.getVariables().get("reasonCategory")))
                .setReasonText(String.valueOf(context.getVariables().get("reasonText")))
                .setPassword(String.valueOf(context.getTransientVariables().get("password")))
                .setComment((String) context.getVariables().get("comment"));
    }

    private MockedStatic<SecurityFrameworkUtils> mockLoginUser() {
        MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class);
        security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(ACTOR_ID);
        return security;
    }
}
