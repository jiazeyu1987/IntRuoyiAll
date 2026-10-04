"""Read-only verification of the reviewed source and the final executable package."""
import hashlib
import io
import json
import pathlib
import zipfile

repo = pathlib.Path(__file__).resolve().parents[3]
task = pathlib.Path(__file__).resolve().parent
backend_task = repo / 'doc/tasks/20261002-dcc-public-backend-completion'
source_pins = {}
for name in ['g46-product-identity-fingerprints.json', 'g47-upload-template-fingerprints.json',
             'g48-storage-fingerprints.json', 'g49-backend-delivery-fingerprints.json']:
    document = json.loads((backend_task / name).read_text(encoding='utf-8-sig'))
    for row in document['files']:
        if '/src/main/java/' in row['path']:
            source_pins[row['path']] = row

def digest(data):
    return hashlib.sha256(data).hexdigest()

last = json.loads((backend_task / 'g49-backend-delivery-fingerprints.json').read_text(encoding='utf-8-sig'))
for row in last['compiledClassInventory']:
    data = (repo / row['path']).read_bytes()
    assert len(data) == row['bytes'] and digest(data) == row['sha256'], row['path']

jar_path = repo / 'IntRuoyiBackend/yudao-server/target/yudao-server-exec.jar'
classes = []
with zipfile.ZipFile(jar_path) as outer:
    entries = [name for name in outer.namelist() if name.startswith('BOOT-INF/lib/yudao-module-dcc-') and name.endswith('.jar')]
    assert len(entries) == 1, entries
    with zipfile.ZipFile(io.BytesIO(outer.read(entries[0]))) as nested:
        for relative, pin in source_pins.items():
            source = repo / relative
            data = source.read_bytes()
            assert len(data) == pin['bytes'] and digest(data) == pin['sha256'], relative
            class_relative = relative.split('/src/main/java/', 1)[1].replace('.java', '.class')
            directory = repo / 'IntRuoyiBackend/yudao-module-dcc/target/classes' / pathlib.PurePosixPath(class_relative).parent
            stem = pathlib.PurePosixPath(class_relative).stem
            primary = directory / (stem + '.class')
            assert primary.is_file(), str(primary)
            for compiled in [primary, *sorted(directory.glob(stem + '$*.class'))]:
                name = compiled.relative_to(repo / 'IntRuoyiBackend/yudao-module-dcc/target/classes').as_posix()
                compiled_data = compiled.read_bytes()
                assert nested.read(name) == compiled_data, name
                classes.append({'path': name, 'sha256': digest(compiled_data), 'bytes': len(compiled_data)})

with jar_path.open('rb') as artifact:
    jar_sha = hashlib.file_digest(artifact, 'sha256').hexdigest()
receipt = {'status': 'FOUR_DIRECTION_REVIEWED_SOURCE_AND_NESTED_PACKAGE_CLASS_BYTES_PASS_NOT_UI',
           'jar': {'path': jar_path.as_posix(), 'bytes': jar_path.stat().st_size, 'sha256': jar_sha},
           'reviewedProductionSources': len(source_pins), 'matchedCompiledClasses': classes,
           'lastStageCompiledInventoryEntries': len(last['compiledClassInventory']),
           'lastStageUniqueCompiledClasses': len({row['path'] for row in last['compiledClassInventory']}),
           'packageSession': 89977, 'packageExitCode': 0, 'databaseMigrated': False, 'actualUiVerified': False}
(task / 'g50-final-package-source-proof.json').write_text(json.dumps(receipt, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print(json.dumps({'status': receipt['status'], 'sources': len(source_pins),
                  'classes': len(classes), 'jarSha256': jar_sha}, ensure_ascii=False))
