package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.change.DccRegistrationCertificateChangeController;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.change.vo.DccRegistrationCertificateChangeApplyReqVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalStartCommand;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.change.DccRegistrationCertificateChangeCommand;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.change.DccRegistrationCertificateChangeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccRegistrationCertificateChangeControllerTest {

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void applyChangeGeneratesRequestTraceIdWhenSkyWalkingTraceIdIsAbsent() {
        DccRegistrationCertificateChangeService changeService =
                mock(DccRegistrationCertificateChangeService.class);
        DccRegistrationCertificateApprovalService approvalService =
                mock(DccRegistrationCertificateApprovalService.class);
        DccRegistrationCertificateChangeController controller =
                new DccRegistrationCertificateChangeController(changeService, approvalService);
        when(changeService.submitChangeForApproval(any(DccRegistrationCertificateChangeCommand.class)))
                .thenReturn(66L);

        TenantContextHolder.setTenantId(11L);
        CommonResult<Long> result;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(22L);
            MockHttpServletRequest request = new MockHttpServletRequest(
                    "POST", "/dcc/registration-certificates/99/changes");
            request.setRemoteAddr("10.8.0.42");
            request.addHeader("User-Agent", "JUnit registration change");

            result = controller.applyChange(99L, "CHANGE-IDEMPOTENCY-1", changeRequest(), request);
        }

        assertEquals(66L, result.getData());
        ArgumentCaptor<DccRegistrationCertificateChangeCommand> command =
                ArgumentCaptor.forClass(DccRegistrationCertificateChangeCommand.class);
        verify(changeService).submitChangeForApproval(command.capture());
        ArgumentCaptor<Long> tenantIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> actorIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<DccRegistrationCertificateApprovalStartCommand> approvalCommand =
                ArgumentCaptor.forClass(DccRegistrationCertificateApprovalStartCommand.class);
        verify(approvalService).startNativeApproval(
                tenantIdCaptor.capture(), actorIdCaptor.capture(), approvalCommand.capture());
        assertEquals(11L, tenantIdCaptor.getValue());
        assertEquals(22L, actorIdCaptor.getValue());
        assertEquals(66L, approvalCommand.getValue().requestId());
        assertEquals(11L, command.getValue().tenantId());
        assertEquals(22L, command.getValue().actorId());
        assertEquals("CHANGE-IDEMPOTENCY-1", command.getValue().idempotencyKey());
        assertFalse(command.getValue().requestTraceId().isBlank());
        assertTrue(command.getValue().requestTraceId().length() <= 128);
        assertEquals(99L, command.getValue().certificateId());
        assertEquals(7, command.getValue().expectedRowVersion());
        assertEquals(LocalDate.of(2026, 8, 29), command.getValue().approvalDate());
        assertEquals("变更后的产品名称", command.getValue().structuredValues().get("PRODUCT_NAME"));
        assertEquals(Boolean.FALSE, command.getValue().entrustedProduction());
        assertEquals(Boolean.TRUE, command.getValue().selfProduction());
    }

    private static DccRegistrationCertificateChangeApplyReqVO changeRequest() {
        DccRegistrationCertificateChangeApplyReqVO reqVO =
                new DccRegistrationCertificateChangeApplyReqVO();
        reqVO.setExpectedRowVersion(7);
        reqVO.setApprovalDate(LocalDate.of(2026, 8, 29));
        reqVO.setStructuredValues(Map.of("PRODUCT_NAME", "变更后的产品名称"));
        reqVO.setEntrustedProduction(false);
        reqVO.setSelfProduction(true);
        reqVO.setFile(new MockMultipartFile("file", "change.pdf", "application/pdf",
                "%PDF-1.4".getBytes(StandardCharsets.UTF_8)));
        return reqVO;
    }
}
