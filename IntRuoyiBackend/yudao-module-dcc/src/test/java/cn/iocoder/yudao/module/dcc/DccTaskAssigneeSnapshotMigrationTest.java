package cn.iocoder.yudao.module.dcc;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DccTaskAssigneeSnapshotMigrationTest {

    @Test
    void taskAssigneeSnapshotSchemaPreservesDepartmentObligationsInsteadOfUserDedup() throws Exception {
        Path projectDir = findProjectDir();
        Path migrationFile = projectDir.resolve("sql/mysql/20260922_dcc_task_assignee_snapshot.sql");
        Path baseSchemaFile = projectDir.resolve("sql/mysql/20260513_dcc_base_schema.sql");
        Path testSchemaFile = projectDir.resolve("yudao-module-dcc/src/test/resources/sql/create_tables.sql");

        assertTrue(Files.exists(migrationFile), "DCC task assignee snapshot migration must exist");

        String migration = Files.readString(migrationFile);
        String baseSchema = Files.readString(baseSchemaFile);
        String testSchema = Files.readString(testSchemaFile);
        for (String schema : List.of(migration, baseSchema, testSchema)) {
            assertTrue(schema.contains("CREATE TABLE IF NOT EXISTS `dcc_controlled_file_task_assignee_snapshot`"),
                    "schema must create task assignee snapshot table");
            assertTrue(schema.contains("`controlled_file_id`"), "schema must keep the controlled file identity");
            assertTrue(schema.contains("`stage_code`"), "schema must keep the stage identity");
            assertTrue(schema.contains("`department_id`"), "schema must keep the department obligation identity");
            assertTrue(schema.contains("`assignee_user_id`"), "schema must freeze the resolved assignee");
            assertTrue(schema.contains("`obligation_id`"), "schema must keep a stable obligation id");
            assertTrue(schema.contains("uk_dcc_task_assignee_obligation"),
                    "schema must prevent duplicate department obligations");
        }
        assertTrue(migration.contains("dependsOn=20260922_dcc_three_workflow_bpmn_seed"),
                "migration must depend on the three-workflow BPMN seed");
    }

    private static Path findProjectDir() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("sql/mysql/20260513_dcc_base_schema.sql"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate IntRuoyiBackend project dir");
    }
}
