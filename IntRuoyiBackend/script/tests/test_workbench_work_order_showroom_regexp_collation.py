"""Offline XML contract and SELECT-only case generator; no DB connection.

Run --generate to print JSON {sql, expected} records for the authorized original
Connector/J connection. Execute every record and compare its one returned row;
capture VERSION(), driverVersion and @@collation_connection separately. Never
change connection settings. This generator does not claim runtime PASS.
"""
import hashlib
import json
import re
import sys
import unittest
from pathlib import Path
import xml.etree.ElementTree as ET

BACKEND = Path(__file__).resolve().parents[2]
BOUNDARY = "\t\n\v\f\r \u00a0\u1680\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2007\u2008\u2009\u200a\u2028\u2029\u202f\u205f\u3000\ufeff"
COLLATIONS = ("utf8mb4_unicode_ci", "utf8mb4_0900_ai_ci")
SOURCES = {
 "WORK_ORDER": ("mes", "Mes", "baseWorkOrder", 8, "f62e9df1365a8b4c4c4716d43d67c3e3c25482560ba121cc8cdcc9a1f7aecb4b"),
 "SHOWROOM_ASSIGNMENT": ("showroom", "Showroom", "baseAssignment", 6, "89d1f0b5f58c49a53c83c86ca0be79ba7a9b7c45762f0619105bd920f3f73715"),
}

def mapper(source):
 module, prefix, base, _, _ = SOURCES[source]
 return BACKEND / ("yudao-module-" + module + "/src/main/resources/mapper/profileworkbench/" + prefix + "WorkbenchTodoMapper.xml")

def detail(source):
 base = SOURCES[source][2]
 text = ET.parse(mapper(source)).getroot().find("sql[@id='" + base + "']").text
 match = re.search(r"(CONCAT_WS\(' · ',.*?\)) AS detail,", text, re.S)
 if not match: raise ValueError("Actual detail missing: " + source)
 return match[1]

def arguments(expression, function):
 """Extract actual balanced SQL arguments, respecting quoted SQL literals."""
 results = []
 for match in re.finditer(re.escape(function) + r"\(", expression):
  depth, quoted, start, parts, i = 1, False, match.end(), [], match.end()
  while i < len(expression):
   char = expression[i]
   if char == "'":
    if quoted and i + 1 < len(expression) and expression[i + 1] == "'": i += 2; continue
    quoted = not quoted
   elif not quoted:
    if char == "(": depth += 1
    elif char == ")":
     depth -= 1
     if depth == 0: parts.append(expression[start:i].strip()); break
    elif char == "," and depth == 1: parts.append(expression[start:i].strip()); start = i + 1
   i += 1
  if depth: raise ValueError("Unbalanced actual SQL")
  results.append(parts)
 return results

def fixture(source, values, collation):
 # JSON transport collation does not confer EXPLICIT coercibility to its typed
 # output columns. Only those columns are used by the actual detail expression.
 payload = json.dumps(values, ensure_ascii=False).encode("utf-8").hex()
 columns = []
 for key in values:
  numeric = key in ("quantity", "target_id")
  kind = "DECIMAL(14,2)" if key == "quantity" else "BIGINT" if numeric else "VARCHAR(512) CHARACTER SET utf8mb4 COLLATE " + collation
  columns.append(key + " " + kind + " PATH '$." + key + "' ERROR ON EMPTY ERROR ON ERROR")
 return "JSON_TABLE(CONVERT(UNHEX('" + payload + "') USING utf8mb4), '$' COLUMNS(" + ", ".join(columns) + ")) f"

def mapped(expression):
 # Alias mapping only: production CASE, trim, formatting and NULLIF untouched.
 return re.sub(r"\b(?:w|i|a|n)\.", "f.", expression)

def cases():
 work = [
  ({"code": BOUNDARY + "工单 Aaé（中文）" + BOUNDARY, "name": BOUNDARY + "产品\u3000内部" + BOUNDARY, "batch_code": BOUNDARY + "批次" + BOUNDARY, "quantity": 1.2}, "工单 Aaé（中文） · 产品\u3000内部 · 批次 · 数量 1.2"),
  ({"code": None, "name": None, "batch_code": None, "quantity": None}, ""),
  ({"code": "", "name": BOUNDARY, "batch_code": BOUNDARY, "quantity": 0}, ""),
  ({"code": "\u200b编号\u200b", "name": "中文\u00a0内", "batch_code": "", "quantity": 1}, "\u200b编号\u200b · 中文\u00a0内 · 数量 1"),
  ({"code": "工单", "name": None, "batch_code": "批次", "quantity": 1.20}, "工单 · 批次 · 数量 1.2"),
 ]
 show = [
  ({"target_type": "COMPANY", "target_id": 17, "field_code": "development_history", "template_content": BOUNDARY + "中文通知" + BOUNDARY}, "公司#17 · 发展历程 · 中文通知"),
  ({"target_type": "PRODUCT", "target_id": 42, "field_code": "core_selling_points", "template_content": ""}, "产品#42 · 卖点文案"),
  ({"target_type": BOUNDARY + "未知Aaé", "target_id": 91, "field_code": BOUNDARY + "未知字段" + BOUNDARY, "template_content": "通知\u00a0内\u200b部"}, "未知Aaé#91 · 未知字段 · 通知\u00a0内\u200b部"),
  ({"target_type": None, "target_id": 99, "field_code": None, "template_content": None}, ""),
  ({"target_type": "", "target_id": None, "field_code": BOUNDARY, "template_content": BOUNDARY}, ""),
  ({"target_type": "PRODUCT", "target_id": 123, "field_code": "\u200b字段\u200b", "template_content": "\u200b通知\u200b"}, "产品#123 · \u200b字段\u200b · \u200b通知\u200b"),
 ]
 # Freeze all current known labels as expected behavior, independent of SQL.
 labels = {
  "COMPANY": {"development_history":"发展历程", "park_introduction":"园区介绍", "incubation_platform":"孵化平台", "subsidiary_overview":"子公司概览", "stock_info":"股权信息", "core_manufacturing_capability":"核心制造能力", "honors_awards":"荣誉奖项"},
  "PRODUCT": {"owner_company_id":"所属公司", "product_owner_type":"产品归属/类型", "lifecycle_stage":"生命周期", "target_market":"在售国家", "pipeline_layout":"BU", "indication_content":"适应症", "core_selling_points":"卖点文案", "model_specification":"型号规格", "cover_image":"封面", "registration_certificate":"注册证", "clinical_effect":"临床效果", "fim_status":"FIM状态", "__PRODUCT_ALL_FIELDS__":"整产品资料"},
 }
 for target, fields in labels.items():
  for field, label in fields.items():
   show.append(({"target_type":target, "target_id":37, "field_code":field, "template_content":None}, ("公司" if target == "COMPANY" else "产品") + "#37 · " + label))
 return {"WORK_ORDER":work, "SHOWROOM_ASSIGNMENT":show}

def records():
 for source, source_cases in cases().items():
  expression = detail(source)
  regexes = arguments(expression, "REGEXP_REPLACE")
  for collation in COLLATIONS:
   for index, (fields, expected) in enumerate(source_cases):
    row = " FROM " + fixture(source, fields, collation)
    yield {"name":source + "/" + collation + "/detail/" + str(index), "sql":"SELECT HEX(" + mapped(expression) + ")" + row, "expected":[expected.encode("utf-8").hex().upper()]}
   fields = source_cases[0][0]
   row = " FROM " + fixture(source, fields, collation)
   column = "code" if source == "WORK_ORDER" else "field_code"
   yield {"name":source + "/" + collation + "/implicit-column", "sql":"SELECT COLLATION(f." + column + "), COERCIBILITY(f." + column + ")" + row, "expected":[collation, "2"]}
   for index, (expr, pattern, replacement) in enumerate(regexes):
    # Probe EACH real expr and full pattern; output exercises REGEXP_REPLACE,
    # unlike COUNT which may be optimized without evaluating the projection.
    sql = "SELECT COLLATION(" + pattern + "), COERCIBILITY(" + pattern + "), COLLATION(" + mapped(expr) + "), COERCIBILITY(" + mapped(expr) + "), HEX(REGEXP_REPLACE(" + mapped(expr) + ", " + pattern + ", " + replacement + "))" + row
    outputs = ["工单 Aaé（中文）", "产品\u3000内部", "批次", "数量 1.2"] if source == "WORK_ORDER" else ["公司#17", "发展历程", "中文通知"]
    yield {"name":source + "/" + collation + "/regexp/" + str(index), "sql":sql,
     "expected_pattern":["utf8mb4_unicode_ci", "0"], "expected_hex":outputs[index].encode("utf-8").hex().upper(),
     "assert_columns":{"0":"utf8mb4_unicode_ci", "1":"0", "4":outputs[index].encode("utf-8").hex().upper()},
     "inspect_expr_collation_coercibility_columns":[2,3]}

class OfflineContract(unittest.TestCase):
 def test_exact_inverse_bytes_and_xml(self):
  for source, (_, _, _, count, old_hash) in SOURCES.items():
   raw = mapper(source).read_bytes()
   self.assertEqual(hashlib.sha256(raw.replace(b" COLLATE utf8mb4_unicode_ci",b"")).hexdigest(),old_hash)
   expression = detail(source)
   self.assertEqual(expression.count("COLLATE utf8mb4_unicode_ci"),count)
   regexes = arguments(expression,"REGEXP_REPLACE")
   self.assertEqual(len(regexes),count // 2)
   for _, pattern, replacement in regexes:
    self.assertEqual(replacement,"''")
    self.assertEqual(pattern.count("COLLATE utf8mb4_unicode_ci"),2)
    for value in re.findall(r"UNHEX\('([0-9A-F]+)'\)",pattern):
     self.assertEqual(bytes.fromhex(value).decode("utf-8"),BOUNDARY)
 def test_generator_select_only_implicit_fixtures_and_actual_expressions(self):
  generated = list(records())
  self.assertEqual(len(generated),80)
  for case in generated:
   self.assertTrue(case["sql"].startswith("SELECT "))
   self.assertNotIn(";",case["sql"])
   self.assertIn("JSON_TABLE(",case["sql"])
   self.assertNotIn("#{",case["sql"])
   self.assertNotRegex(case["sql"],r"\b(?:w|i|a|n)\.")
   self.assertIn(" ERROR ON EMPTY ERROR ON ERROR",case["sql"])
   self.assertNotIn("SET NAMES",case["sql"])

if __name__ == "__main__":
 if sys.argv[1:] == ["--generate"]: print(json.dumps(list(records()),ensure_ascii=False,indent=2))
 else: unittest.main(verbosity=2)
