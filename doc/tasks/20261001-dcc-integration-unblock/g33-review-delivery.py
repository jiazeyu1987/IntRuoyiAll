"""Validate the frozen G33 evidence; optional Jar inspection never starts services."""
from pathlib import Path
import hashlib
import io
import json
import sys
import xml.etree.ElementTree as ET
import zipfile

MAIN = Path(__file__).resolve().parent
REPO = Path('C:/IntRuoyi/20261001-dcc-integration')
CHILD = REPO / 'doc/tasks/20261002-dcc-public-backend-completion'
DETAIL = REPO / 'doc/tasks/20261002-dcc-detail-integration'


def digest(path):
    with Path(path).open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def read(path):
    return json.loads(Path(path).read_text(encoding='utf-8-sig'))


def verify(item):
    path = Path(item['path'])
    if not path.is_absolute():
        path = REPO / path
    assert path.is_file(), str(path)
    assert digest(path) == item['sha256'], 'hash drift: ' + str(path)
    if 'bytes' in item:
        assert path.stat().st_size == item['bytes'], 'length drift: ' + str(path)
    return path


manifest_path = CHILD / 'g33-maintenance-entry-delivery-fingerprints.json'
assert digest(manifest_path) == '8db5aa1cd6e23f59e5f2c73829a43e0944f546d30cb89997b4deaa0d724b99b8'
manifest = read(manifest_path)
assert manifest['productionJavaClasses'] == manifest['testJavaClasses'] == 5
assert not manifest['actualRuntimeOrDatabaseActions']
for group in ['sourceFiles', 'taskAssets', 'compiledInventory']:
    for item in manifest[group]:
        verify(item)
assert len(manifest['sourceFiles']) == 10
assert len(manifest['compiledInventory']) == 16

verification_path = CHILD / 'g33-maintenance-entry-verification.json'
assert digest(verification_path) == '2b1a7b2c4ee29e756691b92047e91f8a83ae4d281fc8f0abd44348d77872f370'
receipt = read(verification_path)
assert receipt['mavenCliExitCode'] == 0
assert (receipt['tests'], receipt['testClasses'], receipt['failures'], receipt['errors'], receipt['skipped']) == (101, 7, 0, 0, 0)
verify(receipt['finalRawLog'])
totals = [0, 0, 0, 0]
for result in receipt['testResults']:
    suite = ET.parse(verify(result['rawXml'])).getroot()
    for index, key in enumerate(['tests', 'failures', 'errors', 'skipped']):
        actual = int(suite.attrib[key])
        assert actual == result[key]
        totals[index] += actual
assert totals == [101, 0, 0, 0]
for item in receipt['evidenceLogs']:
    verify(item)

closure_path = DETAIL / 'g33-independent-entry-closure-receipt.json'
closure = read(closure_path)
production = [item for item in manifest['sourceFiles'] if '/src/main/' in item['path']]
reviewed = {item['path']: item['sha256'] for item in closure['files'] if '/src/main/' in item['path']}
assert len(production) == len(reviewed) == 5
assert all(reviewed[item['path']] == item['sha256'] for item in production)
for item in read(DETAIL / 'g27-legacy-final-delivery-fingerprints.json')['files']:
    verify(item)
front = read(MAIN / 'g20-delivery-fingerprints.json')['assets']
for item in front:
    verify(item)
protected = read(MAIN / 'goal-preserved-nontask-assets.json')
for item in protected['preservedAssets']:
    assert digest(Path(item['workspace']) / item['path']) == item['sha256']

output = {
    'status': 'ROOT_REVIEWED_G33_SOURCE_AND_101_REGRESSION_NOT_RUNTIME_APPROVAL',
    'manifestSha256': digest(manifest_path),
    'verificationSha256': digest(verification_path),
    'independentClosureSha256': digest(closure_path),
    'tests': 101, 'testClasses': 7, 'failures': 0, 'errors': 0, 'skipped': 0,
    'sourceFilesVerified': 10, 'compiledClassesVerified': 16,
    'originalFrozenAssetsUnchanged': 41,
    'priorG20AssetsUnchanged': len(front), 'protectedNontaskAssetsUnchanged': 6,
    'actualQualityApproved': False, 'actualRegistrationRun': False,
    'actualSchemaOrObjectWrites': False, 'actualBrowserAcceptance': False,
    'actualLocalMerge': False,
}
if len(sys.argv) > 1:
    assert sys.argv[1:] == ['--jar']
    jar = REPO / 'IntRuoyiBackend/yudao-server/target/yudao-server-exec.jar'
    with zipfile.ZipFile(jar) as archive:
        names = [name for name in archive.namelist() if name.startswith('BOOT-INF/lib/yudao-module-dcc-') and name.endswith('.jar')]
        assert len(names) == 1
        module_bytes = archive.read(names[0])
        module_path = REPO / 'IntRuoyiBackend/yudao-module-dcc/target' / Path(names[0]).name
        assert hashlib.sha256(module_bytes).hexdigest() == digest(module_path)
        checks = []
        with zipfile.ZipFile(io.BytesIO(module_bytes)) as module:
            for item in manifest['compiledInventory']:
                if '/target/classes/' not in item['path']:
                    continue
                name = item['path'].split('/target/classes/', 1)[1]
                assert hashlib.sha256(module.read(name)).hexdigest() == item['sha256'], 'packaged class drift: ' + name
                checks.append(name)
        assert len(checks) == 7
    output['jar'] = {'path': str(jar), 'sha256': digest(jar), 'bytes': jar.stat().st_size,
                     'module': names[0], 'moduleSha256': hashlib.sha256(module_bytes).hexdigest(),
                     'compiledProductionClassesVerified': checks, 'actualServiceStarted': False}
    output['status'] = 'ROOT_REVIEWED_G33_PACKAGED_SOURCE_NOT_RUNTIME_APPROVAL'
    output_path = MAIN / 'g33-main-package-root-review.json'
else:
    output_path = MAIN / 'g33-delivery-root-review.json'
output_path.write_text(json.dumps(output, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print(json.dumps({'status': output['status'], 'tests': 101, 'output': str(output_path)}, ensure_ascii=False))
