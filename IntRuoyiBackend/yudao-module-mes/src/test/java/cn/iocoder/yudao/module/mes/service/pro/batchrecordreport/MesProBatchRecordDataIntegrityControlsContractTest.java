package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProBatchRecordDataIntegrityControlsContractTest {

    private static final Path REPORT_SERVICE = Path.of(
            "src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/"
                    + "MesProBatchRecordReportServiceImpl.java");
    private static final String REPORT_CONTROLLER =
            "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.MesProBatchRecordReportController";

    @Test
    void dataEntryAndConversionValidation_failFastOnMalformedParsedTables() throws Exception {
        String source = read(REPORT_SERVICE);

        assertTrue(source.contains("validateParsedTablesIntegrity("),
                "批记录 Word/图片解析后必须统一校验表格完整性，避免无效格式静默入库。");
        assertTrue(source.contains("PRO_BATCH_RECORD_REPORT_PARSED_TABLE_INVALID"),
                "解析结构异常必须返回明确业务错误。");
        assertTrue(source.contains("parsedTable.getRows().size()"),
                "解析表格行数必须和 rows 实际数量一致。");
        assertTrue(source.contains("cell.getRowSpan() <= 0") && source.contains("cell.getColSpan() <= 0"),
                "单元格跨度转换必须拒绝非法值，不能静默改写。");
    }

    @Test
    void batchImportAndJsonSync_verifyCompletenessBeforeSuccess() throws Exception {
        String source = read(REPORT_SERVICE);

        assertTrue(source.contains("validateImportPersistenceIntegrity("),
                "批量导入必须校验输入表数、输出报表数、新建/更新计数一致后才能成功返回。");
        assertTrue(source.contains("createdCount + updatedCount != parsedTables.size()"),
                "导入完整性必须比对 created + updated 与解析表格数量。");
        assertTrue(source.contains("validateTotalRecognitionJsonIntegrity("),
                "总识别 JSON 入库前必须校验 JSON 可解析且关键数组字段存在。");
        assertTrue(source.contains("updateProjectCodeTotalRecognitionJson"),
                "总识别 JSON 持久化必须检查 update 影响行数。");
    }

    @Test
    void modificationAndDeletionEndpoints_keepPermissionControls() throws Exception {
        Class<?> controller = Class.forName(REPORT_CONTROLLER);

        assertPermission(controller, "renameGeneratedReport", "mes:pro-batch-record-template:update");
        assertPermission(controller, "deleteGeneratedReport", "mes:pro-batch-record-template:delete");
        assertPermission(controller, "deleteGeneratedReports", "mes:pro-batch-record-template:delete");
        assertPermission(controller, "deleteGeneratedReportsByBatchRecordName",
                "mes:pro-batch-record-template:delete");
        assertPermission(controller, "deleteAllGeneratedReports", "mes:pro-batch-record-template:delete");
    }

    @Test
    void controlledGeneratedReports_cannotBePhysicallyDeletedWithoutGovernancePath() throws Exception {
        String source = read(REPORT_SERVICE);
        String deleteBySlotMethod = methodBody(source, "deleteGeneratedReportByBatchRecordNameAndFormSlotType");

        assertTrue(source.contains("validateReportDeletionAllowed("),
                "受控批记录版本报表物理删除前必须统一校验。");
        assertTrue(source.contains("isControlledBatchRecordReport("),
                "必须识别已纳入批记录定义或版本的数据。");
        assertTrue(source.contains("PRO_BATCH_RECORD_REPORT_DELETE_CONTROLLED_FORBIDDEN"),
                "删除受控报表必须返回明确业务错误。");
        assertTrue(source.contains("skippedControlledReportCount"),
                "批量清理类接口必须返回受控报表跳过数量，避免误认为全部已删除。");
        assertTrue(deleteBySlotMethod.contains("deleteGeneratedReports(reports, false, false)"),
                "按批记录名和表单槽位删除必须复用统一删除链路。");
        assertTrue(!deleteBySlotMethod.contains("jimuReportGateway.deleteReport(")
                        && !deleteBySlotMethod.contains("deleteHardByReportId("),
                "按槽位删除不得绕过统一删除校验直接硬删积木报表和元数据。");
    }

    private static void assertPermission(Class<?> controller, String methodName, String permission) {
        Method method = findMethod(controller, methodName);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, methodName + " must require permission " + permission);
        assertTrue(preAuthorize.value().contains(permission),
                methodName + " must require permission " + permission);
    }

    private static Method findMethod(Class<?> controller, String methodName) {
        for (Method method : controller.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        throw new AssertionError("Missing controller method " + methodName);
    }

    private static String read(Path path) throws Exception {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static String methodBody(String source, String methodName) {
        int methodIndex = source.indexOf(methodName + "(");
        assertTrue(methodIndex >= 0, "Missing method " + methodName);
        int bodyStart = source.indexOf('{', methodIndex);
        assertTrue(bodyStart >= 0, "Missing method body " + methodName);
        int depth = 0;
        for (int i = bodyStart; i < source.length(); i++) {
            char ch = source.charAt(i);
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(bodyStart, i + 1);
                }
            }
        }
        throw new AssertionError("Unclosed method body " + methodName);
    }
}
