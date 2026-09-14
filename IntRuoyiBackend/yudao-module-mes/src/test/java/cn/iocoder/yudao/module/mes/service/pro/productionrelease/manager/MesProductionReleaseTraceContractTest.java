package cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProductionReleaseTraceContractTest {

    @Test
    void completedTraceIncludesReleasedArchivedAndRejectedTerminalBatches() throws Exception {
        String source = Files.readString(sourcePath(
                "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/"
                        + "MesProEdhrBatchExecutionMapper.java"), StandardCharsets.UTF_8);
        int start = source.indexOf("if (Boolean.TRUE.equals(reqVO.getCompletedTraceOnly()))");
        int end = source.indexOf("queryWrapper.notIn", start);
        String traceFilter = source.substring(start, end);

        assertTrue(traceFilter.contains("exists(releasedTransactionExistsSql())"));
        assertTrue(traceFilter.contains("BATCH_STATUS_ARCHIVED"));
        assertTrue(traceFilter.contains("BATCH_STATUS_REJECTED"));

        String releaseService = Files.readString(sourcePath(
                "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/"
                        + "MesProEdhrReleaseServiceImpl.java"), StandardCharsets.UTF_8);
        int statusFilterStart = releaseService.indexOf("private boolean batchExecutionStatusMatches");
        int statusFilterEnd = releaseService.indexOf("private boolean matches", statusFilterStart);
        String statusFilter = releaseService.substring(statusFilterStart, statusFilterEnd);
        assertTrue(statusFilter.contains("return STATUS_RELEASED.equals(item.getReleaseStatus())"));
        assertTrue(statusFilter.contains("BATCH_STATUS_ARCHIVED"));
        assertTrue(statusFilter.contains("BATCH_STATUS_REJECTED"));
    }

    private static Path sourcePath(String moduleRelativePath) {
        List<Path> candidates = new ArrayList<>();
        Path current = Path.of("").toAbsolutePath();
        for (int i = 0; i < 8 && current != null; i++, current = current.getParent()) {
            candidates.add(current.resolve(moduleRelativePath));
            candidates.add(current.resolve("IntRuoyiBackend").resolve(moduleRelativePath));
        }
        return candidates.stream()
                .filter(Files::isRegularFile)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("source path not found: " + moduleRelativePath));
    }
}
