package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReleaseWorkflowContractTest {

    @Test
    void standardWorkflowOwnsScopeIdentityAndPresetOnServer() throws Exception {
        Class<?> contractClass = requireClass(
                "cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow.ReleaseWorkflowContract");
        Method createIdentity = contractClass.getMethod("createIdentity", Instant.class, String.class,
                String.class, String.class);

        Object identity = createIdentity.invoke(null, Instant.parse("2026-09-11T01:02:03Z"), "abcd1234",
                "test-app-release", "2026-09-11");

        assertEquals("app-release", invokeAccessor(identity, "publishScope"));
        assertEquals("test-app-release", invokeAccessor(identity, "presetId"));
        assertEquals("2026-09-11", invokeAccessor(identity, "presetVersion"));
        assertTrue(((String) invokeAccessor(identity, "workflowId")).startsWith("rw-"));
        assertTrue(((String) invokeAccessor(identity, "releaseTag")).startsWith("release-20260911-010203-"));
        assertEquals("manifest.json", contractClass.getField("MANIFEST_FILE_NAME").get(null));
    }

    @Test
    void createIntentRejectsClientOwnedReleaseAndInfrastructureFields() throws Exception {
        Class<?> requestClass = requireClass(
                "cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlReleaseWorkflowCreateReqVO");
        Set<String> fields = Arrays.stream(requestClass.getDeclaredFields())
                .filter(field -> !field.isSynthetic())
                .map(Field::getName)
                .collect(Collectors.toSet());
        assertEquals(Set.of("reason", "sourceSelectionId"), fields);

        ObjectMapper mapper = new ObjectMapper();
        String[] forbiddenFields = {
                "releaseTag", "publishScope", "host", "nasConfigPath", "remoteMinioContainer",
                "backendRepoRoot", "frontendRepoRoot", "localCacheRoot"
        };
        for (String forbiddenField : forbiddenFields) {
            String json = "{\"reason\":\"release\",\"sourceSelectionId\":\"approved\",\""
                    + forbiddenField + "\":\"client-value\"}";
            Exception exception = assertThrows(Exception.class, () -> mapper.readValue(json, requestClass));
            assertTrue(rootMessage(exception).contains("RELEASE_WORKFLOW_FIELD_NOT_ALLOWED"), forbiddenField);
        }
    }

    private static Class<?> requireClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException exception) {
            throw new AssertionError("required P1 contract class is missing: " + className, exception);
        }
    }

    private static Object invokeAccessor(Object value, String name) throws Exception {
        return value.getClass().getMethod(name).invoke(value);
    }

    private static String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return String.valueOf(current.getMessage());
    }
}
