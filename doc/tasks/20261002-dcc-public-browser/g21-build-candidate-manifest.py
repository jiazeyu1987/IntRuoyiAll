"""Read-only Git/task inventory; writes only this existing task's candidate JSON."""
from pathlib import Path
import collections
import hashlib
import json
import re
import subprocess
from datetime import datetime, timezone

INTEGRATION = Path('C:/IntRuoyi/20261001-dcc-integration')
MAIN = Path('C:/IntRuoyiAll-int_main')
OUTPUT = INTEGRATION / 'doc/tasks/20261002-dcc-public-browser'
MAIN_TASK = 'doc/tasks/20261001-dcc-integration-unblock'
TASKS = ['20260930-dcc-d-relations', '20261002-dcc-detail-integration',
         '20261002-dcc-public-backend-completion', '20261002-dcc-public-browser',
         '20261003-g19-job-startup-sync']
def git(root, *args):
    run = subprocess.run(['git', '-c', 'core.quotepath=false', '-c', 'core.safecrlf=false', *args], cwd=root, capture_output=True, check=True)
    return run.stdout.decode('utf-8')
def status(root):
    return [{'status': value[:2], 'path': value[3:]} for value in git(root, 'status', '--porcelain=v1', '-z', '--untracked-files=all').split('\0') if value]
def relative_text(value):
    if not isinstance(value, str): return None
    text = value.replace('\\', '/')
    for root in (str(INTEGRATION), str(MAIN)):
        if text.lower().startswith(root.lower() + '/'): text = text[len(root)+1:]
    return text if re.match(r'^(IntRuoyiBackend|IntRuoyiFronted|docs|doc/tasks)/', text) else None
def hashes(root, path):
    file = root / path
    if not file.is_file(): return {'exists': False}
    raw = file.read_bytes()
    result = {'exists': True, 'bytes': len(raw), 'sha256': hashlib.sha256(raw).hexdigest()}
    try: result['normalizedUtf8LfSha256'] = hashlib.sha256(raw.decode('utf-8-sig').replace('\r\n', '\n').encode('utf-8')).hexdigest()
    except UnicodeDecodeError: pass
    return result
preserved_file = MAIN / MAIN_TASK / 'goal-preserved-nontask-assets.json'
preserved = json.loads(preserved_file.read_text(encoding='utf-8-sig'))['preservedAssets']
exclusions = collections.defaultdict(set)
for item in preserved: exclusions[Path(item['workspace'])].add(item['path'])
preserved_check = []
for item in preserved:
    result = hashes(Path(item['workspace']), item['path'])
    preserved_check.append({**item, 'currentSha256': result.get('sha256'), 'recordedBytesStillMatch': result.get('sha256') == item['sha256']})

provenance = collections.defaultdict(set)
manifests = []
retired_paths = collections.defaultdict(set)
main_docs = MAIN / MAIN_TASK
for directory in [main_docs, *(INTEGRATION / 'doc/tasks' / task for task in TASKS)]:
    if not directory.exists(): continue
    for file in directory.glob('*.json'):
        if file.name == 'g21-git-candidate-manifest.json': continue  # Do not manufacture import provenance from this inventory itself.
        if not re.search(r'(manifest|fingerprint|receipt|delivery|import)', file.name): continue
        try: data = json.loads(file.read_text(encoding='utf-8-sig'))
        except (ValueError, UnicodeDecodeError): continue
        manifest_label = ('main/' if file.is_relative_to(MAIN) else 'integration/') + file.relative_to(MAIN if file.is_relative_to(MAIN) else INTEGRATION).as_posix()
        manifests.append(manifest_label)
        def walk(value):
            if isinstance(value, dict):
                for key, child in value.items():
                    if key == 'deletedPaths' and isinstance(child, list):
                        for deleted in child:
                            retired = relative_text(deleted)
                            if retired: retired_paths[retired].add(manifest_label)
                        continue
                    possible = relative_text(key)
                    if possible: provenance[possible].add(manifest_label)
                    walk(child)
            elif isinstance(value, list):
                for child in value: walk(child)
            else:
                possible = relative_text(value)
                if possible and not possible.startswith('doc/tasks/'):
                    provenance[possible].add(manifest_label)
        walk(data)

cross_reasons = {
 'IntRuoyiBackend/config/gxp-audit-policy.yaml': 'DCC reason/state lifecycle/configuration audit operations added; policy remains PENDING-REVIEW and requires Root policy approval evidence.',
 'IntRuoyiBackend/config/gxp-audit-policy-dcc-d.yaml': 'DCC relation/reference operation audit policy supplied by D delivery; compare with merged full policy to avoid duplicate deploy authority.',
 'IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/formcenter/runtime/FormCenterRuntimeServiceImpl.java': 'Independent DCC obsolete FormCenter freezes one obligation per selected department; repeated leader cannot dedupe obligations.',
 'IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/service/task/BpmTaskServiceImpl.java': 'APPROVE/REJECT entry delegates DccSignedTaskActionGuard to prevent unsigned BPM bypass.',
 'IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/service/task/DccSignedTaskActionGuard.java': 'Scoped DCC signed task/actor guard required by native and obsolete signing contract.',
 'IntRuoyiBackend/yudao-module-bpm/src/test/java/cn/iocoder/yudao/module/bpm/formcenter/runtime/DccWorkflowFormCenterObligationTest.java': 'DCC obsolete exact department obligation regression for actual FormCenter mapping.',
 'IntRuoyiBackend/yudao-module-bpm/src/test/java/cn/iocoder/yudao/module/bpm/service/task/DccWorkflowSignedTaskActionGuardTest.java': 'DCC signed generic BPM entry guard negative/regression proof.',
 'IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/api/user/dto/AdminUserRespDTO.java': 'Formal tenantId identity required for selected reviewer/file owner same-tenant validation; does not grant roles.',
 'IntRuoyiBackend/yudao-module-system/src/test/java/cn/iocoder/yudao/module/system/api/user/AdminUserApiImplPostIdsTest.java': 'Actual AdminUser API tenantId/postId mapping contract; read actual test for complete DCC dependency.',
 'IntRuoyiBackend/yudao-module-infra/src/main/java/cn/iocoder/yudao/module/infra/service/job/JobStartupSyncRunner.java': 'G19 formal default-enabled property controls global startup Quartz sync for authorized isolated DCC runtime.',
 'IntRuoyiBackend/yudao-module-infra/src/test/java/cn/iocoder/yudao/module/infra/service/job/JobStartupSyncRunnerTest.java': 'G19 real property false/true/missing context and scheduler-failure regression; separate from preserved infra FileController.',
 'IntRuoyiBackend/script/tests/test_dcc_b_schema_contract.py': 'B forward schema contract test matches DCC project/folder/attribute migration.',
 'IntRuoyiBackend/sql/mysql/20260920_dcc_project_product_create_approval.sql': 'Only migration metadata dependsOn strips erroneous .sql suffix; historical migration digest/ledger approval remains a manual Review gate, not automatic DB apply.',
 'IntRuoyiFronted/src/router/modules/remaining.ts': 'Public project/workbench management route gate and existing detail identity entry required by DCC UI closure.'
}
raw_pattern = re.compile(r'\.(log|png|jpe?g|webp|zip|class|dumpstream|args|bak|tmp)$|/(?:dist|target|node_modules|__pycache__|e2e-artifacts|evidence|backups|db-backup|tmp-[^/]+|isolated-[^/]+)(?:/|$)', re.I)
copied_snapshot_pattern = re.compile(r'\.java\.p15-green$|/(?:[^/]*diff-check|test-thread-diagnostic)\.txt$')
retained_ignored = {
 'doc/tasks/20261002-dcc-public-browser/dcc-public-ui-acceptance.e2e.cjs': 'Root-requested formal UI-only acceptance script with Cleanup Keep and AST safety validation; no E2E run yet.',
 'doc/tasks/20261002-dcc-public-browser/verify-ui-acceptance-preparation.cjs': 'Reproducible preparation AST/12-flow validation with Cleanup Keep; must retain to audit no API business actions.'
}
retained_preparation = {
 'doc/tasks/20261002-dcc-public-browser/g21-build-candidate-manifest.py': 'Reproducible read-only status/import/provenance inventory; writes only own task manifest.',
 'doc/tasks/20261002-dcc-public-browser/g21-git-candidate-manifest.json': 'Explicit read-only commit candidate snapshot, not stage/apply approval; final rescan required.',
 'doc/tasks/20261002-dcc-public-browser/g21-git-asset-review.md': 'Manual Review questions, exclusions/cross-module rationale and cleanup/task-state boundary.'
}
task_reports = {'task.md','execution-log.md','verification-report.md','integration-notes.md','coordination.md'}
integration_keep = set()
for task in TASKS:
    keep_file = INTEGRATION/'doc/tasks'/task/'task.md'
    if keep_file.exists():
        text = keep_file.read_text(encoding='utf-8-sig')
        found = re.search(r'^## Cleanup Keep\s*\n([\s\S]*?)(?=^## |\Z)',text,re.M)
        if found: integration_keep.update(re.findall(r'^- ([^\n]+)',found.group(1),re.M))
def classify(root, row):
    path = row['path']; provenance_sources = sorted(provenance.get(path, []))
    if path in exclusions[root]: return 'excluded_preserved_non_task', 'Explicit workspace/path in goal-preserved-nontask-assets.json', provenance_sources
    if path in retained_preparation and root == INTEGRATION: return 'candidate_current_task_report', retained_preparation[path], provenance_sources
    if copied_snapshot_pattern.search(path): return 'excluded_copied_production_or_raw_diagnostic', 'Temporary copied production snapshot or raw diff/thread output; official source/summary is authoritative, do not stage duplicate code.', provenance_sources
    if raw_pattern.search(path): return 'excluded_generated_byproduct', 'Raw execution/log/media/sandbox artifact; archive useful summaries before authorized cleanup, never blind stage.', provenance_sources
    if root == MAIN:
        if path.startswith(MAIN_TASK + '/'):
            name = Path(path).name
            if name in task_reports or name.endswith('-review.md') or name in {'goal-acceptance-matrix.md','goal-git-scope.md','manager-decisions.md','goal-preserved-nontask-assets.json'}:
                return 'candidate_main_goal_report', 'Root primary task final/Review/decision/scope document; exact freshness and cleanup still Root-owned.', provenance_sources
            if re.search(r'(import-manifest|integration-manifest|dependency-sync-manifest|freeze.*manifest|delivery-fingerprints|verification-receipt)', name):
                return 'candidate_provenance_receipt', 'Root received-source or final validation provenance, keep exact named manifest rather than all receipts.', provenance_sources
            return 'manual_review_main_task_artifact', 'Intermediate import/sync scripts, runtime/backup/SQL packages and heartbeat/test-count receipts require purpose/keep/security review; not automatic stage.', provenance_sources
        if path.startswith('docs/dcc-parallel-delivery/') or path == 'docs/product/dcc-final-requirements.html':
            return 'candidate_contract_document', 'Formal DCC target/ownership/contract document; main/integration divergence must be explicitly reconciled.', provenance_sources
        if path in cross_reasons or provenance_sources:
            return 'manual_review_main_duplicate_source', 'Task-related source also appears in main dirty tree; do not commit as unrelated or overwrite integration, compare authoritative source first.', provenance_sources
        if path.startswith('doc/tasks/20260930-dcc-four-module-implementation/') or path.startswith('doc/tasks/20260930-dcc-parallel-management/'):
            return 'manual_review_legacy_goal_task', 'Historical parallel/implementation orchestration may duplicate the current primary goal; Root must link/close superseded state without deleting history or inventing completion.', provenance_sources
        if path.startswith('doc/tasks/'):
            return 'excluded_unrelated_or_legacy_task', 'Old task/evidence tree outside current root/received task set; preserve untouched, do not stage wholesale.', provenance_sources
        return 'manual_review_unproven_main_asset', 'Dirty main asset not justified by inspected current-task imports; preserve until Root explicit review.', provenance_sources
    if path in cross_reasons: return 'candidate_necessary_cross_module', cross_reasons[path], provenance_sources
    if path.startswith('IntRuoyiBackend/yudao-module-dcc/') or path.startswith('IntRuoyiFronted/src/api/dcc/') or path.startswith('IntRuoyiFronted/src/views/dcc/'):
        return ('candidate_imported_or_reviewed_source' if provenance_sources else 'candidate_dcc_integration_source'), 'Formal DCC implementation/closure source or owned runtime tests; explicit import proof attached where available, latest Owner handoff/validation still required.', provenance_sources
    if path.startswith(('IntRuoyiFronted/tests/unit/dcc-', 'IntRuoyiFronted/tests/e2e/dcc-', 'IntRuoyiFronted/scripts/dcc-')):
        return 'candidate_owned_dcc_test', 'DCC regression/handler/SFC contract under permanent test path; historical assertion changes need associated behavior proof.', provenance_sources
    if path.startswith('IntRuoyiBackend/sql/mysql/2026') and '_dcc_' in path:
        return 'candidate_forward_migration', 'DCC forward schema/config/BPM migration; source candidate only, apply requires separate Root-approved exact closure/digest executor.', provenance_sources
    if path.startswith('doc/tasks/'):
        parts = path.split('/'); task = parts[2]; name = Path(path).name
        if task not in TASKS: return 'excluded_unrelated_or_legacy_task', 'Task directory not in current inspected delivery set.', provenance_sources
        if name in task_reports or name.endswith('-review.md') or name.endswith('-audit.md') or name in {'g15-current-public-flow-review.md','g15-ui-acceptance-plan.md'}:
            return 'candidate_current_task_report', 'Current child/received task objective/summary/Review evidence; retain current and historical status boundaries.', provenance_sources
        if path in integration_keep and Path(path).suffix in {'.md','.json'}:
            return 'candidate_current_task_retained_receipt', 'Exact current Owner Cleanup Keep report/structured receipt; raw outputs remain separately excluded.', provenance_sources
        if re.search(r'(fingerprint|manifest|test-counts|closeout)', name):
            return 'candidate_provenance_receipt', 'Named current handoff/verification manifest; confirm terminal receipt and no transient secrets before staging.', provenance_sources
        if path in retained_ignored: return 'candidate_ignored_retained_script', retained_ignored[path], provenance_sources
        return 'manual_review_task_artifact', 'Task-local intermediate helper/data/report; not blanket source candidate.', provenance_sources
    if path.startswith('docs/dcc-parallel-delivery/') or path == 'docs/product/dcc-final-requirements.html':
        return 'candidate_contract_document', 'Formal final DCC shared/ownership/requirements contract; main current contract may be newer.', provenance_sources
    if provenance_sources: return 'candidate_imported_or_reviewed_source', 'Exact path appears in received delivery/final source receipts; check current hash and scope before stage.', provenance_sources
    return 'manual_review_unproven_asset', 'No explicit imported/current scope evidence from current manifests; never auto stage by broad directory.', provenance_sources

sections = []
for root in (INTEGRATION, MAIN):
    rows = status(root)
    entries = []
    for row in rows:
        decision, reason, refs = classify(root, row)
        hash_fields = {'exists': True, 'hashDeferredToFinalSnapshot': True} if row['path']=='doc/tasks/20261002-dcc-public-browser/g21-git-candidate-manifest.json' and root==INTEGRATION else hashes(root,row['path'])
        item = {**row, 'decision': decision, 'reason': reason, 'provenance': refs, **hash_fields}
        if decision == 'candidate_dcc_integration_source': item['reviewFlags'] = ['No path in inspected import/fingerprint manifests; confirm current Owner handoff and final validation fingerprint.']
        if row['path']=='IntRuoyiBackend/config/gxp-audit-policy.yaml': item['reviewFlags']=['Policy header PENDING-REVIEW requires explicit Root GxP acceptance, not deploy-ready claim.']
        if row['path']=='IntRuoyiBackend/sql/mysql/20260920_dcc_project_product_create_approval.sql': item['reviewFlags']=['Historical SQL metadata-only diff still changes file digest; do not blanket execute or silently replace ledger hash.']
        entries.append(item)
    ignored = git(root,'ls-files','--others','--ignored','--exclude-standard','-z','--',
        'IntRuoyiFronted/src','IntRuoyiFronted/tests','IntRuoyiFronted/scripts','IntRuoyiBackend/yudao-module-dcc/src',
        *(f'doc/tasks/{task}' for task in TASKS), MAIN_TASK).split('\0')
    ignored = [p for p in ignored if p]
    ignored_entries=[]
    for path in ignored:
        if raw_pattern.search(path): decision, reason='excluded_generated_byproduct','Ignored raw artifact; summarize, keep outside final source commits.'
        elif path in retained_ignored and root==INTEGRATION: decision,reason='candidate_ignored_retained_script',retained_ignored[path]
        else: decision,reason='manual_review_ignored_artifact','Ignored non-log file not automatically retained; check exact Cleanup Keep/purpose and secret-bearing runtime configuration.'
        ignored_entries.append({'path':path,'decision':decision,'reason':reason,**(hashes(root,path) if decision.startswith('candidate') else {'exists':(root/path).is_file()})})
    states=[]
    task_names=TASKS+(['20261001-dcc-integration-unblock','20260930-dcc-four-module-implementation','20260930-dcc-parallel-management','20260930-agents-int-qms-workstation-context'] if root==MAIN else [])
    for task in task_names:
        file=root/'doc/tasks'/task/'task.md'
        if not file.exists(): continue
        value=file.read_text(encoding='utf-8-sig')
        found=re.search(r'^## Current Status\s*\n\s*([^\n]+)',value,re.M)
        keep=re.search(r'^## Cleanup Keep\s*\n([\s\S]*?)(?=^## |\Z)',value,re.M)
        keeps=re.findall(r'^- ([^\n]+)',keep.group(1),re.M) if keep else []
        states.append({'taskDirectory':f'doc/tasks/{task}','statusFirstText':found.group(1).strip() if found else None,'cleanupKeep':keeps,
            'dirtyRawCount':sum(1 for item in entries if item['path'].startswith(f'doc/tasks/{task}/') and item['decision']=='excluded_generated_byproduct'),
            'ignoredRawCount':sum(1 for item in ignored_entries if item['path'].startswith(f'doc/tasks/{task}/') and item['decision']=='excluded_generated_byproduct')})
    sections.append({'workspace':str(root),'branch':git(root,'branch','--show-current').strip(),'head':git(root,'rev-parse','HEAD').strip(),
        'stagedPaths':git(root,'diff','--cached','--name-only','-z').split('\0')[:-1],
        'candidatePaths':[item['path'] for item in entries if item['decision'].startswith('candidate_')],
        'forceAddPaths':[item['path'] for item in ignored_entries if item['decision']=='candidate_ignored_retained_script'],
        'manualReviewPaths':[item['path'] for item in entries if item['decision'].startswith('manual_review_')],
        'excludedDirtyPaths':[item['path'] for item in entries if item['decision'].startswith('excluded_')],
        'dirtyCount':len(entries),'classificationCounts':dict(collections.Counter(item['decision'] for item in entries)),
        'entries':entries,'ignoredEntries':ignored_entries,'ignoredCounts':dict(collections.Counter(item['decision'] for item in ignored_entries)),
        'taskStateAudit':states})

result={'kind':'read_only_git_candidate_snapshot_not_staging_authorization','version':1,'primaryTask':MAIN_TASK,
        'generatedAtUtc':datetime.now(timezone.utc).isoformat(),
        'childTask':'doc/tasks/20261002-dcc-public-browser','generatedFrom':'status/import/freeze/receipt manifests plus reviewed cross-module diffs',
        'limitations':['No git mutation, cleanup or worker write performed','Candidate does not prove business validation, approval, migration readiness or final completion','Rehash/rescan after Owners stop; this snapshot may drift during Root executor work'],
        'preservedNonTaskEntries':preserved_check,'provenanceManifests':sorted(manifests),'workspaces':sections,
        'declaredRetiredPaths':[{'path':p,'provenance':sorted(v)} for p,v in sorted(retired_paths.items())],
        'importedPathCoverage':{'distinctRepositoryPaths':len(provenance),
          'missingInIntegration':[{'path':p,'provenance':sorted(v)} for p,v in sorted(provenance.items()) if not (INTEGRATION/p).exists()]}}
output=OUTPUT/'g21-git-candidate-manifest.json'
output.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps({'manifest':output.relative_to(INTEGRATION).as_posix(),'preservedEntries':len(preserved_check),
                 'preservedHashMismatches':[x['workspace']+'/'+x['path'] for x in preserved_check if not x['recordedBytesStillMatch']],
                 'provenanceManifests':len(manifests),'workspaces':[{k:v for k,v in s.items() if k in {'workspace','branch','head','dirtyCount','classificationCounts','ignoredCounts'}} for s in sections]},ensure_ascii=False,indent=2))
