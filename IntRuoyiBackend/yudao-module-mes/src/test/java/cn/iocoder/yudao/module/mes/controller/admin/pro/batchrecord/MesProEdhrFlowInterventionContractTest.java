package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProEdhrFlowInterventionContractTest {

    private static final Path ROOT = resolveRepoRoot();

    @Test
    void returnShouldRequireReturnPermission() throws Exception {
        String source = read("yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrFlowInterventionController.java");

        assertTrue(source.contains("MesProEdhrFlowInterventionController"));
        assertTrue(source.contains("/mes/pro/edhr-flow-intervention"));
        assertTrue(source.contains("@PostMapping(\"/return\")"));
        assertTrue(source.contains("mes:pro-edhr-flow-intervention:return"));
    }

    @Test
    void withdrawShouldRequireWithdrawPermission() throws Exception {
        String source = read("yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrFlowInterventionController.java");

        assertTrue(source.contains("@PostMapping(\"/withdraw\")"));
        assertTrue(source.contains("mes:pro-edhr-flow-intervention:withdraw"));
    }

    @Test
    void transferShouldRequireTransferPermission() throws Exception {
        String source = read("yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrFlowInterventionController.java");

        assertTrue(source.contains("@PostMapping(\"/transfer\")"));
        assertTrue(source.contains("mes:pro-edhr-flow-intervention:transfer"));
    }

    @Test
    void addSignShouldRequireAddSignPermission() throws Exception {
        String source = read("yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrFlowInterventionController.java");

        assertTrue(source.contains("@PostMapping(\"/add-sign\")"));
        assertTrue(source.contains("mes:pro-edhr-flow-intervention:add-sign"));
    }

    @Test
    void adminInterveneShouldRequireAdminPermission() throws Exception {
        String source = read("yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrFlowInterventionController.java");

        assertTrue(source.contains("@PostMapping(\"/admin-intervene\")"));
        assertTrue(source.contains("mes:pro-edhr-flow-intervention:admin-intervene"));
    }

    @Test
    void eventPageShouldRequireEventQueryPermission() throws Exception {
        String source = read("yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrFlowInterventionController.java");

        assertTrue(source.contains("@GetMapping(\"/event/page\")"));
        assertTrue(source.contains("mes:pro-edhr-flow-intervention:event-query"));
    }

    private String read(String relativePath) throws Exception {
        Path path = ROOT.resolve(relativePath);
        assertTrue(Files.exists(path), relativePath + " must exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static Path resolveRepoRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        if (Files.exists(current.resolve("sql/mysql"))) {
            return current;
        }
        return current.getParent();
    }
}
