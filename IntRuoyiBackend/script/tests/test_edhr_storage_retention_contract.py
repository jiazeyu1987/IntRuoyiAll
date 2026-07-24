import re
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]

FILE_CLIENT = REPO_ROOT / (
    "yudao-module-infra/src/main/java/cn/iocoder/yudao/module/infra/"
    "framework/file/core/client/FileClient.java"
)
FILE_SERVICE = REPO_ROOT / (
    "yudao-module-infra/src/main/java/cn/iocoder/yudao/module/infra/"
    "service/file/FileService.java"
)
S3_FILE_CLIENT = REPO_ROOT / (
    "yudao-module-infra/src/main/java/cn/iocoder/yudao/module/infra/"
    "framework/file/core/client/s3/S3FileClient.java"
)
S3_FILE_CLIENT_CONFIG = REPO_ROOT / (
    "yudao-module-infra/src/main/java/cn/iocoder/yudao/module/infra/"
    "framework/file/core/client/s3/S3FileClientConfig.java"
)
MES_ARCHIVE_SERVICE = REPO_ROOT / (
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/"
    "service/pro/batchrecord/MesProBatchRecordExecutionArchiveServiceImpl.java"
)
MES_ARCHIVE_ERROR_CODES = REPO_ROOT / (
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/"
    "service/pro/batchrecord/MesProBatchRecordExecutionArchiveErrorCodeConstants.java"
)


def _read(path: Path) -> str:
    if not path.exists():
        raise AssertionError(f"required contract source file is missing: {path.relative_to(REPO_ROOT)}")
    return path.read_text(encoding="utf-8")


def _assert_no_missing(path: Path, missing: list[str]) -> None:
    assert not missing, (
        f"{path.relative_to(REPO_ROOT)} is missing storage retention contract pieces:\n"
        + "\n".join(f"- {item}" for item in missing)
    )


def _method_names(source: str) -> set[str]:
    return set(re.findall(r"\b([A-Za-z]\w*)\s*\([^;{}]*\)\s*(?:throws\s+[\w.,\s]+)?(?:;|\{)", source))


def test_file_client_exposes_explicit_storage_retention_evidence_contract() -> None:
    source = _read(FILE_CLIENT)
    methods = _method_names(source)
    missing: list[str] = []

    if "StorageRetentionEvidence" not in source:
        missing.append("a typed StorageRetentionEvidence return/parameter contract")
    if not any(re.search(r"(verify|read|require).*(Storage)?Retention", name) for name in methods):
        missing.append("a verify/read/require retention evidence API on FileClient")
    if "getContentWithStorageRetention" not in methods:
        missing.append("a content read API bound to storage retention evidence and object version id")
    if not any(re.search(r"upload.*(Storage)?Retention|create.*(Storage)?Retention", name) for name in methods):
        missing.append("an upload/create path that binds retention evidence to the stored object")
    if not re.search(
        r"default\s+byte\[\]\s+getContentWithStorageRetention[\s\S]{0,300}UnsupportedOperationException",
        source,
    ):
        missing.append("ordinary FileClient implementations must fail fast for version-bound content reads")
    if not {"upload", "delete", "getContent"}.issubset(methods):
        missing.append("the legacy FileClient methods are not readable, so this static contract cannot compare the surface")

    _assert_no_missing(FILE_CLIENT, missing)


def test_file_service_exposes_create_or_verify_api_with_retention_evidence() -> None:
    source = _read(FILE_SERVICE)
    methods = _method_names(source)
    missing: list[str] = []

    if "StorageRetentionEvidence" not in source:
        missing.append("a StorageRetentionEvidence type exposed at the FileService boundary")
    if not any(re.search(r"create.*(Storage)?Retention|create.*Retention.*Evidence", name) for name in methods):
        missing.append("a create-file API that returns or records storage retention evidence")
    if not any(re.search(r"(verify|read|require).*(Storage)?Retention", name) for name in methods):
        missing.append("a verify/read/require storage retention evidence API for existing files")
    if "getFileContentWithStorageRetention" not in methods:
        missing.append("a fileId + StorageRetentionPolicy content read API for the same protected object version")

    _assert_no_missing(FILE_SERVICE, missing)


def test_s3_file_client_uses_object_lock_retention_legal_hold_and_version_id() -> None:
    source = _read(S3_FILE_CLIENT)
    missing: list[str] = []

    for marker in [
        "GetObjectRetentionRequest",
        "PutObjectRetentionRequest",
        "GetObjectLegalHoldRequest",
        "PutObjectLegalHoldRequest",
    ]:
        if marker not in source:
            missing.append(f"AWS SDK Object Lock API marker `{marker}`")
    for call in [
        "client.getObjectRetention",
        "client.putObjectRetention",
        "client.getObjectLegalHold",
        "client.putObjectLegalHold",
    ]:
        if call not in source:
            missing.append(f"S3 Object Lock client call `{call}(...)`")
    if ".versionId(" not in source:
        missing.append("Object Lock readback requests scoped with `.versionId(...)`")
    if "versionId()" not in source:
        missing.append("capturing uploaded object `versionId()` as retention evidence")
    version_bound_get = re.search(
        r"getContentWithStorageRetention[\s\S]*?GetObjectRequest\.builder\(\)[\s\S]*?\.versionId\(",
        source,
    )
    if not version_bound_get:
        missing.append("version-bound content read must call GetObjectRequest.versionId(policy.getObjectVersionId())")
    if not re.search(
        r"getContentWithStorageRetention[\s\S]*?requireStorageRetentionEvidence\(",
        source,
    ):
        missing.append("version-bound content read must verify storage retention evidence before reading bytes")
    if not re.search(
        r"getContentWithStorageRetention[\s\S]*?response\(\)\.versionId\(\)[\s\S]*?IllegalStateException",
        source,
    ):
        missing.append("version-bound content read must fail if returned GetObject versionId mismatches policy")
    for marker in [
        "requireStorageRetentionPolicy",
        "Object Lock 必须显式启用",
        "legalHoldRequired 不能为空",
    ]:
        if marker not in source:
            missing.append(f"eDHR retention API fail-fast marker `{marker}`")

    _assert_no_missing(S3_FILE_CLIENT, missing)


def test_s3_file_client_config_declares_required_retention_policy_inputs() -> None:
    source = _read(S3_FILE_CLIENT_CONFIG)
    missing: list[str] = []

    if not re.search(r"\b(?:private|record)\b[^;\n]*(StorageRetentionPolicy|RetentionPolicy)", source):
        missing.append("a typed retention policy field such as StorageRetentionPolicy")
    if not re.search(r"\bprivate\s+\w+\s+.*retention.*mode\b", source, re.IGNORECASE):
        missing.append("required retention mode config")
    if not re.search(r"\bprivate\s+\w+\s+.*retention.*(days|duration|until|period)\b", source, re.IGNORECASE):
        missing.append("required retention duration/retain-until config")
    if not re.search(r"\bprivate\s+\w+\s+.*legal.*hold", source, re.IGNORECASE):
        missing.append("required legal hold config")
    if not re.search(r"AssertTrue[\s\S]*(retention|legal|objectLock)", source, re.IGNORECASE):
        missing.append("cross-field validation for Object Lock retention/legal hold policy")
    if not re.search(r"!\s*Boolean\.TRUE\.equals\(objectLockRequired\)[\s\S]{0,120}return\s+true", source):
        missing.append("ordinary S3 config must pass validation when Object Lock is not enabled")
    if not re.search(r"Boolean\.TRUE\.equals\(objectLockRequired\)[\s\S]*retentionMode[\s\S]*retentionDays[\s\S]*retentionRetainUntil[\s\S]*legalHoldRequired", source):
        missing.append("Object Lock enabled config must require retention mode, duration/retain-until, and legal hold")

    _assert_no_missing(S3_FILE_CLIENT_CONFIG, missing)


def test_mes_archive_service_gates_seal_and_download_on_storage_retention_evidence() -> None:
    source = _read(MES_ARCHIVE_SERVICE)
    lower_source = source.lower()
    missing: list[str] = []

    if "storageretention" not in lower_source and "objectlock" not in lower_source and "legalhold" not in lower_source:
        missing.append("storage retention/Object Lock/legal hold gate code in the archive service")

    persist_index = source.find("persistArchiveFile")
    seal_index = source.find("setArchiveStatus(ARCHIVE_STATUS_SEALED)")
    gate_match = re.search(
        r"(verify|require|read|gate)\w*(Storage)?Retention|"
        r"(storageRetention|objectLock|legalHold)\w*(Gate|Evidence|Verification)",
        source[persist_index:seal_index] if persist_index != -1 and seal_index != -1 and persist_index < seal_index else "",
    )
    if persist_index == -1 or seal_index == -1 or gate_match is None:
        missing.append("a storage retention evidence gate after file persistence and before SEALED status")

    download_match = re.search(
        r"downloadExecutionArchive[\s\S]*?(EVENT_DOWNLOAD_SUCCESS|return respVO)",
        source,
    )
    download_body = download_match.group(0) if download_match else ""
    if not re.search(r"(verify|require|read|gate)\w*(Storage)?Retention|storageRetention|objectLock|legalHold", download_body):
        missing.append("download/reverify path re-reading storage retention evidence before successful download")
    if "getFileContentWithStorageRetention" not in download_body:
        missing.append("download/reverify path must read archive bytes through the version-bound FileService API")
    if "getFileContent(" in download_body:
        missing.append("download/reverify path must not fallback to ordinary getFileContent(...)")
    if "metadataJson" not in source or not re.search(r"recordEvent\([\s\S]*(storageRetention|objectLock|legalHold)", source):
        missing.append("append-only archive event metadata recording the storage retention evidence")

    _assert_no_missing(MES_ARCHIVE_SERVICE, missing)


def test_mes_archive_service_preserves_snapshot_and_retention_metadata_when_sealing() -> None:
    source = _read(MES_ARCHIVE_SERVICE)
    missing: list[str] = []

    seal_update_match = re.search(
        r"archive\.setArchiveStatus\(ARCHIVE_STATUS_SEALED\)[\s\S]*?archiveMapper\.updateById\(archive\);",
        source,
    )
    seal_update = seal_update_match.group(0) if seal_update_match else ""
    if not seal_update:
        missing.append("the final SEALED archive update block")
    if ".setApprovalSnapshotId(sourceData.approvalSnapshotId)" not in seal_update:
        missing.append("the final SEALED update must explicitly preserve approvalSnapshotId from SourceData")
    if ".setApprovalSnapshotHash(sourceData.approvalSnapshotHash)" not in seal_update:
        missing.append("the final SEALED update must explicitly preserve approvalSnapshotHash from SourceData")

    success_event_match = re.search(
        r"String\s+storageRetentionMetadata\s*=\s*storageRetentionMetadataJson\("
        r"[\s\S]*?recordEvent\([\s\S]*?EVENT_GENERATE_SUCCESS[\s\S]*?storageRetentionMetadata",
        source,
    )
    if not success_event_match:
        missing.append(
            "GENERATE_SUCCESS must build a named non-secret storageRetentionMetadata value "
            "and pass that value to the append-only event"
        )

    _assert_no_missing(MES_ARCHIVE_SERVICE, missing)


def test_mes_archive_error_codes_include_storage_retention_gate_failure() -> None:
    source = _read(MES_ARCHIVE_ERROR_CODES)
    missing: list[str] = []

    if not re.search(r"PRO_BATCH_RECORD_ARCHIVE_.*(STORAGE_RETENTION|OBJECT_LOCK|LEGAL_HOLD).*=", source):
        missing.append("a dedicated storage retention/Object Lock/legal hold archive gate error code constant")
    if not re.search(r"new ErrorCode\([^)]*(存储侧|Retention|Object Lock|legal hold|Legal Hold)", source):
        missing.append("a user-facing error message that names storage retention/Object Lock/legal hold failure")

    _assert_no_missing(MES_ARCHIVE_ERROR_CODES, missing)
