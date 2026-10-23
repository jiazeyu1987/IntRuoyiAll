package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.distribution.DccDistributionTaskController;
import cn.iocoder.yudao.module.dcc.controller.admin.distribution.vo.*;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.security.access.prepost.PreAuthorize;

class DccAutomaticDistributionRetryControllerTest extends BaseMockitoUnitTest {
    @Mock DccDistributionTaskService distributionTaskService;
    @InjectMocks DccDistributionTaskController controller;
    @Test void existingResourceRetryRequiresFormalDistributePermissionAndUsesActualLoginActor()throws Exception {
        var request=new DccAutomaticDistributionRetryReqVO();request.setControlledFileId(9007199254740993L);request.setBatchId(Long.MAX_VALUE);request.setVersionNo("B/1");request.setApprovalProcessInstanceId("exact-round");
        var result=new DccAutomaticDistributionResultRespVO();result.setControlledFileId(request.getControlledFileId());result.setBatchId(request.getBatchId());result.setStatus("FAILED");result.setErrorCode("ROSTER_SOURCE_MISSING");
        when(distributionTaskService.retryAutomaticDistribution(99L,request)).thenReturn(result);
        try(var security=mockStatic(SecurityFrameworkUtils.class)){security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);assertSame(result,controller.retryAutomaticDistribution(request).getData());}
        verify(distributionTaskService).retryAutomaticDistribution(99L,request);
        var method=DccDistributionTaskController.class.getMethod("retryAutomaticDistribution",DccAutomaticDistributionRetryReqVO.class);
        assertEquals("/automatic-retry",method.getAnnotation(PostMapping.class).value()[0]);
        assertEquals("@ss.hasPermission('dcc:controlled-file:distribute')",method.getAnnotation(PreAuthorize.class).value());
        assertEquals("FAILED",result.getStatus());
    }
}
