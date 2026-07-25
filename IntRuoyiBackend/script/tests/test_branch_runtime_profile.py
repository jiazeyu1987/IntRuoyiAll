import json
import os
import subprocess
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[3]
PROFILE_SCRIPT = REPO_ROOT / "scripts" / "runtime" / "branch-runtime-profile.ps1"
SHOW_SCRIPT = REPO_ROOT / "scripts" / "runtime" / "show-branch-runtime.ps1"
START_FRONTEND_SCRIPT = REPO_ROOT / "scripts" / "runtime" / "start-branch-frontend.ps1"


def _write_registry(tmp_path: Path, entries: list[dict]) -> Path:
    registry_path = tmp_path / "worktree-ports.json"
    registry_path.write_text(
        json.dumps(entries, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    return registry_path


def _run_powershell(command: str, registry_path: Path) -> subprocess.CompletedProcess[str]:
    env = os.environ.copy()
    env["INTRUOYI_WORKTREE_PORT_REGISTRY"] = str(registry_path)
    fail_fast_command = f"$ErrorActionPreference = 'Stop'; {command}"
    return subprocess.run(
        ["powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", fail_fast_command],
        cwd=REPO_ROOT,
        env=env,
        text=True,
        capture_output=True,
        timeout=30,
    )


def test_show_branch_runtime_uses_registered_worktree_slot(tmp_path: Path) -> None:
    registry_path = _write_registry(
        tmp_path,
        [
            {
                "version": 1,
                "worktrees": [
                    {
                        "name": "legacy-shape",
                        "path": "D:\\IntRuoyiWorktree\\legacy-shape",
                        "branch": "codex/legacy-shape",
                        "slot": 1,
                        "frontendPort": 8082,
                        "backendPort": 48082,
                        "active": True,
                    }
                ],
            },
            {
                "name": "system-backup-plan",
                "path": str(REPO_ROOT),
                "branch": "codex/system-backup-plan",
                "profile": "int_main",
                "slot": 2,
                "frontendPort": 8083,
                "backendPort": 48083,
                "active": True,
            },
        ],
    )

    result = _run_powershell(
        f"& '{SHOW_SCRIPT}'",
        registry_path,
    )

    assert result.returncode == 0, result.stderr
    assert "Profile          : int_main" in result.stdout
    assert "Slot             : 2" in result.stdout
    assert "FrontendPort     : 8083" in result.stdout
    assert "BackendPort      : 48083" in result.stdout


def test_registered_worktree_port_mismatch_fails_fast(tmp_path: Path) -> None:
    registry_path = _write_registry(
        tmp_path,
        [
            {
                "name": "bad-ports",
                "path": "D:\\IntRuoyiWorktree\\bad-ports",
                "branch": "codex/bad-ports",
                "profile": "int_main",
                "slot": 2,
                "frontendPort": 9999,
                "backendPort": 48083,
                "active": True,
            }
        ],
    )
    command = (
        f". '{PROFILE_SCRIPT}'; "
        "$context = Resolve-BranchRuntimeContext "
        "-RepoRoot 'D:\\IntRuoyiWorktree\\bad-ports' "
        "-Branch 'codex/bad-ports'; "
        "$context | ConvertTo-Json"
    )

    result = _run_powershell(command, registry_path)

    assert result.returncode != 0
    assert "does not match profile" in result.stderr


def test_duplicate_registered_worktree_entries_fail_fast(tmp_path: Path) -> None:
    entry = {
        "name": "duplicate-entry",
        "path": "D:\\IntRuoyiWorktree\\duplicate-entry",
        "branch": "codex/duplicate-entry",
        "profile": "int_main",
        "slot": 3,
        "frontendPort": 8084,
        "backendPort": 48084,
        "active": True,
    }
    registry_path = _write_registry(tmp_path, [entry, dict(entry)])
    command = (
        f". '{PROFILE_SCRIPT}'; "
        "$context = Resolve-BranchRuntimeContext "
        "-RepoRoot 'D:\\IntRuoyiWorktree\\duplicate-entry' "
        "-Branch 'codex/duplicate-entry'; "
        "$context | ConvertTo-Json"
    )

    result = _run_powershell(command, registry_path)

    assert result.returncode != 0
    assert "Duplicate active worktree port registry entries" in result.stderr


def test_branch_frontend_start_injects_required_local_runtime_env() -> None:
    script = START_FRONTEND_SCRIPT.read_text(encoding="utf-8")

    assert "$env:VITE_API_URL = '/admin-api'" in script
    assert "$env:VITE_APP_CAPTCHA_ENABLE = 'false'" in script
    assert "$env:VITE_BASE_URL = \"http://127.0.0.1:$($ports.BackendPort)\"" in script
