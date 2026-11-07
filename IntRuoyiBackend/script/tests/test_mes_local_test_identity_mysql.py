"""Read-only MySQL regression for the production local-test approved-product query.

Run: python -m unittest discover -s script/tests -p test_mes_local_test_identity_mysql.py -v
CTEs supply isolated fixtures; no tables, business records, or schema are written.
Requires the project's int-ruoyi-mysql container (MySQL 8).
"""
from pathlib import Path
import re
import subprocess
import unittest

MAPPER = Path(__file__).resolve().parents[2] / (
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/"
    "dal/mysql/pro/testdata/MesLocalTestRunDefinitionMapper.java"
)


def approved_sql():
    source = MAPPER.read_text(encoding="utf-8")
    return re.search(r'"""\s*(SELECT DISTINCT.*?)"""', source, re.S).group(1)


def mysql(sql):
    result = subprocess.run(
        ["docker", "exec", "-i", "int-ruoyi-mysql", "sh", "-c",
         'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql --user=root '
         '--database=ruoyi-vue-pro --default-character-set=utf8mb4 '
         '--batch --raw --skip-column-names'],
        input="START TRANSACTION READ ONLY;\n" + sql + ";\nROLLBACK;\n",
        text=True, encoding="utf-8", capture_output=True, timeout=30,
    )
    if result.returncode:
        raise AssertionError(result.stderr)
    return [line.split("\t") for line in result.stdout.splitlines() if line]


def text(value, collation):
    return "_utf8mb4'" + value.replace("'", "''") + "' COLLATE " + collation


def fixture_sql(
    *, catalog_code="PP-A", catalog_name="批准产品A",
    item_code="PP-A", item_name="批准产品A", item_tenant=1,
    binding_project=2, binding_tenant=1, relation_request=1,
    request_status="COMPLETED", relation_deleted=0, query_tenant=1,
):
    request_collation = "utf8mb4_0900_ai_ci"
    legacy_collation = "utf8mb4_unicode_ci"
    ctes = {
        "dcc_project_product_create_request":
            "SELECT 1 id,1 tenant_id,0 deleted,"
            + text(request_status, request_collation)
            + " status,4 relation_id,2 generated_project_code_id,3 generated_product_catalog_id,"
            + text("PP-A", request_collation) + " product_code,"
            + text("批准产品A", request_collation) + " product_name",
        "dcc_project_product_relation":
            f"SELECT 4 id,1 tenant_id,{relation_deleted} deleted,{relation_request} request_id,"
            "'ACTIVE' relation_status,2 project_code_id,3 product_catalog_id",
        "dcc_project_code":
            "SELECT 2 id,1 tenant_id,0 deleted,'ENABLE' status,'PROJECT-A' project_code",
        "dcc_product_catalog":
            "SELECT 3 id,1 tenant_id,0 deleted,"
            + text(catalog_code, legacy_collation) + " product_code,"
            + text(catalog_name, legacy_collation) + " product",
        "mes_md_item":
            f"SELECT 10 id,{item_tenant} tenant_id,0 deleted,0 status,"
            + text(item_code, legacy_collation) + " code,"
            + text(item_name, legacy_collation) + " name",
        "mes_pro_route_product": "SELECT 1 tenant_id,0 deleted,50 route_id,10 item_id",
        "mes_pro_route": "SELECT 50 id,1 tenant_id,0 deleted",
        "mes_pro_route_dcc_project_binding":
            f"SELECT 50 route_id,{binding_tenant} tenant_id,0 deleted,"
            f"{binding_project} dcc_project_code_id",
    }
    return "WITH " + ",\n".join(f"{name} AS ({sql})" for name, sql in ctes.items()) + (
        "\n" + approved_sql().replace("#{tenantId}", str(query_tenant))
    )


class ApprovedIdentityMySqlTest(unittest.TestCase):
    def test_mixed_collations_preserve_exact_formal_identity(self):
        rows = mysql(fixture_sql())
        self.assertEqual(1, len(rows))
        self.assertEqual(["1", "2", "3", "4", "10"], rows[0][:5])
        self.assertEqual(["PP-A", "批准产品A", "PROJECT-A"], rows[0][5:])

    def test_catalog_must_match_entire_code_and_name_exactly(self):
        for changes in (
            {"catalog_code": "PP-B"}, {"catalog_code": "pp-a"},
            {"catalog_code": "PP-A "}, {"catalog_name": "批准产品B"},
            {"catalog_name": "批准产品a"}, {"catalog_name": "批准产品A "},
        ):
            with self.subTest(changes=changes):
                self.assertEqual([], mysql(fixture_sql(**changes)))

    def test_mes_text_mismatch_keeps_application_but_blocks_product_mapping(self):
        for changes in (
            {"item_code": "PP-B"}, {"item_code": "pp-a"},
            {"item_code": "PP-A "}, {"item_name": "批准产品B"},
            {"item_name": "批准产品a"}, {"item_name": "批准产品A "},
        ):
            with self.subTest(changes=changes):
                rows = mysql(fixture_sql(**changes))
                self.assertEqual(1, len(rows))
                self.assertEqual("NULL", rows[0][4])

    def test_tenant_and_explicit_route_relationship_remain_required(self):
        self.assertEqual([], mysql(fixture_sql(query_tenant=2)))
        for changes in ({"item_tenant": 2}, {"binding_tenant": 2}, {"binding_project": 999}):
            with self.subTest(changes=changes):
                self.assertEqual("NULL", mysql(fixture_sql(**changes))[0][4])

    def test_only_completed_intact_formal_links_are_candidates(self):
        for changes in (
            {"request_status": "REJECTED"}, {"relation_request": 999},
            {"relation_deleted": 1},
        ):
            with self.subTest(changes=changes):
                self.assertEqual([], mysql(fixture_sql(**changes)))

    def test_real_schema_resolves_complete_production_join(self):
        rows = mysql("EXPLAIN " + approved_sql().replace("#{tenantId}", "1"))
        self.assertTrue(rows)


if __name__ == "__main__":
    unittest.main()
