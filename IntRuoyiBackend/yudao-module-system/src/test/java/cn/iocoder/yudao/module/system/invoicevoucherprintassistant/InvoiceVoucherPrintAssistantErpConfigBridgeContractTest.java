package cn.iocoder.yudao.module.system.invoicevoucherprintassistant;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvoiceVoucherPrintAssistantErpConfigBridgeContractTest {

    private static final Path BACKEND_ROOT = resolveBackendRoot();
    private static final Path SYSTEM_MODULE_ROOT = BACKEND_ROOT.resolve("yudao-module-system");
    private static final Path ERP_MODULE_ROOT = BACKEND_ROOT.resolve("yudao-module-erp");
    private static final Path ASSISTANT_ROOT = BACKEND_ROOT.getParent().getParent()
            .resolve("ProjectPackage").resolve("erp-invoice-voucher-print-assistant").normalize();

    @Test
    void ticketValidationSendsCurrentKingdeeConfigSnapshotToAssistant() throws IOException {
        String controllerSource = read(SYSTEM_MODULE_ROOT.resolve(
                "src/main/java/cn/iocoder/yudao/module/system/controller/admin/auth/AuthController.java"));
        String responseSource = read(SYSTEM_MODULE_ROOT.resolve(
                "src/main/java/cn/iocoder/yudao/module/system/controller/admin/auth/vo/" +
                        "AuthInvoiceVoucherPrintTicketValidateRespVO.java"));
        String providerSource = read(ERP_MODULE_ROOT.resolve(
                "src/main/java/cn/iocoder/yudao/module/erp/service/config/" +
                        "ErpInvoiceVoucherPrintKingdeeConfigProvider.java"));

        assertTrue(responseSource.contains("KingdeeConfig"),
                "ticket validation response must include a server-side Kingdee config snapshot");
        assertTrue(controllerSource.contains("buildInvoiceVoucherPrintKingdeeConfig"),
                "ticket validation must attach the current ERP Kingdee config after permission checks");
        assertTrue(providerSource.contains("implements InvoiceVoucherPrintKingdeeConfigProvider"),
                "ERP module must expose the system bridge provider as a Spring bean");
        assertTrue(providerSource.contains("getEffectiveProperties()"),
                "ticket validation must read the saved active connection selector");
        assertTrue(providerSource.contains("getBaseUrl()"));
        assertTrue(providerSource.contains("getAcctId()"));
        assertTrue(providerSource.contains("getUsername()"));
        assertTrue(providerSource.contains("getPassword()"));
        assertTrue(providerSource.contains("getLcid()"));
        assertTrue(providerSource.contains("getAppId()"));
        assertTrue(providerSource.contains("getSignedData()"));
        assertTrue(providerSource.contains("getTimestamp()"));
    }

    @Test
    void assistantRuntimeMaterializesSessionKingdeeConfigWithoutGlobalEnvFile() throws IOException {
        String serverSource = read(ASSISTANT_ROOT.resolve("server.js"));
        String querySource = read(ASSISTANT_ROOT.resolve("Query-ErpInvoiceVoucherMappings-List.py"));
        String voucherSource = read(ASSISTANT_ROOT.resolve("Get-ErpVoucherPrintData.ps1"));

        assertTrue(serverSource.contains("kingdeeConfig"),
                "assistant server must keep the validated ERP config in the server-side session");
        assertTrue(serverSource.contains("writeKingdeeEnvFile"),
                "assistant server must materialize a session-owned env file from the injected config");
        assertTrue(serverSource.contains("deleteAssistantSessionEnvFile"),
                "assistant server must clean up session-owned ERP env files");
        assertTrue(serverSource.contains("requireAssistantKingdeeEnvPath"),
                "assistant ERP calls must fail fast when the session-owned config file is missing");
        assertTrue(serverSource.contains("const kingdeeEnvPath = requireAssistantKingdeeEnvPath(assistantSession)"),
                "assistant ERP calls must validate the server-side session before invoking ERP scripts");
        assertTrue(serverSource.contains("'-EnvPath', kingdeeEnvPath"),
                "assistant PowerShell ERP calls must use the validated session-owned config file path");
        assertTrue(serverSource.contains("'--env', kingdeeEnvPath"),
                "assistant Python ERP calls must use the validated session-owned config file path");
        assertFalse(serverSource.contains("KINGDEE_ENV_PATH"),
                "assistant runtime must not depend on a deployed global Kingdee env file");
        assertTrue(querySource.contains("KINGDEE_APP_ID"));
        assertTrue(querySource.contains("KINGDEE_SIGNED_DATA"));
        assertTrue(querySource.contains("KINGDEE_TIMESTAMP"));
        assertTrue(voucherSource.contains("Read-DotEnv"));
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static Path resolveBackendRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path probe = current;
        while (probe != null) {
            if (Files.isDirectory(probe.resolve("yudao-module-system"))
                    && Files.isDirectory(probe.resolve("yudao-module-erp"))) {
                return probe;
            }
            probe = probe.getParent();
        }
        throw new IllegalStateException("Cannot locate IntRuoyiBackend root from " + current);
    }
}
