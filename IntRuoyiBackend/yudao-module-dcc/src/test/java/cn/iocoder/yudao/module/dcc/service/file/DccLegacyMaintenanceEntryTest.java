package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.biz.system.oauth2.OAuth2TokenCommonApi;
import cn.iocoder.yudao.framework.common.biz.system.oauth2.dto.OAuth2AccessTokenCheckRespDTO;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import cn.iocoder.yudao.module.system.dal.mysql.oauth2.OAuth2AccessTokenMapper;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.LocalDateTime;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccLegacyMaintenanceEntryTest {

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    @Test
    void runnerDefaultAbsentFalseAndNonlocalNeverReadsStdinOrCallsCommand() {
        var command = mock(DccLegacyNameRegistrationMaintenanceCommand.class);
        for (String[] properties : new String[][] { {}, { "yudao.dcc.legacy-registration-maintenance.enabled=false" }, { "yudao.dcc.legacy-registration-maintenance.enabled=true" } }) {
            new ApplicationContextRunner().withUserConfiguration(DccLegacyNameRegistrationMaintenanceRunner.class).withBean(DccLegacyNameRegistrationMaintenanceCommand.class, () -> command).withPropertyValues(properties).run(c -> {
                assertThat(c).hasNotFailed().doesNotHaveBean(DccLegacyNameRegistrationMaintenanceRunner.class);
                verifyNoInteractions(command);
            });
        }
    }

    @Test
    void localDisabledStillHasNoRunner() {
        new ApplicationContextRunner().withUserConfiguration(DccLegacyNameRegistrationMaintenanceRunner.class).withPropertyValues("spring.profiles.active=local-maintenance", "yudao.dcc.legacy-registration-maintenance.enabled=false").run(c -> assertThat(c).hasNotFailed().doesNotHaveBean(DccLegacyNameRegistrationMaintenanceRunner.class));
    }

    @Test
    void productionOrTestProfileCannotOptIntoMaintenance() {
        for (String forbidden : new String[] { "prod", "production", "test", "unit-test", "backup" }) new ApplicationContextRunner().withUserConfiguration(DccLegacyNameRegistrationMaintenanceRunner.class).withPropertyValues("spring.profiles.active=local-maintenance," + forbidden, "yudao.dcc.legacy-registration-maintenance.enabled=true").run(c -> assertThat(c).hasNotFailed().doesNotHaveBean(DccLegacyNameRegistrationMaintenanceRunner.class));
    }

    record Auth(DccLegacyMaintenanceAuthAdapter adapter, OAuth2TokenCommonApi api, OAuth2AccessTokenMapper tokens, AdminUserApi users, OAuth2AccessTokenCheckRespDTO checked, OAuth2AccessTokenDO stored, AdminUserRespDTO account) {
    }

    Auth auth() {
        var api = mock(OAuth2TokenCommonApi.class);
        var tokens = mock(OAuth2AccessTokenMapper.class);
        var users = mock(AdminUserApi.class);
        var checked = new OAuth2AccessTokenCheckRespDTO();
        checked.setUserId(7L);
        checked.setTenantId(1L);
        checked.setUserType(2);
        checked.setUserInfo(Map.of("username", "actual", "nickname", "Actual user"));
        checked.setExpiresTime(LocalDateTime.now().plusMinutes(2));
        var stored = new OAuth2AccessTokenDO();
        stored.setId(5L);
        stored.setAccessToken("OFFLINE-TOKEN");
        stored.setUserId(7L);
        stored.setTenantId(1L);
        stored.setUserType(2);
        stored.setExpiresTime(checked.getExpiresTime());
        var account = new AdminUserRespDTO();
        account.setId(7L);
        account.setTenantId(1L);
        account.setUsername("actual");
        account.setNickname("Actual user");
        account.setStatus(0);
        when(api.checkAccessToken("OFFLINE-TOKEN")).thenReturn(checked);
        when(tokens.selectByAccessToken("OFFLINE-TOKEN")).thenReturn(stored);
        when(users.getUser(7L)).thenReturn(account);
        return new Auth(new DccLegacyMaintenanceAuthAdapter(api, tokens, users), api, tokens, users, checked, stored, account);
    }

    @Test
    void formalTokenIdentityBindsActualPrincipalAndRestoresOriginalContextOnFailure() {
        var a = auth();
        var before = SecurityContextHolder.getContext();
        TenantContextHolder.setTenantId(99L);
        TenantContextHolder.setIgnore(false);
        assertThrows(IllegalStateException.class, () -> a.adapter.withVerifiedActor("OFFLINE-TOKEN", () -> {
            var actual = SecurityFrameworkUtils.getLoginUser();
            assertEquals(7L, actual.getId());
            assertEquals("actual", actual.getInfo().get("username"));
            assertEquals(1L, TenantContextHolder.getRequiredTenantId());
            assertFalse(TenantContextHolder.isIgnore());
            throw new IllegalStateException("late audited failure");
        }));
        assertSame(before, SecurityContextHolder.getContext());
        assertEquals(99L, TenantContextHolder.getTenantId());
        assertFalse(TenantContextHolder.isIgnore());
        verify(a.api, never()).createAccessToken(any());
        verify(a.api, never()).refreshAccessToken(any(), any());
    }

    @Test
    void refreshCompatibleCachedOrRevokedTokenCannotCreateAuthenticatedPrincipal() {
        var a = auth();
        when(a.tokens.selectByAccessToken("OFFLINE-TOKEN")).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> a.adapter.withVerifiedActor("OFFLINE-TOKEN", () -> fail("service must not be reached")));
        assertNull(SecurityFrameworkUtils.getLoginUser());
    }

    @Test
    void foreignDisabledWrongTypeExpiredOrStaleNamesReject() {
        for (String changed : new String[] { "tenant", "type", "expired", "disabled", "name", "storeduser", "scopes" }) {
            var a = auth();
            switch(changed) {
                case "tenant" ->
                    a.checked.setTenantId(2L);
                case "type" ->
                    a.checked.setUserType(1);
                case "expired" ->
                    a.checked.setExpiresTime(LocalDateTime.now().minusSeconds(1));
                case "disabled" ->
                    a.account.setStatus(1);
                case "name" ->
                    a.checked.setUserInfo(Map.of("username", "SYSTEM_ACTOR", "nickname", "Actual user"));
                case "storeduser" ->
                    a.stored.setUserId(99L);
                case "scopes" ->
                    a.checked.setScopes(java.util.List.of("forged-scope"));
            }
            assertThrows(IllegalArgumentException.class, () -> a.adapter.withVerifiedActor("OFFLINE-TOKEN", () -> fail("must not activate")));
            assertNull(SecurityFrameworkUtils.getLoginUser());
        }
    }

    @Test
    void formalNicknameAndDeptTokenInfoUsesDirectoryUsernameWithoutMutatingCache() {
        var a = auth();
        var cached = Map.of("nickname", "Actual user", "deptId", "9");
        var original = new java.util.HashMap<>(cached);
        a.checked.setUserInfo(cached);
        var before = SecurityContextHolder.getContext();
        assertEquals("called", a.adapter.withVerifiedActor("OFFLINE-TOKEN", () -> {
            var principal = SecurityFrameworkUtils.getLoginUser();
            assertEquals("actual", principal.getInfo().get("username"));
            assertEquals(a.account.getNickname(), principal.getInfo().get("nickname"));
            assertEquals("9", principal.getInfo().get("deptId"));
            assertNotSame(cached, principal.getInfo());
            return "called";
        }));
        assertEquals(original, cached);
        assertSame(cached, a.checked.getUserInfo());
        assertSame(before, SecurityContextHolder.getContext());
    }

    @Test
    void formalUserInfoStillRequiresExactDirectoryIdentityAndOptionalUsername() {
        for (String invalid : new String[] { "username", "nullusername", "blankusername", "nickname", "missingnickname",
                "disabled", "id", "tenant" }) {
            var a = auth();
            var cache = new java.util.HashMap<>(Map.of("nickname", "Actual user", "deptId", "9"));
            switch (invalid) {
                case "username" -> cache.put("username", "other");
                case "nullusername" -> cache.put("username", null);
                case "blankusername" -> cache.put("username", " ");
                case "nickname" -> cache.put("nickname", "other");
                case "missingnickname" -> cache.remove("nickname");
                case "disabled" -> a.account.setStatus(1);
                case "id" -> a.account.setId(99L);
                case "tenant" -> a.account.setTenantId(2L);
            }
            a.checked.setUserInfo(cache);
            var original = new java.util.HashMap<>(cache);
            var before = SecurityContextHolder.getContext();
            TenantContextHolder.setTenantId(99L);
            var error = assertThrows(IllegalArgumentException.class,
                    () -> a.adapter.withVerifiedActor("OFFLINE-TOKEN", () -> fail("invalid directory must not activate")));
            assertEquals("DCC_MAINTENANCE_CURRENT_ACCOUNT_INVALID", error.getMessage());
            assertEquals(original, cache);
            assertSame(before, SecurityContextHolder.getContext());
            assertEquals(99L, TenantContextHolder.getTenantId());
        }
    }

    @Test
    void sameTokenDatetimeZeroRoundingUpAndDownMatchesCachedMilliseconds() {
        var second = LocalDateTime.now().plusMinutes(2).withNano(0);
        for (int millis : new int[] { 1, 499, 500, 796, 999 }) {
            var a = auth();
            a.checked.setExpiresTime(second.plusNanos(millis * 1_000_000L));
            a.stored.setExpiresTime(millis < 500 ? second : second.plusSeconds(1));
            assertEquals("called", a.adapter.withVerifiedActor("OFFLINE-TOKEN", () -> {
                assertEquals(7L, SecurityFrameworkUtils.getLoginUserId());
                return "called";
            }));
            assertNull(SecurityFrameworkUtils.getLoginUser());
            verify(a.api, never()).refreshAccessToken(any(), any());
        }
        var a = auth();
        var midnight = second.toLocalDate().plusDays(1).atStartOfDay();
        a.checked.setExpiresTime(midnight.minusNanos(204_000_000));
        a.stored.setExpiresTime(midnight);
        assertEquals("called", a.adapter.withVerifiedActor("OFFLINE-TOKEN", () -> "called"));
    }

    @Test
    void actualRedisJsonTimeSerializationRoundtripRetainsMillisAndMatchesRoundedRow() {
        var a = auth();
        var original = LocalDateTime.now().plusMinutes(2).withNano(796_123_456);
        a.stored.setExpiresTime(original);
        var raw = cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(a.stored);
        var cached = cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(raw, OAuth2AccessTokenDO.class);
        assertEquals(original.withNano(796_000_000), cached.getExpiresTime());
        a.checked.setExpiresTime(cached.getExpiresTime());
        a.stored.setExpiresTime(original.plusSeconds(1).withNano(0));
        assertEquals("called", a.adapter.withVerifiedActor("OFFLINE-TOKEN", () -> "called"));
        assertNull(SecurityFrameworkUtils.getLoginUser());
        verify(a.api, never()).refreshAccessToken(any(), any());
    }

    @Test
    void cacheMissWholeSecondsAndExactFractionalEqualityStillAuthenticate() {
        for (int nanos : new int[] { 0, 123_456_789 }) {
            var a = auth();
            var exact = LocalDateTime.now().plusMinutes(2).withNano(nanos);
            a.checked.setExpiresTime(exact);
            a.stored.setExpiresTime(exact);
            assertEquals("called", a.adapter.withVerifiedActor("OFFLINE-TOKEN", () -> "called"));
        }
    }

    @Test
    void wrongSecondAndDifferentFractionalStorageNeverUseTolerance() {
        var second = LocalDateTime.now().plusMinutes(2).withNano(0);
        for (int[] pair : new int[][] { {499_000_000, 1}, {500_000_000, 0}, {796_000_000, 2} }) {
            var a = auth();
            a.checked.setExpiresTime(second.withNano(pair[0]));
            a.stored.setExpiresTime(second.plusSeconds(pair[1]));
            assertThrows(IllegalArgumentException.class,
                    () -> a.adapter.withVerifiedActor("OFFLINE-TOKEN", () -> fail("wrong expiry must not activate")));
            verifyNoInteractions(a.users);
        }
        var a = auth();
        a.checked.setExpiresTime(second.withNano(796_000_000));
        a.stored.setExpiresTime(second.plusSeconds(1).withNano(1));
        assertThrows(IllegalArgumentException.class,
                () -> a.adapter.withVerifiedActor("OFFLINE-TOKEN", () -> fail("nonwhole mismatch must not activate")));
        verifyNoInteractions(a.users);
    }

    @Test
    void bothOriginalExpiriesMustRemainStrictlyFutureEvenWhenRoundedIdentityMatches() {
        var second = LocalDateTime.of(2026, 10, 4, 15, 0, 0);
        var expiredCache = auth();
        expiredCache.checked.setExpiresTime(second.withNano(796_000_000));
        expiredCache.stored.setExpiresTime(second.plusSeconds(1));
        var expiredStored = auth();
        expiredStored.checked.setExpiresTime(second.withNano(499_000_000));
        expiredStored.stored.setExpiresTime(second);
        try (var clock = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            // DB is still future, but the original cached instant has expired.
            clock.when(LocalDateTime::now).thenReturn(second.withNano(900_000_000));
            assertThrows(IllegalArgumentException.class, () -> expiredCache.adapter.withVerifiedActor(
                    "OFFLINE-TOKEN", () -> fail("expired cache must not activate")));
            // Cache is still future, but the original stored instant has expired.
            clock.when(LocalDateTime::now).thenReturn(second.withNano(200_000_000));
            assertThrows(IllegalArgumentException.class, () -> expiredStored.adapter.withVerifiedActor(
                    "OFFLINE-TOKEN", () -> fail("expired row must not activate")));
        }
        verifyNoInteractions(expiredCache.users, expiredStored.users);
    }

    @Test
    void inheritedTenantIgnoreCannotBeUsedForMaintenance() {
        var a = auth();
        TenantContextHolder.setIgnore(true);
        assertThrows(IllegalArgumentException.class, () -> a.adapter.withVerifiedActor("OFFLINE-TOKEN", () -> fail("ignore must not activate")));
        verifyNoInteractions(a.api, a.tokens, a.users);
    }
}
