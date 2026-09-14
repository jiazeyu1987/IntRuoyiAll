from __future__ import annotations

import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260812_mes_pqc_dcc_qa_c00_backfill.sql"


def _sql_text() -> str:
    return SQL_PATH.read_text(encoding="utf-8")


def _compact(text: str) -> str:
    return re.sub(r"\s+", " ", text).strip().lower()


def test_task_rule_candidate_pins_expected_key_to_target_collation() -> None:
    text = _sql_text()

    assert "CREATE TEMPORARY TABLE c00_backfill_task_rule_candidate AS" not in text

    match = re.search(
        r"CREATE\s+TEMPORARY\s+TABLE\s+c00_backfill_task_rule_candidate\s*"
        r"\((?P<body>.*?)\)\s*ENGINE=InnoDB;",
        text,
        flags=re.IGNORECASE | re.DOTALL,
    )

    assert match, "task rule candidate must declare text column collations explicitly"
    assert (
        "expected_rule_key varchar(32) character set utf8mb4 collate utf8mb4_unicode_ci default null"
        in _compact(match.group("body"))
    )


def test_manifest_hash_columns_match_legacy_task_text_collation() -> None:
    text = _compact(_sql_text())

    assert (
        "piece_detail_sha256 char(64) character set utf8mb4 collate utf8mb4_unicode_ci default null"
        in text
    )
    assert (
        "submitted_content_hash char(64) character set utf8mb4 collate utf8mb4_unicode_ci default null"
        in text
    )
    assert "task.inspection_rule_key <> candidate.expected_rule_key" in text
    assert "not (task.submitted_content_hash <=> manifest.submitted_content_hash)" in text
