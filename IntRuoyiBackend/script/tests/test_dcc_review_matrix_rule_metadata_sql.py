from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260625_dcc_review_matrix_rule_metadata.sql"


def test_dcc_review_matrix_rule_metadata_declares_release_contract():
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260623_dcc_view_matrix_independent_source; type=schema; riskLevel=medium\n"
    )
