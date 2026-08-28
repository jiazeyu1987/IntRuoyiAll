package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.module.bpm.dal.dataobject.signature.BpmApprovalSignatureRecordDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.signature.BpmApprovalSignatureRecordMapper;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormInstanceStatus;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseApproveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleasePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleasePrecheckReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseRejectReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseWithdrawReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionAttachmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionSignatureDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionSignatureDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseCheckItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseDecisionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionAttachmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionSignatureMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionSignatureMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseCheckItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseDecisionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleMapper;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesProductionReportManagementSummaryService;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseManagerApprovalService;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.MesReleaseUpstreamStatePort;
import cn.iocoder.yudao.module.mes.productionrelease.core.IndependentBatchPrerequisiteReceipt;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseAuthoritativeContextPort;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFinalizationCommand;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFinalizationEvidence;
import cn.iocoder.yudao.module.mes.productionrelease.core.CompletionBackfillReceipt;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseMaterialGateReceipt;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseOrigin;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerException;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomLongId;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_RELEASE_PRECHECK_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_RELEASE_SIGNOFF_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_WORK_TASK_CANDIDATE_POOL_EMPTY;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.USER_PASSWORD_FAILED;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import({MesProEdhrReleaseServiceImpl.class, MesProEdhrCandidateResolver.class})
class MesProEdhrReleaseServiceImplTest extends BaseDbUnitTest {

    @Resource
    private MesProEdhrReleaseService releaseService;
    @Resource
    private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Resource
    private MesProEdhrBatchExecutionTaskMapper batchTaskMapper;
    @Resource
    private MesProBatchRecordExecutionMapper executionMapper;
    @Resource
    private MesProBatchRecordExecutionSignatureMapper signatureMapper;
    @Resource
    private MesProBatchRecordExecutionAttachmentMapper attachmentMapper;
    @Resource
    private MesProEdhrReleaseCheckItemMapper releaseCheckItemMapper;
    @Resource
    private MesProEdhrReleaseTransactionMapper releaseTransactionMapper;
    @Resource
    private MesProEdhrReleaseDecisionMapper releaseDecisionMapper;
    @Resource
    private MesProEdhrBatchExecutionSignatureMapper batchSignatureMapper;
    @Resource
    private MesProEdhrWorkTaskAssignmentRuleMapper assignmentRuleMapper;
    @MockitoBean
    private AdminUserApi adminUserApi;
    @MockitoBean
    private PermissionApi permissionApi;
    @MockitoBean
    private DeptApi deptApi;
    @MockitoBean
    private MesProEdhrWorkTaskService workTaskService;
    @MockitoBean
    private MesProEdhrOperationAuditService operationAuditService;
    @MockitoBean
    private FormActionInstanceMapper formActionInstanceMapper;
    @MockitoBean
    private BpmApprovalSignatureRecordMapper approvalSignatureRecordMapper;
    @MockitoBean
    private MesProEdhrReleaseDossierRequirementSettingService dossierRequirementSettingService;
    @MockitoBean
    private MesOrderReleaseCompletenessService releaseCompletenessService;
    @MockitoBean
    private MesProductionReportManagementSummaryService reportManagementSummaryService;
    @MockitoBean
    private MesProductionReleaseManagerApprovalService managerApprovalService;
    @MockitoBean
    private MesReleaseUpstreamStatePort upstreamStatePort;
    @MockitoBean
    private MesReleaseAuthoritativeContextPort authoritativeContextPort;
    @MockitoBean
    private MesProEdhrFourMaterialGateService fourMaterialGateService;

    @BeforeEach
    void setUpDossierRequirementDefaults() {
        mockDossierRequirementState(false, false, false, false, "dossier-hash-all-false");
        mockReleaseCompletenessSourcesAsNotApplicable();
        when(fourMaterialGateService.evaluate(any())).thenAnswer(invocation ->
                readyMaterialGate(invocation.getArgument(0)));
        when(fourMaterialGateService.requireMaterialsReady(any())).thenAnswer(invocation ->
                readyMaterialGate(invocation.getArgument(0)));
        when(adminUserApi.getUser(any())).thenAnswer(invocation -> {
            Long userId = invocation.getArgument(0);
            return user(userId, "用户" + userId);
        });
        when(workTaskService.createReleaseApprovalTaskAfterSubmit(any(), any()))
                .thenAnswer(invocation -> releaseApprovalTask(
                        ((MesProEdhrReleaseTransactionDO) invocation.getArgument(0)).getId(), 9900L));
        when(authoritativeContextPort.require(any())).thenAnswer(invocation -> {
            MesReleaseFinalizationCommand command = invocation.getArgument(0);
            return new MesReleaseFinalizationEvidence()
                    .setIndependentPrerequisiteReceipt(command.getIndependentPrerequisiteReceipt())
                    .setMaterialGateReceipt(command.getMaterialGateReceipt())
                    .setCompletionBackfillReceipt(command.getOrigin() == MesReleaseOrigin.ACTIVE_ORDER
                            ? new CompletionBackfillReceipt()
                            .setReceiptId(command.getCompletionBackfillReceiptId())
                            .setActiveOrderId(command.getActiveOrderId())
                            .setWorkOrderId(command.getWorkOrderId())
                            .setPickListBindingId(command.getPickListBindingId())
                            .setPickListId(command.getPickListId())
                            .setSourceSnapshotHash(command.getSourceSnapshotHash())
                            .setBindingVersion(1)
                            .setCompletionVersion(1)
                            .setCompletionTransactionId("completion-tx")
                            .setCompletionEventId(command.getCompletionEventId())
                            .setBatchRecordId(1L)
                            .setProcessInspectionId(2L)
                            .setHasActualLoss(false)
                            .setLossDecision("NO_LOSS")
                            .setLossReportStatus("NOT_REQUIRED")
                            .setReceiptHash("completion-receipt")
                            .setIdempotencyKey("completion-idem")
                            .setAuditEventId("completion-audit")
                            .setStatus(CompletionBackfillReceipt.STATUS_BACKFILL_SUCCEEDED)
                            .setIssuedAt(LocalDateTime.now())
                            : null);
        });
    }

    @Test
    void precheckFailsWhenOrdinaryProcessMissingSubmitSignature() {
        MesProEdhrBatchExecutionDO batch = insertClosedBatch("BATCH-REL-NO-SUBMIT-SIGNATURE");
        MesProEdhrBatchExecutionTaskDO task = insertApprovedOrdinaryTask(batch.getId(), 7101L);
        insertCompletedExecution(task.getExecutionId(), false);

        MesProEdhrReleaseRespVO result = precheckAsUser(10001L, batch.getId());

        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_FAIL, result.getDhrStatus());
    }

    @Test
    void precheckDhrCompletenessPassesWithoutOrdinaryReviewApproveTasksWhenFillSignedAndClosed() {
        MesProEdhrBatchExecutionDO batch = insertClosedBatch("BATCH-REL-SUBMIT-SIGNED");
        MesProEdhrBatchExecutionTaskDO task = insertApprovedOrdinaryTask(batch.getId(), 7201L);
        insertCompletedExecution(task.getExecutionId(), true);

        MesProEdhrReleaseRespVO result = precheckAsUser(10001L, batch.getId());

        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS, result.getDhrStatus());
    }

    @Test
    void precheckDhrCompletenessPassesWhenBatchSharedFormCenterRouteTaskIsEffective() {
        MesProEdhrBatchExecutionDO batch = insertClosedBatch("BATCH-REL-FORMCENTER-SHARED-EFFECTIVE");
        MesProEdhrBatchExecutionTaskDO firstSharedTask = insertApprovedFormCenterRouteTask(batch.getId(), 83001L,
                "LOSS_SHARED", 0);
        MesProEdhrBatchExecutionTaskDO laterSharedTask = insertApprovedFormCenterRouteTask(batch.getId(), 83001L,
                "LOSS_SHARED", 1);
        batchExecutionMapper.updateById(new MesProEdhrBatchExecutionDO()
                .setId(batch.getId())
                .setTaskTotal(2)
                .setTaskApprovedCount(2));
        when(formActionInstanceMapper.selectById(83001L))
                .thenReturn(FormActionInstanceDO.builder()
                        .id(83001L)
                        .instanceCode("FCI-REL-FORMCENTER-SHARED")
                        .tenantId(1L)
                        .policyId(1L)
                        .applicantUserId(10001L)
                        .status(FormInstanceStatus.EFFECTIVE.name())
                        .dataDomain("MES")
                        .systemCode("MES")
                        .objectType("EDHR_ROUTE_FORM")
                        .objectId(String.valueOf(firstSharedTask.getId()))
                        .objectVersion("9001")
                        .actionCode("EDHR_RF_9001_LOSS_SHARED")
                        .objectState("ACTIVE")
                        .idempotencyKey("EDHR_ROUTE_FORM:" + batch.getId() + ":" + firstSharedTask.getId() + ":LOSS_SHARED")
                        .businessContextJson("{}")
                        .formDataJson("{}")
                        .build());

        MesProEdhrReleaseRespVO result = precheckAsUser(10001L, batch.getId());

        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS, result.getDhrStatus());
        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_PRECHECK_PASSED, result.getReleaseStatus());
    }

    @Test
    void dossierRequirementEvidenceBlocksDuplicateSpecialNodeTasks() throws Exception {
        MesProEdhrBatchExecutionDO batch = insertClosedBatch("BATCH-REL-DUP-SPECIAL-NODE");
        insertSpecialTask(batch.getId(),
                MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_INCOMING_INSPECTION_REPORT,
                MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED);
        insertSpecialTask(batch.getId(),
                MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_INCOMING_INSPECTION_REPORT,
                MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED);

        Field mapperField = MesProEdhrReleaseServiceImpl.class.getDeclaredField("batchExecutionTaskMapper");
        mapperField.setAccessible(true);
        mapperField.set(releaseService, batchTaskMapper);

        Method method = MesProEdhrReleaseServiceImpl.class.getDeclaredMethod(
                "resolveDossierRequirementEvidence", Long.class, String.class, String.class);
        method.setAccessible(true);
        Object evidence = method.invoke(releaseService, batch.getId(),
                MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_INCOMING_INSPECTION_REPORT,
                "来料检报告");
        Method failureReason = evidence.getClass().getDeclaredMethod("failureReason");
        failureReason.setAccessible(true);

        assertEquals("来料检报告特殊节点存在重复任务，无法确认放行资料已上传",
                failureReason.invoke(evidence));
    }

    @Test
    void precheckDhrCompletenessIgnoresNotIntegratedSpecialReportTasksWhenOrdinaryEvidenceComplete() {
        MesProEdhrBatchExecutionDO batch = insertReadyToCloseBatch("BATCH-REL-SPECIAL-NODES-NOT-INTEGRATED");
        MesProEdhrBatchExecutionTaskDO task = insertApprovedOrdinaryTask(batch.getId(), 7251L);
        insertCompletedExecution(task.getExecutionId(), true);
        insertWaitingSpecialTask(batch.getId(), "INCOMING_INSPECTION_REPORT");
        insertWaitingSpecialTask(batch.getId(), "STERILIZATION_REPORT");
        batchExecutionMapper.updateById(new MesProEdhrBatchExecutionDO()
                .setId(batch.getId())
                .setTaskTotal(3)
                .setTaskApprovedCount(1));

        MesProEdhrReleaseRespVO result = precheckAsUser(10001L, batch.getId());

        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS, result.getDhrStatus());
        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_PRECHECK_PASSED, result.getReleaseStatus());
    }

    @Test
    void legacyDefaultOffCannotDisableFourMaterialGate() {
        MesProEdhrBatchExecutionDO batch = insertReadyToCloseBatch("BATCH-REL-DOSSIER-DEFAULT-OFF");
        MesProEdhrBatchExecutionTaskDO task = insertApprovedOrdinaryTask(batch.getId(), 7261L);
        insertCompletedExecution(task.getExecutionId(), true);
        insertWaitingSpecialTask(batch.getId(), MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_INCOMING_INSPECTION_REPORT);
        insertWaitingSpecialTask(batch.getId(), MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_STERILIZATION_REPORT);
        insertWaitingSpecialTask(batch.getId(),
                MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_FINISHED_PRODUCT_INSPECTION_REPORT);
        insertWaitingSpecialTask(batch.getId(),
                MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_FINISHED_PRODUCT_INSPECTION_RECORD);
        batchExecutionMapper.updateById(new MesProEdhrBatchExecutionDO()
                .setId(batch.getId())
                .setTaskTotal(5)
                .setTaskApprovedCount(1));

        when(fourMaterialGateService.evaluate(batch.getId())).thenReturn(new MesProEdhrFourMaterialGateResult(
                MesProEdhrFourMaterialGateResult.STATUS_MATERIALS_PENDING, false, null, List.of()));
        MesProEdhrReleaseRespVO result = precheckAsUser(10001L, batch.getId());

        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_PRECHECK_FAILED, result.getReleaseStatus());
        assertEquals(4, result.getFailedCheckCount());
        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_BLOCKER,
                selectCheckItem(result.getReleaseTransactionId(),
                        MesProEdhrReleaseServiceImpl.CHECK_DOSSIER_INCOMING_INSPECTION_REPORT).getCheckResult());
        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_BLOCKER,
                selectCheckItem(result.getReleaseTransactionId(),
                        MesProEdhrReleaseServiceImpl.CHECK_DOSSIER_STERILIZATION_REPORT).getCheckResult());
        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_BLOCKER,
                selectCheckItem(result.getReleaseTransactionId(),
                        MesProEdhrReleaseServiceImpl.CHECK_DOSSIER_FINISHED_PRODUCT_INSPECTION_REPORT).getCheckResult());
        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_BLOCKER,
                selectCheckItem(result.getReleaseTransactionId(),
                        MesProEdhrReleaseServiceImpl.CHECK_DOSSIER_FINISHED_PRODUCT_INSPECTION_RECORD).getCheckResult());
    }

    @Test
    void precheckDossierRequirementFailsForEachEnabledSpecialNodeWithoutSavedAttachment() {
        List<DossierRequirementCase> cases = List.of(
                new DossierRequirementCase(MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_INCOMING_INSPECTION_REPORT,
                        MesProEdhrReleaseServiceImpl.CHECK_DOSSIER_INCOMING_INSPECTION_REPORT,
                        true, false, false, false),
                new DossierRequirementCase(MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_STERILIZATION_REPORT,
                        MesProEdhrReleaseServiceImpl.CHECK_DOSSIER_STERILIZATION_REPORT,
                        false, true, false, false),
                new DossierRequirementCase(
                        MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_FINISHED_PRODUCT_INSPECTION_REPORT,
                        MesProEdhrReleaseServiceImpl.CHECK_DOSSIER_FINISHED_PRODUCT_INSPECTION_REPORT,
                        false, false, true, false),
                new DossierRequirementCase(
                        MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_FINISHED_PRODUCT_INSPECTION_RECORD,
                        MesProEdhrReleaseServiceImpl.CHECK_DOSSIER_FINISHED_PRODUCT_INSPECTION_RECORD,
                        false, false, false, true));

        for (DossierRequirementCase requirementCase : cases) {
            mockDossierRequirementState(requirementCase.incomingRequired(), requirementCase.sterilizationRequired(),
                    requirementCase.finishedReportRequired(), requirementCase.finishedRecordRequired(),
                    "dossier-hash-" + requirementCase.nodeType());
            MesProEdhrBatchExecutionDO batch = insertReadyToCloseBatch("BATCH-REL-DOSSIER-FAIL-"
                    + requirementCase.nodeType());
            MesProEdhrBatchExecutionTaskDO task = insertApprovedOrdinaryTask(batch.getId(), randomLongId());
            insertCompletedExecution(task.getExecutionId(), true);
            insertWaitingSpecialTask(batch.getId(), requirementCase.nodeType());
            batchExecutionMapper.updateById(new MesProEdhrBatchExecutionDO()
                    .setId(batch.getId())
                    .setTaskTotal(2)
                    .setTaskApprovedCount(1));
            when(fourMaterialGateService.evaluate(batch.getId())).thenReturn(new MesProEdhrFourMaterialGateResult(
                    MesProEdhrFourMaterialGateResult.STATUS_MATERIALS_PENDING, false, null, List.of()));

            MesProEdhrReleaseRespVO result = precheckAsUser(10001L, batch.getId());
            MesProEdhrReleaseCheckItemDO item =
                    selectCheckItem(result.getReleaseTransactionId(), requirementCase.checkCode());

            assertEquals(MesProEdhrReleaseServiceImpl.STATUS_PRECHECK_FAILED, result.getReleaseStatus(),
                    requirementCase.nodeType());
            assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_BLOCKER, item.getCheckResult(),
                    requirementCase.nodeType());
            assertEquals("BLOCKER", item.getSeverity(), requirementCase.nodeType());
            assertTrue(item.getFailureReason().contains("MATERIALS_PENDING"), requirementCase.nodeType());
        }
    }

    @Test
    void precheckMapsFourMaterialReadyGateToPassItems() {
        mockDossierRequirementState(true, false, false, false, "dossier-hash-incoming-required");
        MesProEdhrBatchExecutionDO batch = insertReadyToCloseBatch("BATCH-REL-DOSSIER-PASS-INCOMING");
        MesProEdhrBatchExecutionTaskDO ordinaryTask = insertApprovedOrdinaryTask(batch.getId(), 7271L);
        insertCompletedExecution(ordinaryTask.getExecutionId(), true);
        MesProEdhrBatchExecutionTaskDO specialTask = insertSpecialTask(batch.getId(),
                MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_INCOMING_INSPECTION_REPORT,
                MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED);
        insertSavedSpecialNodeAttachment(specialTask, "ADD", "SPECIAL_NODE:9001",
                "eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee");
        batchExecutionMapper.updateById(new MesProEdhrBatchExecutionDO()
                .setId(batch.getId())
                .setTaskTotal(2)
                .setTaskApprovedCount(2));

        MesProEdhrReleaseRespVO result = precheckAsUser(10001L, batch.getId());
        MesProEdhrReleaseCheckItemDO item = selectCheckItem(result.getReleaseTransactionId(),
                MesProEdhrReleaseServiceImpl.CHECK_DOSSIER_INCOMING_INSPECTION_REPORT);

        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_PRECHECK_PASSED, result.getReleaseStatus());
        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS, item.getCheckResult());
        assertEquals("INFO", item.getSeverity());
    }

    @Test
    void submitIgnoresLegacyDossierRequirementConfigChangeAfterPrecheck() {
        mockDossierRequirementState(false, false, false, false, "dossier-hash-before-submit");
        MesProEdhrBatchExecutionDO batch = insertClosedBatch("BATCH-REL-DOSSIER-HASH-STALE");
        insertRouteReleaseOwnerRule(batch.getRouteId(), 10001L);
        MesProEdhrBatchExecutionTaskDO task = insertApprovedOrdinaryTask(batch.getId(), 7281L);
        insertCompletedExecution(task.getExecutionId(), true);
        MesProEdhrReleaseRespVO precheck = precheckAsUser(10001L, batch.getId());
        mockDossierRequirementState(true, false, false, false, "dossier-hash-after-submit");

        MesProEdhrReleaseRespVO submitted;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10001L);
            submitted = releaseService.submit(new MesProEdhrReleaseSubmitReqVO()
                    .setReleaseTransactionId(precheck.getReleaseTransactionId())
                    .setIdempotencyKey("submit-dossier-hash-stale")
                    .setPassword("owner-sign-secret"));
        }

        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL, submitted.getReleaseStatus());
        verify(adminUserApi).validatePassword(10001L, "owner-sign-secret");
    }

    @Test
    void submitCreatesPendingApprovalWhenOwnerSignsAndDhrPasses() {
        MesProEdhrBatchExecutionDO batch = insertClosedBatch("BATCH-REL-DHR-PASS-SOURCE-NA");
        insertRouteReleaseOwnerRule(batch.getRouteId(), 10001L);
        MesProEdhrBatchExecutionTaskDO task = insertApprovedOrdinaryTask(batch.getId(), 7301L);
        insertCompletedExecution(task.getExecutionId(), true);

        MesProEdhrReleaseRespVO precheck = precheckAsUser(10001L, batch.getId());

        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_PRECHECK_PASSED, precheck.getReleaseStatus());
        assertEquals(0, precheck.getFailedCheckCount());
        assertEquals(0, precheck.getBlockingCheckCount());
        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_NOT_APPLICABLE, precheck.getInspectionStatus());
        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_NOT_APPLICABLE, precheck.getDeviationStatus());
        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_NOT_APPLICABLE, precheck.getReworkStatus());
        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_NOT_APPLICABLE, precheck.getScrapStatus());
        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_NOT_APPLICABLE, precheck.getInventoryStatus());

        MesProEdhrReleaseRespVO submitted;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10001L);
            submitted = releaseService.submit(new MesProEdhrReleaseSubmitReqVO()
                    .setReleaseTransactionId(precheck.getReleaseTransactionId())
                    .setIdempotencyKey("submit-dhr-pass-source-na")
                    .setPassword("owner-sign-secret")
                    .setSubmitReason("DHR 完整且外部来源未接入项按非阻塞信息项处理"));
        }

        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL, submitted.getReleaseStatus());
        assertEquals(10001L, submitted.getSubmittedBy());
        assertEquals(null, submitted.getApprovedBy());
        assertNotNull(submitted.getSubmittedAt());
        assertEquals(null, submitted.getApprovedAt());
        assertNotNull(submitted.getReleaseApprovalWorkTaskId());
        verify(adminUserApi).validatePassword(10001L, "owner-sign-secret");
        verify(workTaskService).createReleaseApprovalTaskAfterSubmit(any(), any());
        List<MesProEdhrBatchExecutionSignatureDO> signatures =
                batchSignatureMapper.selectListByBatchExecutionId(batch.getId());
        assertEquals(0, signatures.size());
    }

    @Test
    void submitRecordsTerminalOperationAuditWhenOwnerSignsRelease() {
        MesProEdhrBatchExecutionDO batch = insertClosedBatch("BATCH-REL-SUBMIT-AUDIT");
        insertRouteReleaseOwnerRule(batch.getRouteId(), 10001L);
        MesProEdhrBatchExecutionTaskDO task = insertApprovedOrdinaryTask(batch.getId(), 7351L);
        insertCompletedExecution(task.getExecutionId(), true);
        MesProEdhrReleaseRespVO precheck = precheckAsUser(10001L, batch.getId());
        clearInvocations(operationAuditService);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10001L);
            security.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("放行负责人");
            releaseService.submit(new MesProEdhrReleaseSubmitReqVO()
                    .setReleaseTransactionId(precheck.getReleaseTransactionId())
                    .setIdempotencyKey("submit-terminal-audit")
                    .setPassword("owner-sign-secret")
                    .setSubmitReason("终态审计验证"));
        }

        ArgumentCaptor<MesProEdhrOperationAuditCommand> captor =
                ArgumentCaptor.forClass(MesProEdhrOperationAuditCommand.class);
        verify(operationAuditService).record(captor.capture());
        MesProEdhrOperationAuditCommand audit = captor.getValue();
        assertEquals("submit-terminal-audit", audit.getRequestId());
        assertEquals("RELEASE_TRANSACTION", audit.getObjectType());
        assertEquals(String.valueOf(precheck.getReleaseTransactionId()), audit.getObjectId());
        assertEquals(batch.getId(), audit.getBatchExecutionId());
        assertEquals(MesProEdhrReleaseServiceImpl.EVENT_TYPE_SUBMIT, audit.getOperationType());
        assertEquals("mes:pro-edhr-release:submit", audit.getPermissionCode());
        assertEquals("ALLOW", audit.getPermissionDecision());
        assertEquals("SUCCESS", audit.getResultStatus());
        assertTrue(audit.getMetadataJson().contains("\"toStatus\":\"PENDING_APPROVAL\""));
    }

    @Test
    void submitDoesNotCloseReadyBatchBeforeApproval() {
        MesProEdhrBatchExecutionDO batch = insertReadyToCloseBatch("BATCH-REL-PRE-CLOSE-DHR-PASS");
        insertRouteReleaseOwnerRule(batch.getRouteId(), 10001L);
        MesProEdhrBatchExecutionTaskDO task = insertApprovedOrdinaryTask(batch.getId(), 7401L);
        insertCompletedExecution(task.getExecutionId(), true);

        MesProEdhrReleaseRespVO precheck = precheckAsUser(10001L, batch.getId());
        LocalDateTime closedAtBeforeSubmit = batchExecutionMapper.selectById(batch.getId()).getClosedAt();

        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_PRECHECK_PASSED, precheck.getReleaseStatus());
        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS, precheck.getDhrStatus());

        MesProEdhrReleaseRespVO submitted;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10001L);
            submitted = releaseService.submit(new MesProEdhrReleaseSubmitReqVO()
                    .setReleaseTransactionId(precheck.getReleaseTransactionId())
                    .setIdempotencyKey("submit-pre-close-dhr-pass")
                    .setPassword("owner-sign-secret"));
        }

        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL, submitted.getReleaseStatus());
        MesProEdhrBatchExecutionDO pendingBatch = batchExecutionMapper.selectById(batch.getId());
        assertEquals(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_READY_TO_CLOSE, pendingBatch.getStatus());
        assertEquals(closedAtBeforeSubmit, pendingBatch.getClosedAt());
        verify(workTaskService, never()).createArchiveTaskAfterBatchClose(any());
        verify(workTaskService).createReleaseApprovalTaskAfterSubmit(any(), any());
    }

    @Test
    void submitCreatesPendingApprovalWhenRouteReleaseRoleMemberSigns() {
        MesProEdhrBatchExecutionDO batch = insertClosedBatch("BATCH-REL-ROLE-MEMBER");
        insertRouteReleaseOwnerRoleRule(batch.getRouteId(), 8802L);
        when(permissionApi.getUserRoleIdListByRoleIds(Set.of(8802L))).thenReturn(Set.of(10002L));
        when(adminUserApi.getUserList(Set.of(10002L))).thenReturn(List.of(user(10002L, "角色放行人")));
        MesProEdhrBatchExecutionTaskDO task = insertApprovedOrdinaryTask(batch.getId(), 7402L);
        insertCompletedExecution(task.getExecutionId(), true);

        MesProEdhrReleaseRespVO precheck = precheckAsUser(10001L, batch.getId());

        MesProEdhrReleaseRespVO submitted;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10002L);
            submitted = releaseService.submit(new MesProEdhrReleaseSubmitReqVO()
                    .setReleaseTransactionId(precheck.getReleaseTransactionId())
                    .setIdempotencyKey("submit-role-member-dhr-pass")
                    .setPassword("role-owner-sign-secret"));
        }

        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL, submitted.getReleaseStatus());
        assertEquals(10002L, submitted.getSubmittedBy());
        assertEquals(null, submitted.getApprovedBy());
        verify(adminUserApi).validatePassword(10002L, "role-owner-sign-secret");
    }

    @Test
    void shouldRejectConcurrentReleaseTerminalWhenPrecheckWasConsumedUnderForUpdateLock() throws Exception {
        MesProEdhrBatchExecutionDO batch = insertClosedBatch("BATCH-REL-CONCURRENT-TERMINAL");
        insertRouteReleaseOwnerRule(batch.getRouteId(), 10001L);
        MesProEdhrBatchExecutionTaskDO task = insertApprovedOrdinaryTask(batch.getId(), 7403L);
        insertCompletedExecution(task.getExecutionId(), true);
        MesProEdhrReleaseRespVO precheck = precheckAsUser(10001L, batch.getId());
        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_PRECHECK_PASSED, precheck.getReleaseStatus());

        MesProEdhrReleaseRespVO released;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10001L);
            released = releaseService.submit(new MesProEdhrReleaseSubmitReqVO()
                    .setReleaseTransactionId(precheck.getReleaseTransactionId())
                    .setIdempotencyKey("submit-ac-m23-primary")
                    .setPassword("owner-sign-secret"));
        }
        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL, released.getReleaseStatus());

        String releaseServiceSource = Files.readString(sourcePath(
                "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/"
                        + "MesProEdhrReleaseServiceImpl.java"));
        assertTrue(releaseServiceSource.contains("selectByIdForUpdate"),
                "release terminal transitions must reread the release transaction with selectByIdForUpdate");

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10001L);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> releaseService.submit(new MesProEdhrReleaseSubmitReqVO()
                            .setReleaseTransactionId(precheck.getReleaseTransactionId())
                            .setIdempotencyKey("submit-ac-m23-racing-duplicate")
                            .setPassword("owner-sign-secret")));
            assertEquals(PRO_EDHR_RELEASE_PRECHECK_REQUIRED.getCode(), exception.getCode());
        }
        assertEquals(0, batchSignatureMapper.selectListByBatchExecutionId(batch.getId()).size());
    }

    @Test
    void rejectPrecheckPassedReleaseWhenOwnerReturnsAndRecordsAudit() {
        MesProEdhrBatchExecutionDO batch = insertClosedBatch("BATCH-REL-OWNER-RETURN");
        insertRouteReleaseOwnerRule(batch.getRouteId(), 10001L);
        MesProEdhrBatchExecutionTaskDO task = insertApprovedOrdinaryTask(batch.getId(), 7451L);
        insertCompletedExecution(task.getExecutionId(), true);
        MesProEdhrReleaseRespVO precheck = precheckAsUser(10001L, batch.getId());
        clearInvocations(operationAuditService, workTaskService);

        MesProEdhrReleaseRespVO rejected;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10001L);
            security.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("放行负责人");
            rejected = releaseService.reject(new MesProEdhrReleaseRejectReqVO()
                    .setReleaseTransactionId(precheck.getReleaseTransactionId())
                    .setIdempotencyKey("reject-owner-return")
                    .setRejectReason("预检后退回补充记录"));
        }

        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_REJECTED, rejected.getReleaseStatus());
        assertEquals(10001L, rejected.getRejectedBy());
        assertEquals("预检后退回补充记录", rejected.getRejectReason());
        verify(workTaskService, never()).validateReleaseApprovalTask(any(), any());
        verify(workTaskService, never()).completeReleaseApprovalTask(any(), any(), any(), any());
        ArgumentCaptor<MesProEdhrOperationAuditCommand> captor =
                ArgumentCaptor.forClass(MesProEdhrOperationAuditCommand.class);
        verify(operationAuditService).record(captor.capture());
        MesProEdhrOperationAuditCommand audit = captor.getValue();
        assertEquals("reject-owner-return", audit.getRequestId());
        assertEquals(MesProEdhrReleaseServiceImpl.EVENT_TYPE_REJECT, audit.getOperationType());
        assertEquals("mes:pro-edhr-release:reject", audit.getPermissionCode());
        assertTrue(audit.getMetadataJson().contains("\"fromStatus\":\"PRECHECK_PASSED\""));
        assertTrue(audit.getMetadataJson().contains("\"toStatus\":\"REJECTED\""));
    }

    @Test
    void terminalDecisionCannotBeAddedWhenReleaseTransactionAlreadyHasAnotherTerminalDecision() {
        MesProEdhrBatchExecutionDO batch = insertClosedBatch("BATCH-REL-ONE-TERMINAL-DECISION");
        insertRouteReleaseOwnerRule(batch.getRouteId(), 10001L);
        MesProEdhrBatchExecutionTaskDO task = insertApprovedOrdinaryTask(batch.getId(), 7453L);
        insertCompletedExecution(task.getExecutionId(), true);
        MesProEdhrReleaseRespVO precheck = precheckAsUser(10001L, batch.getId());
        releaseDecisionMapper.insert(MesProEdhrReleaseDecisionDO.builder()
                .releaseTransactionId(precheck.getReleaseTransactionId())
                .batchExecutionId(batch.getId())
                .decisionStatus(MesProEdhrReleaseServiceImpl.STATUS_RELEASED)
                .idempotencyKey("released-before-reject")
                .payloadHash("released-payload")
                .actorUserId(10001L)
                .auditSnapshotJson("{}")
                .decidedAt(LocalDateTime.now())
                .version(1)
                .build());

        MesReleaseFlowBlockerException exception;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10001L);
            exception = assertThrows(MesReleaseFlowBlockerException.class, () ->
                    releaseService.reject(new MesProEdhrReleaseRejectReqVO()
                            .setReleaseTransactionId(precheck.getReleaseTransactionId())
                            .setIdempotencyKey("reject-after-release")
                            .setRejectReason("重复终态")));
        }

        assertEquals(MesReleaseFlowBlockerType.RELEASE_DECISION_ALREADY_FINALIZED,
                exception.getFailure().getBlockers().get(0).getBlockerType());
        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_PRECHECK_PASSED,
                releaseTransactionMapper.selectById(precheck.getReleaseTransactionId()).getReleaseStatus());
    }

    @Test
    void rejectPrecheckPassedReleaseRejectsNonOwner() {
        MesProEdhrBatchExecutionDO batch = insertClosedBatch("BATCH-REL-RETURN-NON-OWNER");
        insertRouteReleaseOwnerRule(batch.getRouteId(), 10001L);
        MesProEdhrBatchExecutionTaskDO task = insertApprovedOrdinaryTask(batch.getId(), 7452L);
        insertCompletedExecution(task.getExecutionId(), true);
        MesProEdhrReleaseRespVO precheck = precheckAsUser(10001L, batch.getId());
        clearInvocations(operationAuditService, workTaskService);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10002L);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> releaseService.reject(new MesProEdhrReleaseRejectReqVO()
                            .setReleaseTransactionId(precheck.getReleaseTransactionId())
                            .setIdempotencyKey("reject-non-owner-return")
                            .setRejectReason("非负责人退回")));
            assertEquals(1_040_750_435, exception.getCode());
        }

        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_PRECHECK_PASSED,
                releaseTransactionMapper.selectById(precheck.getReleaseTransactionId()).getReleaseStatus());
        verify(workTaskService, never()).validateReleaseApprovalTask(any(), any());
        verify(operationAuditService, never()).record(any());
    }

    @Test
    void approveRejectsUnverifiableSignoffEvidenceHash() {
        MesProEdhrBatchExecutionDO batch = insertClosedBatch("BATCH-REL-APPROVE-FAKE-SIGNOFF");
        MesProEdhrReleaseRespVO precheck = insertPendingApprovalRelease(batch);
        MesProEdhrWorkTaskDO approvalTask = releaseApprovalTask(precheck.getReleaseTransactionId(), 7901L);
        when(workTaskService.validateReleaseApprovalTask(any(), any()))
                .thenReturn(approvalTask);
        when(approvalSignatureRecordMapper.selectList(any())).thenReturn(List.of());
        clearInvocations(operationAuditService);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10001L);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> releaseService.approve(approvalRequest(precheck, batch, approvalTask,
                            "approve-fake-signoff", "f".repeat(64), "伪造签名证据")));
            assertEquals(1_040_750_433, exception.getCode());
        }

        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL,
                releaseTransactionMapper.selectById(precheck.getReleaseTransactionId()).getReleaseStatus());
        verify(workTaskService, never()).completeReleaseApprovalTask(any(), any(), any(), any());
        verify(operationAuditService, never()).record(any());
    }

    @Test
    void withdrawPendingApprovalRoutesThroughUnifiedFinalizer() {
        MesProEdhrBatchExecutionDO batch = insertClosedBatch("BATCH-REL-WITHDRAW-FINALIZER");
        MesProEdhrReleaseRespVO precheck = insertPendingApprovalRelease(batch);
        clearInvocations(operationAuditService, workTaskService);

        MesProEdhrReleaseRespVO withdrawn;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10001L);
            withdrawn = releaseService.withdraw(new MesProEdhrReleaseWithdrawReqVO()
                    .setReleaseTransactionId(precheck.getReleaseTransactionId())
                    .setIdempotencyKey("withdraw-unified-finalizer")
                    .setWithdrawReason("撤回待审批申请"));
        }

        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_WITHDRAWN, withdrawn.getReleaseStatus());
        assertEquals(10001L, withdrawn.getWithdrawnBy());
        assertEquals("撤回待审批申请", withdrawn.getWithdrawReason());
        verify(workTaskService).cancelReleaseApprovalTask(precheck.getReleaseTransactionId(), "撤回待审批申请");
        verify(operationAuditService).record(any());
    }

    @Test
    void approveFailsWithSignoffBlockerWhenApprovalTaskAdapterReturnsNoTask() {
        MesProEdhrBatchExecutionDO batch = insertClosedBatch("BATCH-REL-APPROVE-NO-TASK");
        MesProEdhrReleaseRespVO precheck = insertPendingApprovalRelease(batch);
        MesProEdhrWorkTaskDO requestedTask = releaseApprovalTask(precheck.getReleaseTransactionId(), 7903L);
        when(workTaskService.validateReleaseApprovalTask(any(), any())).thenReturn(null);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10001L);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> releaseService.approve(approvalRequest(precheck, batch, requestedTask,
                            "approve-no-task", "a".repeat(64), "审批任务缺失")));
            assertEquals(PRO_EDHR_RELEASE_SIGNOFF_REQUIRED.getCode(), exception.getCode());
        }

        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL,
                releaseTransactionMapper.selectById(precheck.getReleaseTransactionId()).getReleaseStatus());
        verify(workTaskService, never()).completeReleaseApprovalTask(any(), any(), any(), any());
    }

    @Test
    void approveRecordsTerminalAuditWhenApprovalCenterSignatureEvidenceMatches() {
        MesProEdhrBatchExecutionDO batch = insertClosedBatch("BATCH-REL-APPROVE-AUDIT");
        MesProEdhrReleaseRespVO precheck = insertPendingApprovalRelease(batch);
        MesProEdhrWorkTaskDO approvalTask = releaseApprovalTask(precheck.getReleaseTransactionId(), 7902L);
        when(workTaskService.validateReleaseApprovalTask(any(), any()))
                .thenReturn(approvalTask);
        String signatureUrl = "http://localhost/signature/edhr-release-7902.png";
        String signoffEvidenceHash = DigestUtil.sha256Hex(signatureUrl);
        when(approvalSignatureRecordMapper.selectList(any())).thenReturn(List.of(
                BpmApprovalSignatureRecordDO.builder()
                        .moduleCode("EDHR")
                        .sourceTaskType("EDHR_WORK_TASK")
                        .sourceTaskId(String.valueOf(approvalTask.getId()))
                        .signerUserId(10001L)
                        .reviewResult("APPROVE")
                        .passwordVerified(Boolean.TRUE)
                        .signatureImageFileUrl(signatureUrl)
                        .build()));
        clearInvocations(operationAuditService);

        MesProEdhrReleaseRespVO approved;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10001L);
            security.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("审批放行人");
            approved = releaseService.approve(approvalRequest(precheck, batch, approvalTask,
                    "approve-terminal-audit", signoffEvidenceHash, "审批中心签名放行"));
        }

        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_RELEASED, approved.getReleaseStatus());
        assertEquals(signoffEvidenceHash, approved.getApprovalSignoffEvidenceHash());
        verify(workTaskService).completeReleaseApprovalTask(approvalTask.getId(),
                precheck.getReleaseTransactionId(), "APPROVE", "审批中心签名放行");
        ArgumentCaptor<MesProEdhrOperationAuditCommand> captor =
                ArgumentCaptor.forClass(MesProEdhrOperationAuditCommand.class);
        verify(operationAuditService).record(captor.capture());
        MesProEdhrOperationAuditCommand audit = captor.getValue();
        assertEquals("approve-terminal-audit", audit.getRequestId());
        assertEquals(MesProEdhrReleaseServiceImpl.EVENT_TYPE_APPROVE, audit.getOperationType());
        assertEquals("mes:pro-edhr-release:approve", audit.getPermissionCode());
        assertTrue(audit.getMetadataJson().contains("\"signoffEvidenceHash\":\"" + signoffEvidenceHash + "\""));
        assertTrue(audit.getMetadataJson().contains("\"toStatus\":\"RELEASED\""));
    }

    @Test
    void approveRejectsIdempotencyPayloadConflictOnReplay() {
        MesProEdhrBatchExecutionDO batch = insertClosedBatch("BATCH-REL-APPROVE-IDEMPOTENCY-CONFLICT");
        MesProEdhrReleaseRespVO precheck = insertPendingApprovalRelease(batch);
        MesProEdhrWorkTaskDO approvalTask = releaseApprovalTask(precheck.getReleaseTransactionId(), 7904L);
        when(workTaskService.validateReleaseApprovalTask(any(), any())).thenReturn(approvalTask);
        String signatureUrl = "http://localhost/signature/edhr-release-7904.png";
        String signoffEvidenceHash = DigestUtil.sha256Hex(signatureUrl);
        when(approvalSignatureRecordMapper.selectList(any())).thenReturn(List.of(
                BpmApprovalSignatureRecordDO.builder()
                        .moduleCode("EDHR")
                        .sourceTaskType("EDHR_WORK_TASK")
                        .sourceTaskId(String.valueOf(approvalTask.getId()))
                        .signerUserId(10001L)
                        .reviewResult("APPROVE")
                        .passwordVerified(Boolean.TRUE)
                        .signatureImageFileUrl(signatureUrl)
                        .build()));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10001L);
            releaseService.approve(approvalRequest(precheck, batch, approvalTask,
                    "approve-idempotency-conflict", signoffEvidenceHash, "首次批准"));

            MesProEdhrReleaseApproveReqVO changedPayload = approvalRequest(precheck, batch, approvalTask,
                    "approve-idempotency-conflict", signoffEvidenceHash, "修改后的批准意见");
            MesReleaseFlowBlockerException exception = assertThrows(MesReleaseFlowBlockerException.class,
                    () -> releaseService.approve(changedPayload));
            assertEquals(MesReleaseFlowBlockerType.IDEMPOTENCY_PAYLOAD_CONFLICT,
                    exception.getFailure().getBlockers().get(0).getBlockerType());
        }

        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_RELEASED,
                releaseTransactionMapper.selectById(precheck.getReleaseTransactionId()).getReleaseStatus());
    }

    @Test
    void submitRejectsWhenCurrentUserIsNotRouteReleaseOwner() {
        MesProEdhrBatchExecutionDO batch = insertClosedBatch("BATCH-REL-NON-OWNER");
        insertRouteReleaseOwnerRule(batch.getRouteId(), 10001L);
        MesProEdhrBatchExecutionTaskDO task = insertApprovedOrdinaryTask(batch.getId(), 7501L);
        insertCompletedExecution(task.getExecutionId(), true);
        MesProEdhrReleaseRespVO precheck = precheckAsUser(10001L, batch.getId());

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10002L);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> releaseService.submit(new MesProEdhrReleaseSubmitReqVO()
                            .setReleaseTransactionId(precheck.getReleaseTransactionId())
                            .setIdempotencyKey("submit-non-owner")
                            .setPassword("non-owner-secret")
                            .setSubmitReason("非负责人尝试放行")));
            assertEquals(1_040_750_435, exception.getCode());
        }

        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_PRECHECK_PASSED,
                releaseTransactionMapper.selectById(precheck.getReleaseTransactionId()).getReleaseStatus());
        assertEquals(0, batchSignatureMapper.selectListByBatchExecutionId(batch.getId()).size());
        verify(workTaskService, never()).createReleaseApprovalTaskAfterSubmit(any(), any());
    }

    @Test
    void submitRejectsWhenOwnerSignaturePasswordIsMissingOrInvalid() {
        MesProEdhrBatchExecutionDO missingPasswordBatch = insertClosedBatch("BATCH-REL-MISSING-SIGNATURE");
        insertRouteReleaseOwnerRule(missingPasswordBatch.getRouteId(), 10001L);
        MesProEdhrBatchExecutionTaskDO missingPasswordTask =
                insertApprovedOrdinaryTask(missingPasswordBatch.getId(), 7601L);
        insertCompletedExecution(missingPasswordTask.getExecutionId(), true);
        MesProEdhrReleaseRespVO missingPasswordPrecheck = precheckAsUser(10001L, missingPasswordBatch.getId());

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10001L);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> releaseService.submit(new MesProEdhrReleaseSubmitReqVO()
                            .setReleaseTransactionId(missingPasswordPrecheck.getReleaseTransactionId())
                            .setIdempotencyKey("submit-missing-owner-signature")
                            .setPassword(" ")));
            assertEquals(1_040_750_436, exception.getCode());
        }
        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_PRECHECK_PASSED,
                releaseTransactionMapper.selectById(missingPasswordPrecheck.getReleaseTransactionId()).getReleaseStatus());
        assertEquals(0, batchSignatureMapper.selectListByBatchExecutionId(missingPasswordBatch.getId()).size());

        MesProEdhrBatchExecutionDO invalidPasswordBatch = insertClosedBatch("BATCH-REL-INVALID-SIGNATURE");
        insertRouteReleaseOwnerRule(invalidPasswordBatch.getRouteId(), 10001L);
        MesProEdhrBatchExecutionTaskDO invalidPasswordTask =
                insertApprovedOrdinaryTask(invalidPasswordBatch.getId(), 7602L);
        insertCompletedExecution(invalidPasswordTask.getExecutionId(), true);
        MesProEdhrReleaseRespVO invalidPasswordPrecheck = precheckAsUser(10001L, invalidPasswordBatch.getId());
        doThrow(new ServiceException(USER_PASSWORD_FAILED)).when(adminUserApi).validatePassword(10001L, "wrong-pass");

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10001L);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> releaseService.submit(new MesProEdhrReleaseSubmitReqVO()
                            .setReleaseTransactionId(invalidPasswordPrecheck.getReleaseTransactionId())
                            .setIdempotencyKey("submit-invalid-owner-signature")
                            .setPassword("wrong-pass")));
            assertEquals(USER_PASSWORD_FAILED.getCode(), exception.getCode());
        }
        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_PRECHECK_PASSED,
                releaseTransactionMapper.selectById(invalidPasswordPrecheck.getReleaseTransactionId()).getReleaseStatus());
        assertEquals(0, batchSignatureMapper.selectListByBatchExecutionId(invalidPasswordBatch.getId()).size());
        verify(workTaskService, never()).createReleaseApprovalTaskAfterSubmit(any(), any());
    }

    @Test
    void submitRejectsWhenOnlyRouteCloseOwnerIsConfigured() {
        MesProEdhrBatchExecutionDO batch = insertClosedBatch("BATCH-REL-CLOSE-OWNER-NOT-RELEASE");
        insertRouteCloseOwnerRule(batch.getRouteId(), 10001L);
        MesProEdhrBatchExecutionTaskDO task = insertApprovedOrdinaryTask(batch.getId(), 7603L);
        insertCompletedExecution(task.getExecutionId(), true);
        MesProEdhrReleaseRespVO precheck = precheckAsUser(10001L, batch.getId());

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10001L);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> releaseService.submit(new MesProEdhrReleaseSubmitReqVO()
                            .setReleaseTransactionId(precheck.getReleaseTransactionId())
                            .setIdempotencyKey("submit-close-owner-without-release-owner")
                            .setPassword("owner-sign-secret")
                            .setSubmitReason("关闭负责人不能替代放行负责人")));
            assertEquals(1_040_750_435, exception.getCode());
        }

        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_PRECHECK_PASSED,
                releaseTransactionMapper.selectById(precheck.getReleaseTransactionId()).getReleaseStatus());
        assertEquals(0, batchSignatureMapper.selectListByBatchExecutionId(batch.getId()).size());
        verify(adminUserApi, never()).validatePassword(10001L, "owner-sign-secret");
    }

    @Test
    void submitRejectsWhenRouteReleaseRoleHasNoEnabledMembers() {
        MesProEdhrBatchExecutionDO batch = insertClosedBatch("BATCH-REL-EMPTY-ROLE");
        insertRouteReleaseOwnerRoleRule(batch.getRouteId(), 8803L);
        when(permissionApi.getUserRoleIdListByRoleIds(Set.of(8803L))).thenReturn(Set.of());
        MesProEdhrBatchExecutionTaskDO task = insertApprovedOrdinaryTask(batch.getId(), 7604L);
        insertCompletedExecution(task.getExecutionId(), true);
        MesProEdhrReleaseRespVO precheck = precheckAsUser(10001L, batch.getId());

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10001L);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> releaseService.submit(new MesProEdhrReleaseSubmitReqVO()
                            .setReleaseTransactionId(precheck.getReleaseTransactionId())
                            .setIdempotencyKey("submit-empty-release-role")
                            .setPassword("owner-sign-secret")));
            assertEquals(PRO_EDHR_WORK_TASK_CANDIDATE_POOL_EMPTY.getCode(), exception.getCode());
        }

        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_PRECHECK_PASSED,
                releaseTransactionMapper.selectById(precheck.getReleaseTransactionId()).getReleaseStatus());
        assertEquals(0, batchSignatureMapper.selectListByBatchExecutionId(batch.getId()).size());
        verify(adminUserApi, never()).validatePassword(10001L, "owner-sign-secret");
    }

    @Test
    void precheckRestartsRejectedReleaseTransactionByBatchExecutionId() {
        MesProEdhrBatchExecutionDO batch = insertClosedBatch("E2E-REL-RETURNED-REJECTED");
        batchExecutionMapper.updateById(new MesProEdhrBatchExecutionDO()
                .setId(batch.getId())
                .setBatchExecutionCode("EDHRB-1784554940202"));
        batch = batchExecutionMapper.selectById(batch.getId());
        MesProEdhrBatchExecutionTaskDO task = insertApprovedOrdinaryTask(batch.getId(), randomLongId());
        insertCompletedExecution(task.getExecutionId(), true);
        Long transactionId = randomLongId();
        releaseTransactionMapper.insert(MesProEdhrReleaseTransactionDO.builder()
                .id(transactionId)
                .releaseCode("EDHR-REL-RETURNED-" + transactionId)
                .batchExecutionId(batch.getId())
                .batchExecutionCode(batch.getBatchExecutionCode())
                .workOrderId(batch.getWorkOrderId())
                .workOrderCode(batch.getWorkOrderCode())
                .batchCode(batch.getBatchCode())
                .productId(batch.getProductId())
                .productCode(batch.getProductCode())
                .productName(batch.getProductName())
                .routeId(batch.getRouteId())
                .routeCode(batch.getRouteCode())
                .routeName(batch.getRouteName())
                .releaseStatus(MesProEdhrReleaseServiceImpl.STATUS_REJECTED)
                .dhrStatus(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS)
                .inspectionStatus(MesProEdhrReleaseServiceImpl.CHECK_RESULT_NOT_APPLICABLE)
                .deviationStatus(MesProEdhrReleaseServiceImpl.CHECK_RESULT_NOT_APPLICABLE)
                .reworkStatus(MesProEdhrReleaseServiceImpl.CHECK_RESULT_NOT_APPLICABLE)
                .scrapStatus(MesProEdhrReleaseServiceImpl.CHECK_RESULT_NOT_APPLICABLE)
                .inventoryStatus(MesProEdhrReleaseServiceImpl.CHECK_RESULT_NOT_APPLICABLE)
                .requiredCheckCount(6)
                .failedCheckCount(0)
                .blockingCheckCount(0)
                .submittedBy(10001L)
                .submittedAt(LocalDateTime.now())
                .rejectedBy(10002L)
                .rejectedAt(LocalDateTime.now())
                .rejectReason("资料需重新确认")
                .version(1)
                .build());

        MesProEdhrReleaseRespVO result = precheckAsUser(10001L, batch.getId());

        assertEquals(transactionId, result.getReleaseTransactionId());
        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_PRECHECK_PASSED, result.getReleaseStatus());
        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS, result.getDhrStatus());
        assertEquals(0, result.getFailedCheckCount());
        assertEquals(0, result.getBlockingCheckCount());
    }

    @Test
    void pageFiltersPendingApprovalBeforeBatchPagination() {
        MesProEdhrBatchExecutionDO pendingBatch = insertClosedBatch(910000L, "BATCH-REL-PENDING-SECOND-PAGE");
        releaseTransactionMapper.insert(MesProEdhrReleaseTransactionDO.builder()
                .id(920000L)
                .releaseCode("EDHR-REL-PENDING-SECOND-PAGE")
                .batchExecutionId(pendingBatch.getId())
                .batchExecutionCode(pendingBatch.getBatchExecutionCode())
                .workOrderId(pendingBatch.getWorkOrderId())
                .workOrderCode(pendingBatch.getWorkOrderCode())
                .batchCode(pendingBatch.getBatchCode())
                .productId(pendingBatch.getProductId())
                .productCode(pendingBatch.getProductCode())
                .productName(pendingBatch.getProductName())
                .routeId(pendingBatch.getRouteId())
                .routeCode(pendingBatch.getRouteCode())
                .routeName(pendingBatch.getRouteName())
                .releaseStatus(MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL)
                .dhrStatus(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS)
                .inspectionStatus(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS)
                .deviationStatus(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS)
                .reworkStatus(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS)
                .scrapStatus(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS)
                .inventoryStatus(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS)
                .requiredCheckCount(6)
                .failedCheckCount(0)
                .blockingCheckCount(0)
                .submittedBy(10001L)
                .submittedAt(LocalDateTime.now())
                .version(1)
                .build());
        for (int i = 1; i <= 20; i++) {
            insertClosedBatch(910000L + i, "BATCH-REL-NO-TX-" + i);
        }
        MesProEdhrReleasePageReqVO reqVO = new MesProEdhrReleasePageReqVO()
                .setReleaseStatus(MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL)
                .setBatchExecutionCode(pendingBatch.getBatchExecutionCode());
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);

        PageResult<MesProEdhrReleaseRespVO> page = releaseService.getPage(reqVO);

        assertEquals(1L, page.getTotal());
        assertEquals(1, page.getList().size());
        assertEquals(pendingBatch.getId(), page.getList().get(0).getBatchExecutionId());
        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL,
                page.getList().get(0).getReleaseStatus());
    }

    @Test
    void pageCompletedTraceIncludesReleasedAndTerminalBatchPartitionsBeforePagination() {
        MesProEdhrBatchExecutionDO releasedClosedBatch = insertClosedBatch(930000L, "BATCH-TRACE-RELEASED-CLOSED");
        releaseTransactionMapper.insert(MesProEdhrReleaseTransactionDO.builder()
                .id(931000L)
                .releaseCode("EDHR-REL-COMPLETED-CLOSED")
                .batchExecutionId(releasedClosedBatch.getId())
                .batchExecutionCode(releasedClosedBatch.getBatchExecutionCode())
                .workOrderId(releasedClosedBatch.getWorkOrderId())
                .workOrderCode(releasedClosedBatch.getWorkOrderCode())
                .batchCode(releasedClosedBatch.getBatchCode())
                .productId(releasedClosedBatch.getProductId())
                .productCode(releasedClosedBatch.getProductCode())
                .productName(releasedClosedBatch.getProductName())
                .routeId(releasedClosedBatch.getRouteId())
                .routeCode(releasedClosedBatch.getRouteCode())
                .routeName(releasedClosedBatch.getRouteName())
                .releaseStatus(MesProEdhrReleaseServiceImpl.STATUS_RELEASED)
                .dhrStatus(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS)
                .inspectionStatus(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS)
                .deviationStatus(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS)
                .reworkStatus(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS)
                .scrapStatus(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS)
                .inventoryStatus(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS)
                .requiredCheckCount(6)
                .failedCheckCount(0)
                .blockingCheckCount(0)
                .submittedBy(10001L)
                .submittedAt(LocalDateTime.now())
                .approvedBy(10001L)
                .approvedAt(LocalDateTime.now())
                .version(1)
                .build());
        MesProEdhrBatchExecutionDO archivedBatch = insertClosedBatch(930001L, "BATCH-TRACE-ARCHIVED");
        batchExecutionMapper.updateById(new MesProEdhrBatchExecutionDO()
                .setId(archivedBatch.getId())
                .setStatus(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_ARCHIVED));
        MesProEdhrBatchExecutionDO rejectedBatch = insertClosedBatch(930002L, "BATCH-TRACE-REJECTED");
        batchExecutionMapper.updateById(new MesProEdhrBatchExecutionDO()
                .setId(rejectedBatch.getId())
                .setStatus(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_REJECTED));
        insertClosedBatch(930003L, "BATCH-TRACE-STILL-EXECUTING-CLOSED");

        MesProEdhrReleasePageReqVO reqVO = new MesProEdhrReleasePageReqVO()
                .setCompletedTraceOnly(true);
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        PageResult<MesProEdhrReleaseRespVO> page = releaseService.getPage(reqVO);
        List<Long> batchIds = page.getList().stream()
                .map(MesProEdhrReleaseRespVO::getBatchExecutionId)
                .toList();

        assertEquals(3L, page.getTotal());
        assertTrue(batchIds.contains(releasedClosedBatch.getId()));
        assertTrue(batchIds.contains(archivedBatch.getId()));
        assertTrue(batchIds.contains(rejectedBatch.getId()));
    }

    private MesProEdhrBatchExecutionDO insertClosedBatch(String batchCode) {
        return insertClosedBatch(null, batchCode);
    }

    private MesProEdhrReleaseRespVO insertPendingApprovalRelease(MesProEdhrBatchExecutionDO batch) {
        insertRouteReleaseOwnerRule(batch.getRouteId(), 10001L);
        MesProEdhrBatchExecutionTaskDO task = insertApprovedOrdinaryTask(batch.getId(), randomLongId());
        insertCompletedExecution(task.getExecutionId(), true);
        MesProEdhrReleaseRespVO precheck = precheckAsUser(10001L, batch.getId());
        releaseTransactionMapper.updateById(new MesProEdhrReleaseTransactionDO()
                .setId(precheck.getReleaseTransactionId())
                .setReleaseStatus(MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL)
                .setSubmittedBy(10001L)
                .setSubmittedAt(LocalDateTime.now()));
        return precheck;
    }

    private MesProEdhrWorkTaskDO releaseApprovalTask(Long releaseTransactionId, Long workTaskId) {
        return new MesProEdhrWorkTaskDO()
                .setId(workTaskId)
                .setTaskType(MesProEdhrWorkTaskService.TASK_TYPE_RELEASE_APPROVE)
                .setBusinessScopeType("RELEASE_TRANSACTION")
                .setBusinessScopeId(releaseTransactionId);
    }

    private MesProEdhrReleaseApproveReqVO approvalRequest(MesProEdhrReleaseRespVO precheck,
                                                            MesProEdhrBatchExecutionDO batch,
                                                            MesProEdhrWorkTaskDO approvalTask,
                                                            String idempotencyKey,
                                                            String signoffEvidenceHash,
                                                            String opinion) {
        String sourceRelation = "manual-source-" + batch.getId();
        String sourceSnapshotHash = "snapshot-" + batch.getId();
        LocalDateTime now = LocalDateTime.now();
        return new MesProEdhrReleaseApproveReqVO()
                .setReleaseTransactionId(precheck.getReleaseTransactionId())
                .setBatchExecutionId(batch.getId())
                .setOrigin(MesReleaseOrigin.MANUAL)
                .setEntryType("MANUAL")
                .setSourceRelation(sourceRelation)
                .setSourceSnapshotHash(sourceSnapshotHash)
                .setWorkOrderId(70001L)
                .setIndependentPrerequisiteReceiptId("independent-" + batch.getId())
                .setMaterialGateReceiptId("gate-" + batch.getId())
                .setMaterialGateManifestHash("gate-manifest-" + batch.getId())
                .setMaterialGateSourceSnapshotHash(sourceSnapshotHash)
                .setIndependentPrerequisiteReceipt(new IndependentBatchPrerequisiteReceipt()
                        .setReceiptId("independent-" + batch.getId())
                        .setTenantId(1L)
                        .setEntryType("MANUAL")
                        .setBatchExecutionId(batch.getId())
                        .setWorkOrderId(70001L)
                        .setWorkOrderCode("WO-70001")
                        .setRouteId(80001L)
                        .setRouteVersion("v1")
                        .setBatchCode(batch.getBatchCode())
                        .setSourceRelationId("source-id-" + batch.getId())
                        .setSourceRelation(sourceRelation)
                        .setSourceIds(List.of("source-id-" + batch.getId()))
                        .setSourceSnapshotHash(sourceSnapshotHash)
                        .setBusinessReason("manual release")
                        .setIssuerSystem("MES")
                        .setIssuerUserId(10001L)
                        .setIssuerUserRole("MANAGEMENT_REPRESENTATIVE")
                        .setReceiptHash("receipt-hash-" + batch.getId())
                        .setIssuedBy(10001L)
                        .setIssuedAt(now.minusDays(1))
                        .setExpiresAt(now.plusDays(1))
                        .setCredentialVersion(1)
                        .setPayloadHash("payload-hash-" + batch.getId())
                        .setSignature("signature-" + batch.getId())
                        .setAuditEventId("audit-" + batch.getId())
                        .setIdempotencyKey("independent-idem-" + batch.getId())
                        .setVersion(1))
                .setMaterialGateReceipt(new MesReleaseMaterialGateReceipt()
                        .setReceiptId("gate-" + batch.getId())
                        .setBatchExecutionId(batch.getId())
                        .setGateStatus(MesReleaseMaterialGateReceipt.STATUS_MATERIALS_READY)
                        .setMaterialTypeKeys(MesReleaseMaterialGateReceipt.REQUIRED_MATERIAL_TYPES)
                        .setManifestHash("gate-manifest-" + batch.getId())
                        .setSourceSnapshotHash(sourceSnapshotHash)
                        .setMaterialVersionSetHash("version-set-" + batch.getId())
                        .setReceiptHash("gate-receipt-" + batch.getId())
                        .setIssuedBy(10001L)
                        .setAuditEventId("gate-audit-" + batch.getId())
                        .setVersion(1))
                .setWorkTaskId(approvalTask.getId())
                .setExpectedVersion(releaseTransactionMapper.selectById(precheck.getReleaseTransactionId()).getVersion())
                .setIdempotencyKey(idempotencyKey)
                .setSignoffEvidenceHash(signoffEvidenceHash)
                .setApprovalOpinion(opinion);
    }

    private MesProEdhrReleaseRespVO precheckAsUser(Long actorUserId, Long batchExecutionId) {
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(actorUserId);
            return releaseService.precheck(new MesProEdhrReleasePrecheckReqVO()
                    .setBatchExecutionId(batchExecutionId));
        }
    }

    private Path sourcePath(String relativePath) {
        Path current = Path.of("").toAbsolutePath();
        for (int i = 0; i < 8 && current != null; i++, current = current.getParent()) {
            Path direct = current.resolve(relativePath);
            Path backend = current.resolve("IntRuoyiBackend").resolve(relativePath);
            if (Files.isRegularFile(direct)) {
                return direct;
            }
            if (Files.isRegularFile(backend)) {
                return backend;
            }
        }
        throw new IllegalStateException("source file not found: " + relativePath);
    }

    private void mockDossierRequirementState(boolean incomingRequired,
                                             boolean sterilizationRequired,
                                             boolean finishedReportRequired,
                                             boolean finishedRecordRequired,
                                             String configHash) {
        when(dossierRequirementSettingService.getRequirementState())
                .thenReturn(new MesProEdhrReleaseDossierRequirementState(
                        incomingRequired,
                        sterilizationRequired,
                        finishedReportRequired,
                        finishedRecordRequired,
                        configHash));
    }

    private void mockReleaseCompletenessSourcesAsNotApplicable() {
        when(releaseCompletenessService.evaluateInspectionResult(any()))
                .thenReturn(notApplicableSource(MesProEdhrReleaseServiceImpl.CHECK_INSPECTION_RESULT,
                        "检验结果检查", "INSPECTION", "QMS"));
        when(releaseCompletenessService.evaluateDeviationClosed(any()))
                .thenReturn(notApplicableSource(MesProEdhrReleaseServiceImpl.CHECK_DEVIATION_CLOSED,
                        "偏差关闭检查", "DEVIATION", "QMS"));
        when(releaseCompletenessService.evaluateReworkClosed(any()))
                .thenReturn(notApplicableSource(MesProEdhrReleaseServiceImpl.CHECK_REWORK_CLOSED,
                        "返工完成检查", "REWORK", "MES"));
        when(releaseCompletenessService.evaluateScrapRecorded(any()))
                .thenReturn(notApplicableSource(MesProEdhrReleaseServiceImpl.CHECK_SCRAP_RECORDED,
                        "报废记录检查", "SCRAP", "MES"));
        when(releaseCompletenessService.evaluateInventoryConsistency(any()))
                .thenReturn(notApplicableSource(MesProEdhrReleaseServiceImpl.CHECK_INVENTORY_CONSISTENCY,
                        "库存一致性检查", "INVENTORY", "WMS"));
    }

    private MesOrderReleaseCompletenessCheck notApplicableSource(String checkCode, String checkName,
                                                                String category, String module) {
        return new MesOrderReleaseCompletenessCheck(checkCode, checkName, category,
                MesProEdhrReleaseServiceImpl.CHECK_RESULT_NOT_APPLICABLE, "INFO", module,
                "EDHR_BATCH_EXECUTION", "test", "test",
                "该老用例不覆盖 M4 外部来源适配器", "无需处理");
    }

    private MesProEdhrReleaseCheckItemDO selectCheckItem(Long releaseTransactionId, String checkCode) {
        MesProEdhrReleaseCheckItemDO item = releaseCheckItemMapper.selectOne(
                new LambdaQueryWrapperX<MesProEdhrReleaseCheckItemDO>()
                        .eq(MesProEdhrReleaseCheckItemDO::getReleaseTransactionId, releaseTransactionId)
                        .eq(MesProEdhrReleaseCheckItemDO::getCheckCode, checkCode)
                        .eq(MesProEdhrReleaseCheckItemDO::getItemStatus,
                                MesProEdhrReleaseServiceImpl.ITEM_STATUS_OPEN));
        assertNotNull(item, "check item must exist: " + checkCode);
        return item;
    }

    private MesProEdhrBatchExecutionDO insertReadyToCloseBatch(String batchCode) {
        MesProEdhrBatchExecutionDO batch = insertClosedBatch(batchCode);
        batchExecutionMapper.updateById(new MesProEdhrBatchExecutionDO()
                .setId(batch.getId())
                .setStatus(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_READY_TO_CLOSE)
                .setAggregateHash(null)
                .setClosedBy(null)
                .setClosedAt(null));
        return batchExecutionMapper.selectById(batch.getId());
    }

    private MesProEdhrBatchExecutionDO insertClosedBatch(Long id, String batchCode) {
        MesProEdhrBatchExecutionDO batch = MesProEdhrBatchExecutionDO.builder()
                .id(id)
                .batchExecutionCode("EDHR-BATCH-" + randomLongId())
                .workOrderId(randomLongId())
                .workOrderCode("WO-" + randomLongId())
                .batchCode(batchCode)
                .productId(randomLongId())
                .productCode("PRODUCT-" + randomLongId())
                .productName("测试产品")
                .routeId(randomLongId())
                .routeCode("ROUTE-" + randomLongId())
                .routeName("测试路线")
                .status(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CLOSED)
                .taskTotal(1)
                .taskApprovedCount(1)
                .blockedCount(0)
                .aggregateHash("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                .closedBy(10001L)
                .closedAt(LocalDateTime.now())
                .build();
        batchExecutionMapper.insert(batch);
        return batch;
    }

    private MesProEdhrBatchExecutionTaskDO insertApprovedOrdinaryTask(Long batchExecutionId, Long executionId) {
        MesProEdhrBatchExecutionTaskDO task = MesProEdhrBatchExecutionTaskDO.builder()
                .batchExecutionId(batchExecutionId)
                .nodeType("ROUTE_FORM")
                .routeProcessId(randomLongId())
                .routeProcessSort(1)
                .processId(randomLongId())
                .processCode("PROC-" + randomLongId())
                .processName("普通工序")
                .batchRecordReportId("RPT-" + randomLongId())
                .batchRecordReportName("普通工序记录")
                .batchRecordSort(1)
                .executionMode("SEQUENTIAL")
                .recordCategory("BATCH_RECORD")
                .validationProfile("CONTROLLED_BATCH")
                .requiredPolicy("REQUIRED")
                .ownerRoleKey("PRODUCTION")
                .archiveVisibility("FINAL_DHR")
                .executionId(executionId)
                .status(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED)
                .requiredFlag(Boolean.TRUE)
                .submittedAt(LocalDateTime.now())
                .approvedAt(LocalDateTime.now())
                .build();
        batchTaskMapper.insert(task);
        return task;
    }

    private MesProEdhrBatchExecutionTaskDO insertApprovedFormCenterRouteTask(Long batchExecutionId, Long instanceId,
                                                                            String sharedFormKey,
                                                                            int routeProcessSort) {
        MesProEdhrBatchExecutionTaskDO task = MesProEdhrBatchExecutionTaskDO.builder()
                .batchExecutionId(batchExecutionId)
                .nodeType("ROUTE_FORM")
                .routeProcessId(randomLongId())
                .routeProcessSort(routeProcessSort)
                .processId(randomLongId())
                .processCode("PROC-FC-" + randomLongId())
                .processName("FormCenter 共享表单工序")
                .batchRecordSort(routeProcessSort)
                .instanceScope("BATCH_SHARED")
                .sharedFormKey(sharedFormKey)
                .executionMode("SEQUENTIAL")
                .formSlotType("LOSS_REPORT")
                .formBindingKey(sharedFormKey)
                .formTemplateId(25L)
                .formTemplateNameSnapshot("损耗单")
                .formTemplateVersionId(27L)
                .formTemplateVersionNo("V1.0")
                .formCenterInstanceId(instanceId)
                .recordCategory("ROUTE_FORM")
                .validationProfile("FORM_CENTER")
                .requiredPolicy("REQUIRED")
                .ownerRoleKey("PRODUCTION")
                .archiveVisibility("FINAL_DHR")
                .status(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED)
                .requiredFlag(Boolean.TRUE)
                .submittedAt(LocalDateTime.now())
                .approvedAt(LocalDateTime.now())
                .build();
        batchTaskMapper.insert(task);
        return task;
    }

    private void insertRouteCloseOwnerRule(Long routeId, Long ownerUserId) {
        assignmentRuleMapper.insert(MesProEdhrWorkTaskAssignmentRuleDO.builder()
                .scopeType("ROUTE")
                .scopeId(routeId)
                .taskType(MesProEdhrWorkTaskService.TASK_TYPE_CLOSE)
                .assigneeUserId(ownerUserId)
                .candidateSourceType("USER")
                .candidateSourceId(ownerUserId)
                .enabled(Boolean.TRUE)
                .build());
    }

    private void insertRouteReleaseOwnerRule(Long routeId, Long ownerUserId) {
        assignmentRuleMapper.insert(MesProEdhrWorkTaskAssignmentRuleDO.builder()
                .scopeType("ROUTE")
                .scopeId(routeId)
                .taskType(MesProEdhrWorkTaskService.TASK_TYPE_RELEASE_APPROVE)
                .assigneeUserId(ownerUserId)
                .candidateSourceType("USER")
                .candidateSourceId(ownerUserId)
                .enabled(Boolean.TRUE)
                .build());
    }

    private void insertRouteReleaseOwnerRoleRule(Long routeId, Long roleId) {
        assignmentRuleMapper.insert(MesProEdhrWorkTaskAssignmentRuleDO.builder()
                .scopeType("ROUTE")
                .scopeId(routeId)
                .taskType(MesProEdhrWorkTaskService.TASK_TYPE_RELEASE_APPROVE)
                .candidateSourceType("ROLE_GROUP")
                .candidateSourceId(roleId)
                .enabled(Boolean.TRUE)
                .build());
    }

    private AdminUserRespDTO user(Long id, String nickname) {
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(id);
        user.setNickname(nickname);
        user.setStatus(CommonStatusEnum.ENABLE.getStatus());
        return user;
    }

    private void insertWaitingSpecialTask(Long batchExecutionId, String nodeType) {
        insertSpecialTask(batchExecutionId, nodeType, MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_WAITING);
    }

    private MesProEdhrBatchExecutionTaskDO insertSpecialTask(Long batchExecutionId, String nodeType, int status) {
        MesProEdhrBatchExecutionTaskDO task = MesProEdhrBatchExecutionTaskDO.builder()
                .batchExecutionId(batchExecutionId)
                .nodeType(nodeType)
                .routeProcessSort(2)
                .processName(nodeType)
                .recordCategory("SPECIAL_REPORT")
                .validationProfile("EXTERNAL_SOURCE")
                .requiredPolicy("REQUIRED")
                .ownerRoleKey("QUALITY")
                .archiveVisibility("FINAL_DHR")
                .status(status)
                .requiredFlag(Boolean.TRUE)
                .submittedAt(status == MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED
                        ? LocalDateTime.now() : null)
                .approvedAt(status == MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED
                        ? LocalDateTime.now() : null)
                .build();
        batchTaskMapper.insert(task);
        return task;
    }

    private void insertSavedSpecialNodeAttachment(MesProEdhrBatchExecutionTaskDO task,
                                                  String action,
                                                  String groupKey,
                                                  String attachmentHash) {
        String nodeType = task.getNodeType();
        attachmentMapper.insert(MesProBatchRecordExecutionAttachmentDO.builder()
                .executionId(0L)
                .batchExecutionId(task.getBatchExecutionId())
                .batchTaskId(task.getId())
                .rowIndex(0)
                .columnIndex(0)
                .fieldKey(nodeType)
                .fieldPath("specialNode." + nodeType)
                .fieldLabel(task.getProcessName())
                .attachmentType("PDF")
                .attachmentGroupKey(groupKey)
                .attachmentAction(action)
                .versionNo(1)
                .fileId(9001L)
                .fileUrl("http://127.0.0.1/files/dossier.pdf")
                .storageConfigId(28L)
                .storagePath("/edhr/dossier.pdf")
                .fileName("dossier.pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .sha256("dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd")
                .storageRetentionJson("{\"fileId\":9001,\"retention\":\"special-node\"}")
                .storageRetentionHash("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff")
                .previousAttachmentHash(
                        "0000000000000000000000000000000000000000000000000000000000000000")
                .attachmentHash(attachmentHash)
                .operatorId(10001L)
                .operatorName("生产负责人")
                .operatedAt(LocalDateTime.now())
                .reasonCategory("SPECIAL_NODE_ATTACHMENT")
                .reasonText("测试保存资料附件")
                .build());
    }

    private void insertCompletedExecution(Long executionId, boolean includeSubmitSignature) {
        executionMapper.insert(MesProBatchRecordExecutionDO.builder()
                .id(executionId)
                .executionCode("BRE-" + randomLongId())
                .templateId(randomLongId())
                .templateCode("TPL-" + randomLongId())
                .templateName("普通工序模板")
                .workOrderId(randomLongId())
                .workOrderCode("WO-" + randomLongId())
                .routeProcessId(randomLongId())
                .batchRecordReportId("RPT-" + randomLongId())
                .batchCode("BATCH")
                .status(4)
                .sheetLayoutJson("{\"rows\":{}}")
                .metaJson("{}")
                .executionSnapshotJson("{\"layout\":{\"rows\":{}},\"fields\":[]}")
                .cellValuesJson("[{\"rowIndex\":1,\"columnIndex\":1,\"value\":\"已填写\"}]")
                .cellValuesHash("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
                .fieldAuditRevision(1L)
                .fieldAuditHeadHash("cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc")
                .submittedBy(10001L)
                .submittedAt(LocalDateTime.now())
                .closedAt(LocalDateTime.now())
                .domainTraceStatus("VERIFIED")
                .build());
        if (includeSubmitSignature) {
            signatureMapper.insert(MesProBatchRecordExecutionSignatureDO.builder()
                    .executionId(executionId)
                    .actorId(10001L)
                    .actorName("生产填写人")
                    .actionType("SUBMIT")
                    .signatureMode("PASSWORD")
                    .passwordVerified(Boolean.TRUE)
                    .signedAt(LocalDateTime.now())
                    .fieldAuditRevision(1L)
                    .fieldAuditHeadHash("cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc")
                    .cellValuesHash("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
                    .build());
        }
    }

    private MesProEdhrFourMaterialGateResult readyMaterialGate(Long batchExecutionId) {
        List<MesProBatchRecordExecutionAttachmentDO> materials =
                MesProEdhrFourMaterialGateService.REQUIRED_MATERIAL_TYPES.stream()
                        .map(node -> MesProBatchRecordExecutionAttachmentDO.builder()
                                .batchExecutionId(batchExecutionId)
                                .batchTaskId(Math.abs((long) node.hashCode()))
                                .fieldKey(node)
                                .attachmentGroupKey(node)
                                .build())
                        .toList();
        return new MesProEdhrFourMaterialGateResult(
                MesProEdhrFourMaterialGateResult.STATUS_MATERIALS_READY, true,
                "gate-manifest-" + batchExecutionId, materials);
    }

    private record DossierRequirementCase(String nodeType,
                                           String checkCode,
                                           boolean incomingRequired,
                                           boolean sterilizationRequired,
                                           boolean finishedReportRequired,
                                           boolean finishedRecordRequired) {
    }
}
