"""Execute the real retention migration in a disposable MySQL schema only.

Requires QA_AUDIT_DB_* credentials with CREATE DATABASE permission.
Business tables are never modified. Scratch schema is always removed.
"""
import importlib.util
from pathlib import Path
import re
import unittest
import uuid

import pymysql

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('qa_audit', ROOT / 'scripts/repair/audit-qa-handoff-integrity.py')
audit = importlib.util.module_from_spec(spec)
spec.loader.exec_module(audit)


class QaRetentionMySqlCheck(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.connection = audit.connect()
        cls.addClassCleanup(cls.connection.close)
        cls.schema = 'qa_retention_test_' + uuid.uuid4().hex
        with cls.connection.cursor() as q:
            q.execute('CREATE DATABASE `' + cls.schema + '`')
        cls.addClassCleanup(cls.drop_schema)
        cls.connection.select_db(cls.schema)
        with cls.connection.cursor() as q:
            q.execute('CREATE TABLE mes_pro_edhr_nonconformance_review ('
                      'id BIGINT PRIMARY KEY,tenant_id BIGINT NOT NULL,review_status VARCHAR(32),'
                      'deleted BIT NOT NULL DEFAULT 0,disposition VARCHAR(32)) ENGINE=InnoDB')
            q.execute('CREATE TABLE mes_active_order_handoff_task ('
                      'id BIGINT PRIMARY KEY,tenant_id BIGINT,source_type VARCHAR(64),source_id BIGINT,'
                      'task_type VARCHAR(64),status VARCHAR(32),active_order_id BIGINT,work_order_id BIGINT,'
                      'deleted BIT NOT NULL DEFAULT 0) ENGINE=InnoDB')
        cls.migration = (ROOT / 'IntRuoyiBackend/sql/mysql/20261122_mes_qa_review_retention_guard.sql').read_text(encoding='utf-8')
        cls.apply_migration()

    @classmethod
    def apply_migration(cls):
        # Match the actual migration statements, without rewriting the trigger body.
        source = re.sub(r'^--.*$', '', cls.migration, flags=re.M)
        head, triggers = source.split('DELIMITER $$', 1)
        bodies, tail = triggers.split('DELIMITER ;', 1)
        assert not tail.strip()
        with cls.connection.cursor() as q:
            for statement in head.split(';') + bodies.split('$$'):
                if statement.strip():
                    q.execute(statement)

    @classmethod
    def drop_schema(cls):
        assert re.fullmatch(r'qa_retention_test_[a-f0-9]{32}', cls.schema)
        cls.connection.rollback()
        with cls.connection.cursor() as q:
            q.execute('DROP DATABASE `' + cls.schema + '`')

    def setUp(self):
        self.connection.rollback()
        with self.connection.cursor() as q:
            q.execute('TRUNCATE TABLE mes_active_order_handoff_task')
            q.execute('TRUNCATE TABLE mes_pro_edhr_nonconformance_review')
            q.execute("INSERT INTO mes_pro_edhr_nonconformance_review(id,tenant_id,review_status) VALUES (1,186,'pending_review')")
        self.connection.commit()

    def execute(self, sql):
        with self.connection.cursor() as q:
            q.execute(sql)

    def link(self, tenant=186, deleted=0, source='NONCONFORMANCE_REVIEW'):
        with self.connection.cursor() as q:
            q.execute('INSERT INTO mes_active_order_handoff_task '
                      '(id,tenant_id,source_type,source_id,task_type,status,deleted) '
                      "VALUES (10,%s,%s,1,'QA_REVIEW','DONE',%s)", (tenant, source, deleted))
        self.connection.commit()

    def assert_blocked(self, sql):
        with self.assertRaises(pymysql.MySQLError) as raised:
            self.execute(sql)
        self.assertEqual(1644, raised.exception.args[0])
        self.assertIn('QA_REVIEW_RETAINED:', raised.exception.args[1])
        self.connection.rollback()

    def test_linked_review_cannot_be_deleted_or_reidentified(self):
        self.link()
        for sql in ('DELETE FROM mes_pro_edhr_nonconformance_review WHERE id=1',
                    'UPDATE mes_pro_edhr_nonconformance_review SET deleted=1 WHERE id=1',
                    'UPDATE mes_pro_edhr_nonconformance_review SET id=2 WHERE id=1',
                    'UPDATE mes_pro_edhr_nonconformance_review SET tenant_id=187 WHERE id=1'):
            with self.subTest(sql=sql):
                self.assert_blocked(sql)

    def test_closed_decision_retained_without_handoff(self):
        self.execute("UPDATE mes_pro_edhr_nonconformance_review SET review_status='closed' WHERE id=1")
        self.connection.commit()
        self.assert_blocked('DELETE FROM mes_pro_edhr_nonconformance_review WHERE id=1')
        self.assert_blocked('UPDATE mes_pro_edhr_nonconformance_review SET deleted=1 WHERE id=1')
        self.assert_blocked("UPDATE mes_pro_edhr_nonconformance_review SET review_status='pending_review' WHERE id=1")

    def test_hidden_handoff_still_requires_source(self):
        self.link(deleted=1)
        self.assert_blocked('DELETE FROM mes_pro_edhr_nonconformance_review WHERE id=1')

    def test_other_tenant_or_source_is_not_a_reference(self):
        self.link(tenant=187)
        self.execute('DELETE FROM mes_pro_edhr_nonconformance_review WHERE id=1')
        self.connection.rollback()
        self.execute('UPDATE mes_active_order_handoff_task SET tenant_id=186,source_type=\'ACTIVE_ORDER\' WHERE id=10')
        self.execute('DELETE FROM mes_pro_edhr_nonconformance_review WHERE id=1')

    def test_normal_disposition_and_idempotent_migration(self):
        self.link()
        self.execute("UPDATE mes_pro_edhr_nonconformance_review SET review_status='closed',disposition='rework' WHERE id=1")
        self.connection.commit()
        self.apply_migration()
        self.assert_blocked('DELETE FROM mes_pro_edhr_nonconformance_review WHERE id=1')

    def test_audit_reports_missing_and_deleted_sources(self):
        self.link()
        self.assertEqual('PASSED', audit.audit(self.connection, 186)['status'])
        self.execute('UPDATE mes_active_order_handoff_task SET source_id=2 WHERE id=10')
        self.connection.commit()
        report = audit.audit(self.connection, 186)
        self.assertEqual('SOURCE_MISSING', report['issues'][0]['issue'])
        self.assertEqual(1, report['affectedReviewCount'])
        self.execute("INSERT INTO mes_pro_edhr_nonconformance_review(id,tenant_id,review_status,deleted) VALUES (2,186,'closed',1)")
        self.connection.commit()
        self.assertEqual('SOURCE_DELETED', audit.audit(self.connection, 186)['issues'][0]['issue'])
        self.assertEqual('PASSED', audit.audit(self.connection, 187)['status'])


if __name__ == '__main__':
    unittest.main()
