from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def test_runtime_control_uses_nas_backup_points_root_config():
    compose_text = read_text(REPO_ROOT / "script" / "deploy" / "int-ruoyi-test" / "docker-compose.yml")
    local_yaml = read_text(REPO_ROOT / "yudao-server" / "src" / "main" / "resources" / "application-local.yaml")

    assert (
        "--yudao.runtime-control.backup-ops.nas-backup-points-root="
        "${RUNTIME_CONTROL_NAS_BACKUP_POINTS_ROOT:-Backup/BackupPackage}"
    ) in compose_text
    assert (
        "--yudao.runtime-control.release-package.nas-release-root="
        "${RUNTIME_CONTROL_NAS_RELEASE_ROOT:-Backup/ReleasePackage}"
    ) in compose_text
    assert (
        "nas-backup-points-root: ${INTRUOYI_RUNTIME_CONTROL_NAS_BACKUP_POINTS_ROOT:Backup/BackupPackage}"
    ) in local_yaml
    assert (
        "nas-release-root: ${INTRUOYI_RUNTIME_CONTROL_NAS_RELEASE_ROOT:Backup/ReleasePackage}"
    ) in local_yaml
