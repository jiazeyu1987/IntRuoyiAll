package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MesProEdhrBatchVisibilitySessionTest {
    private final MesProEdhrBatchExecutionVisibilityService service = new MesProEdhrBatchExecutionVisibilityService();
    private final MesMarketReleaseAccess access = mock(MesMarketReleaseAccess.class);
    private final PermissionApi permissions = mock(PermissionApi.class);
    private final MesProEdhrBatchExecutionTaskMapper tasks = mock(MesProEdhrBatchExecutionTaskMapper.class);
    private final MesProEdhrBatchExecutionOriginMapper origins = mock(MesProEdhrBatchExecutionOriginMapper.class);
    private final MesProcessPoolActiveOrderMapper orders = mock(MesProcessPoolActiveOrderMapper.class);
    private final MesProcessPoolActiveOrderReleaseApplicationMapper applications = mock(MesProcessPoolActiveOrderReleaseApplicationMapper.class);
    private final MesProEdhrReleaseTransactionMapper transactions = mock(MesProEdhrReleaseTransactionMapper.class);
    private final MesProEdhrWorkTaskMapper workTasks = mock(MesProEdhrWorkTaskMapper.class);
    private final MesProBatchRecordReportMapper reports = mock(MesProBatchRecordReportMapper.class);
    private final MesProEdhrProcessFormPermissionRuleMapper rules = mock(MesProEdhrProcessFormPermissionRuleMapper.class);
    private final MesProEdhrCandidateResolver resolver = mock(MesProEdhrCandidateResolver.class);
    private static final long USER = 71L;

    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        Map.of("marketReleaseAccess", access, "permissionApi", permissions, "batchTaskMapper", tasks,
                "batchOriginMapper", origins, "activeOrderMapper", orders, "releaseApplicationMapper", applications,
                "releaseTransactionMapper", transactions, "workTaskMapper", workTasks,
                "reportMapper", reports, "processFormPermissionRuleMapper", rules)
                .forEach((name, bean) -> ReflectionTestUtils.setField(service, name, bean));
        ReflectionTestUtils.setField(service, "candidateResolver", resolver);
        when(access.getAll(anyCollection(), eq(USER))).thenAnswer(call -> {
            Map<Long, MesMarketReleaseAccess.Access> result = new HashMap<>();
            for (Long id : call.<Collection<Long>>getArgument(0)) result.put(id,
                    new MesMarketReleaseAccess.Access(true, true, false, false, null, null, null, null));
            return result;
        });
    }

    @AfterEach void clearTenant() { TenantContextHolder.clear(); }

    @Test void hundredOrdinaryOwnerCandidatesUseOneQueryPerEvidenceTypeAndKeepExactVisibleCount() {
        List<MesProEdhrBatchExecutionDO> batches = LongStream.rangeClosed(1, 100).mapToObj(this::batch).toList();
        when(origins.selectList(any(Wrapper.class))).thenReturn(batches.stream().map(b -> origin(b.getId())).toList());
        when(orders.selectList(any(Wrapper.class))).thenReturn(List.of(order(USER)));
        // A malformed formal association must not inherit the owner's visibility.
        batches.get(99).setRouteVersionId(999L);
        assertEquals(99, service.newVisibilitySession(USER).filterVisible(batches).size());
        verify(origins).selectList(any(Wrapper.class));
        verify(orders).selectList(any(Wrapper.class));
        verify(tasks).selectVisibilityTasksByBatchExecutionIds(List.of(100L));
        verifyNoInteractions(applications);
        verify(transactions).selectLatestByBatchExecutionIds(anyCollection());
        verifyNoInteractions(workTasks);
        verify(origins, never()).selectListByBatchExecutionId(anyLong());
        verify(orders, never()).selectById(anyLong());
        verify(applications, never()).selectByBatchExecutionId(anyLong());
        verify(workTasks, never()).selectById(anyLong());
        verify(permissions).hasAnyPermissions(eq(USER), any(String[].class));
    }

    @Test void ownerAndManagerShortCircuitMalformedDownstreamEvidence() {
        when(origins.selectList(any(Wrapper.class))).thenReturn(List.of(origin(1)));
        when(orders.selectList(any(Wrapper.class))).thenReturn(List.of(order(USER)));
        when(tasks.selectVisibilityTasksByBatchExecutionIds(anyCollection()))
                .thenThrow(new AssertionError("owner must not load form tasks"));
        when(applications.selectList(any(Wrapper.class)))
                .thenThrow(new AssertionError("owner must not inspect downstream release applications"));
        assertEquals(List.of(1L), service.filterVisible(List.of(batch(1)), USER).stream()
                .map(MesProEdhrBatchExecutionDO::getId).toList());
        verifyNoInteractions(tasks, applications, reports, rules, resolver);

        clearInvocations(origins, orders);
        when(transactions.selectLatestByBatchExecutionIds(anyCollection())).thenReturn(List.of(
                new MesProEdhrReleaseTransactionDO().setId(101L).setBatchExecutionId(1L)));
        when(workTasks.selectList(any(Wrapper.class))).thenReturn(List.of(new MesProEdhrWorkTaskDO()
                .setId(102L).setBatchExecutionId(1L).setTaskType("RELEASE_APPROVE")
                .setBusinessScopeType("RELEASE_TRANSACTION").setBusinessScopeId(101L)
                .setOwnershipLocked(true).setStatus("DONE").setCandidateUserSnapshot("71")));
        assertEquals(1, service.filterVisible(List.of(batch(1)), USER).size());
        verifyNoInteractions(origins, orders, tasks, applications, reports, rules, resolver);
    }

    @Test void inconsistentOriginsAndCrossTenantOwnersDoNotGrantAccess() {
        var batch = batch(1);
        var wrong = origin(1).setId(2L).setActiveOrderId(22L);
        when(origins.selectList(any(Wrapper.class))).thenReturn(List.of(origin(1), wrong));
        when(orders.selectList(any(Wrapper.class))).thenReturn(List.of(order(USER)));
        assertTrue(service.filterVisible(List.of(batch), USER).isEmpty());
        when(origins.selectList(any(Wrapper.class))).thenReturn(List.of(origin(1)));
        var crossTenant = order(USER); crossTenant.setTenantId(2L);
        when(orders.selectList(any(Wrapper.class))).thenReturn(List.of(crossTenant));
        assertTrue(service.filterVisible(List.of(batch), USER).isEmpty());
        var session = service.newVisibilitySession(USER);
        TenantContextHolder.setTenantId(2L);
        assertThrows(IllegalStateException.class, () -> session.filterVisible(List.of(batch)));
    }

    @Test void recalledRecipientCannotRegainAccessThroughOwnerOrOverview() {
        when(permissions.hasAnyPermissions(eq(USER), any(String[].class))).thenReturn(true);
        when(access.getAll(anyCollection(), eq(USER))).thenReturn(Map.of(1L,
                new MesMarketReleaseAccess.Access(false, false, false, false, 1L, 72L, 72L, "owner")));
        assertTrue(service.filterVisible(List.of(batch(1)), USER).isEmpty());
        verifyNoInteractions(tasks, origins, orders, applications, transactions, workTasks);
    }

    @Test void approvedFrozenPqcCandidateRemainsReadableButWrongBusinessScopeDoesNot() {
        when(origins.selectList(any(Wrapper.class))).thenReturn(List.of(origin(1)));
        when(orders.selectList(any(Wrapper.class))).thenReturn(List.of(order(72L)));
        var application = new MesProcessPoolActiveOrderReleaseApplicationDO();
        application.setId(31L); application.setTenantId(1L); application.setBatchExecutionId(1L);
        application.setActiveOrderId(20L); application.setWorkOrderId(30L); application.setRouteId(40L);
        application.setRouteVersionId(50L); application.setPqcReleaseWorkTaskId(61L);
        application.setApplicationStatus("RELEASED"); application.setPqcDecision("APPROVE");
        when(applications.selectList(any(Wrapper.class))).thenReturn(List.of(application));
        var task = new MesProEdhrWorkTaskDO().setId(61L).setBatchExecutionId(1L).setWorkOrderId(30L)
                .setRouteId(40L).setTaskType("PQC_PRODUCTION_RELEASE").setBusinessScopeType("RELEASE_APPLICATION")
                .setBusinessScopeId(31L).setOwnershipLocked(true).setCandidateUserSnapshot("71")
                .setStatus("DONE").setReason("APPROVE");
        when(workTasks.selectList(any(Wrapper.class))).thenReturn(List.of(task));
        assertEquals(1, service.filterVisible(List.of(batch(1)), USER).size());
        task.setBusinessScopeId(32L);
        assertTrue(service.filterVisible(List.of(batch(1)), USER).isEmpty());
        when(applications.selectList(any(Wrapper.class))).thenReturn(List.of(application, application));
        assertThrows(org.apache.ibatis.exceptions.TooManyResultsException.class,
                () -> service.filterVisible(List.of(batch(1)), USER));
    }

    @Test void formVersionAndRoutePrecedenceRemainExactAndSourcesResolveOnceAcrossChunks() {
        var task = formTask();
        when(tasks.selectVisibilityTasksByBatchExecutionIds(anyCollection())).thenReturn(List.of(task));
        var report = new MesProBatchRecordReportDO();
        report.setReportId("R"); report.setBatchRecordDefinitionId(80L); report.setBatchRecordVersionId(81L);
        when(reports.selectList(any(Wrapper.class))).thenReturn(List.of(report));
        var route = rule(90L, 41L, 81L, "ROLE", "7");
        var form = rule(89L, 0L, 81L, "USER", "72");
        var legacy = rule(88L, 41L, null, "USER", "72");
        when(rules.selectList(any(Wrapper.class))).thenReturn(List.of(route, form, legacy));
        when(resolver.resolveProcessFormRule(route)).thenReturn(
                new MesProEdhrCandidateResolver.MesProEdhrCandidateContract("ROLE", null, "71"));
        var session = service.newVisibilitySession(USER);
        assertEquals(1, session.filterVisible(List.of(batch(1))).size());
        assertEquals(1, session.filterVisible(List.of(batch(1))).size());
        verify(reports).selectList(any(Wrapper.class));
        verify(rules).selectList(any(Wrapper.class));
        verify(resolver).resolveProcessFormRule(route);
        verify(resolver, never()).resolveProcessFormRule(form);
        verify(resolver, never()).resolveProcessFormRule(legacy);
        // A bad frozen binding denies visibility; a bad candidate source still fails the request.
        task.setBatchRecordVersionId(82L);
        assertTrue(session.filterVisible(List.of(batch(1))).isEmpty());
        task.setBatchRecordVersionId(81L);
        when(resolver.resolveProcessFormRule(route)).thenThrow(new IllegalStateException("bad candidate evidence"));
        assertThrows(IllegalStateException.class, () -> service.filterVisible(List.of(batch(1)), USER));
        // Resolving an earlier owner's access must not skip a later batch's invalid dynamic rule.
        when(origins.selectList(any(Wrapper.class))).thenReturn(List.of(origin(2)));
        when(orders.selectList(any(Wrapper.class))).thenReturn(List.of(order(USER)));
        assertThrows(IllegalStateException.class, () -> service.filterVisible(List.of(batch(2), batch(1)), USER));
    }

    @Test void managerUsesLatestTransactionAndCurrentHandoverActorsKeepTheirReadScope() {
        var latest = new MesProEdhrReleaseTransactionDO().setId(101L).setBatchExecutionId(1L);
        var old = new MesProEdhrReleaseTransactionDO().setId(100L).setBatchExecutionId(1L);
        when(transactions.selectLatestByBatchExecutionIds(anyCollection())).thenReturn(List.of(latest));
        var task = new MesProEdhrWorkTaskDO().setId(102L).setBatchExecutionId(1L)
                .setTaskType("RELEASE_APPROVE").setBusinessScopeType("RELEASE_TRANSACTION")
                .setBusinessScopeId(100L).setOwnershipLocked(true).setStatus("DONE").setCandidateUserSnapshot("71");
        when(workTasks.selectList(any(Wrapper.class))).thenReturn(List.of(task));
        assertTrue(service.filterVisible(List.of(batch(1)), USER).isEmpty());
        task.setBusinessScopeId(101L);
        assertEquals(1, service.filterVisible(List.of(batch(1)), USER).size());
        task.setOwnershipLocked(false);
        assertTrue(service.filterVisible(List.of(batch(1)), USER).isEmpty());
        when(access.getAll(anyCollection(), eq(USER))).thenReturn(Map.of(1L,
                new MesMarketReleaseAccess.Access(true, false, true, true, 1L, USER, 72L, "recipient")));
        assertEquals(1, service.filterVisible(List.of(batch(1)), USER).size());
        when(access.getAll(anyCollection(), eq(USER))).thenReturn(Map.of(1L,
                new MesMarketReleaseAccess.Access(true, true, false, true, 1L, 72L, USER, "recipient")));
        assertEquals(1, service.filterVisible(List.of(batch(1)), USER).size());
    }

    @Test void currentProcessUsesRequiredMainFormAndPreservesControlledSkipEvidence() {
        var skipped = formTask().setId(1L).setRouteProcessId(41L).setStatus(
                MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_SKIPPED).setRequiredPolicy("SKIPPABLE_CONTROLLED");
        var optional = formTask().setId(2L).setRouteProcessId(42L).setRequiredFlag(false);
        var secondary = formTask().setId(3L).setRouteProcessId(43L).setFormSlotType("SECONDARY");
        var main = formTask().setId(4L).setRouteProcessId(43L).setBatchRecordSort(1)
                .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_WAITING);
        assertSame(skipped, service.resolveCurrentProcessTask(List.of(skipped, optional, secondary, main)));
        skipped.setSkippedBy(USER).setSkippedAt(java.time.LocalDateTime.now());
        assertSame(main, service.resolveCurrentProcessTask(List.of(skipped, optional, secondary, main)));
    }

    private MesProEdhrBatchExecutionDO batch(long id) {
        return new MesProEdhrBatchExecutionDO().setId(id).setTenantId(1L).setWorkOrderId(30L)
                .setRouteId(40L).setRouteVersionId(50L);
    }
    private MesProEdhrBatchExecutionOriginDO origin(long batchId) {
        return new MesProEdhrBatchExecutionOriginDO().setId(batchId).setTenantId(1L)
                .setBatchExecutionId(batchId).setActiveOrderId(20L).setWorkOrderId(30L);
    }
    private MesProcessPoolActiveOrderDO order(long owner) {
        var order = new MesProcessPoolActiveOrderDO().setId(20L).setLeaderUserId(owner)
                .setWorkOrderId(30L).setRouteId(40L).setRouteVersionId(50L);
        order.setTenantId(1L); return order;
    }
    private MesProEdhrBatchExecutionTaskDO formTask() {
        return new MesProEdhrBatchExecutionTaskDO().setId(91L).setBatchExecutionId(1L)
                .setNodeType("ROUTE_FORM").setRouteProcessId(41L).setFormSlotType("MAIN")
                .setBatchRecordReportId("R").setBatchRecordDefinitionId(80L).setBatchRecordVersionId(81L)
                .setStatus(10).setRequiredFlag(true);
    }
    private MesProEdhrProcessFormPermissionRuleDO rule(long id, long process, Long version, String type, String ids) {
        return new MesProEdhrProcessFormPermissionRuleDO().setId(id).setRouteProcessId(process)
                .setBatchRecordReportId("R").setBatchRecordVersionId(version).setEnabled(true)
                .setRuleType("FILL").setCandidateSourceType(type).setCandidateSourceIds(ids);
    }
}
