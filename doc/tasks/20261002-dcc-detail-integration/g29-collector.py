"""G29 read-only fresh-input collector/backup. Never executes DDL, restore, clone provisioning, services or object writes."""
from __future__ import annotations
import argparse, gzip, hashlib, json, re, shutil, subprocess, time
from pathlib import Path
import importlib.util

HERE=Path(__file__).resolve().parent;ROOT=HERE.parents[2]
MAIN=Path('C:/IntRuoyiAll-int_main/doc/tasks/20261001-dcc-integration-unblock');PROTECTED=Path('C:/IntRuoyiBackups/20261003-dcc-integration')
SOURCE='ruoyi-vue-pro';CLONE='dcc_intqms_g18_rehearsal';UUID='92ca05d0-aec8-11f1-a944-02b4e226a5ef'
TABLES=['dcc_controlled_file','dcc_controlled_file_master','dcc_controlled_file_name_claim','dcc_controlled_file_source_ownership','dcc_controlled_file_signature','system_electronic_signature','infra_release_migration']
MIGRATION='20261003_dcc_legacy_source_name_occupancy';SQL_SHA='621fe041064ac07c5bfe995abd556c3f9436b0253c883a3b7d2b33014e68de2a';G28TOOL=HERE/'g28-driver.py'
G28_SEAL_SHA='ea64fd36d37fc9f3cf98da9f2ebd7b1949301a2ada671c0c96cc4fe1bd5fcadf'
seal=HERE/'g28-delivery-fingerprints.json'
if hashlib.sha256(seal.read_bytes()).hexdigest()!=G28_SEAL_SHA:raise ValueError('frozen G28 delivery seal changed')
for sealed_asset in json.loads(seal.read_text(encoding='utf-8'))['assets']:
    path=ROOT/sealed_asset['path']
    if path.stat().st_size!=sealed_asset['bytes'] or hashlib.sha256(path.read_bytes()).hexdigest()!=sealed_asset['sha256']:raise ValueError('frozen G28 dependency asset changed')
spec=importlib.util.spec_from_file_location('g28',G28TOOL);g28=importlib.util.module_from_spec(spec);spec.loader.exec_module(g28)
def require(x,m):
    if not x:raise ValueError(m)
def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def descriptor(p):
    p=Path(p);return {'path':str(p.resolve()),'sha256':sha(p),'bytes':p.stat().st_size}
def read(p):return json.loads(Path(p).read_text(encoding='utf-8-sig'))
def verify_empty_target(db):
    # Collector never issues write-capable SQL. Root only supplies a read transport.
    rows=db.read("SELECT JSON_OBJECT('kind','target_ledger_presence','rows',COUNT(*)) FROM infra_release_migration WHERE migration_id='"+MIGRATION+"';\n")
    row=json.loads(rows.strip());require(row.get('rows')==0,'target migration ledger already present; no fresh input');return row
def runtime_sql():return "SELECT JSON_OBJECT('kind','runtime','database',DATABASE(),'serverUuid',@@server_uuid,'mysqlVersion',VERSION(),'pageSize',@@innodb_page_size,'rowFormat',@@innodb_default_row_format,'sqlMode',@@session.sql_mode);\n"
def writer_sql():return "SELECT JSON_OBJECT('kind','writer_exclusion','transactions',(SELECT COUNT(*) FROM information_schema.INNODB_TRX),'otherConnections',(SELECT COUNT(*) FROM information_schema.PROCESSLIST WHERE ID<>CONNECTION_ID() AND USER NOT IN ('event_scheduler','system user')),'enabledEvents',(SELECT COUNT(*) FROM information_schema.EVENTS WHERE EVENT_SCHEMA=DATABASE() AND STATUS='ENABLED'));\n"
def schema_sql():return "SELECT JSON_OBJECT('kind','table','table',TABLE_NAME,'engine',ENGINE,'charset',SUBSTRING_INDEX(TABLE_COLLATION,'_',1),'collation',TABLE_COLLATION,'rowFormat',ROW_FORMAT) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ("+','.join("'"+t+"'" for t in TABLES)+') ORDER BY TABLE_NAME;\n'
def table_columns_sql():return "SELECT JSON_OBJECT('kind','column','table',TABLE_NAME,'name',COLUMN_NAME,'ordinal',ORDINAL_POSITION,'type',COLUMN_TYPE,'nullable',IS_NULLABLE,'charset',CHARACTER_SET_NAME,'collation',COLLATION_NAME,'default',COLUMN_DEFAULT,'precision',DATETIME_PRECISION,'extra',TRIM(REPLACE(EXTRA,'DEFAULT_GENERATED','')),'expression',GENERATION_EXPRESSION) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ("+','.join("'"+t+"'" for t in TABLES)+') ORDER BY TABLE_NAME,ORDINAL_POSITION;\n'
def ledger_sql():return "SELECT JSON_OBJECT('kind','ledger','migrationId',migration_id,'sha256',sha256,'status',status,'environment',target_environment,'deleted',deleted+0) FROM infra_release_migration WHERE migration_id IN ("+','.join("'"+k+"'" for k in set(g28.expected_old19())|{MIGRATION})+") ORDER BY migration_id;\n"
def baseline_sql():
    # Every original column is obtained from information_schema then row hashes are selected through frozen support logic.
    return "SELECT JSON_OBJECT('kind','baseline_scope','table',TABLE_NAME,'columnCount',COUNT(*)) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ("+','.join("'"+t+"'" for t in TABLES)+') GROUP BY TABLE_NAME ORDER BY TABLE_NAME;\n'
def run_select(db,sql):
    text=db.read(sql);return [json.loads(x) for x in text.splitlines() if x.strip()]

def dump_command(database,kind,docker):
    require(database in {SOURCE,CLONE} and kind in {'schema','protected-original-rows'},'fixed readonly dump scope required')
    shell='test -n "$MYSQL_ROOT_PASSWORD" || exit 3; export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"; exec mysqldump --user=root --default-character-set=utf8mb4 --single-transaction --skip-lock-tables --skip-add-locks --skip-triggers --skip-routines --skip-events --set-gtid-purged=OFF --no-tablespaces "$@"'
    options=['--no-data'] if kind=='schema' else ['--no-create-info','--skip-extended-insert']
    return [docker,'exec','-i','int-ruoyi-mysql','sh','-c',shell,'dcc-g29-readonly-dump',*options,database,*TABLES]

class RecordingAdapter:
    """Existing local Docker only; SQL boundary accepts SELECT/SHOW, mysqldump fixed readonly profiles."""
    def __init__(self,database,directory):
        require(database in {SOURCE,CLONE},'existing local database required')
        require(sha(MAIN/'g21_mysql_support.py')==g28.SUPPORT_SHA,'frozen Root transport changed')
        spec=importlib.util.spec_from_file_location('g29_frozen_support',MAIN/'g21_mysql_support.py');self.support=importlib.util.module_from_spec(spec);spec.loader.exec_module(self.support)
        self.mysql=self.support.LocalMysql(database);self.database=database;self.directory=directory;self.calls=0;self.capture_receipts=[]
    def read(self,sql):
        statements=[s.strip() for s in re.sub(r'(?m)^\s*--.*$','',sql).split(';') if s.strip()]
        require(statements and all(re.match(r'^(SELECT|SHOW)\b',s,re.I) for s in statements),'read-only SQL required')
        require(not re.search(r'\b(?:INTO\s+(?:OUTFILE|DUMPFILE)|LOAD_FILE|GET_LOCK|RELEASE_LOCK|SLEEP|BENCHMARK)\b|/\*!',sql,re.I),'side-effecting readonly function/directive forbidden')
        self.calls+=1;stem=f'read-{self.calls:03d}';out=self.directory/(stem+'.facts.txt');err=self.directory/(stem+'.stderr.txt')
        started=time.time();result=subprocess.run(self.mysql.command(),input=(runtime_sql()+sql).encode('utf-8'),capture_output=True)
        raw=self.directory/(stem+'.raw-stdout.txt');raw.write_bytes(result.stdout);err.write_bytes(result.stderr)
        require(result.returncode==0,'readonly MySQL failed; protected raw stdout/stderr preserved, no retries')
        split=result.stdout.split(b'\n',1);require(len(split)==2,'actual same-session runtime envelope required');envelope=json.loads(split[0])
        require(envelope.get('kind')=='runtime' and envelope.get('database')==self.database and envelope.get('serverUuid')==UUID and envelope.get('mysqlVersion')=='8.0.40','actual same-session source identity mismatch')
        out.write_bytes(split[1])
        receipt={'database':self.database,'serverUuid':envelope['serverUuid'],'actualRead':True,'databaseWrites':False,'sqlSha256':hashlib.sha256(sql.encode('utf-8')).hexdigest(),'envelopedSqlSha256':hashlib.sha256((runtime_sql()+sql).encode('utf-8')).hexdigest(),'factsSha256':sha(out),'exitCode':result.returncode,'capturedEpoch':time.time(),'startedEpoch':started,'capture':envelope,'stdout':descriptor(out),'rawStdout':descriptor(raw),'stderr':descriptor(err)}
        path=self.directory/(stem+'.read-receipt.json');path.write_text(json.dumps(receipt,indent=2)+'\n',encoding='utf-8');self.capture_receipts.append(descriptor(path))
        return split[1].decode('utf-8')
    def snapshot(self):return self.support.snapshot_original_rows(self,TABLES)
    def backup(self,kind,directory):
        argv=dump_command(self.database,kind,self.mysql.docker);path=directory/(kind+'.sql.gz');stderr=directory/(kind+'.dump.stderr.txt');started=time.time();expanded=0;counts={t:0 for t in TABLES}
        with stderr.open('xb') as errors,gzip.open(path,'xb') as compressed:
            process=subprocess.Popen(argv,stdout=subprocess.PIPE,stderr=errors)
            try:
                for line in iter(process.stdout.readline,b''):
                    expanded+=len(line);compressed.write(line)
                    match=re.match(rb'INSERT INTO `([^`]+)` VALUES \(',line)
                    if match:
                        name=match.group(1).decode('utf-8');require(name in counts,'dump unexpected table');counts[name]+=1
                process.stdout.close();code=process.wait()
            except BaseException:
                if process.poll() is None:process.terminate()
                process.wait();raise
        command={'profile':'G28_SCHEMA_SINGLE_TRANSACTION_NO_DATA' if kind=='schema' else 'G28_PROTECTED_ROWS_SINGLE_TRANSACTION_SINGLE_ROW_INSERTS','database':self.database,'serverUuid':UUID,'container':'int-ruoyi-mysql','exitCode':code,'databaseWrites':False,'credentialsPersisted':False,'artifactSha256':sha(path),'artifactBytes':path.stat().st_size,'startedEpoch':started,'finishedEpoch':time.time(),'uncompressedBytes':expanded,'tables':TABLES,'singleRowInsertCounts':counts,'argvSha256':hashlib.sha256(json.dumps(argv).encode()).hexdigest(),'stderr':descriptor(stderr)}
        receipt=directory/(kind+'-dump-command-receipt.json');receipt.write_text(json.dumps(command,indent=2)+'\n',encoding='utf-8')
        require(code==0 and stderr.stat().st_size==0,'actual readonly dump failed; preserve partial artifact, do not retry')
        return {'kind':kind,**descriptor(path),'dumpExitCode':code,'dumpCommandReceipt':descriptor(receipt)}

def _collect_fresh_inputs(database,destination,attestation,adapter_factory=RecordingAdapter,invocation=None):
    """Root invocation after review; readonly gathering does not require or grant new DDL permission."""
    require(database in {SOURCE,CLONE},'fixed existing database required');destination=Path(destination).resolve()
    require(destination.parent==PROTECTED.resolve() and not destination.exists(),'new direct protected task child required')
    require(attestation.get('database')==database and attestation.get('serverUuid')==UUID and attestation.get('allApplicationWritersExcluded') is True
            and type(attestation.get('capturedEpoch')) in {int,float} and 0<=time.time()-attestation['capturedEpoch']<=900,'fresh Root writer registry/attestation required')
    g28.prior_proofs();g28.policy_closure();destination.mkdir()
    if invocation is not None:invocation['created']=True
    db=adapter_factory(database,destination);proofs=[]
    def persist(name,value):
        path=destination/name;path.write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8');return descriptor(path)
    def proof(name,extra):return persist(name,{'database':database,'serverUuid':UUID,'actualRead':True,'capturedEpoch':time.time(),**extra})
    runtime=run_select(db,runtime_sql());require(len(runtime)==1 and runtime[0].get('database')==database and runtime[0].get('serverUuid')==UUID and runtime[0].get('mysqlVersion')=='8.0.40','actual fixed runtime differs')
    exclusion=run_select(db,writer_sql());require(len(exclusion)==1 and all(type(exclusion[0].get(k)) is int and exclusion[0][k]==0 for k in ['transactions','otherConnections','enabledEvents']),'live writers block collection')
    writer=proof('writer-exclusion.json',{'allApplicationWritersExcluded':True,**exclusion[0],'rootAttestation':attestation})
    fresh=g28.collect_schema(db,g28.schema.prepare_contract());require(g28.schema.validate_facts(g28.schema.prepare_contract(),fresh,'pre',database=database)=='FIRST_REQUIRED','newtarget absent required, no implicitrerun')
    preflight=proof('fresh-preflight.json',{'status':'FIRST_REQUIRED','schemaFacts':fresh,'runtime':runtime[0]})
    baseline=db.snapshot();require(set(baseline)==set(TABLES),'exact actual7 protected baseline required')
    baseline_obj={'source':'ACTUAL_SOURCE_READ_NOT_CLONE' if database==SOURCE else 'ACTUAL_OWNED_CLONE_READ','tables':baseline};baseline_desc=proof('fresh-original-baseline.json',baseline_obj)
    baseline_full=read(Path(baseline_desc['path']));writer_full=read(Path(writer['path']))
    contract=g28.schema.base;schema_query=(HERE/'g21-postflight-schema-queries.sql').read_bytes().decode('utf-8');env_query=(HERE/'g21-postflight-environment-query.sql').read_bytes().decode('utf-8')
    schema_raw=db.read(schema_query);schema_path=destination/'post19-schema-facts.jsonl';schema_path.write_bytes(schema_raw.encode('utf-8'))
    raw_receipts=getattr(db,'capture_receipts',[])
    actual_schema_read=read(Path(raw_receipts[-1]['path'])) if raw_receipts else None
    require(actual_schema_read is not None or getattr(db,'explicitOfflineTestAdapter',False),'actual same-session schema capture receipt required')
    if actual_schema_read:
        require(actual_schema_read['factsSha256']==sha(schema_path) and actual_schema_read['sqlSha256']==sha(HERE/'g21-postflight-schema-queries.sql') and actual_schema_read['exitCode']==0,'actual schema raw query/facts capture differs')
    schema_capture=proof('post19-schema-capture.json',{'factsSha256':sha(schema_path),'sqlSha256':hashlib.sha256(schema_query.encode()).hexdigest(),'databaseWrites':False,'capture':actual_schema_read['capture'] if actual_schema_read else {'database':database,'serverUuid':UUID},'actualReadReceipt':raw_receipts[-1] if raw_receipts else None,'offlineFixtureOnly':actual_schema_read is None})
    env_raw=db.read(env_query);env_path=destination/'post19-environment.jsonl';env_path.write_bytes(env_raw.encode('utf-8'))
    validation=contract.validate(read(HERE/'g21-postflight-schema-contract.json'),contract.read_facts(schema_path),contract.read_environment(env_path));require(validation['status']=='POSTFLIGHT_SCHEMA_PASS_NOT_EXECUTION_EVIDENCE','actual post19 schema prerequisite fails')
    validation_desc=persist('post19-validation.json',validation)
    oldledger=run_select(db,g28.old19_ledger_query());expected=g28.expected_old19();require(len(oldledger)==19 and {r.get('migrationId') for r in oldledger}==set(expected),'actual exact19ledger required')
    for row in oldledger:require(row.get('sha256')==expected[row['migrationId']] and row.get('status')=='APPLIED' and row.get('environment')=='test' and row.get('deleted')==0,'actualold19ledgerpayload differs')
    prerequisites=proof('prerequisite-proof.json',{'status':'EXACT_SEVEN_FACTS_AND_OLD19_LEDGER_VERIFIED','prerequisiteIds':sorted(g28.expected_prerequisites()),'originalNineteenLedger':oldledger,'post19SchemaFacts':descriptor(schema_path),'post19Environment':descriptor(env_path),'post19Validation':validation_desc,'schemaQuerySha256':sha(HERE/'g21-postflight-schema-queries.sql'),'schemaValidatorSha256':sha(HERE/'g21-postflight-schema.py'),'schemaContractSha256':sha(HERE/'g21-postflight-schema-contract.json'),'schemaCaptureReceipt':schema_capture})
    before_dump=run_select(db,writer_sql());require(len(before_dump)==1 and all(type(before_dump[0].get(k)) is int and before_dump[0][k]==0 for k in ['transactions','otherConnections','enabledEvents']),'writerschangedbeforedump')
    artifacts=[db.backup(kind,destination) for kind in ['schema','protected-original-rows']]
    backup_obj={'status':'FRESH_G28_ORIGINAL_BACKUP_COMPLETE','protectedTables':TABLES,'baselineAggregateSha256':hashlib.sha256(json.dumps(baseline,sort_keys=True,separators=(',',':')).encode()).hexdigest(),'artifacts':artifacts}
    backup_desc=proof('fresh-backup.json',backup_obj);g28.validate_backup(read(Path(backup_desc['path'])),baseline_full,database,writer_full)
    final_snapshot=db.snapshot();g28.compare_snapshots(baseline,final_snapshot,{})
    after_dump=run_select(db,writer_sql());require(len(after_dump)==1 and all(type(after_dump[0].get(k)) is int and after_dump[0][k]==0 for k in ['transactions','otherConnections','enabledEvents']),'writerschangedafterdump')
    require(0<=time.time()-attestation['capturedEpoch']<=900 and all(0<=time.time()-read(Path(p['path']))['capturedEpoch']<=900 for p in [writer,preflight,baseline_desc,backup_desc,prerequisites]),'collector finished after technical freshness window; retain facts but no ready request')
    request={'version':'G28-EXEC-1','status':'FRESH_READONLY_INPUTS_READY_DDL_AUTHORIZATION_STILL_ABSENT','database':database,'operationId':'dcc-g28-local-new-sidecars','specificNewMigrationAuthorized':False,'authorization':None,'freshPreflight':preflight,'freshOriginalBaseline':baseline_desc,'writerExclusion':writer,'freshBackup':backup_desc,'prerequisiteProof':prerequisites,'newMigrationClonePass':None,'privateDirectory':None,'actualWritesExecuted':False}
    request_desc=persist('g28-execution-request.json',request)
    result={'status':'G29_FRESH_READONLY_INPUT_COLLECTION_COMPLETE_NOT_DDL_EXECUTION','database':database,'serverUuid':UUID,'actualRead':True,'databaseWrites':False,'objectWrites':False,'credentialsPersisted':False,'specificNewMigrationAuthorized':False,'capturedEpoch':time.time(),'request':request_desc,'readReceipts':getattr(db,'capture_receipts',[])}
    persist('g29-collection-receipt.json',result);return result

def collect_fresh_inputs(database,destination,attestation,adapter_factory=RecordingAdapter):
    invocation={'created':False}
    try:return _collect_fresh_inputs(database,destination,attestation,adapter_factory,invocation)
    except BaseException as error:
        path=Path(destination).resolve()
        if invocation['created'] and path.parent==PROTECTED.resolve() and path.is_dir():
            (path/'g29-collection-failure.json').write_text(json.dumps({'status':'STOPPED_READONLY_COLLECTION_FAILED','database':database,'databaseWrites':False,'objectWrites':False,'errorType':type(error).__name__,'noAutomaticRetry':True,'readyExecutionRequestCreated':False},indent=2)+'\n',encoding='utf-8')
            (path/'private-error-type.txt').write_text(type(error).__name__+'\n',encoding='utf-8')
        raise
def main():
    parser=argparse.ArgumentParser();parser.add_argument('--plan',action='store_true');parser.add_argument('--collect-readonly',action='store_true');parser.add_argument('--database',choices=[SOURCE,CLONE]);parser.add_argument('--output',type=Path);parser.add_argument('--writer-attestation',type=Path);args=parser.parse_args()
    if args.plan:
        print(json.dumps({'status':'PREPARED_READONLY_COLLECTOR_NOT_INVOKED','source':SOURCE,'clone':CLONE,'tables':TABLES,'writes':False}));return
    require(args.collect_readonly and args.database and args.output and args.writer_attestation,'explicit Root --collect-readonly database/output/attestation required')
    try:print(json.dumps(collect_fresh_inputs(args.database,args.output,read(args.writer_attestation))))
    except BaseException as error:print(json.dumps({'status':'STOPPED_READONLY_COLLECTION_FAILED','errorType':type(error).__name__,'databaseWrites':False}));raise SystemExit(1)
if __name__=='__main__':main()
