from pathlib import Path
import subprocess
import sys

import pytest
from openpyxl import Workbook

ROOT = Path(__file__).resolve().parents[2]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from script.registration_certificate_migration_preflight import (
    EXPECTED_SOURCE_SHA256,
    MIGRATION_PREFLIGHT_PERMISSION,
    MigrationPreflightError,
    MigrationPreflightPermissionError,
    assert_preflight_permission,
    build_preflight_report,
)


SOURCE_WORKBOOK = Path(r"C:\Users\BJB110\Desktop\文档\1\医疗器械注册证信息表20260814.xlsx")

HEADERS = ["公司名称", "项目代码", "产品名称", "注册证号", "首次获证日期", "生效日期", "有效期至", "类别", "备注"]


def write_workbook(path, domestic_rows, extra_sheets=True):
    wb = Workbook()
    ws = wb.active
    ws.title = "国内注册证"
    ws.append(HEADERS)
    for row in domestic_rows:
        ws.append([row.get(header, "") for header in HEADERS])
    if extra_sheets:
        for sheet_name in ["CE证书", "FDA证书", "公司证件"]:
            other = wb.create_sheet(sheet_name)
            other.append(["名称", "证号"])
            other.append([sheet_name, "SHOULD_NOT_IMPORT"])
    wb.save(path)


def test_official_workbook_preflight_freezes_hash_counts_and_excluded_sheets():
    report = build_preflight_report(SOURCE_WORKBOOK, permissions={MIGRATION_PREFLIGHT_PERMISSION})

    assert report["source_sha256"] == EXPECTED_SOURCE_SHA256
    assert report["domestic_row_count"] == 119
    assert report["company_count"] == 8
    assert report["missing_project_code_count"] == 44
    assert report["sheet_import_counts"] == {
        "国内注册证": 119,
        "CE证书": 0,
        "FDA证书": 0,
        "公司证件": 0,
    }
    assert report["excluded_sheet_row_counts"] == {
        "CE证书": 50,
        "FDA证书": 24,
        "公司证件": 42,
    }
    assert len(report["rows"]) == 119
    assert {row["review_status"] for row in report["rows"]} == {"NEEDS_REVIEW"}
    assert not [row for row in report["rows"] if row["review_status"] == "READY"]


def test_preflight_requires_global_migration_permission():
    with pytest.raises(MigrationPreflightPermissionError):
        assert_preflight_permission(set())

    assert_preflight_permission({MIGRATION_PREFLIGHT_PERMISSION})
    assert_preflight_permission({"system:migration:admin"})


def test_row_level_review_keeps_raw_normalized_evidence_and_never_infers_missing_facts(tmp_path):
    workbook = tmp_path / "source.xlsx"
    write_workbook(workbook, [{
        "公司名称": "上海瑛泰医疗器械股份有限公司",
        "项目代码": "",
        "产品名称": "一次性使用造影导管",
        "注册证号": "沪械注准20252030001",
        "首次获证日期": "2020.1.2",
        "生效日期": "2026.8.14",
        "有效期至": "2031-08-13",
        "类别": "二类",
        "备注": "委托山东瑛泰生产+自行生产",
    }])

    report = build_preflight_report(workbook, permissions={MIGRATION_PREFLIGHT_PERMISSION}, expected_sha256=None)
    row = report["rows"][0]

    assert row["source_sheet"] == "国内注册证"
    assert row["source_row"] == 2
    assert row["raw"]["项目代码"] == ""
    assert row["normalized"]["project_code"] is None
    assert row["normalized"]["first_obtained_date"] == "2020-01-02"
    assert row["normalized"]["effective_date"] == "2026-08-14"
    assert row["normalized"]["expiry_date"] == "2031-08-13"
    assert row["normalized"]["approval_date"] is None
    assert row["normalized"]["registrant_name"] is None
    assert row["normalized"]["production_relation"] is None
    assert row["normalized"]["attachment_evidence"] is None
    assert row["evidence"]["remark"]["raw"] == "委托山东瑛泰生产+自行生产"
    assert row["evidence"]["approval_date"]["status"] == "MISSING_FORMAL_EVIDENCE"
    assert row["evidence"]["registrant_name"]["status"] == "MISSING_FORMAL_EVIDENCE"
    assert row["evidence"]["production_relation"]["status"] == "MISSING_FORMAL_EVIDENCE"
    assert row["evidence"]["attachment_evidence"]["status"] == "MISSING_FORMAL_EVIDENCE"
    assert row["review_status"] == "NEEDS_REVIEW"
    assert {
        "missing_project_code",
        "missing_approval_date",
        "missing_registrant_name",
        "missing_production_relation",
        "missing_attachment_evidence",
        "missing_owner_company_mapping",
        "missing_product_mapping",
    }.issubset(set(row["review_reasons"]))


def test_non_domestic_sheets_are_reported_but_never_imported(tmp_path):
    workbook = tmp_path / "source.xlsx"
    write_workbook(workbook, [
        {
            "公司名称": "公司A",
            "项目代码": "A1",
            "产品名称": "产品A",
            "注册证号": "A-CERT",
            "首次获证日期": "2020.1.2",
            "生效日期": "2021.2.3",
            "有效期至": "2026-02-02",
            "类别": "二类",
            "备注": "",
        },
        {
            "公司名称": "公司B",
            "项目代码": "",
            "产品名称": "产品B",
            "注册证号": "B-CERT",
            "首次获证日期": "2020.1.2",
            "生效日期": "2021.2.3",
            "有效期至": "2026-02-02",
            "类别": "二类",
            "备注": "",
        },
    ])

    report = build_preflight_report(workbook, permissions={MIGRATION_PREFLIGHT_PERMISSION}, expected_sha256=None)

    assert report["domestic_row_count"] == 2
    assert report["company_count"] == 2
    assert report["missing_project_code_count"] == 1
    assert report["sheet_import_counts"] == {
        "国内注册证": 2,
        "CE证书": 0,
        "FDA证书": 0,
        "公司证件": 0,
    }
    assert report["excluded_sheet_row_counts"] == {
        "CE证书": 1,
        "FDA证书": 1,
        "公司证件": 1,
    }


def test_source_hash_mismatch_fails_fast(tmp_path):
    workbook = tmp_path / "source.xlsx"
    write_workbook(workbook, [{
        "公司名称": "公司A",
        "项目代码": "A1",
        "产品名称": "产品A",
        "注册证号": "A-CERT",
        "首次获证日期": "2020.1.2",
        "生效日期": "2021.2.3",
        "有效期至": "2026-02-02",
        "类别": "二类",
        "备注": "",
    }], extra_sheets=False)

    with pytest.raises(MigrationPreflightError):
        build_preflight_report(workbook, permissions={MIGRATION_PREFLIGHT_PERMISSION}, expected_sha256="WRONG")


def test_cli_creates_parent_directory_for_review_report(tmp_path):
    workbook = tmp_path / "source.xlsx"
    write_workbook(workbook, [{
        "公司名称": "公司A",
        "项目代码": "A1",
        "产品名称": "产品A",
        "注册证号": "A-CERT",
        "首次获证日期": "2020.1.2",
        "生效日期": "2021.2.3",
        "有效期至": "2026-02-02",
        "类别": "二类",
        "备注": "",
    }], extra_sheets=False)
    output = tmp_path / "nested" / "review" / "preflight.json"

    cp = subprocess.run([
        "python", "-X", "utf8", str(ROOT / "script" / "registration_certificate_migration_preflight.py"),
        "--workbook", str(workbook),
        "--expected-sha256", "",
        "--output-json", str(output),
    ], cwd=ROOT, text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=False)

    assert cp.returncode == 0, cp.stderr
    assert output.exists()
