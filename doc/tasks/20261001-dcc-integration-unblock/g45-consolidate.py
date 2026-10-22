"""Root's exact local DCC consolidation inventory and protected archive, no database/network calls."""
from pathlib import Path
import argparse
import collections
import hashlib
import json
import re
import subprocess
import tarfile

MAIN = Path('C:/IntRuoyiAll-int_main')
INTEGRATION = Path('C:/IntRuoyi/20261001-dcc-integration')
ROOT = MAIN / 'doc/tasks/20261001-dcc-integration-unblock'
BACKUP = Path('C:/IntRuoyiBackups/20261004-dcc-single-trunk-r2')
WORKSPACES = [MAIN, INTEGRATION, *(Path('C:/IntRuoyi') / ('20260930-dcc-' + x) for x in 'abcd')]
PROTECTED = {'AGENTS.md',
    'IntRuoyiBackend/yudao-module-infra/src/main/java/cn/iocoder/yudao/module/infra/controller/admin/file/FileController.java',
    'IntRuoyiBackend/yudao-module-infra/src/test/java/cn/iocoder/yudao/module/infra/controller/admin/file/FileControllerTest.java'}
GENERATED = re.compile(r'/(?:node_modules|target|dist|__pycache__|\.pytest-temp|\.runtime|\.git)(?:/|$)|(?:^|/)\.env(?:\.|$)|\.(?:log|png|jpe?g|webp|zip|jar|class|pyc|dumpstream|args|bak|tmp)$|\.java\.p15-green$', re.I)
INT_TASKS = ['20260930-dcc-d-relations', '20261002-dcc-detail-integration', '20261002-dcc-public-backend-completion', '20261002-dcc-public-browser', '20261003-g19-job-startup-sync']
MAIN_TASKS = ['20260930-dcc-parallel-management', '20260930-dcc-four-module-implementation', '20261001-dcc-integration-unblock']
DEFERRED = {'g34-real-ui-negative-runner.cjs', 'g34-real-ui-observer.cjs', 'g34-real-ui-contract-test.cjs'}
FORBIDDEN_LITERAL_VERIFIER = 'doc/tasks/20261002-dcc-public-browser/verify-ui-acceptance-preparation.cjs'

def git(repo, *arguments):
    run = subprocess.run(['git', '-c', 'core.quotepath=false', *arguments], cwd=repo, capture_output=True)
    if run.returncode:
        raise RuntimeError('Git failed: ' + arguments[0])
    return run.stdout

def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()

def write(path, value):
    Path(path).write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

def dirty(repo):
    return [{'status': x[:2].decode('ascii'), 'path': x[3:].decode('utf-8')} for x in git(repo, 'status', '--porcelain=v1', '-z', '--untracked-files=all').split(b'\0') if x]

def keep(repo, tasks):
    results = set()
    for task in tasks:
        folder = repo / 'doc/tasks' / task
        if not folder.exists():
            continue
        for p in folder.glob('*'):
            if p.is_file() and p.suffix in {'.md', '.json', '.py', '.sql', '.yaml', '.java', '.ps1', '.html'}:
                results.add(p.relative_to(repo).as_posix())
        taskfile = folder / 'task.md'
        if taskfile.exists():
            text = taskfile.read_text(encoding='utf-8-sig')
            for value in re.findall(r'^- (doc/tasks/[^\n`*]+)$', text, re.M):
                relative = value.strip().replace('\\', '/')
                if (repo / relative).is_file():
                    results.add(relative)
    return results

def plan():
    preserved = json.loads((ROOT / 'goal-preserved-nontask-assets.json').read_text(encoding='utf-8'))['preservedAssets']
    for row in preserved:
        if sha(Path(row['workspace']) / row['path']) != row['sha256']:
            raise ValueError('Protected non-task asset drift')
    sections = []
    for repo in [MAIN, INTEGRATION]:
        records = dirty(repo)
        selected = set()
        for row in records:
            relative = row['path']
            if relative in PROTECTED or GENERATED.search('/' + relative):
                continue
            if repo == INTEGRATION and relative.startswith(('IntRuoyiBackend/', 'IntRuoyiFronted/')):
                selected.add(relative)
            elif relative.startswith('docs/dcc-parallel-delivery/') or relative in {
                    'docs/product/dcc-final-requirements.html', 'docs/system/gxp-audit-trail-config-security-deployment.md'}:
                selected.add(relative)
            elif repo == MAIN and relative in {'docs/database-rules.md', 'docs/frontend-development.md', 'docs/experience-index.md'}:
                selected.add(relative)
        selected.update(keep(repo, MAIN_TASKS if repo == MAIN else INT_TASKS))
        selected = {p for p in selected if p not in PROTECTED and not GENERATED.search('/' + p)
                    and Path(p).name not in DEFERRED and not p.endswith('.txt')
                    and Path(p).suffix and Path(p).name != 'g45-consolidation-manifest.json'}
        assets = []
        for relative in sorted(selected):
            path = repo / relative
            if not path.is_file():
                raise ValueError('Candidate file missing')
            if path.stat().st_size > 12_000_000:
                raise ValueError('Candidate too large')
            payload = path.read_text(encoding='utf-8-sig')
            reviewed = payload
            if relative == FORBIDDEN_LITERAL_VERIFIER:
                # This exact existing AST verifier names the prohibited default in assert.doesNotMatch;
                # it never supplies the value to a login or a business request.
                lines = [line for line in payload.splitlines() if ('admin' + '123') in line]
                if len(lines) != 1 or not lines[0].startswith('assert.doesNotMatch(source, /'):
                    raise ValueError('Unexpected credential verifier structure')
                reviewed = payload.replace(lines[0], '')
            if ('admin' + '123') in reviewed or re.search(r'(?:UT-[0-9A-F]{32}|eyJ[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]+\.)', reviewed):
                raise ValueError('Potential runtime credential in candidate: ' + relative)
            assets.append({'path': relative, 'bytes': path.stat().st_size, 'sha256': sha(path)})
        sections.append({'workspace': str(repo), 'branch': git(repo, 'branch', '--show-current').decode().strip(),
                         'head': git(repo, 'rev-parse', 'HEAD').decode().strip(), 'candidates': assets,
                         'excludedDirtyPaths': [x['path'] for x in records if x['path'] not in selected],
                         'stagedPaths': [x.decode() for x in git(repo, 'diff', '--cached', '--name-only', '-z').split(b'\0') if x]})
    if sections[0]['branch'] != 'int_qms' or sections[1]['branch'] != 'codex/20261001-dcc-integration':
        raise ValueError('Unexpected development branch')
    if any(s['stagedPaths'] for s in sections):
        raise ValueError('Existing index needs ownership review')
    result = {'status': 'EXACT_LOCAL_DCC_CONSOLIDATION_CANDIDATES_NOT_FULL_BUSINESS_ACCEPTANCE',
              'target': 'int_qms', 'newUserOrder': 'consolidate first, then LD01-04 sequentially',
              'workspaces': sections, 'protectedNonTaskAssetsUnchanged': 6, 'noOriginPush': True,
              'knownFourLogicDifferencesRemain': True}
    write(ROOT / 'g45-consolidation-manifest.json', result)
    print(json.dumps({'status': result['status'], 'candidateCounts': [len(s['candidates']) for s in sections]}))

def archive():
    BACKUP.mkdir(exist_ok=False)
    receipts = []
    for repo in WORKSPACES:
        rows = dirty(repo)
        # Only task evidence and local config are at risk when retiring DCC worktrees.
        # Legacy output/runtime Jars and continuously written old logs are not this task's archive scope.
        ignored = [x.decode('utf-8') for x in git(repo, 'ls-files', '--others', '--ignored', '--exclude-standard', '-z', '--',
            'doc/tasks/', 'IntRuoyiFronted/.env*', 'IntRuoyiBackend/config/',
            ':(exclude)**/__pycache__/**', ':(exclude)**/node_modules/**', ':(exclude)**/target/**').split(b'\0') if x]
        names = sorted({x['path'] for x in rows} | set(ignored))
        files, nonfiles = [], []
        directory = BACKUP / repo.name
        directory.mkdir()
        for relative in names:
            path = repo / relative
            if path.is_file() and not path.is_symlink():
                files.append({'path': relative, 'bytes': path.stat().st_size, 'sha256': sha(path)})
            else:
                nonfiles.append({'path': relative, 'exists': path.exists(), 'symbolic': path.is_symlink()})
        bundle = directory / 'dirty-and-ignored-assets.tar.gz'
        with tarfile.open(bundle, 'x:gz', compresslevel=3) as target:
            for item in files:
                target.add(repo / item['path'], arcname=item['path'], recursive=False)
        with tarfile.open(bundle, 'r:gz') as source:
            for item in files:
                current = source.extractfile(item['path'])
                if current is None or hashlib.sha256(current.read()).hexdigest() != item['sha256']:
                    raise ValueError('Archive content verification failed')
        data = {'status': 'DIRTY_AND_NONDERIVED_IGNORED_ARCHIVE_EVERY_FILE_HASH_VERIFIED', 'workspace': str(repo),
                'branch': git(repo, 'branch', '--show-current').decode().strip(),
                'head': git(repo, 'rev-parse', 'HEAD').decode().strip(), 'gitStatus': rows, 'files': files,
                'nonFileRecords': nonfiles, 'archive': str(bundle), 'archiveSha256': sha(bundle),
                'derivableDependenciesNotArchived': ['node_modules', 'target', 'dist', '.runtime', '__pycache__'],
                'untouchedOldRuntimeOutputsNotInCurrentTaskScope': ['output/runtime', 'outputs', 'e2e_test'],
                'credentialsMayExistOnlyInProtectedArchive': True, 'worktreeRetired': False}
        write(directory / 'receipt.json', data)
        receipts.append({'workspace': str(repo), 'receipt': str(directory / 'receipt.json'),
                         'receiptSha256': sha(directory / 'receipt.json'), 'files': len(files), 'archiveBytes': bundle.stat().st_size})
        print(json.dumps({'archivedWorkspace': repo.name, 'verifiedFiles': len(files)}), flush=True)
    write(ROOT / 'g45-protected-archive-receipt.json', {'status': 'SIX_WORKSPACES_HASH_VERIFIED_PROTECTED_ARCHIVE', 'receipts': receipts,
         'originalFilesDeleted': False, 'gitHistoryBundlePending': True})

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('mode', choices=['plan', 'archive'])
    args = parser.parse_args()
    try:
        (plan if args.mode == 'plan' else archive)()
    except Exception as error:
        print(json.dumps({'status': 'CONSOLIDATION_STOPPED_FOR_REVIEW', 'errorType': type(error).__name__,
                          'safeReason': str(error) if isinstance(error, (ValueError, RuntimeError)) else 'inspect owned protected receipt'}))
        raise SystemExit(1) from None
