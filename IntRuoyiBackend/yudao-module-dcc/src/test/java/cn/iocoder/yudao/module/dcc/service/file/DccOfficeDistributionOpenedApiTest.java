package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.file.DccControlledFileController;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccDistributionOfficeOpenedReqVO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class DccOfficeDistributionOpenedApiTest extends BaseMockitoUnitTest {
    @Mock DccControlledFileQueryService queryService;
    @InjectMocks DccControlledFileController controller;
    @Test void onlyAuthenticatedRecipientWithExistingPreviewProofCanConfirmAnOfficeOpening() throws Exception {
        var proof=new DccDistributionOfficeOpenedReqVO();proof.setViewerToken("actual-viewer-token");
        proof.setAccessEventCode("actual-event");proof.setWatermarkTraceCode("actual-watermark");
        proof.setViewerTokenId("token-id");proof.setViewerTokenNonce("nonce");
        var request=new MockHttpServletRequest();
        request.addHeader("User-Agent","DCC task-owned offline Controller fixture");
        request.addHeader("X-DCC-Request-Id","IC15-OFFICE-OPENED-TEST");
        when(queryService.confirmDistributionOfficeBodyOpened(eq(99L),eq(91L),eq(92L),eq(93L),
                eq("actual-viewer-token"),eq("actual-event"),eq("actual-watermark"),eq("token-id"),eq("nonce"),any())).thenReturn(true);
        try(var login=mockStatic(SecurityFrameworkUtils.class)) {
            login.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            assertTrue(controller.confirmDistributionOfficeOpened(91L,92L,93L,proof,request).getData());
            verify(queryService).confirmDistributionOfficeBodyOpened(eq(99L),eq(91L),eq(92L),eq(93L),
                    eq("actual-viewer-token"),eq("actual-event"),eq("actual-watermark"),eq("token-id"),eq("nonce"),any());
        }
        var method=DccControlledFileController.class.getMethod("confirmDistributionOfficeOpened",Long.class,Long.class,
                Long.class,DccDistributionOfficeOpenedReqVO.class,jakarta.servlet.http.HttpServletRequest.class);
        assertEquals("isAuthenticated()",method.getAnnotation(PreAuthorize.class).value());
    }
    @Test void emptyProofFailsRealBeanValidation() {
        try(var factory=jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            assertEquals(5,factory.getValidator().validate(new DccDistributionOfficeOpenedReqVO()).size());
        }
    }
}
