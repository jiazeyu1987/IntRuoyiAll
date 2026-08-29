from pathlib import Path


BACKEND_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = BACKEND_ROOT / "sql/mysql/20260817_dcc_registration_certificate_core.sql"
MYSQL_SCRIPT_PATH = BACKEND_ROOT / "script/tests/test-dcc-registration-certificate-core-mysql.ps1"


def _normalized(path: Path) -> str:
    return path.read_text(encoding="utf-8").lower()


def test_core_sql_repairs_legacy_registrant_name_nullability_before_assertion():
    sql = _normalized(SQL_PATH)

    repair_marker = "legacy registrant_name not null drift"
    repair_statement = (
        "modify column `registrant_name` varchar(255) default null "
        "comment 'registrant name snapshot'"
    )
    assertion_marker = "lower(actual_column.column_type) <> expected_column.column_type"

    assert repair_marker in sql
    assert repair_statement in sql
    assert sql.index(repair_marker) < sql.index(assertion_marker)


def test_mysql_core_contract_script_keeps_registrant_name_nullable():
    script = _normalized(MYSQL_SCRIPT_PATH)

    assert "modify column registrant_name varchar(255) null comment 'registrant name snapshot'" in script
    assert "modify column registrant_name varchar(255) not null comment 'registrant name snapshot'" not in script
    assert "modify column product_name varchar(255) null comment 'product name snapshot'" in script
    assert "modify column product_name varchar(255) not null comment 'product name snapshot'" in script
