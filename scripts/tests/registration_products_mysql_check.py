"""Exercise the release migration in an explicitly named, isolated task MySQL container."""
import argparse
import subprocess
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
parser = argparse.ArgumentParser()
parser.add_argument("--container", required=True)
args = parser.parse_args()
if not args.container.startswith("codex-registration-products-"):
    raise SystemExit("Only an isolated registration-products task container is allowed")


def sql(text, database=None, success=True):
    command = ["docker", "exec", "-i", args.container, "mysql", "-uroot", "--default-character-set=utf8mb4", "--batch", "--skip-column-names"]
    if database:
        command.append(database)
    result = subprocess.run(command, input=text, text=True, encoding="utf-8", capture_output=True, timeout=60)
    if success and result.returncode:
        raise AssertionError(result.stderr)
    if not success and not result.returncode:
        raise AssertionError("Expected SQL to reject the operation")
    return result.stdout.strip(), result.stderr


fixture = """
CREATE TABLE dcc_registration_certificate(id BIGINT PRIMARY KEY,tenant_id BIGINT,project_code_id BIGINT,current_version_id BIGINT,pending_version_id BIGINT,current_snapshot_id BIGINT,deleted INT);
CREATE TABLE dcc_registration_certificate_version(id BIGINT PRIMARY KEY,tenant_id BIGINT,certificate_id BIGINT,status VARCHAR(32),deleted INT);
CREATE TABLE dcc_registration_certificate_snapshot(id BIGINT PRIMARY KEY,tenant_id BIGINT,version_id BIGINT,facts_json JSON,update_time DATETIME);
CREATE TABLE dcc_product_catalog(id BIGINT PRIMARY KEY,tenant_id BIGINT,product_code VARCHAR(128),product VARCHAR(255),deleted INT);
CREATE TABLE dcc_data_relation(id BIGINT PRIMARY KEY,tenant_id BIGINT,project_code_id BIGINT,product_catalog_id BIGINT,registration_certificate_id BIGINT,relation_status VARCHAR(32),deleted INT);
CREATE TABLE dcc_project_product_relation(id BIGINT,tenant_id BIGINT,project_code_id BIGINT,product_catalog_id BIGINT,request_id BIGINT,relation_status VARCHAR(32),deleted INT);
CREATE TABLE dcc_project_product_create_request(id BIGINT,tenant_id BIGINT,status VARCHAR(32),generated_project_code_id BIGINT,generated_product_catalog_id BIGINT,relation_id BIGINT,deleted INT);
CREATE TABLE dcc_registration_certificate_access_request(id BIGINT,tenant_id BIGINT,certificate_id BIGINT,request_type VARCHAR(64),status VARCHAR(32),deleted INT);
INSERT INTO dcc_registration_certificate VALUES(1,1,100,11,NULL,111,0);
INSERT INTO dcc_registration_certificate_version VALUES(11,1,1,'CURRENT',0);
INSERT INTO dcc_registration_certificate_snapshot VALUES(111,1,11,JSON_OBJECT('ownProduct',TRUE,'certificateType','DOMESTIC','certificateNo','TEST-1'),NOW());
INSERT INTO dcc_product_catalog VALUES(10,1,'P10','产品十',0),(20,1,'P20','产品二十',0);
INSERT INTO dcc_data_relation VALUES(1,1,100,10,1,'CONFIRMED',0),(2,1,200,20,NULL,'CONFIRMED',0);
"""
migration = (ROOT / "IntRuoyiBackend/sql/mysql/20261125_dcc_registration_certificate_products.sql").read_text(encoding="utf-8")
preflight = (ROOT / "IntRuoyiBackend/sql/mysql/target-preflight/20261125_dcc_registration_certificate_products.preflight.sql").read_text(encoding="utf-8")
sql("CREATE DATABASE reg_products_ok CHARACTER SET utf8mb4; CREATE DATABASE reg_products_conflict CHARACTER SET utf8mb4; CREATE DATABASE reg_products_snapshot_conflict CHARACTER SET utf8mb4;")
sql(fixture, "reg_products_ok")
assert sql(preflight, "reg_products_ok")[0] == ""
sql(migration, "reg_products_ok")
assert sql("SELECT CONCAT(certificate_id,':',product_catalog_id,':',state) FROM dcc_registration_certificate_product;", "reg_products_ok")[0] == "1:10:BOUND"
assert sql("SELECT JSON_UNQUOTE(JSON_EXTRACT(facts_json,'$.projectProducts[0].productCatalogId')) FROM dcc_registration_certificate_snapshot;", "reg_products_ok")[0] == "10"
# Idempotent migration and restored snapshot immutability.
sql(migration, "reg_products_ok")
assert "append-only" in sql("UPDATE dcc_registration_certificate_snapshot SET facts_json=JSON_OBJECT('ownProduct',FALSE) WHERE id=111;", "reg_products_ok", False)[1]
sql("INSERT INTO dcc_registration_certificate_product VALUES(1,20,1,200,'P20','产品二十','BOUND',1,NOW());", "reg_products_ok")
assert sql("SELECT COUNT(*) FROM dcc_registration_certificate_product WHERE certificate_id=1;", "reg_products_ok")[0] == "2"
assert "Duplicate entry" in sql("INSERT INTO dcc_registration_certificate_product VALUES(1,20,2,300,'P20','产品二十','RESERVED',1,NOW());", "reg_products_ok", False)[1]
# Shared certificate query must find both projects through its snapshot list.
sql("INSERT INTO dcc_registration_certificate_snapshot VALUES(112,1,11,JSON_OBJECT('projectProducts',JSON_ARRAY(JSON_OBJECT('projectCodeId','100'),JSON_OBJECT('projectCodeId','200'))),NOW());", "reg_products_ok")
assert sql("SELECT COUNT(*) FROM dcc_registration_certificate_snapshot s WHERE s.id=112 AND EXISTS(SELECT 1 WHERE JSON_CONTAINS(JSON_EXTRACT(s.facts_json,'$.projectProducts[*].projectCodeId'),JSON_QUOTE(CAST(200 AS CHAR)))=1);", "reg_products_ok")[0] == "1"
# Exercise the exact production project-label expression, including numeric and string JSON IDs.
query_source = (ROOT / "IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/dal/mysql/DccRegistrationCertificateQueryMapper.java").read_text(encoding="utf-8")
label_expression = re.search(r"ELSE (\(SELECT GROUP_CONCAT.*?\)) END AS project_code,", query_source).group(1)
sql("CREATE TABLE dcc_project_code(id BIGINT,tenant_id BIGINT,project_code VARCHAR(64),deleted INT); INSERT INTO dcc_project_code VALUES(100,1,'P100',0),(200,1,'P200',0);", "reg_products_ok")
for project_ids in ["JSON_ARRAY(JSON_OBJECT('projectCodeId','100'),JSON_OBJECT('projectCodeId','200'))", "JSON_ARRAY(JSON_OBJECT('projectCodeId',100),JSON_OBJECT('projectCodeId',200))"]:
    sql(f"INSERT INTO dcc_registration_certificate_snapshot VALUES(113,1,11,JSON_OBJECT('projectProducts',{project_ids}),NOW());", "reg_products_ok")
    result=sql(f"SELECT {label_expression} FROM dcc_registration_certificate_snapshot s JOIN dcc_registration_certificate c ON c.id=1 WHERE s.id=113;", "reg_products_ok")[0]
    assert result == "P100、P200", result
    # The test row is outside the registered certificate's historical snapshot pointers.
    sql("DELETE FROM dcc_registration_certificate_snapshot WHERE id=113;", "reg_products_ok")
sql(fixture, "reg_products_snapshot_conflict")
sql("UPDATE dcc_registration_certificate_snapshot SET facts_json=JSON_SET(facts_json,'$.projectProduct',JSON_OBJECT('projectCodeId','100','productCatalogId','999')) WHERE id=111;", "reg_products_snapshot_conflict")
assert "SNAPSHOT_PRODUCT_MISMATCH" in sql(preflight,"reg_products_snapshot_conflict")[0]
assert "business resolution required" in sql(migration,"reg_products_snapshot_conflict",False)[1]
assert sql("SELECT COUNT(*) FROM dcc_registration_certificate_product;","reg_products_snapshot_conflict")[0] == "0"
sql(fixture, "reg_products_conflict")
sql("INSERT INTO dcc_registration_certificate VALUES(2,1,100,22,NULL,NULL,0); INSERT INTO dcc_registration_certificate_version VALUES(22,1,2,'CURRENT',0);", "reg_products_conflict")
assert "MULTIPLE_CERTIFICATES" in sql(preflight, "reg_products_conflict")[0]
assert "business resolution required" in sql(migration, "reg_products_conflict", False)[1]
assert sql("SELECT COUNT(*) FROM dcc_registration_certificate_product;", "reg_products_conflict")[0] == "0"
assert sql("SELECT JSON_CONTAINS_PATH(facts_json,'one','$.projectProducts') FROM dcc_registration_certificate_snapshot;", "reg_products_conflict")[0] == "0"
print("PASS: MySQL migration, repeat execution, conflict report, fail-before-data-write, snapshot immutability, shared projects and unique product constraint")
