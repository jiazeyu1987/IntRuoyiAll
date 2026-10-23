"""V02 source/AST-only contract: no production compilation and no service JUnit execution."""
import argparse
import base64
import difflib
import hashlib
import json
from pathlib import Path
import re
import subprocess
import sys

sys.stdout.reconfigure(encoding='utf-8')
TASK = Path(__file__).resolve().parent
ROOT = TASK.parents[2]


def digest(value):
    return hashlib.sha256(value).hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--phase', required=True)
    parser.add_argument('--h2', action='store_true')
    args = parser.parse_args()
    manifest = json.loads((TASK / 'v02-file-manifest.json').read_text(encoding='utf-8'))
    before_ast = json.loads((TASK / 'v02-assertions-before.json').read_text(encoding='utf-8'))
    output = TASK / ('v02-' + args.phase + '-results.json')
    if output.exists():
        raise FileExistsError(output)
    run = subprocess.run(['java', '-Dfile.encoding=UTF-8', '--source', '17', str(TASK / 'V02AssertionAst.java'),
                          *[str(ROOT / name) for name in manifest]], capture_output=True, cwd=ROOT)
    if run.returncode:
        raise RuntimeError(run.stderr.decode('utf-8', errors='replace'))
    actual_ast = {}
    decode = lambda value: base64.b64decode(value).decode('utf-8')
    for line in run.stdout.decode('utf-8').splitlines():
        cells = line.split('\t')
        name = Path(decode(cells[1])).relative_to(ROOT).as_posix()
        if cells[0] == 'FILE':
            actual_ast[name] = []
        elif cells[0] == 'AST':
            actual_ast[name].append({'method': decode(cells[2]), 'kind': cells[3], 'tree': decode(cells[4])})
    results = []
    minimal = []
    modes = []
    for name, entry in manifest.items():
        data = (ROOT / name).read_bytes()
        original = (TASK / 'v02-baseline' / entry['snapshot']).read_bytes()
        # Exactly one newly added call per authorized existing builder; preserve newlines/encoding.
        restored = data.replace(b'.approvalSourceMode("PUBLIC_MATRIX")', b'')
        source = data.decode('utf-8-sig')
        builders = list(re.finditer(r'DccFileCategoryDO\.builder\(\).*?\.build\(\)', source, re.S))
        actual = sum('.approvalSourceMode("PUBLIC_MATRIX")' in builder[0] for builder in builders)
        modes.extend(re.findall(r'\.approvalSourceMode\("(PUBLIC_MATRIX)"\)', ''.join(item[0] for item in builders)))
        checks = {
            'all-authorized-builders-explicit': actual == entry['authorized_calls'] == len(builders),
            'original-assertion-AST-identical': actual_ast.get(name) == before_ast[name],
            'remove-only-added-calls-restores-bytes': restored == original,
            'remove-only-added-calls-restores-LF': restored.replace(b'\r\n', b'\n') == original.replace(b'\r\n', b'\n'),
        }
        for label, ok in checks.items():
            results.append({'file': name, 'check': label, 'pass': ok})
            print(('PASS ' if ok else 'FAIL ') + Path(name).name + ': ' + label)
        entry['current_sha256'] = digest(data)
        entry['current_lf_sha256'] = digest(data.replace(b'\r\n', b'\n'))
        entry['assertion_count'] = len(before_ast[name])
        entry['assertion_before_sha256'] = digest(json.dumps(before_ast[name], ensure_ascii=False).encode('utf-8'))
        entry['assertion_current_sha256'] = digest(json.dumps(actual_ast[name], ensure_ascii=False).encode('utf-8'))
        minimal += difflib.unified_diff(original.decode('utf-8-sig').replace('\r\n', '\n').splitlines(keepends=True),
                                       source.replace('\r\n', '\n').splitlines(keepends=True),
                                       fromfile='v02-before/' + name, tofile='v02-current/' + name, n=3)
    summary = {'phase': args.phase, 'files': len(manifest), 'builders': sum(x['authorized_calls'] for x in manifest.values()),
               'assertions': sum(map(len, before_ast.values())), 'pass': sum(x['pass'] for x in results),
               'fail': sum(not x['pass'] for x in results), 'checks': results}
    output.write_text(json.dumps(summary, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    if summary['fail'] == 0:
        (TASK / 'v02-assertions-after.json').write_text(json.dumps(actual_ast, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
        (TASK / 'v02-file-manifest.json').write_text(json.dumps(manifest, indent=2) + '\n', encoding='utf-8')
        (TASK / 'v02-minimal.diff').write_text(''.join(minimal), encoding='utf-8')
    if args.h2:
        schema = (ROOT / 'IntRuoyiBackend/yudao-module-dcc/src/test/resources/sql/create_tables.sql').read_text(encoding='utf-8')
        ddl = re.search(r'CREATE TABLE IF NOT EXISTS `dcc_file_category`\s*\(.*?\n\);', schema, re.S)
        if not ddl:
            raise ValueError('Current category CREATE TABLE absent')
        ddlfile = TASK / 'v02-category-current.sql'
        ddlfile.write_text(ddl[0], encoding='utf-8')
        cp = (ROOT / 'IntRuoyiBackend/yudao-module-dcc/target/dcc-test.classpath').read_text(encoding='utf-8-sig').split(';')
        h2 = [value for value in cp if re.search(r'[\\/]com[\\/]h2database[\\/]h2[\\/]', value)]
        if len(h2) != 1 or len(modes) != summary['builders']:
            raise ValueError('Precise H2 dependency/explicit fixture literal inputs required')
        run_h2 = subprocess.run(['java', '-Dfile.encoding=UTF-8', '--source', '17', '-cp', h2[0],
                                 str(TASK / 'V02CategorySchema.java'), str(ddlfile), *modes], capture_output=True, cwd=ROOT)
        message = (run_h2.stdout + run_h2.stderr).decode('utf-8', errors='replace')
        print(message)
        summary['h2_exit'] = run_h2.returncode
        summary['h2_output'] = message
        summary['schema_sha256'] = digest((ROOT / 'IntRuoyiBackend/yudao-module-dcc/src/test/resources/sql/create_tables.sql').read_bytes())
        output.write_text(json.dumps(summary, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
        if run_h2.returncode:
            return run_h2.returncode
    print('V02 static/AST SUMMARY: ' + json.dumps({key: summary[key] for key in ['files', 'builders', 'assertions', 'pass', 'fail']}))
    return 1 if summary['fail'] else 0


if __name__ == '__main__':
    sys.exit(main())
