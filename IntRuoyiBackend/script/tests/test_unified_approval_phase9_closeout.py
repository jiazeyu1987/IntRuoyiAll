import json
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SCANNER = ROOT / "script" / "unified_approval" / "governance_scan.py"
OPERATIONS_RUNBOOK = ROOT / "docs" / "engineering" / "unified-approval-platform-operations-runbook.md"
FULL_MATRIX = ROOT / "docs" / "engineering" / "unified-approval-platform-full-closeout-matrix.md"


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


def test_phase9_scanner_outputs_full_closeout_inventory():
    result = run_scanner("--format", "json")

    assert result.returncode == 0, result.stderr or result.stdout
    payload = json.loads(result.stdout)

    assert payload["schemaVersion"] == "unified-approval-governance/v2"
    inventory = payload["closeout_inventory"]
    assert {item["category"] for item in inventory} >= {"MODULE", "PAGE", "API", "MENU", "SCRIPT"}
    assert "PARTIAL" not in {item["status"] for item in inventory}
    assert "BLOCKED" not in {item["status"] for item in inventory}

    required = {
        ("MODULE", "BPM"),
        ("MODULE", "DCC"),
        ("MODULE", "EDHR"),
        ("MODULE", "SHOWROOM"),
        ("MODULE", "SRM"),
        ("MODULE", "MES_FEEDBACK"),
        ("PAGE", "/approval-center"),
        ("API", "/approval-center/tasks/page"),
        ("MENU", "bpm/task/todo/index"),
        ("SCRIPT", "script/unified_approval/governance_scan.py"),
    }
    actual = {(item["category"], item["key"]) for item in inventory}
    assert required <= actual

    mes_feedback = next(item for item in inventory if item["category"] == "MODULE" and item["key"] == "MES_FEEDBACK")
    assert mes_feedback["status"] == "COMPLETED"
    assert "DECLARED_CAPABILITY_BOUNDARY" in mes_feedback["evidence"]


def test_phase9_scanner_fails_on_private_dto_or_bridge_fixture():
    result = run_scanner("--fixture", "legacy-dto-bridge", "--format", "json")

    assert result.returncode == 2, result.stdout
    payload = json.loads(result.stdout)
    rules = {item["rule"] for item in payload["violations"]}
    assert "NO_PRIVATE_APPROVAL_DTO" in rules
    assert "NO_COMPATIBILITY_APPROVAL_BRIDGE" in rules


def test_phase9_stable_operations_docs_are_explicit():
    assert FULL_MATRIX.exists(), "Phase9 must publish a full closeout matrix artifact"
    assert OPERATIONS_RUNBOOK.exists(), "Operations runbook must exist"

    matrix = FULL_MATRIX.read_text(encoding="utf-8")
    runbook = OPERATIONS_RUNBOOK.read_text(encoding="utf-8")

    for text in ["全量接入矩阵", "已完成", "已分类", "已退役", "MES_FEEDBACK", "CRM_BACKLOG_RETIRED"]:
        assert text in matrix
    for text in ["上线后稳定期", "异常监控", "失败审计", "超时/SLA", "催办", "告警", "值守排查清单", "问题闭环"]:
        assert text in runbook
    assert "MES_FEEDBACK" in runbook and "不得声明 REMINDER" in runbook
