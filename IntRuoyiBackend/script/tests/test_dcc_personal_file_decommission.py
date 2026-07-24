from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
WORKSPACE_ROOT = REPO_ROOT.parent


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def test_dcc_base_schema_does_not_seed_personal_file_menu() -> None:
    schema = read_text(REPO_ROOT / "sql/mysql/20260513_dcc_base_schema.sql")

    assert "controlled-file/mine" not in schema
    assert "dcc/controlled-file/mine/index" not in schema
    assert "DccControlledFileMine" not in schema
    assert "个人文件" not in schema
    assert "controlled-file/browser" in schema
    assert "dcc/controlled-file/browser/index" in schema


def test_dcc_personal_file_decommission_migration_retires_existing_menu() -> None:
    migration = read_text(REPO_ROOT / "sql/mysql/20260714_dcc_personal_file_decommission.sql")

    for token in [
        "controlled-file/mine",
        "dcc/controlled-file/mine/index",
        "DccControlledFileMine",
        "`deleted` = b'1'",
        "system_role_menu",
    ]:
        assert token in migration

    assert "controlled-file/browser" in migration


def test_dcc_personal_file_decommission_migration_has_release_metadata() -> None:
    migration = read_text(REPO_ROOT / "sql/mysql/20260714_dcc_personal_file_decommission.sql")

    assert migration.splitlines()[0].strip() == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260513_dcc_base_schema; type=menu; riskLevel=low"
    )


def test_frontend_personal_file_artifacts_are_removed() -> None:
    frontend_root = WORKSPACE_ROOT / "yudao-ui-admin-vue3"
    assert not (frontend_root / "src/views/dcc/controlled-file/mine").exists()

    scanned_files = [
        frontend_root / "src/router/modules/remaining.ts",
        frontend_root / "src/api/dcc/controlledFile/workflow.ts",
        frontend_root / "src/views/dcc/controlled-file/workbench/index.vue",
        frontend_root / "src/views/dcc/controlled-file/workbench/presentation.ts",
        frontend_root / "src/views/dcc/controlled-file/upload/index.vue",
        frontend_root / "src/views/dcc/controlled-file/external-review/index.vue",
    ]
    merged = "\n".join(read_text(path) for path in scanned_files)

    for forbidden in [
        "controlled-file/mine",
        "DccControlledFileMine",
        "/dcc/controlled-files/page",
        "getControlledFilePage",
        "个人文件",
    ]:
        assert forbidden not in merged

    assert "controlled-file/browser" in merged
    assert "/dcc/controlled-files/browser-page" in read_text(
        frontend_root / "src/api/dcc/controlledFile/workflow.ts"
    )
