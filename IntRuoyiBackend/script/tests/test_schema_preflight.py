import json
import subprocess
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
PREFLIGHT_SCRIPT = REPO_ROOT / "script" / "release" / "run-schema-preflight.ps1"


def _write_json(path: Path, value: dict[str, object]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def _release_manifest() -> dict[str, object]:
    return {
        "releaseTag": "20260609-schema-preflight",
        "schemaVersion": "schema-20260609",
        "schemaContract": {
            "tables": [
                {
                    "name": "dcc_document",
                    "fields": [
                        {"name": "id", "type": "bigint", "nullable": False},
                        {"name": "audit_status", "type": "varchar(32)", "nullable": False},
                    ],
                    "indexes": ["idx_dcc_document_audit_status"],
                },
                {
                    "name": "system_tenant",
                    "fields": [{"name": "id", "type": "bigint", "nullable": False}],
                    "indexes": [],
                },
            ]
        },
        "migrationPlan": [],
        "requiredSql": [
            {
                "id": "menu-permission-20260609",
                "preconditions": [
                    {"type": "table", "name": "system_tenant"},
                    {"type": "field", "name": "system_tenant.id"},
                ],
                "destructive": False,
            }
        ],
    }


def _target_schema() -> dict[str, object]:
    return {
        "schemaVersion": "schema-20260601",
        "tables": [
            {
                "name": "dcc_document",
                "fields": [
                    {"name": "id", "type": "bigint", "nullable": False},
                    {"name": "audit_status", "type": "varchar(32)", "nullable": False},
                ],
                "indexes": ["idx_dcc_document_audit_status"],
            },
            {
                "name": "system_tenant",
                "fields": [{"name": "id", "type": "bigint", "nullable": False}],
                "indexes": [],
            },
        ],
    }


def _run_preflight(manifest: Path, target_schema: Path, output_path: Path) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        [
            "powershell.exe",
            "-NoProfile",
            "-ExecutionPolicy",
            "Bypass",
            "-File",
            str(PREFLIGHT_SCRIPT),
            "-ReleaseManifestPath",
            str(manifest),
            "-TargetSchemaPath",
            str(target_schema),
            "-TargetEnvironment",
            "local",
            "-OutputPath",
            str(output_path),
        ],
        cwd=REPO_ROOT,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        check=False,
    )


def _read_json(path: Path) -> dict[str, object]:
    return json.loads(path.read_text(encoding="utf-8"))


def test_missing_field_blocks_before_release(tmp_path: Path) -> None:
    manifest = _release_manifest()
    target_schema = _target_schema()
    target_schema["tables"][0]["fields"] = [{"name": "id", "type": "bigint", "nullable": False}]
    manifest_path = tmp_path / "release.json"
    target_path = tmp_path / "target.json"
    output_path = tmp_path / "preflight.json"
    _write_json(manifest_path, manifest)
    _write_json(target_path, target_schema)

    result = _run_preflight(manifest_path, target_path, output_path)

    assert result.returncode == 2, result.stderr + result.stdout
    payload = _read_json(output_path)
    assert payload["status"] == "blocked"
    assert payload["findings"][0]["code"] == "missing_field"
    assert payload["findings"][0]["objectName"] == "dcc_document.audit_status"
    assert "migration" in payload["findings"][0]["requiredResolution"]


def test_compatible_schema_passes(tmp_path: Path) -> None:
    manifest_path = tmp_path / "release.json"
    target_path = tmp_path / "target.json"
    output_path = tmp_path / "preflight.json"
    _write_json(manifest_path, _release_manifest())
    _write_json(target_path, _target_schema())

    result = _run_preflight(manifest_path, target_path, output_path)

    assert result.returncode == 0, result.stderr + result.stdout
    payload = _read_json(output_path)
    assert payload["status"] == "pass"
    assert payload["findings"] == []


def test_extra_field_is_warning_not_delete_plan(tmp_path: Path) -> None:
    target_schema = _target_schema()
    target_schema["tables"][0]["fields"].append({"name": "legacy_flag", "type": "tinyint", "nullable": True})
    manifest_path = tmp_path / "release.json"
    target_path = tmp_path / "target.json"
    output_path = tmp_path / "preflight.json"
    _write_json(manifest_path, _release_manifest())
    _write_json(target_path, target_schema)

    result = _run_preflight(manifest_path, target_path, output_path)

    assert result.returncode == 0, result.stderr + result.stdout
    payload = _read_json(output_path)
    assert payload["status"] == "pass"
    assert payload["warnings"][0]["code"] == "extra_field"
    assert "never delete automatically" in payload["warnings"][0]["requiredResolution"]


def test_destructive_migration_blocks_automatic_execution(tmp_path: Path) -> None:
    manifest = _release_manifest()
    manifest["migrationPlan"] = [
        {"id": "DROP_OLD", "destructive": True, "direction": "forward"}
    ]
    manifest_path = tmp_path / "release.json"
    target_path = tmp_path / "target.json"
    output_path = tmp_path / "preflight.json"
    _write_json(manifest_path, manifest)
    _write_json(target_path, _target_schema())

    result = _run_preflight(manifest_path, target_path, output_path)

    assert result.returncode == 2, result.stderr + result.stdout
    payload = _read_json(output_path)
    assert payload["status"] == "blocked"
    assert payload["findings"][0]["code"] == "destructive_migration"


def test_required_sql_precondition_missing_blocks_sql(tmp_path: Path) -> None:
    target_schema = _target_schema()
    target_schema["tables"] = [table for table in target_schema["tables"] if table["name"] != "system_tenant"]
    manifest_path = tmp_path / "release.json"
    target_path = tmp_path / "target.json"
    output_path = tmp_path / "preflight.json"
    _write_json(manifest_path, _release_manifest())
    _write_json(target_path, target_schema)

    result = _run_preflight(manifest_path, target_path, output_path)

    assert result.returncode == 2, result.stderr + result.stdout
    payload = _read_json(output_path)
    assert payload["status"] == "blocked"
    assert any(item["code"] == "missing_required_sql_precondition" for item in payload["findings"])
