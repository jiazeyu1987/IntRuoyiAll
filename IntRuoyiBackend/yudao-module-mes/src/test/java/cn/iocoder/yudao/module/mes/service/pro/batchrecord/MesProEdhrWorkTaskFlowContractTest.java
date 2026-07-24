package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProEdhrWorkTaskFlowContractTest {

    private static final Path MODULE_ROOT = Path.of(System.getProperty("user.dir"));

    @Test
    void submitAndApprovalRequestsMustCarryWorkTaskId() throws Exception {
        assertContains("src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/"
                        + "MesProBatchRecordExecutionSubmitReqVO.java",
                "@NotNull(message = \"workTaskId 不能为空\")", "private Long workTaskId;");
        assertContains("src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/"
                        + "MesProBatchRecordExecutionApproveReqVO.java",
                "@NotNull(message = \"workTaskId 不能为空\")", "private Long workTaskId;");
        assertContains("src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/"
                        + "MesProBatchRecordExecutionRejectReqVO.java",
                "@NotNull(message = \"workTaskId 不能为空\")", "private Long workTaskId;");
    }

    @Test
    void taskCreationMustSendNotifyAfterRealActionUrlIsBuilt() throws Exception {
        String source = read("src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/"
                + "MesProEdhrWorkTaskServiceImpl.java");
        int insertIndex = source.indexOf("workTaskMapper.insert(task);");
        int actionUrlIndex = source.indexOf("task.setActionUrl(buildActionUrl(task));");
        int notifyIndex = source.indexOf("sendNotify(task);");
        assertTrue(insertIndex >= 0, "工作任务必须先插入以获得真实主键。");
        assertTrue(actionUrlIndex > insertIndex, "工作任务必须在插入后用真实主键生成 actionUrl。");
        assertTrue(notifyIndex > actionUrlIndex, "站内信必须在真实 actionUrl 回填后发送。");
    }

    @Test
    void fillAndReworkActionUrlMustOpenFormWorkspaceDirectly() throws Exception {
        String source = read("src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/"
                + "MesProEdhrWorkTaskServiceImpl.java");
        assertTrue(source.contains("/mes/pro/feedback/edhr-execution/form"),
                "填写/返工工作任务 actionUrl 必须直达 eDHR 填写工作区。");
        assertTrue(source.contains("fillCarrier=FORM"),
                "填写/返工工作任务 actionUrl 必须携带批记录填写载体。");
        assertTrue(source.contains("recordCategory=BATCH_RECORD"),
                "填写/返工工作任务 actionUrl 必须携带批记录记录分类。");
        assertTrue(source.contains("batchExecutionId="),
                "填写/返工工作任务 actionUrl 必须携带批次执行 ID，便于从个人工作台回到批次上下文。");
        assertTrue(source.contains("batchTaskId="),
                "填写/返工工作任务 actionUrl 必须携带批次任务 ID，便于校验真实任务归属。");
    }

    @Test
    void workTaskNotifyTemplatesMustSeedEveryRuntimeTaskType() throws Exception {
        String javaSource = read("src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/"
                + "MesProEdhrWorkTaskServiceImpl.java");
        String seedSql = read("../sql/mysql/20260611_mes_edhr_work_task_flow.sql")
                + read("../sql/mysql/20260612_mes_edhr_final_archive_work_task.sql")
                + read("../sql/mysql/20260718_mes_edhr_fill_task_reassignment_notify.sql");
        assertRuntimeTemplateSeeded(javaSource, seedSql, "MES_EDHR_FILL_TASK_ASSIGNED");
        assertRuntimeTemplateSeeded(javaSource, seedSql, "MES_EDHR_FILL_TASK_REASSIGNED");
        assertRuntimeTemplateSeeded(javaSource, seedSql, "MES_EDHR_REVIEW_TASK_ASSIGNED");
        assertRuntimeTemplateSeeded(javaSource, seedSql, "MES_EDHR_APPROVE_TASK_ASSIGNED");
        assertRuntimeTemplateSeeded(javaSource, seedSql, "MES_EDHR_REWORK_TASK_ASSIGNED");
        assertRuntimeTemplateSeeded(javaSource, seedSql, "MES_EDHR_ARCHIVE_TASK_ASSIGNED");
        assertRuntimeTemplateSeeded(javaSource, seedSql, "MES_EDHR_WORK_TASK_OVERDUE");
    }

    @Test
    void myWorkTaskPageMustAllowFillerBatchExecutionQueryWithoutOpeningManagementActions() throws Exception {
        String controller = read("src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/"
                + "MesProEdhrWorkTaskController.java");
        assertTrue(controller.contains(
                        "@ss.hasAnyPermissions('mes:pro-edhr-work-task:query', 'mes:pro-edhr-batch-execution:query')"),
                "个人工作台我的 eDHR 任务页必须允许填写人的批次执行 query 动态权益访问。");
        assertTrue(controller.contains(
                        "@PreAuthorize(\"@ss.hasPermission('mes:pro-edhr-work-task:update')\")"),
                "工作任务重派和候选签名完成仍必须保留工作任务 update 权限。");
        assertTrue(controller.contains(
                        "@PreAuthorize(\"@ss.hasPermission('mes:pro-edhr-work-task-rule:update')\")"),
                "工作任务规则维护仍必须保留规则 update 权限。");
    }

    private static void assertRuntimeTemplateSeeded(String javaSource, String seedSql, String templateCode) {
        assertTrue(javaSource.contains("\"" + templateCode + "\""),
                "运行时代码必须显式引用模板编码 " + templateCode);
        assertTrue(seedSql.contains("'" + templateCode + "'"),
                "正式 MySQL 种子脚本必须插入模板编码 " + templateCode);
    }

    private static void assertContains(String relativePath, String... expectedTexts) throws Exception {
        String source = read(relativePath);
        for (String expectedText : expectedTexts) {
            assertTrue(source.contains(expectedText), relativePath + " must contain " + expectedText);
        }
    }

    private static String read(String relativePath) throws Exception {
        return Files.readString(MODULE_ROOT.resolve(relativePath), StandardCharsets.UTF_8);
    }
}
