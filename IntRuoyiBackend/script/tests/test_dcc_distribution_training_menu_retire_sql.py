import re
from pathlib import Path


WORKSPACE_ROOT = Path(__file__).resolve().parents[3]
SQL_PATH = WORKSPACE_ROOT / "ruoyi-vue-pro/sql/mysql/20260714_dcc_distribution_training_menu_retire.sql"


def read_sql() -> str:
    return SQL_PATH.read_text(encoding="utf-8")


def test_dcc_distribution_training_menu_retire_hides_legacy_pages():
    sql = read_sql()

    assert "`visible` = b'0'" in sql, "old distribution/training menu entries must be hidden"
    assert "`status` = 1" in sql, "old menu records must become non-routable page entries"
    assert "controlled-file/distribution" in sql, "distribution menu path must be retired"
    assert "controlled-file/training" in sql, "training rule menu path must be retired"
    assert "`id` IN (6808, 6809)" in sql, "known legacy DCC menu ids must be covered"
    assert not re.search(r"^\s*DELETE\b", sql, re.IGNORECASE | re.MULTILINE), (
        "retirement must not delete historical menu rows"
    )
