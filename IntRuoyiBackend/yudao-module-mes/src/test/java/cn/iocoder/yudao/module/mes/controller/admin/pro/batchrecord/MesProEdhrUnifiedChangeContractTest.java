package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProEdhrUnifiedChangeContractTest {

    private static final Path ROOT = resolveRepoRoot();

    @Test
    void pageShouldRequireUnifiedQueryPermission() throws Exception {
        String source = read("yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrUnifiedChangeController.java");

        assertTrue(source.contains("MesProEdhrUnifiedChangeController"));
        assertTrue(source.contains("/mes/pro/edhr-change/unified"));
        assertTrue(source.contains("@GetMapping(\"/page\")"));
        assertTrue(source.contains("mes:pro-edhr-change:unified-query"));
    }

    @Test
    void createShouldRequireUnifiedCreatePermission() throws Exception {
        String source = read("yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrUnifiedChangeController.java");

        assertTrue(source.contains("@PostMapping(\"/create\")"));
        assertTrue(source.contains("mes:pro-edhr-change:unified-create"));
    }

    @Test
    void submitShouldRequireUnifiedSubmitPermission() throws Exception {
        String source = read("yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrUnifiedChangeController.java");

        assertTrue(source.contains("@PostMapping(\"/submit\")"));
        assertTrue(source.contains("@PostMapping(\"/recalculate-impact\")"));
        assertTrue(source.contains("mes:pro-edhr-change:unified-submit"));
    }

    @Test
    void approveShouldRequireUnifiedApprovePermission() throws Exception {
        String source = read("yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrUnifiedChangeController.java");

        assertTrue(source.contains("@PostMapping(\"/approve\")"));
        assertTrue(source.contains("mes:pro-edhr-change:unified-approve"));
    }

    @Test
    void effectShouldRequireUnifiedEffectPermission() throws Exception {
        String source = read("yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrUnifiedChangeController.java");

        assertTrue(source.contains("@PostMapping(\"/effect\")"));
        assertTrue(source.contains("mes:pro-edhr-change:unified-effect"));
    }

    @Test
    void impactAndEventPagesShouldRequireExplicitPermissions() throws Exception {
        String source = read("yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrUnifiedChangeController.java");

        assertTrue(source.contains("@GetMapping(\"/impact/page\")"));
        assertTrue(source.contains("@GetMapping(\"/event/page\")"));
        assertTrue(source.contains("mes:pro-edhr-change:impact-query"));
        assertTrue(source.contains("mes:pro-edhr-change:event-query"));
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
