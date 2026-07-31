from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MIGRATION = ROOT / "sql" / "mysql" / "20260731_dcc_file_category_match_rule.sql"
TEST_SCHEMA = ROOT / "yudao-module-dcc" / "src" / "test" / "resources" / "sql" / "create_tables.sql"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8").lower()


def test_migration_has_release_metadata_and_required_columns():
    sql = read(MIGRATION)
    assert sql.startswith("-- release-migration:")
    assert "type=schema" in sql
    assert "create table if not exists `dcc_file_category_match_rule`" in sql
    for column in [
        "`category_id` bigint not null",
        "`match_text` varchar(255) not null",
        "`match_type` varchar(32) not null",
        "`weight` int not null default 0",
        "`active` bit(1) not null default b'1'",
        "`tenant_id` bigint not null default 0",
        "`deleted` bit(1) not null default b'0'",
    ]:
        assert column in sql


def test_unit_test_schema_contains_match_rule_table():
    sql = read(TEST_SCHEMA)
    assert "create table if not exists `dcc_file_category_match_rule`" in sql
    for column in ["`category_id` bigint not null", "`match_text` varchar(255) not null",
                   "`match_type` varchar(32) not null", "`weight` int not null default 0"]:
        assert column in sql
