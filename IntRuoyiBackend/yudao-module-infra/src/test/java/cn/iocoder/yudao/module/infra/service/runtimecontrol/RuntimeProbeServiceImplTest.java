package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlAlertCreateReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOwnerMatrixSaveReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlProbeLatestRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlProbeRespVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeProbeServiceImplTest {

    @TempDir
    private Path tempDir;

    private RuntimeOpsProbeServiceImpl probeService;
    private RuntimeOpsAlertServiceImpl alertService;
    private RuntimeOpsCapturingSiteMessageSender siteMessageSender;

    @BeforeEach
    void setUp() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        properties.setEnvironments(Map.of("local", environment()));
        RuntimeOpsResponsibilityServiceImpl responsibilityService =
                new RuntimeOpsResponsibilityServiceImpl(new RuntimeOpsOwnerMatrixStore(properties));
        responsibilityService.createOwner(owner());
        siteMessageSender = new RuntimeOpsCapturingSiteMessageSender();
        alertService = new RuntimeOpsAlertServiceImpl(new RuntimeOpsAlertStore(properties),
                responsibilityService, siteMessageSender);
        probeService = new RuntimeOpsProbeServiceImpl(properties, new RuntimeOpsProbeStore(properties),
                new FixtureProbeHttpClient(), alertService);
    }

    @Test
    void runProbesShouldRecordFailuresAndCreateSiteMessageAlertWhenThresholdIsReached() {
        RuntimeControlProbeLatestRespVO result = probeService.runProbes();

        assertEquals(RuntimeOpsInspectionStatus.NO_GO, result.getStatus());
        assertEquals(3, result.getProbes().size());
        assertProbe(result.getProbes(), "intruoyi-backend", RuntimeOpsInspectionStatus.PASS, 200, null);
        assertProbe(result.getProbes(), "intruoyi-frontend", RuntimeOpsInspectionStatus.NO_GO, null, "connect timed out");
        assertProbe(result.getProbes(), "website-frontend", RuntimeOpsInspectionStatus.NO_GO, 503, "HTTP 503");
        assertNotNull(result.getAlert());
        assertTrue(result.getAlert().getContent().contains("目标=http://frontend.test/"));
        assertTrue(result.getAlert().getContent().contains("目标=http://website.test/"));
        assertEquals(RuntimeControlSiteMessageStatus.SENT, result.getAlert().getSiteMessageStatus());
        assertEquals(1, siteMessageSender.callCount.get());

        RuntimeControlProbeLatestRespVO latest = probeService.getLatestProbes();
        assertEquals(result.getStatus(), latest.getStatus());
        assertEquals(3, latest.getProbes().size());
    }

    @Test
    void runProbesShouldIncludeProductionTargetsWhenProductionWriteAccessIsDisabled() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir.resolve("prod-probes"));
        CapturingProbeHttpClient httpClient = new CapturingProbeHttpClient();
        RuntimeOpsProbeServiceImpl service = new RuntimeOpsProbeServiceImpl(properties, new RuntimeOpsProbeStore(properties),
                httpClient, alertService);

        RuntimeControlProbeLatestRespVO result = service.runProbes();

        assertEquals(RuntimeOpsInspectionStatus.PASS, result.getStatus());
        assertProbe(result.getProbes(), "prod", "intruoyi-backend", RuntimeOpsInspectionStatus.PASS, 200, null);
        assertProbe(result.getProbes(), "prod", "intruoyi-frontend", RuntimeOpsInspectionStatus.PASS, 200, null);
        assertProbe(result.getProbes(), "prod", "website-frontend", RuntimeOpsInspectionStatus.PASS, 200, null);
        assertTrue(httpClient.urls.stream().anyMatch(url -> url.contains("172.30.30.57:48081")));
        assertTrue(httpClient.urls.stream().anyMatch(url -> url.contains("172.30.30.57:8081")));
        assertTrue(httpClient.urls.stream().anyMatch(url -> url.contains("172.30.30.57:8083")));
    }

    private void assertProbe(List<RuntimeControlProbeRespVO> probes, String component,
                             RuntimeOpsInspectionStatus status, Integer httpStatus, String reason) {
        RuntimeControlProbeRespVO probe = probes.stream()
                .filter(item -> component.equals(item.getComponent()))
                .findFirst()
                .orElseThrow();
        assertEquals(status, probe.getStatus());
        assertEquals(httpStatus, probe.getHttpStatusCode());
        if (reason != null) {
            assertTrue(probe.getError().contains(reason));
        }
    }

    private void assertProbe(List<RuntimeControlProbeRespVO> probes, String environment, String component,
                             RuntimeOpsInspectionStatus status, Integer httpStatus, String reason) {
        RuntimeControlProbeRespVO probe = probes.stream()
                .filter(item -> environment.equals(item.getEnvironment()) && component.equals(item.getComponent()))
                .findFirst()
                .orElseThrow();
        assertEquals(status, probe.getStatus());
        assertEquals(httpStatus, probe.getHttpStatusCode());
        if (reason != null) {
            assertTrue(probe.getError().contains(reason));
        }
    }

    private RuntimeControlProperties.Environment environment() {
        RuntimeControlProperties.Environment environment = new RuntimeControlProperties.Environment();
        environment.setLabel("Local");
        environment.setLocal(true);
        Map<String, RuntimeControlProperties.Target> targets = new LinkedHashMap<>();
        targets.put("intruoyi-backend", target("IntRuoyi 后端", "http://backend.test/health", "backend"));
        targets.put("intruoyi-frontend", target("IntRuoyi 前端", "http://frontend.test/", "frontend"));
        targets.put("website-frontend", target("Website 前端", "http://website.test/", "website"));
        environment.setTargets(targets);
        return environment;
    }

    private RuntimeControlProperties.Target target(String label, String url, String actionComponent) {
        RuntimeControlProperties.Target target = new RuntimeControlProperties.Target();
        target.setLabel(label);
        target.setUrl(url);
        target.setActionComponent(actionComponent);
        target.setActionEnabled(true);
        return target;
    }

    private RuntimeControlOwnerMatrixSaveReqVO owner() {
        RuntimeControlOwnerMatrixSaveReqVO reqVO = new RuntimeControlOwnerMatrixSaveReqVO();
        reqVO.setEnvironment("local");
        reqVO.setAction("probe-failed");
        reqVO.setRole("ops-owner");
        reqVO.setRequired(true);
        reqVO.setOwnerUserId(1001L);
        reqVO.setOwnerName("owner-1001");
        return reqVO;
    }

    private static final class FixtureProbeHttpClient implements RuntimeOpsProbeHttpClient {

        @Override
        public RuntimeOpsProbeHttpResult probe(String url, Duration timeout) {
            if (url.contains("backend")) {
                return new RuntimeOpsProbeHttpResult(200, 15L);
            }
            if (url.contains("frontend")) {
                throw new IllegalStateException("connect timed out");
            }
            return new RuntimeOpsProbeHttpResult(503, 20L);
        }
    }

    private static final class CapturingProbeHttpClient implements RuntimeOpsProbeHttpClient {

        private final List<String> urls = new java.util.ArrayList<>();

        @Override
        public RuntimeOpsProbeHttpResult probe(String url, Duration timeout) {
            urls.add(url);
            return new RuntimeOpsProbeHttpResult(200, 12L);
        }
    }

    private static final class RuntimeOpsCapturingSiteMessageSender implements RuntimeOpsSiteMessageSender {

        private final AtomicInteger callCount = new AtomicInteger();

        @Override
        public Long sendSingleMessageToAdmin(Long userId, String templateCode, Map<String, Object> templateParams) {
            callCount.incrementAndGet();
            assertEquals(1001L, userId);
            assertEquals("RUNTIME_OPS_ALERT", templateCode);
            assertEquals("探针失败", templateParams.get("title"));
            String content = String.valueOf(templateParams.get("content"));
            assertTrue(content.contains("website-frontend"));
            assertTrue(content.contains("目标=http://frontend.test/"));
            assertTrue(content.contains("目标=http://website.test/"));
            return 9001L;
        }
    }
}
