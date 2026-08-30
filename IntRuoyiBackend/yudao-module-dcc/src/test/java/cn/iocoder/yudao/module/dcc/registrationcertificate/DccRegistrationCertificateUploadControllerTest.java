package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.upload.DccRegistrationCertificateUploadController;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.upload.vo.DccRegistrationCertificateUploadCompanyRespVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.upload.vo.DccRegistrationCertificateUploadEntrustedEnterpriseRespVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.upload.vo.DccRegistrationCertificateUploadSubmitReqVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalStartCommand;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.upload.DccRegistrationCertificateUploadCommand;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.upload.DccRegistrationCertificateUploadService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.upload.DccRegistrationCertificateUploadSubmitResult;
import cn.iocoder.yudao.module.mdm.api.enterprise.dto.MdmEnterpriseRespDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        ArgumentCaptor<DccRegistrationCertificateUploadCommand> command =
                ArgumentCaptor.forClass(DccRegistrationCertificateUploadCommand.class);
        verify(uploadService).submitUploadForApproval(
                eq(11L), eq(22L), eq("UPLOAD-IDEMPOTENCY-1"), requestTraceId.capture(),
                command.capture());
        assertFalse(requestTraceId.getValue().isBlank());
        assertTrue(requestTraceId.getValue().length() <= 128);
        assertEquals(1001L, command.getValue().projectCodeId());
        assertEquals(501L, command.getValue().companyId());
        assertEquals(Boolean.TRUE, command.getValue().entrustedProduction());
        assertEquals(Boolean.FALSE, command.getValue().selfProduction());
        assertEquals("一次性使用无菌导管", command.getValue().productName());
        assertEquals(List.of(301L), command.getValue().entrustedEnterpriseIds());
        verify(approvalService).startNativeApproval(eq(11L), eq(22L),
                eq(new DccRegistrationCertificateApprovalStartCommand(33L)));
    }

    @Test
    void submitRequestTreatsProjectCodeAsOptionalAndProductNameAsRequired() throws Exception {
        Field projectCodeId = DccRegistrationCertificateUploadSubmitReqVO.class.getDeclaredField("projectCodeId");
        Field companyId = DccRegistrationCertificateUploadSubmitReqVO.class.getDeclaredField("companyId");
        Field productName = DccRegistrationCertificateUploadSubmitReqVO.class.getDeclaredField("productName");

        assertFalse(hasAnnotation(projectCodeId, "jakarta.validation.constraints.NotNull"));
        assertTrue(hasAnnotation(projectCodeId, "jakarta.validation.constraints.Positive"));
        assertTrue(hasAnnotation(companyId, "jakarta.validation.constraints.NotNull"));
        assertTrue(hasAnnotation(companyId, "jakarta.validation.constraints.Positive"));
        assertTrue(hasAnnotation(productName, "jakarta.validation.constraints.NotBlank"));
        assertThrows(NoSuchFieldException.class,
                () -> DccRegistrationCertificateUploadSubmitReqVO.class.getDeclaredField("companyName"));
    }

    @Test
    void listEntrustedEnterprisesReturnsUploadCandidates() {
        DccRegistrationCertificateUploadService uploadService = mock(DccRegistrationCertificateUploadService.class);
        DccRegistrationCertificateApprovalService approvalService = mock(DccRegistrationCertificateApprovalService.class);
        DccRegistrationCertificateUploadController controller =
                new DccRegistrationCertificateUploadController(uploadService, approvalService);
        MdmEnterpriseRespDTO candidate = MdmEnterpriseRespDTO.builder()
                .id(301L)
                .enterpriseCode("TRUST-301")
                .name("受托企业：上海受托制造有限公司")
                .build();
        when(uploadService.listEntrustedEnterprises(11L, "上海受托")).thenReturn(List.of(candidate));

        TenantContextHolder.setTenantId(11L);
        CommonResult<List<DccRegistrationCertificateUploadEntrustedEnterpriseRespVO>> result =
                controller.listEntrustedEnterprises("上海受托");

        assertEquals(1, result.getData().size());
        assertEquals(301L, result.getData().get(0).getId());
        assertEquals("TRUST-301", result.getData().get(0).getEnterpriseCode());
        assertEquals("受托企业：上海受托制造有限公司", result.getData().get(0).getName());
        verify(uploadService).listEntrustedEnterprises(11L, "上海受托");
    }

    @Test
    void listOwnerCompaniesReturnsCurrentUserScopedCandidates() {
        DccRegistrationCertificateUploadService uploadService = mock(DccRegistrationCertificateUploadService.class);
        DccRegistrationCertificateApprovalService approvalService = mock(DccRegistrationCertificateApprovalService.class);
        DccRegistrationCertificateUploadController controller =
                new DccRegistrationCertificateUploadController(uploadService, approvalService);
        MdmEnterpriseRespDTO candidate = MdmEnterpriseRespDTO.builder()
                .id(501L)
                .enterpriseCode("COMP-501")
                .name("上海七木医疗器械有限公司")
                .build();
        when(uploadService.listOwnerCompanies(11L, 22L, "上海七木")).thenReturn(List.of(candidate));

        TenantContextHolder.setTenantId(11L);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(22L);

            CommonResult<List<DccRegistrationCertificateUploadCompanyRespVO>> result =
                    controller.listOwnerCompanies("上海七木");

            assertEquals(1, result.getData().size());
            assertEquals(501L, result.getData().get(0).getId());
            assertEquals("COMP-501", result.getData().get(0).getEnterpriseCode());
            assertEquals("上海七木医疗器械有限公司", result.getData().get(0).getName());
        }
        verify(uploadService).listOwnerCompanies(11L, 22L, "上海七木");
    }

    private static DccRegistrationCertificateUploadSubmitReqVO uploadRequest() {
        DccRegistrationCertificateUploadSubmitReqVO reqVO = new DccRegistrationCertificateUploadSubmitReqVO();
        reqVO.setProjectCodeId(1001L);
        reqVO.setCompanyId(501L);
        reqVO.setProductName("一次性使用无菌导管");
        reqVO.setCertificateNo("REG-CERT-UPLOAD-1");
        reqVO.setFirstObtainedDate(LocalDate.of(2025, 1, 1));
        reqVO.setEffectiveDate(LocalDate.of(2025, 1, 2));
        reqVO.setExpiryDate(LocalDate.of(2026, 1, 1));
        reqVO.setClassification("A类");
        reqVO.setEntrustedProduction(true);
        reqVO.setSelfProduction(false);
        reqVO.setEntrustedEnterpriseIds(List.of(301L));
        reqVO.setRemark("上传注册证审批");
        reqVO.setFile(new MockMultipartFile("file", "registration.pdf", "application/pdf",
                "%PDF-1.4".getBytes(StandardCharsets.UTF_8)));
        return reqVO;
    }

    private static boolean hasAnnotation(Field field, String annotationClassName) {
        for (java.lang.annotation.Annotation annotation : field.getAnnotations()) {
            if (annotation.annotationType().getName().equals(annotationClassName)) {
                return true;
            }
        }
        return false;
    }
}
