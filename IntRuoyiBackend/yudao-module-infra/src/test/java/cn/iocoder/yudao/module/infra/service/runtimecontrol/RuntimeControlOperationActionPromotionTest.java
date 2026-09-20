package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlActionReqVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeControlOperationActionPromotionTest {

    @TempDir
    private Path tempDir;

    @Test
    void promoteProdButtonDispatchesOnlyTheTestedPackage() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        RuntimeControlActionReqVO request = new RuntimeControlActionReqVO();
        request.setReleaseTag("release-button-prod-r1");

        List<String> args = RuntimeControlOperationAction.PROMOTE_PROD
                .buildArguments(request, "operator", properties);

        assertEquals("deploy-release", args.get(args.indexOf("-Mode") + 1));
        assertEquals("prod", args.get(args.indexOf("-Environment") + 1));
        assertEquals("release-button-prod-r1", args.get(args.indexOf("-ReleaseTag") + 1));
        assertTrue(args.contains("-RequireTested"));
        assertTrue(args.contains("-ConfirmText"));
        assertEquals("PROD", args.get(args.indexOf("-ConfirmText") + 1));
        assertFalse(args.contains("build-release"));
        assertFalse(args.contains("publish-test"));
        assertFalse(args.contains("-TestOperationId"));
        assertFalse(args.contains("-TestOperationEvidencePath"));
    }
}
