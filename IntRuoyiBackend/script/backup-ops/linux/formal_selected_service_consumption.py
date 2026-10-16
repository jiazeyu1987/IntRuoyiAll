"""Selected author journal: durable distinct grants and STARTED, no source effects.

The installed normal caller must first perform A037 and real resource admission.
Storage is independently approved stable identity, never an input/future leaf.
Interrupted/unknown bytes are retained; no journal deletion, issuer or OPEN shim.
"""
import fcntl
import hashlib
import json
import os
import re
import stat
import time

from formal_boot_state import _directory, _read_secure, _signature
from formal_selected_release_entry import _canonical, _unpack
from formal_snapshot_linux import FormalSnapshotError, observation_timeout, require

CODE='RELEASE_SELECTED_CONSUMPTION_REFUSED'
ROLES=('stopAuthority','restoreAuthority')
LOCK=b'selected-service-consumption/v1\n'


def _check(value):require(value,CODE)


def _intent(admitted,capsule,owner):
    _check(type(admitted) is dict and admitted['state']=='SELECTED_NATIVE_ENTRY_POINT_IN_TIME_ADMITTED' and
        admitted['signatureVerified'] is True and admitted['grantConsumed'] is False and admitted['startedPersisted'] is False and
        admitted['sourceEffectsPerformed'] is False and admitted['releaseReady'] is False and
        re.fullmatch('[0-9a-f]{64}',admitted['attemptId']) and capsule['attemptId']==admitted['attemptId'] and
        capsule['scopeDigest']==admitted['scopeDigest'] and re.fullmatch('[0-9a-f]{64}',admitted['scopeDigest']))
    resource=admitted['resource'];_check(resource['state']=='SELECTED_RESOURCE_POINT_IN_TIME_ADMITTED' and
        type(resource['plan']['resources']['budget']['totalSeconds']) is int and
        resource['plan']['resources']['budget']['totalSeconds']>0)
    authorities=admitted['authorities'];expected=capsule['expectedBindings'];_check(set(authorities)==set(expected)==set(ROLES))
    for role in ROLES:
        body=authorities[role]
        _check(_canonical({k:body[k] for k in expected[role]})==_canonical(expected[role]) and body['role']==role and
            re.fullmatch('[0-9a-f]{32}',body['grantId']))
        envelope=json.loads(_unpack(capsule['approvals'][role],32768))
        import base64
        _check(_canonical(json.loads(base64.b64decode(envelope['payloadBase64'],validate=True)))==_canonical(body))
    _check(authorities[ROLES[0]]['grantId']!=authorities[ROLES[1]]['grantId'] and
        _canonical(expected[ROLES[0]]['sourceBinding'])==_canonical(expected[ROLES[1]]['sourceBinding']) and
        _canonical(expected[ROLES[0]]['dispatchBinding'])==_canonical(expected[ROLES[1]]['dispatchBinding']))
    scope=expected[ROLES[0]]['sourceBinding']['scope'];dispatch=expected[ROLES[0]]['dispatchBinding']
    _check(type(owner) is dict and set(owner)==set(scope)|{'lockOperationId','releaseTag','leaseTokenDigest','executorHost','executorPid','executorIdentity'} and
        all(owner[key]==value for key,value in scope.items()) and owner['releaseTag']==dispatch['releaseTag'] and
        owner['lockOperationId']=='test-'+owner['releaseTag'] and re.fullmatch('[0-9a-f]{64}',owner['leaseTokenDigest']))
    return dict(schemaVersion='release-selected-service-consumption-intent/v1',attemptId=admitted['attemptId'],
        scopeDigest=admitted['scopeDigest'],owner=owner,expectedBindings=expected,authorities=authorities,
        approvalSha256={role:capsule['approvals'][role]['sha256'] for role in ROLES},publicKeySha256=capsule['publicKey']['sha256'],
        collectionSha256=resource['collectionSha256'],resourcePlanSha256=hashlib.sha256(_canonical(resource['plan'])).hexdigest())


def consume_selected_service_authorities(storage,admitted,capsule,owner,*,guard):
    """Internal verified caller only; durable ACK precedes every downstream effect.

    Exact PREPARED may resume missing grant writes; exact STARTED is readonly.
    Cross-attempt grants, corrupt/orphan records and retained staging refuse.
    No retry, cleanup or source/service action occurs in this function.
    """
    guard();observation_timeout(1)
    try:
        intent=_intent(admitted,capsule,owner);raw=_canonical(intent);storage_raw=_canonical(storage);_check(len(raw)<=131072)
        _check(type(storage) is dict and set(storage)=={'root','fileSystemId','rootInode'} and
            type(storage['root']) is str and re.fullmatch('/[A-Za-z0-9_./-]+',storage['root']) and
            not storage['root'].endswith('/') and all(p not in ('','.','..') for p in storage['root'].split('/')[1:]) and
            type(storage['fileSystemId']) is int and type(storage['rootInode']) is int and storage['rootInode']>0 and
            storage['fileSystemId']==admitted['resource']['plan']['resources']['roles']['control']['fileSystemId'])
    except (OSError,ValueError,TypeError,KeyError,AttributeError,RecursionError):
        raise FormalSnapshotError(CODE) from None
    root=storage['root'];attempt=intent['attemptId'];digest=hashlib.sha256(raw).hexdigest()
    attempt_name='selected-service-attempt-'+attempt+'.json'
    grants={role:'selected-service-grant-'+intent['authorities'][role]['grantId']+'.json' for role in ROLES}
    grant_raw={role:_canonical(dict(schemaVersion='release-selected-service-grant-consumption/v1',
        attemptId=attempt,intentSha256=digest,role=role,grantId=intent['authorities'][role]['grantId'],
        approvalSha256=intent['approvalSha256'][role])) for role in ROLES}
    prepared=_canonical(dict(schemaVersion='release-selected-service-consumption/v1',state='PREPARED',intent=intent))
    started=_canonical(dict(schemaVersion='release-selected-service-consumption/v1',state='STARTED',intent=intent))
    held={}
    try:
        with _directory(root,no_atime=True) as (fd,_):
            info=os.fstat(fd)
            _check(info.st_uid==0 and stat.S_IMODE(info.st_mode)==0o700 and
                info.st_dev==storage['fileSystemId'] and info.st_ino==storage['rootInode'])
            root_identity=(info.st_dev,info.st_ino,info.st_mode,info.st_uid,info.st_gid)
            def current():
                guard();observation_timeout(1)
                _check(_canonical(_intent(admitted,capsule,owner))==raw and _canonical(storage)==storage_raw)
                now=time.time();budget=admitted['resource']['plan']['resources']['budget']['totalSeconds']
                _check(all(type(value['issuedAt']) is int and type(value['expiresAt']) is int and
                    value['issuedAt']<=now and now+budget+1<value['expiresAt'] for value in
                    (*admitted['authorities'].values(),*admitted['resource']['plan'].values())))
                with _directory(root,no_atime=True) as (other,_):
                    s=os.fstat(other);_check((s.st_dev,s.st_ino,s.st_mode,s.st_uid,s.st_gid)==root_identity)
                for name,value in held.items():_check(_read_secure(root+'/'+name,0o600,131072,no_atime=True)==value)
            def read(name):
                try:os.stat(name,dir_fd=fd,follow_symlinks=False)
                except FileNotFoundError:return None
                return _read_secure(root+'/'+name,0o600,131072,no_atime=True)
            def prior():
                value=read(attempt_name);markers={role:read(name) for role,name in grants.items()}
                if value is None:_check(all(item is None for item in markers.values()))
                else:
                    _check(value[0] in (prepared,started))
                    for role,item in markers.items():_check(item is None or item[0]==grant_raw[role])
                    if value[0]==started:_check(all(item is not None for item in markers.values()))
                _check(read('.selected-service-stage-'+attempt) is None)
                return value,markers
            # Refuse a pre-existing orphan/replay before even creating a lock.
            prior();current()
            lock_name='selected-service.lock'
            try:
                lock_fd=os.open(lock_name,os.O_CREAT|os.O_EXCL|os.O_RDWR|os.O_NOFOLLOW|os.O_CLOEXEC,0o600,dir_fd=fd)
            except FileExistsError:
                lock_fd=os.open(lock_name,os.O_RDWR|os.O_NOFOLLOW|os.O_NONBLOCK|os.O_CLOEXEC|os.O_NOATIME,dir_fd=fd)
            else:
                try:_check(os.write(lock_fd,LOCK)==len(LOCK));os.fsync(lock_fd);os.fsync(fd)
                except BaseException:os.close(lock_fd);raise
            try:
                s=os.fstat(lock_fd);_check(stat.S_ISREG(s.st_mode) and stat.S_IMODE(s.st_mode)==0o600 and s.st_uid==0 and s.st_nlink==1 and s.st_size==len(LOCK))
                _check(os.pread(lock_fd,len(LOCK)+1,0)==LOCK)
                try:fcntl.flock(lock_fd,fcntl.LOCK_EX|fcntl.LOCK_NB)
                except BlockingIOError:raise FormalSnapshotError('RELEASE_SELECTED_CONSUMPTION_BUSY') from None
                held[lock_name]=_read_secure(root+'/'+lock_name,0o600,131072,no_atime=True)
                _check(_signature(os.fstat(lock_fd))==held[lock_name][1])
                value,markers=prior();resumed=value is not None
                if value is not None:held[attempt_name]=value
                for role,item in markers.items():
                    if item is not None:held[grants[role]]=item
                def create(name,data):
                    current()
                    item=os.open(name,os.O_WRONLY|os.O_CREAT|os.O_EXCL|os.O_NOFOLLOW|os.O_CLOEXEC,0o600,dir_fd=fd)
                    try:
                        offset=0
                        while offset<len(data):
                            count=os.write(item,data[offset:]);_check(count>0);offset+=count
                        os.fsync(item)
                    finally:os.close(item)
                    os.fsync(fd)
                    checked=read(name);_check(checked is not None and checked[0]==data);held[name]=checked
                    current()
                if value is None:create(attempt_name,prepared)
                for role,item in markers.items():
                    if item is None:create(grants[role],grant_raw[role])
                if value is None or value[0]==prepared:
                    stage='.selected-service-stage-'+attempt;create(stage,started);current()
                    os.replace(stage,attempt_name,src_dir_fd=fd,dst_dir_fd=fd);held.pop(stage);held.pop(attempt_name)
                    os.fsync(fd)
                    checked=read(attempt_name);_check(checked is not None and checked[0]==started);held[attempt_name]=checked
                current()
                # Recheck real durable file/directory semantics even on resume.
                os.fsync(lock_fd);os.fsync(fd);current()
                return dict(state='SELECTED_NATIVE_ENTRY_STARTED_PROTECTION_REQUIRED',attemptId=attempt,
                    scopeDigest=intent['scopeDigest'],intentSha256=digest,startedSha256=hashlib.sha256(started).hexdigest(),
                    resumed=resumed,signatureVerified=True,grantConsumed=True,startedPersisted=True,sourceEffectsPerformed=False,
                    continuousProtectionProven=False,recoveryCompleted=False,releaseReady=False)
            finally:os.close(lock_fd)
    except (OSError,ValueError,TypeError,KeyError,AttributeError,RecursionError):
        # Retain every uncertain PREPARED/marker/staging/STARTED byte; no cleanup.
        raise FormalSnapshotError('RELEASE_SELECTED_CONSUMPTION_UNCONFIRMED') from None
