from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MIGRATION_SQL = ROOT / "sql/mysql/20260707_system_role_category_management.sql"
BASE_SCHEMA_SQL = ROOT / "sql/mysql/ruoyi-vue-pro.sql"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def test_role_category_migration_has_release_metadata() -> None:
    sql = read(MIGRATION_SQL)

    assert sql.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=; type=schema; riskLevel=medium"
    )


def test_role_category_migration_creates_tenant_scoped_category_schema() -> None:
    sql = read(MIGRATION_SQL)

    for token in [
        "CREATE TABLE IF NOT EXISTS `system_role_category`",
        "`tenant_id` bigint NOT NULL DEFAULT 0",
        "UNIQUE KEY `uk_role_category_tenant_code` (`tenant_id`, `code`, `deleted`)",
        "ALTER TABLE `system_role` ADD COLUMN `category_id` bigint NULL",
        "ALTER TABLE `system_role` ADD KEY `idx_role_category_id` (`category_id`)",
    ]:
        assert token in sql


def test_role_category_migration_seeds_default_categories_and_permissions() -> None:
    sql = read(MIGRATION_SQL)

    for token in [
        "'展厅' AS category_name, 'showroom'",
        "UNION ALL SELECT '批记录', 'batch-record'",
        "UNION ALL SELECT '排产', 'scheduling'",
        "UNION ALL SELECT '文控', 'dcc'",
        "UNION ALL SELECT 'SRM', 'srm'",
        "UNION ALL SELECT '菜单', 'menu'",
        "'system:role-category:query'",
        "'system:role-category:create'",
        "'system:role-category:update'",
        "'system:role-category:delete'",
    ]:
        assert token in sql


def test_role_category_migration_fails_fast_for_unmatched_roles() -> None:
    sql = read(MIGRATION_SQL)

    assert "SELECT unmatched_roles AS unmatched_roles" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql
    assert "角色分类迁移存在未匹配历史角色" in sql
    assert "WHERE `deleted` = b'0'\n    AND `category_id` IS NULL" in sql
    assert "SELECT '未分类'" not in sql
    assert "'uncategorized'" not in sql


def test_role_category_migration_uses_declared_keyword_rules() -> None:
    sql = read(MIGRATION_SQL)

    for token in [
        "LIKE '%showroom%'",
        "LIKE '%展厅%'",
        "LIKE '%edhr%'",
        "LIKE '%batch%'",
        "LIKE '%pressure_pump%'",
        "LIKE '%批记录%'",
        "LIKE '%schedule%'",
        "LIKE '%scheduler%'",
        "LOWER(role.`code`) LIKE 'post_release_mes_smoke_%'",
        "LIKE '%排产%'",
        "LIKE '%dcc%'",
        "LOWER(role.`code`) IN ('doc_control', 'wenkong', 'wenkong_download')",
        "LIKE '%文控%'",
        "LIKE '%体系工程师%'",
        "LIKE '%srm%'",
        "LIKE '%menu%'",
        "LOWER(role.`code`) IN ('super_admin', 'common', 'tenant_admin', 'crm_admin', 'test-dp', 'approval_center_entry', 'approval_admin')",
        "LIKE '%system:role%'",
        "LIKE '%system:permission%'",
        "LIKE '%菜单%'",
    ]:
        assert token in sql


def test_role_category_base_schema_contains_category_table_and_query_permission() -> None:
    sql = read(BASE_SCHEMA_SQL)

    assert "CREATE TABLE `system_role_category`" in sql
    assert "`category_id` bigint NULL DEFAULT NULL COMMENT '角色分类编号'" in sql
    assert "KEY `idx_role_category_id` (`category_id`) USING BTREE" in sql
    assert "'角色分类查询', 'system:role-category:query'" in sql
    assert "'角色分类新增', 'system:role-category:create'" in sql
    assert "'角色分类修改', 'system:role-category:update'" in sql
    assert "'角色分类删除', 'system:role-category:delete'" in sql
