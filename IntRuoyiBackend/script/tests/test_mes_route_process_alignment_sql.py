from pathlib import Path
import re


SQL_PATH = Path("sql/mysql/20260709_mes_route_process_master_alignment.sql")


def read_sql() -> str:
    assert SQL_PATH.exists(), "route process master alignment SQL must exist"
    return SQL_PATH.read_text(encoding="utf-8")


def executable_sql(sql: str) -> str:
    return "\n".join(line for line in sql.splitlines() if not re.match(r"^\s*--", line)).upper()


def test_route_process_alignment_sql_contains_required_scope_and_rule_tokens():
    sql = read_sql()

    for token in [
        "release-migration:",
        "allowedEnvironments=test,backup,prod",
        "dependsOn=20260512_mes_base_schema",
        "type=data",
        "riskLevel=medium",
        "intruoyi_align_mes_route_process_to_process_master_by_code",
        "mes_pro_route_process",
        "mes_pro_process",
        "tenant_id",
        "code",
        "process_id",
        "MIN(`id`)",
        "tmp_route_process_alignment_updates",
    ]:
        assert token in sql, f"alignment SQL must include token: {token}"


def test_route_process_alignment_sql_uses_valid_release_migration_metadata():
    first_line = read_sql().splitlines()[0]
    assert first_line.startswith("-- release-migration:")
    metadata = first_line.split(":", 1)[1]

    segments = [segment.strip() for segment in metadata.split(";") if segment.strip()]
    keys = {segment.split("=", 1)[0].strip() for segment in segments}

    assert keys == {"allowedEnvironments", "dependsOn", "type", "riskLevel"}
    assert all("=" in segment for segment in segments)


def test_route_process_alignment_sql_is_fail_fast_for_missing_and_ambiguous_data():
    upper_sql = executable_sql(read_sql())

    for token in [
        "SIGNAL SQLSTATE '45000'",
        "MES ROUTE PROCESS ALIGNMENT MISSING PROCESS_ID",
        "MES ROUTE PROCESS ALIGNMENT MISSING PROCESS MASTER",
        "MES ROUTE PROCESS ALIGNMENT MISSING PROCESS CODE",
        "MES ROUTE PROCESS ALIGNMENT DUPLICATE CODE NAME CONFLICT",
        "MES ROUTE PROCESS ALIGNMENT CANONICAL PROCESS MISSING",
    ]:
        assert token in upper_sql, f"alignment SQL must fail fast with token: {token}"


def test_route_process_alignment_sql_updates_only_non_canonical_route_process_links():
    upper_sql = executable_sql(read_sql())

    assert "UPDATE `MES_PRO_ROUTE_PROCESS` RP" in upper_sql
    assert "RP.`ID` = U.`ROUTE_PROCESS_ID`" in upper_sql
    assert "RP.`PROCESS_ID` = U.`CANONICAL_PROCESS_ID`" in upper_sql
    assert "U.`CURRENT_PROCESS_ID` <> U.`CANONICAL_PROCESS_ID`" in upper_sql
    assert "RP.`DELETED` = B'0'" in upper_sql or "RP.`DELETED` = 0" in upper_sql


def test_route_process_alignment_sql_is_non_destructive_and_does_not_merge_process_master():
    upper_sql = executable_sql(read_sql())

    for forbidden in [
        "TRUNCATE",
        "DROP TABLE",
        "DELETE FROM",
        "UPDATE `MES_PRO_PROCESS`",
        "UPDATE MES_PRO_PROCESS",
        "ALTER TABLE",
    ]:
        assert forbidden not in upper_sql


def test_route_process_alignment_sql_uses_transaction_and_preview_counts():
    upper_sql = executable_sql(read_sql())

    for token in [
        "START TRANSACTION",
        "COMMIT",
        "SELECT 'ROUTE_PROCESS_ALIGNMENT_PREVIEW'",
        "SELECT 'ROUTE_PROCESS_ALIGNMENT_APPLIED'",
        "COUNT(*)",
    ]:
        assert token in upper_sql, f"alignment SQL must expose verification token: {token}"
