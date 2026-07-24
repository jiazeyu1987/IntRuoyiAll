import hashlib
import json
import re
import shutil
import subprocess
from datetime import datetime
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
FIXTURES = ROOT / "script" / "tests" / "fixtures" / "deploy-precheck"
SCRIPT = ROOT / "script" / "release" / "run-deploy-precheck-report.ps1"
MODULE = ROOT / "script" / "release" / "lib" / "DeployPrecheckReport.psm1"
SCHEMA = ROOT / "script" / "release" / "templates" / "deploy-target-config.schema.json"

REQUIRED_RESULT_FIELDS = {
    "status",
    "mode",
    "deployBehavior",
    "packageId",
    "manifestVersion",
    "environment",
    "targetConfigId",
    "checkedAt",
    "changesDeployExitCode",
    "errors",
    "warnings",
    "checks",
}
REQUIRED_DIAGNOSTIC_FIELDS = {
    "code",
    "status",
    "scope",
    "message",
    "impact",
    "nextStep",
}


def _copy_package(name: str, tmp_path: Path) -> Path:
    package = tmp_path / name
    shutil.copytree(FIXTURES / "packages" / name, package)
    if (package / "manifest.json").exists():
        _refresh_manifest_hashes(package)
    return package


def _read_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def _write_json(path: Path, value: dict) -> None:
    path.write_text(
        json.dumps(value, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )


def _sha256_prefixed(path: Path) -> str:
    return f"sha256:{hashlib.sha256(path.read_bytes()).hexdigest()}"


def _load_manifest(package: Path) -> dict:
    return _read_json(package / "manifest.json")


def _write_manifest(package: Path, manifest: dict) -> None:
    _write_json(package / "manifest.json", manifest)


def _write_resource_delta_proof(package: Path, proof: dict | str) -> None:
    proof_path = package / "resources" / "resource-delta-proof.json"
    proof_path.parent.mkdir(parents=True, exist_ok=True)
    if isinstance(proof, str):
        proof_path.write_text(proof, encoding="utf-8")
    else:
        _write_json(proof_path, proof)

    manifest = _load_manifest(package)
    manifest["targetRequirements"]["requiresResourceDeltaProof"] = True
    manifest["resources"]["resourceDeltaPrepared"] = True
    manifest["resources"]["resourceDeltaId"] = "resource-delta-test"
    manifest["resources"]["resourceDeltaProofPath"] = "resources/resource-delta-proof.json"
    manifest["resources"]["resourceSnapshotId"] = None
    _write_manifest(package, manifest)


def _resource_delta_proof(status: str, *, conflict_objects: int = 0) -> dict:
    return {
        "schemaVersion": "1.0",
        "status": status,
        "mode": "plan-only",
        "plannedAt": "2026-06-06T00:00:00Z",
        "summary": {
            "sourceReferenceCount": 3,
            "targetObjectCount": 0,
            "copyObjects": 3,
            "verifyOnlyObjects": 0,
            "conflictObjects": conflict_objects,
            "tombstoneObjects": 0,
        },
        "copyObjects": [],
        "verifyOnlyObjects": [],
        "conflictObjects": [
            {
                "storageProfileId": "minio-yudao-default",
                "bucket": "yudao",
                "objectKey": "dcc/conflict.pdf",
                "size": 1,
                "sha256": "sha256:0000000000000000000000000000000000000000000000000000000000000001",
            }
        ][:conflict_objects],
        "tombstoneObjects": [],
        "errors": (
            [
                {
                    "code": "RESOURCE_DELTA_CONFLICT",
                    "message": "conflict",
                    "impact": "conflict",
                    "nextStep": "resolve conflict",
                }
            ]
            if status == "failed"
            else []
        ),
    }


def _refresh_manifest_hashes(package: Path) -> None:
    manifest = _load_manifest(package)
    for artifact in manifest.get("artifacts", []):
        if artifact.get("includedInPackage") is True and artifact.get("path"):
            artifact_path = package / artifact["path"]
            if artifact_path.exists():
                digest = _sha256_prefixed(artifact_path)
                artifact["sha256"] = digest
                artifact["digest"] = digest
                artifact["artifactHash"] = digest
                artifact["size"] = artifact_path.stat().st_size

    database = manifest.get("database", {})
    for migration in database.get("schemaMigrations", []):
        if migration.get("file"):
            migration_path = package / migration["file"]
            if migration_path.exists():
                migration["sha256"] = _sha256_prefixed(migration_path)
    for data_set in database.get("requiredDataSets", []):
        if data_set.get("file"):
            data_set_path = package / data_set["file"]
            if data_set_path.exists():
                data_set["sha256"] = _sha256_prefixed(data_set_path)

    _write_manifest(package, manifest)


def _run_precheck(
    package: Path,
    tmp_path: Path,
    *,
    environment: str = "test",
    target_config: Path | None = None,
) -> tuple[dict, subprocess.CompletedProcess[str]]:
    output_path = tmp_path / "deploy-precheck-result.json"
    command = [
        "powershell.exe",
        "-NoProfile",
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        str(SCRIPT),
        "-PackagePath",
        str(package),
        "-Environment",
        environment,
        "-TargetConfigPath",
        str(target_config or FIXTURES / "target-config.test.json"),
        "-Mode",
        "report-only",
        "-OutputPath",
        str(output_path),
    ]
    result = subprocess.run(
        command,
        cwd=ROOT,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        check=False,
    )
    assert output_path.exists(), (
        "deploy precheck must always write deploy-precheck-result.json; "
        f"returncode={result.returncode}, stdout={result.stdout!r}, stderr={result.stderr!r}"
    )
    return _read_json(output_path), result


def _assert_result_contract(payload: dict) -> None:
    assert REQUIRED_RESULT_FIELDS <= set(payload)
    assert payload["mode"] == "report-only"
    assert payload["deployBehavior"] == "deploy-release"
    assert payload["changesDeployExitCode"] is False
    assert isinstance(payload["errors"], list)
    assert isinstance(payload["warnings"], list)
    assert isinstance(payload["checks"], list)
    datetime.fromisoformat(payload["checkedAt"].replace("Z", "+00:00"))
    for collection_name in ("errors", "warnings", "checks"):
        for item in payload[collection_name]:
            assert REQUIRED_DIAGNOSTIC_FIELDS <= set(item), collection_name


def _assert_code(payload: dict, code: str, *, collection: str = "errors") -> None:
    assert any(item["code"] == code for item in payload[collection]), payload


def _assert_failed_with(
    package: Path,
    tmp_path: Path,
    code: str,
    *,
    environment: str = "test",
    target_config: Path | None = None,
) -> dict:
    payload, result = _run_precheck(
        package,
        tmp_path,
        environment=environment,
        target_config=target_config,
    )
    _assert_result_contract(payload)
    assert result.returncode == 2, result.stdout + result.stderr
    assert payload["status"] == "failed"
    _assert_code(payload, code)
    return payload


def test_valid_v1_precheck_report_passes_without_changing_deploy_exit_code(
    tmp_path: Path,
) -> None:
    package = _copy_package("valid-v1", tmp_path)

    payload, result = _run_precheck(package, tmp_path)

    _assert_result_contract(payload)
    assert result.returncode == 0, result.stdout + result.stderr
    assert payload["status"] == "passed"
    assert payload["packageId"] == "deploy-precheck-valid-v1"
    assert payload["manifestVersion"] == "1.0"
    assert payload["environment"] == "test"
    assert payload["targetConfigId"] == "test-default"
    assert payload["errors"] == []
    assert payload["warnings"] == []
    assert {check["status"] for check in payload["checks"]} == {"passed"}
    assert "status=passed" in result.stdout


def test_missing_target_config_fails_fast_with_structured_report(
    tmp_path: Path,
) -> None:
    package = _copy_package("valid-v1", tmp_path)
    missing_config = tmp_path / "missing-target-config.json"

    payload = _assert_failed_with(
        package,
        tmp_path,
        "DEPLOY_TARGET_CONFIG_MISSING",
        target_config=missing_config,
    )

    assert payload["targetConfigId"] is None


def test_invalid_target_config_json_reports_parse_failure(tmp_path: Path) -> None:
    package = _copy_package("valid-v1", tmp_path)

    _assert_failed_with(
        package,
        tmp_path,
        "DEPLOY_TARGET_CONFIG_INVALID",
        target_config=FIXTURES / "target-config.invalid-json.json",
    )


def test_unsupported_target_config_schema_reports_schema_failure(tmp_path: Path) -> None:
    package = _copy_package("valid-v1", tmp_path)

    _assert_failed_with(
        package,
        tmp_path,
        "DEPLOY_TARGET_CONFIG_SCHEMA_UNSUPPORTED",
        target_config=FIXTURES / "target-config.unsupported-schema.json",
    )


def test_invalid_environment_reports_without_powershell_parameter_fallback(
    tmp_path: Path,
) -> None:
    package = _copy_package("valid-v1", tmp_path)

    payload = _assert_failed_with(
        package,
        tmp_path,
        "DEPLOY_ENVIRONMENT_INVALID",
        environment="staging",
    )

    assert payload["environment"] == "staging"


def test_target_config_environment_mismatch_reports_selected_environment(
    tmp_path: Path,
) -> None:
    package = _copy_package("valid-v1", tmp_path)

    payload = _assert_failed_with(
        package,
        tmp_path,
        "DEPLOY_TARGET_CONFIG_ENVIRONMENT_MISMATCH",
        target_config=FIXTURES / "target-config.prod.json",
    )

    assert any(
        error["scope"] == "targetConfig.environmentCode"
        for error in payload["errors"]
    )


def test_manifest_environment_mismatch_reports_target_requirement(
    tmp_path: Path,
) -> None:
    package = _copy_package("valid-v1", tmp_path)

    payload = _assert_failed_with(
        package,
        tmp_path,
        "DEPLOY_TARGET_REQUIREMENT_MISMATCH",
        environment="backup",
        target_config=FIXTURES / "target-config.backup.json",
    )

    assert any(
        error["scope"] == "targetRequirements.environmentCodes"
        for error in payload["errors"]
    )


def test_storage_profile_mismatch_reports_target_capability(tmp_path: Path) -> None:
    package = _copy_package("valid-v1", tmp_path)

    _assert_failed_with(
        package,
        tmp_path,
        "DEPLOY_STORAGE_PROFILE_MISMATCH",
        target_config=FIXTURES / "target-config.storage-mismatch.json",
    )


def test_docker_profile_mismatch_reports_target_capability(tmp_path: Path) -> None:
    package = _copy_package("valid-v1", tmp_path)

    _assert_failed_with(
        package,
        tmp_path,
        "DEPLOY_DOCKER_PROFILE_MISMATCH",
        target_config=FIXTURES / "target-config.docker-mismatch.json",
    )


def test_missing_artifact_reports_deploy_artifact_missing(tmp_path: Path) -> None:
    package = _copy_package("valid-v1", tmp_path)
    (package / "backend" / "app.txt").unlink()

    payload = _assert_failed_with(package, tmp_path, "DEPLOY_ARTIFACT_MISSING")

    assert any(error["scope"] == "artifacts[0].path" for error in payload["errors"])


def test_artifact_sha256_mismatch_reports_deploy_integrity_failure(
    tmp_path: Path,
) -> None:
    package = _copy_package("valid-v1", tmp_path)
    (package / "backend" / "app.txt").write_text("changed artifact\n", encoding="utf-8")

    payload = _assert_failed_with(
        package,
        tmp_path,
        "DEPLOY_ARTIFACT_SHA256_MISMATCH",
    )

    assert any(error["scope"] == "artifacts[0].sha256" for error in payload["errors"])


def test_cache_artifact_without_target_cache_profile_reports_unavailable(
    tmp_path: Path,
) -> None:
    package = _copy_package("valid-v1", tmp_path)
    manifest = _load_manifest(package)
    manifest["artifacts"][0]["includedInPackage"] = False
    manifest["artifacts"][0]["artifactCacheUri"] = "cache://edge-release-cache/backend-app"
    _write_manifest(package, manifest)
    (package / "backend" / "app.txt").unlink()

    _assert_failed_with(
        package,
        tmp_path,
        "DEPLOY_ARTIFACT_CACHE_UNAVAILABLE",
    )


def test_resource_delta_not_verified_reports_phase1_gate(tmp_path: Path) -> None:
    package = _copy_package("valid-v1", tmp_path)
    manifest = _load_manifest(package)
    manifest["targetRequirements"]["requiresResourceDeltaProof"] = True
    manifest["resources"]["resourceDeltaPrepared"] = False
    manifest["resources"]["resourceDeltaId"] = None
    manifest["resources"]["resourceDeltaProofPath"] = None
    manifest["resources"]["resourceSnapshotId"] = None
    _write_manifest(package, manifest)

    _assert_failed_with(
        package,
        tmp_path,
        "DEPLOY_RESOURCE_DELTA_NOT_VERIFIED",
    )


def test_plan_only_resource_delta_proof_does_not_pass_deploy_precheck(tmp_path: Path) -> None:
    package = _copy_package("valid-v1", tmp_path)
    _write_resource_delta_proof(package, _resource_delta_proof("passed"))

    payload = _assert_failed_with(
        package,
        tmp_path,
        "DEPLOY_RESOURCE_DELTA_NOT_VERIFIED",
    )

    assert any(
        error["scope"] == "resources.resourceDeltaProofPath"
        for error in payload["errors"]
    )


def test_failed_resource_delta_proof_fails_deploy_precheck(tmp_path: Path) -> None:
    package = _copy_package("valid-v1", tmp_path)
    _write_resource_delta_proof(
        package,
        _resource_delta_proof("failed", conflict_objects=1),
    )

    payload = _assert_failed_with(
        package,
        tmp_path,
        "DEPLOY_RESOURCE_DELTA_PROOF_FAILED",
    )

    assert any("RESOURCE_DELTA_CONFLICT" in error["message"] for error in payload["errors"])


def test_completed_verified_resource_delta_proof_passes_resource_gate(tmp_path: Path) -> None:
    package = _copy_package("valid-v1", tmp_path)
    _write_resource_delta_proof(package, _resource_delta_proof("completed_verified"))

    payload, result = _run_precheck(package, tmp_path)

    _assert_result_contract(payload)
    assert result.returncode == 0, result.stdout + result.stderr
    assert payload["status"] == "passed"
    assert not any(error["scope"].startswith("resources.") for error in payload["errors"])
    assert any(
        check["code"] == "DEPLOY_RESOURCE_GATE_VERIFIED"
        and check["status"] == "passed"
        for check in payload["checks"]
    )


def test_invalid_resource_delta_proof_json_fails_deploy_precheck(tmp_path: Path) -> None:
    package = _copy_package("valid-v1", tmp_path)
    _write_resource_delta_proof(package, "{ invalid json")

    _assert_failed_with(
        package,
        tmp_path,
        "DEPLOY_RESOURCE_DELTA_PROOF_INVALID",
    )


def test_legacy_package_reports_warning_without_v1_failure(tmp_path: Path) -> None:
    package = _copy_package("legacy-v0", tmp_path)

    payload, result = _run_precheck(package, tmp_path)

    _assert_result_contract(payload)
    assert result.returncode == 0, result.stdout + result.stderr
    assert payload["status"] == "warning"
    assert payload["packageId"] == "legacy-deploy-precheck-fixture"
    assert payload["manifestVersion"] == "legacy-v0"
    assert payload["errors"] == []
    _assert_code(payload, "LEGACY_DEPLOY_PRECHECK_REPORT_ONLY", collection="warnings")


def test_target_requirements_hardcoded_ip_reports_forbidden_target(
    tmp_path: Path,
) -> None:
    package = _copy_package("valid-v1", tmp_path)
    manifest = _load_manifest(package)
    manifest["targetRequirements"]["operatorNote"] = "deploy target 172.30.30.58"
    _write_manifest(package, manifest)

    payload = _assert_failed_with(
        package,
        tmp_path,
        "FORBIDDEN_HARDCODED_TARGET_IP",
    )

    assert any("172.30.30.58" in error["message"] for error in payload["errors"])


def test_deploy_contract_hardcoded_ip_reports_forbidden_target(
    tmp_path: Path,
) -> None:
    package = _copy_package("valid-v1", tmp_path)
    manifest = _load_manifest(package)
    manifest["deployContract"]["targetHost"] = "172.30.30.58"
    _write_manifest(package, manifest)

    payload = _assert_failed_with(
        package,
        tmp_path,
        "FORBIDDEN_HARDCODED_TARGET_IP",
    )

    assert any("172.30.30.58" in error["message"] for error in payload["errors"])


def test_target_config_schema_documents_report_only_contract() -> None:
    schema = _read_json(SCHEMA)

    assert schema["properties"]["schemaVersion"]["const"] == "1.0"
    assert set(schema["properties"]["environmentCode"]["enum"]) == {
        "test",
        "prod",
        "backup",
    }
    assert "targetConfigId" in schema["required"]
    assert "dockerProfileId" in schema["required"]
    assert "storageProfileIds" in schema["required"]
    assert "artifactCacheProfiles" in schema["required"]
    assert "remoteReadOnlyProbe" in schema["required"]
    assert schema["properties"]["remoteReadOnlyProbe"]["properties"]["enabled"]["type"] == "boolean"


def test_precheck_implementation_and_fixtures_do_not_contain_remote_write_commands_or_server_ips() -> None:
    forbidden_ips = ("172.30.30.57", "172.30.30.58", "172.30.30.59")
    remote_write_markers = (
        "ssh ",
        "scp ",
        "rsync ",
        "Invoke-Sqlcmd",
        "docker exec",
        "docker-compose up",
        "mysql -",
        "mysqldump",
        "mc mirror",
        "mc cp",
        "kubectl ",
    )

    paths = [SCRIPT, MODULE, SCHEMA]
    paths.extend(
        path
        for path in FIXTURES.rglob("*")
        if path.is_file() and path.suffix.lower() in {".json", ".txt", ".sql", ".ps1"}
    )

    for path in paths:
        text = path.read_text(encoding="utf-8")
        for ip in forbidden_ips:
            assert ip not in text, f"{path} must not hardcode target server IP {ip}"
        for marker in remote_write_markers:
            assert marker not in text, f"{path} must not contain remote write command {marker!r}"
        assert not re.search(r"\b(INSERT|UPDATE|DELETE|DROP|TRUNCATE)\b", text, re.IGNORECASE), path
