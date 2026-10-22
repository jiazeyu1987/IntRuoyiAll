import importlib.util, unittest, gzip, json
from pathlib import Path
HERE=Path(__file__).resolve().parent
class DumpParserTest(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  spec=importlib.util.spec_from_file_location('dump_parser',HERE/'g29-dump-parser.py');cls.p=importlib.util.module_from_spec(spec);spec.loader.exec_module(cls.p)
 def schema(self):return {'t':{'columns':['id','payload','computed'],'generatedColumns':['computed'],'insertColumns':['id','payload']}}
 def test_formal_generated_table_explicit_ordered_non_generated_columns(self):
  self.assertEqual('t',self.p.insert_table("INSERT INTO `t` (`id`, `payload`) VALUES (1,'some, text');\n",self.schema()))
 def test_non_generated_table_implicit_column_format(self):
  schema={'t':{'columns':['id','payload'],'generatedColumns':[],'insertColumns':['id','payload']}}
  self.assertEqual('t',self.p.insert_table("INSERT INTO `t` VALUES (1,'text');\n",schema))
 def test_generated_missing_or_unknown_duplicate_reordered_columns_fail(self):
  for line in ["INSERT INTO `t` VALUES (1,'x');","INSERT INTO `t` (`id`,`computed`) VALUES (1,'x');","INSERT INTO `t` (`payload`,`id`) VALUES ('x',1);","INSERT INTO `t` (`id`,`id`) VALUES (1,1);","INSERT INTO `t` (`id`,`other`) VALUES (1,'x');"]:
   with self.assertRaises(ValueError):self.p.insert_table(line,self.schema())
 def test_literals_keep_quote_comma_escaped_terminators_without_extra_rows(self):
  for literal in ["'escaped \\' quote, ); INSERT INTO `t` VALUES (x);'","'doubled '' quote,) text'","_binary 'utf8 and \\n'","NULL","0xAF","b'0101'","X'0AFF'","-12.40e-2"]:
   self.assertEqual('t',self.p.insert_table("INSERT INTO `t` (`id`,`payload`) VALUES (1,"+literal+");",self.schema()))
 def test_multirow_truncated_foreign_and_wrong_value_count_fail(self):
  for line in ["INSERT INTO `t` (`id`,`payload`) VALUES (1,'x'),(2,'y');","INSERT INTO `t` (`id`,`payload`) VALUES (1,'x'); SELECT 1;","INSERT INTO `t` (`id`,`payload`) VALUES (1,'unterminated);","INSERT INTO `t` (`id`,`payload`) VALUES (1);","INSERT INTO `t` (`id`,`payload`) VALUES (1,'x',3);","INSERT INTO `foreign` VALUES (1,'x');"]:
   with self.assertRaises(ValueError):self.p.insert_table(line,self.schema())
 def test_exact_schema_baseline_order_and_generated_projection(self):
  text="CREATE TABLE `t` (\n `id` bigint NOT NULL,\n `payload` varchar(256),\n `computed` varbinary(1024) GENERATED ALWAYS AS (cast(`payload` as binary)) STORED,\n PRIMARY KEY (`id`)\n);\n"
  result=self.p.parse_schema(text,{'t':{'columns':['id','payload','computed']}});self.assertEqual(self.schema(),result)
  with self.assertRaises(ValueError):self.p.parse_schema(text,{'t':{'columns':['payload','id','computed']}})
 def test_actual_preserved_failed_backup_counts_are_exact_without_writing_it(self):
  folder=Path('C:/IntRuoyiBackups/20261003-dcc-integration/g29-clone-fresh-inputs');baseline=json.loads((folder/'fresh-original-baseline.json').read_text(encoding='utf-8-sig'))['tables']
  with gzip.open(folder/'schema.sql.gz','rt',encoding='utf-8') as stream:schema=self.p.parse_schema(stream.read(),baseline)
  with gzip.open(folder/'protected-original-rows.sql.gz','rt',encoding='utf-8') as stream:counts=self.p.scan_data(stream,schema)
  self.assertEqual(41615,counts['dcc_controlled_file']);self.assertEqual(27,counts['dcc_controlled_file_name_claim'])
  self.assertEqual({t:v['count'] for t,v in baseline.items()},counts)
if __name__=='__main__':unittest.main()
