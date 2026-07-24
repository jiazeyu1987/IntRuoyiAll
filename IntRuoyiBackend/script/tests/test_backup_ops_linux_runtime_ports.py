import importlib.util
import json
import shlex
import shutil
from pathlib import Path

import pytest


def _backup_root() -> Path:
    return Path(__file__).resolve().parents[1] / "backup-ops"


def _load_linux_module():
    module_path = _backup_root() / "linux" / "backup_ops_linux.py"
    spec = importlib.util.spec_from_file_location("backup_ops_linux_ports", module_path)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


def _base_config(tmp_path: Path) -> tuple[dict, Path, Path]:
    app_dir = tmp_path / "runtime"
    app_dir.mkdir()
    (app_dir / "docker-compose.yml").write_text("services: {}\n", encoding="utf-8")
    backup_points_root = tmp_path / "backup-points"
    backup_points_root.mkdir()
    config = {
        "environment": "linux-local",
        "servers": {
            "production": {
                "host": "127.0.0.1",
                "appDir": str(app_dir),
                "tmpRoot": str(tmp_path / "tmp"),
            },
            "test": {
                "backupPointsRoot": str(backup_points_root),
                "releasePackagesRoot": str(tmp_path / "release-packages"),
                "minioContainer": "ragflow_compose-minio-1",
            },
            "backup": {
                "host": "172.30.30.59",
                "runtimeDir": str(app_dir),
                "tmpRoot": str(tmp_path / "backup-tmp"),
                "backupPointsRoot": str(backup_points_root),
                "releasePackagesRoot": str(tmp_path / "release-packages"),
                "minioContainer": "intruoyi-minio",
            },
        },
        "backup": {
            "mysqlDatabase": "ruoyi-vue-pro",
            "mysqlBackupMode": "full-dump-baseline",
            "objectBucket": "yudao",
        },
        "containers": {
            "mysql": "intruoyi-mysql",
            "minio": "intruoyi-minio",
        },
        "tools": {
            "minioClientImage": "quay.io/minio/mc:latest",
        },
    }
    return config, app_dir, backup_points_root


def _write_runtime_env(app_dir: Path, *, backend_port: str = "49081", frontend_port: str = "18081") -> None:
    lines = [
        "IMAGE_TAG=release-current",
        "MYSQL_ROOT_PASSWORD=not-a-real-password",
    ]
    if backend_port:
        lines.append(f"BACKEND_HOST_PORT={backend_port}")
    if frontend_port:
        lines.append(f"FRONTEND_HOST_PORT={frontend_port}")
    (app_dir / ".env").write_text("\n".join(lines) + "\n", encoding="utf-8")


def _valid_dcc_backup_manifest(backup_id: str = "20260526-220000") -> dict[str, object]:
    return {
        "schemaVersion": "dcc-backup-manifest-v1",
        "backupId": backup_id,
        "targetEnvironment": "test",
        "targetHost": "172.30.30.58",
        "status": "success",
        "chainStatus": "COMPLETE",
        "backupMode": "full",
        "baselineBackupId": backup_id,
        "baselineRestorePointId": "B1",
        "previousBackupId": "",
        "previousRestorePointId": "",
        "restoreVerified": False,
        "restoreRehearsal": {"status": "not-run"},
        "fullBaseline": {"restorePointId": "B1", "checksum": "sha256:" + "1" * 64},
        "incrementalChain": [],
        "restorePoints": [
            {"id": "B1", "databaseRestorePointId": "B1", "objectInventoryRestorePointId": "B1"}
        ],
        "objectInventories": [{"restorePointId": "B1", "objects": []}],
        "databaseRecords": [],
        "dccEvents": [],
    }


def _invalid_deleted_dcc_backup_manifest(backup_id: str = "20260526-220000") -> dict[str, object]:
    manifest = _valid_dcc_backup_manifest(backup_id)
    manifest["incrementalChain"] = [
        {"from": "B1", "to": "B2", "checksum": "sha256:" + "2" * 64}
    ]
    manifest["restorePoints"] = [
        {"id": "B1", "databaseRestorePointId": "B1", "objectInventoryRestorePointId": "B1"},
        {"id": "B2", "databaseRestorePointId": "B2", "objectInventoryRestorePointId": "B2"},
    ]
    manifest["objectInventories"] = [
        {"restorePointId": "B1", "objects": []},
        {
            "restorePointId": "B2",
            "objects": [
                {
                    "fileKey": "DCC-B",
                    "state": "deleted",
                    "contentHash": "sha256:" + "b" * 64,
                    "storedHash": "sha256:" + "b" * 64,
                    "present": False,
                }
            ],
        },
    ]
    manifest["databaseRecords"] = [
        {"restorePointId": "B2", "fileKey": "DCC-B", "state": "deleted"}
    ]
    manifest["dccEvents"] = []
    return manifest


def _write_dcc_backup_manifest(
    manifest_dir: Path,
    backup_id: str = "20260526-220000",
    manifest: dict[str, object] | None = None,
) -> None:
    manifest_dir.mkdir(parents=True, exist_ok=True)
    (manifest_dir / "dcc-backup-manifest.json").write_text(
        json.dumps(
            manifest or _valid_dcc_backup_manifest(backup_id),
            ensure_ascii=False,
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )


class _NoDockerRunner:
    def run(self, *_args, **_kwargs) -> str:
        raise AssertionError("runtime port validation must finish before Docker commands run")


class _NoHighRiskRunner:
    def __init__(self) -> None:
        self.calls = []

    def run(self, *args, **kwargs) -> str:
        self.calls.append((args, kwargs))
        raise AssertionError("high-risk action must be blocked before Docker or restore commands run")


class _NoopRunner:
    def run(self, *_args, **_kwargs) -> str:
        return ""


class _DccBackupNowRunner:
    def __init__(self) -> None:
        self.commands: list[str] = []
        self.logs: list[str] = []
        self.copied_objects: list[str] = []

    def log(self, message: str) -> None:
        self.logs.append(message)

    def run(self, command: str, *_args, **_kwargs) -> str:
        self.commands.append(command)
        parts = shlex.split(command)
        if "docker image inspect" in command:
            return ""
        if "SELECT @@global.log_bin" in command:
            return "1\tROW\t604800\nmysql-bin.000123\t456789\t\t\t\n"
        if "mysqlbinlog" in command:
            self._write_redirect(command, b"-- binlog segment\n")
            return ""
        if "docker exec -i rehearsal-mysql mysql" in command:
            return ""
        if "mysqldump" in command:
            self._write_redirect(command, b"not-a-real-dump")
            return ""
        if "mc mirror --overwrite" in command:
            volume = self._volume_argument(parts)
            object_path = volume / "yudao" / "dcc" / "original" / "codex-linux-dcc.docx"
            object_path.parent.mkdir(parents=True, exist_ok=True)
            object_path.write_bytes(b"codex-linux-dcc")
            return ""
        if "mc cp " in command:
            volume = self._volume_argument(parts)
            source = next(part for part in shlex.split(parts[-1]) if part.startswith("src/yudao/"))
            object_key = source.removeprefix("src/yudao/")
            self.copied_objects.append(object_key)
            object_path = volume / "yudao" / object_key
            object_path.parent.mkdir(parents=True, exist_ok=True)
            object_path.write_bytes(b"codex-linux-dcc")
            return ""
        if "mc ls --recursive --json" in command:
            return (
                '{"status":"success","type":"file","lastModified":"2026-06-09T12:00:00Z",'
                '"size":15,"key":"dcc/original/codex-linux-dcc.docx","etag":"etag"}\n'
            )
        if command.startswith("/bin/cp -a "):
            source = Path(parts[-2])
            target = Path(parts[-1])
            shutil.copytree(source, target)
            return ""
        if command == "sync":
            return ""
        if "dcc-database-query.sql" in command:
            self._write_redirect(
                command,
                (
                    "controlledFileId\ttenantId\tfileNumber\tversionNo\tstatus\tupdatedAt\t"
                    "objectRole\tobjectFileId\tobjectPath\tobjectSha256\tpermissionDigest\n"
                    "2054545668044049608\t122\tCDR-RED\tV1.0\tPUBLISHED\t"
                    "2026-06-09T12:00:00+08:00\tsource\t9001\t"
                    "dcc/original/codex-linux-dcc.docx\t\tsha256:permission\n"
                ).encode("utf-8"),
            )
            return ""
        raise AssertionError(f"unexpected command: {command}")

    def _write_redirect(self, command: str, content: bytes) -> None:
        parts = shlex.split(command)
        assert ">" in parts, command
        target = Path(parts[parts.index(">") + 1])
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(content)

    def _volume_argument(self, parts: list[str]) -> Path:
        assert "-v" in parts, parts
        value = parts[parts.index("-v") + 1]
        host_path, marker = value.rsplit(":/backup", 1)
        assert marker == ""
        return Path(host_path)


def _write_restore_backup(
    backup_root: Path,
    *,
    backup_id: str = "20260526-220000",
    image_tag: str = "release-current",
    include_redis: bool = True,
    include_configuration: bool = True,
    include_checksums: bool = True,
    include_backup_strategy: bool = True,
    include_dcc_manifest: bool = True,
    backup_strategy: dict | None = None,
    rehearsal_status: str | None = "PASSED",
    last_rehearsed_at: str | None = "2026-06-09T06:20:00+08:00",
) -> None:
    (backup_root / "mysql").mkdir(parents=True)
    (backup_root / "objects" / "yudao").mkdir(parents=True)
    (backup_root / "manifest").mkdir(parents=True)
    (backup_root / "mysql" / "ruoyi-vue-pro.sql.gz").write_bytes(b"not-a-real-dump")
    recovery_set = {
        "id": backup_id,
        "status": "COMPLETE",
        "program": {"imageTag": image_tag},
    }
    if include_redis:
        recovery_set["redis"] = {"policy": "CLEAR_AND_REBUILD"}
    if include_configuration:
        recovery_set["configuration"] = {
            "manifestPath": "deploy/runtime.env",
            "composePath": "deploy/docker-compose.yml",
        }
    if include_checksums:
        recovery_set["checksums"] = {"sha256": "abc"}
    if include_dcc_manifest:
        recovery_set["dcc"] = {"manifestPath": "manifest/dcc-backup-manifest.json"}
        _write_dcc_backup_manifest(backup_root / "manifest", backup_id=backup_id)
    manifest = {"schemaVersion": "v2", "status": "success", "recoverySet": recovery_set}
    if backup_strategy is not None:
        manifest["backupStrategy"] = backup_strategy
    elif include_backup_strategy:
        manifest["backupStrategy"] = {
            "mode": "incremental-manifest",
            "mysqlBackupMode": "full-dump-baseline",
            "mysqlBaseline": "full-dump",
        }
    if rehearsal_status is not None or last_rehearsed_at is not None:
        manifest["validation"] = {
            "mysqlDumpCreated": True,
            "objectBackupCreated": True,
            "checksumsGenerated": True,
            "rehearsalStatus": rehearsal_status,
            "lastRehearsedAt": last_rehearsed_at,
        }
    (backup_root / "manifest" / "manifest.json").write_text(
        json.dumps(manifest),
        encoding="utf-8",
    )


@pytest.mark.parametrize(
    ("mode", "missing_key"),
    [
        ("backup-now", "BACKEND_HOST_PORT"),
        ("backup-now", "FRONTEND_HOST_PORT"),
        ("restore-data", "BACKEND_HOST_PORT"),
        ("restore-data", "FRONTEND_HOST_PORT"),
        ("rollback-app", "BACKEND_HOST_PORT"),
        ("rollback-app", "FRONTEND_HOST_PORT"),
    ],
)
def test_linux_runtime_ports_are_required_before_mutating_actions(tmp_path: Path, mode: str, missing_key: str) -> None:
    module = _load_linux_module()
    config, app_dir, backup_points_root = _base_config(tmp_path)
    _write_runtime_env(
        app_dir,
        backend_port="" if missing_key == "BACKEND_HOST_PORT" else "49081",
        frontend_port="" if missing_key == "FRONTEND_HOST_PORT" else "18081",
    )

    if mode == "restore-data":
        config["environment"] = "test"
        backup_root = backup_points_root / "20260526-220000"
        (backup_root / "mysql").mkdir(parents=True)
        (backup_root / "mysql" / "ruoyi-vue-pro.sql.gz").write_bytes(b"not-a-real-dump")
    if mode == "rollback-app":
        config["environment"] = "test"
        backup_root = backup_points_root / "20260526-220000" / "deploy"
        backup_root.mkdir(parents=True)
        (backup_root / "image-tag.txt").write_text("release-previous\n", encoding="utf-8")

    with pytest.raises(module.BackupOpsError) as exc_info:
        if mode == "backup-now":
            module.backup_now(config, _NoDockerRunner())
        elif mode == "restore-data":
            module.restore_data(config, _NoDockerRunner(), "20260526-220000")
        else:
            module.rollback_app(config, _NoDockerRunner(), "release-previous")

    assert exc_info.value.status == "blocked"
    assert missing_key in exc_info.value.message
    assert "48081" not in exc_info.value.message
    assert "8081" not in exc_info.value.message


def test_linux_backup_manifest_uses_runtime_env_ports(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> None:
    module = _load_linux_module()
    config, app_dir, backup_points_root = _base_config(tmp_path)
    config["backup"]["dccTenantId"] = 122
    config["servers"]["test"].update(
        {
            "host": "172.30.30.58",
            "runtimeDir": str(app_dir),
            "tmpRoot": str(tmp_path / "test-tmp"),
            "backupPointsRoot": str(backup_points_root),
        }
    )
    config = module.project_target_environment(config, "backup-now", "test")
    _write_runtime_env(app_dir, backend_port="49123", frontend_port="18099")
    monkeypatch.setattr(module, "backup_sequence", lambda: "20260609-010203")
    monkeypatch.setattr(module, "get_minio_creds", lambda _container_name: ("access", "secret"))
    monkeypatch.setattr(module, "require_executable", lambda path, _code, _message: path)

    module.backup_now(config, _DccBackupNowRunner())

    manifest_paths = list(backup_points_root.glob("*/manifest/manifest.json"))
    assert len(manifest_paths) == 1
    manifest = json.loads(manifest_paths[0].read_text(encoding="utf-8"))
    assert manifest["deploy"]["backendPort"] == 49123
    assert manifest["deploy"]["frontendPort"] == 18099
    assert manifest["deploy"]["backendPort"] != 48081
    assert manifest["deploy"]["frontendPort"] != 8081


def test_linux_backup_now_generates_dcc_restore_candidate_contract(
    tmp_path: Path,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    module = _load_linux_module()
    config, app_dir, backup_points_root = _base_config(tmp_path)
    config["backup"]["dccTenantId"] = 122
    config["servers"]["test"].update(
        {
            "host": "172.30.30.58",
            "runtimeDir": str(app_dir),
            "tmpRoot": str(tmp_path / "test-tmp"),
            "backupPointsRoot": str(backup_points_root),
        }
    )
    config = module.project_target_environment(config, "backup-now", "test")
    _write_runtime_env(app_dir, backend_port="49123", frontend_port="18099")
    monkeypatch.setattr(module, "backup_sequence", lambda: "20260609-000001")
    monkeypatch.setattr(module, "get_minio_creds", lambda _container_name: ("access", "secret"))
    monkeypatch.setattr(module, "require_executable", lambda path, _code, _message: path)

    module.backup_now(config, _DccBackupNowRunner())

    backup_root = backup_points_root / "20260609-000001"
    object_inventory = json.loads((backup_root / "objects" / "manifest-object-inventory.json").read_text(encoding="utf-8"))
    snapshot = json.loads((backup_root / "manifest" / "dcc-database-snapshot.json").read_text(encoding="utf-8"))
    dcc_manifest = json.loads((backup_root / "manifest" / "dcc-backup-manifest.json").read_text(encoding="utf-8"))
    manifest = json.loads((backup_root / "manifest" / "manifest.json").read_text(encoding="utf-8"))

    assert object_inventory["mode"] == "incremental-manifest"
    assert object_inventory["bucket"] == "yudao"
    assert object_inventory["objects"][0]["path"] == "dcc/original/codex-linux-dcc.docx"
    assert object_inventory["objects"][0]["repositoryKey"].startswith("sha256:")
    assert object_inventory["objects"][0]["repositoryPath"].startswith("sha256/")
    assert (backup_points_root / "object-store" / object_inventory["objects"][0]["repositoryPath"]).is_file()
    assert not (backup_root / "objects" / "yudao").exists()
    assert snapshot["targetEnvironment"] == "test"
    assert snapshot["targetHost"] == "172.30.30.58"
    assert snapshot["controlledFiles"][0]["fileKey"] == "controlled-file:2054545668044049608"
    assert dcc_manifest["schemaVersion"] == "dcc-backup-manifest-v1"
    assert dcc_manifest["status"] == "success"
    assert dcc_manifest["backupScopeType"] == "target-dataset"
    assert dcc_manifest["backupScopeId"] == "dcc-tenant-122"
    assert dcc_manifest["changeSummary"] == {
        "addedRecords": 1,
        "changedRecords": 0,
        "deletedRecords": 0,
        "invalidatedRecords": 0,
        "addedObjects": 1,
        "changedObjects": 0,
        "reusedObjects": 0,
        "tombstoneObjects": 0,
    }
    assert dcc_manifest["fullBaseline"]["restorePointId"] == "20260609-000001"
    assert dcc_manifest["restorePoints"][0]["id"] == "20260609-000001"
    assert dcc_manifest["dccEvents"][0]["eventType"] == "add"
    assert manifest["targetEnvironment"] == "test"
    assert manifest["targetHost"] == "172.30.30.58"
    assert manifest["backupStrategy"]["mode"] == "incremental-manifest"
    assert manifest["recoverySet"]["minio"]["snapshotPath"] == "objects/manifest-object-inventory.json"
    assert manifest["recoverySet"]["dcc"]["manifestPath"] == "manifest/dcc-backup-manifest.json"


def test_linux_backup_now_reuses_unchanged_object_without_bucket_mirror(
    tmp_path: Path,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    module = _load_linux_module()
    config, app_dir, backup_points_root = _base_config(tmp_path)
    config["backup"]["dccTenantId"] = 122
    config["servers"]["test"].update(
        {
            "host": "172.30.30.58",
            "runtimeDir": str(app_dir),
            "tmpRoot": str(tmp_path / "test-tmp"),
            "backupPointsRoot": str(backup_points_root),
        }
    )
    config = module.project_target_environment(config, "backup-now", "test")
    _write_runtime_env(app_dir, backend_port="49123", frontend_port="18099")
    digest = module.sha256_file(Path(__file__).resolve())
    repository_path = module.repository_path_from_sha256(digest)
    object_store_file = backup_points_root / "object-store" / repository_path
    object_store_file.parent.mkdir(parents=True)
    object_store_file.write_bytes(b"existing-content")
    previous_root = backup_points_root / "20260608-000001"
    (previous_root / "objects").mkdir(parents=True)
    (previous_root / "objects" / "manifest-object-inventory.json").write_text(
        json.dumps(
            {
                "mode": "incremental-manifest",
                "bucket": "yudao",
                "objectStoreRoot": str(backup_points_root / "object-store"),
                "objects": [
                    {
                        "path": "dcc/original/codex-linux-dcc.docx",
                        "size": 15,
                        "sourceEtag": "etag",
                        "repositoryKey": digest,
                        "repositoryPath": repository_path,
                        "status": "active",
                    }
                ],
            },
            ensure_ascii=False,
        ),
        encoding="utf-8",
    )
    monkeypatch.setattr(module, "backup_sequence", lambda: "20260609-000001")
    monkeypatch.setattr(module, "get_minio_creds", lambda _container_name: ("access", "secret"))
    monkeypatch.setattr(module, "require_executable", lambda path, _code, _message: path)
    runner = _DccBackupNowRunner()

    module.backup_now(config, runner)

    backup_root = backup_points_root / "20260609-000001"
    object_inventory = json.loads((backup_root / "objects" / "manifest-object-inventory.json").read_text(encoding="utf-8"))
    assert not any("mc mirror --overwrite" in command for command in runner.commands)
    assert runner.copied_objects == []
    assert object_inventory["stats"] == {
        "addedCount": 0,
        "modifiedCount": 0,
        "deletedCount": 0,
        "reusedCount": 1,
    }
    assert object_inventory["objects"][0]["repositoryPath"] == repository_path
    assert object_inventory["objects"][0]["changeType"] == "reused"
    assert not (backup_root / "objects" / "yudao").exists()


def test_linux_backup_manifest_declares_complete_recovery_set(tmp_path: Path) -> None:
    module = _load_linux_module()
    config, _app_dir, backup_points_root = _base_config(tmp_path)
    config["environment"] = "test"
    config["servers"]["production"]["host"] = "172.30.30.58"
    backup_root = backup_points_root / "20260526-220000"
    for relative in [
        "deploy",
        "mysql",
        "objects",
    ]:
        (backup_root / relative).mkdir(parents=True)
    (backup_root / "deploy" / "docker-compose.yml").write_text("services: {}\n", encoding="utf-8")
    (backup_root / "deploy" / "runtime.env").write_text(
        "IMAGE_TAG=release-v2\nBACKEND_HOST_PORT=49123\nFRONTEND_HOST_PORT=18099\n",
        encoding="utf-8",
    )
    (backup_root / "deploy" / "image-tag.txt").write_text("release-v2\n", encoding="utf-8")
    (backup_root / "mysql" / "ruoyi-vue-pro.sql.gz").write_bytes(b"dump")
    (backup_root / "objects" / "manifest-object-inventory.json").write_text(
        '{"mode":"incremental-manifest","objects":[]}', encoding="utf-8"
    )
    _write_dcc_backup_manifest(backup_root / "manifest")
    module.write_checksums(backup_root)

    module.write_manifest(backup_root, "20260526-220000", "manual", config, "release-v2", 49123, 18099)

    manifest = json.loads((backup_root / "manifest" / "manifest.json").read_text(encoding="utf-8"))
    recovery_set = manifest["recoverySet"]
    assert manifest["schemaVersion"] == "v2"
    assert manifest["targetEnvironment"] == "test"
    assert manifest["targetHost"] == "172.30.30.58"
    assert manifest["artifacts"]["objectSnapshot"] == "objects/manifest-object-inventory.json"
    assert recovery_set["id"] == "20260526-220000"
    assert recovery_set["status"] == "COMPLETE"
    assert recovery_set["program"]["imageTag"] == "release-v2"
    assert recovery_set["minio"]["snapshotPath"] == "objects/manifest-object-inventory.json"
    assert recovery_set["businessFiles"]["snapshotPath"] == "objects/manifest-object-inventory.json"
    assert recovery_set["dcc"]["manifestPath"] == "manifest/dcc-backup-manifest.json"
    assert recovery_set["redis"]["policy"] == "CLEAR_AND_REBUILD"
    assert recovery_set["configuration"]["manifestPath"] == "deploy/runtime.env"
    assert recovery_set["checksums"]["sha256"]
    assert manifest["backupStrategy"]["mode"] == "incremental-manifest"
    assert manifest["backupStrategy"]["mysqlBackupMode"] == "full-dump-baseline"
    assert manifest["backupStrategy"]["mysqlBaseline"] == "full-dump"
    assert manifest["backupStrategy"]["mysqlIncrementalPlan"]["binlog"]["status"] == "requires-prerequisite"
    assert manifest["backupStrategy"]["mysqlIncrementalPlan"]["xtrabackup"]["status"] == "requires-prerequisite"
    assert "binlogPosition" in manifest["backupStrategy"]["mysqlIncrementalPlan"]["binlog"]["requiredEvidence"]
    assert "xtrabackupCheckpoint" in manifest["backupStrategy"]["mysqlIncrementalPlan"]["xtrabackup"]["requiredEvidence"]
    assert "No silent full dump fallback" in manifest["backupStrategy"]["mysqlIncrementalPlan"]["noFallbackRule"]


def test_linux_backup_manifest_blocks_mysql_incremental_request_without_prerequisites(tmp_path: Path) -> None:
    module = _load_linux_module()
    config, _app_dir, backup_points_root = _base_config(tmp_path)
    config["environment"] = "test"
    config["servers"]["production"]["host"] = "172.30.30.58"
    config["backup"]["mysqlBackupMode"] = "binlog-incremental"
    backup_root = backup_points_root / "20260526-220000"
    for relative in [
        "deploy",
        "mysql",
        "objects",
    ]:
        (backup_root / relative).mkdir(parents=True)
    (backup_root / "deploy" / "docker-compose.yml").write_text("services: {}\n", encoding="utf-8")
    (backup_root / "deploy" / "runtime.env").write_text(
        "IMAGE_TAG=release-v2\nBACKEND_HOST_PORT=49123\nFRONTEND_HOST_PORT=18099\n",
        encoding="utf-8",
    )
    (backup_root / "deploy" / "image-tag.txt").write_text("release-v2\n", encoding="utf-8")
    (backup_root / "mysql" / "ruoyi-vue-pro.sql.gz").write_bytes(b"dump")
    (backup_root / "objects" / "manifest-object-inventory.json").write_text(
        '{"mode":"incremental-manifest","bucket":"yudao","objectStoreRoot":"/mnt/nas/Backup/BackupPackage/object-store","objects":[]}',
        encoding="utf-8",
    )
    _write_dcc_backup_manifest(backup_root / "manifest")
    module.write_checksums(backup_root)

    with pytest.raises(module.BackupOpsError) as exc_info:
        module.write_manifest(backup_root, "20260526-220000", "manual", config, "release-v2", 49123, 18099)

    assert exc_info.value.code == "INTBK-6001"
    assert exc_info.value.status == "blocked"
    assert "mysqlBackupMode=binlog-incremental" in exc_info.value.message
    assert "No silent full dump fallback" in exc_info.value.message


def test_linux_backup_now_records_binlog_preflight_before_rehearsal_gate(
    tmp_path: Path,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    module = _load_linux_module()
    config, app_dir, backup_points_root = _base_config(tmp_path)
    config["backup"]["dccTenantId"] = 122
    config["backup"]["mysqlBackupMode"] = "binlog-incremental"
    config["servers"]["test"].update(
        {
            "host": "172.30.30.58",
            "runtimeDir": str(app_dir),
            "tmpRoot": str(tmp_path / "test-tmp"),
            "backupPointsRoot": str(backup_points_root),
        }
    )
    config = module.project_target_environment(config, "backup-now", "test")
    _write_runtime_env(app_dir, backend_port="49123", frontend_port="18099")
    monkeypatch.setattr(module, "backup_sequence", lambda: "20260609-010205")
    monkeypatch.setattr(module, "get_minio_creds", lambda _container_name: ("access", "secret"))
    monkeypatch.setattr(module, "require_executable", lambda path, _code, _message: path)
    runner = _DccBackupNowRunner()

    result = module.backup_now(config, runner)

    backup_root = Path(result["backupRoot"])
    preflight = json.loads((backup_root / "mysql" / "binlog-preflight.json").read_text(encoding="utf-8"))
    assert preflight["status"] == "preflight-passed"
    assert preflight["evidence"]["binlogEnabled"] == "1"
    assert preflight["evidence"]["binlogFormat"] == "ROW"
    assert preflight["evidence"]["binlogPosition"] == "mysql-bin.000123:456789"
    manifest = json.loads((backup_root / "manifest" / "manifest.json").read_text(encoding="utf-8"))
    assert manifest["validation"]["rehearsalStatus"] == "not-run"


def test_linux_backup_now_exports_binlog_segment_manifest_before_replay_gate(
    tmp_path: Path,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    module = _load_linux_module()
    config, app_dir, _backup_points_root = _base_config(tmp_path)
    config["backup"]["dccTenantId"] = 122
    config["backup"]["mysqlBackupMode"] = "binlog-incremental"
    config["servers"]["test"].update(
        {
            "host": "172.30.30.58",
            "runtimeDir": str(app_dir),
            "tmpRoot": str(tmp_path / "test-tmp"),
            "backupPointsRoot": str(_backup_points_root),
        }
    )
    config = module.project_target_environment(config, "backup-now", "test")
    _write_runtime_env(app_dir, backend_port="49123", frontend_port="18099")
    monkeypatch.setattr(module, "backup_sequence", lambda: "20260609-010206")
    monkeypatch.setattr(module, "get_minio_creds", lambda _container_name: ("access", "secret"))
    monkeypatch.setattr(module, "require_executable", lambda path, _code, _message: path)
    runner = _DccBackupNowRunner()

    result = module.backup_now(config, runner)

    backup_root = Path(result["backupRoot"])
    segment = json.loads((backup_root / "mysql" / "binlog-segment-manifest.json").read_text(encoding="utf-8"))
    assert segment["schemaVersion"] == "mysql-binlog-segment-v1"
    assert segment["status"] == "exported"
    assert segment["replayStatus"] == "not-run"
    assert segment["segments"][0]["binlogFile"] == "mysql-bin.000123"
    assert segment["segments"][0]["startPosition"] == 4
    assert segment["segments"][0]["stopPosition"] == 456789
    exported = backup_root / "mysql" / "binlog" / "mysql-bin.000123.sql"
    assert exported.read_text(encoding="utf-8") == "-- binlog segment\n"
    assert segment["segments"][0]["sha256"]
    manifest = json.loads((backup_root / "manifest" / "manifest.json").read_text(encoding="utf-8"))
    assert manifest["backupStrategy"]["mysqlIncrementalPlan"]["binlog"]["replayStatus"] == "not-run"


def test_linux_backup_now_writes_binlog_manifest_and_requires_rehearsal(
    tmp_path: Path,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    module = _load_linux_module()
    config, app_dir, backup_points_root = _base_config(tmp_path)
    config["backup"]["dccTenantId"] = 122
    config["backup"]["mysqlBackupMode"] = "binlog-incremental"
    config["servers"]["test"].update(
        {
            "host": "172.30.30.58",
            "runtimeDir": str(app_dir),
            "tmpRoot": str(tmp_path / "test-tmp"),
            "backupPointsRoot": str(backup_points_root),
        }
    )
    config = module.project_target_environment(config, "backup-now", "test")
    _write_runtime_env(app_dir, backend_port="49123", frontend_port="18099")
    monkeypatch.setattr(module, "backup_sequence", lambda: "20260609-010204")
    monkeypatch.setattr(module, "get_minio_creds", lambda _container_name: ("access", "secret"))
    monkeypatch.setattr(module, "require_executable", lambda path, _code, _message: path)
    runner = _DccBackupNowRunner()

    result = module.backup_now(config, runner)

    backup_root = Path(result["backupRoot"])
    assert backup_root.parent == backup_points_root
    manifest = json.loads((backup_root / "manifest" / "manifest.json").read_text(encoding="utf-8"))
    strategy = manifest["backupStrategy"]
    assert strategy["mysqlBackupMode"] == "binlog-incremental"
    assert strategy["mysqlIncrementalPlan"]["binlog"]["status"] == "requires-rehearsal"
    assert strategy["mysqlIncrementalPlan"]["binlog"]["evidence"]["binlogPosition"] == "mysql-bin.000123:456789"
    assert strategy["mysqlIncrementalPlan"]["binlog"]["segmentManifestPath"] == "mysql/binlog-segment-manifest.json"
    assert manifest["validation"]["rehearsalStatus"] == "not-run"
    assert (backup_root / "mysql" / "binlog-segment-manifest.json").is_file()


def test_linux_rehearsal_replays_binlog_segment_and_records_evidence(tmp_path: Path) -> None:
    module = _load_linux_module()
    backup_root = tmp_path / "backup" / "20260526-220000"
    binlog_dir = backup_root / "mysql" / "binlog"
    binlog_dir.mkdir(parents=True)
    binlog_file = binlog_dir / "mysql-bin.000123.sql"
    binlog_file.write_text("-- binlog segment\n", encoding="utf-8")
    manifest_path = backup_root / "mysql" / "binlog-segment-manifest.json"
    manifest_path.write_text(
        json.dumps(
            {
                "schemaVersion": "mysql-binlog-segment-v1",
                "status": "exported",
                "mode": "binlog-incremental",
                "replayStatus": "not-run",
                "segments": [
                    {
                        "binlogFile": "mysql-bin.000123",
                        "path": "mysql/binlog/mysql-bin.000123.sql",
                        "startPosition": 4,
                        "stopPosition": 456789,
                        "sha256": module.sha256_file(binlog_file),
                    }
                ],
            }
        ),
        encoding="utf-8",
    )
    runner = _DccBackupNowRunner()

    result = module.replay_mysql_binlog_segments(backup_root, runner, "rehearsal-mysql", "not-a-real-password", "ruoyi-vue-pro")

    assert result["status"] == "passed"
    replayed = json.loads(manifest_path.read_text(encoding="utf-8"))
    assert replayed["replayStatus"] == "passed"
    assert replayed["segments"][0]["replayStatus"] == "passed"
    assert replayed["segments"][0]["replayedAt"]
    assert any("mysql-bin.000123.sql" in command and "docker exec -i rehearsal-mysql mysql" in command for command in runner.commands)


def test_linux_restore_data_blocks_mysql_incremental_without_replay_evidence_before_actions(tmp_path: Path) -> None:
    module = _load_linux_module()
    config, app_dir, backup_points_root = _base_config(tmp_path)
    config["environment"] = "test"
    _write_runtime_env(app_dir, backend_port="49123", frontend_port="18099")
    backup_id = "20260526-220000"
    backup_root = backup_points_root / backup_id
    _write_restore_backup(
        backup_root,
        backup_id=backup_id,
        image_tag="release-current",
        backup_strategy={
            "mode": "incremental-manifest",
            "mysqlBackupMode": "binlog-incremental",
            "mysqlBaseline": "full-dump",
            "mysqlIncrementalPlan": {
                "binlog": {
                    "status": "requires-prerequisite",
                    "requiredEvidence": ["binlogPosition", "restoreReplayRehearsal"],
                },
                "xtrabackup": {"status": "not-used"},
                "noFallbackRule": "No silent full dump fallback is allowed for an incremental MySQL backup request.",
            },
        },
    )
    runner = _NoHighRiskRunner()

    with pytest.raises(module.BackupOpsError) as exc_info:
        module.restore_data(config, runner, backup_id)

    assert exc_info.value.code == "INTBK-3002"
    assert exc_info.value.status == "blocked"
    assert "mysqlBackupMode=binlog-incremental" in exc_info.value.message
    assert "restoreReplayRehearsal" in exc_info.value.message
    assert runner.calls == []
    assert not (tmp_path / "tmp" / backup_id / "pre-restore").exists()


def test_linux_backup_manifest_blocks_success_without_dcc_manifest(tmp_path: Path) -> None:
    module = _load_linux_module()
    config, _app_dir, backup_points_root = _base_config(tmp_path)
    config["environment"] = "test"
    config["servers"]["production"]["host"] = "172.30.30.58"
    backup_root = backup_points_root / "20260526-220000"
    for relative in [
        "deploy",
        "mysql",
        "objects",
    ]:
        (backup_root / relative).mkdir(parents=True)
    (backup_root / "deploy" / "docker-compose.yml").write_text("services: {}\n", encoding="utf-8")
    (backup_root / "deploy" / "runtime.env").write_text(
        "IMAGE_TAG=release-v2\nBACKEND_HOST_PORT=49123\nFRONTEND_HOST_PORT=18099\n",
        encoding="utf-8",
    )
    (backup_root / "deploy" / "image-tag.txt").write_text("release-v2\n", encoding="utf-8")
    (backup_root / "mysql" / "ruoyi-vue-pro.sql.gz").write_bytes(b"dump")
    (backup_root / "objects" / "manifest-object-inventory.json").write_text(
        '{"mode":"incremental-manifest","bucket":"yudao","objectStoreRoot":"/mnt/nas/Backup/BackupPackage/object-store","objects":[]}',
        encoding="utf-8",
    )
    module.write_checksums(backup_root)

    with pytest.raises(module.BackupOpsError) as exc_info:
        module.write_manifest(backup_root, "20260526-220000", "manual", config, "release-v2", 49123, 18099)

    assert exc_info.value.code == "INTBK-6001"
    assert exc_info.value.status == "blocked"
    assert "dcc-backup-manifest.json" in exc_info.value.message


def test_linux_backup_manifest_blocks_success_without_test_target_proof(tmp_path: Path) -> None:
    module = _load_linux_module()
    config, _app_dir, backup_points_root = _base_config(tmp_path)
    config["environment"] = "prod"
    config["servers"]["production"]["host"] = "172.30.30.57"
    backup_root = backup_points_root / "20260526-220000"
    for relative in [
        "deploy",
        "mysql",
        "objects",
    ]:
        (backup_root / relative).mkdir(parents=True)
    (backup_root / "deploy" / "docker-compose.yml").write_text("services: {}\n", encoding="utf-8")
    (backup_root / "deploy" / "runtime.env").write_text(
        "IMAGE_TAG=release-v2\nBACKEND_HOST_PORT=49123\nFRONTEND_HOST_PORT=18099\n",
        encoding="utf-8",
    )
    (backup_root / "deploy" / "image-tag.txt").write_text("release-v2\n", encoding="utf-8")
    (backup_root / "mysql" / "ruoyi-vue-pro.sql.gz").write_bytes(b"dump")
    (backup_root / "objects" / "manifest-object-inventory.json").write_text(
        '{"mode":"incremental-manifest","bucket":"yudao","objectStoreRoot":"/mnt/nas/Backup/BackupPackage/object-store","objects":[]}',
        encoding="utf-8",
    )
    module.write_checksums(backup_root)

    with pytest.raises(module.BackupOpsError) as exc_info:
        module.write_manifest(backup_root, "20260526-220000", "manual", config, "release-v2", 49123, 18099)

    assert exc_info.value.code == "INTBK-1003"
    assert "targetEnvironment=test" in exc_info.value.message
    assert "172.30.30.58" in exc_info.value.message


def test_linux_target_environment_test_projects_restore_data_to_test_runtime(tmp_path: Path) -> None:
    module = _load_linux_module()
    config, _app_dir, _backup_points_root = _base_config(tmp_path)
    config["servers"]["test"].update(
        {
            "host": "172.30.30.58",
            "runtimeDir": "/opt/intruoyi/runtime",
            "tmpRoot": "/opt/intruoyi/ops/backup/tmp",
        }
    )

    projected = module.project_target_environment(config, "restore-data", "test")

    assert projected["environment"] == "test"
    assert projected["servers"]["production"]["host"] == "172.30.30.58"
    assert projected["servers"]["production"]["appDir"] == "/opt/intruoyi/runtime"
    assert projected["servers"]["production"]["tmpRoot"] == "/opt/intruoyi/ops/backup/tmp"
    assert projected["servers"]["test"]["backupPointsRoot"] == config["servers"]["test"]["backupPointsRoot"]
    assert projected["containers"]["minio"] == "ragflow_compose-minio-1"


def test_linux_target_environment_backup_projects_restore_data_to_backup_runtime(tmp_path: Path) -> None:
    module = _load_linux_module()
    config, _app_dir, _backup_points_root = _base_config(tmp_path)
    config["servers"]["backup"].update(
        {
            "host": "172.30.30.59",
            "runtimeDir": "/opt/intruoyi/runtime",
            "tmpRoot": "/opt/intruoyi/ops/backup/tmp",
        }
    )

    projected = module.project_target_environment(config, "restore-data", "backup")

    assert projected["environment"] == "backup"
    assert projected["servers"]["production"]["host"] == "172.30.30.59"
    assert projected["servers"]["production"]["appDir"] == "/opt/intruoyi/runtime"
    assert projected["servers"]["production"]["tmpRoot"] == "/opt/intruoyi/ops/backup/tmp"
    assert projected["servers"]["test"]["backupPointsRoot"] == config["servers"]["test"]["backupPointsRoot"]
    assert projected["containers"]["minio"] == "intruoyi-minio"


def test_linux_restore_data_forbids_production_target_environment(tmp_path: Path) -> None:
    module = _load_linux_module()
    config, _app_dir, _backup_points_root = _base_config(tmp_path)

    with pytest.raises(module.BackupOpsError) as exc_info:
        module.project_target_environment(config, "restore-data", "prod")

    assert exc_info.value.status == "blocked"
    assert "restore-data only supports --target-environment test or backup" in exc_info.value.message


@pytest.mark.parametrize("mode", ["backup-now", "backup-scheduled"])
def test_linux_backup_modes_require_test_target_environment(tmp_path: Path, mode: str) -> None:
    module = _load_linux_module()
    config, _app_dir, _backup_points_root = _base_config(tmp_path)

    with pytest.raises(module.BackupOpsError) as exc_info:
        module.project_target_environment(config, mode, "prod")

    assert exc_info.value.status == "blocked"
    assert "backup-now and backup-scheduled require --target-environment test" in exc_info.value.message


def test_linux_backup_scheduled_projects_to_test_runtime(tmp_path: Path) -> None:
    module = _load_linux_module()
    config, _app_dir, _backup_points_root = _base_config(tmp_path)
    config["servers"]["test"].update(
        {
            "host": "172.30.30.58",
            "runtimeDir": "/opt/intruoyi/runtime",
            "tmpRoot": "/opt/intruoyi/ops/backup/tmp",
        }
    )

    projected = module.project_target_environment(config, "backup-scheduled", "test")

    assert projected["environment"] == "test"
    assert projected["servers"]["production"]["host"] == "172.30.30.58"


def test_linux_target_environment_projects_rollback_app(tmp_path: Path) -> None:
    module = _load_linux_module()
    config, _app_dir, _backup_points_root = _base_config(tmp_path)
    config["servers"]["test"].update(
        {
            "host": "172.30.30.58",
            "runtimeDir": "/opt/intruoyi/runtime",
            "tmpRoot": "/opt/intruoyi/ops/backup/tmp",
        }
    )

    projected = module.project_target_environment(config, "rollback-app", "test")

    assert projected["environment"] == "test"
    assert projected["servers"]["production"]["host"] == "172.30.30.58"
    assert projected["servers"]["production"]["appDir"] == "/opt/intruoyi/runtime"
    assert projected["servers"]["production"]["tmpRoot"] == "/opt/intruoyi/ops/backup/tmp"


def test_linux_restore_data_requires_explicit_selected_backup_id_before_restore_actions(tmp_path: Path) -> None:
    module = _load_linux_module()
    config, app_dir, backup_points_root = _base_config(tmp_path)
    config["environment"] = "test"
    _write_runtime_env(app_dir, backend_port="49123", frontend_port="18099")
    backup_root = backup_points_root / "20260526-220000"
    (backup_root / "mysql").mkdir(parents=True)
    (backup_root / "mysql" / "ruoyi-vue-pro.sql.gz").write_bytes(b"not-a-real-dump")
    (backup_root / "manifest").mkdir()
    (backup_root / "manifest" / "manifest.json").write_text(
        json.dumps(
            {
                "status": "success",
                "validation": {
                    "mysqlDumpCreated": True,
                    "objectBackupCreated": True,
                    "checksumsGenerated": True,
                },
            }
        ),
        encoding="utf-8",
    )
    runner = _NoHighRiskRunner()

    with pytest.raises(module.BackupOpsError) as exc_info:
        module.restore_data(config, runner, "")

    assert exc_info.value.status == "blocked"
    assert "selected_backup_id" in exc_info.value.message
    assert runner.calls == []


def test_linux_restore_data_blocks_manifest_without_recovery_set_before_actions(tmp_path: Path) -> None:
    module = _load_linux_module()
    config, app_dir, backup_points_root = _base_config(tmp_path)
    config["environment"] = "test"
    config["containers"]["redis"] = "intruoyi-redis"
    _write_runtime_env(app_dir, backend_port="49123", frontend_port="18099")
    backup_root = backup_points_root / "20260526-220000"
    (backup_root / "mysql").mkdir(parents=True)
    (backup_root / "objects" / "yudao").mkdir(parents=True)
    (backup_root / "manifest").mkdir(parents=True)
    (backup_root / "mysql" / "ruoyi-vue-pro.sql.gz").write_bytes(b"not-a-real-dump")
    (backup_root / "manifest" / "manifest.json").write_text(
        json.dumps({"schemaVersion": "v1", "status": "success"}),
        encoding="utf-8",
    )
    (backup_root / "manifest" / "checksums.txt").write_text("abc  deploy/runtime.env\n", encoding="utf-8")
    runner = _NoHighRiskRunner()

    with pytest.raises(module.BackupOpsError) as exc_info:
        module.restore_data(config, runner, "20260526-220000")

    assert exc_info.value.status == "blocked"
    assert "recoverySet" in exc_info.value.message
    assert runner.calls == []


def test_linux_restore_data_blocks_program_image_mismatch_before_actions(tmp_path: Path) -> None:
    module = _load_linux_module()
    config, app_dir, backup_points_root = _base_config(tmp_path)
    config["environment"] = "test"
    _write_runtime_env(app_dir, backend_port="49123", frontend_port="18099")
    backup_id = "20260526-220000"
    _write_restore_backup(backup_points_root / backup_id, backup_id=backup_id, image_tag="release-from-backup")
    runner = _NoHighRiskRunner()

    with pytest.raises(module.BackupOpsError) as exc_info:
        module.restore_data(config, runner, backup_id)

    assert exc_info.value.status == "blocked"
    assert "recoverySet.program.imageTag" in exc_info.value.message
    assert "release-from-backup" in exc_info.value.message
    assert "release-current" in exc_info.value.message
    assert runner.calls == []
    assert not (tmp_path / "tmp" / backup_id / "pre-restore").exists()


def test_linux_restore_data_blocks_unverified_backup_before_actions(tmp_path: Path) -> None:
    module = _load_linux_module()
    config, app_dir, backup_points_root = _base_config(tmp_path)
    config["environment"] = "test"
    _write_runtime_env(app_dir, backend_port="49123", frontend_port="18099")
    backup_id = "20260526-220000"
    _write_restore_backup(
        backup_points_root / backup_id,
        backup_id=backup_id,
        image_tag="release-current",
        rehearsal_status="unverified",
        last_rehearsed_at=None,
    )
    runner = _NoHighRiskRunner()

    with pytest.raises(module.BackupOpsError) as exc_info:
        module.restore_data(config, runner, backup_id)

    assert exc_info.value.status == "blocked"
    assert "rehearsalStatus" in exc_info.value.message
    assert "PASSED" in exc_info.value.message
    assert runner.calls == []
    assert not (tmp_path / "tmp" / backup_id / "pre-restore").exists()


def test_linux_restore_data_blocks_manifest_without_backup_strategy_before_actions(tmp_path: Path) -> None:
    module = _load_linux_module()
    config, app_dir, backup_points_root = _base_config(tmp_path)
    config["environment"] = "test"
    _write_runtime_env(app_dir, backend_port="49123", frontend_port="18099")
    backup_id = "20260526-220000"
    _write_restore_backup(
        backup_points_root / backup_id,
        backup_id=backup_id,
        image_tag="release-current",
        include_backup_strategy=False,
    )
    runner = _NoHighRiskRunner()

    with pytest.raises(module.BackupOpsError) as exc_info:
        module.restore_data(config, runner, backup_id)

    assert exc_info.value.status == "blocked"
    assert "backupStrategy" in exc_info.value.message
    assert runner.calls == []
    assert not (tmp_path / "tmp" / backup_id / "pre-restore").exists()


def test_linux_restore_data_blocks_manifest_without_dcc_backup_manifest_before_actions(tmp_path: Path) -> None:
    module = _load_linux_module()
    config, app_dir, backup_points_root = _base_config(tmp_path)
    config["environment"] = "test"
    _write_runtime_env(app_dir, backend_port="49123", frontend_port="18099")
    backup_id = "20260526-220000"
    _write_restore_backup(
        backup_points_root / backup_id,
        backup_id=backup_id,
        image_tag="release-current",
        include_dcc_manifest=False,
    )
    runner = _NoHighRiskRunner()

    with pytest.raises(module.BackupOpsError) as exc_info:
        module.restore_data(config, runner, backup_id)

    assert exc_info.value.status == "blocked"
    assert "recoverySet.dcc.manifestPath" in exc_info.value.message
    assert runner.calls == []
    assert not (tmp_path / "tmp" / backup_id / "pre-restore").exists()


def test_linux_restore_data_blocks_invalid_dcc_backup_manifest_before_actions(tmp_path: Path) -> None:
    module = _load_linux_module()
    config, app_dir, backup_points_root = _base_config(tmp_path)
    config["environment"] = "test"
    _write_runtime_env(app_dir, backend_port="49123", frontend_port="18099")
    backup_id = "20260526-220000"
    backup_root = backup_points_root / backup_id
    _write_restore_backup(
        backup_root,
        backup_id=backup_id,
        image_tag="release-current",
    )
    _write_dcc_backup_manifest(
        backup_root / "manifest",
        backup_id=backup_id,
        manifest=_invalid_deleted_dcc_backup_manifest(backup_id),
    )
    runner = _NoHighRiskRunner()

    with pytest.raises(module.BackupOpsError) as exc_info:
        module.restore_data(config, runner, backup_id)

    assert exc_info.value.status == "blocked"
    assert "dcc_delete_event_missing" in exc_info.value.message
    assert runner.calls == []
    assert not (tmp_path / "tmp" / backup_id / "pre-restore").exists()


@pytest.mark.parametrize(
    ("mutator", "expected_message"),
    [
        (lambda manifest: manifest.__setitem__("schemaVersion", "dcc-backup-manifest-v0"), "schema_version_invalid"),
        (lambda manifest: manifest.__setitem__("chainStatus", "INCOMPLETE"), "chain_status_incomplete"),
        (lambda manifest: manifest.__setitem__("fullBaseline", {"restorePointId": "B1", "checksum": "not-sha256"}), "baseline_checksum_invalid"),
        (
            lambda manifest: (
                manifest.__setitem__("backupMode", "incremental"),
                manifest.__setitem__("previousBackupId", ""),
                manifest.__setitem__("previousRestorePointId", ""),
            ),
            "previous_pointer_missing",
        ),
    ],
)
def test_linux_restore_data_blocks_dcc_chain_preflight_before_actions(
    tmp_path: Path,
    mutator,
    expected_message: str,
) -> None:
    module = _load_linux_module()
    config, app_dir, backup_points_root = _base_config(tmp_path)
    config["environment"] = "test"
    _write_runtime_env(app_dir, backend_port="49123", frontend_port="18099")
    backup_id = "20260526-220000"
    backup_root = backup_points_root / backup_id
    _write_restore_backup(
        backup_root,
        backup_id=backup_id,
        image_tag="release-current",
    )
    manifest = _valid_dcc_backup_manifest(backup_id)
    mutator(manifest)
    _write_dcc_backup_manifest(backup_root / "manifest", backup_id=backup_id, manifest=manifest)
    runner = _NoHighRiskRunner()

    with pytest.raises(module.BackupOpsError) as exc_info:
        module.restore_data(config, runner, backup_id)

    assert exc_info.value.status == "blocked"
    assert expected_message in exc_info.value.message
    assert runner.calls == []
    assert not (tmp_path / "tmp" / backup_id / "pre-restore").exists()


def test_linux_restore_data_blocks_missing_object_store_file_before_actions(tmp_path: Path) -> None:
    module = _load_linux_module()
    config, app_dir, backup_points_root = _base_config(tmp_path)
    config["environment"] = "test"
    _write_runtime_env(app_dir, backend_port="49123", frontend_port="18099")
    backup_id = "20260526-220000"
    backup_root = backup_points_root / backup_id
    _write_restore_backup(
        backup_root,
        backup_id=backup_id,
        image_tag="release-current",
    )
    (backup_root / "objects" / "manifest-object-inventory.json").write_text(
        json.dumps(
            {
                "mode": "incremental-manifest",
                "bucket": "yudao",
                "objects": [
                    {
                        "path": "dcc/original/codex-linux-dcc.docx",
                        "sha256": "sha256:" + "a" * 64,
                        "repositoryKey": "sha256:" + "a" * 64,
                        "repositoryPath": "sha256/aa/" + "a" * 64,
                        "status": "active",
                    }
                ],
            }
        ),
        encoding="utf-8",
    )
    runner = _NoHighRiskRunner()

    with pytest.raises(module.BackupOpsError) as exc_info:
        module.restore_data(config, runner, backup_id)

    assert exc_info.value.status == "blocked"
    assert "Object store file is missing" in exc_info.value.message
    assert runner.calls == []
    assert not (tmp_path / "tmp" / backup_id / "pre-restore").exists()


@pytest.mark.parametrize(
    ("missing_scope", "expected_message"),
    [
        ("redis", "recoverySet.redis.policy"),
        ("configuration", "recoverySet.configuration.manifestPath"),
        ("checksums", "recoverySet.checksums.sha256"),
    ],
)
def test_linux_restore_data_requires_recovery_scope_before_actions(
    tmp_path: Path,
    missing_scope: str,
    expected_message: str,
) -> None:
    module = _load_linux_module()
    config, app_dir, backup_points_root = _base_config(tmp_path)
    config["environment"] = "test"
    _write_runtime_env(app_dir, backend_port="49123", frontend_port="18099")
    backup_id = "20260526-220000"
    _write_restore_backup(
        backup_points_root / backup_id,
        backup_id=backup_id,
        image_tag="release-current",
        include_redis=missing_scope != "redis",
        include_configuration=missing_scope != "configuration",
        include_checksums=missing_scope != "checksums",
    )
    runner = _NoHighRiskRunner()

    with pytest.raises(module.BackupOpsError) as exc_info:
        module.restore_data(config, runner, backup_id)

    assert exc_info.value.status == "blocked"
    assert expected_message in exc_info.value.message
    assert runner.calls == []
    assert not (tmp_path / "tmp" / backup_id / "pre-restore").exists()


def test_linux_restore_data_requires_object_repository_path_before_object_restore(
    tmp_path: Path,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    module = _load_linux_module()
    config, app_dir, backup_points_root = _base_config(tmp_path)
    config["environment"] = "test"
    _write_runtime_env(app_dir, backend_port="49123", frontend_port="18099")
    backup_id = "20260526-220000"
    backup_root = backup_points_root / backup_id
    _write_restore_backup(
        backup_root,
        backup_id=backup_id,
        image_tag="release-current",
    )
    (backup_root / "objects" / "manifest-object-inventory.json").write_text(
        json.dumps(
            {
                "mode": "incremental-manifest",
                "bucket": "yudao",
                "objects": [
                    {
                        "path": "dcc/original/codex-linux-dcc.docx",
                        "sha256": "sha256:" + "a" * 64,
                        "repositoryKey": "sha256:" + "a" * 64,
                        "status": "active",
                    }
                ],
            }
        ),
        encoding="utf-8",
    )
    monkeypatch.setattr(module, "get_minio_creds", lambda _container_name: ("access", "secret"))
    runner = _NoopRunner()

    with pytest.raises(module.BackupOpsError) as exc_info:
        module.restore_data(config, runner, backup_id)

    assert exc_info.value.status == "blocked"
    assert "repositoryPath" in exc_info.value.message


def test_linux_restore_data_use_case_forbids_non_test_environment_before_actions(tmp_path: Path) -> None:
    module = _load_linux_module()
    config, app_dir, backup_points_root = _base_config(tmp_path)
    _write_runtime_env(app_dir, backend_port="49123", frontend_port="18099")
    backup_root = backup_points_root / "20260526-220000"
    (backup_root / "mysql").mkdir(parents=True)
    (backup_root / "mysql" / "ruoyi-vue-pro.sql.gz").write_bytes(b"not-a-real-dump")
    runner = _NoHighRiskRunner()

    with pytest.raises(module.BackupOpsError) as exc_info:
        module.restore_data(config, runner, "20260526-220000")

    assert exc_info.value.status == "blocked"
    assert "test" in exc_info.value.message
    assert runner.calls == []


def test_linux_rollback_app_requires_explicit_selected_image_tag_before_rollback_actions(tmp_path: Path) -> None:
    module = _load_linux_module()
    config, app_dir, backup_points_root = _base_config(tmp_path)
    _write_runtime_env(app_dir, backend_port="49123", frontend_port="18099")
    backup_root = backup_points_root / "20260526-220000" / "deploy"
    backup_root.mkdir(parents=True)
    (backup_root / "image-tag.txt").write_text("release-previous\n", encoding="utf-8")
    runner = _NoHighRiskRunner()
    env_before = (app_dir / ".env").read_text(encoding="utf-8")

    with pytest.raises(module.BackupOpsError) as exc_info:
        module.rollback_app(config, runner, "")

    assert exc_info.value.status == "blocked"
    assert "selected_image_tag" in exc_info.value.message
    assert (app_dir / ".env").read_text(encoding="utf-8") == env_before
    assert runner.calls == []


def test_linux_rollback_app_requires_release_package_root_before_mutation(tmp_path: Path) -> None:
    module = _load_linux_module()
    config, app_dir, _backup_points_root = _base_config(tmp_path)
    config["environment"] = "test"
    config["servers"]["test"].pop("releasePackagesRoot", None)
    _write_runtime_env(app_dir, backend_port="49123", frontend_port="18099")
    runner = _NoHighRiskRunner()
    env_before = (app_dir / ".env").read_text(encoding="utf-8")

    with pytest.raises(module.BackupOpsError) as exc_info:
        module.rollback_app(config, runner, "release-previous")

    assert exc_info.value.status == "blocked"
    assert "releasePackagesRoot" in exc_info.value.message
    assert (app_dir / ".env").read_text(encoding="utf-8") == env_before
    assert runner.calls == []


def test_linux_rehearsal_requires_explicit_selected_backup_id_for_manual_operator(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> None:
    module = _load_linux_module()
    config, _app_dir, _backup_points_root = _base_config(tmp_path)
    runner = _NoHighRiskRunner()

    def fail_if_latest_is_selected(_backup_points_root: Path) -> str:
        raise AssertionError("manual rehearsal must not auto-select the latest backup point")

    monkeypatch.setattr(module, "pick_latest_backup", fail_if_latest_is_selected)

    with pytest.raises(module.BackupOpsError) as exc_info:
        module.rehearsal(config, runner, "", "worker-a")

    assert exc_info.value.status == "blocked"
    assert "selected_backup_id" in exc_info.value.message
    assert "scheduler" in exc_info.value.message
    assert runner.calls == []
