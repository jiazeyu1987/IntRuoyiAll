package cn.iocoder.yudao.module.mes.approval;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.bpm.approval.core.*;
import cn.iocoder.yudao.module.bpm.approval.service.*;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MesEdhrApprovalCenterWindowTest {
    private MesProEdhrWorkTaskService tasks;
    private MesActiveOrderHandoffApprovalProjection handoffs;
    private MesProEdhrApprovalTaskAdapter adapter;
    private final Map<Long, cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO> persisted = new HashMap<>();
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 6, 22, 0);

    @BeforeEach
    void setup() {
        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(1L);
        persisted.clear();
        tasks = mock(MesProEdhrWorkTaskService.class);
        handoffs = mock(MesActiveOrderHandoffApprovalProjection.class);
        adapter = new MesProEdhrApprovalTaskAdapter(tasks, mock(MesProEdhrReleaseService.class),
                mock(MesManagerReleaseCompletedActorResolver.class));
        ReflectionTestUtils.setField(adapter, "handoffProjection", handoffs);
        ReflectionTestUtils.setField(adapter, "permissions", mock(cn.iocoder.yudao.module.system.api.permission.PermissionApi.class));
        var mapper = mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper.class);
        when(mapper.selectOne(any())).thenAnswer(call -> {
            var query = (cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<?>) call.getArgument(0);
            query.getSqlSegment();
            return persisted.get(query.getParamNameValuePairs().get("MPGENVAL1"));
        });
        var reader = new MesEdhrApprovalOutcomeReader(mapper,
                mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper.class),
                mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseDecisionMapper.class),
                mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper.class),
                mock(MesManagerReleaseCompletedActorResolver.class),
                mock(cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService.class),
                mock(cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseSignoffService.class),
                mock(MesEdhrExecutionFinalOutcomeReader.class));
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(new org.apache.ibatis.builder.MapperBuilderAssistant(
                new com.baomidou.mybatisplus.core.MybatisConfiguration(), "work"),
                cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO.class);
        ReflectionTestUtils.setField(adapter, "outcomeReader", reader);
        when(handoffs.prefix(any(), anyLong())).thenReturn(new PageResult<>(List.of(), 0L));
    }

    @org.junit.jupiter.api.AfterEach
    void clearTenant() { cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear(); }

    @Test
    void prefix400SupportsPageTwoWithInverseIdAndCreationOrderWithoutHttpPageSize400() {
        var rows = LongStream.rangeClosed(1, 500).mapToObj(id -> row(id, NOW.minusSeconds(id), null)).toList();
        when(tasks.getApprovalCenterAuthorizedPrefix(any(), eq(false), eq(false), eq(400L))).thenAnswer(call -> {
            var filters = (cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskPageReqVO) call.getArgument(0);
            assertTrue(filters.getPageSize() <= 200, "internal K must not be carried by the HTTP PageParam");
            assertEquals("batch", filters.getBatchCode());
            return new PageResult<>(rows.subList(0, 400), 500L);
        });
        var page = adapter.page(context(ApprovalTaskViewType.TODO, 2, 200).setKeyword("batch"));
        assertEquals(500L, page.getTotal());
        assertEquals(LongStream.rangeClosed(201, 400).mapToObj(String::valueOf).toList(), ids(page));
        verify(tasks, never()).getApprovalCenterTodoPage(any(), anyBoolean());
        verify(tasks, never()).getApprovalCenterCandidateSignatureTodoPage(any(), anyBoolean());
        verify(tasks, never()).countApprovalCenterTodoDuplicateTasks(any(), anyBoolean());
    }

    @Test
    void doneUsesCompletedTimeEvenWhenCreationOrderIsOppositeAndMissingTimeIsLast() {
        var completedFirst = row(1, NOW.minusDays(2), NOW);
        var createdFirst = row(2, NOW, NOW.minusDays(2));
        var missing = row(3, NOW.plusDays(1), null);
        for (var row : List.of(completedFirst, createdFirst, missing)) { row.task().setStatus("DONE"); row.display().setStatus("DONE"); }
        when(tasks.getApprovalCenterAuthorizedPrefix(any(), eq(false), eq(true), eq(3L)))
                .thenReturn(new PageResult<>(List.of(createdFirst, missing, completedFirst), 3L));
        assertEquals(List.of("1", "2", "3"), ids(adapter.page(context(ApprovalTaskViewType.DONE, 1, 3))));
    }

    @Test
    void equalTimeStableBinaryIdsCrossPageAndHandoffSourceTypePrecedesWorkTask() {
        var workRows = List.of(row(10, NOW, null), row(2, NOW, null), row(3, NOW, null));
        when(tasks.getApprovalCenterAuthorizedPrefix(any(), eq(false), eq(false), anyLong()))
                .thenReturn(new PageResult<>(workRows, 3L));
        var handoff = ApprovalTaskSummary.builder().moduleCode(ApprovalModuleCode.EDHR)
                .sourceTaskType("ACTIVE_ORDER_HANDOFF").sourceTaskId("99").id("EDHR:ACTIVE_ORDER_HANDOFF:99")
                .taskCreatedAt(NOW).build();
        when(handoffs.prefix(any(), anyLong())).thenReturn(new PageResult<>(List.of(handoff), 1L));
        var first = adapter.page(context(ApprovalTaskViewType.TODO, 1, 2));
        var second = adapter.page(context(ApprovalTaskViewType.TODO, 2, 2));
        assertEquals(List.of("99", "10"), ids(first));
        assertEquals(List.of("2", "3"), ids(second));
        assertEquals(4L, first.getTotal());
        assertTrue(Collections.disjoint(ids(first), ids(second)));
    }

    @Test
    void handoffPrefixCountAddsFullEligibleTotalInsteadOfLoadedPrefixSize() {
        when(tasks.getApprovalCenterAuthorizedPrefix(any(), eq(false), eq(false), eq(1L)))
                .thenReturn(new PageResult<>(List.of(row(1, NOW.minusSeconds(1), null)), 800L));
        var handoff = ApprovalTaskSummary.builder().moduleCode(ApprovalModuleCode.EDHR)
                .sourceTaskType("ACTIVE_ORDER_HANDOFF").sourceTaskId("1").id("EDHR:ACTIVE_ORDER_HANDOFF:1")
                .taskCreatedAt(NOW).build();
        when(handoffs.prefix(any(), eq(1L))).thenReturn(new PageResult<>(List.of(handoff), 900L));
        var page = adapter.page(context(ApprovalTaskViewType.TODO, 1, 1));
        assertEquals(1700L, page.getTotal());
        assertEquals("ACTIVE_ORDER_HANDOFF", page.getList().get(0).getSourceTaskType());
        verify(handoffs, never()).list(any());
    }

    @Test
    void invalidOrUnmaterializableWindowFailsBeforeAnyQuery() {

        assertThrows(IllegalArgumentException.class, () -> adapter.page(context(ApprovalTaskViewType.TODO, 0, 200)));
        verifyNoInteractions(tasks, handoffs);
        long prefix = 429496729400L;
        when(tasks.getApprovalCenterAuthorizedPrefix(any(), eq(false), eq(false), eq(prefix)))
                .thenReturn(new PageResult<>(List.of(), 0L));
        when(handoffs.prefix(any(), eq(prefix))).thenReturn(new PageResult<>(List.of(), 0L));
        assertEquals(0L, adapter.page(context(ApprovalTaskViewType.TODO, Integer.MAX_VALUE, 200)).getTotal());
        verify(tasks).getApprovalCenterAuthorizedPrefix(any(), eq(false), eq(false), eq(prefix));
    }

    private MesEdhrAuthorizedTaskProjection row(long id, LocalDateTime created, LocalDateTime completed) {
        var row = new MesProEdhrWorkTaskRespVO().setId(id).setTaskType("APPROVE").setTaskCode("task-" + id)
                .setAssigneeUserId(12L).setStatus("TODO").setCompletedAt(completed);
        row.setCreateTime(created);
        var task = new cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO()
                .setId(id).setTaskType("APPROVE").setTaskCode("task-" + id)
                .setAssigneeUserId(12L).setStatus("TODO").setCompletedAt(completed);
        task.setCreateTime(created);
        persisted.put(id, task);
        return new MesEdhrAuthorizedTaskProjection(task, row);
    }

    private ApprovalTaskQueryContext context(ApprovalTaskViewType view, int page, int size) {
        return ApprovalTaskQueryContext.of(12L, view, ApprovalModuleCode.EDHR, null, page, size, false);
    }

    private List<String> ids(PageResult<ApprovalTaskSummary> page) {
        return page.getList().stream().map(ApprovalTaskSummary::getSourceTaskId).toList();
    }
}
