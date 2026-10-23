"""Root-only read-only 39-object proof. Credentials stay in memory/stdin; no body persistence."""
import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import re
import shutil
import subprocess
from urllib.parse import urlsplit


def positive_long(value):
    if not isinstance(value, str) or not re.fullmatch(r'[1-9][0-9]*', value) or int(value) > 9223372036854775807:
        raise ValueError('Exact positive Long string required')
    return value


def expectations(facts):
    runtime = [r for r in facts if r.get('kind') == 'runtime']
    if len(runtime) != 1 or runtime[0].get('database') != 'ruoyi-vue-pro' or runtime[0].get('tenantId') != '1':
        raise ValueError('Sealed source runtime is required')
    versions = [r for r in facts if r.get('kind') == 'version']
    storage = [r for r in facts if r.get('kind') == 'storage']
    if len(versions) != 39 or len(storage) != 39:
        raise ValueError('Exact 39-version and 39-storage evidence required')
    expected = {}
    for row in versions:
        ident = positive_long(row.get('sourceFileId'))
        sha = row.get('sourceSha256')
        if row.get('tenantId') != '1' or row.get('deleted') != 0 or not isinstance(sha, str) or not re.fullmatch(r'[0-9a-fA-F]{64}', sha):
            raise ValueError('Source tenant/hash/deletion evidence invalid')
        if ident in expected:
            raise ValueError('Source IDs must be unique in this sealed 39-object bundle')
        expected[ident] = {'sha256': sha.lower(), 'version': dict(row)}
    seen = set()
    for row in storage:
        ident = positive_long(row.get('id'))
        if ident not in expected or ident in seen or row.get('configId') != '28' or row.get('deleted') != 0:
            raise ValueError('Source storage identity invalid')
        size = row.get('size')
        if not isinstance(size, str) or not re.fullmatch(r'0|[1-9][0-9]*', size) or int(size) > 9223372036854775807:
            raise ValueError('Storage length must be exact Long string')
        name = row.get('name')
        if not isinstance(name, str) or not name or row.get('nameHex') != name.encode('utf-8').hex().upper():
            raise ValueError('Storage name bytes disagree')
        expected[ident].update(size=size, nameHex=row['nameHex'], configId='28')
        seen.add(ident)
    if seen != set(expected):
        raise ValueError('Storage coverage incomplete')
    return expected, {'database': runtime[0]['database'], 'serverUuid': runtime[0]['serverUuid']}


def reader_payload(expected, identity, fresh):
    runtimes = [r for r in fresh if r.get('kind') == 'runtime']
    configs = [r for r in fresh if r.get('kind') == 'config']
    rows = [r for r in fresh if r.get('kind') == 'storage']
    versions = [r for r in fresh if r.get('kind') == 'version']
    if len(fresh) != 80 or len(runtimes) != 1 or len(configs) != 1 or len(rows) != 39:
        raise ValueError('Fresh collector requires exact runtime/config/39 metadata')
    sealed_versions = {e['version']['id']: e['version'] for e in expected.values()}
    if len(versions) != 39 or len({r.get('id') for r in versions}) != 39:
        raise ValueError('Fresh version coverage invalid')
    for row in versions:
        if row.get('id') not in sealed_versions or row != sealed_versions[row['id']]:
            raise ValueError('Fresh original version identity/hash drift')
    if any(runtimes[0].get(k) != v for k, v in identity.items()):
        raise ValueError('Fresh source identity differs')
    config = configs[0]
    if config.get('id') != '28' or config.get('storage') != 20 or config.get('deleted') != 0 or not isinstance(config.get('config'), dict):
        raise ValueError('Fresh storage config identity invalid')
    c = config['config']
    endpoint = c.get('endpoint')
    if not isinstance(endpoint, str): raise ValueError('Explicit local endpoint required')
    parsed = urlsplit(endpoint)
    if parsed.scheme not in ('http', 'https') or parsed.hostname not in ('127.0.0.1', 'localhost') or parsed.username or parsed.password or parsed.query or parsed.fragment or parsed.path not in ('', '/'):
        raise ValueError('Only explicit loopback endpoint permitted')
    try: parsed.port
    except ValueError: raise ValueError('Endpoint port invalid') from None
    if c.get('enablePathStyleAccess') is not True:
        raise ValueError('Actual path-style configuration required')
    for field in ('bucket', 'region', 'accessKey', 'accessSecret'):
        if not isinstance(c.get(field), str) or not c[field].strip():
            raise ValueError('Actual explicit storage configuration incomplete')
    mapped = {}
    for row in rows:
        ident = positive_long(row.get('id'))
        if ident not in expected or ident in mapped or row.get('deleted') != 0:
            raise ValueError('Fresh metadata identity invalid')
        if any(row.get(k) != expected[ident][k] for k in ('configId', 'size', 'nameHex')):
            raise ValueError('Fresh metadata drift from sealed evidence')
        if not isinstance(row.get('key'), str) or not row['key']:
            raise ValueError('Actual object key missing')
        mapped[ident] = {'id': ident, 'configId': '28', 'key': row['key'],
                         'rootExpectedSha256': expected[ident]['sha256'], 'rootExpectedSize': expected[ident]['size']}
    if set(mapped) != set(expected): raise ValueError('Fresh metadata coverage incomplete')
    return {'config': {'configId': '28', 'storage': 20, 'pathStyle': True,
                       **{field: c[field] for field in ('endpoint', 'bucket', 'region', 'accessKey', 'accessSecret')}},
            'files': [mapped[i] for i in sorted(mapped, key=int)]}


def verified_results(expected, rows, exit_code):
    fields = {'id', 'status', 'actualSha256', 'actualLength', 'httpStatus', 'errorCode'}
    http_errors = {'NoSuchKey', 'NoSuchBucket', 'AccessDenied', 'InvalidAccessKeyId', 'SignatureDoesNotMatch',
                   'AuthorizationHeaderMalformed', 'InvalidObjectState', 'RequestTimeout', 'SlowDown',
                   'InternalError', 'ServiceUnavailable', 'S3_ERROR'}
    seen = set(); all_match = True
    for row in rows:
        if set(row) != fields or row.get('id') not in expected or row['id'] in seen:
            raise ValueError('Reader output identity/schema invalid')
        seen.add(row['id'])
        digest, length, http, code = (row[k] for k in ('actualSha256', 'actualLength', 'httpStatus', 'errorCode'))
        if digest is not None and (not isinstance(digest, str) or not re.fullmatch(r'[a-f0-9]{64}', digest)):
            raise ValueError('Unsafe output digest')
        if length is not None and (type(length) is not int or not 0 <= length <= 9223372036854775807):
            raise ValueError('Unsafe output length')
        if http is not None and (type(http) is not int or not 100 <= http <= 599):
            raise ValueError('Unsafe output HTTP status')
        if row['status'] == 'MATCH':
            e = expected[row['id']]
            if row['actualSha256'] != e['sha256'] or type(row['actualLength']) is not int or row['actualLength'] != int(e['size']) or row['httpStatus'] != 200 or row['errorCode'] is not None:
                raise ValueError('Reader MATCH contradicted by actual proof')
        elif row['status'] == 'MISMATCH':
            if digest is None or length is None or http != 200 or code is not None:
                raise ValueError('Invalid mismatch evidence')
            if digest == expected[row['id']]['sha256'] and length == int(expected[row['id']]['size']):
                raise ValueError('Mismatch contradicts actual metrics')
            all_match = False
        elif row['status'] == 'HTTP_ERROR':
            if digest is not None or length is not None or http is None or code not in http_errors:
                raise ValueError('Unsafe HTTP failure output')
            all_match = False
        elif row['status'] == 'READ_ERROR':
            if digest is not None or http is not None or code not in {'SDK_CLIENT_ERROR', 'IO_ERROR', 'READER_ERROR'}:
                raise ValueError('Unsafe read failure output')
            all_match = False
        else: raise ValueError('Reader status invalid')
    if seen != set(expected) or exit_code not in (0, 1) or (exit_code == 0) != all_match:
        raise ValueError('Reader aggregate status/exit/coverage invalid')
    return all_match


def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--facts', required=True, type=Path)
    parser.add_argument('--reader-task', required=True, type=Path)
    parser.add_argument('--output-directory', required=True, type=Path)
    args = parser.parse_args()
    if args.output_directory.exists(): raise ValueError('Never overwrite proof output')
    receipt = json.loads(args.facts.with_suffix(args.facts.suffix+'.receipt.json').read_text(encoding='utf-8'))
    if receipt.get('databaseWrites') is not False or receipt.get('factsSha256') != sha(args.facts):
        raise ValueError('Sealed facts receipt invalid')
    facts = [json.loads(line) for line in args.facts.read_text(encoding='utf-8').splitlines() if line]
    expected, identity = expectations(facts)
    manifest = json.loads((args.reader_task/'g25-readonly-source-bytes-delivery-fingerprints.json').read_text(encoding='utf-8'))
    integration = args.reader_task.parents[2]
    for asset in manifest['sourceAssets'] + manifest['temporaryCompiledClasses']:
        path = (integration/(asset.get('file') or asset['path'])).resolve()
        if not path.is_relative_to(args.reader_task.resolve()):
            raise ValueError('Reader asset escaped its task')
        if not path.is_file() or sha(path) != asset['sha256']:
            raise ValueError('Reader code/class fingerprint drift')
    runtime = args.reader_task/'g25-readonly-source-bytes-runtime'
    extraction = json.loads((runtime/'extraction-receipt.json').read_text(encoding='utf-8'))
    # Exact dependency filenames and raw hashes were sealed by the reader owner.
    if sha(runtime/'extraction-receipt.json') != manifest['libsExtractionReceiptSha256']:
        raise ValueError('Dependency extraction receipt drift')
    expected_classes = {(integration/a['path']).resolve() for a in manifest['temporaryCompiledClasses']}
    if {p.resolve() for p in (runtime/'classes').iterdir()} != expected_classes:
        raise ValueError('Reader class directory differs from exact sealed inventory')
    libs = extraction['libs']
    if len(libs) != 41 or {p.name for p in (runtime/'libs').iterdir()} != {a['name'] for a in libs}:
        raise ValueError('Reader dependency directory differs from exact sealed inventory')
    for item in libs:
        if Path(item['name']).name != item['name']:
            raise ValueError('Dependency filename invalid')
        path = runtime/'libs'/item['name']
        if not path.is_file() or sha(path) != item['sha256']:
            raise ValueError('Reader library fingerprint drift')
    ids = ','.join(sorted(expected, key=int))
    version_ids = ','.join(sorted((e['version']['id'] for e in expected.values()), key=int))
    sql = ("SELECT JSON_OBJECT('kind','runtime','database',DATABASE(),'serverUuid',@@server_uuid,'collectedAtUtc',UTC_TIMESTAMP(6));\n"
           "SELECT JSON_OBJECT('kind','config','id',CAST(id AS CHAR),'storage',storage,'deleted',deleted+0,'config',CAST(config AS JSON)) FROM infra_file_config WHERE id=28;\n"
           "SELECT JSON_OBJECT('kind','storage','id',CAST(id AS CHAR),'configId',CAST(config_id AS CHAR),'nameHex',HEX(name),'size',CAST(size AS CHAR),'key',path,'deleted',deleted+0) FROM infra_file WHERE id IN ("+ids+") ORDER BY id;\n")
    sql += ("SELECT JSON_OBJECT('kind','version','id',CAST(v.id AS CHAR),'tenantId',CAST(v.tenant_id AS CHAR),'masterId',CAST(v.master_id AS CHAR),'sourceFileId',CAST(v.source_file_id AS CHAR),'originalFileId',CAST(v.original_file_id AS CHAR),'sourceSha256',v.source_sha256,'projectId',CAST(v.dcc_project_code_id AS CHAR),'leafId',CAST(v.file_type_taxonomy_id AS CHAR),'fileNumber',v.file_number,'versionNo',v.version_no,'status',v.status,'processInstanceId',CAST(v.process_instance_id AS CHAR),'predecessorId',CAST(v.predecessor_controlled_file_id AS CHAR),'deleted',v.deleted+0) FROM dcc_controlled_file v WHERE v.id IN ("+version_ids+") ORDER BY v.id;\n")
    docker = shutil.which('docker.exe'); java = shutil.which('java.exe')
    if not docker or not java: raise RuntimeError('Existing Docker CLI and JDK required')
    shell = 'test -n "$MYSQL_ROOT_PASSWORD" || exit 3; export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"; exec mysql --user=root --database=ruoyi-vue-pro --default-character-set=utf8mb4 --batch --raw --skip-column-names'
    started = datetime.now(timezone.utc).isoformat()
    capture = subprocess.run([docker, 'exec', '-i', 'int-ruoyi-mysql', 'sh', '-c', shell], input=sql.encode(), capture_output=True)
    if capture.returncode: raise RuntimeError('Actual read-only metadata query failed')
    fresh = [json.loads(line) for line in capture.stdout.decode('utf-8').splitlines() if line]
    payload = reader_payload(expected, identity, fresh)
    args.output_directory.mkdir(parents=True)
    log_config = args.output_directory/'logback-root-off.xml'
    log_config.write_text('<configuration><root level="OFF"/></configuration>\n', encoding='utf-8')
    extra = Path(__file__).resolve().parent/'g25-source-bytes-extra-runtime'
    extra_receipt = json.loads((extra/'receipt.json').read_text(encoding='utf-8'))
    extra_jar = Path(extra_receipt['path']).resolve()
    if not extra_jar.is_relative_to(extra.resolve()) or sha(extra_jar) != extra_receipt['sha256'] or extra_receipt['sourceJarSha256'] != manifest['jarSha256']:
        raise ValueError('Extra official synchronous client dependency drift')
    cp = str(runtime/'classes')+';'+str(runtime/'libs'/'*')+';'+str(extra_jar)
    read = subprocess.run([java, '-Dlogback.configurationFile='+str(log_config), '-cp', cp, 'G25ReadOnlySourceBytes'],
                          input=json.dumps(payload, ensure_ascii=False).encode('utf-8'), capture_output=True)
    # No captured config, stdin payload, keys, bucket or body is ever written.
    rows = [json.loads(line) for line in read.stdout.decode('utf-8').splitlines() if line]
    valid = verified_results(expected, rows, read.returncode)
    if read.stderr: raise RuntimeError('Reader unexpectedly emitted stderr; no secret-bearing diagnostics persisted')
    output = args.output_directory/'source-bytes-results.jsonl'
    output.write_text(''.join(json.dumps(row, separators=(',', ':'))+'\n' for row in rows), encoding='utf-8')
    proof = {'status': 'SOURCE_BYTES_VERIFIED' if valid else 'SOURCE_BYTES_NOT_VERIFIED',
             'sourceBytesVerified': valid, 'databaseWrites': False, 'objectWrites': False, 'bodyPersisted': False,
             'credentialsPersisted': False, 'source': identity, 'metadataCapturedAtUtc': fresh[0]['collectedAtUtc'],
             'startedAtUtc': started, 'finishedAtUtc': datetime.now(timezone.utc).isoformat(),
             'sourceFactsSha256': sha(args.facts), 'metadataSelectSha256': hashlib.sha256(sql.encode()).hexdigest(),
             'readerManifestSha256': sha(args.reader_task/'g25-readonly-source-bytes-delivery-fingerprints.json'),
             'readerExitCode': read.returncode, 'objectsRead': len(rows), 'matches': sum(r['status']=='MATCH' for r in rows),
             'resultsSha256': sha(output), 'resultsPath': str(output.resolve())}
    (args.output_directory/'receipt.json').write_text(json.dumps(proof, indent=2)+'\n', encoding='utf-8')
    print(json.dumps({k: proof[k] for k in ('status', 'objectsRead', 'matches', 'databaseWrites', 'objectWrites')}))
    return 0 if valid else 1


if __name__ == '__main__':
    try: raise SystemExit(main())
    except Exception as error:
        print(json.dumps({'status': 'READONLY_PROOF_FAILED', 'errorType': type(error).__name__}))
        raise SystemExit(2) from None
