package cn.iocoder.yudao.module.mes.approval;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskRespVO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MesEdhrUnifiedTaskNavigationTest {
    @Test
    void pqcDetailRetainsApplicationAndAddsExactTaskIdentity() {
        MesProEdhrWorkTaskRespVO task = new MesProEdhrWorkTaskRespVO()
                .setId(9007199254740999L).setTaskType("PQC_PRODUCTION_RELEASE")
                .setActionUrl("/mes/production-release/pqc?applicationId=9");
        Object action = ReflectionTestUtils.invokeMethod(MesProEdhrApprovalTaskAdapter.class,
                "resolveDecisionActionUrl", task);
        Map<String, String> query = ReflectionTestUtils.invokeMethod(MesProEdhrApprovalTaskAdapter.class,
                "resolveDecisionDetailQuery", task, Map.of(), action);
        assertEquals("9", query.get("applicationId"));
        assertEquals("9007199254740999", query.get("workTaskId"));
        assertEquals("/mes/production-release/pqc", ReflectionTestUtils.invokeMethod(
                MesProEdhrApprovalTaskAdapter.class, "resolveDecisionDetailRoute", task, action));
    }

    @Test
    void reviewGoesDirectlyToApprovalDetailWithExecutionIdentity() {
        MesProEdhrWorkTaskRespVO task = new MesProEdhrWorkTaskRespVO()
                .setId(12L).setTaskType("REVIEW").setExecutionId(30L);
        assertEquals("/mes/pro/feedback/edhr-approval/detail", ReflectionTestUtils.invokeMethod(
                MesProEdhrApprovalTaskAdapter.class, "resolveDecisionDetailRoute", task, null));
        Map<String, String> query = ReflectionTestUtils.invokeMethod(MesProEdhrApprovalTaskAdapter.class,
                "resolveDecisionDetailQuery", task, Map.of(), null);
        assertEquals("30", query.get("id"));
        assertEquals("12", query.get("workTaskId"));
    }

    @Test
    void conflictingTaskIdentityIsRejectedInsteadOfOpeningAnotherTask() {
        MesProEdhrWorkTaskRespVO task = new MesProEdhrWorkTaskRespVO()
                .setId(12L).setTaskType("PQC_PRODUCTION_RELEASE")
                .setActionUrl("/mes/production-release/pqc?applicationId=9&workTaskId=13");
        Object action = ReflectionTestUtils.invokeMethod(MesProEdhrApprovalTaskAdapter.class,
                "resolveDecisionActionUrl", task);
        assertThrows(IllegalStateException.class, () -> ReflectionTestUtils.invokeMethod(
                MesProEdhrApprovalTaskAdapter.class, "resolveDecisionDetailQuery", task, Map.of(), action));
    }
}
