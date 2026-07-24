import json
import subprocess
from copy import deepcopy
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
VALIDATOR_SCRIPT = REPO_ROOT / "script" / "backup-ops" / "scripts" / "validate-dcc-backup-chain.ps1"


def _write_json(path: Path, value: dict[str, object]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def _read_json(path: Path) -> dict[str, object]:
    return json.loads(path.read_text(encoding="utf-8"))


def _hash(label: str) -> str:
    return "sha256:" + label.lower().ljust(64, "0")[:64]


def _valid_manifest() -> dict[str, object]:
    return {
        "schemaVersion": "dcc-backup-manifest-v1",
        "backupId": "dcc-chain-20260609",
        "targetEnvironment": "test",
        "backupMode": "incremental",
        "baselineBackupId": "backup-B1",
        "baselineRestorePointId": "B1",
        "previousBackupId": "backup-B4",
        "previousRestorePointId": "B4",
        "chainStatus": "COMPLETE",
        "restoreVerified": False,
        "restoreRehearsal": {"status": "not-run"},
        "fullBaseline": {"restorePointId": "B1", "checksum": _hash("b1")},
        "incrementalChain": [
            {"from": "B1", "to": "B2", "checksum": _hash("b2")},
            {"from": "B2", "to": "B3", "checksum": _hash("b3")},
            {"from": "B3", "to": "B4", "checksum": _hash("b4")},
            {"from": "B4", "to": "B5", "checksum": _hash("b5")},
        ],
        "restorePoints": [
            {"id": "B3", "databaseRestorePointId": "B3", "objectInventoryRestorePointId": "B3"},
            {"id": "B4", "databaseRestorePointId": "B4", "objectInventoryRestorePointId": "B4"},
            {"id": "B5", "databaseRestorePointId": "B5", "objectInventoryRestorePointId": "B5"},
        ],
        "objectInventories": [
            {
                "restorePointId": "B3",
                "objects": [
                    {
                        "fileKey": "B",
                        "state": "active",
                        "contentHash": _hash("b3"),
                        "storedHash": _hash("b3"),
                        "present": True,
                    }
                ],
            },
            {
                "restorePointId": "B4",
                "objects": [
                    {
                        "fileKey": "B",
                        "state": "active",
                        "contentHash": _hash("b4"),
                        "storedHash": _hash("b4"),
                        "present": True,
                    }
                ],
            },
            {
                "restorePointId": "B5",
                "objects": [
                    {
                        "fileKey": "B",
                        "state": "deleted",
                        "contentHash": _hash("b4"),
                        "storedHash": _hash("b4"),
                        "present": False,
                    }
                ],
            },
        ],
        "databaseRecords": [
            {"restorePointId": "B3", "fileKey": "B", "state": "active"},
            {"restorePointId": "B4", "fileKey": "B", "state": "active"},
            {"restorePointId": "B5", "fileKey": "B", "state": "deleted"},
        ],
        "dccEvents": [
            {"restorePointId": "B3", "fileKey": "B", "eventType": "add"},
            {"restorePointId": "B4", "fileKey": "B", "eventType": "modify"},
            {"restorePointId": "B5", "fileKey": "B", "eventType": "delete"},
        ],
    }


def _run_validator(
    tmp_path: Path,
    manifest: dict[str, object],
    *,
    mode: str = "validate-chain",
    restore_point: str | None = None,
    expect_file: str | None = None,
    expect_state: str | None = None,
    expect_content_hash: str | None = None,
) -> tuple[dict[str, object], subprocess.CompletedProcess[str]]:
    manifest_path = tmp_path / f"{mode}.json"
    output_path = tmp_path / f"{mode}-result.json"
    _write_json(manifest_path, manifest)
    command = [
        "powershell.exe",
        "-NoProfile",
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        str(VALIDATOR_SCRIPT),
        "-Mode",
        mode,
        "-BackupManifestPath",
        str(manifest_path),
        "-OutputPath",
        str(output_path),
    ]
    if restore_point is not None:
        command.extend(["-RestorePoint", restore_point])
    if expect_file is not None:
        command.extend(["-ExpectFile", expect_file])
    if expect_state is not None:
        command.extend(["-ExpectState", expect_state])
    if expect_content_hash is not None:
        command.extend(["-ExpectContentHash", expect_content_hash])

    result = subprocess.run(
        command,
        cwd=REPO_ROOT,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        check=False,
    )
    assert output_path.exists(), result.stderr + result.stdout
    return _read_json(output_path), result


def _assert_code(payload: dict[str, object], code: str) -> None:
    errors = payload.get("errors", [])
    assert isinstance(errors, list)
    assert any(item.get("code") == code for item in errors), payload


def test_missing_delete_event_blocks_chain(tmp_path: Path) -> None:
    manifest = _valid_manifest()
    manifest["dccEvents"] = [event for event in manifest["dccEvents"] if event["eventType"] != "delete"]

    payload, result = _run_validator(tmp_path, manifest)

    assert result.returncode == 2, result.stderr + result.stdout
    assert payload["status"] == "blocked"
    _assert_code(payload, "dcc_delete_event_missing")


def test_b3_b4_b5_restore_point_semantics(tmp_path: Path) -> None:
    manifest = _valid_manifest()

    payload, result = _run_validator(
        tmp_path,
        manifest,
        mode="validate-point",
        restore_point="B3",
        expect_file="B",
        expect_state="active",
        expect_content_hash=_hash("b3"),
    )
    assert result.returncode == 0, result.stderr + result.stdout
    assert payload["status"] == "passed"

    payload, result = _run_validator(
        tmp_path,
        manifest,
        mode="validate-point",
        restore_point="B4",
        expect_file="B",
        expect_state="active",
        expect_content_hash=_hash("b4"),
    )
    assert result.returncode == 0, result.stderr + result.stdout
    assert payload["status"] == "passed"

    payload, result = _run_validator(
        tmp_path,
        manifest,
        mode="validate-point",
        restore_point="B5",
        expect_file="B",
        expect_state="deleted",
    )
    assert result.returncode == 0, result.stderr + result.stdout
    assert payload["status"] == "passed"


def test_plan_restore_replays_chain_to_requested_restore_point(tmp_path: Path) -> None:
    manifest = _valid_manifest()

    payload, result = _run_validator(tmp_path, manifest, mode="plan-restore", restore_point="B4")

    assert result.returncode == 0, result.stderr + result.stdout
    assert payload["status"] == "passed"
    assert payload["restorePlan"]["baselineRestorePointId"] == "B1"
    assert [segment["to"] for segment in payload["restorePlan"]["segments"]] == ["B2", "B3", "B4"]
    assert payload["restorePlan"]["databaseRecords"][0]["restorePointId"] == "B4"
    assert payload["restorePlan"]["objectInventory"]["restorePointId"] == "B4"


def test_plan_restore_blocks_unreachable_restore_point(tmp_path: Path) -> None:
    manifest = _valid_manifest()

    payload, result = _run_validator(tmp_path, manifest, mode="plan-restore", restore_point="B6")

    assert result.returncode == 2, result.stderr + result.stdout
    assert payload["status"] == "blocked"
    _assert_code(payload, "restore_point_unreachable")


def test_missing_increment_segment_blocks_chain(tmp_path: Path) -> None:
    manifest = _valid_manifest()
    manifest["incrementalChain"] = [
        {"from": "B1", "to": "B2", "checksum": _hash("b2")},
        {"from": "B3", "to": "B4", "checksum": _hash("b4")},
    ]

    payload, result = _run_validator(tmp_path, manifest)

    assert result.returncode == 2, result.stderr + result.stdout
    _assert_code(payload, "incremental_chain_broken")


def test_restore_preflight_requires_schema_chain_status_and_previous_pointer(tmp_path: Path) -> None:
    missing_schema_manifest = _valid_manifest()
    del missing_schema_manifest["schemaVersion"]
    payload, result = _run_validator(tmp_path, missing_schema_manifest)
    assert result.returncode == 2, result.stderr + result.stdout
    _assert_code(payload, "schema_version_invalid")

    incomplete_chain_manifest = _valid_manifest()
    incomplete_chain_manifest["chainStatus"] = "INCOMPLETE"
    payload, result = _run_validator(tmp_path, incomplete_chain_manifest)
    assert result.returncode == 2, result.stderr + result.stdout
    _assert_code(payload, "chain_status_incomplete")

    missing_previous_manifest = _valid_manifest()
    missing_previous_manifest["previousBackupId"] = ""
    payload, result = _run_validator(tmp_path, missing_previous_manifest)
    assert result.returncode == 2, result.stderr + result.stdout
    _assert_code(payload, "previous_pointer_missing")


def test_restore_preflight_blocks_invalid_segment_checksum(tmp_path: Path) -> None:
    manifest = _valid_manifest()
    manifest["incrementalChain"][1]["checksum"] = "sha256:not-a-valid-segment-checksum"

    payload, result = _run_validator(tmp_path, manifest)

    assert result.returncode == 2, result.stderr + result.stdout
    _assert_code(payload, "segment_checksum_invalid")


def test_active_object_missing_or_hash_mismatch_blocks_chain(tmp_path: Path) -> None:
    missing_object_manifest = _valid_manifest()
    missing_object_manifest["objectInventories"][0]["objects"][0]["present"] = False
    payload, result = _run_validator(tmp_path, missing_object_manifest)
    assert result.returncode == 2, result.stderr + result.stdout
    _assert_code(payload, "object_missing")

    hash_mismatch_manifest = _valid_manifest()
    hash_mismatch_manifest["objectInventories"][0]["objects"][0]["storedHash"] = _hash("wrong")
    payload, result = _run_validator(tmp_path, hash_mismatch_manifest)
    assert result.returncode == 2, result.stderr + result.stdout
    _assert_code(payload, "object_hash_mismatch")


def test_missing_permission_change_event_blocks_chain(tmp_path: Path) -> None:
    manifest = _valid_manifest()
    manifest["databaseRecords"].append(
        {"restorePointId": "B4", "fileKey": "B", "state": "active", "permissionChanged": True}
    )

    payload, result = _run_validator(tmp_path, manifest)

    assert result.returncode == 2, result.stderr + result.stdout
    _assert_code(payload, "permission_event_missing")


def test_restore_verified_requires_rehearsal_evidence(tmp_path: Path) -> None:
    manifest = deepcopy(_valid_manifest())
    manifest["restoreVerified"] = True
    manifest["restoreRehearsal"] = {"status": "not-run"}

    payload, result = _run_validator(tmp_path, manifest)

    assert result.returncode == 2, result.stderr + result.stdout
    _assert_code(payload, "restore_rehearsal_missing")
