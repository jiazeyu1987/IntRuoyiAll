import copy
import json
from pathlib import Path
import unittest

from g21_execution_materials import compose_script, sql_literal
from g21_migration_scope import ScopeError

TASK = Path(__file__).resolve().parent
BACKEND = Path('C:/IntRuoyi/20261001-dcc-integration/IntRuoyiBackend')


class ScriptCompositionTests(unittest.TestCase):
    def setUp(self):
        self.entries = json.loads((TASK / 'g18-approved-scope-candidate.json').read_text(encoding='utf-8-sig'))['executionOrder']

    def test_first_executes_only_real_candidates_and_only_appends_new_task_ledger(self):
        text = compose_script(self.entries, BACKEND, 'dcc-g21-unit-review', repeat=False).decode('utf-8')
        self.assertEqual(text.count("'taskPhase','sql_begin'"), 19)
        self.assertEqual(text.count('INSERT INTO infra_release_migration'), 19)
        self.assertNotIn('ON DUPLICATE KEY UPDATE', text)
        self.assertNotIn('UPDATE infra_release_migration', text)
        self.assertNotIn('20260710_dcc_product_catalog_database.sql', text)
        self.assertNotIn('20260513_dcc_base_schema.sql', text)

    def test_repeat_retains_all_nineteen_raw_sql_but_never_writes_ledger(self):
        text = compose_script(self.entries, BACKEND, 'dcc-g21-unit-review', repeat=True).decode('utf-8')
        self.assertEqual(text.count("'taskPhase','sql_complete'"), 19)
        self.assertNotIn('infra_release_migration', text)

    def test_metadata_injection_extra_identity_and_changed_sql_reject(self):
        for value in ["x'; DROP TABLE t;", 'x\nUSE other', '中文']:
            with self.assertRaises(ValueError): sql_literal(value)
        entries = copy.deepcopy(self.entries); entries[0]['sha256'] = '0' * 64
        with self.assertRaises(ScopeError): compose_script(entries, BACKEND, 'test-id', repeat=False)
        with self.assertRaises(ScopeError): compose_script(self.entries[:-1], BACKEND, 'test-id', repeat=False)


if __name__ == '__main__': unittest.main(verbosity=2)
