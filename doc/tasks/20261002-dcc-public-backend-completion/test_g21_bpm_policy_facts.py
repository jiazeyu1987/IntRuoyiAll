import copy, hashlib, json, re, tempfile, unittest
from pathlib import Path
from g21_validate_bpm_policy_facts import ContractError, validate, parse_jsonl

TASK=Path(__file__).resolve().parent
class BpmPolicyFactsTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.contract=json.loads((TASK/"g21-bpm-policy-contract.json").read_text(encoding="utf-8"))
        cls.fixture=json.loads((TASK/"g21-bpm-policy-offline-fixture.json").read_text(encoding="utf-8"))
    def setUp(self):self.evidence=copy.deepcopy(self.fixture)
    def rejected(self,regex=".*"):
        with self.assertRaisesRegex(ContractError,regex):validate(self.contract,self.evidence)
    def reseal(self):self.evidence["counts"]={k:len(v) for k,v in self.evidence["sections"].items()}
    def test_protected_snapshot_satisfies_eight_current_fact_contracts_only_offline(self):
        result=validate(self.contract,self.evidence)
        self.assertEqual("offline_fixture_acceptance_not_fresh_target_proof",result["status"])
        self.assertEqual(8,len(result["prerequisites"]));self.assertFalse(result["migrationExecutionProven"])
        self.assertTrue(all(x["checks"] for x in result["prerequisites"]))
    def test_missing_native_v3_definition_rejects_after_count_reseal(self):
        rows=self.evidence["sections"]["procdefs"];rows[:]=[r for r in rows if not (r["key"]=="dcc-controlled-file-revision" and r["version"]==3 and r["tenant"]=="122")];self.reseal();self.rejected("identities")
    def test_duplicate_info_rejects_even_with_same_values(self):
        rows=self.evidence["sections"]["infos"];rows.append(copy.deepcopy(rows[0]));self.reseal();self.rejected("Duplicate")
    def test_wrong_native_tenant_does_not_borrow_another_tenant_model(self):
        r=next(r for r in self.evidence["sections"]["models"] if r["key"]=="dcc-controlled-file-upload" and r["version"]==3);r["tenant"]="999";self.rejected("Fact mismatch")
    def test_old_version_cannot_replace_current_v3(self):
        r=next(r for r in self.evidence["sections"]["procdefs"] if r["key"]=="dcc-controlled-file-upload" and r["version"]==3);r["version"]=2;r["suspension"]=2;self.rejected("Fact mismatch")
    def test_conflicting_xml_is_detected_after_attacker_changes_transport_hash(self):
        r=next(r for r in self.evidence["sections"]["bodies"] if "-v3-tenant-1" in r["id"]);raw=bytes.fromhex(r["xmlHex"]).replace(b">35<",b">34<",1);r["xmlHex"]=raw.hex().upper();r["sha256"]=hashlib.sha256(raw).hexdigest();self.rejected("sha256")
    def test_xml_hex_mismatch_is_detected_when_pinned_hash_left_unchanged(self):
        r=next(r for r in self.evidence["sections"]["bodies"] if "-v3-tenant-1" in r["id"]);r["xmlHex"]="00"+r["xmlHex"][2:];self.rejected("XML bytes/hash")
    def test_missing_or_conflicting_xml_cannot_be_blank_success(self):
        r=next(r for r in self.evidence["sections"]["bodies"] if "-v3-tenant-1" in r["id"]);r["xmlHex"]=None;self.rejected("XML bytes")
    def test_legacy_info_exact_historical_shared_model_is_preserved(self):
        r=next(r for r in self.evidence["sections"]["infos"] if r["procdefId"]=="dcc-controlled-file-approval:1:codex122")
        m=next(x for x in self.evidence["sections"]["models"] if x["id"]==r["modelId"]);self.assertEqual("1",m["tenant"])
        self.assertEqual("offline_fixture_acceptance_not_fresh_target_proof",validate(self.contract,self.evidence)["status"])
    def test_legacy_pointer_change_is_not_fallback_resolved(self):
        r=next(r for r in self.evidence["sections"]["infos"] if r["procdefId"]=="dcc-controlled-file-approval:1:codex122");r["modelId"]="missing-model";self.rejected("modelId")
    def test_wrong_deployment_resource_linkage_rejects(self):
        r=self.evidence["sections"]["procdefs"][0];r["deploymentId"]="foreign";self.rejected("deploymentId")
    def test_duplicate_published_policy_rejects_even_different_id(self):
        rows=self.evidence["sections"]["policies"];row=copy.deepcopy(rows[0]);row["id"]="9007199254740993";rows.append(row);self.reseal();self.rejected("identities")
    def test_direct_policy_cannot_be_replaced_with_old_bpm_required(self):
        r=next(r for r in self.evidence["sections"]["policies"] if r["mode"]=="DIRECT");r["mode"]="BPM_REQUIRED";self.rejected("mode")
    def test_old_disabled_publish_policy_must_remain_disabled(self):
        r=next(r for r in self.evidence["sections"]["policies"] if r["status"]=="DISABLED");r["status"]="PUBLISHED";self.rejected("status")
    def test_policy_executor_or_form_slots_change_rejects(self):
        for field,value in [("executor","OTHER"),("formSlots",'["guessed"]'),("formPolicyType",None)]:
            self.evidence=copy.deepcopy(self.fixture);self.evidence["sections"]["policies"][0][field]=value;self.rejected("Fact mismatch")
    def test_template_content_or_params_conflict_rejects_without_overwrite(self):
        for field,value in [("contentHash","0"*64),("params",'[]'),("status",1),("nickname","fallback")]:
            self.evidence=copy.deepcopy(self.fixture);self.evidence["sections"]["templates"][0][field]=value;self.rejected("Fact mismatch")
    def test_missing_unique_constraint_rejects(self):
        rows=self.evidence["sections"]["indexes"];rows[:]=[r for r in rows if r["index"]!="uk_bpm_business_approval_policy_published"];self.reseal();self.rejected("index identity")
    def test_generated_match_key_omitting_state_rejects(self):
        r=next(r for r in self.evidence["sections"]["columns"] if r["column"]=="published_match_key");r["generationExpression"]=r["generationExpression"].replace(",`object_state`","");self.rejected("Generated identity")
    def test_mysql_escaped_literal_rendering_is_exactly_equivalent(self):
        for row in self.evidence["sections"]["columns"]:
            if row.get("generationExpression"):
                row["generationExpression"]=row["generationExpression"].replace("_utf8mb4'PUBLISHED'", "_utf8mb4\\'PUBLISHED\\'").replace("_utf8mb4'PENDING_BPM'", "_utf8mb4\\'PENDING_BPM\\'").replace("_utf8mb4'|'", "_utf8mb4\\'|\\'")
        self.assertEqual("offline_fixture_acceptance_not_fresh_target_proof",validate(self.contract,self.evidence)["status"])

    def test_escaped_rendering_does_not_normalize_changed_status_or_separator(self):
        row=next(r for r in self.evidence["sections"]["columns"] if r["column"]=="published_match_key")
        original=row["generationExpression"]
        for old,new in [("PUBLISHED","DISABLED"),("'|'","':'"),(" and "," or ")]:
            row["generationExpression"]=original.replace(old,new);self.rejected("Generated identity")

    def test_wrong_collation_rejects(self):
        r=next(r for r in self.evidence["sections"]["columns"] if r["column"]=="process_definition_key");r["collation"]="utf8mb4_general_ci";self.rejected("collation")
    def test_wrong_database_or_server_and_truncation_reject(self):
        for field,value in [("database","wrong-db"),("mysqlVersion","8.0.39"),("endServerUuid","other-server")]:
            self.evidence=copy.deepcopy(self.fixture);self.evidence["capture"][field]=value;self.rejected()
        self.evidence=copy.deepcopy(self.fixture);self.evidence["counts"]["procdefs"]+=1;self.rejected("count")
    def test_offline_marker_cannot_be_claimed_as_fresh_target(self):
        self.evidence["offlineOnly"]=False;self.rejected("frozen server identity")
    def test_query_is_select_only_and_raw_checksum_matches_contract(self):
        raw=(TASK/"g21-bpm-policy-facts.sql").read_bytes();self.assertEqual(self.contract["querySha256"],hashlib.sha256(raw).hexdigest())
        text=re.sub(r"(?m)^--.*$","",raw.decode())
        statements=[x.strip() for x in text.split(";") if x.strip()]
        self.assertEqual(22,len(statements));self.assertTrue(all(x.startswith("SELECT ") for x in statements))
        self.assertNotRegex(text,r"(?im)^\s*(UPDATE|INSERT|DELETE|SET|USE|ALTER|CREATE|DROP|CALL)\b")
    def test_cli_failure_saves_rejected_output_and_keeps_exit_one(self):
        import sys
        from unittest.mock import patch
        from g21_validate_bpm_policy_facts import main
        with tempfile.TemporaryDirectory() as tmp:
            output=Path(tmp)/"rejected.json"
            with patch.object(sys,"argv",["validator","--evidence",str(Path(tmp)/"missing.jsonl"),"--output",str(output)]):
                with self.assertRaises(SystemExit) as failed:main()
            self.assertEqual(1,failed.exception.code)
            result=json.loads(output.read_text(encoding="utf-8"));self.assertEqual("rejected",result["status"])
            self.assertFalse(result["ledgerWrites"])

    def test_jsonl_transport_rejects_missing_end_and_duplicate_seal(self):
        begin={"section":"capture_begin","contractVersion":self.contract["contractVersion"],"database":"ruoyi-vue-pro"}
        with tempfile.TemporaryDirectory() as tmp:
            p=Path(tmp)/"capture.jsonl";p.write_text(json.dumps(begin)+"\n",encoding="utf-8")
            with self.assertRaisesRegex(ContractError,"begin/end"):parse_jsonl(p)
            seal={"section":"count","name":"procdefs","count":0};p.write_text("\n".join(json.dumps(x) for x in [begin,seal,seal]),encoding="utf-8")
            with self.assertRaisesRegex(ContractError,"Duplicate count"):parse_jsonl(p)

if __name__=="__main__":unittest.main(verbosity=2)
