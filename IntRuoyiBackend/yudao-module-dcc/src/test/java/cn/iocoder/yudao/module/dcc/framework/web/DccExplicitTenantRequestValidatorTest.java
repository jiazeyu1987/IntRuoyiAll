package cn.iocoder.yudao.module.dcc.framework.web;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.web.config.WebProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.FORBIDDEN;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DccExplicitTenantRequestValidatorTest {

    private final DccExplicitTenantRequestValidator validator =
            new DccExplicitTenantRequestValidator(new WebProperties());

    @Test
    void validate_targetEndpointRequiresExplicitPositiveIntegerTenant() {
        LoginUser user = loginUser(31L);

        assertCode(BAD_REQUEST.getCode(), request("/admin-api/dcc/controlled-files/upload-preview", null), user);
        assertCode(BAD_REQUEST.getCode(), request("/admin-api/dcc/controlled-files/upload-temporary/status", "abc"), user);
        assertCode(BAD_REQUEST.getCode(), request(
                "/admin-api/dcc/controlled-files/upload-temporary/session-cleanup", "0"), user);
        assertCode(BAD_REQUEST.getCode(), request("/admin-api/dcc/controlled-files/upload-preview", "-1"), user);
    }

    @Test
    void validate_targetEndpointRejectsCrossTenantAndAcceptsMatchingTenant() {
        LoginUser user = loginUser(31L);

        assertCode(FORBIDDEN.getCode(), request("/admin-api/dcc/controlled-files/upload-preview", "32"), user);
        assertDoesNotThrow(() -> validator.validate(
                request("/admin-api/dcc/controlled-files/upload-preview", "31"), user));
    }

    @Test
    void validate_nonTargetEndpointDoesNotChangeGlobalTenantProtocol() {
        assertDoesNotThrow(() -> validator.validate(
                request("/admin-api/system/user/page", null), loginUser(31L)));
    }

    private void assertCode(int expectedCode, MockHttpServletRequest request, LoginUser user) {
        ServiceException exception = assertThrows(ServiceException.class, () -> validator.validate(request, user));
        assertEquals(expectedCode, exception.getCode());
    }

    private MockHttpServletRequest request(String uri, String tenantId) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        if (tenantId != null) {
            request.addHeader("tenant-id", tenantId);
        }
        return request;
    }

    private LoginUser loginUser(Long tenantId) {
        LoginUser user = new LoginUser();
        user.setId(99L);
        user.setTenantId(tenantId);
        return user;
    }
}
