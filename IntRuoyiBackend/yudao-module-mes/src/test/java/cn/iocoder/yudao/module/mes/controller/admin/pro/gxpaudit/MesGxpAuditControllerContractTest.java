package cn.iocoder.yudao.module.mes.controller.admin.pro.gxpaudit;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.MesProEdhrBatchExecutionController;
import cn.iocoder.yudao.module.mes.controller.admin.pro.productionrelease.MesPqcReleaseOrderDetailController;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.MesProcessPoolTeamLeaderController;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MesGxpAuditControllerContractTest {

    @Test
    void existingDetailControllersExposeReadOnlyAuditQueries() {
        assertEndpoint(MesProcessPoolTeamLeaderController.class,
                "/active-order/audit/page", "mes:pro-process-pool-team-leader:query");
        assertEndpoint(MesProcessPoolTeamLeaderController.class,
                "/active-order/audit/get", "mes:pro-process-pool-team-leader:query");
        assertEndpoint(MesPqcReleaseOrderDetailController.class,
                "/audit/page", "mes:pro-production-release:query");
        assertEndpoint(MesPqcReleaseOrderDetailController.class,
                "/audit/get", "mes:pro-production-release:query");
        assertEndpoint(MesProEdhrBatchExecutionController.class,
                "/audit/page", "mes:pro-edhr-batch-execution:query");
        assertEndpoint(MesProEdhrBatchExecutionController.class,
                "/audit/get", "mes:pro-edhr-batch-execution:query");
    }

    private void assertEndpoint(Class<?> controller, String path, String permission) {
        Method method = Arrays.stream(controller.getDeclaredMethods())
                .filter(candidate -> candidate.isAnnotationPresent(GetMapping.class))
                .filter(candidate -> Arrays.stream(candidate.getAnnotation(GetMapping.class).value())
                        .anyMatch(path::equals))
                .findFirst()
                .orElseThrow(() -> new AssertionError(controller.getSimpleName() + " missing " + path));
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertTrue(preAuthorize != null && preAuthorize.value().contains(permission),
                controller.getSimpleName() + " audit endpoint must reuse the current query permission");
    }
}
