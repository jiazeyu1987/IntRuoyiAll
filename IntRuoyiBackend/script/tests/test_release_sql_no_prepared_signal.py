from pathlib import Path
import re

REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_ROOT = REPO_ROOT / "sql" / "mysql"


def _statement_windows(text: str):
    lines = text.splitlines()
    for index, line in enumerate(lines):
        if re.search(r"\bPREPARE\s+\w+\s+FROM\s+@sql\b", line, re.IGNORECASE):
            start = max(0, index - 8)
            yield index + 1, "\n".join(lines[start:index + 1])


def test_release_sql_does_not_prepare_signal_statements() -> None:
    offenders = []
    for sql_path in sorted(SQL_ROOT.glob("*.sql")):
        text = sql_path.read_text(encoding="utf-8")
        for line_number, window in _statement_windows(text):
            if re.search(r"SIGNAL\s+SQLSTATE", window, re.IGNORECASE):
                offenders.append(f"{sql_path.relative_to(REPO_ROOT)}:{line_number}")
    assert not offenders, "SIGNAL SQLSTATE cannot be executed through PREPARE: " + ", ".join(offenders)