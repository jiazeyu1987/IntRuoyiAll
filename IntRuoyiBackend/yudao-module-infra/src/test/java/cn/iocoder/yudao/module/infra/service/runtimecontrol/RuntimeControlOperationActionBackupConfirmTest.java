package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlActionReqVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeControlOperationActionBackupConfirmTest {

    @TempDir
    private Path tempDir;

    @Test
    void prodBackupNowShouldPassBackupScriptProductionConfirmation() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setTargetEnvironment("prod");

        List<String> args = RuntimeControlOperationAction.BACKUP_NOW.buildArguments(reqVO, "scheduler", properties);

        assertTrue(args.contains("-ProductionBackupConfirmText"));
        int index = args.indexOf("-ProductionBackupConfirmText");
        assertEquals("PROD-BACKUP-172.30.30.57", args.get(index + 1));
    }
}
