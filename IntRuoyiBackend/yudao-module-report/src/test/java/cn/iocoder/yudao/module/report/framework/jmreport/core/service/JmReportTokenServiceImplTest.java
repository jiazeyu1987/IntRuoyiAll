package cn.iocoder.yudao.module.report.framework.jmreport.core.service;

import cn.iocoder.yudao.framework.common.biz.system.oauth2.OAuth2TokenCommonApi;
import cn.iocoder.yudao.framework.common.biz.system.oauth2.dto.OAuth2AccessTokenCheckRespDTO;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.security.config.SecurityProperties;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.framework.common.biz.system.permission.PermissionCommonApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JmReportTokenServiceImplTest {

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    @Test
    void verifyToken_shouldUseJimuAccessTokenHeaderWhenTokenArgumentIsBlank() {
        OAuth2TokenCommonApi tokenApi = mock(OAuth2TokenCommonApi.class);
        PermissionCommonApi permissionApi = mock(PermissionCommonApi.class);
        SecurityProperties securityProperties = new SecurityProperties();
        String accessToken = "jimu-header-token";
        when(tokenApi.checkAccessToken(accessToken)).thenReturn(validToken());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Access-Token", accessToken);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Boolean verified = new JmReportTokenServiceImpl(tokenApi, permissionApi, securityProperties)
                .verifyToken(null);

        assertTrue(verified);
    }

    private OAuth2AccessTokenCheckRespDTO validToken() {
        OAuth2AccessTokenCheckRespDTO token = new OAuth2AccessTokenCheckRespDTO();
        token.setUserId(1L);
        token.setUserType(UserTypeEnum.ADMIN.getValue());
        token.setTenantId(122L);
        token.setScopes(List.of("default"));
        token.setExpiresTime(LocalDateTime.now().plusMinutes(30));
        return token;
    }

}
