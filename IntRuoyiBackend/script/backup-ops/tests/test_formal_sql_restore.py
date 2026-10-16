"""Bound SQL files and explicit target/process doubles; no application proof."""
import gzip
import hashlib
import importlib.util
import json
import sys
from pathlib import Path
from unittest.mock import patch

import pytest
from test_formal_data_evidence import contract_observation

LINUX = Path(__file__).parents[1] / 'linux'
sys.path.insert(0, str(LINUX))


def load():
    spec = importlib.util.spec_from_file_location('formal_sql_restore', LINUX / 'formal_sql_restore.py')
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def fixture(tmp_path):
    from formal_data_evidence import source_binding
    from formal_snapshot_linux import _canonical_bytes
    capture = tmp_path / 'capture'
    capture.mkdir()
    sql = capture / 'database.sql.gz'
    sql.write_bytes(gzip.compress(b'CREATE TABLE proof(id INT); INSERT INTO proof VALUES (7);\n', mtime=0))
    sql.chmod(0o600)
    payload = {'state': 'CAPTURED_PENDING_FULL_BARRIER_PROOF',
               'database': {'sha256': hashlib.sha256(sql.read_bytes()).hexdigest(), 'bytes': sql.stat().st_size}}
    receipt = capture / '040-payload.json'
    receipt.write_bytes(json.dumps(payload).encode()); receipt.chmod(0o600)
    binding = tmp_path/'binding.json'
    binding.write_bytes(json.dumps({'operationId':'owned-capture','sourceFingerprint':'e'*64,
        'databaseUuid':'0fa6c22b-5a90-11f1-ade1-0242ac150002','minioContainerId':'f'*64}).encode())
    binding.chmod(0o600)
    proof=contract_observation('d'*64,'0fa6c22b-5a90-11f1-ade1-0242ac150002')
    bound=source_binding(binding.read_bytes(),proof,proof,payload['database']['sha256'])
    for name,value in [('011-source-data.json',proof),('012-source-data-after.json',proof),
                       ('015-source-data-binding.json',bound)]:
        (capture/name).write_bytes(_canonical_bytes(value));(capture/name).chmod(0o600)
    payload['sourceDataEvidence']={'bindingSha256':hashlib.sha256(_canonical_bytes(bound)).hexdigest(),
                                  'sourceObservationSha256':proof['observationSha256']}
    receipt.write_bytes(_canonical_bytes(payload))
    credential = tmp_path / 'mysql.secret'
    credential.write_bytes(b'owned-private-secret\n'); credential.chmod(0o600)
    request = {'schemaVersion': 'release-formal-sql-restore-request/v1', 'operationId': 'owned-sql-restore',
               'sourceCapture': str(capture), 'sourceCaptureReceiptSha256': hashlib.sha256(receipt.read_bytes()).hexdigest(),
               'sourceSqlSha256': payload['database']['sha256'],
               'sourceDatabaseUuid': '0fa6c22b-5a90-11f1-ade1-0242ac150002',
               'sourceClaimBindingSha256': hashlib.sha256(binding.read_bytes()).hexdigest(),
               'evidenceRoot': str(tmp_path / 'evidence'), 'maxExpandedBytes': 4096, 'timeoutSeconds': 10,
               'dataLimits':{'max_rows':1000,'max_nodes':10000,'max_bytes':1024*1024,'seconds':30},
               'target': {'containerId': 'a'*64, 'imageId': 'sha256:'+'b'*64, 'database': 'owned_probe',
                          'databaseUuid': '72490a64-bbb7-11f1-8379-0242ac1ff302',
                          'credentialFile': str(credential), 'networkId': 'none', 'dataVolume': None}}
    path = tmp_path / 'request.json'
    path.write_bytes(json.dumps(request).encode()); path.chmod(0o600)
    row = {'Id': 'a'*64, 'Image': 'sha256:'+'b'*64, 'State': {'Running': True, 'Paused': False, 'Restarting': False},
           'Config': {'Labels': {'intruoyi.formal-sql-restore-operation': 'owned-sql-restore'}},
           'HostConfig': {'Privileged': False, 'NetworkMode': 'none', 'PortBindings': {},
                          'Tmpfs': {'/var/lib/mysql':'rw,size=512m'}},
           'Mounts': [],
           'NetworkSettings': {'Networks': {'none': {'IPAddress':'','GlobalIPv6Address':'', 'Gateway':'',
                              'IPv6Gateway':'', 'NetworkID':'c'*64}}}}
    return request, path, row


class Mysql:
    def __init__(self, target): self.target = target; self.imported = False; self.nonempty = False
    def query(self, sql):
        if '@@server_uuid' in sql: return self.target['databaseUuid']+'\t0\t0\t0\tOFF\n'
        if 'information_schema.routines' in sql: return '0\t0\t0\n'
        if sql == 'SHOW DATABASES;': return 'information_schema\nmysql\nperformance_schema\nsys\nowned_probe\n'
        if sql.startswith('SHOW FULL TABLES'): return 'proof\tBASE TABLE\n' if self.imported or self.nonempty else ''
        if sql.startswith('SELECT COUNT'): return '1\n'
        raise AssertionError(sql)


@pytest.mark.parametrize('case', ['pass', 'request', 'capture', 'sql', 'same-uuid', 'label', 'image',
    'privileged', 'mount', 'network', 'none-ip', 'ports', 'nonempty', 'routines', 'scheduler', 'binding',
    'oversize', 'import-failed', 'request-after-import', 'repeat', 'source-data-missing',
    'source-data-changed', 'source-data-after-import', 'restored-data-mismatch', 'target-proof-identity',
    'limits-missing', 'limits-invalid', 'target-file-drift', 'comparison-file-drift'])
def test_standard_sql_restore_is_bound_and_never_replayed(tmp_path, capsys, case):
    m = load(); request, path, row = fixture(tmp_path)
    if case == 'same-uuid': request['target']['databaseUuid'] = request['sourceDatabaseUuid']
    if case == 'oversize': request['maxExpandedBytes'] = 5
    if case == 'limits-missing': request.pop('dataLimits')
    if case == 'limits-invalid': request['dataLimits']['seconds']=0
    path.write_bytes(json.dumps(request).encode())
    digest = hashlib.sha256(path.read_bytes()).hexdigest()
    if case == 'request': path.write_bytes(b'{}')
    if case == 'capture': (tmp_path/'capture/040-payload.json').write_bytes(b'{}')
    if case == 'sql': (tmp_path/'capture/database.sql.gz').write_bytes(gzip.compress(b'ALTERED'))
    if case == 'label': row['Config']['Labels'] = {}
    if case == 'image': row['Image'] = 'sha256:'+'d'*64
    if case == 'privileged': row['HostConfig']['Privileged'] = True
    if case == 'mount': row['Mounts'] = [{'Type': 'bind', 'Source': '/production', 'Destination': '/var/lib/mysql'}]
    if case == 'network': row['HostConfig']['NetworkMode'] = 'host'
    if case == 'none-ip': row['NetworkSettings']['Networks']['none']['IPAddress'] = '172.18.0.2'
    if case == 'ports': row['HostConfig']['PortBindings'] = {'3306/tcp':[{'HostPort':'3306'}]}
    if case == 'binding': (tmp_path/'binding.json').write_bytes(b'{}')
    if case == 'source-data-missing': (tmp_path/'capture/011-source-data.json').unlink()
    if case == 'source-data-changed': (tmp_path/'capture/011-source-data.json').write_bytes(b'{}')
    mysql = Mysql(request['target']); mysql.nonempty = case == 'nonempty'
    if case == 'routines':
        original=mysql.query
        mysql.query=lambda sql: '1\t0\t0\n' if 'information_schema.routines' in sql else original(sql)
    if case == 'scheduler':
        original=mysql.query
        mysql.query=lambda sql: original(sql).replace('\tOFF\n','\tON\n')
    def importer(client, expanded, checksum, seconds, *, database, guard):
        assert database == 'owned_probe'
        assert callable(guard)
        assert expanded.read_bytes().startswith(b'CREATE TABLE proof')
        assert hashlib.sha256(expanded.read_bytes()).hexdigest() == checksum
        assert (tmp_path/'evidence/010-import-intent.json').is_file()
        mysql.imported = True
        if case == 'import-failed': raise m.FormalSnapshotError('FORMAL_SQL_IMPORT_FAILED')
        if case == 'request-after-import': path.write_bytes(b'{}')
        if case == 'source-data-after-import': (tmp_path/'capture/011-source-data.json').write_bytes(b'{}')
    def observe(client,database,uuid,*,guard,scratch,**limits):
        guard();assert mysql.imported;guard()
        return contract_observation('c'*64 if case=='target-proof-identity' else request['target']['containerId'],uuid,
            value='changed' if case=='restored-data-mismatch' else 'original')
    record=m.private_record
    def drift_record(root,name,value):
        record(root,name,value)
        if name=={'target-file-drift':'020-target-data.json','comparison-file-drift':'030-data-comparison.json'}.get(case):
            (root/name).write_bytes(b'{}')
    with patch.object(m, 'require_native_runtime'), patch.object(m, '_private_docker_json', return_value=row), \
         patch.object(m, 'IsolatedTargetMySql', return_value=mysql), patch.object(m, 'stream_import', side_effect=importer) as imported, \
         patch.object(m, 'private_record', side_effect=drift_record), \
         patch.object(m, 'observe_data', side_effect=observe,create=True):
        result = m.main(['--request', str(path), '--expected-request-sha256', digest])
        assert result == (0 if case in ('pass','repeat') else 1)
        if case == 'repeat':
            saved = (tmp_path/'evidence/restore-receipt.json').read_bytes()
            assert m.main(['--request', str(path), '--expected-request-sha256', digest]) == 1
            assert (tmp_path/'evidence/restore-receipt.json').read_bytes() == saved
            imported.assert_called_once()
        elif case not in ('pass', 'import-failed', 'request-after-import', 'source-data-after-import',
                          'restored-data-mismatch', 'target-proof-identity', 'target-file-drift', 'comparison-file-drift'):
            imported.assert_not_called()
        if result == 0:
            receipt = json.loads((tmp_path/'evidence/restore-receipt.json').read_bytes())
            assert receipt['state'] == 'SQL_RESTORED_PENDING_APPLICATION_PROOF'
            assert receipt['targetDatabaseUuid'] == request['target']['databaseUuid']
            assert receipt['sourceSqlSha256'] == request['sourceSqlSha256']
            assert {'name':'probe', 'type':'BASE TABLE', 'rows':1} in receipt['tables']
            assert receipt['fullSchemaDataEquivalenceVerified'] is True
            assert receipt['schemaComparisonEncoding']=='mysql8-catalog-bound-ddl/v1'
            assert receipt['sourceSchemaSha256']==receipt['schemaSha256']
            assert len(receipt['targetSchemaSha256'])==len(receipt['canonicalSchemaSha256'])==64
            assert receipt['applicationVerified'] is False
            for name,key in [('020-target-data.json','targetDataFileSha256'),('030-data-comparison.json','dataComparisonFileSha256')]:
                assert hashlib.sha256((tmp_path/'evidence'/name).read_bytes()).hexdigest()==receipt[key]
            assert receipt['productionEvidence'] is False
        else:
            assert not (tmp_path/'evidence/restore-receipt.json').exists()
            if case in ('source-data-after-import','restored-data-mismatch','target-proof-identity',
                        'target-file-drift','comparison-file-drift'):
                failed=json.loads((tmp_path/'evidence/900-failed.json').read_bytes())
                assert failed['stage']=='READBACK' and failed['targetReconciliationRequired'] is True
                assert failed['code']=={'source-data-after-import':'FORMAL_DATA_SOURCE_CHANGED',
                    'restored-data-mismatch':'FORMAL_DATA_MISMATCH','target-proof-identity':'FORMAL_SQL_TARGET_CHANGED',
                    'target-file-drift':'FORMAL_SQL_TARGET_EVIDENCE_CHANGED',
                    'comparison-file-drift':'FORMAL_SQL_TARGET_EVIDENCE_CHANGED'}[case]
                imported.assert_called_once()
        output=capsys.readouterr().out
        assert 'owned-private-secret' not in output
        if case in ('source-data-missing','source-data-changed'):
            assert json.loads(output)['code']=='FORMAL_DATA_SOURCE_CHANGED'


def test_stream_import_deadline_includes_stalled_stdin(tmp_path):
    m = load()
    class Client:
        _password = 'stream-secret'
        def _command(self, program, arguments):
            assert program == 'timeout --signal=TERM --kill-after=5 1s mysql'
            assert '--binary-mode=1' in arguments and 'stream-secret' not in repr(arguments)
            return [sys.executable, '-I', '-c', 'import time; time.sleep(30)']
    source = tmp_path/'expanded.sql'; source.write_bytes(b'X'*(1024*1024))
    with pytest.raises(m.FormalSnapshotError, match='FORMAL_SQL_IMPORT_TIMEOUT'):
        m.stream_import(Client(), source, hashlib.sha256(source.read_bytes()).hexdigest(), 1, database='owned_probe', guard=lambda: None)


def test_target_credentials_cannot_redirect_the_mysql_socket():
    m = load(); secret='owned-password-not-in-argv'
    args=m.IsolatedTargetMySql('a'*64,secret)._command('mysql',['-uroot'])
    assert secret not in repr(args)
    assert '--no-defaults' in args and '--protocol=SOCKET' in args
    assert 'MYSQL_TEST_LOGIN_FILE' in args[6] and '.mylogin.cnf' in args[6]


def test_native_stream_import_explicit_utf8mb4_preserves_unicode_bytes(tmp_path):
    m=load();raw="SELECT '迁移甲😀';\n".encode('utf-8')
    source=tmp_path/'expanded.sql';source.write_bytes(raw)
    class Client:
        _password='private-import-sentinel'
        def _command(self,program,arguments):
            assert '--default-character-set=utf8mb4' in arguments
            assert self._password not in repr(arguments)
            expected=(self._password+'\n').encode()+raw
            code='import sys; assert sys.stdin.buffer.read() == bytes.fromhex('+repr(expected.hex())+')'
            return [sys.executable,'-I','-c',code]
    checks=[]
    def guard(): checks.append(True)
    m.stream_import(Client(),source,hashlib.sha256(raw).hexdigest(),5,database='owned_probe',guard=guard)
    assert len(checks)>=3
