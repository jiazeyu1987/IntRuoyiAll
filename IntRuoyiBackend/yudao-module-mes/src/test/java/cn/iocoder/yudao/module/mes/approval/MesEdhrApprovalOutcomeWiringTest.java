package cn.iocoder.yudao.module.mes.approval;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.approval.core.*;
import cn.iocoder.yudao.module.bpm.approval.service.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.handoff.MesActiveOrderHandoffTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.handoff.MesActiveOrderHandoffTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseSignoffService;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureEvidenceDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real Spring injection, work service, adapter and reader; persistence/signature ports are fixtures. */
class MesEdhrApprovalOutcomeWiringTest {
    private static final LocalDateTime AT = LocalDateTime.of(2026, 10, 7, 10, 0);
    private static final String OPINION = "原意见\n  \"引号\" \\ 保留";
    private AnnotationConfigApplicationContext beans;
    private MockedStatic<SecurityFrameworkUtils> login;
    private MesProEdhrApprovalTaskAdapter adapter;
    private MesProEdhrWorkTaskMapper tasks;
    private MesActiveOrderHandoffTaskMapper handoffs;
    private MesProcessPoolActiveOrderReleaseApplicationMapper applications;
    private MesManagerReleaseCompletedActorResolver actors;
    private ElectronicSignatureQueryService signatures;
    private ReviewFixture reviewFixture;

    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        login = mockStatic(SecurityFrameworkUtils.class);
        login.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(12L);
        beans = new AnnotationConfigApplicationContext();
        for (var type : List.of(MesProEdhrWorkTaskServiceImpl.class, MesActiveOrderHandoffApprovalProjection.class, MesProEdhrApprovalTaskAdapter.class)) {
            for (var field : type.getDeclaredFields()) {
                if (field.isAnnotationPresent(Resource.class) && field.getType() != MesEdhrApprovalOutcomeReader.class
                        && field.getType() != MesActiveOrderHandoffApprovalProjection.class) {
                    registerPort(field.getType());
                }
            }
        }
        for (var type : List.of(MesProEdhrReleaseService.class, MesManagerReleaseCompletedActorResolver.class,
                MesProEdhrReleaseDecisionMapper.class, ElectronicSignatureQueryService.class,
                MesProductionReleaseSignoffService.class, MesEdhrExecutionFinalOutcomeReader.class)) registerPort(type);
        beans.registerBean(MesProEdhrWorkTaskServiceImpl.class);
        beans.registerBean(MesEdhrApprovalOutcomeReader.class);
        beans.registerBean(MesActiveOrderHandoffApprovalProjection.class);
        beans.registerBean(MesProEdhrApprovalTaskAdapter.class);
        beans.refresh();
        adapter = beans.getBean(MesProEdhrApprovalTaskAdapter.class);
        tasks = beans.getBean(MesProEdhrWorkTaskMapper.class);
        handoffs = beans.getBean(MesActiveOrderHandoffTaskMapper.class);
        applications = beans.getBean(MesProcessPoolActiveOrderReleaseApplicationMapper.class);
        actors = beans.getBean(MesManagerReleaseCompletedActorResolver.class);
        signatures = beans.getBean(ElectronicSignatureQueryService.class);
        when(beans.getBean(AdminUserApi.class).getUserMap(anyCollection())).thenReturn(Map.of());
        when(handoffs.selectApprovalCenterPrefix(eq(1L),eq(12L),anyBoolean(),anyBoolean(),nullable(String.class),anyLong()))
                .thenReturn(new PageResult<>(List.of(),0L));
    }

    private <T> void registerPort(Class<T> type) {
        // External ports are already constructed mocks, not real services to autowire.
        if (!beans.getBeanFactory().containsSingleton(type.getName())) {
            beans.getBeanFactory().registerSingleton(type.getName(), mock(type));
        }
    }

    @AfterEach void cleanup() {
        if (beans != null) beans.close();
        if (login != null) login.close();
        TenantContextHolder.clear();
    }

    @ParameterizedTest
    @ValueSource(strings={"APPROVE","REJECT","FUTURE",""})
    void formalPqcOutcomeAndRawOpinionAreIdenticalInListTimelineAndJson(String decision) throws Exception {
        var task = task("PQC_PRODUCTION_RELEASE").setBusinessScopeType("RELEASE_APPLICATION")
                .setBusinessScopeId(95L).setReason(decision);
        expose(task);
        var app = application().setPqcReleaseWorkTaskId(177L).setPqcDecision(decision)
                .setPqcDecidedBy(188L).setPqcDecidedAt(AT).setPqcRejectReason(OPINION)
                .setApplicationStatus("REJECT".equals(decision)?"PQC_RELEASE_REJECTED":"MANAGER_RELEASE_PENDING")
                .setDossierSummaryJson("{\"signatureId\":88}");
        when(applications.selectOne(any())).thenReturn(app);
        when(applications.selectByIds(anyCollection())).thenReturn(List.of(app));
        if (Set.of("APPROVE","REJECT").contains(decision)) {
            when(actors.resolvePqc(177L,"RELEASE_APPLICATION",95L,null,93L,decision,AT)).thenReturn(188L);
        }
        if ("APPROVE".equals(decision)) {
            var signature = mock(ElectronicSignatureEvidenceDTO.class);
            when(signature.id()).thenReturn(88L);
            when(signature.actorId()).thenReturn(188L);
            when(signature.reason()).thenReturn(OPINION);
            String subject = MesBatchRecordSignatureSubjectAdapter.withOpinionSource(
                    MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(null,"PQC_RELEASE",
                            null,null,null,null,null,null,null,"PQC_RELEASE_APPLICATION",95L,
                            "PQC生产放行","PQC_RELEASE",null,null,null,null), OPINION);
            when(signature.subjectId()).thenReturn(subject);
            when(signatures.getById(88L)).thenReturn(signature);
        }
        var row = page(false).getList().get(0);
        var entry = adapter.listTimeline(timeline("EDHR_WORK_TASK","177",false)).get(0);
        var proven = Set.of("APPROVE","REJECT").contains(decision);
        assertEquals("COMPLETED",row.getTaskOutcome());
        assertEquals("DONE",row.getSourceStatus());
        assertEquals(app.getApplicationStatus(),row.getBusinessStatus());
        assertEquals(proven?decision:decision.isEmpty()?"UNKNOWN":"UNSUPPORTED",row.getDecisionState());
        assertEquals(proven?ApprovalTaskReviewResult.valueOf(decision):null,row.getApprovalResult());
        assertEquals(proven?OPINION:null,row.getApprovalRemark());
        assertEquals(proven?188L:null,row.getAssigneeUserId());
        assertEquals(decision,row.getSourceRemark());
        assertEquals(row.getApprovalResult(),entry.getApprovalResult());
        assertEquals(row.getDecisionState(),entry.getDecisionState());
        assertEquals(row.getBusinessStatus(),entry.getBusinessStatus());
        assertEquals(row.getDecisionSourceStatus(),entry.getDecisionSourceStatus());
        assertEquals(row.getApprovalRemark(),entry.getComment());
        assertEquals(row.getApprovalRemark(),entry.getApprovalRemark());
        assertEquals(row.getSourceRemark(),entry.getSourceRemark());
        assertEquals(row.getDecisionActorUserId(),entry.getActorUserId());
        assertEquals(row.getDecisionAt(),entry.getActedAt());
        assertEquals(row.getEvidenceId(),entry.getEvidenceId());
        assertEquals(proven?decision:"COMPLETED",entry.getAction());
        var json = new ObjectMapper().findAndRegisterModules();
        var listJson = json.readTree(json.writeValueAsString(row));
        var timelineJson = json.readTree(json.writeValueAsString(entry));
        for (var field : List.of("taskOutcome","sourceStatus","businessStatus","decisionState",
                "decisionSourceStatus","approvalResult","approvalRemark","sourceRemark","terminationReason",
                "decisionActorUserId","decisionAt","evidenceType","evidenceId")) {
            assertTrue(listJson.has(field),field);
            assertEquals(listJson.get(field),timelineJson.get(field),field);
        }
        verify(tasks).selectApprovalCenterPrefix(any(),eq(12L),eq(1L),eq(true),eq("TODO"),eq(10L));
        assertEquals(999L,task.getAssigneeUserId(),"formal completion actor does not mutate frozen owner");
        verifyNoInteractions(beans.getBean(MesProEdhrReleaseService.class));
    }

    @Test void unknownCompletedTaskDoesNotUseDoneRejectTextOrOwnerAsDecision() {
        var task = formalReviewTask(177L).setReason("REJECT: 原来源备注");
        expose(task);
        var row = page(false).getList().get(0);
        var entry = adapter.listTimeline(timeline("EDHR_WORK_TASK","177",false)).get(0);
        assertNull(row.getApprovalResult());
        assertNull(row.getAssigneeUserId());
        assertNull(entry.getActorUserId());
        assertNull(entry.getComment());
        assertEquals("UNKNOWN",row.getDecisionState());
        assertEquals("任务已完成",entry.getActionLabel());
        assertEquals(task.getReason(),row.getSourceRemark());
        assertEquals(row.getSourceRemark(),entry.getSourceRemark());
        verifyNoInteractions(actors,signatures);
    }

    @Test void managerReleaseUsesExactFormalDecisionOpinionAndEvidenceInBothViews() {
        var task = task("RELEASE_APPROVE").setBusinessScopeType("RELEASE_TRANSACTION").setBusinessScopeId(91L)
                .setBatchExecutionId(92L).setReason("APPROVE");
        expose(task);
        var tx = new cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO()
                .setId(91L).setBatchExecutionId(92L).setWorkOrderId(93L).setReleaseDecisionId(94L)
                .setReleaseStatus("RELEASED").setApprovedBy(188L).setApprovedAt(AT).setApprovalSignatureId(88L)
                .setApprovalSignoffEvidenceHash("evidence").setApprovalOpinion(OPINION);
        var decision = new cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseDecisionDO()
                .setId(94L).setReleaseTransactionId(91L).setBatchExecutionId(92L).setWorkOrderId(93L)
                .setReleaseApplicationId(95L).setDecisionStatus("RELEASED").setActorUserId(188L)
                .setDecidedAt(AT).setApprovalOpinion(OPINION);
        var app = application().setReleaseApprovalWorkTaskId(177L).setReleaseTransactionId(91L).setBatchExecutionId(92L);
        var transactions = beans.getBean(MesProEdhrReleaseTransactionMapper.class);
        when(transactions.selectListByIds(anyCollection())).thenReturn(List.of(tx));
        when(transactions.selectOne(any())).thenReturn(tx);
        when(beans.getBean(MesProEdhrReleaseDecisionMapper.class).selectOne(any())).thenReturn(decision);
        when(applications.selectOne(any())).thenReturn(app);
        when(actors.resolve(177L,"RELEASE_TRANSACTION",91L,92L,93L)).thenReturn(188L);
        var signature = mock(ElectronicSignatureEvidenceDTO.class);
        when(signature.id()).thenReturn(88L);
        when(signature.reason()).thenReturn(OPINION);
        when(signature.subjectId()).thenReturn("subject");
        when(signatures.getById(88L)).thenReturn(signature);
        when(beans.getBean(MesProductionReleaseSignoffService.class).findVerifiedSignatureId(
                177L,188L,"subject","evidence",OPINION)).thenReturn(Optional.of(88L));
        // The authorized batch timeline contains this exact persisted task.
        when(tasks.selectTimelineListByBatchExecutionId(92L)).thenReturn(List.of(task));
        var row = page(false).getList().get(0);
        var entry = adapter.listTimeline(timeline("EDHR_WORK_TASK","177",false)).get(0);
        assertEquals(ApprovalTaskReviewResult.APPROVE,row.getApprovalResult());
        assertEquals("RELEASED",row.getBusinessStatus());
        assertEquals("EXISTING_MARKET_RELEASE_DECISION",row.getEvidenceType());
        assertEquals("94",row.getEvidenceId());
        assertEquals(OPINION,row.getApprovalRemark());
        assertEquals(row.getApprovalRemark(),entry.getComment());
        assertEquals(row.getApprovalResult(),entry.getApprovalResult());
        assertEquals(row.getEvidenceId(),entry.getEvidenceId());
        assertEquals(188L,entry.getActorUserId());
        assertEquals("APPROVE",row.getSourceRemark());
    }

    @ParameterizedTest
    @ValueSource(strings={"CANCELED","LEGACY_UNKNOWN"})
    void taskCancellationOrUnsupportedCodeDoesNotBecomeOpinionOrSystemReason(String status) {
        var task = formalReviewTask(177L).setStatus(status).setReason("原始未分类来源");
        expose(task);
        var row = page(false).getList().get(0);
        var entry = adapter.listTimeline(timeline("EDHR_WORK_TASK","177",false)).get(0);
        assertEquals("CANCELED".equals(status)?"CANCELLED":"UNSUPPORTED",row.getTaskOutcome());
        assertEquals(status,row.getSourceStatus());
        assertNull(row.getApprovalResult());
        assertNull(row.getApprovalRemark());
        assertNull(row.getTerminationReason());
        assertNull(entry.getComment());
        assertEquals(task.getCompletedAt(),entry.getTaskCompletedAt());
        assertNull(entry.getDecisionAt());
        assertEquals(row.getSourceRemark(),entry.getSourceRemark());
    }

    @Test void crossObjectFormalApplicationFailsInBothAuthorizedListAndTimeline() {
        var task = task("PQC_PRODUCTION_RELEASE").setBusinessScopeType("RELEASE_APPLICATION").setBusinessScopeId(95L);
        expose(task);
        var app = application().setPqcReleaseWorkTaskId(178L).setPqcDecision("APPROVE");
        when(applications.selectOne(any())).thenReturn(app);
        when(applications.selectByIds(anyCollection())).thenReturn(List.of(app));
        assertThrows(IllegalStateException.class,() -> page(false));
        assertThrows(IllegalStateException.class,() -> adapter.listTimeline(timeline("EDHR_WORK_TASK","177",false)));
        verifyNoInteractions(actors,signatures);
    }

    @Test void enrichmentPreservesPersonalGlobalQueryTotalAndCompletePersistedRow() {
        var task = formalReviewTask(177L);
        expose(task);
        assertEquals(7L,page(false).getTotal());
        assertEquals(7L,page(true).getTotal());
        verify(tasks).selectApprovalCenterPrefix(any(),eq(12L),eq(1L),eq(true),eq("TODO"),eq(10L));
        verify(tasks).selectApprovalCenterPrefix(any(),isNull(),eq(1L),eq(true),eq("TODO"),eq(10L));
        var projection = beans.getBean(MesProEdhrWorkTaskService.class).getApprovalCenterAuthorizedPrefix(
                new cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskPageReqVO(),false,true,400);
        assertSame(task,projection.getList().get(0).task(),"must retain actual complete DO, not reconstruct VO");
        assertEquals(7L,projection.getTotal());
    }

    @Test void enrichmentKeepsCompletedTimeNullLastOrderAndFullCount() {
        var newest = formalReviewTask(10L);
        newest.setCreateTime(AT.minusDays(10));
        var older = formalReviewTask(2L).setCompletedAt(AT.minusDays(2));
        older.setCreateTime(AT.plusDays(1));
        var missing = formalReviewTask(3L).setCompletedAt(null);
        missing.setCreateTime(AT.plusDays(2));
        when(tasks.selectApprovalCenterPrefix(any(),eq(12L),eq(1L),eq(true),eq("TODO"),eq(10L)))
                .thenReturn(new PageResult<>(List.of(newest,older,missing),700L));
        when(tasks.selectOne(any())).thenReturn(newest,older,missing);
        var page = page(false);
        assertEquals(700L,page.getTotal());
        assertEquals(List.of("10","2","3"),page.getList().stream().map(ApprovalTaskSummary::getSourceTaskId).toList());
        assertTrue(page.getList().stream().allMatch(row -> row.getApprovalResult()==null));
        assertNull(page.getList().get(2).getTaskCompletedAt());
    }

    @Test void requiredLoginOrTenantFailsBeforeAuthorizedPrefixOrOutcomeReads() {
        var service = beans.getBean(MesProEdhrWorkTaskService.class);
        login.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(null);
        assertThrows(RuntimeException.class,() -> service.getApprovalCenterAuthorizedPrefix(
                new cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskPageReqVO(),true,true,400));
        login.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(12L);
        TenantContextHolder.clear();
        assertThrows(RuntimeException.class,() -> service.getApprovalCenterAuthorizedPrefix(
                new cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskPageReqVO(),true,true,400));
        verify(tasks,never()).selectApprovalCenterPrefix(any(),nullable(Long.class),anyLong(),anyBoolean(),anyString(),anyLong());
        verify(tasks,never()).selectOne(any());
    }

    @Test void timelinePermissionDeniesBeforeFormalEnrichmentWhileGlobalRetainsExistingAccess() {
        var task = task("REVIEW").setCandidateUserSnapshot("112");
        expose(task);
        assertThrows(IllegalStateException.class,() -> adapter.listTimeline(timeline("EDHR_WORK_TASK","177",false)));
        verify(tasks,times(1)).selectOne(any()); // source lookup only, no outcome enrichment
        assertEquals("UNKNOWN",adapter.listTimeline(timeline("EDHR_WORK_TASK","177",true)).get(0).getDecisionState());
    }

    @Test void handoffBusinessCompletionAndOriginRemarkNeverBecomeApprovalOrOpinion() {
        var task = handoff().setStatus("DONE").setCompletedBy(12L).setCompletionSourceId(91L).setCompletedAt(AT)
                .setReason("创建时冻结来源");
        when(tasks.selectApprovalCenterPrefix(any(),nullable(Long.class),eq(1L),eq(true),eq("TODO"),anyLong()))
                .thenReturn(new PageResult<>(List.of(),0L));
        when(handoffs.selectApprovalCenterPrefix(1L,12L,false,true,null,10L)).thenReturn(new PageResult<>(List.of(task),1L));
        when(handoffs.selectById(41L)).thenReturn(task);
        var row = page(false).getList().get(0);
        var entries = adapter.listTimeline(timeline("ACTIVE_ORDER_HANDOFF","41",false));
        var completed = entries.get(1);
        assertEquals("COMPLETED",row.getTaskOutcome());
        assertEquals(12L,row.getTaskCompletedByUserId());
        assertNull(row.getDecisionActorUserId());
        assertNull(row.getDecisionAt());
        assertNull(row.getApprovalResult());
        assertNull(row.getApprovalRemark());
        assertNull(completed.getComment());
        assertEquals("UNKNOWN",completed.getDecisionState());
        assertEquals(row.getDecisionAt(),completed.getDecisionAt());
        assertEquals(row.getEvidenceId(),completed.getEvidenceId());
        assertEquals(task.getReason(),row.getSourceRemark());
        assertEquals(task.getReason(),completed.getSourceRemark());
        assertNull(entries.get(0).getApprovalResult());
        assertNull(entries.get(0).getComment());
        assertEquals("CREATED",entries.get(0).getTaskOutcome());
        assertEquals("TODO",entries.get(0).getSourceStatus());
        assertNull(entries.get(0).getTaskCompletedAt());
        assertNull(entries.get(0).getTaskCompletedByUserId());
        assertEquals(task.getCreateTime(),entries.get(0).getActedAt());
        assertEquals(12L,completed.getActorUserId());
        assertEquals(task.getCompletedAt(),completed.getActedAt());
        task.setCandidateUserSnapshot("112");
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> adapter.listTimeline(timeline("ACTIVE_ORDER_HANDOFF","41",false)));
        assertEquals(2,adapter.listTimeline(timeline("ACTIVE_ORDER_HANDOFF","41",true)).size());
    }

    /** Normal FILL -> REVIEW identities; separate from application/manager release fixtures. */
    private record ReviewFixture(Long tenantId, MesProWorkOrderDO workOrder,
            MesProEdhrBatchExecutionDO batch, MesProEdhrBatchExecutionTaskDO batchTask,
            MesProBatchRecordExecutionDO execution, MesProEdhrWorkTaskDO submittedFill) { }

    private MesProEdhrWorkTaskDO formalReviewTask(Long taskId) {
        if (reviewFixture == null) {
            Long tenant = TenantContextHolder.getRequiredTenantId();
            var workOrder = new MesProWorkOrderDO();
            workOrder.setId(93L);
            workOrder.setTenantId(tenant);
            var batch = new MesProEdhrBatchExecutionDO().setId(601L).setTenantId(tenant)
                    .setWorkOrderId(workOrder.getId());
            var execution = new MesProBatchRecordExecutionDO().setId(603L)
                    .setBatchExecutionId(batch.getId()).setWorkOrderId(workOrder.getId());
            var batchTask = new MesProEdhrBatchExecutionTaskDO().setId(602L)
                    .setBatchExecutionId(batch.getId()).setExecutionId(execution.getId())
                    .setNodeType(MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_ROUTE_FORM)
                    .setProcessName("正式批记录审核夹具");
            var submittedFill = task("FILL").setId(176L).setBatchExecutionId(batch.getId())
                    .setBatchTaskId(batchTask.getId()).setExecutionId(execution.getId())
                    .setWorkOrderId(workOrder.getId()).setBusinessScopeType("BATCH_TASK")
                    .setBusinessScopeId(batchTask.getId());
            reviewFixture = new ReviewFixture(tenant, workOrder, batch, batchTask, execution, submittedFill);
            when(beans.getBean(MesProEdhrBatchExecutionMapper.class).selectById(batch.getId())).thenReturn(batch);
            when(beans.getBean(MesProWorkOrderMapper.class).selectById(workOrder.getId())).thenReturn(workOrder);
            when(beans.getBean(MesProEdhrBatchExecutionTaskMapper.class).selectByIds(anyCollection()))
                    .thenAnswer(call -> ((Collection<?>) call.getArgument(0)).contains(batchTask.getId())
                            ? List.of(batchTask) : List.of());
        }
        if (!Objects.equals(reviewFixture.tenantId(), TenantContextHolder.getRequiredTenantId())) {
            throw new IllegalStateException("REVIEW_FIXTURE_TENANT_CONFLICT");
        }
        var source = reviewFixture.submittedFill();
        var review = task("REVIEW").setId(taskId).setBatchExecutionId(source.getBatchExecutionId())
                .setBatchTaskId(source.getBatchTaskId()).setExecutionId(source.getExecutionId())
                .setWorkOrderId(source.getWorkOrderId()).setBusinessScopeType("BATCH_TASK")
                .setBusinessScopeId(source.getBatchTaskId());
        // This existing external port represents absence of a final decision/snapshot.
        // Its fixture includes the historical null-completion row; it is not a test of
        // MesEdhrExecutionFinalOutcomeReader or of the real FILL/REVIEW writer.
        when(beans.getBean(MesEdhrExecutionFinalOutcomeReader.class).read(same(review),eq(reviewFixture.tenantId())))
                .thenAnswer(call -> new MesEdhrApprovalOutcome("COMPLETED",null,null,review.getStatus(),
                        "UNKNOWN",null,null,review.getReason(),null,null,null,null,null,null));
        return review;
    }

    private void expose(MesProEdhrWorkTaskDO task) {
        when(tasks.selectApprovalCenterPrefix(any(),nullable(Long.class),eq(1L),anyBoolean(),eq("TODO"),anyLong()))
                .thenReturn(new PageResult<>(List.of(task),7L));
        when(tasks.selectOne(any())).thenReturn(task);
        when(tasks.selectById(task.getId())).thenReturn(task);
        if ("BATCH_TASK".equals(task.getBusinessScopeType()) && task.getExecutionId() != null) {
            when(tasks.selectTimelineListByExecutionId(task.getExecutionId())).thenReturn(List.of(task));
        }
    }

    private PageResult<ApprovalTaskSummary> page(boolean global) {
        return adapter.page(ApprovalTaskQueryContext.of(12L,ApprovalTaskViewType.DONE,ApprovalModuleCode.EDHR,null,1,10,global));
    }
    private ApprovalTaskTimelineQueryContext timeline(String type,String id,boolean global) {
        return ApprovalTaskTimelineQueryContext.of(12L,ApprovalModuleCode.EDHR,type,id,id,null).setGlobalView(global);
    }
    private static MesProEdhrWorkTaskDO task(String type) {
        var task = new MesProEdhrWorkTaskDO().setId(177L).setTaskCode("WT177").setTaskType(type).setStatus("DONE")
                .setAssigneeUserId(999L).setCandidateUserSnapshot("12,112").setWorkOrderId(93L).setCompletedAt(AT);
        task.setCreateTime(AT.minusDays(1));
        return task;
    }
    private static MesProcessPoolActiveOrderReleaseApplicationDO application() {
        var app = new MesProcessPoolActiveOrderReleaseApplicationDO().setId(95L).setActiveOrderId(180L).setWorkOrderId(93L);
        app.setTenantId(1L);
        return app;
    }
    private static MesActiveOrderHandoffTaskDO handoff() {
        var task = new MesActiveOrderHandoffTaskDO().setId(41L).setActiveOrderId(7L).setWorkOrderId(8L)
                .setTaskType("PRODUCTION_HANDOFF").setSourceType("ACTIVE_ORDER").setSourceId(7L).setRoundId(7L)
                .setCandidateUserSnapshot("12,112").setActionUrl("/mes/pro/feedback/edhr-batch-production-fill"
                        + "?activeOrderId=7&cycleId=7&handoffTaskId=41&roundId=7&handoffType=PRODUCTION_HANDOFF");
        task.setTenantId(1L);
        task.setCreateTime(AT.minusDays(1));
        return task;
    }
}
