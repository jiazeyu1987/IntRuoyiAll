package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlInspectionCheckRespVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

class RuntimeOpsTrustedTimeCollectorImplTest extends BaseMockitoUnitTest {

    @TempDir
    private Path tempDir;

    @Mock
    private RuntimeControlCommandExecutor commandExecutor;

    private RuntimeTrustedTimeCollector collector;

    @BeforeEach
    void setUp() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        properties.setRepoRoot(tempDir.toString());
        collector = new RuntimeTrustedTimeCollectorImpl(properties, commandExecutor, 1000D);
    }

    @Test
    void collectShouldUseFixedProdAndAuditHostsAndPersistBothResults() {
        when(commandExecutor.executeForOutput(argThat(command -> command != null
                        && "prod".equals(command.getEnvironment())
                        && command.getArguments().containsAll(List.of("-ServerHost", "172.30.30.57"))),
                any(Duration.class))).thenReturn(normalJson("prod", "172.30.30.57"));
        when(commandExecutor.executeForOutput(argThat(command -> command != null
                        && "backup".equals(command.getEnvironment())
                        && command.getArguments().containsAll(List.of("-ServerHost", "172.30.30.59"))),
                any(Duration.class))).thenReturn(normalJson("backup", "172.30.30.59"));

        List<RuntimeControlInspectionCheckRespVO> checks = collector.collect();

        assertEquals(2, checks.size());
        assertEquals("正式服可信时间", checks.get(0).getName());
        assertEquals("审查服可信时间", checks.get(1).getName());
        assertTrue(checks.stream().allMatch(item -> RuntimeOpsInspectionStatus.PASS == item.getStatus()));
    }

    @Test
    void collectShouldRecordBlockedCheckWhenRemoteCommandFails() {
        when(commandExecutor.executeForOutput(argThat(command -> command != null
                        && "prod".equals(command.getEnvironment())),
                any(Duration.class))).thenThrow(new ServiceException(1, "SSH connection timed out"));
        when(commandExecutor.executeForOutput(argThat(command -> command != null
                        && "backup".equals(command.getEnvironment())),
                any(Duration.class))).thenReturn(normalJson("backup", "172.30.30.59"));

        List<RuntimeControlInspectionCheckRespVO> checks = collector.collect();

        assertEquals(RuntimeOpsInspectionStatus.BLOCKED, checks.get(0).getStatus());
        assertTrue(checks.get(0).getReason().contains("SSH connection timed out"));
        assertEquals("172.30.30.57", checks.get(0).getTrustedTime().getServerHost());
        assertEquals(RuntimeOpsInspectionStatus.PASS, checks.get(1).getStatus());
    }

    @Test
    void collectShouldRecordBlockedCheckWhenChronyCommandFails() {
        when(commandExecutor.executeForOutput(argThat(command -> command != null
                        && "prod".equals(command.getEnvironment())),
                any(Duration.class))).thenThrow(new ServiceException(1, "Missing required command: chronyc"));
        when(commandExecutor.executeForOutput(argThat(command -> command != null
                        && "backup".equals(command.getEnvironment())),
                any(Duration.class))).thenReturn(normalJson("backup", "172.30.30.59"));

        List<RuntimeControlInspectionCheckRespVO> checks = collector.collect();

        assertEquals(RuntimeOpsInspectionStatus.BLOCKED, checks.get(0).getStatus());
        assertTrue(checks.get(0).getReason().contains("chronyc"));
        assertEquals(RuntimeOpsInspectionStatus.PASS, checks.get(1).getStatus());
    }

    @Test
    void collectShouldContinueWithoutOffsetThresholdAndPreserveMeasuredOffsets() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        RuntimeTrustedTimeCollector collectorWithoutThreshold =
                new RuntimeTrustedTimeCollectorImpl(properties, commandExecutor, null);
        when(commandExecutor.executeForOutput(argThat(command -> command != null
                        && "prod".equals(command.getEnvironment())), any(Duration.class)))
                .thenReturn(normalJson("prod", "172.30.30.57"));
        when(commandExecutor.executeForOutput(argThat(command -> command != null
                        && "backup".equals(command.getEnvironment())), any(Duration.class)))
                .thenReturn(normalJson("backup", "172.30.30.59"));

        List<RuntimeControlInspectionCheckRespVO> checks = collectorWithoutThreshold.collect();

        assertTrue(checks.stream().allMatch(item -> RuntimeOpsInspectionStatus.PASS == item.getStatus()));
        assertTrue(checks.stream().allMatch(item -> item.getTrustedTime().getLastOffsetMillis() != null));
        assertTrue(checks.stream().allMatch(item -> item.getTrustedTime().getRmsOffsetMillis() != null));
        assertTrue(checks.stream().allMatch(item -> item.getTrustedTime().getLastOffsetMillis() == 0.12D));
        assertTrue(checks.stream().allMatch(item -> item.getTrustedTime().getRmsOffsetMillis() == 0.2D));
        assertTrue(checks.stream().allMatch(item -> item.getTrustedTime().getMaxOffsetMillis() == null));
    }

    @Test
    void configuredThresholdShouldTreatBlankAsDisabledAndRejectInvalidNonBlankValues() {
        assertNull(RuntimeTrustedTimeCollectorImpl.configuredMaxOffsetMillis(null));
        assertNull(RuntimeTrustedTimeCollectorImpl.configuredMaxOffsetMillis("  "));
        assertEquals(100D, RuntimeTrustedTimeCollectorImpl.configuredMaxOffsetMillis("100"));
        assertThrows(IllegalStateException.class,
                () -> RuntimeTrustedTimeCollectorImpl.configuredMaxOffsetMillis("invalid"));
        assertThrows(IllegalStateException.class,
                () -> RuntimeTrustedTimeCollectorImpl.configuredMaxOffsetMillis("0"));
        assertThrows(IllegalStateException.class,
                () -> RuntimeTrustedTimeCollectorImpl.configuredMaxOffsetMillis("-1"));
    }

    private String normalJson(String environment, String host) {
        return """
                {
                  "targetEnvironment": "%s",
                  "serverHost": "%s",
                  "chronycTracking": "Stratum : 2\\nLast offset : +0.000120 seconds\\nRMS offset : 0.000200 seconds\\nLeap status : Normal",
                  "chronycSources": "^* ntp1.company.local 2 6 377 34 +120us[+110us] +/- 2ms",
                  "timedatectlStatus": "System clock synchronized: yes\\nNTP service: active",
                  "serverTimeUtc": "2026-09-07T10:00:00Z",
                  "databaseTimeUtc": "2026-09-07T10:00:00.123456Z",
                  "checkedAtUtc": "2026-09-07T10:00:01Z"
                }
                """.formatted(environment, host);
    }
}
