package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProEdhrReleaseTransactionLifecycleContractTest {

    private static final Path ROOT = resolveRepoRoot();

    @Test
    void submitShouldRequireSubmitPermission() throws Exception {
        String source = read("yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrReleaseController.java");

        assertTrue(source.contains("MesProEdhrReleaseController"));
        assertTrue(source.contains("/mes/pro/edhr-release"));
        assertTrue(source.contains("@PostMapping(\"/submit\")"));
        assertTrue(source.contains("mes:pro-edhr-release:submit"));
    }

    @Test
    void approveShouldRequireApprovePermission() throws Exception {
        String source = read("yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrReleaseController.java");

        assertTrue(source.contains("@PostMapping(\"/approve\")"));
        assertTrue(source.contains("mes:pro-edhr-release:approve"));
    }

    @Test
    void rejectShouldRequireRejectPermission() throws Exception {
        String source = read("yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrReleaseController.java");

        assertTrue(source.contains("@PostMapping(\"/reject\")"));
        assertTrue(source.contains("mes:pro-edhr-release:reject"));
    }

    @Test
    void withdrawShouldRequireWithdrawPermission() throws Exception {
        String source = read("yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrReleaseController.java");

        assertTrue(source.contains("@PostMapping(\"/withdraw\")"));
        assertTrue(source.contains("mes:pro-edhr-release:withdraw"));
    }

    @Test
    void eventPageShouldRequireEventQueryPermission() throws Exception {
        String source = read("yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrReleaseController.java");

        assertTrue(source.contains("@GetMapping(\"/event/page\")"));
        assertTrue(source.contains("mes:pro-edhr-release:event-query"));
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
