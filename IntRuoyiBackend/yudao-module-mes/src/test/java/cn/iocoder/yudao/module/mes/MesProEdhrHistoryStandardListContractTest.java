package cn.iocoder.yudao.module.mes;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProEdhrHistoryStandardListContractTest {

    private static final Path ROOT = resolveRepoRoot();

    @Test
    void historyListUsesReleasedTransactionContract() throws Exception {
        String reqVo = read("yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/EdhrBatchExecutionPageReqVO.java");
        String respVo = read("yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/EdhrBatchExecutionRespVO.java");
        String mapper = read("yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/MesProEdhrBatchExecutionMapper.java");
        String service = read("yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java");

        assertTrue(reqVo.contains("private String productName;"));
        assertTrue(reqVo.contains("private Boolean releasedOnly;"));
        assertTrue(reqVo.contains("private LocalDateTime[] releaseApprovedTime;"));

        assertTrue(respVo.contains("private Long releaseTransactionId;"));
        assertTrue(respVo.contains("private String releaseStatus;"));
        assertTrue(respVo.contains("private Long releaseApprovedBy;"));
        assertTrue(respVo.contains("private LocalDateTime releaseApprovedAt;"));

        assertTrue(mapper.contains("likeIfPresent(MesProEdhrBatchExecutionDO::getProductName, reqVO.getProductName())"));
        assertTrue(mapper.contains("Boolean.TRUE.equals(reqVO.getReleasedOnly())"));
        assertTrue(mapper.contains("queryWrapper.exists(releasedTransactionExistsSql(reqVO.getReleaseApprovedTime()))"));
        assertTrue(mapper.contains("AND rt.release_status = 'RELEASED'"));
        assertTrue(mapper.contains("rt.approved_at >= TIMESTAMP"));
        assertTrue(mapper.contains("rt.approved_at <= TIMESTAMP"));
        assertTrue(service.contains(".setReleaseTransactionId(releaseTransaction == null ? null : releaseTransaction.getId())"));
        assertTrue(service.contains(".setReleaseStatus(releaseTransaction == null ? null : releaseTransaction.getReleaseStatus())"));
        assertTrue(service.contains(".setReleaseApprovedBy(releaseTransaction == null ? null : releaseTransaction.getApprovedBy())"));
        assertTrue(service.contains(".setReleaseApprovedAt(releaseTransaction == null ? null : releaseTransaction.getApprovedAt())"));
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
