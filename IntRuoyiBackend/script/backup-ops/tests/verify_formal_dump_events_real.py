"""Real owned dump/import fidelity for routines, triggers and disabled events.

No formal maintenance claim, full-database equivalence or release proof.
"""
import gzip
import argparse
import hashlib
import json
import os
from pathlib import Path
import secrets
import subprocess
import sys
import tempfile
import time

sys.path.insert(0, str(Path(__file__).parents[1] / 'linux'))
from formal_snapshot_linux import PrivateDockerMySql, FormalSnapshotError
from formal_sql_restore import IsolatedTargetMySql, expand_checked, stream_import
from formal_schema_evidence import SchemaMySql, observe_schema, compare_schema

operation = 'owned-formal-events-' + secrets.token_hex(6)
owned = []
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--evidence-parent',type=Path)
arguments=parser.parse_args()


def docker(arguments, *, timeout=60):
    result = subprocess.run(['docker', *arguments], capture_output=True, timeout=timeout)
    if result.returncode:
        raise RuntimeError('OWNED_EVENTS_DOCKER_FAILURE')
    return result.stdout.decode('utf-8').strip()


def create(role, envfile):
    name = operation+'-'+role
    identity = docker(['run', '-d', '--pull=never', '--network', 'none', '--name', name,
        '--label', 'intruoyi.task='+operation, '--env-file', str(envfile),
        '--tmpfs', '/var/lib/mysql:rw,size=512m', 'mysql:8.0.39', '--event-scheduler=OFF'])
    owned.append(identity)
    return identity


def wait_ready(mysql):
    deadline = time.monotonic()+90
    while True:
        try:
            return mysql.query('SELECT @@server_uuid;').strip()
        except FormalSnapshotError as error:
            if error.code != 'FORMAL_MYSQL_QUERY_FAILED' or time.monotonic() >= deadline:
                raise
            time.sleep(1)


def observe(mysql):
    sql = """SELECT routine_name,routine_type FROM information_schema.routines
WHERE routine_schema='owned_events' ORDER BY routine_name;
SELECT trigger_name,action_timing,event_manipulation FROM information_schema.triggers
WHERE trigger_schema='owned_events' ORDER BY trigger_name;
SELECT event_name,status,event_definition FROM information_schema.events
WHERE event_schema='owned_events' ORDER BY event_name;
SELECT id,value FROM owned_events.probe ORDER BY id;
SELECT @@GLOBAL.event_scheduler;
"""
    return mysql.query(sql)


try:
    with tempfile.TemporaryDirectory(prefix=operation+'-') as temp:
        root = Path(temp)
        password = secrets.token_hex(32)
        envfile = root/'mysql.env'
        envfile.write_text('MYSQL_ROOT_PASSWORD='+password+'\n', encoding='utf-8')
        envfile.chmod(0o600)
        source_id = create('source', envfile)
        source = PrivateDockerMySql(source_id, password, timeout=60)
        source_uuid = wait_ready(source)
        source.query("""CREATE DATABASE owned_events;
CREATE TABLE owned_events.probe(id INT PRIMARY KEY,value VARCHAR(100));
INSERT INTO owned_events.probe VALUES (7,'original');
CREATE VIEW owned_events.owned_view AS SELECT id,value FROM owned_events.probe;
CREATE PROCEDURE owned_events.owned_procedure() SELECT id,value FROM owned_events.probe;
CREATE FUNCTION owned_events.owned_function() RETURNS INT DETERMINISTIC RETURN 7;
CREATE TRIGGER owned_events.owned_trigger BEFORE INSERT ON owned_events.probe
FOR EACH ROW SET NEW.value=CONCAT(NEW.value,'-trigger');
CREATE TRIGGER owned_events.a_second_trigger BEFORE INSERT ON owned_events.probe
FOR EACH ROW SET NEW.id=NEW.id;
CREATE EVENT owned_events.owned_event ON SCHEDULE EVERY 1 DAY DISABLE
DO UPDATE owned_events.probe SET value='event';
ALTER TABLE owned_events.probe ADD COLUMN literal_null VARCHAR(20) DEFAULT 'NULL',
 ADD COLUMN real_null VARCHAR(20) DEFAULT NULL,
 ADD COLUMN unicode_comment INT COMMENT 'schema 中文',
 ADD COLUMN generated_value INT GENERATED ALWAYS AS (id+1) VIRTUAL;
SET GLOBAL read_only=ON; SET GLOBAL super_read_only=ON;
""")
        before = observe(source)
        source_schema_client=SchemaMySql(source_id,password,timeout=60)
        def source_guard():
            assert source.query('SELECT @@server_uuid,@@GLOBAL.read_only,@@GLOBAL.super_read_only;')==source_uuid+'\t1\t1\n'
            assert docker(['inspect','--format','{{.HostConfig.NetworkMode}}',source_id])=='none'
        original_schema=observe_schema(source_schema_client,'owned_events',source_uuid,guard=source_guard)
        columns={row[1]:row for row in original_schema['catalog']['COLUMNS'] if row[0]=='probe'}
        assert columns['literal_null'][8]=='NULL' and columns['real_null'][8] is None
        assert columns['generated_value'][10]
        trigger_orders={row[0]:row[4] for row in original_schema['catalog']['TRIGGERS']}
        assert trigger_orders=={'a_second_trigger':'2','owned_trigger':'1'}
        compressed = root/'database.sql.gz'
        dump = source.dump('owned_events', compressed,guard=source_guard)
        raw = gzip.decompress(compressed.read_bytes())
        assert all(token in raw for token in (b'owned_procedure',b'owned_function',b'owned_trigger',b'owned_event'))
        assert b'GTID_PURGED' not in raw
        assert observe(source) == before
        target_id = create('target', envfile)
        target = IsolatedTargetMySql(target_id, password, timeout=60)
        target_uuid = wait_ready(target)
        assert target_uuid != source_uuid
        target.query('CREATE DATABASE owned_events;')
        expanded = root/'expanded.sql'
        sha, size = expand_checked(compressed, expanded, dump['sha256'], 16*1024*1024, 60)
        target_schema_client=SchemaMySql(target_id,password,timeout=60)
        def target_guard():
            assert target.query('SELECT @@server_uuid,@@GLOBAL.event_scheduler;')==target_uuid+'\tOFF\n'
            assert docker(['inspect','--format','{{.HostConfig.NetworkMode}}',target_id])=='none'
        stream_import(target, expanded, sha, 60, database='owned_events',guard=target_guard)
        restored_schema=observe_schema(target_schema_client,'owned_events',target_uuid,guard=target_guard)
        schema_result=compare_schema(original_schema,restored_schema)
        assert observe(target) == before
        assert target.query('CALL owned_events.owned_procedure();') == '7\toriginal\n'
        assert target.query('SELECT owned_events.owned_function();') == '7\n'
        target.query("INSERT INTO owned_events.probe(id,value) VALUES(8,'restored');")
        assert target.query('SELECT value FROM owned_events.probe WHERE id=8;') == 'restored-trigger\n'
        assert target.query('SELECT @@GLOBAL.event_scheduler;') == 'OFF\n'
        target.query('ALTER TABLE owned_events.probe MODIFY COLUMN unicode_comment BIGINT;')
        altered_schema=observe_schema(target_schema_client,'owned_events',target_uuid,guard=target_guard)
        try:compare_schema(original_schema,altered_schema)
        except FormalSnapshotError as error:assert error.code=='FORMAL_SCHEMA_MISMATCH'
        else:raise RuntimeError('OWNED_SCHEMA_DRIFT_ACCEPTED')
        assert observe_schema(source_schema_client,'owned_events',source_uuid,guard=source_guard)==original_schema
        retained=None
        if arguments.evidence_parent is not None:
            parent=arguments.evidence_parent.absolute()
            assert parent.is_dir() and not parent.is_symlink()
            retained_root=parent/(operation+'-schema-retained')
            retained_root.mkdir(mode=0o700)
            hashes={}
            for name,proof in [('source.json',original_schema),('restored.json',restored_schema),
                    ('altered.json',altered_schema),('comparison.json',schema_result)]:
                raw=(json.dumps(proof,sort_keys=True,separators=(',',':'))+'\n').encode()
                with (retained_root/name).open('xb') as stream:
                    os.chmod(stream.name,0o600);stream.write(raw);stream.flush();os.fsync(stream.fileno())
                hashes[name]=hashlib.sha256(raw).hexdigest()
            retained={'root':str(retained_root),'sha256':hashes}
        print(json.dumps({'status':'REAL_OWNED_DUMP_EVENTS_ROUTINES_TRIGGERS_PASS',
            'sourceUuid':source_uuid,'targetUuid':target_uuid,'dumpSha256':dump['sha256'],
            'expandedSha256':sha,'expandedBytes':size,'disabledEventPreserved':True,
            'routineFunctionAndTriggerExecuted':True,'sourceUnchanged':True,
            'schemaEvidence':schema_result,'alteredColumnSchemaRejected':True,
            'nullDefaultDistinguishedFromTextNull':True,
            'retainedSchemaEvidence':retained,
            'fullSchemaDataEquivalenceVerified':False,'productionEvidence':False}))
finally:
    for identity in reversed(owned):
        label = docker(['inspect','--format','{{index .Config.Labels "intruoyi.task"}}',identity])
        if label != operation:
            raise RuntimeError('OWNED_EVENTS_CLEANUP_OWNERSHIP_CHANGED')
        docker(['rm','--force',identity])
    print('EXACT_OWNED_EVENT_CONTAINERS_REMOVED')
