package cn.iocoder.yudao.module.mes;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProEdhrTraceTerminalPartitionContractTest {

    private static final Path ROOT = resolveRepoRoot();

    @Test
    void batchExecutionPageSupportsTraceOnlyExclusionFilters() throws Exception {
        String reqVo = read("yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/EdhrBatchExecutionPageReqVO.java");
        String mapper = read("yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/MesProEdhrBatchExecutionMapper.java");

        assertTrue(reqVo.contains("private List<Integer> statuses;"));
        assertTrue(reqVo.contains("private List<Integer> excludeStatuses;"));
        assertTrue(reqVo.contains("private Boolean excludeReleased;"));
        assertTrue(reqVo.contains("private Boolean completedTraceOnly;"));

        assertTrue(mapper.contains("queryWrapper.in(MesProEdhrBatchExecutionDO::getStatus, reqVO.getStatuses())"));
        assertTrue(mapper.contains("queryWrapper.notIn(MesProEdhrBatchExecutionDO::getStatus, reqVO.getExcludeStatuses())"));
        assertTrue(mapper.contains("queryWrapper.notExists(\"SELECT 1 FROM mes_pro_edhr_release_transaction rt \""));
        assertTrue(mapper.contains("AND rt.release_status = 'RELEASED'"));
        assertTrue(mapper.contains("Boolean.TRUE.equals(reqVO.getCompletedTraceOnly())"));
        assertTrue(mapper.contains("queryWrapper.and(wrapper -> wrapper"));
        assertTrue(mapper.contains("BATCH_STATUS_ARCHIVED, BATCH_STATUS_REJECTED"));
        assertTrue(mapper.contains(".exists(releasedTransactionExistsSql())"));
    }

    @Test
    void releaseTracePageFiltersByCompletedBatchScopeBeforePagination() throws Exception {
        String reqVo = read("yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/MesProEdhrReleasePageReqVO.java");
        String service = read("yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrReleaseServiceImpl.java");

        assertTrue(reqVo.contains("private List<Integer> batchExecutionStatuses;"));
        assertTrue(reqVo.contains("private Boolean completedTraceOnly;"));

        assertTrue(service.contains("batchReqVO.setCompletedTraceOnly(reqVO.getCompletedTraceOnly());"));
        assertTrue(service.contains(".filter(item -> batchExecutionStatusMatches(reqVO, item))"));
        assertTrue(service.contains("private boolean batchExecutionStatusMatches(MesProEdhrReleasePageReqVO reqVO, MesProEdhrReleaseRespVO item)"));
        assertTrue(service.contains("Boolean.TRUE.equals(reqVO.getCompletedTraceOnly())"));
        assertTrue(service.contains("STATUS_RELEASED.equals(item.getReleaseStatus())"));
        assertTrue(service.contains("MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_ARCHIVED"));
        assertTrue(service.contains("MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_REJECTED"));
        assertTrue(service.contains("List<Integer> expectedStatuses = reqVO.getBatchExecutionStatuses();"));
        assertTrue(service.contains("expectedStatuses.contains(item.getBatchExecutionStatus())"));
        assertTrue(service.contains("List<MesProEdhrReleaseRespVO> filteredList = transactions.stream()"));
        assertTrue(service.contains("return new PageResult<>(pageList, (long) filteredList.size());"));
    }

    private String read(String relativePath) throws Exception {
        Path path = ROOT.resolve(relativePath);
        assertTrue(Files.exists(path), relativePath + " must exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static Path resolveRepoRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        if (Files.exists(current.resolve("sql/mysql"))) {
            return current;
        }
        return current.getParent();
    }
}
