"""Forward migration tests in the isolated product-admission fixture container only."""
import argparse
from pathlib import Path
import unittest
import test_registration_product_admission_mysql as admission

SETUP, SQL_ROOT = admission.SETUP, admission.SQL_ROOT

MID = '20261127_dcc_registration_certificate_multi_products'
FIXTURE = SETUP + '''
ALTER TABLE dcc_registration_certificate_snapshot ADD version_id BIGINT, ADD update_time DATETIME;
UPDATE dcc_registration_certificate_snapshot SET version_id=11,update_time=NOW();
CREATE TABLE dcc_registration_certificate_version(id BIGINT,tenant_id BIGINT,certificate_id BIGINT,status VARCHAR(32),deleted INT);
INSERT INTO dcc_registration_certificate_version VALUES(11,1,1,'CURRENT',0);
'''
LEGACY_TABLE = (SQL_ROOT / '20261125_dcc_registration_certificate_products.sql').read_text('utf-8')
LEGACY_TABLE = LEGACY_TABLE[LEGACY_TABLE.index('CREATE TABLE'):LEGACY_TABLE.index('DROP TEMPORARY')]
BOUND_ROW = "INSERT INTO dcc_registration_certificate_product VALUES(1,10,1,100,'PRODUCT-10','Synthetic product','BOUND',77,'2026-01-01');"


class MultiCertificateTests(unittest.TestCase):
    container = None
    query = admission.ProductAdmissionTests.__dict__['query']
    setUpClass = admission.ProductAdmissionTests.__dict__['setUpClass']

    def setUp(self):
        self.query(FIXTURE)

    def check(self):
        return self.query('USE product_admission_fixture; START TRANSACTION READ ONLY;\n'
            + (SQL_ROOT / 'target-preflight' / (MID + '.preflight.sql')).read_text('utf-8') + '\nROLLBACK;').stdout.strip()

    def apply(self, success=True):
        return self.query('USE product_admission_fixture;\n' + (SQL_ROOT / (MID + '.sql')).read_text('utf-8'), not success)

    def scalar(self, sql):
        return self.query('USE product_admission_fixture;' + sql).stdout.strip()

    def test_shared_product_migrates_both_certificates_and_preserves_selected_relation(self):
        self.query("USE product_admission_fixture; INSERT INTO dcc_registration_certificate VALUES(2,1,100,12,NULL,0); UPDATE dcc_data_relation SET registration_certificate_id=1;")
        self.assertEqual('TARGET_PREFLIGHT_PASS:' + MID,self.check())
        self.apply()
        self.assertEqual('1:10:BOUND\n2:10:BOUND',self.scalar("SELECT CONCAT(certificate_id,':',product_catalog_id,':',state) FROM dcc_registration_certificate_product ORDER BY certificate_id;"))
        self.assertEqual('1',self.scalar('SELECT registration_certificate_id FROM dcc_data_relation;'))
        self.assertEqual('10',self.scalar("SELECT JSON_UNQUOTE(JSON_EXTRACT(facts_json,'$.projectProducts[0].productCatalogId')) FROM dcc_registration_certificate_snapshot;"))
        self.apply()  # Repeat must neither duplicate rows nor modify a formal snapshot again.
        self.assertEqual('2',self.scalar('SELECT COUNT(*) FROM dcc_registration_certificate_product;'))
        blocked=self.query("USE product_admission_fixture; UPDATE dcc_registration_certificate_snapshot SET facts_json=JSON_OBJECT();",True)
        self.assertIn('append-only',blocked.stderr)

    def test_legacy_empty_and_populated_tables_upgrade_without_rewriting_existing_rows(self):
        for populated in (False,True):
            with self.subTest(populated=populated):
                self.query(FIXTURE + LEGACY_TABLE + (BOUND_ROW if populated else '')
                    + 'INSERT INTO dcc_registration_certificate VALUES(2,1,100,12,NULL,0);')
                self.assertEqual('TARGET_PREFLIGHT_PASS:' + MID,self.check())
                self.apply()
                self.assertEqual('2',self.scalar('SELECT COUNT(*) FROM dcc_registration_certificate_product;'))
                if populated:
                    self.assertEqual('77:2026-01-01 00:00:00',self.scalar("SELECT CONCAT(created_by,':',created_at) FROM dcc_registration_certificate_product WHERE certificate_id=1;"))
                self.assertEqual('tenant_id,certificate_id,product_catalog_id',self.scalar("SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_registration_certificate_product' AND INDEX_NAME='PRIMARY';"))

    def test_certificate_local_uniqueness_and_cross_tenant_identity(self):
        self.apply()
        self.query("USE product_admission_fixture; INSERT INTO dcc_registration_certificate_product VALUES(1,10,2,100,'P10','Product','RESERVED',9,NOW()),(2,10,1,100,'P10','Other tenant','RESERVED',9,NOW());")
        for row in ("(1,10,1,200,'P10','Duplicate product','RESERVED',9,NOW())",
                    "(1,20,1,100,'P20','Duplicate project','RESERVED',9,NOW())"):
            with self.subTest(row=row):
                failed=self.query('USE product_admission_fixture; INSERT INTO dcc_registration_certificate_product VALUES'+row+';',True)
                self.assertIn('Duplicate entry',failed.stderr)
        self.assertEqual('3',self.scalar('SELECT COUNT(*) FROM dcc_registration_certificate_product;'))

    def test_real_identity_conflicts_still_block_before_binding_ddl_and_snapshot_writes(self):
        cases = [
            ('missing relation','DELETE FROM dcc_data_relation;','UNRESOLVED_PRODUCT'),
            ('wrong tenant','UPDATE dcc_data_relation SET tenant_id=2;','UNRESOLVED_PRODUCT'),
            ('duplicate source',"INSERT INTO dcc_data_relation VALUES(2,1,100,10,'CONFIRMED',0,NULL);",'UNRESOLVED_PRODUCT'),
            ('blank code',"UPDATE dcc_product_catalog SET product_code='';",'UNRESOLVED_PRODUCT'),
            ('deleted catalog','UPDATE dcc_product_catalog SET deleted=1;','UNRESOLVED_PRODUCT'),
            ('ambiguous project',"INSERT INTO dcc_data_relation VALUES(2,1,100,20,'CONFIRMED',0,NULL);",'UNRESOLVED_PRODUCT'),
            ('different snapshot product',"UPDATE dcc_registration_certificate_snapshot SET facts_json=JSON_SET(facts_json,'$.projectProduct.productCatalogId','99');",'SNAPSHOT_PRODUCT_MISMATCH'),
            ('different snapshot project',"UPDATE dcc_registration_certificate_snapshot SET facts_json=JSON_SET(facts_json,'$.projectProduct.projectCodeId','99');",'SNAPSHOT_PRODUCT_MISMATCH'),
            ('missing snapshot identity',"UPDATE dcc_registration_certificate_snapshot SET facts_json=JSON_REMOVE(facts_json,'$.projectProduct.productCatalogId');",'SNAPSHOT_PRODUCT_MISMATCH'),
            ('second tenant unresolved','INSERT INTO dcc_registration_certificate VALUES(3,2,300,33,NULL,0);','UNRESOLVED_PRODUCT'),
            ('pending upload unresolved',"UPDATE dcc_registration_certificate SET current_version_id=NULL; INSERT INTO dcc_registration_certificate_access_request VALUES(1,1,1,0,'UPLOAD_CERTIFICATE','SUBMITTED'); DELETE FROM dcc_data_relation;",'UNRESOLVED_PRODUCT'),
        ]
        for name,mutation,reason in cases:
            with self.subTest(name=name):
                self.query(FIXTURE+mutation)
                before=self.scalar('SELECT SHA2(CAST(facts_json AS CHAR),256) FROM dcc_registration_certificate_snapshot;')
                self.assertEqual('TARGET_PREFLIGHT_BLOCKED:'+MID+':'+reason,self.check())
                result=self.apply(False)
                self.assertNotEqual(0,result.returncode)
                self.assertIn(reason,result.stderr)
                self.assertEqual('0',self.scalar("SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_registration_certificate_product';"))
                self.assertEqual(before,self.scalar('SELECT SHA2(CAST(facts_json AS CHAR),256) FROM dcc_registration_certificate_snapshot;'))

    def test_unknown_partial_schema_is_rejected_explicitly(self):
        self.query('USE product_admission_fixture; CREATE TABLE dcc_registration_certificate_product(id BIGINT);')
        self.assertIn('UNSUPPORTED_BINDING_SCHEMA',self.check())
        self.assertIn('UNSUPPORTED_BINDING_SCHEMA',self.apply(False).stderr)

    def test_existing_wrong_identity_is_not_overwritten_during_key_upgrade(self):
        self.query('USE product_admission_fixture;'+LEGACY_TABLE+BOUND_ROW.replace('(1,10,1,100,','(1,99,1,100,'))
        self.assertEqual('TARGET_PREFLIGHT_PASS:'+MID,self.check())  # Optional table row validation occurs under stopped writers.
        self.assertIn('conflicts with migration',self.apply(False).stderr)
        self.assertEqual('99',self.scalar('SELECT product_catalog_id FROM dcc_registration_certificate_product;'))
        self.assertEqual('0',self.scalar("SELECT JSON_CONTAINS_PATH(facts_json,'one','$.projectProducts') FROM dcc_registration_certificate_snapshot;"))


if __name__ == '__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--container',required=True)
    args,remaining=parser.parse_known_args();MultiCertificateTests.container=args.container
    unittest.main(argv=['multi-certificate-mysql']+remaining)
