from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
WORKSPACE = ROOT.parent
SQL = ROOT / "sql" / "mysql" / "20260615_system_config_package_menu.sql"
PAGE = WORKSPACE / "yudao-ui-admin-vue3" / "src" / "views" / "system" / "config-package" / "index.vue"
API = WORKSPACE / "yudao-ui-admin-vue3" / "src" / "api" / "system" / "configPackage" / "index.ts"
MANIFEST = WORKSPACE / "yudao-ui-admin-vue3" / "src" / "utils" / "frontendComponentManifest.ts"


def read_text(path: Path) -> str:
    assert path.exists(), f"missing required file: {path}"
    return path.read_text(encoding="utf-8")


def test_sql_adds_config_package_menu_without_overwrite_branch():
    sql = read_text(SQL)

    assert "DROP PROCEDURE IF EXISTS ensure_system_config_package_menu" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql
    assert "ON DUPLICATE KEY UPDATE" not in sql
    for token in [
        "910300, '配置包中心'",
        "910301, '配置包导出'",
        "910302, '配置包导入'",
        "system:config-package:query",
        "system:config-package:export",
        "system:config-package:import",
        "system/config-package/index",
        "system_role_menu",
        "system_tenant_package",
    ]:
        assert token in sql


def test_sql_and_frontend_component_path_match():
    sql = read_text(SQL)
    page = read_text(PAGE)
    api = read_text(API)
    manifest = read_text(MANIFEST)

    assert "defineOptions({ name: 'SystemConfigPackage' })" in page
    assert "v-hasPermi=\"['system:config-package:export']\"" in page
    assert "v-hasPermi=\"['system:config-package:import']\"" in page
    assert "/system/config-package/export-excel" in api
    assert "/system/config-package/precheck" in api
    assert "/system/config-package/import" in api
    assert "import.meta.glob('../views/**/*.{vue,tsx}')" in manifest
    assert "system/config-package/index" in sql


def test_sql_does_not_use_hidden_fallback_or_empty_silence():
    sql = read_text(SQL)
    page = read_text(PAGE)

    for forbidden in ["catch {}", "catch{}", "fallback", "兼容"]:
        assert forbidden not in sql
        assert forbidden not in page
