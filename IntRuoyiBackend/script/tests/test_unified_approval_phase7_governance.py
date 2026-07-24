import json
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
PHASE7_REPORT = ROOT / "docs" / "engineering" / "unified-approval-platform-governance-report.md"
LONG_TERM_MECHANISM = ROOT / "docs" / "engineering" / "unified-approval-platform-long-term-governance.md"
SCANNER = ROOT / "script" / "unified_approval" / "governance_scan.py"


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


def test_phase7_governance_scanner_reports_current_repository_state():
    result = run_scanner("--format", "json")

    assert result.returncode == 0, result.stderr or result.stdout
    payload = json.loads(result.stdout)

    assert payload["summary"]["compliant_modules"] == ["BPM", "DCC", "EDHR", "SHOWROOM", "SRM", "MES_FEEDBACK"]
    assert payload["summary"]["blocking_violations"] == 0
    assert payload["summary"]["risk_items"] == 0
    assert not any(item["module"] == "MES_FEEDBACK" for item in payload["risk_items"])
    assert not any(item["module"] == "CRM_BACKLOG" for item in payload["risk_items"])
    assert payload["operations"]["checks"]
    assert all(check["status"] in {"PASS", "RISK"} for check in payload["operations"]["checks"])


def test_phase7_governance_scanner_fails_on_private_approval_center_fixture():
    result = run_scanner("--fixture", "private-approval-center", "--format", "json")

    assert result.returncode == 2, result.stdout
    payload = json.loads(result.stdout)
    assert payload["summary"]["blocking_violations"] >= 1
    assert any(
        item["rule"] == "NO_PRIVATE_APPROVAL_CENTER" and item["severity"] == "BLOCKER"
        for item in payload["violations"]
    )


def test_phase7_governance_scanner_fails_on_legacy_approval_route_fixture():
    result = run_scanner("--fixture", "legacy-route-entry", "--format", "json")

    assert result.returncode == 2, result.stdout
    payload = json.loads(result.stdout)
    assert payload["summary"]["blocking_violations"] >= 1
    assert any(
        item["rule"] == "NO_LEGACY_APPROVAL_LIST_ENTRY" and item["severity"] == "BLOCKER"
        for item in payload["violations"]
    )


def test_phase7_governance_report_and_long_term_mechanism_are_codified():
    assert PHASE7_REPORT.exists(), "Phase7 must publish the repository-wide governance report"
    assert LONG_TERM_MECHANISM.exists(), "Phase7 must publish the long-term governance mechanism"

    report = PHASE7_REPORT.read_text(encoding="utf-8")
    mechanism = LONG_TERM_MECHANISM.read_text(encoding="utf-8")

    for text in ["已合规模块", "已闭环模块", "违规点", "风险级别", "MES 报工审批", "CRM 待办"]:
        assert text in report
    for text in ["新模块接入流程", "评审流程", "季度审计流程", "CI / review / design gate"]:
        assert text in mechanism
