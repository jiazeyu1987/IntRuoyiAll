import hashlib
import json
import shutil
import subprocess
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
FIXTURES = ROOT / "script" / "tests" / "fixtures" / "release-manifest-v1"
VALIDATOR = ROOT / "script" / "release" / "validate-release-manifest.ps1"
SCHEMA = ROOT / "script" / "release" / "manifest-v1.schema.json"
VALIDATOR_MODULE = ROOT / "script" / "release" / "lib" / "ReleaseManifestValidator.psm1"


REQUIRED_PAYLOAD_FIELDS = {
    "status",
    "mode",
    "code",
    "scope",
    "path",
    "message",
    "impact",
    "nextStep",
    "errors",
    "warnings",
    "checks",
}
REQUIRED_DIAGNOSTIC_FIELDS = {
    "status",
    "code",
    "scope",
    "path",
    "message",
    "impact",
    "nextStep",
}


def copy_fixture(name: str, tmp_path: Path) -> Path:
    package = tmp_path / name
    shutil.copytree(FIXTURES / name, package)
    return package


def load_manifest(package: Path) -> dict:
    return json.loads((package / "manifest.json").read_text(encoding="utf-8"))


def write_manifest(package: Path, manifest: dict) -> None:
    (package / "manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )


def sha256_prefixed(path: Path) -> str:
    digest = hashlib.sha256(path.read_bytes()).hexdigest()
    return f"sha256:{digest}"


def add_declared_artifact(package: Path, relative_path: str, content: str) -> None:
    target = package / relative_path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(content, encoding="utf-8")
    manifest = load_manifest(package)
    manifest["artifacts"].append(
        {
            "artifactId": relative_path.replace("/", "-"),
            "componentName": "validator-test",
            "artifactType": "fixture-file",
            "path": relative_path,
            "includedInPackage": True,
            "artifactCacheUri": None,
            "sha256": sha256_prefixed(target),
            "size": target.stat().st_size,
            "digest": sha256_prefixed(target),
            "buildCacheKey": "validator-test",
            "sourceHash": "sha256:0000000000000000000000000000000000000000000000000000000000000000",
            "dependencyClosureHash": "sha256:0000000000000000000000000000000000000000000000000000000000000000",
            "buildToolchainHash": "sha256:0000000000000000000000000000000000000000000000000000000000000000",
        }
    )
    write_manifest(package, manifest)


def run_validator(
    package: Path,
    *,
    output_path: Path | None = None,
    mode: str = "report-only",
) -> tuple[dict, subprocess.CompletedProcess[str]]:
    command = [
        "powershell.exe",
        "-NoProfile",
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        str(VALIDATOR),
        "-PackagePath",
        str(package),
        "-Mode",
        mode,
    ]
    if output_path is not None:
        command.extend(["-OutputPath", str(output_path)])

    result = subprocess.run(
        command,
        cwd=ROOT,
        text=True,
        encoding="utf-8",
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    if output_path is not None:
        assert output_path.exists(), (
            "validator must write structured JSON to -OutputPath; "
            f"returncode={result.returncode}, stdout={result.stdout!r}, stderr={result.stderr!r}"
        )
        return json.loads(output_path.read_text(encoding="utf-8")), result

    assert result.stdout.strip(), (
        "validator must write structured JSON to stdout; "
        f"returncode={result.returncode}, stderr={result.stderr!r}"
    )
    try:
        payload = json.loads(result.stdout)
    except json.JSONDecodeError as exc:
        raise AssertionError(
            f"validator stdout must be JSON, got:\n{result.stdout}\nstderr:\n{result.stderr}"
        ) from exc
    return payload, result


def assert_payload_contract(payload: dict) -> None:
    assert REQUIRED_PAYLOAD_FIELDS <= set(payload)
    assert isinstance(payload["errors"], list)
    assert isinstance(payload["warnings"], list)
    assert isinstance(payload["checks"], list)
    for collection_name in ("errors", "warnings", "checks"):
        for item in payload[collection_name]:
            assert REQUIRED_DIAGNOSTIC_FIELDS <= set(item), collection_name


def assert_failed_with(package: Path, expected_code: str) -> dict:
    payload, result = run_validator(package)
    assert_payload_contract(payload)
    assert result.returncode == 1
    assert payload["status"] == "failed"
    assert payload["mode"] == "report-only"
    assert payload["code"] == expected_code
    assert any(error["code"] == expected_code for error in payload["errors"])
    return payload


def test_missing_manifest_reports_failed_structured_json(tmp_path: Path) -> None:
    package = tmp_path / "missing-manifest"
    package.mkdir()

    payload = assert_failed_with(package, "MANIFEST_MISSING")

    assert payload["path"] == str(package)
    assert payload["errors"][0]["nextStep"]


def test_invalid_json_reports_parse_error(tmp_path: Path) -> None:
    package = copy_fixture("valid-v1", tmp_path)
    (package / "manifest.json").write_text("{ invalid json", encoding="utf-8")

    assert_failed_with(package, "MANIFEST_JSON_INVALID")


def test_required_field_missing_reports_field_scope(tmp_path: Path) -> None:
    package = copy_fixture("valid-v1", tmp_path)
    manifest = load_manifest(package)
    del manifest["packageId"]
    write_manifest(package, manifest)

    payload = assert_failed_with(package, "MANIFEST_REQUIRED_FIELD_MISSING")

    assert any(error["scope"] == "packageId" for error in payload["errors"])


def test_missing_schema_version_reports_field_scope(tmp_path: Path) -> None:
    package = copy_fixture("valid-v1", tmp_path)
    manifest = load_manifest(package)
    del manifest["schemaVersion"]
    write_manifest(package, manifest)

    payload = assert_failed_with(package, "MANIFEST_REQUIRED_FIELD_MISSING")

    assert any(error["scope"] == "schemaVersion" for error in payload["errors"])


def test_invalid_package_type_reports_supported_values(tmp_path: Path) -> None:
    package = copy_fixture("valid-v1", tmp_path)
    manifest = load_manifest(package)
    manifest["packageType"] = "backup-restore-resource-delta"
    write_manifest(package, manifest)

    payload = assert_failed_with(package, "MANIFEST_PACKAGE_TYPE_INVALID")

    assert "full-release" in payload["errors"][0]["nextStep"]


def test_smart_release_requires_baseline_manifest_id(tmp_path: Path) -> None:
    package = copy_fixture("valid-v1", tmp_path)
    manifest = load_manifest(package)
    manifest["packageType"] = "smart-release"
    manifest.pop("baselineManifestId", None)
    write_manifest(package, manifest)

    assert_failed_with(package, "MANIFEST_SMART_BASELINE_MISSING")


def test_package_undeclared_file_reports_file_path(tmp_path: Path) -> None:
    package = copy_fixture("valid-v1", tmp_path)
    (package / "unexpected.sql").write_text("select 1;\n", encoding="utf-8")

    payload = assert_failed_with(package, "PACKAGE_UNDECLARED_FILE")

    assert any(error["path"].endswith("unexpected.sql") for error in payload["errors"])


def test_declared_file_missing_reports_manifest_path(tmp_path: Path) -> None:
    package = copy_fixture("valid-v1", tmp_path)
    (package / "backend" / "app.txt").unlink()

    payload = assert_failed_with(package, "PACKAGE_DECLARED_FILE_MISSING")

    assert any(error["scope"] == "artifacts[0].path" for error in payload["errors"])


def test_sha256_mismatch_reports_declared_artifact(tmp_path: Path) -> None:
    package = copy_fixture("valid-v1", tmp_path)
    (package / "backend" / "app.txt").write_text("changed artifact\n", encoding="utf-8")

    payload = assert_failed_with(package, "PACKAGE_FILE_SHA256_MISMATCH")

    assert any(error["scope"] == "artifacts[0].sha256" for error in payload["errors"])


def test_hardcoded_target_ip_reports_forbidden_ip(tmp_path: Path) -> None:
    package = copy_fixture("valid-v1", tmp_path)
    manifest = load_manifest(package)
    manifest["targetRequirements"]["operatorNote"] = "deploy target 172.30.30.58"
    write_manifest(package, manifest)

    payload = assert_failed_with(package, "FORBIDDEN_HARDCODED_TARGET_IP")

    assert "172.30.30.58" in payload["errors"][0]["message"]


def test_deploy_contract_hardcoded_target_ip_reports_forbidden_ip(tmp_path: Path) -> None:
    package = copy_fixture("valid-v1", tmp_path)
    manifest = load_manifest(package)
    manifest["deployContract"]["targetHost"] = "172.30.30.58"
    write_manifest(package, manifest)

    payload = assert_failed_with(package, "FORBIDDEN_HARDCODED_TARGET_IP")

    assert any("172.30.30.58" in error["message"] for error in payload["errors"])


def test_local_resource_reference_ip_does_not_fail_target_ip_scan(tmp_path: Path) -> None:
    package = copy_fixture("valid-v1", tmp_path)
    resource_manifest = package / "resources" / "resource-reference-manifest.json"
    resource_manifest.write_text(
        json.dumps(
            {
                "resources": [
                    {
                        "storageProfileId": "minio-yudao-default",
                        "url": "http://127.0.0.1:9000/yudao/showroom/demo.png",
                        "objectKey": "showroom/demo.png",
                    }
                ]
            },
            ensure_ascii=False,
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )

    payload, result = run_validator(package)

    assert_payload_contract(payload)
    assert result.returncode == 0
    assert payload["status"] == "passed"
    assert not any(
        error["code"] == "FORBIDDEN_HARDCODED_TARGET_IP"
        for error in payload["errors"]
    )


def test_secret_pattern_reports_declared_file(tmp_path: Path) -> None:
    package = copy_fixture("valid-v1", tmp_path)
    add_declared_artifact(package, "config/runtime.env", "DB_PASSWORD=super-secret\n")

    payload = assert_failed_with(package, "FORBIDDEN_SECRET_PATTERN")

    assert any(error["path"].endswith("config/runtime.env") for error in payload["errors"])


def test_legacy_v0_manifest_reports_warning_without_failure(tmp_path: Path) -> None:
    package = copy_fixture("legacy-v0", tmp_path)

    payload, result = run_validator(package)

    assert_payload_contract(payload)
    assert result.returncode == 0
    assert payload["status"] == "warning"
    assert payload["code"] == "LEGACY_MANIFEST_V0"
    assert payload["errors"] == []
    assert any(warning["code"] == "LEGACY_MANIFEST_V0" for warning in payload["warnings"])


def test_valid_v1_manifest_passes_with_file_checks(tmp_path: Path) -> None:
    package = copy_fixture("valid-v1", tmp_path)

    payload, result = run_validator(package)

    assert_payload_contract(payload)
    assert result.returncode == 0
    assert payload["status"] == "passed"
    assert payload["mode"] == "report-only"
    assert payload["code"] == "MANIFEST_VALIDATION_PASSED"
    assert payload["errors"] == []
    assert payload["warnings"] == []
    assert {check["status"] for check in payload["checks"]} == {"passed"}


def test_report_only_output_path_writes_structured_json(tmp_path: Path) -> None:
    package = copy_fixture("valid-v1", tmp_path)
    output_path = tmp_path / "manifest-validation-result.json"

    payload, result = run_validator(package, output_path=output_path)

    assert_payload_contract(payload)
    assert result.returncode == 0
    assert payload["status"] == "passed"
    assert payload["mode"] == "report-only"
    assert payload["code"] == "MANIFEST_VALIDATION_PASSED"
    assert "MANIFEST_VALIDATION_PASSED" in result.stdout


def test_schema_documents_phase1_manifest_contract() -> None:
    schema = json.loads(SCHEMA.read_text(encoding="utf-8"))

    assert "packageId" in schema["required"]
    assert "releaseTag" in schema["required"]
    assert "schemaVersion" in schema["required"]
    assert "schemaDigest" in schema["required"]
    assert "migrationPlan" in schema["required"]
    assert "requiredSql" in schema["required"]
    assert "buildModules" in schema["required"]
    assert "compatibilityMatrix" in schema["required"]
    assert "operationEvidencePolicy" in schema["required"]
    assert "targetRequirements" in schema["required"]
    artifact_required = set(schema["properties"]["artifacts"]["items"]["required"])
    assert {
        "artifactHash",
        "sourceHash",
        "dependencyHash",
        "buildParameterHash",
        "contractHash",
    } <= artifact_required
    assert set(schema["properties"]["packageType"]["enum"]) == {
        "full-release",
        "smart-release",
        "data-release",
        "resource-check-only",
    }
    assert "backup-restore-resource-delta" not in schema["properties"]["packageType"]["enum"]
    assert schema["properties"]["buildContract"]["properties"]["mode"]["const"] == "report-only"
    assert schema["properties"]["buildContract"]["properties"]["changesBuildExitCode"]["const"] is False
    assert schema["properties"]["deployContract"]["properties"]["mode"]["const"] == "report-only"
    assert schema["properties"]["deployContract"]["properties"]["changesDeployExitCode"]["const"] is False
    assert schema["properties"]["deployContract"]["properties"]["targetConfigSource"]["const"] == "server-side-runtime-control-config"
    assert set(
        schema["properties"]["targetRequirements"]["properties"]["environmentCodes"]["items"]["enum"]
    ) == {"test", "prod", "backup"}
    assert any(
        "baselineManifestId" in rule.get("then", {}).get("required", [])
        for rule in schema.get("allOf", [])
    )


def test_validator_scripts_do_not_hardcode_server_ips() -> None:
    forbidden_ips = ("172.30.30.57", "172.30.30.58", "172.30.30.59")

    for path in (VALIDATOR, VALIDATOR_MODULE):
        text = path.read_text(encoding="utf-8")
        for ip in forbidden_ips:
            assert ip not in text, f"{path} must not hardcode target server IP {ip}"
