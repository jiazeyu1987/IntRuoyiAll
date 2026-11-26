"""Exercise product admission against a task-owned, network-isolated MySQL container.

Run: python scripts/tests/test_registration_product_admission_mysql.py --container NAME
The named disposable container must have label intruoyi.task=product-admission-20261127.
"""
import argparse
import json
from pathlib import Path
import subprocess
import unittest


ROOT = Path(__file__).resolve().parents[2]
MID = '20261127_dcc_registration_certificate_products_admission'
SQL_ROOT = ROOT / 'IntRuoyiBackend/sql/mysql'
SETUP = '''
DROP DATABASE IF EXISTS product_admission_fixture;
CREATE DATABASE product_admission_fixture;
USE product_admission_fixture;
CREATE TABLE dcc_data_relation(id BIGINT,tenant_id BIGINT,project_code_id BIGINT,
 product_catalog_id BIGINT,relation_status VARCHAR(32),deleted INT,registration_certificate_id BIGINT);
CREATE TABLE dcc_project_product_relation(id BIGINT,tenant_id BIGINT,project_code_id BIGINT,
 product_catalog_id BIGINT,request_id BIGINT,relation_status VARCHAR(32),deleted INT);
CREATE TABLE dcc_project_product_create_request(id BIGINT,tenant_id BIGINT,deleted INT,status VARCHAR(32),
 generated_project_code_id BIGINT,generated_product_catalog_id BIGINT,relation_id BIGINT);
CREATE TABLE dcc_product_catalog(id BIGINT,tenant_id BIGINT,product_code VARCHAR(128),product VARCHAR(255),deleted INT);
CREATE TABLE dcc_registration_certificate(id BIGINT,tenant_id BIGINT,project_code_id BIGINT,
 current_version_id BIGINT,current_snapshot_id BIGINT,deleted INT);
CREATE TABLE dcc_registration_certificate_access_request(id BIGINT,tenant_id BIGINT,certificate_id BIGINT,
 deleted INT,request_type VARCHAR(32),status VARCHAR(32));
CREATE TABLE dcc_registration_certificate_snapshot(id BIGINT,tenant_id BIGINT,facts_json JSON);
INSERT INTO dcc_data_relation VALUES(1,1,100,10,'CONFIRMED',0,NULL);
INSERT INTO dcc_product_catalog VALUES(10,1,'PRODUCT-10','Synthetic product',0);
INSERT INTO dcc_registration_certificate VALUES(1,1,100,11,21,0);
INSERT INTO dcc_registration_certificate_snapshot VALUES(21,1,
 JSON_OBJECT('ownProduct',true,'projectProduct',JSON_OBJECT('productCatalogId','10','projectCodeId','100')));
'''


class ProductAdmissionTests(unittest.TestCase):
    container = None
    checked = 0

    @classmethod
    def setUpClass(cls):
        info = json.loads(subprocess.check_output(['docker', 'inspect', cls.container], text=True))[0]
        if (info['Config']['Labels'].get('intruoyi.task') != 'product-admission-20261127'
                or info['HostConfig']['NetworkMode'] != 'none'
                or info['HostConfig'].get('PortBindings')):
            raise RuntimeError('Only the labeled isolated disposable fixture container is allowed')
        cls.query('SELECT VERSION();')

    @classmethod
    def query(cls, sql, allow_failure=False):
        result = subprocess.run(['docker', 'exec', '-i', cls.container, 'mysql', '-uroot',
                                 '--batch', '--raw', '--skip-column-names', '--default-character-set=utf8mb4'],
                                input=sql, capture_output=True, text=True, encoding='utf-8', timeout=30)
        if result.returncode and not allow_failure:
            raise AssertionError(result.stderr.strip())
        return result

    def test_readonly_admission_and_quiescent_guard_agree(self):
        cases = [
            ('valid confirmed binding', '', None),
            ('missing product relation', 'DELETE FROM dcc_data_relation;', 'UNRESOLVED_PRODUCT'),
            ('deleted relation ignored', 'UPDATE dcc_data_relation SET deleted=1;', 'UNRESOLVED_PRODUCT'),
            ('wrong-tenant relation ignored', 'UPDATE dcc_data_relation SET tenant_id=2;', 'UNRESOLVED_PRODUCT'),
            ('blank product code', "UPDATE dcc_product_catalog SET product_code='';", 'UNRESOLVED_PRODUCT'),
            ('null product name', 'UPDATE dcc_product_catalog SET product=NULL;', 'UNRESOLVED_PRODUCT'),
            ('deleted catalog ignored', 'UPDATE dcc_product_catalog SET deleted=1;', 'UNRESOLVED_PRODUCT'),
            ('duplicate source', "INSERT INTO dcc_data_relation VALUES(2,1,100,10,'CONFIRMED',0,NULL);", 'UNRESOLVED_PRODUCT'),
            ('multiple project products', "INSERT INTO dcc_data_relation VALUES(2,1,100,20,'CONFIRMED',0,NULL);", 'UNRESOLVED_PRODUCT'),
            ('snapshot wrong product', "UPDATE dcc_registration_certificate_snapshot SET facts_json=JSON_SET(facts_json,'$.projectProduct.productCatalogId','99');", 'SNAPSHOT_PRODUCT_MISMATCH'),
            ('snapshot wrong project', "UPDATE dcc_registration_certificate_snapshot SET facts_json=JSON_SET(facts_json,'$.projectProduct.projectCodeId','99');", 'SNAPSHOT_PRODUCT_MISMATCH'),
            ('snapshot missing product identity', "UPDATE dcc_registration_certificate_snapshot SET facts_json=JSON_REMOVE(facts_json,'$.projectProduct.productCatalogId');", 'SNAPSHOT_PRODUCT_MISMATCH'),
            ('snapshot-only certificate also checked', 'UPDATE dcc_registration_certificate SET current_version_id=NULL,project_code_id=NULL;', 'SNAPSHOT_PRODUCT_MISMATCH'),
            ('one product claimed by two certificates', 'INSERT INTO dcc_registration_certificate VALUES(2,1,100,12,NULL,0);', 'MULTIPLE_CERTIFICATES'),
            ('relation claims another certificate', 'UPDATE dcc_data_relation SET registration_certificate_id=99;', 'RELATION_CERTIFICATE_MISMATCH'),
            ('reserved upload candidate checked', "UPDATE dcc_registration_certificate SET current_version_id=NULL; INSERT INTO dcc_registration_certificate_access_request VALUES(1,1,1,0,'UPLOAD_CERTIFICATE','SUBMITTED'); DELETE FROM dcc_data_relation;", 'UNRESOLVED_PRODUCT'),
            ('BPM-bound upload candidate checked', "UPDATE dcc_registration_certificate SET current_version_id=NULL; INSERT INTO dcc_registration_certificate_access_request VALUES(1,1,1,0,'UPLOAD_CERTIFICATE','BPM_BOUND'); DELETE FROM dcc_data_relation;", 'UNRESOLVED_PRODUCT'),
            ('completed approved source accepted', "DELETE FROM dcc_data_relation; INSERT INTO dcc_project_product_relation VALUES(2,1,100,10,3,'ACTIVE',0); INSERT INTO dcc_project_product_create_request VALUES(3,1,0,'COMPLETED',100,10,2);", None),
            ('incomplete approval rejected', "DELETE FROM dcc_data_relation; INSERT INTO dcc_project_product_relation VALUES(2,1,100,10,3,'ACTIVE',0); INSERT INTO dcc_project_product_create_request VALUES(3,1,0,'PENDING',100,10,2);", 'UNRESOLVED_PRODUCT'),
            ('mismatched approval identity rejected', "DELETE FROM dcc_data_relation; INSERT INTO dcc_project_product_relation VALUES(2,1,100,10,3,'ACTIVE',0); INSERT INTO dcc_project_product_create_request VALUES(3,1,0,'COMPLETED',100,10,99);", 'UNRESOLVED_PRODUCT'),
            ('confirmed plus approved same product accepted', "INSERT INTO dcc_project_product_relation VALUES(2,1,100,10,3,'ACTIVE',0); INSERT INTO dcc_project_product_create_request VALUES(3,1,0,'COMPLETED',100,10,2);", None),
            ('conflict in a second tenant checked', "INSERT INTO dcc_registration_certificate VALUES(99,2,200,22,NULL,0);", 'UNRESOLVED_PRODUCT'),
            ('deleted certificate ignored', 'UPDATE dcc_registration_certificate SET deleted=1; DELETE FROM dcc_data_relation;', None),
            ('partial prior binding table rejected', 'CREATE TABLE dcc_registration_certificate_product(id BIGINT);', 'PRIOR_BINDING_TABLE_REQUIRES_REVIEW'),
        ]
        preflight = (SQL_ROOT / 'target-preflight' / (MID + '.preflight.sql')).read_text('utf-8')
        guard = (SQL_ROOT / (MID + '.sql')).read_text('utf-8')
        for name, mutation, reason in cases:
            with self.subTest(name=name):
                self.query(SETUP + mutation)
                expected = ('TARGET_PREFLIGHT_PASS:' + MID if reason is None
                            else 'TARGET_PREFLIGHT_BLOCKED:' + MID + ':' + reason)
                readonly = self.query('USE product_admission_fixture; START TRANSACTION READ ONLY;\n'
                                      + preflight + '\nROLLBACK;')
                self.assertEqual(expected, readonly.stdout.strip())
                applied = self.query('USE product_admission_fixture;\n' + guard, allow_failure=True)
                if reason:
                    self.assertNotEqual(0, applied.returncode)
                    self.assertIn('ERROR 1644 (45000)', applied.stderr)
                    self.assertIn(reason, applied.stderr)
                else:
                    self.assertEqual(0, applied.returncode, applied.stderr)
                    self.assertEqual('0', self.query("SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='product_admission_fixture' AND TABLE_NAME='dcc_registration_certificate_product';").stdout.strip())
                self.assertEqual('1', self.query('SELECT COUNT(*) FROM product_admission_fixture.dcc_registration_certificate_snapshot;').stdout.strip())
                type(self).checked += 1

    def test_original_first_assertion_fails_before_business_writes(self):
        self.query(SETUP + 'DELETE FROM dcc_data_relation;')
        before = self.query('SELECT SHA2(CAST(facts_json AS CHAR),256) FROM product_admission_fixture.dcc_registration_certificate_snapshot;').stdout
        original = (SQL_ROOT / '20261125_dcc_registration_certificate_products.sql').read_text('utf-8')
        result = self.query('USE product_admission_fixture;\n' + original, allow_failure=True)
        self.assertNotEqual(0, result.returncode)
        self.assertIn('ERROR 1644 (45000)', result.stderr)
        self.assertIn('Unresolved DCC product identity', result.stderr)
        self.assertEqual('0', self.query('SELECT COUNT(*) FROM product_admission_fixture.dcc_registration_certificate_product;').stdout.strip())
        self.assertEqual(before, self.query('SELECT SHA2(CAST(facts_json AS CHAR),256) FROM product_admission_fixture.dcc_registration_certificate_snapshot;').stdout)
        self.assertEqual('0', self.query("SELECT COUNT(*) FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA='product_admission_fixture' AND TABLE_NAME='dcc_registration_certificate_product';").stdout.strip())
        self.assertEqual('1', self.query('SELECT COUNT(*) FROM product_admission_fixture.dcc_registration_certificate c JOIN product_admission_fixture.dcc_registration_certificate_snapshot s ON c.tenant_id=s.tenant_id AND c.current_snapshot_id=s.id;').stdout.strip())


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--container', required=True)
    args = parser.parse_args()
    ProductAdmissionTests.container = args.container
    result = unittest.TextTestRunner(verbosity=2).run(
        unittest.defaultTestLoader.loadTestsFromTestCase(ProductAdmissionTests))
    if result.wasSuccessful():
        print('PRODUCT_ADMISSION_MYSQL_PASS:' + str(ProductAdmissionTests.checked))
    raise SystemExit(not result.wasSuccessful())
