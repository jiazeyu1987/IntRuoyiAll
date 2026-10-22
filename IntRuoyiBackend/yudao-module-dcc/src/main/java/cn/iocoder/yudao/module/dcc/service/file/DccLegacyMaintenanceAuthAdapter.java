package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.biz.system.oauth2.OAuth2TokenCommonApi;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.dal.mysql.oauth2.OAuth2AccessTokenMapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Formal token authentication for a local maintenance lane, never an E2E login bootstrap.
 */
@Component
public class DccLegacyMaintenanceAuthAdapter {

    private final OAuth2TokenCommonApi tokens;

    private final OAuth2AccessTokenMapper storedTokens;

    private final AdminUserApi users;

    public DccLegacyMaintenanceAuthAdapter(OAuth2TokenCommonApi tokens, OAuth2AccessTokenMapper storedTokens, AdminUserApi users) {
        this.tokens = tokens;
        this.storedTokens = storedTokens;
        this.users = users;
    }

    public <T> T withVerifiedActor(String accessToken, Supplier<T> action) {
        if (TenantContextHolder.isIgnore())
            throw new IllegalArgumentException("DCC_MAINTENANCE_TENANT_IGNORE_FORBIDDEN");
        if (accessToken == null || accessToken.isBlank() || accessToken.length() > 65536 || accessToken.codePoints().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("DCC_MAINTENANCE_TOKEN_REQUIRED");
        // Exact database access-token existence prevents the formal API's refresh-token compatibility lane.
        var stored = storedTokens.selectByAccessToken(accessToken);
        if (stored == null || !accessToken.equals(stored.getAccessToken()))
            throw new IllegalArgumentException("DCC_MAINTENANCE_EXACT_ACCESS_TOKEN_REQUIRED");
        var checked = tokens.checkAccessToken(accessToken);
        var now = LocalDateTime.now();
        if (checked == null || checked.getUserId() == null || !Long.valueOf(1).equals(checked.getTenantId()) || !Integer.valueOf(UserTypeEnum.ADMIN.getValue()).equals(checked.getUserType()) || checked.getExpiresTime() == null || !checked.getExpiresTime().isAfter(now) || !Objects.equals(stored.getUserId(), checked.getUserId()) || !Objects.equals(stored.getUserType(), checked.getUserType()) || !Objects.equals(stored.getTenantId(), checked.getTenantId()) || !sameStoredExpiry(stored.getExpiresTime(), checked.getExpiresTime()) || !Objects.equals(stored.getScopes(), checked.getScopes()) || stored.getExpiresTime() == null || !stored.getExpiresTime().isAfter(now))
            throw new IllegalArgumentException("DCC_MAINTENANCE_VERIFIED_TOKEN_IDENTITY_INVALID");
        var previous = SecurityContextHolder.getContext();
        Long previousTenant = TenantContextHolder.getTenantId();
        boolean previousIgnore = TenantContextHolder.isIgnore();
        try {
            TenantContextHolder.setTenantId(checked.getTenantId());
            TenantContextHolder.setIgnore(false);
            var current = users.getUser(checked.getUserId());
            if (current == null || !Objects.equals(current.getId(), checked.getUserId()) || !Objects.equals(current.getTenantId(), checked.getTenantId()) || !Integer.valueOf(0).equals(current.getStatus()) || current.getUsername() == null || current.getUsername().isBlank() || current.getNickname() == null || current.getNickname().isBlank() || checked.getUserInfo() == null || (checked.getUserInfo().containsKey("username") && !current.getUsername().equals(checked.getUserInfo().get("username"))) || !current.getNickname().equals(checked.getUserInfo().get("nickname")))
                throw new IllegalArgumentException("DCC_MAINTENANCE_CURRENT_ACCOUNT_INVALID");
            // Formal ADMIN token info contains nickname/deptId, not username. Bind audit identity to the current server directory.
            var principalInfo = new java.util.HashMap<>(checked.getUserInfo());
            principalInfo.put("username", current.getUsername());
            principalInfo.put(LoginUser.INFO_KEY_NICKNAME, current.getNickname());
            var principal = new LoginUser().setId(checked.getUserId()).setTenantId(checked.getTenantId()).setUserType(checked.getUserType()).setInfo(principalInfo).setScopes(checked.getScopes()).setExpiresTime(checked.getExpiresTime());
            var security = SecurityContextHolder.createEmptyContext();
            security.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, Collections.emptyList()));
            SecurityContextHolder.setContext(security);
            return Objects.requireNonNull(action).get();
        } finally {
            SecurityContextHolder.setContext(previous);
            TenantContextHolder.clear();
            if (previousTenant != null)
                TenantContextHolder.setTenantId(previousTenant);
            TenantContextHolder.setIgnore(previousIgnore);
        }
    }

    private static boolean sameStoredExpiry(LocalDateTime stored, LocalDateTime checked) {
        if (stored == null || checked == null)
            return false;
        if (stored.equals(checked))
            return true;
        // This lane's formal token column is DATETIME(0), without TIME_TRUNCATE_FRACTIONAL.
        // Redis keeps milliseconds; accept only the exact second produced by normal half-up rounding.
        // Different fractional storage precision or an adjacent arbitrary second is never a match.
        return stored.getNano() == 0 && stored.equals(checked.plusNanos(500_000_000).withNano(0));
    }
}
