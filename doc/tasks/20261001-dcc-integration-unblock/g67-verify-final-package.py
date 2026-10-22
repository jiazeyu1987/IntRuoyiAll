"""Verify reviewed source pins and executable nested class bytes without writes to runtime data."""
from pathlib import Path
import hashlib
import io
import json
import subprocess
import zipfile

task = Path(__file__).resolve().parent
repo = task.parents[2]
load = lambda path: json.loads(path.read_text(encoding='utf-8-sig'))
digest = lambda data: hashlib.sha256(data).hexdigest()
previous = load(task / 'g64-final-package-stage-pins.json')
delivery = load(repo / 'doc/tasks/20261002-dcc-public-backend-completion/g67-current-relations-freeze-fingerprints.json')
pins = {row['path']: row for row in previous['assets']}
pins.update({row['path']: row for row in delivery['assets']})
for name in ['g63-approval-center-signoff-entry-fingerprints.json',
             'g64-upload-http-business-error-fingerprints-r2.json','g67-relation-remediation-notification-fingerprints.json']:
    frontend_delivery = load(repo / 'doc/tasks/20261002-dcc-public-browser' / name)
    pins.update({row['path']: row for row in frontend_delivery['assets']})
for path, row in pins.items():
    data = (repo / path).read_bytes()
    assert digest(data) == row['sha256'], path
    assert len(data) == row['bytes'], path
for row in delivery['compiledClasses']:
    data = (repo / row['path']).read_bytes()
    assert digest(data) == row['sha256'] and len(data) == row['bytes'], row['path']
jar = repo / 'IntRuoyiBackend/yudao-server/target/yudao-server-exec.jar'
classes = []
with zipfile.ZipFile(jar) as outer:
    for module in ['dcc', 'system']:
        names = [name for name in outer.namelist()
                 if name.startswith('BOOT-INF/lib/yudao-module-' + module + '-') and name.endswith('.jar')]
        assert len(names) == 1, names
        with zipfile.ZipFile(io.BytesIO(outer.read(names[0]))) as nested:
            for path in pins:
                prefix = 'IntRuoyiBackend/yudao-module-' + module + '/src/main/java/'
                if not path.startswith(prefix):
                    continue
                relative = path.removeprefix(prefix).removesuffix('.java') + '.class'
                class_root = repo / ('IntRuoyiBackend/yudao-module-' + module + '/target/classes')
                primary = class_root / relative
                assert primary.is_file(), relative
                for compiled in [primary, *sorted(primary.parent.glob(primary.stem + '$*.class'))]:
                    name = compiled.relative_to(class_root).as_posix()
                    data = compiled.read_bytes()
                    assert data == nested.read(name), name
                    classes.append({'module': module, 'class': name, 'sha256': digest(data)})
head = subprocess.run(['git', 'rev-parse', 'HEAD'], cwd=repo, capture_output=True, check=True).stdout.decode().strip()
receipt = {'status': 'FINAL_G67_REVIEWED_SOURCE_AND_NESTED_PACKAGE_PASS',
           'exitCode': 0, 'packageSession': 31492, 'sourceCommit': head,
           'jarSha256': digest(jar.read_bytes()), 'sourcePins': len(pins), 'classes': classes,
           'actualUI': 'PENDING_REPLACEMENT_CURRENT_RELATION_REAL_ACCEPTANCE', 'credentialsPersisted': False}
(task / 'g67-package-root-review.json').write_text(json.dumps(receipt, indent=2) + '\n', encoding='utf-8')
(task / 'g67-final-package-stage-pins.json').write_text(json.dumps({'assets': list(pins.values())}, indent=2) + '\n', encoding='utf-8')
print(json.dumps({'status': receipt['status'], 'sources': len(pins), 'classes': len(classes), 'sha256': receipt['jarSha256']}))
