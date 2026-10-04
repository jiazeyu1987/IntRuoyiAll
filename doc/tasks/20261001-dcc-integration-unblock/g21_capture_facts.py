"""Capture reviewed SELECT-only task prerequisites from the existing local container."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
import subprocess
from datetime import datetime, timezone


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--sql', required=True, type=Path)
    parser.add_argument('--output', required=True, type=Path)
    args = parser.parse_args()
    raw = args.sql.read_bytes()
    sql = raw.decode('utf-8-sig')
    reviewed = re.sub(r'^\s*--[^\n]*(?:\n|$)', '', sql, flags=re.MULTILINE)
    statements = [item.strip() for item in reviewed.split(';') if item.strip()]
    if not statements or any(not re.match(r'^SELECT\b', item, re.IGNORECASE) for item in statements):
        raise ValueError('Only reviewed SELECT statements may be collected')
    if re.search(r'\bINTO\s+(?:OUTFILE|DUMPFILE)|\b(?:SLEEP|GET_LOCK|RELEASE_LOCK|LOAD_FILE)\s*\(', reviewed, re.IGNORECASE):
        raise ValueError('Side-effect/file-access SELECT is outside prerequisite collection')
    docker = shutil.which('docker.exe')
    if not docker:
        raise RuntimeError('The existing Docker CLI is required')
    if args.output.exists():
        raise RuntimeError('Capture cannot replace existing evidence; use a new exact output path')
    shell = 'test -n "$MYSQL_ROOT_PASSWORD" || exit 3; export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"; exec mysql --user=root --database=ruoyi-vue-pro --default-character-set=utf8mb4 --batch --raw --skip-column-names'
    identity = "SELECT JSON_OBJECT('kind','capture','database',DATABASE(),'serverUuid',@@server_uuid,'version',@@version,'collectedAtUtc',UTC_TIMESTAMP(6));\n"
    started = datetime.now(timezone.utc).isoformat()
    result = subprocess.run([docker, 'exec', '-i', 'int-ruoyi-mysql', 'sh', '-c', shell],
                            input=(identity + sql).encode('utf-8'), capture_output=True)
    if result.returncode:
        error = args.output.with_suffix(args.output.suffix + '.error.txt')
        error.write_bytes(result.stderr)
        raise RuntimeError('Read-only query failed; protected error file: ' + str(error))
    result.stdout.decode('utf-8')
    rows = [json.loads(line) for line in result.stdout.decode('utf-8').splitlines() if line]
    if not rows or rows[0].get('database') != 'ruoyi-vue-pro' or rows[0].get('kind') != 'capture':
        raise RuntimeError('Actual source database identity is not verified')
    # The collector's own identity envelope belongs in its receipt, not in a domain validator's facts.
    # Remove exactly that verified first line; all query-result rows remain intact and unfiltered.
    identity_line, separator, facts_payload = result.stdout.partition(b'\n')
    if not separator or not facts_payload:
        raise RuntimeError('The verified identity envelope has no query result payload')
    args.output.write_bytes(facts_payload)
    receipt = {'status': 'read_only_facts_collected_not_execution', 'databaseWrites': False,
               'sourceContainer': 'int-ruoyi-mysql', 'capture': rows[0], 'startedAtUtc': started,
               'sqlPath': str(args.sql.resolve()), 'sqlSha256': hashlib.sha256(raw).hexdigest(),
               'factsPath': str(args.output.resolve()), 'factsSha256': hashlib.sha256(facts_payload).hexdigest(),
               'rawCaptureSha256': hashlib.sha256(result.stdout).hexdigest(),
               'rowCount': len(rows) - 1, 'selectCount': len(statements)}
    args.output.with_suffix(args.output.suffix + '.receipt.json').write_text(json.dumps(receipt, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'status': receipt['status'], 'rowCount': len(rows) - 1, 'selectCount': len(statements)}))


if __name__ == '__main__':
    main()
