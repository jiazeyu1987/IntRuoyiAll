"""Root readonly canonical preimages and sealed manifest; never activates registration."""
from pathlib import Path
import hashlib
import importlib.util
import json
import shutil
import subprocess

ROOT = Path(__file__).resolve().parent
REPO = Path('C:/IntRuoyi/20261001-dcc-integration')
TASK = REPO / 'doc/tasks/20261002-dcc-detail-integration'
PROTECTED = Path('C:/IntRuoyiBackups/20261003-dcc-integration')


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def main():
    delivery = json.loads((TASK / 'g32-delivery-fingerprints.json').read_text())
    if sha(TASK / 'g32-delivery-fingerprints.json') != '348f8bae91e62fcf2b4e115f8f71b4c112904706f7f5c5145098f0c2ada669d8':
        raise ValueError('Reviewed manifest helper drift')
    for item in delivery['assets']:
        path = Path(item['path'])
        if sha(path) != item['sha256'] or path.stat().st_size != item['bytes']:
            raise ValueError('Sealed manifest helper assets differ')
    extraction = json.loads((TASK / 'g32-runtime-extraction-receipt.json').read_text())
    for item in extraction['assets']:
        path = Path(item['path'])
        if sha(path) != item['sha256'] or path.stat().st_size != item['bytes']:
            raise ValueError('Sealed canonical dependencies differ')
    spec = importlib.util.spec_from_file_location('g39_manifest_builder', TASK / 'g32-manifest.py')
    builder = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(builder)
    facts = PROTECTED / 'g23-legacy-name-facts.jsonl'
    receipt = PROTECTED / 'g39-all39-source-bytes/receipt.json'
    results = PROTECTED / 'g39-all39-source-bytes/source-bytes-results.jsonl'
    if builder.verify_bytes(facts.read_bytes(), receipt.read_bytes(), results.read_bytes())['status'] != 'COMPLETE_MATCH_SOURCES':
        raise ValueError('Complete actual original bodies required')
    scope = builder.reviewed_scope_contract(facts.read_bytes())
    destination = PROTECTED / 'g39-legacy-manifest-inputs'
    if destination.exists():
        raise ValueError('New protected manifest input directory required')
    docker, java = shutil.which('docker.exe'), shutil.which('java.exe')
    if not docker or not java:
        raise ValueError('Existing local runtimes required')
    # Docker environment is held only in this process and Java stdin; never serialized to disk/logs.
    inspected = subprocess.run([docker, 'inspect', '--format={{json .Config.Env}}', 'int-ruoyi-mysql'], capture_output=True)
    if inspected.returncode:
        raise ValueError('Current local container inspection failed')
    environment = json.loads(inspected.stdout)
    passwords = [value.split('=', 1)[1] for value in environment if value.startswith('MYSQL_ROOT_PASSWORD=')]
    if len(passwords) != 1 or not passwords[0]:
        raise ValueError('Existing local database credential absent')
    payload = {'jdbcUrl': 'jdbc:mysql://127.0.0.1:23306/ruoyi-vue-pro?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai',
               'username': 'root', 'password': passwords[0], 'factsSha256': scope['factsSha256'], 'exactScopeIds': scope['exactScopeIds']}
    cp = str(TASK / 'g32-java-runtime/classes') + ';' + str(TASK / 'g32-java-runtime/libs/*')
    process = subprocess.run([java, '-Dfile.encoding=UTF-8', '-cp', cp, 'cn.iocoder.yudao.module.dcc.service.file.G32ManifestSupport', 'collect-preimages-readonly'],
                             input=json.dumps(payload).encode(), capture_output=True)
    del payload, passwords, environment, inspected
    if process.returncode or process.stderr:
        raise ValueError('Canonical readonly helper failed; no raw diagnostics exposed')
    preimages = builder.strict(process.stdout)
    if preimages.get('status') != 'ACTUAL_JDBC_JAVA_ROW_HASH_RECEIPT':
        raise ValueError('Actual readonly preimage receipt absent')
    destination.mkdir()
    preimage_path = destination / 'canonical-preimages.json'
    preimage_path.write_bytes(process.stdout)
    decision = ROOT / 'g25-user-authorization.json'
    manifest = builder.build(facts.read_bytes(), receipt.read_bytes(), results.read_bytes(), decision.read_bytes(),
                             preimages, 'dcc_g39_verified_legacy_originals', 'Preserve verified historical original names under the confirmed user policy',
                             'dcc-g39-exact-legacy-registration')
    path = destination / 'activation-manifest.json'
    path.write_bytes((json.dumps(manifest, ensure_ascii=False, separators=(',', ':')) + '\n').encode())
    verified = subprocess.run([java, '-Dfile.encoding=UTF-8', '-cp', cp, 'cn.iocoder.yudao.module.dcc.service.file.G32ManifestSupport', 'verify-manifest',
                               str(path), sha(path), str(facts), str(receipt), str(results), str(decision)], capture_output=True)
    if verified.returncode or verified.stderr or verified.stdout.decode().strip() != 'ACTUAL_STRICT_JAVA_FACTORY_PASS_OFFLINE_NO_ACTIVATION':
        raise ValueError('Strict formal manifest factory did not accept prepared evidence')
    summary = {'status': 'ACTUAL_READONLY_CANONICAL_MANIFEST_AND_STRICT_FACTORY_PASS_NOT_ACTIVATED',
               'claims': len({e['claimId'] for e in manifest['evidence']}), 'versions': len(manifest['evidence']),
               'nameGroups': len({e['sourceName'] for e in manifest['evidence']}), 'manifestPath': str(path), 'manifestSha256': sha(path),
               'preimagesSha256': sha(preimage_path), 'sourceFactsSha256': sha(facts), 'actualReadOnlyConnection': preimages['actualReadOnlyConnection'],
               'strictJavaFactoryExit': 0, 'actualDatabaseWrites': False, 'actualQualityApproved': False,
               'legacyActivationExecuted': False, 'credentialsPersisted': False, 'runtimeApplicationCanonicalProfileProven': False}
    (ROOT / 'g39-legacy-manifest-root-review.json').write_text(json.dumps(summary, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({key: summary[key] for key in ['status', 'claims', 'versions', 'nameGroups', 'actualDatabaseWrites', 'legacyActivationExecuted']}))


if __name__ == '__main__':
    try:
        main()
    except Exception as error:
        print(json.dumps({'status': 'LEGACY_PREPARATION_FAILED_NOT_ACTIVATED', 'errorType': type(error).__name__}))
        raise SystemExit(1) from None
