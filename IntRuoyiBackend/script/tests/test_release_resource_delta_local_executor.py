import hashlib
import json
import subprocess
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
EXECUTOR_SCRIPT = REPO_ROOT / "script" / "release" / "run-resource-delta-execute-local.ps1"


def _object_path(root: Path, bucket: str, object_key: str) -> Path:
    return root / bucket / Path(object_key)


def _content(seed: int) -> bytes:
    return f"resource-delta-object-{seed}\n".encode("utf-8")


def _sha256_prefixed(content: bytes) -> str:
    return f"sha256:{hashlib.sha256(content).hexdigest()}"


def _write_object(root: Path, bucket: str, object_key: str, content: bytes) -> dict[str, object]:
    path = _object_path(root, bucket, object_key)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(content)
    return _delta_object(bucket, object_key, content)


def _delta_object(bucket: str, object_key: str, content: bytes) -> dict[str, object]:
    return {
        "storageProfileId": "minio-yudao-default",
        "bucket": bucket,
        "objectKey": object_key,
        "size": len(content),
        "sha256": _sha256_prefixed(content),
    }


def _write_json(path: Path, payload: dict[str, object]) -> None:
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def _write_plan(path: Path, *, copy_objects: list[dict[str, object]], verify_only_objects: list[dict[str, object]], tombstone_objects: list[dict[str, object]] | None = None) -> None:
    tombstones = tombstone_objects or []
    _write_json(
        path,
        {
            "schemaVersion": "1.0",
            "status": "passed",
            "mode": "plan-only",
            "plannedAt": "2026-06-06T00:00:00Z",
            "summary": {
                "sourceReferenceCount": len(copy_objects) + len(verify_only_objects),
                "targetObjectCount": len(verify_only_objects) + len(tombstones),
                "copyObjects": len(copy_objects),
                "verifyOnlyObjects": len(verify_only_objects),
                "conflictObjects": 0,
                "tombstoneObjects": len(tombstones),
            },
            "copyObjects": copy_objects,
            "verifyOnlyObjects": verify_only_objects,
            "conflictObjects": [],
            "tombstoneObjects": tombstones,
            "errors": [],
        },
    )


def _run_executor(plan_path: Path, source_root: Path, target_root: Path, output_path: Path) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        [
            "powershell.exe",
            "-NoProfile",
            "-ExecutionPolicy",
            "Bypass",
            "-File",
            str(EXECUTOR_SCRIPT),
            "-ResourceDeltaPlanPath",
            str(plan_path),
            "-SourceObjectRoot",
            str(source_root),
            "-TargetObjectRoot",
            str(target_root),
            "-OutputPath",
            str(output_path),
            "-Mode",
            "local-execute",
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


def test_local_executor_copies_only_three_new_objects_with_10000_verify_only(tmp_path: Path) -> None:
    source_root = tmp_path / "source"
    target_root = tmp_path / "target"
    bucket = "yudao"
    verify_only_objects = [
        _write_object(target_root, bucket, f"dcc/file-{index:05d}.bin", _content(index))
        for index in range(10_000)
    ]
    copy_objects = [
        _write_object(source_root, bucket, f"dcc/file-{index:05d}.bin", _content(index))
        for index in range(10_000, 10_003)
    ]
    plan_path = tmp_path / "resource-delta-plan.json"
    output_path = tmp_path / "resource-delta-completed.json"
    _write_plan(plan_path, copy_objects=copy_objects, verify_only_objects=verify_only_objects)

    result = _run_executor(plan_path, source_root, target_root, output_path)

    assert result.returncode == 0, result.stdout + result.stderr
    completed = _read_json(output_path)
    assert completed["status"] == "completed_verified"
    assert completed["mode"] == "local-execute"
    assert completed["summary"]["copyObjects"] == 3
    assert completed["summary"]["verifyOnlyObjects"] == 10_000
    assert completed["summary"]["tombstoneObjects"] == 0
    assert completed["summary"]["conflictObjects"] == 0
    assert len(completed["copiedObjects"]) == 3
    assert len(list((target_root / bucket / "dcc").glob("*.bin"))) == 10_003
    assert _object_path(target_root, bucket, "dcc/file-10000.bin").read_bytes() == _content(10_000)
    assert _object_path(target_root, bucket, "dcc/file-10001.bin").read_bytes() == _content(10_001)
    assert _object_path(target_root, bucket, "dcc/file-10002.bin").read_bytes() == _content(10_002)


def test_local_executor_fails_when_copy_target_already_exists_without_overwrite(tmp_path: Path) -> None:
    source_root = tmp_path / "source"
    target_root = tmp_path / "target"
    bucket = "yudao"
    copy_object = _write_object(source_root, bucket, "dcc/existing.pdf", b"new-content")
    _write_object(target_root, bucket, "dcc/existing.pdf", b"old-content")
    plan_path = tmp_path / "resource-delta-plan.json"
    output_path = tmp_path / "resource-delta-completed.json"
    _write_plan(plan_path, copy_objects=[copy_object], verify_only_objects=[])

    result = _run_executor(plan_path, source_root, target_root, output_path)

    assert result.returncode == 2, result.stdout + result.stderr
    completed = _read_json(output_path)
    assert completed["status"] == "failed"
    assert completed["errors"][0]["code"] == "RESOURCE_DELTA_TARGET_ALREADY_EXISTS"
    assert _object_path(target_root, bucket, "dcc/existing.pdf").read_bytes() == b"old-content"


def test_local_executor_fails_when_verify_only_readback_mismatches(tmp_path: Path) -> None:
    source_root = tmp_path / "source"
    target_root = tmp_path / "target"
    bucket = "yudao"
    expected = _delta_object(bucket, "dcc/verify.pdf", b"expected-content")
    _write_object(target_root, bucket, "dcc/verify.pdf", b"tampered-content")
    plan_path = tmp_path / "resource-delta-plan.json"
    output_path = tmp_path / "resource-delta-completed.json"
    _write_plan(plan_path, copy_objects=[], verify_only_objects=[expected])

    result = _run_executor(plan_path, source_root, target_root, output_path)

    assert result.returncode == 2, result.stdout + result.stderr
    completed = _read_json(output_path)
    assert completed["status"] == "failed"
    assert completed["errors"][0]["code"] == "RESOURCE_DELTA_READBACK_MISMATCH"


def test_local_executor_fails_when_verify_only_size_mismatches(tmp_path: Path) -> None:
    source_root = tmp_path / "source"
    target_root = tmp_path / "target"
    bucket = "yudao"
    content = b"expected-content"
    expected = _delta_object(bucket, "dcc/verify-size.pdf", content)
    expected["size"] = len(content) + 1
    _write_object(target_root, bucket, "dcc/verify-size.pdf", content)
    plan_path = tmp_path / "resource-delta-plan.json"
    output_path = tmp_path / "resource-delta-completed.json"
    _write_plan(plan_path, copy_objects=[], verify_only_objects=[expected])

    result = _run_executor(plan_path, source_root, target_root, output_path)

    assert result.returncode == 2, result.stdout + result.stderr
    completed = _read_json(output_path)
    assert completed["status"] == "failed"
    assert completed["errors"][0]["code"] == "RESOURCE_DELTA_READBACK_MISMATCH"


def test_local_executor_records_tombstones_without_physical_delete(tmp_path: Path) -> None:
    source_root = tmp_path / "source"
    target_root = tmp_path / "target"
    bucket = "yudao"
    tombstone = _write_object(target_root, bucket, "dcc/no-longer-referenced.pdf", b"retained")
    plan_path = tmp_path / "resource-delta-plan.json"
    output_path = tmp_path / "resource-delta-completed.json"
    _write_plan(plan_path, copy_objects=[], verify_only_objects=[], tombstone_objects=[tombstone])

    result = _run_executor(plan_path, source_root, target_root, output_path)

    assert result.returncode == 0, result.stdout + result.stderr
    completed = _read_json(output_path)
    assert completed["status"] == "completed_verified"
    assert completed["summary"]["tombstoneObjects"] == 1
    assert completed["tombstoneObjects"][0]["objectKey"] == "dcc/no-longer-referenced.pdf"
    assert _object_path(target_root, bucket, "dcc/no-longer-referenced.pdf").exists()
    assert "deleteObjects" not in completed
    assert "overwriteObjects" not in completed
