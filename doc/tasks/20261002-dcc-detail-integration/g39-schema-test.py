"""Finite offline actual-capture CHECK parser regression; no transport/client."""
import importlib.util
import copy
import hashlib
import json
from pathlib import Path
import unittest

HERE = Path(__file__).resolve().parent
ACTUAL = Path('C:/IntRuoyiBackups/20261003-dcc-integration/g39-clone-migration-run/first-postflight-captured-after-validator-stop.json')
ACTUAL_SHA = '0d51a3fa3c0c940c3ecfe582dfe1b140e0eeeae8424c8a78d0b9c7699d366200'

class ActualCheckTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.path = HERE / ('g39-schema.py' if (HERE / 'g39-schema.py').exists() else 'g28-schema.py')
        spec = importlib.util.spec_from_file_location('g39_schema_tested', cls.path)
        cls.s = importlib.util.module_from_spec(spec); spec.loader.exec_module(cls.s)
        assert hashlib.sha256(ACTUAL.read_bytes()).hexdigest() == ACTUAL_SHA
        cls.rows = json.loads(ACTUAL.read_text(encoding='utf-8-sig'))
        cls.contract = cls.s.prepare_contract()

    def test_actual104_clone_facts_all7_checks_validate_exact_source_contract(self):
        self.assertEqual(104, len(self.rows)); self.assertEqual(7, sum(r['kind']=='check' for r in self.rows))
        self.assertEqual('POSTFLIGHT_PASS', self.s.validate_facts(self.contract,self.rows,'post',database='dcc_intqms_g18_rehearsal'))

    def test_actual_literals_wrong_case_underscore_or_regex_rejected(self):
        for old,new in [('MATCH','match'),('LEGACY_GROUP','LEGACY'),('MODERN','modern'),('^[0-9a-f]{64}$','^[0-9a-f]{63}$')]:
            rows=copy.deepcopy(self.rows); row=next(r for r in rows if r['kind']=='check' and old in r['expression']);row['expression']=row['expression'].replace(old,new,1)
            with self.assertRaises(ValueError):self.s.validate_facts(self.contract,rows,'post',database='dcc_intqms_g18_rehearsal')

    def test_outer_escaped_quotes_never_strip_literal_payload_backslashes(self):
        ordinary=r"value REGEXP '^A\\d+_Case$'"
        escaped=r"regexp_like(`value`,_utf8mb4\'^A\\d+_Case$\')"
        self.assertEqual(self.s.check_ast(ordinary),self.s.check_ast(escaped))
        self.assertNotEqual(self.s.check_ast(escaped),self.s.check_ast(r"regexp_like(`value`,_utf8mb4\'^Ad+_Case$\')"))

    def test_external_introducer_only_and_malformed_tokens_fail(self):
        self.assertEqual(self.s.check_ast("kind='LEGACY_GROUP'"),self.s.check_ast(r"kind=_utf8mb4\'LEGACY_GROUP\'"))
        for text in [r"kind=_utf8mb4\'LEGACY_GROUP'",r"kind=_utf8mb4'LEGACY_GROUP\'",r"kind=\'LEGACY_GROUP\'",r"kind=_utf8mb4\'LEGACY_GROUP\' unsupported",r"kind=_utf8mb4\'UNTERMINATED"]:
            with self.assertRaises(ValueError):self.s.check_ast(text)

if __name__=='__main__':unittest.main()
