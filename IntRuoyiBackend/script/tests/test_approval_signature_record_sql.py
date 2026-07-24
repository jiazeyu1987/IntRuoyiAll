from pathlib import Path
import re


ROOT = Path(__file__).resolve().parents[2]
MIGRATION = ROOT / "sql/mysql/20260715_bpm_approval_signature_record.sql"
TRACEABILITY_MIGRATION = ROOT / "sql/mysql/20260717_bpm_approval_signature_traceability.sql"
BPM_TEST_SCHEMA = ROOT / "yudao-module-bpm/src/test/resources/sql/create_tables.sql"
SIGNATURE_RECORD_MAPPER = ROOT / (
    "yudao-module-dcc/src/main/resources/mapper/signature/governance/"
    "SignatureGovernanceRecordMapper.xml"
)


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def test_bpm_approval_signature_record_migration_exists_and_is_append_only():
    assert MIGRATION.exists(), "approval signature migration must exist"
    text = read(MIGRATION)
    assert "CREATE TABLE IF NOT EXISTS `bpm_approval_signature_record`" in text
    assert "`module_code` varchar(64) NOT NULL" in text
    assert "`source_task_type` varchar(128) NOT NULL" in text
    assert "`source_task_id` varchar(128) DEFAULT NULL" in text
    assert "`signer_user_id` bigint NOT NULL" in text
    assert "`password_verified` bit(1) NOT NULL DEFAULT b'1'" in text
    assert "`signature_image_id` bigint DEFAULT NULL" in text
    assert "`signature_image_version_no` int DEFAULT NULL" in text
    assert "`signature_image_file_id` bigint DEFAULT NULL" in text
    assert "`signature_image_sha256` varchar(128) DEFAULT NULL" in text
    assert "`signature_image_verified_status` varchar(32) DEFAULT NULL" in text
    assert "`signed_at` datetime NOT NULL" in text
    assert "DROP TABLE" not in text.upper()
    assert "DELETE FROM" not in text.upper()


def test_bpm_approval_signature_traceability_migration_is_idempotent_and_non_destructive():
    assert TRACEABILITY_MIGRATION.exists(), "approval signature traceability migration must exist"
    text = read(TRACEABILITY_MIGRATION)
    assert "CREATE PROCEDURE ensure_bpm_signature_column" in text
    for column in [
        "signature_image_id",
        "signature_image_version_no",
        "signature_image_file_id",
        "signature_image_file_url",
        "signature_image_sha256",
        "signature_image_content_type",
        "signature_image_file_size",
        "signature_image_status_snapshot",
        "signature_image_verified_status",
    ]:
        assert f"'{column}'" in text
    assert "DROP TABLE" not in text.upper()
    assert "DELETE FROM" not in text.upper()


def test_bpm_h2_fixture_contains_signature_record_table():
    text = read(BPM_TEST_SCHEMA)
    assert 'CREATE TABLE IF NOT EXISTS "bpm_approval_signature_record"' in text
    for column in [
        '"module_code" varchar(64) NOT NULL',
        '"source_task_type" varchar(128) NOT NULL',
        '"signer_user_id" bigint NOT NULL',
        '"password_verified" bit NOT NULL DEFAULT TRUE',
        '"signature_image_id" bigint DEFAULT NULL',
        '"signature_image_version_no" int DEFAULT NULL',
        '"signature_image_file_id" bigint DEFAULT NULL',
        '"signature_image_sha256" varchar(128) DEFAULT NULL',
        '"signature_image_verified_status" varchar(32) DEFAULT NULL',
        '"signed_at" timestamp NOT NULL',
    ]:
        assert column in text


def test_unified_signature_record_mapper_includes_bpm_review_signatures():
    text = read(SIGNATURE_RECORD_MAPPER)
    assert "FROM bpm_approval_signature_record sig" in text
    assert "WHEN sig.module_code = 'BPM' THEN 'BPM审批'" in text
    assert "WHEN sig.module_code = 'MES_FEEDBACK' THEN '报工审批'" in text
    assert "CONVERT(sig.module_code USING utf8mb4) AS source_code" in text
    assert "sig.signature_image_id AS signature_image_id" in text
    assert "CONVERT(sig.signature_image_sha256 USING utf8mb4) AS signature_image_sha256" in text
    assert "PASSWORD_VERIFIED" in text


def test_unified_signature_record_mapper_routes_dcc_records_to_viewer_detail():
    text = read(SIGNATURE_RECORD_MAPPER)
    assert re.search(
        r"CONCAT\('/dcc/controlled-file/detail/',\s*sig\.controlled_file_id,\s*"
        r"'\?viewer=1&amp;from=signature-governance'\)",
        text,
    )
    assert "CONCAT('/dcc/controlled-file/detail?id=', sig.controlled_file_id)" not in text
