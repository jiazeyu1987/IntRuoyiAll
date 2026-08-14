from pathlib import Path


BACKEND_ROOT = Path(__file__).resolve().parents[2]
MIGRATION = BACKEND_ROOT / "sql" / "mysql" / "20260807_mes_pqc_task_identity_closure.sql"
M6_PREFLIGHT = (
    BACKEND_ROOT / "sql" / "mysql" / "20260802_role_requirement_matrix_m6_migration_preflight.sql"
)


def read(path: Path) -> str:
    assert path.exists(), f"required file missing: {path}"
    return path.read_text(encoding="utf-8")


def test_pqc_task_identity_closure_is_release_managed_and_fail_fast() -> None:
    sql = read(MIGRATION)
    normalized = " ".join(sql.split())

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260802_mes_pqc_inspection_task,20260802_mes_process_pool_active_order_process_snapshot,"
        "20260802_mes_qa_inspection_regulation; type=schema; riskLevel=medium\n"
    )
    assert "SIGNAL SQLSTATE '45000'" in sql
    assert "PQC task process identity closure requires mes_pqc_inspection_task" in sql
    assert "PQC task process identity closure unresolved null process identities" in sql
    assert "PQC task process identity closure duplicate task identities after backfill" in sql
    assert "MODIFY COLUMN `route_process_id` bigint NOT NULL COMMENT '工艺路线工序ID'" in normalized
    assert "MODIFY COLUMN `process_id` bigint NOT NULL COMMENT '工序ID'" in normalized


def test_pqc_task_identity_closure_backfills_only_from_formal_sources() -> None:
    sql = read(MIGRATION)
    normalized = " ".join(sql.split())

    assert "JOIN `mes_qa_inspection_regulation_version` `version`" in normalized
    assert "`version`.`id` = `task`.`regulation_version_id`" in normalized
    assert "JOIN `mes_qa_inspection_regulation` `regulation`" in normalized
    assert "`regulation`.`id` = `version`.`regulation_id`" in normalized
    assert "JOIN `mes_pro_process_pool_active_order_process_snapshot` `snapshot`" in normalized
    assert "`snapshot`.`route_process_id` = `regulation`.`route_process_id`" in normalized
    assert "`snapshot`.`process_id` = `regulation`.`process_id`" in normalized
    assert "SET `task`.`route_process_id` = `regulation`.`route_process_id`" in normalized
    assert "`task`.`process_id` = `regulation`.`process_id`" in normalized

    forbidden = ["COALESCE(`task`.`route_process_id`", "IFNULL(`task`.`route_process_id`", "ORDER BY `id` LIMIT 1"]
    for token in forbidden:
        assert token not in sql


def test_m6_preflight_requires_process_id_as_part_of_pqc_task_identity() -> None:
    sql = read(M6_PREFLIGHT)
    normalized = " ".join(sql.split())

    guard_start = normalized.index("assert_rrm_m6_pqc_task_authority")
    guard = normalized[guard_start : normalized.index("END$$", guard_start)]
    assert "`route_process_id` IS NULL" in guard
    assert "`process_id` IS NULL" in guard
