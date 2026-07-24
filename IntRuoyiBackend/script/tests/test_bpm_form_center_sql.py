import re
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260717_bpm_form_center.sql"

MENU_IDS = [
    605071200,
    605071201,
    605071202,
    605071203,
    605071204,
    605071205,
    605071206,
    605071207,
    605071208,
    605071210,
    605071211,
    605071212,
    605071213,
    605071214,
    605071215,
    605071216,
    605071217,
    605071218,
    605071219,
    605071220,
    605071221,
]

PERMISSIONS = [
    "form:template:query",
    "form:template:create",
    "form:template:update",
    "form:template:publish",
    "form:template:disable",
    "form:template:obsolete",
    "form:template-source:download",
    "form:instance:create",
    "form:instance:update",
    "form:instance:submit",
    "form:instance:abandon",
    "form:policy:query",
    "form:policy:create",
    "form:policy:publish",
    "form:bpm-callback:handle",
    "form:instance:snapshot:query",
    "form:effect:query",
    "form:effect:retry",
]


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing form-center migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_form_center_migration_declares_release_metadata_and_guards() -> None:
    text = read_sql()

    assert text.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260615_system_config_package_menu; type=schema; riskLevel=medium"
    )
    assert "SET NAMES utf8mb4;" in text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "Conflicting form center system_menu id or permission exists" in text
    assert "Invalid system_tenant_package.menu_ids JSON" in text
    assert "Missing or duplicated form center menu rows" in text

    upper_sql = text.upper()
    assert "DROP TABLE" not in upper_sql
    assert "TRUNCATE TABLE" not in upper_sql
    assert "DELETE FROM" not in upper_sql


def test_form_center_migration_creates_tenant_scoped_tables() -> None:
    text = read_sql()

    for table_name in [
        "bpm_form_template_version",
        "bpm_form_action_policy",
        "bpm_form_action_instance",
        "bpm_form_action_snapshot",
        "bpm_form_task_permission",
        "bpm_form_effect_execution",
    ]:
        assert f"CREATE TABLE IF NOT EXISTS `{table_name}`" in text

    assert "`tenant_id` bigint NOT NULL" in text
    assert (
        "UNIQUE KEY `uk_bpm_form_template_version` "
        "(`tenant_id`, `template_id`, `version_no`)"
    ) in text
    assert "KEY `idx_bpm_form_action_policy_match`" in text
    assert "UNIQUE KEY `uk_bpm_form_action_instance_idem`" in text
    assert "UNIQUE KEY `uk_bpm_form_action_snapshot_version`" in text
    assert "UNIQUE KEY `uk_bpm_form_task_permission_task_user`" in text
    assert "UNIQUE KEY `uk_bpm_form_effect_execution_idem`" in text
    assert "KEY `idx_bpm_form_task_permission_active`" in text
    assert "KEY `idx_bpm_form_effect_execution_pending`" in text


def test_form_center_migration_creates_runtime_menu_and_permissions() -> None:
    text = read_sql()

    for menu_id in MENU_IDS:
        assert re.search(rf"\b{menu_id}\b", text), f"missing menu id {menu_id}"

    for permission in PERMISSIONS:
        assert f"'{permission}'" in text, f"missing permission {permission}"

    assert "'form-center'" in text
    assert "'form-center/template/index'" in text
    assert "'FormCenterTemplate'" in text
    assert "'form-center/business-action/index'" not in text
    assert "'FormCenterBusinessAction'" not in text
    assert "'business-action'" not in text
    assert "'业务动作表单'" not in text
    assert "`id` = 605071209" in text
    assert "`menu_id` = 605071209" in text
    assert "'form-center/policy/index'" in text
    assert "'FormCenterPolicy'" in text
    assert "'form-center/effect/index'" in text
    assert "'FormCenterEffect'" in text
    assert re.search(
        r"\(605071210,\s*'实例创建',\s*'form:instance:create',\s*3,\s*20,\s*605071200,",
        text,
    )
    assert re.search(
        r"\(605071219,\s*'实例快照查询',\s*'form:instance:snapshot:query',\s*3,\s*25,\s*605071200,",
        text,
    )
    assert re.search(
        r"\(605071220,\s*'生效待处理',\s*'form:effect:query',\s*2,\s*3,\s*605071200,\s*'effect',",
        text,
    )
    assert re.search(
        r"\(605071221,\s*'生效重试',\s*'form:effect:retry',\s*3,\s*1,\s*605071220,",
        text,
    )
    assert "`parent_id` = 1186" in text or ", 1186, 'form-center'," in text


def test_form_center_migration_merges_tenant_packages_and_role_menus() -> None:
    text = read_sql()

    assert "`system_tenant_package`" in text
    assert "JSON_VALID(`package`.`menu_ids`)" in text
    assert "JSON_TABLE(" in text
    assert "JSON_ARRAYAGG" in text
    assert "JSON_CONTAINS(`package`.`menu_ids`, CAST('1186' AS JSON), '$')" in text
    assert "tmp_form_center_menu_ids" in text

    assert "INSERT INTO `system_role_menu`" in text
    assert "CROSS JOIN `tmp_form_center_menu_ids`" in text
    assert "`role`.`code` = 'super_admin'" in text
    assert "`role`.`code` = 'tenant_admin'" in text
    assert "NOT EXISTS (" in text


def test_form_center_migration_expands_tenant_package_menu_ids_before_merge() -> None:
    text = read_sql()
    lower_sql = text.lower()

    alter_match = re.search(
        r"alter\s+table\s+`system_tenant_package`[\s\S]+?"
        r"modify\s+column\s+`menu_ids`\s+text",
        lower_sql,
    )
    assert alter_match, "menu_ids must be expanded before adding many form-center menu IDs"

    update_index = lower_sql.index("update `system_tenant_package` as `package`")
    assert alter_match.start() < update_index


def test_form_center_migration_avoids_mysql_temp_table_reopen_patterns() -> None:
    text = read_sql()

    assert re.search(
        r"CREATE TEMPORARY TABLE `tmp_form_center_menu_defs`[\s\S]*?\)\s*"
        r"DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;",
        text,
    )
    assert "LEFT JOIN `system_menu` AS `existing`\n    ON `existing`.`id` = `def`.`id`" in text
    assert (
        "WHERE NOT EXISTS (\n"
        "    SELECT 1\n"
        "    FROM `system_menu` AS `existing`\n"
        "    WHERE `existing`.`id` = `def`.`id`"
    ) not in text
    assert (
        ") OR EXISTS (\n"
        "    SELECT 1\n"
        "    FROM `system_menu` AS `existing`\n"
        "    JOIN `tmp_form_center_menu_defs` AS `def`"
    ) not in text
    assert "AND `existing`.`id` NOT IN (" in text


def test_form_center_menu_guard_allows_controlled_layout_upgrade_for_same_ids() -> None:
    text = read_sql()
    conflict_guard = text.split("IF EXISTS (", 2)[1].split(") THEN", 1)[0]

    assert "COALESCE(`existing`.`permission`, '') <> `def`.`permission`" in conflict_guard
    assert "OR `existing`.`type` <> `def`.`type`" not in conflict_guard
    assert "OR `existing`.`parent_id` <> `def`.`parent_id`" not in conflict_guard
    assert "OR COALESCE(`existing`.`path`, '') <> `def`.`path`" not in conflict_guard
    assert "OR COALESCE(`existing`.`component`, '') <> `def`.`component`" not in conflict_guard
    assert "OR COALESCE(`existing`.`component_name`, '') <> `def`.`component_name`" not in conflict_guard
