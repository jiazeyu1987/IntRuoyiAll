"""Run V01 current-schema static and real H2 contracts. Does not compile G05/application sources."""
import argparse
import difflib
import hashlib
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys

sys.stdout.reconfigure(encoding='utf-8')
TASK = Path(__file__).resolve().parent
ROOT = TASK.parents[2]
BE = ROOT / 'IntRuoyiBackend'
DCC_SCHEMA = 'IntRuoyiBackend/yudao-module-dcc/src/test/resources/sql/create_tables.sql'
SIG_SCHEMA = 'IntRuoyiBackend/yudao-module-signature/src/test/resources/sql/create_tables.sql'
FIXTURE = 'IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/category/DccFileCategoryAdminServiceImplTest.java'


def read(name):
    return (ROOT / name).read_text(encoding='utf-8-sig')


def table(source, name):
    matches = list(re.finditer(r'CREATE TABLE IF NOT EXISTS [`"]?' + re.escape(name) + r'[`"]?\s*\(.*?\n\);', source, re.S | re.I))
    if len(matches) != 1:
        raise ValueError('Expected exactly one current CREATE TABLE: ' + name)
    return matches[0].group()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--phase', required=True)
    args = parser.parse_args()
    output = TASK / ('v01-' + args.phase)
    output.mkdir(exist_ok=False)
    results = []

    def check(name, condition, detail=''):
        results.append({'case': name, 'pass': bool(condition), 'detail': detail})
        print(('PASS ' if condition else 'FAIL ') + name + (': ' + detail if detail else ''))

    dcc, signature, fixture = map(read, [DCC_SCHEMA, SIG_SCHEMA, FIXTURE])
    migration = read('IntRuoyiBackend/sql/mysql/20261014_dcc_delayed_effective_confirmation.sql')
    columns = re.findall(r"CALL ensure_dcc_delay_column\('([^']+)','([^']+)','([^']+)','([^']+)'\);", migration)
    check('formal-six-confirmation-columns', len(columns) == 6)
    declarations = {}
    for target, name, definition, kind in columns:
        statement = table(dcc, target)
        found = re.search(r'(?m)^\s*[`"]' + name + r'[`"]\s+([^,\n]+)', statement)
        expected = definition.upper().replace('DATETIME(6)', 'TIMESTAMP(6)')
        actual = found[1].strip().upper() if found else ''
        check(target + '.' + name + '.ddl', actual == expected, actual or 'MISSING')
        declarations[name] = (target, definition)
        camel = re.sub(r'_([a-z])', lambda match: match[1].upper(), name)
        owner = 'DccControlledFileDO.java' if target == 'dcc_controlled_file' else 'DccControlledFileSignatureDO.java'
        java = read('IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/dataobject/file/' + owner)
        check(owner + '.' + camel, bool(re.search(r'private\s+\w+\s+' + camel + r'\s*;', java)))
    for schema, target, name, null_clause in [(signature, 'system_electronic_signature', 'signed_at', 'NOT NULL'),
                                               (dcc, 'dcc_controlled_file_signature', 'signed_at', 'NULL'),
                                               (dcc, 'dcc_controlled_file', 'controlled_time', 'NULL')]:
        statement = table(schema, target)
        found = re.search(r'(?m)^\s*[`"]' + name + r'[`"]\s+([^,\n]+)', statement)
        check(target + '.' + name + '.explicit-timestamp6', bool(found and found[1].upper().strip() == 'TIMESTAMP(6) ' + null_clause))
    category = table(dcc, 'dcc_file_category')
    check('category.explicit-not-null-no-default', bool(re.search(r'`approval_source_mode` VARCHAR\(32\) NOT NULL,', category)))
    requests = list(re.finditer(r'DccFileCategorySaveReqVO\s+(\w+)\s*=\s*new DccFileCategorySaveReqVO\(\);', fixture))
    check('category.six-existing-requests', len(requests) == 6)
    for index, request in enumerate(requests):
        end = fixture.index('\n    }', request.end())
        body = fixture[request.end():end]
        check('category.request-' + str(index + 1) + '.explicit-source', bool(re.search(r'\b' + request[1] + r'\.setApprovalSourceMode\("PUBLIC_MATRIX"\)', body)))
    helper = fixture[fixture.index('private DccFileCategoryDO createCategory(String code, String name, String source, boolean active, int sort,'):]
    check('category.helper.explicit-source', '.approvalSourceMode("PUBLIC_MATRIX")' in helper[:helper.index('categoryMapper.insert(category);')])
    checkout = table(dcc, 'dcc_controlled_file_checkout')
    check('A21.checkout.explicit-CHAR64-null-no-default', bool(re.search(r'`checkin_payload_hash` CHAR\(64\) NULL,', checkout)))
    route_snapshot = table(dcc, 'dcc_controlled_file_route_snapshot')
    check('A26.route-snapshot.version-null-no-default', bool(re.search(r'`route_version_no` INT NULL,', route_snapshot)))
    for name, kind in [('draft_approval_source_mode', 'VARCHAR(32)'), ('draft_application_custom_matrix_json', 'LONGTEXT'), ('draft_application_context_key', 'VARCHAR(128)')]:
        check('A23.file.' + name + '.nullable-no-default', bool(re.search(r'`' + name + r'` ' + re.escape(kind) + r' NULL,', table(dcc, 'dcc_controlled_file'))))

    # Removing exactly the authorized hunks must restore every original assertion/table/column.
    baseline = json.loads((TASK / 'v01-source-hashes.json').read_text(encoding='utf-8'))
    def previous(name):
        return (TASK / 'v01-baseline' / baseline[name]['snapshot']).read_text(encoding='utf-8-sig')
    restored_dcc = dcc
    for line in ['  `approval_source_mode` VARCHAR(32) NOT NULL,\n',
                 '  `delayed_effective_time` TIMESTAMP(6) NULL,\n',
                 '  `delayed_effective_confirmed_at` TIMESTAMP(6) NULL,\n',
                 '  `delayed_effective_signature_id` BIGINT NULL,\n',
                 '  `delayed_effective_process_instance_id` VARCHAR(128) NULL,\n',
                 '  `selected_effective_time` TIMESTAMP(6) NULL,\n', '  `attribute_round` INT NULL,\n',
                 '  `checkin_payload_hash` CHAR(64) NULL,\n',
                 '  `draft_approval_source_mode` VARCHAR(32) NULL,\n',
                 '  `draft_application_custom_matrix_json` LONGTEXT NULL,\n',
                 '  `draft_application_context_key` VARCHAR(128) NULL,\n']:
        restored_dcc = restored_dcc.replace(line, '')
    restored_dcc = restored_dcc.replace('  `controlled_time` TIMESTAMP(6) NULL,', '  `controlled_time` DATETIME NULL,')
    restored_dcc = restored_dcc.replace('  `signed_at` TIMESTAMP(6) NULL,', '  `signed_at` DATETIME NULL,')
    restored_dcc = restored_dcc.replace('  `route_version_no` INT NULL,', '  `route_version_no` INT NOT NULL,')
    check('scope.dcc-all-other-schema-identical', restored_dcc == previous(DCC_SCHEMA))
    check('scope.signature-all-other-schema-identical', signature.replace('"signed_at" timestamp(6) not null,', '"signed_at" timestamp not null,') == previous(SIG_SCHEMA))
    restored_fixture = re.sub(r'^        (?:req|reqVO)\.setApprovalSourceMode\("PUBLIC_MATRIX"\);\n', '', fixture, flags=re.M)
    restored_fixture = restored_fixture.replace('                .approvalSourceMode("PUBLIC_MATRIX")\n', '')
    check('scope.category-all-original-assertions-identical', restored_fixture == previous(FIXTURE))

    paths = []
    for target, source in [('dcc_file_category', dcc), ('dcc_controlled_file', dcc), ('dcc_controlled_file_signature', dcc), ('system_electronic_signature', signature)]:
        name = output / (target + '.sql')
        name.write_text(table(source, target), encoding='utf-8')
        paths.append(name)
    dependencies = (BE / 'yudao-module-dcc/target/dcc-test.classpath').read_text(encoding='utf-8-sig').strip().split(';')
    h2 = [Path(value) for value in dependencies if re.search(r'[\\/]com[\\/]h2database[\\/]h2[\\/]', value)]
    if len(h2) != 1 or not h2[0].exists():
        raise FileNotFoundError('One real H2 dependency required')
    command = [shutil.which('java'), '-Dfile.encoding=UTF-8', '--source', '17', '-cp', str(h2[0]), str(TASK / 'V01SchemaContract.java'), *map(str, paths)]
    run = subprocess.run(command, cwd=ROOT, capture_output=True)
    text = (run.stdout + run.stderr).decode('utf-8', errors='replace')
    (output / 'h2.log').write_text(text, encoding='utf-8')
    print(text)
    h2_cases = []
    for line in text.splitlines():
        if line.startswith('CASE\t'):
            cells = line.split('\t', 3)
            h2_cases.append({'case': cells[1], 'pass': cells[2] == 'PASS', 'detail': cells[3] if len(cells) == 4 else ''})
    check('real-H2-test-discovery', len(h2_cases) == 16, 'H2 exit=' + str(run.returncode))
    results.extend(h2_cases)
    summary = {'phase': args.phase, 'results': results, 'pass': sum(item['pass'] for item in results),
               'fail': sum(not item['pass'] for item in results), 'h2_exit': run.returncode,
               'hashes': {name: hashlib.sha256((ROOT / name).read_bytes()).hexdigest() for name in [DCC_SCHEMA, SIG_SCHEMA, FIXTURE]}}
    (output / 'result.json').write_text(json.dumps(summary, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print('V01 SUMMARY pass=' + str(summary['pass']) + ' fail=' + str(summary['fail']))
    return 1 if summary['fail'] or run.returncode else 0


if __name__ == '__main__':
    sys.exit(main())
