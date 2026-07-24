from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SERVICE_DIR = (
    ROOT
    / "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord"
)


def read_java(name: str) -> str:
    path = SERVICE_DIR / name
    assert path.exists(), f"missing Java contract file: {path}"
    return path.read_text(encoding="utf-8")


def test_common_status_contract_declares_required_states():
    source = read_java("MesProEdhrCommonStatus.java")
    for status in [
        "DRAFT",
        "PRECHECK_FAILED",
        "BLOCKED",
        "PENDING_APPROVAL",
        "COMPLETED",
        "VOIDED",
    ]:
        assert status in source
    assert "Set<String> names()" in source
    assert "isTerminal" in source
    assert "isBlocking" in source


def test_contract_error_codes_are_explicit_and_fail_fast_oriented():
    source = read_java("MesProEdhrContractErrorCodeConstants.java")
    for constant in [
        "PRO_EDHR_CONTRACT_IDEMPOTENCY_KEY_REQUIRED",
        "PRO_EDHR_CONTRACT_IDEMPOTENCY_KEY_INVALID",
        "PRO_EDHR_CONTRACT_EVIDENCE_HASH_INPUT_REQUIRED",
        "PRO_EDHR_CONTRACT_AUDIT_EVENT_FIELD_REQUIRED",
    ]:
        assert constant in source
    assert "new ErrorCode" in source
    assert "eDHR 公共契约" in source


def test_idempotency_evidence_hash_and_audit_event_helpers_exist():
    idempotency = read_java("MesProEdhrIdempotencySupport.java")
    assert "requireIdempotencyKey" in idempotency
    assert "PRO_EDHR_CONTRACT_IDEMPOTENCY_KEY_REQUIRED" in idempotency
    assert "PRO_EDHR_CONTRACT_IDEMPOTENCY_KEY_INVALID" in idempotency
    assert "return trimmed" in idempotency

    evidence_hash = read_java("MesProEdhrEvidenceHashSupport.java")
    assert "sha256" in evidence_hash
    assert "MessageDigest.getInstance(\"SHA-256\")" in evidence_hash
    assert "StandardCharsets.UTF_8" in evidence_hash
    assert "PRO_EDHR_CONTRACT_EVIDENCE_HASH_INPUT_REQUIRED" in evidence_hash

    audit = read_java("MesProEdhrAuditEventContract.java")
    for field in [
        "sourceModule",
        "sourceObjectId",
        "action",
        "permissionCode",
        "result",
        "reason",
        "evidenceHash",
        "idempotencyKey",
    ]:
        assert f"private String {field};" in audit
    assert "validateRequiredFields" in audit
    assert "MesProEdhrIdempotencySupport.requireIdempotencyKey" in audit


def test_no_silent_default_success_or_empty_hash():
    combined = "\n".join(
        read_java(name)
        for name in [
            "MesProEdhrIdempotencySupport.java",
            "MesProEdhrEvidenceHashSupport.java",
            "MesProEdhrAuditEventContract.java",
        ]
    )
    forbidden = [
        "catch (Exception",
        "return \"\"",
        "return null",
        "DEFAULT_SUCCESS",
        "UNKNOWN_SUCCESS",
    ]
    for token in forbidden:
        assert token not in combined
