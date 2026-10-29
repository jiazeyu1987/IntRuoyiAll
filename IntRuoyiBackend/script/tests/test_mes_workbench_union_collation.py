"""Exercise the actual eDHR UNION text projections with SELECT-only MySQL fixtures."""
import itertools
import json
import re
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

import test_dcc_workbench_regexp_collation as fixture


MAPPER = Path(__file__).resolve().parents[2] / (
    "yudao-module-mes/src/main/resources/mapper/profileworkbench/MesWorkbenchTodoMapper.xml"
)


def branch(fragment, collation, task_type, action_url, empty=False, original=False):
    alias = "mes_pro_edhr_work_task" if fragment == "ordinaryEdhr" else "h"
    text = ET.parse(MAPPER).getroot().find("sql[@id='" + fragment + "']").text
    expressions = []
    for column, output in (("task_type", "domain_task_type"), ("action_url", "action_url")):
        pattern = (r"CONVERT\(" + re.escape(alias + "." + column)
                   + r" USING utf8mb4\) COLLATE utf8mb4_unicode_ci AS " + output)
        match = re.search(pattern, text)
        if match is None:
            raise AssertionError("Missing explicit UNION collation for " + alias + "." + column)
        expressions.append(alias + "." + column + " AS " + output if original else match[0])
    payload = fixture.literal(json.dumps({"task_type": task_type, "action_url": action_url},
                                        ensure_ascii=False), collation)
    columns = ",".join(column + " VARCHAR(512) CHARACTER SET utf8mb4 COLLATE " + collation
                       + " PATH '$." + column + "' ERROR ON ERROR"
                       for column in ("task_type", "action_url"))
    return ("SELECT " + ",".join(expressions) + " FROM JSON_TABLE(" + payload
            + ",'$' COLUMNS(" + columns + ")) " + alias + (" WHERE 1=0" if empty else ""))


class MesWorkbenchUnionCollationTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        fixture.DccWorkbenchRegexpCollationTest.setUpClass()

    def union(self, left, right):
        return fixture.mysql("SELECT HEX(domain_task_type),HEX(action_url) FROM ("
                             + left + " UNION ALL " + right + ") r")

    def test_different_table_collations_preserve_both_navigation_payloads(self):
        values = (("FILL", "/mes/填写?批次=甲"),
                  ("QA_DECISION_HANDOFF", "/mes/交接?订单=乙"))
        expected = "\n".join("\t".join(value.encode().hex().upper() for value in row)
                             for row in values)
        for collations in itertools.product(fixture.COLLATIONS, repeat=2):
            with self.subTest(collations=collations):
                self.assertEqual(expected, self.union(
                    branch("ordinaryEdhr", collations[0], *values[0]),
                    branch("handoffEdhr", collations[1], *values[1])))

    def test_empty_handoff_and_null_navigation_still_form_valid_union(self):
        for empty_left, empty_right in ((False, True), (True, False), (True, True)):
            with self.subTest(empty_left=empty_left, empty_right=empty_right):
                result = self.union(
                    branch("ordinaryEdhr", fixture.COLLATIONS[0], None, None, empty_left),
                    branch("handoffEdhr", fixture.COLLATIONS[1], None, None, empty_right))
                self.assertEqual("" if empty_left and empty_right else "NULL\tNULL", result)

    def test_original_implicit_columns_fail_even_with_empty_handoff(self):
        with self.assertRaisesRegex(AssertionError, r"ERROR 1271 .*Illegal mix of collations"):
            self.union(branch("ordinaryEdhr", fixture.COLLATIONS[0], "FILL", "/fill", original=True),
                       branch("handoffEdhr", fixture.COLLATIONS[1], "QA_REVIEW", "/review",
                              empty=True, original=True))


if __name__ == "__main__":
    unittest.main(verbosity=2)
