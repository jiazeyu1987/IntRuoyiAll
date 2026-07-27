package cn.iocoder.yudao.module.mes;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteScheduleConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProRouteVersionLifecycleSchemaTest {

    private static final String SCHEDULE_BASE_SCHEMA_FILE = "sql/mysql/20260610_mes_schedule_order_p1.sql";
    private static final String ROUTE_BASE_SCHEMA_FILE = "sql/mysql/20260613_mes_smart_scheduling_t1_schema.sql";
    private static final String EDHR_BASE_SCHEMA_FILE = "sql/mysql/20260608_edhr_batch_execution_schema.sql";
    private static final String LIFECYCLE_MIGRATION_FILE = "sql/mysql/20260715_mes_route_version_lifecycle.sql";
    private static final String APPROVAL_PROCESS_ID_STRING_MIGRATION_FILE =
            "sql/mysql/20260717_mes_route_version_approval_instance_id_string.sql";
    private static final String ROUTE_SNAPSHOT_MEDIUMTEXT_MIGRATION_FILE =
            "sql/mysql/20260727_mes_route_version_snapshot_mediumtext.sql";
    private static final String TEST_SCHEMA_FILE = "yudao-module-mes/src/test/resources/sql/create_tables.sql";

    @Test
    void routeVersionLifecycleSchemaRequiresCandidatePublishAndSingleActiveContracts() throws Exception {
        Path projectDir = findProjectDir();
        String runtimeSchema = read(projectDir, ROUTE_BASE_SCHEMA_FILE)
                + "\n" + read(projectDir, LIFECYCLE_MIGRATION_FILE)
                + "\n" + read(projectDir, APPROVAL_PROCESS_ID_STRING_MIGRATION_FILE);
        String testSchema = read(projectDir, TEST_SCHEMA_FILE);

        for (String schema : new String[] { runtimeSchema, testSchema }) {
            assertSchemaContainsColumns(schema, "mes_pro_route_version",
                    "lifecycle_status", "change_summary_json", "published_by", "published_time",
                    "submitted_by", "submitted_time", "approval_process_instance_id", "active_unique_flag");
            assertTrue(schemaContainsToken(schema, "uk_mes_pro_route_version_active_one"),
                    "schema must enforce one active route version per tenant and route");
        }

        assertTrue(runtimeSchema.contains("duplicate active route versions must be resolved before migration"),
                "migration must fail fast when existing data already has multiple active versions");

        assertHasFields(MesProRouteVersionDO.class,
                "lifecycleStatus", "changeSummaryJson", "publishedBy", "publishedTime",
                "submittedBy", "submittedTime", "approvalProcessInstanceId", "activeUniqueFlag");
        assertEquals(String.class, declaredField(MesProRouteVersionDO.class, "approvalProcessInstanceId").getType(),
                "Flowable process instance IDs are strings and must not be mapped as Long");
        assertTrue(schemaColumnUsesStringType(runtimeSchema, "mes_pro_route_version", "approval_process_instance_id"),
                "runtime schema must store BPM process instance IDs as varchar");
        assertTrue(schemaColumnUsesStringType(testSchema, "mes_pro_route_version", "approval_process_instance_id"),
                "test schema must store BPM process instance IDs as varchar");
    }

    @Test
    void routeVersionSnapshotSchemaSupportsLargeCandidateSnapshots() throws Exception {
        Path projectDir = findProjectDir();
        String runtimeSchema = read(projectDir, ROUTE_BASE_SCHEMA_FILE)
                + "\n" + read(projectDir, LIFECYCLE_MIGRATION_FILE)
                + "\n" + read(projectDir, ROUTE_SNAPSHOT_MEDIUMTEXT_MIGRATION_FILE);
        String testSchema = read(projectDir, TEST_SCHEMA_FILE);

        assertTrue(schemaColumnUsesLargeTextType(runtimeSchema, "mes_pro_route_version", "route_snapshot_json"),
                "runtime route snapshots must exceed MySQL TEXT capacity for large route candidates");
        assertTrue(schemaColumnUsesClobType(testSchema, "mes_pro_route_version", "route_snapshot_json"),
                "test route snapshots must use CLOB capacity equivalent");
    }

    @Test
    void edhrBatchExecutionSchemaFreezesRouteVersionSnapshot() throws Exception {
        Path projectDir = findProjectDir();
        String runtimeSchema = read(projectDir, EDHR_BASE_SCHEMA_FILE)
                + "\n" + read(projectDir, LIFECYCLE_MIGRATION_FILE);
        String testSchema = read(projectDir, TEST_SCHEMA_FILE);

        for (String schema : new String[] { runtimeSchema, testSchema }) {
            assertSchemaContainsColumns(schema, "mes_pro_edhr_batch_execution",
                    "route_version_id", "route_version_no", "route_snapshot_json");
            assertTrue(schemaContainsToken(schema, "idx_mes_pro_edhr_batch_execution_route_version"),
                    "schema must index batch execution by frozen route version");
        }

        assertHasFields(MesProEdhrBatchExecutionDO.class,
                "routeVersionId", "routeVersionNo", "routeSnapshotJson");
        assertHasFields(EdhrBatchExecutionRespVO.class,
                "routeVersionId", "routeVersionNo");
    }

    @Test
    void scheduleSchemaFreezesRouteVersionAndRouteScheduleConfigContract() throws Exception {
        Path projectDir = findProjectDir();
        String runtimeSchema = read(projectDir, SCHEDULE_BASE_SCHEMA_FILE)
                + "\n" + read(projectDir, ROUTE_BASE_SCHEMA_FILE);
        String testSchema = read(projectDir, TEST_SCHEMA_FILE);

        for (String schema : new String[] { runtimeSchema, testSchema }) {
            assertSchemaContainsColumns(schema, "mes_pro_schedule_order", "route_version_id");
            assertSchemaContainsColumns(schema, "mes_pro_schedule_order_process", "route_version_id");
            assertSchemaContainsColumns(schema, "mes_pro_route_schedule_config",
                    "route_version_id", "route_process_id");
            assertTrue(schemaContainsToken(schema, "uk_mes_pro_route_schedule_config_process")
                            || schemaContainsToken(schema, "uk_mes_pro_route_schedule_config_active_process"),
                    "schema must keep active route schedule config unique by route version and route process");
        }

        assertHasFields(MesProScheduleOrderDO.class, "routeVersionId");
        assertHasFields(MesProScheduleOrderProcessDO.class, "routeVersionId");
        assertHasFields(MesProRouteScheduleConfigDO.class, "routeVersionId", "routeProcessId");
    }

    @Test
    void scheduleRuntimeSelectsOnlyActiveLifecycleRouteVersionAsLatestApprovedReference() throws Exception {
        Path projectDir = findProjectDir();
        String mapperSource = read(projectDir,
                "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/route/MesProRouteVersionMapper.java");

        assertTrue(mapperSource.contains("MesProRouteVersionDO::getLifecycleStatus")
                        && mapperSource.contains("STATUS_ACTIVE"),
                "排产运行态解析路线版本时必须同时要求 active=true 和 lifecycleStatus=ACTIVE");
    }

    private static void assertSchemaContainsColumns(String schema, String tableName, String... columns) {
        assertTrue(schemaContainsToken(schema, tableName), "Missing table reference in schema: " + tableName);
        for (String column : columns) {
            assertTrue(schemaContainsToken(schema, column), "Missing column " + tableName + "." + column);
        }
    }

    private static boolean schemaContainsToken(String schema, String token) {
        return Pattern.compile(Pattern.quote(token), Pattern.CASE_INSENSITIVE).matcher(schema).find();
    }

    private static boolean schemaColumnUsesStringType(String schema, String tableName, String columnName) {
        return Pattern.compile(Pattern.quote(tableName) + "[\\s\\S]*" + Pattern.quote(columnName)
                + "[`\"\\s]+varchar\\s*\\(", Pattern.CASE_INSENSITIVE).matcher(schema).find();
    }

    private static boolean schemaColumnUsesLargeTextType(String schema, String tableName, String columnName) {
        return Pattern.compile(Pattern.quote(tableName) + "[\\s\\S]*" + Pattern.quote(columnName)
                + "[`\"\\s]+(mediumtext|longtext|json)\\b", Pattern.CASE_INSENSITIVE).matcher(schema).find();
    }

    private static boolean schemaColumnUsesClobType(String schema, String tableName, String columnName) {
        return Pattern.compile(Pattern.quote(tableName) + "[\\s\\S]*" + Pattern.quote(columnName)
                + "[`\"\\s]+clob\\b", Pattern.CASE_INSENSITIVE).matcher(schema).find();
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

    private static String read(Path projectDir, String relativePath) throws Exception {
        return Files.readString(projectDir.resolve(relativePath), StandardCharsets.UTF_8);
    }

    private static Path findProjectDir() {
        Path currentDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (currentDir != null) {
            if (Files.exists(currentDir.resolve(ROUTE_BASE_SCHEMA_FILE))) {
                return currentDir;
            }
            currentDir = currentDir.getParent();
        }
        throw new AssertionError("Unable to locate project directory containing " + ROUTE_BASE_SCHEMA_FILE);
    }
}
