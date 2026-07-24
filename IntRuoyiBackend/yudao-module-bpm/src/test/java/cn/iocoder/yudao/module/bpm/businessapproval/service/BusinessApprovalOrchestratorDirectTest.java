package cn.iocoder.yudao.module.bpm.businessapproval.service;

import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalPolicyMode;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequest;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequestStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BusinessApprovalOrchestratorDirectTest {

    @Test
    void submitDirectPolicyExecutesDomainExecutorWithoutBpmProcess() {
        RecordingExecutor executor = new RecordingExecutor("MES_ROUTE_VERSION_PUBLISH");
        InMemoryBusinessApprovalRequestStore store = new InMemoryBusinessApprovalRequestStore();
        RecordingBpmStarter bpmStarter = new RecordingBpmStarter("should-not-start");
        BusinessApprovalOrchestrator orchestrator = new BusinessApprovalOrchestrator(
                new BusinessApprovalPolicyResolveService(List.of(
                        BusinessApprovalPolicyResolveServiceTest.basePolicy()
                                .mode(BusinessApprovalPolicyMode.DIRECT)
                                .processDefinitionKey(null)
                                .build())),
                new BusinessApprovalEffectExecutorRegistry(List.of(executor)),
                store,
                bpmStarter,
                null,
                null);

        BusinessApprovalRequest request = orchestrator.submit(BusinessApprovalPolicyResolveServiceTest.baseContext().build());

        assertEquals(BusinessApprovalRequestStatus.DIRECT_EXECUTED, request.getStatus());
        assertNull(request.getProcessInstanceId());
        assertEquals(1, executor.getDirectExecutions());
        assertEquals(0, executor.getPendingMarks());
        assertEquals(0, bpmStarter.getStartCount());
    }

}
