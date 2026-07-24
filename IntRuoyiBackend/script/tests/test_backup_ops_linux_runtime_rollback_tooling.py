from pathlib import Path


def _backup_root() -> Path:
    return Path(__file__).resolve().parents[1] / "backup-ops"


def test_linux_runtime_entry_supports_rollback_app_mode() -> None:
    text = (_backup_root() / "linux" / "backup_ops_linux.py").read_text(encoding="utf-8")

    assert "rollback-app" in text


def test_linux_runtime_rollback_reads_image_tag_candidates_and_updates_runtime_env() -> None:
    text = (_backup_root() / "linux" / "backup_ops_linux.py").read_text(encoding="utf-8")

    assert "image-tag.txt" in text
    assert "runtime.env" in text or ".env" in text
    assert "docker compose up -d backend frontend" in text
    assert "actuator/health" in text
