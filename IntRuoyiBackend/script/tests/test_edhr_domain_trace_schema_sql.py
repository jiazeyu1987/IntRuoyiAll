import re
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260528_edhr_domain_trace_schema.sql"
MES_JAVA_ROOT = REPO_ROOT / "yudao-module-mes" / "src" / "main" / "java" / "cn" / "iocoder" / "yudao" / "module" / "mes"


def read_sql() -> str:
    return SQL_PATH.read_text(encoding="utf-8")


def read_mes_java(relative_path: str) -> str:
    return (MES_JAVA_ROOT / relative_path).read_text(encoding="utf-8")


def test_domain_trace_sql_fails_fast_on_required_edhr_tables() -> None:
    text = read_sql()

    for table_name in [
        "mes_pro_batch_record_execution",
        "mes_pro_batch_record_execution_signature",
        "mes_pro_batch_record_approval_snapshot",
        "mes_pro_batch_record_execution_archive",
    ]:
        pattern = (
            r"IF NOT EXISTS\s*\(\s*SELECT 1\s+FROM information_schema\.TABLES"
            r".*?TABLE_SCHEMA\s*=\s*DATABASE\(\)"
            rf".*?TABLE_NAME\s*=\s*'{table_name}'"
            r".*?\)\s*THEN"
            r".*?SIGNAL SQLSTATE '45000'"
            rf".*?{table_name}"
            r".*?END IF;"
        )
        assert re.search(pattern, text, flags=re.DOTALL | re.IGNORECASE), (
            f"{table_name} must fail fast before domain trace schema mutation"
        )

    first_signal = text.index("SIGNAL SQLSTATE '45000'")
    first_mutation = min(
        text.index("ALTER TABLE `mes_pro_batch_record_execution`"),
        text.index("CREATE TABLE IF NOT EXISTS `mes_pro_batch_record_domain_trace_snapshot`"),
    )
    assert first_signal < first_mutation


def test_domain_trace_sql_adds_execution_projection_fields() -> None:
    text = read_sql()

    for column in [
        "`domain_trace_snapshot_id` bigint DEFAULT NULL",
        "`domain_trace_hash` char(64) DEFAULT NULL",
        "`domain_trace_status` varchar(32) DEFAULT NULL",
        "`domain_trace_verified_at` datetime DEFAULT NULL",
    ]:
        assert column in text

    for column_name in [
        "domain_trace_snapshot_id",
        "domain_trace_hash",
        "domain_trace_status",
        "domain_trace_verified_at",
    ]:
        pattern = (
            r"IF NOT EXISTS\s*\(\s*SELECT 1\s+FROM information_schema\.COLUMNS"
            r".*?TABLE_NAME\s*=\s*'mes_pro_batch_record_execution'"
            rf".*?COLUMN_NAME\s*=\s*'{column_name}'"
            r".*?\)\s*THEN"
            r".*?ALTER TABLE `mes_pro_batch_record_execution`"
            r".*?END IF;"
        )
        assert re.search(pattern, text, flags=re.DOTALL | re.IGNORECASE)


def test_domain_trace_snapshot_and_item_tables_are_append_only() -> None:
    text = read_sql()

    assert "CREATE TABLE IF NOT EXISTS `mes_pro_batch_record_domain_trace_snapshot`" in text
    assert "CREATE TABLE IF NOT EXISTS `mes_pro_batch_record_domain_trace_item`" in text

    for column in [
        "`execution_id` bigint NOT NULL",
        "`snapshot_version` varchar(32) NOT NULL",
        "`snapshot_json` longtext NOT NULL",
        "`snapshot_hash` char(64) NOT NULL",
        "`completeness_status` varchar(32) NOT NULL",
        "`blocker_count` int NOT NULL",
        "`verified_by` bigint DEFAULT NULL",
        "`verified_at` datetime NOT NULL",
    ]:
        assert column in text

    for column in [
        "`snapshot_id` bigint NOT NULL",
        "`execution_id` bigint NOT NULL",
        "`item_type` varchar(64) NOT NULL",
        "`item_key` varchar(128) NOT NULL",
        "`source_table` varchar(128) DEFAULT NULL",
        "`source_id` bigint DEFAULT NULL",
        "`source_code` varchar(128) DEFAULT NULL",
        "`source_version` varchar(64) DEFAULT NULL",
        "`snapshot_json` longtext DEFAULT NULL",
        "`snapshot_hash` char(64) DEFAULT NULL",
        "`required_flag` bit(1) NOT NULL DEFAULT b'1'",
        "`status` varchar(32) NOT NULL",
        "`blocker_code` varchar(128) DEFAULT NULL",
        "`blocker_message` varchar(500) DEFAULT NULL",
        "`blocker_reason` varchar(500) DEFAULT NULL",
    ]:
        assert column in text

    for index_name in [
        "uk_domain_trace_snapshot_hash",
        "idx_domain_trace_snapshot_execution",
        "idx_domain_trace_item_execution_type",
        "idx_domain_trace_item_source",
        "idx_domain_trace_item_snapshot",
    ]:
        assert f"`{index_name}`" in text

    for trigger_name in [
        "trg_domain_trace_snapshot_no_update",
        "trg_domain_trace_snapshot_no_delete",
        "trg_domain_trace_item_no_update",
        "trg_domain_trace_item_no_delete",
    ]:
        pattern = (
            rf"CREATE TRIGGER `{trigger_name}`"
            r".*?SIGNAL SQLSTATE '45000'"
        )
        assert re.search(pattern, text, flags=re.DOTALL | re.IGNORECASE), (
            f"{trigger_name} must reject mutation with SIGNAL SQLSTATE '45000'"
        )

    assert not re.search(r"ON\s+DUPLICATE\s+KEY\s+UPDATE", text, flags=re.IGNORECASE)


def test_domain_trace_permissions_are_inserted_under_edhr_parent() -> None:
    text = read_sql()

    for permission in [
        "mes:pro-batch-record-execution:domain-trace-query",
        "mes:pro-batch-record-execution:domain-trace-verify",
    ]:
        insert_pattern = (
            r"INSERT INTO `system_menu`"
            r".*?SELECT\s+9000\d+,\s*'[^']+',\s*'"
            + re.escape(permission)
            + r"'"
            r".*?,\s*3,\s*\d+,\s*900002"
            r".*?WHERE NOT EXISTS\s*\("
            r"\s*SELECT 1 FROM `system_menu`"
            r".*?WHERE `permission`\s*=\s*'"
            + re.escape(permission)
            + r"'"
            r"\s*\)"
        )
        assert re.search(insert_pattern, text, flags=re.DOTALL | re.IGNORECASE)

    assert "Invalid system_tenant_package.menu_ids JSON" in text
    assert "system_tenant_package" in text
    assert "system_role_menu" in text
    assert "tenant_admin" in text


def test_domain_trace_java_contract_exposes_required_fields() -> None:
    execution_do = read_mes_java(
        "dal/dataobject/pro/batchrecord/MesProBatchRecordExecutionDO.java"
    )
    detail_vo = read_mes_java(
        "controller/admin/pro/batchrecord/vo/MesProBatchRecordDomainTraceDetailRespVO.java"
    )
    page_vo = read_mes_java(
        "controller/admin/pro/batchrecord/vo/MesProBatchRecordDomainTracePageRespVO.java"
    )
    verify_req_vo = read_mes_java(
        "controller/admin/pro/batchrecord/vo/MesProBatchRecordDomainTraceVerifyReqVO.java"
    )

    for field in [
        "private Long domainTraceSnapshotId;",
        "private String domainTraceHash;",
        "private String domainTraceStatus;",
        "private LocalDateTime domainTraceVerifiedAt;",
    ]:
        assert field in execution_do

    for field in [
        "private Long executionId;",
        "private String executionCode;",
        "private String status;",
        "private Long domainTraceSnapshotId;",
        "private String domainTraceHash;",
        "private LocalDateTime verifiedAt;",
        "private List<Blocker> blockers;",
        "private List<Item> items;",
        "private String itemType;",
        "private String itemKey;",
        "private String blockerCode;",
        "private String blockerMessage;",
        "private String snapshotJson;",
        "private String snapshotHash;",
        "private String blockerReason;",
    ]:
        assert field in detail_vo

    for field in [
        "private Long executionId;",
        "private String executionCode;",
        "private String workOrderCode;",
        "private String batchCode;",
        "private String status;",
        "private String domainTraceHash;",
        "private LocalDateTime verifiedAt;",
        "private Integer blockerCount;",
    ]:
        assert field in page_vo

    for field in [
        "private Long executionId;",
        "private String expectedDomainTraceHash;",
    ]:
        assert field in verify_req_vo
