package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.hutool.core.util.EscapeUtil;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlInspectionCheckRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlInspectionRunRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlTrustedTimeRespVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_INVALID;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_OPERATION_STORE_FAILED;

class RuntimeTimeEvidenceExporter {

    static final String SUMMARY_FILE = "审查摘要.html";
    static final String RAW_FILE = "原始证据.json";
    static final String CHECKSUM_FILE = "SHA256SUMS.txt";

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    byte[] export(RuntimeControlInspectionRunRespVO run) {
        validateTrustedTimeEvidence(run);
        byte[] summary = summary(run).getBytes(StandardCharsets.UTF_8);
        byte[] raw = raw(run);
        byte[] checksums = (sha256(summary) + "  " + SUMMARY_FILE + "\n"
                + sha256(raw) + "  " + RAW_FILE + "\n").getBytes(StandardCharsets.UTF_8);
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            add(zip, SUMMARY_FILE, summary);
            add(zip, RAW_FILE, raw);
            add(zip, CHECKSUM_FILE, checksums);
            zip.finish();
            return bytes.toByteArray();
        } catch (IOException ex) {
            throw exception(RUNTIME_CONTROL_OPERATION_STORE_FAILED,
                    "时间戳证据 ZIP 生成失败：" + ex.getMessage());
        }
    }

    private void validateTrustedTimeEvidence(RuntimeControlInspectionRunRespVO run) {
        List<RuntimeControlInspectionCheckRespVO> checks = run.getChecks();
        if (checks == null) {
            throw incompleteEvidence("巡检检查项缺失");
        }
        validateTrustedTimeCheck(checks, "trusted-time-prod", "prod",
                RuntimeControlProperties.PROD_SERVER_HOST);
        validateTrustedTimeCheck(checks, "trusted-time-audit", "backup",
                RuntimeControlProperties.BACKUP_SERVER_HOST);
    }

    private void validateTrustedTimeCheck(List<RuntimeControlInspectionCheckRespVO> checks, String code,
                                          String environment, String host) {
        List<RuntimeControlInspectionCheckRespVO> matching = checks.stream()
                .filter(check -> check != null && code.equals(check.getCode()))
                .toList();
        if (matching.size() != 1) {
            throw incompleteEvidence(code + " 数量必须为 1，实际为 " + matching.size());
        }
        RuntimeControlTrustedTimeRespVO trustedTime = matching.get(0).getTrustedTime();
        if (trustedTime == null) {
            throw incompleteEvidence(code + " 缺少 trustedTime");
        }
        if (!environment.equals(trustedTime.getTargetEnvironment())
                || !host.equals(trustedTime.getServerHost())) {
            throw incompleteEvidence(code + " 的 targetEnvironment/serverHost 与固定目标不匹配");
        }
    }

    private RuntimeException incompleteEvidence(String reason) {
        return exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "可信时间证据不完整：" + reason);
    }

    private byte[] raw(RuntimeControlInspectionRunRespVO run) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(run);
        } catch (JsonProcessingException ex) {
            throw exception(RUNTIME_CONTROL_OPERATION_STORE_FAILED,
                    "时间戳原始证据序列化失败：" + ex.getOriginalMessage());
        }
    }

    private String summary(RuntimeControlInspectionRunRespVO run) {
        StringBuilder rows = new StringBuilder();
        for (RuntimeControlInspectionCheckRespVO check : safeChecks(run.getChecks())) {
            rows.append("<tr><td>").append(html(check.getCode())).append("</td><td>")
                    .append(html(check.getName())).append("</td><td>")
                    .append(html(String.valueOf(check.getStatus()))).append("</td><td>")
                    .append(html(check.getEvidence())).append("</td><td>")
                    .append(html(check.getReason())).append("</td></tr>");
        }
        return """
                <!doctype html><html lang="zh-CN"><head><meta charset="UTF-8">
                <title>可信时间审查摘要</title>
                <style>body{font-family:Arial,"Microsoft YaHei",sans-serif;margin:32px;color:#202124}table{border-collapse:collapse;width:100%%}th,td{border:1px solid #999;padding:8px;text-align:left;vertical-align:top}th{background:#f2f3f5}</style>
                </head><body><h1>可信时间审查摘要</h1>
                <p>巡检编号：%s</p><p>结论：%s</p><p>开始时间：%s</p><p>完成时间：%s</p>
                <table><thead><tr><th>编码</th><th>检查项</th><th>状态</th><th>证据</th><th>原因</th></tr></thead><tbody>%s</tbody></table>
                </body></html>
                """.formatted(run.getId(), run.getStatus(), run.getStartedAt(), run.getCompletedAt(), rows);
    }

    private List<RuntimeControlInspectionCheckRespVO> safeChecks(List<RuntimeControlInspectionCheckRespVO> checks) {
        return checks == null ? List.of() : checks;
    }

    private String html(Object value) {
        return value == null ? "" : EscapeUtil.escapeHtml4(String.valueOf(value));
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("JDK 缺少 SHA-256", ex);
        }
    }

    private void add(ZipOutputStream zip, String name, byte[] content) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        entry.setTime(0L);
        zip.putNextEntry(entry);
        zip.write(content);
        zip.closeEntry();
    }
}
