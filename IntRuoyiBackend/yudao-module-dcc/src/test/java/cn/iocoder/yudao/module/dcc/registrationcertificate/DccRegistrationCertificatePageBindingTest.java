package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.query.vo.DccRegistrationCertificatePageReqVO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.ServletRequestDataBinder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DccRegistrationCertificatePageBindingTest {

    @Test
    void bracketParametersReachTheTypedQueryWithoutDroppingMapFilters() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("pageNo", "2");
        request.addParameter("certificateType", "FDA");
        request.addParameter("fieldFilters[kpNumber]", "E2E-K1030-NOT-EXIST");
        request.addParameter("fieldFilters[ownProduct]", "false");
        request.addParameter("fieldFilters[productNameEn]", "Product & Description");
        DccRegistrationCertificatePageReqVO vo = new DccRegistrationCertificatePageReqVO();
        ServletRequestDataBinder binder = new ServletRequestDataBinder(vo);
        binder.bind(request);

        assertFalse(binder.getBindingResult().hasErrors());
        assertEquals(2, vo.toQuery().getPageNo());
        assertEquals("FDA", vo.toQuery().getCertificateType());
        assertEquals(Map.of("kpNumber", "E2E-K1030-NOT-EXIST", "ownProduct", "false",
                "productNameEn", "Product & Description"), vo.toQuery().getFieldFilters());
    }
}
