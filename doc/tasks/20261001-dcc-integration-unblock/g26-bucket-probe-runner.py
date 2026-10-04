"""Root-only local bucket settings read; input credentials stay in memory/stdin."""
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
from datetime import datetime, timezone


TASK = Path('C:/IntRuoyi/20261001-dcc-integration/doc/tasks/20261002-dcc-public-backend-completion')
EXTRA = Path(__file__).resolve().parent/'g25-source-bytes-extra-runtime'
OUTPUT = Path('C:/IntRuoyiBackups/20261003-dcc-integration/g26-bucket-policy-probe')


def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()


def main():
    if OUTPUT.exists(): raise ValueError('Never overwrite evidence')
    delivery = json.loads((TASK/'g26-delivery-fingerprints.json').read_text(encoding='utf-8'))
    integration = TASK.parents[2]
    for asset in delivery['assets']+delivery['compiledClasses']:
        p = (integration/asset['path']).resolve()
        if not p.is_relative_to(TASK.resolve()) or sha(p) != asset['sha256']:
            raise ValueError('G26 source/class drift')
    classes = TASK/'g26-exact-object-recovery-runtime/classes'
    sealed = {(integration/a['path']).resolve() for a in delivery['compiledClasses']}
    if {p.resolve() for p in classes.iterdir()} != sealed: raise ValueError('Class inventory drift')
    g25 = json.loads((TASK/'g25-readonly-source-bytes-runtime/extraction-receipt.json').read_text())
    libs = TASK/'g25-readonly-source-bytes-runtime/libs'
    if {p.name for p in libs.iterdir()} != {a['name'] for a in g25['libs']}:
        raise ValueError('Dependency inventory drift')
    for a in g25['libs']:
        if sha(libs/a['name']) != a['sha256']: raise ValueError('Dependency bytes drift')
    extra = json.loads((EXTRA/'receipt.json').read_text())
    extra_jar = Path(extra['path'])
    if not extra_jar.resolve().is_relative_to(EXTRA.resolve()) or sha(extra_jar) != extra['sha256']:
        raise ValueError('Extra official dependency drift')
    docker, java = shutil.which('docker.exe'), shutil.which('java.exe')
    if not docker or not java: raise ValueError('Existing runtime required')
    sql = "SELECT JSON_OBJECT('database',DATABASE(),'serverUuid',@@server_uuid,'collectedAtUtc',UTC_TIMESTAMP(6));\nSELECT JSON_OBJECT('id',CAST(id AS CHAR),'storage',storage,'deleted',deleted+0,'config',CAST(config AS JSON)) FROM infra_file_config WHERE id=28;\n"
    shell = 'test -n "$MYSQL_ROOT_PASSWORD" || exit 3; export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"; exec mysql --user=root --database=ruoyi-vue-pro --default-character-set=utf8mb4 --batch --raw --skip-column-names'
    result = subprocess.run([docker,'exec','-i','int-ruoyi-mysql','sh','-c',shell],input=sql.encode(),capture_output=True)
    if result.returncode: raise ValueError('Actual readonly configuration query failed')
    rows = [json.loads(l) for l in result.stdout.decode().splitlines() if l]
    if len(rows) != 2 or rows[0].get('database') != 'ruoyi-vue-pro' or rows[0].get('serverUuid') != '92ca05d0-aec8-11f1-a944-02b4e226a5ef' or rows[1].get('id') != '28' or rows[1].get('storage') != 20 or rows[1].get('deleted') != 0:
        raise ValueError('Actual config identity invalid')
    c = rows[1]['config']
    payload = {'config': {'configId':'28','storage':20,'pathStyle':c['enablePathStyleAccess'],**{k:c[k] for k in ('endpoint','bucket','region','accessKey','accessSecret')}}}
    OUTPUT.mkdir()
    log = OUTPUT/'logback-root-off.xml'
    log.write_text('<configuration><root level="OFF"/></configuration>\n',encoding='utf-8')
    started = datetime.now(timezone.utc).isoformat()
    cp = str(classes)+';'+str(libs/'*')+';'+str(extra_jar)
    read = subprocess.run([java,'-Dlogback.configurationFile='+str(log),'-cp',cp,'G26ReadOnlyBucketProbe'],input=json.dumps(payload).encode(),capture_output=True)
    if read.stderr: raise ValueError('Unexpected Java diagnostics; raw input/config not persisted')
    report = json.loads(read.stdout.decode())
    fields = {'bucketSha256','status','versioningStatus','mfaDeleteStatus','versioningErrorCode','objectLockStatus','retentionMode','objectLockErrorCode','lifecycleErrorCode','versioningHttpStatus','retentionDays','retentionYears','objectLockHttpStatus'}
    if set(report) != fields: raise ValueError('Probe output schema invalid')
    enum = {'status':{'READONLY_POLICY_CAPTURED','PROBE_FAILED','INPUT_REJECTED'},'versioningStatus':{'Enabled','Suspended','UNSET'},'mfaDeleteStatus':{'Enabled','Disabled','UNSET'},'objectLockStatus':{'Enabled','NOT_CONFIGURED'},'retentionMode':{'COMPLIANCE','GOVERNANCE'}}
    errors = {'ObjectLockConfigurationNotFoundError','NoSuchObjectLockConfiguration','NoSuchBucket','AccessDenied','InvalidAccessKeyId','SignatureDoesNotMatch','InvalidRequest','NotImplemented','InternalError','ServiceUnavailable','RequestTimeout','S3_ERROR','VERSIONING_READ_OR_RESPONSE_ERROR','OBJECT_LOCK_READ_OR_RESPONSE_ERROR','CLIENT_LIFECYCLE_ERROR','INVALID_INPUT'}
    for k,allowed in enum.items():
        if report[k] is not None and report[k] not in allowed: raise ValueError('Unsafe probe enum')
    for k in ('versioningErrorCode','objectLockErrorCode','lifecycleErrorCode'):
        if report[k] is not None and report[k] not in errors: raise ValueError('Unsafe probe error code')
    for k in ('versioningHttpStatus','objectLockHttpStatus'):
        if report[k] is not None and (type(report[k]) is not int or not 100<=report[k]<=599): raise ValueError('Unsafe HTTP status')
    for k in ('retentionDays','retentionYears'):
        if report[k] is not None and (type(report[k]) is not int or report[k]<=0): raise ValueError('Unsafe retention value')
    if report['bucketSha256'] != 'eef43e6566706fff3d910f5ea7220e06c51ad4bbcd34aa71e8c863ca7cece381': raise ValueError('Wrong bucket proof')
    safe = OUTPUT/'bucket-policy.json'; safe.write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    receipt = {'status':report['status'],'exitCode':read.returncode,'startedAtUtc':started,'finishedAtUtc':datetime.now(timezone.utc).isoformat(),'source':rows[0],'databaseWrites':False,'objectWrites':False,'objectBodyRead':False,'credentialsPersisted':False,'deliverySha256':sha(TASK/'g26-delivery-fingerprints.json'),'resultSha256':sha(safe)}
    (OUTPUT/'receipt.json').write_text(json.dumps(receipt,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(report))
    return read.returncode


if __name__ == '__main__':
    try: raise SystemExit(main())
    except Exception as e:
        print(json.dumps({'status':'READONLY_BUCKET_PROBE_REJECTED','errorType':type(e).__name__}))
        raise SystemExit(2) from None
