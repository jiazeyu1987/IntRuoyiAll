package cn.iocoder.yudao.module.dcc.controller.admin.file;

import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileUploadTemporaryCleanupReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileUploadTemporaryStatusRespVO;
import cn.iocoder.yudao.module.dcc.service.audit.DccAccessBoundaryLogCreateCommand;
import cn.iocoder.yudao.module.dcc.service.audit.DccControlledFileAccessAuditService;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DccControlledFileTemporaryCleanupControllerTest extends BaseMockitoUnitTest {

    @Mock
    private DccUploadTicketService uploadTicketService;
    @Mock
    private DccControlledFileAccessAuditService accessAuditService;

    @InjectMocks
    private DccControlledFileController controller;

    @BeforeEach
    void setLoginUser() {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(99L);
        loginUser.setUserType(UserTypeEnum.ADMIN.getValue());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        SecurityFrameworkUtils.setLoginUser(loginUser, request);
    }

    @AfterEach
    void clearLoginUser() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void cleanupUploadTemporarySession_recordsAuditBeforeCleanup() throws Exception {
        DccControlledFileUploadTemporaryCleanupReqVO reqVO = cleanupReq();
        MockHttpServletRequest request = auditRequest("REQ-CLEANUP-1");
        when(uploadTicketService.cleanupSessionTemporaryFiles(any(), any(), any(), any())).thenReturn(2);

        CommonResult<DccControlledFileUploadTemporaryStatusRespVO> result =
                controller.cleanupUploadTemporarySession(reqVO, request);

        assertEquals(2, result.getData().getCleanedCount());
        InOrder inOrder = inOrder(accessAuditService, uploadTicketService);
        inOrder.verify(accessAuditService).recordBoundaryLog(any(DccAccessBoundaryLogCreateCommand.class));
        inOrder.verify(uploadTicketService).cleanupSessionTemporaryFiles(any(), any(), any(), any());
    }

    @Test
    void cleanupUploadTemporarySession_auditFailureDoesNotCleanSession() throws Exception {
        DccControlledFileUploadTemporaryCleanupReqVO reqVO = cleanupReq();
        MockHttpServletRequest request = auditRequest("REQ-CLEANUP-AUDIT-FAIL");
        IllegalStateException auditFailure = new IllegalStateException("audit failed");
        doThrow(auditFailure).when(accessAuditService).recordBoundaryLog(any(DccAccessBoundaryLogCreateCommand.class));

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> controller.cleanupUploadTemporarySession(reqVO, request));

        assertSame(auditFailure, thrown);
        verifyNoInteractions(uploadTicketService);
    }

    private DccControlledFileUploadTemporaryCleanupReqVO cleanupReq() {
        DccControlledFileUploadTemporaryCleanupReqVO reqVO = new DccControlledFileUploadTemporaryCleanupReqVO();
        reqVO.setSessionId("session-1");
        return reqVO;
    }

    private MockHttpServletRequest auditRequest(String requestId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(DccControlledFileController.REQUEST_ID_HEADER, requestId);
        request.addHeader("User-Agent", "JUnit");
        request.setRemoteAddr("10.0.0.9");
        return request;
    }
}
