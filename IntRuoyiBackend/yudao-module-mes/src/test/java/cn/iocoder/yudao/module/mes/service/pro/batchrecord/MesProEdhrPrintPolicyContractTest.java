package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProEdhrPrintPolicyContractTest {

    @Test
    void printPolicyReprintVoidCopyAndExportContractsAreDeclared() throws Exception {
        Path projectDir = findProjectDir();
        String serviceImpl = read(projectDir,
                "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/"
                        + "MesProEdhrLabelPrintServiceImpl.java");
        String printTaskController = read(projectDir,
                "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/"
                        + "MesProEdhrPrintTaskController.java");
        String policyController = read(projectDir,
                "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/"
                        + "MesProEdhrPrintPolicyController.java");
        String schema = read(projectDir, "sql/mysql/20260618_mes_edhr_print_policy_reissue_void.sql");

        for (String fragment : new String[]{
                "requireActivePrintPolicy",
                "requireReasonInPolicy",
                "requireReprintLimit",
                "requireVoidRestrictedSource",
                "requireVoidWatermark",
                "requireExportIdempotency",
                "PRO_EDHR_PRINT_VOID_COPY_SOURCE_INVALID",
                "PRINT_REPRINT_POLICY_ACCEPTED",
                "PRINT_VOID_HISTORY_COPY_CREATED",
                "PRINT_HISTORY_EXPORTED"
        }) {
            assertTrue(serviceImpl.contains(fragment), "Service contract must contain " + fragment);
        }

        for (String fragment : new String[]{
                "@PostMapping(\"/reprint/apply\")",
                "@PostMapping(\"/history-copy\")",
                "@PostMapping(\"/export-history\")",
                "mes:pro-edhr-print-task:reprint",
                "mes:pro-edhr-print-task:history-copy",
                "mes:pro-edhr-print-task:export"
        }) {
            assertTrue(printTaskController.contains(fragment), "Print task controller must contain " + fragment);
        }

        for (String fragment : new String[]{
                "@RequestMapping(\"/mes/pro/edhr-print-policy\")",
                "mes:pro-edhr-print-policy:query",
                "mes:pro-edhr-print-policy:create",
                "mes:pro-edhr-print-policy:activate"
        }) {
            assertTrue(policyController.contains(fragment), "Policy controller must contain " + fragment);
        }

        for (String fragment : new String[]{
                "mes_pro_edhr_print_policy",
                "mes_pro_edhr_reprint_request",
                "mes_pro_edhr_print_history_copy",
                "mes_pro_edhr_print_export_audit",
                "mes:pro-edhr-print-policy:query",
                "mes:pro-edhr-print-task:history-copy"
        }) {
            assertTrue(schema.contains(fragment), "Schema contract must contain " + fragment);
        }

        assertFalse(serviceImpl.contains("DEFAULT_SUCCESS"), "Service must not declare default print success");
        assertFalse(serviceImpl.contains("window.print"), "Backend must not depend on browser printing");
        assertFalse(serviceImpl.contains("catch (Exception"), "Service must not swallow generic exceptions");
    }

    private static String read(Path projectDir, String relativePath) throws Exception {
        return Files.readString(projectDir.resolve(relativePath), StandardCharsets.UTF_8);
    }

    private static Path findProjectDir() {
        Path currentDir = Path.of(System.getProperty("user.dir"));
        return "yudao-module-mes".equals(currentDir.getFileName().toString()) ? currentDir.getParent() : currentDir;
    }

}
