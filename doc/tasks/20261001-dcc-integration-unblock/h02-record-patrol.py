"""Compare H02 evidence and fingerprints with H01 without inferring client state."""
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
assert previous['patrol_id'] == 'H01', 'Record ordering changed; inspect the intervening heartbeat'
rows = []
for old in previous['workers']:
    repo = Path(old['worktree'])
    row = dict(old)
    files = []
    for old_file in old['files']:
        path = repo / old_file['path']
        raw = path.read_bytes()
        files.append({'path': old_file['path'], 'sha256': hashlib.sha256(raw).hexdigest(),
            'changed_since_previous': hashlib.sha256(raw).hexdigest() != old_file['sha256'],
            'mtime_utc': datetime.fromtimestamp(path.stat().st_mtime, timezone.utc).isoformat()})
    task_file = next(f['path'] for f in files if f['path'].endswith('/task.md'))
    text = (repo / task_file).read_text(encoding='utf-8')
    match = re.search(r'(?m)^## Current Status\s*\n\s*([^\n]+)', text)
    row['task_status_text'] = match.group(1) if match else 'Current Status header not matched'
    row['files'] = files
    row['client_running_state'] = 'unavailable: no official thread status API'
    rows.append(row)
selected = json.loads((TASK / 'h02-regression-selected.json').read_text(encoding='utf-8-sig'))
reports = INTEGRATED / 'IntRuoyiBackend/yudao-module-dcc/target/surefire-reports'
suites = [ET.parse(p).getroot() for p in reports.glob('TEST-*.xml')]
suites = [s for s in suites if s.attrib['name'].split('.')[-1] in selected]
assert len(suites) == 33
assert all(int(s.attrib[key]) == 0 for s in suites for key in ('failures', 'errors', 'skipped'))
executions = sum(int(s.attrib['tests']) for s in suites)
assert executions == 608
children = [s for s in suites if s.attrib['name'].endswith(('DccProjectReservedDraftCombinationTest', 'DccProjectFolderDeletionCombinationTest'))]
names = [set(t.attrib['name'] for t in s.findall('testcase')) for s in children]
duplicates = len(names[0] & names[1])
assert duplicates == 27
counts = {'classes': 33, 'executions': executions, 'inherited_duplicate_executions': duplicates,
          'distinct_scenarios': executions - duplicates, 'failures': 0, 'errors': 0, 'skipped': 0}
(TASK / 'h02-test-counts.json').write_text(json.dumps(counts, indent=2)+'\n', encoding='utf-8')
report = {'automation_id': 'dcc', 'patrol_id': 'H02', 'previous_patrol_id': previous['patrol_id'],
          'trigger_at_utc': '2026-10-01T18:11:49.496Z', 'recorded_at_utc': datetime.now(timezone.utc).isoformat(),
          'thread_message_api_available': False, 'instructions_sent_to_threads': False,
          'pending_resume_thread_ids': previous['pending_resume_thread_ids'], 'workers': rows,
          'verification': counts, 'whole_business_complete': False,
          'actions': ['Accepted C 23-file frozen CC-2 contract and 27 reviewed increments',
                     'Preserved newer B shared schema fields while adding C INITIAL candidate role uniqueness',
                     'Repaired old A integration fixture with actual date, origin pointer and new scope/date dependencies',
                     'Retired incomplete browser quick-submit: exact selected version opens its management application entry',
                     'Passed 581 distinct backend scenarios, 36 C frontend cases, 2 new handler cases and 3 existing browser cases',
                     'Passed full existing types, targeted lint, main compile, configured frontend build and 7-migration closure',
                     'Synced 31 verified C/Root dependency files to each A and D with hash protection',
                     'Updated all four feedback files; actual thread messages not sent'],
          'remaining': ['A actual INITIAL/date/draft/submitReserved/cross-file rework transaction wiring',
                        'Root full application forms, relation/reference/approval evidence and distribution UI',
                        'D fresh combined acceptance after official C/B/A dependencies',
                        'No official independent-thread resume API; A/D commands pending'],
          'services_or_real_database_or_git_or_e2e': False}
raw = json.dumps(report, ensure_ascii=False, indent=2)+'\n'
(TASK / 'heartbeat-h02.json').write_text(raw, encoding='utf-8')
(TASK / 'heartbeat-latest.json').write_text(raw, encoding='utf-8')
print(json.dumps({'PASS': True, 'counts': counts, 'compared_with': previous['patrol_id'],
                  'client_state_not_inferred': True, 'actual_thread_messages_sent': False}, ensure_ascii=False))
