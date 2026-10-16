"""Real network-none MySQL8 owned full-row comparison. Never production proof."""
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

sys.path.insert(0,str(Path(__file__).parents[1]/'linux'))
from formal_snapshot_linux import PrivateDockerMySql, FormalSnapshotError, _canonical_bytes
from formal_sql_restore import IsolatedTargetMySql, expand_checked, stream_import
from formal_data_evidence import DataMySql, observe_data, compare_data, table_sql
from formal_schema_evidence import observe_schema

parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--evidence-parent',required=True,type=Path)
args=parser.parse_args()
operation='owned-formal-data-'+secrets.token_hex(6)
owned=[]


def docker(args,timeout=60):
    result=subprocess.run(['docker',*args],capture_output=True,timeout=timeout)
    if result.returncode:raise RuntimeError('OWNED_DATA_DOCKER_FAILED')
    return result.stdout.decode().strip()


def create(role,env):
    identity=docker(['run','-d','--pull=never','--network','none','--name',operation+'-'+role,
        '--label','intruoyi.task='+operation,'--env-file',str(env),'--tmpfs','/var/lib/mysql:rw,size=512m',
        'mysql:8.0.39','--event-scheduler=OFF'])
    owned.append(identity);return identity


def ready(client):
    deadline=time.monotonic()+90
    while True:
        try:return client.query('SELECT @@server_uuid;').strip()
        except FormalSnapshotError as error:
            if error.code!='FORMAL_MYSQL_QUERY_FAILED' or time.monotonic()>=deadline:raise
            time.sleep(1)


def check_mismatch(source,target):
    try:compare_data(source,target)
    except FormalSnapshotError as error:assert error.code=='FORMAL_DATA_MISMATCH'
    else:raise RuntimeError('OWNED_DATA_CHANGED_CONTENT_ACCEPTED')


try:
    with tempfile.TemporaryDirectory(prefix=operation) as temporary:
        root=Path(temporary);password=secrets.token_hex(32);env=root/'mysql.env'
        env.write_text('MYSQL_ROOT_PASSWORD='+password+'\n',encoding='utf-8');env.chmod(0o600)
        source_id=create('source',env);source=PrivateDockerMySql(source_id,password,timeout=90)
        source_uuid=ready(source)
        source.query("""CREATE DATABASE owned_data CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE TABLE owned_data.values_probe(
 b BIGINT UNSIGNED, bits BIT(64), binary_small BLOB, fixed CHAR(5), day DATE,
 moment DATETIME(6), precise DECIMAL(65,30), approximate DOUBLE, display_double DOUBLE(11,2),
 integer_value INT, json_value JSON, json_second JSON, binary_large LONGBLOB, long_text LONGTEXT,
 small SMALLINT, text_value TEXT, stamped TIMESTAMP(6) NULL, tiny TINYINT, variable VARCHAR(100)
);
INSERT INTO owned_data.values_probe VALUES(
18446744073709551615,18446744073709551615,X'00FF0A0D09',CONVERT(0xE4B8ADE69687 USING utf8mb4),'2026-10-02',
'2026-10-02 11:22:33.123456',12345678901234567890123456789012345.123456789012345678901234567890,
1.2345678901234567e-200,123.45,-2147483648,
JSON_OBJECT('array',JSON_ARRAY(1,1.5e0,true,NULL,'NULL','',JSON_OBJECT('q.[]',JSON_ARRAY())),
 'object',JSON_OBJECT(),'slash\\key',JSON_OBJECT('quote"key','value')),
JSON_ARRAY(JSON_OBJECT('nested',JSON_ARRAY(1,2,3))),X'FF000001',CONCAT('case sensitive ',CHAR(0),' tail  '),
-32768,'NULL','2026-10-02 11:22:33.654321',-128,' trailing  ');
INSERT INTO owned_data.values_probe SELECT * FROM owned_data.values_probe;
INSERT INTO owned_data.values_probe(b,approximate,json_value,json_second,variable) VALUES
(0,1.7976931348623157e308,CAST('null' AS JSON),JSON_OBJECT(),''),
(1,4.9406564584124654e-324,NULL,JSON_ARRAY(),NULL),
(2,0.0,JSON_ARRAY('é','中文','😀'),JSON_OBJECT('','empty-key'),'NULL'),
(3,1.0000000000000002,CAST('42' AS JSON),JSON_ARRAY(),'adjacent');
ALTER TABLE owned_data.values_probe ADD COLUMN generated_value INT GENERATED ALWAYS AS (tiny+1) VIRTUAL;
CREATE TABLE owned_data.empty_probe(value INT);
SET GLOBAL read_only=ON; SET GLOBAL super_read_only=ON;
""")
        def source_guard():
            assert source.query('SELECT @@server_uuid,@@GLOBAL.read_only,@@GLOBAL.super_read_only;')==source_uuid+'\t1\t1\n'
            assert docker(['inspect','--format','{{.HostConfig.NetworkMode}}',source_id])=='none'
        source_client=DataMySql(source_id,password,timeout=90)
        def observation(client,uuid,guard,name):
            try:
                return observe_data(client,'owned_data',uuid,guard=guard,scratch=root/name,
                    max_rows=1000,max_nodes=10000,max_bytes=4*1024*1024,seconds=600)
            except FormalSnapshotError:
                # Only this disposable owned fixture: failed-query diagnostics, never a retry receipt.
                schema=observe_schema(client,'owned_data',uuid,guard=guard)
                for table,kind in schema['catalog']['TABLES']:
                    if kind!='BASE TABLE':continue
                    sql=table_sql(table,[c for c in schema['catalog']['COLUMNS'] if c[0]==table])
                    command=client._command('timeout --signal=TERM --kill-after=5 30s mysql',
                        ['-uroot','--batch','--skip-column-names','--default-character-set=utf8mb4','owned_data'])
                    failed=subprocess.run(command,input=(password+'\n'+sql).encode(),capture_output=True,timeout=35)
                    if failed.stderr:print('OWNED_QUERY_DIAGNOSTIC '+failed.stderr.decode(errors='replace').replace(password,'[REDACTED]')[:1000],flush=True)
                raise
        original=observation(source_client,source_uuid,source_guard,'original')
        assert original['tables']['values_probe']['rows']==6 and original['tables']['empty_probe']['rows']==0
        assert len({c[3] for c in original['schema']['catalog']['COLUMNS']})==17
        print('SOURCE_17_TYPES_AND_JSON_OBSERVED',flush=True)
        compressed=root/'database.sql.gz';dump=source.dump('owned_data',compressed,guard=source_guard)
        target_id=create('target',env);target=IsolatedTargetMySql(target_id,password,timeout=90)
        target_uuid=ready(target);assert source_uuid!=target_uuid
        target.query('CREATE DATABASE owned_data CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;')
        raw=root/'expanded.sql';expanded_sha,_=expand_checked(compressed,raw,dump['sha256'],16*1024*1024,90)
        def target_guard():
            assert target.query('SELECT @@server_uuid,@@GLOBAL.event_scheduler;')==target_uuid+'\tOFF\n'
            assert docker(['inspect','--format','{{.HostConfig.NetworkMode}}',target_id])=='none'
        stream_import(target,raw,expanded_sha,90,database='owned_data',guard=target_guard)
        target_client=DataMySql(target_id,password,timeout=90)
        restored=observation(target_client,target_uuid,target_guard,'restored')
        comparison=compare_data(original,restored)
        print('SOURCE_RESTORED_SCHEMA_AND_ALL_ROWS_EQUAL',flush=True)
        target.query('UPDATE owned_data.values_probe SET tiny=-127 WHERE b=18446744073709551615 LIMIT 1;')
        changed=observation(target_client,target_uuid,target_guard,'changed')
        check_mismatch(original,changed)
        assert changed['tables']['values_probe']['rows']==original['tables']['values_probe']['rows']
        target.query('UPDATE owned_data.values_probe SET approximate=1.0000000000000004 WHERE b=3;')
        adjacent=observation(target_client,target_uuid,target_guard,'adjacent-double')
        check_mismatch(changed,adjacent)
        print('SAME_COUNTS_AND_ADJACENT_DOUBLE_CHANGES_REJECTED',flush=True)
        target.query("UPDATE owned_data.values_probe SET json_value=JSON_OBJECT('native',CAST('2026-10-02' AS DATE),"
                     " 'decimal',CAST(1.25 AS DECIMAL(5,2))) WHERE b=0;")
        native=observation(target_client,target_uuid,target_guard,'native')
        native_text=target.query('SELECT CAST(json_value AS CHAR) FROM owned_data.values_probe WHERE b=0;')
        native_dump=root/'native.sql.gz';native_meta=target.dump('owned_data',native_dump,guard=target_guard)
        third_id=create('native-restore',env);third=IsolatedTargetMySql(third_id,password,timeout=90)
        third_uuid=ready(third)
        third.query('CREATE DATABASE owned_data CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;')
        native_raw=root/'native.sql';native_sha,_=expand_checked(native_dump,native_raw,native_meta['sha256'],16*1024*1024,90)
        def third_guard():
            assert third.query('SELECT @@server_uuid,@@GLOBAL.event_scheduler;')==third_uuid+'\tOFF\n'
            assert docker(['inspect','--format','{{.HostConfig.NetworkMode}}',third_id])=='none'
        stream_import(third,native_raw,native_sha,90,database='owned_data',guard=third_guard)
        textual=observation(DataMySql(third_id,password,timeout=90),third_uuid,third_guard,'textual')
        assert third.query('SELECT CAST(json_value AS CHAR) FROM owned_data.values_probe WHERE b=0;')==native_text
        check_mismatch(native,textual)
        print('ACTUAL_NATIVE_JSON_DUMP_IMPORT_TYPE_LOSS_REJECTED',flush=True)
        target.query("UPDATE owned_data.values_probe SET json_value=JSON_OBJECT('binary',CAST(X'00FF' AS BINARY)) WHERE b=0;")
        try:
            observe_data(target_client,'owned_data',target_uuid,guard=target_guard,scratch=root/'opaque',
                max_rows=1000,max_nodes=10000,max_bytes=4*1024*1024,seconds=600)
        except FormalSnapshotError as error:assert error.code=='FORMAL_DATA_JSON_TYPE_UNSUPPORTED'
        else:raise RuntimeError('OWNED_UNSUPPORTED_JSON_TYPE_ACCEPTED')
        final_source=observation(source_client,source_uuid,source_guard,'source-after')
        assert final_source==original
        parent=args.evidence_parent.absolute();assert parent.is_dir() and not parent.is_symlink()
        retained=parent/(operation+'-retained');retained.mkdir(mode=0o700);hashes={}
        for name,proof in [('source.json',original),('restored.json',restored),('changed.json',changed),
                ('adjacent-double.json',adjacent),('native-json.json',native),('text-json.json',textual),('comparison.json',comparison)]:
            content=_canonical_bytes(proof)
            with (retained/name).open('xb') as stream:
                os.chmod(stream.name,0o600);stream.write(content);stream.flush();os.fsync(stream.fileno())
            hashes[name]=hashlib.sha256(content).hexdigest()
        print(json.dumps({'status':'REAL_OWNED_FULL_DATA_PASS','sourceUuid':source_uuid,'targetUuid':target_uuid,
            'comparison':comparison,'sameCountChangedValueRejected':True,'nativeJsonTypeLossRejected':True,
            'actualNativeJsonDumpTypeLossRejected':True,'adjacentDoubleChangeRejected':True,
            'unsupportedNativeJsonBinaryRejected':True,'generatedColumnPreserved':True,
            'sourceUnchanged':True,'observedTypes':17,'retained':{'root':str(retained),'sha256':hashes},
            'productionEvidence':False}),flush=True)
finally:
    for identity in reversed(owned):
        assert docker(['inspect','--format','{{index .Config.Labels "intruoyi.task"}}',identity])==operation
        docker(['rm','--force',identity])
    print('EXACT_OWNED_DATA_CONTAINERS_REMOVED',flush=True)
