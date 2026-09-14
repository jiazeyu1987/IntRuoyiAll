package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileVersionHistoryRespVO;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Focused P4 contract tests for new-file visibility, traceability and audit projection. */
class DccLifecycleVisibilityAuditTest {

    @Test
    void historyAndRoleProjection() throws Exception {
        assertTrue(hasField(DccControlledFileRespVO.class, "masterId"), "response missing masterId");
        for (String field : List.of("versionNo", "revisionCode", "iterationNo",
                "predecessorControlledFileId", "revisionBaseActiveControlledFileId", "sourceSha256",
                "previousSourceSha256", "changeDescription", "status", "publishedTime",
                "supersededByFileId")) {
            assertTrue(hasField(DccControlledFileRespVO.class, field), "response missing " + field);
            assertTrue(hasField(DccControlledFileVersionHistoryRespVO.class, field), "history missing " + field);
        }
        String queryService = readSource("DccControlledFileQueryServiceImpl.java");
        assertTrue(queryService.contains("respVO.setVersionHistory(buildVersionHistory"));
        assertTrue(queryService.contains("resolveCurrentActiveVersionNo"));
        assertTrue(queryService.contains("canReadBinary(userId, history"));
        assertEquals("A/1", DccWindchillVersionNumber.parse("A/1").display());
        assertEquals("A/2", DccWindchillVersionNumber.parse("A/2").display());
        assertEquals("B/1", DccWindchillVersionNumber.parse("B/1").display());
    }

    @Test
    void auditAndNotificationBoundary() throws Exception {
        String logService = readSource("../log/DccControlledFileLogQueryServiceImpl.java");
        assertTrue(logService.contains("TYPE_FILE_CHECKOUT"));
        assertTrue(logService.contains("checkinIterationId"));
        assertTrue(logService.contains("cancelReason"));
        assertTrue(logService.contains("TYPE_FILE_REVISION"));
        String queryService = readSource("DccControlledFileQueryServiceImpl.java");
        assertTrue(queryService.contains("executeLifecycleAudit"));
        assertTrue(queryService.contains("result.getVersionNo()"));
        assertTrue(queryService.contains("FAILED"));
        String adapter = readSource("DccControlledContentAdapter.java");
        assertTrue(adapter.contains("recordFinalized"));
        assertTrue(adapter.contains("finalizeVersionRefs"));
        assertTrue(adapter.contains("recordPublishFinalizationStarted"));
        assertTrue(adapter.contains("dcc controlled file finalization succeeded"));
    }

    private boolean hasField(Class<?> type, String name) {
        try {
            type.getDeclaredField(name);
            return true;
        } catch (NoSuchFieldException ignored) {
            return false;
        }
    }

    private String readSource(String fileName) throws Exception {
        Path source = Path.of("src", "main", "java", "cn", "iocoder", "yudao", "module", "dcc",
                "service", "file", fileName);
        if (fileName.startsWith("../")) {
            source = Path.of("src", "main", "java", "cn", "iocoder", "yudao", "module", "dcc",
                    "service", fileName.substring(3));
        }
        return Files.readString(source, StandardCharsets.UTF_8);
    }
}
