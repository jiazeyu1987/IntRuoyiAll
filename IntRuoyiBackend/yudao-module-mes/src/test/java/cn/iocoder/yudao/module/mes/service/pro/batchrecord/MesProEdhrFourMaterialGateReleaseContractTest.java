package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProEdhrFourMaterialGateReleaseContractTest {

    @Test
    void everyReleaseEntryUsesSharedServerGate() throws Exception {
        Path source = Path.of("src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/"
                + "MesProEdhrReleaseServiceImpl.java");
        String code = Files.readString(source);
        assertTrue(code.contains("fourMaterialGateService.evaluate(batch.getId())"));
        assertTrue(count(code, "requirePrecheckMaterialManifestCurrent(transaction);") >= 2,
                "submit and submitForApproval must reject a stale precheck manifest");
        assertTrue(code.contains("snapshot.put(\"materialGateManifestHash\", materialGateManifestHash)"));
        assertTrue(code.contains("fourMaterialGateService.requireMaterialsReady(transaction.getBatchExecutionId())"));
        assertTrue(code.indexOf("fourMaterialGateService.requireMaterialsReady(command.getBatchExecutionId())")
                < code.indexOf("authoritativeContextPort.require(command)"));
        assertTrue(!Files.readString(Path.of("src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/"
                        + "MesProEdhrFourMaterialGateServiceImpl.java")).contains("STATUS_RELEASED"),
                "Flow8 gate must not write the Flow10 terminal release state");
    }

    private int count(String value, String needle) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
