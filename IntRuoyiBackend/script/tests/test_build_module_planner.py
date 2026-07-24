import hashlib
import json
import subprocess
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
PLANNER_SCRIPT = REPO_ROOT / "script" / "release" / "run-build-module-plan.ps1"


def _sha(seed: int) -> str:
    return f"sha256:{seed:064x}"


def _write_json(path: Path, value: dict[str, object]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def _write_artifact(path: Path, content: str) -> str:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")
    return "sha256:" + hashlib.sha256(path.read_bytes()).hexdigest()


def _base_manifest(tmp_path: Path, artifact_hash: str) -> dict[str, object]:
    return {
        "moduleName": "backend",
        "requestedBuildAction": "reused",
        "releaseTag": "20260609-test",
        "artifactSourceReleaseTag": "20260608-base",
        "sourceHash": _sha(1),
        "dependencyHash": _sha(2),
        "buildParameterHash": _sha(3),
        "contractHash": _sha(4),
        "artifactHash": artifact_hash,
        "artifactPath": str(tmp_path / "cache" / "backend.jar"),
    }


def _run_planner(module_manifest: Path, output_path: Path) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        [
            "powershell.exe",
            "-NoProfile",
            "-ExecutionPolicy",
            "Bypass",
            "-File",
            str(PLANNER_SCRIPT),
            "-ModuleManifestPath",
            str(module_manifest),
            "-OutputPath",
            str(output_path),
            "-Mode",
            "plan-only",
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


def test_build_parameter_change_blocks_forced_reuse(tmp_path: Path) -> None:
    artifact_hash = _write_artifact(tmp_path / "cache" / "backend.jar", "backend artifact\n")
    manifest = _base_manifest(tmp_path, artifact_hash)
    manifest["candidate"] = {
        **{key: manifest[key] for key in ("sourceHash", "dependencyHash", "contractHash", "artifactHash", "artifactPath")},
        "buildParameterHash": _sha(99),
        "releaseTag": "20260608-base",
    }
    module_manifest = tmp_path / "module.json"
    output_path = tmp_path / "plan.json"
    _write_json(module_manifest, manifest)

    result = _run_planner(module_manifest, output_path)

    assert result.returncode == 2, result.stderr + result.stdout
    plan = _read_json(output_path)
    assert plan["status"] == "blocked"
    assert plan["buildAction"] == "invalid"
    assert plan["errors"][0]["code"] == "BUILD_MODULE_CACHE_INPUT_HASH_MISMATCH"
    assert "buildParameterHash" in plan["errors"][0]["message"]


def test_all_hashes_match_allows_reuse(tmp_path: Path) -> None:
    artifact_hash = _write_artifact(tmp_path / "cache" / "backend.jar", "backend artifact\n")
    manifest = _base_manifest(tmp_path, artifact_hash)
    manifest["candidate"] = {
        **{key: manifest[key] for key in ("sourceHash", "dependencyHash", "buildParameterHash", "contractHash", "artifactHash", "artifactPath")},
        "releaseTag": "20260608-base",
    }
    module_manifest = tmp_path / "module.json"
    output_path = tmp_path / "plan.json"
    _write_json(module_manifest, manifest)

    result = _run_planner(module_manifest, output_path)

    assert result.returncode == 0, result.stderr + result.stdout
    plan = _read_json(output_path)
    assert plan["status"] == "passed"
    assert plan["buildAction"] == "reused"
    assert plan["validation"]["hashVerified"] is True
    assert plan["artifactSourceReleaseTag"] == "20260608-base"


def test_source_dependency_or_contract_change_triggers_rebuild(tmp_path: Path) -> None:
    artifact_hash = _write_artifact(tmp_path / "cache" / "backend.jar", "backend artifact\n")
    for changed_hash in ("sourceHash", "dependencyHash", "contractHash"):
        manifest = _base_manifest(tmp_path, artifact_hash)
        manifest["requestedBuildAction"] = "auto"
        manifest["candidate"] = {
            **{
                key: manifest[key]
                for key in ("sourceHash", "dependencyHash", "buildParameterHash", "contractHash", "artifactHash", "artifactPath")
            },
            "releaseTag": "20260608-base",
        }
        manifest[changed_hash] = _sha(100)
        module_manifest = tmp_path / f"{changed_hash}.json"
        output_path = tmp_path / f"{changed_hash}-plan.json"
        _write_json(module_manifest, manifest)

        result = _run_planner(module_manifest, output_path)

        assert result.returncode == 0, result.stderr + result.stdout
        plan = _read_json(output_path)
        assert plan["status"] == "passed"
        assert plan["buildAction"] == "rebuilt"
        assert changed_hash in plan["rebuildReasons"]


def test_artifact_hash_mismatch_blocks_reuse(tmp_path: Path) -> None:
    actual_hash = _write_artifact(tmp_path / "cache" / "backend.jar", "changed backend artifact\n")
    manifest = _base_manifest(tmp_path, _sha(5))
    manifest["candidate"] = {
        **{key: manifest[key] for key in ("sourceHash", "dependencyHash", "buildParameterHash", "contractHash", "artifactPath")},
        "artifactHash": actual_hash,
        "releaseTag": "20260608-base",
    }
    module_manifest = tmp_path / "module.json"
    output_path = tmp_path / "plan.json"
    _write_json(module_manifest, manifest)

    result = _run_planner(module_manifest, output_path)

    assert result.returncode == 2, result.stderr + result.stdout
    plan = _read_json(output_path)
    assert plan["status"] == "blocked"
    assert plan["errors"][0]["code"] == "BUILD_MODULE_ARTIFACT_HASH_MISMATCH"
