package cn.iocoder.yudao.module.mes;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordCellLinkRuleDO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProBatchRecordCellLinkSchemaTest {

    private static final String RUNTIME_SCHEMA_FILE =
            "sql/mysql/20260711_mes_batch_record_cell_link_rule.sql";
    private static final String WORK_ORDER_SOURCE_SCHEMA_FILE =
            "sql/mysql/20260726_mes_batch_record_cell_link_work_order_source.sql";
    private static final String STRUCTURED_SOURCE_SCHEMA_FILE =
            "sql/mysql/20260830_mes_batch_record_cell_link_structured_source_widths.sql";
    private static final String TEST_SCHEMA_FILE =
            "yudao-module-mes/src/test/resources/sql/create_tables.sql";

    @Test
    void dataObjectDeclaresCrossFormCellLinkContract() {
        assertHasFields(MesProBatchRecordCellLinkRuleDO.class,
                "scopeType", "scopeId", "routeId", "batchRecordDefinitionId", "batchRecordVersionId",
                "sourceType", "sourceReportId", "sourceReportName", "sourceRowIndex", "sourceColumnIndex", "sourceCellKey",
                "sourceFieldCode", "sourceFieldName", "sourceLabel", "sourceValueType", "targetReportId", "targetReportName", "targetRowIndex",
                "targetColumnIndex", "targetCellKey", "targetLabel", "targetValueType",
                "aggregationStrategy", "overwritePolicy", "templateSnapshotHash", "ruleVersion", "enabled", "remark");
    }

    @Test
    void runtimeAndTestSchemasDeclareCrossFormCellLinkContract() throws Exception {
        Path projectDir = findProjectDir();
        String runtimeSchema = Files.readString(projectDir.resolve(RUNTIME_SCHEMA_FILE), StandardCharsets.UTF_8)
                + "\n"
                + Files.readString(projectDir.resolve(WORK_ORDER_SOURCE_SCHEMA_FILE), StandardCharsets.UTF_8)
                + "\n"
                + Files.readString(projectDir.resolve(STRUCTURED_SOURCE_SCHEMA_FILE), StandardCharsets.UTF_8);
        String testSchema = Files.readString(projectDir.resolve(TEST_SCHEMA_FILE), StandardCharsets.UTF_8);

        assertSchemaIsNonDestructive(runtimeSchema);
        assertSchemaContainsColumns(runtimeSchema);
        assertSchemaContainsColumns(testSchema);
        assertSchemaContainsStructuredSourceWidths(runtimeSchema, "`");
        assertSchemaContainsStructuredSourceWidths(testSchema, "\"");
        assertTrue(schemaContainsToken(runtimeSchema, "uk_mes_batch_record_cell_link_pair"));
        assertTrue(schemaContainsToken(runtimeSchema, "uk_mes_batch_record_cell_link_target"));
        assertTrue(schemaContainsToken(runtimeSchema, "idx_mes_batch_record_cell_link_source"));
        assertTrue(schemaContainsToken(runtimeSchema, "mes:pro-batch-record-cell-link:query"));
        assertTrue(schemaContainsToken(runtimeSchema, "mes:pro-batch-record-cell-link:update"));
        assertFalse(schemaContainsToken(runtimeSchema, "`batch_record_definition_id` bigint NOT NULL"));
        assertFalse(schemaContainsToken(runtimeSchema, "`batch_record_version_id` bigint NOT NULL"));
    }

    private static void assertSchemaContainsStructuredSourceWidths(String schema, String quote) {
        assertTrue(schemaContainsToken(schema, quote + "source_cell_key" + quote + " varchar(128) NOT NULL"),
                "source_cell_key must fit deterministic structured-source keys");
        assertTrue(schemaContainsToken(schema, quote + "source_field_code" + quote + " varchar(1024)"),
                "source_field_code must keep full process-pool report field codes");
        assertTrue(schemaContainsToken(schema, quote + "source_field_name" + quote + " varchar(255)"),
                "source_field_name must keep readable structured-source labels");
    }

    @Test
    void serviceSupportsLegacyReportSetScopeForExistingBatchRecordReports() throws Exception {
        Path projectDir = findProjectDir();
        String service = Files.readString(projectDir.resolve(
                "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordcelllink/MesProBatchRecordCellLinkServiceImpl.java"),
                StandardCharsets.UTF_8);
        String reportMapper = Files.readString(projectDir.resolve(
                "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecordreport/MesProBatchRecordReportMapper.java"),
                StandardCharsets.UTF_8);

        assertTrue(schemaContainsToken(service, "SCOPE_TYPE_REPORT_SET"),
                "Existing imported reports without batch_record_version_id must use a formal report-set scope");
        assertTrue(schemaContainsToken(service, "resolveReportSetScope"),
                "Legacy report-set scope must be resolved from the source report instead of failing scope validation");
        assertTrue(schemaContainsToken(service, "getSourceFileSha256"),
                "Report-set scope must group the actual imported workbook forms together");
        assertTrue(schemaContainsToken(reportMapper, "selectListBySourceFileSha256AndRouteKey"),
                "Mapper must load all real forms from the same imported workbook and route");
    }

    private static void assertHasFields(Class<?> type, String... fieldNames) {
        for (String fieldName : fieldNames) {
            assertDoesNotThrow(() -> declaredField(type, fieldName),
                    () -> "Missing field " + type.getSimpleName() + "." + fieldName);
        }
    }

    private static Field declaredField(Class<?> type, String fieldName) throws NoSuchFieldException {
        return type.getDeclaredField(fieldName);
    }

    private static void assertSchemaContainsColumns(String schema) {
        assertTrue(schemaContainsToken(schema, "mes_pro_batch_record_cell_link_rule"));
        for (String column : List.of(
                "scope_type", "scope_id", "route_id", "batch_record_definition_id", "batch_record_version_id",
                "source_type", "source_report_id", "source_report_name", "source_row_index", "source_column_index",
                "source_cell_key", "source_field_code", "source_field_name", "source_label", "source_value_type", "target_report_id",
                "target_report_name", "target_row_index", "target_column_index", "target_cell_key",
                "target_label", "target_value_type", "aggregation_strategy", "overwrite_policy", "template_snapshot_hash",
                "rule_version", "enabled", "tenant_id", "active_pair_unique_flag",
                "active_target_unique_flag")) {
            assertTrue(schemaContainsToken(schema, column), "Missing column " + column);
        }
    }

    private static void assertSchemaIsNonDestructive(String schema) {
        assertFalse(Pattern.compile("\\b(DROP\\s+TABLE|TRUNCATE\\s+TABLE)\\b", Pattern.CASE_INSENSITIVE)
                        .matcher(schema).find(),
                "Cell link schema must not contain destructive table operations");
        assertFalse(Pattern.compile("\\bDELETE\\s+FROM\\s+`?mes_", Pattern.CASE_INSENSITIVE)
                        .matcher(schema).find(),
                "Cell link schema must not delete MES data");
    }

    private static boolean schemaContainsToken(String schema, String token) {
        return Pattern.compile(Pattern.quote(token), Pattern.CASE_INSENSITIVE).matcher(schema).find();
    }

    private static Path findProjectDir() {
        Path currentDir = Path.of(System.getProperty("user.dir"));
        return "yudao-module-mes".equals(currentDir.getFileName().toString()) ? currentDir.getParent() : currentDir;
    }
}
