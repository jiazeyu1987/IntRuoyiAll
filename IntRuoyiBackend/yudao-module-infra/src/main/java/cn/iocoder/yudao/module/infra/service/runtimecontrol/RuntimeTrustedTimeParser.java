package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlInspectionCheckRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlTrustedTimeRespVO;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class RuntimeTrustedTimeParser {

    private static final Pattern SELECTED_SOURCE = Pattern.compile("(?m)^\\s*[\\^=#]\\*\\s+(\\S+)");
    private static final Pattern STRATUM = Pattern.compile("(?im)^\\s*Stratum\\s*:\\s*(\\d+)\\s*$");
    private static final Pattern LAST_OFFSET = Pattern.compile(
            "(?im)^\\s*Last offset\\s*:\\s*([+-]?\\d+(?:\\.\\d+)?)\\s+seconds?\\s*$");
    private static final Pattern RMS_OFFSET = Pattern.compile(
            "(?im)^\\s*RMS offset\\s*:\\s*([+-]?\\d+(?:\\.\\d+)?)\\s+seconds?\\s*$");
    private static final Pattern LEAP_STATUS = Pattern.compile("(?im)^\\s*Leap status\\s*:\\s*(.+?)\\s*$");
    private static final Pattern CLOCK_SYNCHRONIZED = Pattern.compile(
            "(?im)^\\s*System clock synchronized\\s*:\\s*(\\S+)\\s*$");
    private static final Pattern NTP_SERVICE = Pattern.compile("(?im)^\\s*NTP service\\s*:\\s*(\\S+)\\s*$");
    private static final Pattern LEGACY_NTP_SYNCHRONIZED = Pattern.compile(
            "(?im)^\\s*NTP synchronized\\s*:\\s*(\\S+)\\s*$");
    private static final Pattern LEGACY_NTP_ENABLED = Pattern.compile(
            "(?im)^\\s*NTP enabled\\s*:\\s*(\\S+)\\s*$");

    private final Double maxOffsetMillis;

    RuntimeTrustedTimeParser(Double maxOffsetMillis) {
        if (maxOffsetMillis != null && (!Double.isFinite(maxOffsetMillis) || maxOffsetMillis <= 0)) {
            throw new IllegalArgumentException("maxOffsetMillis must be positive");
        }
        this.maxOffsetMillis = maxOffsetMillis;
    }

    RuntimeControlInspectionCheckRespVO parse(String targetEnvironment, String nodeName, String serverHost,
                                              RuntimeTrustedTimeCommandOutput output) {
        RuntimeControlTrustedTimeRespVO evidence = evidence(targetEnvironment, nodeName, serverHost, output);
        List<String> failures = validate(targetEnvironment, serverHost, output, evidence);
        RuntimeControlInspectionCheckRespVO check = new RuntimeControlInspectionCheckRespVO();
        check.setCode("prod".equals(targetEnvironment) ? "trusted-time-prod" : "trusted-time-audit");
        check.setName(nodeName + "可信时间");
        check.setRequired(true);
        check.setTrustedTime(evidence);
        check.setSampledAt(LocalDateTime.now());
        if (failures.isEmpty()) {
            check.setStatus(RuntimeOpsInspectionStatus.PASS);
            check.setEvidence(summary(evidence));
        } else {
            check.setStatus(RuntimeOpsInspectionStatus.BLOCKED);
            check.setReason(String.join("；", failures));
            check.setEvidence(summary(evidence));
        }
        return check;
    }

    RuntimeControlInspectionCheckRespVO blocked(String targetEnvironment, String nodeName, String serverHost,
                                                String reason) {
        RuntimeTrustedTimeCommandOutput output = new RuntimeTrustedTimeCommandOutput();
        output.setTargetEnvironment(targetEnvironment);
        output.setServerHost(serverHost);
        RuntimeControlInspectionCheckRespVO check = parse(targetEnvironment, nodeName, serverHost, output);
        check.setReason(reason);
        return check;
    }

    private RuntimeControlTrustedTimeRespVO evidence(String targetEnvironment, String nodeName, String serverHost,
                                                     RuntimeTrustedTimeCommandOutput output) {
        RuntimeControlTrustedTimeRespVO evidence = new RuntimeControlTrustedTimeRespVO();
        evidence.setTargetEnvironment(targetEnvironment);
        evidence.setNodeName(nodeName);
        evidence.setServerHost(serverHost);
        evidence.setMaxOffsetMillis(maxOffsetMillis);
        if (output == null) {
            return evidence;
        }
        evidence.setSelectedSource(find(SELECTED_SOURCE, output.getChronycSources()));
        evidence.setStratum(parseInteger(find(STRATUM, output.getChronycTracking())));
        evidence.setLastOffsetMillis(secondsToMillis(find(LAST_OFFSET, output.getChronycTracking())));
        evidence.setRmsOffsetMillis(secondsToMillis(find(RMS_OFFSET, output.getChronycTracking())));
        evidence.setLeapStatus(find(LEAP_STATUS, output.getChronycTracking()));
        String clockSynchronized = find(CLOCK_SYNCHRONIZED, output.getTimedatectlStatus());
        String legacyNtpSynchronized = find(LEGACY_NTP_SYNCHRONIZED, output.getTimedatectlStatus());
        evidence.setSystemClockSynchronized(allPresentSignalsAffirmative(
                clockSynchronized, "yes", legacyNtpSynchronized, "yes"));
        evidence.setNtpServiceState(normalizeNtpServiceState(
                find(NTP_SERVICE, output.getTimedatectlStatus()),
                find(LEGACY_NTP_ENABLED, output.getTimedatectlStatus())));
        evidence.setServerTimeUtc(StrUtil.trim(output.getServerTimeUtc()));
        evidence.setDatabaseTimeUtc(StrUtil.trim(output.getDatabaseTimeUtc()));
        evidence.setCheckedAtUtc(StrUtil.trim(output.getCheckedAtUtc()));
        evidence.setChronycTracking(output.getChronycTracking());
        evidence.setChronycSources(output.getChronycSources());
        evidence.setTimedatectlStatus(output.getTimedatectlStatus());
        return evidence;
    }

    private List<String> validate(String targetEnvironment, String serverHost, RuntimeTrustedTimeCommandOutput output,
                                  RuntimeControlTrustedTimeRespVO evidence) {
        List<String> failures = new ArrayList<>();
        if (output == null) {
            failures.add("时间检查命令未返回证据");
            return failures;
        }
        if (!targetEnvironment.equals(output.getTargetEnvironment()) || !serverHost.equals(output.getServerHost())) {
            failures.add("命令结果无法证明目标环境和固定服务器一致");
        }
        if (StrUtil.isBlank(output.getChronycTracking())) {
            failures.add("缺少 chronyc tracking 证据");
        }
        if (StrUtil.isBlank(output.getChronycSources())) {
            failures.add("缺少 chronyc sources 证据");
        } else if (StrUtil.isBlank(evidence.getSelectedSource())) {
            failures.add("chrony 没有选中时间源");
        }
        if (!"Normal".equalsIgnoreCase(evidence.getLeapStatus())) {
            failures.add("Leap status 不是 Normal");
        }
        if (!Boolean.TRUE.equals(evidence.getSystemClockSynchronized())) {
            failures.add("系统时钟未同步");
        }
        if (!"active".equalsIgnoreCase(evidence.getNtpServiceState())) {
            failures.add("NTP service 不是 active");
        }
        if (evidence.getStratum() == null) {
            failures.add("缺少 Stratum");
        }
        if (evidence.getLastOffsetMillis() == null) {
            failures.add("缺少 Last offset");
        } else if (maxOffsetMillis != null && Math.abs(evidence.getLastOffsetMillis()) > maxOffsetMillis) {
            failures.add("时间偏差超过批准阈值 " + formatNumber(maxOffsetMillis) + " ms");
        }
        if (evidence.getRmsOffsetMillis() == null) {
            failures.add("缺少 RMS offset");
        } else if (maxOffsetMillis != null && Math.abs(evidence.getRmsOffsetMillis()) > maxOffsetMillis) {
            failures.add("RMS 时间偏差超过批准阈值 " + formatNumber(maxOffsetMillis) + " ms");
        }
        if (StrUtil.isBlank(evidence.getServerTimeUtc())) {
            failures.add("缺少服务器 UTC 时间");
        } else if (!isUtcInstant(evidence.getServerTimeUtc())) {
            failures.add("服务器 UTC 时间格式无效");
        }
        if (StrUtil.isBlank(evidence.getDatabaseTimeUtc())) {
            failures.add("缺少数据库 UTC 时间");
        } else if (!isUtcInstant(evidence.getDatabaseTimeUtc())) {
            failures.add("数据库 UTC 时间格式无效");
        }
        if (StrUtil.isBlank(evidence.getCheckedAtUtc())) {
            failures.add("缺少检查时间");
        } else if (!isUtcInstant(evidence.getCheckedAtUtc())) {
            failures.add("检查时间格式无效");
        }
        return failures;
    }

    private String summary(RuntimeControlTrustedTimeRespVO evidence) {
        return "host=" + evidence.getServerHost()
                + "; source=" + StrUtil.blankToDefault(evidence.getSelectedSource(), "MISSING")
                + "; offsetMillis=" + evidence.getLastOffsetMillis()
                + "; leap=" + StrUtil.blankToDefault(evidence.getLeapStatus(), "MISSING")
                + "; checkedAtUtc=" + StrUtil.blankToDefault(evidence.getCheckedAtUtc(), "MISSING");
    }

    private String find(Pattern pattern, String source) {
        if (StrUtil.isBlank(source)) {
            return null;
        }
        Matcher matcher = pattern.matcher(source);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private Integer parseInteger(String value) {
        return value == null ? null : Integer.valueOf(value);
    }

    private Double secondsToMillis(String value) {
        return value == null ? null : new BigDecimal(value).movePointRight(3).doubleValue();
    }

    private String formatNumber(double value) {
        if (value == Math.rint(value)) {
            return Long.toString((long) value);
        }
        return String.format(Locale.ROOT, "%s", value);
    }

    private boolean isUtcInstant(String value) {
        try {
            Instant.parse(value);
            return value.endsWith("Z");
        } catch (DateTimeParseException ex) {
            return false;
        }
    }

    private boolean allPresentSignalsAffirmative(String first, String firstAffirmative,
                                                 String second, String secondAffirmative) {
        if (first == null && second == null) {
            return false;
        }
        return (first == null || firstAffirmative.equalsIgnoreCase(first))
                && (second == null || secondAffirmative.equalsIgnoreCase(second));
    }

    private String normalizeNtpServiceState(String serviceState, String legacyEnabled) {
        if (serviceState == null && legacyEnabled == null) {
            return null;
        }
        if (serviceState != null && !"active".equalsIgnoreCase(serviceState)) {
            return serviceState;
        }
        if (legacyEnabled != null && !"yes".equalsIgnoreCase(legacyEnabled)) {
            return "inactive";
        }
        return "active";
    }
}
