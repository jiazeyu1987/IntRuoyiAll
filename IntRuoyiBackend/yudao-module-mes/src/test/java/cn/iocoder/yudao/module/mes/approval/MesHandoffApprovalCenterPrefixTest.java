package cn.iocoder.yudao.module.mes.approval;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.approval.core.*;
import cn.iocoder.yudao.module.bpm.approval.service.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.handoff.MesActiveOrderHandoffTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.handoff.MesActiveOrderHandoffTaskMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MesHandoffApprovalCenterPrefixTest {
    private MesActiveOrderHandoffTaskMapper tasks;
    private MesActiveOrderHandoffApprovalProjection projection;
    private cn.iocoder.yudao.module.system.api.permission.PermissionApi permissions;

    @BeforeEach
    void setup() {
        TenantContextHolder.setTenantId(1L);
        tasks = mock(MesActiveOrderHandoffTaskMapper.class);
        projection = new MesActiveOrderHandoffApprovalProjection();
        ReflectionTestUtils.setField(projection, "tasks", tasks);
        permissions = mock(cn.iocoder.yudao.module.system.api.permission.PermissionApi.class);
        when(permissions.hasAnyPermissions(anyLong(), any(String[].class))).thenReturn(true);
        ReflectionTestUtils.setField(projection, "permissions", permissions);
        ReflectionTestUtils.setField(projection, "outcomeReader", new MesEdhrApprovalOutcomeReader(
                mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper.class),
                mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper.class),
                mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseDecisionMapper.class),
                mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper.class),
                mock(MesManagerReleaseCompletedActorResolver.class),
                mock(cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService.class),
                mock(cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseSignoffService.class),
                mock(MesEdhrExecutionFinalOutcomeReader.class)));
    }

    @AfterEach
    void clear() { TenantContextHolder.clear(); }

    @Test
    void decisionHandoffIsExecutionWorkRatherThanApprovalTodo() {
        var handoff = mock(cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffService.class);
        ReflectionTestUtils.setField(projection, "handoff", handoff);
        var task = target("QA_DECISION_HANDOFF");
        when(tasks.own(1L, 12L)).thenReturn(List.of(task));
        when(handoff.current(task)).thenReturn(true);
        var context = ApprovalTaskQueryContext.of(12L, ApprovalTaskViewType.TODO,
                ApprovalModuleCode.EDHR, null, 1, 20, false);
        assertTrue(projection.list(context).isEmpty());
        task.setStatus("DONE").setCompletedBy(12L).setCompletionSourceId(71L)
                .setCompletedAt(LocalDateTime.of(2026, 10, 7, 1, 0));
        var completed = projection.list(ApprovalTaskQueryContext.of(12L, ApprovalTaskViewType.DONE,
                ApprovalModuleCode.EDHR, null, 1, 20, false)).get(0);
        assertFalse(completed.getRequiresSignature());
        assertTrue(completed.getAvailableActions().isEmpty());
    }

    @Test
    void prefixProjectsExistingFormalModuleEntryAndTotalWithoutAllLoading() {
        var task = task();
        when(tasks.selectApprovalCenterPrefix(1L,12L,false,false,"生产",400L))
                .thenReturn(new PageResult<>(List.of(task),901L));
        var context = ApprovalTaskQueryContext.of(12L,ApprovalTaskViewType.TODO,ApprovalModuleCode.EDHR,"生产",2,200,false);
        var page = projection.prefix(context,400L);
        assertEquals(901L,page.getTotal());
        var summary = page.getList().get(0);
        assertEquals("ACTIVE_ORDER_HANDOFF",summary.getSourceTaskType());
        assertEquals("41",summary.getSourceTaskId());
        assertEquals(task.getCreateTime(),summary.getTaskCreatedAt());
        assertEquals("/mes/pro/feedback/edhr-batch-production-fill",summary.getDetailRoute());
        assertEquals("7",summary.getDetailQuery().get("activeOrderId"));
        assertEquals(Set.of("PROCESS_IN_MODULE"),summary.getAvailableActions());
        verify(tasks,never()).own(anyLong(),anyLong());
        verify(tasks,never()).selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }

    @Test
    void donePreservesFormalCompletedActorTimeAndNoGenericApprovalAction() {
        var task = task().setStatus("DONE").setCompletedBy(12L).setCompletionSourceId(71L).setCompletedAt(LocalDateTime.of(2026,10,7,1,0));
        when(tasks.selectApprovalCenterPrefix(1L,12L,true,true,null,20L))
                .thenReturn(new PageResult<>(List.of(task),1L));
        var summary = projection.prefix(ApprovalTaskQueryContext.of(12L,ApprovalTaskViewType.DONE,ApprovalModuleCode.EDHR,null,1,20,true),20L)
                .getList().get(0);
        assertEquals(12L,summary.getAssigneeUserId());
        assertEquals(task.getCompletedAt(),summary.getTaskCompletedAt());
        assertEquals("COMPLETED", summary.getTaskOutcome());
        assertEquals("UNKNOWN", summary.getDecisionState());
        assertNull(summary.getApprovalResult());
        assertNull(summary.getApprovalRemark());
        assertEquals(12L,summary.getTaskCompletedByUserId());
        assertNull(summary.getDecisionActorUserId());
        assertNull(summary.getDecisionAt());
        assertTrue(summary.getAvailableActions().isEmpty());
        assertEquals("1", summary.getDecisionDetailQuery().get("handoffReadOnly"));
        assertEquals("7", summary.getDecisionDetailQuery().get("cycleId"));
        assertFalse(task.getActionUrl().contains("handoffReadOnly"));
    }

    @Test void everyRealTargetChecksEachMandatoryPermissionWithoutRemovingHistory() {
        for (String type : List.of("PRODUCTION_HANDOFF", "PQC_HANDOFF", "PRODUCTION_RETURN", "PQC_RETURN",
                "PRODUCTION_REVIEW", "PQC_REVIEW", "QA_REVIEW", "QA_DECISION_HANDOFF")) {
            var row = target(type);
            var required = type.endsWith("_HANDOFF") && !type.startsWith("QA_")
                    ? List.of("mes:pro-feedback:query", "mes:pro-feedback:create")
                    : type.endsWith("_RETURN") ? List.of("mes:pro-feedback:query")
                    : type.equals("QA_REVIEW") ? List.of("mes:pro-edhr-nonconformance-review:query", "mes:pro-edhr-nonconformance-review:dispose")
                    : List.of("mes:pro-process-pool-team-leader:query");
            for (String missing : required) {
                when(permissions.hasAnyPermissions(anyLong(), any(String[].class))).thenAnswer(call ->
                        !missing.equals(((String[]) call.getRawArguments()[1])[0]));
                when(tasks.selectApprovalCenterPrefix(1L,12L,false,false,null,20L))
                        .thenReturn(new PageResult<>(List.of(row),1L));
                var summary = projection.prefix(ApprovalTaskQueryContext.of(12L,ApprovalTaskViewType.TODO,
                        ApprovalModuleCode.EDHR,null,1,20,false),20L).getList().get(0);
                assertEquals(missing, summary.getDecisionDetailQuery().get("requiredPermissionsMissing"));
                assertTrue(summary.getAvailableActions().isEmpty());
                assertTrue(summary.getCapabilities().contains(ApprovalTaskCapability.TIMELINE));
                assertEquals("41", summary.getSourceTaskId());
                assertEquals("7", summary.getDecisionDetailQuery().get("activeOrderId"));
                when(tasks.selectById(41L)).thenReturn(row);
                assertEquals(1, projection.timeline(ApprovalTaskTimelineQueryContext.of(12L,ApprovalModuleCode.EDHR,
                        "ACTIVE_ORDER_HANDOFF","41","41",null)).size());
            }
        }
    }

    @Test void doneLeaderEntryKeepsOriginalEventRoundAndReadOnlyDespiteMissingTargetPermission() {
        var row = target("PRODUCTION_REVIEW").setStatus("DONE").setCompletedBy(12L)
                .setCompletedAt(LocalDateTime.of(2026,10,7,1,0)).setCompletionSourceId(71L);
        when(permissions.hasAnyPermissions(anyLong(), any(String[].class))).thenReturn(false);
        when(tasks.selectApprovalCenterPrefix(1L,12L,false,true,null,20L)).thenReturn(new PageResult<>(List.of(row),1L));
        var summary = projection.prefix(ApprovalTaskQueryContext.of(12L,ApprovalTaskViewType.DONE,
                ApprovalModuleCode.EDHR,null,1,20,false),20L).getList().get(0);
        assertEquals("1", summary.getDecisionDetailQuery().get("handoffReadOnly"));
        assertEquals("71", summary.getDecisionDetailQuery().get("eventId"));
        assertEquals("71", summary.getDecisionDetailQuery().get("roundId"));
        assertEquals("mes:pro-process-pool-team-leader:query", summary.getDecisionDetailQuery().get("requiredPermissionsMissing"));
        assertEquals("COMPLETED", summary.getTaskOutcome());
    }

    @Test void voidProfileTargetRequiresAnyOwnHandoffReadPermissionWithoutAffectingTimeline() {
        var row = target("QA_DECISION_HANDOFF").setStatus("DONE").setReason("void：正式作废")
                .setCompletedBy(12L).setCompletedAt(LocalDateTime.of(2026,10,7,1,0)).setCompletionSourceId(71L)
                .setActionUrl("/user/profile?activeOrderId=7&reviewId=71&handoffTaskId=41&roundId=71&handoffType=QA_DECISION_HANDOFF&tab=notifyMessage");
        when(tasks.selectApprovalCenterPrefix(1L,12L,false,true,null,20L)).thenReturn(new PageResult<>(List.of(row),1L));
        when(permissions.hasAnyPermissions(anyLong(), any(String[].class))).thenReturn(false);
        var context = ApprovalTaskQueryContext.of(12L,ApprovalTaskViewType.DONE,ApprovalModuleCode.EDHR,null,1,20,false);
        var denied = projection.prefix(context,20L).getList().get(0);
        assertEquals("查看交接结果缺少本人交接查询权限", denied.getDecisionDetailQuery().get("navigationContractMissing"));
        assertEquals("1", denied.getDecisionDetailQuery().get("handoffReadOnly"));
        when(tasks.selectById(41L)).thenReturn(row);
        assertEquals(2, projection.timeline(ApprovalTaskTimelineQueryContext.of(12L,ApprovalModuleCode.EDHR,
                "ACTIVE_ORDER_HANDOFF","41","41",null)).size());
        when(permissions.hasAnyPermissions(anyLong(), any(String[].class))).thenAnswer(call ->
                java.util.Arrays.asList((String[]) call.getRawArguments()[1]).contains("mes:pro-edhr-work-task:query"));
        var allowed = projection.prefix(context,20L).getList().get(0);
        assertFalse(allowed.getDecisionDetailQuery().containsKey("navigationContractMissing"));
        assertEquals("notifyMessage", allowed.getDecisionDetailQuery().get("tab"));
    }

    private MesActiveOrderHandoffTaskDO target(String type) {
        var row = task().setTaskType(type).setRouteProcessId(type.startsWith("PQC_") ? null : 9L);
        String path, key, source;
        if (type.equals("PRODUCTION_HANDOFF")) { path="/mes/pro/feedback/edhr-batch-production-fill"; key="cycleId"; source="ACTIVE_ORDER"; }
        else if (type.equals("PQC_HANDOFF")) { path="/mes/pro/feedback/edhr-batch-pqc-fill"; key="pqcTaskId"; source="PQC_INSPECTION_TASK"; }
        else if (type.equals("QA_REVIEW")) { path="/mes/pro/feedback/edhr-nonconformance-review"; key="reviewId"; source="NONCONFORMANCE_REVIEW"; }
        else if (type.equals("QA_DECISION_HANDOFF")) { path="/mes/pro/process-pool/production-leader"; key="reviewId"; source="NONCONFORMANCE_REVIEW"; }
        else { path=type.endsWith("_RETURN") ? "/mes/pro/feedback/edhr-batch-"+(type.startsWith("PQC")?"pqc":"production")+"-fill"
                : "/mes/pro/process-pool/"+(type.startsWith("PQC")?"pqc":"production")+"-leader"; key="eventId"; source="PROCESS_POOL_EVENT"; }
        long sourceId = key.equals("cycleId") ? 7L : 71L;
        row.setSourceType(source).setSourceId(sourceId).setRoundId(sourceId)
                .setActionUrl(path+"?activeOrderId=7&"+key+"="+sourceId+"&handoffTaskId=41&roundId="+sourceId+"&handoffType="+type
                        +(type.endsWith("_RETURN")?"&returnTaskId=41&rejectedReviewId="+sourceId:""));
        return row;
    }

    @Test void todoTimelineExposesOnlyItsActualCreationWithoutCompletionOrDecision() {
        var task = task().setInitiatedBy(17L);
        when(tasks.selectById(41L)).thenReturn(task);
        var rows = projection.timeline(ApprovalTaskTimelineQueryContext.of(12L,ApprovalModuleCode.EDHR,
                "ACTIVE_ORDER_HANDOFF","41","41",null));
        assertEquals(1,rows.size());
        var created = rows.get(0);
        assertEquals("CREATED",created.getTaskOutcome()); assertEquals("TODO",created.getSourceStatus());
        assertEquals(17L,created.getActorUserId()); assertEquals(task.getCreateTime(),created.getActedAt());
        assertNull(created.getTaskCompletedAt()); assertNull(created.getTaskCompletedByUserId());
        assertEquals("UNKNOWN",created.getDecisionState()); assertNull(created.getApprovalResult());
        assertNull(created.getDecisionActorUserId()); assertNull(created.getDecisionAt()); assertNull(created.getApprovalRemark());
    }

    @Test void doneTimelineHasDistinctCreationAndCompletionFactsWithoutApprovalDecision() {
        var task = task().setStatus("DONE").setInitiatedBy(17L).setCompletedBy(12L).setCompletionSourceId(71L)
                .setCompletedAt(LocalDateTime.of(2026,10,7,1,0));
        when(tasks.selectById(41L)).thenReturn(task);
        var rows = projection.timeline(ApprovalTaskTimelineQueryContext.of(12L,ApprovalModuleCode.EDHR,
                "ACTIVE_ORDER_HANDOFF","41","41",null));
        assertEquals(2,rows.size());
        var created = rows.get(0); var done = rows.get(1);
        assertEquals("CREATED",created.getTaskOutcome()); assertEquals("TODO",created.getSourceStatus());
        assertEquals(17L,created.getActorUserId()); assertEquals(task.getCreateTime(),created.getActedAt());
        assertNull(created.getTaskCompletedAt()); assertNull(created.getTaskCompletedByUserId());
        assertEquals(12L,done.getTaskCompletedByUserId()); assertEquals(12L,done.getActorUserId());
        assertEquals(task.getCompletedAt(),done.getTaskCompletedAt()); assertEquals(task.getCompletedAt(),done.getActedAt());
        for (var row : rows) {
            assertEquals("UNKNOWN",row.getDecisionState()); assertNull(row.getApprovalResult());
            assertNull(row.getDecisionActorUserId()); assertNull(row.getDecisionAt()); assertNull(row.getApprovalRemark());
        }
    }

    @Test
    void malformedFormalEntryFailsInsteadOfFabricatingNavigation() {
        var task = task().setActionUrl("/mes/pro/feedback/edhr-batch-production-fill?activeOrderId=8&cycleId=7&handoffTaskId=41&roundId=7&handoffType=PRODUCTION_HANDOFF");
        when(tasks.selectApprovalCenterPrefix(1L,12L,false,false,null,20L))
                .thenReturn(new PageResult<>(List.of(task),1L));
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> projection.prefix(ApprovalTaskQueryContext.of(12L,ApprovalTaskViewType.TODO,ApprovalModuleCode.EDHR,null,1,20,false),20L));
    }

    private MesActiveOrderHandoffTaskDO task() {
        var task = new MesActiveOrderHandoffTaskDO().setId(41L).setActiveOrderId(7L).setWorkOrderId(8L)
                .setTaskType("PRODUCTION_HANDOFF").setSourceType("ACTIVE_ORDER").setSourceId(7L).setRoundId(7L)
                .setCandidateUserSnapshot("12,112").setStatus("TODO")
                .setActionUrl("/mes/pro/feedback/edhr-batch-production-fill?activeOrderId=7&cycleId=7&handoffTaskId=41&roundId=7&handoffType=PRODUCTION_HANDOFF");
        task.setTenantId(1L);
        task.setCreateTime(LocalDateTime.of(2026,10,6,23,0));
        return task;
    }
}
