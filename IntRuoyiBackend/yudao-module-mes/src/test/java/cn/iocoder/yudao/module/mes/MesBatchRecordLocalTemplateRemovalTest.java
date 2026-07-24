package cn.iocoder.yudao.module.mes;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesBatchRecordLocalTemplateRemovalTest {

    @Test
    void localTemplateGenerationSourcesShouldBeRemovedWithoutBreakingSharedBatchRecordDependencies() throws Exception {
        Path projectDir = findProjectDir();

        assertFalse(Files.exists(projectDir.resolve(
                        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProBatchRecordTemplateController.java")),
                "Template page controller should be removed");
        assertFalse(Files.exists(projectDir.resolve(
                        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProBatchRecordTemplateImportController.java")),
                "Template import controller should be removed");
        assertFalse(Files.exists(projectDir.resolve(
                        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordTemplateService.java")),
                "Template service interface should be removed");
        assertFalse(Files.exists(projectDir.resolve(
                        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordTemplateImportService.java")),
                "Template import service interface should be removed");
        assertFalse(Files.exists(projectDir.resolve(
                        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordTemplateServiceImpl.java")),
                "Template service implementation should be removed");
        assertFalse(Files.exists(projectDir.resolve(
                        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordWordParser.java")),
                "Local template word parser should be removed");
        assertFalse(Files.exists(projectDir.resolve(
                        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordLegacyDocConverter.java")),
                "Legacy doc converter should be removed");
        assertFalse(Files.exists(projectDir.resolve(
                        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordWordComLegacyDocConverter.java")),
                "Word COM legacy converter should be removed");
        assertFalse(Files.exists(projectDir.resolve(
                        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordTemplateLayoutRules.java")),
                "Local template layout rules should be removed");
        assertFalse(Files.exists(projectDir.resolve(
                        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/MesProBatchRecordImportDO.java")),
                "Local template import DO should be removed");
        assertFalse(Files.exists(projectDir.resolve(
                        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/MesProBatchRecordImportMapper.java")),
                "Local template import mapper should be removed");

        assertTrue(Files.exists(projectDir.resolve(
                        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/MesProBatchRecordTemplateDO.java")),
                "Template DO must remain because batch-record execution still snapshots from it");
        assertTrue(Files.exists(projectDir.resolve(
                        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/MesProBatchRecordTemplateMapper.java")),
                "Template mapper must remain because batch-record execution still reads it");
        assertTrue(Files.exists(projectDir.resolve(
                        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordExecutionServiceImpl.java")),
                "Batch-record execution service must remain");
        assertTrue(Files.exists(projectDir.resolve(
                        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordReportServiceImpl.java")),
                "Jimu batch-record report service must remain");
    }

    @Test
    void runtimeAndTestSchemasShouldDropOnlyBatchRecordImportTable() throws Exception {
        Path projectDir = findProjectDir();
        Path runtimeSchemaFile = projectDir.resolve("sql/mysql/20260512_mes_base_schema.sql");
        Path testSchemaFile = projectDir.resolve("yudao-module-mes/src/test/resources/sql/create_tables.sql");

        String runtimeSchema = Files.readString(runtimeSchemaFile);
        String testSchema = Files.readString(testSchemaFile);

        assertFalse(runtimeSchema.contains("mes_pro_batch_record_import"),
                "Runtime schema should no longer define mes_pro_batch_record_import");
        assertFalse(testSchema.contains("mes_pro_batch_record_import"),
                "Test schema should no longer define mes_pro_batch_record_import");

        assertTrue(runtimeSchema.contains("mes_pro_batch_record_template"),
                "Runtime schema must keep mes_pro_batch_record_template for execution dependencies");
        assertTrue(testSchema.contains("mes_pro_batch_record_template"),
                "Test schema must keep mes_pro_batch_record_template for execution dependencies");
        assertTrue(runtimeSchema.contains("mes_pro_batch_record_report"),
                "Runtime schema must keep mes_pro_batch_record_report");
        assertTrue(testSchema.contains("mes_pro_batch_record_report"),
                "Test schema must keep mes_pro_batch_record_report");
    }

    private static Path findProjectDir() {
        Path currentDir = Path.of(System.getProperty("user.dir"));
        return "yudao-module-mes".equals(currentDir.getFileName().toString()) ? currentDir.getParent() : currentDir;
    }
}
