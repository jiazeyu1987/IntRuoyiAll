"""MySQL regression for the identifier predicates actually used by approval SQL."""
import itertools
import re
import unittest
from pathlib import Path
import test_dcc_workbench_regexp_collation as db
MAPPER=Path(__file__).resolve().parents[2]/'yudao-module-bpm/src/main/resources/mapper/approval/ApprovalFlowableTaskQueryMapper.xml'
class ApprovalIdentifierCollationTest(unittest.TestCase):
 def test_cross_schema_identifiers_are_exact_for_all_collations(self):
  text=MAPPER.read_text(encoding='utf-8')
  predicates=re.findall(r'CAST\((\w+\.\w+) AS BINARY\)=CAST\((\w+\.\w+) AS BINARY\)',text)
  self.assertGreaterEqual(len(set(predicates)),12)
  for left,right in sorted(set(predicates)):
   la,lc=left.split('.');ra,rc=right.split('.')
   for ca,cb in itertools.product((*db.COLLATIONS,'utf8mb4_bin'),repeat=2):
    for lv,rv,expected in [('Id-1','Id-1','1'),('Id-1','id-1','0'),('Id-1','Id-1 ','0')]:
     def table(alias,col,collation,value):
      import json
      return "JSON_TABLE("+db.literal(json.dumps({'v':value}),collation)+", '$' COLUMNS("+col+" VARCHAR(128) CHARACTER SET utf8mb4 COLLATE "+collation+" PATH '$.v')) "+alias
     sql='SELECT CAST('+left+' AS BINARY)=CAST('+right+' AS BINARY) FROM '+table(la,lc,ca,lv)+' CROSS JOIN '+table(ra,rc,cb,rv)
     with self.subTest(predicate=(left,right),collations=(ca,cb),values=(lv,rv)):self.assertEqual(expected,db.mysql(sql))
if __name__=='__main__':unittest.main()
