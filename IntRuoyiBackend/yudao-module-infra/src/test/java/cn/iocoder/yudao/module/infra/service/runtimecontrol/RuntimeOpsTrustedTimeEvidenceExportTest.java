package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlBusinessHealthRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlInspectionCheckRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlInspectionRunRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlProbeLatestRespVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_INVALID;

class RuntimeOpsTrustedTimeEvidenceExportTest {

    @TempDir
    private Path tempDir;

    @Test
    void exportShouldReadSavedInspectionOnceAndCreateFixedThreeFileZipWithValidHashes() throws Exception {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        AtomicInteger collections = new AtomicInteger();
        RuntimeOpsInspectionService service = new RuntimeOpsInspectionServiceImpl(
                new RuntimeOpsInspectionRunStore(properties), this::passBusinessHealth, new RuntimeOpsProbeService() {
                    @Override
                    public RuntimeControlProbeLatestRespVO runProbes() {
                        return passProbes();
                    }

                    @Override
                    public RuntimeControlProbeLatestRespVO getLatestProbes() {
                        return passProbes();
                    }
                },
                () -> {
                    collections.incrementAndGet();
                    return List.of(blockedTrustedTime());
                });
        RuntimeControlInspectionRunRespVO run = service.runInspection();

        assertEquals(RuntimeOpsInspectionStatus.NO_GO, run.getStatus());
        assertEquals(RuntimeOpsInspectionStatus.NO_GO, run.getChecks().stream()
                .filter(check -> "pre-release-check".equals(check.getCode()))
                .findFirst().orElseThrow().getStatus());

        byte[] zip = service.exportTimeEvidence(run.getId());
        Map<String, byte[]> entries = unzip(zip);

        assertEquals(1, collections.get(), "导出不得重新执行时间巡检");
        assertEquals(List.of("审查摘要.html", "原始证据.json", "SHA256SUMS.txt"),
                entries.keySet().stream().toList());
        String html = text(entries.get("审查摘要.html"));
        assertTrue(html.contains("巡检编号：" + run.getId()));
        assertTrue(html.contains("结论：NO_GO"));
        assertFalse(html.contains("结论：通过"));
        assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt;&amp;&quot;"));
        assertFalse(html.contains("<script>alert(1)</script>"));
        String json = text(entries.get("原始证据.json"));
        assertTrue(json.contains("\"id\" : " + run.getId()));
        assertTrue(json.contains("trusted-time-prod"));
        assertTrue(json.matches("(?s).*\\\"completedAt\\\" : \\\"\\d{4}-.*"));
        String sums = text(entries.get("SHA256SUMS.txt"));
        assertTrue(sums.contains(hex(entries.get("审查摘要.html")) + "  审查摘要.html"));
        assertTrue(sums.contains(hex(entries.get("原始证据.json")) + "  原始证据.json"));
        assertArrayEquals(zip, service.exportTimeEvidence(run.getId()),
                "同一已保存巡检必须生成同一冻结证据包");
        assertEquals(1, collections.get(), "重复导出也不得重新执行时间巡检");
    }

    @Test
    void exportShouldFailWithCanonicalErrorWhenInspectionDoesNotExist() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        RuntimeOpsInspectionService service = new RuntimeOpsInspectionServiceImpl(
                new RuntimeOpsInspectionRunStore(properties), this::passBusinessHealth, new RuntimeOpsProbeService() {
                    @Override
                    public RuntimeControlProbeLatestRespVO runProbes() {
                        return passProbes();
                    }

                    @Override
                    public RuntimeControlProbeLatestRespVO getLatestProbes() {
                        return passProbes();
                    }
                }, List::of);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.exportTimeEvidence(999L));

        assertEquals(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("巡检报告不存在：999"));
    }

    private RuntimeControlBusinessHealthRespVO passBusinessHealth() {
        RuntimeControlBusinessHealthRespVO result = new RuntimeControlBusinessHealthRespVO();
        result.setStatus(RuntimeOpsInspectionStatus.PASS);
        result.setSampledAt(LocalDateTime.of(2026, 9, 7, 10, 0));
        result.setItems(List.of());
        return result;
    }

    private RuntimeControlProbeLatestRespVO passProbes() {
        RuntimeControlProbeLatestRespVO result = new RuntimeControlProbeLatestRespVO();
        result.setStatus(RuntimeOpsInspectionStatus.PASS);
        result.setSampledAt(LocalDateTime.of(2026, 9, 7, 10, 0));
        result.setProbes(List.of());
        return result;
    }

    private RuntimeControlInspectionCheckRespVO blockedTrustedTime() {
        RuntimeControlInspectionCheckRespVO check = new RuntimeControlInspectionCheckRespVO();
        check.setCode("trusted-time-prod");
        check.setName("正式服可信时间");
        check.setStatus(RuntimeOpsInspectionStatus.BLOCKED);
        check.setRequired(true);
        check.setReason("没有选中时间源<script>alert(1)</script>&\"");
        check.setSampledAt(LocalDateTime.of(2026, 9, 7, 10, 0));
        return check;
    }

    private Map<String, byte[]> unzip(byte[] zip) throws Exception {
        Map<String, byte[]> result = new LinkedHashMap<>();
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(zip), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                result.put(entry.getName(), input.readAllBytes());
            }
        }
        return result;
    }

    private String text(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private String hex(byte[] bytes) throws Exception {
        return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
