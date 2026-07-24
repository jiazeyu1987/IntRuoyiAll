from pathlib import Path

import pytest

from script.release.release_target_state_collector import (
    TargetStateCollectionError,
    collect_target_state,
)


def write_file(path: Path, content: str) -> Path:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")
    return path


def test_target_state_collection_requires_release_tag(tmp_path: Path) -> None:
    with pytest.raises(TargetStateCollectionError, match="releaseTag"):
        collect_target_state(tmp_path)


def test_target_state_collection_requires_migration_state(tmp_path: Path) -> None:
    write_file(tmp_path / "release-tag.txt", "20260613_150000\n")

    with pytest.raises(TargetStateCollectionError, match="migration-state.json"):
        collect_target_state(tmp_path)


def test_target_state_collection_returns_complete_snapshot(tmp_path: Path) -> None:
    write_file(tmp_path / "release-tag.txt", "20260613_150000\n")
    write_file(
        tmp_path / "migration-state.json",
        '{"migrations":[{"migrationId":"m1","sha256":"aaa","status":"APPLIED"}]}\n',
    )
    ops_script = write_file(tmp_path / "ops" / "deploy.sh", "#!/bin/sh\necho deploy\n")
    compose = write_file(tmp_path / "docker-compose.yml", "services: {}\n")
    env = write_file(tmp_path / ".env", "IMAGE_TAG=20260613_150000\n")
    write_file(tmp_path / "images.json", '{"backend":"backend:20260613","frontend":"frontend:20260613"}\n')

    snapshot = collect_target_state(tmp_path)

    assert snapshot["status"] == "collected"
    assert snapshot["releaseTag"] == "20260613_150000"
    assert snapshot["migrations"]["m1"]["status"] == "APPLIED"
    assert snapshot["images"]["backend"] == "backend:20260613"
    assert snapshot["opsScripts"][ops_script.relative_to(tmp_path).as_posix()].startswith("sha256:")
    assert snapshot["composeHash"].startswith("sha256:")
    assert snapshot["envHash"].startswith("sha256:")
