package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlBusinessHealthRespVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeBusinessHealthServiceImplTest {

    private static final Set<String> REQUIRED_CODES = Set.of("login", "erp", "mes", "file-object",
            "api-error", "slow-request", "job-failure");

    @Test
    void getBusinessHealthShouldExposeCollectorFailureAsBlockedReason() {
        RuntimeOpsBusinessHealthServiceImpl service = new RuntimeOpsBusinessHealthServiceImpl(List.of(
                () -> RuntimeOpsBusinessHealthCheckResult.pass("login", "登录", "HTTP 200", LocalDateTime.now()),
                () -> {
                    throw new IllegalStateException("ERP health endpoint missing");
                }
        ));

        RuntimeControlBusinessHealthRespVO health = service.getBusinessHealth();

        assertEquals(RuntimeOpsInspectionStatus.NO_GO, health.getStatus());
        assertRequiredCodesPresent(health);
        assertEquals(RuntimeOpsInspectionStatus.PASS, health.getItems().stream()
                .filter(item -> "login".equals(item.getCode()))
                .findFirst()
                .orElseThrow()
                .getStatus());
        assertTrue(health.getItems().stream()
                .anyMatch(item -> "business-health-collector-failed".equals(item.getCode())
                        && RuntimeOpsInspectionStatus.BLOCKED.equals(item.getStatus())
                        && item.getReason().contains("ERP health endpoint missing")));
    }

    @Test
    void getBusinessHealthShouldReturnRequiredBlockedItemsWhenCollectorsAreMissing() {
        RuntimeOpsBusinessHealthServiceImpl service = new RuntimeOpsBusinessHealthServiceImpl(List.of());

        RuntimeControlBusinessHealthRespVO health = service.getBusinessHealth();

        assertEquals(RuntimeOpsInspectionStatus.NO_GO, health.getStatus());
        assertRequiredCodesPresent(health);
        assertTrue(health.getItems().stream()
                .filter(item -> REQUIRED_CODES.contains(item.getCode()))
                .allMatch(item -> RuntimeOpsInspectionStatus.PASS != item.getStatus()));
        assertTrue(health.getItems().stream()
                .filter(item -> Set.of("login", "erp", "mes", "file-object").contains(item.getCode()))
                .allMatch(item -> item.getReason().contains("缺少") || item.getReason().contains("未注入")));
    }

    private void assertRequiredCodesPresent(RuntimeControlBusinessHealthRespVO health) {
        Set<String> actualCodes = health.getItems().stream()
                .map(item -> item.getCode())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        assertTrue(actualCodes.containsAll(REQUIRED_CODES),
                () -> "业务健康必须稳定返回 required codes, missing=" + missing(actualCodes)
                        + ", actual=" + actualCodes);
    }

    private Set<String> missing(Set<String> actualCodes) {
        Set<String> missing = new LinkedHashSet<>(REQUIRED_CODES);
        missing.removeAll(actualCodes);
        return missing;
    }
}
