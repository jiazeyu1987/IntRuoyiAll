import json
import subprocess
import sys
import tempfile
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SCRIPT_PATH = REPO_ROOT / "script" / "p0" / "verify_p0_tdd_evidence_gate.py"


def _run_gate(task_dir: Path) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        [sys.executable, "-X", "utf8", str(SCRIPT_PATH), "--task-dir", str(task_dir)],
        cwd=REPO_ROOT,
        text=True,
        capture_output=True,
        encoding="utf-8",
        errors="replace",
    )


def _write_task_log(task_dir: Path, body: str) -> None:
    task_dir.mkdir(parents=True, exist_ok=True)
    (task_dir / "execution-log.md").write_text(body, encoding="utf-8")


def test_p0_tdd_evidence_gate_blocks_when_m2_original_red_is_missing():
    assert SCRIPT_PATH.exists(), "P0 TDD evidence gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        task_dir = Path(tmp)
        _write_task_log(
            task_dir,
            "\n".join(
                [
                    "- EVIDENCE-GAP: M2 复核签名原始 RED -> BLOCKED, 未找到精确 RED。",
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL，空 `reviewSignatureSnapshotJson` 未入口阻断。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )

        result = _run_gate(task_dir)
        payload = json.loads(result.stdout)

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert any(blocker["code"] == "P0_TDD_EVIDENCE_GAP" for blocker in payload["blockers"])
        assert payload["m2OriginalRed"]["found"] is False
        assert payload["m2SnapshotRed"]["found"] is True


def test_p0_tdd_evidence_gate_passes_only_with_original_unsigned_review_red():
    assert SCRIPT_PATH.exists(), "P0 TDD evidence gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        task_dir = Path(tmp)
        _write_task_log(
            task_dir,
            "\n".join(
                [
                    "- BDD: 复核必须要求电子签名 -> Given 班组长没有签名 When 复核或确认分配 Then 必须拒绝。",
                    "- RED: workdir=`IntRuoyiBackend`; `mvn -pl yudao-module-mes -am \"-Dtest=MesP0TeamLeaderReviewSignatureServiceTest\" \"-Dsurefire.failIfNoSpecifiedTests=false\" test` -> FAIL，`Tests run: 8, Failures: 2`；无签名仍可复核或确认分配，证明班组长复核尚未要求电子签名。",
                    "- GREEN: workdir=`IntRuoyiBackend`; `mvn -pl yudao-module-mes -am \"-Dtest=MesP0TeamLeaderReviewSignatureServiceTest\" \"-Dsurefire.failIfNoSpecifiedTests=false\" test` -> PASS。",
                ]
            ),
        )

        result = _run_gate(task_dir)
        payload = json.loads(result.stdout)

        assert result.returncode == 0
        assert payload["status"] == "PASS"
        assert payload["blockers"] == []
        assert payload["m2OriginalRed"]["found"] is True


def test_p0_tdd_evidence_gate_allows_resolved_historical_gap_after_replay_red():
    assert SCRIPT_PATH.exists(), "P0 TDD evidence gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        task_dir = Path(tmp)
        _write_task_log(
            task_dir,
            "\n".join(
                [
                    "- EVIDENCE-GAP: M2 复核签名原始 RED -> BLOCKED，历史执行日志未记录精确失败输出。",
                    "- RED: workdir=`D:\\IntRuoyiWorktree\\p0_m2_red_replay_20260804\\IntRuoyiBackend`; `mvn -pl yudao-module-mes -am \"-Dtest=MesP0TeamLeaderReviewSignatureServiceTest\" \"-Dsurefire.failIfNoSpecifiedTests=false\" test` -> FAIL，`Tests run: 2, Failures: 2`；无签名仍可复核或确认分配，证明班组长复核尚未要求电子签名。",
                    "- EVIDENCE-RESOLVED: M2 复核签名原始 RED -> replay worktree 已补齐可追溯 Maven/Surefire 失败输出。",
                ]
            ),
        )

        result = _run_gate(task_dir)
        payload = json.loads(result.stdout)

        assert result.returncode == 0
        assert payload["status"] == "PASS"
        assert payload["m2OriginalRed"]["found"] is True
        assert payload["m2EvidenceGap"]["found"] is True
        assert payload["m2EvidenceResolved"]["found"] is True


def test_p0_tdd_evidence_gate_does_not_treat_bdd_wording_as_gap_marker():
    assert SCRIPT_PATH.exists(), "P0 TDD evidence gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        task_dir = Path(tmp)
        _write_task_log(
            task_dir,
            "\n".join(
                [
                    "- BDD: M2 原始 RED 证据不得由后补边界 RED 替代 -> Given 任务已补齐空/非 JSON 签名快照 fail-fast RED/GREEN When 收尾前核验 M2 复核电子签名 TDD 证据 Then 必须能定位“无签名仍可复核或确认分配”的原始 RED 命令与失败输出；若只找到签名快照 RED 或 `EVIDENCE-GAP` 标记，则 M6 必须 BLOCKED。",
                    "- RED: workdir=`IntRuoyiBackend`; `mvn -pl yudao-module-mes -am \"-Dtest=MesP0TeamLeaderReviewSignatureServiceTest\" \"-Dsurefire.failIfNoSpecifiedTests=false\" test` -> FAIL，`Tests run: 8, Failures: 2`；无签名仍可复核或确认分配，证明班组长复核尚未要求电子签名。",
                    "- GREEN: workdir=`IntRuoyiBackend`; `mvn -pl yudao-module-mes -am \"-Dtest=MesP0TeamLeaderReviewSignatureServiceTest\" \"-Dsurefire.failIfNoSpecifiedTests=false\" test` -> PASS。",
                ]
            ),
        )

        result = _run_gate(task_dir)
        payload = json.loads(result.stdout)

        assert result.returncode == 0
        assert payload["status"] == "PASS"
        assert payload["m2EvidenceGap"]["found"] is False


def test_p0_tdd_evidence_gate_default_task_dir_targets_workspace_task():
    assert SCRIPT_PATH.exists(), "P0 TDD evidence gate script is required"

    result = subprocess.run(
        [sys.executable, "-X", "utf8", str(SCRIPT_PATH)],
        cwd=REPO_ROOT,
        text=True,
        capture_output=True,
        encoding="utf-8",
        errors="replace",
    )
    payload = json.loads(result.stdout)

    assert "IntRuoyiBackend\\doc\\tasks" not in payload["taskDir"]
    assert payload["taskDir"].endswith("\\doc\\tasks\\20260803-p0-production-execution-loop-implementation")
    assert payload["m2SnapshotRed"]["found"] is True
    assert payload["status"] == "PASS"
    assert payload["blockers"] == []
    assert payload["m2OriginalRed"]["found"] is True
    assert payload["m2EvidenceResolved"]["found"] is True


if __name__ == "__main__":
    test_p0_tdd_evidence_gate_blocks_when_m2_original_red_is_missing()
    test_p0_tdd_evidence_gate_passes_only_with_original_unsigned_review_red()
    test_p0_tdd_evidence_gate_allows_resolved_historical_gap_after_replay_red()
    test_p0_tdd_evidence_gate_does_not_treat_bdd_wording_as_gap_marker()
    test_p0_tdd_evidence_gate_default_task_dir_targets_workspace_task()
    print("PASS: P0 TDD evidence gate contract")
