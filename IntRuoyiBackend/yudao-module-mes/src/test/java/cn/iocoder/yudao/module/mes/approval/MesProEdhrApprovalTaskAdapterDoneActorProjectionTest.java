package cn.iocoder.yudao.module.mes.approval;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskViewType;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskQueryContext;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskSummary;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskTimelineQueryContext;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskService;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchRecordSignatureSubjectAdapter;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditHasher;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureEvidenceDTO;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureVerificationDTO;
import cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectCommand;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Executes the real resolver's mapper queries; service mocks only supply already-authorized rows. */
@Import({MesProEdhrApprovalTaskAdapter.class, MesManagerReleaseCompletedActorResolver.class,
        MesEdhrApprovalOutcomeReader.class, MesEdhrExecutionFinalOutcomeReader.class,
        cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseSignoffService.class})
// Verification DTOs are consumer-port inputs, not proof of digest/time/creation-audit algorithms.
class MesProEdhrApprovalTaskAdapterDoneActorProjectionTest extends BaseDbUnitTest {
    @org.springframework.boot.test.mock.mockito.MockBean private cn.iocoder.yudao.module.mes.approval.MesActiveOrderHandoffApprovalProjection handoffProjection;


    private static final Long SIGNER = 9908090347L;
    private static final Long OWNER = 1L;
    private static final Long BATCH = 900000001228L;
    private static final Long WORK_ORDER = 990274L;
    private static final String CANDIDATES = "1,9908090347,9908090348";

    @Resource private MesProEdhrApprovalTaskAdapter adapter;
    @Resource private MesManagerReleaseCompletedActorResolver resolver;
    @Resource private MesProEdhrWorkTaskMapper tasks;
    @Resource private MesProEdhrReleaseTransactionMapper transactions;
    @Resource private cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseDecisionMapper decisions;
    @Resource private DataSource dataSource;
    @MockBean private MesProEdhrWorkTaskService workTaskService;
    @MockBean private MesProEdhrReleaseService releaseService;
    @Resource private MesProcessPoolActiveOrderReleaseApplicationMapper applications;
    @MockBean private AdminUserMapper users;
    @MockBean private ElectronicSignatureQueryService signatures;
    @MockBean private cn.iocoder.yudao.module.system.api.permission.PermissionApi permissions;
    @MockBean private cn.iocoder.yudao.module.signature.service.ElectronicSignatureVerifiedFactService verifiedFacts;
    @MockBean private cn.iocoder.yudao.module.system.service.gxpaudit.GxpCreationAuditVerificationService creationAudits;
    private JdbcTemplate jdbc;

    @org.junit.jupiter.api.BeforeAll
    static void initializeUserMapperMetadata() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(new org.apache.ibatis.builder.MapperBuilderAssistant(
                new com.baomidou.mybatisplus.core.MybatisConfiguration(), AdminUserDO.class.getName()), AdminUserDO.class);
    }

    @BeforeEach
    void prepareFixtureAccess() {
        jdbc = new JdbcTemplate(dataSource);
        cn.iocoder.yudao.module.mes.service.pro.MesVerifiedConsumerFixture.wire(resolver, signatures, 1L);
        when(handoffProjection.prefix(any(), anyLong())).thenReturn(new PageResult<>(List.of(), 0L));
    }

    @AfterEach
    void removeOwnPqcApplicationFixture() {
        jdbc.update("DELETE FROM mes_pro_process_pool_active_order_release_application WHERE request_idempotency_key='pqc-completed-request'");
        jdbc.update("DELETE FROM mes_pro_process_pool_active_order_release_application WHERE request_idempotency_key='manager-completed-request'");
        jdbc.update("DELETE FROM mes_pro_edhr_release_decision WHERE idempotency_key LIKE 'manager-completed-%'");
    }

    @Test
    void doneSummaryAndTimelineShowFormalSignerWithoutChangingFrozenOwnership() {
        Fixture fixture = releasedManagerTask();
        Snapshot before = snapshot();
        exposeDone(fixture, false);

        ApprovalTaskSummary summary = adapter.page(query(ApprovalTaskViewType.DONE, false)).getList().get(0);
        assertEquals(SIGNER, summary.getAssigneeUserId());
        assertNull(summary.getAssigneeRoleCode());
        assertNull(summary.getAssigneeRoleName(), "a role label must not hide the actual completed actor");
        assertEquals("RELEASED", summary.getBusinessStatus());
        assertEquals("DONE", summary.getSourceStatus());
        assertEquals("APPROVE", summary.getDecisionState());
        assertEquals(String.valueOf(fixture.transaction().getId()), summary.getDetailQuery().get("releaseTransactionId"));
        assertEquals(SIGNER, adapter.listTimeline(timeline(fixture)).get(0).getActorUserId());
        assertEquals(before, snapshot(), "projection must not mutate either formal table");
        assertEquals(OWNER, tasks.selectById(fixture.task().getId()).getAssigneeUserId());
        assertEquals(CANDIDATES, tasks.selectById(fixture.task().getId()).getCandidateUserSnapshot());
        assertEquals(OWNER, fixture.summary().getAssigneeUserId(), "do not mutate the source response row");
        verifyNoInteractions(releaseService);
    }

    @Test
    void globalDoneViewProjectsActualSignerInsteadOfOwnerOrViewingUser() {
        Fixture fixture = releasedManagerTask();
        exposeDone(fixture, true);
        ApprovalTaskQueryContext viewer = query(ApprovalTaskViewType.DONE, true).setLoginUserId(9908090348L);
        assertEquals(SIGNER, adapter.page(viewer).getList().get(0).getAssigneeUserId());
        verifyNoInteractions(releaseService);
    }

    @ParameterizedTest(name = "{0} cannot manufacture a completed manager actor")
    @EnumSource(BrokenAssociation.class)
    void persistedFormalAssociationMustMatchInBothSummaryAndTimeline(BrokenAssociation broken) {
        Fixture fixture = releasedManagerTask();
        Long taskId = fixture.task().getId();
        Long transactionId = fixture.transaction().getId();
        switch (broken) {
            case TASK_MISSING -> jdbc.update("DELETE FROM mes_pro_edhr_work_task WHERE id = ?", taskId);
            case TASK_OTHER_TENANT -> jdbc.update("UPDATE mes_pro_edhr_work_task SET tenant_id = 2 WHERE id = ?", taskId);
            case TASK_DELETED -> jdbc.update("UPDATE mes_pro_edhr_work_task SET deleted = TRUE WHERE id = ?", taskId);
            case TASK_NOT_DONE -> jdbc.update("UPDATE mes_pro_edhr_work_task SET status = 'TODO' WHERE id = ?", taskId);
            case TASK_OTHER_TYPE -> jdbc.update("UPDATE mes_pro_edhr_work_task SET task_type = 'REVIEW' WHERE id = ?", taskId);
            case TASK_OTHER_SCOPE -> jdbc.update("UPDATE mes_pro_edhr_work_task SET business_scope_type = 'BATCH_TASK' WHERE id = ?", taskId);
            case TASK_OTHER_TRANSACTION -> jdbc.update("UPDATE mes_pro_edhr_work_task SET business_scope_id = ? WHERE id = ?", transactionId + 1, taskId);
            case TASK_OTHER_BATCH -> jdbc.update("UPDATE mes_pro_edhr_work_task SET batch_execution_id = ? WHERE id = ?", BATCH + 1, taskId);
            case TASK_OTHER_WORK_ORDER -> jdbc.update("UPDATE mes_pro_edhr_work_task SET work_order_id = ? WHERE id = ?", WORK_ORDER + 1, taskId);
            case TASK_REJECTED_AS_APPROVED -> jdbc.update("UPDATE mes_pro_edhr_work_task SET reason = 'REJECT:returned' WHERE id = ?", taskId);
            case TRANSACTION_MISSING -> jdbc.update("DELETE FROM mes_pro_edhr_release_transaction WHERE id = ?", transactionId);
            case TRANSACTION_OTHER_TENANT -> jdbc.update("UPDATE mes_pro_edhr_release_transaction SET tenant_id = 2 WHERE id = ?", transactionId);
            case TRANSACTION_DELETED -> jdbc.update("UPDATE mes_pro_edhr_release_transaction SET deleted = TRUE WHERE id = ?", transactionId);
            case TRANSACTION_NOT_RELEASED -> jdbc.update("UPDATE mes_pro_edhr_release_transaction SET release_status = 'PENDING_APPROVAL' WHERE id = ?", transactionId);
            case TRANSACTION_REJECTED -> jdbc.update("UPDATE mes_pro_edhr_release_transaction SET release_status = 'REJECTED' WHERE id = ?", transactionId);
            case TRANSACTION_OTHER_BATCH -> jdbc.update("UPDATE mes_pro_edhr_release_transaction SET batch_execution_id = ? WHERE id = ?", BATCH + 1, transactionId);
            case TRANSACTION_OTHER_WORK_ORDER -> jdbc.update("UPDATE mes_pro_edhr_release_transaction SET work_order_id = ? WHERE id = ?", WORK_ORDER + 1, transactionId);
            case APPROVER_MISSING -> jdbc.update("UPDATE mes_pro_edhr_release_transaction SET approved_by = NULL WHERE id = ?", transactionId);
            case APPROVER_ZERO -> jdbc.update("UPDATE mes_pro_edhr_release_transaction SET approved_by = 0 WHERE id = ?", transactionId);
            case APPROVER_NEGATIVE -> jdbc.update("UPDATE mes_pro_edhr_release_transaction SET approved_by = -1 WHERE id = ?", transactionId);
        }
        exposeDone(fixture, false);
        Snapshot before = snapshot();
        if (java.util.Set.of(BrokenAssociation.TRANSACTION_MISSING,BrokenAssociation.TRANSACTION_OTHER_TENANT,
                BrokenAssociation.TRANSACTION_DELETED).contains(broken)) {
            var summary = adapter.page(query(ApprovalTaskViewType.DONE,false)).getList().get(0);
            var entry = adapter.listTimeline(timeline(fixture)).get(0);
            assertEquals("UNKNOWN",summary.getDecisionState());
            assertNull(summary.getApprovalResult());
            assertNull(summary.getAssigneeUserId());
            assertNull(entry.getActorUserId());
            assertNull(entry.getApprovalResult());
        } else {
            IllegalStateException summaryError = assertThrows(IllegalStateException.class,
                    () -> adapter.page(query(ApprovalTaskViewType.DONE, false)));
            IllegalStateException timelineError = assertThrows(IllegalStateException.class,
                    () -> adapter.listTimeline(timeline(fixture)));
            String prefix = java.util.Set.of(BrokenAssociation.APPROVER_MISSING,BrokenAssociation.APPROVER_ZERO,
                    BrokenAssociation.APPROVER_NEGATIVE).contains(broken)
                    ? "APPROVAL_COMPLETED_ACTOR_INVALID:" : "EDHR_APPROVAL_OUTCOME_INVALID:";
            assertTrue(summaryError.getMessage().startsWith(prefix));
            assertTrue(timelineError.getMessage().startsWith(prefix));
        }
        assertEquals(before, snapshot(), "failed projection must not repair or overwrite the formal source");
        verifyNoInteractions(releaseService);
    }

    @Test
    void explicitTenantPredicateStillRejectsForeignTransactionWhenInterceptorIsIgnored() {
        Fixture fixture = releasedManagerTask();
        jdbc.update("UPDATE mes_pro_edhr_release_transaction SET tenant_id = 2 WHERE id = ?", fixture.transaction().getId());
        exposeDone(fixture, false);
        TenantContextHolder.setIgnore(true);
        var summary = adapter.page(query(ApprovalTaskViewType.DONE,false)).getList().get(0);
        assertEquals("UNKNOWN",summary.getDecisionState());
        assertNull(summary.getAssigneeUserId());
        assertNull(summary.getApprovalResult());
        verifyNoInteractions(releaseService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"transaction", "batch", "workOrder"})
    void incompleteOrWrongProjectedIdentityIsRejected(String field) {
        Fixture fixture = releasedManagerTask();
        switch (field) {
            case "transaction" -> fixture.task().setBusinessScopeId(null);
            case "batch" -> fixture.task().setBatchExecutionId(null);
            case "workOrder" -> fixture.task().setWorkOrderId(null);
            default -> throw new AssertionError(field);
        }
        exposeDone(fixture, false);
        assertThrows(IllegalStateException.class, () -> adapter.page(query(ApprovalTaskViewType.DONE, false)));
        verifyNoInteractions(releaseService);
    }

    @Test
    void missingTenantCannotUseFrozenOwnerAsCompletedActor() {
        Fixture fixture = releasedManagerTask();
        exposeDone(fixture, false);
        TenantContextHolder.clear();
        assertThrows(RuntimeException.class, () -> adapter.page(query(ApprovalTaskViewType.DONE, false)));
        verifyNoInteractions(releaseService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"TODO", "OVERDUE"})
    void pendingManagerSummaryAndTimelineKeepAssignedOwnerWithoutReleasedTransaction(String status) {
        Fixture fixture = releasedManagerTask();
        jdbc.update("DELETE FROM mes_pro_edhr_release_transaction WHERE id = ?", fixture.transaction().getId());
        fixture.summary().setStatus(status).setCompletedAt(null);
        fixture.task().setStatus(status).setCompletedAt(null);
        persistTaskChanges(fixture.task());
        when(workTaskService.getApprovalCenterAuthorizedPrefix(any(), eq(false), eq(false), anyLong()))
                .thenReturn(new PageResult<>(List.of(new MesEdhrAuthorizedTaskProjection(fixture.task(),fixture.summary())), 1L));
        exposeTimeline(fixture);
        assertEquals(OWNER, adapter.page(query(ApprovalTaskViewType.TODO, false)).getList().get(0).getAssigneeUserId());
        assertNull(adapter.listTimeline(timeline(fixture)).get(0).getActorUserId());
        verifyNoInteractions(releaseService);
    }

    @Test
    void ordinaryDoneReviewWithoutFormalReceiptDoesNotBorrowFrozenOwner() {
        Fixture fixture = releasedManagerTask();
        jdbc.update("DELETE FROM mes_pro_edhr_release_transaction WHERE id = ?", fixture.transaction().getId());
        fixture.summary().setTaskType("REVIEW").setBusinessScopeType("BATCH_TASK");
        fixture.task().setTaskType("REVIEW").setBusinessScopeType("BATCH_TASK");
        persistTaskChanges(fixture.task());
        exposeDone(fixture, false);
        var summary = adapter.page(query(ApprovalTaskViewType.DONE,false)).getList().get(0);
        assertEquals("UNKNOWN",summary.getDecisionState());
        assertNull(summary.getAssigneeUserId());
        assertNull(summary.getApprovalResult());
        assertNull(adapter.listTimeline(timeline(fixture)).get(0).getActorUserId());
        verifyNoInteractions(releaseService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"BATCH_EXECUTION", "BATCH_TASK"})
    void incompatibleReleaseApprovalScopesFailWithoutInventingFormalActor(String scopeType) {
        Fixture fixture = releasedManagerTask();
        fixture.summary().setBusinessScopeType(scopeType).setBusinessScopeId(BATCH);
        fixture.task().setBusinessScopeType(scopeType).setBusinessScopeId(BATCH);
        persistTaskChanges(fixture.task());
        jdbc.update("DELETE FROM mes_pro_edhr_release_transaction WHERE id = ?", fixture.transaction().getId());
        exposeDone(fixture, false);
        Snapshot before = snapshot();
        assertTrue(assertThrows(IllegalStateException.class,() -> adapter.page(query(ApprovalTaskViewType.DONE,false)))
                .getMessage().contains("market task scope invalid"));
        assertTrue(assertThrows(IllegalStateException.class,() -> adapter.listTimeline(timeline(fixture)))
                .getMessage().contains("market task scope invalid"));
        assertEquals(scopeType, tasks.selectById(fixture.task().getId()).getBusinessScopeType());
        assertEquals(before, snapshot());
        verifyNoInteractions(releaseService);
    }

    @Test
    void legacyRejectedManagerTaskDoesNotBorrowAnApprovedActor() {
        Fixture fixture = releasedManagerTask();
        jdbc.update("UPDATE mes_pro_edhr_release_transaction SET release_status = 'REJECTED' WHERE id = ?", fixture.transaction().getId());
        fixture.summary().setReason("REJECT:returned");
        fixture.task().setReason("REJECT:returned");
        persistTaskChanges(fixture.task());
        jdbc.update("UPDATE mes_pro_edhr_release_decision SET decision_status='REJECTED',release_application_id=NULL WHERE release_transaction_id=?",fixture.transaction().getId());
        exposeDone(fixture, false);
        var summary = adapter.page(query(ApprovalTaskViewType.DONE,false)).getList().get(0);
        assertNull(summary.getAssigneeUserId());
        assertNull(summary.getApprovalResult());
        assertEquals("UNKNOWN",summary.getDecisionState());
        assertEquals("REJECTED",summary.getBusinessStatus());
        var entry = adapter.listTimeline(timeline(fixture)).get(0);
        assertNull(entry.getActorUserId());
        assertEquals("COMPLETED", entry.getAction());
        assertNull(entry.getComment());
        verifyNoInteractions(releaseService);
    }

    @Test
    void nonpositiveTaskIdentityIsRejectedBeforeFormalLookup() {
        assertThrows(IllegalStateException.class,
                () -> resolver.resolve(0L, "RELEASE_TRANSACTION", 229L, BATCH, WORK_ORDER));
        assertThrows(IllegalStateException.class,
                () -> resolver.resolve(2741L, "BATCH_EXECUTION", 229L, BATCH, WORK_ORDER));
        verifyNoInteractions(releaseService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"APPROVE", "REJECT"})
    void completedPqcSummaryAndTimelineUseFormalDeciderAndDecisionWithoutChangingOwnership(String decision) {
        PqcFixture fixture = completedPqc(decision);
        exposePqc(fixture);
        var before = pqcSnapshot();
        var summary = adapter.page(query(ApprovalTaskViewType.DONE, false)).getList().get(0);
        assertEquals(9908090346L, summary.getAssigneeUserId());
        assertEquals(decision, summary.getApprovalResult().name());
        var entry = adapter.listTimeline(pqcTimeline(fixture)).get(0);
        assertEquals(9908090346L, entry.getActorUserId());
        assertEquals(decision, entry.getAction());
        assertEquals("APPROVE".equals(decision)?"checked and accepted":"returned",summary.getApprovalRemark());
        assertEquals(summary.getApprovalRemark(),entry.getComment());
        assertEquals("DONE", entry.getStatus());
        assertEquals(before, pqcSnapshot());
        assertEquals(OWNER, tasks.selectById(fixture.task().getId()).getAssigneeUserId());
        assertEquals("1,9908090346,9908090348", tasks.selectById(fixture.task().getId()).getCandidateUserSnapshot());
        assertEquals(OWNER, fixture.summary().getAssigneeUserId());
        if ("REJECT".equals(decision)) verifyNoInteractions(signatures);
        verifyNoInteractions(releaseService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"REPORT_UPLOAD_PENDING", "MANAGER_RELEASE_PENDING", "RELEASED"})
    void completedPqcApprovalRemainsVisibleThroughItsOfficialDownstreamStatuses(String status) {
        var fixture = completedPqc("APPROVE");
        jdbc.update("UPDATE mes_pro_process_pool_active_order_release_application SET application_status=? WHERE id=?", status, fixture.application().getId());
        exposePqc(fixture);
        assertEquals(9908090346L, adapter.page(query(ApprovalTaskViewType.DONE, false)).getList().get(0).getAssigneeUserId());
    }

    @ParameterizedTest(name = "{0} cannot substitute an assigned candidate for a formal PQC decider")
    @ValueSource(strings = {"taskMissing", "taskTenant", "taskDeleted", "taskNotDone", "taskType", "taskScope", "taskApplication",
            "taskOrder", "taskBatch", "taskReason", "taskTime", "applicationMissing", "applicationTenant", "applicationDeleted",
            "applicationTaskFk", "applicationOrder", "applicationBatch", "applicationActive", "applicationPending", "applicationDecision",
            "applicationActor", "applicationTime", "receiptTask", "receiptActor", "receiptSignature", "receiptBatch",
            "userMissing", "userTenant", "userWrongId", "signatureMissing", "signatureOtherActor",
            "signatureOtherAction", "signatureOtherSubject", "signatureCanonical", "signatureHashMismatch", "signaturePersistedStatus", "signatureAuthentication"})
    void completedPqcProjectionFailsForMissingOrContradictoryFormalEvidence(String broken) {
        var fixture = completedPqc("APPROVE");
        Long taskId = fixture.task().getId(), appId = fixture.application().getId();
        switch (broken) {
            case "taskMissing" -> jdbc.update("DELETE FROM mes_pro_edhr_work_task WHERE id=?", taskId);
            case "taskTenant" -> jdbc.update("UPDATE mes_pro_edhr_work_task SET tenant_id=2 WHERE id=?", taskId);
            case "taskDeleted" -> jdbc.update("UPDATE mes_pro_edhr_work_task SET deleted=TRUE WHERE id=?", taskId);
            case "taskNotDone" -> jdbc.update("UPDATE mes_pro_edhr_work_task SET status='TODO' WHERE id=?", taskId);
            case "taskType" -> jdbc.update("UPDATE mes_pro_edhr_work_task SET task_type='REVIEW' WHERE id=?", taskId);
            case "taskScope" -> jdbc.update("UPDATE mes_pro_edhr_work_task SET business_scope_type='BATCH_EXECUTION' WHERE id=?", taskId);
            case "taskApplication" -> jdbc.update("UPDATE mes_pro_edhr_work_task SET business_scope_id=? WHERE id=?", appId+1, taskId);
            case "taskOrder" -> jdbc.update("UPDATE mes_pro_edhr_work_task SET work_order_id=88 WHERE id=?", taskId);
            case "taskBatch" -> jdbc.update("UPDATE mes_pro_edhr_work_task SET batch_execution_id=88 WHERE id=?", taskId);
            case "taskReason" -> jdbc.update("UPDATE mes_pro_edhr_work_task SET reason='REJECT' WHERE id=?", taskId);
            case "taskTime" -> jdbc.update("UPDATE mes_pro_edhr_work_task SET completed_at=? WHERE id=?", fixture.task().getCompletedAt().plusSeconds(1), taskId);
            case "applicationMissing" -> jdbc.update("DELETE FROM mes_pro_process_pool_active_order_release_application WHERE id=?", appId);
            case "applicationTenant" -> jdbc.update("UPDATE mes_pro_process_pool_active_order_release_application SET tenant_id=2 WHERE id=?", appId);
            case "applicationDeleted" -> jdbc.update("UPDATE mes_pro_process_pool_active_order_release_application SET deleted=TRUE WHERE id=?", appId);
            case "applicationTaskFk" -> jdbc.update("UPDATE mes_pro_process_pool_active_order_release_application SET pqc_release_work_task_id=? WHERE id=?", taskId+1, appId);
            case "applicationOrder" -> jdbc.update("UPDATE mes_pro_process_pool_active_order_release_application SET work_order_id=88 WHERE id=?", appId);
            case "applicationBatch" -> jdbc.update("UPDATE mes_pro_process_pool_active_order_release_application SET batch_execution_id=NULL WHERE id=?", appId);
            case "applicationActive" -> jdbc.update("UPDATE mes_pro_process_pool_active_order_release_application SET active_order_id=0 WHERE id=?", appId);
            case "applicationPending" -> jdbc.update("UPDATE mes_pro_process_pool_active_order_release_application SET application_status='PQC_RELEASE_PENDING' WHERE id=?", appId);
            case "applicationDecision" -> jdbc.update("UPDATE mes_pro_process_pool_active_order_release_application SET pqc_decision='NONCONFORMANCE_REWORK' WHERE id=?", appId);
            case "applicationActor" -> jdbc.update("UPDATE mes_pro_process_pool_active_order_release_application SET pqc_decided_by=0 WHERE id=?", appId);
            case "applicationTime" -> jdbc.update("UPDATE mes_pro_process_pool_active_order_release_application SET pqc_decided_at=NULL WHERE id=?", appId);
            case "receiptTask" -> changePqcReceipt(appId, "pqcReleaseWorkTaskId", taskId+1);
            case "receiptActor" -> changePqcReceipt(appId, "decidedBy", OWNER);
            case "receiptSignature" -> changePqcReceipt(appId, "signatureId", null);
            case "receiptBatch" -> changePqcReceipt(appId, "batchExecutionId", BATCH+1);
            case "userMissing" -> when(users.selectOne(any())).thenReturn(null);
            case "userTenant" -> pqcUser(9908090346L, 2L, 0);
            case "userWrongId" -> pqcUser(OWNER, 1L, 0);
            case "signatureMissing" -> when(signatures.getById(22006L)).thenReturn(null);
            case "signatureOtherActor" -> pqcSignature(fixture.application(), OWNER, "PQC_RELEASE", false, null);
            case "signatureOtherAction" -> pqcSignature(fixture.application(), 9908090346L, "MARKET_RELEASE", false, null);
            case "signatureOtherSubject" -> pqcSignature(fixture.application(), 9908090346L, "PQC_RELEASE", true, null);
            case "signatureCanonical" -> pqcSignature(fixture.application(), 9908090346L, "PQC_RELEASE", false, "{}");
            case "signatureHashMismatch" -> {
                when(signatures.verifyEvidence(22006L)).thenReturn(new ElectronicSignatureVerificationDTO(22006L,"MISMATCH","content","different","evidence","evidence","SHA-256","1",
                    "LOCAL_DIGEST_TIME_AND_CREATION_AUDIT", "MISMATCH", "CONSISTENT", "SERVER_CLOCK", "NOT_BOUND",
                    "CONSISTENT", List.of("DIGEST_MISMATCH"), LocalDateTime.of(2026, 10, 7, 12, 0), "V1"));
                var strictPort = (cn.iocoder.yudao.module.signature.service.ElectronicSignatureVerifiedFactService)
                        org.springframework.test.util.ReflectionTestUtils.getField(resolver, "verifiedSignatureFacts");
                var invalidEvidencePort = org.mockito.Mockito.mock(
                        cn.iocoder.yudao.module.signature.service.ElectronicSignatureVerifiedFactService.class,
                        org.mockito.AdditionalAnswers.delegatesTo(strictPort));
                org.mockito.Mockito.doThrow(cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(
                        cn.iocoder.yudao.module.signature.enums.SignatureErrorCodeConstants.ESIGN_COMMAND_INVALID))
                        .when(invalidEvidencePort).requireVerifiedFact(22006L);
                org.springframework.test.util.ReflectionTestUtils.setField(resolver, "verifiedSignatureFacts", invalidEvidencePort);
            }
            case "signaturePersistedStatus" -> pqcSignature(fixture.application(),9908090346L,"PQC_RELEASE",false,null,"REVOKED","SESSION_PLUS_PASSWORD");
            case "signatureAuthentication" -> pqcSignature(fixture.application(),9908090346L,"PQC_RELEASE",false,null,"VALID","DEVICE_PROFILE");
            default -> throw new AssertionError(broken);
        }
        exposePqc(fixture);
        var before = pqcSnapshot();
        if (java.util.Set.of("applicationMissing","applicationTenant","applicationDeleted","applicationDecision").contains(broken)) {
            var summary = adapter.page(query(ApprovalTaskViewType.DONE,false)).getList().get(0);
            var entry = adapter.listTimeline(pqcTimeline(fixture)).get(0);
            assertEquals("applicationDecision".equals(broken)?"UNSUPPORTED":"UNKNOWN",summary.getDecisionState());
            if ("applicationDecision".equals(broken)) assertEquals("NONCONFORMANCE_REWORK",summary.getDecisionSourceStatus());
            assertNull(summary.getApprovalResult());
            assertNull(summary.getAssigneeUserId());
            assertNull(entry.getActorUserId());
            assertNull(entry.getComment());
        } else {
            var summaryFailure = assertThrows(IllegalStateException.class, () -> adapter.page(query(ApprovalTaskViewType.DONE,false)));
            var timelineFailure = assertThrows(IllegalStateException.class, () -> adapter.listTimeline(pqcTimeline(fixture)));
            String prefix = broken.startsWith("task") || java.util.Set.of("applicationTaskFk","applicationOrder","applicationActive").contains(broken)
                    ? "EDHR_APPROVAL_OUTCOME_INVALID:" : "APPROVAL_COMPLETED_ACTOR_INVALID:";
            assertTrue(summaryFailure.getMessage().startsWith(prefix),summaryFailure.getMessage());
            assertTrue(timelineFailure.getMessage().startsWith(prefix),timelineFailure.getMessage());
            if ("signatureHashMismatch".equals(broken)) {
                var summaryCause = assertInstanceOf(cn.iocoder.yudao.framework.common.exception.ServiceException.class, summaryFailure.getCause());
                var timelineCause = assertInstanceOf(cn.iocoder.yudao.framework.common.exception.ServiceException.class, timelineFailure.getCause());
                assertEquals(cn.iocoder.yudao.module.signature.enums.SignatureErrorCodeConstants.ESIGN_COMMAND_INVALID.getCode(), summaryCause.getCode());
                assertEquals(cn.iocoder.yudao.module.signature.enums.SignatureErrorCodeConstants.ESIGN_COMMAND_INVALID.getCode(), timelineCause.getCode());
            }
            if ("signatureCanonical".equals(broken)) {
                assertInstanceOf(IllegalArgumentException.class, summaryFailure.getCause());
                assertInstanceOf(IllegalArgumentException.class, timelineFailure.getCause());
            }
        }
        assertEquals(before, pqcSnapshot(), "invalid read cannot repair task ownership or official decision evidence");
        verifyNoInteractions(releaseService);
    }

    @Test
    void pqcOtherTenantApplicationIsRejectedEvenWhenTenantInterceptorIsIgnored() {
        var fixture = completedPqc("APPROVE");
        jdbc.update("UPDATE mes_pro_process_pool_active_order_release_application SET tenant_id=2 WHERE id=?", fixture.application().getId());
        exposePqc(fixture); TenantContextHolder.setIgnore(true);
        var entry = adapter.listTimeline(pqcTimeline(fixture)).get(0);
        assertEquals("UNKNOWN",entry.getDecisionState());
        assertNull(entry.getActorUserId());
        assertNull(entry.getApprovalResult());
    }

    @ParameterizedTest
    @ValueSource(strings = {"batchExecutionId", "signatureId", "rejectReason", "missingRejectReason"})
    void completedPqcRejectionUsesItsOfficialUnsignedReceiptSchema(String contradicted) {
        var fixture = completedPqc("REJECT");
        switch (contradicted) {
            case "batchExecutionId" -> changePqcReceipt(fixture.application().getId(), "batchExecutionId", BATCH);
            case "signatureId" -> changePqcReceipt(fixture.application().getId(), "signatureId", 22006L);
            case "rejectReason" -> changePqcReceipt(fixture.application().getId(), "rejectReason", "another reason");
            case "missingRejectReason" -> changePqcReceipt(fixture.application().getId(), "rejectReason", null);
            default -> throw new AssertionError(contradicted);
        }
        exposePqc(fixture);
        var before = pqcSnapshot();
        assertThrows(IllegalStateException.class, () -> adapter.page(query(ApprovalTaskViewType.DONE,false)));
        assertThrows(IllegalStateException.class, () -> adapter.listTimeline(pqcTimeline(fixture)));
        assertEquals(before,pqcSnapshot());
        verifyNoInteractions(signatures,releaseService);
    }

    @Test
    void pendingPqcStillUsesFrozenResponsibilityWithoutReadingCompletedDecisionEvidence() {
        var fixture = completedPqc("APPROVE");
        fixture.summary().setStatus("TODO").setCompletedAt(null);
        fixture.task().setStatus("TODO").setCompletedAt(null);
        persistTaskChanges(fixture.task());
        when(workTaskService.getApprovalCenterAuthorizedPrefix(any(),eq(false),eq(false),anyLong()))
                .thenReturn(new PageResult<>(List.of(new MesEdhrAuthorizedTaskProjection(fixture.task(),fixture.summary())),1L));
        when(workTaskService.getApprovalCenterTimelineTasks(fixture.task().getId(),null,false)).thenReturn(List.of(fixture.task()));
        org.mockito.Mockito.clearInvocations(signatures,users);
        assertEquals(OWNER, adapter.page(query(ApprovalTaskViewType.TODO,false)).getList().get(0).getAssigneeUserId());
        assertNull(adapter.listTimeline(pqcTimeline(fixture)).get(0).getActorUserId());
        verifyNoInteractions(signatures,users,releaseService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"application", "scope", "workOrder", "decision", "completedAt"})
    void incompletePqcSummaryCannotUseOriginalAssignedOwner(String field) {
        var fixture = completedPqc("APPROVE");
        switch (field) {
            case "application" -> fixture.task().setBusinessScopeId(null);
            case "scope" -> fixture.task().setBusinessScopeType(null);
            case "workOrder" -> fixture.task().setWorkOrderId(null);
            case "decision" -> fixture.task().setReason(null);
            case "completedAt" -> fixture.task().setCompletedAt(null);
            default -> throw new AssertionError(field);
        }
        exposePqc(fixture);
        assertThrows(IllegalStateException.class, () -> adapter.page(query(ApprovalTaskViewType.DONE,false)));
    }

    @Test
    void missingTenantCannotProjectAssignedOwnerForCompletedPqcDecision() {
        var fixture = completedPqc("APPROVE"); exposePqc(fixture); TenantContextHolder.clear();
        assertThrows(RuntimeException.class, () -> adapter.page(query(ApprovalTaskViewType.DONE,false)));
    }

    @ParameterizedTest @ValueSource(strings={"APPROVE","REJECT"})
    void disabledHistoricalPqcSignerKeepsVerifiedListAndTimelineFacts(String decision) {
        var fixture = completedPqc(decision);
        pqcUser(9908090346L,1L,1);
        exposePqc(fixture);
        var row = adapter.page(query(ApprovalTaskViewType.DONE,false)).getList().get(0);
        var entry = adapter.listTimeline(pqcTimeline(fixture)).get(0);
        assertEquals(decision,row.getApprovalResult().name());
        assertEquals(9908090346L,row.getDecisionActorUserId());
        assertEquals(9908090346L,row.getTaskCompletedByUserId());
        assertEquals(row.getDecisionActorUserId(),entry.getDecisionActorUserId());
        assertEquals(row.getTaskCompletedByUserId(),entry.getActorUserId());
        assertEquals(row.getApprovalRemark(),entry.getComment());
        assertEquals(OWNER,tasks.selectById(fixture.task().getId()).getAssigneeUserId());
        verifyNoInteractions(releaseService);
    }

    private void changePqcReceipt(Long applicationId, String field, Object value) {
        var receipt = cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(
                applications.selectById(applicationId).getDossierSummaryJson(), java.util.LinkedHashMap.class);
        receipt.put(field,value);
        jdbc.update("UPDATE mes_pro_process_pool_active_order_release_application SET dossier_summary_json=? WHERE id=?",
                cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(receipt), applicationId);
    }

    private PqcFixture completedPqc(String decision) {
        var time = LocalDateTime.of(2026,10,5,12,0);
        var application = new MesProcessPoolActiveOrderReleaseApplicationDO().setActiveOrderId(414L).setWorkOrderId(WORK_ORDER)
                .setRouteId(980091L).setRouteVersionId(749L).setBatchExecutionId(BATCH)
                .setApplicationStatus("APPROVE".equals(decision)?"MANAGER_RELEASE_PENDING":"PQC_RELEASE_REJECTED")
                .setPqcDecision(decision).setPqcDecidedBy(9908090346L).setPqcDecidedAt(time).setPqcRejectReason("returned")
                .setSourceSnapshotHash("source").setRequestIdempotencyKey("pqc-completed-request").setBusinessIdempotencyKey("pqc-completed-business");
        application.setTenantId(1L); applications.insert(application);
        var task = new MesProEdhrWorkTaskDO().setTaskCode("PQC-RELEASE-"+application.getId()).setTaskType("PQC_PRODUCTION_RELEASE")
                .setBusinessScopeType("RELEASE_APPLICATION").setBusinessScopeId(application.getId()).setWorkOrderId(WORK_ORDER)
                .setAssigneeUserId(OWNER).setCandidateUserSnapshot("1,9908090346,9908090348").setStatus("DONE")
                .setCompletedAt(time).setReason(decision).setActionUrl("/mes/production-release/pqc?applicationId="+application.getId());
        tasks.insert(task); jdbc.update("UPDATE mes_pro_edhr_work_task SET tenant_id=1 WHERE id=?",task.getId());
        task = tasks.selectById(task.getId());
        application.setPqcReleaseWorkTaskId(task.getId());
        var receipt = new cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesPqcProductionReleaseDecisionResult()
                .setApplicationId(application.getId()).setPqcReleaseWorkTaskId(task.getId()).setDecision(decision)
                .setStatus(application.getApplicationStatus()).setDecidedBy(9908090346L).setDecidedAt(time);
        if ("APPROVE".equals(decision)) receipt.setBatchExecutionId(BATCH).setSignatureId(22006L);
        else receipt.setRejectReason("returned");
        application.setDossierSummaryJson(com.alibaba.fastjson.JSON.toJSONString(receipt));
        applications.updateById(application);
        var summary = new MesProEdhrWorkTaskRespVO().setId(task.getId()).setTaskCode(task.getTaskCode()).setTaskType(task.getTaskType())
                .setBusinessScopeType(task.getBusinessScopeType()).setBusinessScopeId(task.getBusinessScopeId()).setWorkOrderId(WORK_ORDER)
                .setAssigneeUserId(OWNER).setStatus("DONE").setCompletedAt(time).setReason(decision).setActionUrl(task.getActionUrl());
        pqcUser(9908090346L,1L,0);
        if ("APPROVE".equals(decision)) pqcSignature(application,9908090346L,"PQC_RELEASE",false,null);
        return new PqcFixture(application,task,summary);
    }

    private void pqcUser(Long id, Long tenant, Integer status) {
        var user = new AdminUserDO(); user.setId(id); user.setTenantId(tenant); user.setStatus(status);
        when(users.selectOne(any())).thenReturn(user);
    }

    private void pqcSignature(MesProcessPoolActiveOrderReleaseApplicationDO application, Long actor, String action, boolean wrongApplication, String overriddenCanonical) {
        pqcSignature(application,actor,action,wrongApplication,overriddenCanonical,"VALID","SESSION_PLUS_PASSWORD");
    }

    private void pqcSignature(MesProcessPoolActiveOrderReleaseApplicationDO application, Long actor, String action,
                              boolean wrongApplication, String overriddenCanonical, String persistedStatus, String authentication) {
        String subject = MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(application.getBatchExecutionId(),action,
                null,null,null,null,null,null,null,"PQC_RELEASE_APPLICATION",application.getId()+(wrongApplication?1:0),
                "PQC生产放行","PQC_RELEASE",null,null,null,null);
        if ("PQC_RELEASE".equals(action)) {
            subject = MesBatchRecordSignatureSubjectAdapter.withOpinionSource(subject, "checked and accepted");
        }
        String version = MesProBatchRecordExecutionFieldAuditHasher.sha256(subject);
        String canonical = overriddenCanonical == null ? new MesBatchRecordSignatureSubjectAdapter().loadAndAuthorize(new SignatureSubjectCommand(
                actor,"MES",action,"MES_BATCH_RECORD",subject,version,"checked and accepted")).canonicalContentJson() : overriddenCanonical;
        if (overriddenCanonical == null) {
            var business = (com.fasterxml.jackson.databind.node.ObjectNode) cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(canonical);
            business.put("reason", "checked and accepted");
            canonical = cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(business);
        }
        when(signatures.getById(22006L)).thenReturn(new ElectronicSignatureEvidenceDTO(22006L,"MES",action,"MES_BATCH_RECORD",subject,
                version,actor,action,"PQC生产放行","checked and accepted",application.getPqcDecidedAt(),null,authentication,
                "content","evidence","SHA-256","1","1",persistedStatus,null,null,null,null,canonical,null,null,null,"PQC decider"));
        when(signatures.verifyEvidence(22006L)).thenReturn(new ElectronicSignatureVerificationDTO(22006L,"VALID","content","content","evidence","evidence","SHA-256","1",
                "LOCAL_DIGEST_TIME_AND_CREATION_AUDIT", "CONSISTENT", "CONSISTENT", "SERVER_CLOCK", "NOT_BOUND",
                "CONSISTENT", List.of(), LocalDateTime.of(2026, 10, 7, 12, 0), "V1"));
    }

    private void exposePqc(PqcFixture fixture) {
        when(workTaskService.getApprovalCenterAuthorizedPrefix(any(),eq(false),eq(true),anyLong()))
                .thenReturn(new PageResult<>(List.of(new MesEdhrAuthorizedTaskProjection(fixture.task(),fixture.summary())),1L));
        when(workTaskService.getApprovalCenterTimelineTasks(fixture.task().getId(),null,false)).thenReturn(List.of(fixture.task()));
    }

    private ApprovalTaskTimelineQueryContext pqcTimeline(PqcFixture fixture) {
        return ApprovalTaskTimelineQueryContext.of(9908090346L,ApprovalModuleCode.EDHR,"EDHR_WORK_TASK",
                fixture.task().getId().toString(),fixture.task().getId().toString(),null);
    }

    private List<List<Map<String,Object>>> pqcSnapshot() {
        return List.of(snapshotRows("SELECT * FROM mes_pro_edhr_work_task ORDER BY id"),
                snapshotRows("SELECT * FROM mes_pro_process_pool_active_order_release_application ORDER BY id"));
    }

    private List<Map<String,Object>> snapshotRows(String sql) {
        return jdbc.query(sql, (result, rowNumber) -> {
            Map<String,Object> row = new java.util.LinkedHashMap<>();
            var metadata = result.getMetaData();
            for (int column = 1; column <= metadata.getColumnCount(); column++) {
                Object value = result.getObject(column);
                if (value instanceof java.sql.Clob clob) {
                    value = clob.getSubString(1, Math.toIntExact(clob.length()));
                }
                row.put(metadata.getColumnLabel(column), value);
            }
            return row;
        });
    }

    private record PqcFixture(MesProcessPoolActiveOrderReleaseApplicationDO application, MesProEdhrWorkTaskDO task,
                              MesProEdhrWorkTaskRespVO summary) {}

    private Fixture releasedManagerTask() {
        LocalDateTime completedAt = LocalDateTime.of(2026, 10, 4, 12, 0);
        var transaction = new MesProEdhrReleaseTransactionDO().setReleaseCode("MANAGER-RELEASE-227")
                .setBatchExecutionId(BATCH).setWorkOrderId(WORK_ORDER).setReleaseStatus("RELEASED")
                .setApprovedBy(SIGNER).setApprovedAt(completedAt);
        transactions.insert(transaction);
        jdbc.update("UPDATE mes_pro_edhr_release_transaction SET tenant_id = 1 WHERE id = ?", transaction.getId());
        var task = new MesProEdhrWorkTaskDO().setTaskCode("MANAGER-RELEASE-227")
                .setTaskType("RELEASE_APPROVE").setBusinessScopeType("RELEASE_TRANSACTION")
                .setBusinessScopeId(transaction.getId()).setBatchExecutionId(BATCH).setWorkOrderId(WORK_ORDER)
                .setAssigneeUserId(OWNER).setCandidateUserSnapshot(CANDIDATES).setStatus("DONE")
                .setCompletedAt(completedAt).setReason("APPROVE:release accepted").setActionUrl("/market-release");
        tasks.insert(task);
        jdbc.update("UPDATE mes_pro_edhr_work_task SET tenant_id = 1 WHERE id = ?", task.getId());
        task = tasks.selectById(task.getId());
        var application = new MesProcessPoolActiveOrderReleaseApplicationDO().setActiveOrderId(414L).setWorkOrderId(WORK_ORDER)
                .setRouteId(980091L).setRouteVersionId(749L).setBatchExecutionId(BATCH).setApplicationStatus("RELEASED")
                .setReleaseApprovalWorkTaskId(task.getId()).setReleaseTransactionId(transaction.getId())
                .setSourceSnapshotHash("source").setRequestIdempotencyKey("manager-completed-request")
                .setBusinessIdempotencyKey("manager-completed-business");
        application.setTenantId(1L);
        applications.insert(application);
        var decision = new cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseDecisionDO()
                .setReleaseTransactionId(transaction.getId()).setReleaseApplicationId(application.getId())
                .setBatchExecutionId(BATCH).setWorkOrderId(WORK_ORDER).setActiveOrderId(414L).setDecisionStatus("RELEASED")
                .setActorUserId(SIGNER).setDecidedAt(completedAt).setApprovalOpinion("release accepted")
                .setIdempotencyKey("manager-completed-"+transaction.getId()).setPayloadHash("fixture-payload")
                .setAuditSnapshotJson("{}");
        decisions.insert(decision);
        jdbc.update("UPDATE mes_pro_edhr_release_decision SET tenant_id=1 WHERE id=?",decision.getId());
        transaction.setReleaseDecisionId(decision.getId()).setApprovalSignatureId(22007L)
                .setApprovalSignoffEvidenceHash("manager-evidence").setApprovalOpinion("release accepted");
        transactions.updateById(transaction);
        managerSignature(task,completedAt);
        var summary = new MesProEdhrWorkTaskRespVO().setId(task.getId()).setTaskCode(task.getTaskCode())
                .setTaskType(task.getTaskType()).setBusinessScopeType(task.getBusinessScopeType())
                .setBusinessScopeId(task.getBusinessScopeId()).setBatchExecutionId(BATCH).setWorkOrderId(WORK_ORDER)
                .setAssigneeUserId(OWNER).setStatus("DONE").setCompletedAt(completedAt)
                .setReason(task.getReason()).setActionUrl(task.getActionUrl());
        return new Fixture(transaction, task, summary);
    }

    private void exposeDone(Fixture fixture, boolean globalView) {
        when(workTaskService.getApprovalCenterAuthorizedPrefix(any(), eq(globalView), eq(true), anyLong()))
                .thenReturn(new PageResult<>(List.of(new MesEdhrAuthorizedTaskProjection(fixture.task(),fixture.summary())), 1L));
        exposeTimeline(fixture);
    }

    private void exposeTimeline(Fixture fixture) {
        when(workTaskService.getApprovalCenterTimelineTasks(fixture.task().getId(), null, false))
                .thenReturn(List.of(fixture.task()));
    }

    private void persistTaskChanges(MesProEdhrWorkTaskDO task) {
        tasks.updateById(task);
        if (task.getCompletedAt() == null) {
            jdbc.update("UPDATE mes_pro_edhr_work_task SET completed_at=NULL WHERE id=?", task.getId());
        }
        task.setUpdateTime(tasks.selectById(task.getId()).getUpdateTime());
    }

    private void managerSignature(MesProEdhrWorkTaskDO task,LocalDateTime signedAt) {
        String imageHash = "a".repeat(64);
        String id = task.getId().toString();
        String subject = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("EDHR\nEDHR_WORK_TASK\n"+id+"\n"+id+"\n\n"+imageHash).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String canonical = cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(Map.of(
                "adapter","BPM_APPROVAL","moduleCode","EDHR","sourceTaskType","EDHR_WORK_TASK",
                "sourceTaskId",id,"businessKey",id,"processInstanceId","","reviewResult","APPROVE",
                "reason","release accepted","signatureImagePayloadHash",imageHash));
        var signature = new ElectronicSignatureEvidenceDTO(22007L,"BPM","APPROVE","BPM_APPROVAL_TASK",subject,
                cn.hutool.crypto.digest.DigestUtil.sha256Hex(subject),SIGNER,"APPROVE","批准","release accepted",signedAt,
                "time","SESSION_PLUS_PASSWORD","manager-content","manager-evidence","SHA-256","key","v1","VALID",
                "",id,"node",1,canonical,null,null,null,"manager actor");
        when(signatures.getById(22007L)).thenReturn(signature);
        when(signatures.listBySubject("BPM","BPM_APPROVAL_TASK",subject)).thenReturn(List.of(signature));
        when(signatures.verifyEvidence(22007L)).thenReturn(new ElectronicSignatureVerificationDTO(
                22007L,"VALID","manager-content","manager-content","manager-evidence","manager-evidence","SHA-256","v1",
                "LOCAL_DIGEST_TIME_AND_CREATION_AUDIT", "CONSISTENT", "CONSISTENT", "SERVER_CLOCK", "NOT_BOUND",
                "CONSISTENT", List.of(), LocalDateTime.of(2026, 10, 7, 12, 0), "V1"));
    }

    private ApprovalTaskQueryContext query(ApprovalTaskViewType viewType, boolean globalView) {
        return ApprovalTaskQueryContext.of(SIGNER, viewType, ApprovalModuleCode.EDHR, null, 1, 20, globalView);
    }

    private ApprovalTaskTimelineQueryContext timeline(Fixture fixture) {
        return ApprovalTaskTimelineQueryContext.of(SIGNER, ApprovalModuleCode.EDHR, "EDHR_WORK_TASK",
                String.valueOf(fixture.task().getId()), String.valueOf(fixture.task().getId()), null);
    }

    private Snapshot snapshot() {
        return new Snapshot(snapshotRows("SELECT * FROM mes_pro_edhr_work_task ORDER BY id"),
                snapshotRows("SELECT * FROM mes_pro_edhr_release_transaction ORDER BY id"),
                snapshotRows("SELECT * FROM mes_pro_edhr_release_decision ORDER BY id"),
                snapshotRows("SELECT * FROM mes_pro_process_pool_active_order_release_application ORDER BY id"));
    }

    private enum BrokenAssociation {
        TASK_MISSING, TASK_OTHER_TENANT, TASK_DELETED, TASK_NOT_DONE, TASK_OTHER_TYPE, TASK_OTHER_SCOPE,
        TASK_OTHER_TRANSACTION, TASK_OTHER_BATCH, TASK_OTHER_WORK_ORDER, TASK_REJECTED_AS_APPROVED,
        TRANSACTION_MISSING, TRANSACTION_OTHER_TENANT, TRANSACTION_DELETED, TRANSACTION_NOT_RELEASED,
        TRANSACTION_REJECTED, TRANSACTION_OTHER_BATCH, TRANSACTION_OTHER_WORK_ORDER,
        APPROVER_MISSING, APPROVER_ZERO, APPROVER_NEGATIVE
    }

    private record Fixture(MesProEdhrReleaseTransactionDO transaction, MesProEdhrWorkTaskDO task,
                           MesProEdhrWorkTaskRespVO summary) { }

    private record Snapshot(List<Map<String, Object>> tasks, List<Map<String, Object>> transactions,
                            List<Map<String, Object>> decisions, List<Map<String, Object>> applications) { }
}
