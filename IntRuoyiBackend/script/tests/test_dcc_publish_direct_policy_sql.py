from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL = ROOT / "sql/mysql/20260911_dcc_publish_direct_policy.sql"


def test_dcc_publish_direct_policy_has_release_metadata_and_idempotent_wrapper() -> None:
    source = SQL.read_text(encoding="utf-8")

    assert source.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260719_business_approval_policy,20260720_dcc_publish_form_policy_seed; "
        "type=data; riskLevel=medium\n"
    )
    assert "DELIMITER //" in source
    assert "DROP PROCEDURE IF EXISTS `apply_dcc_publish_direct_policy`//" in source
    assert "CALL `apply_dcc_publish_direct_policy`();" in source
    assert source.rstrip().endswith("DROP PROCEDURE IF EXISTS `apply_dcc_publish_direct_policy`;")
