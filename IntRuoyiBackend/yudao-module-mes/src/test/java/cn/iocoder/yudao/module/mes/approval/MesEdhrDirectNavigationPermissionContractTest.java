package cn.iocoder.yudao.module.mes.approval;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.bpm.approval.core.*;
import cn.iocoder.yudao.module.bpm.approval.service.*;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class MesEdhrDirectNavigationPermissionContractTest {
    private static final List<String> APPROVAL = List.of("mes:pro-batch-record-execution:approve",
            "mes:pro-batch-record-execution:track", "mes:pro-batch-record-execution:signature-query");

    @Test
    void reviewAndApproveRequireEveryMandatoryReadAndPreserveExactIdentity() {
        for (String type : List.of("REVIEW", "APPROVE")) {
            for (String missing : APPROVAL) {
                var row = page(type, "TODO", false, List.of(missing), null);
                assertEquals(missing, row.getDecisionDetailQuery().get("requiredPermissionsMissing"));
                assertTrue(row.getAvailableActions().isEmpty());
                assertEquals("/mes/pro/feedback/edhr-approval/detail", row.getDecisionDetailRoute());
                assertEquals("9001", row.getDecisionDetailQuery().get("id"));
                assertEquals("8001", row.getDecisionDetailQuery().get("workTaskId"));
                assertEquals("task-9001", row.getDecisionDetailQuery().get("bpmTaskId"));
            }
            var allowed = page(type, "TODO", false, List.of(), null);
            assertNull(allowed.getDecisionDetailQuery().get("requiredPermissionsMissing"));
            assertTrue(allowed.getAvailableActions().contains(type + "_IN_MODULE"));
        }
    }

    @Test
    void globalViewAndDoneKeepHistoryWhenDestinationPermissionsAreRevoked() {
        for (boolean global : List.of(false, true)) {
            var row = page("APPROVE", global ? "TODO" : "DONE", global, APPROVAL, null);
            assertEquals("8001", row.getSourceTaskId());
            assertEquals(String.join(",", APPROVAL), row.getDecisionDetailQuery().get("requiredPermissionsMissing"));
            assertTrue(row.getAvailableActions().isEmpty());
            assertEquals(global ? "TODO" : "DONE", row.getSourceStatus());
            assertEquals("保留正式意见", row.getApprovalRemark());
        }
    }

    @Test
    void eachNonApprovalTargetUsesItsActualReadContractAndUnknownTargetFailsClosed() {
        for (var contract : Map.of(
                "/mes/pro/feedback/edhr-execution/form?id=9001&workTaskId=8001", "mes:pro-batch-record-execution:query",
                "/mes/pro/feedback/edhr-batch-execution/detail?id=7001", "mes:pro-edhr-batch-execution:query",
                "/mes/production-release/pqc?applicationId=95", "mes:pro-production-release:query").entrySet()) {
            var denied = page("FILL", "TODO", false, List.of(contract.getValue()), contract.getKey());
            assertEquals(contract.getValue(), denied.getDecisionDetailQuery().get("requiredPermissionsMissing"));
            assertTrue(denied.getAvailableActions().isEmpty());
            var allowed = page("FILL", "TODO", false, List.of(), contract.getKey());
            assertTrue(allowed.getAvailableActions().contains("PROCESS_IN_MODULE"));
        }
        assertEquals("mes:pro-edhr-work-task:query", page("FILL", "TODO", false,
                List.of("mes:pro-edhr-work-task:query"), null).getDecisionDetailQuery().get("requiredPermissionsMissing"));
        for (String missing : List.of("mes:pro-batch-record-execution:track", "mes:pro-batch-record-execution:signature-query")) {
            assertTrue(page("FILL", "TODO", false, List.of(missing),
                    "/mes/pro/feedback/edhr-execution/form?id=9001").getAvailableActions().isEmpty());
        }
        var release = page("RELEASE_APPROVE", "TODO", false, List.of("mes:pro-edhr-batch-execution:query"), null);
        assertTrue(release.getAvailableActions().isEmpty());
        assertEquals("7001", release.getDecisionDetailQuery().get("id"));
        assertEquals("approval", release.getDecisionDetailQuery().get("focus"));
        var unknown = page("FILL", "TODO", false, List.of(), "/unknown?id=9001");
        assertNotNull(unknown.getDecisionDetailQuery().get("navigationContractMissing"));
        assertTrue(unknown.getAvailableActions().isEmpty());
    }

    private ApprovalTaskSummary page(String type, String status, boolean global, List<String> missing, String action) {
        var tasks = mock(MesProEdhrWorkTaskService.class);
        var permissions = mock(PermissionApi.class);
        when(permissions.hasAnyPermissions(eq(88L), any(String[].class)))
                .thenAnswer(call -> !missing.contains(((String[]) call.getRawArguments()[1])[0]));
        var reader = mock(MesEdhrApprovalOutcomeReader.class);
        when(reader.readAuthorizedTask(any())).thenReturn(new MesEdhrApprovalOutcome(
                "DONE".equals(status) ? "COMPLETED" : "WAIT", null, "SUBMITTED", status, "UNKNOWN", null,
                "保留正式意见", null, null, null, null, null, null, 88L));
        var handoff = mock(MesActiveOrderHandoffApprovalProjection.class);
        when(handoff.prefix(any(), anyLong())).thenReturn(new PageResult<>(List.of(), 0L));
        var task = new MesProEdhrWorkTaskDO().setId(8001L).setTaskType(type).setStatus(status);
        var display = new MesProEdhrWorkTaskRespVO().setId(8001L).setTaskType(type).setStatus(status)
                .setExecutionId(9001L).setBatchExecutionId(7001L).setBpmTaskId("task-9001").setActionUrl(action);
        when(tasks.getApprovalCenterAuthorizedPrefix(any(), eq(global), eq("DONE".equals(status)), anyLong()))
                .thenReturn(new PageResult<>(List.of(new MesEdhrAuthorizedTaskProjection(task, display)), 1L));
        var adapter = new MesProEdhrApprovalTaskAdapter(tasks, mock(MesProEdhrReleaseService.class),
                mock(MesManagerReleaseCompletedActorResolver.class));
        ReflectionTestUtils.setField(adapter, "permissions", permissions);
        ReflectionTestUtils.setField(adapter, "outcomeReader", reader);
        ReflectionTestUtils.setField(adapter, "handoffProjection", handoff);
        var page = adapter.page(ApprovalTaskQueryContext.of(88L, "DONE".equals(status) ? ApprovalTaskViewType.DONE
                : ApprovalTaskViewType.TODO, ApprovalModuleCode.EDHR, null, 1, 20, global));
        assertEquals(1L, page.getTotal());
        return page.getList().get(0);
    }
}
