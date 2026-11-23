package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.DccProjectCodeService;
import cn.iocoder.yudao.module.mdm.api.companyscope.MdmCompanyScopeApi;
import cn.iocoder.yudao.module.mdm.api.enterprise.MdmEnterpriseApi;
import cn.iocoder.yudao.module.mdm.api.enterprise.dto.MdmEnterpriseRespDTO;
import cn.iocoder.yudao.module.mdm.api.product.MdmProductApi;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccRegistrationCertificateTextValidationTest {
    @Test void productAndRegistrantLengthsReportInputErrorsInsteadOfStateConflicts() {
        var enterprise = mock(MdmEnterpriseApi.class);
        when(enterprise.getEnabledEnterprises(eq(List.of(10L)), any())).thenReturn(
                List.of(MdmEnterpriseRespDTO.builder().id(10L).tenantId(1L).name("Owner").build()));
        var validator = new DccRegistrationCertificatePrerequisiteValidator(mock(MdmCompanyScopeApi.class),
                enterprise, mock(MdmProductApi.class), mock(DccProjectCodeService.class),
                new DccRegistrationCertificateBusinessClock(Clock.fixed(Instant.parse("2026-08-17T01:00:00Z"), ZoneId.of("Asia/Shanghai"))));
        assertDoesNotThrow(() -> validator.validate(1L, 99L, draft("字".repeat(255), "字".repeat(255))));
        var productError = assertThrows(ServiceException.class, () -> validator.validate(1L, 99L, draft("字".repeat(256), "Registrant")));
        assertEquals(400, productError.getCode());
        assertEquals("产品名称长度不能超过255个字符", productError.getMessage());
        var registrantError = assertThrows(ServiceException.class, () -> validator.validate(1L, 99L, draft("Product", "字".repeat(256))));
        assertEquals(400, registrantError.getCode());
        assertEquals("注册人名称长度不能超过255个字符", registrantError.getMessage());
    }
    private DccRegistrationCertificateDraftData draft(String product, String registrant) {
        return new DccRegistrationCertificateDraftData(10L, null, product, null,
                LocalDate.of(2026, 1, 1), "CERT-001", null, LocalDate.of(2026, 2, 1), LocalDate.of(2031, 2, 1),
                "II", registrant, null, null, null, null, null, null, false, true, List.of(), null);
    }
}
