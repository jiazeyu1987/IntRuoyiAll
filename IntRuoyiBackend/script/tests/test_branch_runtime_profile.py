import json
import os
import subprocess
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[3]
PROFILE_SCRIPT = REPO_ROOT / "scripts" / "runtime" / "branch-runtime-profile.ps1"
SHOW_SCRIPT = REPO_ROOT / "scripts" / "runtime" / "show-branch-runtime.ps1"
START_FRONTEND_SCRIPT = REPO_ROOT / "scripts" / "runtime" / "start-branch-frontend.ps1"
RESERVE_SLOT_SCRIPT = REPO_ROOT / "scripts" / "runtime" / "reserve-worktree-slot.ps1"


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


def _slot_allocator_command(
    registry_path: Path,
    *,
    name: str,
    profile: str = "int_main",
) -> list[str]:
    return [
        "powershell",
        "-NoProfile",
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        str(RESERVE_SLOT_SCRIPT),
        "-Name",
        name,
        "-Path",
        f"D:\\IntRuoyiWorktree\\{name}",
        "-Branch",
        f"codex/{name}",
        "-Profile",
        profile,
        "-RegistryPath",
        str(registry_path),
        "-AsJson",
    ]


def test_registered_worktree_context_uses_registered_slot(tmp_path: Path) -> None:
    worktree_root = "D:\\IntRuoyiWorktree\\system-backup-plan"
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
                        "profile": "int_main",
                        "slot": 1,
                        "frontendPort": 8082,
                        "backendPort": 48082,
                        "active": True,
                    }
                ],
            },
            {
                "name": "system-backup-plan",
                "path": worktree_root,
                "branch": "codex/system-backup-plan",
                "profile": "int_main",
                "slot": 2,
                "frontendPort": 8083,
                "backendPort": 48083,
                "active": True,
            },
        ],
    )
    command = (
        f". '{PROFILE_SCRIPT}'; "
        "$context = Resolve-BranchRuntimeContext "
        f"-RepoRoot '{worktree_root}' "
        "-Branch 'codex/system-backup-plan'; "
        "$context | ConvertTo-Json -Depth 4"
    )

    result = _run_powershell(command, registry_path)

    assert result.returncode == 0, result.stderr
    assert '"Name":  "int_main"' in result.stdout
    assert '"Slot":  2' in result.stdout
    assert '"FrontendPort":  8083' in result.stdout
    assert '"BackendPort":  48083' in result.stdout


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


def test_registered_worktree_extended_slot_uses_dedicated_port_band(tmp_path: Path) -> None:
    registry_path = _write_registry(
        tmp_path,
        [
            {
                "name": "extended-slot",
                "path": "D:\\IntRuoyiWorktree\\extended-slot",
                "branch": "codex/extended-slot",
                "profile": "int_main",
                "slot": 20,
                "frontendPort": 8154,
                "backendPort": 48154,
                "active": True,
            }
        ],
    )
    command = (
        f". '{PROFILE_SCRIPT}'; "
        "$context = Resolve-BranchRuntimeContext "
        "-RepoRoot 'D:\\IntRuoyiWorktree\\extended-slot' "
        "-Branch 'codex/extended-slot'; "
        "$context | ConvertTo-Json"
    )

    result = _run_powershell(command, registry_path)

    assert result.returncode == 0, result.stderr
    assert '"Slot":  20' in result.stdout
    assert '"FrontendPort":  8154' in result.stdout
    assert '"BackendPort":  48154' in result.stdout


def test_registered_worktree_second_extended_slot_uses_dedicated_port_band(tmp_path: Path) -> None:
    registry_path = _write_registry(
        tmp_path,
        [
            {
                "name": "second-extended-slot",
                "path": "D:\\IntRuoyiWorktree\\second-extended-slot",
                "branch": "codex/second-extended-slot",
                "profile": "int_main",
                "slot": 31,
                "frontendPort": 8206,
                "backendPort": 48206,
                "active": True,
            }
        ],
    )
    command = (
        f". '{PROFILE_SCRIPT}'; "
        "$context = Resolve-BranchRuntimeContext "
        "-RepoRoot 'D:\\IntRuoyiWorktree\\second-extended-slot' "
        "-Branch 'codex/second-extended-slot'; "
        "$context | ConvertTo-Json -Depth 4"
    )

    result = _run_powershell(command, registry_path)

    assert result.returncode == 0, result.stderr
    assert '"Slot":  31' in result.stdout
    assert '"FrontendPort":  8206' in result.stdout
    assert '"BackendPort":  48206' in result.stdout


def test_registered_worktree_slot_above_40_fails_fast(tmp_path: Path) -> None:
    registry_path = _write_registry(
        tmp_path,
        [
            {
                "name": "slot-above-limit",
                "path": "D:\\IntRuoyiWorktree\\slot-above-limit",
                "branch": "codex/slot-above-limit",
                "profile": "int_main",
                "slot": 41,
                "frontendPort": 9999,
                "backendPort": 49999,
                "active": True,
            }
        ],
    )
    command = (
        f". '{PROFILE_SCRIPT}'; "
        "$context = Resolve-BranchRuntimeContext "
        "-RepoRoot 'D:\\IntRuoyiWorktree\\slot-above-limit' "
        "-Branch 'codex/slot-above-limit'; "
        "$context | ConvertTo-Json"
    )

    result = _run_powershell(command, registry_path)

    assert result.returncode != 0
    assert "must be between 1 and 40" in result.stderr


def test_all_profile_worktree_ports_are_unique_through_slot_40(tmp_path: Path) -> None:
    registry_path = _write_registry(tmp_path, [])
    command = (
        f". '{PROFILE_SCRIPT}'; "
        "$rows = foreach ($profile in Get-BranchRuntimeProfiles) { "
        "foreach ($slot in 1..40) { "
        "$ports = Get-BranchRuntimePorts -Profile $profile -Slot $slot; "
        "[pscustomobject]@{ profile = $profile.Name; slot = $slot; "
        "frontendPort = $ports.FrontendPort; backendPort = $ports.BackendPort } "
        "} }; "
        "$rows | ConvertTo-Json -Depth 3"
    )

    result = _run_powershell(command, registry_path)

    assert result.returncode == 0, result.stderr
    rows = json.loads(result.stdout)
    assert len(rows) == 200
    assert len({row["frontendPort"] for row in rows}) == 200
    assert len({row["backendPort"] for row in rows}) == 200
    assert {row["frontendPort"] for row in rows}.isdisjoint(
        {8021, 8041, 8061, 8081, 8101}
    )
    assert {row["backendPort"] for row in rows}.isdisjoint(
        {48021, 48041, 48061, 48081, 48101}
    )

    by_profile_slot = {
        (row["profile"], row["slot"]): (row["frontendPort"], row["backendPort"])
        for row in rows
    }
    assert by_profile_slot[("int_main", 19)] == (8100, 48100)
    expected_extension_ranges = {
        "int_shedule": ((8121, 48121), (8131, 48131)),
        "int_batch": ((8132, 48132), (8142, 48142)),
        "int_qms": ((8143, 48143), (8153, 48153)),
        "int_main": ((8154, 48154), (8164, 48164)),
        "int_main_d": ((8165, 48165), (8175, 48175)),
    }
    expected_second_extension_ports = {
        "int_shedule": (8185, 48185),
        "int_batch": (8195, 48195),
        "int_qms": (8205, 48205),
        "int_main": (8215, 48215),
        "int_main_d": (8225, 48225),
    }
    for profile, (slot_20_ports, slot_30_ports) in expected_extension_ranges.items():
        assert by_profile_slot[(profile, 20)] == slot_20_ports
        assert by_profile_slot[(profile, 30)] == slot_30_ports
        assert by_profile_slot[(profile, 40)] == expected_second_extension_ports[profile]


def test_duplicate_active_profile_slots_fail_fast(tmp_path: Path) -> None:
    registry_path = _write_registry(
        tmp_path,
        [
            {
                "name": "slot-owner-a",
                "path": "D:\\IntRuoyiWorktree\\slot-owner-a",
                "branch": "codex/slot-owner-a",
                "profile": "int_main",
                "slot": 2,
                "frontendPort": 8083,
                "backendPort": 48083,
                "active": True,
            },
            {
                "name": "slot-owner-b",
                "path": "D:\\IntRuoyiWorktree\\slot-owner-b",
                "branch": "codex/slot-owner-b",
                "profile": "int_main",
                "slot": 2,
                "frontendPort": 8083,
                "backendPort": 48083,
                "active": True,
            },
        ],
    )
    command = (
        f". '{PROFILE_SCRIPT}'; "
        "$context = Resolve-BranchRuntimeContext "
        "-RepoRoot 'D:\\IntRuoyiWorktree\\slot-owner-a' "
        "-Branch 'codex/slot-owner-a'; "
        "$context | ConvertTo-Json"
    )

    result = _run_powershell(command, registry_path)

    assert result.returncode != 0
    assert "Duplicate active runtime slot 'int_main/2'" in result.stderr


def test_base_workspace_cannot_request_additional_slot(tmp_path: Path) -> None:
    registry_path = _write_registry(tmp_path, [])
    command = (
        f". '{PROFILE_SCRIPT}'; "
        "$context = Resolve-BranchRuntimeContext "
        "-RepoRoot 'E:\\IntRuoyi' "
        "-Branch 'int_main' "
        "-RequestedSlot 1; "
        "$context | ConvertTo-Json"
    )

    result = _run_powershell(command, registry_path)

    assert result.returncode != 0
    assert "Base workspace must use runtime slot 0" in result.stderr


def test_main_workspace_resolves_int_main_base_profile(tmp_path: Path) -> None:
    registry_path = _write_registry(tmp_path, [])
    command = (
        f". '{PROFILE_SCRIPT}'; "
        "$context = Resolve-BranchRuntimeContext "
        "-RepoRoot 'E:\\IntRuoyi' "
        "-Branch 'int_main'; "
        "$context | ConvertTo-Json -Depth 4"
    )

    result = _run_powershell(command, registry_path)

    assert result.returncode == 0, result.stderr
    assert '"Name":  "int_main"' in result.stdout
    assert '"FrontendPort":  8081' in result.stdout
    assert '"BackendPort":  48081' in result.stdout


def test_slot_allocator_reserves_lowest_available_profile_slot(tmp_path: Path) -> None:
    registry_path = _write_registry(
        tmp_path,
        [
            {
                "name": "existing-slot",
                "path": "D:\\IntRuoyiWorktree\\existing-slot",
                "branch": "codex/existing-slot",
                "profile": "int_main",
                "slot": 1,
                "frontendPort": 8082,
                "backendPort": 48082,
                "active": True,
            }
        ],
    )
    result = subprocess.run(
        _slot_allocator_command(registry_path, name="allocated-slot"),
        cwd=REPO_ROOT,
        text=True,
        capture_output=True,
        timeout=30,
    )

    assert result.returncode == 0, result.stderr
    allocation = json.loads(result.stdout)
    assert allocation["slot"] == 2
    assert allocation["frontendPort"] == 8083
    assert allocation["backendPort"] == 48083

    registry = json.loads(registry_path.read_text(encoding="utf-8"))
    active_entries = [entry for entry in registry["worktrees"] if entry["active"]]
    assert {(entry["profile"], entry["slot"]) for entry in active_entries} == {
        ("int_main", 1),
        ("int_main", 2),
    }


def test_concurrent_slot_allocators_receive_distinct_slots(tmp_path: Path) -> None:
    registry_path = _write_registry(tmp_path, [])
    first = subprocess.Popen(
        _slot_allocator_command(registry_path, name="concurrent-a"),
        cwd=REPO_ROOT,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    second = subprocess.Popen(
        _slot_allocator_command(registry_path, name="concurrent-b"),
        cwd=REPO_ROOT,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )

    first_stdout, first_stderr = first.communicate(timeout=30)
    second_stdout, second_stderr = second.communicate(timeout=30)

    assert first.returncode == 0, first_stderr
    assert second.returncode == 0, second_stderr
    allocations = [json.loads(first_stdout), json.loads(second_stdout)]
    assert sorted(allocation["slot"] for allocation in allocations) == [1, 2]

    registry = json.loads(registry_path.read_text(encoding="utf-8"))
    active_entries = [entry for entry in registry["worktrees"] if entry["active"]]
    assert sorted(entry["slot"] for entry in active_entries) == [1, 2]


def test_slot_allocator_uses_first_extended_slot_after_legacy_band(tmp_path: Path) -> None:
    entries = [
        {
            "name": f"occupied-{slot}",
            "path": f"D:\\IntRuoyiWorktree\\occupied-{slot}",
            "branch": f"codex/occupied-{slot}",
            "profile": "int_main",
            "slot": slot,
            "frontendPort": 8081 + slot,
            "backendPort": 48081 + slot,
            "active": True,
        }
        for slot in range(1, 20)
    ]
    registry_path = _write_registry(tmp_path, entries)

    result = subprocess.run(
        _slot_allocator_command(registry_path, name="extended-slot"),
        cwd=REPO_ROOT,
        text=True,
        capture_output=True,
        timeout=30,
    )

    assert result.returncode == 0, result.stderr
    allocation = json.loads(result.stdout)
    assert allocation["slot"] == 20
    assert allocation["frontendPort"] == 8154
    assert allocation["backendPort"] == 48154


def test_slot_allocator_uses_second_extended_slot_after_first_extension(tmp_path: Path) -> None:
    entries = [
        {
            "name": f"occupied-{slot}",
            "path": f"D:\\IntRuoyiWorktree\\occupied-{slot}",
            "branch": f"codex/occupied-{slot}",
            "profile": "int_main",
            "slot": slot,
            "frontendPort": (
                8081 + slot
                if slot <= 19
                else 8154 + slot - 20
            ),
            "backendPort": (
                48081 + slot
                if slot <= 19
                else 48154 + slot - 20
            ),
            "active": True,
        }
        for slot in range(1, 31)
    ]
    registry_path = _write_registry(tmp_path, entries)

    result = subprocess.run(
        _slot_allocator_command(registry_path, name="second-extension"),
        cwd=REPO_ROOT,
        text=True,
        capture_output=True,
        timeout=30,
    )

    assert result.returncode == 0, result.stderr
    allocation = json.loads(result.stdout)
    assert allocation["slot"] == 31
    assert allocation["frontendPort"] == 8206
    assert allocation["backendPort"] == 48206


def test_slot_allocator_fails_when_profile_band_is_exhausted(tmp_path: Path) -> None:
    entries = [
        {
            "name": f"occupied-{slot}",
            "path": f"D:\\IntRuoyiWorktree\\occupied-{slot}",
            "branch": f"codex/occupied-{slot}",
            "profile": "int_main",
            "slot": slot,
            "frontendPort": (
                8081 + slot
                if slot <= 19
                else 8154 + slot - 20
                if slot <= 30
                else 8206 + slot - 31
            ),
            "backendPort": (
                48081 + slot
                if slot <= 19
                else 48154 + slot - 20
                if slot <= 30
                else 48206 + slot - 31
            ),
            "active": True,
        }
        for slot in range(1, 41)
    ]
    registry_path = _write_registry(tmp_path, entries)

    result = subprocess.run(
        _slot_allocator_command(registry_path, name="no-slot-left"),
        cwd=REPO_ROOT,
        text=True,
        capture_output=True,
        timeout=30,
    )

    assert result.returncode != 0
    assert "No available runtime slot for profile 'int_main' in range 1..40" in result.stderr


def test_branch_frontend_start_injects_required_local_runtime_env() -> None:
    script = START_FRONTEND_SCRIPT.read_text(encoding="utf-8")

    assert "$env:VITE_API_URL = '/admin-api'" in script
    assert "$env:VITE_APP_CAPTCHA_ENABLE = 'false'" in script
    assert "$env:VITE_BASE_URL = \"http://127.0.0.1:$($ports.BackendPort)\"" in script
