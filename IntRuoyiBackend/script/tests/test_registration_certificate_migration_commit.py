from pathlib import Path
import sys

import pytest
from openpyxl import Workbook

ROOT = Path(__file__).resolve().parents[2]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from script.registration_certificate_migration_commit import (
    MIGRATION_COMMIT_PERMISSION,
    MigrationCommitPermissionError,
    MigrationCommitPolicyError,
    build_commit_plan,
)
from script.registration_certificate_migration_preflight import (
    EXPECTED_SOURCE_SHA256,
    MIGRATION_PREFLIGHT_PERMISSION,
    build_preflight_report,
)


SOURCE_WORKBOOK = Path(r"C:\Users\BJB110\Desktop\文档\1\医疗器械注册证信息表20260814.xlsx")
HEADERS = ["公司名称", "项目代码", "产品名称", "注册证号", "首次获证日期", "生效日期", "有效期至", "类别", "备注"]


def write_workbook(path, rows):
    wb = Workbook()
    ws = wb.active
    ws.title = "国内注册证"
    ws.append(HEADERS)
    for row in rows:
        ws.append([row.get(header, "") for header in HEADERS])
    for name in ["CE证书", "FDA证书", "公司证件"]:
        wb.create_sheet(name)
    wb.save(path)


def permissions():
    return {MIGRATION_COMMIT_PERMISSION}


def test_official_workbook_builds_approved_restricted_plan_without_files():
    plan = build_commit_plan(SOURCE_WORKBOOK, permissions=permissions())

    assert plan["source_sha256"] == EXPECTED_SOURCE_SHA256
    assert plan["target_tenant_id"] == 1
    assert plan["target_tenant_name"] == "芋道源码"
    assert plan["domestic_row_count"] == 119
    assert plan["company_count"] == 8
    assert plan["missing_project_code_count"] == 44
    assert plan["files_to_import"] == 0
    assert plan["fake_file_links"] == 0
    assert plan["sheet_import_counts"]["国内注册证"] == 119
    assert plan["sheet_import_counts"]["CE证书"] == 0
    assert plan["sheet_import_counts"]["FDA证书"] == 0
    assert plan["sheet_import_counts"]["公司证件"] == 0
    assert plan["restricted_row_count"] == 119
    assert all(row["approval_date"] == row["effective_date"] for row in plan["rows"])
    assert all("NO_ATTACHMENT_POLICY" in row["restricted_reasons"] for row in plan["rows"])
    assert all("business_file_id" not in row and "infra_file_id" not in row and "file_url" not in row
               for row in plan["rows"])
    assert plan["status"] == "BLOCKED"


def test_official_workbook_is_ready_when_every_formal_mapping_is_provided():
    report = build_preflight_report(SOURCE_WORKBOOK, permissions={MIGRATION_PREFLIGHT_PERMISSION})
    owners = sorted({row["normalized"]["owner_company_name"] for row in report["rows"]
                    if row["normalized"]["owner_company_name"]})
    products = sorted({row["normalized"]["product_name"] for row in report["rows"]
                      if row["normalized"]["product_name"]})
    project_codes = sorted({row["normalized"]["project_code"] for row in report["rows"]
                           if row["normalized"]["project_code"]})
    certificate_owners = {
        row["normalized"]["certificate_no"]: row["normalized"]["owner_company_name"]
        for row in report["rows"]
        if row["normalized"]["certificate_no"] and row["normalized"]["owner_company_name"]
    }

    plan = build_commit_plan(
        SOURCE_WORKBOOK,
        permissions=permissions(),
        formal_mapping={
            "owner_company_ids": {name: 1000 + index for index, name in enumerate(owners, start=1)},
            "product_master_ids": {name: 2000 + index for index, name in enumerate(products, start=1)},
            "project_code_ids": {code: 3000 + index for index, code in enumerate(project_codes, start=1)},
            "entrusted_enterprise_ids": {
                "山东瑛泰医疗器械有限公司": 4001,
                "珠海德瑞医疗器械有限公司": 4002,
            },
            "registrant_names": certificate_owners,
        },
    )

    assert plan["status"] == "READY"
    assert plan["ready_row_count"] == 119
    assert plan["blocked_row_count"] == 0
    assert plan["restricted_row_count"] == 119
    assert plan["missing_project_code_count"] == 44
    assert sum("MISSING_PROJECT_CODE" in row["restricted_reasons"] for row in plan["rows"]) == 44
    assert all(row["status"] == "READY_FOR_COMMIT" for row in plan["rows"])
    assert all(row["approval_date"] == row["effective_date"] for row in plan["rows"])
    assert all("business_file_id" not in row and "infra_file_id" not in row and "file_url" not in row
               for row in plan["rows"])


def test_explicit_production_and_missing_project_can_be_ready_with_formal_mapping(tmp_path):
    workbook = tmp_path / "source.xlsx"
    write_workbook(workbook, [{
        "公司名称": "公司A",
        "项目代码": "",
        "产品名称": "产品A",
        "注册证号": "CERT-A",
        "首次获证日期": "2020.1.2",
        "生效日期": "2021.2.3",
        "有效期至": "2026-02-02",
        "类别": "二类",
        "备注": "2025.1委托山东瑛泰生产+自行生产",
    }])
    plan = build_commit_plan(
        workbook,
        permissions=permissions(),
        expected_sha256=None,
        formal_mapping={
            "owner_company_ids": {"公司A": 101},
            "product_master_ids": {"产品A": 201},
            "project_code_ids": {},
            "entrusted_enterprise_ids": {"山东瑛泰医疗器械有限公司": 301},
            "registrant_names": {"CERT-A": "公司A"},
        },
    )
    row = plan["rows"][0]
    assert plan["status"] == "READY"
    assert row["status"] == "READY_FOR_COMMIT"
    assert row["production_relation"] == "BOTH"
    assert row["self_production"] is True
    assert row["entrusted_production"] is True
    assert row["entrusted_enterprises"] == ["山东瑛泰医疗器械有限公司"]
    assert row["entrusted_enterprise_ids"] == [301]
    assert row["registrant_name"] == "公司A"
    assert row["approval_date"] == "2021-02-03"
    assert row["restricted_reasons"] == ["MISSING_PROJECT_CODE", "NO_ATTACHMENT_POLICY"]
    assert plan["files_to_import"] == 0


def test_excel_company_is_authoritative_registrant_and_blank_remark_means_self_production(tmp_path):
    workbook = tmp_path / "source.xlsx"
    write_workbook(workbook, [{
        "公司名称": "公司A",
        "项目代码": "A1",
        "产品名称": "产品A",
        "注册证号": "CERT-A",
        "首次获证日期": "2020.1.2",
        "生效日期": "2021.2.3",
        "有效期至": "2026-02-02",
        "类别": "二类",
        "备注": "",
    }])

    plan = build_commit_plan(
        workbook,
        permissions=permissions(),
        expected_sha256=None,
        formal_mapping={
            "owner_company_ids": {"公司A": 101},
            "product_master_ids": {"产品A": 201},
            "project_code_ids": {"A1": 301},
        },
    )

    row = plan["rows"][0]
    assert plan["status"] == "READY"
    assert row["registrant_name"] == "公司A"
    assert row["production_relation"] == "SELF"
    assert row["self_production"] is True
    assert row["entrusted_production"] is False
    assert row["entrusted_enterprises"] == []


def test_unknown_or_blank_production_fact_is_blocked_not_guessed(tmp_path):
    workbook = tmp_path / "source.xlsx"
    write_workbook(workbook, [{
        "公司名称": "公司A",
        "项目代码": "A1",
        "产品名称": "产品A",
        "注册证号": "CERT-A",
        "首次获证日期": "2020.1.2",
        "生效日期": "2021.2.3",
        "有效期至": "2026-02-02",
        "类别": "二类",
        "备注": "委托外部企业生产",
    }])
    plan = build_commit_plan(
        workbook,
        permissions=permissions(),
        expected_sha256=None,
        formal_mapping={"owner_company_ids": {"公司A": 101}, "product_master_ids": {"产品A": 201},
                        "project_code_ids": {"A1": 301}},
    )
    assert plan["status"] == "BLOCKED"
    assert "UNRESOLVED_PRODUCTION_RELATION" in plan["rows"][0]["blockers"]


def test_missing_permission_and_attachment_import_fail_fast(tmp_path):
    workbook = tmp_path / "source.xlsx"
    write_workbook(workbook, [{
        "公司名称": "公司A", "项目代码": "A1", "产品名称": "产品A", "注册证号": "CERT-A",
        "首次获证日期": "2020.1.2", "生效日期": "2021.2.3", "有效期至": "2026-02-02",
        "类别": "二类", "备注": "自行生产",
    }])
    with pytest.raises(MigrationCommitPermissionError):
        build_commit_plan(workbook, permissions=set(), expected_sha256=None)
    with pytest.raises(MigrationCommitPolicyError):
        build_commit_plan(workbook, permissions=permissions(), expected_sha256=None, import_attachments=True)


def test_cli_writes_utf8_plan_json(tmp_path):
    workbook = tmp_path / "source.xlsx"
    write_workbook(workbook, [{
        "公司名称": "公司A", "项目代码": "A1", "产品名称": "产品A", "注册证号": "CERT-A",
        "首次获证日期": "2020.1.2", "生效日期": "2021.2.3", "有效期至": "2026-02-02",
        "类别": "二类", "备注": "自行生产",
    }])
    output = tmp_path / "nested" / "commit-plan.json"
    import subprocess

    cp = subprocess.run([
        "python", "-X", "utf8", str(ROOT / "script" / "registration_certificate_migration_commit.py"),
        "--workbook", str(workbook), "--expected-sha256", "", "--output-json", str(output),
    ], cwd=ROOT, text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=False)
    assert cp.returncode == 0, cp.stderr
    assert output.exists()
    assert '"target_tenant_name": "芋道源码"' in output.read_text(encoding="utf-8")


def test_cli_accepts_explicit_formal_mapping_file_and_outputs_ready_plan(tmp_path):
    workbook = tmp_path / "source.xlsx"
    write_workbook(workbook, [{
        "公司名称": "公司A", "项目代码": "A1", "产品名称": "产品A", "注册证号": "CERT-A",
        "首次获证日期": "2020.1.2", "生效日期": "2021.2.3", "有效期至": "2026-02-02",
        "类别": "二类", "备注": "自行生产",
    }])
    mapping = tmp_path / "formal-mapping.json"
    mapping.write_text(
        '{"owner_company_ids":{"公司A":101},"product_master_ids":{"产品A":201},'
        '"project_code_ids":{"A1":301},"registrant_names":{"CERT-A":"公司A"}}',
        encoding="utf-8",
    )
    output = tmp_path / "ready-plan.json"
    import subprocess

    cp = subprocess.run([
        "python", "-X", "utf8", str(ROOT / "script" / "registration_certificate_migration_commit.py"),
        "--workbook", str(workbook), "--expected-sha256", "",
        "--formal-mapping-json", str(mapping), "--output-json", str(output),
    ], cwd=ROOT, text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=False)

    assert cp.returncode == 0, cp.stderr
    plan = output.read_text(encoding="utf-8")
    assert '"status": "READY"' in plan
    assert '"ready_row_count": 1' in plan
    assert '"blocked_row_count": 0' in plan
