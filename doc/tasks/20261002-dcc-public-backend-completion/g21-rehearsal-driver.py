"""Fixed clone-only rehearsal. Offline prepare/validate; writes require a technical explicit gate.
This flag is not user approval. Root must obtain actual authorization before invoking rehearse.
No source upgrade, delete, drop, automatic cleanup or recovery mode exists.
"""
from __future__ import annotations
import argparse, gzip, hashlib, importlib.util, json, re, subprocess, sys
from pathlib import Path

HERE=Path(__file__).resolve().parent
SOURCE="ruoyi-vue-pro"
CLONE="dcc_intqms_g18_rehearsal"
BACKUPS=Path("C:/IntRuoyiBackups/20261003-dcc-integration")
MAIN_TASK=Path("C:/IntRuoyiAll-int_main/doc/tasks/20261001-dcc-integration-unblock")
INTEGRATION=HERE.parents[2]

class RehearsalError(ValueError):pass

def require(condition,reason):
    if not condition:raise RehearsalError(reason)

def read_json(path):
    value=json.loads(Path(path).read_text(encoding="utf-8-sig"))
    require(isinstance(value,dict),"Expected exact JSON object")
    return value

def sha(path):
    with Path(path).open("rb") as stream:return hashlib.file_digest(stream,"sha256").hexdigest()

def load_support(main_task,contract):
    for name,digest in contract["supportHashes"].items():
        require(name in {"g21_migration_scope.py","g21_execution_materials.py","g21_mysql_support.py"},"Unexpected support module")
        require(sha(main_task/name)==digest,"Root support implementation changed: "+name)
    sys.path.insert(0,str(main_task))
    modules={}
    for name in ["g21_migration_scope","g21_execution_materials","g21_mysql_support"]:
        spec=importlib.util.spec_from_file_location(name,main_task/(name+".py"));module=importlib.util.module_from_spec(spec)
        sys.modules[name]=module;spec.loader.exec_module(module);modules[name]=module
    return modules

def ensure_select_only(path):
    text=Path(path).read_text(encoding="utf-8-sig")
    statements=[x.strip() for x in re.sub(r"(?m)^\s*--.*$","",text).split(";") if x.strip()]
    require(statements and all(re.match(r"^SELECT\b",x,re.I) for x in statements),"Postflight queries must be SELECT-only")
    return text

def validate_postflight(package,integration):
    require(package.get("status")=="prepared_exact_postflight_contract" and package.get("readyForDriver") is True and package.get("executionAuthorized") is False,"Unfinished postflight contract")
    owner=package.get("ownerReceipt",{});owner_path=(integration/owner.get("path","")).resolve(strict=True)
    require(owner_path.is_relative_to(integration.resolve()) and sha(owner_path)==owner.get("sha256"),"Postflight Owner receipt drift")
    owner_receipt=read_json(owner_path)
    require(owner_receipt.get("status")=="prepared_exact_postflight_contract" and owner_receipt.get("readyForDriver") is True,"Postflight Owner is not ready")
    require(package.get("database")==CLONE and package.get("serverUuid"),"Postflight must target fixed rehearsal clone")
    result={}
    for field in ["query","environmentQuery","validator","contract"]:
        row=package.get(field,{})
        require(isinstance(row,dict) and isinstance(row.get("path"),str),"Missing postflight field: "+field)
        relative=Path(row["path"]);path=(integration/relative).resolve(strict=True)
        require(not relative.is_absolute() and path.is_relative_to(integration.resolve()),"Postflight path escaped integration")
        require(sha(path)==row.get("sha256"),"Postflight fingerprint drift: "+field)
        result[field]=str(path)
    for row in package.get("dependencies",[]):
        path=(integration/row["path"]).resolve(strict=True)
        require(path.is_relative_to(integration.resolve()) and sha(path)==row["sha256"],"Postflight dependency drift")
    ensure_select_only(result["query"]);ensure_select_only(result["environmentQuery"])
    return result

def prepare(*,contract_path=HERE/"g21-rehearsal-contract.json",manifest_path=BACKUPS/"exec-g21-0610/manifest.json",postflight_path,main_task=MAIN_TASK,integration=INTEGRATION):
    # Disk-only verification: do not instantiate LocalMysql or subprocess a transport here.
    contract=read_json(contract_path);manifest=read_json(manifest_path)
    require(contract.get("rehearsalDatabase")==CLONE and contract.get("sourceDatabase")==SOURCE,"Only fixed rehearsal database may be prepared")
    require(sha(manifest_path)==contract["materialsManifestSha256"],"Execution materials manifest drift")
    require(manifest.get("status")=="prepared_verified_inputs_not_database_execution" and manifest.get("executionCount")==19
            and manifest.get("sourceDatabase")==SOURCE and manifest.get("rehearsalDatabase")==CLONE
            and manifest.get("serverUuid")==contract["serverUuid"],"Execution material scope differs")
    require(Path(manifest_path).resolve().parent.parent==BACKUPS.resolve(),"Materials must remain in protected backup task directory")
    modules=load_support(main_task,contract);scope_module=modules["g21_migration_scope"];materials=modules["g21_execution_materials"]
    for field,name in [("scopeSha256","g18-approved-scope-candidate.json"),("packageSha256","g18-migration-package.json"),("prerequisiteReceiptSha256","g21-prerequisite-runtime-receipt.json"),("backupReceiptSha256","g18-backup-receipt.json")]:
        require(sha(main_task/name)==manifest[field],"Frozen scope/proof/backup receipt drift: "+name)
    scope=read_json(main_task/"g18-approved-scope-candidate.json")
    bundle=scope_module.validate_bundle(scope,read_json(main_task/"g18-migration-package.json"),integration)
    proofs=materials.verify_prerequisite_receipt(main_task,integration)
    require(set(bundle["externalPrerequisiteIds"])==set(proofs["prerequisiteIds"]) and proofs["serverUuid"]==contract["serverUuid"],"External proof coverage/target mismatch")
    require(scope["existingTablesAffected"]==contract["originalTables"] and scope["newTables"]==contract["newTables"],"Frozen table scope differs")
    receipt=read_json(main_task/"g18-backup-receipt.json");verified=scope_module.verify_backups(receipt,BACKUPS)
    require(verified==manifest["verifiedBackups"],"Backup evidence differs from prepared materials")
    scripts={}
    require(len(manifest["scripts"])==2 and {x["phase"] for x in manifest["scripts"]}=={"first","repeat"},"First/repeat material scope differs")
    for row in manifest["scripts"]:
        path=Path(row["path"]).resolve(strict=True)
        require(path.parent==Path(manifest_path).resolve().parent and path.name==row["phase"]+".sql","Execution script escaped material directory")
        require(sha(path)==row["sha256"],"Frozen execution SQL drift")
        exact=materials.compose_script(scope["executionOrder"],integration/"IntRuoyiBackend",manifest["operationId"],repeat=row["phase"]=="repeat")
        require(path.read_bytes()==exact,"Script is not exact nineteen-item composer result")
        scripts[row["phase"]]=str(path)
    schema=next(x for x in receipt["artifacts"] if x["kind"]=="schema")
    data=next(x for x in receipt["artifacts"] if x["kind"]=="data")
    schema_names=[];data_names=[]
    for row,names,pattern in [(schema,schema_names,r"^CREATE TABLE `([^`]+)`"),(data,data_names,r"^-- Dumping data for table `([^`]+)`")]:
        with gzip.open(row["path"],"rt",encoding="utf-8") as stream:
            for line in stream:
                require(not re.match(r"\s*(USE\s|(?:CREATE|DROP)\s+(?:DATABASE|SCHEMA)\b|SOURCE\s|\\!)",line,re.I),"Backup contains forbidden restore routing")
                match=re.match(pattern,line)
                if match:names.append(match[1])
    require(len(schema_names)==len(set(schema_names))==contract["schemaTableCount"]==922,"Schema dump is not exact922-table scope")
    require(len(data_names)==len(set(data_names))==contract["dataTableCount"]==210,"Data dump is not exact210-table scope")
    require(set(data_names)<=set(schema_names) and not set(scope["newTables"])&set(schema_names),"New table already present or data schema mismatch")
    seed_contract=HERE/"g21-bpm-policy-contract.json"
    require(sha(seed_contract)==contract["seedFactContractSha256"],"Protected seed fact contract drift")
    require(sha(postflight_path)==contract["postflightPackageSha256"],"Frozen postflight package drift")
    postflight=read_json(postflight_path);paths=validate_postflight(postflight,integration)
    require(postflight["serverUuid"]==contract["serverUuid"],"Postflight server identity mismatch")
    return {"status":"prepared_verified_rehearsal_not_database_execution","databaseWritesExecuted":False,"writeAuthorizationGranted":False,
       "sourceDatabase":SOURCE,"rehearsalDatabase":CLONE,"serverUuid":contract["serverUuid"],"mysqlVersion":contract["mysqlVersion"],"operationId":manifest["operationId"],
       "contractPath":str(Path(contract_path).resolve()),"contractSha256":sha(contract_path),"manifestPath":str(Path(manifest_path).resolve()),"manifestSha256":sha(manifest_path),
       "postflightPath":str(Path(postflight_path).resolve()),"postflightSha256":sha(postflight_path),"postflight":paths,
       "mainTask":str(main_task.resolve()),"integrationRoot":str(integration.resolve()),"scripts":scripts,"schemaDump":schema["path"],"dataDump":data["path"],
       "schemaTableCount":922,"schemaTableNames":schema_names,"dataTableCount":210,"newTables":scope["newTables"],"originalTables":scope["existingTablesAffected"],"entries":scope["executionOrder"],"permittedAdditions":contract["permittedAdditions"]}

def validate_prepared(prepared):
    require(prepared.get("status")=="prepared_verified_rehearsal_not_database_execution" and prepared.get("rehearsalDatabase")==CLONE,"Only fixed prepared rehearsal may execute")
    for field,digest in [("contractPath","contractSha256"),("manifestPath","manifestSha256"),("postflightPath","postflightSha256")]:
        require(sha(prepared[field])==prepared[digest],"Prepared input drift: "+field)
    fresh=prepare(contract_path=Path(prepared["contractPath"]),manifest_path=Path(prepared["manifestPath"]),postflight_path=Path(prepared["postflightPath"]),main_task=Path(prepared["mainTask"]),integration=Path(prepared["integrationRoot"]))
    require(fresh==prepared,"Prepared receipt differs from current exact inputs")
    return fresh

def json_rows(text):
    return [json.loads(x) for x in text.splitlines() if x.strip()]

def save_json(path,value):
    Path(path).write_text(json.dumps(value,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")

class RecordingMysql:
    """Readonly adapter for frozen snapshot support; one private phase/table/read receipt per call."""
    def __init__(self,mysql,private,phase):
        self.mysql=mysql;self.database=mysql.database;self.private=private;self.phase=phase;self.sequence=0
    def read(self,sql):
        self.sequence+=1
        metadata=re.search(r"TABLE_NAME='([A-Za-z0-9_]+)'",sql)
        table=metadata[1] if metadata else re.search(r"FROM `([A-Za-z0-9_]+)`",sql)[1]
        kind="columns" if metadata else "rows"
        return protected_read(self.mysql,sql,self.private,f"{self.phase}-snapshot-{table}-{kind}-{self.sequence:03d}")

def read_identity(mysql,expected,private,stem):
    sql="SELECT JSON_OBJECT('database',DATABASE(),'serverUuid',@@server_uuid,'mysqlVersion',@@version);"
    rows=json_rows(protected_read(mysql,sql,private,stem))
    require(len(rows)==1 and rows[0]=={"database":expected["database"],"serverUuid":expected["serverUuid"],"mysqlVersion":expected["mysqlVersion"]},"Runtime database/server identity mismatch")

def protected_read(mysql,sql,private,stem):
    try:
        text=mysql.read(sql)
        facts=private/(stem+".facts.txt");receipt=private/(stem+".read-receipt.json")
        require(not facts.exists() and not receipt.exists(),"Readonly step evidence exists; refusing to overwrite it")
        with facts.open("xb") as stream:stream.write(text.encode("utf-8"))
        save_json(receipt,{"sqlSha256":hashlib.sha256(sql.encode("utf-8")).hexdigest(),"factsSha256":sha(facts),"facts":str(facts),"database":mysql.database})
        return text
    except Exception as exc:
        payload=getattr(exc,"private_error",None)
        if payload is not None:
            error=private/(stem+".stderr.txt");receipt=private/(stem+".failure-receipt.json")
            require(not error.exists() and not receipt.exists(),"Private read failure evidence exists; refusing overwrite")
            with error.open("xb") as stream:stream.write(payload)
            save_json(receipt,{"database":mysql.database,"sqlSha256":hashlib.sha256(sql.encode("utf-8")).hexdigest(),"stderrSha256":sha(error),"stderr":str(error),"status":"FAILED_EXACT_PRIVATE_ERROR_PRESERVED"})
        raise

def check_markers(path,entries):
    markers=[]
    for line in Path(path).read_text(encoding="utf-8-sig").splitlines():
        if not line.lstrip().startswith("{"):continue
        row=json.loads(line)
        if "taskPhase" in row:markers.append((row["taskPhase"],row.get("migrationId")))
    expected=[(phase,x["migrationId"]) for x in entries for phase in ("sql_begin","sql_complete")]
    require(markers==expected,"First/repeat SQL stopped or marker identities/order differ")

def verify_session_environment(stdout,frozen):
    environments=[]
    for line in Path(stdout).read_text(encoding="utf-8-sig").splitlines():
        if line.lstrip().startswith("{"):
            row=json.loads(line)
            if row.get("kind")=="environment":environments.append(row)
    require(len(environments)==1 and environments[0]==frozen,"Actual SQL session environment missing/duplicate/different from frozen pre-migration facts")
    return environments[0]

def verify_new_tables(mysql,prepared,private,phase):
    counts={table:int(protected_read(mysql,"SELECT COUNT(*) FROM `"+table+"`;",private,phase+"-new-table-count-"+table).strip()) for table in prepared["newTables"]}
    require(len(counts)==17 and not any(counts.values()),"New structural tables must stay empty")
    return counts

def run_postflight(mysql,prepared,private,phase,environment,previous=None):
    path=private/(phase+"-schema.jsonl");path.write_text(protected_read(mysql,Path(prepared["postflight"]["query"]).read_text(encoding="utf-8-sig"),private,phase+"-schema"),encoding="utf-8")
    result=private/(phase+"-schema-result.json")
    command=[sys.executable,"-B",prepared["postflight"]["validator"],"validate","--facts",str(path),"--environment",str(environment),"--result",str(result)]
    if previous:command.extend(["--previous",str(previous)])
    stdout=private/(phase+"-postflight.stdout.txt");stderr=private/(phase+"-postflight.stderr.txt")
    with stdout.open("xb") as out,stderr.open("xb") as err:done=subprocess.run(command,stdout=out,stderr=err)
    require(done.returncode==0 and result.is_file(),"Postflight validator failed; protected receipts retained")
    proof=read_json(result);require(proof.get("status")=="POSTFLIGHT_SCHEMA_PASS_NOT_EXECUTION_EVIDENCE" and proof.get("database")==CLONE and proof.get("serverUuid")==prepared["serverUuid"],"Postflight contract incomplete/failed")
    return result,proof

def rehearse(prepared,*,authorize_rehearsal_writes=False,transport_factory=None,private_directory=None):
    require(authorize_rehearsal_writes,"An explicit authorize-rehearsal-writes technical gate is required; Root still owns actual user approval")
    require(prepared.get("rehearsalDatabase")==CLONE,"Only the fixed isolated rehearsal database may execute")
    prepared=validate_prepared(prepared)
    private=Path(private_directory or "")
    require(private_directory and private.resolve().parent==BACKUPS.resolve() and not private.exists(),"Protected new rehearsal receipt directory required")
    modules=load_support(Path(prepared["mainTask"]),read_json(prepared["contractPath"]));support=modules["g21_mysql_support"]
    factory=transport_factory or support.LocalMysql
    private.mkdir();journal={"status":"RUNNING","technicalWriteGateProvided":True,"actualUserApprovalProvenByFlag":False,"sourceDatabase":SOURCE,"rehearsalDatabase":CLONE,"steps":[]}
    save_json(private/"driver-receipt.json",journal)
    try:
        admin=factory(SOURCE)
        read_identity(admin,{"database":SOURCE,"serverUuid":prepared["serverUuid"],"mysqlVersion":prepared["mysqlVersion"]},private,"source-identity")
        exists=protected_read(admin,"SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='"+CLONE+"';",private,"clone-existence")
        require(exists.strip()=="0","Fixed rehearsal clone already exists; never reuse or delete it")
        defaults=json_rows(protected_read(admin,"SELECT JSON_OBJECT('charset',DEFAULT_CHARACTER_SET_NAME,'collation',DEFAULT_COLLATION_NAME) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME=DATABASE();",private,"source-database-defaults"))
        require(len(defaults)==1 and defaults[0]["charset"]=="utf8mb4" and re.fullmatch(r"utf8mb4_[a-z0-9_]+",defaults[0]["collation"]),"Source database charset/collation fact is invalid")
        # Only fixed clone CREATE DATABASE is sent through this administrative connection; no source DML.
        create=admin.run_authorized_sql(("CREATE DATABASE `"+CLONE+"` CHARACTER SET utf8mb4 COLLATE "+defaults[0]["collation"]+";").encode(),private_directory=private,stem="create-fixed-clone")
        journal["steps"].append(create);save_json(private/"driver-receipt.json",journal)
        clone=factory(CLONE);read_identity(clone,{"database":CLONE,"serverUuid":prepared["serverUuid"],"mysqlVersion":prepared["mysqlVersion"]},private,"clone-identity")
        for field,stem in [("schemaDump","restore-schema"),("dataDump","restore-data")]:
            journal["steps"].append(clone.restore_gzip(Path(prepared[field]),private_directory=private,stem=stem));save_json(private/"driver-receipt.json",journal)
        names=protected_read(clone,"SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() ORDER BY TABLE_NAME;",private,"restored-tables").splitlines()
        require(set(names)==set(prepared["schemaTableNames"]) and len(names)==922,"Restored schema table identities/count mismatch")
        environment=private/"frozen-environment.jsonl";environment.write_text(protected_read(clone,Path(prepared["postflight"]["environmentQuery"]).read_text(encoding="utf-8-sig"),private,"frozen-environment"),encoding="utf-8")
        environment_rows=json_rows(environment.read_text(encoding="utf-8-sig"))
        require(len(environment_rows)==1 and environment_rows[0].get("kind")=="environment" and environment_rows[0].get("database")==CLONE and environment_rows[0].get("serverUuid")==prepared["serverUuid"],"Invalid frozen clone environment identity")
        frozen_environment=environment_rows[0]
        # The support helper uses explicit original-column null/value-separated per-row SHA; protect all source rows plus ledger.
        tables=prepared["originalTables"]+["infra_release_migration"]
        before=support.snapshot_original_rows(RecordingMysql(clone,private,"baseline"),tables);save_json(private/"baseline-original-rows.json",before)
        columns={table:row["columns"] for table,row in before.items()}
        ledger_existing=json_rows(protected_read(clone,ledger_query(prepared),private,"baseline-task-ledger"));require(not ledger_existing,"A task candidate migration ledger identity already exists in restored baseline")
        copied_baseline=capture_copied_baseline(clone,prepared,private);save_json(private/"baseline-v3-complete-copied-business.json",copied_baseline)
        first_schema_result=None;first_snapshot=None;seed_first=None
        for phase in ["first","repeat"]:
            read_identity(clone,{"database":CLONE,"serverUuid":prepared["serverUuid"],"mysqlVersion":prepared["mysqlVersion"]},private,phase+"-identity")
            validate_prepared(prepared)
            # Same-connection environment SELECT precedes the unchanged frozen nineteen-item bytes.
            prefix=Path(prepared["postflight"]["environmentQuery"]).read_bytes()+b"\n"
            material=Path(prepared["scripts"][phase]).read_bytes()
            run=clone.run_authorized_sql(prefix+material,private_directory=private,stem=phase+"-nineteen")
            require(run.get("sqlSha256")==hashlib.sha256(prefix+material).hexdigest(),"Actual combined SQL execution hash differs")
            run["materialSqlSha256"]=hashlib.sha256(material).hexdigest();run["sessionEnvelopeQuerySha256"]=hashlib.sha256(prefix[:-1]).hexdigest()
            journal["steps"].append(run);save_json(private/"driver-receipt.json",journal);check_markers(run["stdout"],prepared["entries"]);verify_session_environment(run["stdout"],frozen_environment)
            current=support.snapshot_original_rows(RecordingMysql(clone,private,phase),tables,columns);save_json(private/(phase+"-original-rows.json"),current)
            permitted=dict(prepared["permittedAdditions"]);permitted["infra_release_migration"]=19
            history=support.compare_original_rows(before,current,permitted);save_json(private/(phase+"-history-proof.json"),history)
            seeds=verify_seed_additions(clone,prepared,before,current,private,phase,copied_baseline);save_json(private/(phase+"-seed33-proof.json"),seeds)
            ledger=verify_ledger_additions(clone,prepared,before,current,private,phase);save_json(private/(phase+"-ledger19-proof.json"),ledger)
            table_names=protected_read(clone,"SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() ORDER BY TABLE_NAME;",private,phase+"-all-table-identities").splitlines()
            require(len(table_names)==939 and set(table_names)==set(prepared["schemaTableNames"])|set(prepared["newTables"]),"Final table identities are not original922 plus new17")
            empty=verify_new_tables(clone,prepared,private,phase);save_json(private/(phase+"-new17-empty.json"),empty)
            schema_result,schema=run_postflight(clone,prepared,private,phase,environment,first_schema_result)
            if phase=="first":first_schema_result=schema_result;first_snapshot=current;seed_first=seeds
            else:
                require(current==first_snapshot and seeds==seed_first,"Repeat changed original/new rows or ledger after first successful execution")
            journal["steps"].append({"phase":phase,"oldRowsUnchanged":True,"exactConfigAdditionCount":33,"exactLedgerAdditionCount":19,"newTablesEmpty":17,"schemaFingerprint":schema["schemaFingerprint"]});save_json(private/"driver-receipt.json",journal)
        journal["status"]="ISOLATED_REHEARSAL_PASS";journal["databaseWritesExecuted"]=True
        save_json(private/"driver-receipt.json",journal);return journal
    except Exception as exc:
        journal["status"]="FAILED_STOPPED_NO_AUTOMATIC_RECOVERY";journal["errorType"]=type(exc).__name__;journal["protectedErrorMessageFile"]=str(private/"driver-error.txt")
        (private/"driver-error.txt").write_text(str(exc),encoding="utf-8");save_json(private/"driver-receipt.json",journal)
        raise RehearsalError("Rehearsal stopped; inspect protected receipts. Clone retained; no automatic deletion/rollback") from exc

def ledger_query(prepared):
    ids=",".join("'"+x["migrationId"]+"'" for x in prepared["entries"])
    return "SELECT JSON_OBJECT('id',CAST(id AS CHAR),'migrationId',migration_id,'sha256',sha256,'fileName',file_name,'environment',target_environment,'status',status,'operationId',operation_id,'releaseTag',release_tag,'deleted',deleted+0,'tenant',CAST(tenant_id AS CHAR)) FROM infra_release_migration WHERE target_environment='test' AND migration_id IN ("+ids+") ORDER BY migration_id;"

def verify_ledger_additions(mysql,prepared,before,current,private,phase):
    rows=json_rows(protected_read(mysql,ledger_query(prepared),private,phase+"-ledger19"));by={r["migrationId"]:r for r in rows}
    require(len(rows)==len(by)==19 and set(by)=={x["migrationId"] for x in prepared["entries"]},"Task ledger identities missing/duplicate/extra")
    for entry in prepared["entries"]:
        r=by[entry["migrationId"]]
        require((r["sha256"],r["fileName"],r["environment"],r["status"],r["operationId"],r["releaseTag"],r["deleted"],r["tenant"])
                ==(entry["sha256"],entry["file"],"test","APPLIED",prepared["operationId"],prepared["operationId"],0,"0"),"Task-only appended ledger facts mismatch")
    added=set(current["infra_release_migration"]["rows"])-set(before["infra_release_migration"]["rows"])
    require({r["id"].encode().hex().upper() for r in rows}==added,"Ledger additions not exact queried task-owned keys")
    return {"oldLedgerUnchanged":True,"exactNewRows":rows,"newCount":19}

def copied_business_columns(prepared):
    entry=next(x for x in prepared["entries"] if x["migrationId"]=="20260930_dcc_a_workflow_bpmn_v4")
    sql=(Path(prepared["integrationRoot"])/"IntRuoyiBackend"/entry["file"]).read_text(encoding="utf-8-sig")
    result={}
    for table,alias in [("bpm_process_definition_info","old_info"),("act_re_procdef","old_procdef")]:
        match=re.search(r"INSERT INTO "+table+r"\s*\(.*?\)\s*SELECT (.*?)\nFROM tmp_dcc_three_workflow_processes",sql,re.S)
        require(match is not None,"Formal V4 copied business SELECT missing")
        fields=list(dict.fromkeys(re.findall(alias+r"\.([A-Za-z_0-9]+)",match[1])))
        if table=="act_re_procdef":fields=[x for x in fields if x!="ID_"] # ID_ maps to DERIVED_FROM_, already exact asserted.
        require(fields and all(re.fullmatch(r"[A-Za-z_][A-Za-z_0-9]*",x) for x in fields),"Invalid copied business field")
        require((table!="bpm_process_definition_info" or {"form_id","print_template_setting"}<=set(fields)),"Required form/copied fields missing")
        result[table]=fields
    return result

def copied_hash_sql(columns):
    terms=["CASE WHEN `"+x+"` IS NULL THEN 'N' ELSE CONCAT('V',HEX(CAST(`"+x+"` AS BINARY))) END" for x in columns]
    return "SHA2(CONCAT_WS('|',"+','.join(terms)+"),256)"

def capture_copied_baseline(mysql,prepared,private):
    contract=read_json(HERE/"g21-bpm-policy-contract.json")
    sources=[x for x in contract["sections"]["procdefs"] if x["key"] in {"dcc-controlled-file-upload","dcc-controlled-file-revision","dcc-controlled-file-obsolete"} and x["version"]==3]
    fields=copied_business_columns(prepared);ids={x["id"]:x["tenant"] for x in sources};require(len(ids)==6,"Exact six V3 sources required")
    quoted=",".join("'"+x+"'" for x in ids);result={}
    for table,columns in fields.items():
        identity="ID_" if table=="act_re_procdef" else "process_definition_id";tenant="TENANT_ID_" if table=="act_re_procdef" else "tenant_id"
        query="SELECT JSON_OBJECT('sourceDefinitionId',`"+identity+"`,'tenant',CAST(`"+tenant+"` AS CHAR),'copiedBusinessHash',"+copied_hash_sql(columns)+") FROM `"+table+"` WHERE `"+identity+"` IN ("+quoted+")"
        if table=="bpm_process_definition_info":query+=" AND deleted=b'0'"
        query+=" ORDER BY `"+identity+"`;"
        rows=json_rows(protected_read(mysql,query,private,"baseline-v3-complete-copy-"+table))
        require(len(rows)==6 and len({x['sourceDefinitionId'] for x in rows})==6 and {x['sourceDefinitionId'] for x in rows}==set(ids),"V3 complete copied source missing/duplicate")
        require(all(x["tenant"]==ids[x["sourceDefinitionId"]] and re.fullmatch(r"[0-9a-f]{64}",x["copiedBusinessHash"]) for x in rows),"V3 copied source tenant/hash mismatch")
        result[table]={"columns":columns,"sources":{x['sourceDefinitionId']:x['copiedBusinessHash'] for x in rows}}
    return result

def expected_seed_facts(prepared,copied_baseline):
    source=read_json(HERE/"g21-bpm-policy-contract.json")["sections"]
    require({table:row["columns"] for table,row in copied_baseline.items()}==copied_business_columns(prepared),"Full copied business projection differs")
    entry=next(x for x in prepared["entries"] if x["migrationId"]=="20260930_dcc_a_workflow_bpmn_v4")
    sql=(Path(prepared["integrationRoot"])/"IntRuoyiBackend"/entry["file"]).read_text(encoding="utf-8-sig")
    expected={x:[] for x in prepared["permittedAdditions"]}
    for action in ["upload","revision","obsolete"]:
        key="dcc-controlled-file-"+action
        match=re.search(r"SET @dcc_"+action+r"_bpmn = '(.*?)';",sql,re.S)
        require(match is not None,"Frozen V4 source XML missing")
        xml=match[1];name=re.search(r'<process id="'+key+r'" name="([^"]+)"',xml)[1]
        for tenant in ["1","122"]:
            old=next(x for x in source["procdefs"] if x["key"]==key and x["tenant"]==tenant and x["version"]==3)
            info=next(x for x in source["infos"] if x["procdefId"]==old["id"] and x["tenant"]==tenant)
            model=next(x for x in source["models"] if x["id"]==info["modelId"])
            deployment=key+"-deploy-v4-tenant-"+tenant;body=key+"-bpmn-v4-tenant-"+tenant;model_id=key+"-model-v4-tenant-"+tenant;definition=key+":4:dcc-three-workflow-tenant-"+tenant
            expected["act_re_deployment"].append({"identity":deployment,"key":key,"tenant":tenant,"name":name,"category":"DCC","parentId":deployment,"derivedFrom":None,"derivedRoot":None,"engineVersion":None})
            expected["act_ge_bytearray"].append({"identity":body,"revision":1,"name":key+".bpmn","deploymentId":deployment,"generated":0,"canonicalXmlSha256":hashlib.sha256(xml.encode()).hexdigest()})
            expected["act_re_model"].append({"identity":model_id,"key":key,"tenant":tenant,"name":name,"category":"DCC","version":4,"deploymentId":deployment,"editorSourceId":body,"editorExtraId":None,"metaHash":model["metaHash"],"revision":1})
            expected["act_re_procdef"].append({"identity":definition,"key":key,"tenant":tenant,"name":old["name"],"category":old["category"],"version":4,"suspension":1,"deploymentId":deployment,"resource":old["resource"],"derivedFrom":old["id"],"revision":1,"derivedVersion":0,"copiedBusinessHash":copied_baseline["act_re_procdef"]["sources"][old["id"]]})
            new_info={key:value for key,value in info.items() if key not in ["id","procdefId","modelId"]}
            new_info.update({"identity":definition,"procdefId":definition,"modelId":model_id,"creator":"codex","updater":"codex","copiedBusinessHash":copied_baseline["bpm_process_definition_info"]["sources"][old["id"]]});expected["bpm_process_definition_info"].append(new_info)
    for old in source["policies"]:
        if old["action"]=="OBSOLETE" and old["state"]=="ACTIVE" and old["status"]=="PUBLISHED":
            row={key:value for key,value in old.items() if key!="id"};row.update({"identity":old["tenant"]+":OBSOLETE:CONTROLLED_PENDING_EFFECTIVE","state":"CONTROLLED_PENDING_EFFECTIVE"});expected["bpm_business_approval_policy"].append(row)
    template_sql=(Path(prepared["integrationRoot"])/"IntRuoyiBackend/sql/mysql/20260930_dcc_d_notification_template.sql").read_text(encoding="utf-8-sig")
    match=re.search(r"VALUES \('([^']*)','dcc_relation_remediation',2,'([^']*)',\s*'([^']*)',\s*'([^']*)',0",template_sql,re.S)
    require(match is not None,"Remediation source template is missing")
    expected["system_notify_template"].append({"identity":"dcc_relation_remediation","code":"dcc_relation_remediation","name":match[1],"nickname":match[2],"contentHash":hashlib.sha256(match[3].encode()).hexdigest(),"params":match[4],"type":2,"status":0,"deleted":0})
    require(sum(len(v) for v in expected.values())==33,"Expected configuration identity count differs")
    return expected

def seed_queries(expected,copied_columns):
    quote=lambda v:"'"+v.replace("'","''")+"'"
    queries={}
    field_maps={
      "act_re_deployment":{"identity":"ID_","key":"KEY_","tenant":"TENANT_ID_","name":"NAME_","category":"CATEGORY_","parentId":"PARENT_DEPLOYMENT_ID_","derivedFrom":"DERIVED_FROM_","derivedRoot":"DERIVED_FROM_ROOT_","engineVersion":"ENGINE_VERSION_"},
      "act_ge_bytearray":{"identity":"ID_","revision":"REV_","name":"NAME_","deploymentId":"DEPLOYMENT_ID_","generated":"GENERATED_"},
      "act_re_model":{"identity":"ID_","key":"KEY_","tenant":"TENANT_ID_","name":"NAME_","category":"CATEGORY_","version":"VERSION_","deploymentId":"DEPLOYMENT_ID_","editorSourceId":"EDITOR_SOURCE_VALUE_ID_","editorExtraId":"EDITOR_SOURCE_EXTRA_VALUE_ID_","revision":"REV_"},
      "act_re_procdef":{"identity":"ID_","key":"KEY_","tenant":"TENANT_ID_","name":"NAME_","category":"CATEGORY_","version":"VERSION_","suspension":"SUSPENSION_STATE_","deploymentId":"DEPLOYMENT_ID_","resource":"RESOURCE_NAME_","derivedFrom":"DERIVED_FROM_","revision":"REV_","derivedVersion":"DERIVED_VERSION_"},
      "bpm_process_definition_info":{"identity":"process_definition_id","procdefId":"process_definition_id","modelId":"model_id","tenant":"tenant_id","deleted":"deleted","formType":"form_type","createPath":"form_custom_create_path","viewPath":"form_custom_view_path","modelType":"model_type","visible":"visible","creator":"creator","updater":"updater"},
      "bpm_business_approval_policy":{"tenant":"tenant_id","domain":"data_domain","system":"system_code","objectType":"object_type","action":"action_code","state":"object_state","mode":"policy_mode","processKey":"process_definition_key","executor":"effect_executor_code","formPolicyType":"form_policy_type","formSlots":"form_slots_json","status":"status","deleted":"deleted"},
      "system_notify_template":{"identity":"code","code":"code","name":"name","nickname":"nickname","type":"type","status":"status","params":"params","deleted":"deleted"}}
    for table,rows in expected.items():
        primary="ID_" if table.startswith("act_") else "id"
        pairs=["'id'","CAST(`"+primary+"` AS CHAR)"]
        for field,col in field_maps[table].items():
            value="`"+col+"`"
            if field=="tenant":value="CAST("+value+" AS CHAR)"
            if field in ["deleted","visible","generated"]:value="("+value+"+0)"
            pairs.extend([quote(field),value])
        if table in copied_columns:pairs.extend(["'copiedBusinessHash'",copied_hash_sql(copied_columns[table])])
        if table=="act_ge_bytearray":pairs.extend(["'canonicalXmlSha256'","SHA2(REPLACE(CONVERT(BYTES_ USING utf8mb4),CONCAT(CHAR(13),CHAR(10)),CHAR(10)),256)"])
        if table=="act_re_model":pairs.extend(["'metaHash'","SHA2(META_INFO_,256)"])
        if table=="bpm_process_definition_info":
            for field in ["form_conf","form_fields","start_user_ids","start_dept_ids","manager_user_ids","title_setting","summary_setting","process_before_trigger_setting","process_after_trigger_setting","task_before_trigger_setting","task_after_trigger_setting"]:pairs.extend([quote(field+"Hash"),"SHA2(`"+field+"`,256)"])
        if table=="bpm_business_approval_policy":
            pairs.extend(["'identity'","CONCAT(tenant_id,':OBSOLETE:CONTROLLED_PENDING_EFFECTIVE')"])
            where="tenant_id IN (1,122) AND data_domain='DCC' AND system_code='DCC' AND object_type='CONTROLLED_FILE' AND action_code='OBSOLETE' AND object_state='CONTROLLED_PENDING_EFFECTIVE'"
        elif table=="system_notify_template":pairs.extend(["'contentHash'","SHA2(content,256)"]);where="code='dcc_relation_remediation'"
        else:where="`"+field_maps[table]["identity"]+"` IN ("+",".join(quote(r["identity"]) for r in rows)+")"
        queries[table]="SELECT JSON_OBJECT("+",".join(pairs)+") FROM `"+table+"` WHERE "+where+" ORDER BY `"+primary+"`;"
    return queries

def validate_seed_rows(expected,actual,before,current):
    require(set(actual)==set(expected),"Exact seed table scope differs")
    outcomes={}
    for table,wanted in expected.items():
        rows=actual[table];ids=[row.get("identity") for row in rows];by={x["identity"]:x for x in wanted}
        require(len(ids)==len(set(ids))==len(wanted) and set(ids)==set(by),"Seed identity missing/duplicate/conflicting: "+table)
        for row in rows:
            require(isinstance(row.get("id"),str) and row["id"],"Actual appended seed primary identity is required")
            require({k:v for k,v in row.items() if k!="id"}==by[row["identity"]],"Exact seed payload/linkage/XML/policy/template mismatch: "+table)
        added=set(current[table]["rows"])-set(before[table]["rows"])
        require({x["id"].encode().hex().upper() for x in rows}==added,"Seed new primary keys not exact33 additions: "+table)
        outcomes[table]={"count":len(rows),"identities":sorted(ids),"payloadSha256":hashlib.sha256(json.dumps(rows,sort_keys=True,ensure_ascii=False,separators=(",",":")).encode()).hexdigest()}
    require(sum(x["count"] for x in outcomes.values())==33,"Configuration additions are not exact33")
    return outcomes

def verify_seed_additions(mysql,prepared,before,current,private,phase,copied_baseline):
    expected=expected_seed_facts(prepared,copied_baseline)
    actual={table:json_rows(protected_read(mysql,query,private,phase+"-seed33-"+table)) for table,query in seed_queries(expected,{table:row["columns"] for table,row in copied_baseline.items()}).items()}
    return validate_seed_rows(expected,actual,before,current)

def main():
    parser=argparse.ArgumentParser(description=__doc__);commands=parser.add_subparsers(dest="mode",required=True)
    setup=commands.add_parser("prepare");setup.add_argument("--postflight-package",required=True);setup.add_argument("--result",required=True)
    check=commands.add_parser("validate");check.add_argument("--prepared",required=True)
    run=commands.add_parser("rehearse");run.add_argument("--prepared",required=True);run.add_argument("--private-directory",required=True);run.add_argument("--authorize-rehearsal-writes",action="store_true")
    args=parser.parse_args()
    try:
        if args.mode=="prepare":result=prepare(postflight_path=Path(args.postflight_package));save_json(args.result,result)
        elif args.mode=="validate":result=validate_prepared(read_json(args.prepared))
        else:result=rehearse(read_json(args.prepared),authorize_rehearsal_writes=args.authorize_rehearsal_writes,private_directory=args.private_directory)
    except (ValueError,OSError,KeyError) as exc:
        print(json.dumps({"status":"REJECTED_NOT_PASS","errorType":type(exc).__name__,"reason":str(exc),"actualUserApprovalProvenByFlag":False}));raise SystemExit(1)
    print(json.dumps({"status":result["status"],"databaseWritesExecuted":result.get("databaseWritesExecuted",False),"actualUserApprovalProvenByFlag":False}))

if __name__=="__main__":main()
