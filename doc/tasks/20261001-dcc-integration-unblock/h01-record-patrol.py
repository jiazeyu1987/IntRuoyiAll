"""Capture evidence-based module states and fingerprints for the next heartbeat."""
from pathlib import Path
from datetime import datetime, timezone
import hashlib
import json
import re
import xml.etree.ElementTree as ET

ROOT = Path('C:/IntRuoyiAll-int_main')
TASK = ROOT / 'doc/tasks/20261001-dcc-integration-unblock'
INTEGRATED = Path('C:/IntRuoyi/20261001-dcc-integration')
prior_path = TASK / 'heartbeat-latest.json'
prior = json.loads(prior_path.read_text(encoding='utf-8')) if prior_path.exists() else None
ids = {
    'a': '01a0f2a6-eb43-7e11-8e4e-cf951a911cbf',
    'b': '01a0f2a7-4005-77c3-80ab-be583a1a9a47',
    'c': '01a0f2a7-888d-7382-a8bc-8e379c1969f0',
    'd': '01a0f2a7-c122-76d0-8cfb-fd4bb2b1b1e5',
}
suffix = {'a': 'workflow', 'b': 'project', 'c': 'version', 'd': 'relations'}
root_be = 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/'
watched = {
    'a': [root_be + 'service/file/' + n for n in ('DccControlledFileWorkflowServiceImpl.java', 'DccControlledFileLifecycleService.java', 'DccWorkflowFileStateAudit.java', 'DccApplicationRoundService.java')],
    'b': [root_be + 'service/projectcode/' + n for n in ('attributes/DccProjectApplicationSnapshotService.java', 'folder/DccProjectFolderMaintenanceService.java', 'productcreate/DccProjectProductAuditService.java')],
    'c': [root_be + 'service/file/' + n for n in ('DccControlledFileRevisionServiceImpl.java', 'DccControlledFileQueryServiceImpl.java', 'DccControlledFileSelectorQuery.java')],
    'd': [root_be + 'service/file/relations/' + n for n in ('DccProjectReferenceService.java', 'DccRelationControlledEventConsumer.java', 'DccRelationRemediationService.java')],
}
rows = []
for module in 'abcd':
    repo = Path('C:/IntRuoyi/20260930-dcc-' + module)
    task = repo / ('doc/tasks/20260930-dcc-' + module + '-' + suffix[module])
    task_text = (task / 'task.md').read_text(encoding='utf-8')
    match = re.search(r'(?m)^## Current Status\s*\n\s*([^\n]+)', task_text)
    paths = watched[module] + [str((task / name).relative_to(repo)).replace('\\', '/')
                              for name in ('task.md', 'verification-report.md', 'manager-feedback.md')]
    files = []
    for rel in paths:
        path = repo / rel
        if not path.is_file():
            continue
        raw = path.read_bytes()
        files.append({'path': rel, 'sha256': hashlib.sha256(raw).hexdigest(),
                      'mtime_utc': datetime.fromtimestamp(path.stat().st_mtime, timezone.utc).isoformat()})
    rows.append({'module': module.upper(), 'thread_id': ids[module], 'worktree': str(repo),
                 'task_status_text': match.group(1) if match else 'explicit current-status header not matched',
                 'client_running_state': 'unavailable: no official thread status API', 'files': files})

tests = ('DccWorkflowControlledRemediationIntegrationTest', 'DccWorkflowObsoleteTransactionTest',
         'DccWorkflowLifecycleTransactionTest', 'DccControlledFileFinalizationServiceImplTest',
         'DccControlledFileObsoleteServiceTest', 'DccRelationControlledEventIntegrationTest',
         'DccWorkflowObsoleteCompletionReadTest', 'DccProjectReservedDraftCombinationTest',
         'DccProjectFolderDeletionCombinationTest', 'DccApplicationDraftRoundTest',
         'DccApplicationRoundMappingTest', 'DccProjectProductLedgerTest', 'DccSignedRelationAssignmentContractTest')
reports = INTEGRATED / 'IntRuoyiBackend/yudao-module-dcc/target/surefire-reports'
suites = [ET.parse(p).getroot() for p in reports.glob('TEST-*.xml')]
suites = [s for s in suites if s.attrib['name'].split('.')[-1] in tests]
assert len(suites) == 13
executions = sum(int(s.attrib['tests']) for s in suites)
assert executions == 244
assert all(int(s.attrib[key]) == 0 for s in suites for key in ('failures', 'errors', 'skipped'))
children = [s for s in suites if s.attrib['name'].endswith(('DccProjectReservedDraftCombinationTest', 'DccProjectFolderDeletionCombinationTest'))]
duplicate_names = set(t.attrib['name'] for t in children[0].findall('testcase')) & set(t.attrib['name'] for t in children[1].findall('testcase'))
assert len(duplicate_names) == 27
counts = {'classes': 13, 'executions': executions, 'inherited_duplicate_executions': len(duplicate_names),
          'distinct_scenarios': executions - len(duplicate_names), 'failures': 0, 'errors': 0, 'skipped': 0}
(TASK / 'h01-test-counts.json').write_text(json.dumps(counts, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
report = {'automation_id': 'dcc', 'patrol_id': 'H01',
          'recorded_at_utc': datetime.now(timezone.utc).isoformat(),
          'trigger_at_utc': '2026-10-01T16:55:49.498Z', 'previous_patrol_id': prior.get('patrol_id') if prior else None,
          'thread_message_api_available': False, 'instructions_sent_to_threads': False,
          'pending_resume_thread_ids': [ids['a'], ids['d']], 'workers': rows,
          'verification': counts,
          'actions': ['Received frozen A audit delivery with source/target hash protection',
                      'Preserved newer D real signed-control scenarios in shared test merge',
                      'Registered four candidate file-state operations; coverage 32 operations/11 annotations',
                      'Synced 40 reviewed B/policy dependencies to A and 10 A/policy dependencies to D',
                      'Updated A/D continuation and C test-compile repair instructions; C active source untouched'],
          'whole_business_complete': False, 'services_or_real_database_or_git_or_e2e': False}
raw = json.dumps(report, ensure_ascii=False, indent=2)+'\n'
(TASK / 'heartbeat-h01.json').write_text(raw, encoding='utf-8')
prior_path.write_text(raw, encoding='utf-8')
print(json.dumps({'PASS': True, 'counts': counts, 'client_state_not_inferred': True,
                  'actual_thread_messages_sent': False, 'fingerprinted_workers': 4}, ensure_ascii=False))
