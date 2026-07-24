package cn.iocoder.yudao.module.bpm.businessapproval.service;

import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BpmProcessInstanceBusinessApprovalStarterTest {

    private BpmProcessInstanceBusinessApprovalStarter starter;

    @Mock
    private BpmProcessInstanceApi processInstanceApi;

    @BeforeEach
    void setUp() {
        starter = new BpmProcessInstanceBusinessApprovalStarter();
        ReflectionTestUtils.setField(starter, "processInstanceApi", processInstanceApi);
    }

    @Test
    void startUsesApplicantAndStableBusinessKey() {
        BusinessApprovalRequest request = BusinessApprovalRequest.builder()
                .requestId(2001L)
                .context(BusinessApprovalPolicyResolveServiceTest.baseContext().build())
                .build();
        when(processInstanceApi.createProcessInstance(org.mockito.ArgumentMatchers.eq(501L),
                org.mockito.ArgumentMatchers.any(BpmProcessInstanceCreateReqDTO.class)))
                .thenReturn("bpm-2001");

        String processInstanceId = starter.start(request, "mes-route-version-approval-v1",
                Map.of("tenantId", 122L, "approvalRequestId", 2001L));

        assertEquals("bpm-2001", processInstanceId);
        ArgumentCaptor<BpmProcessInstanceCreateReqDTO> captor =
                ArgumentCaptor.forClass(BpmProcessInstanceCreateReqDTO.class);
        verify(processInstanceApi).createProcessInstance(org.mockito.ArgumentMatchers.eq(501L), captor.capture());
        BpmProcessInstanceCreateReqDTO reqDTO = captor.getValue();
        assertEquals("mes-route-version-approval-v1", reqDTO.getProcessDefinitionKey());
        assertEquals("BUSINESS_APPROVAL:2001", reqDTO.getBusinessKey());
        assertEquals(122L, reqDTO.getVariables().get("tenantId"));
        assertEquals(2001L, reqDTO.getVariables().get("approvalRequestId"));
    }

    @Test
    void cancelUsesApplicantAndProcessInstanceId() {
        BusinessApprovalRequest request = BusinessApprovalRequest.builder()
                .requestId(2001L)
                .context(BusinessApprovalPolicyResolveServiceTest.baseContext().build())
                .build();

        starter.cancel(request, " bpm-2001 ", "domain pending failed");

        verify(processInstanceApi).cancelProcessInstance(501L, "bpm-2001", "domain pending failed");
    }

}
