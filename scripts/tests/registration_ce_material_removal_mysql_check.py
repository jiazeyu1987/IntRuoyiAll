"""Verify CE material removal without touching any application or E2E schema."""
import argparse
from pathlib import Path
import subprocess
import uuid

ROOT = Path(__file__).resolve().parents[2]
parser = argparse.ArgumentParser()
parser.add_argument("--container", required=True)
args = parser.parse_args()
if not args.container.startswith("codex-registration-products-"):
    raise SystemExit("An isolated registration-products task container is required")
schema = "codex_ce_material_guard_" + uuid.uuid4().hex[:12]


def sql(source, database=None, success=True):
    command = ["docker", "exec", "-i", args.container, "sh", "-c",
               'exec env MYSQL_PWD="${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD is required}" '
               'mysql -uroot --default-character-set=utf8mb4 --batch --skip-column-names "$@"',
               "ce-material-check"]
    if database:
        command.append(database)
    result = subprocess.run(command, input=source, text=True, encoding="utf-8",
                            capture_output=True, timeout=60)
    if success and result.returncode:
        raise AssertionError(result.stderr)
    if not success and not result.returncode:
        raise AssertionError("Expected SQL rejection")
    return result.stdout + result.stderr


directory = ROOT / "IntRuoyiBackend/sql/mysql"
migration = (directory / "20261126_dcc_registration_certificate_remove_ce_materials.sql").read_text(encoding="utf-8")
preflight = (directory / "target-preflight/20261126_dcc_registration_certificate_remove_ce_materials.preflight.sql").read_text(encoding="utf-8")
sql(f"CREATE DATABASE `{schema}` CHARACTER SET utf8mb4;")
try:
    sql("""
        CREATE TABLE dcc_registration_certificate_file (
          id BIGINT PRIMARY KEY, tenant_id BIGINT, file_kind VARCHAR(64), deleted INT DEFAULT 0,
          CONSTRAINT chk_dcc_reg_cert_file_kind CHECK(file_kind IN
            ('REGISTRATION_CERTIFICATE','CERTIFICATE_ATTACHMENT','FDA_PRODUCT_DESCRIPTION','CHANGE_APPROVAL',
             'RENEWAL_ACCEPTANCE_RECEIPT','RENEWAL_SUPPLEMENT_NOTICE','CE_RENEWAL_CONTRACT','CE_RENEWAL_QUOTATION')));
        CREATE TABLE dcc_registration_certificate_supporting_document (
          id BIGINT PRIMARY KEY, tenant_id BIGINT, document_type VARCHAR(64), deleted INT DEFAULT 0,
          CONSTRAINT chk_dcc_reg_cert_support_type CHECK(document_type IN
            ('RENEWAL_ACCEPTANCE_RECEIPT','RENEWAL_SUPPLEMENT_NOTICE','CE_RENEWAL_CONTRACT','CE_RENEWAL_QUOTATION')));
        INSERT INTO dcc_registration_certificate_file VALUES (1,1,'REGISTRATION_CERTIFICATE',0);
        INSERT INTO dcc_registration_certificate_supporting_document VALUES (1,1,'RENEWAL_ACCEPTANCE_RECEIPT',0);
        """, schema)
    for table, column in [("file", "file_kind"), ("supporting_document", "document_type")]:
        table = "dcc_registration_certificate_" + table
        for kind in ["CE_RENEWAL_CONTRACT", "CE_RENEWAL_QUOTATION"]:
            # Deleted evidence must also block contraction, without silently removing it.
            sql(f"INSERT INTO {table} VALUES (2,2,'{kind}',1);", schema)
            assert "TARGET_PREFLIGHT_BLOCKED:" in sql(preflight, schema)
            assert "DCC_UNSUPPORTED_MATERIALS_REQUIRE_EXPLICIT_REMEDIATION" in sql(migration, schema, success=False)
            assert sql(f"SELECT COUNT(*) FROM {table} WHERE id=2 AND {column}='{kind}' AND deleted=1;", schema).strip() == "1"
            sql(f"DELETE FROM {table} WHERE id=2;", schema)
    assert "TARGET_PREFLIGHT_PASS:" in sql(preflight, schema)
    sql(migration, schema)
    for kind in ["CE_RENEWAL_CONTRACT", "CE_RENEWAL_QUOTATION"]:
        sql(f"INSERT INTO dcc_registration_certificate_file VALUES (2,1,'{kind}',0);", schema, success=False)
        sql(f"INSERT INTO dcc_registration_certificate_supporting_document VALUES (2,1,'{kind}',0);", schema, success=False)
    sql("INSERT INTO dcc_registration_certificate_file VALUES (2,1,'RENEWAL_SUPPLEMENT_NOTICE',0);"
        "INSERT INTO dcc_registration_certificate_supporting_document VALUES (2,1,'RENEWAL_SUPPLEMENT_NOTICE',0);", schema)
    assert sql("SELECT (SELECT COUNT(*) FROM dcc_registration_certificate_file) + "
               "(SELECT COUNT(*) FROM dcc_registration_certificate_supporting_document);", schema).strip() == "4"
    print("PASS: conflicts block without data loss; CE types rejected; domestic materials preserved")
finally:
    sql(f"DROP DATABASE `{schema}`;")
