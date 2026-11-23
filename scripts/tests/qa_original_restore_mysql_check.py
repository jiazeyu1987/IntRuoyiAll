"""Check restoration on connection-local copies of actual schemas and evidence."""
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
s = importlib.util.spec_from_file_location('restore', ROOT / 'scripts/repair/restore-qa134-original-row.py')
r = importlib.util.module_from_spec(s)
s.loader.exec_module(r)
SOURCE = ROOT / 'workspace/qa134-original-binlog-evidence-20261122.json'


class RestoreMySqlCheck(unittest.TestCase):
    def setUp(self):
        self.c = r.db.connect()
        self.addCleanup(self.c.close)
        self.tmp = tempfile.TemporaryDirectory(prefix='qa-restore-')
        self.addCleanup(self.tmp.cleanup)
        self.out = Path(self.tmp.name) / 'repair.json'
        self.before = {}
        with self.c.cursor() as q:
            for table, where in [(r.TABLE,'id=134'), ('system_electronic_signature','id=64662'),
                                 ('mes_active_order_handoff_task','id=2123151543621255168')]:
                q.execute('SELECT * FROM ' + table + ' WHERE ' + where)
                rows = list(q.fetchall())
                self.before[table] = rows
                q.execute('SHOW CREATE TABLE ' + table)
                ddl = q.fetchone()['Create Table']
                q.execute(ddl.replace('CREATE TABLE','CREATE TEMPORARY TABLE',1))
                if table != r.TABLE:
                    for row in rows:
                        cols = list(row)
                        q.execute('INSERT INTO '+table+' ('+','.join('`'+k+'`' for k in cols)+') VALUES ('+','.join(['%s']*len(cols))+')',list(row.values()))
        self.c.commit()

    def call(self, source=SOURCE, **kw):
        self.c.rollback()
        try:
            return r.repair(self.c, source, **kw)
        except Exception:
            self.c.rollback()
            raise

    def test_readonly_restore_all_columns_and_replay(self):
        self.assertTrue(self.call()['needsInsert'])
        with self.c.cursor() as q:
            q.execute('SELECT COUNT(*) n FROM '+r.TABLE)
            self.assertEqual(0,q.fetchone()['n'])
        self.call(apply=True,evidence=self.out)
        self.assertFalse(self.call(apply=True,evidence=self.out)['needsInsert'])
        with self.c.cursor() as q:
            for table in ['system_electronic_signature','mes_active_order_handoff_task']:
                q.execute('SELECT * FROM '+table)
                self.assertEqual(self.before[table],list(q.fetchall()))

    def test_corrupt_or_changed_signed_content_rejected(self):
        data=json.loads(SOURCE.read_text(encoding='utf-8'))
        for target in ['trace_snapshot_json','review_opinion']:
            altered=json.loads(json.dumps(data))
            altered['matches'][2]['row']['values'][target]='tampered'
            source=Path(self.tmp.name)/(target+'.json')
            source.write_text(json.dumps(altered),encoding='utf-8')
            with self.assertRaises(ValueError):self.call(source,apply=True,evidence=self.out)
        self.assertFalse(self.out.exists())

    def test_conflicting_signature_or_handoff_rejected(self):
        for table,mutation in [('system_electronic_signature',"verification_status='INVALID'"),
                               ('mes_active_order_handoff_task',"status='CANCELED'")]:
            with self.c.cursor() as q:q.execute('UPDATE '+table+' SET '+mutation)
            self.c.commit()
            with self.assertRaises(ValueError):self.call(apply=True,evidence=self.out)
            self.assertFalse(self.out.exists())

    def test_evidence_is_exclusive_and_existing_row_never_overwritten(self):
        self.out.write_text('keep',encoding='utf-8')
        with self.assertRaises(FileExistsError):self.call(apply=True,evidence=self.out)
        self.out.unlink()
        self.call(apply=True,evidence=self.out)
        with self.c.cursor() as q:q.execute('UPDATE '+r.TABLE+" SET review_opinion='conflicting'")
        self.c.commit()
        with self.assertRaises(ValueError):self.call(apply=True,evidence=self.out)


if __name__=='__main__':unittest.main()
