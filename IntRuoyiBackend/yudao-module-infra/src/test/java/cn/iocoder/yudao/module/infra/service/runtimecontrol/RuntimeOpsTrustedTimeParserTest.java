package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlInspectionCheckRespVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeOpsTrustedTimeParserTest {

    private final RuntimeTrustedTimeParser parser = new RuntimeTrustedTimeParser(1000D);

    @Test
    void parseShouldPassAndPreserveChronyEvidenceWhenSynchronized() {
        RuntimeControlInspectionCheckRespVO check = parser.parse("prod", "正式服", "172.30.30.57",
                normalOutput("+0.000120 seconds"));

        assertEquals(RuntimeOpsInspectionStatus.PASS, check.getStatus());
        assertEquals("ntp1.company.local", check.getTrustedTime().getSelectedSource());
        assertEquals(0.12D, check.getTrustedTime().getLastOffsetMillis());
        assertEquals("Normal", check.getTrustedTime().getLeapStatus());
        assertEquals("2026-09-07T10:00:00Z", check.getTrustedTime().getServerTimeUtc());
        assertEquals("2026-09-07T10:00:00.123456Z", check.getTrustedTime().getDatabaseTimeUtc());
    }

    @Test
    void parseShouldBlockWhenChronyHasNoSelectedSource() {
        RuntimeTrustedTimeCommandOutput output = normalOutput("+0.000120 seconds");
        output.setChronycSources("^? ntp1.company.local 0 6 0 - +0ns[+0ns] +/- 0ns");

        RuntimeControlInspectionCheckRespVO check = parser.parse("prod", "正式服", "172.30.30.57", output);

        assertEquals(RuntimeOpsInspectionStatus.BLOCKED, check.getStatus());
        assertTrue(check.getReason().contains("选中时间源"));
    }

    @Test
    void parseShouldBlockWhenOffsetExceedsApprovedThreshold() {
        RuntimeControlInspectionCheckRespVO check = parser.parse("prod", "正式服", "172.30.30.57",
                normalOutput("-1.500000 seconds"));

        assertEquals(RuntimeOpsInspectionStatus.BLOCKED, check.getStatus());
        assertEquals(-1500D, check.getTrustedTime().getLastOffsetMillis());
        assertTrue(check.getReason().contains("1000"));
    }

    @Test
    void parseShouldBlockWhenChronyCommandEvidenceIsMissing() {
        RuntimeTrustedTimeCommandOutput output = normalOutput("+0.000120 seconds");
        output.setChronycTracking("");

        RuntimeControlInspectionCheckRespVO check = parser.parse("prod", "正式服", "172.30.30.57", output);

        assertEquals(RuntimeOpsInspectionStatus.BLOCKED, check.getStatus());
        assertTrue(check.getReason().contains("chronyc tracking"));
    }

    @Test
    void parseShouldBlockWhenLeapStatusIsNotNormal() {
        RuntimeTrustedTimeCommandOutput output = normalOutput("+0.000120 seconds");
        output.setChronycTracking(output.getChronycTracking().replace("Leap status     : Normal",
                "Leap status     : Not synchronised"));

        RuntimeControlInspectionCheckRespVO check = parser.parse("prod", "正式服", "172.30.30.57", output);

        assertEquals(RuntimeOpsInspectionStatus.BLOCKED, check.getStatus());
        assertTrue(check.getReason().contains("Leap status"));
    }

    @Test
    void parseShouldAcceptLegacyTimedatectlSynchronizedAndEnabledEvidence() {
        RuntimeTrustedTimeCommandOutput output = normalOutput("+0.000120 seconds");
        output.setTimedatectlStatus("""
                NTP enabled: yes
                NTP synchronized: yes
                RTC in local TZ: no
                """);

        RuntimeControlInspectionCheckRespVO check = parser.parse(
                "backup", "审查服", "172.30.30.59", outputForTarget(output, "backup", "172.30.30.59"));

        assertEquals(RuntimeOpsInspectionStatus.PASS, check.getStatus());
        assertEquals(Boolean.TRUE, check.getTrustedTime().getSystemClockSynchronized());
        assertEquals("active", check.getTrustedTime().getNtpServiceState());
    }

    @ParameterizedTest(name = "旧版 timedatectl 任一 no 必须阻断：{0}")
    @MethodSource("legacyTimedatectlFailures")
    void parseShouldBlockWhenLegacyTimedatectlHasAnyNo(
            String scenario, String enabled, String synchronizedValue, String expectedReason) {
        RuntimeTrustedTimeCommandOutput output = normalOutput("+0.000120 seconds");
        output.setTimedatectlStatus("NTP enabled: " + enabled
                + "\nNTP synchronized: " + synchronizedValue);

        RuntimeControlInspectionCheckRespVO check = parser.parse(
                "backup", "审查服", "172.30.30.59", outputForTarget(output, "backup", "172.30.30.59"));

        assertEquals(RuntimeOpsInspectionStatus.BLOCKED, check.getStatus(), scenario);
        assertTrue(check.getReason().contains(expectedReason), check.getReason());
    }

    private static Stream<Arguments> legacyTimedatectlFailures() {
        return Stream.of(
                Arguments.of("NTP enabled=no", "no", "yes", "NTP service"),
                Arguments.of("NTP synchronized=no", "yes", "no", "系统时钟未同步")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidRequiredEvidence")
    void parseShouldBlockWhenRequiredEvidenceIsMissingInvalidOrOutOfRange(
            String scenario, String field, String value, String expectedReason) {
        RuntimeTrustedTimeCommandOutput output = normalOutput("+0.000120 seconds");
        mutate(output, field, value);

        RuntimeControlInspectionCheckRespVO check = parser.parse("prod", "正式服", "172.30.30.57", output);

        assertEquals(RuntimeOpsInspectionStatus.BLOCKED, check.getStatus(), scenario);
        assertTrue(check.getReason().contains(expectedReason), check.getReason());
    }

    private static Stream<Arguments> invalidRequiredEvidence() {
        return Stream.of(
                Arguments.of("RMS offset 缺失", "rms", "", "RMS offset"),
                Arguments.of("RMS offset 超限", "rms", "2.000000 seconds", "RMS 时间偏差超过"),
                Arguments.of("系统时钟未同步", "clock", "no", "系统时钟未同步"),
                Arguments.of("NTP 服务未运行", "ntp", "inactive", "NTP service"),
                Arguments.of("Stratum 缺失", "stratum", "", "Stratum"),
                Arguments.of("Stratum 无效", "stratum", "invalid", "Stratum"),
                Arguments.of("服务器 UTC 缺失", "serverUtc", "", "服务器 UTC 时间"),
                Arguments.of("服务器 UTC 无效", "serverUtc", "not-a-time", "服务器 UTC 时间格式无效"),
                Arguments.of("数据库 UTC 缺失", "databaseUtc", "", "数据库 UTC 时间"),
                Arguments.of("数据库 UTC 无效", "databaseUtc", "2026-09-07 10:00:00", "数据库 UTC 时间格式无效"),
                Arguments.of("检查 UTC 缺失", "checkedUtc", "", "检查时间"),
                Arguments.of("检查 UTC 无效", "checkedUtc", "2026-09-07T10:00:01+08:00", "检查时间格式无效")
        );
    }

    private void mutate(RuntimeTrustedTimeCommandOutput output, String field, String value) {
        switch (field) {
            case "rms" -> output.setChronycTracking(output.getChronycTracking().replace(
                    "RMS offset      : 0.000200 seconds", value.isEmpty() ? "" : "RMS offset      : " + value));
            case "clock" -> output.setTimedatectlStatus(output.getTimedatectlStatus().replace(
                    "System clock synchronized: yes", "System clock synchronized: " + value));
            case "ntp" -> output.setTimedatectlStatus(output.getTimedatectlStatus().replace(
                    "NTP service: active", "NTP service: " + value));
            case "stratum" -> output.setChronycTracking(output.getChronycTracking().replace(
                    "Stratum         : 2", value.isEmpty() ? "" : "Stratum         : " + value));
            case "serverUtc" -> output.setServerTimeUtc(value);
            case "databaseUtc" -> output.setDatabaseTimeUtc(value);
            case "checkedUtc" -> output.setCheckedAtUtc(value);
            default -> throw new IllegalArgumentException("unknown test field: " + field);
        }
    }

    private RuntimeTrustedTimeCommandOutput normalOutput(String offset) {
        RuntimeTrustedTimeCommandOutput output = new RuntimeTrustedTimeCommandOutput();
        output.setTargetEnvironment("prod");
        output.setServerHost("172.30.30.57");
        output.setChronycTracking("""
                Reference ID    : 0A000001 (ntp1.company.local)
                Stratum         : 2
                Last offset     : %s
                RMS offset      : 0.000200 seconds
                Leap status     : Normal
                """.formatted(offset));
        output.setChronycSources("^* ntp1.company.local 2 6 377 34 +120us[+110us] +/- 2ms");
        output.setTimedatectlStatus("""
                System clock synchronized: yes
                NTP service: active
                """);
        output.setServerTimeUtc("2026-09-07T10:00:00Z");
        output.setDatabaseTimeUtc("2026-09-07T10:00:00.123456Z");
        output.setCheckedAtUtc("2026-09-07T10:00:01Z");
        return output;
    }

    private RuntimeTrustedTimeCommandOutput outputForTarget(RuntimeTrustedTimeCommandOutput output,
                                                            String environment, String host) {
        output.setTargetEnvironment(environment);
        output.setServerHost(host);
        return output;
    }
}
