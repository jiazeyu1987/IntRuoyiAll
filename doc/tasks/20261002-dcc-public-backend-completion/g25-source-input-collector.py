"""Task-only actual-source readonly capture and protected backup collection.
Root executes live mode; child development/tests never connect. No upgrade or DDL/DML API.
Credentials stay inside existing local container, never arguments/output/evidence.
"""
import argparse, copy, datetime as dt, gzip, hashlib, importlib.util, json, re, shutil, subprocess, sys
from pathlib import Path

HERE=Path(__file__).resolve().parent
MAIN=Path('C:/IntRuoyiAll-int_main/doc/tasks/20261001-dcc-integration-unblock')
INTEGRATION=HERE.parents[2]
BACKUPS=Path('C:/IntRuoyiBackups/20261003-dcc-integration')
SOURCE='ruoyi-vue-pro'
UUID='92ca05d0-aec8-11f1-a944-02b4e226a5ef'

def require(condition,reason):
    if not condition:raise ValueError(reason)

def sha(path):
    with Path(path).open('rb') as stream:return hashlib.file_digest(stream,'sha256').hexdigest()

def read(path):
    obj=json.loads(Path(path).read_text(encoding='utf-8-sig'));require(isinstance(obj,dict),'Exact JSON object required');return obj

def save(path,value):Path(path).write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def utc():return dt.datetime.now(dt.timezone.utc).isoformat()
def descriptor(path):return {'path':str(Path(path).resolve()),'sha256':sha(path)}

def load(name,path):
    spec=importlib.util.spec_from_file_location(name,path);m=importlib.util.module_from_spec(spec);sys.modules[name]=m;spec.loader.exec_module(m);return m

def tools():
    contract=read(HERE/'g25-source-input-collector-contract.json')
    for name,digest in contract['sourceToolHashes'].items():
        path=Path(name) if Path(name).is_absolute() else HERE/name;require(sha(path)==digest,'Frozen collection/source tool drift')
    source=load('g25_existing_source',HERE/'g23-source-upgrade-driver.py');g21=source.g21_module()
    modules=g21.load_support(MAIN,read(HERE/'g21-rehearsal-contract.json'))
    return source,g21,modules

def plan(*,transport_factory=None):
    source,g21,modules=tools();materials=read(HERE/'g21-rehearsal-prepared-inputs.json');g21.validate_prepared(materials)
    reference=read(MAIN/'g21-prerequisite-runtime-receipt.json');queries={'writer':source.writer_query(),'identity':"SELECT JSON_OBJECT('database',DATABASE(),'serverUuid',@@server_uuid,'mysqlVersion',@@version,'captureId',CAST(CONNECTION_ID() AS CHAR),'collectedAtUtc',UTC_TIMESTAMP(6));",'tables':"SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() ORDER BY TABLE_NAME;",'environment':Path(materials['postflight']['environmentQuery']).read_text(encoding='utf-8-sig')}
    for kind in ['structure','bpm']:
        group=next(x for x in reference['groups'] if x['kind']==kind);queries[kind]=Path(INTEGRATION/group['query']).read_text(encoding='utf-8-sig')
    for text in queries.values():
        statements=[x.strip() for x in re.sub(r'(?m)^\s*--.*$','',text).split(';') if x.strip()];require(statements and all(re.match(r'^SELECT\b',x,re.I) for x in statements),'Non-readonly collector query rejected')
    return {'status':'PREPARED_READONLY_SOURCE_COLLECTION_NOT_EXECUTED','databaseWritesExecuted':False,'actualCollectionExecuted':False,'queries':queries,'materials':materials,'prerequisiteTemplate':reference,'backupDataTables':read(MAIN/'g18-backup-scope.json')['dataBackupTables']}

def validate_attestation(options):
    require(options.get('writerAttestation'),'Root writer exclusion attestation required')
    path=Path(options['writerAttestation']).resolve(strict=True);attest=read(path)
    require(attest.get('database')==SOURCE and attest.get('serverUuid')==UUID and attest.get('allKnownWritersExcluded') is True and attest.get('attestedBy')=='Root' and attest.get('exclusionReference'),'Explicit Root actual writer exclusion attestation required')
    return path,attest

def verify_authorization(path):
    path=Path(path).resolve(strict=True);require(path==MAIN/'g25-user-authorization.json','Only exact real Root user authorization receipt is allowed')
    auth=read(path);values=auth.get('localDatabaseAuthorization',{})
    require(auth.get('source') and values.get('isolatedFirstRepeatRehearsal') is True and values.get('upgradeOriginalTestDatabaseAfterSuccessfulRehearsal') is True and values.get('remoteOrProductionDeployment') is False,'Actual local user authorization receipt missing')
    return path

def clone_descriptor(journal_path,source,g21,prepared):
    journal_path=Path(journal_path).resolve(strict=True);require(journal_path.is_relative_to(BACKUPS.resolve()) and journal_path.name=='driver-receipt.json','Clone journal must be protected actual driver receipt')
    value=descriptor(journal_path);value.update({'actualRuntimeReceipt':True,'testFixture':False,'driverSha256':source.FROZEN_G21_SHA,'artifacts':[{'relativePath':p.relative_to(journal_path.parent).as_posix(),'sha256':sha(p)} for p in sorted(journal_path.parent.rglob('*')) if p.is_file() and p!=journal_path]})
    source.verify_clone_pass(value,prepared,g21)
    return value

def collect_query(mysql,path,output,*,identity):
    raw=Path(path).read_bytes();query=raw.decode('utf-8-sig');statements=[x.strip() for x in re.sub(r'(?m)^\s*--.*$','',query).split(';') if x.strip()]
    require(statements and all(re.match(r'^SELECT\b',x,re.I) for x in statements),'Only reviewed SELECT may capture facts')
    require(not re.search(r'\bINTO\s+(?:OUTFILE|DUMPFILE)|\b(?:SLEEP|GET_LOCK|RELEASE_LOCK|LOAD_FILE)\s*\(',query,re.I),'Side-effect SELECT outside collection scope')
    require(not output.exists() and not Path(str(output)+'.receipt.json').exists(),'Fresh query capture may not overwrite')
    prefix="SELECT JSON_OBJECT('kind','capture','database',DATABASE(),'serverUuid',@@server_uuid,'version',@@version,'collectedAtUtc',UTC_TIMESTAMP(6));\n"
    started=utc()
    try:text=mysql.read(prefix+query)
    except Exception as exc:
        if getattr(exc,'private_error',None) is not None:Path(str(output)+'.error.txt').write_bytes(exc.private_error)
        raise
    header,newline,body=text.partition('\n');require(newline and body,'Capture missing source identity/facts');capture=json.loads(header)
    require(capture.get('kind')=='capture' and capture.get('database')==SOURCE and capture.get('serverUuid')==UUID and capture.get('version')=='8.0.40','Fresh query actual source identity mismatch')
    rows=[json.loads(x) for x in body.splitlines() if x.strip()];require(rows,'Missing actual query facts');output.write_bytes(body.encode('utf-8'))
    receipt={'status':'read_only_facts_collected_not_execution','databaseWrites':False,'sourceContainer':'int-ruoyi-mysql','capture':capture,'startedAtUtc':started,'sqlPath':str(Path(path).resolve()),'sqlSha256':hashlib.sha256(raw).hexdigest(),'factsPath':str(output.resolve()),'factsSha256':sha(output),'rawCaptureSha256':hashlib.sha256(text.encode()).hexdigest(),'rowCount':len(rows),'selectCount':len(statements)}
    save(Path(str(output)+'.receipt.json'),receipt);return receipt

def validate_group(group,destination):
    validator=load('g25_fresh_'+group['kind'],INTEGRATION/group['validator']);contract=read(INTEGRATION/group['contract']);facts=destination/group['facts']
    if group['kind']=='bpm':
        result=validator.validate(contract,validator.parse_jsonl(facts));result['contractSha256']=group['contractSha256'];result['evidenceSha256']=sha(facts);result['captureReceiptSha256']=sha(Path(str(facts)+'.receipt.json'))
    else:result=validator.validate_facts(contract,validator.read_facts(facts))
    wanted='satisfied_by_existing_target_facts' if group['kind']=='bpm' else 'SATISFIED_READ_ONLY_FACTS_NOT_APPLIED'
    require(result.get('status')==wanted,'Fresh actual source prerequisite validator rejected')
    save(destination/group['proof'],result);return result

def dump_backup(directory,kind,table_args,identity):
    require(kind in {'schema','data','affected'},'Backup kind outside fixed source scope')
    require(all(isinstance(x,str) and (x in {'--no-data','--no-create-info','--skip-triggers'} or re.fullmatch(r'[A-Za-z0-9_]+',x)) for x in table_args),'Unsafe or out-of-scope dump argument')
    target=directory/('source-'+kind+'.sql.gz');stderr=directory/('source-'+kind+'.stderr.txt');require(not target.exists() and not stderr.exists(),'New source backup cannot overwrite old artifact')
    docker=shutil.which('docker.exe');require(docker,'Existing local Docker CLI required')
    shell='test -n "$MYSQL_ROOT_PASSWORD" || exit 3; export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"; exec mysqldump --user=root --default-character-set=utf8mb4 --single-transaction --skip-lock-tables --skip-add-locks --no-tablespaces --set-gtid-purged=OFF --hex-blob "$@"'
    argv=[docker,'exec','int-ruoyi-mysql','sh','-c',shell,'dcc-source-readonly-dump',SOURCE,*table_args];started=utc()
    with stderr.open('xb') as err:
        process=subprocess.Popen(argv,stdout=subprocess.PIPE,stderr=err)
        try:
            with gzip.open(target,'xb',compresslevel=6) as compressed:shutil.copyfileobj(process.stdout,compressed)
            process.stdout.close();code=process.wait()
        except BaseException:
            if process.poll() is None:process.terminate()
            process.wait();raise
    require(code==0,'New source backup failed; protected stderr retained')
    size=0
    with gzip.open(target,'rb') as payload:
        while block:=payload.read(1024*1024):size+=len(block)
    require(size>0,'Source backup expanded empty')
    completed=utc();command=directory/(kind+'-dump-command-receipt.json')
    save(command,{'kind':kind,'database':SOURCE,'sourceContainer':'int-ruoyi-mysql','serverUuid':UUID,'captureId':identity['captureId'],'exitCode':code,'path':str(target.resolve()),'sha256':sha(target),'startedAtUtc':started,'completedAtUtc':completed,'argvProfile':'mysqldump-readonly-fixed-local-no-routines-events-no-credentials-export','stderrSha256':sha(stderr)})
    return {'kind':kind,'path':str(target.resolve()),'bytes':target.stat().st_size,'uncompressedBytes':size,'sha256':sha(target),'gzipIntegrity':'PASS','dumpExitCode':0,'dumpCommandReceipt':descriptor(command)}

def collect(options,*,transport_factory=None,backup_runner=None):
    # User/Root owns actual invocation. This tool cannot run the source driver upgrade.
    attestation_path,attestation=validate_attestation(options)
    auth=verify_authorization(options.get('authorization',''))
    prepared_plan=plan();source,g21,modules=tools();materials=prepared_plan['materials']
    clone=clone_descriptor(options.get('cloneJournal',''),source,g21,materials)
    destination=Path(options.get('destination','')).resolve();require(destination.parent==BACKUPS.resolve() and not destination.exists(),'Fresh collection must use a new protected backup child directory')
    destination.mkdir();journal={'status':'COLLECTING_SOURCE_READONLY_INPUTS','databaseWritesExecuted':False,'upgradeInvoked':False,'steps':[]};save(destination/'collection-receipt.json',journal)
    try:
        mysql=(transport_factory or modules['g21_mysql_support'].LocalMysql)(SOURCE)
        identities=g21.json_rows(g21.protected_read(mysql,prepared_plan['queries']['identity'],destination,'source-initial-identity'));require(len(identities)==1,'One actual source connection capture required');identity=identities[0]
        require(identity.get('database')==SOURCE and identity.get('serverUuid')==UUID and identity.get('mysqlVersion')=='8.0.40' and identity.get('captureId'),'Collector actual source identity mismatch')
        writers=g21.json_rows(g21.protected_read(mysql,prepared_plan['queries']['writer'],destination,'source-initial-writers'));source.require_writer_free(writers,{})
        writer_receipt=destination/'source-writer-exclusion.json';save(writer_receipt,{'database':SOURCE,'serverUuid':UUID,'collectedAtUtc':utc(),'allKnownWritersExcluded':True,'rootAttestation':descriptor(attestation_path),'exclusionReference':attestation['exclusionReference'],'mysqlFacts':writers[0]})
        reference=copy.deepcopy(prepared_plan['prerequisiteTemplate'])
        for group in reference['groups']:
            group['proof']='fresh-'+group['kind']+'-proof.json';group['facts']='fresh-'+group['kind']+'-facts.jsonl';group['captureReceipt']=group['facts']+'.receipt.json'
            capture=collect_query(mysql,INTEGRATION/group['query'],destination/group['facts'],identity=identity);group['factsSha256']=capture['factsSha256'];group['captureReceiptSha256']=sha(destination/group['captureReceipt']);group['collectedAtUtc']=capture['capture']['collectedAtUtc']
            validate_group(group,destination);group['proofSha256']=sha(destination/group['proof'])
        reference['databaseWritesExecuted']=False;reference['writeAuthorizationGranted']=False;proof_path=destination/'g21-prerequisite-runtime-receipt.json';save(proof_path,reference)
        source.verify_source_prerequisites(descriptor(proof_path),g21,source.now_utc(),900)
        table_names=g21.protected_read(mysql,prepared_plan['queries']['tables'],destination,'source-schema-table-identities').splitlines();require(len(table_names)==922 and set(table_names)==set(materials['schemaTableNames']),'Current source922 table names differ from rehearsed baseline; re-review required')
        ledger=g21.json_rows(g21.protected_read(mysql,g21.ledger_query(materials),destination,'source-task-ledger-absence'));require(not ledger,'Source candidate ledger already present')
        baseline=modules['g21_mysql_support'].snapshot_original_rows(g21.RecordingMysql(mysql,destination,'source-baseline'),materials['originalTables']+['infra_release_migration'])
        baseline_path=destination/'source-baseline.json';save(baseline_path,{'database':SOURCE,'serverUuid':UUID,'origin':'ACTUAL_SOURCE_READ_NOT_CLONE','databaseWrites':False,'collectedAtUtc':utc(),'snapshot':baseline})
        envs=g21.json_rows(g21.protected_read(mysql,prepared_plan['queries']['environment'],destination,'source-environment'));require(len(envs)==1 and envs[0].get('database')==SOURCE and envs[0].get('serverUuid')==UUID,'Actual source environment mismatch')
        environment_path=destination/'source-environment.json';save(environment_path,{'database':SOURCE,'serverUuid':UUID,'databaseWrites':False,'collectedAtUtc':utc(),'environment':envs[0]})
        source.require_writer_free(g21.json_rows(g21.protected_read(mysql,prepared_plan['queries']['writer'],destination,'source-pre-backup-writers')), {})
        backup_started=utc();runner=backup_runner or dump_backup
        artifacts=[runner(destination,'schema',['--no-data'],identity),runner(destination,'data',['--no-create-info','--skip-triggers',*prepared_plan['backupDataTables']],identity),runner(destination,'affected',materials['originalTables']+['infra_release_migration'],identity)]
        backup_path=destination/'source-backup-receipt.json';save(backup_path,{'status':'fresh_actual_source_backup_not_restored','database':SOURCE,'sourceContainer':'int-ruoyi-mysql','serverUuid':UUID,'tableCount':210,'databaseWrites':False,'credentialsExported':False,'backupDirectory':str(destination),'startedAtUtc':backup_started,'completedAtUtc':utc(),'sourceCapture':identity,'sourceSnapshotSha256':sha(baseline_path),'writerExclusionReceiptSha256':sha(writer_receipt),'affectedTables':materials['originalTables']+['infra_release_migration'],'artifacts':artifacts})
        source.require_writer_free(g21.json_rows(g21.protected_read(mysql,prepared_plan['queries']['writer'],destination,'source-post-backup-writers')), {})
        after=modules['g21_mysql_support'].snapshot_original_rows(g21.RecordingMysql(mysql,destination,'source-after-backup'),materials['originalTables']+['infra_release_migration']);require(after==baseline,'Source original rows changed during readonly collection/backup')
        g21.save_json(destination/'source-after-backup-rows.json',after)
        request=read(HERE/'g23-source-upgrade-request-template.json');request.update({'clonePass':clone,'sourceProof':descriptor(proof_path),'newBackupReceipt':descriptor(backup_path),'sourceBaseline':descriptor(baseline_path),'sourceEnvironment':descriptor(environment_path),'writerExclusionReceipt':descriptor(writer_receipt),'actualUserWriteApprovalRecordedByRoot':True,'rootAuthorizationReference':str(auth)+':'+sha(auth)})
        request_path=destination/'source-upgrade-request.json';save(request_path,request)
        verified=source.prepare(request);require(verified.get('status')=='PREPARED_VERIFIED_SOURCE_UPGRADE_NOT_EXECUTED','Strict G23 input preparation failed')
        save(destination/'source-upgrade-prepared.json',verified);journal.update({'status':'FRESH_SOURCE_INPUTS_VERIFIED_NOT_UPGRADED','sourceDatabase':SOURCE,'serverUuid':UUID,'request':descriptor(request_path),'prepared':descriptor(destination/'source-upgrade-prepared.json'),'backup':descriptor(backup_path),'sourceProof':descriptor(proof_path),'userAuthorizationReceiptSha256':sha(auth),'databaseWritesExecuted':False,'upgradeInvoked':False});save(destination/'collection-receipt.json',journal);return journal
    except Exception as exc:
        (destination/'collection-error.txt').write_text(str(exc),encoding='utf-8');journal.update({'status':'COLLECTION_FAILED_STOPPED_NO_SOURCE_UPGRADE','errorType':type(exc).__name__,'privateErrorFile':str(destination/'collection-error.txt')});save(destination/'collection-receipt.json',journal)
        raise ValueError('Actual source collection stopped; inspect protected evidence; source upgrade not invoked') from exc

def main():
    parser=argparse.ArgumentParser(description=__doc__);commands=parser.add_subparsers(dest='command',required=True)
    prepare=commands.add_parser('plan');prepare.add_argument('--result',required=True)
    run=commands.add_parser('collect');run.add_argument('--destination',required=True);run.add_argument('--writer-attestation',required=True);run.add_argument('--authorization',required=True);run.add_argument('--clone-journal',required=True)
    args=parser.parse_args()
    try:
        if args.command=='plan':result=plan();save(args.result,result)
        else:result=collect({'destination':args.destination,'writerAttestation':args.writer_attestation,'authorization':args.authorization,'cloneJournal':args.clone_journal})
    except (ValueError,OSError,KeyError) as exc:print(json.dumps({'status':'COLLECTOR_REJECTED','errorType':type(exc).__name__,'reason':'Exact failure retained in protected receipt where collection started','databaseWritesExecuted':False,'upgradeInvoked':False}));raise SystemExit(1)
    print(json.dumps({'status':result['status'],'databaseWritesExecuted':False,'upgradeInvoked':False}))

if __name__=='__main__':main()
