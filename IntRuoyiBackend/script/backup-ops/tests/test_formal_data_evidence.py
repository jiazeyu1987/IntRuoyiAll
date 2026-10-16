"""Contract tests; token fixtures are not source database evidence."""
import hashlib
import importlib
from pathlib import Path
import sys

import pytest

sys.path.insert(0, str(Path(__file__).parents[1] / 'linux'))
UUID = '11111111-1111-1111-1111-111111111111'


def module():
    return importlib.import_module('formal_data_evidence')


@pytest.mark.parametrize('expired',[False,True])
def test_data_observation_never_extends_existing_outer_deadline(tmp_path,monkeypatch,expired):
    import formal_snapshot_linux as snapshot
    m=module();calls=[]
    monkeypatch.setattr(m.time,'monotonic',lambda:13. if expired else 10.)
    client=m.DataMySql('a'*64,'owned-private',timeout=2)
    def observe(mysql,*args,**kwargs):
        kwargs['guard']();calls.append(kwargs['deadline'])
        assert mysql.deadline==12. and snapshot.observation_deadline.get()==12.
        return {'contractOnly':True}
    monkeypatch.setattr(m,'_observe_data',observe)
    token=snapshot.observation_deadline.set(12.)
    try:
        kwargs=dict(guard=lambda:None,scratch=tmp_path/'data',max_rows=10,max_nodes=10,max_bytes=1024,seconds=7200)
        if expired:
            with pytest.raises(snapshot.FormalSnapshotError,match='FORMAL_DATA_DEADLINE'):m.observe_data(client,'owned_probe',UUID,**kwargs)
            assert not calls
        else:assert m.observe_data(client,'owned_probe',UUID,**kwargs)=={'contractOnly':True} and calls==[12.]
        assert getattr(client,'deadline',None) is None and snapshot.observation_deadline.get()==12.
    finally:snapshot.observation_deadline.reset(token)


def test_total_deadline_bounds_mysql_inside_guard(tmp_path,monkeypatch):
    import time
    import formal_snapshot_linux as snapshot
    m=module()
    class Stalled(snapshot.PrivateDockerMySql):
        def _command(self,program,args):
            return [sys.executable,'-I','-c','import time; time.sleep(30)']
    guard_client=Stalled('a'*64,'owned-private',timeout=2)
    data_client=m.DataMySql('a'*64,'owned-private',timeout=2)
    start=time.monotonic()
    with pytest.raises(snapshot.FormalSnapshotError,match='FORMAL_MYSQL_QUERY_TIMEOUT'):
        m.observe_data(data_client,'owned_probe',UUID,guard=lambda:guard_client.query('SELECT @@server_uuid;'),
            scratch=tmp_path/'data',max_rows=10,max_nodes=10,max_bytes=1024,seconds=1)
    assert time.monotonic()-start<1.7
    assert data_client.deadline is None


@pytest.mark.parametrize('kind',['source-inspect','target-inspect'])
def test_total_deadline_bounds_docker_inside_guard(tmp_path,monkeypatch,kind):
    import subprocess
    import time
    import formal_snapshot_linux as snapshot
    import formal_replica_gate as replica
    timeouts=[]
    def stalled(command,**kwargs):
        timeouts.append(kwargs['timeout']);raise subprocess.TimeoutExpired(command,kwargs['timeout'])
    children=[]
    if kind=='source-inspect':
        monkeypatch.setattr(subprocess,'run',stalled)
        guard=lambda:snapshot.inspect_formal_container('a'*64)
    else:
        native=subprocess.Popen
        def docker_protocol(command,**kwargs):
            assert command==['docker','inspect','a'*64]
            child=native([sys.executable,'-I','-c','import time; time.sleep(30)'],**kwargs)
            children.append(child);return child
        monkeypatch.setattr(subprocess,'Popen',docker_protocol)
        guard=lambda:replica._private_docker_json(['inspect','a'*64],guard=lambda:None)
    started=time.monotonic()
    with pytest.raises(snapshot.FormalSnapshotError):
        module().observe_data(module().DataMySql('a'*64,'owned-private',timeout=2),'owned_probe',UUID,
            guard=guard,scratch=tmp_path/'data',max_rows=10,max_nodes=10,max_bytes=1024,seconds=1)
    if kind=='source-inspect':
        assert len(timeouts)==1 and 0<timeouts[0]<=1
    else:
        assert len(children)==1 and children[0].poll() is not None and children[0].stdout.closed
        assert time.monotonic()-started<1.7


def digest(value):
    return hashlib.sha256(value.encode()).hexdigest()


@pytest.mark.parametrize('operation',['-invalid','_invalid','.invalid','invalid/operation'])
def test_source_binding_rejects_invalid_claim_operation(operation):
    import json
    import formal_snapshot_linux as snapshot
    proof=contract_observation('a'*64,UUID)
    claim=json.dumps({'operationId':operation,'sourceFingerprint':'b'*64,'databaseUuid':UUID}).encode()
    with pytest.raises(snapshot.FormalSnapshotError,match='FORMAL_DATA_SOURCE_CHANGED'):
        module().source_binding(claim,proof,proof,'c'*64)


def contract_observation(container,uuid,database='owned_probe',value='original'):
    """Explicit contract fixture. Never a real captured observation."""
    import json
    from test_formal_schema_evidence import Catalog, capture
    from formal_snapshot_linux import _canonical_bytes
    schema=capture(Catalog())
    schema=json.loads(json.dumps(schema).replace('owned_schema',database))
    schema['observationIdentity']={'containerId':container,'databaseUuid':uuid}
    schema['schemaSha256']=hashlib.sha256(_canonical_bytes({k:v for k,v in schema.items()
        if k not in ('schemaSha256','observationIdentity','observationSha256')})).hexdigest()
    schema['observationSha256']=hashlib.sha256(_canonical_bytes({k:v for k,v in schema.items()
        if k!='observationSha256'})).hexdigest()
    proof={'encoding':'mysql8-typed-row-multiset-sha256/v1','schema':schema,
        'tables':{'probe':{'rows':1,'jsonNodes':0,'multisetSha256':digest(value)}},
        'observationIdentity':schema['observationIdentity'],'applicationVerified':False,'productionEvidence':False}
    proof['observationSha256']=hashlib.sha256(_canonical_bytes(proof)).hexdigest()
    return proof


def stream(tmp_path, rows, *, prefix=UUID, trailer='0\n'+UUID+'\n', **limits):
    raw = (prefix+'\n'+''.join(f'{i}\t{col}\t{token}\n' for i,col,token in rows)+trailer).encode()
    path = tmp_path/'tokens'
    path.write_bytes(raw)
    return module().summarize_tokens(path, UUID, tmp_path, max_rows=limits.get('max_rows',100),
        max_nodes=limits.get('max_nodes',1000), chunk_rows=2)


def test_order_independent_multiset_preserves_duplicate_occurrences(tmp_path):
    summaries=[]
    for label,values in [('original',['one','two','one','three']),
                         ('reordered',['three','one','two','one']),
                         ('duplicate_lost',['one','two','three'])]:
        root=tmp_path/label;root.mkdir()
        summaries.append(stream(root,[(i+1,0,digest(v)) for i,v in enumerate(values)]))
    assert summaries[0]==summaries[1]
    assert summaries[0]['rows']==4
    assert summaries[0]['multisetSha256']!=summaries[2]['multisetSha256']


def test_same_counts_and_same_values_with_different_json_type_refuse_equality(tmp_path):
    first=tmp_path/'first';first.mkdir();second=tmp_path/'second';second.mkdir()
    a=stream(first,[(1,0,digest('base')),(1,1,digest('DATE:2026-10-02'))])
    b=stream(second,[(1,0,digest('base')),(1,1,digest('STRING:2026-10-02'))])
    assert a['rows']==b['rows']==1 and a['jsonNodes']==b['jsonNodes']==1
    assert a['multisetSha256']!=b['multisetSha256']


@pytest.mark.parametrize('rows',[
    [(2,0,digest('a'))], [(1,1,digest('a'))], [(1,0,digest('a')),(1,0,digest('b'))],
    [(1,0,digest('a')),(3,0,digest('b'))],
    [(1,0,digest('a')),(1,2,digest('b')),(1,1,digest('c'))],
    [(1,0,digest('a')),(1,1,'f'*64),(1,1,'a'*64)],
    [(1,0,'not-a-sha')], [(1,0,'A'*64)]])
def test_ambiguous_or_partial_row_stream_refuses(tmp_path,rows):
    with pytest.raises(Exception,match='FORMAL_DATA_TOKEN_INVALID'):
        stream(tmp_path,rows)


@pytest.mark.parametrize('trailer',['','0\n','1\n'+UUID+'\n','0\n'+'2'*36+'\n',
    '0\n'+UUID+'\nextra\n'])
def test_warning_truncation_wrong_identity_extra_output_refuses(tmp_path,trailer):
    with pytest.raises(Exception,match='FORMAL_DATA_TRANSPORT_INVALID'):
        stream(tmp_path,[(1,0,digest('a'))],trailer=trailer)


def test_explicit_row_and_json_node_limits_fail_closed(tmp_path):
    for kind in ('rows','nodes'):
        root=tmp_path/kind;root.mkdir()
        with pytest.raises(Exception,match='FORMAL_DATA_LIMIT'):
            stream(root,[(1,0,digest('a')),*[(1,1,v) for v in sorted([digest('b'),digest('c')])],(2,0,digest('d'))],
                   max_rows=1 if kind=='rows' else 100,max_nodes=1 if kind=='nodes' else 100)


def test_empty_table_has_explicit_empty_multiset(tmp_path):
    result=stream(tmp_path,[])
    assert result['rows']==0 and result['jsonNodes']==0
    assert len(result['multisetSha256'])==64


@pytest.mark.parametrize('nullable',['NO','YES'])
def test_observed_enum_hashes_label_bytes_with_a_distinct_null_tag(nullable):
    # Actual history67 definition; this tests generated SQL, not a live DB result.
    column=['smallapp_objects','state','7','enum',"enum('STAGED','ACTIVE','DELETE_PENDING')",
            'utf8mb4','utf8mb4_0900_ai_ci',nullable,None,'','']
    cell=module()._cell(column)
    assert cell=="CAST(IF(`state` IS NULL,'N',CONCAT('V',SHA2(CAST(CAST(`state` AS CHAR CHARACTER SET utf8mb4) AS BINARY),256))) AS BINARY)"
    # NULL is tagged N; every non-NULL label (including empty text) is tagged V.
    # Declared member ordinals must never affect the label-byte expression.
    reordered=list(column);reordered[4]="enum('ACTIVE','DELETE_PENDING','STAGED')"
    assert module()._cell(reordered)==cell
    assert 'UNSIGNED' not in cell and '+0' not in cell


def test_actual_smallapp_objects_columns_generate_enum_row_fingerprint_sql():
    definitions=[('id','char','char(32)'),('tenant_id','bigint','bigint'),
                 ('bucket','varchar','varchar(63)'),('object_key','varchar','varchar(200)'),
                 ('size_bytes','int','int'),('sha256','char','char(64)'),
                 ('state','enum',"enum('STAGED','ACTIVE','DELETE_PENDING')")]
    columns=[['smallapp_objects',name,str(i+1),kind,definition,
              'utf8mb4' if kind in ('char','varchar','enum') else '',
              'utf8mb4_0900_ai_ci' if kind in ('char','varchar','enum') else '',
              'NO',None,'',''] for i,(name,kind,definition) in enumerate(definitions)]
    sql=module().table_sql('smallapp_objects',columns)
    assert module()._cell(columns[-1]) in sql
    assert sql.count(" IS NULL,'N',CONCAT('V',SHA2(")==7
    assert "SHA2(CONCAT('row/v1:'," in sql
    assert sql.endswith(' FROM `smallapp_objects`) SELECT rid,0,token FROM base ORDER BY rid;')


@pytest.mark.parametrize('kind',['geometry','unknown','set'])
def test_unimplemented_datatypes_are_not_cast_to_text(kind):
    with pytest.raises(Exception,match='FORMAL_DATA_TYPE_UNSUPPORTED'):
        module().table_sql('probe',[['probe','value','1',kind,kind,None,None,'YES',None,'','']])


def test_private_stream_to_disk_retains_exact_bytes_without_in_memory_result(tmp_path):
    from formal_schema_evidence import _bounded_process
    destination=tmp_path/'bounded-output'
    with destination.open('xb') as sink:
        result=_bounded_process([sys.executable,'-I','-c','import sys; sys.stdout.buffer.write(b"x"*1000000)'],
                                b'',1000000,5,sink=sink)
    assert result==b'' and destination.read_bytes()==b'x'*1000000


def test_disk_stream_byte_limit_does_not_return_success(tmp_path):
    from formal_schema_evidence import _bounded_process
    with (tmp_path/'bounded-output').open('xb') as sink:
        with pytest.raises(Exception,match='FORMAL_SCHEMA_STREAM_LIMIT_OR_IO'):
            _bounded_process([sys.executable,'-I','-c','import sys; sys.stdout.buffer.write(b"x"*1000000)'],
                             b'',64,5,sink=sink)


def test_expired_global_deadline_refuses_before_token_read(tmp_path):
    with pytest.raises(Exception,match='FORMAL_DATA_DEADLINE'):
        module().summarize_tokens(tmp_path/'absent',UUID,tmp_path,max_rows=100,max_nodes=100,
                                  deadline=0)


def test_global_deadline_is_checked_during_python_sort(tmp_path,monkeypatch):
    import time
    rows=[(i+1,0,digest(str(i))) for i in range(20)]
    path=tmp_path/'tokens';path.write_bytes((UUID+'\n'+''.join(f'{i}\t{c}\t{v}\n' for i,c,v in rows)+'0\n'+UUID+'\n').encode())
    clock=iter(range(1000));monkeypatch.setattr(time,'monotonic',lambda:next(clock))
    with pytest.raises(Exception,match='FORMAL_DATA_DEADLINE'):
        module().summarize_tokens(path,UUID,tmp_path,max_rows=100,max_nodes=100,chunk_rows=2,deadline=15)


def test_unknown_native_json_type_is_an_explicit_blocker(tmp_path):
    with pytest.raises(Exception,match='FORMAL_DATA_JSON_TYPE_UNSUPPORTED'):
        stream(tmp_path,[(1,0,digest('base')),(1,1,'UNSUPPORTED:BLOB')])


def test_tampered_full_proof_is_not_equal_even_to_itself():
    with pytest.raises(Exception,match='FORMAL_DATA_PROOF_CHANGED'):
        module().compare_data({'tables':{}},{'tables':{}})


def test_guard_failure_precedes_any_database_or_disk_action(tmp_path):
    class NoQueries:
        pass
    def refuse():raise RuntimeError('MAINTAINED_GUARD_LOST')
    scratch=tmp_path/'uncreated'
    with pytest.raises(RuntimeError,match='MAINTAINED_GUARD_LOST'):
        module().observe_data(NoQueries(),'probe',UUID,guard=refuse,scratch=scratch,max_rows=10,
                              max_nodes=100,max_bytes=1000,seconds=20)
    assert not scratch.exists()
