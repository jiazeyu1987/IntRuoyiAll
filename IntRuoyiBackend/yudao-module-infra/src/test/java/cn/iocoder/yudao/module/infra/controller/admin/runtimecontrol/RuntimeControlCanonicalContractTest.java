package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlActionReqVO;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class RuntimeControlCanonicalContractTest {

    private static final Set<String> REQUIRED_ENDPOINTS = new LinkedHashSet<>(List.of(
            "GET /infra/runtime-control/alerts/page",
            "POST /infra/runtime-control/alerts",
            "POST /infra/runtime-control/alerts/{id}/resend-site-message",
            "POST /infra/runtime-control/alerts/{id}/acknowledge",
            "GET /infra/runtime-control/owner-matrix",
            "POST /infra/runtime-control/owner-matrix",
            "PUT /infra/runtime-control/owner-matrix/{id}",
            "GET /infra/runtime-control/wizard/scenarios",
            "GET /infra/runtime-control/rollback-candidates",
            "GET /infra/runtime-control/restore-candidates",
            "POST /infra/runtime-control/inspection-runs",
            "GET /infra/runtime-control/business-health",
            "GET /infra/runtime-control/probes/latest",
            "GET /infra/runtime-control/capacity/status",
            "POST /infra/runtime-control/capacity/refresh",
            "GET /infra/runtime-control/release-status",
            "GET /infra/runtime-control/backup-points",
            "GET /infra/runtime-control/backup-points/{backupId}",
            "GET /infra/runtime-control/incidents/page",
            "POST /infra/runtime-control/incidents",
            "POST /infra/runtime-control/incidents/{id}/actions",
            "POST /infra/runtime-control/incidents/{id}/close"
    ));

    @Test
    void controllerShouldExposeCanonicalFoolproofOpsEndpointsWithoutOpsSubPrefix() {
        Set<String> endpoints = collectControllerEndpoints();

        Set<String> missingEndpoints = new LinkedHashSet<>(REQUIRED_ENDPOINTS);
        missingEndpoints.removeAll(endpoints);
        assertTrue(missingEndpoints.isEmpty(), () -> "Missing canonical endpoints: " + missingEndpoints
                + ", actual endpoints: " + endpoints);

        List<String> opsPrefixedEndpoints = endpoints.stream()
                .filter(endpoint -> endpoint.contains("/infra/runtime-control/ops/"))
                .toList();
        assertEquals(List.of(), opsPrefixedEndpoints,
                "Canonical contract must not introduce /ops sub-prefix endpoints");
    }

    @Test
    void actionRequestShouldUseServerGeneratedCandidateIdsForRollbackAndRestore() throws Exception {
        assertWriteableProperty(RuntimeControlActionReqVO.class, "selectedImageCandidateId");
        assertWriteableProperty(RuntimeControlActionReqVO.class, "selectedRecoverySetCandidateId");
    }

    @Test
    void releaseStatusResponseShouldExposeReadonlyReleaseSnapshotContract() throws Exception {
        Class<?> statusRespVO = Class.forName(
                "cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlReleaseStatusRespVO");
        for (String property : List.of("releasePackages", "targetStates", "recentOperations",
                "testCurrentReleaseTag", "latestTestedReleaseTag")) {
            assertWriteableProperty(statusRespVO, property);
        }
    }

    @Test
    void alertResponseShouldExposeSiteMessageStatusForSentFailedAndBlocked() throws Exception {
        Class<?> alertRespVO = Class.forName("cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlAlertRespVO");
        PropertyDescriptor statusProperty = assertWriteableProperty(alertRespVO, "siteMessageStatus");
        Class<?> propertyType = statusProperty.getPropertyType();
        if (propertyType.isEnum()) {
            Set<String> constants = Arrays.stream(propertyType.getEnumConstants())
                    .map(Objects::toString)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            assertTrue(constants.containsAll(List.of("SENT", "FAILED", "BLOCKED")),
                    () -> "siteMessageStatus enum must express SENT/FAILED/BLOCKED, actual: " + constants);
        } else {
            assertEquals(String.class, propertyType,
                    "siteMessageStatus must be either a String contract field or an enum with SENT/FAILED/BLOCKED");
        }
    }

    private Set<String> collectControllerEndpoints() {
        RequestMapping rootMapping = RuntimeControlController.class.getAnnotation(RequestMapping.class);
        String rootPath = firstPath(rootMapping.value(), rootMapping.path());
        Set<String> endpoints = new LinkedHashSet<>();
        for (Method method : RuntimeControlController.class.getDeclaredMethods()) {
            addEndpoint(endpoints, "GET", rootPath, method.getAnnotation(GetMapping.class));
            addEndpoint(endpoints, "POST", rootPath, method.getAnnotation(PostMapping.class));
            addEndpoint(endpoints, "PUT", rootPath, method.getAnnotation(PutMapping.class));
        }
        return endpoints;
    }

    private void addEndpoint(Set<String> endpoints, String httpMethod, String rootPath, GetMapping mapping) {
        if (mapping != null) {
            endpoints.add(httpMethod + " " + joinPath(rootPath, firstPath(mapping.value(), mapping.path())));
        }
    }

    private void addEndpoint(Set<String> endpoints, String httpMethod, String rootPath, PostMapping mapping) {
        if (mapping != null) {
            endpoints.add(httpMethod + " " + joinPath(rootPath, firstPath(mapping.value(), mapping.path())));
        }
    }

    private void addEndpoint(Set<String> endpoints, String httpMethod, String rootPath, PutMapping mapping) {
        if (mapping != null) {
            endpoints.add(httpMethod + " " + joinPath(rootPath, firstPath(mapping.value(), mapping.path())));
        }
    }

    private String firstPath(String[] values, String[] paths) {
        if (values.length > 0) {
            return values[0];
        }
        if (paths.length > 0) {
            return paths[0];
        }
        return "";
    }

    private String joinPath(String rootPath, String methodPath) {
        String root = rootPath.startsWith("/") ? rootPath : "/" + rootPath;
        if (methodPath == null || methodPath.isBlank()) {
            return root;
        }
        String child = methodPath.startsWith("/") ? methodPath : "/" + methodPath;
        return root + child;
    }

    private PropertyDescriptor assertWriteableProperty(Class<?> type, String propertyName) throws Exception {
        BeanInfo beanInfo = Introspector.getBeanInfo(type);
        PropertyDescriptor property = Arrays.stream(beanInfo.getPropertyDescriptors())
                .filter(descriptor -> propertyName.equals(descriptor.getName()))
                .findFirst()
                .orElseGet(() -> fail("Missing contract property: " + type.getName() + "." + propertyName));
        if (property.getWriteMethod() == null) {
            Method setter = findSetter(type, propertyName, property.getPropertyType());
            assertNotNull(setter, "Contract property must be writable: " + propertyName);
        }
        return property;
    }

    private Method findSetter(Class<?> type, String propertyName, Class<?> propertyType) {
        String setterName = "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
        return Arrays.stream(type.getMethods())
                .filter(method -> setterName.equals(method.getName()))
                .filter(method -> method.getParameterCount() == 1)
                .filter(method -> propertyType == null || propertyType.equals(method.getParameterTypes()[0]))
                .findFirst()
                .orElse(null);
    }
}
