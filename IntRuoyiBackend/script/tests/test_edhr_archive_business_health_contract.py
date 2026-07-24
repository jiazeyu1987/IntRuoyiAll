import re
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]

COLLECTOR = REPO_ROOT / (
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/"
    "service/pro/batchrecord/MesProBatchRecordExecutionArchiveBusinessHealthCollector.java"
)


def _read(path: Path) -> str:
    if not path.exists():
        raise AssertionError(
            "production collector source is missing: "
            f"{path.relative_to(REPO_ROOT)}"
        )
    return path.read_text(encoding="utf-8")


def _collector_source() -> str:
    return _read(COLLECTOR)


def test_archive_business_health_collector_exists_and_implements_runtime_contract() -> None:
    source = _collector_source()
    missing: list[str] = []

    if "RuntimeOpsBusinessHealthCollector" not in source:
        missing.append("implements RuntimeOpsBusinessHealthCollector")
    if not re.search(r"class\s+MesProBatchRecordExecutionArchiveBusinessHealthCollector\b", source):
        missing.append("collector class with the planned name")
    if not re.search(r"@(Component|Service)\b", source):
        missing.append("Spring bean annotation")
    if "collect()" not in source:
        missing.append("collect() implementation")
    if "edhr-archive-integrity" not in source:
        missing.append("health code edhr-archive-integrity")
    if "eDHR 归档完整性" not in source:
        missing.append("health name eDHR 归档完整性")

    assert not missing, "collector runtime contract is incomplete:\n" + "\n".join(f"- {item}" for item in missing)


def test_archive_business_health_collector_is_readonly_db_only() -> None:
    source = _collector_source()
    missing: list[str] = []
    forbidden = [
        ".insert(",
        ".update(",
        ".updateById(",
        ".delete(",
        ".deleteById(",
        "generateExecutionArchive(",
        "downloadExecutionArchive(",
        "createFile",
        "deleteFile",
        "getFileContent(",
    ]

    if "MesProBatchRecordExecutionArchiveMapper" not in source:
        missing.append("archive mapper read source")
    if "MesProBatchRecordExecutionArchiveEventMapper" not in source:
        missing.append("archive event mapper read source")
    if "readOnly = true" not in source:
        missing.append("@Transactional(readOnly = true) boundary")
    if not re.search(r"\bselect(List|One|Count|Page)?\b", source):
        missing.append("select-only mapper reads")

    present_forbidden = [marker for marker in forbidden if marker in source]
    assert not missing, "collector read-only DB contract is incomplete:\n" + "\n".join(f"- {item}" for item in missing)
    assert not present_forbidden, (
        "collector must not mutate archives, call archive side effects, or read files:\n"
        + "\n".join(f"- {item}" for item in present_forbidden)
    )


def test_archive_business_health_collector_has_no_storage_retention_api_or_s3_coupling() -> None:
    source = _collector_source()
    forbidden = [
        "FileService",
        "StorageRetentionEvidence",
        "StorageRetentionPolicy",
        "S3FileClient",
        "GetObjectRetentionRequest",
        "PutObjectRetentionRequest",
        "GetObjectLegalHoldRequest",
        "PutObjectLegalHoldRequest",
        "getFileContentWithStorageRetention",
        "requireStorageRetentionEvidence",
        "createFileWithStorageRetention",
    ]

    present = [marker for marker in forbidden if marker in source]

    assert not present, (
        "collector must stay decoupled from FileService, StorageRetention types, and S3 retention APIs:\n"
        + "\n".join(f"- {item}" for item in present)
    )


def test_archive_business_health_collector_separates_seal_event_from_storage_retention_source_event() -> None:
    source = _collector_source()
    missing: list[str] = []

    if "ARCHIVE_SEAL" not in source:
        missing.append("ARCHIVE_SEAL event check for SEALED archive seal evidence")
    if "storageRetention" not in source:
        missing.append("append-only metadata_json.storageRetention source event check")
    if "GENERATE_SUCCESS" not in source:
        missing.append("current known storageRetention source marker GENERATE_SUCCESS")
    if "metadataJson" not in source and "getMetadataJson" not in source:
        missing.append("metadataJson parsing from archive event rows")

    seal_requires_storage = re.search(
        r"ARCHIVE_SEAL[\s\S]{0,180}(metadataJson|getMetadataJson)[\s\S]{0,180}storageRetention",
        source,
    )

    assert not missing, "collector event responsibility contract is incomplete:\n" + "\n".join(f"- {item}" for item in missing)
    assert seal_requires_storage is None, (
        "ARCHIVE_SEAL must prove seal evidence only; do not require ARCHIVE_SEAL.metadataJson.storageRetention"
    )


def test_archive_business_health_collector_reports_pass_warn_and_blocked_states() -> None:
    source = _collector_source()
    missing: list[str] = []

    for marker in ["PASS", "WARN", "BLOCKED"]:
        if marker not in source:
            missing.append(f"RuntimeOpsInspectionStatus.{marker} result")
    for marker in ["sealed", "failed", "fileId", "sha256", "objectVersionId", "retainUntil", "verifiedAt"]:
        if marker not in source:
            missing.append(f"evidence/validation marker {marker}")

    assert not missing, "collector status and evidence contract is incomplete:\n" + "\n".join(f"- {item}" for item in missing)
