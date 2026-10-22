package cn.iocoder.yudao.module.mes.approval;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.approval.service.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseSignoffService;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.signature.api.dto.*;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

/** Actual reader, resolver and projections. Only persistence and signature ports are fixtures. */
// Verification DTOs are consumer-port inputs, not proof of digest/time/creation-audit algorithms.
class MesEdhrHistoricalReadContractTest {
    final MesProEdhrWorkTaskMapper tasks = mock(MesProEdhrWorkTaskMapper.class);
    final MesProEdhrReleaseTransactionMapper transactions = mock(MesProEdhrReleaseTransactionMapper.class);
    final MesProcessPoolActiveOrderReleaseApplicationMapper applications = mock(MesProcessPoolActiveOrderReleaseApplicationMapper.class);
    final MesProEdhrNonconformanceReviewMapper reviews = mock(MesProEdhrNonconformanceReviewMapper.class);
    final AdminUserMapper users = mock(AdminUserMapper.class);
    final ElectronicSignatureQueryService signatures = mock(ElectronicSignatureQueryService.class);
    final MesManagerReleaseCompletedActorResolver actors = new MesManagerReleaseCompletedActorResolver(tasks, transactions);
    final MesEdhrApprovalOutcomeReader reader = new MesEdhrApprovalOutcomeReader(tasks, transactions,
            mock(MesProEdhrReleaseDecisionMapper.class), applications, actors, signatures,
            new MesProductionReleaseSignoffService(signatures), mock(MesEdhrExecutionFinalOutcomeReader.class));
    static final LocalDateTime AT = LocalDateTime.of(2026,10,7,10,0);
    MesProEdhrWorkTaskDO task;
    MesProcessPoolActiveOrderReleaseApplicationDO app;
    MesProEdhrNonconformanceReviewDO review;
    AdminUserDO user;

    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        for (var type : List.of(MesProEdhrWorkTaskDO.class, MesProcessPoolActiveOrderReleaseApplicationDO.class,
                MesProEdhrNonconformanceReviewDO.class, AdminUserDO.class)) {
            com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(new org.apache.ibatis.builder.MapperBuilderAssistant(
                    new com.baomidou.mybatisplus.core.MybatisConfiguration(),type.getName()), type);
        }
        ReflectionTestUtils.setField(actors,"applicationMapper",applications);
        ReflectionTestUtils.setField(actors,"nonconformanceReviews",reviews);
        ReflectionTestUtils.setField(actors,"users",users);
        ReflectionTestUtils.setField(actors,"signatures",signatures);
        cn.iocoder.yudao.module.mes.service.pro.MesVerifiedConsumerFixture.wire(actors, signatures, 1L);
        task = new MesProEdhrWorkTaskDO().setId(177L).setTaskType("PQC_PRODUCTION_RELEASE").setStatus("DONE")
                .setBusinessScopeType("RELEASE_APPLICATION").setBusinessScopeId(95L).setWorkOrderId(93L)
                .setBatchExecutionId(92L).setAssigneeUserId(999L).setCompletedAt(AT);
        app = new MesProcessPoolActiveOrderReleaseApplicationDO().setId(95L).setPqcReleaseWorkTaskId(177L)
                .setActiveOrderId(180L).setBatchExecutionId(92L).setWorkOrderId(93L);
        app.setTenantId(1L);
        user = new AdminUserDO().setId(188L).setStatus(1); // historic signer now disabled
        user.setTenantId(1L);
        when(tasks.selectOne(any())).thenReturn(task);
        when(applications.selectOne(any())).thenReturn(app);
        when(users.selectOne(any())).thenReturn(user);
        when(users.selectById(188L)).thenReturn(user);
    }
    @AfterEach void cleanup() { TenantContextHolder.clear(); }

    @ParameterizedTest @org.junit.jupiter.params.provider.NullSource
    @ValueSource(strings={"PQC_RELEASE","", " ","真实用户意见", "真实用户意见\n第二行，保留标点"})
    void formalPqcReceiptAndSignatureKeepDecisionWhenOpinionIsAbsentOrAmbiguous(String original) {
        // Current source-bearing subjects distinguish known user opinion from the system action reason.
        String reason = original == null || original.isBlank() ? "PQC_RELEASE" : original;
        approve(reason,"PQC_RELEASE".equals(reason) ? OpinionFixture.SYSTEM : OpinionFixture.USER);
        var result = reader.readAuthorizedTask(task);
        String opinion = reason.isBlank() || "PQC_RELEASE".equals(reason) ? null : reason;
        assertEquals("APPROVE",result.decisionState());
        assertEquals(opinion,result.approvalRemark());
        assertEquals(188L,result.actorUserId());
        assertEquals(AT,result.actedAt());
        assertEquals("95",result.evidenceId());
        assertBoth(result,188L,opinion);
        verify(signatures,atLeastOnce()).verifyEvidence(88L);
    }

    @ParameterizedTest
    @ValueSource(strings={"历史自由文本不能证明用户来源", "历史自由文本\n第二行，保留标点"})
    void historicalSubjectWithoutOpinionSourceKeepsDecisionButDoesNotInventUserOpinion(String reason) {
        // This is a separate historical 17-part input, not a rewritten stored signature.
        approve(reason,OpinionFixture.HISTORICAL);
        var result = reader.readAuthorizedTask(task);
        assertEquals("APPROVE",result.decisionState());
        assertEquals(188L,result.actorUserId());
        assertEquals(AT,result.actedAt());
        assertEquals("95",result.evidenceId());
        assertNull(result.approvalRemark());
        assertEquals(reason,result.sourceRemark());
        assertBoth(result,188L,null);
        verify(signatures,atLeastOnce()).verifyEvidence(88L);
    }

    @Test void realRejectedReceiptPreservesMultilineOpinionWithDisabledHistoricalActor() {
        String opinion = "真实原意见\n第二行，保留标点";
        task.setReason("REJECT");
        app.setApplicationStatus("PQC_RELEASE_REJECTED").setPqcDecision("REJECT").setPqcDecidedBy(188L).setPqcDecidedAt(AT)
                .setPqcRejectReason(opinion).setDossierSummaryJson(JsonUtils.toJsonString(Map.of("applicationId",95L,
                        "pqcReleaseWorkTaskId",177L,"decidedBy",188L,"decision","REJECT","rejectReason",opinion)));
        var result = reader.readAuthorizedTask(task);
        assertEquals("REJECT",result.decisionState()); assertBoth(result,188L,opinion);
        verifyNoInteractions(signatures);
    }

    @ParameterizedTest @ValueSource(strings={"rework","void"})
    void qaClosesUndecidedPqcWithCompletionEvidenceButNoPqcDecision(String disposition) {
        qa(disposition);
        var result = reader.readAuthorizedTask(task);
        assertNull(result.approvalResult()); assertEquals("UNKNOWN",result.decisionState());
        assertNull(result.actorUserId()); assertNull(result.actedAt()); assertNull(result.approvalRemark());
        assertEquals("QA正式处置意见",result.sourceRemark());
        assertEquals("EXISTING_QA_NONCONFORMANCE_REVIEW",result.evidenceType());
        assertEquals("96",result.evidenceId());
        assertBoth(result,188L,null);
    }

    @ParameterizedTest @ValueSource(strings={"reviewFk","taskFk","reviewTenant","order","activeOrder","batch",
            "time","actorTenant","actorId","signatureMissing","signatureActor","signatureHash","reviewStatus"})
    void contradictoryQaEvidenceIsRejectedBeforeProjection(String broken) {
        qa("void");
        switch(broken) {
            case "reviewFk" -> app.setQaClosureReviewId(97L);
            case "taskFk" -> app.setPqcReleaseWorkTaskId(178L);
            case "reviewTenant" -> review.setTenantId(2L);
            case "order" -> review.setWorkOrderId(94L);
            case "activeOrder" -> review.setActiveOrderId(181L);
            case "batch" -> review.setBatchExecutionId(94L);
            case "time" -> review.setClosedAt(AT.plusSeconds(1));
            case "actorTenant" -> user.setTenantId(2L);
            case "actorId" -> user.setId(189L);
            case "signatureMissing" -> when(signatures.getById(88L)).thenReturn(null);
            case "signatureActor" -> evidence("QA_DISPOSITION","QA正式处置意见",189L,96L,"aggregate");
            case "signatureHash" -> when(signatures.verifyEvidence(88L)).thenReturn(
                    new ElectronicSignatureVerificationDTO(88L,"MISMATCH","content","bad","evidence","evidence","SHA-256","v1",
                            "LOCAL_DIGEST_TIME_AND_CREATION_AUDIT", "MISMATCH", "CONSISTENT", "SERVER_CLOCK", "NOT_BOUND",
                            "CONSISTENT", java.util.List.of("DIGEST_MISMATCH"), LocalDateTime.of(2026, 10, 7, 12, 0), "V1"));
            case "reviewStatus" -> review.setReviewStatus("pending");
            default -> throw new AssertionError(broken);
        }
        assertThrows(IllegalStateException.class,() -> reader.readAuthorizedTask(task));
        assertEquals(999L,task.getAssigneeUserId());
    }

    @Test void missingQaRelationshipStaysUnknownWithoutInventingActor() {
        qa("void"); task.setReviewSourceId(null);
        var result = reader.readAuthorizedTask(task);
        assertEquals("UNKNOWN",result.decisionState()); assertNull(result.taskCompletedByUserId());
        verifyNoInteractions(signatures);
    }

    @ParameterizedTest @ValueSource(strings={"APPROVE","REJECT"})
    void qaAfterPqcDecisionKeepsOriginalPqcDecisionActorOpinionAndTime(String decision) {
        qa("rework");
        review.setClosedAt(AT.plusHours(1)); // The later QA event cannot replace the original PQC time.
        task.setReviewSourceType(null).setReviewSourceId(null).setReason(decision);
        app.setPqcDecision(decision).setPqcDecidedBy(188L).setPqcDecidedAt(AT).setPqcRejectReason("原PQC驳回意见");
        Map<String,Object> receipt = new HashMap<>(Map.of("applicationId",95L,"pqcReleaseWorkTaskId",177L,
                "decision",decision,"decidedBy",188L));
        if (decision.equals("APPROVE")) {
            receipt.put("batchExecutionId",92L); receipt.put("signatureId",89L);
            evidence(89L,"PQC_RELEASE","原PQC批准意见",188L,95L,null);
        } else receipt.put("rejectReason","原PQC驳回意见");
        app.setDossierSummaryJson(JsonUtils.toJsonString(receipt));
        var result = reader.readAuthorizedTask(task);
        assertEquals(decision,result.decisionState()); assertEquals(188L,result.actorUserId());
        assertEquals(AT,result.actedAt()); assertEquals("95",result.evidenceId());
        assertEquals(decision.equals("APPROVE")?"原PQC批准意见":"原PQC驳回意见",result.approvalRemark());
        assertBoth(result,188L,result.approvalRemark());
    }

    private void assertBoth(MesEdhrApprovalOutcome outcome,Long completedBy,String opinion) {
        var row = MesEdhrApprovalOutcomeProjection.apply(ApprovalTaskSummary.builder().taskCompletedAt(AT).build(),outcome);
        var entry = MesEdhrApprovalOutcomeProjection.apply(ApprovalTaskTimelineEntry.builder().taskCompletedAt(AT).build(),outcome);
        assertEquals(completedBy,row.getTaskCompletedByUserId());
        assertEquals(completedBy,entry.getTaskCompletedByUserId()); assertEquals(completedBy,entry.getActorUserId());
        assertEquals(AT,entry.getActedAt()); assertEquals(opinion,row.getApprovalRemark()); assertEquals(opinion,entry.getComment());
        assertEquals(row.getDecisionActorUserId(),entry.getDecisionActorUserId());
        assertEquals(row.getDecisionAt(),entry.getDecisionAt()); assertEquals(row.getEvidenceId(),entry.getEvidenceId());
        assertEquals(999L,task.getAssigneeUserId());
        var service = mock(MesProEdhrWorkTaskService.class);
        var handoff = mock(MesActiveOrderHandoffApprovalProjection.class);
        var adapter = new MesProEdhrApprovalTaskAdapter(service,mock(MesProEdhrReleaseService.class),actors);
        ReflectionTestUtils.setField(adapter,"outcomeReader",reader);
        ReflectionTestUtils.setField(adapter,"handoffProjection",handoff);
        var display = new cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskRespVO()
                .setId(task.getId()).setTaskType(task.getTaskType()).setStatus(task.getStatus()).setCompletedAt(AT)
                .setBusinessScopeType(task.getBusinessScopeType()).setBusinessScopeId(task.getBusinessScopeId())
                .setWorkOrderId(task.getWorkOrderId()).setBatchExecutionId(task.getBatchExecutionId()).setReason(task.getReason())
                .setActionUrl("/mes/production-release/pqc?applicationId=95");
        when(service.getApprovalCenterAuthorizedPrefix(any(),org.mockito.ArgumentMatchers.eq(false),
                org.mockito.ArgumentMatchers.eq(true),org.mockito.ArgumentMatchers.anyLong())).thenReturn(
                new cn.iocoder.yudao.framework.common.pojo.PageResult<>(List.of(new MesEdhrAuthorizedTaskProjection(task,display)),1L));
        when(handoff.prefix(any(),org.mockito.ArgumentMatchers.anyLong())).thenReturn(
                new cn.iocoder.yudao.framework.common.pojo.PageResult<>(List.of(),0L));
        when(service.getApprovalCenterTimelineTasks(177L,null,false)).thenReturn(List.of(task));
        ReflectionTestUtils.setField(adapter, "permissions", mock(cn.iocoder.yudao.module.system.api.permission.PermissionApi.class));
        var actualRow = adapter.page(ApprovalTaskQueryContext.of(188L,
                cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskViewType.DONE,
                cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode.EDHR,null,1,20,false)).getList().get(0);
        var actualEntry = adapter.listTimeline(ApprovalTaskTimelineQueryContext.of(188L,
                cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode.EDHR,"EDHR_WORK_TASK","177","177",null)).get(0);
        assertEquals(completedBy,actualRow.getTaskCompletedByUserId()); assertEquals(completedBy,actualEntry.getActorUserId());
        assertEquals(outcome.actorUserId(),actualRow.getDecisionActorUserId());
        assertEquals(outcome.actedAt(),actualRow.getDecisionAt());
        assertEquals(actualRow.getDecisionActorUserId(),actualEntry.getDecisionActorUserId());
        assertEquals(actualRow.getDecisionAt(),actualEntry.getDecisionAt());
        assertEquals(opinion,actualRow.getApprovalRemark()); assertEquals(opinion,actualEntry.getComment());
        assertEquals(outcome.evidenceId(),actualRow.getEvidenceId()); assertEquals(actualRow.getEvidenceId(),actualEntry.getEvidenceId());
    }
    private void approve(String reason,OpinionFixture opinionSource) {
        task.setReason("APPROVE");
        app.setApplicationStatus("MANAGER_RELEASE_PENDING").setPqcDecision("APPROVE").setPqcDecidedBy(188L).setPqcDecidedAt(AT)
                .setDossierSummaryJson(JsonUtils.toJsonString(Map.of("applicationId",95L,"pqcReleaseWorkTaskId",177L,
                        "batchExecutionId",92L,"decidedBy",188L,"decision","APPROVE","signatureId",88L)));
        evidence(88L,"PQC_RELEASE",reason,188L,95L,null,opinionSource);
    }
    private void qa(String disposition) {
        task.setReason("NONCONFORMANCE_"+disposition.toUpperCase()).setReviewSourceType("EDHR_NONCONFORMANCE_REVIEW").setReviewSourceId(96L);
        app.setApplicationStatus(task.getReason()).setQaClosureReviewId(96L);
        review = new MesProEdhrNonconformanceReviewDO().setId(96L).setActiveOrderId(180L).setWorkOrderId(93L)
                .setBatchExecutionId(92L).setReviewStatus("closed").setDisposition(disposition).setQaUserId(188L)
                .setClosedAt(AT).setReviewOpinion("QA正式处置意见").setTraceSnapshotJson(JsonUtils.toJsonString(Map.of(
                        "qaSignatureSnapshotJson",Map.of("reviewId",96L,"qaUserId",188L,"disposition",disposition,
                                "actionType","QA_DISPOSITION","signatureId",88L,"aggregateHash","aggregate"))));
        review.setTenantId(1L);
        when(reviews.selectOne(any())).thenReturn(review);
        evidence("QA_DISPOSITION","QA正式处置意见",188L,96L,"aggregate");
    }
    private void evidence(String action,String reason,Long actor,Long source,String hash) { evidence(88L,action,reason,actor,source,hash); }
    private void evidence(Long id,String action,String reason,Long actor,Long source,String hash) {
        evidence(id,action,reason,actor,source,hash,OpinionFixture.USER);
    }
    private enum OpinionFixture { USER, SYSTEM, HISTORICAL }
    private void evidence(Long id,String action,String reason,Long actor,Long source,String hash,OpinionFixture opinionSource) {
        boolean qa = action.equals("QA_DISPOSITION");
        String subject = MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(qa?0L:92L,action,null,null,null,null,null,null,null,
                qa?"EDHR_NONCONFORMANCE_REVIEW":"PQC_RELEASE_APPLICATION",source,qa?"eDHR不合格评审处置":"PQC生产放行",action,null,null,hash,null);
        if (opinionSource != OpinionFixture.HISTORICAL && "PQC_RELEASE".equals(action)) {
            // Source is explicitly selected by each scenario; free text never proves USER.
            subject = MesBatchRecordSignatureSubjectAdapter.withOpinionSource(subject,
                    opinionSource == OpinionFixture.USER ? reason : null);
        }
        String version = MesProBatchRecordExecutionFieldAuditHasher.sha256(subject);
        var canonical = (com.fasterxml.jackson.databind.node.ObjectNode) JsonUtils.parseTree(
                new MesBatchRecordSignatureSubjectAdapter().loadAndAuthorize(new SignatureSubjectCommand(
                        actor,"MES",action,"MES_BATCH_RECORD",subject,version,reason)).canonicalContentJson());
        canonical.put("reason",reason == null ? "" : reason.trim());
        when(signatures.getById(id)).thenReturn(new ElectronicSignatureEvidenceDTO(id,"MES",action,"MES_BATCH_RECORD",subject,
                version,actor,action,"正式签名",reason,AT,null,"SESSION_PLUS_PASSWORD","content","evidence","SHA-256","key",
                "mes-batch-record-signature-v1","VALID",null,null,null,null,JsonUtils.toJsonString(canonical),null,null,null,"历史签名人"));
        when(signatures.verifyEvidence(id)).thenReturn(new ElectronicSignatureVerificationDTO(id,"VALID","content","content","evidence","evidence","SHA-256","v1",
                "LOCAL_DIGEST_TIME_AND_CREATION_AUDIT", "CONSISTENT", "CONSISTENT", "SERVER_CLOCK", "NOT_BOUND",
                "CONSISTENT", java.util.List.of(), LocalDateTime.of(2026, 10, 7, 12, 0), "V1"));
    }
}
