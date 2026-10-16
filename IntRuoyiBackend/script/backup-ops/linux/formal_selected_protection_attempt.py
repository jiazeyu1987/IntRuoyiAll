"""Main-owned selected attempt custody; not a complete protection author.

Reuse the installed normal entry and its existing consumption lock/journal.
No public request extension, authority creation, source action or COMPLETE.
The control callback alone cannot prove post-stop SQL ownership/all writers.
"""
from contextlib import contextmanager
import copy
import fcntl
import hashlib
import json
import os
from pathlib import Path
import re
import stat
import sys
import threading
import time

import formal_snapshot_linux as snapshot
import formal_selected_release_entry as entry
from formal_boot_state import _directory, _read_secure, _signature
from formal_selected_resource_admission import _read_inputs
from formal_selected_service_consumption import LOCK, ROLES, _intent
from formal_snapshot_linux import FormalSnapshotError, require

ACK_FIELDS={'state','attemptId','scopeDigest','intentSha256','startedSha256','resumed',
    'signatureVerified','grantConsumed','startedPersisted','sourceEffectsPerformed',
    'continuousProtectionProven','recoveryCompleted','releaseReady'}
CONTROL='SELECTED_PROTECTION_CONTROL_CHANGED'
ACK='SELECTED_PROTECTION_ACK_INVALID'
BUDGET='SELECTED_PROTECTION_BUDGET_INVALID'


def _sha(raw):return hashlib.sha256(raw).hexdigest()


def _canonical(value):return entry._canonical(value)


def _boot():
    value=Path('/proc/sys/kernel/random/boot_id').read_text(encoding='ascii').strip()
    require(re.fullmatch('[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}',value),CONTROL)
    return value


class _SelectedProtectionAttempt:
    """Internal owner-thread controls; no public state/phase authorizer."""
    def __init__(self,request,installation_guard,acknowledgement,original,key,held,
                 root_identity,lock_fd,clock,wall,deadline,boot):
        self._request=request;self._request_bytes=_canonical(request)
        self._installation_guard=installation_guard;self._ack=copy.deepcopy(acknowledgement)
        self._original=original;self._key=key;self._held=held;self._root_identity=root_identity
        self._lock_fd=lock_fd;self._clock=clock;self._wall=wall;self._deadline=deadline;self._boot=boot
        self._owner_thread=threading.get_ident()

    @property
    def acknowledgement(self):return copy.deepcopy(self._ack)

    @property
    def resource_plan(self):return copy.deepcopy(self._original[0])

    def remaining_seconds(self):
        return snapshot.observation_timeout(self._deadline-time.monotonic())

    def assert_control_owned(self):
        """Frozen-safe controls only; NOT SQL ownership or continuous exclusion."""
        require(threading.get_ident()==self._owner_thread,CONTROL)
        self.remaining_seconds();self._installation_guard();self.remaining_seconds()
        elapsed=time.monotonic()-self._clock;now=time.time()
        require(elapsed>=0 and now>=self._wall and abs((now-self._wall)-elapsed)<2,BUDGET)
        request=self._request
        require(_canonical(request)==self._request_bytes and _boot()==self._boot,CONTROL)
        dispatch=request['currentBindings']['stopAuthority']['dispatchBinding']
        require(_read_inputs(request['privateRoot'],request['collectionSha256'],dispatch)==self._original,CONTROL)
        require(_read_secure(request['privateRoot']+'/test.service-authority-public.der',0o644,4096,no_atime=True)==self._key,CONTROL)
        root=request['consumptionStorage']['root']
        with _directory(root,no_atime=True) as (_,identity):require(identity==self._root_identity,CONTROL)
        require(_signature(os.fstat(self._lock_fd))==self._held['selected-service.lock'][1],CONTROL)
        for name,original in self._held.items():
            self.remaining_seconds()
            require(_read_secure(root+'/'+name,0o600,131072,no_atime=True)==original,CONTROL)
        self._installation_guard();self.remaining_seconds()

    def assert_pre_effect(self):
        """Before intentional source changes only: actual source plus SQL owner."""
        self.assert_control_owned()
        entry.selected_entry_current_guard(self._request,self._installation_guard)
        self.assert_control_owned()


@contextmanager
def hold_selected_protection_attempt(request, *, installation_guard):
    """Fresh entry/ACK followed by retained existing lock and private originals.

    No cached ACK argument, public callback or inferred stable storage accepted.
    Existing STARTED mtime anchors the total budget on exact same-attempt resume.
    Missing full phase mechanisms remain the complete author's prerequisite.
    """
    require(os.name=='posix' and os.geteuid()==0 and callable(installation_guard),
        'SELECTED_PROTECTION_OWNER_REQUIRED')
    try:
        trusted_request_bytes=_canonical(request)
        require(len(trusted_request_bytes)<=2097152,'SELECTED_PROTECTION_INPUT_INVALID')
    except (ValueError,TypeError,RecursionError):
        raise FormalSnapshotError('SELECTED_PROTECTION_INPUT_INVALID') from None
    # Every invocation goes through the existing real A037/admission/consumer.
    # This API never reconstructs success from a prior cached acknowledgement.
    answer=entry.run_selected_entry_request(request,installation_guard=installation_guard)
    require(_canonical(request)==trusted_request_bytes,CONTROL)
    lock_fd=None;token=None;entered=False
    try:
        require(type(answer) is dict and set(answer)==ACK_FIELDS and
            answer['state']=='SELECTED_NATIVE_ENTRY_STARTED_PROTECTION_REQUIRED' and
            answer['attemptId']==request['attemptId'] and answer['scopeDigest']==request['scopeDigest'] and
            type(answer['resumed']) is bool and
            all(answer[key] is True for key in ('signatureVerified','grantConsumed','startedPersisted')) and
            all(answer[key] is False for key in ('sourceEffectsPerformed','continuousProtectionProven','recoveryCompleted','releaseReady')) and
            all(type(answer[key]) is str and re.fullmatch('[0-9a-f]{64}',answer[key]) for key in ('intentSha256','startedSha256')),ACK)
        installation_guard();snapshot.observation_timeout(1)
        dispatch=request['currentBindings']['stopAuthority']['dispatchBinding']
        original=_read_inputs(request['privateRoot'],request['collectionSha256'],dispatch)
        key=_read_secure(request['privateRoot']+'/test.service-authority-public.der',0o644,4096,no_atime=True)
        require(key[0]==entry._unpack(request['capsule']['publicKey'],4096),ACK)
        storage=request['consumptionStorage'];root=storage['root']
        control=original[0]['resources']['roles']['control']['fileSystemId']
        require(storage['fileSystemId']==control and original[2][-1][0]==control and key[1][0]==control and
            all(value[1][0]==control for value in original[1].values()),CONTROL)
        with _directory(root,no_atime=True) as (directory,root_identity):
            info=os.fstat(directory)
            require(info.st_dev==control and info.st_ino==storage['rootInode'] and stat.S_IMODE(info.st_mode)==0o700,CONTROL)
            lock_fd=os.open('selected-service.lock',os.O_RDWR|os.O_NOFOLLOW|os.O_NONBLOCK|os.O_CLOEXEC|os.O_NOATIME,dir_fd=directory)
            lock=_read_secure(root+'/selected-service.lock',0o600,131072,no_atime=True)
            require(lock[0]==LOCK and _signature(os.fstat(lock_fd))==lock[1],CONTROL)
            try:fcntl.flock(lock_fd,fcntl.LOCK_EX|fcntl.LOCK_NB)
            except BlockingIOError:raise FormalSnapshotError('SELECTED_PROTECTION_BUSY') from None
            # Close the consumer return/acquire race by reading every exact
            # original/ACK/grant again under the retained existing lock.
            name='selected-service-attempt-'+request['attemptId']+'.json'
            started=_read_secure(root+'/'+name,0o600,131072,no_atime=True)
            require(_sha(started[0])==answer['startedSha256'],ACK)
            record=json.loads(started[0]);intent=record['intent']
            require(record==dict(schemaVersion='release-selected-service-consumption/v1',state='STARTED',intent=intent) and
                started[0]==_canonical(record) and _sha(_canonical(intent))==answer['intentSha256'],ACK)
            admitted=dict(state='SELECTED_NATIVE_ENTRY_POINT_IN_TIME_ADMITTED',attemptId=request['attemptId'],
                scopeDigest=request['scopeDigest'],resource=dict(state='SELECTED_RESOURCE_POINT_IN_TIME_ADMITTED',
                    collectionSha256=request['collectionSha256'],plan=original[0]),authorities=intent['authorities'],
                signatureVerified=True,grantConsumed=False,startedPersisted=False,sourceEffectsPerformed=False,releaseReady=False)
            require(_canonical(_intent(admitted,request['capsule'],request['owner']))==_canonical(intent),ACK)
            held={'selected-service.lock':lock,name:started}
            for role in ROLES:
                grant=intent['authorities'][role]['grantId'];filename='selected-service-grant-'+grant+'.json'
                saved=_read_secure(root+'/'+filename,0o600,131072,no_atime=True)
                expected=_canonical(dict(schemaVersion='release-selected-service-grant-consumption/v1',
                    attemptId=request['attemptId'],intentSha256=answer['intentSha256'],role=role,grantId=grant,
                    approvalSha256=intent['approvalSha256'][role]))
                require(saved[0]==expected,ACK);held[filename]=saved
            budget=original[0]['resources']['budget']['totalSeconds'];clock=time.monotonic();wall=time.time()
            values=(*intent['authorities'].values(),*original[0].values())
            require(type(budget) is int and 0<budget<=7200 and all(type(value['issuedAt']) is int and
                type(value['expiresAt']) is int and value['issuedAt']<=wall<value['expiresAt'] for value in values),BUDGET)
            started_at=started[1][7]/1000000000
            require(max(value['issuedAt'] for value in values)<=started_at<=wall,BUDGET)
            remaining=min(started_at+budget,*[value['expiresAt']-1 for value in values])-wall
            require(remaining>0,BUDGET)
            deadline=clock+remaining
            outer=snapshot.observation_deadline.get()
            if outer is not None:deadline=min(deadline,outer)
            require(deadline>time.monotonic(),'FORMAL_DATA_DEADLINE')
            token=snapshot.observation_deadline.set(deadline)
            attempt=_SelectedProtectionAttempt(request,installation_guard,answer,original,key,held,
                root_identity,lock_fd,clock,wall,deadline,_boot())
            attempt.assert_pre_effect()
            # Read back durability under retained ownership, also on restart.
            for filename in held:
                item=os.open(filename,os.O_RDWR|os.O_NOFOLLOW|os.O_NONBLOCK|os.O_CLOEXEC|os.O_NOATIME,dir_fd=directory)
                try:require(_signature(os.fstat(item))==held[filename][1],CONTROL);os.fsync(item)
                finally:os.close(item)
            os.fsync(directory);attempt.assert_pre_effect()
            entered=True
            yield attempt
            # The source may intentionally be stopped/frozen; never run the
            # unchanged-source SQL guard here. Final control custody is real.
            attempt.assert_control_owned()
    except (OSError,ValueError,TypeError,KeyError,AttributeError,RecursionError):
        if entered:raise
        raise FormalSnapshotError('SELECTED_PROTECTION_UNCONFIRMED_RETAINED') from None
    finally:
        primary=sys.exc_info()[1]
        try:
            if lock_fd is not None:
                try:os.close(lock_fd)
                except OSError:
                    if primary is not None:primary.ownerCleanupUncertain=True
                    else:raise FormalSnapshotError('SELECTED_PROTECTION_CLOSE_UNCONFIRMED') from None
        finally:
            if token is not None:snapshot.observation_deadline.reset(token)


def capture_selected_pre_effect_originals(request, *, installation_guard):
    """Actual finite main stage: five bound original copies under fresh custody.

    Returns only the existing pending-config result, never full protection.
    Full source fence/capture/approved recovery remain main engineering.
    """
    from formal_selected_config_capture import capture_selected_config_originals
    with hold_selected_protection_attempt(request,installation_guard=installation_guard) as attempt:
        source=request['currentBindings']['stopAuthority']['sourceBinding']
        originals=request['sourceObservation']['originals']
        require(type(originals) is dict and set(originals)==set(source['originalsSha256']) and
            all(item['sha256']==source['originalsSha256'][role] for role,item in originals.items()),CONTROL)
        role=attempt.resource_plan['resources']['roles']['control']
        with _directory(role['parent'],no_atime=True) as (parent,_):
            info=os.fstat(parent)
            require(info.st_dev==role['fileSystemId'] and info.st_ino==role['parentInode'] and
                stat.S_IMODE(info.st_mode)==0o700 and Path(role['path']).parent==Path(role['parent']),CONTROL)
        attempt.assert_pre_effect()
        result=capture_selected_config_originals(source['instanceIdentity']['target']['root'],originals,
            role['path'],guard=attempt.assert_pre_effect)
        attempt.assert_pre_effect()
        return dict(result,attemptId=request['attemptId'],scopeDigest=request['scopeDigest'],
            intentSha256=attempt.acknowledgement['intentSha256'],startedSha256=attempt.acknowledgement['startedSha256'],
            grantConsumed=True,startedPersisted=True,sourceEffectsPerformed=False,releaseReady=False)
