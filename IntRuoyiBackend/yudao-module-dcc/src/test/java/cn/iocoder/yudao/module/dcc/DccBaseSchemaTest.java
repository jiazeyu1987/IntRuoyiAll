package cn.iocoder.yudao.module.dcc;

import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DccBaseSchemaTest {

    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile(
            "@TableName\\s*\\(\\s*(?:value\\s*=\\s*)?\"([^\"]+)\"");
    private static final Pattern FIELD_PATTERN = Pattern.compile(
            "^\\s*private\\s+(?!static)(?:final\\s+)?[\\w<>?,\\s]+?\\s+([a-zA-Z][a-zA-Z0-9_]*)\\s*;",
            Pattern.MULTILINE);
    private static final List<String> BASE_COLUMNS = List.of(
            "create_time", "update_time", "creator", "updater", "deleted", "tenant_id");
    private static final Map<String, List<String>> REQUIRED_FOUNDATION_COLUMNS = Map.ofEntries(
            Map.entry("dcc_file_category", List.of("description", "distribution_required", "training_required")),
            Map.entry("dcc_file_category_match_rule", List.of(
                    "category_id", "match_text", "match_type", "weight", "active", "remark")),
            Map.entry("dcc_file_type_taxonomy", List.of("parent_id", "level_no", "code", "name", "active", "sort",
                    "remark")),
            Map.entry("dcc_file_category_permission_rule", List.of(
                    "category_id", "action_type", "subject_type", "subject_id", "active", "remark")),
            Map.entry("dcc_file_category_distribution_rule", List.of(
                    "category_id", "department_id", "distribution_medium", "active")),
            Map.entry("dcc_file_category_training_rule", List.of("category_id", "department_id", "active")),
            Map.entry("dcc_file_directory", List.of(
                    "parent_id", "code", "name", "active", "sort", "remark", "access_rule_manually_bound")),
            Map.entry("dcc_category_approval_route_node", List.of(
                    "stage_code", "stage_order", "candidate_source_ids", "require_all_approvals")),
            Map.entry("dcc_controlled_file_master", List.of(
                    "category_id", "directory_id", "file_name", "file_number",
                    "current_active_controlled_file_id", "status")),
            Map.entry("dcc_controlled_file", List.of(
                    "master_id", "file_name", "file_number", "source_file_id", "published_file_id",
                    "submitter_id", "published_time", "obsoleted_by", "obsoleted_time",
                    "obsolete_reason", "superseded_by_file_id", "finalization_error")),
            Map.entry("dcc_controlled_file_route_snapshot", List.of(
                    "stage_code", "stage_name", "stage_order", "candidate_source_ids", "require_all_approvals")),
            Map.entry("dcc_controlled_file_signature", List.of(
                    "controlled_file_id", "task_id", "actor_id", "action_type",
                    "signature_mode", "password_verified", "comment", "signed_at",
                    "actor_post_names_snapshot", "actor_role_names_snapshot", "signature_purpose",
                    "authorization_basis", "authentication_method", "record_version_snapshot",
                    "record_hash_snapshot", "client_ip_snapshot", "user_agent_snapshot", "snapshot_status",
                    "signature_image_id", "signature_image_version_no", "signature_image_file_id",
                    "signature_image_file_url", "signature_image_sha256", "signature_image_content_type",
                    "signature_image_file_size", "signature_image_status_snapshot",
                    "signature_image_verified_status")),
            Map.entry("dcc_electronic_signature_image", List.of(
                    "user_id", "version_no", "file_id", "file_url", "storage_path", "file_name",
                    "content_type", "file_size", "sha256", "image_status", "active",
                    "uploaded_by", "uploaded_at", "enabled_at", "disabled_at", "disable_reason",
                    "referenced_count")),
            Map.entry("dcc_electronic_signature_authorization", List.of(
                    "user_id", "electronic_signature_enabled")),
            Map.entry("dcc_controlled_file_distribution", List.of(
                    "controlled_file_id", "department_id", "distribution_medium", "status",
                    "acknowledged_by", "acknowledged_at")),
            Map.entry("dcc_controlled_file_distribution_recipient", List.of(
                    "distribution_id", "user_id", "message_job_id", "read_at", "acknowledged_at")),
            Map.entry("dcc_controlled_file_training", List.of("controlled_file_id", "department_id", "status")),
            Map.entry("dcc_controlled_file_training_assignment", List.of(
                    "training_id", "user_id", "message_job_id", "status", "acknowledged_at")),
            Map.entry("dcc_controlled_file_training_progress", List.of(
                    "controlled_file_id", "user_id", "required_view_seconds", "accumulated_view_seconds",
                    "first_viewed_at", "last_viewed_at", "acknowledged_at")),
            Map.entry("dcc_controlled_file_training_view_session", List.of(
                    "training_progress_id", "user_id", "client_session_id", "started_at",
                    "last_heartbeat_at", "ended_at", "accumulated_seconds")),
            Map.entry("dcc_controlled_file_message_job", List.of(
                    "business_type", "business_id", "template_code", "recipient_user_id",
                    "status", "error_message", "sent_at")),
            Map.entry("dcc_controlled_file_obsolete_audit", List.of(
                    "controlled_file_id", "operator_id", "obsolete_reason", "status_before", "status_after"))
    );
    private static final Set<String> REQUIRED_REVISION_STATUSES = Set.of(
            "DRAFT",
            "PENDING_DOC_CONTROL_REVIEW",
            "PENDING_MATRIX_REVIEW",
            "PENDING_MATRIX_APPROVAL",
            "PENDING_DOC_CONTROL_APPROVAL",
            "READY_TO_PUBLISH",
            "FINALIZING",
            "ACTIVE",
            "REJECTED",
            "WITHDRAWN",
            "OBSOLETE",
            "SUPERSEDED",
            "FINALIZATION_FAILED"
    );

    @Test
    void mysqlSchemaShouldCoverEveryDccDoTableAndColumn() throws Exception {
        Path projectDir = findProjectDir();
        Path testSchemaFile = projectDir.resolve("yudao-module-dcc/src/test/resources/sql/create_tables.sql");

        assertTrue(Files.exists(testSchemaFile), "DCC test schema file must exist");

        String runtimeSchema = readDccRuntimeSchema(projectDir);
        String testSchema = Files.readString(testSchemaFile)
                + "\n"
                + runtimeSchema;
        assertSchemaIsNonDestructive(runtimeSchema, "runtime");
        assertSchemaIsNonDestructive(testSchema, "test");

        Path doRoot = projectDir.resolve(
                "yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/dataobject");
        List<Path> doFiles;
        try (var stream = Files.walk(doRoot)) {
            doFiles = stream
                    .filter(path -> path.getFileName().toString().endsWith("DO.java"))
                    .sorted()
                    .toList();
        }

        int coveredTables = 0;
        for (Path doFile : doFiles) {
            String source = Files.readString(doFile);
            String tableName = parseTableName(source);
            if (tableName == null || !tableName.startsWith("dcc_")) {
                continue;
            }
            coveredTables++;
            assertSchemaBlockHasExpectedColumns(runtimeSchema, tableName, expectedColumns(source), "runtime");
            assertSchemaBlockHasExpectedColumns(testSchema, tableName, expectedColumns(source), "test");
        }

        assertTrue(coveredTables > 0, "DCC DO table scan must find tables");
        assertFoundationTables(runtimeSchema, "runtime");
        assertFoundationTables(testSchema, "test");
        assertRevisionLifecycleEnum();
    }

    @Test
    void mysqlSchemaShouldRetirePersonalFilesAndSeedApprovalTaskMenus() throws Exception {
        Path projectDir = findProjectDir();
        Path schemaFile = projectDir.resolve("sql/mysql/20260513_dcc_base_schema.sql");
        String schema = Files.readString(schemaFile);

        assertFalse(schema.contains("controlled-file/mine"),
                "DCC schema must not seed the retired personal-file menu path");
        assertFalse(schema.contains("dcc/controlled-file/mine/index"),
                "DCC schema must not seed the retired personal-file menu component");
        assertFalse(schema.contains("DccControlledFileMine"),
                "DCC schema must not seed the retired personal-file route name");
        assertTrue(schema.contains("controlled-file/approval-tasks"),
                "DCC schema must seed the approval-task menu path");
        assertTrue(schema.contains("dcc/controlled-file/approval-tasks/index"),
                "DCC schema must seed the approval-task menu component");
        assertTrue(schema.contains("controlled-file/training-mine"),
                "DCC schema must seed the my-training menu path");
        assertTrue(schema.contains("dcc/controlled-file/training/mine/index"),
                "DCC schema must seed the my-training menu component");
    }

    @Test
    void mysqlSchemaShouldNotSeedStandalonePreviewPermissionMenu() throws Exception {
        Path projectDir = findProjectDir();
        Path baseSchemaFile = projectDir.resolve("sql/mysql/20260513_dcc_base_schema.sql");
        Path migrationFile = projectDir.resolve("sql/mysql/20260626_dcc_merge_view_preview_permission.sql");

        String baseSchema = Files.readString(baseSchemaFile);
        assertFalse(baseSchema.contains("dcc:controlled-file:preview"),
                "DCC base schema must not keep a standalone preview permission seed");

        assertTrue(Files.exists(migrationFile), "DCC preview-permission merge migration must exist");
        String migrationSchema = Files.readString(migrationFile);
        assertSchemaIsNonDestructive(migrationSchema, "preview permission merge");
        assertTrue(migrationSchema.contains("`permission` = 'dcc:controlled-file:preview'"),
                "DCC preview merge migration must target the legacy preview permission row");
        assertTrue(migrationSchema.contains("`deleted` = b'1'"),
                "DCC preview merge migration must retire the obsolete preview menu");
    }

    @Test
    void mysqlSchemaShouldSeedGlobalBasicDataSubMenusAndProjectCodeTables() throws Exception {
        Path projectDir = findProjectDir();
        String schema = readDccRuntimeSchema(projectDir);

        assertNotNull(findCreateBlock(schema, "dcc_project_code"),
                "DCC schema must create the project-code base data table");
        assertNotNull(findCreateBlock(schema, "dcc_project_code_import_batch"),
                "DCC schema must create the project-code import batch table");
        assertNotNull(findCreateBlock(schema, "dcc_project_code_import_row"),
                "DCC schema must create the project-code import row table");
        assertSchemaHasColumns(schema, "dcc_project_code", List.of(
                "doc_control_no", "project_name", "project_code", "category",
                "commissioned_production", "project_leader", "project_engineer",
                "storage_location", "priority", "status", "last_import_batch_id"));
        assertNotNull(findCreateBlock(schema, "dcc_project_code_alias_mapping"),
                "DCC schema must create the project-code alias mapping table");
        assertSchemaHasColumns(schema, "dcc_project_code_alias_mapping", List.of(
                "project_code_id", "alias_text", "normalized_alias_text", "alias_source", "status", "active"));
        assertUniqueKeyColumns(schema, "dcc_project_code", "uk_dcc_project_code_tenant_project",
                List.of("tenant_id", "project_name", "project_code"), "runtime");

        assertTrue(schema.contains("/mdm"),
                "DCC schema must mount basic-data pages under the global /mdm menu");
        assertTrue(schema.contains("DCC项目代码"),
                "DCC schema must seed the project-code submenu name");
        assertTrue(schema.contains("DCC产品目录"),
                "DCC schema must seed the product-catalog submenu name");
        assertTrue(schema.contains("project-code"),
                "DCC schema must seed the project-code submenu path");
        assertTrue(schema.contains("product-catalog"),
                "DCC schema must seed the product-catalog submenu path");
        assertTrue(schema.contains("dcc/controlled-file/basic-data/project-code/index"),
                "DCC schema must seed the project-code submenu component");
        assertTrue(schema.contains("dcc/controlled-file/basic-data/product-catalog/index"),
                "DCC schema must seed the product-catalog submenu component");
        assertTrue(schema.contains("SET @dcc_project_code_menu_id"),
                "DCC basic-data button seed must resolve the page id before assigning button parents");
        assertTrue(schema.contains("`parent_id` = @dcc_project_code_menu_id"),
                "DCC basic-data button seed must repair existing permission rows to the project-code page");
        assertTrue(schema.contains("dcc:project-code:query"),
                "DCC schema must seed query permission");
        assertTrue(schema.contains("dcc:project-code:import"),
                "DCC schema must seed import permission");
        assertTrue(schema.contains("dcc:project-code:export"),
                "DCC schema must seed export permission");
        assertTrue(schema.contains("SET @dcc_global_basic_data_menu_id := (")
                        || schema.contains("SET @mdm_basic_data_menu_id := ("),
                "DCC schema must resolve the global basic-data parent menu id before wiring child pages");
        assertTrue(schema.contains("FROM `system_role_menu` src"),
                "DCC basic-data migration must mirror existing DCC role coverage");
        assertTrue(schema.contains("source_menu.`path` = 'controlled-file/categories'"),
                "DCC basic-data migration must mirror role coverage from the existing DCC base-maintenance page");
        assertFalse(schema.contains("src.`menu_id` = 6803"),
                "DCC basic-data role migration must resolve the source menu by path instead of hard-coding menu id");
    }

    @Test
    void mysqlSchemaShouldSupportProductOnboardingAndProjectMdmBinding() throws Exception {
        Path projectDir = findProjectDir();
        String schema = readDccRuntimeSchema(projectDir);
        String testSchema = Files.readString(projectDir.resolve(
                "yudao-module-dcc/src/test/resources/sql/create_tables.sql"));

        assertSchemaHasColumns(schema, "dcc_project_code", List.of("product_master_id"));
        assertNotNull(findCreateBlock(schema, "dcc_product_onboarding_request"),
                "DCC schema must create product onboarding request table");
        assertSchemaHasColumns(schema, "dcc_product_onboarding_request", List.of(
                "product_master_id", "product_code", "dcc_product_code", "product_name_cn",
                "project_name", "project_code", "status", "applicant_user_id", "approver_user_id",
                "approved_time", "generated_project_code_id", "reject_reason"));
        assertTrue(schema.contains("idx_dcc_product_onboarding_status"),
                "DCC onboarding requests must be queryable by tenant and approval status");
        assertTrue(schema.contains("uk_dcc_product_onboarding_pending_project"),
                "DCC onboarding requests must block duplicate pending project codes");

        assertSchemaHasColumns(testSchema, "dcc_project_code", List.of("product_master_id"));
        assertNotNull(findCreateBlock(testSchema, "dcc_product_onboarding_request"),
                "DCC test schema must create product onboarding request table");
    }

    @Test
    void mysqlSchemaShouldCreateProjectCodeAssignmentAuditTablesAndMenus() throws Exception {
        Path projectDir = findProjectDir();
        Path migrationFile = projectDir.resolve("sql/mysql/20260712_dcc_project_code_assignment_audit.sql");

        assertTrue(Files.exists(migrationFile), "DCC project-code assignment audit migration must exist");

        String schema = readDccRuntimeSchema(projectDir);
        String migrationSchema = Files.readString(migrationFile);
        assertSchemaIsNonDestructive(migrationSchema, "project-code assignment audit");

        assertNotNull(findCreateBlock(schema, "dcc_project_code_assignment"),
                "DCC schema must create assignment task table");
        assertSchemaHasColumns(schema, "dcc_project_code_assignment", List.of(
                "assignment_no", "project_code_id", "scope_mode", "assignee_user_id",
                "assigned_by", "assigned_time", "expire_time", "status", "assignment_reason",
                "file_count", "changed_file_count", "changed_field_count", "revoked_by",
                "revoked_time", "revoke_reason"));
        assertNotNull(findCreateBlock(schema, "dcc_project_code_assignment_file"),
                "DCC schema must create assignment file snapshot table");
        assertSchemaHasColumns(schema, "dcc_project_code_assignment_file", List.of(
                "assignment_id", "project_code_id", "controlled_file_id", "master_id",
                "file_number_snapshot", "file_name_snapshot", "category_id_snapshot",
                "directory_id_snapshot", "initial_file_type_level1", "initial_file_type_level2",
                "initial_file_type_level3", "initial_file_type_level4", "initial_file_type_level5",
                "changed", "changed_field_count", "last_changed_time"));
        assertNotNull(findCreateBlock(schema, "dcc_controlled_file_metadata_change"),
                "DCC schema must create controlled-file metadata change group table");
        assertSchemaHasColumns(schema, "dcc_controlled_file_metadata_change", List.of(
                "assignment_id", "project_code_id", "controlled_file_id", "master_id",
                "operator_user_id", "source", "request_id", "change_reason",
                "changed_field_count", "before_snapshot_json", "after_snapshot_json", "changed_time"));
        assertNotNull(findCreateBlock(schema, "dcc_controlled_file_metadata_change_item"),
                "DCC schema must create controlled-file metadata field change item table");
        assertSchemaHasColumns(schema, "dcc_controlled_file_metadata_change_item", List.of(
                "change_id", "assignment_id", "project_code_id", "controlled_file_id",
                "operator_user_id", "field_name", "field_label", "old_value_text",
                "new_value_text", "old_value_json", "new_value_json", "changed_time"));

        for (String permission : List.of(
                "dcc:project-code-assignment:assign",
                "dcc:project-code-assignment:query",
                "dcc:project-code-assignment:revoke",
                "dcc:project-code-assignment:execute",
                "dcc:project-code-assignment:audit:query")) {
            assertTrue(schema.contains(permission), "DCC schema must seed permission " + permission);
        }
        assertTrue(schema.contains("controlled-file/project-code-assignments/mine"),
                "DCC schema must seed my project-code assignment menu path");
        assertTrue(schema.contains("dcc/controlled-file/project-code-assignments/mine/index"),
                "DCC schema must seed my project-code assignment component");
        assertTrue(schema.contains("controlled-file/project-code-assignment-audit"),
                "DCC schema must seed project-code assignment audit menu path");
        assertTrue(schema.contains("dcc/controlled-file/project-code-assignment-audit/index"),
                "DCC schema must seed project-code assignment audit component");
        assertTrue(schema.contains("source_menu.`path` = 'controlled-file/categories'"),
                "DCC assignment migration must mirror doc-control role coverage from resolved DCC page");
        assertTrue(schema.contains("role_admin.`code` IN ('super_admin', 'doc_control', 'wenkong')"),
                "DCC assignment admin and audit permissions must be limited to document-control roles");
        assertFalse(schema.contains("src.`menu_id` = 6803"),
                "DCC assignment role migration must not hard-code runtime DCC menu ids");
    }

    @Test
    void mysqlSchemaShouldRepairProjectCodeAssignmentAssigneeMenuVisibility() throws Exception {
        Path projectDir = findProjectDir();
        Path migrationFile = projectDir.resolve("sql/mysql/20260713_dcc_project_code_assignment_assignee_menu_repair.sql");

        assertTrue(Files.exists(migrationFile), "DCC project-code assignment assignee menu repair migration must exist");

        String migrationSchema = Files.readString(migrationFile);
        assertSchemaIsNonDestructive(migrationSchema, "project-code assignment assignee menu repair");
        assertTrue(migrationSchema.contains("dcc_project_code_assignment"),
                "Repair migration must use real assignment rows to repair existing assignee role menus");
        assertTrue(migrationSchema.contains("system_user_role"),
                "Repair migration must resolve assigned users' current roles");
        assertTrue(migrationSchema.contains("controlled-file/project-code-assignments/mine"),
                "Repair migration must grant the controlled assignee task entry");
        assertTrue(migrationSchema.contains("root_menu.`path` = '/dcc'"),
                "Repair migration must also grant the DCC root menu so the child route is visible");
        assertTrue(migrationSchema.contains("parent_menu.`path` = '/mdm'"),
                "Repair migration must repair the DCC project-code parent tab for roles that already have the leaf");
        assertFalse(migrationSchema.contains("dcc:project-code:create"),
                "Repair migration must not grant project-code management permissions to assignees");
        assertFalse(migrationSchema.contains("dcc:project-code:update"),
                "Repair migration must not grant project-code edit permissions to assignees");
        assertFalse(migrationSchema.contains("dcc:project-code:delete"),
                "Repair migration must not grant project-code delete permissions to assignees");
    }

    @Test
    void mysqlSchemaShouldSeedReadableTrainingMineMenuAndRestoreRoleCoverage() throws Exception {
        Path projectDir = findProjectDir();
        Path schemaFile = projectDir.resolve("sql/mysql/20260513_dcc_base_schema.sql");
        String schema = Files.readString(schemaFile);

        assertTrue(schema.contains("SELECT '我的培训', 'dcc:controlled-file:training:mine'"),
                "DCC base schema must seed my-training menu with readable Chinese copy");
        assertFalse(schema.contains("DCC鎴戠殑鍩硅"),
                "DCC base schema must not contain mojibake for the my-training menu");
        assertFalse(schema.contains("WHERE `id` = 6816 OR `path` = 'controlled-file/training-mine'"),
                "DCC my-training menu seed must not be blocked by an unrelated runtime menu id collision");

        Path closedLoopMenuFile = projectDir.resolve("sql/mysql/20260516_dcc_training_closed_loop_menu.sql");
        assertTrue(Files.exists(closedLoopMenuFile), "DCC closed-loop training menu seed must exist");
        String closedLoopMenuSchema = Files.readString(closedLoopMenuFile);
        assertSchemaIsNonDestructive(closedLoopMenuSchema, "closed-loop training menu seed");
        assertTrue(closedLoopMenuSchema.contains("SELECT '我的培训', 'dcc:controlled-file:training:mine'"),
                "DCC closed-loop menu seed must use readable Chinese copy");
        assertFalse(closedLoopMenuSchema.contains("`id`, `name`, `permission`"),
                "DCC closed-loop menu seed must not hard-code a runtime menu id");
        assertFalse(closedLoopMenuSchema.contains("WHERE `id` = 6816 OR `path` = 'controlled-file/training-mine'"),
                "DCC closed-loop menu seed must not be blocked by an unrelated runtime menu id collision");

        Path restoreFile = projectDir.resolve("sql/mysql/20260529_dcc_training_mine_menu_restore.sql");
        assertTrue(Files.exists(restoreFile), "DCC my-training menu restore migration must exist");

        String restoreSchema = Files.readString(restoreFile);
        assertSchemaIsNonDestructive(restoreSchema, "my-training menu restore");
        assertTrue(restoreSchema.contains("controlled-file/training-mine"),
                "DCC my-training restore must insert the menu path");
        assertTrue(restoreSchema.contains("dcc:controlled-file:training:mine"),
                "DCC my-training restore must insert the permission");
        assertTrue(restoreSchema.contains("FROM `system_role_menu` src"),
                "DCC my-training restore must mirror existing DCC role coverage");
        assertFalse(restoreSchema.contains("src.`menu_id` = 6809"),
                "DCC my-training restore must not hard-code the runtime DCC training menu id");
        assertTrue(restoreSchema.contains("training_menu.`path` = 'controlled-file/training'"),
                "DCC my-training restore must mirror from the DCC training menu resolved by path");
        assertTrue(restoreSchema.contains("src.`menu_id` = training_menu.`id`"),
                "DCC my-training restore must mirror role coverage from the resolved DCC training menu id");
        assertTrue(restoreSchema.contains("mine_menu.`path` = 'controlled-file/training-mine'"),
                "DCC my-training restore must resolve the runtime menu id by path");
        assertTrue(restoreSchema.contains("existing.`menu_id` = mine_menu.`id`"),
                "DCC my-training restore must be idempotent per resolved role/menu pair");
    }

    @Test
    void mysqlRuntimeRepairSchemaShouldUpgradeLegacyDccTables() throws Exception {
        Path projectDir = findProjectDir();
        Path repairSchemaFile = projectDir.resolve("sql/mysql/20260515_dcc_runtime_schema_repair.sql");

        assertTrue(Files.exists(repairSchemaFile), "DCC runtime repair schema file must exist");

        String schema = Files.readString(repairSchemaFile);
        assertSchemaIsNonDestructive(schema, "runtime repair");
        assertTrue(schema.contains("CREATE PROCEDURE ensure_dcc_column"),
                "DCC runtime repair schema must use an idempotent column-repair helper");
        assertTrue(schema.contains("CALL ensure_dcc_column("),
                "DCC runtime repair schema must patch legacy runtime columns through the helper");
        assertTrue(schema.contains("'dcc_file_category'"),
                "DCC runtime repair schema must patch legacy dcc_file_category columns");
        assertTrue(schema.contains("'lifecycle_stage'"),
                "DCC runtime repair schema must patch DCC category lifecycle stage");
        assertTrue(schema.contains("DCC_FVM_DHF_001") && schema.contains("DCC_FVM_DMR_%")
                        && schema.contains("DCC_OTHER_TEMPLATE_%"),
                "DCC runtime repair schema must explicitly backfill known DHF, DMR, and OTHER category stages");
        assertTrue(schema.contains("SIGNAL SQLSTATE '45000'") && schema.contains("lifecycle_stage"),
                "DCC runtime repair schema must fail fast when category stage mapping is incomplete");
        assertTrue(schema.contains("ensure_dcc_category_lifecycle_stage_not_null_ready")
                        && schema.contains("DCC category lifecycle_stage not-null normalization incomplete"),
                "DCC runtime repair schema must fail fast before NOT NULL when any category stage remains blank");
        assertTrue(schema.contains("'description'"),
                "DCC runtime repair schema must add the category description column");
        assertTrue(schema.contains("'dcc_controlled_file'"),
                "DCC runtime repair schema must patch legacy dcc_controlled_file columns");
        assertTrue(schema.contains("'master_id'"),
                "DCC runtime repair schema must add the controlled-file master reference");
        assertTrue(Pattern.compile(
                        "ensure_dcc_column\\s*\\(\\s*'dcc_registration_certificate_version'\\s*,\\s*'remark'",
                        Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(schema).find(),
                "DCC runtime repair schema must patch registration-certificate remark through ensure_dcc_column");
        assertTrue(schema.contains("'dcc_category_approval_route_node'"),
                "DCC runtime repair schema must patch legacy route-node columns");
        assertTrue(schema.contains("'stage_type'"),
                "DCC runtime repair schema must add the route-node stage_type metadata column");
        assertTrue(schema.contains("'subject_label'"),
                "DCC runtime repair schema must add the route-node subject_label metadata column");
        assertTrue(schema.contains("'marker'"),
                "DCC runtime repair schema must add the route-node marker metadata column");
        assertTrue(schema.contains("'subject_type'"),
                "DCC runtime repair schema must add the route-node subject_type metadata column");
        assertTrue(schema.contains("'subject_id'"),
                "DCC runtime repair schema must add the route-node subject_id metadata column");
        assertTrue(schema.contains("'subject_name'"),
                "DCC runtime repair schema must add the route-node subject_name metadata column");
        assertTrue(schema.contains("'subject_department_path'"),
                "DCC runtime repair schema must add the route-node subject_department_path metadata column");
        assertTrue(schema.contains("'rule_remark'"),
                "DCC runtime repair schema must add the route-node rule_remark metadata column");
        assertTrue(schema.contains("'dcc_controlled_file_route_snapshot'"),
                "DCC runtime repair schema must patch legacy route-snapshot columns");
        assertTrue(schema.contains("CREATE TABLE IF NOT EXISTS `dcc_controlled_file_master`"),
                "DCC runtime repair schema must create the controlled-file master table when absent");
        assertTrue(schema.contains("CREATE TABLE IF NOT EXISTS `dcc_file_category_permission_rule`"),
                "DCC runtime repair schema must create missing category permission-rule tables");
        assertTrue(schema.contains("CREATE TABLE IF NOT EXISTS `dcc_controlled_file_signature`"),
                "DCC runtime repair schema must create missing signature and lifecycle tables");
        assertTrue(schema.contains("CALL ensure_dcc_column("),
                "DCC runtime repair schema must keep using the idempotent column helper");
        assertTrue(schema.contains("'dcc_file_category_distribution_rule'"),
                "DCC runtime repair schema must patch category distribution-rule columns");
        assertTrue(schema.contains("'distribution_medium'"),
                "DCC runtime repair schema must add distribution medium columns");
        assertTrue(schema.contains("CREATE TABLE IF NOT EXISTS `dcc_controlled_file_training_progress`"),
                "DCC runtime repair schema must create the training-progress table when absent");
        assertTrue(schema.contains("CREATE TABLE IF NOT EXISTS `dcc_controlled_file_training_view_session`"),
                "DCC runtime repair schema must create the training-view-session table when absent");
    }

    @Test
    void mysqlRuntimeRepairSchemaShouldUndeleteReferencedControlledFiles() throws Exception {
        Path projectDir = findProjectDir();
        Path repairSchemaFile = projectDir.resolve("sql/mysql/20260515_dcc_runtime_schema_repair.sql");

        assertTrue(Files.exists(repairSchemaFile), "DCC runtime repair schema file must exist");

        String schema = Files.readString(repairSchemaFile);
        assertTrue(schema.contains("UPDATE `dcc_controlled_file`"),
                "DCC runtime repair schema must patch legacy controlled-file runtime data");
        assertTrue(schema.contains("SET `deleted` = 0"),
                "DCC runtime repair schema must restore logically deleted controlled-file records");
        assertTrue(schema.contains("`process_definition_key` = 'dcc-controlled-file-approval'"),
                "DCC runtime repair schema must scope undelete repair to DCC workflow records");
    }

    @Test
    void mysqlSchemaShouldSupportControlledFileProductName() throws Exception {
        Path projectDir = findProjectDir();
        Path baseSchemaFile = projectDir.resolve("sql/mysql/20260513_dcc_base_schema.sql");
        Path repairSchemaFile = projectDir.resolve("sql/mysql/20260515_dcc_runtime_schema_repair.sql");
        Path migrationFile = projectDir.resolve("sql/mysql/20260604_dcc_controlled_file_product_name.sql");
        Path testSchemaFile = projectDir.resolve("yudao-module-dcc/src/test/resources/sql/create_tables.sql");

        assertTrue(Files.exists(migrationFile), "DCC product_name migration must exist");

        String baseSchema = Files.readString(baseSchemaFile);
        assertSchemaHasColumns(baseSchema, "dcc_controlled_file", List.of("product_name"));

        String repairSchema = Files.readString(repairSchemaFile);
        assertSchemaIsNonDestructive(repairSchema, "runtime repair");
        assertTrue(Pattern.compile(
                        "ensure_dcc_column\\s*\\(\\s*'dcc_controlled_file'\\s*,\\s*'product_name'",
                        Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(repairSchema).find(),
                "DCC runtime repair schema must patch product_name through ensure_dcc_column");

        String migrationSchema = Files.readString(migrationFile);
        assertSchemaIsNonDestructive(migrationSchema, "controlled file product name");
        assertTrue(Pattern.compile(
                        "ALTER\\s+TABLE\\s+`dcc_controlled_file`\\s+ADD\\s+COLUMN\\s+`product_name`",
                        Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(migrationSchema).find(),
                "DCC product_name migration must add dcc_controlled_file.product_name");

        String testSchema = Files.readString(testSchemaFile);
        assertSchemaHasColumns(testSchema, "dcc_controlled_file", List.of("product_name"));
    }

    @Test
    void mysqlSchemaShouldSupportProjectCodeRecognitionLink() throws Exception {
        Path projectDir = findProjectDir();
        Path baseSchemaFile = projectDir.resolve("sql/mysql/20260513_dcc_base_schema.sql");
        Path migrationFile = projectDir.resolve("sql/mysql/20260618_dcc_project_code_recognition_link.sql");
        Path testSchemaFile = projectDir.resolve("yudao-module-dcc/src/test/resources/sql/create_tables.sql");

        assertTrue(Files.exists(migrationFile), "DCC project-code recognition link migration must exist");

        List<String> expectedColumns = List.of(
                "dcc_project_code_id",
                "project_code_recognition_type",
                "project_code_recognition_text",
                "project_code_recognized_by",
                "project_code_recognized_time");

        String baseSchema = Files.readString(baseSchemaFile);
        assertSchemaHasColumns(baseSchema, "dcc_controlled_file", expectedColumns);
        assertTrue(baseSchema.contains("idx_dcc_controlled_file_project_code"),
                "DCC base schema must index tenant/project-code reverse lookup");

        String migrationSchema = Files.readString(migrationFile);
        assertSchemaIsNonDestructive(migrationSchema, "project-code recognition link");
        for (String column : expectedColumns) {
            assertTrue(Pattern.compile(
                            "ensure_dcc_column\\s*\\(\\s*'dcc_controlled_file'\\s*,\\s*'"
                                    + Pattern.quote(column) + "'",
                            Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(migrationSchema).find(),
                    "DCC project-code link migration must patch " + column + " through ensure_dcc_column");
        }
        assertTrue(Pattern.compile(
                        "ensure_dcc_index\\s*\\(\\s*'dcc_controlled_file'\\s*,\\s*'idx_dcc_controlled_file_project_code'",
                        Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(migrationSchema).find(),
                "DCC project-code link migration must patch reverse lookup index through ensure_dcc_index");
        assertTrue(migrationSchema.contains("`tenant_id`, `dcc_project_code_id`"),
                "DCC project-code reverse lookup index must be tenant scoped");

        String testSchema = Files.readString(testSchemaFile);
        assertSchemaHasColumns(testSchema, "dcc_controlled_file", expectedColumns);
        assertTrue(testSchema.contains("idx_dcc_controlled_file_project_code"),
                "DCC test schema must include project-code reverse lookup index");
    }

    @Test
    void mysqlSchemaShouldIncludeDccBrowserPerformanceIndexes() throws Exception {
        Path projectDir = findProjectDir();
        Path migrationFile = projectDir.resolve("sql/mysql/20260617_dcc_browser_performance_indexes.sql");

        assertTrue(Files.exists(migrationFile), "DCC browser performance index migration must exist");

        String migrationSchema = Files.readString(migrationFile);
        assertSchemaIsNonDestructive(migrationSchema, "DCC browser performance indexes");
        assertTrue(migrationSchema.contains("idx_dcc_directory_lazy_parent"),
                "DCC directory lazy loading migration must add parent lookup index");
        assertTrue(migrationSchema.contains("idx_dcc_controlled_file_browser_directory"),
                "DCC browser migration must add directory/status/create_time index");
        assertTrue(migrationSchema.contains("idx_dcc_controlled_file_browser_master"),
                "DCC browser migration must add master/version summary index");
    }

    @Test
    void mysqlSchemaShouldUseBinaryCollationForExactNasIdentifiers() throws Exception {
        Path projectDir = findProjectDir();
        Path baseSchemaFile = projectDir.resolve("sql/mysql/20260513_dcc_base_schema.sql");
        Path transferSchemaFile = projectDir.resolve("sql/mysql/20260523_dcc_nas_transfer_task.sql");
        Path repairSchemaFile = projectDir.resolve("sql/mysql/20260515_dcc_runtime_schema_repair.sql");
        Path migrationFile = projectDir.resolve("sql/mysql/20260530_dcc_exact_nas_identifier_collation.sql");
        Path testSchemaFile = projectDir.resolve("yudao-module-dcc/src/test/resources/sql/create_tables.sql");

        assertTrue(Files.exists(migrationFile), "DCC exact NAS identifier collation migration must exist");

        String baseSchema = Files.readString(baseSchemaFile);
        assertColumnUsesBinaryCollation(baseSchema, "dcc_controlled_file_master", "file_name", "base");
        assertColumnUsesBinaryCollation(baseSchema, "dcc_controlled_file", "file_name", "base");
        assertColumnUsesBinaryCollation(baseSchema, "dcc_controlled_file_nas_transfer_task_item", "nas_path",
                "base");

        String transferSchema = Files.readString(transferSchemaFile);
        assertColumnUsesBinaryCollation(transferSchema, "dcc_controlled_file_nas_transfer_task_item", "nas_path",
                "NAS transfer");

        String repairSchema = Files.readString(repairSchemaFile);
        assertColumnUsesBinaryCollation(repairSchema, "dcc_controlled_file_master", "file_name", "runtime repair");
        assertTrue(Pattern.compile(
                        "ADD\\s+COLUMN\\s+`file_name`\\s+varchar\\(256\\)\\s+CHARACTER\\s+SET\\s+utf8mb4\\s+"
                                + "COLLATE\\s+utf8mb4_bin",
                        Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(repairSchema).find(),
                "DCC runtime repair schema must add controlled-file file_name with binary collation");

        String migrationSchema = Files.readString(migrationFile);
        assertSchemaIsNonDestructive(migrationSchema, "exact NAS identifier collation");
        assertModifyColumnUsesBinaryCollation(migrationSchema, "dcc_controlled_file_master", "file_name",
                "exact NAS identifier collation");
        assertModifyColumnUsesBinaryCollation(migrationSchema, "dcc_controlled_file", "file_name",
                "exact NAS identifier collation");
        assertModifyColumnUsesBinaryCollation(migrationSchema, "dcc_controlled_file_nas_transfer_task_item",
                "nas_path", "exact NAS identifier collation");

        String testSchema = Files.readString(testSchemaFile);
        assertColumnUsesBinaryCollation(testSchema, "dcc_controlled_file_master", "file_name", "test");
        assertColumnUsesBinaryCollation(testSchema, "dcc_controlled_file", "file_name", "test");
        assertColumnUsesBinaryCollation(testSchema, "dcc_controlled_file_nas_transfer_task_item", "nas_path",
                "test");
    }

    @Test
    void mysqlSchemaShouldSupportLocalFolderImportSourceFields() throws Exception {
        Path projectDir = findProjectDir();
        Path migrationFile = projectDir.resolve("sql/mysql/20260613_dcc_nas_local_folder_import.sql");
        Path testSchemaFile = projectDir.resolve("yudao-module-dcc/src/test/resources/sql/create_tables.sql");

        assertTrue(Files.exists(migrationFile), "DCC local folder import migration must exist");

        String runtimeSchema = readDccRuntimeSchema(projectDir);
        assertSchemaIsNonDestructive(runtimeSchema, "local folder import runtime");
        assertSchemaHasColumns(runtimeSchema, "dcc_controlled_file_nas_transfer_task", List.of("source_type"));
        assertSchemaHasColumns(runtimeSchema, "dcc_controlled_file_nas_transfer_task_item", List.of("source_file_id"));
        assertTrue(runtimeSchema.contains("DEFAULT 'NAS'"),
                "DCC local folder migration must default existing transfer tasks to NAS source");
        assertTrue(runtimeSchema.contains("LOCAL_FOLDER"),
                "DCC local folder migration must document LOCAL_FOLDER source type");

        String testSchema = Files.readString(testSchemaFile);
        assertSchemaHasColumns(testSchema, "dcc_controlled_file_nas_transfer_task", List.of("source_type"));
        assertSchemaHasColumns(testSchema, "dcc_controlled_file_nas_transfer_task_item", List.of("source_file_id"));
    }

    @Test
    void mysqlSchemaShouldPersistDccProjectCodeForNasTransferTasks() throws Exception {
        Path projectDir = findProjectDir();
        Path migrationFile = projectDir.resolve("sql/mysql/20260728_dcc_nas_transfer_project_code.sql");
        Path baseSchemaFile = projectDir.resolve("sql/mysql/20260513_dcc_base_schema.sql");
        Path testSchemaFile = projectDir.resolve("yudao-module-dcc/src/test/resources/sql/create_tables.sql");

        assertTrue(Files.exists(migrationFile), "DCC NAS project-code migration must exist");
        String migrationSchema = Files.readString(migrationFile);
        assertSchemaIsNonDestructive(migrationSchema, "DCC NAS project-code migration");
        assertSchemaHasColumns(migrationSchema, "dcc_controlled_file_nas_transfer_task",
                List.of("dcc_project_code_id"));
        assertSchemaHasColumns(Files.readString(baseSchemaFile),
                "dcc_controlled_file_nas_transfer_task", List.of("dcc_project_code_id"));
        assertSchemaHasColumns(Files.readString(testSchemaFile),
                "dcc_controlled_file_nas_transfer_task", List.of("dcc_project_code_id"));
    }

    @Test
    void mysqlSchemaShouldSupportLocalFolderImportUploadProgressFields() throws Exception {
        Path projectDir = findProjectDir();
        Path migrationFile = projectDir.resolve("sql/mysql/20260614_dcc_nas_local_folder_large_import.sql");
        Path testSchemaFile = projectDir.resolve("yudao-module-dcc/src/test/resources/sql/create_tables.sql");

        assertTrue(Files.exists(migrationFile), "DCC local folder large import migration must exist");

        String runtimeSchema = readDccRuntimeSchema(projectDir);
        assertSchemaIsNonDestructive(runtimeSchema, "local folder large import runtime");
        assertSchemaHasColumns(runtimeSchema, "dcc_controlled_file_nas_transfer_task",
                List.of("expected_file_count", "expected_total_bytes", "uploaded_file_count",
                        "uploaded_total_bytes", "upload_completed_at"));

        String testSchema = Files.readString(testSchemaFile);
        assertSchemaHasColumns(testSchema, "dcc_controlled_file_nas_transfer_task",
                List.of("expected_file_count", "expected_total_bytes", "uploaded_file_count",
                        "uploaded_total_bytes", "upload_completed_at"));
    }

    @Test
    void mysqlSchemaShouldSupportDccBatchRecognitionTask() throws Exception {
        Path projectDir = findProjectDir();
        Path migrationFile = projectDir.resolve("sql/mysql/20260623_dcc_browser_batch_recognition_task.sql");
        Path fileCategoryMigrationFile = projectDir.resolve("sql/mysql/20260710_dcc_file_category_batch_task.sql");
        Path activeTaskUniqueGuardMigrationFile = projectDir.resolve(
                "sql/mysql/20260710_dcc_batch_recognition_active_task_unique_guard.sql");
        Path testSchemaFile = projectDir.resolve("yudao-module-dcc/src/test/resources/sql/create_tables.sql");

        assertTrue(Files.exists(migrationFile), "DCC batch recognition migration must exist");
        assertTrue(Files.exists(fileCategoryMigrationFile), "DCC file category batch task migration must exist");
        assertTrue(Files.exists(activeTaskUniqueGuardMigrationFile),
                "DCC batch recognition active task unique guard migration must exist");

        String migrationSchema = Files.readString(migrationFile);
        assertSchemaIsNonDestructive(migrationSchema, "DCC batch recognition task");
        assertTrue(migrationSchema.contains("CREATE TABLE IF NOT EXISTS `dcc_controlled_file_batch_recognition_task`"),
                "DCC batch recognition migration must create the task table");
        assertSchemaHasColumns(migrationSchema, "dcc_controlled_file_batch_recognition_task",
                List.of("operator_user_id", "scope_type", "candidate_ids_json", "status",
                        "total_count", "processed_count", "success_count", "failed_count",
                        "skipped_existing_count", "remaining_count", "worker_count"));
        String fileCategoryMigrationSchema = Files.readString(fileCategoryMigrationFile);
        assertSchemaIsNonDestructive(fileCategoryMigrationSchema, "DCC file category batch task");
        assertSchemaHasColumns(fileCategoryMigrationSchema, "dcc_controlled_file_batch_recognition_task",
                List.of("recognition_type", "unclassified_count", "ambiguous_count", "conflict_count"));
        String activeTaskUniqueGuardMigrationSchema = Files.readString(activeTaskUniqueGuardMigrationFile);
        assertSchemaIsNonDestructive(activeTaskUniqueGuardMigrationSchema,
                "DCC batch recognition active task unique guard");
        assertSchemaHasColumns(activeTaskUniqueGuardMigrationSchema,
                "dcc_controlled_file_batch_recognition_task", List.of("active_recognition_type"));
        assertTrue(activeTaskUniqueGuardMigrationSchema.contains(
                        "uk_dcc_batch_recognition_task_active_type"),
                "DCC batch recognition active task unique index must exist");

        String testSchema = Files.readString(testSchemaFile);
        assertSchemaHasColumns(testSchema, "dcc_controlled_file_batch_recognition_task",
                List.of("operator_user_id", "scope_type", "candidate_ids_json", "status",
                        "total_count", "processed_count", "success_count", "failed_count",
                        "skipped_existing_count", "remaining_count", "worker_count",
                        "recognition_type", "unclassified_count", "ambiguous_count", "conflict_count",
                        "active_recognition_type"));
        assertTrue(testSchema.contains("uk_dcc_batch_recognition_task_active_type"),
                "DCC batch recognition test schema must include active task unique index");
        assertSchemaHasColumns(testSchema, "dcc_controlled_file_recognition_record",
                List.of("controlled_file_id", "recognition_scope", "recognition_version",
                        "status", "batch_task_id"));
    }

    @Test
    void mysqlSchemaShouldSupportDccFileCategoryMatchRules() throws Exception {
        Path projectDir = findProjectDir();
        Path schemaMigrationFile = projectDir.resolve("sql/mysql/20260731_dcc_file_category_match_rule.sql");
        Path seedMigrationFile = projectDir.resolve("sql/mysql/20260731_dcc_file_category_match_rule_seed.sql");
        Path testSchemaFile = projectDir.resolve("yudao-module-dcc/src/test/resources/sql/create_tables.sql");

        assertTrue(Files.exists(schemaMigrationFile), "DCC file category match-rule schema migration must exist");
        assertTrue(Files.exists(seedMigrationFile), "DCC file category match-rule seed migration must exist");

        String schemaMigration = Files.readString(schemaMigrationFile);
        String seedMigration = Files.readString(seedMigrationFile);
        String testSchema = Files.readString(testSchemaFile);

        assertSchemaIsNonDestructive(schemaMigration, "DCC file category match rule");
        assertSchemaHasColumns(schemaMigration, "dcc_file_category_match_rule",
                List.of("category_id", "match_text", "match_type", "weight", "active", "remark"));
        assertTrue(schemaMigration.contains("uk_dcc_file_category_match_rule_unique"),
                "DCC match-rule table must prevent duplicate active rule rows");
        assertTrue(schemaMigration.contains("idx_dcc_file_category_match_rule_category"),
                "DCC match-rule table must index category lookups");
        assertSchemaIsNonDestructive(seedMigration, "DCC file category match rule seed");
        assertTrue(seedMigration.contains("DCC_FILE_CATEGORY_MATCH_RULE_SEED_CATEGORY_MISSING"),
                "DCC match-rule seed must fail fast when required categories are missing");
        assertTrue(seedMigration.contains("DCC_FILE_CATEGORY_MATCH_RULE_SEED_INSERT_INCOMPLETE"),
                "DCC match-rule seed must fail fast when rule insertion is incomplete");
        assertTrue(seedMigration.contains("过程运行确认（OQ）报告"),
                "DCC match-rule seed must include explicit OQ report rules");
        assertTrue(seedMigration.contains("sldprt"),
                "DCC match-rule seed must include SolidWorks part extension rules");
        assertFalse(Pattern.compile("\\bUPDATE\\s+`?dcc_controlled_file\\b", Pattern.CASE_INSENSITIVE)
                        .matcher(seedMigration).find(),
                "DCC match-rule seed must not directly rewrite controlled-file category results");
        assertSchemaHasColumns(testSchema, "dcc_file_category_match_rule",
                List.of("category_id", "match_text", "match_type", "weight", "active", "remark"));
    }

    @Test
    void mysqlSchemaShouldSupportDccRecognitionFileTypeLevels() throws Exception {
        Path projectDir = findProjectDir();
        Path migrationFile = projectDir.resolve("sql/mysql/20260702_dcc_recognition_file_type_levels.sql");
        Path testSchemaFile = projectDir.resolve("yudao-module-dcc/src/test/resources/sql/create_tables.sql");

        assertTrue(Files.exists(migrationFile), "DCC recognition file type level migration must exist");

        String migrationSchema = Files.readString(migrationFile);
        assertSchemaIsNonDestructive(migrationSchema, "DCC recognition file type levels");
        assertSchemaHasColumns(migrationSchema, "dcc_controlled_file",
                List.of("file_type_level1", "file_type_level2", "file_type_level3",
                        "file_type_level4", "file_type_level5"));
        assertSchemaHasColumns(migrationSchema, "dcc_controlled_file_recognition_record",
                List.of("file_type_level1", "file_type_level2", "file_type_level3",
                        "file_type_level4", "file_type_level5"));

        String testSchema = Files.readString(testSchemaFile);
        assertSchemaHasColumns(testSchema, "dcc_controlled_file",
                List.of("file_type_level1", "file_type_level2", "file_type_level3",
                        "file_type_level4", "file_type_level5"));
        assertSchemaHasColumns(testSchema, "dcc_controlled_file_recognition_record",
                List.of("file_type_level1", "file_type_level2", "file_type_level3",
                        "file_type_level4", "file_type_level5"));
    }

    @Test
    void mysqlSchemaShouldSupportDccProjectCodeAliasMappingRecognition() throws Exception {
        Path projectDir = findProjectDir();
        Path migrationFile = projectDir.resolve("sql/mysql/20260703_dcc_project_code_alias_mapping.sql");
        Path baseSchemaFile = projectDir.resolve("sql/mysql/20260513_dcc_base_schema.sql");
        Path testSchemaFile = projectDir.resolve("yudao-module-dcc/src/test/resources/sql/create_tables.sql");

        assertTrue(Files.exists(migrationFile), "DCC project alias mapping migration must exist");

        String migrationSchema = Files.readString(migrationFile);
        String baseSchema = Files.readString(baseSchemaFile);
        String testSchema = Files.readString(testSchemaFile);

        assertSchemaIsNonDestructive(migrationSchema, "DCC project alias mapping");
        for (String schema : List.of(migrationSchema, baseSchema, testSchema)) {
            assertSchemaHasColumns(schema, "dcc_project_code_alias_mapping",
                    List.of("project_code_id", "alias_text", "normalized_alias_text",
                            "alias_source", "status", "active"));
        }
        assertSchemaHasColumns(migrationSchema, "dcc_controlled_file_recognition_record",
                List.of("matched_project_alias_id", "matched_project_alias_text", "matched_project_alias_source"));
        assertSchemaHasColumns(testSchema, "dcc_controlled_file_recognition_record",
                List.of("matched_project_alias_id", "matched_project_alias_text", "matched_project_alias_source"));
    }

    @Test
    void mysqlSchemaShouldRepairDccAiCategoryMenuPermissions() throws Exception {
        Path projectDir = findProjectDir();
        Path migrationFile = projectDir.resolve("sql/mysql/20260707_dcc_ai_category_permission_menu.sql");

        assertTrue(Files.exists(migrationFile), "DCC AI category permission repair migration must exist");

        String migrationSchema = Files.readString(migrationFile);
        assertSchemaIsNonDestructive(migrationSchema, "DCC AI category permission menu repair");
        for (String permission : List.of(
                "dcc:project-code:update",
                "dcc:controlled-file:update"
        )) {
            assertTrue(migrationSchema.contains(permission),
                    "DCC AI category permission migration must seed permission " + permission);
        }
        assertTrue(migrationSchema.contains("@dcc_project_code_menu_id"),
                "DCC AI category permission migration must resolve the project-code page before adding buttons");
        assertTrue(migrationSchema.contains("@dcc_controlled_file_browser_menu_id"),
                "DCC AI category permission migration must resolve the controlled-file browser page before adding buttons");
        assertTrue(migrationSchema.contains("`permission` IN ("),
                "DCC AI category permission migration must repair existing project-code action button parents by permission");
        assertTrue(migrationSchema.contains("source_menu.`path` = 'controlled-file/categories'"),
                "DCC AI category permission migration must mirror existing DCC role coverage from the category maintenance page");
        assertTrue(migrationSchema.contains("target_menu.`permission` IN ("),
                "DCC AI category permission migration must grant repaired permissions through role-menu mirroring");
    }

    @Test
    void mysqlSchemaShouldSupportDccCategoryLifecycleStage() throws Exception {
        Path projectDir = findProjectDir();
        Path baseSchemaFile = projectDir.resolve("sql/mysql/20260513_dcc_base_schema.sql");
        Path repairSchemaFile = projectDir.resolve("sql/mysql/20260515_dcc_runtime_schema_repair.sql");
        Path testSchemaFile = projectDir.resolve("yudao-module-dcc/src/test/resources/sql/create_tables.sql");

        String baseSchema = Files.readString(baseSchemaFile);
        String repairSchema = Files.readString(repairSchemaFile);
        String testSchema = Files.readString(testSchemaFile);

        assertSchemaHasColumns(baseSchema, "dcc_file_category", List.of("lifecycle_stage"));
        assertSchemaHasColumns(repairSchema, "dcc_file_category", List.of("lifecycle_stage"));
        assertSchemaHasColumns(testSchema, "dcc_file_category", List.of("lifecycle_stage"));
        for (String stage : List.of("PLAN", "INPUT", "OUTPUT", "VERIFICATION", "VALIDATION", "TRANSFER")) {
            assertTrue(baseSchema.contains(stage), "DCC base schema must document lifecycle stage " + stage);
            assertTrue(repairSchema.contains(stage), "DCC runtime repair schema must backfill lifecycle stage " + stage);
        }
    }

    @Test
    void mysqlSchemaShouldSupportDccFileTypeTaxonomyConfig() throws Exception {
        Path projectDir = findProjectDir();
        Path migrationFile = projectDir.resolve("sql/mysql/20260719_dcc_file_type_taxonomy.sql");
        Path testSchemaFile = projectDir.resolve("yudao-module-dcc/src/test/resources/sql/create_tables.sql");

        assertTrue(Files.exists(migrationFile), "DCC file type taxonomy migration must exist");

        String migrationSchema = Files.readString(migrationFile);
        assertSchemaIsNonDestructive(migrationSchema, "DCC file type taxonomy config migration");
        assertFalse(migrationSchema.contains("ADD COLUMN IF NOT EXISTS"),
                "DCC file type taxonomy migration must use information_schema + PREPARE for MySQL-compatible idempotent columns");
        assertTrue(migrationSchema.contains("information_schema.COLUMNS"),
                "DCC file type taxonomy migration must check column existence before adding taxonomy references");
        assertTrue(migrationSchema.contains("PREPARE dcc_file_category_taxonomy_column_stmt"),
                "DCC file type taxonomy migration must add reference columns through prepared idempotent DDL");
        assertTrue(migrationSchema.contains("COLLATE utf8mb4_unicode_ci"),
                "DCC file type taxonomy migration must compare Chinese menu names with explicit collation");

        String runtimeSchema = readDccRuntimeSchema(projectDir);
        assertSchemaIsNonDestructive(runtimeSchema, "DCC file type taxonomy config");
        assertTrue(runtimeSchema.contains("CREATE TABLE IF NOT EXISTS `dcc_file_type_taxonomy`"),
                "runtime schema must create DCC file type taxonomy table");
        assertSchemaHasColumns(runtimeSchema, "dcc_file_type_taxonomy",
                List.of("parent_id", "level_no", "code", "name", "active", "sort", "remark"));
        assertSchemaHasColumns(runtimeSchema, "dcc_file_category", List.of("file_type_taxonomy_id"));
        assertSchemaHasColumns(runtimeSchema, "dcc_controlled_file", List.of("file_type_taxonomy_id"));
        assertSchemaHasColumns(runtimeSchema, "dcc_controlled_file_recognition_record",
                List.of("file_type_taxonomy_id"));
        assertTrue(runtimeSchema.contains("uk_dcc_file_type_taxonomy_tenant_code_deleted"),
                "DCC file type taxonomy must enforce tenant-scoped code uniqueness");
        assertTrue(runtimeSchema.contains("uk_dcc_file_type_taxonomy_sibling_name_deleted"),
                "DCC file type taxonomy must prevent duplicate sibling names");
        assertTrue(runtimeSchema.contains("idx_dcc_file_type_taxonomy_parent"),
                "DCC file type taxonomy must index tree queries");
        assertTrue(runtimeSchema.contains("idx_dcc_file_category_taxonomy"),
                "DCC file categories must index taxonomy references");
        assertTrue(runtimeSchema.contains("idx_dcc_controlled_file_taxonomy"),
                "DCC controlled files must index taxonomy references");
        assertTrue(runtimeSchema.contains("idx_dcc_recognition_record_taxonomy"),
                "DCC recognition records must index taxonomy references");

        String testSchema = Files.readString(testSchemaFile);
        assertTrue(testSchema.contains("CREATE TABLE IF NOT EXISTS `dcc_file_type_taxonomy`"),
                "DCC test schema must include file type taxonomy table");
        assertSchemaHasColumns(testSchema, "dcc_file_type_taxonomy",
                List.of("parent_id", "level_no", "code", "name", "active", "sort", "remark"));
        assertSchemaHasColumns(testSchema, "dcc_file_category", List.of("file_type_taxonomy_id"));
        assertSchemaHasColumns(testSchema, "dcc_controlled_file", List.of("file_type_taxonomy_id"));
        assertSchemaHasColumns(testSchema, "dcc_controlled_file_recognition_record",
                List.of("file_type_taxonomy_id"));
        assertTrue(testSchema.contains("idx_dcc_file_type_taxonomy_parent"),
                "DCC test schema must index taxonomy tree queries");
        assertTrue(testSchema.contains("idx_dcc_file_category_taxonomy"),
                "DCC test schema must index category taxonomy references");
        assertTrue(testSchema.contains("idx_dcc_controlled_file_taxonomy"),
                "DCC test schema must index controlled-file taxonomy references");
        assertTrue(testSchema.contains("idx_dcc_recognition_record_taxonomy"),
                "DCC test schema must index recognition-record taxonomy references");
    }

    @Test
    void mysqlRuntimeRepairSchemaShouldBackfillLegacyQmsfcAndE2eCategoryStages() throws Exception {
        Path projectDir = findProjectDir();
        Path repairSchemaFile = projectDir.resolve("sql/mysql/20260515_dcc_runtime_schema_repair.sql");

        String repairSchema = Files.readString(repairSchemaFile);

        for (String categoryCode : List.of("QMSFC-0001", "QMSFC-0011", "QMSFC-0012", "QMSFC-0025",
                "QMSFC-0036", "QMSFC-0048", "INTAUTH-1", "INTAUTH-28", "INTAUTH-36", "INTAUTH-48",
                "NASCAT-%", "CODEX_DCC_LOCAL\\_%", "CODEX_E2E\\_%")) {
            assertTrue(repairSchema.contains(categoryCode),
                    "DCC runtime repair schema must backfill legacy category lifecycle stage for " + categoryCode);
        }
        assertTrue(repairSchema.contains("WHERE `deleted` <> 0")
                        && repairSchema.contains("SET `lifecycle_stage` = 'TRANSFER'"),
                "DCC runtime repair schema must normalize deleted legacy categories before lifecycle stage becomes NOT NULL");
    }

    @Test
    void mysqlSchemaShouldSupportLocalFolderResumableChunkUpload() throws Exception {
        Path projectDir = findProjectDir();
        Path migrationFile = projectDir.resolve("sql/mysql/20260615_dcc_local_folder_chunk_upload.sql");
        Path testSchemaFile = projectDir.resolve("yudao-module-dcc/src/test/resources/sql/create_tables.sql");

        assertTrue(Files.exists(migrationFile), "DCC local folder chunk upload migration must exist");

        String runtimeSchema = readDccRuntimeSchema(projectDir);
        assertSchemaIsNonDestructive(runtimeSchema, "local folder chunk upload runtime");
        assertTrue(runtimeSchema.contains("CREATE TABLE IF NOT EXISTS `dcc_controlled_file_local_folder_upload_chunk`"),
                "DCC chunk upload table must exist");
        assertSchemaHasColumns(runtimeSchema, "dcc_controlled_file_local_folder_upload_chunk",
                List.of("task_id", "relative_path", "file_name", "file_size", "chunk_index",
                        "total_chunks", "chunk_size", "chunk_sha256", "chunk_temp_path", "status"));
        assertTrue(runtimeSchema.contains("uk_dcc_local_folder_chunk_position"),
                "DCC chunk upload table must enforce idempotent chunk position uniqueness");

        String testSchema = Files.readString(testSchemaFile);
        assertTrue(testSchema.contains("CREATE TABLE IF NOT EXISTS `dcc_controlled_file_local_folder_upload_chunk`"),
                "DCC test schema must include chunk upload table");
        assertSchemaHasColumns(testSchema, "dcc_controlled_file_local_folder_upload_chunk",
                List.of("task_id", "relative_path", "file_name", "file_size", "chunk_index",
                        "total_chunks", "chunk_size", "chunk_sha256", "chunk_temp_path", "status"));
    }

    @Test
    void mysqlSchemaShouldSupportDccNasControlAuditFileDetails() throws Exception {
        Path projectDir = findProjectDir();
        Path migrationFile = projectDir.resolve("sql/mysql/20260803_dcc_nas_control_audit_file.sql");
        Path testSchemaFile = projectDir.resolve("yudao-module-dcc/src/test/resources/sql/create_tables.sql");

        assertTrue(Files.exists(migrationFile), "DCC NAS control audit file migration must exist");

        String migrationSchema = Files.readString(migrationFile);
        assertSchemaIsNonDestructive(migrationSchema, "NAS control audit file");

        String runtimeSchema = readDccRuntimeSchema(projectDir);
        assertNotNull(findCreateBlock(runtimeSchema, "dcc_nas_control_audit_file"),
                "DCC runtime schema must create queryable NAS audit file details");
        assertSchemaHasColumns(runtimeSchema, "dcc_nas_control_audit_file", List.of(
                "task_id", "nas_share_name", "root_path", "normalized_relative_path", "path_hash",
                "file_name", "file_size", "modified_at", "source_signature", "control_status",
                "classification_status", "matched_project_code_id", "matched_file_type_taxonomy_id",
                "matched_file_type_level1", "matched_file_type_level2", "matched_file_type_level3",
                "matched_file_type_level4", "matched_file_type_level5", "classification_reason",
                "download_status", "archive_status", "local_relative_path", "local_write_error_code",
                "local_write_error", "archive_error_code", "archive_error", "controlled_file_id"));
        assertColumnUsesBinaryCollation(runtimeSchema, "dcc_nas_control_audit_file",
                "normalized_relative_path", "runtime");
        assertColumnUsesBinaryCollation(runtimeSchema, "dcc_nas_control_audit_file",
                "local_relative_path", "runtime");
        assertTrue(runtimeSchema.contains("PENDING_RECOGNITION"),
                "DCC audit file schema must define the initial recognition status");
        assertTrue(runtimeSchema.contains("NOT_SELECTED") && runtimeSchema.contains("NOT_STARTED"),
                "DCC audit file schema must define independent initial download/archive statuses");
        assertIndexColumns(runtimeSchema, "dcc_nas_control_audit_file", "idx_dcc_nas_audit_file_task",
                List.of("tenant_id", "task_id", "id"), "runtime");
        assertIndexColumns(runtimeSchema, "dcc_nas_control_audit_file", "idx_dcc_nas_audit_file_path_hash",
                List.of("tenant_id", "nas_share_name", "path_hash", "deleted"), "runtime");
        assertIndexColumns(runtimeSchema, "dcc_nas_control_audit_file", "idx_dcc_nas_audit_file_status",
                List.of("tenant_id", "classification_status", "download_status", "archive_status"), "runtime");

        String testSchema = Files.readString(testSchemaFile);
        assertNotNull(findCreateBlock(testSchema, "dcc_nas_control_audit_file"),
                "DCC test schema must include NAS audit file details");
        assertSchemaHasColumns(testSchema, "dcc_nas_control_audit_file", List.of(
                "task_id", "nas_share_name", "root_path", "normalized_relative_path", "path_hash",
                "file_name", "file_size", "modified_at", "source_signature", "control_status",
                "classification_status", "download_status", "archive_status", "local_write_error_code",
                "archive_error_code", "tenant_id"));
    }

    @Test
    void mysqlSchemaShouldSupportDccNasControlAuditFileRecognitionSnapshot() throws Exception {
        Path projectDir = findProjectDir();
        Path migrationFile = projectDir.resolve("sql/mysql/20260803_dcc_nas_control_audit_file.sql");
        Path testSchemaFile = projectDir.resolve("yudao-module-dcc/src/test/resources/sql/create_tables.sql");
        Path respVOFile = projectDir.resolve("yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccNasControlAuditFileRespVO.java");
        Path doFile = projectDir.resolve("yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/dataobject/file/DccNasControlAuditFileDO.java");

        String migrationSchema = Files.readString(migrationFile);
        assertSchemaHasColumns(migrationSchema, "dcc_nas_control_audit_file", List.of(
                "classification_candidates_json", "expected_local_relative_path"));
        assertColumnUsesBinaryCollation(migrationSchema, "dcc_nas_control_audit_file",
                "expected_local_relative_path", "recognition snapshot migration");

        String testSchema = Files.readString(testSchemaFile);
        assertSchemaHasColumns(testSchema, "dcc_nas_control_audit_file", List.of(
                "classification_candidates_json", "expected_local_relative_path"));

        String respVO = Files.readString(respVOFile);
        assertTrue(respVO.contains("private String classificationCandidatesJson;"),
                "Audit file response must expose persisted recognition candidates");
        assertTrue(respVO.contains("private String expectedLocalRelativePath;"),
                "Audit file response must expose backend-generated expected local relative path");

        String dataObject = Files.readString(doFile);
        assertTrue(dataObject.contains("private String classificationCandidatesJson;"),
                "Audit file DO must persist recognition candidates");
        assertTrue(dataObject.contains("private String expectedLocalRelativePath;"),
                "Audit file DO must persist expected local relative path");
    }

    @Test
    void mysqlSchemaShouldSupportNasUncontrolledImportTaskSnapshots() throws Exception {
        Path projectDir = findProjectDir();
        Path migrationFile = projectDir.resolve("sql/mysql/20260803_dcc_nas_uncontrolled_import_task_snapshot.sql");
        Path testSchemaFile = projectDir.resolve("yudao-module-dcc/src/test/resources/sql/create_tables.sql");
        Path taskDOFile = projectDir.resolve("yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/dataobject/file/DccControlledFileNasTransferTaskDO.java");
        Path itemDOFile = projectDir.resolve("yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/dataobject/file/DccControlledFileNasTransferTaskItemDO.java");
        Path auditFileDOFile = projectDir.resolve("yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/dataobject/file/DccNasControlAuditFileDO.java");

        assertTrue(Files.exists(migrationFile), "DCC NAS uncontrolled import task snapshot migration must exist");

        String migrationSchema = Files.readString(migrationFile);
        assertSchemaIsNonDestructive(migrationSchema, "NAS uncontrolled import task snapshot");
        assertSchemaHasColumns(migrationSchema, "dcc_controlled_file_nas_transfer_task",
                List.of("audit_task_id", "idempotency_key", "request_hash"));
        assertTrue(Pattern.compile("MODIFY\\s+COLUMN\\s+`template_category_id`\\s+bigint\\s+DEFAULT\\s+NULL",
                        Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(migrationSchema).find(),
                "NAS uncontrolled import migration must allow task template_category_id to be nullable");
        assertTrue(Pattern.compile("MODIFY\\s+COLUMN\\s+`effective_date`\\s+date\\s+DEFAULT\\s+NULL",
                        Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(migrationSchema).find(),
                "NAS uncontrolled import migration must allow task effective_date to be nullable");
        assertSchemaHasColumns(migrationSchema, "dcc_controlled_file_nas_transfer_task_item",
                List.of("audit_file_id", "source_signature", "classification_status_snapshot",
                        "matched_project_code_id_snapshot", "matched_file_type_taxonomy_id_snapshot",
                        "matched_file_type_level1_snapshot", "matched_file_type_level2_snapshot",
                        "matched_file_type_level3_snapshot", "matched_file_type_level4_snapshot",
                        "matched_file_type_level5_snapshot", "classification_reason_snapshot",
                        "classification_candidates_json_snapshot", "local_relative_path",
                        "local_write_status", "local_write_error_code", "local_write_error",
                        "archive_status", "archive_error_code", "archive_error",
                        "archive_category_id_snapshot", "archive_directory_id_snapshot",
                        "archive_dcc_project_code_id_snapshot", "archive_file_type_taxonomy_id_snapshot",
                        "archive_change_type_snapshot", "archive_file_name_snapshot",
                        "archive_file_number_snapshot", "archive_version_no_snapshot",
                        "archive_effective_date_snapshot", "archive_remark_snapshot"));
        assertSchemaHasColumns(migrationSchema, "dcc_nas_control_audit_file",
                List.of("selected_import_task_id", "selected_import_task_item_id"));
        assertTrue(Pattern.compile("ADD\\s+INDEX\\s+`idx_dcc_nas_transfer_import_idempotency`\\s*"
                                + "\\(`tenant_id`,\\s*`operator_user_id`,\\s*`idempotency_key`,\\s*`deleted`\\)",
                        Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(migrationSchema).find(),
                "NAS uncontrolled import task snapshot migration must index idempotency lookup");
        assertTrue(Pattern.compile("ADD\\s+INDEX\\s+`idx_dcc_nas_transfer_item_audit_file`\\s*"
                                + "\\(`tenant_id`,\\s*`audit_file_id`,\\s*`deleted`\\)",
                        Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(migrationSchema).find(),
                "NAS uncontrolled import task snapshot migration must index audit-file lookup");
        assertTrue(Pattern.compile("`local_relative_path`\\s+varchar\\(1024\\)\\s+CHARACTER\\s+SET\\s+utf8mb4\\s+COLLATE\\s+utf8mb4_bin",
                        Pattern.CASE_INSENSITIVE).matcher(migrationSchema).find(),
                "NAS uncontrolled import task snapshot migration must store local relative paths with binary collation");

        String testSchema = Files.readString(testSchemaFile);
        assertSchemaHasColumns(testSchema, "dcc_controlled_file_nas_transfer_task",
                List.of("audit_task_id", "idempotency_key", "request_hash"));
        assertColumnNullable(testSchema, "dcc_controlled_file_nas_transfer_task", "template_category_id",
                "test NAS uncontrolled import task");
        assertTrue(Pattern.compile("`effective_date`\\s+DATE\\s+NULL",
                        Pattern.CASE_INSENSITIVE).matcher(testSchema).find(),
                "DCC test schema must allow nullable dcc_controlled_file_nas_transfer_task.effective_date");
        assertSchemaHasColumns(testSchema, "dcc_controlled_file_nas_transfer_task_item",
                List.of("audit_file_id", "source_signature", "classification_status_snapshot",
                        "classification_candidates_json_snapshot", "local_relative_path",
                        "local_write_status", "archive_status",
                        "archive_category_id_snapshot", "archive_directory_id_snapshot",
                        "archive_dcc_project_code_id_snapshot", "archive_file_type_taxonomy_id_snapshot",
                        "archive_change_type_snapshot", "archive_file_name_snapshot",
                        "archive_file_number_snapshot", "archive_version_no_snapshot",
                        "archive_effective_date_snapshot", "archive_remark_snapshot"));
        assertSchemaHasColumns(testSchema, "dcc_nas_control_audit_file",
                List.of("selected_import_task_id", "selected_import_task_item_id"));

        String taskDO = Files.readString(taskDOFile);
        assertTrue(taskDO.contains("private Long auditTaskId;"), "NAS transfer task DO must bind audit task");
        assertTrue(taskDO.contains("private String idempotencyKey;"), "NAS transfer task DO must persist idempotency key");
        assertTrue(taskDO.contains("private String requestHash;"), "NAS transfer task DO must persist canonical request hash");

        String itemDO = Files.readString(itemDOFile);
        assertTrue(itemDO.contains("private Long auditFileId;"), "NAS transfer item DO must bind audit file");
        assertTrue(itemDO.contains("private String sourceSignature;"), "NAS transfer item DO must persist source signature snapshot");
        assertTrue(itemDO.contains("private String localRelativePath;"), "NAS transfer item DO must persist local relative path snapshot");
        assertTrue(itemDO.contains("private String localWriteStatus;"), "NAS transfer item DO must separate local write status");
        assertTrue(itemDO.contains("private String archiveStatus;"), "NAS transfer item DO must separate archive status");
        assertTrue(itemDO.contains("private Long archiveCategoryIdSnapshot;"),
                "NAS uncontrolled import item DO must persist formal archive category id snapshot");
        assertTrue(itemDO.contains("private Long archiveDirectoryIdSnapshot;"),
                "NAS uncontrolled import item DO must persist formal archive directory id snapshot");
        assertTrue(itemDO.contains("private Long archiveDccProjectCodeIdSnapshot;"),
                "NAS uncontrolled import item DO must persist formal archive project code id snapshot");
        assertTrue(itemDO.contains("private Long archiveFileTypeTaxonomyIdSnapshot;"),
                "NAS uncontrolled import item DO must persist formal archive file type taxonomy id snapshot");
        assertTrue(itemDO.contains("private String archiveChangeTypeSnapshot;"),
                "NAS uncontrolled import item DO must persist formal archive change type snapshot");
        assertTrue(itemDO.contains("private String archiveFileNameSnapshot;"),
                "NAS uncontrolled import item DO must persist formal archive file name snapshot");
        assertTrue(itemDO.contains("private String archiveFileNumberSnapshot;"),
                "NAS uncontrolled import item DO must persist formal archive file number snapshot");
        assertTrue(itemDO.contains("private String archiveVersionNoSnapshot;"),
                "NAS uncontrolled import item DO must persist formal archive version snapshot");
        assertTrue(itemDO.contains("private LocalDate archiveEffectiveDateSnapshot;"),
                "NAS uncontrolled import item DO must persist formal archive effective date snapshot");
        assertTrue(itemDO.contains("private String archiveRemarkSnapshot;"),
                "NAS uncontrolled import item DO must persist formal archive remark snapshot");

        String auditFileDO = Files.readString(auditFileDOFile);
        assertTrue(auditFileDO.contains("private Long selectedImportTaskId;"),
                "Audit file DO must expose selected import task binding");
        assertTrue(auditFileDO.contains("private Long selectedImportTaskItemId;"),
                "Audit file DO must expose selected import task item binding");
    }

    @Test
    void mysqlSchemaShouldAllowNasTransferWithoutProductBinding() throws Exception {
        Path projectDir = findProjectDir();
        Path baseSchemaFile = projectDir.resolve("sql/mysql/20260513_dcc_base_schema.sql");
        Path migrationFile = projectDir.resolve("sql/mysql/20260614_dcc_optional_product_binding.sql");
        Path testSchemaFile = projectDir.resolve("yudao-module-dcc/src/test/resources/sql/create_tables.sql");

        assertTrue(Files.exists(migrationFile), "DCC optional product binding migration must exist");

        String baseSchema = Files.readString(baseSchemaFile);
        assertColumnNullable(baseSchema, "dcc_controlled_file_nas_transfer_task", "product_master_id",
                "base NAS transfer task");

        String migrationSchema = Files.readString(migrationFile);
        assertSchemaIsNonDestructive(migrationSchema, "optional product binding");
        assertTrue(Pattern.compile(
                        "MODIFY\\s+COLUMN\\s+`product_master_id`\\s+bigint\\s+DEFAULT\\s+NULL",
                        Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(migrationSchema).find(),
                "DCC optional product binding migration must make NAS transfer product_master_id nullable");

        String testSchema = Files.readString(testSchemaFile);
        assertColumnNullable(testSchema, "dcc_controlled_file_nas_transfer_task", "product_master_id",
                "test NAS transfer task");
    }

    @Test
    void mysqlSchemaShouldKeepControlledFileMasterDistinctPerDirectory() throws Exception {
        Path projectDir = findProjectDir();
        Path baseSchemaFile = projectDir.resolve("sql/mysql/20260513_dcc_base_schema.sql");
        Path repairSchemaFile = projectDir.resolve("sql/mysql/20260515_dcc_runtime_schema_repair.sql");
        Path migrationFile = projectDir.resolve("sql/mysql/20260614_dcc_master_directory_identity.sql");
        Path testSchemaFile = projectDir.resolve("yudao-module-dcc/src/test/resources/sql/create_tables.sql");

        assertTrue(Files.exists(migrationFile), "DCC master directory identity migration must exist");

        String baseSchema = Files.readString(baseSchemaFile);
        assertSchemaHasColumns(baseSchema, "dcc_controlled_file_master", List.of("directory_id"));
        assertTrue(baseSchema.contains(
                        "`uk_dcc_controlled_file_master_chain` (`category_id`, `directory_id`, `file_name`)"),
                "base DCC master unique key must include directory_id");

        String repairSchema = Files.readString(repairSchemaFile);
        assertSchemaIsNonDestructive(repairSchema, "DCC master directory identity runtime repair");
        assertTrue(repairSchema.contains("'dcc_controlled_file_master'"),
                "runtime repair must patch DCC master table");
        assertTrue(repairSchema.contains("'directory_id'"),
                "runtime repair must add the DCC master directory_id column");

        String migrationSchema = Files.readString(migrationFile);
        assertSchemaIsNonDestructive(migrationSchema, "DCC master directory identity");
        assertTrue(migrationSchema.contains("`category_id`, `directory_id`, `file_name`"),
                "DCC master directory identity migration must rebuild the unique key with directory_id");

        String testSchema = Files.readString(testSchemaFile);
        assertSchemaHasColumns(testSchema, "dcc_controlled_file_master", List.of("directory_id"));
    }

    @Test
    void mysqlSchemaShouldSupportLongNasFileNames() throws Exception {
        Path projectDir = findProjectDir();
        Path baseSchemaFile = projectDir.resolve("sql/mysql/20260513_dcc_base_schema.sql");
        Path repairSchemaFile = projectDir.resolve("sql/mysql/20260515_dcc_runtime_schema_repair.sql");
        Path migrationFile = projectDir.resolve("sql/mysql/20260530_dcc_long_file_name_length.sql");
        Path testSchemaFile = projectDir.resolve("yudao-module-dcc/src/test/resources/sql/create_tables.sql");

        assertTrue(Files.exists(migrationFile), "DCC long file-name migration must exist");

        String baseSchema = Files.readString(baseSchemaFile);
        assertColumnVarcharLengthAtLeast(baseSchema, "dcc_controlled_file_master", "file_name", 256, "base");
        assertColumnVarcharLengthAtLeast(baseSchema, "dcc_controlled_file", "file_name", 256, "base");
        assertColumnVarcharLengthAtLeast(baseSchema, "dcc_controlled_file", "title", 256, "base");

        String repairSchema = Files.readString(repairSchemaFile);
        assertColumnVarcharLengthAtLeast(repairSchema, "dcc_controlled_file_master", "file_name", 256,
                "runtime repair");
        assertSchemaContainsVarcharLengthAtLeast(repairSchema, "file_name", 256,
                "runtime repair controlled-file file_name add-column");

        String migrationSchema = Files.readString(migrationFile);
        assertSchemaIsNonDestructive(migrationSchema, "long file-name length");
        assertModifyColumnVarcharLengthAtLeast(migrationSchema, "dcc_controlled_file_master", "file_name", 256,
                "long file-name length");
        assertModifyColumnVarcharLengthAtLeast(migrationSchema, "dcc_controlled_file", "file_name", 256,
                "long file-name length");
        assertModifyColumnVarcharLengthAtLeast(migrationSchema, "dcc_controlled_file", "title", 256,
                "long file-name length");

        String testSchema = Files.readString(testSchemaFile);
        assertColumnVarcharLengthAtLeast(testSchema, "dcc_controlled_file_master", "file_name", 256, "test");
        assertColumnVarcharLengthAtLeast(testSchema, "dcc_controlled_file", "file_name", 256, "test");
        assertColumnVarcharLengthAtLeast(testSchema, "dcc_controlled_file", "title", 256, "test");
    }

    @Test
    void mysqlSchemaShouldSupportLongNasTransferSourceRemarks() throws Exception {
        Path projectDir = findProjectDir();
        Path baseSchemaFile = projectDir.resolve("sql/mysql/20260513_dcc_base_schema.sql");
        Path migrationFile = projectDir.resolve("sql/mysql/20260530_dcc_long_nas_source_remark.sql");
        Path testSchemaFile = projectDir.resolve("yudao-module-dcc/src/test/resources/sql/create_tables.sql");

        assertTrue(Files.exists(migrationFile), "DCC long NAS source remark migration must exist");

        String baseSchema = Files.readString(baseSchemaFile);
        assertColumnVarcharLengthAtLeast(baseSchema, "dcc_controlled_file", "remark", 1024, "base");

        String migrationSchema = Files.readString(migrationFile);
        assertSchemaIsNonDestructive(migrationSchema, "long NAS source remark");
        assertModifyColumnVarcharLengthAtLeast(migrationSchema, "dcc_controlled_file", "remark", 1024,
                "long NAS source remark");

        String testSchema = Files.readString(testSchemaFile);
        assertColumnVarcharLengthAtLeast(testSchema, "dcc_controlled_file", "remark", 1024, "test");
    }

    @Test
    void mysqlSchemaShouldScopeDccCodeUniquenessByTenant() throws Exception {
        Path projectDir = findProjectDir();
        Path baseSchemaFile = projectDir.resolve("sql/mysql/20260513_dcc_base_schema.sql");
        Path migrationFile = projectDir.resolve("sql/mysql/20260530_dcc_tenant_scoped_code_indexes.sql");
        Path testSchemaFile = projectDir.resolve("yudao-module-dcc/src/test/resources/sql/create_tables.sql");

        assertTrue(Files.exists(migrationFile), "DCC tenant-scoped code index migration must exist");

        String baseSchema = Files.readString(baseSchemaFile);
        assertUniqueKeyColumns(baseSchema, "dcc_file_category", "uk_dcc_file_category_tenant_code",
                List.of("tenant_id", "code"), "base");
        assertUniqueKeyColumns(baseSchema, "dcc_approval_position", "uk_dcc_approval_position_tenant_code",
                List.of("tenant_id", "code"), "base");

        String migrationSchema = Files.readString(migrationFile);
        assertSchemaIsNonDestructive(migrationSchema, "tenant-scoped DCC code indexes");
        assertMigrationReplacesUniqueKey(migrationSchema, "dcc_file_category", "uk_dcc_file_category_code",
                "uk_dcc_file_category_tenant_code", List.of("tenant_id", "code"),
                "tenant-scoped DCC code indexes");
        assertMigrationReplacesUniqueKey(migrationSchema, "dcc_approval_position", "uk_dcc_approval_position_code",
                "uk_dcc_approval_position_tenant_code", List.of("tenant_id", "code"),
                "tenant-scoped DCC code indexes");

        String testSchema = Files.readString(testSchemaFile);
        assertUniqueKeyColumns(testSchema, "dcc_file_category", "uk_dcc_file_category_tenant_code",
                List.of("tenant_id", "code"), "test");
        assertUniqueKeyColumns(testSchema, "dcc_approval_position", "uk_dcc_approval_position_tenant_code",
                List.of("tenant_id", "code"), "test");
    }

    @Test
    void mysqlSchemaShouldIncludeElectronicSignatureHardeningMigration() throws Exception {
        Path projectDir = findProjectDir();
        Path schemaFile = projectDir.resolve("sql/mysql/20260526_dcc_electronic_signature_hardening.sql");

        assertTrue(Files.exists(schemaFile), "DCC electronic signature hardening migration must exist");

        String schema = Files.readString(schemaFile);
        assertSchemaIsNonDestructive(schema, "electronic signature hardening");
        assertSchemaHasColumns(schema, "dcc_controlled_file_signature", List.of(
                "revision_id", "version_no", "meaning_code", "meaning_label", "source_file_id",
                "source_file_hash", "source_file_hash_algorithm", "source_file_hash_status",
                "controlled_copy_file_id", "controlled_copy_hash", "controlled_copy_hash_algorithm",
                "controlled_copy_hash_status", "evidence_payload_version", "evidence_hash",
                "evidence_hash_algorithm", "evidence_status", "actor_username_snapshot",
                "actor_nickname_snapshot", "actor_dept_id_snapshot", "actor_dept_name_snapshot",
                "actor_post_names_snapshot", "actor_role_names_snapshot", "signature_purpose",
                "authorization_basis", "authentication_method", "record_version_snapshot",
                "record_hash_snapshot", "client_ip_snapshot", "user_agent_snapshot", "snapshot_status"));
        assertSchemaHasColumns(schema, "dcc_electronic_signature_authorization", List.of(
                "authorization_state", "locked_until", "lock_reason", "last_failure_at", "failure_count"));
        assertNotNull(findCreateBlock(schema, "dcc_electronic_signature_authorization_audit"),
                "DCC hardening migration must create authorization audit table");
        assertNotNull(findCreateBlock(schema, "dcc_electronic_signature_failure_audit"),
                "DCC hardening migration must create failure audit table");
        assertNotNull(findCreateBlock(schema, "dcc_electronic_signature_policy"),
                "DCC hardening migration must create electronic signature policy table");
        assertTrue(schema.contains("PHASE1_FAIL_CLOSED_INITIALIZATION"),
                "DCC hardening migration must record fail-closed initialization audit reason");
        assertTrue(schema.contains("INSERT INTO `dcc_electronic_signature_authorization`")
                        && schema.contains("dcc_category_approval_route_node")
                        && schema.contains("dcc_position_assignment")
                        && schema.contains("candidate_source_ids"),
                "DCC hardening migration must initialize enabled authorization rows from resolvable active routes");
        assertTrue(schema.contains("dcc_controlled_file_route_snapshot")
                        && schema.contains("resolved_user_ids")
                        && schema.contains("FIND_IN_SET"),
                "DCC hardening migration must initialize enabled authorization rows from route snapshots");
        assertFindInSetUsesExplicitUtf8mb4Collation(schema);
        assertTrue(schema.contains("ACT_RU_TASK")
                        && schema.contains("PROC_INST_ID_")
                        && schema.contains("ASSIGNEE_"),
                "DCC hardening migration must initialize enabled authorization rows from current unfinished BPM tasks");
        assertTrue(schema.contains("authorization_existing.`user_id` IS NULL"),
                "DCC hardening migration must not overwrite explicit existing authorization rows");
        assertTrue(schema.contains("password_failure_window_minutes") && schema.contains("15")
                        && schema.contains("password_failure_threshold") && schema.contains("5")
                        && schema.contains("lock_minutes") && schema.contains("30"),
                "DCC hardening migration must seed the 15/5/30 lock policy");
        assertTenantScopedElectronicSignaturePolicySeed(schema);
        assertTrue(schema.contains("idx_dcc_signature_revision"),
                "DCC hardening migration must add signature revision index");
        assertTrue(schema.contains("idx_dcc_signature_source_file"),
                "DCC hardening migration must add source file evidence index");
        assertTrue(schema.contains("idx_dcc_signature_copy_file"),
                "DCC hardening migration must add controlled-copy evidence index");
    }

    private static void assertSchemaIsNonDestructive(String schema, String schemaName) {
        String executableSchema = stripSqlComments(schema);
        assertFalse(Pattern.compile("\\b(DROP\\s+TABLE|TRUNCATE\\s+TABLE)\\b", Pattern.CASE_INSENSITIVE)
                        .matcher(executableSchema).find(),
                "DCC " + schemaName + " schema must not contain destructive table operations");
        assertFalse(Pattern.compile("\\bDELETE\\s+FROM\\s+`?dcc_", Pattern.CASE_INSENSITIVE)
                        .matcher(executableSchema).find(),
                "DCC " + schemaName + " schema must not delete DCC data");
    }

    private static String stripSqlComments(String schema) {
        return schema
                .replaceAll("(?s)/\\*.*?\\*/", "")
                .replaceAll("(?m)--.*$", "");
    }

    private static void assertSchemaBlockHasExpectedColumns(String schema, String tableName, List<String> columns,
                                                            String schemaName) {
        String createBlock = findCreateBlock(schema, tableName);
        assertTrue(createBlock != null || schemaContainsColumnPatch(schema, tableName),
                "Missing idempotent CREATE TABLE or migration patch for " + tableName + " in " + schemaName + " schema");
        for (String column : columns) {
            assertColumnCovered(schema, createBlock, tableName, column, schemaName);
        }
    }

    private static void assertFoundationTables(String schema, String schemaName) {
        for (Map.Entry<String, List<String>> entry : REQUIRED_FOUNDATION_COLUMNS.entrySet()) {
            String tableName = entry.getKey();
            String createBlock = findCreateBlock(schema, tableName);
            assertNotNull(createBlock, "Missing foundation table " + tableName + " in " + schemaName + " schema");
            for (String column : entry.getValue()) {
                assertColumnExists(createBlock, tableName, column, schemaName);
            }
        }
    }

    private static void assertRevisionLifecycleEnum() {
        Set<String> actualStatuses = Set.of(DccControlledFileStatusEnum.ARRAYS);
        assertTrue(actualStatuses.containsAll(REQUIRED_REVISION_STATUSES),
                "DCC revision status enum must contain the approved lifecycle states");
    }

    private static void assertColumnExists(String createBlock, String tableName, String column, String schemaName) {
        assertTrue(Pattern.compile("`" + Pattern.quote(column) + "`\\s+", Pattern.CASE_INSENSITIVE)
                        .matcher(createBlock).find(),
                "Missing column " + tableName + "." + column + " in " + schemaName + " schema");
    }

    private static void assertColumnCovered(String schema, String createBlock, String tableName, String column,
                                            String schemaName) {
        boolean existsInCreateBlock = createBlock != null
                && Pattern.compile("`" + Pattern.quote(column) + "`\\s+", Pattern.CASE_INSENSITIVE)
                .matcher(createBlock).find();
        boolean existsInMigrationPatch = Pattern.compile(
                "ensure_dcc_column\\s*\\(\\s*'" + Pattern.quote(tableName) + "'\\s*,\\s*'"
                        + Pattern.quote(column) + "'",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(schema).find()
                || Pattern.compile("ALTER\\s+TABLE\\s+`?" + Pattern.quote(tableName)
                        + "`?\\s+ADD\\s+COLUMN\\s+`?" + Pattern.quote(column) + "`?",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(schema).find();
        assertTrue(existsInCreateBlock || existsInMigrationPatch,
                "Missing column " + tableName + "." + column + " in " + schemaName + " schema");
    }

    private static boolean schemaContainsColumnPatch(String schema, String tableName) {
        return Pattern.compile("ensure_dcc_column\\s*\\(\\s*'" + Pattern.quote(tableName) + "'",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(schema).find()
                || Pattern.compile("ALTER\\s+TABLE\\s+`?" + Pattern.quote(tableName) + "`?\\s+ADD\\s+COLUMN",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(schema).find();
    }

    private static void assertFindInSetUsesExplicitUtf8mb4Collation(String schema) {
        Matcher matcher = Pattern.compile("FIND_IN_SET\\s*\\((.*?)\\)\\s*>\\s*0",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(schema);
        int findInSetCount = 0;
        while (matcher.find()) {
            findInSetCount++;
            String expression = matcher.group(1);
            assertTrue(Pattern.compile("\\bCONVERT\\s*\\(", Pattern.CASE_INSENSITIVE).matcher(expression).find(),
                    "DCC hardening migration FIND_IN_SET must normalize charset: " + expression);
            assertTrue(countMatches(expression, Pattern.compile("\\bCOLLATE\\s+utf8mb4_unicode_ci\\b",
                            Pattern.CASE_INSENSITIVE)) >= 2,
                    "DCC hardening migration FIND_IN_SET must collate both comparison sides: " + expression);
        }
        assertTrue(findInSetCount >= 3,
                "DCC hardening migration must guard all authorization initialization FIND_IN_SET comparisons");
    }

    private static void assertColumnUsesBinaryCollation(String schema, String tableName, String column,
                                                        String schemaName) {
        String createBlock = findCreateBlock(schema, tableName);
        assertNotNull(createBlock, "Missing table " + tableName + " in " + schemaName + " schema");
        assertTrue(Pattern.compile("`" + Pattern.quote(column)
                                + "`\\s+varchar\\(\\d+\\)\\s+CHARACTER\\s+SET\\s+utf8mb4\\s+COLLATE\\s+utf8mb4_bin",
                        Pattern.CASE_INSENSITIVE).matcher(createBlock).find(),
                "DCC " + schemaName + " schema must store " + tableName + "." + column
                        + " with binary collation");
    }

    private static void assertModifyColumnUsesBinaryCollation(String schema, String tableName, String column,
                                                              String schemaName) {
        assertTrue(Pattern.compile("ALTER\\s+TABLE\\s+`" + Pattern.quote(tableName)
                                + "`\\s+MODIFY\\s+`" + Pattern.quote(column)
                                + "`\\s+varchar\\(\\d+\\)\\s+CHARACTER\\s+SET\\s+utf8mb4\\s+COLLATE\\s+utf8mb4_bin",
                        Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(schema).find(),
                "DCC " + schemaName + " migration must modify " + tableName + "." + column
                        + " to binary collation");
    }

    private static void assertColumnVarcharLengthAtLeast(String schema, String tableName, String column,
                                                         int minLength, String schemaName) {
        String createBlock = findCreateBlock(schema, tableName);
        assertNotNull(createBlock, "Missing table " + tableName + " in " + schemaName + " schema");
        assertSchemaContainsVarcharLengthAtLeast(createBlock, column, minLength,
                schemaName + " " + tableName + "." + column);
    }

    private static void assertColumnNullable(String schema, String tableName, String column, String schemaName) {
        String createBlock = findCreateBlock(schema, tableName);
        assertNotNull(createBlock, "Missing table " + tableName + " in " + schemaName + " schema");
        Matcher matcher = Pattern.compile("`" + Pattern.quote(column) + "`\\s+bigint\\s+(?:DEFAULT\\s+)?NULL",
                Pattern.CASE_INSENSITIVE).matcher(createBlock);
        assertTrue(matcher.find(), "DCC " + schemaName + " schema must allow nullable "
                + tableName + "." + column);
    }

    private static void assertSchemaContainsVarcharLengthAtLeast(String schema, String column, int minLength,
                                                                 String schemaName) {
        Matcher matcher = Pattern.compile("`" + Pattern.quote(column) + "`\\s+varchar\\((\\d+)\\)",
                Pattern.CASE_INSENSITIVE).matcher(schema);
        assertTrue(matcher.find(), "Missing varchar column " + column + " in " + schemaName + " schema");
        int actualLength = Integer.parseInt(matcher.group(1));
        assertTrue(actualLength >= minLength, "DCC " + schemaName + " schema must store " + column
                + " with varchar(" + minLength + ") or larger, actual varchar(" + actualLength + ")");
    }

    private static void assertModifyColumnVarcharLengthAtLeast(String schema, String tableName, String column,
                                                               int minLength, String schemaName) {
        Matcher matcher = Pattern.compile("ALTER\\s+TABLE\\s+`" + Pattern.quote(tableName)
                        + "`\\s+MODIFY\\s+`" + Pattern.quote(column) + "`\\s+varchar\\((\\d+)\\)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(schema);
        assertTrue(matcher.find(), "DCC " + schemaName + " migration must modify "
                + tableName + "." + column);
        int actualLength = Integer.parseInt(matcher.group(1));
        assertTrue(actualLength >= minLength, "DCC " + schemaName + " migration must modify "
                + tableName + "." + column + " to varchar(" + minLength + ") or larger, actual varchar("
                + actualLength + ")");
    }

    private static void assertUniqueKeyColumns(String schema, String tableName, String indexName,
                                               List<String> expectedColumns, String schemaName) {
        String createBlock = findCreateBlock(schema, tableName);
        assertNotNull(createBlock, "Missing table " + tableName + " in " + schemaName + " schema");
        Matcher matcher = Pattern.compile("(?:UNIQUE\\s+(?:KEY|INDEX)\\s+`" + Pattern.quote(indexName)
                        + "`|CONSTRAINT\\s+`" + Pattern.quote(indexName) + "`\\s+UNIQUE)\\s*\\(([^)]*)\\)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(createBlock);
        assertTrue(matcher.find(), "DCC " + schemaName + " schema must define unique key "
                + tableName + "." + indexName);
        assertTrue(extractIndexColumns(matcher.group(1)).equals(expectedColumns),
                "DCC " + schemaName + " schema unique key " + tableName + "." + indexName
                        + " must use columns " + expectedColumns + ", actual "
                        + extractIndexColumns(matcher.group(1)));
    }

    private static void assertIndexColumns(String schema, String tableName, String indexName,
                                           List<String> expectedColumns, String schemaName) {
        String createBlock = findCreateBlock(schema, tableName);
        assertNotNull(createBlock, "Missing table " + tableName + " in " + schemaName + " schema");
        Matcher matcher = Pattern.compile("(?:KEY|INDEX)\\s+`" + Pattern.quote(indexName)
                        + "`\\s*\\(([^)]*)\\)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(createBlock);
        assertTrue(matcher.find(), "DCC " + schemaName + " schema must define index "
                + tableName + "." + indexName);
        assertTrue(extractIndexColumns(matcher.group(1)).equals(expectedColumns),
                "DCC " + schemaName + " schema index " + tableName + "." + indexName
                        + " must use columns " + expectedColumns + ", actual "
                        + extractIndexColumns(matcher.group(1)));
    }

    private static void assertMigrationReplacesUniqueKey(String schema, String tableName, String oldIndexName,
                                                         String newIndexName, List<String> expectedColumns,
                                                         String schemaName) {
        assertTrue(Pattern.compile("ALTER\\s+TABLE\\s+`" + Pattern.quote(tableName)
                                + "`.*?DROP\\s+INDEX\\s+`" + Pattern.quote(oldIndexName) + "`",
                        Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(schema).find(),
                "DCC " + schemaName + " migration must drop legacy global unique key "
                        + tableName + "." + oldIndexName);
        Matcher matcher = Pattern.compile("ALTER\\s+TABLE\\s+`" + Pattern.quote(tableName)
                        + "`.*?ADD\\s+UNIQUE\\s+KEY\\s+`" + Pattern.quote(newIndexName)
                        + "`\\s*\\(([^)]*)\\)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(schema);
        assertTrue(matcher.find(), "DCC " + schemaName + " migration must add tenant-scoped unique key "
                + tableName + "." + newIndexName);
        assertTrue(extractIndexColumns(matcher.group(1)).equals(expectedColumns),
                "DCC " + schemaName + " migration unique key " + tableName + "." + newIndexName
                        + " must use columns " + expectedColumns + ", actual "
                        + extractIndexColumns(matcher.group(1)));
    }

    private static List<String> extractIndexColumns(String columnsExpression) {
        Matcher matcher = Pattern.compile("`([^`]+)`").matcher(columnsExpression);
        List<String> columns = new ArrayList<>();
        while (matcher.find()) {
            columns.add(matcher.group(1));
        }
        return columns;
    }

    private static void assertTenantScopedElectronicSignaturePolicySeed(String schema) {
        assertTrue(Pattern.compile(
                        "INSERT\\s+INTO\\s+`dcc_electronic_signature_policy`.*?FROM\\s+`system_tenant`\\s+tenant",
                        Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(schema).find(),
                "DCC hardening migration must seed lock policies from active system tenants");
        assertTrue(schema.contains("policy_existing.`tenant_id` = tenant.`id`"),
                "DCC hardening migration must check existing enabled policies per tenant");
        assertTrue(schema.contains("tenant.`status` = 0") && schema.contains("tenant.`deleted` = 0"),
                "DCC hardening migration must seed only active, undeleted tenants");
        assertTrue(schema.contains("policy_existing.`id` IS NULL"),
                "DCC hardening migration policy seed must be idempotent per tenant");
        assertFalse(Pattern.compile(
                        "SELECT\\s+15\\s*,\\s*5\\s*,\\s*30\\s*,\\s*'v1'\\s*,\\s*'HMAC_SHA256'\\s*,\\s*0\\s*,\\s*0\\s*,",
                        Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(schema).find(),
                "DCC hardening migration must not seed only tenant_id=0 lock policy");
        assertTrue(schema.contains("idx_dcc_signature_policy_tenant_status"),
                "DCC hardening migration must index tenant-scoped enabled policy lookup");
    }

    private static void assertSchemaHasColumns(String schema, String tableName, List<String> columns) {
        for (String column : columns) {
            assertTrue(Pattern.compile("`" + Pattern.quote(column) + "`\\s+", Pattern.CASE_INSENSITIVE)
                            .matcher(schema).find(),
                    "Missing column " + tableName + "." + column + " in DCC hardening schema");
        }
    }

    private static int countMatches(String value, Pattern pattern) {
        Matcher matcher = pattern.matcher(value);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static String readDccRuntimeSchema(Path projectDir) throws Exception {
        Path mysqlDir = projectDir.resolve("sql/mysql");
        try (var stream = Files.list(mysqlDir)) {
            StringBuilder schema = new StringBuilder();
            for (Path path : stream
                    .filter(item -> item.getFileName().toString().contains("_dcc_"))
                    .filter(item -> item.getFileName().toString().endsWith(".sql"))
                    .sorted()
                    .toList()) {
                schema.append(Files.readString(path)).append('\n');
            }
            return schema.toString();
        }
    }

    private static String readOptionalSchema(Path path) throws Exception {
        return Files.exists(path) ? Files.readString(path) : "";
    }

    private static String parseTableName(String source) {
        Matcher matcher = TABLE_NAME_PATTERN.matcher(source);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static List<String> expectedColumns(String source) {
        List<String> columns = new ArrayList<>();
        Matcher matcher = FIELD_PATTERN.matcher(source);
        while (matcher.find()) {
            columns.add(camelToSnake(matcher.group(1)));
        }
        for (String column : BASE_COLUMNS) {
            if (!columns.contains(column)) {
                columns.add(column);
            }
        }
        return columns;
    }

    private static String findCreateBlock(String schema, String tableName) {
        Pattern pattern = Pattern.compile(
                "CREATE\\s+TABLE\\s+IF\\s+NOT\\s+EXISTS\\s+`?" + Pattern.quote(tableName)
                        + "`?\\s*\\(([^;]+?)\\)\\s*(?:ENGINE|;)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(schema);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String camelToSnake(String value) {
        return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }

    private static Path findProjectDir() {
        Path currentDir = Path.of(System.getProperty("user.dir"));
        return "yudao-module-dcc".equals(currentDir.getFileName().toString()) ? currentDir.getParent() : currentDir;
    }

}
