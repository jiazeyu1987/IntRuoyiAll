package cn.iocoder.yudao.module.bpm.businessapproval.service;

import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalException;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalPolicyMode;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequest;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequestStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BusinessApprovalOrchestratorBpmRequiredTest {

    @Test
    void submitBpmRequiredPolicyStartsProcessAndMarksDomainPending() {
        RecordingExecutor executor = new RecordingExecutor("MES_ROUTE_VERSION_PUBLISH");
        InMemoryBusinessApprovalRequestStore store = new InMemoryBusinessApprovalRequestStore();
        RecordingBpmStarter bpmStarter = new RecordingBpmStarter("bpm-001");
        BusinessApprovalOrchestrator orchestrator = new BusinessApprovalOrchestrator(
                new BusinessApprovalPolicyResolveService(List.of(BusinessApprovalPolicyResolveServiceTest.basePolicy()
                        .mode(BusinessApprovalPolicyMode.BPM_REQUIRED)
                        .processDefinitionKey("mes-route-version-approval-v1")
                        .build())),
                new BusinessApprovalEffectExecutorRegistry(List.of(executor)),
                store,
                bpmStarter,
                null,
                null);

        BusinessApprovalRequest request = orchestrator.submit(BusinessApprovalPolicyResolveServiceTest.baseContext().build());

        assertEquals(BusinessApprovalRequestStatus.PENDING_BPM, request.getStatus());
        assertEquals("bpm-001", request.getProcessInstanceId());
        assertEquals("mes-route-version-approval-v1", bpmStarter.getLastProcessDefinitionKey());
        assertEquals("122", String.valueOf(bpmStarter.getLastVariables().get("tenantId")));
        assertEquals("ROUTE_VERSION", bpmStarter.getLastVariables().get("objectType"));
        assertEquals("PUBLISH", bpmStarter.getLastVariables().get("actionCode"));
        assertEquals(1, executor.getPendingMarks());
        assertEquals("bpm-001", executor.getLastPendingRequest().getProcessInstanceId());
        assertEquals(0, executor.getDirectExecutions());
    }

    @Test
    void submitBpmRequiredPolicyMergesDomainVariablesWithoutOverwritingPlatformVariables() {
        RecordingExecutor executor = new RecordingExecutor("MES_ROUTE_VERSION_PUBLISH");
        RecordingBpmStarter bpmStarter = new RecordingBpmStarter("bpm-domain-variables");
        BusinessApprovalOrchestrator orchestrator = new BusinessApprovalOrchestrator(
                new BusinessApprovalPolicyResolveService(List.of(BusinessApprovalPolicyResolveServiceTest.basePolicy()
                        .mode(BusinessApprovalPolicyMode.BPM_REQUIRED)
                        .processDefinitionKey("mes-route-version-approval-v1")
                        .build())),
                new BusinessApprovalEffectExecutorRegistry(List.of(executor)),
                new InMemoryBusinessApprovalRequestStore(),
                bpmStarter,
                null,
                null);

        BusinessApprovalRequest request = orchestrator.submit(BusinessApprovalPolicyResolveServiceTest.baseContext()
                .variables(Map.of(
                        "tenantId", 999L,
                        "businessType", "legacy-domain-business-type",
                        "edhrExecutionId", 9001L,
                        "approvalSnapshotHash", "snapshot-hash-9001"))
                .build());

        assertEquals(BusinessApprovalRequestStatus.PENDING_BPM, request.getStatus());
        assertEquals("122", String.valueOf(bpmStarter.getLastVariables().get("tenantId")));
        assertEquals("MES_ROUTE_VERSION_PUBLISH", bpmStarter.getLastVariables().get("businessType"));
        assertEquals(9001L, bpmStarter.getLastVariables().get("edhrExecutionId"));
        assertEquals("snapshot-hash-9001", bpmStarter.getLastVariables().get("approvalSnapshotHash"));
    }

    @Test
    void submitBpmRequiredPolicyCancelsStartedProcessWhenDomainPendingFails() {
        RecordingExecutor executor = new RecordingExecutor("MES_ROUTE_VERSION_PUBLISH").failOnMarkPending();
        InMemoryBusinessApprovalRequestStore store = new InMemoryBusinessApprovalRequestStore();
        RecordingBpmStarter bpmStarter = new RecordingBpmStarter("bpm-failed-pending");
        BusinessApprovalOrchestrator orchestrator = new BusinessApprovalOrchestrator(
                new BusinessApprovalPolicyResolveService(List.of(BusinessApprovalPolicyResolveServiceTest.basePolicy()
                        .mode(BusinessApprovalPolicyMode.BPM_REQUIRED)
                        .processDefinitionKey("mes-route-version-approval-v1")
                        .build())),
                new BusinessApprovalEffectExecutorRegistry(List.of(executor)),
                store,
                bpmStarter,
                null,
                null);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orchestrator.submit(BusinessApprovalPolicyResolveServiceTest.baseContext().build()));

        assertEquals("domain pending failed", ex.getMessage());
        assertEquals(1, executor.getPendingMarks());
        assertEquals(1, bpmStarter.getCancelCount());
        assertEquals("bpm-failed-pending", bpmStarter.getLastCancelledProcessInstanceId());
    }

    @Test
    void submitBpmRequiredPolicyFailsFastWhenProcessDefinitionKeyIsMissing() {
        RecordingExecutor executor = new RecordingExecutor("MES_ROUTE_VERSION_PUBLISH");
        BusinessApprovalOrchestrator orchestrator = new BusinessApprovalOrchestrator(
                new BusinessApprovalPolicyResolveService(List.of(BusinessApprovalPolicyResolveServiceTest.basePolicy()
                        .mode(BusinessApprovalPolicyMode.BPM_REQUIRED)
                        .processDefinitionKey(null)
                        .build())),
                new BusinessApprovalEffectExecutorRegistry(List.of(executor)),
                new InMemoryBusinessApprovalRequestStore(),
                new RecordingBpmStarter("bpm-001"),
                null,
                null);

        BusinessApprovalException ex = assertThrows(BusinessApprovalException.class,
                () -> orchestrator.submit(BusinessApprovalPolicyResolveServiceTest.baseContext().build()));

        assertEquals(BusinessApprovalErrorCode.BUSINESS_APPROVAL_PROCESS_DEFINITION_MISSING, ex.getErrorCode());
        assertEquals(0, executor.getDirectExecutions());
        assertEquals(0, executor.getPendingMarks());
    }

}
