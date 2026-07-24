import csv
import json
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
GENERATOR = ROOT / "script/showroom_generate_english_name_legacy_mapping_sql.py"


FIELDNAMES = [
    "tenant_id",
    "product_code",
    "product_name_cn",
    "product_name_en",
    "candidate_int_code",
    "candidate_int_name_cn",
    "candidate_int_name_en",
    "candidate_legacy_product_code",
    "score",
    "strict_equal_cn",
    "strict_equal_en",
    "source_status",
    "source_reason",
]


def write_audit_csv(path: Path, rows: list[dict[str, str]]) -> None:
    with path.open("w", encoding="utf-8-sig", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=FIELDNAMES)
        writer.writeheader()
        writer.writerows(rows)


def row(**overrides: str) -> dict[str, str]:
    data = {field: "" for field in FIELDNAMES}
    data.update({
        "tenant_id": "122",
        "product_code": "product_015",
        "product_name_cn": "按压式球囊扩充压力泵",
        "product_name_en": "Inflation Device II",
        "candidate_int_code": "INT-15",
        "candidate_int_name_cn": "按压式球囊扩张压力泵",
        "candidate_int_name_en": "Inflation\u00a0Device   II",
        "candidate_legacy_product_code": "",
        "source_status": "UNMAPPED_NO_EXACT_CN",
    })
    data.update(overrides)
    return data


def run_generator(input_csv: Path, output_sql: Path, report: Path) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        [
            sys.executable,
            str(GENERATOR),
            "--input",
            str(input_csv),
            "--output-sql",
            str(output_sql),
            "--report",
            str(report),
        ],
        cwd=ROOT,
        text=True,
        encoding="utf-8",
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
    )


def test_english_name_generator_emits_sql_for_unique_normalized_english_match(tmp_path):
    input_csv = tmp_path / "audit.csv"
    output_sql = tmp_path / "english.sql"
    report = tmp_path / "report.json"
    write_audit_csv(input_csv, [row()])

    result = run_generator(input_csv, output_sql, report)

    assert result.returncode == 0, result.stdout
    sql = output_sql.read_text(encoding="utf-8")
    assert "START TRANSACTION;" in sql
    assert "UPDATE showroom_product" in sql
    assert "SET legacy_product_code = 'product_015'" in sql
    assert "tenant_id = 122" in sql
    assert "product_code = 'INT-15'" in sql
    assert "legacy_product_code IS NULL" in sql
    assert "NOT EXISTS" in sql
    assert "UPDATE showroom_product_revision" not in sql
    assert sql.rstrip().endswith("COMMIT;")
    payload = json.loads(report.read_text(encoding="utf-8"))
    assert payload["status"] == "PASS"
    assert payload["mapping_count"] == 1
    assert payload["blocker_count"] == 0


def test_english_name_generator_blocks_multi_candidate_matches(tmp_path):
    input_csv = tmp_path / "audit.csv"
    output_sql = tmp_path / "english.sql"
    report = tmp_path / "report.json"
    write_audit_csv(input_csv, [
        row(candidate_int_code="INT-67", product_code="product_066", product_name_en="Neural Aspiration Catheter",
            candidate_int_name_en="Neural Aspiration Catheter"),
        row(candidate_int_code="INT-97", product_code="product_066", product_name_en="Neural Aspiration Catheter",
            candidate_int_name_en="Neural Aspiration Catheter"),
    ])

    result = run_generator(input_csv, output_sql, report)

    assert result.returncode == 2
    assert not output_sql.exists()
    payload = json.loads(report.read_text(encoding="utf-8"))
    assert payload["status"] == "BLOCKED"
    assert payload["mapping_count"] == 0
    assert payload["blocker_count"] == 1
    assert payload["blockers"][0]["reason"] == "ENGLISH_NAME_MATCH_NOT_UNIQUE"


def test_english_name_generator_emits_sql_for_unique_matches_and_reports_remaining_blockers(tmp_path):
    input_csv = tmp_path / "audit.csv"
    output_sql = tmp_path / "english.sql"
    report = tmp_path / "report.json"
    write_audit_csv(input_csv, [
        row(),
        row(candidate_int_code="INT-67", product_code="product_066", product_name_en="Neural Aspiration Catheter",
            candidate_int_name_en="Neural Aspiration Catheter"),
        row(candidate_int_code="INT-97", product_code="product_066", product_name_en="Neural Aspiration Catheter",
            candidate_int_name_en="Neural Aspiration Catheter"),
    ])

    result = run_generator(input_csv, output_sql, report)

    assert result.returncode == 0, result.stdout
    sql = output_sql.read_text(encoding="utf-8")
    assert "product_015" in sql
    assert "product_066" not in sql
    payload = json.loads(report.read_text(encoding="utf-8"))
    assert payload["status"] == "PASS_WITH_BLOCKERS"
    assert payload["mapping_count"] == 1
    assert payload["blocker_count"] == 1
    assert payload["blockers"][0]["reason"] == "ENGLISH_NAME_MATCH_NOT_UNIQUE"


def test_english_name_generator_resolves_exact_name_multi_candidate_by_unique_nearest_number(tmp_path):
    input_csv = tmp_path / "audit.csv"
    output_sql = tmp_path / "english.sql"
    report = tmp_path / "report.json"
    write_audit_csv(input_csv, [
        row(candidate_int_code="INT-67", product_code="product_066", product_name_cn="血栓抽吸导管",
            product_name_en="Neural Aspiration Catheter", candidate_int_name_cn="血栓抽吸导管",
            candidate_int_name_en="Neural Aspiration Catheter"),
        row(candidate_int_code="INT-97", product_code="product_066", product_name_cn="血栓抽吸导管",
            product_name_en="Neural Aspiration Catheter", candidate_int_name_cn="血栓抽吸导管",
            candidate_int_name_en="Neural Aspiration Catheter"),
    ])

    result = run_generator(input_csv, output_sql, report)

    assert result.returncode == 0, result.stdout
    sql = output_sql.read_text(encoding="utf-8")
    assert "SET legacy_product_code = 'product_066'" in sql
    assert "product_code = 'INT-67'" in sql
    assert "product_code = 'INT-97'" not in sql
    payload = json.loads(report.read_text(encoding="utf-8"))
    assert payload["status"] == "PASS"
    assert payload["mapping_count"] == 1
    assert payload["mappings"][0]["resolution"] == "PROXIMITY_UNIQUE_NEAREST"
    assert payload["mappings"][0]["target_int_code"] == "INT-67"


def test_english_name_generator_blocks_proximity_when_distance_is_not_close(tmp_path):
    input_csv = tmp_path / "audit.csv"
    output_sql = tmp_path / "english.sql"
    report = tmp_path / "report.json"
    write_audit_csv(input_csv, [
        row(candidate_int_code="INT-72", product_code="product_066", product_name_cn="血栓抽吸导管",
            product_name_en="Neural Aspiration Catheter", candidate_int_name_cn="血栓抽吸导管",
            candidate_int_name_en="Neural Aspiration Catheter"),
        row(candidate_int_code="INT-97", product_code="product_066", product_name_cn="血栓抽吸导管",
            product_name_en="Neural Aspiration Catheter", candidate_int_name_cn="血栓抽吸导管",
            candidate_int_name_en="Neural Aspiration Catheter"),
    ])

    result = run_generator(input_csv, output_sql, report)

    assert result.returncode == 2
    assert not output_sql.exists()
    payload = json.loads(report.read_text(encoding="utf-8"))
    assert payload["status"] == "BLOCKED"
    assert payload["blockers"][0]["reason"] == "PROXIMITY_NEAREST_DISTANCE_TOO_LARGE"


def test_english_name_generator_blocks_missing_occupied_and_non_int_candidates(tmp_path):
    input_csv = tmp_path / "audit.csv"
    output_sql = tmp_path / "english.sql"
    report = tmp_path / "report.json"
    write_audit_csv(input_csv, [
        row(product_code="product_001", product_name_en="Manifold", candidate_int_code="INT-1",
            candidate_int_name_en="Manifold for Single"),
        row(product_code="product_002", product_name_en="Occupied", candidate_int_code="INT-2",
            candidate_int_name_en="Occupied", candidate_legacy_product_code="product_200"),
        row(product_code="product_003", product_name_en="Legacy", candidate_int_code="product_003",
            candidate_int_name_en="Legacy"),
    ])

    result = run_generator(input_csv, output_sql, report)

    assert result.returncode == 2
    assert not output_sql.exists()
    payload = json.loads(report.read_text(encoding="utf-8"))
    assert payload["status"] == "BLOCKED"
    assert payload["mapping_count"] == 0
    assert payload["blocker_count"] == 3
    assert {blocker["reason"] for blocker in payload["blockers"]} == {
        "ENGLISH_NAME_MATCH_NOT_FOUND",
        "CANDIDATE_LEGACY_CODE_ALREADY_SET",
        "CANDIDATE_CODE_NOT_CURRENT_INT",
    }
