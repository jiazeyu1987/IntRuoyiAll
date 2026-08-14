package cn.iocoder.yudao.module.dcc.framework.web;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.web.config.WebProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.FORBIDDEN;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils.HEADER_TENANT_ID;

public class DccExplicitTenantRequestValidator implements HandlerInterceptor {

    private final Set<String> targetPaths;

    public DccExplicitTenantRequestValidator(WebProperties webProperties) {
        this.targetPaths = DccUploadApiContractPaths.resolve(webProperties);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        validate(request, SecurityFrameworkUtils.getLoginUser());
        return true;
    }

    void validate(HttpServletRequest request, LoginUser loginUser) {
        if (!targetPaths.contains(DccUploadApiContractPaths.requestPath(request))) {
            return;
        }
        String rawTenantId = StrUtil.trim(request.getHeader(HEADER_TENANT_ID));
        Long tenantId = parsePositiveTenantId(rawTenantId);
        if (loginUser != null && loginUser.getTenantId() != null
                && !Objects.equals(loginUser.getTenantId(), tenantId)) {
            throw new ServiceException(FORBIDDEN.getCode(), "您无权访问该租户的数据");
        }
    }

    private Long parsePositiveTenantId(String rawTenantId) {
        if (StrUtil.isBlank(rawTenantId) || !rawTenantId.matches("[0-9]+")) {
            throw invalidParamException("DCC 请求必须显式携带正整数 tenant-id");
        }
        try {
            long tenantId = Long.parseLong(rawTenantId);
            if (tenantId <= 0) {
                throw invalidParamException("DCC 请求必须显式携带正整数 tenant-id");
            }
            return tenantId;
        } catch (NumberFormatException ex) {
            throw invalidParamException("DCC 请求必须显式携带正整数 tenant-id");
        }
    }
}
