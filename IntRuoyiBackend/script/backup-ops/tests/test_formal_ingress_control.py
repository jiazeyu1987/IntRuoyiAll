"""Plan rejection tests; actual nft traffic is checked by the owned Linux driver."""
import copy
import importlib
import json
from pathlib import Path
import sys

import pytest

sys.path.insert(0, str(Path(__file__).parents[1] / 'linux'))


def plan():
    return {'bootId':'00000000-0000-0000-0000-000000000001',
        'networkNamespace':'net:[1234]', 'hostAddresses':['192.0.2.1','2001:db8::1','127.0.0.1','::1'],
        'hostPorts':[8081,9000,9090],
        'targets':[{'address':'172.21.0.7','ports':[48081]},
                   {'address':'172.17.0.2','ports':[9000,9001]}]}


@pytest.mark.parametrize('field,value',[
    ('hostPorts',[22,8081]),('hostPorts',[True]),('hostPorts',[]),
    ('hostPorts',[8081,8081]),('hostPorts',[65536]),
    ('hostAddresses',['0.0.0.0/0']),('hostAddresses',['192.0.2.1; flush ruleset']),
    ('hostAddresses',['192.0.2.1']),('hostAddresses',['2001:db8::1']),
    ('networkNamespace','net:[1];'),('bootId','wrong'),
    ('targets',[{'address':'172.17.0.2','ports':[22]}]),
    ('targets',[{'address':'172.17.0.2','ports':[9000],'optional':True}])])
def test_rejects_unbound_or_overbroad_network_scope(field,value):
    module=importlib.import_module('formal_ingress_control')
    request=plan();request[field]=value
    with pytest.raises(module.snapshot.FormalSnapshotError,match='FORMAL_INGRESS_PLAN_INVALID'):
        module.render_policy('a'*64,request)


def test_policy_covers_forward_and_local_both_families_without_bypassing_established():
    module=importlib.import_module('formal_ingress_control')
    request=plan();before=copy.deepcopy(request)
    text=module.render_policy('a'*64,request).decode()
    assert 'create table inet ir_maint_' in text
    for hook in ['input','forward','output']:
        assert f'hook {hook} priority -10' in text
    assert 'ip6 daddr 2001:db8::1' in text
    assert 'ip daddr 172.17.0.2' in text
    assert 'ct state' not in text
    assert 'flush ruleset' not in text and 'accept' not in text
    assert request==before


def flows():
    return [{'source':'172.21.0.7','destination':'172.17.0.2','ports':[9000]}]


def test_recovery_policy_only_returns_exact_dependency_flow_in_owned_chains():
    module=importlib.import_module('formal_ingress_control')
    policy=module.render_recovery_policy('a'*64,plan(),flows()).decode()
    assert policy == ('insert rule inet ir_maint_'+('a'*24)+
        ' forward ip saddr 172.21.0.7 ip daddr 172.17.0.2 tcp dport { 9000 } return\n')
    assert 'accept' not in policy and 'output' not in policy and 'input' not in policy


@pytest.mark.parametrize('change',[
    {'source':'192.0.2.1'}, {'source':'0.0.0.0'}, {'source':'172.17.0.2'},
    {'source':'2001:db8::7'}, {'destination':'172.17.0.3'}, {'ports':[9000,3306]},
    {'ports':[22]}, {'extra':True}])
def test_recovery_policy_refuses_host_unknown_target_or_unapproved_ports(change):
    module=importlib.import_module('formal_ingress_control')
    selected={**flows()[0],**change}
    with pytest.raises(module.snapshot.FormalSnapshotError,match='FORMAL_INGRESS_RECOVERY_PLAN_INVALID'):
        module.render_recovery_policy('a'*64,plan(),[selected])


def test_recovery_policy_refuses_duplicate_flow():
    module=importlib.import_module('formal_ingress_control')
    with pytest.raises(module.snapshot.FormalSnapshotError,match='FORMAL_INGRESS_RECOVERY_PLAN_INVALID'):
        module.render_recovery_policy('a'*64,plan(),flows()*2)


def test_recovery_transition_atomically_requires_old_table_identity():
    module=importlib.import_module('formal_ingress_control')
    policy=module.render_recovery_transition('a'*64,plan(),flows(),71)
    assert policy==b'delete table inet handle 71\n'+module.render_policy('a'*64,plan())+module.render_recovery_policy('a'*64,plan(),flows())
    assert 'flush ruleset' not in policy.decode()


@pytest.mark.parametrize('handle',[0,-1,True,'71'])
def test_recovery_transition_refuses_unbound_handle(handle):
    module=importlib.import_module('formal_ingress_control')
    with pytest.raises(module.snapshot.FormalSnapshotError,match='FORMAL_INGRESS_RECOVERY_PLAN_INVALID'):
        module.render_recovery_transition('a'*64,plan(),flows(),handle)


@pytest.mark.parametrize('callback',['native','thaw'])
def test_recovery_transition_uses_one_canonical_flow_snapshot(tmp_path,monkeypatch,callback):
    module=importlib.import_module('formal_ingress_control');selected=flows();approved=copy.deepcopy(selected)
    class Claim:
        def __init__(self):self.root=tmp_path;self._plan=b'owned-binding';self.events=[]
        def assert_owned(self):pass
        def _read(self,path):return path.read_bytes()
        def _write(self,path,raw):path.write_bytes(raw)
        def record(self,step,details):self.events.append({'step':step,'details':copy.deepcopy(details)})
    claim=Claim();(tmp_path/'ingress-plan.json').write_bytes(module.snapshot._canonical_bytes(plan()))
    (tmp_path/'ingress-installed.json').write_bytes(module.snapshot._canonical_bytes([{'table':{'handle':71}}]))
    held={'table':'owned','tableHandle':71,'rulesSha256':'a'*64};calls=[]
    monkeypatch.setattr(module,'assert_ingress',lambda *a,**k:held)
    monkeypatch.setattr(module,'_bound',lambda *a:({},b''))
    monkeypatch.setattr(module,'_verify_recovery_rules',lambda *a,**k:calls.append(copy.deepcopy(a[3])))
    class Native:
        def run(self,args,raw):
            selected[0]['ports']=[9001] if args==['-c','-f','-'] else [9000]
            if args==['-f','-']:calls.append(raw)
        def inspect(self,table):return [{'table':{'handle':72}}]
    def thaw():
        if callback=='thaw':selected[0]['ports']=[9001]
    module.permit_recovery_dependencies(claim,selected,guard=lambda:None,thaw_guard=thaw,native=Native())
    assert json.loads((tmp_path/'ingress-recovery-plan.json').read_bytes())==approved
    import hashlib
    assert calls[1]==approved and calls[0]==module.render_recovery_transition(hashlib.sha256(claim._plan).hexdigest(),plan(),approved,71)


def test_recovery_transition_checks_thaw_before_held_network_or_guard(monkeypatch):
    module=importlib.import_module('formal_ingress_control');seen=[]
    def unthawed():
        seen.append('thaw');raise module.snapshot.FormalSnapshotError('FORMAL_SHARED_THAW_REQUIRED')
    monkeypatch.setattr(module,'assert_ingress',lambda *a,**k:seen.append('network'))
    with pytest.raises(module.snapshot.FormalSnapshotError,match='FORMAL_SHARED_THAW_REQUIRED'):
        module.permit_recovery_dependencies(object(),flows(),guard=lambda:seen.append('guard'),
                                             thaw_guard=unthawed)
    assert seen==['thaw']


def test_pending_recovery_reconciliation_is_explicit_and_never_adopts_rules(monkeypatch):
    module=importlib.import_module('formal_ingress_control')
    monkeypatch.setattr(module,'_bound',lambda *a:({'table':'owned'},b''))
    monkeypatch.setattr(module,'_event',lambda claim,step:{} if step in
        ('ingress-installed','ingress-recovery-intent') else None)
    monkeypatch.setattr(module,'assert_ingress',lambda *a,**k:pytest.fail('must not adopt unacknowledged rule transition'))
    native=type('Native',(),{'inspect':lambda self,t:[{'table':{'name':t}}]})()
    assert module.observe_ingress_reconciliation(object(),native=native)=={
        'table':'owned','present':True,'actionReplayed':False,'state':'RECOVERY_UNCERTAIN_RETAINED'}


def test_recovery_readback_refuses_missing_new_rule_and_retained_handle_drift():
    module=importlib.import_module('formal_ingress_control')
    original=[{'table':{'family':'inet','name':'owned','handle':1}},
        {'rule':{'family':'inet','table':'owned','chain':'forward','handle':2,'expr':[{'drop':None}]}}]
    for changed in [original, [{**original[0]}, {'rule':{**original[1]['rule'],'handle':99}}]]:
        with pytest.raises(module.snapshot.FormalSnapshotError,match='FORMAL_INGRESS_RECOVERY_POLICY_CHANGED'):
            module._verify_recovery_rules(original,changed,'owned',flows(),plan())


def test_both_capture_chains_refuse_recovery_network_intent_before_shared_reads(monkeypatch):
    import formal_split_capture as split
    import formal_snapshot_linux as snapshot
    claim=type('Claim',(),{'events':[{'step':'ingress-recovery-intent','details':{}}],
                          'assert_owned':lambda self:None})()
    with pytest.raises(snapshot.FormalSnapshotError,match='FORMAL_SPLIT_STAGE_INVALID'):
        split._active(claim,set())
    monkeypatch.setattr(snapshot,'_claim_database_identity',lambda _:pytest.fail('capture touched shared state first'))
    with pytest.raises(snapshot.FormalSnapshotError,match='FORMAL_CAPTURE_CLAIM_STAGE_INVALID'):
        snapshot.capture_claimed_formal_payload(claim,None,'ruoyi','/owned/minio')


@pytest.mark.parametrize('when',['before','during'])
def test_combined_capture_barrier_refuses_new_recovery_phase_before_further_reads(monkeypatch,when):
    module=importlib.import_module('formal_snapshot_linux')
    claim=type('Claim',(),{'events':[{'step':'db-fenced','details':{'originalSha256':'a'*64}}],
                          'assert_owned':lambda self:None})()
    def transition():claim.events.append({'step':'ingress-recovery-intent','details':{}})
    if when=='before':transition()
    monkeypatch.setattr(module,'_claimed_database_original',lambda _:('owned-uuid',{},'a'*64))
    monkeypatch.setattr(module,'_database_flags',lambda *a:{'serverUuid':'owned-uuid','readOnly':1,'superReadOnly':1})
    monkeypatch.setattr(module,'observe_database_barrier',lambda _,*,dump_in_progress=False:transition() if when=='during' else None)
    monkeypatch.setattr(module,'_claimed_container',lambda *a:pytest.fail('capture read container after recovery began'))
    with pytest.raises(module.FormalSnapshotError,match='FORMAL_CAPTURE_CLAIM_STAGE_INVALID'):
        module.observe_claimed_capture_barrier(claim,object())
