"""Normal selected native input gate; authenticity is not resource admission.

All paths are independently installed private originals, not chosen by a capsule.
No grant/state writes or service effects occur in this finite gate.
"""
import base64
import binascii
import hashlib
import json
import subprocess
import sys
import time

from formal_boot_state import _read_secure
from formal_open_authorization import verify_selected_service_approval, _selected_binding
from formal_snapshot_linux import FormalSnapshotError, observation_timeout, require

CODE = 'RELEASE_SELECTED_NATIVE_ORIGINALS_INVALID'
ROLES = ('stopAuthority','restoreAuthority')

def selected_entry_current_guard(request, installation_guard):
    """Fresh independent source and readonly SQL owner from trusted generator.

    sourceProbe and mysqlCommand are executor code, never approval fields.
    Command output is compared privately; only fixed refusal codes escape.
    """
    from formal_selected_resource_admission import _json
    installation_guard(); observation_timeout(1)
    def execute(argv, data, code):
        try:
            result=subprocess.run(argv,input=data,stdout=subprocess.PIPE,stderr=subprocess.PIPE,
                timeout=min(5,observation_timeout(5)))
            require(result.returncode==0 and not result.stderr.strip() and len(result.stdout)<=2097152,code)
            return result.stdout
        except (OSError,subprocess.SubprocessError):
            raise FormalSnapshotError(code) from None
    source=execute([sys.executable,'-I','-B','-S','-'],request['sourceProbe'].encode('utf-8'),
        'RELEASE_SELECTED_CURRENT_SOURCE_CHANGED')
    try: observed=_json(source)
    except (FormalSnapshotError,ValueError,UnicodeError,RecursionError):
        raise FormalSnapshotError('RELEASE_SELECTED_CURRENT_SOURCE_CHANGED') from None
    require(_canonical(observed)==_canonical(request['sourceObservation']),'RELEASE_SELECTED_CURRENT_SOURCE_CHANGED')
    owner=request['owner']
    def literal(value):
        _check(type(value) is str and '\0' not in value and '\\' not in value and '\n' not in value and '\r' not in value)
        return "'"+value.replace("'","''")+"'"
    conditions=["deleted=b'0'","BINARY status=BINARY 'RUNNING'"]
    for field,key in (('operation_id','lockOperationId'),('release_tag','releaseTag'),
                      ('executor_host','executorHost'),('executor_pid','executorPid'),('executor_identity','executorIdentity')):
        conditions.append('BINARY '+field+'=BINARY '+literal(owner[key]))
    conditions.append('SHA2(lease_token,256)='+literal(owner['leaseTokenDigest']))
    sql=("SELECT CASE WHEN COUNT(*)=1 AND @@server_uuid="+literal(owner['serverUuid'])+
         " AND DATABASE()="+literal(owner['database'])+
         " AND COALESCE(SUM(CASE WHEN "+' AND '.join(conditions)+
         " THEN 1 ELSE 0 END),0)=1 THEN 'LOCK_OWNED' ELSE 'LOCK_LOST' END FROM infra_release_operation_lock "
         "WHERE BINARY target_environment=BINARY "+literal(owner['environment'])+";\n")
    answer=execute(['sh','-c',request['mysqlCommand']],sql.encode('utf-8'),'RELEASE_CAPTURE_LOCK_LOST')
    require(answer.strip()==b'LOCK_OWNED','RELEASE_CAPTURE_LOCK_LOST')
    installation_guard(); observation_timeout(1)


def run_selected_entry_request(request, *, installation_guard):
    """Called only by the fixed, installed-byte-verifying normal bootstrap."""
    _check(type(request) is dict and set(request)=={'privateRoot','collectionSha256','capsule','currentBindings','consumptionStorage',
        'attemptId','scopeDigest','expectedHost','expectedMachineId','owner','mysqlCommand','sourceProbe','sourceObservation'}
        and callable(installation_guard))
    from formal_selected_resource_admission import _read_inputs
    from formal_selected_service_consumption import consume_selected_service_authorities
    selected_entry_current_guard(request,installation_guard)
    request_bytes=_canonical(request)
    dispatch=request['currentBindings']['stopAuthority']['dispatchBinding']
    original=_internal(lambda:_read_inputs(request['privateRoot'],request['collectionSha256'],dispatch))
    key_path=request['privateRoot']+'/test.service-authority-public.der'
    key=_internal(lambda:_read_secure(key_path,0o644,4096,no_atime=True))
    _internal(lambda:_check(key[0]==_unpack(request['capsule']['publicKey'],4096)))
    def guard():
        selected_entry_current_guard(request,installation_guard)
        _internal(lambda:_check(_canonical(request)==request_bytes and
            _read_inputs(request['privateRoot'],request['collectionSha256'],dispatch)==original and
            _read_secure(key_path,0o644,4096,no_atime=True)==key))
    admitted=admit_selected_entry_resources(request['privateRoot'],request['collectionSha256'],
        request['capsule'],request['currentBindings'],request['attemptId'],request['scopeDigest'],
        expected_host=request['expectedHost'],expected_machine_id=request['expectedMachineId'],guard=guard)
    guard()
    return consume_selected_service_authorities(request['consumptionStorage'],admitted,request['capsule'],request['owner'],guard=guard)

def _check(ok):
    require(ok, CODE)

def _internal(action):
    try:
        return action()
    except (OSError, ValueError, TypeError, KeyError, AttributeError, RecursionError,
            FormalSnapshotError, binascii.Error):
        raise FormalSnapshotError(CODE) from None

def _canonical(value):
    return json.dumps(value,sort_keys=True,separators=(',',':'),allow_nan=False).encode('utf-8')

def _unpack(value, limit):
    _check(type(value) is dict and set(value)=={'sha256','sizeBytes','base64'} and
        type(value['sizeBytes']) is int and 0<value['sizeBytes']<=limit and type(value['base64']) is str and
        len(value['base64'])<=4*((limit+2)//3))
    raw=base64.b64decode(value['base64'],validate=True)
    _check(len(raw)==value['sizeBytes'] and base64.b64encode(raw).decode('ascii')==value['base64'] and
        hashlib.sha256(raw).hexdigest()==value['sha256'])
    return raw

def verify_selected_service_originals(capsule, current_bindings, attempt_id, scope_digest,
                                      approval_paths, key_path, *, guard):
    """Real A037 for identical original bytes; pending admission/consumption."""
    def preflight():
        _check(type(capsule) is dict and set(capsule)=={'schemaVersion','state','attemptId','scopeDigest',
            'expectedBindings','approvals','publicKey','signatureVerified','grantConsumed','sourceEffectsPerformed'} and
            capsule['schemaVersion']=='release-selected-test-native-authorization-input/v1' and
            capsule['state']=='NATIVE_INPUT_PINNED_RESOURCE_ADMISSION_REQUIRED' and callable(guard) and
            type(attempt_id) is str and bool(attempt_id) and capsule['attemptId']==attempt_id and
            type(scope_digest) is str and len(scope_digest)==64 and capsule['scopeDigest']==scope_digest)
        _check(all(capsule[key] is False for key in ('signatureVerified','grantConsumed','sourceEffectsPerformed')) and
            type(current_bindings) is dict and set(current_bindings)==set(ROLES) and
            type(approval_paths) is dict and set(approval_paths)==set(ROLES) and
            type(capsule['approvals']) is dict and set(capsule['approvals'])==set(ROLES) and
            len(set(approval_paths.values())|{key_path})==3 and
            _canonical(capsule['expectedBindings'])==_canonical(current_bindings))
        for role in ROLES:
            _check(current_bindings[role]['role']==role)
            _selected_binding(current_bindings[role])
        approvals={role:_unpack(capsule['approvals'][role],32768) for role in ROLES}
        key=_unpack(capsule['publicKey'],4096)
        _check(len(key)==44)
        return approvals,key,_canonical(current_bindings),_canonical(capsule)
    originals,key,binding_bytes,capsule_bytes=_internal(preflight)
    held={}
    def unchanged():
        _check(_canonical(current_bindings)==binding_bytes and _canonical(capsule)==capsule_bytes)
        for role in ROLES:
            value=_read_secure(approval_paths[role],0o600,32768,no_atime=True)
            _check(value[0]==originals[role])
            if role in held:_check(value==held[role])
            else:held[role]=value
        value=_read_secure(key_path,0o644,4096,no_atime=True)
        _check(value[0]==key)
        if 'key' in held:_check(value==held['key'])
        else:held['key']=value
    guard(); observation_timeout(1); _internal(unchanged)
    authorities={}
    for role in ROLES:
        result=verify_selected_service_approval(approval_paths[role],key_path,capsule['publicKey']['sha256'],
            current_bindings[role],now_seconds=int(time.time()),guard=guard)
        authorities[role]=result['authority']
    guard(); observation_timeout(1); _internal(unchanged)
    _internal(lambda:_check(authorities['stopAuthority']['grantId']!=authorities['restoreAuthority']['grantId']))
    return dict(state='SELECTED_SERVICE_ORIGINALS_VERIFIED_ADMISSION_REQUIRED',attemptId=attempt_id,
        scopeDigest=scope_digest,authorities=authorities,signatureVerified=True,grantConsumed=False,
        startedPersisted=False,sourceEffectsPerformed=False,releaseReady=False)


def admit_selected_entry_resources(private_root, collection_sha256, capsule, current_bindings,
                                    attempt_id, scope_digest, *, expected_host, expected_machine_id, guard):
    """One normal native entry: identical A037 originals then real resource reads.

    private_root/collection pin are independently installed executor inputs.
    No callback or path is accepted from a public approval body or inferred from
    resource roles. Missing installed custody/host/device/runtime facts refuse.
    """
    from formal_selected_resource_admission import admit_selected_test_resources, _read_inputs, _json
    from formal_selected_resource_observer import observe_selected_local_resources
    def bindings():
        _check(_canonical(current_bindings['stopAuthority']['dispatchBinding']) ==
               _canonical(current_bindings['restoreAuthority']['dispatchBinding']) and
               _canonical(current_bindings['stopAuthority']['sourceBinding']) ==
               _canonical(current_bindings['restoreAuthority']['sourceBinding']))
        source = current_bindings['stopAuthority']['sourceBinding']
        _check(source['scope']['host'] == expected_host and source['scope']['machineId'] == expected_machine_id)
        return current_bindings['stopAuthority']['dispatchBinding'], source
    dispatch, source = _internal(bindings)
    guard(); observation_timeout(1)
    original = _internal(lambda: _read_inputs(private_root,collection_sha256,dispatch))
    collection_path = private_root+'/test.capture-sources.json'
    collection = _json(original[1][collection_path][0])
    approval_paths = {role:private_root+'/'+collection['originals'][role]['file'] for role in ROLES}
    approval = verify_selected_service_originals(capsule,current_bindings,attempt_id,scope_digest,
        approval_paths,private_root+'/test.service-authority-public.der',guard=guard)
    resource = admit_selected_test_resources(private_root,collection_sha256,dispatch,source,
        observe=lambda plan:observe_selected_local_resources(plan,expected_host=expected_host,
            expected_machine_id=expected_machine_id,guard=guard),guard=guard)
    guard(); observation_timeout(1)
    _internal(lambda:_check(_read_inputs(private_root,collection_sha256,dispatch)==original))
    # The scan may outlive a signature's validity. Both finite approvals must
    # still cover the budget before a later author can consider consuming them.
    _internal(lambda:_check(all(time.time()+resource['plan']['resources']['budget']['totalSeconds']+1 <
        value['expiresAt'] for value in approval['authorities'].values())))
    return dict(state='SELECTED_NATIVE_ENTRY_POINT_IN_TIME_ADMITTED',attemptId=attempt_id,
        scopeDigest=scope_digest,resource=resource,authorities=approval['authorities'],
        signatureVerified=True,grantConsumed=False,startedPersisted=False,sourceEffectsPerformed=False,
        continuousProtectionProven=False,recoveryCompleted=False,releaseReady=False)
