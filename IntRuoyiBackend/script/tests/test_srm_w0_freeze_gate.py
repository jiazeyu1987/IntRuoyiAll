from __future__ import annotations

import subprocess
import sys
import json
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SCRIPT = REPO_ROOT / "script" / "srm" / "check_srm_w0_freeze_gate.py"
FREEZE_PACK = REPO_ROOT / "docs" / "srm" / "srm9-w0-freeze-pack.md"
LANDING_PLAN = REPO_ROOT / "docs" / "srm" / "srm9-landing-plan.md"
BLOCKER_MANIFEST = REPO_ROOT / "docs" / "dependencies" / "srm9-blocker-manifest.json"
MANIFEST_SCRIPT = REPO_ROOT / "script" / "srm" / "check_srm_blocker_manifest.py"


def run_gate(*args: str) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        [sys.executable, str(SCRIPT), *args],
        cwd=REPO_ROOT,
        text=True,
        encoding="utf-8",
        capture_output=True,
        check=False,
    )


def test_w1_is_blocked_until_master_data_freeze() -> None:
    result = run_gate("--freeze-pack", str(FREEZE_PACK), "--wave", "W1")

    assert result.returncode == 1
    assert "W1" in result.stdout
    assert "W0-01" in result.stdout
    assert "BLOCKED" in result.stdout


def test_w0_01_records_admin_nas_discovery_without_unblocking_k3() -> None:
    text = FREEZE_PACK.read_text(encoding="utf-8")

    assert "admin NAS 只读发现证据" in text
    assert "NAS 配置完整且根目录可读" in text
    assert "`BD_Supplier` 保存探针能登录 K3" in text
    assert "创建/使用组织字段契约未解" in text
    assert "| K3 FormId | 明确供应商主数据或其他目标业务对象 FormId | `BLOCKED` |" in text


def test_w0_01_records_existing_kingdee_config_without_unblocking_writeback() -> None:
    freeze_text = FREEZE_PACK.read_text(encoding="utf-8")
    landing_text = LANDING_PLAN.read_text(encoding="utf-8")
    manifest = json.loads(BLOCKER_MANIFEST.read_text(encoding="utf-8"))

    assert "admin ERP 金蝶配置与同步历史只读证据" in freeze_text
    assert "`/erp/kingdee-config` 可由 `芋道源码/admin` 打开" in freeze_text
    assert "`98` 条不同金蝶供应商号映射" in freeze_text
    assert "`297` 条 `PUR_PurchaseOrder` 成功同步记录" in freeze_text
    assert "代码和表结构未发现 SRM 供应商主数据写回 K3 的保存实现" in freeze_text
    assert "不得重复开发供应商门户、准入审批、公开/邀请询价主链、采购订单主链、现有 ERP 金蝶基础配置/读同步" in landing_text
    assert "Existing ERP Kingdee config readonly evidence" in manifest["waves"]["W1"]["requiredEvidence"]
    assert manifest["blockers"]["SRM9-DEP-002"]["missing"] == "K3 supplier master write-back contract"


def test_w0_01_records_nearest_real_candidate_without_unblocking_writeback() -> None:
    freeze_text = FREEZE_PACK.read_text(encoding="utf-8")
    landing_text = LANDING_PLAN.read_text(encoding="utf-8")
    manifest = json.loads(BLOCKER_MANIFEST.read_text(encoding="utf-8"))

    assert "测试租户最近似候选样本" in freeze_text
    assert "供应商 ID `103`" in freeze_text
    assert "`山东瑛泰医疗器械有限公司`" in freeze_text
    assert "金蝶来源编号 `INT-010`" in freeze_text
    assert "返回 `eligible=true`" in freeze_text
    assert "最近似候选样本真实页面验证" in landing_text
    assert "K3-mapped portal-approved SRM page E2E sample" in manifest["waves"]["W1"]["requiredEvidence"]
    assert manifest["waves"]["W1"]["status"] == "BLOCKED"


def test_w0_01_records_portal_ready_readonly_e2e_sample() -> None:
    freeze_text = FREEZE_PACK.read_text(encoding="utf-8")
    landing_text = LANDING_PLAN.read_text(encoding="utf-8")

    assert "SRM 侧可走通候选样本" in freeze_text
    assert "供应商 ID `108`" in freeze_text
    assert "`SRM Portal E2E 20260620183546`" in freeze_text
    assert "无金蝶映射" in freeze_text
    assert "103。108 仍保留为无金蝶映射的页面链路对照样本" in freeze_text
    assert "108 / SRM Portal E2E 20260620183546" in landing_text


def test_w0_check_passes_when_freeze_pack_is_readable() -> None:
    result = run_gate("--freeze-pack", str(FREEZE_PACK), "--wave", "W0")

    assert result.returncode == 0
    assert "W0 freeze pack readable" in result.stdout


def test_w0_06_regression_baseline_is_frozen() -> None:
    result = run_gate("--freeze-pack", str(FREEZE_PACK), "--wave", "W0-06")

    assert result.returncode == 0
    assert "W0-06" in result.stdout
    assert "FROZEN" in result.stdout


def test_w3_dashboard_wave_is_blocked() -> None:
    result = run_gate("--freeze-pack", str(FREEZE_PACK), "--wave", "W3")

    assert result.returncode == 1
    assert "W3" in result.stdout
    assert "W0-03" in result.stdout
    assert "BLOCKED" in result.stdout


def test_missing_freeze_pack_fails_fast(tmp_path: Path) -> None:
    missing = tmp_path / "missing-freeze-pack.md"

    result = run_gate("--freeze-pack", str(missing), "--wave", "W1")

    assert result.returncode == 2
    assert "Freeze pack not found" in result.stderr


def test_landing_plan_ranks_all_waves_and_excel_items() -> None:
    text = LANDING_PLAN.read_text(encoding="utf-8")

    expected_wave_levels = {
        "W0": "`L4`",
        "W1": "`L1`",
        "W2": "`L2`",
        "W3": "`L1`",
        "W4": "`L1`",
        "W5": "`L1`",
        "W6": "`L0`",
    }
    for wave, level in expected_wave_levels.items():
        assert f"| {wave} " in text
        assert level in text

    for item in range(1, 13):
        assert f"`{item}`" in text

    assert "不得重复开发 `5/6`" in text
    assert "不得把受控模拟改名为真实联通" in text


def run_manifest_check(*extra_args: str) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        [
            sys.executable,
            str(MANIFEST_SCRIPT),
            "--manifest",
            str(BLOCKER_MANIFEST),
            "--freeze-pack",
            str(FREEZE_PACK),
            "--landing-plan",
            str(LANDING_PLAN),
            *extra_args,
        ],
        cwd=REPO_ROOT,
        text=True,
        encoding="utf-8",
        capture_output=True,
        check=False,
    )


def test_blocker_manifest_matches_freeze_pack_and_landing_plan() -> None:
    result = run_manifest_check()

    assert result.returncode == 0
    assert "SRM9 blocker manifest valid" in result.stdout


def test_blocker_manifest_detects_status_drift(tmp_path: Path) -> None:
    drifted_manifest = tmp_path / "srm9-blocker-manifest.json"
    data = json.loads(BLOCKER_MANIFEST.read_text(encoding="utf-8"))
    data["waves"]["W1"]["freezePackageStatus"] = "FROZEN"
    drifted_manifest.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")

    result = subprocess.run(
        [
            sys.executable,
            str(MANIFEST_SCRIPT),
            "--manifest",
            str(drifted_manifest),
            "--freeze-pack",
            str(FREEZE_PACK),
            "--landing-plan",
            str(LANDING_PLAN),
        ],
        cwd=REPO_ROOT,
        text=True,
        encoding="utf-8",
        capture_output=True,
        check=False,
    )

    assert result.returncode == 1
    assert "W1 status mismatch" in result.stderr
