from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def test_repository_keeps_showroom_backend_scaffold_on_mainline() -> None:
    root_pom = (REPO_ROOT / "pom.xml").read_text(encoding="utf-8")
    server_pom = (REPO_ROOT / "yudao-server" / "pom.xml").read_text(encoding="utf-8")

    assert "yudao-module-showroom" in root_pom, (
        "root pom should include the showroom module after the mainline restore"
    )
    assert "yudao-module-showroom" in server_pom, (
        "server pom should depend on the showroom module after the mainline restore"
    )
    assert (REPO_ROOT / "yudao-module-showroom").exists(), (
        "repository should keep the showroom module directory after the mainline restore"
    )
    assert (REPO_ROOT / "sql" / "showroom" / "20260519_showroom_v1_schema.sql").exists(), (
        "repository should keep the showroom schema after the mainline restore"
    )
    assert (REPO_ROOT / "script" / "tests" / "test_showroom_sql_scripts.py").exists(), (
        "repository should keep the showroom SQL regression test after the mainline restore"
    )
