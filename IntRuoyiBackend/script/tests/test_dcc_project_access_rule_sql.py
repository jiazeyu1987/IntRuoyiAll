from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL = ROOT / "sql" / "mysql" / "20260911_dcc_project_access_rule.sql"
SERVICE = ROOT / "yudao-module-dcc" / "src" / "main" / "java" / "cn" / "iocoder" / "yudao" / "module" / "dcc" / "service" / "projectcode" / "access" / "DccProjectAccessServiceImpl.java"
MAPPER = ROOT / "yudao-module-dcc" / "src" / "main" / "java" / "cn" / "iocoder" / "yudao" / "module" / "dcc" / "dal" / "mysql" / "projectcode" / "DccProjectAccessRuleMapper.java"


def test_project_access_rule_is_authoritative_and_scoped():
    text = SQL.read_text(encoding="utf-8")
    assert text.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260513_dcc_base_schema; type=schema; riskLevel=medium"
    )
    assert "CREATE TABLE IF NOT EXISTS `dcc_project_access_rule`" in text
    for column in (
        "`dcc_project_code_id` bigint NOT NULL",
        "`subject_type` varchar(32) NOT NULL",
        "`subject_id` bigint NOT NULL",
        "`access_level` varchar(16) NOT NULL",
        "`active` bit(1) NOT NULL",
        "`valid_from` datetime NULL",
        "`expire_time` datetime NULL",
        "`change_reason` varchar(512) NOT NULL",
    ):
        assert column in text
    assert "active_rule_unique_flag" in text
    assert "CASE WHEN `active` = b'1' AND `deleted` = b'0' THEN 1 ELSE NULL END" in text
    assert "`tenant_id`,`dcc_project_code_id`,`subject_type`,`subject_id`,`active_rule_unique_flag`" in text
    assert "CHECK (`subject_type` IN ('USER','DEPT','ROLE','POSITION'))" in text
    assert "CHECK (`access_level` IN ('OWNER','EDIT','VIEW'))" in text
    assert "CHECK (CHAR_LENGTH(TRIM(`change_reason`)) > 0)" in text
    assert "dcc_project_code_assignment" not in text


def test_owner_service_never_uses_project_correction_assignments():
    service = SERVICE.read_text(encoding="utf-8")
    mapper = MAPPER.read_text(encoding="utf-8")
    assert "DccProjectAccessRuleMapper" in service
    assert "DccProjectCodeAssignmentMapper" not in service
    assert "dcc_project_code_assignment" not in service
    assert 'assertProjectAccess(userId, projectCodeId, Set.of("OWNER"))' in service
    assert 'Set.of("OWNER", "EDIT")' in service
    assert "getValidFrom" in mapper
    assert "getExpireTime" in mapper
