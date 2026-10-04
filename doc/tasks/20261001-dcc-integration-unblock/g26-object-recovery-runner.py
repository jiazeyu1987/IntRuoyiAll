"""Root exact three-object recovery launcher; no write without recorded real authorization."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
from datetime import datetime, timezone

TASK=Path('C:/IntRuoyi/20261001-dcc-integration/doc/tasks/20261002-dcc-public-backend-completion')
MAIN=Path(__file__).resolve().parent
BACKUPS=Path('C:/IntRuoyiBackups/20261003-dcc-integration')
IDS={'9198354931064','9198354931068','9198354931079','9198354931095'}
KEYS={'9198354931064':'8e0457c92be8e53d420496a5202065e667392fc6e2f06f1fb919d66ae161097e',
      '9198354931068':'1484202bd71e6b66bf6d21d0900ddcd997ce4d5cdd01b51dac6debd98d58803a',
      '9198354931079':'94de5840221043406c340aff0a96e0bc7244040fd1cea1ad195711a791b21779',
      '9198354931095':'94de5840221043406c340aff0a96e0bc7244040fd1cea1ad195711a791b21779'}

def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()

def verify_delivery():
    delivery=json.loads((TASK/'g26-delivery-fingerprints.json').read_text(encoding='utf-8'))
    for item in delivery['assets']+delivery['compiledClasses']:
        p=Path(item['path']).resolve()
        if not (p.is_relative_to(TASK.resolve()) or p.is_relative_to(MAIN.resolve())) or sha(p)!=item['sha256']:
            raise ValueError('Recovery source/class/dependency drift')
    classes=TASK/'g26-exact-object-recovery-runtime/classes'
    if {p.resolve() for p in classes.iterdir()}!={Path(a['path']).resolve() for a in delivery['compiledClasses']}:
        raise ValueError('Unsealed runtime class')
    libs=TASK/'g25-readonly-source-bytes-runtime/libs'
    if {p.name for p in libs.iterdir()}!={a['name'] for a in delivery['dependencies'] if not a['name'].startswith('reactive-streams-')}:
        raise ValueError('Unsealed runtime library')
    for item in delivery['dependencies']:
        p=(MAIN/'g25-source-bytes-extra-runtime'/item['name']) if item['name'].startswith('reactive-streams-') else libs/item['name']
        if sha(p)!=item['sha256']:raise ValueError('Runtime dependency fingerprint drift')
    return delivery

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--mode',choices=['prepare','recover'],required=True)
    parser.add_argument('--output-directory',type=Path,required=True)
    parser.add_argument('--authorization',type=Path)
    args=parser.parse_args()
    if args.output_directory.exists() or args.output_directory.resolve().parent!=BACKUPS.resolve():raise ValueError('New exact protected child required')
    delivery=verify_delivery()
    auth=None
    if args.mode=='recover':
        if not args.authorization or args.authorization.resolve()!=MAIN/'g26-object-recovery-authorization.json':raise ValueError('Real Root authorization receipt required')
        auth=json.loads(args.authorization.read_text(encoding='utf-8'))
        if auth.get('actualUserApproval') is not True or auth.get('maximumUniqueObjectPuts')!=3 or set(auth.get('sourceFileIds',[]))!=IDS or not auth.get('sourceMessageReference'):raise ValueError('Specific actual object approval missing')
    sealed=json.loads((BACKUPS/'g23-legacy-name-facts.jsonl.receipt.json').read_text())
    facts_path=BACKUPS/'g23-legacy-name-facts.jsonl'
    if sha(facts_path)!=sealed['factsSha256']:raise ValueError('Source facts drift')
    facts=[json.loads(l) for l in facts_path.read_text(encoding='utf-8').splitlines() if l]
    old={r['id']:r for r in facts if r.get('kind')=='storage' and r.get('id') in IDS}
    source_versions=[r for r in facts if r.get('kind')=='version' and r.get('sourceFileId') in IDS]
    if len(source_versions)!=4 or {r['sourceFileId'] for r in source_versions}!=IDS or len({r['id'] for r in source_versions})!=4:raise ValueError('Sealed four exact source versions required')
    versions={r['sourceFileId']:r for r in source_versions}
    docker,java=shutil.which('docker.exe'),shutil.which('java.exe')
    if not docker or not java:raise ValueError('Existing runtimes required')
    sql=("SELECT JSON_OBJECT('database',DATABASE(),'serverUuid',@@server_uuid,'collectedAtUtc',UTC_TIMESTAMP(6));\n"
         "SELECT JSON_OBJECT('id',CAST(id AS CHAR),'storage',storage,'deleted',deleted+0,'config',CAST(config AS JSON)) FROM infra_file_config WHERE id=28;\n"
         "SELECT JSON_OBJECT('id',CAST(id AS CHAR),'configId',CAST(config_id AS CHAR),'nameHex',HEX(name),'mime',type,'size',CAST(size AS CHAR),'key',path,'deleted',deleted+0) FROM infra_file WHERE id IN ("+','.join(sorted(IDS,key=int))+") ORDER BY id;\n"
         "SELECT JSON_OBJECT('id',CAST(id AS CHAR),'masterId',CAST(master_id AS CHAR),'sourceFileId',CAST(source_file_id AS CHAR),'tenantId',CAST(tenant_id AS CHAR),'sourceSha256',source_sha256,'versionNo',version_no,'deleted',deleted+0) FROM dcc_controlled_file WHERE id IN ("+','.join(v['id'] for v in versions.values())+") ORDER BY id;\n")
    shell='test -n "$MYSQL_ROOT_PASSWORD" || exit 3; export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"; exec mysql --user=root --database=ruoyi-vue-pro --default-character-set=utf8mb4 --batch --raw --skip-column-names'
    capture=subprocess.run([docker,'exec','-i','int-ruoyi-mysql','sh','-c',shell],input=sql.encode(),capture_output=True)
    if capture.returncode:raise ValueError('Readonly current metadata query failed')
    rows=[json.loads(l) for l in capture.stdout.decode().splitlines() if l]
    if len(rows)!=10 or rows[0].get('database')!='ruoyi-vue-pro' or rows[0].get('serverUuid')!='92ca05d0-aec8-11f1-a944-02b4e226a5ef' or rows[1].get('id')!='28' or rows[1].get('storage')!=20 or rows[1].get('deleted')!=0:raise ValueError('Actual source identity invalid')
    if {r['id'] for r in rows[6:]}!={r['id'] for r in source_versions} or {r['sourceFileId'] for r in rows[6:]}!=IDS or {r['id'] for r in rows[2:6]}!=IDS:raise ValueError('Fresh four exact identities incomplete')
    for row in rows[6:]:
        expected=versions.get(row.get('sourceFileId'))
        if expected is None or any(row.get(k)!=expected[k] for k in row):raise ValueError('Current source version drift')
    c=rows[1]['config']; config={'configId':'28','storage':20,'pathStyle':c['enablePathStyleAccess'],**{k:c[k] for k in ('endpoint','bucket','region','accessKey','accessSecret')}}
    candidates={'pdf':Path('C:/IntRuoyiAll-int_main/doc/tasks/20260918-dcc-void-e2e/obsolete-e2e-source.pdf'),'docx':Path('C:/IntRuoyiAll-int_main/doc/tasks/20260729-test-server-wangsiyu-file-upload-simulation/input/codex-upload-simulation-20260729.docx')}
    files=[];safe_files=[]
    for row in rows[2:6]:
        ident=row['id']; prior=old.get(ident)
        if not prior or any(row.get(k)!=prior[k] for k in ('configId','nameHex','mime','size','deleted')) or hashlib.sha256(row['key'].encode()).hexdigest()!=KEYS[ident]:raise ValueError('Exact source object drift')
        candidate=candidates['pdf' if row['mime']=='application/pdf' else 'docx']
        digest=versions[ident]['sourceSha256'].lower()
        if sha(candidate)!=digest or candidate.stat().st_size!=int(row['size']):raise ValueError('Local original body proof failed')
        files.append({'id':ident,'configId':'28','key':row['key'],'rootExpectedSha256':digest,'rootExpectedSize':row['size'],'mime':row['mime'],'candidatePath':str(candidate)})
        safe_files.append({'id':ident,'keySha256':KEYS[ident],'bodySha256':digest,'size':row['size'],'candidateSha256':sha(candidate)})
    args.output_directory.mkdir()
    receipt={'status':'PREPARED_NOT_EXECUTED','mode':args.mode,'source':rows[0],'files':safe_files,'uniqueKeys':3,'maximumPuts':3,'deliverySha256':sha(TASK/'g26-delivery-fingerprints.json'),'authorizationSha256':sha(args.authorization) if auth else None,'objectWritesAttempted':False,'databaseWrites':False,'credentialsPersisted':False}
    dest=args.output_directory/'receipt.json'
    dest.write_text(json.dumps(receipt,indent=2)+'\n',encoding='utf-8')
    if args.mode=='prepare':print(json.dumps({'status':receipt['status'],'files':4,'uniqueKeys':3,'objectWritesAttempted':False}));return 0
    payload={'authorizationReference':auth['sourceMessageReference'],'config':config,'files':files}
    log=args.output_directory/'logback-root-off.xml';log.write_text('<configuration><root level="OFF"/></configuration>\n',encoding='utf-8')
    cp=str(TASK/'g26-exact-object-recovery-runtime/classes')+';'+str(TASK/'g25-readonly-source-bytes-runtime/libs/*')+';'+str(MAIN/'g25-source-bytes-extra-runtime/reactive-streams-1.0.4.jar')
    receipt.update(status='RUNNING_ACTUAL_RECOVERY',objectWritesAttempted=True,startedAtUtc=datetime.now(timezone.utc).isoformat());dest.write_text(json.dumps(receipt,indent=2)+'\n',encoding='utf-8')
    result=subprocess.run([java,'-Dlogback.configurationFile='+str(log),'-cp',cp,'G26ExactObjectRecovery','--authorize-exact-local-object-recovery'],input=json.dumps(payload).encode(),capture_output=True)
    receipt.update(status='STOPPED_ACTUAL_OUTCOME_UNCERTAIN_PENDING_OUTPUT_VERIFICATION',exitCode=result.returncode,finishedAtUtc=datetime.now(timezone.utc).isoformat(),stdoutBytes=len(result.stdout),stdoutSha256=hashlib.sha256(result.stdout).hexdigest(),stderrBytes=len(result.stderr),stderrSha256=hashlib.sha256(result.stderr).hexdigest(),automaticRetryAllowed=False)
    dest.write_text(json.dumps(receipt,indent=2)+'\n',encoding='utf-8')
    if result.stderr:raise ValueError('Unexpected diagnostics; inspect only safe process facts')
    proofs=[json.loads(l) for l in result.stdout.decode().splitlines() if l]
    fields=set(delivery['outputFields']['recovery'])
    safe_enums={'RESTORED_VERIFIED','NOT_ATTEMPTED','PREFLIGHT_BLOCKED','PUT_REJECTED','PUT_OUTCOME_UNCERTAIN','CREATED_NOT_VERIFIED','FINAL_READ_ERROR','FINAL_BODY_MISMATCH','CLIENT_ERROR','LOCAL_PROOF_FAILED','INPUT_REJECTED'}
    error_codes={'AccessDenied','ALIAS_BODY_CONFLICT','BadDigest','CLIENT_LIFECYCLE_ERROR','ConditionalRequestConflict','EXISTING_DELETE_MARKER','FINAL_BODY_MISMATCH','FINAL_READ_ERROR','InternalError','INVALID_INPUT_OR_AUTHORIZATION','InvalidAccessKeyId','InvalidArgument','InvalidObjectState','InvalidRequest','LOCAL_BODY_MISMATCH','LOCAL_SOURCE_PROOF_ERROR','LOCAL_SOURCE_UNAVAILABLE','NoSuchBucket','NoSuchKey','NotImplemented','OBJECT_ALREADY_EXISTS','PreconditionFailed','PREFLIGHT_READ_ERROR','PUT_NON_SUCCESS','PUT_OUTCOME_UNCERTAIN','RequestTimeout','S3_ERROR','ServiceUnavailable','SignatureDoesNotMatch','SlowDown'}
    for row in proofs:
        if set(row)!=fields or row.get('status') not in safe_enums or row.get('id') not in IDS|{None}:raise ValueError('Recovery output schema invalid')
        for k in ('actualSha256',):
            if row[k] is not None and (not isinstance(row[k],str) or len(row[k])!=64 or any(ch not in 'abcdef0123456789' for ch in row[k])):raise ValueError('Unsafe digest output')
        if row['actualLength'] is not None and (type(row['actualLength']) is not int or row['actualLength']<0):raise ValueError('Unsafe size output')
        if row['errorCode'] is not None and row['errorCode'] not in error_codes:raise ValueError('Unsafe error output')
        if row['httpStatus'] is not None and (type(row['httpStatus']) is not int or not 100<=row['httpStatus']<=599):raise ValueError('Unsafe HTTP output')
        if type(row['putAttempted']) is not bool or type(row['putAccepted']) is not bool or (row['putAccepted'] and not row['putAttempted']):raise ValueError('Unsafe actual write flags')
        if row['phase'] not in {'INPUT','LOCAL_SOURCE','NONE','PREFLIGHT','PUT','FINAL_GET','LIFECYCLE'}:raise ValueError('Unsafe phase output')
    output=args.output_directory/'recovery-results.jsonl';output.write_text(''.join(json.dumps(r,separators=(',',':'))+'\n' for r in proofs),encoding='utf-8')
    complete=len(proofs)==4 and {r['id'] for r in proofs}==IDS and all(r['status']=='RESTORED_VERIFIED' and r['actualSha256']==versions[r['id']]['sourceSha256'].lower() and r['actualLength']==int(old[r['id']]['size']) and r['httpStatus']==200 and r['putAccepted'] is True for r in proofs)
    if (result.returncode==0)!=complete:raise ValueError('Exit/result success contradicted')
    receipt.update(status='RECOVERY_PASS' if complete else 'RECOVERY_FAILED_PRESERVE_ACTUAL_EFFECTS',exitCode=result.returncode,finishedAtUtc=datetime.now(timezone.utc).isoformat(),resultSha256=sha(output),acceptedUniqueKeys=len({KEYS[r['id']] for r in proofs if r['id'] in IDS and r['putAccepted'] is True}))
    dest.write_text(json.dumps(receipt,indent=2)+'\n',encoding='utf-8');print(json.dumps({'status':receipt['status'],'acceptedUniqueKeys':receipt['acceptedUniqueKeys'],'exitCode':result.returncode}));return result.returncode

if __name__=='__main__':
    try:raise SystemExit(main())
    except Exception as error:
        print(json.dumps({'status':'RECOVERY_RUNNER_STOPPED','errorType':type(error).__name__}));raise SystemExit(2) from None
