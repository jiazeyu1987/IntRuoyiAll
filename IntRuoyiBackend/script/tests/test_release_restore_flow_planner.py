import json
import subprocess
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
FLOW_SCRIPT = REPO_ROOT / "script" / "release" / "run-release-restore-flow.ps1"


def _write_json(path: Path, value: dict[str, object]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def _read_json(path: Path) -> dict[str, object]:
    return json.loads(path.read_text(encoding="utf-8"))


def _release_manifest(**overrides: object) -> dict[str, object]:
    manifest: dict[str, object] = {
        "releaseTag": "20260609_010000",
        "publishScope": "code-only",
        "schemaVersion": "schema-20260609",
        "schemaDigest": "sha256:" + "1" * 64,
        "migrationPlan": [{"id": "V20260609_001", "direction": "forward", "destructive": False}],
        "requiredSql": [{"id": "D20260609_001", "sha256": "sha256:" + "2" * 64}],
        "schemaPreflight": {"status": "pass", "operationId": "op-schema-preflight-1"},
        "compatibilityMatrix": {
            "supportedBackupReleaseTags": ["20260609_010000"],
            "supportedBackupSchemaVersions": ["schema-20260609"],
        },
    }
    manifest.update(overrides)
    return manifest


def _backup_manifest(**overrides: object) -> dict[str, object]:
    manifest: dict[str, object] = {
        "backupId": "20260609-010000",
        "targetEnvironment": "test",
        "releaseTag": "20260609_010000",
        "programVersion": "20260609_010000",
        "schemaVersion": "schema-20260609",
        "restorePointId": "rp-20260609-010000",
        "fullBaseline": {"backupId": "20260609-000000", "checksum": "sha256:" + "3" * 64},
        "incrementalChain": [{"from": "rp-0", "to": "rp-20260609-010000", "checksum": "sha256:" + "4" * 64}],
        "objectInventory": {
            "restorePointId": "rp-20260609-010000",
            "path": "objects/inventory.json",
            "checksum": "sha256:" + "5" * 64,
        },
        "checksums": {"manifest": "sha256:" + "6" * 64},
        "recoverySet": {
            "id": "20260609-010000",
            "status": "COMPLETE",
            "program": {"imageTag": "20260609_010000"},
            "mysql": {"dumpPath": "mysql/ruoyi-vue-pro.sql.gz"},
            "minio": {"snapshotPath": "objects/manifest-object-inventory.json"},
            "businessFiles": {"snapshotPath": "objects/manifest-object-inventory.json"},
            "dcc": {"manifestPath": "manifest/dcc-backup-manifest.json"},
            "checksums": {"path": "manifest/checksums.txt", "sha256": "sha256:" + "6" * 64},
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


def _run_flow(
    tmp_path: Path,
    flow: str,
    *,
    release_manifest: dict[str, object] | None = None,
    backup_manifest: dict[str, object] | None = None,
    plan_fixture: dict[str, object] | None = None,
    target_environment: str = "test",
) -> tuple[dict[str, object], subprocess.CompletedProcess[str]]:
    output_path = tmp_path / f"{flow}-result.json"
    command = [
        "powershell.exe",
        "-NoProfile",
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        str(FLOW_SCRIPT),
        "-Flow",
        flow,
        "-TargetEnvironment",
        target_environment,
        "-Mode",
        "dry-run",
        "-OutputPath",
        str(output_path),
    ]
    if release_manifest is not None:
        release_path = tmp_path / f"{flow}-release.json"
        _write_json(release_path, release_manifest)
        command.extend(["-ReleaseManifestPath", str(release_path)])
    if backup_manifest is not None:
        backup_path = tmp_path / f"{flow}-backup.json"
        _write_json(backup_path, backup_manifest)
        command.extend(["-BackupManifestPath", str(backup_path)])
    if plan_fixture is not None:
        plan_path = tmp_path / f"{flow}-plan.json"
        _write_json(plan_path, plan_fixture)
        command.extend(["-PlanFixturePath", str(plan_path)])

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
    diagnostics = payload.get("errors", [])
    assert isinstance(diagnostics, list)
    assert any(item.get("code") == code for item in diagnostics), payload


def test_restore_only_blocks_program_publish_steps(tmp_path: Path) -> None:
    payload, result = _run_flow(
        tmp_path,
        "restore-only",
        backup_manifest=_backup_manifest(),
        plan_fixture={"steps": [{"id": "restore-database"}, {"id": "switch-backend-image"}]},
    )

    assert result.returncode == 2, result.stderr + result.stdout
    assert payload["status"] == "blocked"
    _assert_code(payload, "RESTORE_MUST_NOT_PUBLISH_PROGRAM")


def test_publish_only_code_only_forbids_business_data_and_object_sync(tmp_path: Path) -> None:
    payload, result = _run_flow(tmp_path, "publish-only", release_manifest=_release_manifest())

    assert result.returncode == 0, result.stderr + result.stdout
    assert payload["status"] == "passed"
    step_ids = {step["id"] for step in payload["steps"]}
    assert {"deploy-backend", "deploy-admin-frontend", "apply-schema-migration", "apply-required-sql"} <= step_ids
    assert "import-business-data" not in step_ids
    assert "sync-dcc-objects" not in step_ids


def test_publish_only_with_data_blocks_business_data_scope(tmp_path: Path) -> None:
    payload, result = _run_flow(
        tmp_path,
        "publish-only",
        release_manifest=_release_manifest(publishScope="with-data"),
    )

    assert result.returncode == 2, result.stderr + result.stdout
    assert payload["status"] == "blocked"
    _assert_code(payload, "PUBLISH_ONLY_MUST_NOT_SYNC_BUSINESS_DATA")


def test_missing_contracts_block_flow_preflight(tmp_path: Path) -> None:
    release_without_migration = _release_manifest()
    release_without_migration.pop("migrationPlan")
    payload, result = _run_flow(tmp_path, "publish-only", release_manifest=release_without_migration)
    assert result.returncode == 2, result.stderr + result.stdout
    _assert_code(payload, "RELEASE_MANIFEST_CONTRACT_MISSING")

    backup_without_inventory = _backup_manifest()
    backup_without_inventory.pop("objectInventory")
    payload, result = _run_flow(tmp_path, "restore-only", backup_manifest=backup_without_inventory)
    assert result.returncode == 2, result.stderr + result.stdout
    _assert_code(payload, "BACKUP_MANIFEST_CONTRACT_MISSING")


def test_restore_flows_block_incomplete_backup_strategy_or_recovery_set(tmp_path: Path) -> None:
    backup_without_strategy = _backup_manifest()
    backup_without_strategy.pop("backupStrategy")
    payload, result = _run_flow(tmp_path, "restore-only", backup_manifest=backup_without_strategy)
    assert result.returncode == 2, result.stderr + result.stdout
    _assert_code(payload, "BACKUP_MANIFEST_CONTRACT_MISSING")

    incomplete_recovery = _backup_manifest(
        recoverySet={
            "id": "20260609-010000",
            "status": "BLOCKED",
            "program": {"imageTag": "20260609_010000"},
            "mysql": {"dumpPath": "mysql/ruoyi-vue-pro.sql.gz"},
            "minio": {"snapshotPath": "objects/manifest-object-inventory.json"},
            "businessFiles": {"snapshotPath": "objects/manifest-object-inventory.json"},
            "dcc": {"manifestPath": "manifest/dcc-backup-manifest.json"},
            "checksums": {"path": "manifest/checksums.txt", "sha256": "sha256:" + "6" * 64},
        }
    )
    payload, result = _run_flow(tmp_path, "restore-only", backup_manifest=incomplete_recovery)
    assert result.returncode == 2, result.stderr + result.stdout
    _assert_code(payload, "BACKUP_RECOVERY_SET_INCOMPLETE")


def test_restore_flows_block_unverified_backup_manifest(tmp_path: Path) -> None:
    unverified_backup = _backup_manifest(
        validation={
            "rehearsalStatus": "unverified",
            "lastRehearsedAt": None,
        }
    )
    payload, result = _run_flow(tmp_path, "restore-only", backup_manifest=unverified_backup)

    assert result.returncode == 2, result.stderr + result.stdout
    assert payload["status"] == "blocked"
    _assert_code(payload, "BACKUP_RESTORE_REHEARSAL_MISSING")


def test_cross_version_restore_publish_flows_require_compatibility_matrix(tmp_path: Path) -> None:
    payload, result = _run_flow(
        tmp_path,
        "restore-then-publish",
        release_manifest=_release_manifest(
            compatibilityMatrix={"supportedBackupReleaseTags": [], "supportedBackupSchemaVersions": []}
        ),
        backup_manifest=_backup_manifest(schemaVersion="schema-20260601"),
    )
    assert result.returncode == 2, result.stderr + result.stdout
    _assert_code(payload, "FLOW_COMPATIBILITY_BLOCKED")

    payload, result = _run_flow(
        tmp_path,
        "publish-then-restore",
        release_manifest=_release_manifest(
            releaseTag="20260601_010000",
            schemaVersion="schema-20260601",
            compatibilityMatrix={"supportedBackupReleaseTags": [], "supportedBackupSchemaVersions": []},
        ),
        backup_manifest=_backup_manifest(releaseTag="20260609_010000", programVersion="20260609_010000"),
    )
    assert result.returncode == 2, result.stderr + result.stdout
    _assert_code(payload, "FLOW_COMPATIBILITY_BLOCKED")


def test_failed_child_operation_is_not_masked_by_passed_health_check(tmp_path: Path) -> None:
    payload, result = _run_flow(
        tmp_path,
        "publish-only",
        release_manifest=_release_manifest(),
        plan_fixture={
            "steps": [{"id": "apply-schema-migration"}, {"id": "health-check"}],
            "operationResults": [
                {"id": "apply-schema-migration", "status": "failed"},
                {"id": "health-check", "status": "passed"},
            ],
        },
    )

    assert result.returncode == 2, result.stderr + result.stdout
    assert payload["status"] == "failed"
    _assert_code(payload, "CHILD_OPERATION_FAILED")
