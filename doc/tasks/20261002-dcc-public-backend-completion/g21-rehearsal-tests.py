import importlib.util, sys, unittest
from pathlib import Path
from unittest.mock import Mock

HERE=Path(__file__).resolve().parent

def driver():
    path=HERE/"g21-rehearsal-driver.py"
    if not path.exists(): raise AssertionError("The isolated rehearsal driver is missing")
    spec=importlib.util.spec_from_file_location("g21_rehearsal_driver",path)
    module=importlib.util.module_from_spec(spec);sys.modules[spec.name]=module;spec.loader.exec_module(module);return module

def fixture_copy_baseline(d,prepared):
    import hashlib
    fields=d.copied_business_columns(prepared)
    rows=d.read_json(HERE/"g21-bpm-policy-contract.json")["sections"]["procdefs"]
    ids=[x["id"] for x in rows if x["key"] in {"dcc-controlled-file-upload","dcc-controlled-file-revision","dcc-controlled-file-obsolete"} and x["version"]==3]
    return {table:{"columns":columns,"sources":{identity:hashlib.sha256((table+identity+"isolated full source business fields").encode()).hexdigest() for identity in ids}} for table,columns in fields.items()}

class RehearsalGateTest(unittest.TestCase):
    def test_no_authorization_rejects_before_any_client(self):
        d=driver();client=Mock()
        with self.assertRaisesRegex(ValueError,"explicit.*rehearsal"):
            d.rehearse({},authorize_rehearsal_writes=False,transport_factory=client)
        client.assert_not_called()
    def test_source_and_foreign_database_modes_reject_before_client(self):
        d=driver()
        for name in ["ruoyi-vue-pro","foreign","dcc_intqms_g18_rehearsal_2"]:
            client=Mock()
            with self.assertRaisesRegex(ValueError,"fixed.*rehearsal"):
                d.rehearse({"rehearsalDatabase":name},authorize_rehearsal_writes=True,transport_factory=client)
            client.assert_not_called()


class RehearsalOfflineMaterialTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.d=driver()
        from unittest.mock import patch
        with patch("subprocess.run") as transport, patch("subprocess.Popen") as streaming:
            cls.prepared=cls.d.prepare(postflight_path=HERE/"g21-rehearsal-postflight-package.json")
            cls.d.save_json(HERE/"g21-rehearsal-prepared-inputs.json",cls.prepared)
            transport.assert_not_called();streaming.assert_not_called()
    def test_actual_scope_and_materials_prepare_without_transport(self):
        self.assertEqual("prepared_verified_rehearsal_not_database_execution",self.prepared["status"])
        self.assertEqual(19,len(self.prepared["entries"]));self.assertEqual(17,len(self.prepared["newTables"]))
        self.assertEqual(922,len(self.prepared["schemaTableNames"]));self.assertFalse(self.prepared["databaseWritesExecuted"])
    def test_manifest_hash_drift_rejected_before_network(self):
        from unittest.mock import patch
        d=self.d;real=d.sha
        with patch.object(d,"sha",side_effect=lambda p:"wrong" if str(p).endswith("manifest.json") else real(p)),patch("subprocess.run") as transport:
            with self.assertRaisesRegex(ValueError,"manifest drift"):d.prepare(postflight_path=HERE/"g21-rehearsal-postflight-package.json")
            transport.assert_not_called()
    def test_support_source_drift_rejects(self):
        from unittest.mock import patch
        d=self.d;real=d.sha
        with patch.object(d,"sha",side_effect=lambda p:"wrong" if str(p).endswith("g21_mysql_support.py") else real(p)):
            with self.assertRaisesRegex(ValueError,"support implementation changed"):d.prepare(postflight_path=HERE/"g21-rehearsal-postflight-package.json")
    def test_frozen_script_and_prerequisite_receipt_drift_rejects(self):
        from unittest.mock import patch
        d=self.d;real=d.sha
        for suffix,reason in [("first.sql","execution SQL drift"),("g21-prerequisite-runtime-receipt.json","proof/backup receipt drift")]:
            with patch.object(d,"sha",side_effect=lambda p,suffix=suffix:"wrong" if str(p).endswith(suffix) else real(p)),patch("subprocess.run") as transport:
                with self.assertRaisesRegex(ValueError,reason):d.prepare(postflight_path=HERE/"g21-rehearsal-postflight-package.json")
                transport.assert_not_called()
    def test_backup_checksum_failure_propagates_before_transport(self):
        from unittest.mock import patch
        d=self.d;modules=d.load_support(d.MAIN_TASK,d.read_json(HERE/"g21-rehearsal-contract.json"))
        with patch.object(modules["g21_migration_scope"],"verify_backups",side_effect=ValueError("Backup payload size/checksum changed")),patch.object(d,"load_support",return_value=modules),patch("subprocess.run") as transport:
            with self.assertRaisesRegex(ValueError,"Backup payload"):d.prepare(postflight_path=HERE/"g21-rehearsal-postflight-package.json")
            transport.assert_not_called()

    def test_postflight_unfinished_or_dependency_drift_rejects(self):
        import copy
        package=self.d.read_json(HERE/"g21-rehearsal-postflight-package.json")
        for field,value in [("status","pending"),("readyForDriver",False),("executionAuthorized",True)]:
            wrong=copy.deepcopy(package);wrong[field]=value
            with self.assertRaisesRegex(ValueError,"Unfinished"):self.d.validate_postflight(wrong,self.d.INTEGRATION)
        wrong=copy.deepcopy(package);wrong["validator"]["sha256"]="0"*64
        with self.assertRaisesRegex(ValueError,"fingerprint drift"):self.d.validate_postflight(wrong,self.d.INTEGRATION)
    def test_foreign_postflight_path_rejects(self):
        import copy
        wrong=copy.deepcopy(self.d.read_json(HERE/"g21-rehearsal-postflight-package.json"));wrong["query"]["path"]=str(Path(__file__).resolve().anchor)+"Windows/System32/cmd.exe"
        with self.assertRaises((ValueError,OSError)):self.d.validate_postflight(wrong,self.d.INTEGRATION)
    def test_prepared_receipt_change_rejects(self):
        import copy
        wrong=copy.deepcopy(self.prepared);wrong["serverUuid"]="foreign"
        with self.assertRaisesRegex(ValueError,"differs"):self.d.validate_prepared(wrong)
    def test_sql_first_and_repeat_have_exact_nineteen_markers(self):
        import tempfile,json
        with tempfile.TemporaryDirectory() as tmp:
            p=Path(tmp)/"markers.txt";text="1\nalready exists\n"
            for row in self.prepared["entries"]:
                for phase in ["sql_begin","sql_complete"]:text+=json.dumps({"taskPhase":phase,"migrationId":row["migrationId"]})+"\n"
            p.write_text(text,encoding="utf-8");self.d.check_markers(p,self.prepared["entries"])
            p.write_text(text[:text.rfind('{')],encoding="utf-8")
            with self.assertRaisesRegex(ValueError,"marker"):self.d.check_markers(p,self.prepared["entries"])
    def test_same_connection_environment_must_match_frozen_and_not_be_missing_or_duplicate(self):
        import tempfile,json
        env={"kind":"environment","database":"dcc_intqms_g18_rehearsal","serverUuid":"frozen","databaseCharset":"utf8mb4","databaseCollation":"utf8mb4_general_ci","utf8mb4DefaultCollation":"utf8mb4_0900_ai_ci","defaultStorageEngine":"InnoDB"}
        with tempfile.TemporaryDirectory() as tmp:
            path=Path(tmp)/"stdout.txt";path.write_text(json.dumps(env)+"\n1\n",encoding="utf-8")
            self.d.verify_session_environment(path,env)
            for rows in [[],[env,env],[dict(env,database="ruoyi-vue-pro")]]:
                path.write_text("\n".join(json.dumps(x) for x in rows),encoding="utf-8")
                with self.assertRaisesRegex(ValueError,"session environment"):self.d.verify_session_environment(path,env)

    def test_exact_seed_thirty_three_rows_and_tampered_same_counts(self):
        import copy
        d=self.d;expected=d.expected_seed_facts(self.prepared,fixture_copy_baseline(d,self.prepared));actual={};before={};current={}
        for table,rows in expected.items():
            actual[table]=[];before[table]={"rows":{"old-key":"old-hash"}};current[table]={"rows":{"old-key":"old-hash"}}
            for n,row in enumerate(rows,1):
                value=copy.deepcopy(row);value["id"]=table+str(n);actual[table].append(value);current[table]["rows"][value["id"].encode().hex().upper()]="new-hash"
        result=d.validate_seed_rows(expected,actual,before,current)
        self.assertEqual(33,sum(x["count"] for x in result.values()))
        actual["act_ge_bytearray"][0]["canonicalXmlSha256"]="0"*64
        with self.assertRaisesRegex(ValueError,"payload/linkage/XML"):d.validate_seed_rows(expected,actual,before,current)
    def test_seed_duplicate_missing_wrong_tenant_or_policy_executor_rejected(self):
        import copy
        d=self.d;expected=d.expected_seed_facts(self.prepared,fixture_copy_baseline(d,self.prepared));actual={};before={};current={}
        for table,rows in expected.items():
            actual[table]=[dict(x,id=table+str(n)) for n,x in enumerate(rows)];before[table]={"rows":{}};current[table]={"rows":{r["id"].encode().hex().upper():"hash" for r in actual[table]}}
        for table,field,value in [("act_re_procdef","tenant","foreign"),("act_re_model","editorSourceId","foreign-body"),("bpm_business_approval_policy","executor","OTHER"),("system_notify_template","params","[]")]:
            changed=copy.deepcopy(actual);changed[table][0][field]=value
            with self.assertRaisesRegex(ValueError,"payload"):d.validate_seed_rows(expected,changed,before,current)
        changed=copy.deepcopy(actual);changed["act_re_model"][0]["identity"]=changed["act_re_model"][1]["identity"]
        with self.assertRaisesRegex(ValueError,"identity"):d.validate_seed_rows(expected,changed,before,current)
    def test_postflight_zero_exit_is_not_enough_without_exact_json_database_status(self):
        import tempfile,json,types
        from unittest.mock import patch
        d=self.d;mysql=Mock();mysql.database="dcc_intqms_g18_rehearsal";mysql.read.return_value="fixture facts only"
        for payload in ["not-json",json.dumps({"status":"FAIL"}),json.dumps({"status":"POSTFLIGHT_SCHEMA_PASS_NOT_EXECUTION_EVIDENCE","database":"ruoyi-vue-pro","serverUuid":self.prepared["serverUuid"]})]:
            with tempfile.TemporaryDirectory() as tmp:
                private=Path(tmp);environment=private/"environment.jsonl";environment.write_text('{}',encoding="utf-8")
                def cli(command,**kwargs):Path(command[command.index("--result")+1]).write_text(payload,encoding="utf-8");return types.SimpleNamespace(returncode=0)
                with patch("subprocess.run",side_effect=cli):
                    with self.assertRaises((ValueError,KeyError)):d.run_postflight(mysql,self.prepared,private,"first",environment)
    def test_new_structural_tables_must_be_seventeen_and_empty(self):
        from unittest.mock import patch
        d=self.d;mysql=Mock()
        with patch.object(d,"protected_read",return_value="0"):self.assertEqual(17,len(d.verify_new_tables(mysql,self.prepared,HERE,"unit-new-tables")))
        with patch.object(d,"protected_read",return_value="1"):
            with self.assertRaisesRegex(ValueError,"stay empty"):d.verify_new_tables(mysql,self.prepared,HERE,"unit-new-tables")

    def test_private_raw_read_captures_are_phase_specific_and_never_overwritten(self):
        import tempfile,json
        d=self.d;mysql=Mock();mysql.database="dcc_intqms_g18_rehearsal"
        with tempfile.TemporaryDirectory() as tmp:
            private=Path(tmp);mysql.read.return_value="first exact raw facts"
            d.protected_read(mysql,"SELECT 1;",private,"first-seed33-act_re_model")
            first=(private/"first-seed33-act_re_model.facts.txt").read_bytes()
            mysql.read.return_value="repeat exact raw facts"
            d.protected_read(mysql,"SELECT 1;",private,"repeat-seed33-act_re_model")
            self.assertEqual(first,(private/"first-seed33-act_re_model.facts.txt").read_bytes())
            self.assertTrue((private/"repeat-seed33-act_re_model.read-receipt.json").is_file())
            with self.assertRaisesRegex(ValueError,"refusing to overwrite"):d.protected_read(mysql,"SELECT 1;",private,"first-seed33-act_re_model")
            self.assertEqual(first,(private/"first-seed33-act_re_model.facts.txt").read_bytes())

    def test_config_queries_are_select_only_and_state_specific(self):
        d=self.d;queries=d.seed_queries(d.expected_seed_facts(self.prepared,fixture_copy_baseline(d,self.prepared)),d.copied_business_columns(self.prepared));self.assertEqual(7,len(queries))
        for query in queries.values():self.assertTrue(query.startswith("SELECT "))
        self.assertIn("CONTROLLED_PENDING_EFFECTIVE",queries["bpm_business_approval_policy"])
        self.assertIn("dcc_relation_remediation",queries["system_notify_template"])
    def test_ledger_only_appended_task_nineteen_matches_exact_primary_additions(self):
        import json
        from unittest.mock import patch
        d=self.d;rows=[]
        for n,entry in enumerate(self.prepared["entries"]):rows.append({"id":str(n+100),"migrationId":entry["migrationId"],"sha256":entry["sha256"],"fileName":entry["file"],"environment":"test","status":"APPLIED","operationId":self.prepared["operationId"],"releaseTag":self.prepared["operationId"],"deleted":0,"tenant":"0"})
        mysql=Mock();mysql.read.return_value="\n".join(json.dumps(x) for x in rows)
        before={"infra_release_migration":{"rows":{"old-key":"old"}}};current={"infra_release_migration":{"rows":{"old-key":"old",**{x["id"].encode().hex().upper():"hash" for x in rows}}}}
        with patch.object(d,"protected_read",side_effect=lambda mysql,*args:mysql.read("fixture")):
            self.assertEqual(19,d.verify_ledger_additions(mysql,self.prepared,before,current,HERE,"unit-ledger")["newCount"])
        rows[0]["operationId"]="foreign";mysql.read.return_value="\n".join(json.dumps(x) for x in rows)
        with patch.object(d,"protected_read",side_effect=lambda mysql,*args:mysql.read("fixture")):
            with self.assertRaisesRegex(ValueError,"ledger facts mismatch"):d.verify_ledger_additions(mysql,self.prepared,before,current,HERE,"unit-ledger")


class RehearsalPipelineTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.d=driver()
        cls.prepared=cls.d.prepare(postflight_path=HERE/"g21-rehearsal-postflight-package.json")
    def fixture(self,d,prepared,private,*,exists=False,fail_at=None,mutate_repeat=False,postflight_fail=False,wrong_session=False):
        import copy,json,types
        root_support=d.load_support(Path(prepared["mainTask"]),d.read_json(prepared["contractPath"]))["g21_mysql_support"]
        tables=prepared["originalTables"]+["infra_release_migration"]
        before={table:{"columns":["ID_" if table.startswith("act_") else "id","payload"],"rows":{"old-key":"a"*64},"count":1,"aggregateSha256":"a"*64} for table in tables}
        current=copy.deepcopy(before);expected=d.expected_seed_facts(prepared,fixture_copy_baseline(d,prepared));actual={}
        for table,rows in expected.items():
            actual[table]=[dict(x,id=table+str(n)) for n,x in enumerate(rows)]
            for row in actual[table]:current[table]["rows"][row["id"].encode().hex().upper()]="b"*64
            current[table]["count"]=len(current[table]["rows"])
        ledger=[]
        for n,entry in enumerate(prepared["entries"]):
            row={"id":str(n+100),"migrationId":entry["migrationId"],"sha256":entry["sha256"],"fileName":entry["file"],"environment":"test","status":"APPLIED","operationId":prepared["operationId"],"releaseTag":prepared["operationId"],"deleted":0,"tenant":"0"};ledger.append(row);current["infra_release_migration"]["rows"][row["id"].encode().hex().upper()]="b"*64
        current["infra_release_migration"]["count"]=20
        state={"phase":None,"calls":[],"sourceWrites":[],"created":False}
        support=types.SimpleNamespace(compare_original_rows=root_support.compare_original_rows)
        def snapshot(mysql,names,columns=None):
            state["calls"].append("snapshot:"+str(state["phase"]))
            result=copy.deepcopy(before if state["phase"] is None else current)
            if state["phase"]=="repeat" and mutate_repeat:result["dcc_controlled_file"]["rows"]["old-key"]="changed"
            return result
        support.snapshot_original_rows=snapshot
        class Mysql:
            def __init__(self,database):self.database=database
            def read(self,sql):
                state["calls"].append("read:"+self.database)
                if "JSON_OBJECT('database'" in sql:return json.dumps({"database":self.database,"serverUuid":prepared["serverUuid"],"mysqlVersion":prepared["mysqlVersion"]})
                if "SCHEMA_NAME='dcc_intqms_g18_rehearsal'" in sql:return "1" if exists else "0"
                if "JSON_OBJECT('charset'" in sql:return json.dumps({"charset":"utf8mb4","collation":"utf8mb4_general_ci"})
                if sql.startswith("SELECT TABLE_NAME"):return "\n".join(prepared["schemaTableNames"]+(prepared["newTables"] if state["phase"] else []))
                if "'kind','environment'" in sql:return json.dumps({"kind":"environment","database":self.database,"serverUuid":prepared["serverUuid"],"databaseCharset":"utf8mb4","databaseCollation":"utf8mb4_general_ci","utf8mb4DefaultCollation":"utf8mb4_0900_ai_ci","defaultStorageEngine":"InnoDB"})
                if "FROM infra_release_migration" in sql:return "" if state["phase"] is None else "\n".join(json.dumps(x) for x in ledger)
                if sql.startswith("SELECT COUNT(*) FROM `"):return "0"
                for table in actual:
                    if "FROM `"+table+"`" in sql:return "\n".join(json.dumps(x) for x in actual[table])
                raise AssertionError("Unexpected fake readonly query")
            def run_authorized_sql(self,sql,*,private_directory,stem):
                state["calls"].append("write:"+stem)
                if self.database==d.SOURCE:
                    state["sourceWrites"].append(sql.decode());self_outer.assertEqual("CREATE DATABASE `dcc_intqms_g18_rehearsal` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;",sql.decode());state["created"]=True
                else:state["phase"]="first" if stem.startswith("first") else "repeat"
                out=private_directory/(stem+".stdout.txt");err=private_directory/(stem+".stderr.txt")
                out.write_text("",encoding="utf-8");err.write_text("",encoding="utf-8")
                if stem==fail_at:
                    err.write_text("isolated fake first SQL failure",encoding="utf-8");raise RuntimeError("protected failure")
                if self.database==d.CLONE:
                    self_outer.assertTrue(sql.startswith(Path(prepared["postflight"]["environmentQuery"]).read_bytes()))
                    env={"kind":"environment","database":self.database,"serverUuid":prepared["serverUuid"],"databaseCharset":"utf8mb4","databaseCollation":"utf8mb4_general_ci","utf8mb4DefaultCollation":"utf8mb4_0900_ai_ci","defaultStorageEngine":"InnoDB"}
                    if wrong_session:env["serverUuid"]="foreign-session"
                    out.write_text(json.dumps(env)+"\n"+"\n".join(json.dumps({"taskPhase":phase,"migrationId":e["migrationId"]}) for e in prepared["entries"] for phase in ["sql_begin","sql_complete"]),encoding="utf-8")
                import hashlib
                return {"database":self.database,"sqlSha256":hashlib.sha256(sql).hexdigest(),"exitCode":0,"stdout":str(out),"stderr":str(err)}
            def restore_gzip(self,path,*,private_directory,stem):
                state["calls"].append(stem)
                if stem==fail_at:raise RuntimeError("isolated fake restore failure")
                return {"database":self.database,"exitCode":0,"dumpSha256":"fixture-only"}
        self_outer=self
        def postflight(mysql,prepared,private,phase,environment,previous=None):
            state["calls"].append("postflight:"+phase)
            if postflight_fail:raise ValueError("isolated fake schema contract failure")
            path=private/(phase+"-schema-result.json");path.write_text('{}',encoding="utf-8")
            return path,{"schemaFingerprint":"same-final-schema"}
        return Mysql,support,postflight,state
    def run_fixture(self,*,exists=False,fail_at=None,mutate_repeat=False,postflight_fail=False,wrong_session=False):
        import tempfile,copy,json
        from unittest.mock import patch
        d=self.d;prepared=copy.deepcopy(self.prepared)
        with tempfile.TemporaryDirectory() as tmp:
            backup=Path(tmp);private=backup/"private-run"
            factory,support,postflight,state=self.fixture(d,prepared,private,exists=exists,fail_at=fail_at,mutate_repeat=mutate_repeat,postflight_fail=postflight_fail,wrong_session=wrong_session)
            modules={"g21_mysql_support":support}
            with patch.object(d,"BACKUPS",backup),patch.object(d,"validate_prepared",side_effect=lambda x:x),patch.object(d,"load_support",return_value=modules),patch.object(d,"run_postflight",side_effect=postflight),patch.object(d,"capture_copied_baseline",return_value=fixture_copy_baseline(d,prepared)):
                try:result=d.rehearse(prepared,authorize_rehearsal_writes=True,transport_factory=factory,private_directory=private)
                except ValueError:result=json.loads((private/"driver-receipt.json").read_text(encoding="utf-8"))
            return result,state
    def test_fake_full_first_repeat_checks_all_and_flag_is_not_approval(self):
        result,state=self.run_fixture();self.assertEqual("ISOLATED_REHEARSAL_PASS",result["status"])
        self.assertFalse(result["actualUserApprovalProvenByFlag"]);self.assertEqual(1,len(state["sourceWrites"]))
        self.assertEqual(["write:create-fixed-clone","write:first-nineteen","write:repeat-nineteen"],[x for x in state["calls"] if x.startswith("write:")])
        checks=[x for x in result["steps"] if "phase" in x];self.assertEqual(2,len(checks));self.assertTrue(all(x["exactConfigAdditionCount"]==33 and x["exactLedgerAdditionCount"]==19 for x in checks))
    def test_existing_clone_stops_before_create_or_restore(self):
        result,state=self.run_fixture(exists=True);self.assertEqual("FAILED_STOPPED_NO_AUTOMATIC_RECOVERY",result["status"])
        self.assertFalse(state["sourceWrites"]);self.assertFalse(any(x.startswith("restore") for x in state["calls"]))
    def test_different_session_environment_cannot_reach_postflight_or_repeat(self):
        result,state=self.run_fixture(wrong_session=True);self.assertEqual("FAILED_STOPPED_NO_AUTOMATIC_RECOVERY",result["status"])
        self.assertNotIn("postflight:first",state["calls"]);self.assertNotIn("write:repeat-nineteen",state["calls"])

    def test_first_sql_error_stops_repeat_and_retains_clone(self):
        result,state=self.run_fixture(fail_at="first-nineteen");self.assertEqual("FAILED_STOPPED_NO_AUTOMATIC_RECOVERY",result["status"])
        self.assertTrue(state["created"]);self.assertNotIn("write:repeat-nineteen",state["calls"])
    def test_restore_error_stops_migration_and_does_not_delete_clone(self):
        result,state=self.run_fixture(fail_at="restore-data");self.assertTrue(state["created"]);self.assertNotIn("write:first-nineteen",state["calls"])
        self.assertEqual("FAILED_STOPPED_NO_AUTOMATIC_RECOVERY",result["status"])
    def test_repeat_old_row_mutation_rejects_pass(self):
        result,state=self.run_fixture(mutate_repeat=True);self.assertEqual("FAILED_STOPPED_NO_AUTOMATIC_RECOVERY",result["status"])
    def test_failed_first_postflight_cannot_continue_to_repeat(self):
        result,state=self.run_fixture(postflight_fail=True);self.assertEqual("FAILED_STOPPED_NO_AUTOMATIC_RECOVERY",result["status"])
        self.assertNotIn("write:repeat-nineteen",state["calls"])


class RehearsalReviewRegressionTest(unittest.TestCase):
    def test_form_id_and_print_setting_are_in_exact_copy_business_projection(self):
        d=driver();prepared=d.read_json(HERE/"g21-rehearsal-prepared-inputs.json")
        fields=d.copied_business_columns(prepared)
        self.assertIn("form_id",fields["bpm_process_definition_info"])
        self.assertIn("print_template_setting",fields["bpm_process_definition_info"])
        self.assertIn("HAS_START_FORM_KEY_",fields["act_re_procdef"])
        self.assertIn("DGRM_RESOURCE_NAME_",fields["act_re_procdef"])
    def test_snapshot_read_failure_retains_exact_private_error_and_hash(self):
        import tempfile,types,json,hashlib
        d=driver()
        class PrivateFailure(RuntimeError):
            def __init__(self):self.private_error=b"ERROR exact isolated snapshot SQL failure";super().__init__("generic protected error")
        mysql=Mock();mysql.database="dcc_intqms_g18_rehearsal";mysql.read.side_effect=PrivateFailure()
        with tempfile.TemporaryDirectory() as tmp:
            private=Path(tmp);adapter=d.RecordingMysql(mysql,private,"baseline")
            support=d.load_support(d.MAIN_TASK,d.read_json(HERE/"g21-rehearsal-contract.json"))["g21_mysql_support"]
            with self.assertRaises(PrivateFailure):support.snapshot_original_rows(adapter,["dcc_controlled_file"])
            errors=list(private.glob("*.stderr.txt"));self.assertEqual(1,len(errors));self.assertEqual(b"ERROR exact isolated snapshot SQL failure",errors[0].read_bytes())
            receipt=json.loads(next(private.glob("*.failure-receipt.json")).read_text(encoding="utf-8"));self.assertEqual(hashlib.sha256(errors[0].read_bytes()).hexdigest(),receipt["stderrSha256"])

    def test_full_copied_digest_rejects_wrong_form_or_print_even_same33_and_repeat(self):
        import copy
        d=driver();prepared=d.read_json(HERE/"g21-rehearsal-prepared-inputs.json");baseline=fixture_copy_baseline(d,prepared)
        expected=d.expected_seed_facts(prepared,baseline);actual={};before={};current={}
        for table,rows in expected.items():
            actual[table]=[dict(x,id=table+str(n)) for n,x in enumerate(rows)];before[table]={"rows":{"old":"unchanged"}};current[table]={"rows":{"old":"unchanged",**{x["id"].encode().hex().upper():"new-row-same-on-repeat" for x in actual[table]}}}
        for field in ["form_id","print_template_setting","HAS_START_FORM_KEY_"]:
            changed=copy.deepcopy(actual);table="act_re_procdef" if field.isupper() else "bpm_process_definition_info"
            changed[table][0]["copiedBusinessHash"]="0"*64
            with self.assertRaisesRegex(ValueError,"payload/linkage"):d.validate_seed_rows(expected,changed,before,current)
        queries=d.seed_queries(expected,d.copied_business_columns(prepared))
        self.assertIn("`form_id`",queries["bpm_process_definition_info"]);self.assertIn("`print_template_setting`",queries["bpm_process_definition_info"])
        self.assertIn("`DGRM_RESOURCE_NAME_`",queries["act_re_procdef"])
    def test_complete_copy_baseline_rejects_missing_wrong_tenant_duplicate(self):
        import tempfile,json,copy
        d=driver();prepared=d.read_json(HERE/"g21-rehearsal-prepared-inputs.json");baseline=fixture_copy_baseline(d,prepared)
        tenants={x["id"]:x["tenant"] for x in d.read_json(HERE/"g21-bpm-policy-contract.json")["sections"]["procdefs"]}
        good={table:[{"sourceDefinitionId":key,"tenant":tenants[key],"copiedBusinessHash":value} for key,value in row["sources"].items()] for table,row in baseline.items()}
        class Mysql:
            database="dcc_intqms_g18_rehearsal"
            def __init__(self,rows):self.rows=rows
            def read(self,sql):
                table="act_re_procdef" if 'FROM `act_re_procdef`' in sql else "bpm_process_definition_info"
                return "\n".join(json.dumps(x) for x in self.rows[table])
        with tempfile.TemporaryDirectory() as tmp:self.assertEqual(baseline,d.capture_copied_baseline(Mysql(good),prepared,Path(tmp)))
        for invalid in ["missing","tenant","duplicate"]:
            rows=copy.deepcopy(good)
            if invalid=="missing":rows["bpm_process_definition_info"].pop()
            elif invalid=="tenant":rows["act_re_procdef"][0]["tenant"]="foreign"
            else:rows["bpm_process_definition_info"][0]=copy.deepcopy(rows["bpm_process_definition_info"][1])
            with tempfile.TemporaryDirectory() as tmp:
                with self.assertRaisesRegex(ValueError,"V3.*source"):d.capture_copied_baseline(Mysql(rows),prepared,Path(tmp))
    def test_recording_snapshot_success_has_each_phase_table_columns_and_rows(self):
        import tempfile,json
        d=driver();support=d.load_support(d.MAIN_TASK,d.read_json(HERE/"g21-rehearsal-contract.json"))["g21_mysql_support"]
        mysql=Mock();mysql.database="dcc_intqms_g18_rehearsal"
        mysql.read.side_effect=lambda sql:"id\npayload" if "COLUMN_NAME" in sql else "31\t"+"a"*64
        with tempfile.TemporaryDirectory() as tmp:
            private=Path(tmp);first=support.snapshot_original_rows(d.RecordingMysql(mysql,private,"baseline"),["dcc_controlled_file"])
            support.snapshot_original_rows(d.RecordingMysql(mysql,private,"first"),["dcc_controlled_file"],{"dcc_controlled_file":first["dcc_controlled_file"]["columns"]})
            names={x.name for x in private.glob("*.facts.txt")}
            self.assertIn("baseline-snapshot-dcc_controlled_file-columns-001.facts.txt",names)
            self.assertIn("baseline-snapshot-dcc_controlled_file-rows-002.facts.txt",names)
            self.assertIn("first-snapshot-dcc_controlled_file-rows-001.facts.txt",names)

if __name__=="__main__":unittest.main(verbosity=2)
