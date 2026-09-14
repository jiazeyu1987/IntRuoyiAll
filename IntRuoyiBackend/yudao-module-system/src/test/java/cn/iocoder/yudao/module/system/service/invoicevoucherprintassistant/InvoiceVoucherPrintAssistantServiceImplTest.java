package cn.iocoder.yudao.module.system.service.invoicevoucherprintassistant;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.system.controller.admin.auth.vo.AuthInvoiceVoucherPrintAssistantStatusRespVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

class InvoiceVoucherPrintAssistantServiceImplTest extends BaseMockitoUnitTest {

    @TempDir
    Path tempDir;

    @Spy
    private InvoiceVoucherPrintAssistantProperties properties = new InvoiceVoucherPrintAssistantProperties();
    @Mock
    private InvoiceVoucherPrintAssistantHealthProbe healthProbe;
    @Mock
    private InvoiceVoucherPrintAssistantProcessStarter processStarter;
    @InjectMocks
    private InvoiceVoucherPrintAssistantServiceImpl service;

    @BeforeEach
    void setUp() throws IOException {
        Path rootDir = tempDir.resolve("erp-invoice-voucher-print-assistant");
        Files.createDirectories(rootDir);
        Files.writeString(rootDir.resolve("server.js"), "console.log('assistant');", StandardCharsets.UTF_8);
        Path nodeCommand = tempDir.resolve("node.exe");
        Files.writeString(nodeCommand, "node", StandardCharsets.UTF_8);

        properties.setAssistantHost("127.0.0.1");
        properties.setAssistantPort(18733);
        properties.setAssistantRootDir(rootDir.toString());
        properties.setAssistantScript("server.js");
        properties.setNodeCommand(nodeCommand.toString());
        properties.setTicketValidateUrl(
                "http://127.0.0.1:48081/admin-api/system/auth/invoice-voucher-print-ticket/validate");
        properties.setSessionTtlSeconds(14_400L);
        properties.setStartupTimeoutSeconds(2);
        properties.setProbeTimeoutSeconds(1);
        properties.setProbeIntervalMillis(1);
        properties.afterPropertiesSet();
    }

    @Test
    void getStatusReturnsRunningWhenAssistantIsAlreadyOnline() {
        when(healthProbe.isRunning()).thenReturn(true);

        AuthInvoiceVoucherPrintAssistantStatusRespVO respVO = service.getStatus();

        assertTrue(respVO.getRunning());
        assertTrue(respVO.getLaunchable());
        assertEquals("发票凭证打印助手已启动", respVO.getMessage());
        verify(processStarter, never()).start();
    }

    @Test
    void getStatusReturnsLaunchableWhenAssistantIsOffline() {
        when(healthProbe.isRunning()).thenReturn(false);

        AuthInvoiceVoucherPrintAssistantStatusRespVO respVO = service.getStatus();

        assertFalse(respVO.getRunning());
        assertTrue(respVO.getLaunchable());
        assertEquals("发票凭证打印助手尚未启动，请点击启动助手。", respVO.getMessage());
    }

    @Test
    void startLaunchesAssistantAndWaitsForHealthProbe() {
        Process process = mock(Process.class);
        when(healthProbe.isRunning()).thenReturn(false, false, true);
        when(processStarter.start()).thenReturn(process);
        when(process.isAlive()).thenReturn(true);

        AuthInvoiceVoucherPrintAssistantStatusRespVO respVO = service.start();

        assertTrue(respVO.getRunning());
        assertTrue(respVO.getLaunchable());
        assertEquals("发票凭证打印助手已启动", respVO.getMessage());
        verify(processStarter).start();
    }

    @Test
    void startRejectsWhenLaunchConfigurationIsMissing() {
        properties.setAssistantScript("");
        when(healthProbe.isRunning()).thenReturn(false);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.start());

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("assistant-script"));
        verify(processStarter, never()).start();
    }

    @Test
    void afterPropertiesSetRejectsNonFixedAssistantPort() {
        properties.setAssistantPort(18734);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> properties.afterPropertiesSet());

        assertTrue(exception.getMessage().contains("18733"));
    }

}
