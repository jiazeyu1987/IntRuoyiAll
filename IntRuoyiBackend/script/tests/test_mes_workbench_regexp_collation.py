"""Run actual MES workbench detail/count projections using SELECT-only fixtures."""
import json
import re
import unittest
from pathlib import Path
import xml.etree.ElementTree as ET

import test_dcc_workbench_regexp_collation as fixture

MAPPER = Path(__file__).resolve().parents[2] / "yudao-module-mes/src/main/resources/mapper/profileworkbench/MesWorkbenchTodoMapper.xml"


def detail(base):
    text = ET.parse(MAPPER).getroot().find("sql[@id='" + base + "']").text
    match = re.search(r"(CONCAT_WS\(' · ',.*?\)) AS detail,", text, re.S)
    if match is None:
        raise AssertionError("Actual MES detail projection is missing")
    return match.group(1)


def row(alias, fields, collation):
    columns = ", ".join(
        key + (" DECIMAL(14,2)" if key == "quantity" else " BIGINT" if key == "id" else
               " VARCHAR(512) CHARACTER SET utf8mb4 COLLATE " + collation) +
        " PATH '$." + key + "' ERROR ON EMPTY ERROR ON ERROR" for key in fields
    )
    return "JSON_TABLE(" + fixture.literal(json.dumps(fields, ensure_ascii=False), collation) + \
        ", '$' COLUMNS(" + columns + ")) " + alias


def source(base, fields, collation):
    if base == "baseEdhr":
        return row("mes_pro_edhr_work_task", fields, collation)
    return row("w", {key: value for key, value in fields.items() if key != "name"}, collation) + \
        " CROSS JOIN " + row("i", {"name": fields["name"]}, collation)


def select_detail(base, fields, collation):
    return "SELECT HEX(" + detail(base) + ") FROM " + source(base, fields, collation)


class MesWorkbenchRegexpCollationTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        fixture.DccWorkbenchRegexpCollationTest.setUpClass()

    def test_actual_work_order_projection_preserves_unicode_and_quantity(self):
        boundary = fixture.BOUNDARY
        cases = (
            ({"code": boundary + "工单 编号" + boundary, "name": boundary + "中文\u3000产品" + boundary,
              "batch_code": boundary + "批次甲" + boundary, "quantity": 10.20},
             "工单 编号 · 中文\u3000产品 · 批次甲 · 数量 10.2"),
            ({"code": boundary, "name": None, "batch_code": "", "quantity": 0}, ""),
            ({"code": None, "name": boundary + "中文\u00a0内\u200b边界" + boundary,
              "batch_code": None, "quantity": None}, "中文\u00a0内\u200b边界"),
            ({"code": "工单", "name": "", "batch_code": None, "quantity": -2.50}, "工单 · 数量 -2.5"),
        )
        for collation in fixture.COLLATIONS:
            for fields, expected in cases:
                with self.subTest(source_collation=collation, expected=expected):
                    self.assertEqual(expected.encode("utf-8").hex().upper(),
                                     fixture.mysql(select_detail("baseWorkOrder", fields, collation)))

    def test_actual_edhr_projection_preserves_task_identity_and_labels(self):
        boundary = fixture.BOUNDARY
        cases = (
            ({"id": 37, "task_code": boundary + "任务 编号" + boundary,
              "work_order_code": boundary + "工单甲" + boundary, "batch_code": boundary + "批次甲" + boundary,
              "process_name": boundary + "工序\u3000内部" + boundary, "task_type": "FILL"},
             "任务 编号 · 工单甲 · 批次甲 · 工序\u3000内部 · 填写"),
            ({"id": 37, "task_code": None, "work_order_code": None, "batch_code": boundary,
              "process_name": None, "task_type": "REVIEW"}, "任务#37 · 复核"),
            ({"id": 37, "task_code": "", "work_order_code": "", "batch_code": None,
              "process_name": None, "task_type": boundary + "自定义" + boundary}, "任务#37 · 自定义"),
            ({"id": 37, "task_code": boundary, "work_order_code": None, "batch_code": None,
              "process_name": None, "task_type": None}, ""),
        )
        for collation in fixture.COLLATIONS:
            for fields, expected in cases:
                with self.subTest(source_collation=collation, expected=expected):
                    self.assertEqual(expected.encode("utf-8").hex().upper(),
                                     fixture.mysql(select_detail("baseEdhr", fields, collation)))

    def test_actual_count_projection_executes_for_both_source_collations(self):
        cases = (
            ("baseWorkOrder", {"code": "工单", "name": "中文产品", "batch_code": "批次", "quantity": 2}),
            ("baseEdhr", {"id": 37, "task_code": "任务", "work_order_code": "工单", "batch_code": "批次",
                          "process_name": "工序", "task_type": "FILL"}),
        )
        for base, fields in cases:
            for collation in fixture.COLLATIONS:
                with self.subTest(base=base, source_collation=collation):
                    projection = "SELECT (" + detail(base) + ") AS detail FROM " + source(base, fields, collation)
                    # Referencing detail prevents the optimizer from pruning the
                    # REGEXP evaluation out of this SELECT COUNT(*) projection.
                    query = "SELECT COUNT(*) FROM (" + projection + ") r WHERE OCTET_LENGTH(r.detail) > 0"
                    self.assertEqual("1", fixture.mysql(query))

    def test_unfixed_work_order_pattern_reproduces_mysql_1267(self):
        fields = {"code": "工单", "name": "中文产品", "batch_code": "批次", "quantity": 2}
        expression = detail("baseWorkOrder").replace(" COLLATE utf8mb4_unicode_ci", "")
        sql = "SELECT HEX(" + expression + ") FROM " + source("baseWorkOrder", fields, "utf8mb4_unicode_ci")
        with self.assertRaisesRegex(AssertionError, r"ERROR 1267 .*Illegal mix of collations"):
            fixture.mysql(sql)

    def test_count_and_page_share_the_fixed_base_projections(self):
        mapper = ET.parse(MAPPER).getroot()
        for suffix, base in (("WorkOrder", "baseWorkOrder"), ("Edhr", "baseEdhr")):
            for prefix in ("count", "chunk"):
                with self.subTest(statement=prefix + suffix):
                    statement = mapper.find("select[@id='" + prefix + suffix + "']")
                    refs = [node.get("value") for node in statement.iter("property")
                            if node.get("name") == "baseRef"]
                    self.assertEqual([mapper.get("namespace") + "." + base], refs)

    def test_all_actual_patterns_are_explicit_and_source_columns_implicit(self):
        text = MAPPER.read_text(encoding="utf-8-sig")
        self.assertEqual(18, text.count("COLLATE utf8mb4_unicode_ci"))
        self.assertEqual(18, len(re.findall(
            r"CONVERT\(UNHEX\('[0-9A-F]+'\) USING utf8mb4\) COLLATE utf8mb4_unicode_ci", text)))
        pattern = re.search(r"CONVERT\(UNHEX\('[0-9A-F]+'\) USING utf8mb4\) COLLATE utf8mb4_unicode_ci", text)
        self.assertIsNotNone(pattern)
        for collation in fixture.COLLATIONS:
            result = fixture.mysql("SELECT COLLATION(w.code), COERCIBILITY(w.code), COLLATION(" + pattern[0] +
                                   "), COERCIBILITY(" + pattern[0] + ") FROM " + row("w", {"code": "工单"}, collation))
            self.assertEqual(collation + "\t2\tutf8mb4_unicode_ci\t0", result)


if __name__ == "__main__":
    unittest.main(verbosity=2)
