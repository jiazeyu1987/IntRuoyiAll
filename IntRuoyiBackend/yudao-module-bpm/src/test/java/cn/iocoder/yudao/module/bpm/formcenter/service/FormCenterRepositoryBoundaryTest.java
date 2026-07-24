package cn.iocoder.yudao.module.bpm.formcenter.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormCenterRepositoryBoundaryTest {

    @Test
    void lifecycleAndDuplicateServicesDoNotOwnInMemoryPersistence() {
        assertDoesNotDeclareInMemoryPersistence(FormTemplateLifecycleService.class);
        assertDoesNotDeclareInMemoryPersistence(FormDuplicateRoutingService.class);
    }

    @Test
    void runtimeServiceUsesTenantContextInsteadOfTenantZero() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/bpm/formcenter/runtime/FormCenterRuntimeServiceImpl.java"));

        assertFalse(source.contains(".tenantId(0L)"), "runtime template import must not write tenant_id=0");
        assertTrue(source.contains("TenantContextHolder.getRequiredTenantId()"),
                "runtime service must bind templates and actions to the authenticated tenant context");
    }

    @Test
    void runtimeBpmRequestCarriesBusinessCandidateVariables() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/bpm/formcenter/runtime/FormCenterRuntimeServiceImpl.java"));

        assertTrue(source.contains("variables.put(\"deptCode\""),
                "form center must pass business department to BPM candidate expressions");
        assertTrue(source.contains("variables.put(\"orgCode\""),
                "form center must pass business organization to BPM candidate expressions");
        assertTrue(source.contains("variables.put(\"roleCodes\""),
                "form center must pass business roles to BPM candidate expressions");
        assertTrue(source.contains("variables.put(\"productCode\""),
                "form center must pass product code to BPM candidate expressions");
        assertTrue(source.contains("variables.put(\"categoryCode\""),
                "form center must pass category code to BPM candidate expressions");
    }

    @Test
    void runtimeBpmRequestCarriesStartUserSelectAssignees() throws IOException {
        String submitReq = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/bpm/controller/admin/formcenter/vo/FormInstanceSubmitReqVO.java"));
        String source = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/bpm/formcenter/runtime/FormCenterRuntimeServiceImpl.java"));

        assertTrue(submitReq.contains("Map<String, List<Long>> startUserSelectAssignees"),
                "form submission must expose BPM start-user-selected assignees explicitly");
        assertTrue(source.contains("buildBpmRequest(instance, policy, reqVO.getStartUserSelectAssignees())"),
                "form center must read start-user-selected assignees from the submit request");
        assertTrue(source.contains("reqDTO.setStartUserSelectAssignees(startUserSelectAssignees)"),
                "form center must pass start-user-selected assignees to BpmProcessInstanceCreateReqDTO");
        assertTrue(source.contains("BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_START_USER_SELECT_ASSIGNEES"),
                "form center must also place selected assignees in BPM variables for downstream strategies");
    }

    @Test
    void runtimePersistsSnapshotsTaskPermissionsAndEffectExecutions() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/bpm/formcenter/runtime/FormCenterRuntimeServiceImpl.java"));

        assertTrue(source.contains("FormActionSnapshotMapper"),
                "runtime service must persist immutable draft/submit/rework snapshots");
        assertTrue(source.contains("FormTaskPermissionMapper"),
                "runtime service must persist active taskId-derived approver permissions");
        assertTrue(source.contains("FormEffectExecutionMapper"),
                "runtime service must persist business effect execution and pending-failure records");
        assertTrue(source.contains("recordSnapshot(instance, FormSnapshotType.DRAFT"),
                "saving drafts must create immutable DRAFT snapshots");
        assertTrue(source.contains("recordSnapshot(instance, FormSnapshotType.SUBMIT"),
                "submitting to BPM must create immutable SUBMIT snapshots");
        assertTrue(source.contains("recordSnapshot(instance, FormSnapshotType.REWORK_SUBMIT"),
                "rework submission must create immutable REWORK_SUBMIT snapshots");
        assertTrue(source.contains("onBpmTaskCreated"),
                "runtime service must handle BPM task-created events");
        assertTrue(source.contains("onBpmTaskCompleted"),
                "runtime service must handle BPM task-completed events without applying business effect");
        assertTrue(source.contains("onBpmProcessApproved"),
                "runtime service must handle process-approved events as the only effect trigger");
    }

    private void assertDoesNotDeclareInMemoryPersistence(Class<?> serviceClass) {
        for (Field field : serviceClass.getDeclaredFields()) {
            Class<?> type = field.getType();
            assertFalse(Map.class.isAssignableFrom(type) || Collection.class.isAssignableFrom(type),
                    serviceClass.getSimpleName() + " must persist through an explicit repository, not field "
                            + field.getName());
        }
    }

}
