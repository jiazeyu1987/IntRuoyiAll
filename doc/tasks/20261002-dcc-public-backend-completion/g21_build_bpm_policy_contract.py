"""Offline build of readonly G21 contracts; no database, subprocess or migration execution."""
from pathlib import Path
import argparse, gzip, hashlib, json, re

TASK = Path(__file__).resolve().parent
IDS = ["20260922_dcc_three_workflow_bpmn_seed", "20260923_dcc_three_workflow_candidate_strategy_fix", "20260926_dcc_three_workflow_matrix_multi_instance_fix", "20260719_business_approval_policy", "20260719_dcc_upload_form_policy_seed", "20260720_dcc_publish_form_policy_seed", "20260906_dcc_new_file_lifecycle_p4", "20260907_dcc_publication_notification"]
NATIVE = ["dcc-controlled-file-upload", "dcc-controlled-file-revision", "dcc-controlled-file-obsolete"]
KEYS = NATIVE + ["dcc-controlled-file-approval"]
SCHEMA_TABLES = ["bpm_business_approval_policy", "bpm_business_approval_request", "dcc_publication_notification_delivery", "dcc_publication_notification_audit"]

def digest(value):
    if value is None: return None
    if isinstance(value, bytes): return hashlib.sha256(value).hexdigest()
    return hashlib.sha256(str(value).encode("utf-8")).hexdigest()

def split_sql(text):
    parts, start, quoted, escaped, depth = [], 0, False, False, 0
    for i, c in enumerate(text):
        if quoted:
            if escaped: escaped = False
            elif c == "\\": escaped = True
            elif c == "'": quoted = False
        elif c == "'": quoted = True
        elif c == "(": depth += 1
        elif c == ")": depth -= 1
        elif c == "," and depth == 0: parts.append(text[start:i]); start = i + 1
    if quoted or depth: raise ValueError("Unterminated dump tuple")
    return parts + [text[start:]]

def sql_value(text):
    text = text.strip()
    if text == "NULL": return None
    if text.startswith("0x"): return bytes.fromhex(text[2:])
    if text.startswith("'") and text.endswith("'"):
        return re.sub(r"\\(.)", lambda m: {"0":"\0", "n":"\n", "r":"\r", "t":"\t", "Z":"\x1a"}.get(m[1],m[1]), text[1:-1])
    return int(text)

def number(value):
    return int.from_bytes(value,"big") if isinstance(value,bytes) else value

def read_metadata(affected):
    rows = {}
    for table in ["act_re_procdef", "act_re_model", "act_re_deployment", "act_ge_bytearray", "bpm_process_definition_info", "bpm_business_approval_policy", "system_notify_template"]:
        definition = re.search(r"CREATE TABLE `"+table+r"` \((.*?)\) ENGINE=.*?;", affected, re.S)
        columns = re.findall(r"^\s*`([^`]+)` ", definition[1], re.M)
        rows[table] = []
        for insertion in re.finditer(r"INSERT INTO `"+table+r"`(?: \((.*?)\))? VALUES (.*?);\n", affected, re.S):
            names = re.findall(r"`([^`]+)`", insertion[1]) if insertion[1] else columns
            for item in split_sql(insertion[2]):
                values = [sql_value(v) for v in split_sql(item.strip()[1:-1])]
                if len(names) != len(values): raise ValueError("Dump metadata tuple width mismatch: "+table)
                rows[table].append(dict(zip(names,values)))
    return rows

def make_schema_contract(backend):
    columns, indexes = [], []
    for filename in ["20260719_business_approval_policy.sql", "20260907_dcc_publication_notification.sql"]:
        sql = (backend/"sql/mysql"/filename).read_text(encoding="utf-8-sig")
        for match in re.finditer(r"CREATE TABLE IF NOT EXISTS `([^`]+)` \((.*?)\) ENGINE",sql,re.S):
            table, body = match[1], match[2]
            if table not in SCHEMA_TABLES: continue
            for line in body.splitlines():
                m = re.match(r"\s*`([^`]+)`\s+([a-z]+(?:\(\d+\))?)(.*)",line,re.I)
                if m:
                    name, kind, rest = m.groups()
                    generated = "GENERATED ALWAYS AS" in rest.upper()
                    default = re.search(r"\bDEFAULT\s+(b'[^']*'|'[^']*'|CURRENT_TIMESTAMP|NULL|\d+)",rest,re.I)
                    value = None if not default or default[1].upper()=="NULL" else default[1]
                    if value:
                        if value.upper()=="CURRENT_TIMESTAMP": value="CURRENT_TIMESTAMP"
                        elif value.startswith("b'"): value=value
                        elif value.startswith("'"): value=value[1:-1]
                    entry={"table":table,"column":name,"type":kind.lower(),"nullable":"NO" if "NOT NULL" in rest.upper() else "YES","default":value,"collation":"utf8mb4_unicode_ci" if kind.lower().startswith(("varchar","longtext")) else None}
                    if generated:
                        expression = re.search(r"GENERATED ALWAYS AS\s*\((.*?)\)\s*(VIRTUAL|STORED)",body[body.index(line):],re.S|re.I)
                        entry["generationExpression"]=expression[1];entry["generatedKind"]=expression[2].upper()
                    columns.append(entry)
                m=re.match(r"\s*(PRIMARY KEY|UNIQUE KEY `([^`]+)`|KEY `([^`]+)`)\s*\(([^)]+)\)",line)
                if m:
                    name="PRIMARY" if m[1]=="PRIMARY KEY" else m[2] or m[3]
                    indexes.append({"table":table,"index":name,"nonUnique":0 if name=="PRIMARY" or m[2] else 1,"columns":re.findall(r"`([^`]+)`",m[4])})
    return columns,indexes

def read_schema_fixture(schema, contract):
    sections={"tables":[],"columns":[],"indexes":[]}
    for table in SCHEMA_TABLES:
        match=re.search(r"CREATE TABLE `"+table+r"` \((.*?)\) ENGINE=([^ ]+) .*?COLLATE=([^ ;]+).*?;",schema,re.S)
        if not match:raise ValueError("Protected schema table missing "+table)
        body,engine,collation=match.groups();sections["tables"].append({"table":table,"engine":engine,"collation":collation})
        for line in body.splitlines():
            m=re.match(r"\s*`([^`]+)`\s+([a-z]+(?:\(\d+\))?)(.*)",line,re.I)
            if m:
                name,kind,rest=m.groups();generated=re.search(r"GENERATED ALWAYS AS\s*\((.*?)\)\s*(VIRTUAL|STORED)",rest,re.S|re.I)
                default=re.search(r"\bDEFAULT\s+(b'[^']*'|'[^']*'|CURRENT_TIMESTAMP(?:\(\))?|NULL|\d+)",rest,re.I)
                value=None if not default or default[1].upper()=="NULL" else default[1]
                if value and value.startswith("'"):value=value[1:-1]
                row={"table":table,"column":name,"type":kind.lower(),"nullable":"NO" if "NOT NULL" in rest.upper() else "YES","default":value,"collation":"utf8mb4_unicode_ci" if kind.lower().startswith(("varchar","longtext")) else None,"generationExpression":generated[1] if generated else None,"generatedKind":generated[2].upper() if generated else None}
                sections["columns"].append(row)
            m=re.match(r"\s*(PRIMARY KEY|UNIQUE KEY `([^`]+)`|KEY `([^`]+)`)\s*\(([^)]+)\)",line)
            if m:
                name="PRIMARY" if m[1]=="PRIMARY KEY" else m[2] or m[3]
                for i,col in enumerate(re.findall(r"`([^`]+)`",m[4]),1):sections["indexes"].append({"table":table,"index":name,"sequence":i,"column":col,"nonUnique":0 if name=="PRIMARY" or m[2] else 1,"subPart":None,"indexType":"BTREE"})
    return sections

def build(main, backend):
    receipt=json.loads((main/"g18-backup-receipt.json").read_text(encoding="utf-8-sig"))
    protected={x["kind"]:x for x in receipt["artifacts"]}
    for kind in ["schema","affected"]:
        if digest(Path(protected[kind]["path"]).read_bytes())!=protected[kind]["sha256"]: raise ValueError("Protected backup checksum mismatch")
    metadata=read_metadata(gzip.open(protected["affected"]["path"],"rt",encoding="utf-8").read())
    procs=[x for x in metadata["act_re_procdef"] if x["KEY_"] in KEYS and x["TENANT_ID_"] in ["1","122"]]
    proc_ids={x["ID_"] for x in procs};infos=[x for x in metadata["bpm_process_definition_info"] if x["process_definition_id"] in proc_ids]
    model_ids={x["model_id"] for x in infos};models=[x for x in metadata["act_re_model"] if x["ID_"] in model_ids]
    deployment_ids={x["DEPLOYMENT_ID_"] for x in procs}|{x["DEPLOYMENT_ID_"] for x in models}
    deployments=[x for x in metadata["act_re_deployment"] if x["ID_"] in deployment_ids]
    body_ids={x["EDITOR_SOURCE_VALUE_ID_"] for x in models}|{x["EDITOR_SOURCE_EXTRA_VALUE_ID_"] for x in models if x["EDITOR_SOURCE_EXTRA_VALUE_ID_"]}
    bodies=[x for x in metadata["act_ge_bytearray"] if x["ID_"] in body_ids or any(x["DEPLOYMENT_ID_"]==p["DEPLOYMENT_ID_"] and x["NAME_"]==p["RESOURCE_NAME_"] for p in procs)]
    fields={
     "procdefs":{"id":"ID_","key":"KEY_","name":"NAME_","tenant":"TENANT_ID_","version":"VERSION_","suspension":"SUSPENSION_STATE_","deploymentId":"DEPLOYMENT_ID_","resource":"RESOURCE_NAME_","category":"CATEGORY_","derivedFrom":"DERIVED_FROM_"},
     "models":{"id":"ID_","key":"KEY_","name":"NAME_","tenant":"TENANT_ID_","version":"VERSION_","deploymentId":"DEPLOYMENT_ID_","editorSourceId":"EDITOR_SOURCE_VALUE_ID_","editorExtraId":"EDITOR_SOURCE_EXTRA_VALUE_ID_","category":"CATEGORY_"},
     "deployments":{"id":"ID_","key":"KEY_","name":"NAME_","tenant":"TENANT_ID_","category":"CATEGORY_","parentId":"PARENT_DEPLOYMENT_ID_"},
     "infos":{"id":"id","procdefId":"process_definition_id","modelId":"model_id","tenant":"tenant_id","deleted":"deleted","formType":"form_type","createPath":"form_custom_create_path","viewPath":"form_custom_view_path","modelType":"model_type","visible":"visible"},
     "bodies":{"id":"ID_","name":"NAME_","deploymentId":"DEPLOYMENT_ID_","revision":"REV_","generated":"GENERATED_"},
     "policies":{"id":"id","tenant":"tenant_id","domain":"data_domain","system":"system_code","objectType":"object_type","action":"action_code","state":"object_state","mode":"policy_mode","processKey":"process_definition_key","executor":"effect_executor_code","formPolicyType":"form_policy_type","formSlots":"form_slots_json","status":"status","deleted":"deleted"},
     "templates":{"id":"id","code":"code","name":"name","nickname":"nickname","type":"type","status":"status","params":"params","deleted":"deleted"}}
    selected={"procdefs":procs,"models":models,"deployments":deployments,"infos":infos,"bodies":bodies,
      "policies":[x for x in metadata["bpm_business_approval_policy"] if x["tenant_id"] in [1,122] and x["data_domain"]=="DCC" and x["system_code"]=="DCC" and x["object_type"]=="CONTROLLED_FILE" and x["action_code"] in ["UPLOAD","PUBLISH","OBSOLETE"]],
      "templates":[x for x in metadata["system_notify_template"] if x["code"]=="dcc_publication_released"]}
    expected={}
    for section, rows in selected.items():
        expected[section]=[]
        for row in rows:
            converted={out:str(number(row[col])) if out in ["id","tenant"] and row[col] is not None else number(row[col]) for out,col in fields[section].items()}
            if section=="models": converted["metaHash"]=digest(row["META_INFO_"])
            if section=="bodies":converted.update({"sha256":digest(row["BYTES_"]),"octets":len(row["BYTES_"]),"xmlHex":row["BYTES_"].hex().upper() if row["ID_"].startswith("dcc-controlled-file-") else None})
            if section=="templates":converted["contentHash"]=digest(row["content"])
            if section=="infos":
                for col in ["form_conf","form_fields","start_user_ids","start_dept_ids","manager_user_ids","title_setting","summary_setting","process_before_trigger_setting","process_after_trigger_setting","task_before_trigger_setting","task_after_trigger_setting"]:converted[col+"Hash"]=digest(row[col])
            expected[section].append(converted)
    columns,indexes=make_schema_contract(backend)
    checklist=json.loads((main/"g20-external-prerequisite-checklist.json").read_text(encoding="utf-8-sig"))
    sources={x["migrationId"]:x for x in checklist["prerequisites"] if x["migrationId"] in IDS}
    for mid in IDS:
        raw=(backend/"sql/mysql"/(mid+".sql")).read_bytes()
        if digest(raw)!=sources[mid]["currentFileSha256"]:raise ValueError("Formal source/checklist hash mismatch "+mid)
    fresh_receipt_path=main/"g21-bpm-runtime-facts.jsonl.receipt.json"
    fresh_receipt=json.loads(fresh_receipt_path.read_text(encoding="utf-8-sig"))
    require_uuid=fresh_receipt["capture"]["serverUuid"]
    if fresh_receipt["capture"]["database"]!="ruoyi-vue-pro" or fresh_receipt["capture"]["version"]!="8.0.40":raise ValueError("Root readonly capture target identity mismatch")
    contract={"expectedServerUuid":require_uuid,"serverIdentityReceiptSha256":digest(fresh_receipt_path.read_bytes()),"contractVersion":"g21-bpm-policy-v1","semantics":"satisfied_by_exact_existing_target_facts_not_migration_execution","database":"ruoyi-vue-pro","mysqlVersion":"8.0.40","tenants":["1","122"],"migrationIds":IDS,"migrationMustNotExecute":True,"inventAppliedHistory":False,
     "sourceHashes":{mid:sources[mid]["currentFileSha256"] for mid in IDS},"baselineEvidence":{"g18BackupReceiptSha256":digest((main/"g18-backup-receipt.json").read_bytes()),"affectedDumpSha256":protected["affected"]["sha256"],"schemaDumpSha256":protected["schema"]["sha256"],"g18ConfigLogSha256":digest((main/"g18-runtime-configuration-preflight.log").read_bytes())},
     "sections":expected,"schemaColumns":columns,"schemaIndexes":indexes,
     "acceptance":{"nativeV1V2":"exact frozen historical identities/XML retained suspended under V3","nativeV3":"exact required current six native model/definition/body/info chains active","publish":"exact disabled historical BPM_REQUIRED plus current P4 DIRECT PUBLISHED; never infer July seed executed","publicationTemplate":"one exact registered global template, no overwrite"}}
    # Keep executable XML only in an isolated test fixture; the JSON contract pins hashes.
    fixture={"contractVersion":contract["contractVersion"],"sections":expected.copy()}
    contract["sections"]={k:[{a:b for a,b in x.items() if a!="xmlHex"} for x in rows] for k,rows in expected.items()}
    schema=gzip.open(protected["schema"]["path"],"rt",encoding="utf-8").read()
    fixture["sections"].update(read_schema_fixture(schema,contract))
    fixture["counts"]={k:len(v) for k,v in fixture["sections"].items()}
    fixture["capture"]={"database":"ruoyi-vue-pro","mysqlVersion":"8.0.40","serverUuid":"OFFLINE-PROTECTED-DUMP-NOT-FRESH-DB","captureId":"OFFLINE-G21-ONLY","capturedAt":"2026-10-03T00:00:00.000000Z","queryId":"g21-bpm-policy-facts-select-v1","endDatabase":"ruoyi-vue-pro","endServerUuid":"OFFLINE-PROTECTED-DUMP-NOT-FRESH-DB","endCaptureId":"OFFLINE-G21-ONLY","endCapturedAt":"2026-10-03T00:00:01.000000Z"}
    fixture["offlineOnly"]=True
    return contract,fixture,fields,selected

def quote(value):
    return "'"+value.replace("'","''")+"'"

def make_query(contract, fields):
    literals=lambda values:",".join(quote(str(x)) for x in values)
    keys=literals(KEYS);tenants=literals(contract["tenants"])
    proc_ids=literals(x["id"] for x in contract["sections"]["procdefs"])
    model_ids=literals(x["id"] for x in contract["sections"]["models"])
    dep_ids=literals(x["id"] for x in contract["sections"]["deployments"])
    body_ids=literals(x["id"] for x in contract["sections"]["bodies"])
    proc_scope=f"((p.KEY_ IN ({keys}) AND p.TENANT_ID_ IN ({tenants})) OR p.ID_ IN ({proc_ids}))"
    info_scope=f"(i.process_definition_id IN (SELECT p.ID_ FROM act_re_procdef p WHERE {proc_scope}) OR i.process_definition_id IN ({proc_ids}))"
    model_scope=f"(m.ID_ IN ({model_ids}) OR m.ID_ IN (SELECT i.model_id FROM bpm_process_definition_info i WHERE {info_scope}) OR (m.KEY_ IN ({literals(NATIVE)}) AND m.TENANT_ID_ IN ({tenants})))"
    dep_scope=f"(d.ID_ IN ({dep_ids}) OR d.ID_ IN (SELECT p.DEPLOYMENT_ID_ FROM act_re_procdef p WHERE {proc_scope}) OR d.ID_ IN (SELECT m.DEPLOYMENT_ID_ FROM act_re_model m WHERE {model_scope}))"
    body_scope=f"(b.ID_ IN ({body_ids}) OR EXISTS(SELECT 1 FROM act_re_procdef p WHERE {proc_scope} AND b.DEPLOYMENT_ID_=p.DEPLOYMENT_ID_ AND b.NAME_=p.RESOURCE_NAME_) OR b.ID_ IN (SELECT m.EDITOR_SOURCE_VALUE_ID_ FROM act_re_model m WHERE {model_scope}) OR b.ID_ IN (SELECT m.EDITOR_SOURCE_EXTRA_VALUE_ID_ FROM act_re_model m WHERE {model_scope}))"
    from_where={"procdefs":("act_re_procdef p",proc_scope),"infos":("bpm_process_definition_info i",info_scope),"models":("act_re_model m",model_scope),"deployments":("act_re_deployment d",dep_scope),"bodies":("act_ge_bytearray b",body_scope),
      "policies":("bpm_business_approval_policy a",f"a.tenant_id IN ({tenants}) AND a.data_domain='DCC' AND a.system_code='DCC' AND a.object_type='CONTROLLED_FILE' AND a.action_code IN ('UPLOAD','PUBLISH','OBSOLETE')"),"templates":("system_notify_template n","n.code='dcc_publication_released' OR n.id=6829")}
    aliases={"procdefs":"p","infos":"i","models":"m","deployments":"d","bodies":"b","policies":"a","templates":"n"}
    statements=["-- G21 standalone SELECT-only capture. Run in one target-bound readonly session; no USE/SET/DML/DDL.\n-- mysql --batch --raw --skip-column-names --default-character-set=utf8mb4 DATABASE < this-file\n-- Root verifies file SHA before execution and supplies server/database identity in its fresh merged receipt.\nSELECT JSON_OBJECT('section','capture_begin','contractVersion','g21-bpm-policy-v1','queryId','g21-bpm-policy-facts-select-v1','database',DATABASE(),'mysqlVersion',@@version,'serverUuid',@@server_uuid,'captureId',CAST(CONNECTION_ID() AS CHAR),'capturedAt',DATE_FORMAT(UTC_TIMESTAMP(6),'%Y-%m-%dT%H:%i:%s.%fZ'));" ]
    for section in ["procdefs","models","deployments","infos","bodies","policies","templates"]:
        alias=aliases[section];pairs=["'section'",quote(section)]
        for out,col in fields[section].items():
            expression=f"{alias}.`{col}`"
            if out in ["id","tenant"]:expression=f"CAST({expression} AS CHAR)"
            elif out in ["deleted","visible","generated"]:expression=f"({expression}+0)"
            pairs.extend([quote(out),expression])
        if section=="models":pairs.extend(["'metaHash'",f"SHA2({alias}.META_INFO_,256)"])
        if section=="bodies":pairs.extend(["'sha256'","SHA2(b.BYTES_,256)","'octets'","OCTET_LENGTH(b.BYTES_)","'xmlHex'","CASE WHEN b.ID_ LIKE 'dcc-controlled-file-%' THEN HEX(b.BYTES_) ELSE NULL END"])
        if section=="templates":pairs.extend(["'contentHash'","SHA2(n.content,256)"])
        if section=="infos":
            for key in ["form_conf","form_fields","start_user_ids","start_dept_ids","manager_user_ids","title_setting","summary_setting","process_before_trigger_setting","process_after_trigger_setting","task_before_trigger_setting","task_after_trigger_setting"]:pairs.extend([quote(key+"Hash"),f"SHA2(i.`{key}`,256)"])
        table,predicate=from_where[section]
        statements.append("SELECT JSON_OBJECT("+",".join(pairs)+f") FROM {table} WHERE {predicate} ORDER BY {alias}.`{fields[section]['id']}`;")
        statements.append(f"SELECT JSON_OBJECT('section','count','name','{section}','count',COUNT(*)) FROM {table} WHERE {predicate};")
    scope=f"TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ({literals(SCHEMA_TABLES)})"
    statements.extend([
       "SELECT JSON_OBJECT('section','tables','table',TABLE_NAME,'engine',ENGINE,'collation',TABLE_COLLATION) FROM information_schema.TABLES WHERE "+scope+" ORDER BY TABLE_NAME;",
       "SELECT JSON_OBJECT('section','count','name','tables','count',COUNT(*)) FROM information_schema.TABLES WHERE "+scope+";",
       "SELECT JSON_OBJECT('section','columns','table',TABLE_NAME,'column',COLUMN_NAME,'type',COLUMN_TYPE,'nullable',IS_NULLABLE,'default',COLUMN_DEFAULT,'collation',COLLATION_NAME,'generationExpression',NULLIF(GENERATION_EXPRESSION,''),'generatedKind',CASE WHEN EXTRA LIKE '%VIRTUAL GENERATED%' THEN 'VIRTUAL' WHEN EXTRA LIKE '%STORED GENERATED%' THEN 'STORED' ELSE NULL END) FROM information_schema.COLUMNS WHERE "+scope+" ORDER BY TABLE_NAME,ORDINAL_POSITION;",
       "SELECT JSON_OBJECT('section','count','name','columns','count',COUNT(*)) FROM information_schema.COLUMNS WHERE "+scope+";",
       "SELECT JSON_OBJECT('section','indexes','table',TABLE_NAME,'index',INDEX_NAME,'sequence',SEQ_IN_INDEX,'column',COLUMN_NAME,'nonUnique',NON_UNIQUE,'subPart',SUB_PART,'indexType',INDEX_TYPE) FROM information_schema.STATISTICS WHERE "+scope+" ORDER BY TABLE_NAME,INDEX_NAME,SEQ_IN_INDEX;",
       "SELECT JSON_OBJECT('section','count','name','indexes','count',COUNT(*)) FROM information_schema.STATISTICS WHERE "+scope+";",
       "SELECT JSON_OBJECT('section','capture_end','database',DATABASE(),'serverUuid',@@server_uuid,'captureId',CAST(CONNECTION_ID() AS CHAR),'capturedAt',DATE_FORMAT(UTC_TIMESTAMP(6),'%Y-%m-%dT%H:%i:%s.%fZ'));" ])
    return "\n\n".join(statements)+"\n"

def main():
    parser=argparse.ArgumentParser();parser.add_argument("--main-task",required=True);parser.add_argument("--backend-root",required=True);args=parser.parse_args()
    contract,fixture,fields,selected=build(Path(args.main_task),Path(args.backend_root))
    contract["checksByMigration"]={
      IDS[0]:["native-history-v1","native-current-v3-chain","obsolete-active-policy"],
      IDS[1]:["native-history-v2","native-current-v3-strategies","native-editor-resource-linkage"],
      IDS[2]:["native-current-v3-chain","native-v3-obligation-multi-instance","v3-six-model-definition-info-tenant-links"],
      IDS[3]:["policy-request-table-columns","published-pending-generated-identities","policy-request-unique-indexes","current-dcc-policy-identity-uniqueness"],
      IDS[4]:["two-tenant-upload-published-exact","legacy-latest-active-procdef-info-resource"],
      IDS[5]:["historical-publish-bpm-required-disabled","p4-current-direct-exact","two-tenant-upload-published-exact"],
      IDS[6]:["p4-current-direct-exact","historical-publish-bpm-required-disabled","publish-none-empty-slots"],
      IDS[7]:["notification-table-columns-indexes","one-exact-publication-template"]}
    query=make_query(contract,fields)
    (TASK/"g21-bpm-policy-facts.sql").write_text(query,encoding="utf-8")
    contract["queryFile"]="g21-bpm-policy-facts.sql";contract["querySha256"]=digest((TASK/"g21-bpm-policy-facts.sql").read_bytes())
    (TASK/"g21-bpm-policy-contract.json").write_text(json.dumps(contract,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    (TASK/"g21-bpm-policy-offline-fixture.json").write_text(json.dumps(fixture,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(json.dumps({"migrationCount":len(contract["migrationIds"]),"sections":{k:len(v) for k,v in contract["sections"].items()},"schemaColumns":len(contract["schemaColumns"]),"schemaIndexes":len(contract["schemaIndexes"])}))

if __name__=="__main__":main()
