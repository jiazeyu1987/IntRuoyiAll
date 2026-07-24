import json
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SCANNER = ROOT / "script" / "unified_approval" / "governance_scan.py"
FULL_MATRIX = ROOT / "docs" / "engineering" / "unified-approval-platform-full-closeout-matrix.md"
MIGRATION_INVENTORY = ROOT / "docs" / "engineering" / "unified-approval-platform-migration-inventory.md"
RETIREMENT_INVENTORY = ROOT / "docs" / "engineering" / "unified-approval-platform-retirement-inventory.md"
GOVERNANCE_REPORT = ROOT / "docs" / "engineering" / "unified-approval-platform-governance-report.md"


def run_scanner(*extra_args: str) -> subprocess.CompletedProcess[str]:
    frontend_root = ROOT.parent / "yudao-ui-admin-vue3"
    return subprocess.run(
        [
            sys.executable,
            str(SCANNER),
            "--backend-root",
            str(ROOT),
            "--frontend-root",
            str(frontend_root),
            *extra_args,
        ],
        cwd=ROOT,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
    )


def test_phase_final_scanner_has_no_remaining_partial_or_blocked_items():
    result = run_scanner("--format", "json")

    assert result.returncode == 0, result.stderr or result.stdout
    payload = json.loads(result.stdout)

    assert payload["summary"]["blocking_violations"] == 0
    assert payload["summary"]["risk_items"] == 0
    statuses = {item["status"] for item in payload["closeout_inventory"]}
    assert "PARTIAL" not in statuses
    assert "BLOCKED" not in statuses

    inventory = {(item["category"], item["key"]): item for item in payload["closeout_inventory"]}
    assert inventory[("MODULE", "EDHR")]["status"] == "COMPLETED"
    assert "EDHR_DOMAIN_ACTIONS_CLASSIFIED" in inventory[("MODULE", "EDHR")]["evidence"]
    assert inventory[("MODULE", "SRM")]["status"] == "COMPLETED"
    assert "SRM_DOMAIN_ACTIONS_CLASSIFIED" in inventory[("MODULE", "SRM")]["evidence"]
    assert inventory[("MODULE", "MES_FEEDBACK")]["status"] == "COMPLETED"
    assert "DECLARED_CAPABILITY_BOUNDARY" in inventory[("MODULE", "MES_FEEDBACK")]["evidence"]
    assert inventory[("MODULE", "CRM_BACKLOG")]["status"] == "RETIRED"
    assert inventory[("MODULE", "ERP_APPROVAL_ACTIONS")]["status"] == "CLASSIFIED_DOMAIN_ACTION"


def test_phase_final_docs_do_not_claim_unresolved_partial_or_blocked_items():
    matrix = FULL_MATRIX.read_text(encoding="utf-8")
    migration_inventory = MIGRATION_INVENTORY.read_text(encoding="utf-8")
    retirement_inventory = RETIREMENT_INVENTORY.read_text(encoding="utf-8")
    governance_report = GOVERNANCE_REPORT.read_text(encoding="utf-8")

    for forbidden in ["半完成", "阻塞", "仍需分类", "产品未分类"]:
        assert forbidden not in matrix
    for required in [
        "EDHR_DOMAIN_ACTIONS_CLASSIFIED",
        "SRM_DOMAIN_ACTIONS_CLASSIFIED",
        "CRM_BACKLOG_RETIRED",
        "ERP_DOMAIN_ACTIONS_CLASSIFIED",
        "DECLARED_CAPABILITY_BOUNDARY",
    ]:
        assert required in matrix
    assert "Candidate" not in migration_inventory
    assert "Blocked" not in migration_inventory
    assert "Phase Final 分类结果" in retirement_inventory
    assert "待迁移候选与阻塞" not in retirement_inventory
    assert "ERP/CRM 审核动作" not in governance_report
    assert "待整改风险项" not in governance_report
