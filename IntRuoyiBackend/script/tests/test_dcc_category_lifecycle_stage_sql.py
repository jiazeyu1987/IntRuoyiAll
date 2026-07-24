from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
BASE_SCHEMA = REPO_ROOT / "sql" / "mysql" / "20260513_dcc_base_schema.sql"
REPAIR_SCHEMA = REPO_ROOT / "sql" / "mysql" / "20260515_dcc_runtime_schema_repair.sql"
TEST_SCHEMA = REPO_ROOT / "yudao-module-dcc" / "src" / "test" / "resources" / "sql" / "create_tables.sql"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def test_category_lifecycle_stage_schema_contract():
    base = read(BASE_SCHEMA)
    repair = read(REPAIR_SCHEMA)
    test_schema = read(TEST_SCHEMA)

    for schema in (base, repair, test_schema):
        assert "lifecycle_stage" in schema

    assert "`lifecycle_stage` varchar(32) not null" in base.lower()
    assert "`lifecycle_stage` varchar(32) not null" in test_schema.lower()
    assert "CALL ensure_dcc_column(" in repair
    assert "'lifecycle_stage'" in repair


def test_runtime_repair_backfills_all_confirmed_category_stage_groups_and_blocks_unknowns():
    repair = read(REPAIR_SCHEMA)

    for token in [
        "DCC_FVM_DHF_004",
        "DCC_FVM_DHF_001",
        "DCC_FVM_DHF_011",
        "DCC_FVM_DHF_012",
        "DCC_FVM_DHF_017",
        "DCC_FVM_DHF_019",
        "DCC_FVM_DHF_030",
        "DCC_FVM_DMR_%",
        "DCC_OTHER_TEMPLATE_%",
        "QMSFC-0001",
        "QMSFC-0011",
        "QMSFC-0012",
        "QMSFC-0025",
        "QMSFC-0036",
        "QMSFC-0048",
        "INTAUTH-1",
        "INTAUTH-28",
        "INTAUTH-36",
        "INTAUTH-48",
        "NASCAT-%",
        "CODEX_DCC_LOCAL\\_%",
        "CODEX_E2E\\_%",
    ]:
        assert token in repair

    for stage in ["PLAN", "INPUT", "OUTPUT", "VERIFICATION", "VALIDATION", "TRANSFER"]:
        assert stage in repair

    assert "SIGNAL SQLSTATE '45000'" in repair
    assert "DCC category lifecycle_stage backfill incomplete" in repair
    assert "WHERE `deleted` <> 0" in repair
    assert "SET `lifecycle_stage` = 'TRANSFER'" in repair
    assert "DCC category lifecycle_stage not-null normalization incomplete" in repair
    assert "ensure_dcc_category_lifecycle_stage_not_null_ready" in repair
    assert "ALTER TABLE `dcc_file_category` MODIFY COLUMN `lifecycle_stage` varchar(32) NOT NULL" in repair
