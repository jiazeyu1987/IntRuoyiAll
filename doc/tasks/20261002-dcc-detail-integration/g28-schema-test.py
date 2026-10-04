import importlib.util
from pathlib import Path
import copy
import unittest

HERE=Path(__file__).resolve().parent

class SchemaContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        spec=importlib.util.spec_from_file_location('g28schema',HERE/'g28-schema.py')
        cls.tool=importlib.util.module_from_spec(spec);spec.loader.exec_module(cls.tool)
        cls.contract=cls.tool.prepare_contract()
    def test_exact_sealed_three_tables_and_all_shapes(self):
        self.assertEqual(70,sum(len(t['columns']) for t in self.contract['tables'].values()))
        self.assertEqual(8,sum(c['precision']==6 for t in self.contract['tables'].values() for c in t['columns'].values()))
        self.assertEqual(7,sum(len(t['checks']) for t in self.contract['tables'].values()))
        self.assertEqual(2,sum(bool(c['expression']) for t in self.contract['tables'].values() for c in t['columns'].values()))
        self.tool.validate_facts(self.contract,self.tool.fixture(self.contract),phase='post')
    def test_absent_preflight_is_valid_but_partial_never_is(self):
        facts=self.tool.fixture(self.contract,absent=True)
        self.assertEqual('FIRST_REQUIRED',self.tool.validate_facts(self.contract,facts,phase='pre'))
        facts.append(self.tool.fixture(self.contract)[1])
        with self.assertRaises(ValueError):self.tool.validate_facts(self.contract,facts,phase='pre')
    def test_all_existing_table_shapes_are_required_before_repeat(self):
        facts=self.tool.fixture(self.contract)
        self.assertEqual('REPEAT_ALLOWED',self.tool.validate_facts(self.contract,facts,phase='pre'))
        for mutator in [lambda r:r.pop(),lambda r:r.append(dict(r[1]))]:
            bad=copy.deepcopy(facts);mutator(bad)
            with self.assertRaises(ValueError):self.tool.validate_facts(self.contract,bad,phase='pre')
    def test_runtime_and_wrong_ledger_fail_closed(self):
        for key,value in [('database','other'),('serverUuid','other'),('pageSize',4096),('rowFormat','compact'),('mysqlVersion','8.0.39')]:
            bad=self.tool.fixture(self.contract);bad[0][key]=value
            with self.assertRaises(ValueError):self.tool.validate_facts(self.contract,bad,phase='pre')
        bad=self.tool.fixture(self.contract);next(x for x in bad if x['kind']=='ledger')['sha256']='0'*64
        with self.assertRaises(ValueError):self.tool.validate_facts(self.contract,bad,phase='pre')
    def test_column_type_nullable_charset_default_precision_and_generated_ast(self):
        for key,value in [('type','varchar(12)'),('nullable','NO'),('charset','latin1'),('collation','utf8mb4_general_ci'),('default','wrong'),('precision',0),('extra','VIRTUAL GENERATED'),('expression','LOWER(source_original_file_name)')]:
            bad=self.tool.fixture(self.contract);row=next(x for x in bad if x['kind']=='column' and x['name']=='source_name_key');row[key]=value
            with self.assertRaises(ValueError):self.tool.validate_facts(self.contract,bad,phase='post')
    def test_generated_mysql_cast_binary_semantics_match(self):
        bad=self.tool.fixture(self.contract)
        for row in bad:
            if row['kind']=='column' and row['name']=='source_name_key':row['expression']='cast(`source_original_file_name` as binary)'
        self.tool.validate_facts(self.contract,bad,phase='post')
    def test_exact_index_no_prefix_and_check_enforcement(self):
        for mutation in [('index','prefix',64),('index','unique',False),('index','direction','D'),('index','visible','NO'),('check','enforced','NO'),('check','expression','1=1'),('row_count','count',1)]:
            bad=self.tool.fixture(self.contract);row=next(x for x in bad if x['kind']==mutation[0]);row[mutation[1]]=mutation[2]
            with self.assertRaises(ValueError):self.tool.validate_facts(self.contract,bad,phase='post')
    def test_check_parentheses_changes_do_not_normalize_away(self):
        self.assertNotEqual(self.tool.check_ast("a=1 OR b=2 AND c=3"),self.tool.check_ast("(a=1 OR b=2) AND c=3"))
        self.assertEqual(self.tool.check_ast("a REGEXP '^[0-9a-f]{64}$'"),self.tool.check_ast("regexp_like(`a`,_utf8mb4'^[0-9a-f]{64}$')"))
    def test_preflight_and_postflight_are_select_only(self):
        for text in [self.tool.capture_sql(),self.tool.row_counts_sql()]:
            self.assertTrue(all(s.strip().upper().startswith('SELECT') for s in text.split(';') if s.strip()))
    def test_ordinal_and_case_sensitive_check_literal_are_exact(self):
        bad=self.tool.fixture(self.contract);next(x for x in bad if x['kind']=='column')['ordinal']=2
        with self.assertRaises(ValueError):self.tool.validate_facts(self.contract,bad,'post')
    def test_charset_literal_introducer_never_strips_underscore_inside_payload(self):
        self.assertNotEqual(self.tool.check_ast("reservation_kind='LEGACY_GROUP'"),self.tool.check_ast("reservation_kind='LEGACY'"))
        self.assertEqual(self.tool.check_ast("reservation_kind='LEGACY_GROUP'"),self.tool.check_ast("reservation_kind=_utf8mb4'LEGACY_GROUP'"))
        bad=self.tool.fixture(self.contract);next(x for x in bad if x['kind']=='check')['expression']="status IN ('prepared','verified','invalid')"
        with self.assertRaises(ValueError):self.tool.validate_facts(self.contract,bad,'post')

if __name__=='__main__':unittest.main()
