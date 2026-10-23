"""Prepare and validate fixed local-test source upgrade; no runtime write without real prerequisites.
Never creates/restores/deletes databases or controls services. Original nineteen SQL is unchanged.
Technical CLI flag is not proof of user permission; Root records actual authorization separately.
"""
from __future__ import annotations
import argparse, datetime as dt, gzip, hashlib, importlib.util, json, re, subprocess, sys
from pathlib import Path

HERE=Path(__file__).resolve().parent
INTEGRATION=HERE.parents[2]
BACKUPS=Path("C:/IntRuoyiBackups/20261003-dcc-integration")
SOURCE="ruoyi-vue-pro"
SERVER="92ca05d0-aec8-11f1-a944-02b4e226a5ef"
FROZEN_G21_SHA="86e9e8b5504f926dfd1d7ad529afe006393289ec811378232ded997c097e99a9"

class SourceUpgradeError(ValueError):pass

def require(condition,reason):
    if not condition:raise SourceUpgradeError(reason)

def read_json(path):
    value=json.loads(Path(path).read_text(encoding="utf-8-sig"));require(isinstance(value,dict),"Expected JSON object")
    return value

def sha(path):
    with Path(path).open("rb") as stream:return hashlib.file_digest(stream,"sha256").hexdigest()

def now_utc():return dt.datetime.now(dt.timezone.utc)

def timestamp(value):
    require(isinstance(value,str) and value,"Missing exact evidence UTC timestamp")
    parsed=dt.datetime.fromisoformat(value.replace("Z","+00:00"))
    if parsed.tzinfo is None:parsed=parsed.replace(tzinfo=dt.timezone.utc)
    return parsed.astimezone(dt.timezone.utc)

def fresh(value,now,age=900):
    require(age==900,"Source technical maxAgeSeconds must be explicit900")
    seconds=(now-timestamp(value)).total_seconds()
    require(0<=seconds<=age,"Source evidence stale or from a future clock")

def g21_module():
    path=HERE/"g21-rehearsal-driver.py";require(sha(path)==FROZEN_G21_SHA,"Frozen G21 implementation drift")
    spec=importlib.util.spec_from_file_location("g23_frozen_g21",path);module=importlib.util.module_from_spec(spec);sys.modules[spec.name]=module;spec.loader.exec_module(module);return module

def protected_descriptor(row,*,base=BACKUPS):
    require(isinstance(row,dict) and row.get("path") and re.fullmatch(r"[0-9a-f]{64}",row.get("sha256","")),"Required hashed protected input descriptor missing")
    path=Path(row["path"]).resolve(strict=True);require(path.is_relative_to(base.resolve()) and path!=base.resolve(),"Protected input path escaped approved backup directory")
    require(sha(path)==row["sha256"],"Protected input SHA drift")
    return path

def integration_descriptor(row):
    require(isinstance(row,dict) and row.get("path"),"Integration descriptor missing")
    path=(INTEGRATION/row["path"]).resolve(strict=True);require(path.is_relative_to(INTEGRATION.resolve()),"Integration input escaped workspace")
    require(sha(path)==row.get("sha256"),"Integration input SHA drift")
    return path

def artifact_map(directory,rows):
    require(isinstance(rows,list) and rows,"Complete protected artifact manifest required")
    mapping={}
    for row in rows:
        relative=Path(row.get("relativePath",""));require(relative.parts and not relative.is_absolute() and '..' not in relative.parts,"Clone step path traversal rejected")
        path=(directory/relative).resolve(strict=True);require(path.is_relative_to(directory.resolve()) and path.is_file(),"Clone step path escaped protected run")
        key=relative.as_posix();require(key not in mapping and sha(path)==row.get("sha256"),"Clone step artifact missing/duplicate/SHA drift")
        mapping[key]=path
    return mapping

def verify_clone_pass(descriptor,prepared,g21):
    path=protected_descriptor(descriptor);receipt=read_json(path);directory=path.parent
    require(receipt.get("status")=="ISOLATED_REHEARSAL_PASS" and receipt.get("databaseWritesExecuted") is True
            and receipt.get("sourceDatabase")==SOURCE and receipt.get("rehearsalDatabase")=="dcc_intqms_g18_rehearsal",
            "Real successful isolated rehearsal journal is required")
    require(descriptor.get("actualRuntimeReceipt") is True and descriptor.get("testFixture") is False
            and descriptor.get("driverSha256")==FROZEN_G21_SHA,"Offline/synthetic or unsealed clone journal is not actual PASS")
    files=artifact_map(directory,descriptor.get("artifacts"))
    required={"baseline-original-rows.json","baseline-v3-complete-copied-business.json","frozen-environment.jsonl"}
    for phase in ["first","repeat"]:
        required|={phase+suffix for suffix in ["-schema.jsonl","-original-rows.json","-history-proof.json","-seed33-proof.json","-ledger19-proof.json","-new17-empty.json","-schema-result.json","-nineteen.stdout.txt","-nineteen.stderr.txt"]}
    required|={"create-fixed-clone.stdout.txt","create-fixed-clone.stderr.txt","restore-schema.stdout.txt","restore-schema.stderr.txt","restore-data.stdout.txt","restore-data.stderr.txt"}
    require(required<=set(files),"Clone PASS lacks complete first/repeat/history/schema/config/ledger receipts")
    all_actual={x.relative_to(directory).as_posix() for x in directory.rglob('*') if x.is_file() and x!=path}
    require(all_actual==set(files),"Clone artifact manifest must seal every protected run file")
    for stem,field in [('restore-schema','schemaDump'),('restore-data','dataDump')]:
        steps=[x for x in receipt.get('steps',[]) if x.get('stdout') and Path(x['stdout']).name==stem+'.stdout.txt']
        require(len(steps)==1 and steps[0].get('database')=='dcc_intqms_g18_rehearsal' and steps[0].get('exitCode')==0 and steps[0].get('dumpSha256')==sha(prepared[field]),'Clone restore was not exact verified backup')
        require(Path(steps[0]['stdout']).resolve()==files[stem+'.stdout.txt'] and Path(steps[0]['stderr']).resolve()==files[stem+'.stderr.txt'] and files[stem+'.stderr.txt'].stat().st_size==0,'Clone restore protected receipt differs/contains errors')
    require('restored-tables.facts.txt' in files,'Clone restored schema raw table capture missing')
    restored_names=files['restored-tables.facts.txt'].read_text(encoding='utf-8-sig').splitlines()
    require(len(restored_names)==922 and set(restored_names)==set(prepared['schemaTableNames']),'Clone raw restored table scope differs')
    baseline=read_json(files["baseline-original-rows.json"]);copy_baseline=read_json(files["baseline-v3-complete-copied-business.json"])
    expected=g21.expected_seed_facts(prepared,copy_baseline)
    modules=g21.load_support(Path(prepared["mainTask"]),read_json(prepared["contractPath"]));support=modules["g21_mysql_support"]
    original=prepared["originalTables"]+["infra_release_migration"];require(set(baseline)==set(original),"Clone historical proof table scope differs")
    snapshots={phase:read_json(files[phase+"-original-rows.json"]) for phase in ["first","repeat"]}
    require(snapshots["first"]==snapshots["repeat"],"Clone repeat row/ledger snapshots differ")
    phases=[x for x in receipt.get("steps",[]) if x.get("phase") in ["first","repeat"]]
    require(len(phases)==2 and {x["phase"] for x in phases}=={"first","repeat"},"Clone first/repeat step summary missing")
    frozen=g21.json_rows(files["frozen-environment.jsonl"].read_text(encoding="utf-8-sig"));require(len(frozen)==1 and frozen[0].get("database")=="dcc_intqms_g18_rehearsal" and frozen[0].get("serverUuid")==SERVER,"Clone environment identity differs")
    material_hashes={phase:sha(prepared["scripts"][phase]) for phase in ["first","repeat"]}
    prefix=Path(prepared["postflight"]["environmentQuery"]).read_bytes()+b"\n"
    schema=[]
    for phase in ["first","repeat"]:
        step=next(x for x in phases if x["phase"]==phase)
        require(step.get("oldRowsUnchanged") is True and step.get("exactConfigAdditionCount")==33 and step.get("exactLedgerAdditionCount")==19 and step.get("newTablesEmpty")==17,"Clone stage summary incomplete")
        runs=[x for x in receipt["steps"] if x.get("stdout") and Path(x['stdout']).name==phase+"-nineteen.stdout.txt"]
        require(len(runs)==1 and runs[0].get("database")=="dcc_intqms_g18_rehearsal" and runs[0].get("exitCode")==0,"Clone actual SQL run missing/failed")
        run=runs[0]
        require(Path(run['stdout']).resolve()==files[phase+'-nineteen.stdout.txt'].resolve() and Path(run['stderr']).resolve()==files[phase+'-nineteen.stderr.txt'].resolve(),'Clone run stdout/stderr reference escaped sealed artifacts')
        require(run.get("materialSqlSha256")==material_hashes[phase] and run.get("sessionEnvelopeQuerySha256")==hashlib.sha256(prefix[:-1]).hexdigest()
                and run.get("sqlSha256")==hashlib.sha256(prefix+Path(prepared["scripts"][phase]).read_bytes()).hexdigest(),"Clone SQL byte scope differs")
        g21.check_markers(files[phase+"-nineteen.stdout.txt"],prepared['entries']);g21.verify_session_environment(files[phase+"-nineteen.stdout.txt"],frozen[0])
        require(files[phase+"-nineteen.stderr.txt"].stat().st_size==0,"Clone SQL stderr requires explicit review")
        allowed=dict(prepared['permittedAdditions']);allowed['infra_release_migration']=19
        history=support.compare_original_rows(baseline,snapshots[phase],allowed)
        require(history==read_json(files[phase+"-history-proof.json"]),"Clone history proof inconsistent")
        actual={}
        for table in expected:
            name=phase+"-seed33-"+table+".facts.txt";require(name in files,"Missing exact clone config raw facts")
            actual[table]=g21.json_rows(files[name].read_text(encoding='utf-8-sig'))
        require(g21.validate_seed_rows(expected,actual,baseline,snapshots[phase])==read_json(files[phase+"-seed33-proof.json"]),"Clone seed33 proof differs")
        empty=read_json(files[phase+"-new17-empty.json"]);require(set(empty)==set(prepared['newTables']) and not any(empty.values()),"Clone new structural tables not empty")
        for table in prepared['newTables']:
            raw_name=phase+'-new-table-count-'+table+'.facts.txt';require(raw_name in files and files[raw_name].read_text(encoding='utf-8-sig').strip()=='0','Clone empty-table summary lacks exact zero raw evidence')
        table_raw=phase+'-all-table-identities.facts.txt';require(table_raw in files,'Clone final table raw capture missing')
        table_names=files[table_raw].read_text(encoding='utf-8-sig').splitlines()
        require(len(table_names)==939 and set(table_names)==set(prepared['schemaTableNames'])|set(prepared['newTables']),'Clone final raw table identity set is not exact922+17')
        ledger=read_json(files[phase+"-ledger19-proof.json"]);require(ledger.get('oldLedgerUnchanged') is True and ledger.get('newCount')==19,"Clone ledger preservation proof missing")
        rows=ledger.get('exactNewRows',[])
        ledger_raw=phase+'-ledger19.facts.txt';require(ledger_raw in files and g21.json_rows(files[ledger_raw].read_text(encoding='utf-8-sig'))==rows,'Clone ledger summary differs from actual raw19 facts')
        require(len(rows)==19 and {x['migrationId'] for x in rows}=={x['migrationId'] for x in prepared['entries']},"Clone ledger identity differs")
        for entry in prepared['entries']:
            row=next(x for x in rows if x['migrationId']==entry['migrationId'])
            require((row['sha256'],row['fileName'],row['operationId'],row['releaseTag'],row['environment'],row['status'],row['tenant'],row['deleted'])==(entry['sha256'],entry['file'],prepared['operationId'],prepared['operationId'],'test','APPLIED','0',0),"Clone ledger SQL identity mismatch")
        additions=set(snapshots[phase]['infra_release_migration']['rows'])-set(baseline['infra_release_migration']['rows'])
        require({x['id'].encode().hex().upper() for x in rows}==additions,'Clone ledger nineteen keys differ from actual appended primary identities')
        proof=read_json(files[phase+'-schema-result.json']);require(proof.get('status')=='POSTFLIGHT_SCHEMA_PASS_NOT_EXECUTION_EVIDENCE' and proof.get('database')=='dcc_intqms_g18_rehearsal' and proof.get('serverUuid')==SERVER and not proof.get('errors'),"Clone schema postflight incomplete")
        schema.append(proof)
    require(all(schema[0].get(x)==schema[1].get(x) for x in ['schemaFingerprint','contractFingerprint','environmentFingerprint']),"Clone repeated schema differs")
    validator_path=Path(prepared['postflight']['validator']);spec=importlib.util.spec_from_file_location('g23_clone_schema_offline',validator_path)
    validator=importlib.util.module_from_spec(spec);spec.loader.exec_module(validator)
    contract=read_json(prepared['postflight']['contract']);environment=validator.read_environment(files['frozen-environment.jsonl'])
    previous=None
    for phase in ['first','repeat']:
        computed=validator.validate(contract,validator.read_facts(files[phase+'-schema.jsonl']),environment,previous)
        require(computed==read_json(files[phase+'-schema-result.json']) and computed.get('status')=='POSTFLIGHT_SCHEMA_PASS_NOT_EXECUTION_EVIDENCE','Actual clone schema facts do not reproduce successful validator receipt')
        previous=computed
    return {'journalSha256':sha(path),'artifacts':len(files),'status':'REAL_CLONE_FIRST_REPEAT_VERIFIED'}

def verify_new_backup(descriptor,request,g21,now):
    path=protected_descriptor(descriptor);receipt=read_json(path)
    require(receipt.get('database')==SOURCE and receipt.get('databaseWrites') is False and receipt.get('sourceContainer')=='int-ruoyi-mysql' and receipt.get('serverUuid')==SERVER,
            'New real source backup identity differs')
    fresh(receipt.get('completedAtUtc'),now,request['maxAgeSeconds'])
    backup_start=timestamp(receipt.get('startedAtUtc'));backup_end=timestamp(receipt['completedAtUtc'])
    require(backup_start<=backup_end,'New source backup time order invalid')
    capture=receipt.get('sourceCapture',{})
    require(capture.get('database')==SOURCE and capture.get('serverUuid')==SERVER and capture.get('mysqlVersion')=='8.0.40' and capture.get('captureId'),'New backup requires actual source connection capture')
    directory=Path(receipt.get('backupDirectory','')).resolve(strict=True)
    require(directory.parent==BACKUPS.resolve() and directory!=BACKUPS.resolve(),'Source backup must have a new independent protected subdirectory')
    old=read_json(Path(g21.MAIN_TASK)/'g18-backup-receipt.json');old_paths={Path(x['path']).resolve() for x in old['artifacts']}
    require(all(Path(x.get('path','')).resolve() not in old_paths for x in receipt.get('artifacts',[])),'G18/clone backup cannot be reused as new current source backup')
    require(receipt.get('sourceSnapshotSha256')==request['sourceBaseline']['sha256'] and receipt.get('writerExclusionReceiptSha256')==request['writerExclusionReceipt']['sha256'], 'New backup must seal actual source snapshot and writer exclusion epoch')
    modules=g21.load_support(g21.MAIN_TASK,read_json(HERE/'g21-rehearsal-contract.json'))
    verified=modules['g21_migration_scope'].verify_backups(receipt,directory)
    for row in receipt['artifacts']:
        command=row.get('dumpCommandReceipt',{});command_path=protected_descriptor(command)
        observed=read_json(command_path)
        require(command_path.parent==directory and observed.get('kind')==row['kind'] and observed.get('database')==SOURCE and observed.get('serverUuid')==SERVER and observed.get('sourceContainer')=='int-ruoyi-mysql' and observed.get('captureId')==capture['captureId'] and observed.get('exitCode')==0,'Actual new source dump command receipt missing/mismatched')
        require(observed.get('sha256')==row['sha256'] and observed.get('path')==row['path'] and backup_start<=timestamp(observed.get('startedAtUtc'))<=timestamp(observed.get('completedAtUtc'))<=backup_end,'Dump command evidence does not bind fresh artifact/epoch')
        require(observed.get('argvProfile')=='mysqldump-readonly-fixed-local-no-routines-events-no-credentials-export','Backup command scope is not the fixed readonly source collector')
    affected_expected=set(request['originalTables'])|{'infra_release_migration'}
    require('infra_release_migration' in receipt.get('affectedTables',[]) and set(receipt['affectedTables'])==affected_expected,'Source affected backup must include exact original16 plus ledger')
    actual={}
    for row in receipt['artifacts']:
        tables=[]
        with gzip.open(row['path'],'rt',encoding='utf-8') as stream:
            for line in stream:
                pattern=r"^-- Dumping data for table `([^`]+)`" if row['kind']=='data' else r"^CREATE TABLE `([^`]+)`"
                found=re.match(pattern,line)
                if found:tables.append(found[1])
        require(len(tables)==len(set(tables)),'New backup contains duplicate table section')
        actual[row['kind']]=set(tables)
    data_scope=read_json(Path(g21.MAIN_TASK)/'g18-backup-scope.json')['dataBackupTables']
    expected_schema=set(read_json(integration_descriptor(request['rehearsalPrepared']))['schemaTableNames'])
    require(actual['affected']==affected_expected and actual['data']==set(data_scope) and actual['schema']==expected_schema,'Actual source backup payload table scope differs from exact922schema/210data/17affected receipts')
    return verified

def verify_source_prerequisites(descriptor,g21,now,max_age):
    path=protected_descriptor(descriptor);receipt=read_json(path)
    require(receipt.get('database')==SOURCE and receipt.get('serverUuid')==SERVER and receipt.get('mysqlVersion')=='8.0.40' and receipt.get('sourceContainer')=='int-ruoyi-mysql','Fresh source proof target differs')
    modules=g21.load_support(g21.MAIN_TASK,read_json(HERE/'g21-rehearsal-contract.json'))
    verified=modules['g21_execution_materials'].verify_prerequisite_receipt(path.parent,INTEGRATION)
    require(path.name=='g21-prerequisite-runtime-receipt.json' and verified==receipt,'Fresh source prerequisite aggregation path differs')
    formal=modules['g21_migration_scope'].validate_bundle(read_json(g21.MAIN_TASK/'g18-approved-scope-candidate.json'),read_json(g21.MAIN_TASK/'g18-migration-package.json'),INTEGRATION)
    require(set(verified['prerequisiteIds'])==set(formal['externalPrerequisiteIds']),'Fresh source prerequisite identities differ from exact formal25 closure')
    for group in verified['groups']:
        fresh(group['collectedAtUtc'],now,max_age)
        capture_path=path.parent/group['captureReceipt'];require(sha(capture_path)==group.get('captureReceiptSha256'),'Fresh capture receipt fingerprint must be sealed')
        capture=read_json(capture_path);require(timestamp(group['collectedAtUtc'])==timestamp(capture['capture']['collectedAtUtc']),'Proof group freshness differs from actual capture timestamp')
        validator_path=INTEGRATION/group['validator'];spec=importlib.util.spec_from_file_location('g23_source_'+group['kind'],validator_path);validator=importlib.util.module_from_spec(spec);spec.loader.exec_module(validator)
        contract=read_json(INTEGRATION/group['contract']);stored=read_json(path.parent/group['proof'])
        if group['kind']=='bpm':
            computed=validator.validate(contract,validator.parse_jsonl(path.parent/group['facts']))
            require(all(stored.get(k)==v for k,v in computed.items()),'Fresh BPM actual facts do not reproduce sealed proof')
        else:
            computed=validator.validate_facts(contract,validator.read_facts(path.parent/group['facts']))
            require(computed==stored,'Fresh structural actual facts do not reproduce sealed proof')
        require(computed.get('status') in ['satisfied_by_existing_target_facts','SATISFIED_READ_ONLY_FACTS_NOT_APPLIED'],'Fresh actual prerequisite validation failed')
    return verified

def verify_source_snapshot(descriptor,request,now):
    path=protected_descriptor(descriptor);envelope=read_json(path)
    require(envelope.get('database')==SOURCE and envelope.get('serverUuid')==SERVER and envelope.get('origin')=='ACTUAL_SOURCE_READ_NOT_CLONE' and envelope.get('databaseWrites') is False,'Actual source snapshot origin/identity required')
    fresh(envelope.get('collectedAtUtc'),now,request['maxAgeSeconds'])
    snapshot=envelope.get('snapshot');require(isinstance(snapshot,dict) and set(snapshot)==set(request['originalTables'])|{'infra_release_migration'},'Source16+ledger snapshot table scope differs')
    for table,row in snapshot.items():
        require(row.get('columns') and isinstance(row.get('rows'),dict) and row.get('count')==len(row['rows']),'Source snapshot columns/count malformed')
        require(all(re.fullmatch(r'[0-9A-F]+',key) and re.fullmatch(r'[0-9a-f]{64}',value) for key,value in row['rows'].items()),'Source snapshot exact primary/row digest malformed')
        aggregate=hashlib.sha256(json.dumps(row['rows'],sort_keys=True,separators=(',',':')).encode()).hexdigest()
        require(aggregate==row.get('aggregateSha256'),'Source snapshot aggregate mismatch')
    return snapshot

def require_writer_free(rows,request):
    require(len(rows)==1,'One exact source writer/identity row required');row=rows[0]
    require(row.get('database')==SOURCE and row.get('serverUuid')==SERVER and row.get('mysqlVersion')=='8.0.40','Fresh writer target identity mismatch')
    for field in ['activeTransactions','otherClientConnections','enabledEvents']:
        require(type(row.get(field)) is int and row[field]==0,'Source writer/connection/event detected: '+field)
    return row

def writer_query():
    return "SELECT JSON_OBJECT('database',DATABASE(),'serverUuid',@@server_uuid,'mysqlVersion',@@version,'activeTransactions',(SELECT COUNT(*) FROM information_schema.innodb_trx),'otherClientConnections',(SELECT COUNT(*) FROM information_schema.PROCESSLIST WHERE ID<>CONNECTION_ID() AND COMMAND<>'Daemon' AND USER NOT IN ('system user','event_scheduler')),'enabledEvents',(SELECT COUNT(*) FROM information_schema.EVENTS WHERE STATUS='ENABLED'));"

def prepare(request,*,transport_factory=None,now=None):
    # Offline only. A technical flag or status label is never enough to fabricate clone acceptance.
    if request.get('sourceDatabase') is not None:require(request['sourceDatabase']==SOURCE,'Only fixed local-test source database is permitted')
    if not request.get('clonePass'):
        return {'status':'PREPARED_BLOCKED_MISSING_REAL_REHEARSAL_RECEIPT','writeConnectionAllowed':False,'databaseWritesExecuted':False,'missing':['actual clone ISOLATED_REHEARSAL_PASS plus complete hashed protected artifacts'],'actualUserApprovalProvenByFlag':False}
    require(request.get('sourceDatabase')==SOURCE and request.get('sourceContainer')=='int-ruoyi-mysql' and request.get('serverUuid')==SERVER and request.get('mysqlVersion')=='8.0.40','Fixed actual local test target identity required')
    require(request.get('maxAgeSeconds')==900,'Explicit maxAgeSeconds900 required')
    now=now or now_utc();g21=g21_module()
    prepared=read_json(integration_descriptor(request['rehearsalPrepared']));g21.validate_prepared(prepared)
    require(request.get('originalTables')==prepared['originalTables'],'Exact source original16 table scope differs')
    clone=verify_clone_pass(request['clonePass'],prepared,g21)
    missing=[name for name in ['sourceProof','newBackupReceipt','sourceBaseline','sourceEnvironment','writerExclusionReceipt'] if not request.get(name)]
    if missing:return {'status':'PREPARED_BLOCKED_MISSING_FRESH_SOURCE_INPUTS','writeConnectionAllowed':False,'databaseWritesExecuted':False,'cloneProof':clone,'missing':missing,'actualUserApprovalProvenByFlag':False}
    proofs=verify_source_prerequisites(request['sourceProof'],g21,now,900)
    backups=verify_new_backup(request['newBackupReceipt'],request,g21,now)
    baseline=verify_source_snapshot(request['sourceBaseline'],request,now)
    environment_envelope=read_json(protected_descriptor(request['sourceEnvironment']))
    require(environment_envelope.get('database')==SOURCE and environment_envelope.get('serverUuid')==SERVER and environment_envelope.get('databaseWrites') is False,'Source environment identity differs')
    fresh(environment_envelope.get('collectedAtUtc'),now,900)
    environment=environment_envelope['environment'];require(environment.get('kind')=='environment' and environment.get('database')==SOURCE and environment.get('serverUuid')==SERVER,'Source frozen environment incomplete')
    exclusions=read_json(protected_descriptor(request['writerExclusionReceipt']));fresh(exclusions.get('collectedAtUtc'),now,900)
    require(exclusions.get('database')==SOURCE and exclusions.get('serverUuid')==SERVER and exclusions.get('allKnownWritersExcluded') is True,'Root writer exclusion receipt missing')
    require_writer_free([exclusions['mysqlFacts']],request)
    backup=read_json(protected_descriptor(request['newBackupReceipt']));snapshot=read_json(protected_descriptor(request['sourceBaseline']))
    writer_time=timestamp(exclusions['collectedAtUtc']);backup_start=timestamp(backup['startedAtUtc']);backup_end=timestamp(backup['completedAtUtc'])
    proof_times=[timestamp(x['collectedAtUtc']) for x in proofs['groups']]
    require(writer_time<=min(proof_times) and max(proof_times)<=backup_start and writer_time<=timestamp(snapshot['collectedAtUtc'])<=backup_start and backup_start<=backup_end,'Writer/proof/source-baseline/backup epoch ordering invalid')
    require(writer_time<=timestamp(environment_envelope['collectedAtUtc'])<=backup_start,'Source environment is outside frozen writer/backup epoch')
    return {'status':'PREPARED_VERIFIED_SOURCE_UPGRADE_NOT_EXECUTED','sourceDatabase':SOURCE,'serverUuid':SERVER,'maxAgeSeconds':900,'writeConnectionAllowed':False,'databaseWritesExecuted':False,'actualUserApprovalProvenByFlag':False,'request':request,'materials':prepared,'sourceBaseline':baseline,'sourceEnvironment':environment,'cloneProof':clone,'verifiedBackups':backups,'sourcePrerequisiteIds':proofs['prerequisiteIds']}

def source_postflight(mysql,materials,private,environment,g21):
    facts=private/'source-schema.jsonl';facts.write_text(g21.protected_read(mysql,Path(materials['postflight']['query']).read_text(encoding='utf-8-sig'),private,'source-postflight-schema'),encoding='utf-8')
    result=private/'source-schema-result.json';command=[sys.executable,'-B',materials['postflight']['validator'],'validate','--facts',str(facts),'--environment',str(environment),'--result',str(result)]
    with (private/'source-postflight.stdout.txt').open('xb') as out,(private/'source-postflight.stderr.txt').open('xb') as err:done=subprocess.run(command,stdout=out,stderr=err)
    require(done.returncode==0 and result.is_file(),'Source postflight failed; protected evidence retained')
    proof=read_json(result);require(proof.get('status')=='POSTFLIGHT_SCHEMA_PASS_NOT_EXECUTION_EVIDENCE' and proof.get('database')==SOURCE and proof.get('serverUuid')==SERVER and not proof.get('errors'),'Source postflight must explicitly match sourceDB; clone result is not source proof')
    return proof

def upgrade(prepared,*,authorize_local_test_upgrade=False,transport_factory=None,private_directory=None,now=None):
    require(authorize_local_test_upgrade,'Explicit --authorize-local-test-upgrade technical gate required; actual user permission remains Root-owned')
    require(prepared.get('status')=='PREPARED_VERIFIED_SOURCE_UPGRADE_NOT_EXECUTED','Source upgrade is blocked or not prepared')
    fresh_prepared=prepare(prepared['request'],now=now);require(fresh_prepared==prepared,'Source prepared receipt drift')
    require(prepared['request'].get('actualUserWriteApprovalRecordedByRoot') is True and isinstance(prepared['request'].get('rootAuthorizationReference'),str) and prepared['request']['rootAuthorizationReference'].strip(),'Root actual user write approval/reference has not been recorded; flag is not approval')
    private=Path(private_directory or '');require(private_directory and private.resolve().parent==BACKUPS.resolve() and not private.exists(),'New protected source execution receipt directory required')
    g21=g21_module();materials=prepared['materials'];modules=g21.load_support(Path(materials['mainTask']),read_json(materials['contractPath']));support=modules['g21_mysql_support']
    factory=transport_factory or support.LocalMysql;private.mkdir()
    journal={'status':'RUNNING_SOURCE_UPGRADE','database':SOURCE,'actualUserApprovalProvenByFlag':False,'databaseWritesExecuted':False,'steps':[]}
    g21.save_json(private/'source-driver-receipt.json',journal)
    try:
        mysql=factory(SOURCE)
        require_writer_free(g21.json_rows(g21.protected_read(mysql,writer_query(),private,'source-fresh-writer-preflight')),prepared['request'])
        columns={table:x['columns'] for table,x in prepared['sourceBaseline'].items()}
        before=support.snapshot_original_rows(g21.RecordingMysql(mysql,private,'source-before'),list(columns))
        require(before==prepared['sourceBaseline'],'Actual source16+ledger drifted after fresh backup/snapshot')
        g21.save_json(private/'source-before-original-rows.json',before)
        new_rows=g21.protected_read(mysql,"SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ("+','.join("'"+x+"'" for x in materials['newTables'])+");",private,'source-new-table-absence').splitlines()
        require(not new_rows,'Source new target tables already exist; no ambiguous upgrade retry')
        ledger=g21.json_rows(g21.protected_read(mysql,g21.ledger_query(materials),private,'source-candidate-ledger-absence'));require(not ledger,'Source candidate ledger identity already exists')
        actual_env=g21.json_rows(g21.protected_read(mysql,Path(materials['postflight']['environmentQuery']).read_text(encoding='utf-8-sig'),private,'source-fresh-environment'))
        require(actual_env==[prepared['sourceEnvironment']],'Source environment drifted')
        environment=private/'source-frozen-environment.jsonl';environment.write_text(json.dumps(actual_env[0],ensure_ascii=False)+'\n',encoding='utf-8')
        copied=g21.capture_copied_baseline(mysql,materials,private);g21.save_json(private/'source-baseline-v3-complete-copy.json',copied)
        # Reverify disk evidence/freshness and writer state immediately before the single nineteen-item write.
        require(prepare(prepared['request'],now=now)==prepared,'Source inputs/proofs drift before write')
        require_writer_free(g21.json_rows(g21.protected_read(mysql,writer_query(),private,'source-immediate-writer-preflight')),prepared['request'])
        material=Path(materials['scripts']['first']).read_bytes();prefix=Path(materials['postflight']['environmentQuery']).read_bytes()+b'\n'
        journal['databaseWriteAttempted']=True;journal['partialDdlCommitPossible']=True;g21.save_json(private/'source-driver-receipt.json',journal)
        run=mysql.run_authorized_sql(prefix+material,private_directory=private,stem='source-nineteen-first')
        require(run.get('sqlSha256')==hashlib.sha256(prefix+material).hexdigest(),'Source actual write SQL bytes differ')
        run['materialSqlSha256']=hashlib.sha256(material).hexdigest();run['sessionEnvelopeQuerySha256']=hashlib.sha256(prefix[:-1]).hexdigest()
        journal['databaseWritesExecuted']=True;journal['steps'].append(run);g21.save_json(private/'source-driver-receipt.json',journal)
        g21.check_markers(run['stdout'],materials['entries']);g21.verify_session_environment(run['stdout'],actual_env[0])
        current=support.snapshot_original_rows(g21.RecordingMysql(mysql,private,'source-after'),list(columns),columns);g21.save_json(private/'source-after-original-rows.json',current)
        additions=dict(materials['permittedAdditions']);additions['infra_release_migration']=19
        g21.save_json(private/'source-history-proof.json',support.compare_original_rows(before,current,additions))
        seeds=g21.verify_seed_additions(mysql,materials,before,current,private,'source',copied);g21.save_json(private/'source-seed33-proof.json',seeds)
        ledger=g21.verify_ledger_additions(mysql,materials,before,current,private,'source');g21.save_json(private/'source-ledger19-proof.json',ledger)
        empty=g21.verify_new_tables(mysql,materials,private,'source');g21.save_json(private/'source-new17-empty.json',empty)
        schema=source_postflight(mysql,materials,private,environment,g21);g21.save_json(private/'source-schema-proof.json',schema)
        journal['status']='LOCAL_TEST_SOURCE_NINETEEN_UPGRADE_PASS_NOT_APPLICATION_READINESS';journal['exactConfigAdditions']=33;journal['exactLedgerAdditions']=19;journal['newStructuralTablesEmpty']=17
        g21.save_json(private/'source-driver-receipt.json',journal);return journal
    except Exception as exc:
        journal['status']='FAILED_SOURCE_UPGRADE_STOPPED_NO_AUTOMATIC_RECOVERY';journal['errorType']=type(exc).__name__
        (private/'source-driver-error.txt').write_text(str(exc),encoding='utf-8');g21.save_json(private/'source-driver-receipt.json',journal)
        raise SourceUpgradeError('Source upgrade stopped; protected evidence retained. No automatic recovery or service control performed') from exc

def main():
    parser=argparse.ArgumentParser(description=__doc__);commands=parser.add_subparsers(dest='mode',required=True)
    setup=commands.add_parser('prepare');setup.add_argument('--request',required=True);setup.add_argument('--result',required=True)
    check=commands.add_parser('validate');check.add_argument('--prepared',required=True)
    run=commands.add_parser('upgrade');run.add_argument('--prepared',required=True);run.add_argument('--private-directory',required=True);run.add_argument('--authorize-local-test-upgrade',action='store_true')
    args=parser.parse_args()
    try:
        if args.mode=='prepare':result=prepare(read_json(args.request));Path(args.result).write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
        elif args.mode=='validate':old=read_json(args.prepared);result=prepare(old['request']);require(old==result,'Prepared source receipt drift')
        else:result=upgrade(read_json(args.prepared),authorize_local_test_upgrade=args.authorize_local_test_upgrade,private_directory=args.private_directory)
    except (ValueError,OSError,KeyError) as exc:
        print(json.dumps({'status':'REJECTED_NOT_SOURCE_PASS','errorType':type(exc).__name__,'reason':str(exc),'actualUserApprovalProvenByFlag':False}));raise SystemExit(1)
    print(json.dumps({'status':result['status'],'databaseWritesExecuted':result.get('databaseWritesExecuted',False),'actualUserApprovalProvenByFlag':False}))

if __name__=='__main__':main()
