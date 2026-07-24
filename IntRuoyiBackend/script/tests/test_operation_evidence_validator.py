import json
import subprocess
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
EVIDENCE_SCRIPT = REPO_ROOT / "script" / "release" / "run-operation-evidence-validation.ps1"


def _write_json(path: Path, value: dict[str, object]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def _read_json(path: Path) -> dict[str, object]:
    return json.loads(path.read_text(encoding="utf-8"))


def _log(tmp_path: Path, name: str) -> str:
    path = tmp_path / "logs" / f"{name}.log"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(f"{name} evidence\n", encoding="utf-8")
    return str(path)


def _operation(tmp_path: Path, operation_id: str, status: str = "success", **overrides: object) -> dict[str, object]:
    operation: dict[str, object] = {
        "id": operation_id,
        "status": status,
        "required": True,
        "logPath": _log(tmp_path, operation_id),
    }
    operation.update(overrides)
    return operation


def _valid_publish_evidence(tmp_path: Path, **overrides: object) -> dict[str, object]:
    evidence: dict[str, object] = {
        "operationId": "op-publish-20260609",
        "gate": "publish",
        "targetEnvironment": "test",
        "operations": [
            _operation(tmp_path, "validate-release-manifest"),
            _operation(tmp_path, "schema-preflight"),
            _operation(tmp_path, "deploy-backend"),
            _operation(tmp_path, "deploy-admin-frontend"),
            _operation(tmp_path, "health-check"),
        ],
        "finalHealthCheck": {
            "status": "success",
            "logPath": _log(tmp_path, "final-health-check"),
        },
    }
    evidence.update(overrides)
    return evidence


def _run_validator(
    tmp_path: Path,
    evidence: dict[str, object],
    *,
    gate: str = "publish",
) -> tuple[dict[str, object], subprocess.CompletedProcess[str]]:
    evidence_path = tmp_path / f"{gate}-operation-evidence.json"
    output_path = tmp_path / f"{gate}-operation-evidence-result.json"
    _write_json(evidence_path, evidence)

    result = subprocess.run(
        [
            "powershell.exe",
            "-NoProfile",
            "-ExecutionPolicy",
            "Bypass",
            "-File",
            str(EVIDENCE_SCRIPT),
            "-OperationEvidencePath",
            str(evidence_path),
            "-Gate",
            gate,
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
    assert output_path.exists(), result.stderr + result.stdout
    return _read_json(output_path), result


def _assert_code(payload: dict[str, object], code: str) -> None:
    errors = payload.get("errors", [])
    assert isinstance(errors, list)
    assert any(item.get("code") == code for item in errors), payload


def test_failed_required_operation_is_not_masked_by_successful_health_check(tmp_path: Path) -> None:
    evidence = _valid_publish_evidence(tmp_path)
    evidence["operations"][1] = _operation(
        tmp_path,
        "schema-preflight",
        "failed",
        failureCode="missing_field",
        failedStage="schema-preflight",
        impact="backend would start with a missing field",
        requiredResolution="run the forward schema migration and rerun preflight",
    )

    payload, result = _run_validator(tmp_path, evidence)

    assert result.returncode == 2, result.stderr + result.stdout
    assert payload["status"] == "blocked"
    _assert_code(payload, "REQUIRED_OPERATION_FAILED")
    assert payload["finalHealthStatus"] == "success"


def test_complete_publish_operation_evidence_passes(tmp_path: Path) -> None:
    payload, result = _run_validator(tmp_path, _valid_publish_evidence(tmp_path))

    assert result.returncode == 0, result.stderr + result.stdout
    assert payload["status"] == "pass"
    assert payload["errors"] == []


def test_missing_required_operation_log_path_blocks(tmp_path: Path) -> None:
    evidence = _valid_publish_evidence(tmp_path)
    evidence["operations"][2].pop("logPath")

    payload, result = _run_validator(tmp_path, evidence)

    assert result.returncode == 2, result.stderr + result.stdout
    assert payload["status"] == "blocked"
    _assert_code(payload, "EVIDENCE_LOG_MISSING")


def test_failed_operation_requires_failure_reason_fields(tmp_path: Path) -> None:
    evidence = _valid_publish_evidence(tmp_path)
    evidence["operations"][1] = _operation(tmp_path, "schema-preflight", "failed")

    payload, result = _run_validator(tmp_path, evidence)

    assert result.returncode == 2, result.stderr + result.stdout
    assert payload["status"] == "blocked"
    _assert_code(payload, "FAILURE_REASON_MISSING")


def test_missing_required_operation_blocks(tmp_path: Path) -> None:
    evidence = _valid_publish_evidence(tmp_path)
    evidence["operations"] = [
        operation for operation in evidence["operations"] if operation["id"] != "schema-preflight"
    ]

    payload, result = _run_validator(tmp_path, evidence)

    assert result.returncode == 2, result.stderr + result.stdout
    assert payload["status"] == "blocked"
    _assert_code(payload, "REQUIRED_OPERATION_MISSING")


def test_prod_target_requires_explicit_authorization_evidence(tmp_path: Path) -> None:
    evidence = _valid_publish_evidence(tmp_path, targetEnvironment="prod")

    payload, result = _run_validator(tmp_path, evidence)

    assert result.returncode == 2, result.stderr + result.stdout
    assert payload["status"] == "blocked"
    _assert_code(payload, "PROD_ACCESS_NOT_AUTHORIZED")
