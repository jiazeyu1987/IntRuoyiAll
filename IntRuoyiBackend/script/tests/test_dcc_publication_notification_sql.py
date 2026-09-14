from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260907_dcc_publication_notification.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing DCC publication-notification migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_schema_has_release_metadata_and_additive_notification_tables() -> None:
    sql = read_sql()
    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260815_system_notify_message_business_key,20260907_dcc_publication_impact_assessment; "
        "type=schema; riskLevel=medium"
    )
    lowered = sql.lower()
    assert "create table if not exists `dcc_publication_notification_delivery`" in lowered
    assert "create table if not exists `dcc_publication_notification_audit`" in lowered
    assert "update `dcc_publication_notification_candidate`" not in lowered
    assert "insert into `dcc_publication_notification_candidate`" not in lowered


def test_delivery_is_unique_versioned_and_platform_idempotent() -> None:
    sql = read_sql().lower()
    assert "unique key `uk_dcc_pub_delivery_candidate`" in sql
    assert "unique key `uk_dcc_pub_delivery_business_key`" in sql
    assert "`business_key` varchar(255) not null" in sql
    assert "`status` varchar(16) not null" in sql
    assert "`attempt_count` int not null default 0" in sql
    assert "`system_message_id` bigint null" in sql
    assert "`last_error_summary` varchar(512) null" in sql
    assert "`row_version` int not null default 0" in sql
    assert "`creation_token` varchar(36) not null" in sql


def test_migration_seeds_template_stable_menu_permission_role_and_package_contracts() -> None:
    sql = read_sql().lower()
    assert "dcc_publication_released" in sql
    assert "'受控文件 {filenumber} {versionno} 已正式发布，请查看发布后续。'" in sql
    assert "发布后续：{followupurl}" not in sql
    assert "'[\"filenumber\",\"versionno\",\"followupurl\"]'" in sql
    assert "'dcc系统'" in sql
    assert "${filenumber}" not in sql
    assert "controlled-file/publication-followup" in sql
    assert "dcc/controlled-file/publication-followup/index" in sql
    assert "dcc:controlled-file:publication-followup:manage" in sql
    assert "insert into `system_role_menu`" in sql
    assert "target_role.`code` = 'doc_control'" in sql
    assert "approval_menu.`permission` = 'dcc:controlled-file:approve'" in sql
    assert "approval_role_menu.`tenant_id` = target_role.`tenant_id`" in sql
    assert "update `system_tenant_package`" in sql
    assert "json_array_append" in sql
    assert "insert into `system_tenant_package`" not in sql
    assert "signal sqlstate '45000'" in sql
    assert "menu id 6830" in sql
    assert "release-migration" in sql
