package cn.iocoder.yudao.module.dcc.service.file;

import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccLegacyMaintenanceCommandTest {

    @Test
    void declinedOrWrongScopeArtifactNeverReadsTokenOrCallsAuthOrRegistration() throws Exception {
        var gate = mock(DccLegacyMaintenanceGate.class);
        var auth = mock(DccLegacyMaintenanceAuthAdapter.class);
        var registration = mock(DccLegacyMaintenanceExecutor.class);
        var command = new DccLegacyNameRegistrationMaintenanceCommand(gate, auth, registration, mock(org.apache.ibatis.session.SqlSessionFactory.class));
        when(gate.read(any(), any(), any())).thenThrow(new IllegalArgumentException("DCC_MAINTENANCE_EXACT_REGISTRATION_APPROVAL_REQUIRED"));
        var input = new InputStream() {

            public int read() {
                fail("stdin must not be read without actual permission");
                return -1;
            }
        };
        assertThrows(IllegalStateException.class, () -> command.execute(Path.of("."), Path.of("request"), "a".repeat(64), input));
        verifyNoInteractions(auth, registration);
        assertThrows(IllegalArgumentException.class, () -> command.execute(Path.of("."), Path.of("request"), "a".repeat(64), input));
    }

    @Test
    void incompleteCurrent35Of39ArtifactCannotReachAuthOrPolicyOrRegistration() throws Exception {
        var gate = mock(DccLegacyMaintenanceGate.class);
        var auth = mock(DccLegacyMaintenanceAuthAdapter.class);
        var registration = mock(DccLegacyMaintenanceExecutor.class);
        var request = mock(DccLegacyMaintenanceGate.Input.class);
        when(gate.read(any(), any(), any())).thenReturn(request);
        when(gate.verifiedScope(request)).thenThrow(new IllegalArgumentException("actual complete MATCH receipt required; private source path"));
        var command = new DccLegacyNameRegistrationMaintenanceCommand(gate, auth, registration, mock(org.apache.ibatis.session.SqlSessionFactory.class));
        var input = new InputStream() {

            public int read() {
                fail("incomplete proof must not read token");
                return -1;
            }
        };
        var error = assertThrows(IllegalStateException.class, () -> command.execute(Path.of("."), Path.of("r"), "b".repeat(64), input));
        assertFalse(error.getMessage().contains("private"));
        assertNull(error.getCause());
        verify(gate, never()).currentDevelopmentPolicy(any());
        verifyNoInteractions(auth, registration);
    }

    @Test
    void runnerExplicitLocalEnabledCreatesOnlyOptInBeanNotAutomaticAction() {
        var command = mock(DccLegacyNameRegistrationMaintenanceCommand.class);
        new org.springframework.boot.test.context.runner.ApplicationContextRunner().withUserConfiguration(DccLegacyNameRegistrationMaintenanceRunner.class).withBean(DccLegacyNameRegistrationMaintenanceCommand.class, () -> command).withPropertyValues("spring.profiles.active=local-maintenance", "yudao.dcc.legacy-registration-maintenance.enabled=true", "yudao.dcc.legacy-registration-maintenance.protected-root=C:/task", "yudao.dcc.legacy-registration-maintenance.request-file=C:/task/request.json", "yudao.dcc.legacy-registration-maintenance.request-sha256=" + "a".repeat(64)).run(context -> {
            assertNotNull(context.getBean(DccLegacyNameRegistrationMaintenanceRunner.class));
            verifyNoInteractions(command);
        });
    }

    @Test
    void wrongDevelopmentPolicyRejectsBeforeTokenAndAuthenticatedPorts() throws Exception {
        var gate = mock(DccLegacyMaintenanceGate.class);
        var auth = mock(DccLegacyMaintenanceAuthAdapter.class);
        var executor = mock(DccLegacyMaintenanceExecutor.class);
        var input = mock(DccLegacyMaintenanceGate.Input.class);
        when(gate.read(any(), any(), any())).thenReturn(input);
        doThrow(DccLegacyMaintenanceGate.invalid("EXACT_DEVELOPMENT_POLICY_REQUIRED")).when(gate).requireDevelopmentPolicy(input);
        var command = new DccLegacyNameRegistrationMaintenanceCommand(gate, auth, executor, mock(org.apache.ibatis.session.SqlSessionFactory.class));
        var stdin = new InputStream() {

            public int read() {
                fail("invalid developer policy must not read token");
                return -1;
            }
        };
        assertThrows(IllegalStateException.class, () -> command.execute(Path.of("."), Path.of("request"), "a".repeat(64), stdin));
        verifyNoInteractions(auth, executor);
    }
    @Test
    void nonDevelopmentEnvironmentRejectsBeforeArtifactsStdinAndAuthentication() throws Exception {
        var gate = mock(DccLegacyMaintenanceGate.class);
        doThrow(DccLegacyMaintenanceGate.invalid("DEVELOPMENT_PROFILE_REQUIRED")).when(gate).requireDevelopmentEnvironment();
        var auth = mock(DccLegacyMaintenanceAuthAdapter.class);
        var executor = mock(DccLegacyMaintenanceExecutor.class);
        var command = new DccLegacyNameRegistrationMaintenanceCommand(gate, auth, executor,
                mock(org.apache.ibatis.session.SqlSessionFactory.class));
        var stdin = new InputStream() {
            public int read() { fail("non-development entry must not read a token"); return -1; }
        };
        var error = assertThrows(IllegalStateException.class,
                () -> command.execute(Path.of("."), Path.of("request"), "a".repeat(64), stdin));
        assertEquals("DCC_MAINTENANCE_DEVELOPMENT_PROFILE_REQUIRED", error.getMessage());
        verify(gate, never()).read(any(), any(), any());
        verifyNoInteractions(auth, executor);
    }
}
