"""Real MySQL8 selected-table contracts on one owned network-none container."""
import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import re
import secrets
import subprocess
import sys
import tempfile
import time

sys.path.insert(0,str(Path(__file__).parents[1]/'linux'))
import formal_persisted_work as persisted
import formal_snapshot_linux as snapshot

parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--evidence-parent',required=True,type=Path)
args=parser.parse_args()
operation='owned-persisted-'+secrets.token_hex(6)
image='sha256:ccb8f749bb5e59f9f8f03bf7282c7ef27a93a1814a24f0a8a926fb4e19b7fb97'
database='owned_persisted'
container=None
passed=[]


def docker(argv,timeout=40):
    result=subprocess.run(['docker',*argv],capture_output=True,timeout=timeout)
    if result.returncode: raise RuntimeError('OWNED_PERSISTED_DOCKER_FAILED')
    return result.stdout.decode().strip()


def expect(name, statement, *, confirmed=None, error=None, source=None, field=None, count=None):
    # All fixture writes are confined to this newly created owned database.
    for spec in persisted.SOURCES.values(): client.query('DELETE FROM `'+database+'`.`'+spec['table']+'`;')
    if statement: client.query('USE `'+database+'`; '+statement)
    before=rows_digest()
    try:
        result=persisted.observe_selected_work(client,database,uuid,guard=guard,seconds=20)
    except snapshot.FormalSnapshotError as failure:
        assert error and failure.code==error, (name,failure.code)
    else:
        assert error is None, name
        assert result['selectedTablesTerminalConfirmed'] is confirmed, name
        assert not result['fullDrain'] and not result['releaseReady'] and not result['productionEvidence']
        if source: assert result['counts'][source][field]==count, (name,result['counts'])
    assert rows_digest()==before, name+' changed business fixture rows'
    passed.append(name)


def rows_digest():
    parts=[]
    for source in persisted.SOURCES.values():
        parts.append(client.query('SELECT * FROM `'+database+'`.`'+source['table']+'` ORDER BY id;'))
    return hashlib.sha256('\n'.join(parts).encode()).hexdigest()


def guard():
    assert client.query('SELECT @@server_uuid;').strip()==uuid
    assert docker(['inspect','--format','{{.HostConfig.NetworkMode}}',container])=='none'


task="INSERT INTO dcc_controlled_file_nas_transfer_task(id,tenant_id,status,source_type,completed_at,next_check_at) VALUES(1,122,'COMPLETED','LOCAL_FOLDER',NOW(),NULL);"
item="INSERT INTO dcc_controlled_file_nas_transfer_task_item(id,tenant_id,task_id,status,completed_at) VALUES(1,122,1,'COMPLETED',NOW());"
batch="INSERT INTO dcc_controlled_file_batch_recognition_task(id,tenant_id,status,completed_at,total_count,processed_count,remaining_count) VALUES(1,122,'COMPLETED',NOW(),1,1,0);"
try:
    assert docker(['context','show'])=='desktop-linux'
    docker(['image','inspect',image])
    with tempfile.TemporaryDirectory(prefix=operation) as temporary:
        root=Path(temporary); password=secrets.token_hex(32); env=root/'mysql.env'
        env.write_text('MYSQL_ROOT_PASSWORD='+password+'\n',encoding='utf-8'); env.chmod(0o600)
        container=docker(['run','-d','--pull=never','--network','none','--memory','768m','--cpus','1','--pids-limit','256',
            '--name',operation,'--label','intruoyi.task='+operation,'--env-file',str(env),
            '--tmpfs','/var/lib/mysql:rw,size=512m',image,'--event-scheduler=OFF'])
        client=snapshot.PrivateDockerMySql(container,password,timeout=5)
        deadline=time.monotonic()+60
        while True:
            # The image initializes with a temporary unauthenticated socket.
            # Probe credentials only after entrypoint has exec'd the final daemon;
            # authentication failures remain fatal, never retried or suppressed.
            process=docker(['exec',container,'cat','/proc/1/comm'])
            if process=='mysqld' and client.ready(): break
            assert process in ('bash','docker-entrypoi','mysqld'), 'OWNED_PERSISTED_ENTRYPOINT_UNEXPECTED'
            if time.monotonic()>deadline: raise RuntimeError('OWNED_PERSISTED_MYSQL_NOT_READY')
            time.sleep(1)
        uuid=client.query('SELECT @@server_uuid;').strip()
        client.query('CREATE DATABASE `'+database+'` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;')
        # Minimal actual MySQL fixture of schema-verified required columns, no copied defaults.
        for source in persisted.SOURCES.values():
            definitions=[]
            for column in source['columns']:
                if column=='id': kind='BIGINT PRIMARY KEY'
                elif column.endswith('_at') or column in ('bound_time','cleanup_time'): kind='DATETIME NULL'
                elif column in ('status','cleanup_status','source_type','recognition_scope','report_file_name','cleanup_reason'): kind='VARCHAR(64) NULL'
                else: kind='BIGINT NULL'
                if column=='tenant_id': kind='BIGINT NULL DEFAULT 0'
                if column=='deleted': kind='TINYINT NULL DEFAULT 0'
                definitions.append('`'+column+'` '+kind)
            client.query('CREATE TABLE `'+database+'`.`'+source['table']+'` ('+','.join(definitions)+') ENGINE=InnoDB;')
        expect('empty-selected-set', '', confirmed=True)
        expect('disabled-tenant-soft-deleted-running',
            "INSERT INTO dcc_controlled_file_batch_recognition_task(id,tenant_id,deleted,status,total_count,processed_count,remaining_count) VALUES(1,999,1,'RUNNING',1,0,1);",
            confirmed=False,source='batch-recognition',field='pending',count=1)
        for value in ("NULL", "'completed'", "'COMPLETED '", "'FUTURE'"):
            expect('exact-state-'+value,
                'INSERT INTO dcc_controlled_file_batch_recognition_task(id,status,total_count,processed_count,remaining_count) VALUES(1,'+value+',0,0,0);',
                error='FORMAL_PERSISTED_UNKNOWN_STATE')
        expect('future-waiting-still-pending', task.replace("'COMPLETED'","'WAITING'").replace('NOW(),NULL','NULL,DATE_ADD(NOW(),INTERVAL 1 DAY)'),
               confirmed=False,source='nas-transfer',field='pending',count=1)
        expect('terminal-missing-time', batch.replace('NOW()','NULL'),confirmed=False)
        expect('completed-counters-inconsistent', batch.replace('NOW(),1,1,0','NOW(),1,0,1'),confirmed=False)
        expect('failed-outcome-completed-but-not-success', batch.replace("'COMPLETED'","'FAILED'").replace('NOW(),1,1,0','NOW(),1,0,1'),
               confirmed=True,source='batch-recognition',field='failedOutcome',count=1)
        expect('parent-terminal-live-child',task+item.replace("'COMPLETED'","'RUNNING'").replace('NOW()','NULL'),confirmed=False)
        expect('cross-tenant-item', task+item.replace('1,122,1','1,999,1'),confirmed=False)
        expect('orphan-item',item,confirmed=False)
        expect('terminal-local-folder-retained-chunk',task+item+
               "INSERT INTO dcc_controlled_file_local_folder_upload_chunk(id,tenant_id,task_id,status,chunk_index,total_chunks,file_size,chunk_size) VALUES(1,122,1,'COMPLETED',0,1,10,10);",confirmed=True)
        expect('orphan-chunk',"INSERT INTO dcc_controlled_file_local_folder_upload_chunk(id,tenant_id,task_id,status,chunk_index,total_chunks,file_size,chunk_size) VALUES(1,122,7,'COMPLETED',0,1,10,10);",confirmed=False)
        expect('active-ticket-expiry-not-terminal',
               "INSERT INTO dcc_controlled_file_temporary_file(id,tenant_id,status,cleanup_status) VALUES(1,999,'AVAILABLE','ACTIVE');",confirmed=False)
        expect('bound-ticket',"INSERT INTO dcc_controlled_file_temporary_file(id,status,cleanup_status,bound_controlled_file_id,bound_time) VALUES(1,'BOUND','BOUND',7,NOW());",confirmed=True)
        expect('cleaned-ticket',"INSERT INTO dcc_controlled_file_temporary_file(id,status,cleanup_status,cleanup_time,cleanup_reason) VALUES(1,'AVAILABLE','CLEANED',NOW(),'EXPIRED_UNBOUND');",confirmed=True)
        claim="INSERT INTO dcc_controlled_file_recognition_claim(id,tenant_id,recognition_scope,claimed_at,claimed_by,controlled_file_id,claim_task_id) VALUES(1,122,'BASIC_INFO',NOW(),3,7,NULL);"
        expect('manual-claim-null-owner-legal-but-pending',claim,confirmed=False,source='recognition-claim',field='inconsistent',count=0)
        expect('terminal-owner-claim-retained',batch+claim.replace('7,NULL','7,1'),confirmed=False,source='recognition-claim',field='inconsistent',count=1)
        expect('cross-tenant-claim',batch+claim.replace('1,122','1,999').replace('7,NULL','7,1'),confirmed=False)
        expect('audit-missing-report',"INSERT INTO dcc_nas_control_audit_task(id,status,completed_at) VALUES(1,'COMPLETED',NOW());",confirmed=False)
        expect('audit-terminal-report',"INSERT INTO dcc_nas_control_audit_task(id,status,completed_at,report_file_id,report_file_name) VALUES(1,'COMPLETED',NOW(),7,'report.xlsx');",confirmed=True)
        # Missing schema is refused before the snapshot query; restore no original data.
        table=next(iter(persisted.SOURCES.values()))['table']
        client.query('RENAME TABLE `'+database+'`.`'+table+'` TO `'+database+'`.`retained_missing_source`;')
        try: persisted.observe_selected_work(client,database,uuid,guard=guard,seconds=20)
        except snapshot.FormalSnapshotError as e: assert e.code=='FORMAL_PERSISTED_TABLE_REQUIRED'
        else: raise AssertionError('missing source accepted')
        passed.append('actual-missing-source-refusal')
        result={'status':'OWNED_NATIVE_MYSQL_SELECTED_COUNTS_PASS','observedAt':datetime.now(timezone.utc).isoformat(),
                'cases':passed,'caseCount':len(passed),'databaseUuid':uuid,'imageId':image,
                'networkMode':'none','sourceRowsUnchangedByObserver':True,'productionEvidence':False,'fullDrain':False,'releaseReady':False}
        output=args.evidence_parent/(operation+'.json')
        with output.open('x',encoding='utf-8') as f: json.dump(result,f,indent=2); f.write('\n')
        print(json.dumps({'evidence':str(output),**result}),flush=True)
finally:
    if container:
        inspected=json.loads(docker(['inspect',container]))[0]
        assert inspected['Config']['Labels']['intruoyi.task']==operation
        assert inspected['HostConfig']['NetworkMode']=='none'
        docker(['rm','-f',container])
