import copy
import importlib.util
import json
import tempfile
from pathlib import Path
import unittest

HERE = Path(__file__).resolve().parent


class PostflightTests(unittest.TestCase):
    def setUp(self):
        path = HERE / "g21-postflight-schema.py"
        self.assertTrue(path.exists(), "final schema postflight validator is missing")
        spec = importlib.util.spec_from_file_location("postflight", path)
        self.module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(self.module)
        self.contract = json.loads((HERE / "g21-postflight-schema-contract.json").read_text(encoding="utf-8"))
        self.environment = {"database": "dcc_intqms_g18_rehearsal", "serverUuid": "offline-unit-test-server", "databaseCharset": "utf8mb4", "databaseCollation": "utf8mb4_0900_ai_ci", "utf8mb4DefaultCollation": "utf8mb4_0900_ai_ci", "defaultStorageEngine": "InnoDB"}
        self.facts = self.module.positive_fixture(self.contract, self.environment)

    def reject(self, mutate, fragment):
        facts = copy.deepcopy(self.facts)
        mutate(facts)
        result = self.module.validate(self.contract, facts, self.environment)
        self.assertEqual("FAIL", result["status"])
        self.assertTrue(any(fragment in e for e in result["errors"]), result)

    def test_complete_final_scope_and_positive_schema(self):
        self.assertEqual(19, len(self.contract["candidates"]))
        self.assertEqual(17, len(self.contract["newTables"]))
        self.assertEqual(51, len(self.contract["newColumns"]))
        for key in self.contract["newColumns"]:
            table, column = key.split(".")
            self.assertEqual("YES", self.contract["columns"][table][column]["nullable"], key)
        self.assertIn("uk_dcc_controlled_file_master_chain", self.contract["absentIndexes"]["dcc_controlled_file_master"])
        self.assertNotIn("uk_dcc_controlled_file_master_chain", self.contract["indexes"]["dcc_controlled_file_master"])
        self.assertEqual("POSTFLIGHT_SCHEMA_PASS_NOT_EXECUTION_EVIDENCE", self.module.validate(self.contract, self.facts, self.environment)["status"])

    def test_same_name_wrong_types_nullability_charset_and_collation(self):
        self.reject(lambda f: f["columns"]["dcc_controlled_file"]["c_version_key"].update(type="varbinary(288)"), "c_version_key.type")
        self.reject(lambda f: f["columns"]["dcc_controlled_file"]["file_owner_user_id"].update(nullable="NO"), "file_owner_user_id.nullable")
        self.reject(lambda f: f["columns"]["dcc_controlled_file"]["source_original_file_name"].update(charset="latin1"), "source_original_file_name.charset")
        self.reject(lambda f: f["columns"]["dcc_application_round_link"]["bpm_round"].update(collation="utf8mb4_unicode_ci"), "bpm_round.collation")

    def test_default_generated_and_required_nullable_operators(self):
        self.reject(lambda f: f["columns"]["dcc_project_product_create_request"]["configured_reviewer_user_id"].update(default="1"), "configured_reviewer_user_id.default")
        self.reject(lambda f: f["columns"]["dcc_controlled_file_obsolete_audit"]["operator_id"].update(nullable="NO"), "operator_id.nullable")
        self.reject(lambda f: f["columns"]["dcc_project_folder"]["source_template_id"].update(nullable="NO"), "source_template_id.nullable")
        self.reject(lambda f: f["columns"]["dcc_controlled_file"]["c_version_key"].update(extra={"autoIncrement": False, "generated": "virtual", "onUpdate": None}), "c_version_key.extra")

    def test_required_table_shape_and_old_constraints_absence(self):
        self.reject(lambda f: f["tables"].pop("dcc_project_reviewer_config"), "dcc_project_reviewer_config")
        self.reject(lambda f: f["tables"]["dcc_project_file_placement"].update(collation="utf8mb4_general_ci"), "dcc_project_file_placement.collation")
        self.reject(lambda f: f["indexes"]["dcc_controlled_file_master"].update(uk_dcc_controlled_file_master_chain={}), "uk_dcc_controlled_file_master_chain")
        self.reject(lambda f: f["indexes"]["dcc_controlled_file_name_claim"].update(uk_dcc_file_name_claim_active={}), "uk_dcc_file_name_claim_active")

    def test_index_order_unique_prefix_and_round_placement_keys(self):
        self.reject(lambda f: f["indexes"]["dcc_controlled_file_task_assignee_snapshot"]["uk_dcc_task_assignee_obligation"]["columns"].reverse(), "uk_dcc_task_assignee_obligation")
        self.reject(lambda f: f["indexes"]["dcc_controlled_file_name_claim"]["uk_dcc_c_source_name"].update(unique=False), "uk_dcc_c_source_name")
        self.reject(lambda f: f["indexes"]["dcc_controlled_file_name_claim"]["uk_dcc_c_number"]["columns"][3].update(prefix=32), "uk_dcc_c_number")
        self.reject(lambda f: f["indexes"]["dcc_project_file_placement"].pop("uk_dcc_b_placement_version"), "uk_dcc_b_placement_version")
        self.reject(lambda f: f["indexes"]["dcc_application_round_link"].pop("uk_dcc_round_number"), "uk_dcc_round_number")

    def test_binary_generated_final_expression_semantics_reject(self):
        self.reject(lambda f: f["columns"]["dcc_controlled_file"]["c_version_key"].update(expression="CASE WHEN source_original_file_name IS NOT NULL THEN CONVERT(version_no USING binary) ELSE NULL END"), "c_version_key.expression")
        self.reject(lambda f: f["columns"]["dcc_controlled_file"]["c_version_key"].update(expression=f["columns"]["dcc_controlled_file"]["c_version_key"]["expression"].replace("revision_attempt_no>1", "revision_attempt_no>2")), "c_version_key.expression")
        self.reject(lambda f: f["columns"]["dcc_controlled_file_name_claim"]["source_name_key"].update(expression="CAST(file_name AS BINARY)"), "source_name_key.expression")

    def test_mysql_parentheses_and_binary_cast_normalize_without_losing_function_structure(self):
        self.assertEqual(self.module.expression_ast("CONVERT(source_original_file_name USING binary)"), self.module.expression_ast("(cast(`source_original_file_name` as binary))"))
        self.assertNotEqual(self.module.expression_ast("CONVERT(CONCAT(version_no,'#INITIAL') USING binary)"), self.module.expression_ast("CONCAT(CONVERT(version_no USING binary),'#INITIAL')"))

    def test_environment_is_required_not_a_guessed_mysql_default(self):
        self.assertEqual("FAIL", self.module.validate(self.contract, self.facts, {})["status"])
        self.reject(lambda f: f.update(database="other-database"), "runtime.database")
        self.reject(lambda f: f.update(serverUuid="another-server"), "runtime.serverUuid")

    def test_first_and_repeat_must_have_identical_full_schema_fingerprint(self):
        first = self.module.validate(self.contract, self.facts, self.environment)
        repeat = self.module.validate(self.contract, copy.deepcopy(self.facts), self.environment)
        self.assertEqual(first["schemaFingerprint"], repeat["schemaFingerprint"])
        changed = copy.deepcopy(self.facts)
        changed["indexes"]["dcc_controlled_file"]["extra_replay_index"] = {"unique": False, "type": "BTREE", "columns": [{"column": "id", "prefix": None, "direction": "A", "expression": None}]}
        result = self.module.validate(self.contract, changed, self.environment, previous=first)
        self.assertEqual("FAIL", result["status"])
        self.assertTrue(any("replay" in e for e in result["errors"]))

    def test_reason_capacity_exact_source_binary_keys_and_all_final_nullable_columns(self):
        self.assertEqual("varchar(2000)", self.contract["columns"]["dcc_controlled_file_access_log"]["reason"]["type"])
        self.assertEqual("YES", self.contract["columns"]["dcc_application_round_link"]["bpm_round"]["nullable"])
        self.assertEqual("YES", self.contract["columns"]["dcc_project_folder"]["source_node_key"]["nullable"])
        self.assertEqual("varbinary(1024)", self.contract["columns"]["dcc_controlled_file_name_claim"]["source_name_key"]["type"])
        self.assertEqual("varbinary(512)", self.contract["columns"]["dcc_controlled_file_name_claim"]["number_key"]["type"])
        self.reject(lambda f: f["columns"]["dcc_controlled_file_access_log"]["reason"].update(type="varchar(255)"), "reason.type")

    def test_collector_is_only_select_and_jsonl_rejects_missing_charset_or_nonjson_errors(self):
        sql = (HERE / "g21-postflight-schema-queries.sql").read_text(encoding="utf-8")
        statements = self.module.base.split_select_statements(sql)
        self.assertEqual(5, len(statements))
        self.assertTrue(all(s.upper().startswith("SELECT ") for s in statements))
        self.assertIn("CHARACTER_SET_NAME", sql)
        self.assertIn("EXPRESSION", sql)
        rows = [{"kind": "runtime", "database": self.facts["database"], "serverUuid": self.facts["serverUuid"], "version": "8.0.40"}]
        for table, spec in self.facts["tables"].items(): rows.append({"kind": "table", "table": table, **spec})
        for table, columns in self.facts["columns"].items():
            for column, spec in columns.items():
                extra = " ".join(filter(None, ["auto_increment" if spec["extra"]["autoIncrement"] else "", "STORED GENERATED" if spec["extra"]["generated"] == "stored" else "", "on update CURRENT_TIMESTAMP" if spec["extra"]["onUpdate"] else ""]))
                rows.append({"kind": "column", "table": table, "column": column, **spec, "extra": extra})
        for table, indexes in self.facts["indexes"].items():
            for name, spec in indexes.items():
                for sequence, col in enumerate(spec["columns"], 1): rows.append({"kind": "index", "table": table, "name": name, "type": spec["type"], "unique": int(spec["unique"]), "sequence": sequence, **col})
        for migration_id, spec in self.facts["ledger"].items(): rows.append({"kind": "ledger", "migrationId": migration_id, **spec})
        with tempfile.TemporaryDirectory(prefix="dcc-g21-post-offline-") as directory:
            path = Path(directory) / "offline-facts.jsonl"
            path.write_text("\n".join(json.dumps(row) for row in rows), encoding="utf-8")
            parsed = self.module.read_facts(path)
            self.assertEqual("POSTFLIGHT_SCHEMA_PASS_NOT_EXECUTION_EVIDENCE", self.module.validate(self.contract, parsed, self.environment)["status"])
            path.write_text("ERROR 1054 unknown column", encoding="utf-8")
            with self.assertRaises(ValueError): self.module.read_facts(path)

    def test_prior_original_ledger_and_extra_columns_remain_exact(self):
        self.reject(lambda f: f["ledger"]["20260513_dcc_base_schema"].update(sha256="0" * 64), "ledger")
        self.reject(lambda f: f["columns"]["dcc_project_reviewer_config"].update(unplanned_column={}), "unplanned_column")


if __name__ == "__main__":
    unittest.main()
