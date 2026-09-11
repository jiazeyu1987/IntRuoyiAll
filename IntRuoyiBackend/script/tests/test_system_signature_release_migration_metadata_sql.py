from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


def first_line(sql_name: str) -> str:
    return (ROOT / f"sql/mysql/{sql_name}").read_text(encoding="utf-8").splitlines()[0]


def test_system_signature_sql_files_have_release_migration_metadata():
    assert first_line("20260908_system_signature_password_t1.sql") == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=; type=schema; riskLevel=medium"
    )
    assert first_line("20260908_system_signature_identity_t2.sql") == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260908_system_signature_password_t1; type=schema; riskLevel=medium"
    )
    assert first_line("20260908_system_electronic_signature_t3.sql") == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260908_system_signature_identity_t2; type=schema; riskLevel=medium"
    )
    assert first_line("20260908_system_electronic_signature_subject_id_capacity.sql") == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260908_system_electronic_signature_t3; type=schema; riskLevel=medium"
    )
    assert first_line("20260908_system_electronic_signature_t7.sql") == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260908_system_electronic_signature_subject_id_capacity; type=schema; riskLevel=medium"
    )
    assert first_line("20260908_system_electronic_signature_t8.sql") == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260908_system_electronic_signature_t7; type=schema; riskLevel=medium"
    )
