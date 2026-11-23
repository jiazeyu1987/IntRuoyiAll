"""Explicit integration run; all fixtures shadow real tables with connection-local TEMPORARY tables.

Requires REGCERT_REPAIR_DB_* settings. No business table is written by these tests.
Run directly; do not silently skip when the real MySQL prerequisite is missing.
"""
from datetime import datetime
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('reg_download_repair', Path(__file__).parents[1] / 'repair' /
                                            'repair-registration-download-summary.py')
repair = importlib.util.module_from_spec(spec)
spec.loader.exec_module(repair)


class DownloadSummaryMySqlCheck(unittest.TestCase):
    def setUp(self):
        self.c = repair.connect()
        self.addCleanup(self.c.close)
        self.tmp = tempfile.TemporaryDirectory(prefix='reg-download-summary-')
        self.addCleanup(self.tmp.cleanup)
        self.evidence = Path(self.tmp.name) / 'evidence.json'
        definitions = {
            'dcc_registration_certificate_access_request': 'id BIGINT,tenant_id BIGINT,deleted INT,request_type VARCHAR(64),bpm_process_instance_id VARCHAR(64),status VARCHAR(32),detail_json JSON,owner_company_id BIGINT,certificate_id BIGINT,requested_at DATETIME',
            'ACT_HI_PROCINST': 'PROC_INST_ID_ VARCHAR(64),TENANT_ID_ VARCHAR(32),BUSINESS_KEY_ VARCHAR(128),END_TIME_ DATETIME,PROC_DEF_ID_ VARCHAR(64)',
            'ACT_RE_PROCDEF': 'ID_ VARCHAR(64),KEY_ VARCHAR(128),TENANT_ID_ VARCHAR(32)',
            'dcc_registration_certificate_bpm_binding': 'tenant_id BIGINT,request_id BIGINT,deleted INT,business_key VARCHAR(128),bpm_process_instance_id VARCHAR(64)',
            'ACT_RU_EXECUTION': 'ID_ VARCHAR(64),PROC_INST_ID_ VARCHAR(64)',
            'ACT_HI_VARINST': 'ID_ VARCHAR(64) PRIMARY KEY,REV_ INT,PROC_INST_ID_ VARCHAR(64),EXECUTION_ID_ VARCHAR(64),TASK_ID_ VARCHAR(64),NAME_ VARCHAR(255),VAR_TYPE_ VARCHAR(100),TEXT_ VARCHAR(4000),CREATE_TIME_ DATETIME(3),LAST_UPDATED_TIME_ DATETIME(3)',
            'mdm_enterprise': 'id BIGINT,tenant_id BIGINT,type VARCHAR(32),deleted INT,name VARCHAR(255)',
            'dcc_registration_certificate_access_request_file': 'id BIGINT,tenant_id BIGINT,request_id BIGINT,business_file_id BIGINT,deleted INT',
            'dcc_registration_certificate_file': 'id BIGINT,tenant_id BIGINT,owner_id BIGINT,owner_type VARCHAR(32),deleted INT',
            'dcc_registration_certificate_version': 'id BIGINT,tenant_id BIGINT,certificate_id BIGINT,deleted INT',
            'dcc_registration_certificate_snapshot': 'id BIGINT,tenant_id BIGINT,version_id BIGINT,deleted INT,product_name VARCHAR(255),create_time DATETIME,revision_no INT',
            'dcc_registration_certificate_change': 'id BIGINT,tenant_id BIGINT,source_version_id BIGINT,certificate_id BIGINT,deleted INT',
            'dcc_registration_certificate_supporting_document': 'id BIGINT,tenant_id BIGINT,version_id BIGINT,business_file_id BIGINT,certificate_id BIGINT,deleted INT',
        }
        with self.c.cursor() as q:
            self.assertTrue(self.c.get_server_info().startswith('8.'), 'MySQL 8 required')
            for table, columns in definitions.items():
                q.execute(f'CREATE TEMPORARY TABLE {table} ({columns}) DEFAULT CHARSET=utf8mb4')
            q.execute("INSERT INTO dcc_registration_certificate_access_request VALUES (578,186,0,'DOWNLOAD_FILE','proc','APPROVED',%s,10,20,'2026-11-19 10:00:00')",
                      (json.dumps(dict(requestType='DOWNLOAD_FILE', businessFileIds=[30], payloadHash='untouched')),))
            q.execute("INSERT INTO ACT_HI_PROCINST VALUES('proc','186','request-578','2026-11-19 11:00:00','def')")
            q.execute("INSERT INTO ACT_RE_PROCDEF VALUES('def','dcc-registration-certificate-access','186')")
            q.execute("INSERT INTO dcc_registration_certificate_bpm_binding VALUES(186,578,0,'request-578','proc')")
            q.execute("INSERT INTO mdm_enterprise VALUES(10,186,'OWNED_COMPANY',0,'所属公司')")
            q.execute("INSERT INTO dcc_registration_certificate_access_request_file VALUES(1,186,578,30,0)")
            q.execute("INSERT INTO dcc_registration_certificate_file VALUES(30,186,40,'VERSION',0)")
            q.execute('INSERT INTO dcc_registration_certificate_version VALUES(40,186,20,0)')
            q.execute("INSERT INTO dcc_registration_certificate_snapshot VALUES(50,186,40,0,'申请时证名','2026-11-19 09:00:00',1),(51,186,40,0,'后续证名','2026-11-20',2)")
        self.c.commit()

    def run_repair(self, **kw):
        try:
            return repair.repair(self.c, 186, 578, 'proc', **kw)
        except BaseException:
            self.c.rollback()
            raise

    def values(self):
        with self.c.cursor() as q:
            q.execute('SELECT NAME_,TEXT_ FROM ACT_HI_VARINST ORDER BY NAME_')
            return list(q.fetchall())

    def test_readonly_then_atomic_apply_and_replay_keeps_original_business_facts(self):
        plan = self.run_repair()
        self.assertEqual([], self.values())
        self.assertEqual('申请时证名', plan['insertVariables']['certificateName'])
        self.run_repair(apply=True, evidence=self.evidence)
        self.assertEqual(2, len(self.values()))
        saved = json.loads(self.evidence.read_text(encoding='utf-8'))
        self.assertEqual('committed', saved['status'])
        self.assertEqual(2, len(saved['insertedVariableIds']))
        self.assertEqual({}, self.run_repair(apply=True, evidence=self.evidence)['insertVariables'])
        with self.c.cursor() as q:
            q.execute('SELECT status,detail_json FROM dcc_registration_certificate_access_request WHERE id=578')
            row = q.fetchone()
        self.assertEqual('APPROVED', row['status'])
        self.assertEqual(dict(requestType='DOWNLOAD_FILE', businessFileIds=[30], payloadHash='untouched'), json.loads(row['detail_json']))

    def test_runtime_process_and_missing_file_and_cross_tenant_binding_refuse_all_writes(self):
        for sql, restore in [
            ("INSERT INTO ACT_RU_EXECUTION VALUES('execution','proc')", 'DELETE FROM ACT_RU_EXECUTION'),
            ('UPDATE dcc_registration_certificate_file SET deleted=1', 'UPDATE dcc_registration_certificate_file SET deleted=0'),
            ("UPDATE ACT_HI_PROCINST SET TENANT_ID_='999'", "UPDATE ACT_HI_PROCINST SET TENANT_ID_='186'"),
            ("UPDATE dcc_registration_certificate_bpm_binding SET business_key='other'", "UPDATE dcc_registration_certificate_bpm_binding SET business_key='request-578'"),
        ]:
            with self.c.cursor() as q: q.execute(sql)
            self.c.commit()
            with self.assertRaises(ValueError): self.run_repair(apply=True, evidence=self.evidence)
            self.assertEqual([], self.values())
            self.assertFalse(self.evidence.exists())
            with self.c.cursor() as q: q.execute(restore)
            self.c.commit()

    def test_existing_evidence_cannot_be_overwritten(self):
        self.evidence.write_text('preserve', encoding='utf-8')
        with self.assertRaises(FileExistsError): self.run_repair(apply=True, evidence=self.evidence)
        self.assertEqual([], self.values())
        self.assertEqual('preserve', self.evidence.read_text(encoding='utf-8'))


if __name__ == '__main__':
    unittest.main()
