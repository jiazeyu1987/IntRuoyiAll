import json
import subprocess
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
ROLLBACK_SCRIPT = REPO_ROOT / "script" / "release" / "run-rollback-preflight.ps1"


def _write_json(path: Path, value: dict[str, object]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def _read_json(path: Path) -> dict[str, object]:
    return json.loads(path.read_text(encoding="utf-8"))


def _evidence_file(tmp_path: Path, name: str) -> str:
    path = tmp_path / "evidence" / f"{name}.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("{}\n", encoding="utf-8")
    return str(path)


def _release_manifest(**overrides: object) -> dict[str, object]:
    manifest: dict[str, object] = {
        "releaseTag": "20260609_rollback_target",
        "publishScope": "code-only",
        "schemaVersion": "schema-20260609",
        "schemaDigest": "sha256:" + "1" * 64,
        "migrationPlan": [{"id": "V20260609_001", "direction": "forward", "destructive": False}],
        "artifactHashes": {"backend": "sha256:" + "2" * 64, "adminFrontend": "sha256:" + "3" * 64},
        "compatibilityMatrix": {
            "supportedBackupReleaseTags": ["20260609_rollback_target"],
            "supportedBackupSchemaVersions": ["schema-20260609"],
        },
    }
    manifest.update(overrides)
    return manifest


def _backup_manifest(**overrides: object) -> dict[str, object]:
    manifest: dict[str, object] = {
        "backupId": "20260609-rollback-point",
        "targetEnvironment": "test",
        "releaseTag": "20260609_rollback_target",
        "programVersion": "20260609_rollback_target",
        "schemaVersion": "schema-20260609",
        "restorePointId": "rp-rollback-20260609",
        "fullBaseline": {"backupId": "20260609-baseline", "checksum": "sha256:" + "4" * 64},
        "incrementalChain": [{"from": "rp-0", "to": "rp-rollback-20260609", "checksum": "sha256:" + "5" * 64}],
        "objectInventory": {"path": "objects/inventory.json", "checksum": "sha256:" + "6" * 64},
        "checksums": {"manifest": "sha256:" + "7" * 64},
        "recoverySet": {
            "id": "20260609-rollback-point",
            "status": "COMPLETE",
            "program": {"imageTag": "20260609_rollback_target"},
            "mysql": {"dumpPath": "mysql/ruoyi-vue-pro.sql.gz"},
            "minio": {"snapshotPath": "objects/manifest-object-inventory.json"},
            "businessFiles": {"snapshotPath": "objects/manifest-object-inventory.json"},
            "dcc": {"manifestPath": "manifest/dcc-backup-manifest.json"},
            "checksums": {"path": "manifest/checksums.txt", "sha256": "sha256:" + "7" * 64},
        },
        "backupStrategy": {
            "mode": "incremental-manifest",
            "mysqlBackupMode": "full-dump-baseline",
            "mysqlBaseline": "full-dump",
            "mysqlIncrementalPlan": {
                "binlog": {"status": "requires-prerequisite"},
                "xtrabackup": {"status": "requires-prerequisite"},
                "noFallbackRule": "No silent full dump fallback is allowed for an incremental MySQL backup request.",
            },
        },
        "validation": {
            "rehearsalStatus": "PASSED",
            "lastRehearsedAt": "2026-06-09T06:20:00+08:00",
        },
    }
    manifest.update(overrides)
    return manifest


def _verified_downgrade_evidence(tmp_path: Path, backup_id: str = "20260609-rollback-point") -> dict[str, object]:
    return {
        "approval": {
            "approved": True,
            "approvedBy": "release-owner",
            "approvedAt": "2026-06-09T06:10:00+08:00",
        },
        "backupProof": {
            "backupId": backup_id,
            "evidencePath": _evidence_file(tmp_path, "backup-proof"),
        },
        "rehearsal": {
            "status": "passed",
            "evidencePath": _evidence_file(tmp_path, "rollback-rehearsal"),
        },
        "downgradeScripts": [
            {
                "id": "D20260609_DOWNGRADE",
                "checksum": "sha256:" + "8" * 64,
            }
        ],
    }


def _run_preflight(
    tmp_path: Path,
    *,
    mode: str,
    release_manifest: dict[str, object] | None = None,
    backup_manifest: dict[str, object] | None = None,
    target_environment: str = "test",
) -> tuple[dict[str, object], subprocess.CompletedProcess[str]]:
    output_path = tmp_path / f"rollback-{mode}-preflight.json"
    command = [
        "powershell.exe",
        "-NoProfile",
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        str(ROLLBACK_SCRIPT),
        "-TargetEnvironment",
        target_environment,
        "-Mode",
        mode,
        "-OutputPath",
        str(output_path),
    ]
    if release_manifest is not None:
        release_path = tmp_path / f"rollback-{mode}-release.json"
        _write_json(release_path, release_manifest)
        command.extend(["-ReleaseManifestPath", str(release_path)])
    if backup_manifest is not None:
        backup_path = tmp_path / f"rollback-{mode}-backup.json"
        _write_json(backup_path, backup_manifest)
        command.extend(["-BackupManifestPath", str(backup_path)])

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


def test_code_rollback_preflight_does_not_restore_data(tmp_path: Path) -> None:
    payload, result = _run_preflight(tmp_path, mode="code", release_manifest=_release_manifest())

    assert result.returncode == 0, result.stderr + result.stdout
    assert payload["status"] == "pass"
    step_ids = {step["id"] for step in payload["plannedSteps"]}
    assert "switch-program-version" in step_ids
    assert "health-check" in step_ids
    assert "restore-database" not in step_ids
    assert "restore-dcc-objects" not in step_ids


def test_data_rollback_preflight_requires_backup_and_does_not_switch_program(tmp_path: Path) -> None:
    payload, result = _run_preflight(tmp_path, mode="data")
    assert result.returncode == 2, result.stderr + result.stdout
    _assert_code(payload, "ROLLBACK_BACKUP_MANIFEST_REQUIRED")

    payload, result = _run_preflight(tmp_path, mode="data", backup_manifest=_backup_manifest())
    assert result.returncode == 0, result.stderr + result.stdout
    assert payload["status"] == "pass"
    step_ids = {step["id"] for step in payload["plannedSteps"]}
    assert "restore-database" in step_ids
    assert "restore-dcc-objects" in step_ids
    assert "switch-program-version" not in step_ids


def test_data_rollback_blocks_incomplete_backup_strategy_or_recovery_set(tmp_path: Path) -> None:
    backup_without_strategy = _backup_manifest()
    backup_without_strategy.pop("backupStrategy")
    payload, result = _run_preflight(tmp_path, mode="data", backup_manifest=backup_without_strategy)
    assert result.returncode == 2, result.stderr + result.stdout
    _assert_code(payload, "ROLLBACK_BACKUP_MANIFEST_CONTRACT_MISSING")

    incomplete_recovery = _backup_manifest(
        recoverySet={
            "id": "20260609-rollback-point",
            "status": "BLOCKED",
            "program": {"imageTag": "20260609_rollback_target"},
            "mysql": {"dumpPath": "mysql/ruoyi-vue-pro.sql.gz"},
            "minio": {"snapshotPath": "objects/manifest-object-inventory.json"},
            "businessFiles": {"snapshotPath": "objects/manifest-object-inventory.json"},
            "dcc": {"manifestPath": "manifest/dcc-backup-manifest.json"},
            "checksums": {"path": "manifest/checksums.txt", "sha256": "sha256:" + "7" * 64},
        }
    )
    payload, result = _run_preflight(tmp_path, mode="data", backup_manifest=incomplete_recovery)
    assert result.returncode == 2, result.stderr + result.stdout
    _assert_code(payload, "ROLLBACK_RECOVERY_SET_INCOMPLETE")


def test_data_rollback_blocks_unverified_backup_manifest(tmp_path: Path) -> None:
    unverified_backup = _backup_manifest(
        validation={
            "rehearsalStatus": "unverified",
            "lastRehearsedAt": None,
        }
    )
    payload, result = _run_preflight(tmp_path, mode="data", backup_manifest=unverified_backup)

    assert result.returncode == 2, result.stderr + result.stdout
    assert payload["status"] == "blocked"
    _assert_code(payload, "ROLLBACK_RESTORE_REHEARSAL_MISSING")


def test_combined_rollback_blocks_schema_downgrade_without_evidence(tmp_path: Path) -> None:
    payload, result = _run_preflight(
        tmp_path,
        mode="combined",
        release_manifest=_release_manifest(
            schemaVersion="schema-20260601",
            migrationPlan=[{"id": "D20260609_DOWNGRADE", "direction": "downgrade", "destructive": False}],
            compatibilityMatrix={"supportedBackupReleaseTags": [], "supportedBackupSchemaVersions": []},
        ),
        backup_manifest=_backup_manifest(schemaVersion="schema-20260609"),
    )

    assert result.returncode == 2, result.stderr + result.stdout
    assert payload["status"] == "blocked"
    _assert_code(payload, "DOWNGRADE_EVIDENCE_MISSING")


def test_combined_rollback_passes_with_verified_downgrade_evidence(tmp_path: Path) -> None:
    payload, result = _run_preflight(
        tmp_path,
        mode="combined",
        release_manifest=_release_manifest(
            schemaVersion="schema-20260601",
            migrationPlan=[{"id": "D20260609_DOWNGRADE", "direction": "downgrade", "destructive": False}],
            compatibilityMatrix={"supportedBackupReleaseTags": [], "supportedBackupSchemaVersions": []},
            rollbackEvidence=_verified_downgrade_evidence(tmp_path),
        ),
        backup_manifest=_backup_manifest(schemaVersion="schema-20260609"),
    )

    assert result.returncode == 0, result.stderr + result.stdout
    assert payload["status"] == "pass"
    assert payload["rollbackEvidence"]["rehearsalStatus"] == "passed"


def test_destructive_rollback_requires_approval_backup_and_rehearsal(tmp_path: Path) -> None:
    payload, result = _run_preflight(
        tmp_path,
        mode="combined",
        release_manifest=_release_manifest(
            migrationPlan=[{"id": "DROP_LEGACY_TABLE", "direction": "downgrade", "destructive": True}],
        ),
        backup_manifest=_backup_manifest(),
    )

    assert result.returncode == 2, result.stderr + result.stdout
    assert payload["status"] == "blocked"
    _assert_code(payload, "DESTRUCTIVE_ROLLBACK_REQUIRES_APPROVAL")


def test_prod_rollback_preflight_requires_authorization_evidence(tmp_path: Path) -> None:
    payload, result = _run_preflight(
        tmp_path,
        mode="code",
        target_environment="prod",
        release_manifest=_release_manifest(),
    )

    assert result.returncode == 2, result.stderr + result.stdout
    assert payload["status"] == "blocked"
    _assert_code(payload, "PROD_ACCESS_NOT_AUTHORIZED")
