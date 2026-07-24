from pathlib import Path
import json
import subprocess
import tempfile


def _script_path() -> Path:
    return (
        Path(__file__).resolve().parents[1]
        / "release-readiness"
        / "verify-g6-g7-playwright.ps1"
    )


def _run_tool(*args: str) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        [
            "powershell",
            "-NoProfile",
            "-ExecutionPolicy",
            "Bypass",
            "-File",
            str(_script_path()),
            *args,
        ],
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        check=False,
    )


def _write_evidence_file(root: Path, name: str, content: str = "real evidence") -> str:
    path = root / name
    path.write_text(content, encoding="utf-8")
    return str(path)


def _valid_evidence(root: Path) -> dict[str, object]:
    return {
        "prod": {
            "frontendUrl": "http://172.30.30.57:8081/login?redirect=/index",
            "backendUrl": "http://172.30.30.57:48081",
            "playwrightLogin": {
                "status": "passed",
                "sessionName": "int-ruoyi-prod-g6",
                "usedFrontendPath": True,
                "landedPath": "/index",
                "visibleUserText": "瑛泰管理员",
                "evidencePath": _write_evidence_file(root, "prod-login.txt"),
            },
            "sampleFileFrontendPath": {
                "status": "passed",
                "usedFrontendPath": True,
                "fileConfigId": 28,
                "objectPath": "dcc/stamped/20260513/dcc-sample_controlled.pdf",
                "httpStatus": 200,
                "contentType": "application/pdf;charset=UTF-8",
                "evidencePath": _write_evidence_file(root, "prod-sample.txt"),
            },
            "frontendBackendTarget": {
                "observedTargets": ["http://172.30.30.57:48081"],
                "forbiddenTargets": ["http://172.30.30.58:48081"],
                "evidencePath": _write_evidence_file(root, "prod-target.txt"),
            },
        },
        "test": {
            "frontendUrl": "http://172.30.30.58:8081/login?redirect=/index",
            "backendUrl": "http://172.30.30.58:48081",
            "playwrightLogin": {
                "status": "passed",
                "sessionName": "int-ruoyi-test-g6",
                "usedFrontendPath": True,
                "landedPath": "/index",
                "visibleUserText": "芋道1",
                "evidencePath": _write_evidence_file(root, "test-login.txt"),
            },
            "sampleFileFrontendPath": {
                "status": "passed",
                "usedFrontendPath": True,
                "fileConfigId": 28,
                "objectPath": "dcc/stamped/20260513/dcc-sample_controlled.pdf",
                "httpStatus": 200,
                "contentType": "application/pdf;charset=UTF-8",
                "evidencePath": _write_evidence_file(root, "test-sample.txt"),
            },
        },
    }


def test_missing_evidence_blocks_instead_of_default_success() -> None:
    result = _run_tool()

    assert result.returncode == 2
    assert "BLOCKED" in result.stdout
    assert "EvidencePath" in result.stdout or "EvidenceJson" in result.stdout
    assert "PASS" not in result.stdout


def test_prod_frontend_target_must_not_point_to_test_backend() -> None:
    with tempfile.TemporaryDirectory() as temp_dir:
        evidence = _valid_evidence(Path(temp_dir))
        evidence["prod"]["frontendBackendTarget"]["observedTargets"] = [
            "http://172.30.30.58:48081"
        ]
        evidence_path = Path(temp_dir) / "g6-g7-evidence.json"
        evidence_path.write_text(
            json.dumps(evidence, ensure_ascii=False, indent=2),
            encoding="utf-8",
        )

        result = _run_tool("-EvidencePath", str(evidence_path))

    assert result.returncode == 2
    assert "BLOCKED" in result.stdout
    assert "172.30.30.58:48081" in result.stdout
    assert "production frontend backend target" in result.stdout


def test_direct_url_or_api_shortcut_sample_file_evidence_blocks() -> None:
    with tempfile.TemporaryDirectory() as temp_dir:
        evidence = _valid_evidence(Path(temp_dir))
        evidence["prod"]["sampleFileFrontendPath"]["usedFrontendPath"] = False
        evidence["prod"]["sampleFileFrontendPath"]["method"] = "direct backend URL"
        evidence_path = Path(temp_dir) / "g6-g7-evidence.json"
        evidence_path.write_text(
            json.dumps(evidence, ensure_ascii=False, indent=2),
            encoding="utf-8",
        )

        result = _run_tool("-EvidencePath", str(evidence_path))

    assert result.returncode == 2
    assert "BLOCKED" in result.stdout
    assert "frontend path" in result.stdout
    assert "direct backend URL" in result.stdout


def test_complete_real_frontend_evidence_passes_without_running_release() -> None:
    with tempfile.TemporaryDirectory() as temp_dir:
        evidence = _valid_evidence(Path(temp_dir))
        evidence_path = Path(temp_dir) / "g6-g7-evidence.json"
        evidence_path.write_text(
            json.dumps(evidence, ensure_ascii=False, indent=2),
            encoding="utf-8",
        )

        result = _run_tool("-EvidencePath", str(evidence_path))

    assert result.returncode == 0, result.stdout + result.stderr
    assert "PASS" in result.stdout
    assert "BLOCKED" not in result.stdout
    assert "docker compose up" not in _script_path().read_text(encoding="utf-8")
    assert "publish-int-ruoyi" not in _script_path().read_text(encoding="utf-8")
