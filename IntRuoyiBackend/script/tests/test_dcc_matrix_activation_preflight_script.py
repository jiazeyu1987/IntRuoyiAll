from pathlib import Path
import re


ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "script" / "dcc_matrix_activation_preflight.py"
SEED = ROOT / "sql" / "mysql" / "20260613_dcc_file_view_matrix_seed.sql"


def test_preflight_script_is_readonly_and_utf8_safe():
    text = SCRIPT.read_text(encoding="utf-8")

    assert "SELECT JSON_OBJECT" in text
    assert "UPDATE " not in text.upper()
    assert "DELETE FROM" not in text.upper()
    assert "subprocess.run" in text
    assert "mysql" in text
    assert "cmd = [" in text
    assert "????" not in text


def test_matrix_seed_still_declares_59_categories():
    seed = SEED.read_text(encoding="utf-8")
    start = seed.find("INSERT INTO tmp_dcc_file_view_matrix_category")
    end = seed.find("DROP TEMPORARY TABLE IF EXISTS tmp_dcc_file_view_matrix_department", start)
    assert start >= 0
    assert end > start
    block = seed[start:end].split("VALUES", 1)[1].rsplit(";", 1)[0]

    categories = re.findall(
        r"\('([^']*)',\s*(\d+),\s*'([^']*)',\s*'([^']*)',\s*'([^']*)'\)",
        block,
    )

    assert len(categories) == 59
    assert categories[0][3] == "市场调研报告"
    assert categories[-1][3] == "生产/检验用工装模具维护保养记录表"
