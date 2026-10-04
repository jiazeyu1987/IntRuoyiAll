"""Review accepted OWNER/pin source, actual tests and packaged current classes."""
from pathlib import Path
import hashlib
import io
import json
import zipfile

ROOT = Path(__file__).resolve().parent
REPO = Path('C:/IntRuoyi/20261001-dcc-integration')
TASK = REPO / 'doc/tasks/20261002-dcc-detail-integration'
BACKEND = REPO / 'doc/tasks/20261002-dcc-public-backend-completion'


def sha(path):
    with Path(path).open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def read(path):
    return json.loads(Path(path).read_text(encoding='utf-8-sig'))


def verify(item):
    path = REPO / item['path']
    assert sha(path) == item['sha256'] and path.stat().st_size == item['bytes'], str(path)


owner_path = TASK / 'g39-owner-delivery-fingerprints.json'
pin_path = TASK / 'g39-maintenance-pin-fingerprints.json'
assert sha(owner_path) == 'f0d435ee7acd9ccde8a7b10bf53c11f3a41db3e872e22068af95032bd06e6609'
assert sha(pin_path) == 'e290ccdf6d160175d34e6840b5187bee932a51b947d666124577457e4f876ad0'
owner, pin = read(owner_path), read(pin_path)
for item in owner['assets'] + pin['assets']:
    verify(item)
pin['currentValidator']['path']
verify(pin['currentValidator'])
owner_receipt = read(TASK / 'g39-owner-verification-receipt.json')
pin_receipt = read(TASK / 'g39-maintenance-pin-verification-receipt.json')
assert (owner_receipt['tests'], owner_receipt['classes'], owner_receipt['MavenExitCode']) == (154, 8, 0)
assert (pin_receipt['tests'], pin_receipt['classes'], pin_receipt['MavenExitCode']) == (28, 3, 0)
for receipt in [owner_receipt, pin_receipt]:
    assert all(receipt[k] == 0 for k in ['failures', 'errors', 'skipped'])
for item in [owner_receipt['GREEN'], owner_receipt['regression'], pin_receipt['GREEN']]:
    verify(item)
review = BACKEND / 'g39-project-owner-independent-review.md'
assert sha(review) == '7e52657bd98416a8bc9e2e62cbf8df58d7de3db40eb28c73627e4fbe5ea7db4b'
for item in read(TASK / 'g27-legacy-final-delivery-fingerprints.json')['files']:
    verify(item)
for item in read(BACKEND / 'g33-maintenance-entry-delivery-fingerprints.json')['sourceFiles']:
    if item['path'] not in set(pin['supersededG33PathsOnly']):
        verify(item)
for item in read(ROOT / 'goal-preserved-nontask-assets.json')['preservedAssets']:
    assert sha(Path(item['workspace']) / item['path']) == item['sha256']
run = read(ROOT / 'g39-main-package-execution.json')
assert run['exitCode'] == 0 and not run['actualServiceStarted']
jar = REPO / 'IntRuoyiBackend/yudao-server/target/yudao-server-exec.jar'
classes = []
with zipfile.ZipFile(jar) as application:
    modules = [n for n in application.namelist() if n.startswith('BOOT-INF/lib/yudao-module-dcc-') and n.endswith('.jar')]
    assert len(modules) == 1 and application.getinfo(modules[0]).compress_type == zipfile.ZIP_STORED
    data = application.read(modules[0])
    assert hashlib.sha256(data).hexdigest() == sha(REPO / 'IntRuoyiBackend/yudao-module-dcc/target' / Path(modules[0]).name)
    with zipfile.ZipFile(io.BytesIO(data)) as module:
        for item in owner['assets'] + pin['assets']:
            if '/src/main/java/' not in item['path']:
                continue
            name = item['path'].split('/src/main/java/', 1)[1].replace('.java', '.class')
            candidates = [n for n in module.namelist() if n == name or n.startswith(name[:-6] + '$') and n.endswith('.class')]
            assert name in candidates
            for candidate in candidates:
                path = REPO / 'IntRuoyiBackend/yudao-module-dcc/target/classes' / candidate
                assert hashlib.sha256(module.read(candidate)).hexdigest() == sha(path)
                classes.append({'class': candidate, 'sha256': sha(path)})
result = {'status': 'G39_OWNER_AND_VALIDATOR_PIN_REVIEWED_PACKAGED_NOT_REAL_UI_ACCEPTANCE',
          'ownerTests': 154, 'ownerClasses': 8, 'pinTests': 28, 'pinClasses': 3,
          'failures': 0, 'errors': 0, 'skipped': 0, 'packageExit': 0,
          'ownerSourceManifestSha256': sha(owner_path), 'pinSourceManifestSha256': sha(pin_path),
          'independentOwnerReviewSha256': sha(review),
          'jar': {'path': str(jar), 'bytes': jar.stat().st_size, 'sha256': sha(jar)},
          'nestedDccSha256': hashlib.sha256(data).hexdigest(), 'newProductionCompiledClasses': classes,
          'old41SourceUnchanged': True, 'oldG33SupersededOnlyGateAndGateTest': True,
          'protectedNontask6Unchanged': True, 'actualQualityApproved': False,
          'actualProjectBusinessDataCreated': False, 'actualBrowserAcceptance': False, 'localIntQmsMergeExecuted': False}
(ROOT / 'g39-software-root-review.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
print(json.dumps({'status': result['status'], 'ownerTests': 154, 'pinTests': 28, 'packageExit': 0, 'jarSha256': result['jar']['sha256']}))
