package cn.iocoder.yudao.module.bpm.controller.admin.approval;

import cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalCenterService;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskTimelineQuery;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class ApprovalTimelineOptionalProcessIdTest {
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void nativeTimelineGetTreatsEmptyOptionalProcessIdAsAbsent(String processId) throws Exception {
        var query = executeGet(processId);
        assertNull(query.getProcessInstanceId());
        assertEquals("DCC_PROJECT_PRODUCT_REVIEW", query.getSourceTaskType());
        assertEquals("DCC_PROJECT_PRODUCT:1:19:DCC_PROJECT_PRODUCT_REVIEW", query.getSourceTaskId());
        assertEquals("19", query.getBusinessKey());
    }

    @ParameterizedTest
    @ValueSource(strings = {"flowable-instance-19", " instance-with-spaces "})
    void realProcessIdIsPreservedForProviderValidation(String processId) throws Exception {
        assertEquals(processId, executeGet(processId).getProcessInstanceId());
    }

    private ApprovalTaskTimelineQuery executeGet(String processId) throws Exception {
        var service = mock(ApprovalCenterService.class);
        when(service.listTaskTimeline(eq(2L), any())).thenReturn(List.of());
        MockHttpServletRequestBuilder request = get("/approval-center/tasks/timeline")
                .param("moduleCode", "DCC").param("sourceTaskType", "DCC_PROJECT_PRODUCT_REVIEW")
                .param("sourceTaskId", "DCC_PROJECT_PRODUCT:1:19:DCC_PROJECT_PRODUCT_REVIEW")
                .param("businessKey", "19");
        if (processId != null) request.param("processInstanceId", processId);
        try (var security = mockStatic(WebFrameworkUtils.class)) {
            security.when(WebFrameworkUtils::getLoginUserId).thenReturn(2L);
            standaloneSetup(new ApprovalCenterController(service)).build().perform(request).andExpect(status().isOk());
        }
        var captured = ArgumentCaptor.forClass(ApprovalTaskTimelineQuery.class);
        verify(service).listTaskTimeline(eq(2L), captured.capture());
        return captured.getValue();
    }
}
