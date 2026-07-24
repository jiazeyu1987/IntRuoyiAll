import json
import os
import re
import subprocess
import textwrap
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
FIXTURE_ROOT = Path(__file__).resolve().parent / "fixtures" / "release-intake"
INTAKE_SCRIPT = REPO_ROOT / "script" / "release" / "run-release-intake.ps1"


def _run_intake(
    output_dir: Path,
    *,
    registry_path: Path | None = None,
    include_db_config: bool = True,
) -> subprocess.CompletedProcess[str]:
    args = [
        "powershell.exe",
        "-NoProfile",
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        str(INTAKE_SCRIPT),
        "-RepoRoot",
        str(REPO_ROOT),
        "-BaselineManifestPath",
        str(FIXTURE_ROOT / "baseline-manifest.json"),
        "-DataOwnershipRegistryPath",
        str(registry_path or FIXTURE_ROOT / "data-ownership-registry.valid.json"),
        "-OutputDir",
        str(output_dir),
        "-Mode",
        "report-only",
        "-LocalSchemaFingerprintPath",
        str(FIXTURE_ROOT / "local-schema-added-column.json"),
        "-LocalDataChangeRowsPath",
        str(FIXTURE_ROOT / "local-data-changes.json"),
        "-ResourceRowsPath",
        str(FIXTURE_ROOT / "resource-rows.json"),
    ]
    if include_db_config:
        insert_at = args.index("-DataOwnershipRegistryPath")
        args[insert_at:insert_at] = [
            "-LocalDatabaseConfigPath",
            str(FIXTURE_ROOT / "db-config.fixture.json"),
        ]

    return subprocess.run(
        args,
        cwd=REPO_ROOT,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        check=False,
    )


def _run_live_intake(
    output_dir: Path,
    *,
    db_config_path: Path,
    env: dict[str, str] | None = None,
    extra_args: list[str] | None = None,
) -> subprocess.CompletedProcess[str]:
    args = [
        "powershell.exe",
        "-NoProfile",
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        str(INTAKE_SCRIPT),
        "-RepoRoot",
        str(REPO_ROOT),
        "-BaselineManifestPath",
        str(FIXTURE_ROOT / "baseline-manifest.json"),
        "-LocalDatabaseConfigPath",
        str(db_config_path),
        "-DataOwnershipRegistryPath",
        str(FIXTURE_ROOT / "data-ownership-registry.valid.json"),
        "-OutputDir",
        str(output_dir),
        "-Mode",
        "report-only",
    ]
    if extra_args:
        args.extend(extra_args)

    return subprocess.run(
        args,
        cwd=REPO_ROOT,
        env=env or os.environ.copy(),
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        check=False,
    )


def _read_json(path: Path) -> dict[str, object]:
    return json.loads(path.read_text(encoding="utf-8"))


def _write_fake_docker_runner(path: Path) -> None:
    python_runner = path.with_suffix(".py")
    python_runner.write_text(
        textwrap.dedent(
            r"""
            import json
            import os
            import re
            import sys

            docker_args = sys.argv[1:]
            stdin_text = sys.stdin.read()
            with open(os.environ["FAKE_DOCKER_LEDGER"], "a", encoding="utf-8") as ledger:
                ledger.write(json.dumps({"args": docker_args, "stdin": stdin_text}, ensure_ascii=False) + "\n")

            if re.search(r"(?i)\b(ssh|scp)\b|\b(?:\d{1,3}\.){3}\d{1,3}\b", " ".join(docker_args)):
                sys.exit(51)
            if re.search(r"(?i)\b(DROP|INSERT|UPDATE|DELETE)\b", stdin_text):
                sys.exit(52)

            if re.search(r"information_schema\.tables", stdin_text):
                print("infra_file\tBASE TABLE")
                print("system_menu\tBASE TABLE")
                sys.exit(0)
            if re.search(r"information_schema\.columns", stdin_text):
                print("infra_file\tconfig_id\tbigint\tNO\t\\N\t1")
                print("infra_file\tpath\tvarchar(512)\tNO\t\\N\t2")
                print("infra_file\turl\tvarchar(1024)\tNO\t\\N\t3")
                print("infra_file\ttype\tvarchar(128)\tYES\t\\N\t4")
                print("infra_file\tname\tvarchar(255)\tYES\t\\N\t5")
                print("system_menu\tid\tbigint\tNO\t\\N\t1")
                print("system_menu\tpermission\tvarchar(100)\tYES\t\\N\t2")
                sys.exit(0)
            if re.search(r"information_schema\.statistics", stdin_text):
                sys.exit(0)
            if re.search(r"information_schema\.views", stdin_text):
                sys.exit(0)
            if re.search(r"\binfra_file\b", stdin_text):
                print("28\tshowroom/example.png\thttp://127.0.0.1:9000/yudao/showroom/example.png\timage/png\texample.png")
                sys.exit(0)

            print(f"Unexpected SQL: {stdin_text}", file=sys.stderr)
            sys.exit(53)
            """
        ).strip()
        + "\n",
        encoding="utf-8",
    )
    path.write_text(
        f"@echo off\r\npython \"{python_runner}\" %*\r\n",
        encoding="utf-8",
    )


def _read_ledger(path: Path) -> list[dict[str, object]]:
    return [
        json.loads(line)
        for line in path.read_text(encoding="utf-8-sig").splitlines()
        if line.strip()
    ]


def test_release_intake_reports_added_column(tmp_path: Path) -> None:
    output_dir = tmp_path / "intake"

    result = _run_intake(output_dir)

    assert result.returncode == 0, result.stdout + result.stderr
    assert sorted(path.name for path in output_dir.iterdir()) == [
        "change-set.json",
        "data-change-manifest.json",
        "intake-result.json",
        "local-schema-fingerprint.json",
        "resource-reference-manifest.json",
        "schema-change-report.json",
    ]
    schema_report = _read_json(output_dir / "schema-change-report.json")
    assert schema_report["status"] == "warning"
    assert schema_report["schemaDriftCount"] == 1
    assert schema_report["changes"] == [
        {
            "changeType": "added-column",
            "objectType": "column",
            "tableName": "system_menu",
            "columnName": "icon",
            "currentDefinitionHash": "sha256:menu-icon-local",
            "baselineDefinitionHash": None,
            "migrationBinding": None,
            "blockingCandidate": True,
            "message": "Column exists locally but no migration binding was found.",
        }
    ]

    intake_result = _read_json(output_dir / "intake-result.json")
    assert intake_result["status"] == "warning"
    assert intake_result["mode"] == "report-only"
    assert intake_result["schemaDriftCount"] == 1
    assert intake_result["blockingCandidateCount"] >= 1
    assert intake_result["reports"]["schemaChangeReport"] == "schema-change-report.json"


def test_release_intake_classifies_registry_owned_data(tmp_path: Path) -> None:
    output_dir = tmp_path / "intake"

    result = _run_intake(output_dir)

    assert result.returncode == 0, result.stdout + result.stderr
    data_manifest = _read_json(output_dir / "data-change-manifest.json")
    required = [
        change
        for change in data_manifest["changes"]
        if change["classification"] == "required-data"
    ]
    assert required == [
        {
            "classification": "required-data",
            "registryEntryId": "system-menu",
            "tableName": "system_menu",
            "naturalKeyValue": {"permission": "system:user:list"},
            "changedFields": ["name", "status"],
            "tenantScope": "all-tenants",
            "migrationBinding": None,
            "blockingCandidate": True,
        }
    ]


def test_release_intake_marks_unowned_data_change(tmp_path: Path) -> None:
    output_dir = tmp_path / "intake"

    result = _run_intake(output_dir)

    assert result.returncode == 0, result.stdout + result.stderr
    data_manifest = _read_json(output_dir / "data-change-manifest.json")
    unclassified = [
        change
        for change in data_manifest["changes"]
        if change["classification"] == "unclassified-local-change"
    ]
    assert unclassified == [
        {
            "classification": "unclassified-local-change",
            "tableName": "dcc_controlled_file",
            "reason": "No registry entry owns this table.",
            "blockingCandidate": True,
        }
    ]

    intake_result = _read_json(output_dir / "intake-result.json")
    assert intake_result["requiredDataChangeCount"] == 1
    assert intake_result["unclassifiedLocalChangeCount"] == 1


def test_release_intake_outputs_resource_reference_manifest(tmp_path: Path) -> None:
    output_dir = tmp_path / "intake"

    result = _run_intake(output_dir)

    assert result.returncode == 0, result.stdout + result.stderr
    resource_manifest = _read_json(output_dir / "resource-reference-manifest.json")
    assert resource_manifest["manifestVersion"] == "1.0"
    assert resource_manifest["references"] == [
        {
            "sourceTable": "infra_file",
            "sourceColumn": "url",
            "rowBusinessKey": "config_id=28,path=showroom/example.png",
            "tenantCode": None,
            "fileConfigIdReadback": 28,
            "storageProfileId": "minio-yudao-default",
            "bucket": "yudao",
            "objectKey": "showroom/example.png",
            "urlDomain": "http://127.0.0.1:9000/yudao",
            "expectedDomainPolicy": "target-profile-domain",
            "size": None,
            "sha256": None,
            "contentType": None,
            "requiredForRelease": True,
            "resourcePreparedStatus": "unknown",
        }
    ]
    intake_result = _read_json(output_dir / "intake-result.json")
    assert intake_result["resourceReferenceCount"] == 1


def test_release_intake_rejects_invalid_registry(tmp_path: Path) -> None:
    output_dir = tmp_path / "intake"

    result = _run_intake(
        output_dir,
        registry_path=FIXTURE_ROOT / "data-ownership-registry.invalid.json",
    )

    assert result.returncode == 2
    intake_result = _read_json(output_dir / "intake-result.json")
    assert intake_result["status"] == "failed"
    assert intake_result["errors"][0]["code"] == "INTAKE_DATA_REGISTRY_INVALID"
    assert intake_result["errors"][0]["details"] == [
        {
            "code": "DATA_REGISTRY_TENANT_SCOPE_MISSING",
            "entryId": "system-menu",
            "message": "Registry entry is missing tenantScope.",
        },
        {
            "code": "DATA_REGISTRY_FIELD_OWNERSHIP_CONFLICT",
            "entryId": "system-menu",
            "field": "name",
            "message": "Field cannot be both owned and forbidden.",
        },
    ]


def test_release_intake_missing_local_db_config_fails_fast(tmp_path: Path) -> None:
    output_dir = tmp_path / "intake"

    result = _run_intake(output_dir, include_db_config=False)

    assert result.returncode == 2
    intake_result = _read_json(output_dir / "intake-result.json")
    assert intake_result["status"] == "failed"
    assert intake_result["errors"] == [
        {
            "code": "INTAKE_DB_CONFIG_MISSING",
            "message": "LocalDatabaseConfigPath is required for release intake.",
        }
    ]
    assert intake_result["schemaDriftCount"] == 0
    assert intake_result["requiredDataChangeCount"] == 0
    assert intake_result["resourceReferenceCount"] == 0


def test_release_intake_live_mode_requires_docker_container(tmp_path: Path) -> None:
    output_dir = tmp_path / "intake"
    env = os.environ.copy()
    env["INTRUOYI_INTAKE_TEST_USER"] = "readonly"
    env["INTRUOYI_INTAKE_TEST_PASSWORD"] = "secret"

    result = _run_live_intake(
        output_dir,
        db_config_path=FIXTURE_ROOT / "db-config.live-missing-container.json",
        env=env,
    )

    assert result.returncode == 2
    intake_result = _read_json(output_dir / "intake-result.json")
    assert intake_result["status"] == "failed"
    assert intake_result["errors"] == [
        {
            "code": "INTAKE_DB_DOCKER_CONTAINER_MISSING",
            "message": "Local readonly docker mysql mode requires dockerContainer in LocalDatabaseConfigPath.",
        }
    ]


def test_release_intake_live_mode_requires_credential_env(tmp_path: Path) -> None:
    output_dir = tmp_path / "intake"
    env = os.environ.copy()
    env.pop("INTRUOYI_INTAKE_TEST_USER", None)
    env.pop("INTRUOYI_INTAKE_TEST_PASSWORD", None)

    result = _run_live_intake(
        output_dir,
        db_config_path=FIXTURE_ROOT / "db-config.live.json",
        env=env,
    )

    assert result.returncode == 2
    intake_result = _read_json(output_dir / "intake-result.json")
    assert intake_result["status"] == "failed"
    assert intake_result["errors"][0]["code"] == "INTAKE_DB_CREDENTIAL_ENV_MISSING"
    assert intake_result["errors"][0]["details"] == [
        {
            "envName": "INTRUOYI_INTAKE_TEST_USER",
            "message": "Required database credential environment variable is missing.",
        },
        {
            "envName": "INTRUOYI_INTAKE_TEST_PASSWORD",
            "message": "Required database credential environment variable is missing.",
        },
    ]


def test_release_intake_live_mode_uses_readonly_docker_mysql_queries(tmp_path: Path) -> None:
    output_dir = tmp_path / "intake"
    fake_docker = tmp_path / "fake-docker.cmd"
    ledger_path = tmp_path / "docker-ledger.jsonl"
    _write_fake_docker_runner(fake_docker)
    env = os.environ.copy()
    env["INTRUOYI_INTAKE_TEST_USER"] = "readonly"
    env["INTRUOYI_INTAKE_TEST_PASSWORD"] = "secret"
    env["FAKE_DOCKER_LEDGER"] = str(ledger_path)

    result = _run_live_intake(
        output_dir,
        db_config_path=FIXTURE_ROOT / "db-config.live.json",
        env=env,
        extra_args=["-DockerCliPath", str(fake_docker)],
    )

    assert result.returncode == 0, result.stdout + result.stderr
    ledger = _read_ledger(ledger_path)
    ledger_text = "\n".join(
        " ".join(entry["args"]) + "\n" + entry["stdin"]
        for entry in ledger
    )
    assert "information_schema.tables" in ledger_text
    assert "information_schema.columns" in ledger_text
    assert "information_schema.statistics" in ledger_text
    assert "information_schema.views" in ledger_text
    assert "FROM infra_file" in ledger_text
    assert not re.search(r"\b(DROP|INSERT|UPDATE|DELETE)\b", ledger_text, re.IGNORECASE)
    assert not re.search(r"\b(ssh|scp)\b|\b(?:\d{1,3}\.){3}\d{1,3}\b", ledger_text, re.IGNORECASE)

    schema_fingerprint = _read_json(output_dir / "local-schema-fingerprint.json")
    assert schema_fingerprint["captureSource"] == "local-docker-mysql-readonly"
    assert schema_fingerprint["dockerContainer"] == "int-ruoyi-mysql"
    assert {table["tableName"] for table in schema_fingerprint["tables"]} == {
        "infra_file",
        "system_menu",
    }

    data_manifest = _read_json(output_dir / "data-change-manifest.json")
    assert data_manifest["liveDataChangeMode"] == "not-yet-bound-to-baseline"
    assert data_manifest["changes"] == []
    assert data_manifest["registryCoverage"][0]["entryId"] == "system-menu"

    resource_manifest = _read_json(output_dir / "resource-reference-manifest.json")
    assert resource_manifest["references"][0]["bucket"] == "yudao"
    assert resource_manifest["references"][0]["objectKey"] == "showroom/example.png"
    assert resource_manifest["references"][0]["urlDomain"] == "http://127.0.0.1:9000/yudao"

    intake_result = _read_json(output_dir / "intake-result.json")
    assert intake_result["status"] == "warning"
    assert intake_result["requiredDataChangeCount"] == 0
    assert intake_result["unclassifiedLocalChangeCount"] == 0
    assert intake_result["resourceReferenceCount"] == 1
    assert intake_result["warnings"] == [
        {
            "code": "INTAKE_DATA_BASELINE_NOT_BOUND",
            "message": "Live registry data change extraction is not yet bound to a baseline data snapshot.",
        }
    ]


def test_release_intake_live_mode_uses_generic_remote_reference_guard() -> None:
    module_text = (REPO_ROOT / "script" / "release" / "lib" / "ReleaseIntake.psm1").read_text(encoding="utf-8")

    assert "Test-ReleaseIntakeForbiddenRemoteReference" in module_text
    assert "172.30.30." not in module_text
    assert r"(?:\d{1,3}\.){3}\d{1,3}" in module_text
