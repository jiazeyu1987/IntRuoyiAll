import json
import subprocess
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
PLANNER_SCRIPT = REPO_ROOT / "script" / "release" / "run-resource-delta-plan.ps1"


def _sha(seed: int) -> str:
    return f"sha256:{seed:064x}"


def _resource_ref(
    object_key: str,
    *,
    sha256: str,
    size: int = 1024,
    bucket: str = "yudao",
    storage_profile_id: str = "minio-yudao-default",
) -> dict[str, object]:
    return {
        "sourceTable": "infra_file",
        "sourceColumn": "url",
        "rowBusinessKey": f"config_id=28,path={object_key}",
        "tenantCode": "test",
        "fileConfigIdReadback": 28,
        "storageProfileId": storage_profile_id,
        "bucket": bucket,
        "objectKey": object_key,
        "urlDomain": "http://127.0.0.1:9000/yudao",
        "expectedDomainPolicy": "target-profile-domain",
        "size": size,
        "sha256": sha256,
        "contentType": "application/octet-stream",
        "requiredForRelease": True,
        "resourcePreparedStatus": "unknown",
    }


def _target_object(
    object_key: str,
    *,
    sha256: str,
    size: int = 1024,
    bucket: str = "yudao",
    storage_profile_id: str = "minio-yudao-default",
) -> dict[str, object]:
    return {
        "storageProfileId": storage_profile_id,
        "bucket": bucket,
        "objectKey": object_key,
        "size": size,
        "sha256": sha256,
        "indexedAt": "2026-06-06T00:00:00Z",
    }


def _write_json(path: Path, value: dict[str, object]) -> None:
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2), encoding="utf-8")


def _write_source_manifest(path: Path, references: list[dict[str, object]]) -> None:
    _write_json(
        path,
        {
            "manifestVersion": "1.0",
            "capturedAt": "2026-06-06T00:00:00Z",
            "referenceSetHash": "sha256:source-reference-set",
            "references": references,
        },
    )


def _write_target_index(path: Path, objects: list[dict[str, object]]) -> None:
    _write_json(
        path,
        {
            "schemaVersion": "1.0",
            "indexId": "target-index-test",
            "environmentCode": "test",
            "storageProfileId": "minio-yudao-default",
            "indexedAt": "2026-06-06T00:00:00Z",
            "objects": objects,
        },
    )


def _run_planner(source_manifest: Path, target_index: Path, output_path: Path) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        [
            "powershell.exe",
            "-NoProfile",
            "-ExecutionPolicy",
            "Bypass",
            "-File",
            str(PLANNER_SCRIPT),
            "-SourceResourceReferenceManifestPath",
            str(source_manifest),
            "-TargetResourceIndexPath",
            str(target_index),
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


def test_resource_delta_planner_copies_only_three_added_objects_from_10000_existing(tmp_path: Path) -> None:
    existing_count = 10_000
    target_objects = [
        _target_object(f"dcc/file-{index:05d}.bin", sha256=_sha(index), size=1000 + index)
        for index in range(existing_count)
    ]
    source_refs = [
        _resource_ref(f"dcc/file-{index:05d}.bin", sha256=_sha(index), size=1000 + index)
        for index in range(existing_count)
    ]
    source_refs.extend(
        _resource_ref(f"dcc/file-{index:05d}.bin", sha256=_sha(index), size=1000 + index)
        for index in range(existing_count, existing_count + 3)
    )

    source_manifest = tmp_path / "resource-reference-manifest.json"
    target_index = tmp_path / "target-resource-index.json"
    output_path = tmp_path / "resource-delta-proof.json"
    _write_source_manifest(source_manifest, source_refs)
    _write_target_index(target_index, target_objects)

    result = _run_planner(source_manifest, target_index, output_path)

    assert result.returncode == 0, result.stderr + result.stdout
    proof = _read_json(output_path)
    assert proof["status"] == "passed"
    assert proof["mode"] == "plan-only"
    assert proof["summary"]["sourceReferenceCount"] == 10_003
    assert proof["summary"]["targetObjectCount"] == 10_000
    assert proof["summary"]["copyObjects"] == 3
    assert proof["summary"]["verifyOnlyObjects"] == 10_000
    assert proof["summary"]["conflictObjects"] == 0
    assert proof["summary"]["tombstoneObjects"] == 0
    assert [item["objectKey"] for item in proof["copyObjects"]] == [
        "dcc/file-10000.bin",
        "dcc/file-10001.bin",
        "dcc/file-10002.bin",
    ]
    serialized = json.dumps(proof, ensure_ascii=False).lower()
    assert "fullmirror" not in serialized
    assert "deleteobjects" not in proof
    assert "overwriteobjects" not in proof


def test_resource_delta_planner_fails_on_same_object_key_with_different_sha256(tmp_path: Path) -> None:
    source_manifest = tmp_path / "resource-reference-manifest.json"
    target_index = tmp_path / "target-resource-index.json"
    output_path = tmp_path / "resource-delta-proof.json"
    _write_source_manifest(
        source_manifest,
        [_resource_ref("dcc/conflict.pdf", sha256=_sha(2), size=2048)],
    )
    _write_target_index(
        target_index,
        [_target_object("dcc/conflict.pdf", sha256=_sha(1), size=2048)],
    )

    result = _run_planner(source_manifest, target_index, output_path)

    assert result.returncode == 2, result.stderr + result.stdout
    proof = _read_json(output_path)
    assert proof["status"] == "failed"
    assert proof["summary"]["conflictObjects"] == 1
    assert proof["summary"]["copyObjects"] == 0
    assert proof["summary"]["verifyOnlyObjects"] == 0
    assert proof["conflictObjects"][0]["objectKey"] == "dcc/conflict.pdf"
    assert proof["errors"][0]["code"] == "RESOURCE_DELTA_CONFLICT"


def test_resource_delta_planner_marks_target_only_objects_as_tombstone_without_delete(tmp_path: Path) -> None:
    source_manifest = tmp_path / "resource-reference-manifest.json"
    target_index = tmp_path / "target-resource-index.json"
    output_path = tmp_path / "resource-delta-proof.json"
    _write_source_manifest(
        source_manifest,
        [_resource_ref("dcc/kept.pdf", sha256=_sha(1), size=1024)],
    )
    _write_target_index(
        target_index,
        [
            _target_object("dcc/kept.pdf", sha256=_sha(1), size=1024),
            _target_object("dcc/no-longer-referenced.pdf", sha256=_sha(3), size=1024),
        ],
    )

    result = _run_planner(source_manifest, target_index, output_path)

    assert result.returncode == 0, result.stderr + result.stdout
    proof = _read_json(output_path)
    assert proof["status"] == "passed"
    assert proof["summary"]["copyObjects"] == 0
    assert proof["summary"]["verifyOnlyObjects"] == 1
    assert proof["summary"]["tombstoneObjects"] == 1
    assert proof["tombstoneObjects"][0]["objectKey"] == "dcc/no-longer-referenced.pdf"
    assert "deleteObjects" not in proof


def test_resource_delta_planner_fails_fast_when_required_reference_fields_are_missing(tmp_path: Path) -> None:
    invalid_ref = _resource_ref("dcc/missing-sha.pdf", sha256=_sha(1), size=1024)
    invalid_ref.pop("sha256")
    source_manifest = tmp_path / "resource-reference-manifest.json"
    target_index = tmp_path / "target-resource-index.json"
    output_path = tmp_path / "resource-delta-proof.json"
    _write_source_manifest(source_manifest, [invalid_ref])
    _write_target_index(target_index, [])

    result = _run_planner(source_manifest, target_index, output_path)

    assert result.returncode == 2, result.stderr + result.stdout
    proof = _read_json(output_path)
    assert proof["status"] == "failed"
    assert proof["errors"][0]["code"] == "RESOURCE_REFERENCE_FIELD_MISSING"
    assert "sha256" in proof["errors"][0]["message"]
    assert proof["errors"][0]["impact"]
    assert proof["errors"][0]["nextStep"]
