package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProEdhrMultiSignatureApprovalContractTest {

    private static final Path MODULE_ROOT = Path.of(System.getProperty("user.dir"));

    @Test
    void templateSignatureMarkerMustPersistReviewSourceAndStableCellKey() throws Exception {
        assertContains("src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecordreport/vo/"
                        + "BatchRecordReportSignatureCellMarkerVO.java",
                "private String signatureCellKey;",
                "private String reviewSourceType;",
                "private Long reviewSourceId;",
                "private List<Long> reviewSourceIds;",
                "private String reviewSourceName;");
        assertContains("src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/"
                        + "MesProBatchRecordReportServiceImpl.java",
                "signatureCellKey",
                "reviewSourceType",
                "reviewSourceId",
                "reviewSourceIds",
                "reviewSourceName",
                "\"USER\"",
                "\"ROLES\"",
                "\"USERS\"",
                "APPROVE");
    }

    @Test
    void workTaskAndSignaturePersistenceMustCarrySignatureCellContext() throws Exception {
        assertContains("src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/"
                        + "MesProEdhrWorkTaskDO.java",
                "private String signatureCellKey;",
                "private Integer signatureRowIndex;",
                "private Integer signatureColumnIndex;",
                "private String reviewSourceType;",
                "private Long reviewSourceId;",
                "private String reviewSourceName;",
                "private String bpmTaskId;");
        assertContains("src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/"
                        + "MesProBatchRecordExecutionSignatureDO.java",
                "private String signatureCellKey;",
                "private Integer signatureRowIndex;",
                "private Integer signatureColumnIndex;",
                "private String reviewSourceType;",
                "private Long reviewSourceId;",
                "private String reviewSourceName;");
    }

    @Test
    void executionApprovalMustUseWorkTaskCellContextInsteadOfClientChosenCell() throws Exception {
        assertContains("src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/"
                        + "MesProEdhrWorkTaskService.java",
                "createReviewTasks",
                "completeOneReviewTask",
                "cancelPendingReviewTasks");
        assertContains("src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/"
                        + "MesProBatchRecordExecutionApprovalSignatureCommand.java",
                "signatureCellKey",
                "signatureRowIndex",
                "signatureColumnIndex",
                "reviewSourceType",
                "reviewSourceId",
                "reviewSourceName");
    }

    private static void assertContains(String relativePath, String... expectedTexts) throws Exception {
        String source = Files.readString(MODULE_ROOT.resolve(relativePath), StandardCharsets.UTF_8);
        for (String expectedText : expectedTexts) {
            assertTrue(source.contains(expectedText), relativePath + " must contain " + expectedText);
        }
    }
}
