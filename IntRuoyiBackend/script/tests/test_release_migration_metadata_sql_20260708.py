from pathlib import Path


SQL_ROOT = Path(__file__).resolve().parents[2] / "sql" / "mysql"


def read_first_line(name: str) -> str:
    return (SQL_ROOT / name).read_text(encoding="utf-8").splitlines()[0]


def test_mes_balloon_process_device_capacity_has_release_metadata() -> None:
    assert read_first_line("20260708_mes_balloon_process_device_capacity.sql") == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260513_mes_dv_machinery_extend; type=schema; riskLevel=medium"
    )


def test_mes_batch_record_version_phase_one_has_release_metadata() -> None:
    assert read_first_line("20260708_mes_batch_record_version_phase_one.sql") == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260514_mes_batch_record_report,20260612_mes_edhr_multi_batch_route; "
        "type=schema; riskLevel=medium"
    )


def test_balloon_process_device_capacity_avoids_reopening_seed_tables() -> None:
    sql = (SQL_ROOT / "20260708_mes_balloon_process_device_capacity.sql").read_text(encoding="utf-8")
    assert "WHERE NOT EXISTS (" not in sql
    assert "LEFT JOIN `mes_dv_machinery_process` existing" in sql
    assert "LEFT JOIN `mes_pro_route_process` existing" in sql
    assert "FROM `tmp_balloon_machinery_process_seed` seed\n    WHERE NOT EXISTS" not in sql
    assert "LEFT JOIN `tmp_balloon_route_process_seed` next_seed" not in sql
    assert "next_seed_lookup" not in sql
    assert "tmp_balloon_route_process_next_seed" in sql