package cn.iocoder.yudao.module.mes.service.pro.simulation.stage6;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MesStage6IdiSimulationCommandTest {

    @Test
    void acceptsTheFixedIdiSimulationContract() {
        assertDoesNotThrow(() -> MesStage6IdiSimulationCommand.validate(
                "STAGE6-20260824-001"));
    }

    @Test
    void rejectsMissingRunIdentityOrSignaturePassword() {
        assertThrows(IllegalArgumentException.class, () -> MesStage6IdiSimulationCommand.validate(
                ""));
    }

    @Test
    void simulationServiceConsumesStage5ReleaseSnapshotOnly() throws Exception {
        String source = Files.readString(resolveBackendPath(
                "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage6/"
                        + "MesStage6IdiSimulationServiceImpl.java"), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");

        assertFalse(source.contains("command.getActorUserId()"));
        assertFalse(source.contains("command.getSignaturePassword()"));
        assertTrue(source.contains("stage5Service.getReleaseSnapshot"));
    }

    private static Path resolveBackendPath(String relative) {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        if ("yudao-module-mes".equals(current.getFileName().toString())) {
            return current.getParent().resolve(relative);
        }
        if ("IntRuoyiBackend".equals(current.getFileName().toString())) {
            return current.resolve(relative);
        }
        return current.resolve("IntRuoyiBackend").resolve(relative);
    }
}
