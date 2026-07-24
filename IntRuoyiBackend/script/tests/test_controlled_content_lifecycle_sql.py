from pathlib import Path
import re


ROOT = Path(__file__).resolve().parents[2]
MIGRATION = ROOT / "sql" / "mysql" / "20260718_controlled_content_lifecycle.sql"


def read_text(path: Path) -> str:
    assert path.exists(), f"missing required file: {path}"
    return path.read_text(encoding="utf-8")


def test_controlled_content_lifecycle_migration_creates_minimal_platform_tables():
    sql = read_text(MIGRATION)

    assert "-- release-migration:" in sql
    assert "type=schema" in sql
    assert "riskLevel=medium" in sql
    assert "CREATE TABLE IF NOT EXISTS `controlled_content_version_ref`" in sql
    assert "CREATE TABLE IF NOT EXISTS `controlled_content_transition_audit`" in sql
    for column in [
        "tenant_id",
        "content_type",
        "content_key",
        "native_master_id",
        "native_version_id",
        "version_no",
        "canonical_status",
        "domain_status",
        "source_version_ref_id",
        "source_native_version_id",
        "successor_version_ref_id",
        "successor_native_version_id",
        "active_unique_flag",
        "open_candidate_unique_flag",
        "approval_process_instance_id",
        "last_transition_time",
    ]:
        assert re.search(rf"`{column}`\s+", sql, re.I), f"missing version ref column {column}"
    for column in [
        "version_ref_id",
        "from_status",
        "to_status",
        "domain_from_status",
        "domain_to_status",
        "action",
        "actor_id",
        "reason",
        "event_key",
    ]:
        assert re.search(rf"`{column}`\s+", sql, re.I), f"missing transition audit column {column}"

    for column in [
        "snapshot_hash",
        "lock_version",
        "business_ref_type",
        "business_ref_id",
    ]:
        assert not re.search(rf"`{column}`\s+", sql, re.I), f"unused platform column should not exist: {column}"


def test_controlled_content_lifecycle_migration_has_unique_active_and_open_candidate_guards():
    sql = read_text(MIGRATION)

    assert "uk_controlled_content_active" in sql
    assert "`tenant_id`, `content_type`, `content_key`, `active_unique_flag`" in sql
    assert "uk_controlled_content_open_candidate" in sql
    assert "`tenant_id`, `content_type`, `content_key`, `open_candidate_unique_flag`" in sql
    assert "uk_controlled_content_transition_event" in sql
    assert "`version_ref_id`, `action`, `event_key`" in sql
    assert "idx_controlled_content_ref_native" in sql
    assert "idx_controlled_content_transition_ref" in sql
    assert "idx_controlled_content_transition_business" not in sql


def test_controlled_content_lifecycle_migration_fails_fast_on_native_duplicates():
    sql = read_text(MIGRATION)

    assert "ensure_controlled_content_lifecycle_preflight" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql
    assert "duplicate MES route active versions" in sql
    assert "duplicate MES route open candidate versions" in sql
    assert "duplicate DCC active revisions" in sql
    assert "duplicate DCC open candidate revisions" in sql
    assert "dcc master points to obsolete revision" in sql
    assert "mes_pro_route_version" in sql
    assert "dcc_controlled_file_master" in sql
    assert "dcc_controlled_file" in sql


def test_controlled_content_lifecycle_preflight_rejects_invalid_dcc_current_active_before_schema():
    sql = read_text(MIGRATION)

    preflight_end = sql.index("CALL ensure_controlled_content_lifecycle_preflight();")
    schema_start = sql.index("CREATE PROCEDURE ensure_controlled_content_lifecycle_schema()")
    preflight = sql[:preflight_end]
    assert preflight_end < schema_start
    assert "LEFT JOIN `dcc_controlled_file` file" in preflight
    assert "master.`current_active_controlled_file_id` IS NOT NULL" in preflight
    assert "file.`id` IS NULL" in preflight
    assert "file.`deleted` <> 0" in preflight
    assert "file.`status` <> 'ACTIVE'" in preflight
    assert "dcc master current active revision must be ACTIVE before controlled content lifecycle migration" in preflight


def test_controlled_content_lifecycle_migration_skips_obsolete_dcc_chains():
    sql = read_text(MIGRATION)

    assert "file_no" not in sql
    assert re.search(
        r"WHERE\s+master\.`deleted`\s*=\s*0\s+"
        r"AND\s+master\.`status`\s*<>\s*'OBSOLETE_CHAIN'[\s\S]+?"
        r"file\.`status`\s*=\s*'OBSOLETE'[\s\S]+?"
        r"dcc master points to obsolete revision",
        sql,
        re.I,
    ), "obsolete-current preflight must not block a fully obsolete master chain"
    assert re.search(
        r"master\.`status`\s*=\s*'OBSOLETE_CHAIN'[\s\S]+?"
        r"file\.`status`\s*=\s*'ACTIVE'[\s\S]+?"
        r"dcc obsolete chain has active revision",
        sql,
        re.I,
    ), "obsolete master chains with active revisions must still fail fast"
    assert re.search(
        r"WHERE\s+master\.`deleted`\s*=\s*0\s+"
        r"AND\s+master\.`status`\s*<>\s*'OBSOLETE_CHAIN'\s+"
        r"AND\s+master\.`current_active_controlled_file_id`\s+IS\s+NOT\s+NULL",
        sql,
        re.I,
    ), "adoption preflight must not require ACTIVE current revision for obsolete-only chains"


def test_controlled_content_lifecycle_migration_collates_content_key_casts():
    sql = read_text(MIGRATION)

    assert re.search(
        r"existing_ref\.`content_key`\s*=\s*"
        r"CAST\(route_version\.`route_id`\s+AS\s+CHAR\)\s+COLLATE\s+utf8mb4_unicode_ci",
        sql,
        re.I,
    ), "MES route content_key comparison must not inherit database default collation"
    assert re.search(
        r"existing_ref\.`content_key`\s*=\s*"
        r"CAST\(master\.`id`\s+AS\s+CHAR\)\s+COLLATE\s+utf8mb4_unicode_ci",
        sql,
        re.I,
    ), "DCC file content_key comparison must not inherit database default collation"
    assert len(re.findall(r"content_key`\s*=\s*CAST\([^)]+\s+AS\s+CHAR\)(?!\s+COLLATE)", sql, re.I)) == 0


def test_controlled_content_lifecycle_migration_adopts_existing_active_native_versions():
    sql = read_text(MIGRATION)

    assert "ensure_controlled_content_lifecycle_adoption" in sql
    assert re.search(
        r"INSERT\s+INTO\s+`controlled_content_version_ref`[\s\S]+FROM\s+`mes_pro_route_version`\s+route_version",
        sql,
        re.I,
    )
    assert "route_version.`active` = b'1'" in sql
    assert "route_version.`lifecycle_status` = 'ACTIVE'" in sql
    assert "'MES_ROUTE'" in sql
    assert "CAST(route_version.`route_id` AS CHAR)" in sql
    assert "route_version.`route_id`" in sql
    assert "route_version.`id`" in sql
    assert "route_version.`version_no`" in sql
    assert "route_version.`lifecycle_status`" in sql
    assert "route_version.`approval_process_instance_id`" in sql
    assert re.search(
        r"route_version\.`lifecycle_status`,\s*1,\s*NULL,\s*route_version\.`approval_process_instance_id`",
        sql,
        re.I,
    )

    assert re.search(
        r"INSERT\s+INTO\s+`controlled_content_version_ref`[\s\S]+FROM\s+`dcc_controlled_file_master`\s+master",
        sql,
        re.I,
    )
    assert "master.`current_active_controlled_file_id`" in sql
    assert "file.`status` = 'ACTIVE'" in sql
    assert "'DCC_CONTROLLED_FILE'" in sql
    assert "CAST(master.`id` AS CHAR)" in sql
    assert "file.`id`" in sql
    assert "file.`version_no`" in sql
    assert "file.`process_instance_id`" in sql
    assert re.search(
        r"file\.`status`,\s*1,\s*NULL,\s*file\.`process_instance_id`",
        sql,
        re.I,
    )

    assert "AND existing_ref.`native_version_id` = route_version.`id`" in sql
    assert "AND existing_ref.`native_version_id` = file.`id`" in sql
    assert "controlled content MES active refs drift" in sql
    assert "controlled content DCC active refs drift" in sql
    assert "dcc master current active revision must be ACTIVE" in sql


def test_controlled_content_lifecycle_migration_is_idempotent_and_non_destructive():
    sql = read_text(MIGRATION)

    assert "DROP PROCEDURE IF EXISTS ensure_controlled_content_lifecycle_preflight" in sql
    assert "DROP PROCEDURE IF EXISTS ensure_controlled_content_lifecycle_schema" in sql
    assert "DROP PROCEDURE IF EXISTS ensure_controlled_content_lifecycle_adoption" in sql
    assert not re.search(r"\b(DROP\s+TABLE|TRUNCATE\s+TABLE|DELETE\s+FROM)\b", sql, re.I)
    assert not re.search(r"\bON\s+DUPLICATE\s+KEY\s+UPDATE\b", sql, re.I)
    assert "INSERT IGNORE" not in sql
    assert len(re.findall(r"\bNOT\s+EXISTS\s*\(", sql, re.I)) >= 2
    assert "fallback" not in sql.lower()
