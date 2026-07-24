import json
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
GENERATOR = ROOT / "script/showroom_generate_current_empty_legacy_mapping_sql.py"


def report_row(
    *,
    tenant_id: int = 1,
    target_product_code: str = "INT-67",
    target_name_cn: str = "血栓抽吸导管",
    target_name_en: str = "Neural Aspiration Catheter",
    recognized_product_code: str = "product_066",
    recognized_name_cn: str = "血栓抽吸导管",
    recognized_name_en: str = "Neural Aspiration Catheter",
    rule: str = "CN_EN_UNIQUE",
    distance: int = 1,
) -> dict:
    return {
        "tenant_id": tenant_id,
        "target_product_code": target_product_code,
        "target_name_cn": target_name_cn,
        "target_name_en": target_name_en,
        "current_revision_no": 12,
        "candidate_count": 1,
        "top_candidates": [
            {
                "product_code": recognized_product_code,
                "name_cn": recognized_name_cn,
                "name_en": recognized_name_en,
                "score": 220,
                "reasons": "CN+EN+NUM1",
                "distance": distance,
                "row_no": 65,
            }
        ],
        "recognized_product_code": recognized_product_code,
        "recognized_name_cn": recognized_name_cn,
        "recognized_name_en": recognized_name_en,
        "rule": rule,
        "distance": distance,
        "source_row_no": 65,
    }


def write_report(path: Path, recognitions: list[dict], blocked: list[dict] | None = None) -> None:
    payload = {
        "database": "127.0.0.1:23306/ruoyi-vue-pro",
        "workbook": "sample.xlsx",
        "current_product_count": 10,
        "blank_legacy_current_count": len(recognitions) + len(blocked or []),
        "workbook_product_count": 10,
        "recognized_count": len(recognitions),
        "blocked_count": len(blocked or []),
        "recognition_rule_counts": {},
        "block_reason_counts": {},
        "recognitions": recognitions,
        "blocked": blocked or [],
    }
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")


def run_generator(input_report: Path, output_sql: Path, output_report: Path) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        [
            sys.executable,
            str(GENERATOR),
            "--input-report",
            str(input_report),
            "--output-sql",
            str(output_sql),
            "--output-report",
            str(output_report),
        ],
        cwd=ROOT,
        text=True,
        encoding="utf-8",
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
    )


def test_current_empty_generator_blocks_same_number_only_name_mismatch(tmp_path):
    input_report = tmp_path / "recognition.json"
    output_sql = tmp_path / "mapping.sql"
    output_report = tmp_path / "sql-report.json"
    write_report(input_report, [
        report_row(
            target_product_code="INT-64",
            target_name_cn="按压式球囊扩张压力泵40ml/40atm",
            target_name_en="FastFlator Inflation Device",
            recognized_product_code="product_064",
            recognized_name_cn="微导管（外周）",
            recognized_name_en="RAD CROSS Micro Catheter",
            rule="SAME_NUMBER_ONLY_REVIEW",
            distance=0,
        )
    ])

    result = run_generator(input_report, output_sql, output_report)

    assert result.returncode == 2
    assert not output_sql.exists()
    payload = json.loads(output_report.read_text(encoding="utf-8"))
    assert payload["status"] == "BLOCKED"
    assert payload["mapping_count"] == 0
    assert payload["blocker_count"] == 1
    assert payload["blockers"][0]["reason"] == "SAME_NUMBER_ONLY_NAME_MISMATCH"


def test_current_empty_generator_emits_sql_for_strict_name_match_with_guards(tmp_path):
    input_report = tmp_path / "recognition.json"
    output_sql = tmp_path / "mapping.sql"
    output_report = tmp_path / "sql-report.json"
    write_report(input_report, [report_row()])

    result = run_generator(input_report, output_sql, output_report)

    assert result.returncode == 0, result.stdout
    sql = output_sql.read_text(encoding="utf-8")
    assert "START TRANSACTION;" in sql
    assert "SET legacy_product_code = 'product_066'" in sql
    assert "tenant_id = 1" in sql
    assert "product_code = 'INT-67'" in sql
    assert "legacy_product_code IS NULL" in sql
    assert "NOT EXISTS" in sql
    assert sql.rstrip().endswith("COMMIT;")
    payload = json.loads(output_report.read_text(encoding="utf-8"))
    assert payload["status"] == "PASS"
    assert payload["mapping_count"] == 1
    assert payload["blocker_count"] == 0


def test_current_empty_generator_resolves_old_code_conflict_by_strong_nearest_candidate(tmp_path):
    input_report = tmp_path / "recognition.json"
    output_sql = tmp_path / "mapping.sql"
    output_report = tmp_path / "sql-report.json"
    write_report(input_report, [
        report_row(
            target_product_code="INT-83",
            target_name_cn="可调弯导管",
            target_name_en="Steerable Catheter",
            recognized_product_code="product_149",
            recognized_name_cn="可调弯导管",
            recognized_name_en="Steerable Catheter",
            rule="CN_EN_UNIQUE",
            distance=66,
        ),
        report_row(
            target_product_code="INT-150",
            target_name_cn="可调弯导管",
            target_name_en="Steerable Catheter",
            recognized_product_code="product_149",
            recognized_name_cn="可调弯导管",
            recognized_name_en="Steerable Catheter",
            rule="CN_EN_UNIQUE",
            distance=1,
        ),
    ])

    result = run_generator(input_report, output_sql, output_report)

    assert result.returncode == 0, result.stdout
    sql = output_sql.read_text(encoding="utf-8")
    assert "product_code = 'INT-150'" in sql
    assert "product_code = 'INT-83'" not in sql
    payload = json.loads(output_report.read_text(encoding="utf-8"))
    assert payload["mapping_count"] == 1
    assert payload["mappings"][0]["target_product_code"] == "INT-150"
    assert payload["mappings"][0]["resolution"] == "CONFLICT_STRONG_UNIQUE_NEAREST"


def test_current_empty_generator_prefers_name_match_over_same_number_only(tmp_path):
    input_report = tmp_path / "recognition.json"
    output_sql = tmp_path / "mapping.sql"
    output_report = tmp_path / "sql-report.json"
    write_report(input_report, [
        report_row(
            target_product_code="INT-86",
            target_name_cn="外周支撑导管",
            target_name_en="Peripheral Support Catheter",
            recognized_product_code="product_086",
            recognized_name_cn="通路鞘组",
            recognized_name_en="Introducer Sheath Set",
            rule="SAME_NUMBER_ONLY_REVIEW",
            distance=0,
        ),
        report_row(
            target_product_code="INT-87",
            target_name_cn="通路鞘组",
            target_name_en="Introducer Sheath Set",
            recognized_product_code="product_086",
            recognized_name_cn="通路鞘组",
            recognized_name_en="Introducer Sheath Set",
            rule="EN_UNIQUE",
            distance=1,
        ),
    ])

    result = run_generator(input_report, output_sql, output_report)

    assert result.returncode == 0, result.stdout
    sql = output_sql.read_text(encoding="utf-8")
    assert "product_code = 'INT-87'" in sql
    assert "product_code = 'INT-86'" not in sql
    payload = json.loads(output_report.read_text(encoding="utf-8"))
    assert payload["mappings"][0]["recognized_product_code"] == "product_086"
    assert payload["mappings"][0]["target_product_code"] == "INT-87"


def test_current_empty_generator_blocks_far_single_cn_match(tmp_path):
    input_report = tmp_path / "recognition.json"
    output_sql = tmp_path / "mapping.sql"
    output_report = tmp_path / "sql-report.json"
    write_report(input_report, [
        report_row(
            tenant_id=122,
            target_product_code="INT-1",
            target_name_cn="三通旋塞",
            target_name_en="Three-way Stopcock",
            recognized_product_code="product_163",
            recognized_name_cn="三通旋塞",
            recognized_name_en="",
            rule="CN_UNIQUE",
            distance=162,
        )
    ])

    result = run_generator(input_report, output_sql, output_report)

    assert result.returncode == 2
    assert not output_sql.exists()
    payload = json.loads(output_report.read_text(encoding="utf-8"))
    assert payload["blockers"][0]["reason"] == "MATCH_DISTANCE_TOO_LARGE"
