package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.renewal.DccRegistrationCertificateRenewalController;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.renewal.vo.DccRegistrationCertificateRenewalUploadReqVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalStartCommand;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.renewal.DccRegistrationCertificateRenewalService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.renewal.DccRegistrationCertificateRenewalSubmitCommand;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.renewal.DccRegistrationCertificateRenewalSubmitResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccRegistrationCertificateRenewalControllerTest {

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void submitGeneratesRequestTraceIdWhenSkyWalkingTraceIdIsAbsent() {
        DccRegistrationCertificateRenewalService renewalService =
                mock(DccRegistrationCertificateRenewalService.class);
        DccRegistrationCertificateApprovalService approvalService =
                mock(DccRegistrationCertificateApprovalService.class);
        DccRegistrationCertificateRenewalController controller =
                new DccRegistrationCertificateRenewalController(renewalService, approvalService);
        when(renewalService.submitRenewalForApproval(any(DccRegistrationCertificateRenewalSubmitCommand.class)))
                .thenReturn(new DccRegistrationCertificateRenewalSubmitResult(33L, 44L, 55L, "SUBMITTED"));

        TenantContextHolder.setTenantId(11L);
        CommonResult<Long> result;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(22L);
            MockHttpServletRequest request = new MockHttpServletRequest(
                    "POST", "/dcc/registration-certificates/99/renewals");
            request.setRemoteAddr("10.8.0.41");
            request.addHeader("User-Agent", "JUnit renewal upload");

            result = controller.submitForApproval(99L, "RENEWAL-IDEMPOTENCY-1", renewalRequest(), request);
        }

        assertEquals(33L, result.getData());
        ArgumentCaptor<DccRegistrationCertificateRenewalSubmitCommand> command =
                ArgumentCaptor.forClass(DccRegistrationCertificateRenewalSubmitCommand.class);
        verify(renewalService).submitRenewalForApproval(command.capture());
        assertEquals(11L, command.getValue().tenantId());
        assertEquals(22L, command.getValue().actorId());
        assertEquals("RENEWAL-IDEMPOTENCY-1", command.getValue().idempotencyKey());
        assertFalse(command.getValue().requestTraceId().isBlank());
        assertTrue(command.getValue().requestTraceId().length() <= 128);
        assertEquals(99L, command.getValue().certificateId());
        assertEquals(7, command.getValue().expectedRowVersion());
        assertEquals(88L, command.getValue().currentVersionId());
        assertEquals(LocalDate.of(2026, 8, 29), command.getValue().approvalDate());
        assertEquals(LocalDate.of(2026, 8, 29), command.getValue().effectiveDate());
        assertEquals(LocalDate.of(2029, 8, 29), command.getValue().expiryDate());
        assertEquals(Boolean.TRUE, command.getValue().categoryChanged());
        assertEquals("REG-CERT-RENEWAL-1", command.getValue().certificateNo());
        assertEquals("III类", command.getValue().classification());
        verify(approvalService).startNativeApproval(eq(11L), eq(22L),
                eq(new DccRegistrationCertificateApprovalStartCommand(33L)));
    }

    private static DccRegistrationCertificateRenewalUploadReqVO renewalRequest() {
        DccRegistrationCertificateRenewalUploadReqVO reqVO =
                new DccRegistrationCertificateRenewalUploadReqVO();
        reqVO.setExpectedRowVersion(7);
        reqVO.setCurrentVersionId(88L);
        reqVO.setApprovalDate(LocalDate.of(2026, 8, 29));
        reqVO.setEffectiveDate(LocalDate.of(2026, 8, 29));
        reqVO.setExpiryDate(LocalDate.of(2029, 8, 29));
        reqVO.setCategoryChanged(true);
        reqVO.setCertificateNo("REG-CERT-RENEWAL-1");
        reqVO.setClassification("III类");
        reqVO.setFile(new MockMultipartFile("file", "renewal.pdf", "application/pdf",
                "%PDF-1.4".getBytes(StandardCharsets.UTF_8)));
        return reqVO;
    }
}
