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
        request.setTestOperationId("op-test-12345678");
        request.setTestOperationEvidencePath("release/test-acceptance.json");
        request.setExpectedPackageDigest("a".repeat(64));
        request.setExpectedManifestDigest("b".repeat(64));

        List<String> args = RuntimeControlOperationAction.PROMOTE_PROD
                .buildArguments(request, "operator", properties);

        assertEquals("deploy-release", args.get(args.indexOf("-Mode") + 1));
        assertEquals("prod", args.get(args.indexOf("-Environment") + 1));
        assertEquals("release-button-prod-r1", args.get(args.indexOf("-ReleaseTag") + 1));
        assertTrue(args.contains("-TestOperationId"));
        assertEquals("op-test-12345678", args.get(args.indexOf("-TestOperationId") + 1));
        assertTrue(args.contains("-TestOperationEvidencePath"));
        assertEquals("a".repeat(64), args.get(args.indexOf("-ExpectedPackageDigest") + 1));
        assertEquals("b".repeat(64), args.get(args.indexOf("-ExpectedManifestDigest") + 1));
        assertTrue(args.contains("-ConfirmText"));
        assertEquals("PROD", args.get(args.indexOf("-ConfirmText") + 1));
        assertFalse(args.contains("build-release"));
        assertFalse(args.contains("publish-test"));
        assertFalse(args.contains("-PublishScope"));
    }
}
