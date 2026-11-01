package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.MesProcessPoolTeamLeaderController;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesFixedTestOrderModeGuardTest {
    ConfigApi config = mock(ConfigApi.class);
    MesFixedTestOrderModeGuard guard = new MesFixedTestOrderModeGuard(config);

    @BeforeEach void login() {
        TenantContextHolder.setTenantId(92820L);
        var user = new LoginUser().setId(3001L).setTenantId(92820L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    @AfterEach void clear() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test void enabledRequiresNoRolesOrMaintenancePermission() {
        when(config.getConfigValueByKey(MesFixedTestOrderModeGuard.TEST_MODE_KEY)).thenReturn("true");
        assertDoesNotThrow(() -> guard.requireEnabled(3001L));
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().isEmpty());
    }

    @ParameterizedTest @NullAndEmptySource
    @ValueSource(strings = {"false", "TRUE", "1", " true", "invalid"})
    void disabledMissingAndInvalidConfigRejectBeforeAnyResetDataAccess(String value) {
        when(config.getConfigValueByKey(MesFixedTestOrderModeGuard.TEST_MODE_KEY)).thenReturn(value);
        var service = constructWithMocks(MesTeamLeaderActiveOrderServiceImpl.class);
        var orders = mock(MesProWorkOrderMapper.class);
        ReflectionTestUtils.setField(service, "fixedTestOrderModeGuard", guard);
        ReflectionTestUtils.setField(service, "workOrderMapper", orders);
        var error = assertThrows(ServiceException.class, () -> service.resetFixedSimulationActiveOrder(3001L));
        assertTrue(error.getMessage().contains("必须明确配置为 true"));
        verifyNoInteractions(orders);
    }

    @Test void configFailurePropagates() {
        when(config.getConfigValueByKey(MesFixedTestOrderModeGuard.TEST_MODE_KEY))
                .thenThrow(new IllegalStateException("config unavailable"));
        assertThrows(IllegalStateException.class, () -> guard.requireEnabled(3001L));
    }

    @Test void closingTestModeTakesEffectOnTheNextResetAttempt() {
        when(config.getConfigValueByKey(MesFixedTestOrderModeGuard.TEST_MODE_KEY))
                .thenReturn("true", "false");
        assertDoesNotThrow(() -> guard.requireEnabled(3001L));
        assertThrows(ServiceException.class, () -> guard.requireEnabled(3001L));
        verify(config, times(2)).getConfigValueByKey(MesFixedTestOrderModeGuard.TEST_MODE_KEY);
    }

    @ParameterizedTest @ValueSource(strings = {"anonymous", "otherTenant", "noTenant", "ignoreTenant", "otherUser"})
    void missingOrMismatchedIdentityCannotGainResetAbility(String scenario) {
        switch (scenario) {
            case "anonymous" -> SecurityContextHolder.clearContext();
            case "otherTenant" -> TenantContextHolder.setTenantId(92821L);
            case "noTenant" -> TenantContextHolder.clear();
            case "ignoreTenant" -> TenantContextHolder.setIgnore(true);
            case "otherUser" -> { }
        }
        assertThrows(ServiceException.class, () -> guard.requireEnabled(scenario.equals("otherUser") ? 3002L : 3001L));
        verifyNoInteractions(config);
    }

    @Test void endpointMethodSecurityAllowsAuthenticatedUserWithoutAuthorities() {
        var service = mock(MesTeamLeaderActiveOrderService.class);
        when(service.resetFixedSimulationActiveOrder(3001L)).thenReturn(
                MesTeamLeaderActiveOrderTestResetResult.builder().workOrderId(201L).activeOrderId(101L).build());
        var target = constructWithMocks(MesProcessPoolTeamLeaderController.class);
        ReflectionTestUtils.setField(target, "activeOrderService", service);
        var factory = new ProxyFactory(target);
        factory.addAdvisor(AuthorizationManagerBeforeMethodInterceptor.preAuthorize());
        var controller = (MesProcessPoolTeamLeaderController) factory.getProxy();
        assertDoesNotThrow(controller::resetFixedSimulationActiveOrder);
        verify(service).resetFixedSimulationActiveOrder(3001L);
        clearInvocations(service);
        SecurityContextHolder.clearContext();
        assertThrows(org.springframework.security.core.AuthenticationException.class,
                controller::resetFixedSimulationActiveOrder);
        verifyNoInteractions(service);
    }

    private static <T> T constructWithMocks(Class<T> type) {
        var constructor = type.getConstructors()[0];
        Object[] args = java.util.Arrays.stream(constructor.getParameterTypes())
                .map(org.mockito.Mockito::mock).toArray();
        try {
            return type.cast(constructor.newInstance(args));
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
    }
}
