"""Validate target-specific readonly JSONL evidence. Does not connect, infer APPLIED, or execute SQL."""
import argparse, copy, hashlib, json, re
from pathlib import Path
from xml.etree import ElementTree as ET

TASK = Path(__file__).resolve().parent
NATIVE = {"dcc-controlled-file-upload", "dcc-controlled-file-revision", "dcc-controlled-file-obsolete"}
NS = {"b":"http://www.omg.org/spec/BPMN/20100524/MODEL", "f":"http://flowable.org/bpmn"}
class ContractError(ValueError): pass

def fail(reason): raise ContractError(reason)
def require(condition, reason):
    if not condition: fail(reason)

def generation_tokens(expression):
    if not expression: return []
    # MySQL I_S renders simple _utf8mb4 string delimiters as escaped quotes. Only this
    # introducer/literal grammar is normalized; content, operators and field order stay exact.
    expression=re.sub(r"_utf8mb4\\'([A-Za-z0-9_|]+)\\'",lambda m:"_utf8mb4'"+m[1]+"'",expression)
    result=[]
    for match in re.finditer(r"(?:_[a-z0-9]+)?'(?:[^']|'')*'|b'[01]+'|0x[0-9a-f]+|`[^`]+`|[A-Za-z_][A-Za-z_0-9]*|\d+|[^\s]",expression,re.I):
        value=match[0]
        if value.lower() in ("b'0'","0x00"): value="0"
        if value.startswith("_utf8mb4"):value=value[len("_utf8mb4"):]
        if value in ("(",")"):continue
        if value.startswith("`"):value=value[1:-1]
        if not value.startswith("'"):value=value.lower()
        result.append(value)
    return result

def normalize_default(value):
    if isinstance(value,str) and value.lower().startswith("current_timestamp"):return "CURRENT_TIMESTAMP"
    return value

def validate_sections(contract, sections):
    for section, expected in contract["sections"].items():
        actual=sections.get(section)
        require(isinstance(actual,list),"Missing evidence section: "+section)
        ids=[row.get("id") for row in actual]
        require(all(isinstance(x,str) and x for x in ids),"Non-string/empty identity: "+section)
        require(len(ids)==len(set(ids)),"Duplicate identity: "+section)
        wanted={row["id"]:row for row in expected}
        require(set(ids)==set(wanted),"Missing/unexpected identities: "+section)
        for row in actual:
            for key,value in wanted[row["id"]].items():
                require(key in row and row[key]==value,f"Fact mismatch {section}:{row['id']}:{key}")

    procdefs={x["id"]:x for x in sections["procdefs"]};models={x["id"]:x for x in sections["models"]}
    bodies={x["id"]:x for x in sections["bodies"]};deps={x["id"]:x for x in sections["deployments"]}
    infos=sections["infos"]
    for proc in procdefs.values():
        require(proc["deploymentId"] in deps,"Missing procdef deployment")
        dep=deps[proc["deploymentId"]]
        require(dep["tenant"]==proc["tenant"] and dep["key"]==proc["key"],"Procdef deployment tenant/key mismatch")
        linked=[b for b in bodies.values() if b["deploymentId"]==proc["deploymentId"] and b["name"]==proc["resource"]]
        require(len(linked)==1,"Missing/duplicate procdef resource body")
        local=[i for i in infos if i["procdefId"]==proc["id"] and i["tenant"]==proc["tenant"] and i["deleted"]==0]
        require(len(local)==1,"Missing/duplicate same-tenant procdef info")
        if proc["key"] in NATIVE:
            model=models.get(local[0]["modelId"])
            require(model is not None,"Native model missing")
            require((model["tenant"],model["key"],model["version"],model["deploymentId"])
                    ==(proc["tenant"],proc["key"],proc["version"],proc["deploymentId"]),"Native model tenant/key/version/deployment mismatch")
            require(model["editorSourceId"]==linked[0]["id"],"Native editor/definition XML linkage mismatch")
            require(proc["suspension"]==(1 if proc["version"]==3 else 2),"Native historical/current activation mismatch")
        # Legacy info historically shares a tenant1 model across tenants/versions. It is pinned above,
        # not used as a grant or represented as a same-tenant native chain.

    for body in bodies.values():
        xml_hex=body.get("xmlHex")
        if body["id"].startswith("dcc-controlled-file-"):
            require(isinstance(xml_hex,str) and re.fullmatch(r"[0-9A-F]+",xml_hex) is not None,"Missing/invalid native XML bytes")
            try: payload=bytes.fromhex(xml_hex)
            except ValueError:fail("Invalid XML hex width")
            require(len(payload)==body["octets"] and hashlib.sha256(payload).hexdigest()==body["sha256"],"Conflicting XML bytes/hash")
            try: root=ET.fromstring(payload)
            except ET.ParseError:fail("Malformed native BPMN XML")
            process=root.find("b:process",NS)
            require(process is not None,"Missing native BPMN process")
            attached=[p for p in procdefs.values() if p["deploymentId"]==body["deploymentId"] and p["resource"]==body["name"]]
            require(len(attached)==1 and process.get("id")==attached[0]["key"],"XML process key mismatch")
            if attached[0]["version"]==3:
                for node,strategy in [("MATRIX_REVIEW","35"),("MATRIX_APPROVAL","34"),("DOC_CONTROL_REVIEW","34")]:
                    tasks=[x for x in process.findall("b:userTask",NS) if x.get("id")==node]
                    require(len(tasks)==1,"Missing/duplicate native V3 task: "+node)
                    fact=tasks[0].find("b:extensionElements/f:candidateStrategy",NS)
                    require(fact is not None and fact.text==strategy,"Wrong native V3 candidate strategy: "+node)
                    if node=="MATRIX_REVIEW":
                        mi=tasks[0].find("b:multiInstanceLoopCharacteristics",NS)
                        require(mi is not None and mi.get("isSequential")=="false","Missing parallel V3 obligation flow")
                        completion=mi.find("b:completionCondition",NS)
                        require(completion is not None and completion.text=="${nrOfCompletedInstances == nrOfInstances}","V3 obligation completion mismatch")

    for tenant in contract["tenants"]:
        for key in NATIVE:
            rows=[x for x in procdefs.values() if x["tenant"]==tenant and x["key"]==key]
            require(sorted(x["version"] for x in rows)==[1,2,3],"Native version chain missing/replaced")
        legacy=[x for x in procdefs.values() if x["tenant"]==tenant and x["key"]=="dcc-controlled-file-approval"]
        require(legacy and len([x for x in legacy if x["suspension"]==1])==1,"Legacy active upload definition ambiguous")
        require(max(legacy,key=lambda p:p["version"])["suspension"]==1,"Legacy latest upload version is not active")
        policies=[x for x in sections["policies"] if x["tenant"]==tenant and x["deleted"]==0]
        for action,state,mode,key,executor in [("UPLOAD","DRAFT","BPM_REQUIRED","dcc-controlled-file-approval","DCC_UPLOAD"),("PUBLISH","READY_TO_PUBLISH","DIRECT","dcc-controlled-file-approval","DCC_PUBLISH"),("OBSOLETE","ACTIVE","BPM_REQUIRED","dcc-controlled-file-obsolete","DCC_OBSOLETE")]:
            matching=[x for x in policies if x["action"]==action and x["state"]==state and x["status"]=="PUBLISHED"]
            require(len(matching)==1,"Missing/duplicate published policy")
            p=matching[0];require((p["mode"],p["processKey"],p["executor"],p["formPolicyType"],p["formSlots"])
                 ==(mode,key,executor,"NONE","[]"),"Published policy contract mismatch")
        historic=[x for x in policies if x["action"]=="PUBLISH" and x["state"]=="READY_TO_PUBLISH" and x["mode"]=="BPM_REQUIRED"]
        require(len(historic)==1 and historic[0]["status"]=="DISABLED","Historical publish policy is not explicitly superseded by P4")

def validate_schema(contract, sections):
    columns=sections.get("columns");indexes=sections.get("indexes");tables=sections.get("tables")
    require(isinstance(columns,list) and isinstance(indexes,list) and isinstance(tables,list),"Missing schema evidence")
    expected_tables={x["table"] for x in contract["schemaColumns"]}
    require({x["table"] for x in tables}==expected_tables and len(tables)==len(expected_tables),"Required schema table identity/count mismatch")
    require(all(x["engine"]=="InnoDB" and x["collation"]=="utf8mb4_unicode_ci" for x in tables),"Schema engine/collation mismatch")
    keys=[(x["table"],x["column"]) for x in columns];require(len(keys)==len(set(keys)),"Duplicate schema column")
    by=dict(zip(keys,columns))
    for expected in contract["schemaColumns"]:
        name=(expected["table"],expected["column"]);require(name in by,"Missing column "+str(name));actual=by[name]
        require(actual["type"].lower()==expected["type"] and actual["nullable"]==expected["nullable"],"Column type/nullability mismatch "+str(name))
        require(normalize_default(actual["default"])==normalize_default(expected["default"]),"Column default mismatch "+str(name))
        require(actual.get("collation")==expected.get("collation"),"Column collation mismatch "+str(name))
        require(generation_tokens(actual.get("generationExpression"))==generation_tokens(expected.get("generationExpression")),"Generated identity expression mismatch "+str(name))
        require(actual.get("generatedKind")==expected.get("generatedKind"),"Generated storage-kind mismatch "+str(name))
    index_keys=[(x["table"],x["index"],x["sequence"]) for x in indexes];require(len(index_keys)==len(set(index_keys)),"Duplicate schema index position")
    for expected in contract["schemaIndexes"]:
        rows=sorted([x for x in indexes if x["table"]==expected["table"] and x["index"]==expected["index"]],key=lambda x:x["sequence"])
        require([x["column"] for x in rows]==expected["columns"] and [x["sequence"] for x in rows]==list(range(1,len(expected["columns"])+1)),"Unique/index identity order mismatch")
        require(all(x["nonUnique"]==expected["nonUnique"] and x.get("subPart") is None and x["indexType"]=="BTREE" for x in rows),"Index uniqueness/prefix/type mismatch")

def validate(contract, evidence):
    require(evidence.get("contractVersion")==contract["contractVersion"],"Contract version mismatch")
    capture=evidence.get("capture")
    require(isinstance(capture,dict),"Missing capture envelope")
    require(capture.get("database")==contract["database"] and capture.get("mysqlVersion")==contract["mysqlVersion"],"Wrong database/MySQL version")
    require(isinstance(capture.get("serverUuid"),str) and capture["serverUuid"] and capture.get("captureId"),"Missing server/capture identity")
    require(evidence.get("offlineOnly") or capture.get("serverUuid")==contract["expectedServerUuid"],"Wrong frozen server identity")
    require(capture.get("queryId")=="g21-bpm-policy-facts-select-v1","Wrong query identity")
    require(capture.get("capturedAt") and capture.get("endCapturedAt"),"Missing capture timestamps")
    require(capture.get("endServerUuid")==capture["serverUuid"] and capture.get("endDatabase")==capture["database"] and capture.get("endCaptureId")==capture["captureId"],"Mixed/truncated capture envelope")
    sections=evidence.get("sections");require(isinstance(sections,dict),"Missing section object")
    counts=evidence.get("counts");require(isinstance(counts,dict) and set(counts)==set(sections),"Missing/unexpected section count seals")
    require(all(isinstance(rows,list) and counts[name]==len(rows) for name,rows in sections.items()),"Truncated/count-mismatched capture")
    require(set(sections)==set(contract["sections"])|{"columns","indexes","tables"},"Unexpected/missing capture section")
    validate_sections(contract,sections);validate_schema(contract,sections)
    offline=bool(evidence.get("offlineOnly"))
    status="offline_fixture_acceptance_not_fresh_target_proof" if offline else "satisfied_by_existing_target_facts"
    require(offline or not capture["serverUuid"].startswith("OFFLINE"),"Offline fixture cannot be fresh target proof")
    return {"status":status,"offlineOnly":offline,"migrationExecutionProven":False,"ledgerWrites":False,"migrationMustNotExecute":True,"contractVersion":contract["contractVersion"],"queryFile":contract["queryFile"],"querySha256":contract["querySha256"],"database":capture["database"],"serverUuid":capture["serverUuid"],"captureId":capture["captureId"],"capturedAt":capture["capturedAt"],"endCapturedAt":capture["endCapturedAt"],"snapshotHashes":contract["baselineEvidence"],"prerequisites":[{"migrationId":mid,"status":status,"migrationMustNotExecute":True,"historicalSqlExecutionProven":False,"checks":[{"checkId":name,"status":"PASS"} for name in contract["checksByMigration"][mid]]} for mid in contract["migrationIds"]]}

def parse_jsonl(path):
    result={"contractVersion":None,"capture":{},"sections":{},"counts":{}}
    seen_begin=False;seen_end=False
    for lineno,line in enumerate(Path(path).read_text(encoding="utf-8-sig").splitlines(),1):
        if not line.strip(): continue
        try: row=json.loads(line)
        except json.JSONDecodeError:fail(f"Non-JSON/truncated evidence line {lineno}")
        require(isinstance(row,dict),"Evidence row must be object")
        section=row.pop("section",None)
        if section=="capture_begin":
            require(not seen_begin and not seen_end,"Duplicate/misordered capture begin");seen_begin=True
            result["contractVersion"]=row.pop("contractVersion",None);result["capture"].update(row)
        elif section=="capture_end":
            require(seen_begin and not seen_end,"Missing/duplicate capture end");seen_end=True
            result["capture"].update({"endDatabase":row.get("database"),"endServerUuid":row.get("serverUuid"),"endCaptureId":row.get("captureId"),"endCapturedAt":row.get("capturedAt")})
        else:
            require(seen_begin and not seen_end,"Section outside capture envelope")
            if section=="count":
                name=row.get("name");require(name not in result["counts"],"Duplicate count seal");result["counts"][name]=row.get("count");result["sections"].setdefault(name,[])
            else:
                require(isinstance(section,str) and section,"Missing evidence section");result["sections"].setdefault(section,[]).append(row)
    require(seen_begin and seen_end,"Missing capture begin/end")
    return result

def main():
    parser=argparse.ArgumentParser();parser.add_argument("--contract",default=str(TASK/"g21-bpm-policy-contract.json"));parser.add_argument("--evidence",required=True);parser.add_argument("--capture-receipt");parser.add_argument("--output");args=parser.parse_args()
    try:
        contract=json.loads(Path(args.contract).read_text(encoding="utf-8-sig"))
        query=Path(args.contract).parent/contract["queryFile"]
        require(query.is_file() and hashlib.sha256(query.read_bytes()).hexdigest()==contract["querySha256"],"Readonly query fingerprint mismatch")
        receipt_path=Path(args.capture_receipt) if args.capture_receipt else Path(str(args.evidence)+".receipt.json")
        require(receipt_path.is_file(),"Missing Root readonly capture receipt")
        receipt=json.loads(receipt_path.read_text(encoding="utf-8-sig"))
        require(receipt.get("databaseWrites") is False and receipt.get("status")=="read_only_facts_collected_not_execution","Receipt is not readonly evidence")
        require(receipt.get("sqlSha256")==contract["querySha256"] and receipt.get("factsSha256")==hashlib.sha256(Path(args.evidence).read_bytes()).hexdigest(),"Captured query/evidence fingerprint mismatch")
        require(receipt.get("selectCount")==22,"Wrong SELECT capture count")
        result=validate(contract,parse_jsonl(args.evidence))
        identity=receipt.get("capture",{})
        require(identity.get("database")==result["database"] and identity.get("serverUuid")==result["serverUuid"] and identity.get("version")==contract["mysqlVersion"],"Capture receipt/server mismatch")
        result["captureReceiptSha256"]=hashlib.sha256(receipt_path.read_bytes()).hexdigest()
        result["contractSha256"]=hashlib.sha256(Path(args.contract).read_bytes()).hexdigest();result["evidenceSha256"]=hashlib.sha256(Path(args.evidence).read_bytes()).hexdigest()
    except (ContractError,KeyError,TypeError,ValueError,OSError) as exc:
        result={"status":"rejected","reason":str(exc),"migrationExecutionProven":False,"ledgerWrites":False}
        if args.output:Path(args.output).write_text(json.dumps(result,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
        print(json.dumps(result,ensure_ascii=False));raise SystemExit(1)
    if args.output:Path(args.output).write_text(json.dumps(result,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(json.dumps(result,ensure_ascii=False,indent=2))

if __name__=="__main__":main()
