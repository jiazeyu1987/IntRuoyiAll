package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Reflective construction permits a behavioral RED before the helper exists. */
class MesFixedTestOrderDownstreamCleanupServiceTest {
    private static final String SERVICE = "cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFixedTestOrderDownstreamCleanupService";
    private static final String MAPPER = "cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesFixedTestOrderDownstreamCleanupMapper";
    private final List<String> calls = new ArrayList<>();
    private List<Long> taskIds = List.of(801L);
    private List<Long> deviationIds = List.of(901L);
    private boolean cancellationFails;
    private boolean scopeConflict;

    @AfterEach
    void clearTenant() { TenantContextHolder.clear(); }

    @Test
    void ownershipConflictRejectsResetBeforeCancellationOrDeletion() throws Exception {
        scopeConflict = true;
        var error = assertThrows(IllegalStateException.class, () -> cleanup(service(), 1L, List.of(301L)));
        assertEquals("FIXED_TEST_RESET_DOWNSTREAM_OWNERSHIP_CONFLICT", error.getMessage());
        assertTrue(calls.stream().noneMatch(name -> name.startsWith("delete") || name.startsWith("cancel")));
    }

    @Test
    void cancelsReleaseApplicationTaskWithoutBatchBeforeDeletingIt() throws Exception {
        Object service = service();
        cleanup(service, 1L, List.of());
        assertTrue(calls.contains("cancelTasksForTestReset:[801]"));
        assertTrue(calls.indexOf("cancelTasksForTestReset:[801]") < calls.indexOf("deleteWorkTasks"));
        assertTrue(calls.indexOf("cancelBpm") < calls.indexOf("selectWorkTaskIdsForUpdate:1:71:[]:[]:[601]"));
        assertTrue(calls.contains("selectWorkTaskIdsForUpdate:1:71:[]:[]:[601]"));
    }

    @Test
    void removesDeviationChildrenAndRequestBeforeParentAndAllWorkOrderNcr() throws Exception {
        cleanup(service(), 1L, List.of(301L));
        assertTrue(calls.contains("selectDeviationIdsForUpdate:1:71:[301]"));
        assertTrue(calls.indexOf("deleteDeviationHandling") < calls.indexOf("deleteDeviations"));
        assertTrue(calls.indexOf("deleteDeviationRequests") < calls.indexOf("deleteDeviations"));
        assertTrue(calls.contains("deleteNonconformanceReviewsForWorkOrder"));
    }

    @Test
    void cancellationFailureStopsAllDeletes() throws Exception {
        cancellationFails = true;
        var error = assertThrows(IllegalStateException.class, () -> cleanup(service(), 1L, List.of(301L)));
        assertEquals("ENTITLEMENT_REVOKE_FAILED", error.getMessage());
        assertTrue(calls.stream().noneMatch(name -> name.startsWith("delete")));
    }

    @Test
    void rejectsCrossTenantBeforeAnyMapperCall() throws Exception {
        var service = service();
        assertThrows(IllegalArgumentException.class, () -> cleanup(service, 2L, List.of()));
        assertTrue(calls.isEmpty());
    }

    @Test
    void emptyOptionalScopesDoNotIssueEmptyInDeletes() throws Exception {
        taskIds = List.of();
        deviationIds = List.of();
        cleanup(service(), 1L, List.of());
        assertFalse(calls.contains("deleteWorkTasks"));
        assertFalse(calls.contains("deleteDeviationHandling"));
        assertFalse(calls.contains("deleteDeviationRequests"));
        assertFalse(calls.contains("deleteDeviations"));
        assertTrue(calls.contains("deleteNonconformanceReviewsForWorkOrder"));
    }

    @Test
    void cleanupMapperNeverDeletesSignatureAuditOrTraceEvidence() throws Exception {
        Class<?> mapper = Class.forName(MAPPER);
        for (Method method : mapper.getMethods()) {
            var delete = method.getAnnotation(org.apache.ibatis.annotations.Delete.class);
            if (delete == null) continue;
            String sql = String.join(" ", delete.value()).toLowerCase();
            assertTrue(sql.contains("tenant_id"), method.getName());
            for (String retained : List.of("gxp_", "signature", "audit", "archive", "trace_", "infra_file")) {
                assertFalse(sql.contains(retained), method.getName() + ": " + retained);
            }
        }
    }

    private Object service() throws Exception {
        TenantContextHolder.setTenantId(1L);
        Class<?> mapperClass = Class.forName(MAPPER);
        Object mapper = mock(mapperClass, call -> {
            String name = call.getMethod().getName();
            if (name.equals("countOwnershipConflicts")) return scopeConflict ? 1L : 0L;
            if (name.equals("selectWorkTaskIdsForUpdate") || name.equals("selectDeviationIdsForUpdate")) {
                calls.add(name + ":" + String.join(":", java.util.Arrays.stream(call.getArguments()).map(String::valueOf).toList()));
                return name.equals("selectWorkTaskIdsForUpdate") ? taskIds : deviationIds;
            }
            calls.add(name);
            if (name.startsWith("delete")) return 1;
            return org.mockito.Answers.RETURNS_DEFAULTS.answer(call);
        });
        MesProEdhrWorkTaskService tasks = mock(MesProEdhrWorkTaskService.class, call -> {
            if (call.getMethod().getName().equals("cancelTasksForTestReset")) {
                calls.add("cancelTasksForTestReset:" + call.getArgument(0));
                if (cancellationFails) throw new IllegalStateException("ENTITLEMENT_REVOKE_FAILED");
                return null;
            }
            return org.mockito.Answers.RETURNS_DEFAULTS.answer(call);
        });
        Object service = Class.forName(SERVICE).getConstructor(mapperClass, MesProEdhrWorkTaskService.class)
                .newInstance(mapper, tasks);
        MesFixedTestOrderBpmCancellationService bpm = mock(MesFixedTestOrderBpmCancellationService.class);
        doAnswer(call -> { calls.add("cancelBpm"); return null; }).when(bpm)
                .cancel(any(), anyList(), anyList(), any());
        org.springframework.test.util.ReflectionTestUtils.setField(service, "bpmCancellationService", bpm);
        return service;
    }

    private void cleanup(Object service, Long tenantId, List<Long> batchIds) throws Exception {
        try {
            service.getClass().getMethod("cleanup", Long.class, Long.class, List.class, List.class,
                    List.class, List.class).invoke(service, tenantId, 71L, List.of(81L), batchIds,
                    List.of(), List.of(601L));
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception exception) throw exception;
            throw e;
        }
    }
}
