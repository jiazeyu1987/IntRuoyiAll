"""G29 DRIVER R2 derived from frozen G28; only strict generated-column dump validation changed. PLAN is offline; execute needs separate actual new authorization.
No clone provisioning, retry, restore, services, object writes, audit configuration or legacy activation.
"""
import argparse
import gzip
import hashlib
import importlib.util
import json
from pathlib import Path
import re
import sys
import time

HERE=Path(__file__).resolve().parent;REPO=HERE.parents[2]
MAIN=Path('C:/IntRuoyiAll-int_main/doc/tasks/20261001-dcc-integration-unblock')
PROTECTED=Path('C:/IntRuoyiBackups/20261003-dcc-integration')
SOURCE='ruoyi-vue-pro';CLONE='dcc_intqms_g18_rehearsal'
MIGRATION='20261003_dcc_legacy_source_name_occupancy'
SQL_SHA='621fe041064ac07c5bfe995abd556c3f9436b0253c883a3b7d2b33014e68de2a'
SQL_PATH=REPO/'IntRuoyiBackend/sql/mysql'/f'{MIGRATION}.sql'
SUPPORT_SHA='6416a70ce24ea4fdec6abcbeb1f1a7746bd471533fff5c994b3c6ee08faf1331'
ORIGINAL_TABLES=['dcc_controlled_file','dcc_controlled_file_master','dcc_controlled_file_name_claim','dcc_controlled_file_source_ownership','dcc_controlled_file_signature','system_electronic_signature','infra_release_migration']
EXECUTION_RULES={'automaticRetry':False,'automaticRestore':False,'implicitDdlCommitPossible':True,'firstLedgerInserts':1,'repeatLedgerWrites':0,'provisionClone':False}
spec=importlib.util.spec_from_file_location('g28_schema_owned',HERE/'g28-schema.py');schema=importlib.util.module_from_spec(spec);spec.loader.exec_module(schema)
parser_spec=importlib.util.spec_from_file_location('g29_dump_parser',HERE/'g29-dump-parser.py');dump_parser=importlib.util.module_from_spec(parser_spec);parser_spec.loader.exec_module(dump_parser)

def require(condition,message):
    if not condition:raise ValueError(message)
def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()
def read(path):
    obj=json.loads(Path(path).read_text(encoding='utf-8-sig'));require(isinstance(obj,dict),'exact object required');return obj
def desc(path):return {'path':str(Path(path).resolve()),'sha256':sha(path),'bytes':Path(path).stat().st_size}
def verify_descriptor(row):
    require(isinstance(row,dict) and isinstance(row.get('path'),str),'immutable descriptor required');p=Path(row['path']).resolve(strict=True)
    require(p.is_relative_to(PROTECTED.resolve()) or p.is_relative_to(MAIN.resolve()) or p.is_relative_to(HERE.resolve()),'proof descriptor escaped task scope')
    require(p.is_file() and not p.is_symlink() and sha(p)==row.get('sha256') and p.stat().st_size==row.get('bytes'),'proof bytes drift');return p

def prior_proofs():
    root_receipt=MAIN/'g25-runtime-evidence-receipt.json';proof=read(root_receipt)
    require(proof.get('status')=='SOURCE19_UPGRADE_PASS_APPLICATION_GOAL_STILL_IN_PROGRESS' and proof.get('schema19Executed') is True and proof.get('sourceDb')==SOURCE,'actual source19PASS receipt required')
    for row in proof['proofs']:
        p=verify_descriptor(row)
        if p.name=='source-driver-receipt.json':
            result=read(p);require(result.get('status')=='LOCAL_TEST_SOURCE_NINETEEN_UPGRADE_PASS_NOT_APPLICATION_READINESS' and result.get('database')==SOURCE and result.get('exactLedgerAdditions')==19 and result.get('exactConfigAdditions')==33 and all(r.get('exitCode')==0 for r in result.get('steps',[])),'actual source19 execution proof invalid')
            require(len(result.get('steps',[]))==1,'source19 exact one first-run SQL phase required')
            step=result['steps'][0]
            require(sha(PROTECTED/'exec-g21-0610/first.sql')==step['materialSqlSha256'],'source19 exact protected first material drift')
            require(Path(step['stdout']).resolve().is_relative_to(PROTECTED.resolve()) and Path(step['stderr']).resolve().is_relative_to(PROTECTED.resolve()) and Path(step['stdout']).is_file() and Path(step['stderr']).is_file(),'actual source19 protected raw streams required')
            require(Path(step['stderr']).stat().st_size==0,'source19 stderr is not clean')
            text=Path(step['stdout']).read_text(encoding='utf-8');phases=[json.loads(line) for line in text.splitlines() if line.startswith('{')]
            completed={r['migrationId'] for r in phases if r.get('taskPhase')=='sql_complete'}
            require(completed==set(expected_old19()),'actual source19 raw complete marker IDs differ')
        if p.name=='source-history-proof.json':require(all(r.get('oldRowsUnchanged') is True for r in read(p).values()),'source19 old row proof is not immutable')
    ready_path=MAIN/'g27-legacy-schema-readiness-receipt.json';ready=read(ready_path)
    require(ready.get('status')=='READONLY_PREREQUISITE_FACTS_CAPTURED_NOT_MIGRATION_PASS' and ready.get('database')==SOURCE and ready.get('serverUuid')==schema.UUID and ready.get('mysqlVersion')=='8.0.40' and ready.get('innodbPageSize')==16384 and ready.get('innodbDefaultRowFormat')=='dynamic' and ready.get('newTablesPresent')==0 and ready.get('targetLedgerRows')==0 and ready.get('databaseWrites') is False,'actual16K/dynamic/absent preflight evidence differs')
    require(sha(ready['factsPath'])==ready['factsSha256'],'actual readiness facts changed')
    return {'status':'SOURCE19_PASS_NEW_ONE_NOT_AUTHORIZED','source19Receipt':desc(root_receipt),'sourceReadinessReceipt':desc(ready_path),'sourceReadinessFacts':desc(ready['factsPath']),'actualNewMigrationExecuted':False}

def expected_old19():
    rows=read(MAIN/'g18-approved-scope-candidate.json')['executionOrder'];require(len(rows)==19 and len({r['migrationId'] for r in rows})==19,'frozen old19 ID scope invalid')
    return {r['migrationId']:r['sha256'] for r in rows}

def expected_prerequisites():return {r['migrationId'] for r in policy_closure()['migrations'] if r['migrationId']!=MIGRATION}

def validate_backup(backup,baseline,database,writer):
    require(backup.get('status')=='FRESH_G28_ORIGINAL_BACKUP_COMPLETE' and set(backup.get('protectedTables',[]))==set(ORIGINAL_TABLES),'fresh exact backup scope required')
    require(backup.get('baselineAggregateSha256')==hashlib.sha256(json.dumps(baseline['tables'],sort_keys=True,separators=(',',':')).encode()).hexdigest(),'backup captured baseline payload differs')
    require(len(backup.get('artifacts',[]))==2 and {a.get('kind') for a in backup['artifacts']}=={'schema','protected-original-rows'},'fresh schema and complete protected old-row gzip artifacts required')
    artifacts={a['kind']:a for a in backup['artifacts']}
    with gzip.open(verify_descriptor(artifacts['schema']),'rt',encoding='utf-8',newline='') as stream:dump_schema=dump_parser.parse_schema(stream.read(),baseline['tables'])
    for artifact in backup['artifacts']:
        path=verify_descriptor(artifact);command=read(verify_descriptor(artifact.get('dumpCommandReceipt')))
        expected_profile='G28_SCHEMA_SINGLE_TRANSACTION_NO_DATA' if artifact['kind']=='schema' else 'G28_PROTECTED_ROWS_SINGLE_TRANSACTION_SINGLE_ROW_INSERTS'
        require(path.suffix=='.gz' and artifact.get('dumpExitCode')==0 and command.get('profile')==expected_profile and command.get('database')==database
                and command.get('serverUuid')==schema.UUID and command.get('container')=='int-ruoyi-mysql' and command.get('exitCode')==0
                and command.get('databaseWrites') is False and command.get('credentialsPersisted') is False
                and command.get('artifactSha256')==artifact['sha256'] and command.get('artifactBytes')==artifact['bytes']
                and type(command.get('startedEpoch')) in {int,float} and type(command.get('finishedEpoch')) in {int,float}
                and writer['capturedEpoch']<=baseline['capturedEpoch']<=command['startedEpoch']<=command['finishedEpoch']<=backup['capturedEpoch'],'actual sealed readonly dump command provenance/time differs')
        require(set(command.get('tables',[]))==set(ORIGINAL_TABLES),'dump command exact original table scope differs')
        creates=[];markers=[];inserts={t:0 for t in ORIGINAL_TABLES};bytes_read=0;complete=False
        with gzip.open(path,'rt',encoding='utf-8') as stream:
            for line in stream:
                bytes_read+=len(line.encode('utf-8'))
                require(not re.match(r'\s*(USE\s|(?:CREATE|DROP)\s+(?:DATABASE|SCHEMA)\b|SOURCE\s|\\!)',line,re.I),'backup forbidden routing/shell directive')
                match=re.match(r'CREATE TABLE(?: IF NOT EXISTS)? `([^`]+)`',line)
                if match:creates.append(match.group(1))
                match=re.match(r'-- Dumping data for table `([^`]+)`',line)
                if match:markers.append(match.group(1))
                if line.startswith('INSERT INTO '):
                    table=dump_parser.insert_table(line,dump_schema);inserts[table]+=1
                if line.startswith('-- Dump completed on '):complete=True
        require(complete and command.get('uncompressedBytes')==bytes_read,'complete dump end marker/exact expanded bytes required')
        if artifact['kind']=='schema':require(set(creates)==set(ORIGINAL_TABLES) and len(creates)==len(ORIGINAL_TABLES) and not any(inserts.values()),'schema backup exact definitions required')
        else:
            require(not creates,'protected rows backup must not create schema objects')
            with gzip.open(path,'rt',encoding='utf-8',newline='') as stream:require(dump_parser.scan_data(stream,dump_schema)==inserts,'strict row/table markers differ')
            require(set(markers)==set(ORIGINAL_TABLES) and len(markers)==len(ORIGINAL_TABLES),'data backup exact table markers required')
            require(all(inserts[t]==baseline['tables'][t]['count'] for t in ORIGINAL_TABLES),'data backup one-row INSERT count differs from original baseline')
            require(command.get('singleRowInsertCounts')==inserts,'data dump command row counts differ')

def policy_closure():
    backend=REPO/'IntRuoyiBackend';sys.path.insert(0,str(backend))
    from script.release.release_migration_policy_gate import run_migration_policy_gate
    root=backend/'sql/mysql';pending=[MIGRATION];seen=set()
    while pending:
        ident=pending.pop()
        if ident in seen:continue
        require(re.fullmatch('[A-Za-z0-9_]+',ident),'unsafe migration id');p=root/(ident+'.sql');line=p.read_text(encoding='utf-8').splitlines()[0];seen.add(ident)
        match=re.search(r'dependsOn=([^;]*)',line);require(match is not None,'formal dependency header missing')
        pending.extend(x.strip() for x in match.group(1).split(',') if x.strip())
    require(len(seen)==8,'exact reviewed full dependency closure changed')
    return run_migration_policy_gate(root,sql_paths=[root/(i+'.sql') for i in sorted(seen)],file_prefix='sql/mysql')

def plan():
    require(sha(SQL_PATH)==SQL_SHA,'new SQL changed after review')
    policy=policy_closure();require(policy['status']=='passed','fullclosure gate notPASS')
    contract=schema.prepare_contract()
    return {'status':'PREPARED_SINGLE_MIGRATION_NOT_AUTHORIZED_NOT_EXECUTED','executionCount':1,'migrationId':MIGRATION,'sql':desc(SQL_PATH),
            'sourceDatabase':SOURCE,'existingRootSelectedCloneDatabase':CLONE,'serverUuid':schema.UUID,'writeAuthorizationGranted':False,
            'policyClosureCount':8,'policy':policy,'prerequisitesFactOnly':[r for r in policy['migrations'] if r['migrationId']!=MIGRATION],
            'priorActualProofs':prior_proofs(),'schema':contract,'executionRules':EXECUTION_RULES,
            'runtimeRequired':['specific actual user authorization for this 1 migration','fresh exclusion of all source writers','fresh scoped backup including original ledger','fresh actual original table snapshots','fresh seven prerequisite facts and original nineteen ledger exact payloads','Root-selected already-existing owned clone rehearsal PASS before sourceDDL']}

def compose_material(phase,operation,database):
    require(phase in {'first','repeat'} and database in {SOURCE,CLONE},'fixed task scope/phase required')
    require(re.fullmatch('[A-Za-z0-9_-]{1,100}',operation or ''),'exact safe operation id required')
    source=SQL_PATH.read_bytes();require(hashlib.sha256(source).hexdigest()==SQL_SHA,'SQL rawbytes drift')
    envelope=f"SELECT JSON_OBJECT('g28Phase','SESSION_IDENTITY','database',DATABASE(),'serverUuid',@@server_uuid,'mysqlVersion',VERSION(),'pageSize',@@innodb_page_size,'rowFormat',@@innodb_default_row_format,'intendedDatabase','{database}');\n"
    done=f"SELECT JSON_OBJECT('g28Phase','{phase.upper()}_COMPLETE','migrationId','{MIGRATION}');\n"
    ledger=''
    if phase=='first':ledger=f"INSERT INTO infra_release_migration(release_tag,migration_id,file_name,sha256,target_environment,status,started_at,finished_at,operation_id,creator,updater,deleted,tenant_id) VALUES('{operation}','{MIGRATION}','sql/mysql/{MIGRATION}.sql','{SQL_SHA}','test','APPLIED',UTC_TIMESTAMP(),UTC_TIMESTAMP(),'{operation}','dcc-g28-local-task','dcc-g28-local-task',b'0',0);\n"
    # Transport uses immutable --database and fresh identity prewrite; envelope records the actual same session.
    # No synthetic SQL error or extra guard procedure/schema object is created.
    return envelope.encode()+source+b'\n'+ledger.encode()+done.encode()

def validate_request(request):
    require(request.get('version')=='G28-EXEC-1' and request.get('database') in {SOURCE,CLONE},'explicit Root-selected existing task database required')
    require(request.get('specificNewMigrationAuthorized') is True,'separate specific new migration authorization required')
    authorization=read(verify_descriptor(request.get('authorization')))
    require(authorization.get('status')=='SPECIFIC_USER_DDL_AUTHORIZATION_RECEIVED' and authorization.get('userAnswerReceived') is True
            and authorization.get('migrationId')==MIGRATION and authorization.get('migrationSha256')==SQL_SHA
            and authorization.get('allowedDatabases')==[CLONE,SOURCE] and authorization.get('newLedgerMaximum')==1
            and authorization.get('preserveHistoricalRows') is True and authorization.get('allowLegacyActivation') is False
            and authorization.get('allowObjectRecovery') is False and authorization.get('allowAuditPolicyConfiguration') is False
            and isinstance(authorization.get('sourceQuestionId'),str) and authorization.get('sourceQuestionId')
            and isinstance(authorization.get('actualAnswer'),str) and authorization.get('actualAnswer').strip(),'actual new1DDL user receipt required; flag/old19approval insufficient')
    operation=request.get('operationId');compose_material('first',operation,request['database'])
    require(sha(SQL_PATH)==SQL_SHA,'sourceDDL drift')
    proofs={}
    for key in ['freshPreflight','freshOriginalBaseline','writerExclusion','freshBackup','prerequisiteProof']:
        row=request.get(key);path=verify_descriptor(row);value=read(path);proofs[key]=value
        require(value.get('database')==request['database'] and value.get('serverUuid')==schema.UUID and value.get('actualRead') is True,'fresh proof actual database/UUID wrong: '+key)
        require(type(value.get('capturedEpoch')) in {int,float} and 0<=time.time()-value['capturedEpoch']<=900,'fresh proof required, age is technical not permission expiry')
    excluded=proofs['writerExclusion'];require(excluded.get('allApplicationWritersExcluded') is True and excluded.get('transactions')==0 and excluded.get('otherConnections')==0 and excluded.get('enabledEvents')==0,'writers must be excluded; no guess of ownership')
    baseline=proofs['freshOriginalBaseline'];require(baseline.get('source')=='ACTUAL_SOURCE_READ_NOT_CLONE' if request['database']==SOURCE else baseline.get('source')=='ACTUAL_OWNED_CLONE_READ','actual snapshot source kind differs')
    require(set(baseline.get('tables',{}))==set(ORIGINAL_TABLES),'exact protected original business/signature/ledger tables required')
    for table,snapshot in baseline['tables'].items():
        require(isinstance(snapshot.get('columns'),list) and snapshot['columns'] and isinstance(snapshot.get('rows'),dict) and snapshot.get('count')==len(snapshot['rows']),'full original row payload snapshots required: '+table)
        require(all(re.fullmatch('[0-9A-Fa-f]+',key) and re.fullmatch('[0-9a-f]{64}',value) for key,value in snapshot['rows'].items()),'raw exact original row id/digest invalid')
        aggregate=hashlib.sha256(json.dumps(snapshot['rows'],sort_keys=True,separators=(',',':')).encode()).hexdigest();require(snapshot.get('aggregateSha256')==aggregate,'original aggregate differs')
    backup=proofs['freshBackup'];validate_backup(backup,baseline,request['database'],excluded)
    prerequisite=proofs['prerequisiteProof'];require(prerequisite.get('status')=='EXACT_SEVEN_FACTS_AND_OLD19_LEDGER_VERIFIED' and set(prerequisite.get('prerequisiteIds',[]))==expected_prerequisites(),'seven exact prerequisite IDs differ')
    expected=expected_old19();ledger=prerequisite.get('originalNineteenLedger',[])
    require(len(ledger)==19 and {r.get('migrationId') for r in ledger}==set(expected),'old19 ledger exact identities differ')
    for row in ledger:require(row.get('sha256')==expected[row['migrationId']] and row.get('status')=='APPLIED' and row.get('environment')=='test' and row.get('deleted')==0,'old19 ledger exact payload/hash/status differs')
    facts=verify_descriptor(prerequisite.get('post19SchemaFacts'));environment=verify_descriptor(prerequisite.get('post19Environment'));validation=verify_descriptor(prerequisite.get('post19Validation'))
    original_contract=read(HERE/'g21-postflight-schema-contract.json')
    result=schema.base.validate(original_contract,schema.base.read_facts(facts),schema.base.read_environment(environment))
    require(result['status']=='POSTFLIGHT_SCHEMA_PASS_NOT_EXECUTION_EVIDENCE' and read(validation)==result,'existing prerequisite schema proof is not replayable actual contract PASS')
    require(prerequisite.get('schemaQuerySha256')==sha(HERE/'g21-postflight-schema-queries.sql') and prerequisite.get('schemaValidatorSha256')==sha(HERE/'g21-postflight-schema.py')
            and prerequisite.get('schemaContractSha256')==sha(HERE/'g21-postflight-schema-contract.json'),'exact existing prerequisite tools/source scope drift')
    capture=read(verify_descriptor(prerequisite.get('schemaCaptureReceipt')))
    require(capture.get('factsSha256')==sha(facts) and capture.get('sqlSha256')==prerequisite['schemaQuerySha256'] and capture.get('databaseWrites') is False
            and capture.get('capture',{}).get('database')==request['database'] and capture.get('capture',{}).get('serverUuid')==schema.UUID,'actual SELECT capture provenance differs')
    if request['database']==SOURCE:
        clone=read(verify_descriptor(request.get('newMigrationClonePass')));validate_completed_journal(clone,CLONE)
    destination=Path(request.get('privateDirectory','')).resolve();require(destination.parent==PROTECTED.resolve() and not destination.exists(),'execution journal must be a new protected task directory, no automatic retry')
    return proofs,destination

def collect_schema(transport,contract):
    output=transport.read(schema.capture_sql());rows=[json.loads(line) for line in output.splitlines() if line]
    present=[r for r in rows if r.get('kind')=='table']
    if len(present)==3:
        rows.extend(json.loads(line) for line in transport.read(schema.row_counts_sql()).splitlines() if line)
    return rows

def writer_query():
    return "SELECT JSON_OBJECT('kind','writer_exclusion','transactions',(SELECT COUNT(*) FROM information_schema.INNODB_TRX),'otherConnections',(SELECT COUNT(*) FROM information_schema.PROCESSLIST WHERE ID<>CONNECTION_ID() AND USER NOT IN ('event_scheduler','system user')),'enabledEvents',(SELECT COUNT(*) FROM information_schema.EVENTS WHERE EVENT_SCHEMA=DATABASE() AND STATUS='ENABLED'));\n"

def new_ledger_query():
    return f"SELECT JSON_OBJECT('releaseTag',release_tag,'operationId',operation_id,'file',file_name,'migrationId',migration_id,'sha256',sha256,'status',status,'environment',target_environment,'deleted',deleted+0,'tenantId',CAST(tenant_id AS CHAR),'creator',creator,'updater',updater,'startedAt',CAST(started_at AS CHAR),'finishedAt',CAST(finished_at AS CHAR),'errorMessage',error_message) FROM infra_release_migration WHERE migration_id='{MIGRATION}';\n"

def old19_ledger_query():
    keys=','.join("'"+key+"'" for key in expected_old19())
    return f"SELECT JSON_OBJECT('migrationId',migration_id,'sha256',sha256,'status',status,'environment',target_environment,'deleted',deleted+0) FROM infra_release_migration WHERE migration_id IN ({keys}) ORDER BY migration_id;\n"

def validate_execution_result(result,material,database,phase):
    require(result.get('exitCode')==0 and result.get('database')==database and result.get('sqlSha256')==hashlib.sha256(material).hexdigest(),'SQL execution identity/material/exit differs')
    output=verify_descriptor(result.get('stdout'));errors=verify_descriptor(result.get('stderr'))
    require(errors.stat().st_size==0,'MySQL emitted stderr, inspect protected receipt')
    rows=[json.loads(line) for line in output.read_text(encoding='utf-8').splitlines() if line.strip()]
    require(len(rows)==2 and rows[0].get('g28Phase')=='SESSION_IDENTITY' and rows[0].get('database')==database and rows[0].get('intendedDatabase')==database
            and rows[0].get('serverUuid')==schema.UUID and rows[0].get('mysqlVersion')=='8.0.40' and rows[0].get('pageSize')==16384 and rows[0].get('rowFormat')=='dynamic'
            and rows[1]=={'g28Phase':phase.upper()+'_COMPLETE','migrationId':MIGRATION},'actual same-session envelope/completion differs')

def validate_completed_journal(journal,database):
    expected='G28_OWNED_EXISTING_CLONE_FIRST_REPEAT_PASS' if database==CLONE else 'G28_SOURCE_ONE_MIGRATION_FIRST_REPEAT_PASS_NOT_APPLICATION_READINESS'
    require(journal.get('status')==expected and journal.get('database')==database and journal.get('migrationId')==MIGRATION and journal.get('sqlSha256')==SQL_SHA,'real complete new one-migration journal required')
    require(journal.get('databaseWriteAttempted') is True and [s.get('phase') for s in journal.get('steps',[])]==['first','repeat'],'actual first+repeat required')
    before=read(verify_descriptor(journal.get('originalBefore')));require(set(before)==set(ORIGINAL_TABLES),'full actual baseline scope required')
    for table,snapshot in before.items():
        require(isinstance(snapshot.get('columns'),list) and snapshot['columns'] and isinstance(snapshot.get('rows'),dict) and snapshot.get('count')==len(snapshot['rows']),'complete baseline payload required')
        require(snapshot.get('aggregateSha256')==hashlib.sha256(json.dumps(snapshot['rows'],sort_keys=True,separators=(',',':')).encode()).hexdigest(),'baseline original hash differs')
    post=[];history=[]
    for step in journal['steps']:
        require(step.get('phaseStatus')=='POSTFLIGHT_AND_ORIGINAL_ROWS_PASS','phase not fully verified')
        material=verify_descriptor(step.get('material'));require(sha(material)==step.get('materialSha256'),'phase actual material drift')
        require(material.read_bytes()==compose_material(step['phase'],journal.get('operationId'),database),'actual executed phase differs from exact new migration material')
        validate_execution_result(step['execution'],material.read_bytes(),database,step['phase'])
        rows=json.loads(verify_descriptor(step['postflight']).read_text(encoding='utf-8'));schema.validate_facts(schema.prepare_contract(),rows,'post',database=database);post.append(rows)
        snapshot=read(verify_descriptor(step['originalAfter']));require(set(snapshot)==set(ORIGINAL_TABLES),'actual original row scope incomplete')
        compare_snapshots(before,snapshot,{'infra_release_migration':1});history.append(snapshot)
        ledger=read(verify_descriptor(step['newLedger']));validate_new_ledger(ledger,journal['operationId'])
    require(post[0]==post[1] and history[0]==history[1],'clone repeat drifted schema or original payloads')

def compare_snapshots(before,after,permitted):
    require(set(before)==set(after),'protected table scope changed')
    for table,original in before.items():
        current=after[table];require(current.get('columns')==original['columns'] and isinstance(current.get('rows'),dict) and current.get('count')==len(current['rows']),'actual original metadata/count changed')
        for key,digest in original['rows'].items():require(current['rows'].get(key)==digest,'actual old row payload changed: '+table)
        require(len(set(current['rows'])-set(original['rows']))==permitted.get(table,0),'actual unexpected additions: '+table)

def validate_new_ledger(row,operation):
    require(row.get('releaseTag')==operation and row.get('operationId')==operation and row.get('file')==f'sql/mysql/{MIGRATION}.sql'
            and row.get('migrationId')==MIGRATION and row.get('sha256')==SQL_SHA and row.get('status')=='APPLIED' and row.get('environment')=='test'
            and row.get('deleted')==0 and row.get('tenantId')=='0' and row.get('creator')=='dcc-g28-local-task' and row.get('updater')=='dcc-g28-local-task'
            and row.get('startedAt') is not None and row.get('finishedAt') is not None and row.get('errorMessage') is None,'exact new ledger material payload differs')

def execute(request,technical_authorized,transport_factory):
    require(technical_authorized is True,'technical execute flag absent; no transport initialized')
    proofs,destination=validate_request(request)
    prior_proofs();policy_closure();contract=schema.prepare_contract()
    transport=transport_factory(request['database'])
    preflight=collect_schema(transport,contract);state=schema.validate_facts(contract,preflight,'pre',database=request['database'])
    require(state=='FIRST_REQUIRED','already applied/partial target is review only; do not auto rerun an earlier task')
    original=proofs['freshOriginalBaseline']['tables'];current=transport.snapshot(ORIGINAL_TABLES)
    require(current==original,'fresh original database rows drifted after prepared snapshot; no writes')
    destination.mkdir();before_path=destination/'original-before.json';before_path.write_text(json.dumps(original,indent=2)+'\n',encoding='utf-8')
    journal={'status':'RUNNING','database':request['database'],'operationId':request['operationId'],'originalBefore':desc(before_path),'migrationId':MIGRATION,'sqlSha256':SQL_SHA,'steps':[],'databaseWriteAttempted':False,'automaticRetry':False,'implicitDdlCommitPossible':True}
    def save():
        (destination/'driver-receipt.json').write_text(json.dumps(journal,indent=2)+'\n',encoding='utf-8')
    save()
    try:
        for phase in ['first','repeat']:
            if phase=='repeat':schema.validate_facts(contract,collect_schema(transport,contract),'pre',database=request['database'])
            material=compose_material(phase,request['operationId'],request['database']);(destination/(phase+'.sql')).write_bytes(material)
            # Revalidate current session target AND writer exclusion immediately before the actual write.
            schema.validate_facts(contract,collect_schema(transport,contract),'pre',database=request['database'])
            exclusions=[json.loads(line) for line in transport.read(writer_query()).splitlines() if line]
            require(len(exclusions)==1 and all(type(exclusions[0].get(k)) is int and exclusions[0].get(k)==0 for k in ['transactions','otherConnections','enabledEvents']),'fresh current writer exclusion failed before DDL')
            if 'prerequisiteProof' in proofs:
                actual_ledgers=[json.loads(line) for line in transport.read(old19_ledger_query()).splitlines() if line]
                require(sorted(actual_ledgers,key=lambda r:r.get('migrationId',''))==sorted(proofs['prerequisiteProof']['originalNineteenLedger'],key=lambda r:r.get('migrationId','')),'live original19 ledger changed before DDL')
            journal['databaseWriteAttempted']=True;save()
            result=transport.run(material,destination,phase)
            journal['steps'].append({'phase':phase,'material':desc(destination/(phase+'.sql')),'materialSha256':hashlib.sha256(material).hexdigest(),'execution':result,'phaseStatus':'SQL_RETURNED_NOT_POSTFLIGHT_VERIFIED'});save()
            require(result.get('exitCode')==0,'DDL phase failed; implicit partial commit possible, stop without repeat/restore')
            validate_execution_result(result,material,request['database'],phase)
            postflight=collect_schema(transport,contract);schema.validate_facts(contract,postflight,'post',database=request['database'])
            after=transport.snapshot(ORIGINAL_TABLES);transport.compare(original,after,{'infra_release_migration':1})
            added=set(after['infra_release_migration']['rows'])-set(original['infra_release_migration']['rows']);require(len(added)==1,'exact one new ledger key required')
            ledger_row=json.loads(transport.read(new_ledger_query()).strip())
            validate_new_ledger(ledger_row,request['operationId'])
            ledger=[r for r in postflight if r.get('kind')=='ledger'];require(len(ledger)==1,'exact one new ledger required')
            if phase=='first':
                first_schema=postflight;first_after=after
            else:require(postflight==first_schema and after==first_after,'repeat changed schema/config/originalledger payload')
            for name,value in [('postflight',postflight),('original-after',after)]:
                path=destination/(phase+'-'+name+'.json');path.write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8')
            ledger_path=destination/(phase+'-new-ledger.json');ledger_path.write_text(json.dumps(ledger_row,indent=2)+'\n',encoding='utf-8')
            journal['steps'][-1].update({'phaseStatus':'POSTFLIGHT_AND_ORIGINAL_ROWS_PASS','postflight':desc(destination/(phase+'-postflight.json')),'originalAfter':desc(destination/(phase+'-original-after.json')),'newLedger':desc(ledger_path)});save()
        journal['status']='G28_OWNED_EXISTING_CLONE_FIRST_REPEAT_PASS' if request['database']==CLONE else 'G28_SOURCE_ONE_MIGRATION_FIRST_REPEAT_PASS_NOT_APPLICATION_READINESS';save();return journal
    except BaseException as error:
        journal['status']='STOPPED_DDL_PARTIAL_COMMIT_POSSIBLE_REVIEW_REQUIRED' if journal['databaseWriteAttempted'] else 'GUARD_FAILED_NO_DDL_REACHED';journal['errorType']=type(error).__name__;save();raise

def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('mode',choices=['plan','execute']);parser.add_argument('--request',type=Path);parser.add_argument('--authorize-specific-new-migration-writes',action='store_true');args=parser.parse_args()
    if args.mode=='plan':
        package=plan();(HERE/'g29-r2-plan.json').write_text(json.dumps(package,indent=2)+'\n',encoding='utf-8')
        for phase in ['first','repeat']:(HERE/('g29-r2-source-'+phase+'.sql')).write_bytes(compose_material(phase,'dcc-g28-local-new-sidecars',SOURCE))
        (HERE/'g29-r2-schema-contract.json').write_text(json.dumps(package['schema'],indent=2)+'\n',encoding='utf-8');(HERE/'g29-r2-schema-capture.sql').write_text(schema.capture_sql(),encoding='utf-8');(HERE/'g29-r2-schema-row-counts.sql').write_text(schema.row_counts_sql(),encoding='utf-8')
        print(json.dumps({k:package[k] for k in ['status','executionCount','policyClosureCount','writeAuthorizationGranted']}));return
    require(args.request is not None,'explicit Root reviewed request required')
    # Actual adapter intentionally initialized only inside execute after flags/real authorization/proof validation.
    def factory(database):
        require(sha(MAIN/'g21_mysql_support.py')==SUPPORT_SHA,'frozen Root transport helper drift')
        spec=importlib.util.spec_from_file_location('g28_frozen_mysql_support',MAIN/'g21_mysql_support.py');support=importlib.util.module_from_spec(spec);spec.loader.exec_module(support)
        class Adapter:
            def __init__(self):self.mysql=support.LocalMysql(database)
            def read(self,sql):return self.mysql.read(sql)
            def snapshot(self,tables):return support.snapshot_original_rows(self.mysql,tables)
            def compare(self,before,after,allowed):return support.compare_original_rows(before,after,allowed)
            def run(self,sql,directory,phase):
                import subprocess
                stdout=directory/('g28-'+phase+'.stdout.txt');stderr=directory/('g28-'+phase+'.stderr.txt')
                require(not stdout.exists() and not stderr.exists(),'execution raw receipts exist; ambiguous retry forbidden')
                with stdout.open('xb') as out,stderr.open('xb') as error:
                    result=subprocess.run(self.mysql.command(),input=sql,stdout=out,stderr=error)
                return {'exitCode':result.returncode,'database':database,'sqlSha256':hashlib.sha256(sql).hexdigest(),'stdout':desc(stdout),'stderr':desc(stderr),'noRetry':True}

        return Adapter()
    result=execute(read(args.request),args.authorize_specific_new_migration_writes,factory);print(result['status'])

if __name__=='__main__':main()
