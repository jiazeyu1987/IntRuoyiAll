"""Finite initialization hooks; protocol/runtime doubles, no host exclusion proof."""
import copy
import hashlib
import importlib
from pathlib import Path
from unittest.mock import patch

import pytest

from test_formal_external_callers import fixture as external_fixture
from test_formal_ingress_control import plan as network_plan
from test_formal_maintenance_claim import BINDING, MysqlDouble


@pytest.mark.parametrize('stage',['missing','before','intent','after-observation','after-effect','success'])
def test_external_stop_requires_guard_at_effect_boundary_and_retains_uncertainty(tmp_path,stage):
    m,claim,plan,transport=external_fixture(tmp_path)
    m.bind_external_callers(claim,plan,transport);failed=[False];calls=[]
    original_inspect=transport.inspect
    def inspect(connection,identity):
        row=original_inspect(connection,identity)
        if stage=='after-observation' and any(e['step']=='external-test-backend-stop-intent' for e in claim.events):
            failed[0]=True
        return row
    transport.inspect=inspect
    def guard():
        calls.append('guard')
        intent=any(e['step']=='external-test-backend-stop-intent' for e in claim.events)
        if stage=='before' or stage=='intent' and intent or failed[0] or stage=='after-effect' and transport.actions:
            raise m.snapshot.FormalSnapshotError('OWNED_GUARD_REJECTED')
    if stage=='success':
        m.stop_external_caller(claim,'test-backend',transport,guard=guard)
        assert len(transport.actions)==1 and calls
        assert claim.events[-1]['step']=='external-test-backend-stopped'
    else:
        error='FORMAL_CALLER_MUTATION_GUARD_REQUIRED' if stage=='missing' else 'OWNED_GUARD_REJECTED'
        with pytest.raises(m.snapshot.FormalSnapshotError,match='^'+error+'$'):
            m.stop_external_caller(claim,'test-backend',transport,guard=None if stage=='missing' else guard)
        assert len(transport.actions)==(1 if stage=='after-effect' else 0)
        assert not any(e['step']=='external-test-backend-stopped' for e in claim.events)
        if stage=='after-effect':
            before=copy.deepcopy(claim.events);actions=list(transport.actions)
            with pytest.raises(m.snapshot.FormalSnapshotError,match='FORMAL_CALLER_ACTION_REPLAY_REFUSED'):
                m.stop_external_caller(claim,'test-backend',transport,guard=claim.assert_owned)
            assert claim.events==before and transport.actions==actions


@pytest.mark.parametrize('stage',['missing','before','intent','after-observation','after-effect','success'])
def test_database_fence_requires_guard_before_set_and_before_completion(tmp_path,monkeypatch,stage):
    m=importlib.import_module('formal_snapshot_linux');mysql=MysqlDouble();calls=[];failed=[False]
    class Claim(m.FormalMaintenanceClaim):
        def __init__(self):self.root=tmp_path;self._plan=m._canonical_bytes(BINDING);self.events=[]
        def assert_owned(self):pass
        def _write(self,path,raw):path.write_bytes(raw);path.chmod(0o600)
        def _read(self,path):return path.read_bytes()
        def record(self,step,details):self.events.append({'step':step,'details':copy.deepcopy(details)})
    claim=Claim();original_query=mysql.query
    def query(sql):
        result=original_query(sql)
        if stage=='after-observation' and sql.startswith('SELECT') and claim.events and not mysql.sets:
            failed[0]=True
        return result
    mysql.query=query
    def guard():
        calls.append('guard')
        if stage=='before' or stage=='intent' and claim.events or failed[0] or stage=='after-effect' and mysql.sets:
            raise m.FormalSnapshotError('OWNED_GUARD_REJECTED')
    if stage=='success':
        assert m.acquire_claimed_database_fence(claim,mysql,guard=guard)=={'serverUuid':BINDING['databaseUuid'],'readOnly':0,'superReadOnly':0}
        assert len(mysql.sets)==1 and calls and claim.events[-1]['step']=='db-fenced'
    else:
        error='FORMAL_DATABASE_MUTATION_GUARD_REQUIRED' if stage=='missing' else 'OWNED_GUARD_REJECTED'
        with pytest.raises(m.FormalSnapshotError,match='^'+error+'$'):
            m.acquire_claimed_database_fence(claim,mysql,guard=None if stage=='missing' else guard)
        assert len(mysql.sets)==(1 if stage=='after-effect' else 0)
        assert not any(e['step']=='db-fenced' for e in claim.events)
        if stage=='after-effect':
            saved=copy.deepcopy(claim.events);sets=list(mysql.sets)
            with pytest.raises(m.FormalSnapshotError,match='FORMAL_DATABASE_CLAIM_STAGE_INVALID'):
                m.acquire_claimed_database_fence(claim,mysql,guard=claim.assert_owned)
            assert claim.events==saved and mysql.sets==sets


@pytest.mark.parametrize('stage',['missing','rejected','last-observation','completion','success'])
def test_database_reconciliation_cannot_bypass_rejected_post_effect_guard(tmp_path,stage):
    m=importlib.import_module('formal_snapshot_linux');mysql=MysqlDouble();failed=[False];calls=[]
    class Claim(m.FormalMaintenanceClaim):
        def __init__(self):self.root=tmp_path;self._plan=m._canonical_bytes(BINDING);self.events=[]
        def assert_owned(self):pass
        def _write(self,path,raw):path.write_bytes(raw);path.chmod(0o600)
        def _read(self,path):return path.read_bytes()
        def record(self,step,details):
            self.events.append({'step':step,'details':copy.deepcopy(details)})
            if stage=='completion' and step=='db-fenced':failed[0]=True
    claim=Claim()
    def acquisition_guard():
        if mysql.sets:raise m.FormalSnapshotError('OWNED_GUARD_REJECTED')
    with pytest.raises(m.FormalSnapshotError,match='^OWNED_GUARD_REJECTED$'):
        m.acquire_claimed_database_fence(claim,mysql,guard=acquisition_guard)
    original={p.name:p.read_bytes() for p in tmp_path.iterdir()};events=copy.deepcopy(claim.events)
    sets=list(mysql.sets);observed=[False];original_query=mysql.query
    def query(sql):
        observed[0]=True
        return original_query(sql)
    mysql.query=query
    original_read=claim._read
    def read(path):
        result=original_read(path)
        if stage=='last-observation' and observed[0]:failed[0]=True
        return result
    claim._read=read
    def guard():
        calls.append('guard')
        if stage=='rejected' or failed[0]:raise m.FormalSnapshotError('OWNED_GUARD_REJECTED')
    if stage=='success':
        assert m.reconcile_claimed_database_fence(claim,mysql,guard=guard)=={
            'serverUuid':BINDING['databaseUuid'],'readOnly':0,'superReadOnly':0}
        assert claim.events[-1]=={'step':'db-fenced','details':{
            'originalSha256':hashlib.sha256(original['database-original.json']).hexdigest(),'reconciled':True}}
        assert len(calls)>=3
    else:
        code='FORMAL_DATABASE_MUTATION_GUARD_REQUIRED' if stage=='missing' else 'OWNED_GUARD_REJECTED'
        with pytest.raises(m.FormalSnapshotError,match='^'+code+'$'):
            m.reconcile_claimed_database_fence(claim,mysql,guard=None if stage=='missing' else guard)
        if stage=='completion':
            assert claim.events[-1]['step']=='db-fenced' and claim.events[:-1]==events
        else:assert claim.events==events
        if stage in ('missing','rejected'):assert not observed[0]
    assert mysql.sets==sets
    assert {p.name:p.read_bytes() for p in tmp_path.iterdir()}==original


@pytest.mark.parametrize('stage',['missing','before','intent','after-observation','after-effect','snapshot','success'])
def test_ingress_install_requires_guard_and_fixed_approved_plan(tmp_path,monkeypatch,stage):
    m=importlib.import_module('formal_ingress_control');approved=network_plan();value=copy.deepcopy(approved)
    actions=[];failed=[False];calls=[]
    class Claim:
        def __init__(self):self.root=tmp_path;self._plan=b'owned-guard-claim';self.events=[]
        def assert_owned(self):pass
        def _write(self,path,raw):path.write_bytes(raw)
        def _read(self,path):return path.read_bytes()
        def record(self,step,details):self.events.append({'step':step,'details':copy.deepcopy(details)})
    claim=Claim();table=m._table(hashlib.sha256(claim._plan).hexdigest())
    class Native:
        def identity(self):
            if stage=='after-observation' and any(e['step']=='ingress-install-intent' for e in claim.events):failed[0]=True
            return {k:approved[k] for k in ('bootId','networkNamespace')}
        def inspect(self,name):return [{'table':{'handle':71}}] if actions else None
        def run(self,args,raw):
            if args==['-f','-']:actions.append(raw)
    def guard():
        calls.append('guard')
        if stage=='snapshot':value['hostPorts'].append(9100)
        intent=any(e['step']=='ingress-install-intent' for e in claim.events)
        if stage=='before' or stage=='intent' and intent or failed[0] or stage=='after-effect' and actions:
            raise m.snapshot.FormalSnapshotError('OWNED_GUARD_REJECTED')
    monkeypatch.setattr(m,'_verify_applied_policy',lambda *a:None)
    monkeypatch.setattr(m,'assert_ingress',lambda *a,**k:{'table':table})
    if stage in ('success','snapshot'):
        assert m.install_ingress(claim,value,guard=guard,native=Native())=={'table':table}
        assert actions==[m.render_policy(hashlib.sha256(claim._plan).hexdigest(),approved)]
        assert (tmp_path/'ingress-plan.json').read_bytes()==m.snapshot._canonical_bytes(approved)
        assert claim.events[-1]['step']=='ingress-installed' and calls
    else:
        error='FORMAL_INGRESS_MUTATION_GUARD_REQUIRED' if stage=='missing' else 'OWNED_GUARD_REJECTED'
        with pytest.raises(m.snapshot.FormalSnapshotError,match='^'+error+'$'):
            m.install_ingress(claim,value,guard=None if stage=='missing' else guard,native=Native())
        assert len(actions)==(1 if stage=='after-effect' else 0)
        assert not any(e['step']=='ingress-installed' for e in claim.events)
        if stage=='after-effect':
            saved=copy.deepcopy(claim.events);original=list(actions)
            with pytest.raises(m.snapshot.FormalSnapshotError,match='FORMAL_INGRESS_ACTION_ALREADY_STARTED'):
                m.install_ingress(claim,value,guard=claim.assert_owned,native=Native())
            assert claim.events==saved and actions==original
