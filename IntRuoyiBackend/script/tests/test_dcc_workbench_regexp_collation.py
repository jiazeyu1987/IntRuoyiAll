"""Execute the actual DCC detail projections using SELECT-only MySQL fixtures.

Requires the explicitly authorized local int-ruoyi-mysql container. No application
tables, schemas, session settings, or persistent fixtures are read or changed.
"""
import json
import re
import subprocess
import unittest
from pathlib import Path
import xml.etree.ElementTree as ET


BACKEND = Path(__file__).resolve().parents[2]
MAPPER = BACKEND / "yudao-module-dcc/src/main/resources/mapper/profileworkbench/DccWorkbenchTodoMapper.xml"
CONTAINER = "int-ruoyi-mysql"
COLLATIONS = ("utf8mb4_unicode_ci", "utf8mb4_0900_ai_ci")
BOUNDARY = "\t\n\v\f\r \u00a0\u1680\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2007\u2008\u2009\u200a\u2028\u2029\u202f\u205f\u3000\ufeff"


def mysql(sql):
    # Only fixed shell text is passed. SQL uses stdin; credentials stay inside the
    # existing container environment and never appear in host arguments/output.
    if not sql.lstrip().upper().startswith("SELECT ") or ";" in sql:
        raise ValueError("Only one SELECT statement is permitted")
    result = subprocess.run(
        ["docker", "exec", "-i", CONTAINER, "sh", "-c",
         'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql --user=root --default-character-set=utf8mb4 --batch --raw --skip-column-names'],
        input=sql, encoding="utf-8", capture_output=True, timeout=30,
    )
    if result.returncode:
        raise AssertionError("MySQL SELECT failed: " + result.stderr.strip())
    return result.stdout.strip()


def literal(value, collation):
    if value is None:
        return "NULL"
    return "CONVERT(UNHEX('" + value.encode("utf-8").hex() + "') USING utf8mb4) COLLATE " + collation


def detail(base):
    text = ET.parse(MAPPER).getroot().find("sql[@id='" + base + "']").text
    match = re.search(r"(CONCAT_WS\(' · ',.*?\)) AS detail,", text, re.S)
    if match is None:
        raise AssertionError("Actual DCC detail projection is missing")
    return match.group(1)


def projection_sql(expression, collation, fields):
    # JSON_TABLE gives typed columns IMPLICIT coercibility, just like the DCC
    # fields. Literal-derived columns retain EXPLICIT coercibility and can hide
    # the production conflict by overriding the pattern's collation.
    payload = json.dumps(fields, ensure_ascii=False)
    columns = ", ".join(key + " VARCHAR(512) CHARACTER SET utf8mb4 COLLATE " + collation +
                        " PATH '$." + key + "' ERROR ON EMPTY ERROR ON ERROR" for key in fields)
    row = "JSON_TABLE(" + literal(payload, collation) + ", '$' COLUMNS(" + columns + ")) f"
    return "SELECT HEX(" + expression + ") FROM " + row


class DccWorkbenchRegexpCollationTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        result = subprocess.run(
            ["docker", "inspect", CONTAINER, "--format", "{{json .}}"],
            encoding="utf-8", capture_output=True, timeout=15, check=True,
        )
        metadata = json.loads(result.stdout)
        if metadata["Name"] != "/" + CONTAINER or not metadata["State"]["Running"] or metadata["Config"]["Image"] != "mysql:8.0.39":
            raise AssertionError("Exact authorized local MySQL 8.0.39 container must be running")
        version, connection_collation = mysql("SELECT VERSION(), @@collation_connection").split("\t")
        if not version.startswith("8.0.") or connection_collation != "utf8mb4_0900_ai_ci":
            raise AssertionError("Expected MySQL 8.0 utf8mb4_0900_ai_ci constant-query connection")

    def test_actual_distribution_and_training_preserve_unicode_boundaries(self):
        cases = (
            ({"file_number": BOUNDARY + "文件 编号（中文）" + BOUNDARY,
              "title": BOUNDARY + "中文 标题\u3000内部" + BOUNDARY,
              "file_name": "不使用的文件名", "version_no": "甲版"},
             "文件 编号（中文） · 中文 标题\u3000内部 · 版本 甲版"),
            ({"file_number": BOUNDARY, "title": None,
              "file_name": BOUNDARY + "备用中文 文件名" + BOUNDARY, "version_no": None}, "备用中文 文件名"),
            ({"file_number": None, "title": "", "file_name": BOUNDARY, "version_no": ""}, ""),
            ({"file_number": "编号", "title": "中文\u00a0内\u200b边界", "file_name": None, "version_no": None},
             "编号 · 中文\u00a0内\u200b边界"),
        )
        for base in ("baseDistribution", "baseTraining"):
            expression = detail(base)
            for collation in COLLATIONS:
                for fields, expected in cases:
                    with self.subTest(base=base, source_collation=collation, expected=expected):
                        value = mysql(projection_sql(expression, collation, fields))
                        self.assertEqual(expected.encode("utf-8").hex().upper(), value)

    def test_count_projection_executes_for_both_source_collations(self):
        fields = {"file_number": "编号", "title": "中文标题", "file_name": "文件名", "version_no": "甲"}
        for base in ("baseDistribution", "baseTraining"):
            for collation in COLLATIONS:
                with self.subTest(base=base, source_collation=collation):
                    projection = projection_sql(detail(base), collation, fields).replace("SELECT HEX(", "SELECT (")
                    self.assertEqual("1", mysql("SELECT COUNT(*) FROM (" + projection + ") d"))

    def test_fixture_columns_are_implicit_and_fixed_pattern_is_explicit(self):
        expression = detail("baseDistribution")
        pattern = re.search(r"CONVERT\(UNHEX\('[0-9A-F]+'\) USING utf8mb4\) COLLATE utf8mb4_unicode_ci", expression)
        self.assertIsNotNone(pattern, "The actual mapper must explicitly fix the regex pattern collation")
        for collation in COLLATIONS:
            sql = projection_sql(expression, collation, {"file_number": "中文"})
            source = sql[sql.index(" FROM "):]
            result = mysql("SELECT COLLATION(f.file_number), COERCIBILITY(f.file_number), " +
                           "COLLATION(" + pattern[0] + "), COERCIBILITY(" + pattern[0] + ")" + source)
            self.assertEqual(collation + "\t2\tutf8mb4_unicode_ci\t0", result)


if __name__ == "__main__":
    unittest.main(verbosity=2)
