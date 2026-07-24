from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260624_unified_electronic_signature_menu.sql"

CHILD_MENUS = (
    ("@unified_signature_file_menu_id", "900411", "文件签名记录", "file-signatures", "SignatureGovernanceFileSignatures"),
    ("@unified_signature_batch_menu_id", "900412", "批记录签名记录", "batch-signatures", "SignatureGovernanceBatchSignatures"),
    ("@unified_signature_authorization_menu_id", "900413", "用户授权", "authorizations", "SignatureGovernanceAuthorizations"),
    ("@unified_signature_retention_menu_id", "900414", "长期留存", "retention", "SignatureGovernanceRetention"),
    ("@unified_signature_periodic_review_menu_id", "900415", "周期复核", "periodic-review", "SignatureGovernancePeriodicReview"),
    ("@unified_signature_csv_package_menu_id", "900416", "CSV质量包", "csv-package", "SignatureGovernanceCsvPackage"),
    ("@unified_signature_policy_menu_id", "900417", "统一策略", "policy", "SignatureGovernancePolicy"),
)
REMOVED_OVERVIEW_MENU = (
    "@unified_signature_overview_menu_id",
    "900410",
    "总览",
    "overview",
    "SignatureGovernanceOverview",
)


def test_unified_electronic_signature_menu_sql_exists_and_declares_first_level_menu():
    assert SQL_PATH.exists(), f"{SQL_PATH} must exist"
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert "900218" in sql
    assert "'电子签名'" in sql
    assert "'/signature-governance'" in sql
    assert "'signature-governance/index'" in sql
    assert "'SignatureGovernanceWorkbench'" in sql
    assert "`parent_id` = 0" in sql


def test_unified_electronic_signature_menu_release_metadata_uses_migration_id_stems():
    first_line = SQL_PATH.read_text(encoding="utf-8").splitlines()[0]

    assert first_line == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260520_dcc_signature_menu_restore,"
        "20260526_edhr_approval_archive_schema_contract,"
        "20260528_signature_governance_menu; type=menu; riskLevel=medium"
    )


def test_unified_electronic_signature_menu_declares_visible_child_menus():
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert "tmp_unified_signature_child_menu_ids" in sql
    assert "INSERT INTO `system_menu`" in sql
    assert "SELECT child_def.`id`" in sql
    assert "UNION ALL" in sql
    assert "ON DUPLICATE KEY UPDATE" in sql
    assert "(@unified_signature_overview_menu_id)," not in sql
    assert "(900410)," not in sql
    assert "(900411)," in sql
    assert "(900412)," in sql
    assert "(900413)," in sql
    assert "(900414)," in sql
    assert "(900415)," in sql
    assert "(900416)," in sql
    assert "(900417);" in sql
    for variable_name, menu_id, label, route_path, component_name in CHILD_MENUS:
        assert f"SET {variable_name} := {menu_id};" in sql
        assert f"'{label}'" in sql
        assert f", 2, " in sql
        assert "@unified_signature_menu_id" in sql
        assert f"'{route_path}'" in sql
        assert "'signature-governance/index'" in sql
        assert f"'{component_name}'" in sql
    overview_variable, overview_id, overview_label, overview_path, overview_component = REMOVED_OVERVIEW_MENU
    assert f"SET {overview_variable} := {overview_id};" in sql
    assert f"SELECT {overview_id} AS `id`, '{overview_label}'" not in sql
    assert f"'{overview_path}' AS `path`" not in sql
    assert "`id` = @unified_signature_overview_menu_id" in sql
    assert "`deleted` = b'1'" in sql
    assert "'DCC电子签名管理'" not in sql
    assert "'eDHR签名记录'" not in sql
    assert "OR `parent_id` <> @unified_signature_menu_id" not in sql
    assert "(@unified_signature_overview_menu_id, '总览'" not in sql
    assert "SELECT 900410 AS `id`, '总览'" not in sql
    assert "SELECT 900411 AS `id`, '文件签名记录'" in sql
    assert "SELECT 900412 AS `id`, '批记录签名记录'" in sql
    assert "SELECT 900413 AS `id`, '用户授权'" in sql
    assert "SELECT 900414 AS `id`, '长期留存'" in sql
    assert "SELECT 900415 AS `id`, '周期复核'" in sql
    assert "SELECT 900416 AS `id`, 'CSV质量包'" in sql
    assert "SELECT 900417 AS `id`, '统一策略'" in sql


def test_legacy_signature_menus_become_permission_items_under_matching_child_menus():
    sql = SQL_PATH.read_text(encoding="utf-8")

    for menu_id in ("6815", "900026"):
        assert f"`id` = {menu_id}" in sql
    assert "`type` = 3" in sql
    assert "`parent_id` = @unified_signature_file_menu_id" in sql
    assert "`parent_id` = @unified_signature_batch_menu_id" in sql
    assert "`permission` = 'dcc:controlled-file:signature:manage'" in sql
    assert "`permission` = 'mes:pro-batch-record-execution:signature-query'" in sql
    assert "`path` = ''" in sql
    assert "`component` = ''" in sql


def test_signature_governance_permissions_are_moved_to_matching_child_menus():
    sql = SQL_PATH.read_text(encoding="utf-8")

    for permission, parent_variable in (
        ("signature-governance:policy:query", "@unified_signature_policy_menu_id"),
        ("signature-governance:policy:manage", "@unified_signature_policy_menu_id"),
        ("signature-governance:retention:query", "@unified_signature_retention_menu_id"),
        ("signature-governance:retention:manage", "@unified_signature_retention_menu_id"),
        ("signature-governance:periodic-review:query", "@unified_signature_periodic_review_menu_id"),
        ("signature-governance:periodic-review:manage", "@unified_signature_periodic_review_menu_id"),
        ("signature-governance:csv-package:query", "@unified_signature_csv_package_menu_id"),
        ("signature-governance:csv-package:manage", "@unified_signature_csv_package_menu_id"),
    ):
        assert permission in sql
        assert parent_variable in sql
    assert "`id` <> @unified_signature_menu_id" in sql
    assert "`type` = 3" in sql
    assert "`id` NOT IN (" in sql
    for variable_name, *_ in CHILD_MENUS:
        assert variable_name in sql
    assert "@unified_signature_overview_menu_id" in sql


def test_role_menu_bindings_are_copied_to_visible_child_menus():
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert "INSERT INTO `system_role_menu`" in sql
    assert "tmp_unified_signature_child_menu_ids" in sql
    assert "SELECT DISTINCT" in sql
    assert "src.`role_id`" in sql
    assert "child.`menu_id`" in sql
    assert "(900410)" not in sql
    assert "(900417)" in sql
    assert "existing.`menu_id` = child.`menu_id`" in sql
    assert "UPDATE `system_role_menu`" in sql
    assert "`menu_id` = @unified_signature_overview_menu_id" in sql
    assert "`deleted` = b'1'" in sql
