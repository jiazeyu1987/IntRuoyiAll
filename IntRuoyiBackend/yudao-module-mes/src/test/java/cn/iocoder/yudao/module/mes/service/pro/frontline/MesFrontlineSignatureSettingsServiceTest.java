package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.system.api.signature.SignatureAuthenticationApi;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesFrontlineSignatureSettingsServiceTest {
    private final MesFrontlineEmployeeSwitchService employees = mock(MesFrontlineEmployeeSwitchService.class);
    private final SignatureAuthenticationApi system = mock(SignatureAuthenticationApi.class);
    private final SignatureAuthenticationApi profile = mock(SignatureAuthenticationApi.class);

    @Test void formalEmployeeUsesSelectedWorkersMethodInsteadOfDeviceOperatorsMethod() {
        when(system.identityDomain()).thenReturn("SYSTEM_USER");
        when(profile.identityDomain()).thenReturn("MES_EMPLOYEE_PROFILE");
        when(system.credentialType(7L)).thenReturn("PIN");
        try (var security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(2L);
            var service = new MesFrontlineSignatureSettingsService(employees, List.of(system, profile));
            assertEquals("PIN", service.get(10L, 20L, 30L, 40L, 7L, "SYSTEM_USER").credentialType());
            verify(employees).switchActualEmployee(new MesFrontlineEmployeeSwitchCommand(2L, 10L, 20L, 30L, 40L, 7L, "SYSTEM_USER"));
            verify(system, never()).credentialType(2L);
            verify(profile, never()).credentialType(any());
        }
    }

    @Test void temporaryEmployeeWithSameNumericIdUsesProfileIdentity() {
        when(system.identityDomain()).thenReturn("SYSTEM_USER");
        when(profile.identityDomain()).thenReturn("MES_EMPLOYEE_PROFILE");
        when(profile.credentialType(7L)).thenReturn("PASSWORD");
        var service = new MesFrontlineSignatureSettingsService(employees, List.of(system, profile));
        assertEquals("PASSWORD", service.get(10L, 20L, 30L, 40L, 7L, "MES_EMPLOYEE_PROFILE").credentialType());
        verify(system, never()).credentialType(any());
    }

    @Test void outOfScopeEmployeeFailsBeforeReadingAnyCredentials() {
        when(employees.switchActualEmployee(any())).thenThrow(new IllegalArgumentException("not in team"));
        var service = new MesFrontlineSignatureSettingsService(employees, List.of(system, profile));
        assertThrows(IllegalArgumentException.class, () -> service.get(10L, 20L, 30L, 40L, 7L, "SYSTEM_USER"));
        verifyNoInteractions(system, profile);
    }

    @Test void missingOrDuplicateProviderAndInvalidMethodFailWithoutDefaulting() {
        when(system.identityDomain()).thenReturn("SYSTEM_USER");
        assertThrows(IllegalStateException.class, () -> new MesFrontlineSignatureSettingsService(employees, List.of())
                .get(10L, 20L, 30L, 40L, 7L, "SYSTEM_USER"));
        assertThrows(IllegalStateException.class, () -> new MesFrontlineSignatureSettingsService(employees, List.of(system, system))
                .get(10L, 20L, 30L, 40L, 7L, "SYSTEM_USER"));
        when(system.credentialType(7L)).thenReturn("UNKNOWN");
        assertThrows(IllegalStateException.class, () -> new MesFrontlineSignatureSettingsService(employees, List.of(system))
                .get(10L, 20L, 30L, 40L, 7L, "SYSTEM_USER"));
    }
}
