from pathlib import Path


BACKEND_ROOT = Path(__file__).resolve().parents[2]
MIGRATION = BACKEND_ROOT / "sql" / "mysql" / "20260812_mes_qa_pressure_pump_idi_seed.sql"
PREFLIGHT = (
    BACKEND_ROOT
    / "sql"
    / "mysql"
    / "target-preflight"
    / "20260812_mes_qa_pressure_pump_idi_seed.preflight.sql"
)


def read_migration() -> str:
    assert MIGRATION.exists(), f"missing migration: {MIGRATION}"
    return MIGRATION.read_text(encoding="utf-8")


def read_preflight() -> str:
    assert PREFLIGHT.exists(), f"missing target preflight: {PREFLIGHT}"
    return PREFLIGHT.read_text(encoding="utf-8")


def test_idi_seed_noops_when_all_legacy_source_rows_are_absent() -> None:
    sql = read_migration()
    normalized = " ".join(sql.split())

    assert "v_source_join_count = 0 AND v_source_reg_count = 0" in normalized
    assert "COMMIT; LEAVE seed_block;" in normalized
    assert normalized.index("v_source_join_count = 0 AND v_source_reg_count = 0") < normalized.index(
        "INSERT INTO mes_qa_inspection_regulation ("
    )


def test_idi_seed_still_blocks_partial_or_duplicate_legacy_source_rows() -> None:
    sql = read_migration()
    normalized = " ".join(sql.split())

    assert "v_source_join_count <> 3 OR v_source_reg_count <> 3" in normalized
    assert "IDI旧QA规程源数据必须为空或唯一命中3条" in sql
    assert "COUNT(DISTINCT source_item.item_code), COUNT(1)" in sql
    assert "IDI旧QA规程源项目必须为22个逻辑项目和64条检验类型行" in sql


def test_idi_target_preflight_checks_empty_or_complete_legacy_source_contract() -> None:
    sql = read_preflight()
    normalized = " ".join(sql.split())

    for source_name in (
        "按压式球囊扩充压力泵组装过程检验规程-清洗工序",
        "按压式球囊扩充压力泵组装过程检验规程-清洁工序",
        "按压式球囊扩充压力泵组装过程检验规程-大包装工序",
    ):
        assert source_name in sql

    assert "legacy_source_summary" in normalized
    assert "source_reg_count = 0" in normalized
    assert "source_reg_count = 3" in normalized
    assert "logical_item_count = 22" in normalized
    assert "item_row_count = 64" in normalized
    assert "TARGET_PREFLIGHT_PASS:20260812_mes_qa_pressure_pump_idi_seed" in sql
