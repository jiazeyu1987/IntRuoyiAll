import subprocess
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "script" / "dcc_view_matrix_restore_sql.py"


def run_restore(args):
    return subprocess.run(
        ["python", "-X", "utf8", str(SCRIPT), *args],
        cwd=ROOT,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
    )


def test_restore_sql_uses_official_matrix_seed_and_grants_current_directories_to_matrix_subjects():
    cp = run_restore(["--print-sql"])

    assert cp.returncode == 0, cp.stderr
    sql = cp.stdout
    assert "tmp_dcc_fvm_restore_category" in sql
    assert "DCC_FVM_DHF_001" in sql
    assert "DCC_FVM_DMR_024" in sql
    assert "expected_matrix_view_rules" in sql
    assert "legacy_rule.action_type = 'DOWNLOAD'" in sql
    assert "role.code IN ('wenkong', 'wenkong_download')" in sql
    assert "INSERT INTO dcc_directory_access_rule" in sql
    assert "CROSS JOIN tmp_dcc_fvm_restore_directory_subject" in sql
    assert "DCC_VIEW_MATRIX_RESTORE_DIRECTORY_PRECHECK_FAILED" in sql
    assert "DCC_VIEW_MATRIX_RESTORE_NO_ORIGINAL_DIRECTORY_RULES" not in sql
    assert "??" not in sql


def test_restore_script_feeds_mysql_with_utf8_bytes():
    text = SCRIPT.read_text(encoding="utf-8")

    assert "input=sql.encode(\"utf-8\")" in text
    assert "--default-character-set=utf8mb4" in text
    assert "text=True" not in text


def test_restore_fails_when_seed_is_missing(tmp_path):
    cp = run_restore(["--seed-sql", str(tmp_path / "missing.sql"), "--print-sql"])

    assert cp.returncode != 0
    assert "seed sql does not exist" in cp.stderr
