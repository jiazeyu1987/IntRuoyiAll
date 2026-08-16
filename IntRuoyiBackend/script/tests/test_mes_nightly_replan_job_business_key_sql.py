from pathlib import Path
import re
import unittest


SQL_PATH = Path(__file__).resolve().parents[2] / "sql" / "mysql" / "20260613_mes_nightly_replan_job_2am.sql"


class MesNightlyReplanJobBusinessKeySqlTest(unittest.TestCase):
    def test_registers_by_handler_name_without_fixed_primary_key(self):
        sql = SQL_PATH.read_text(encoding="utf-8")
        self.assertNotRegex(sql, re.compile(r"\b5616\b"))
        self.assertNotRegex(sql, re.compile(r"INSERT\s+INTO\s+`infra_job`\s*\(\s*`id`", re.IGNORECASE))
        self.assertRegex(sql, re.compile(r"handler_name`\s*=\s*'mesProNightlyReplanJob'", re.IGNORECASE))
        self.assertRegex(sql, re.compile(r"WHERE\s+NOT\s+EXISTS", re.IGNORECASE))


if __name__ == "__main__":
    unittest.main()
