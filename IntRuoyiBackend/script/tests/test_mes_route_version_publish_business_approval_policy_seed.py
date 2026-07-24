from pathlib import Path
import re


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260721_mes_route_version_publish_business_approval_policy_seed.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing MES route version publish business approval policy seed migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_route_version_publish_policy_seed_declares_fail_fast_release_metadata() -> None:
    sql = read_sql()

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260717_mes_route_version_approval_bpm_seed,20260719_business_approval_policy; "
        "type=data; riskLevel=medium"
    )
    assert "SET NAMES utf8mb4;" in sql
    assert "ensure_mes_route_version_publish_policy" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql
    assert "MES route version publish policy seed requires bpm_business_approval_policy" in sql
    assert "MES route version publish policy seed requires system_tenant" in sql
    assert "MES route version publish policy requires active tenants" in sql
    assert "MES route version publish policy conflict" in sql
    assert "MES route version publish policy duplicate" in sql


def test_route_version_publish_policy_seed_binds_signature_required_submit_publish_policy() -> None:
    sql = read_sql()

    for literal in [
        "'MES'",
        "'ROUTE_VERSION'",
        "'PUBLISH'",
        "'DRAFT'",
        "'BPM_REQUIRED'",
        "'mes-route-version-approval-v1'",
        "'PUBLISHED'",
        "'MES_ROUTE_VERSION_PUBLISH'",
    ]:
        assert literal in sql

    assert re.search(
        r"INSERT\s+INTO\s+`bpm_business_approval_policy`[\s\S]+"
        r"`data_domain`[\s\S]+`system_code`[\s\S]+`object_type`[\s\S]+"
        r"`action_code`[\s\S]+`object_state`[\s\S]+`policy_mode`[\s\S]+"
        r"`process_definition_key`[\s\S]+`effect_executor_code`",
        sql,
        re.I,
    )
    assert "COALESCE(`policy`.`policy_mode`, '') <> 'BPM_REQUIRED'" in sql
    assert "COALESCE(`policy`.`effect_executor_code`, '') <> 'MES_ROUTE_VERSION_PUBLISH'" in sql
    assert "UPDATE `bpm_business_approval_policy` AS `policy`" in sql
    assert "SET `policy`.`policy_mode` = 'BPM_REQUIRED'" in sql
    assert "`policy`.`process_definition_key` = 'mes-route-version-approval-v1'" in sql
    assert "`process_definition_key`, `effect_executor_code`" in sql
    assert "'BPM_REQUIRED', 'mes-route-version-approval-v1', 'MES_ROUTE_VERSION_PUBLISH'" in sql
    assert "ACT_RE_PROCDEF" not in sql
    assert "'SIGNATURE_REQUIRED'" not in sql
    assert "'DIRECT'" not in sql


def test_route_version_publish_policy_seed_derives_active_tenants() -> None:
    sql = read_sql()

    assert "CREATE TEMPORARY TABLE `tmp_mes_route_version_publish_policy_tenants`" in sql
    assert "FROM `system_tenant` AS `tenant`" in sql
    assert "`tenant`.`deleted` = b'0'" in sql
    assert "`tenant`.`status` = 0" in sql
    assert "`source`.`tenant_id`" in sql


def test_route_version_publish_policy_seed_is_idempotent_and_non_destructive() -> None:
    sql = read_sql()

    assert "DROP PROCEDURE IF EXISTS ensure_mes_route_version_publish_policy" in sql
    assert "WHERE NOT EXISTS (" in sql
    assert "HAVING COUNT(*) > 1" in sql
    assert "CALL ensure_mes_route_version_publish_policy();" in sql
    assert "DROP TEMPORARY TABLE IF EXISTS `tmp_mes_route_version_publish_policy_tenants`" in sql

    assert not re.search(r"\b(DROP\s+TABLE|TRUNCATE\s+TABLE|DELETE\s+FROM)\b", sql, re.I)
    assert not re.search(r"\bON\s+DUPLICATE\s+KEY\s+UPDATE\b", sql, re.I)
    assert "INSERT IGNORE" not in sql
    assert "fallback" not in sql.lower()
