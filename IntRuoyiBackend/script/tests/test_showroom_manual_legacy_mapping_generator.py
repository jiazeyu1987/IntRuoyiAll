import csv
import json
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
GENERATOR = ROOT / "script/showroom_generate_manual_legacy_mapping_sql.py"


FIELDNAMES = [
    "tenant_id",
    "product_code",
    "product_name_cn",
    "product_name_en",
    "source_row_count",
    "source_locations",
    "blocker_status",
    "blocker_reason",
    "same_x_int_code",
    "same_x_int_name_cn",
    "same_x_int_name_en",
    "same_x_current_legacy",
    "top_exact_unassigned_candidate",
    "top_unassigned_candidate",
    "top_unassigned_candidate_name_cn",
    "top_unassigned_candidate_name_en",
    "candidate_review",
    "decision_category",
    "required_before_mapping",
    "manual_decision_int_code",
    "manual_decision_final_name_cn",
    "manual_decision_action",
    "manual_decision_notes",
]


def write_csv(path: Path, rows: list[dict[str, str]]) -> None:
    with path.open("w", encoding="utf-8-sig", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=FIELDNAMES)
        writer.writeheader()
        writer.writerows(rows)


def base_row(**overrides: str) -> dict[str, str]:
    row = {field: "" for field in FIELDNAMES}
    row.update({
        "tenant_id": "122",
        "product_code": "product_066",
        "product_name_cn": "血栓抽吸导管",
        "product_name_en": "Neural Aspiration Catheter",
        "top_unassigned_candidate": "INT-67",
        "top_unassigned_candidate_name_cn": "血栓抽吸导管",
        "top_unassigned_candidate_name_en": "Neural Aspiration Catheter",
    })
    row.update(overrides)
    return row


def run_generator(input_csv: Path, output_sql: Path, report: Path, *extra_args: str) -> subprocess.CompletedProcess[str]:
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
            *extra_args,
        ],
        cwd=ROOT,
        text=True,
        encoding="utf-8",
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
    )


def test_manual_legacy_mapping_generator_blocks_when_decisions_are_blank(tmp_path):
    input_csv = tmp_path / "manual.csv"
    output_sql = tmp_path / "manual.sql"
    report = tmp_path / "report.json"
    write_csv(input_csv, [base_row()])

    result = run_generator(input_csv, output_sql, report)

    assert result.returncode == 2
    assert "BLOCKED" in result.stdout
    assert not output_sql.exists()
    payload = json.loads(report.read_text(encoding="utf-8"))
    assert payload["status"] == "BLOCKED"
    assert payload["blocker_count"] == 1


def test_manual_legacy_mapping_generator_emits_sql_for_confirmed_equal_name_row(tmp_path):
    input_csv = tmp_path / "manual.csv"
    output_sql = tmp_path / "manual.sql"
    report = tmp_path / "report.json"
    write_csv(input_csv, [
        base_row(
            manual_decision_int_code="INT-67",
            manual_decision_final_name_cn="血栓抽吸导管",
            manual_decision_action="MAP_ONLY_NAMES_ALREADY_EQUAL",
        )
    ])

    result = run_generator(input_csv, output_sql, report)

    assert result.returncode == 0, result.stdout
    sql = output_sql.read_text(encoding="utf-8")
    assert "START TRANSACTION;" in sql
    assert "UPDATE showroom_product SET legacy_product_code = 'product_066'" in sql
    assert "product_code = 'INT-67'" in sql
    assert "UPDATE showroom_product_revision" not in sql
    assert sql.rstrip().endswith("COMMIT;")
    payload = json.loads(report.read_text(encoding="utf-8"))
    assert payload["status"] == "PASS"
    assert payload["decision_count"] == 1


def test_manual_legacy_mapping_generator_requires_rename_action_when_names_differ(tmp_path):
    input_csv = tmp_path / "manual.csv"
    output_sql = tmp_path / "manual.sql"
    report = tmp_path / "report.json"
    write_csv(input_csv, [
        base_row(
            product_code="product_001",
            product_name_cn="一次性使用三通旋塞",
            product_name_en="Manifold",
            top_unassigned_candidate="INT-1",
            top_unassigned_candidate_name_cn="三通旋塞",
            top_unassigned_candidate_name_en="Manifold",
            manual_decision_int_code="INT-1",
            manual_decision_final_name_cn="一次性使用三通旋塞",
            manual_decision_action="MAP_ONLY_NAMES_ALREADY_EQUAL",
        )
    ])

    result = run_generator(input_csv, output_sql, report)

    assert result.returncode != 0
    assert "选择 MAP_ONLY 但候选 INT 名称不等于 product 名称" in result.stdout
    assert not output_sql.exists()


def test_manual_legacy_mapping_generator_can_emit_name_update_when_approved(tmp_path):
    input_csv = tmp_path / "manual.csv"
    output_sql = tmp_path / "manual.sql"
    report = tmp_path / "report.json"
    write_csv(input_csv, [
        base_row(
            product_code="product_001",
            product_name_cn="一次性使用三通旋塞",
            product_name_en="Manifold",
            top_unassigned_candidate="INT-1",
            top_unassigned_candidate_name_cn="三通旋塞",
            top_unassigned_candidate_name_en="Manifold",
            manual_decision_int_code="INT-1",
            manual_decision_final_name_cn="一次性使用三通旋塞",
            manual_decision_action="RENAME_INT_TO_PRODUCT_NAME_AND_MAP",
        )
    ])

    result = run_generator(input_csv, output_sql, report)

    assert result.returncode == 0, result.stdout
    sql = output_sql.read_text(encoding="utf-8")
    assert "UPDATE showroom_product_revision r JOIN showroom_product p" in sql
    assert "SET r.name_cn = '一次性使用三通旋塞'" in sql
    assert "UPDATE showroom_product SET legacy_product_code = 'product_001'" in sql
    payload = json.loads(report.read_text(encoding="utf-8"))
    assert payload["status"] == "PASS"
    assert payload["decisions"][0]["action"] == "RENAME_INT_TO_PRODUCT_NAME_AND_MAP"
