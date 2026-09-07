package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlBusinessHealthItemRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlBusinessHealthRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlInspectionCheckRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlInspectionRunRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlProbeLatestRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlProbeRespVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeOpsInspectionServiceImplTest {

    @TempDir
    private Path tempDir;

    private RuntimeOpsInspectionServiceImpl inspectionService;

    @BeforeEach
    void setUp() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        inspectionService = new RuntimeOpsInspectionServiceImpl(new RuntimeOpsInspectionRunStore(properties),
                this::businessHealthWithBlockedEvidence, new RuntimeOpsProbeService() {
            @Override
            public RuntimeControlProbeLatestRespVO runProbes() {
                return probeLatestWithPassEvidence();
            }

            @Override
            public RuntimeControlProbeLatestRespVO getLatestProbes() {
                return probeLatestWithPassEvidence();
            }
        }, () -> List.of(trustedTimeCheck("trusted-time-prod", "正式服"),
                trustedTimeCheck("trusted-time-audit", "审查服")));
    }

    @Test
    void runInspectionShouldPersistNoGoReportWhenCriticalEvidenceIsMissing() throws Exception {
        RuntimeControlInspectionRunRespVO run = inspectionService.runInspection();

        assertNotNull(run.getId());
        assertEquals(RuntimeOpsInspectionStatus.NO_GO, run.getStatus());
        assertTrue(run.getChecks().stream().anyMatch(check -> "business-health".equals(check.getCode())
                && RuntimeOpsInspectionStatus.NO_GO.equals(check.getStatus())));
        assertTrue(run.getChecks().stream().anyMatch(check -> "probe".equals(check.getCode())
                && RuntimeOpsInspectionStatus.PASS.equals(check.getStatus())));
        assertEquals(RuntimeOpsInspectionStatus.PASS,
                checkByCode(run, "trusted-time-prod").getStatus());
        assertEquals("172.30.30.57",
                checkByCode(run, "trusted-time-prod").getTrustedTime().getServerHost());
        assertEquals(RuntimeOpsInspectionStatus.PASS,
                checkByCode(run, "trusted-time-audit").getStatus());
        assertEquals("172.30.30.59",
                checkByCode(run, "trusted-time-audit").getTrustedTime().getServerHost());
        RuntimeControlInspectionCheckRespVO preRelease = checkByCode(run, "pre-release-check");
        assertEquals(RuntimeOpsInspectionStatus.NO_GO, preRelease.getStatus());
        assertTrue(preRelease.getEvidence().contains("POST /infra/runtime-control/inspection-runs"));
        RuntimeControlInspectionCheckRespVO postRelease = checkByCode(run, "post-release-observation");
        assertEquals(RuntimeOpsInspectionStatus.NO_GO, postRelease.getStatus());
        assertTrue(postRelease.getEvidence().contains("GET /infra/runtime-control/inspection-runs/{id}"));

        RuntimeControlInspectionRunRespVO stored = inspectionService.getInspectionRun(run.getId());
        assertEquals(run.getId(), stored.getId());
        assertEquals(RuntimeOpsInspectionStatus.NO_GO, stored.getStatus());
        assertEquals("ntp1.company.local",
                checkByCode(stored, "trusted-time-prod").getTrustedTime().getSelectedSource());
        assertTrue(Files.readString(tempDir.resolve("runtime-ops/inspection-runs.json"))
                .contains("\"trustedTime\""));
    }

    private RuntimeControlInspectionCheckRespVO trustedTimeCheck(String code, String nodeName) {
        RuntimeControlInspectionCheckRespVO check = new RuntimeControlInspectionCheckRespVO();
        check.setCode(code);
        check.setName(nodeName + "可信时间");
        check.setStatus(RuntimeOpsInspectionStatus.PASS);
        check.setRequired(true);
        check.setSampledAt(LocalDateTime.of(2026, 9, 7, 10, 0));
        var evidence = new cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlTrustedTimeRespVO();
        evidence.setTargetEnvironment("trusted-time-prod".equals(code) ? "prod" : "backup");
        evidence.setNodeName(nodeName);
        evidence.setServerHost("trusted-time-prod".equals(code) ? "172.30.30.57" : "172.30.30.59");
        evidence.setSelectedSource("ntp1.company.local");
        evidence.setLastOffsetMillis(0.12D);
        evidence.setLeapStatus("Normal");
        check.setTrustedTime(evidence);
        return check;
    }

    private RuntimeControlInspectionCheckRespVO checkByCode(RuntimeControlInspectionRunRespVO run, String code) {
        return run.getChecks().stream()
                .filter(check -> code.equals(check.getCode()))
                .findFirst()
                .orElseThrow();
    }

    private RuntimeControlBusinessHealthRespVO businessHealthWithBlockedEvidence() {
        RuntimeControlBusinessHealthItemRespVO login = new RuntimeControlBusinessHealthItemRespVO();
        login.setCode("login");
        login.setName("登录");
        login.setStatus(RuntimeOpsInspectionStatus.PASS);
        login.setEvidence("HTTP 200");
        login.setSampledAt(LocalDateTime.now());

        RuntimeControlBusinessHealthItemRespVO erp = new RuntimeControlBusinessHealthItemRespVO();
        erp.setCode("erp");
        erp.setName("ERP");
        erp.setStatus(RuntimeOpsInspectionStatus.BLOCKED);
        erp.setReason("ERP 采集配置缺失");
        erp.setSampledAt(LocalDateTime.now());

        RuntimeControlBusinessHealthRespVO response = new RuntimeControlBusinessHealthRespVO();
        response.setStatus(RuntimeOpsInspectionStatus.NO_GO);
        response.setSampledAt(LocalDateTime.now());
        response.setItems(List.of(login, erp));
        return response;
    }

    private RuntimeControlProbeLatestRespVO probeLatestWithPassEvidence() {
        RuntimeControlProbeRespVO backend = new RuntimeControlProbeRespVO();
        backend.setEnvironment("local");
        backend.setComponent("backend");
        backend.setUrl("http://127.0.0.1:48081/actuator/health");
        backend.setStatus(RuntimeOpsInspectionStatus.PASS);
        backend.setHttpStatusCode(200);
        backend.setDurationMillis(12L);
        backend.setSampledAt(LocalDateTime.now());

        RuntimeControlProbeLatestRespVO response = new RuntimeControlProbeLatestRespVO();
        response.setStatus(RuntimeOpsInspectionStatus.PASS);
        response.setSampledAt(LocalDateTime.now());
        response.setProbes(List.of(backend));
        return response;
    }
}
