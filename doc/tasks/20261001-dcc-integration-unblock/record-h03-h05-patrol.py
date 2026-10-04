"""Close H03 evidence and record coalesced H04/H05 observations without client-state guesses."""
from pathlib import Path
from datetime import datetime, timezone
import hashlib
import json
import re
import xml.etree.ElementTree as ET

ROOT = Path('C:/IntRuoyiAll-int_main')
TASK = ROOT / 'doc/tasks/20261001-dcc-integration-unblock'
INTEGRATED = Path('C:/IntRuoyi/20261001-dcc-integration')
previous = json.loads((TASK / 'heartbeat-latest.json').read_text(encoding='utf-8'))
assert previous['patrol_id'] == 'H02', 'An intervening patrol requires manual ordering review'
bindings = json.loads((TASK / 'h03-automation-bindings.json').read_text(encoding='utf-8'))
for name in ('dcc-a', 'dcc-d', 'dcc'):
    assert bindings[name]['status'] == 'ACTIVE'
read_test_names = {'DccApplicationReadHttpContractTest', 'DccControlledFileDetailAuthorizationGuardTest',
                  'DccControlledFileSelectorDatabaseTest', 'DccControlledFileTaskActionApiTest'}
suites = [ET.parse(p).getroot() for p in (INTEGRATED / 'IntRuoyiBackend/yudao-module-dcc/target/surefire-reports').glob('TEST-*.xml')]
suites = [s for s in suites if s.attrib['name'].split('.')[-1] in read_test_names]
assert len(suites) == 4 and sum(int(s.attrib['tests']) for s in suites) == 27
assert all(int(s.attrib[key]) == 0 for s in suites for key in ('failures', 'errors', 'skipped'))
sync = json.loads((TASK / 'h03-dependency-sync.json').read_text(encoding='utf-8'))
for worker in sync['workers']:
    assert len(worker['verified_files']) == 8
    for row in worker['verified_files']:
        assert hashlib.sha256((Path(worker['path']) / row['path']).read_bytes()).hexdigest() == row['sha256']

rows = []
for old in previous['workers']:
    repo = Path(old['worktree'])
    row = dict(old)
    files = []
    for old_file in old['files']:
        path = repo / old_file['path']
        raw = path.read_bytes()
        sha = hashlib.sha256(raw).hexdigest()
        files.append({'path': old_file['path'], 'sha256': sha, 'changed_since_previous': sha != old_file['sha256'],
                      'mtime_utc': datetime.fromtimestamp(path.stat().st_mtime, timezone.utc).isoformat()})
    task_file = next(f['path'] for f in files if f['path'].endswith('/task.md'))
    match = re.search(r'(?m)^## Current Status\s*\n\s*([^\n]+)', (repo / task_file).read_text(encoding='utf-8'))
    row['task_status_text'] = match.group(1) if match else 'Current Status header not matched'
    row['client_running_state'] = 'unavailable: no official instantaneous thread status API'
    row['files'] = files
    rows.append(row)
now = datetime.now(timezone.utc).isoformat()
h03 = {'automation_id': 'dcc', 'patrol_id': 'H03', 'previous_patrol_id': 'H02',
       'trigger_at_utc': '2026-10-01T19:52:49.691Z', 'recorded_at_utc': now,
       'timed_resume_automations': bindings,
       'instant_thread_messages_sent': False, 'whole_business_complete': False,
       'verification': {'backend_http_and_query_cases': 27, 'new_http_cases': 3,
                        'new_loader_cases': 4, 'new_evidence_panel_cases': 4,
                        'related_selector_and_training_cases_not_added_to_new_cases': 19,
                        'types': 'PASS', 'targeted_lint': 'PASS', 'main_compile': 'PASS', 'configured_build': 'PASS'},
       'actions': ['Created and verified native existing-thread heartbeats dcc-a and dcc-d; updated main prompt without duplicates',
                   'Opened formal current-actor RevisionOptions/ApplicationEvidence HTTP routes through the existing Controller and Query service',
                   'Added exact projectFolder/global selector loader with authoritative pagination, Long, latest and content-permission facts',
                   'Mounted readonly frozen actual/default attributes for native file application and active obsolete BPM in detail page',
                   'Synced 8 Root read bridge/panel assets to A/D with hash preflight; corrected real process-definition type selection'],
       'runtime_or_real_database_or_git_or_e2e': False}
(TASK / 'heartbeat-h03.json').write_text(json.dumps(h03, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
h05 = {'automation_id': 'dcc', 'patrol_id': 'H05', 'previous_patrol_id': 'H03',
       'coalesced_triggers': ['2026-10-01T21:23:19.840Z', '2026-10-01T21:56:19.834Z'], 'recorded_at_utc': now,
       'workers': rows, 'timed_resume_automations': bindings, 'instant_thread_messages_sent': False,
       'actual_execution_observations': {
           'A': {'evidence': 'h02-save-fork-regression2.log / h02-public-create-verify3.log',
                 'observed_pass_executions': 233, 'new_public_create_executions': 9, 'new_public_create_failures': 1,
                 'new_public_create_errors': 1, 'issue': 'H2 SQL deleted=b\'0\' in DccControlledFileMasterMapper.selectByNewLogicalIdentity',
                 'active_source_not_received': True, 'client_goal_not_inferred': True},
           'D': {'evidence': 'h02-formal-combination-baseline.log', 'observed_pass_executions': 100,
                 'task_status': 'in_progress', 'older_report_blocked_header_not_current_execution': True}},
       'actions': ['Recorded real A and D continuation evidence after official scheduled heartbeats',
                   'Granted A exact shared MasterMapper predicate edit for the public NEW creation SQL blocker; feedback and ownership updated',
                   'Preserved B/C frozen source; no unchanged green tests repeated',
                   'Closed H03 validation and synchronized read bridge records'],
       'whole_business_complete': False, 'runtime_or_real_database_or_git_or_e2e': False}
raw = json.dumps(h05, ensure_ascii=False, indent=2)+'\n'
(TASK / 'heartbeat-h05.json').write_text(raw, encoding='utf-8')
(TASK / 'heartbeat-latest.json').write_text(raw, encoding='utf-8')
print(json.dumps({'PASS': True, 'H03_backend_cases': 27, 'H03_new_frontend_cases': 8,
                  'A_D_observed_actual_continuation': True, 'instant_messages_sent': False,
                  'patrols_closed': ['H03', 'H04/H05'], 'whole_business_complete': False}))
