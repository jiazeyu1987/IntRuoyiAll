from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MIGRATION_SQL = ROOT / "sql" / "mysql" / "20260626_role_name_garbled_repair.sql"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def test_role_name_garbled_repair_sql_exists():
    assert MIGRATION_SQL.exists(), "必须提供正式角色乱码修复 SQL：20260626_role_name_garbled_repair.sql"


def test_role_name_garbled_repair_sql_contract():
    source = read(MIGRATION_SQL)

    assert source.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=data; riskLevel=low\n"
    )

    for token in [
        "UPDATE `system_role`",
        "WHERE `id` = 910208",
        "WHERE `id` = 910209",
        "WHERE `id` = 910234",
        "WHERE `id` = 910235",
        "WHERE `id` = 910236",
        "WHERE `id` = 910237",
        "CONVERT(0x454449544F52 USING utf8mb4)",
        "CONVERT(0xE5B195E58E85E7BC96E8BE91 USING utf8mb4)",
        "CONVERT(0xE68F90E4BE9BE5B195E58E85E7BC96E8BE91 USING utf8mb4)",
        "CONVERT(0xE4BC81E5AEA3E8A792E889B2 USING utf8mb4)",
        "CONVERT(0x65444852E6BC94E7BB832DE689A7E8A18CE4BABA USING utf8mb4)",
        "CONVERT(0x65444852E6BC94E7BB832DE5AEA1E689B9E4BABA USING utf8mb4)",
        "CONVERT(0x65444852E6BC94E7BB832DE5BD92E6A1A3E59198 USING utf8mb4)",
        "CONVERT(0x65444852E6BC94E7BB832DE58FAAE8AFBB USING utf8mb4)",
    ]:
        assert token in source, f"角色乱码修复 SQL 缺少必要契约: {token}"
