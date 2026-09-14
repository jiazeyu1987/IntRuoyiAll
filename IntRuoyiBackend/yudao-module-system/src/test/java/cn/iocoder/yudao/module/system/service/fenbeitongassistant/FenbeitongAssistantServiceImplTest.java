package cn.iocoder.yudao.module.system.service.fenbeitongassistant;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.system.controller.admin.auth.vo.AuthFenbeitongAssistantStatusRespVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FenbeitongAssistantServiceImplTest extends BaseMockitoUnitTest {

    @TempDir
    private Path tempDir;

    @InjectMocks
    private FenbeitongAssistantServiceImpl service;
    @Spy
    private FenbeitongAssistantProperties properties = new FenbeitongAssistantProperties();
    @Mock
    private FenbeitongAssistantHealthProbe healthProbe;
    @Mock
    private FenbeitongAssistantProcessStarter processStarter;

    @BeforeEach
    void setUp() throws IOException {
        Path rootDir = tempDir.resolve("student-collaboration-kit");
        Files.createDirectories(rootDir);
        Files.writeString(rootDir.resolve("server.js"), "console.log('test')");
        Path nodeCommand = tempDir.resolve("node.exe");
        Files.writeString(nodeCommand, "test");
        properties.setAssistantRootDir(rootDir.toString());
        properties.setAssistantScript("server.js");
        properties.setNodeCommand(nodeCommand.toString());
        properties.setProbeIntervalMillis(1);
        properties.setStartupTimeoutSeconds(1);
    }

    @Test
    void getStatusReturnsRunningWhenHealthProbeSucceeds() {
        when(healthProbe.isRunning()).thenReturn(true);

        AuthFenbeitongAssistantStatusRespVO status = service.getStatus();

        assertTrue(status.getRunning());
        assertTrue(status.getLaunchable());
    }

    @Test
    void getStatusReturnsLaunchableWhenConfigurationIsComplete() {
        when(healthProbe.isRunning()).thenReturn(false);

        AuthFenbeitongAssistantStatusRespVO status = service.getStatus();

        assertFalse(status.getRunning());
        assertTrue(status.getLaunchable());
        assertTrue(status.getMessage().contains("尚未启动"));
    }

    @Test
    void startWaitsUntilAssistantIsOnline() {
        Process process = mock(Process.class);
        when(healthProbe.isRunning()).thenReturn(false, true);
        when(processStarter.start()).thenReturn(process);

        AuthFenbeitongAssistantStatusRespVO status = service.start();

        assertTrue(status.getRunning());
        verify(processStarter).start();
    }

    @Test
    void startRejectsMissingScript() {
        properties.setAssistantScript("missing.js");
        when(healthProbe.isRunning()).thenReturn(false);

        ServiceException exception = assertThrows(ServiceException.class, service::start);

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("assistant-script"));
        verify(processStarter, never()).start();
    }

    @Test
    void propertiesRejectNonFixedPort() {
        properties.setAssistantPort(18733);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, properties::afterPropertiesSet);

        assertTrue(exception.getMessage().contains("18734"));
    }

}
