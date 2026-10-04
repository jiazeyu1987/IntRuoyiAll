"""Offline assertions; never connects to a database or executes migration SQL."""
import copy
import importlib.util
import json
import tempfile
from pathlib import Path
import unittest

HERE = Path(__file__).resolve().parent


def validator():
    path = HERE / "g21_structure_prerequisites.py"
    if not path.exists():
        raise AssertionError("17 prerequisite fact validator is missing")
    spec = importlib.util.spec_from_file_location("g21_validator", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class StructurePrerequisiteTests(unittest.TestCase):
    def setUp(self):
        self.module = validator()
        self.contract = json.loads((HERE / "g21-structure-prerequisites.json").read_text(encoding="utf-8"))
        self.facts = self.module.positive_fixture(self.contract)

    def reject(self, mutate, fragment):
        facts = copy.deepcopy(self.facts)
        mutate(facts)
        result = self.module.validate_facts(self.contract, facts)
        self.assertEqual("FAIL", result["status"])
        self.assertTrue(any(fragment in item for item in result["errors"]), result)

    def test_17_exact_scope_and_positive_facts_never_claim_applied(self):
        self.assertEqual(17, len(self.contract["prerequisites"]))
        self.assertEqual("SATISFIED_READ_ONLY_FACTS_NOT_APPLIED", self.module.validate_facts(self.contract, self.facts)["status"])
        self.assertFalse(self.contract["executionAuthorized"])
        self.assertTrue(all(p["migrationMustNotExecute"] for p in self.contract["prerequisites"]))

    def test_same_name_wrong_table_engine_or_collation_rejects(self):
        self.reject(lambda f: f["tables"]["dcc_product_catalog"].update(engine="MyISAM"), "dcc_product_catalog.engine")
        self.reject(lambda f: f["tables"]["bpm_form_action_instance"].update(collation="utf8mb4_general_ci"), "bpm_form_action_instance.collation")

    def test_missing_column_and_same_named_wrong_type_reject(self):
        self.reject(lambda f: f["columns"]["dcc_controlled_file"].pop("version_no"), "dcc_controlled_file.version_no")
        self.reject(lambda f: f["columns"]["dcc_product_catalog"]["product_code"].update(type="varchar(64)"), "dcc_product_catalog.product_code.type")

    def test_nullable_default_and_column_collation_reject(self):
        self.reject(lambda f: f["columns"]["system_notify_message"]["business_key"].update(nullable="NO"), "business_key.nullable")
        self.reject(lambda f: f["columns"]["system_notify_message"]["business_key"].update(default=""), "business_key.default")
        self.reject(lambda f: f["columns"]["dcc_controlled_file_name_claim"]["normalized_name"].update(collation="utf8mb4_unicode_ci"), "normalized_name.collation")

    def test_unique_order_prefix_and_uniqueness_reject(self):
        name = "uk_dcc_file_name_claim_active"
        self.reject(lambda f: f["indexes"]["dcc_controlled_file_name_claim"][name]["columns"].reverse(), name)
        self.reject(lambda f: f["indexes"]["dcc_controlled_file_name_claim"][name].update(unique=False), name)
        self.reject(lambda f: f["indexes"]["dcc_controlled_file_name_claim"][name]["columns"][1].update(prefix=32), name)

    def test_generated_expression_and_storage_kind_reject(self):
        self.reject(lambda f: f["columns"]["dcc_controlled_file_checkout"]["active_master_id"].update(expression="CASE WHEN status='CLOSED' THEN master_id ELSE NULL END"), "active_master_id.expression")
        self.reject(lambda f: f["columns"]["dcc_controlled_file_name_claim"]["active_unique_flag"].update(extra="VIRTUAL GENERATED"), "active_unique_flag.extra")

    def test_old_ledger_hash_must_not_be_replaced_with_current_semantics(self):
        self.reject(lambda f: f["ledger"]["20260513_dcc_base_schema"].update(sha256=self.contract["sources"]["currentSqlHashes"]["20260513_dcc_base_schema"]), "20260513_dcc_base_schema.ledger.sha256")
        self.reject(lambda f: f["ledger"]["20260710_dcc_product_catalog_database"].update(status="FAILED"), "20260710_dcc_product_catalog_database.ledger.status")

    def test_data_readiness_requires_no_empty_routes_or_duplicate_notify_keys(self):
        self.reject(lambda f: f["scalars"].update(route_empty_action_type=1), "route_empty_action_type")
        self.reject(lambda f: f["scalars"].update(notify_duplicate_business_keys=2), "notify_duplicate_business_keys")

    def test_pending_new_columns_are_not_false_prerequisites(self):
        for table, fields in {
            "dcc_controlled_file": ["controlled_time", "source_original_file_name", "c_version_key", "revision_attempt_no", "file_owner_user_id"],
            "dcc_project_code": ["project_leader_user_id", "default_attributes_json"],
            "dcc_project_product_create_request": ["configured_reviewer_user_id", "creation_reason"],
            "dcc_controlled_file_master": ["latest_controlled_file_id", "dcc_project_code_id", "file_type_taxonomy_leaf_id", "normalized_file_number"],
        }.items():
            self.assertTrue(all(field not in self.contract["columns"].get(table, {}) for field in fields))
        self.assertEqual("SATISFIED_READ_ONLY_FACTS_NOT_APPLIED", self.module.validate_facts(self.contract, self.facts)["status"])

    def test_query_is_only_select_and_scoped_to_exact_read_facts(self):
        source = (HERE / "g21-structure-prerequisite-queries.sql").read_text(encoding="utf-8")
        statements = self.module.split_select_statements(source)
        self.assertGreater(len(statements), 4)
        self.assertTrue(all(s.upper().startswith("SELECT ") for s in statements))
        self.assertNotIn("CALL ", source.upper())
        self.assertNotIn("UPDATE INFRA_RELEASE_MIGRATION", source.upper())

    def test_json_line_reader_retains_null_default_and_mysql_generated_spelling(self):
        rows = [{"kind": "runtime", "database": self.contract["database"]}]
        for table, spec in self.facts["tables"].items():
            rows.append({"kind": "table", "table": table, **spec})
        for table, columns in self.facts["columns"].items():
            for column, spec in columns.items():
                extra = " ".join(filter(None, ["auto_increment" if spec["extra"]["autoIncrement"] else "", "STORED GENERATED" if spec["extra"]["generated"] == "stored" else "", "DEFAULT_GENERATED on update CURRENT_TIMESTAMP" if spec["extra"]["onUpdate"] else ""]))
                rows.append({"kind": "column", "table": table, "column": column, **spec, "extra": extra})
        for table, indexes in self.facts["indexes"].items():
            for name, spec in indexes.items():
                for sequence, col in enumerate(spec["columns"], 1):
                    rows.append({"kind": "index", "table": table, "name": name, "unique": int(spec["unique"]), "type": spec["type"], "sequence": sequence, **col})
        for migration_id, spec in self.facts["ledger"].items():
            rows.append({"kind": "ledger", "migrationId": migration_id, **spec})
        for key, value in self.facts["scalars"].items():
            rows.append({"kind": "scalar", "name": key, "value": value})
        with tempfile.TemporaryDirectory(prefix="dcc-g21-offline-") as directory:
            path = Path(directory) / "fixture.jsonl"
            path.write_text("\n".join(json.dumps(row) for row in rows), encoding="utf-8")
            parsed = self.module.read_facts(path)
            self.assertEqual("SATISFIED_READ_ONLY_FACTS_NOT_APPLIED", self.module.validate_facts(self.contract, parsed)["status"])
            path.write_text("ERROR 1054 unknown column\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "not JSON"):
                self.module.read_facts(path)

    def test_database_identity_and_missing_ledger_are_fail_closed(self):
        self.reject(lambda f: f.update(database="other-db"), "runtime.database")
        self.reject(lambda f: f["ledger"].pop("20260513_dcc_base_schema"), "20260513_dcc_base_schema.ledger")

    def test_parenthesized_mysql_literal_charset_normalization_does_not_change_expression(self):
        normal = "CASE WHEN status='ACTIVE' THEN master_id ELSE NULL END"
        mysql = "((case when (`status` = _utf8mb4'ACTIVE') then `master_id` else NULL end))"
        self.assertEqual(self.module.expression_tokens(normal), self.module.expression_tokens(mysql))
        self.assertNotEqual(self.module.expression_tokens(normal), self.module.expression_tokens("CASE WHEN status='ACTIVE' THEN actor_id ELSE NULL END"))


if __name__ == "__main__":
    unittest.main()
