import json
import subprocess
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
BUILDER_SCRIPT = REPO_ROOT / "script" / "backup-ops" / "scripts" / "build-dcc-backup-manifest.ps1"
EXPORTER_SCRIPT = REPO_ROOT / "script" / "backup-ops" / "scripts" / "export-dcc-database-snapshot.ps1"


def _hash(label: str) -> str:
    return "sha256:" + label.lower().replace("-", "").ljust(64, "0")[:64]


def _write_json(path: Path, value: dict[str, object]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def _read_json(path: Path) -> dict[str, object]:
    return json.loads(path.read_text(encoding="utf-8"))


def _snapshot(records: list[dict[str, object]]) -> dict[str, object]:
    return {
        "schemaVersion": "dcc-snapshot-v1",
        "schemaVersionTag": "schema-20260609",
        "capturedAt": "2026-06-09T00:00:00+08:00",
        "controlledFiles": records,
    }


def _record(
    key: str,
    object_path: str,
    *,
    state: str = "active",
    permission_digest: str = "permission-v1",
    database_digest: str | None = None,
) -> dict[str, object]:
    return {
        "fileKey": key,
        "controlledFileId": int(key.split(":")[-1]),
        "tenantId": 122,
        "fileNumber": key.replace("controlled-file:", "DCC-"),
        "versionNo": "A",
        "state": state,
        "permissionDigest": permission_digest,
        "databaseDigest": database_digest or f"db-{key}",
        "objects": [{"role": "published", "path": object_path}],
    }


def _object_inventory(paths: dict[str, str]) -> dict[str, object]:
    return {
        "mode": "incremental-manifest",
        "bucket": "yudao",
        "objectStoreRoot": "/mnt/nas/Backup/BackupPackage/object-store",
        "stats": {"addedCount": len(paths), "modifiedCount": 0, "deletedCount": 0, "reusedCount": 0},
        "objects": [
            {
                "path": path,
                "sha256": digest,
                "repositoryKey": digest,
                "size": 10,
                "lastModified": "2026-06-09T00:00:00+08:00",
                "status": "active",
            }
            for path, digest in sorted(paths.items())
        ],
    }


def _run_builder(
    tmp_path: Path,
    snapshot: dict[str, object],
    inventory: dict[str, object],
    *,
    backup_id: str = "20260609-000001",
    restore_point: str = "B1",
    previous_manifest: dict[str, object] | None = None,
    target_environment: str = "test",
    target_host: str = "172.30.30.58",
) -> tuple[dict[str, object], subprocess.CompletedProcess[str]]:
    snapshot_path = tmp_path / f"{restore_point}-snapshot.json"
    inventory_path = tmp_path / f"{restore_point}-inventory.json"
    output_path = tmp_path / f"{restore_point}-dcc-manifest.json"
    _write_json(snapshot_path, snapshot)
    _write_json(inventory_path, inventory)

    command = [
        "powershell.exe",
        "-NoProfile",
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        str(BUILDER_SCRIPT),
        "-BackupId",
        backup_id,
        "-RestorePointId",
        restore_point,
        "-TargetEnvironment",
        target_environment,
        "-TargetHost",
        target_host,
        "-DccSnapshotPath",
        str(snapshot_path),
        "-ObjectInventoryPath",
        str(inventory_path),
        "-OutputPath",
        str(output_path),
    ]
    if previous_manifest is not None:
        previous_path = tmp_path / f"{restore_point}-previous-manifest.json"
        _write_json(previous_path, previous_manifest)
        command.extend(["-PreviousManifestPath", str(previous_path)])

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


def _query_result(rows: list[dict[str, object]]) -> dict[str, object]:
    return {
        "schemaVersion": "dcc-database-query-v1",
        "columns": [
            "controlledFileId",
            "tenantId",
            "fileNumber",
            "versionNo",
            "status",
            "updatedAt",
            "objectRole",
            "objectFileId",
            "objectPath",
            "objectSha256",
            "permissionDigest",
        ],
        "rows": rows,
    }


def _query_row(
    key: int,
    object_path: str,
    *,
    tenant_id: int = 122,
    object_role: str = "published",
    permission_digest: str = "permission-v1",
    status: str = "PUBLISHED",
) -> dict[str, object]:
    return {
        "controlledFileId": key,
        "tenantId": tenant_id,
        "fileNumber": f"DCC-{key}",
        "versionNo": "V1.0",
        "status": status,
        "updatedAt": "2026-06-09T09:00:00+08:00",
        "objectRole": object_role,
        "objectFileId": key + 9000,
        "objectPath": object_path,
        "objectSha256": _hash(f"object-{key}"),
        "permissionDigest": permission_digest,
    }


def _run_exporter(
    tmp_path: Path,
    query_result: dict[str, object],
    *,
    target_environment: str = "test",
    target_host: str = "local",
    tenant_id: int = 122,
) -> tuple[dict[str, object] | None, subprocess.CompletedProcess[str]]:
    query_path = tmp_path / "dcc-query-result.json"
    output_path = tmp_path / "dcc-snapshot.json"
    _write_json(query_path, query_result)
    command = [
        "powershell.exe",
        "-NoProfile",
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        str(EXPORTER_SCRIPT),
        "-TargetEnvironment",
        target_environment,
        "-TargetHost",
        target_host,
        "-TenantId",
        str(tenant_id),
        "-QueryResultJsonPath",
        str(query_path),
        "-OutputPath",
        str(output_path),
    ]
    result = subprocess.run(
        command,
        cwd=REPO_ROOT,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        check=False,
    )
    payload = _read_json(output_path) if output_path.exists() else None
    return payload, result


def _run_exporter_from_mysql_cli_output(
    tmp_path: Path,
    cli_output: str,
    *,
    target_environment: str = "test",
    target_host: str = "local",
    tenant_id: int = 122,
) -> tuple[dict[str, object] | None, subprocess.CompletedProcess[str]]:
    query_path = tmp_path / "dcc-query-result.tsv"
    output_path = tmp_path / "dcc-snapshot.json"
    query_path.write_text(cli_output, encoding="utf-8")
    command = [
        "powershell.exe",
        "-NoProfile",
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        str(EXPORTER_SCRIPT),
        "-TargetEnvironment",
        target_environment,
        "-TargetHost",
        target_host,
        "-TenantId",
        str(tenant_id),
        "-MySqlCliOutputPath",
        str(query_path),
        "-OutputPath",
        str(output_path),
    ]
    result = subprocess.run(
        command,
        cwd=REPO_ROOT,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        check=False,
    )
    payload = _read_json(output_path) if output_path.exists() else None
    return payload, result


def _event_types(manifest: dict[str, object], file_key: str) -> set[str]:
    return {
        event["eventType"]
        for event in manifest.get("dccEvents", [])
        if event.get("fileKey") == file_key
    }


def test_builds_full_baseline_dcc_manifest(tmp_path: Path) -> None:
    snapshot = _snapshot([_record("controlled-file:100", "dcc/published/a.pdf")])
    inventory = _object_inventory({"dcc/published/a.pdf": _hash("a-v1")})

    manifest, result = _run_builder(tmp_path, snapshot, inventory)

    assert result.returncode == 0, result.stderr + result.stdout
    assert manifest["status"] == "success"
    assert manifest["backupMode"] == "baseline"
    assert manifest["baselineBackupId"] == "20260609-000001"
    assert manifest["baselineRestorePointId"] == "B1"
    assert manifest["previousBackupId"] is None
    assert manifest["previousRestorePointId"] is None
    assert manifest["chainStatus"] == "COMPLETE"
    assert manifest["changeSummary"] == {
        "addedRecords": 1,
        "changedRecords": 0,
        "deletedRecords": 0,
        "invalidatedRecords": 0,
        "addedObjects": 1,
        "changedObjects": 0,
        "reusedObjects": 0,
        "tombstoneObjects": 0,
    }
    assert manifest["targetEnvironment"] == "test"
    assert manifest["targetHost"] == "172.30.30.58"
    assert manifest["fullBaseline"]["restorePointId"] == "B1"
    assert manifest["restorePoints"] == [
        {"id": "B1", "databaseRestorePointId": "B1", "objectInventoryRestorePointId": "B1"}
    ]
    assert manifest["databaseRecords"][0]["fileKey"] == "controlled-file:100"
    assert manifest["objectInventories"][0]["objects"][0]["state"] == "active"
    assert manifest["objectInventories"][0]["objects"][0]["present"] is True
    assert _event_types(manifest, "controlled-file:100") == {"add"}


def test_incremental_builder_detects_modify_delete_and_permission_events(tmp_path: Path) -> None:
    previous_snapshot = _snapshot(
        [
            _record("controlled-file:100", "dcc/published/a.pdf", permission_digest="permission-v1"),
            _record("controlled-file:101", "dcc/published/b.pdf"),
            _record("controlled-file:102", "dcc/published/d.pdf"),
        ]
    )
    previous_inventory = _object_inventory(
        {
            "dcc/published/a.pdf": _hash("a-v1"),
            "dcc/published/b.pdf": _hash("b-v1"),
            "dcc/published/d.pdf": _hash("d-v1"),
        }
    )
    previous_manifest, previous_result = _run_builder(
        tmp_path,
        previous_snapshot,
        previous_inventory,
        backup_id="20260609-000001",
        restore_point="B1",
    )
    assert previous_result.returncode == 0, previous_result.stderr + previous_result.stdout

    current_snapshot = _snapshot(
        [
            _record("controlled-file:100", "dcc/published/a.pdf", permission_digest="permission-v2"),
            _record("controlled-file:101", "dcc/published/b.pdf"),
            _record("controlled-file:103", "dcc/published/c.pdf"),
        ]
    )
    current_inventory = _object_inventory(
        {
            "dcc/published/a.pdf": _hash("a-v1"),
            "dcc/published/b.pdf": _hash("b-v2"),
            "dcc/published/c.pdf": _hash("c-v1"),
        }
    )

    manifest, result = _run_builder(
        tmp_path,
        current_snapshot,
        current_inventory,
        backup_id="20260609-000002",
        restore_point="B2",
        previous_manifest=previous_manifest,
    )

    assert result.returncode == 0, result.stderr + result.stdout
    assert manifest["backupMode"] == "incremental"
    assert manifest["baselineBackupId"] == "20260609-000001"
    assert manifest["baselineRestorePointId"] == "B1"
    assert manifest["previousBackupId"] == "20260609-000001"
    assert manifest["previousRestorePointId"] == "B1"
    assert manifest["chainStatus"] == "COMPLETE"
    assert manifest["changeSummary"] == {
        "addedRecords": 1,
        "changedRecords": 2,
        "deletedRecords": 1,
        "invalidatedRecords": 0,
        "addedObjects": 1,
        "changedObjects": 1,
        "reusedObjects": 1,
        "tombstoneObjects": 1,
    }
    assert manifest["fullBaseline"]["restorePointId"] == "B1"
    assert manifest["incrementalChain"][-1]["from"] == "B1"
    assert manifest["incrementalChain"][-1]["to"] == "B2"
    assert _event_types(manifest, "controlled-file:100") == {"add", "permission_change"}
    assert _event_types(manifest, "controlled-file:101") == {"add", "modify"}
    assert _event_types(manifest, "controlled-file:102") == {"add", "delete"}
    assert _event_types(manifest, "controlled-file:103") == {"add"}
    latest_inventory = manifest["objectInventories"][-1]["objects"]
    deleted = [item for item in latest_inventory if item["fileKey"] == "controlled-file:102"]
    assert deleted[0]["state"] == "deleted"
    assert deleted[0]["present"] is False


def test_missing_object_inventory_entry_blocks_active_dcc_record(tmp_path: Path) -> None:
    snapshot = _snapshot([_record("controlled-file:100", "dcc/published/missing.pdf")])
    inventory = _object_inventory({})

    payload, result = _run_builder(tmp_path, snapshot, inventory)

    assert result.returncode == 2, result.stderr + result.stdout
    assert payload["status"] == "blocked"
    assert any(error["code"] == "dcc_object_inventory_missing" for error in payload["errors"])
    coverage = payload["summary"]["dccObjectInventoryCoverage"]
    assert coverage["controlledFileRecordCount"] == 1
    assert coverage["inventoryObjectCount"] == 0
    assert coverage["missingReferenceCount"] == 1
    assert coverage["uniqueMissingReferenceCount"] == 1
    assert coverage["missingReferences"] == [
        {
            "fileKey": "controlled-file:100",
            "path": "dcc/published/missing.pdf",
        }
    ]
    assert coverage["samples"] == [
        {
            "fileKey": "controlled-file:100",
            "path": "dcc/published/missing.pdf",
        }
    ]


def test_target_host_proof_required_for_dcc_manifest(tmp_path: Path) -> None:
    snapshot = _snapshot([_record("controlled-file:100", "dcc/published/a.pdf")])
    inventory = _object_inventory({"dcc/published/a.pdf": _hash("a-v1")})

    payload, result = _run_builder(tmp_path, snapshot, inventory, target_host="172.30.30.57")

    assert result.returncode == 2, result.stderr + result.stdout
    assert payload["status"] == "blocked"
    assert any(error["code"] == "target_host_invalid" for error in payload["errors"])


def test_production_target_proof_allowed_for_dcc_manifest(tmp_path: Path) -> None:
    snapshot = _snapshot([_record("controlled-file:100", "dcc/published/a.pdf")])
    inventory = _object_inventory({"dcc/published/a.pdf": _hash("a-v1")})

    payload, result = _run_builder(
        tmp_path,
        snapshot,
        inventory,
        target_environment="production",
        target_host="172.30.30.57",
    )

    assert result.returncode == 0, result.stderr + result.stdout
    assert payload["status"] == "success"
    assert payload["targetEnvironment"] == "production"
    assert payload["targetHost"] == "172.30.30.57"


def test_exports_dcc_database_query_result_to_manifest_ready_snapshot(tmp_path: Path) -> None:
    payload, result = _run_exporter(
        tmp_path,
        _query_result([_query_row(100, "dcc/published/a.pdf")]),
        target_host="172.30.30.58",
    )

    assert result.returncode == 0, result.stderr + result.stdout
    assert payload is not None
    assert payload["schemaVersion"] == "dcc-snapshot-v1"
    assert payload["targetEnvironment"] == "test"
    assert payload["targetHost"] == "172.30.30.58"
    assert payload["tenantId"] == 122
    assert payload["controlledFiles"][0]["fileKey"] == "controlled-file:100"
    assert payload["controlledFiles"][0]["controlledFileId"] == 100
    assert payload["controlledFiles"][0]["objectPath"] == "dcc/published/a.pdf"
    assert payload["controlledFiles"][0]["permissionDigest"] == "permission-v1"
    assert payload["controlledFiles"][0]["updatedAt"] == "2026-06-09T09:00:00+08:00"
    assert payload["controlledFiles"][0]["objects"] == [
        {
            "role": "published",
            "fileId": 9100,
            "path": "dcc/published/a.pdf",
            "sha256": _hash("object-100"),
        }
    ]

    manifest, build_result = _run_builder(
        tmp_path,
        payload,
        _object_inventory({"dcc/published/a.pdf": _hash("a-v1")}),
    )
    assert build_result.returncode == 0, build_result.stderr + build_result.stdout
    assert manifest["status"] == "success"
    assert manifest["databaseRecords"][0]["fileKey"] == "controlled-file:100"


def test_exports_production_dcc_database_query_result_to_manifest_ready_snapshot(tmp_path: Path) -> None:
    payload, result = _run_exporter(
        tmp_path,
        _query_result([_query_row(100, "dcc/published/a.pdf")]),
        target_environment="production",
        target_host="172.30.30.57",
    )

    assert result.returncode == 0, result.stderr + result.stdout
    assert payload is not None
    assert payload["schemaVersion"] == "dcc-snapshot-v1"
    assert payload["targetEnvironment"] == "production"
    assert payload["targetHost"] == "172.30.30.57"

    manifest, build_result = _run_builder(
        tmp_path,
        payload,
        _object_inventory({"dcc/published/a.pdf": _hash("a-v1")}),
        target_environment="production",
        target_host="172.30.30.57",
    )
    assert build_result.returncode == 0, build_result.stderr + build_result.stdout
    assert manifest["status"] == "success"
    assert manifest["targetEnvironment"] == "production"
    assert manifest["targetHost"] == "172.30.30.57"


def test_dcc_snapshot_export_blocks_active_file_without_object_path(tmp_path: Path) -> None:
    payload, result = _run_exporter(
        tmp_path,
        _query_result([_query_row(100, "")]),
        target_host="172.30.30.58",
    )

    assert result.returncode == 2, result.stderr + result.stdout
    assert payload is not None
    assert payload["status"] == "blocked"
    assert any(error["code"] == "dcc_active_object_path_missing" for error in payload["errors"])


def test_dcc_snapshot_export_requires_target_and_tenant_proof(tmp_path: Path) -> None:
    payload, result = _run_exporter(
        tmp_path,
        _query_result([_query_row(100, "dcc/published/a.pdf")]),
        target_environment="staging",
        target_host="172.30.30.57",
    )

    assert result.returncode != 0, result.stderr + result.stdout
    if payload is not None:
        assert payload["status"] == "blocked"
        codes = {error["code"] for error in payload["errors"]}
        assert {"target_environment_invalid", "target_host_invalid"}.issubset(codes)


def test_dcc_snapshot_export_blocks_missing_required_query_field(tmp_path: Path) -> None:
    row = _query_row(100, "dcc/published/a.pdf")
    del row["permissionDigest"]

    payload, result = _run_exporter(
        tmp_path,
        _query_result([row]),
        target_host="172.30.30.58",
    )

    assert result.returncode == 2, result.stderr + result.stdout
    assert payload is not None
    assert payload["status"] == "blocked"
    assert any(error["code"] == "dcc_query_field_missing" for error in payload["errors"])


def test_dcc_snapshot_export_blocks_empty_query_result(tmp_path: Path) -> None:
    payload, result = _run_exporter(
        tmp_path,
        _query_result([]),
        target_host="172.30.30.58",
    )

    assert result.returncode == 2, result.stderr + result.stdout
    assert payload is not None
    assert payload["status"] == "blocked"
    assert any(error["code"] == "dcc_snapshot_no_records" for error in payload["errors"])


def test_dcc_snapshot_export_ignores_mysql_cli_password_warning_line(tmp_path: Path) -> None:
    columns = _query_result([])["columns"]
    row = _query_row(100, "dcc/published/a.pdf")
    cli_output = "\n".join(
        [
            "\t".join(columns),
            "\t".join(str(row[column]) for column in columns),
            "mysql: [Warning] Using a password on the command line interface can be insecure.",
            "",
        ]
    )

    payload, result = _run_exporter_from_mysql_cli_output(
        tmp_path,
        cli_output,
        target_host="172.30.30.58",
    )

    assert result.returncode == 0, result.stderr + result.stdout
    assert payload is not None
    assert payload["schemaVersion"] == "dcc-snapshot-v1"
    assert payload["controlledFiles"][0]["fileKey"] == "controlled-file:100"
