"""MySQL integration checks using connection-local fixtures, never business writes."""
from datetime import datetime
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('review_repair', Path(__file__).parents[1] / 'repair/repair-registration-review-comment.py')
repair = importlib.util.module_from_spec(spec)
spec.loader.exec_module(repair)


class ReviewCommentMySqlCheck(unittest.TestCase):
    def setUp(self):
        self.c = repair.summary.connect()
        self.addCleanup(self.c.close)
        self.tmp = tempfile.TemporaryDirectory(prefix='reg-review-check-')
        self.addCleanup(self.tmp.cleanup)
        self.evidence = Path(self.tmp.name) / 'evidence.json'
        schemas = {
            'dcc_registration_certificate_access_request': 'id BIGINT,tenant_id BIGINT,status TEXT,bpm_process_instance_id VARCHAR(64),deleted INT',
            'dcc_registration_certificate_bpm_binding': 'id BIGINT,tenant_id BIGINT,request_id BIGINT,business_key TEXT,bpm_process_instance_id VARCHAR(64),detail_json TEXT,deleted INT',
            'ACT_HI_PROCINST': 'PROC_INST_ID_ VARCHAR(64),PROC_DEF_ID_ VARCHAR(64),TENANT_ID_ VARCHAR(64),BUSINESS_KEY_ TEXT,END_TIME_ DATETIME(3)',
            'ACT_RE_PROCDEF': 'ID_ VARCHAR(64),KEY_ TEXT,TENANT_ID_ VARCHAR(64)',
            'ACT_RU_EXECUTION': 'ID_ VARCHAR(64),PROC_INST_ID_ VARCHAR(64)',
            'ACT_HI_TASKINST': 'ID_ VARCHAR(64),PROC_INST_ID_ VARCHAR(64),ASSIGNEE_ VARCHAR(64),TASK_DEF_KEY_ TEXT,END_TIME_ DATETIME(3),TENANT_ID_ VARCHAR(64)',
            'ACT_HI_VARINST': 'ID_ VARCHAR(64),PROC_INST_ID_ VARCHAR(64),TASK_ID_ VARCHAR(64),NAME_ TEXT,VAR_TYPE_ TEXT,TEXT_ TEXT,LONG_ BIGINT',
            'ACT_HI_COMMENT': 'ID_ VARCHAR(64),PROC_INST_ID_ VARCHAR(64),TASK_ID_ VARCHAR(64),USER_ID_ VARCHAR(64),TYPE_ VARCHAR(64),MESSAGE_ TEXT',
        }
        with self.c.cursor() as q:
            self.assertTrue(self.c.get_server_info().startswith('8.'))
            for table, fields in schemas.items():
                q.execute('CREATE TEMPORARY TABLE ' + table + ' (' + fields + ') ENGINE=InnoDB')
            q.execute("INSERT INTO dcc_registration_certificate_access_request VALUES(572,186,'APPROVED','P1',0)")
            detail = json.dumps(dict(reviewComment=None, reviewerUserId=100, reviewedAt='2026-11-18T22:30:07.6', preserved='keep'))
            q.execute("INSERT INTO dcc_registration_certificate_bpm_binding VALUES(534,186,572,'DCC_REG_CERT_ACCESS:572','P1',%s,0)", (detail,))
            q.execute("INSERT INTO ACT_HI_PROCINST VALUES('P1','D1','186','DCC_REG_CERT_ACCESS:572',%s)", (datetime(2026, 11, 18, 22, 30, 7, 600000),))
            q.execute("INSERT INTO ACT_RE_PROCDEF VALUES('D1','dcc-registration-certificate-access','186')")
            q.execute("INSERT INTO ACT_HI_TASKINST VALUES('T1','P1','100','REG_CERT_ACCESS_APPROVAL',%s,'186')", (datetime(2026, 11, 18, 22, 30, 7, 565000),))
            q.execute("INSERT INTO ACT_HI_VARINST VALUES('V1','P1','T1','TASK_REASON','string','原始意见',NULL),"
                      "('V2','P1','T1','TASK_STATUS','integer','2',2),('V3','P1',NULL,'PROCESS_STATUS','integer','2',2)")
            q.execute("INSERT INTO ACT_HI_COMMENT VALUES('C1','P1','T1','100','1','审批通过，原因是：原始意见')")
        self.c.commit()

    def binding(self):
        with self.c.cursor() as q:
            q.execute('SELECT * FROM dcc_registration_certificate_bpm_binding')
            row = q.fetchone()
        self.c.rollback()
        return row

    def apply(self, **kwargs):
        try:
            return repair.repair(self.c, 186, 572, 'P1', **kwargs)
        except Exception:
            self.c.rollback()
            raise

    def test_readonly_apply_exact_change_and_idempotency(self):
        before = self.binding()
        self.assertTrue(self.apply()['needsUpdate'])
        self.assertEqual(before, self.binding())
        self.apply(apply=True, evidence=self.evidence)
        after = self.binding()
        old_detail = json.loads(before['detail_json'])
        old_detail['reviewComment'] = '原始意见'
        self.assertEqual(old_detail, json.loads(after['detail_json']))
        self.assertEqual({k: v for k, v in before.items() if k != 'detail_json'},
                         {k: v for k, v in after.items() if k != 'detail_json'})
        self.assertEqual('committed', json.loads(self.evidence.read_text(encoding='utf-8'))['status'])
        self.assertFalse(self.apply(apply=True, evidence=self.evidence)['needsUpdate'])
        with self.c.cursor() as q:
            q.execute('SELECT status FROM dcc_registration_certificate_access_request')
            self.assertEqual('APPROVED', q.fetchone()['status'])

    def test_wrong_identity_missing_reason_and_running_process_reject_without_write(self):
        before = self.binding()
        with self.assertRaises(ValueError):
            repair.repair(self.c, 186, 572, 'WRONG', apply=True, evidence=self.evidence)
        self.c.rollback()
        self.assertEqual(before, self.binding())
        for mutate, restore in [
            ("DELETE FROM ACT_HI_VARINST WHERE NAME_='TASK_REASON'", "INSERT INTO ACT_HI_VARINST VALUES('V1','P1','T1','TASK_REASON','string','原始意见',NULL)"),
            ("INSERT INTO ACT_RU_EXECUTION VALUES('RUN','P1')", "DELETE FROM ACT_RU_EXECUTION"),
            ("UPDATE ACT_HI_TASKINST SET ASSIGNEE_='999'", "UPDATE ACT_HI_TASKINST SET ASSIGNEE_='100'"),
            ("UPDATE ACT_HI_TASKINST SET TENANT_ID_='999'", "UPDATE ACT_HI_TASKINST SET TENANT_ID_='186'"),
            ("UPDATE ACT_HI_COMMENT SET MESSAGE_='conflict'", "UPDATE ACT_HI_COMMENT SET MESSAGE_='审批通过，原因是：原始意见'"),
            ("UPDATE ACT_HI_VARINST SET LONG_=3 WHERE NAME_='TASK_STATUS'", "UPDATE ACT_HI_VARINST SET LONG_=2 WHERE NAME_='TASK_STATUS'"),
        ]:
            with self.c.cursor() as q:
                q.execute(mutate)
            self.c.commit()
            with self.assertRaises(ValueError):
                self.apply(apply=True, evidence=self.evidence)
            self.assertEqual(before, self.binding())
            self.assertFalse(self.evidence.exists())
            with self.c.cursor() as q:
                q.execute(restore)
            self.c.commit()

    def test_existing_evidence_or_comment_is_never_overwritten(self):
        before = self.binding()
        self.evidence.write_text('keep', encoding='utf-8')
        with self.assertRaises(FileExistsError):
            self.apply(apply=True, evidence=self.evidence)
        self.assertEqual(before, self.binding())
        self.assertEqual('keep', self.evidence.read_text(encoding='utf-8'))
        detail = json.loads(before['detail_json'])
        detail['reviewComment'] = '另一个已记录意见'
        with self.c.cursor() as q:
            q.execute('UPDATE dcc_registration_certificate_bpm_binding SET detail_json=%s', (json.dumps(detail),))
        self.c.commit()
        with self.assertRaises(ValueError):
            self.apply(apply=True, evidence=self.evidence)
        self.assertEqual(detail, json.loads(self.binding()['detail_json']))


if __name__ == '__main__':
    unittest.main()
