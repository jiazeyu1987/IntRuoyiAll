"""Verify packaged main-flow changes without starting a service or writing data."""
from pathlib import Path
import hashlib
import io
import json
import zipfile

MAIN = Path(__file__).resolve().parent
REPO = Path('C:/IntRuoyi/20261001-dcc-integration')
CHILD = REPO / 'doc/tasks/20261002-dcc-detail-integration'
BACKEND = REPO / 'doc/tasks/20261002-dcc-public-backend-completion'


def read(path):
    return json.loads(Path(path).read_text(encoding='utf-8-sig'))


def sha(path):
    with Path(path).open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def verify(item):
    path = REPO / item['path']
    assert sha(path) == item['sha256'], str(path)
    if 'bytes' in item:
        assert path.stat().st_size == item['bytes']
    return path


run = read(MAIN / 'g35-main-package-execution.json')
assert run['exitCode'] == 0 and not run['actualServiceStarted'] and not run['actualDatabaseWrites']
log = Path(run['log'])
assert 'BUILD SUCCESS' in log.read_text(encoding='utf-8', errors='replace')
matrix_path = CHILD / 'g34-matrix-delivery-fingerprints.json'
assert sha(matrix_path) == '06cb3caa852542337fa132b308be410d64994d4c1eaf173184163616be67b98e'
matrix = read(matrix_path)
for item in matrix['assets']:
    verify(item)
for item in read(CHILD / 'g27-legacy-final-delivery-fingerprints.json')['files']:
    verify(item)
g33 = read(BACKEND / 'g33-maintenance-entry-delivery-fingerprints.json')
for item in g33['sourceFiles']:
    verify(item)
for item in read(MAIN / 'goal-preserved-nontask-assets.json')['preservedAssets']:
    assert sha(Path(item['workspace']) / item['path']) == item['sha256']
jar = REPO / 'IntRuoyiBackend/yudao-server/target/yudao-server-exec.jar'
checks = []
with zipfile.ZipFile(jar) as application:
    modules = [name for name in application.namelist() if name.startswith('BOOT-INF/lib/yudao-module-dcc-') and name.endswith('.jar')]
    assert len(modules) == 1
    info = application.getinfo(modules[0])
    assert info.compress_type == zipfile.ZIP_STORED
    raw = application.read(modules[0])
    assert hashlib.sha256(raw).hexdigest() == sha(REPO / 'IntRuoyiBackend/yudao-module-dcc/target' / Path(modules[0]).name)
    with zipfile.ZipFile(io.BytesIO(raw)) as dcc:
        for item in matrix['assets']:
            if '/src/main/java/' not in item['path']:
                continue
            name = item['path'].split('/src/main/java/', 1)[1].replace('.java', '.class')
            prefix = name[:-6]
            names = [n for n in dcc.namelist() if n == name or n.startswith(prefix + '$') and n.endswith('.class')]
            assert name in names
            for entry in names:
                path = REPO / 'IntRuoyiBackend/yudao-module-dcc/target/classes' / entry
                actual = hashlib.sha256(dcc.read(entry)).hexdigest()
                assert actual == sha(path)
                checks.append({'class': entry, 'sha256': actual})
        for item in g33['compiledInventory']:
            if '/target/classes/' not in item['path']:
                continue
            name = item['path'].split('/target/classes/', 1)[1]
            assert hashlib.sha256(dcc.read(name)).hexdigest() == item['sha256']
assert len(checks) >= 2
output = {
    'status': 'ROOT_REVIEWED_CURRENT_MAIN_FLOW_JAR_NOT_RUNTIME_ACCEPTANCE',
    'packageExitCode': 0, 'packageLogSha256': sha(log),
    'jar': {'path': str(jar), 'bytes': jar.stat().st_size, 'sha256': sha(jar)},
    'nestedDcc': {'name': modules[0], 'sha256': hashlib.sha256(raw).hexdigest(), 'compression': 'STORED'},
    'matrixCompiledClasses': checks, 'G33CompiledClassesVerified': 7,
    'old41SourceUnchanged': True, 'G33Source10Unchanged': True, 'protectedNontask6Unchanged': True,
    'actualServiceStarted': False, 'actualDatabaseWrite': False, 'actualBrowserAcceptance': False,
    'actualLocalMerge': False,
}
path = MAIN / 'g35-main-package-root-review.json'
path.write_text(json.dumps(output, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print(json.dumps({'status': output['status'], 'jarSha256': output['jar']['sha256'], 'matrixClasses': len(checks)}, ensure_ascii=False))
