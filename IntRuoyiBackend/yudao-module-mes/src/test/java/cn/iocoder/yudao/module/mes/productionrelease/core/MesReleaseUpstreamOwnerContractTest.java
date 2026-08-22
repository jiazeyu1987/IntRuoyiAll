package cn.iocoder.yudao.module.mes.productionrelease.core;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MesReleaseUpstreamOwnerContractTest {

    @Test
    void releaseMustUseOwnerCommandsForLocalOrderAndWorkOrderClosure() throws Exception {
        Path root = locateWorktreeRoot();
        String port = Files.readString(root.resolve(
                "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/MesReleaseUpstreamStatePort.java"),
                StandardCharsets.UTF_8);
        String activeOrderService = Files.readString(root.resolve(
                "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderService.java"),
                StandardCharsets.UTF_8);
        String workOrderService = Files.readString(root.resolve(
                "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/workorder/MesProWorkOrderService.java"),
                StandardCharsets.UTF_8);
        String migration = Files.readString(root.resolve("sql/mysql/20260822_mes_edhr_release_final_state_trace.sql"),
                StandardCharsets.UTF_8);

        assertTrue(port.contains("closeAfterRelease"));
        assertTrue(activeOrderService.contains("closeForRelease"));
        assertTrue(workOrderService.contains("finishWorkOrderForRelease"));
        assertTrue(migration.contains("release_decision_id"));
    }

    private static Path locateWorktreeRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("sql/mysql"))
                    && Files.isDirectory(current.resolve("yudao-module-mes/src/main"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("MES backend worktree root was not found");
    }
}
