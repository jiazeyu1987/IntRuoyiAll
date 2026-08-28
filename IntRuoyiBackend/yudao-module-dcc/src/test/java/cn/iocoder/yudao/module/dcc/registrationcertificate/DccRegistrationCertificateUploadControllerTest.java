package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.upload.DccRegistrationCertificateUploadController;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.upload.vo.DccRegistrationCertificateUploadSubmitReqVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalStartCommand;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.upload.DccRegistrationCertificateUploadCommand;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.upload.DccRegistrationCertificateUploadService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.upload.DccRegistrationCertificateUploadSubmitResult;
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

class DccRegistrationCertificateUploadControllerTest {

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void submitGeneratesRequestTraceIdWhenSkyWalkingTraceIdIsAbsent() {
        DccRegistrationCertificateUploadService uploadService = mock(DccRegistrationCertificateUploadService.class);
        DccRegistrationCertificateApprovalService approvalService = mock(DccRegistrationCertificateApprovalService.class);
        DccRegistrationCertificateUploadController controller =
                new DccRegistrationCertificateUploadController(uploadService, approvalService);
        DccRegistrationCertificateUploadSubmitReqVO reqVO = uploadRequest();
        when(uploadService.submitUploadForApproval(
                eq(11L), eq(22L), eq("UPLOAD-IDEMPOTENCY-1"), any(String.class),
                any(DccRegistrationCertificateUploadCommand.class)))
                .thenReturn(new DccRegistrationCertificateUploadSubmitResult(33L, 44L, 55L));

        TenantContextHolder.setTenantId(11L);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(22L);
            MockHttpServletRequest request = new MockHttpServletRequest(
                    "POST", "/dcc/registration-certificates/uploads");
            request.setRemoteAddr("10.8.0.31");
            request.addHeader("User-Agent", "JUnit registration upload");

            CommonResult<Long> result = controller.submit("UPLOAD-IDEMPOTENCY-1", reqVO, request);

            assertEquals(33L, result.getData());
        }

        ArgumentCaptor<String> requestTraceId = ArgumentCaptor.forClass(String.class);
        verify(uploadService).submitUploadForApproval(
                eq(11L), eq(22L), eq("UPLOAD-IDEMPOTENCY-1"), requestTraceId.capture(),
                any(DccRegistrationCertificateUploadCommand.class));
        assertFalse(requestTraceId.getValue().isBlank());
        assertTrue(requestTraceId.getValue().length() <= 128);
        verify(approvalService).startNativeApproval(eq(11L), eq(22L),
                eq(new DccRegistrationCertificateApprovalStartCommand(33L)));
    }

    private static DccRegistrationCertificateUploadSubmitReqVO uploadRequest() {
        DccRegistrationCertificateUploadSubmitReqVO reqVO = new DccRegistrationCertificateUploadSubmitReqVO();
        reqVO.setProjectCodeId(1001L);
        reqVO.setCompanyName("上海七木医疗器械有限公司");
        reqVO.setCertificateNo("REG-CERT-UPLOAD-1");
        reqVO.setFirstObtainedDate(LocalDate.of(2025, 1, 1));
        reqVO.setEffectiveDate(LocalDate.of(2025, 1, 2));
        reqVO.setExpiryDate(LocalDate.of(2026, 1, 1));
        reqVO.setClassification("A类");
        reqVO.setRemark("上传注册证审批");
        reqVO.setFile(new MockMultipartFile("file", "registration.pdf", "application/pdf",
                "%PDF-1.4".getBytes(StandardCharsets.UTF_8)));
        return reqVO;
    }
}
