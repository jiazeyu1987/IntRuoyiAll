"""Actual Showroom workbench detail/count/page, with SELECT-only MySQL fixtures."""
import json
import re
import unittest
import xml.etree.ElementTree as ET

import test_dcc_workbench_regexp_collation as fixture
import test_mes_workbench_regexp_collation as mes

MAPPER = fixture.BACKEND / "yudao-module-showroom/src/main/resources/mapper/profileworkbench/ShowroomWorkbenchTodoMapper.xml"
SHARED = fixture.BACKEND / "yudao-module-system/src/main/resources/mapper/profileworkbench/ProfileWorkbenchTodoSql.xml"


def detail():
    text = ET.parse(MAPPER).getroot().find("sql[@id='baseAssignment']").text
    match = re.search(r"(CONCAT_WS\(' · ',.*?\)) AS detail,", text, re.S)
    if match is None:
        raise AssertionError("Actual Showroom detail projection is missing")
    return match.group(1)


def source(fields, collation):
    return mes.row("a", {key: value for key, value in fields.items() if key != "template_content"}, collation) + \
        " CROSS JOIN " + mes.row("n", {"template_content": fields["template_content"]}, collation)


def normalized(fields, collation):
    base = "SELECT 37 AS numeric_id, (" + detail() + ") AS detail, NULL AS due_at, NULL AS created_at FROM " + source(fields, collation)
    node = ET.parse(SHARED).getroot().find("sql[@id='normalizedProjection']")
    return node.text + base + node.find("include").tail


class ShowroomWorkbenchRegexpCollationTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        fixture.DccWorkbenchRegexpCollationTest.setUpClass()

    def test_three_actual_detail_patterns_preserve_unicode_boundaries(self):
        boundary = fixture.BOUNDARY
        cases = (
            ({"target_type": "COMPANY", "target_id": "7", "field_code": "development_history",
              "template_content": boundary + "通知\u3000内部" + boundary}, "公司#7 · 发展历程 · 通知\u3000内部"),
            ({"target_type": "PRODUCT", "target_id": "8", "field_code": "core_selling_points",
              "template_content": None}, "产品#8 · 卖点文案"),
            ({"target_type": boundary + "自定义" + boundary, "target_id": "9",
              "field_code": boundary + "字段\u00a0内\u200b边界" + boundary, "template_content": boundary},
             "自定义" + boundary + "#9 · 字段\u00a0内\u200b边界"),
            ({"target_type": None, "target_id": None, "field_code": None, "template_content": None}, ""),
        )
        for collation in fixture.COLLATIONS:
            for fields, expected in cases:
                with self.subTest(collation=collation, expected=expected):
                    self.assertEqual(expected.encode("utf-8").hex().upper(), fixture.mysql(
                        "SELECT HEX(" + detail() + ") FROM " + source(fields, collation)))

    def test_actual_normalized_count_and_page_projections(self):
        fields = {"target_type": "COMPANY", "target_id": "7", "field_code": "development_history", "template_content": "通知"}
        for collation in fixture.COLLATIONS:
            projection = normalized(fields, collation)
            with self.subTest(collation=collation):
                self.assertEqual("1", fixture.mysql("SELECT COUNT(*) FROM (" + projection + ") r WHERE OCTET_LENGTH(r.detail) > 0"))
                self.assertEqual("公司#7 · 发展历程 · 通知".encode("utf-8").hex().upper() + "\t1\t1970-01-01 00:00:00\t1970-01-01 00:00:00",
                                 fixture.mysql("SELECT HEX(r.detail),r.due_group,r.due_sort,r.created_sort FROM (" + projection + ") r ORDER BY r.due_group ASC,r.due_sort ASC,r.created_sort DESC,r.numeric_id DESC LIMIT 20"))

    def test_each_unfixed_pattern_reproduces_mysql_1267(self):
        fields = {"target_type": "CUSTOM", "target_id": "7", "field_code": "自定义", "template_content": "通知"}
        expression = detail()
        self.assertEqual(6, expression.count(" COLLATE utf8mb4_unicode_ci"))
        for branch in range(3):
            parts = expression.split(" COLLATE utf8mb4_unicode_ci")
            altered = parts[0] + "".join(("" if index // 2 == branch else " COLLATE utf8mb4_unicode_ci") + part
                                        for index, part in enumerate(parts[1:]))
            with self.subTest(branch=branch):
                with self.assertRaisesRegex(AssertionError, r"ERROR 1267 .*Illegal mix of collations"):
                    fixture.mysql("SELECT HEX(" + altered + ") FROM " + source(fields, "utf8mb4_unicode_ci"))

    def test_count_and_page_share_base_and_only_patterns_are_explicit(self):
        mapper = ET.parse(MAPPER).getroot()
        for statement in ("countAssignment", "chunkAssignment"):
            refs = [node.get("value") for node in mapper.find("select[@id='" + statement + "']").iter("property")
                    if node.get("name") == "baseRef"]
            self.assertEqual([mapper.get("namespace") + ".baseAssignment"], refs)
        text = MAPPER.read_text(encoding="utf-8-sig")
        self.assertEqual(6, text.count("COLLATE utf8mb4_unicode_ci"))
        self.assertEqual(6, len(re.findall(r"CONVERT\(UNHEX\('[0-9A-F]+'\) USING utf8mb4\) COLLATE utf8mb4_unicode_ci", text)))
        for collation in fixture.COLLATIONS:
            pattern = re.search(r"CONVERT\(UNHEX\('[0-9A-F]+'\) USING utf8mb4\) COLLATE utf8mb4_unicode_ci", text)[0]
            fields = {"target_type": "COMPANY", "target_id": "7", "field_code": "development_history", "template_content": "通知"}
            self.assertEqual(collation + "\t2\tutf8mb4_unicode_ci\t0", fixture.mysql(
                "SELECT COLLATION(a.field_code),COERCIBILITY(a.field_code),COLLATION(" + pattern + "),COERCIBILITY(" + pattern + ") FROM " + source(fields, collation)))


if __name__ == "__main__":
    unittest.main(verbosity=2)
