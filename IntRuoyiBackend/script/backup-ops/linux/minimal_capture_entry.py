#!/usr/bin/env python3
"""Single-operator FULL/I1 entry; identical requests query the original operation.

The detached job observes the real worker exit. A caller timeout does not kill
the source-restoration worker and never authorizes a replacement operation.
"""
import argparse
from datetime import datetime, timezone
import os
from pathlib import Path
import subprocess
import sys
import time

from formal_capture_incremental import canonical
from formal_capture_recovery_inputs import capture_recovery_inputs, original_bytes
from formal_normal_owner import strict_document
from formal_snapshot_linux import require
from minimal_capture_support import (actual_pin, document, exclusive_json, path_checked, validate_job)
from minimal_capture_worker import failure_code


def now(): return datetime.now(timezone.utc).isoformat()


def operation_result(request, started, state, code, data, pins, exit_code):
    return dict(schemaVersion='backup-restore-operation-result/v1', operationId=request['operationId'],
        requestId=request['operationId'], action=request['action'], state=state, failureCode=code,
        message={'SUCCEEDED': 'Capture completed; source restored; worker exited zero.',
            'FAILED_RETAINED': 'Capture failed; retained operation materials describe the failure.',
            'TERMINAL_UNKNOWN': 'Query this original operationId; capture is not proven complete.'}[state],
        data=data, originalResultPins=pins, startedAt=started, finishedAt=now(), actualExitCode=exit_code)


def validate_completion(root, request, deadline):
    def guard(): require(time.monotonic() < deadline, 'MINIMAL_OUTPUT_VALIDATION_TIMEOUT')
    guard()
    completion, completion_pin = document(root/'worker-completion.json')
    require(set(completion) == {'schemaVersion', 'operationId', 'data', 'finishedAt'} and
        completion['schemaVersion'] == 'minimal-capture-worker-completion/v1' and
        completion['operationId'] == request['operationId'], 'MINIMAL_COMPLETION_INVALID')
    data = completion['data']
    require(data['captureRoot'] == str(root), 'MINIMAL_COMPLETION_SCOPE_INVALID')
    for key in ('captureInputsPin', 'archiveEntriesPin', 'capturePointPin', 'objectContentIndexPin',
            'sourceRestorationPin'):
        guard()
        reference = data[key]
        require(root in path_checked(reference['path']).parents, 'MINIMAL_COMPLETION_SCOPE_INVALID')
        original_bytes(reference)
    entries = strict_document(original_bytes(data['archiveEntriesPin']), 64*1024*1024)
    for relative, marker in entries.items():
        guard()
        require(type(relative) is str and relative and '\\' not in relative and
            all(p not in ('', '.', '..') for p in relative.split('/')) and not relative.startswith('/'),
            'MINIMAL_ARCHIVE_PATH_INVALID')
        path = path_checked(str(root/relative))
        if marker == 'directory':
            require(path.is_dir(), 'MINIMAL_ARCHIVE_CHANGED')
        else:
            reference = actual_pin(path, guard)
            require(marker == 'file|'+str(reference['bytes'])+'|'+reference['sha256'], 'MINIMAL_ARCHIVE_CHANGED')
    inputs = strict_document(original_bytes(data['captureInputsPin']), 64*1024*1024)
    point = strict_document(original_bytes(data['capturePointPin']), 64*1024*1024)
    supplement = document(root/'semantic-capture'/'supplement.json')[0]
    # The input record cannot include its own pin in archiveFilePins.
    payload_entries = {k: v for k, v in entries.items() if k != 'capture-recovery-inputs.json'}
    require(inputs == capture_recovery_inputs(point, source_root=str(root), entries=payload_entries, supplemental=supplement) and
        inputs['backupId'] == request['operationId'] and inputs['pointType'] == request['action'] and
        inputs['recoveryPoint'] == data['recoveryPoint'], 'MINIMAL_CAPTURE_OUTPUT_INVALID')
    restored = strict_document(original_bytes(data['sourceRestorationPin']), 1048576)
    require(restored['schemaVersion'] == 'minimal-capture-source-restoration/v1' and
        restored['restored'] is True and restored['errors'] == [], 'MINIMAL_SOURCE_NOT_RESTORED')
    guard(); return data, completion_pin


def run_job(root, binding):
    request = binding['request']; started = binding['startedAt']
    pins = [binding['configurationPin'], binding['requestPin']]
    exit_code = None; data = dict(captureRoot=str(root)); process = None
    try:
        for reference in pins: original_bytes(reference)
        config = strict_document(original_bytes(binding['configurationPin']), 64*1024*1024)
        # Worker consumes exclusive local copies, closing replacement of CLI input paths.
        exclusive_json(root/'job-configuration.json', config)
        exclusive_json(root/'job-request.json', request)
        worker = Path(__file__).with_name('minimal_capture_worker.py')
        with (root/'worker-stderr.log').open('xb') as log:
            os.chmod(root/'worker-stderr.log', 0o600)
            process = subprocess.Popen([sys.executable, '-B', str(worker), '--configuration',
                str(root/'job-configuration.json'), '--request', str(root/'job-request.json'),
                '--output', str(root/'worker-completion.json')], stdin=subprocess.DEVNULL,
                stdout=subprocess.DEVNULL, stderr=log, close_fds=True)
            exclusive_json(root/'worker-started.json', dict(pid=process.pid, at=now()))
            timeout = config['limits']['workSeconds']+config['limits']['cleanupSeconds']+10
            try: exit_code = process.wait(timeout=timeout)
            except subprocess.TimeoutExpired:
                # Do not stop a process that may still own source restoration.
                uncertain = operation_result(request, started, 'TERMINAL_UNKNOWN', 'MINIMAL_WORKER_EXIT_UNKNOWN',
                    data, pins, None)
                exclusive_json(root/'uncertain-result.json', uncertain)
                exit_code = process.wait()
        data['logRefs'] = [actual_pin(root/'worker-stderr.log')]
        if exit_code == 0:
            captured, reference = validate_completion(root, request, time.monotonic()+config['limits']['workSeconds'])
            data.update(captured); pins.append(reference)
            state, code = 'SUCCEEDED', None
        else:
            state, code = 'FAILED_RETAINED', 'MINIMAL_WORKER_FAILED'
            if (root/'worker-failure.json').is_file():
                failed, reference = document(root/'worker-failure.json')
                code = failed['failureCode']; pins.append(reference)
            if (root/'source-restoration.json').is_file():
                data['sourceRestorationPin'] = actual_pin(root/'source-restoration.json')
                restored, _ = document(root/'source-restoration.json')
                if restored.get('restored') is not True: code = 'MINIMAL_SOURCE_RESTORATION_FAILED'
            elif any(document(p)[0].get('stage') == 'STOP_REQUESTED' for p in root.glob('event-*.json')):
                code = 'MINIMAL_SOURCE_STATE_UNKNOWN'
            # Signals or incomplete restoration cannot establish a known source state.
            if exit_code < 0 or code.endswith(('UNCERTAIN', 'UNKNOWN')):
                state = 'TERMINAL_UNKNOWN'
        result = operation_result(request, started, state, code, data, pins, exit_code)
    except Exception as error:
        state = 'TERMINAL_UNKNOWN' if process is not None and process.poll() is None else 'FAILED_RETAINED'
        result = operation_result(request, started, state, failure_code(error), data, pins, exit_code)
    exclusive_json(root/'terminal-result.json', result)


def start_or_query(config, request, config_pin, request_pin):
    root = validate_job(config, request)
    identity = dict(configurationPin=config_pin, requestPin=request_pin, request=request)
    if root.exists():
        require(root.is_dir() and not root.is_symlink(), 'MINIMAL_OPERATION_PATH_INVALID')
        # Another caller may have created the directory and not published binding yet.
        # It is still the original operation; never start another worker in this case.
        if not (root/'operation.json').is_file():
            return operation_result(request, now(), 'TERMINAL_UNKNOWN', 'MINIMAL_OPERATION_BINDING_PENDING',
                dict(captureRoot=str(root)), [config_pin, request_pin], None)
        binding, _ = document(root/'operation.json')
        require(binding['request'] == request and all(all(binding[k][field] == identity[k][field]
            for field in ('bytes', 'sha256')) for k in ('configurationPin', 'requestPin')),
            'MINIMAL_OPERATION_ID_REUSED')
    else:
        try: root.mkdir(mode=0o700)
        except FileExistsError:
            return start_or_query(config, request, config_pin, request_pin)
        binding = dict(identity, startedAt=now())
        exclusive_json(root/'operation.json', binding)
        try:
            with (root/'job-stderr.log').open('xb') as log:
                os.chmod(root/'job-stderr.log', 0o600)
                process = subprocess.Popen([sys.executable, '-B', str(Path(__file__).absolute()),
                    '--job', str(root/'operation.json')], stdin=subprocess.DEVNULL,
                    stdout=subprocess.DEVNULL, stderr=log, start_new_session=True, close_fds=True)
            exclusive_json(root/'job-started.json', dict(pid=process.pid, at=now()))
        except OSError:
            result = operation_result(request, binding['startedAt'], 'FAILED_RETAINED', 'MINIMAL_JOB_START_FAILED',
                dict(captureRoot=str(root)), [config_pin, request_pin], None)
            exclusive_json(root/'terminal-result.json', result)
    end = time.monotonic()+config['limits']['clientWaitSeconds']
    while True:
        if (root/'terminal-result.json').is_file():
            result, _ = document(root/'terminal-result.json')
            require(result['operationId'] == request['operationId'], 'MINIMAL_OPERATION_RESULT_CHANGED')
            # Replay must fail if previously completed capture bytes were altered.
            if result['state'] == 'SUCCEEDED':
                validate_completion(root, request, time.monotonic()+config['limits']['workSeconds'])
            return result
        if (root/'uncertain-result.json').is_file():
            return document(root/'uncertain-result.json')[0]
        if time.monotonic() >= end:
            return operation_result(request, binding['startedAt'], 'TERMINAL_UNKNOWN', 'MINIMAL_OPERATION_IN_PROGRESS',
                dict(captureRoot=str(root), operationRef=str(root/'operation.json')),
                [config_pin, request_pin], None)
        time.sleep(min(.2, max(.001, end-time.monotonic())))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--configuration'); parser.add_argument('--request'); parser.add_argument('--output')
    parser.add_argument('--job', help=argparse.SUPPRESS)
    args = parser.parse_args()
    if args.job:
        require(not any((args.configuration, args.request, args.output)), 'MINIMAL_INTERNAL_ARGUMENTS_INVALID')
        binding, _ = document(args.job)
        run_job(Path(args.job).parent, binding)
        return 0
    require(all((args.configuration, args.request, args.output)), 'MINIMAL_ENTRY_ARGUMENTS_REQUIRED')
    request = None
    try:
        config, config_pin = document(args.configuration); request, request_pin = document(args.request)
        root = validate_job(config, request)
        output = path_checked(args.output)
        require(root != output and root not in output.parents and output not in
            (Path(args.configuration), Path(args.request)), 'MINIMAL_RESULT_SCOPE_INVALID')
        require(not output.exists(), 'MINIMAL_RESULT_EXISTS_USE_NEW_OUTPUT_PATH')
        result = start_or_query(config, request, config_pin, request_pin)
        exclusive_json(output, result)
        return {'SUCCEEDED': 0, 'FAILED_RETAINED': 1, 'TERMINAL_UNKNOWN': 3}[result['state']]
    except Exception as error:
        print(failure_code(error), file=sys.stderr)
        return 2


if __name__ == '__main__': sys.exit(main())
