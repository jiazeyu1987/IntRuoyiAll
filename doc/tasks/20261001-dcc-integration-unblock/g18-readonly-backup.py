"""Read-only task schema/data dump. Credentials stay inside the existing container."""
import gzip
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import subprocess

task_dir = Path(__file__).resolve().parent
scope = json.loads((task_dir / 'g18-backup-scope.json').read_text(encoding='utf-8-sig'))
parser = argparse.ArgumentParser()
parser.add_argument('--affected-only', action='store_true')
args = parser.parse_args()
docker_cli = shutil.which('docker.exe')
if not docker_cli:
    raise RuntimeError('Existing Docker CLI is required')
backup_dir = Path(scope['backupDirectory'])
backup_dir.mkdir(parents=True, exist_ok=True)
receipt = []
plans = [('schema', ['--no-data']), ('data', ['--no-create-info', '--skip-triggers', *scope['dataBackupTables']])]
if args.affected_only:
    approved_scope = json.loads((task_dir / 'g18-approved-scope-candidate.json').read_text(encoding='utf-8-sig'))
    affected_tables = approved_scope['existingTablesAffected']
    if not affected_tables or not set(affected_tables).issubset(scope['dataBackupTables']):
        raise RuntimeError('Affected table backup scope is not covered by the frozen source scope')
    prior = json.loads((task_dir / 'g18-backup-receipt.json').read_text(encoding='utf-8-sig'))
    for item in prior['artifacts']:
        with Path(item['path']).open('rb') as payload:
            if hashlib.file_digest(payload, 'sha256').hexdigest() != item['sha256']:
                raise RuntimeError('Existing backup checksum changed')
    receipt = prior['artifacts']
    plans = [('affected', affected_tables)]
for kind, options in plans:
    target = backup_dir / ('dcc-' + kind + '.sql.gz')
    if target.exists():
        raise RuntimeError('Refusing to replace an existing backup: ' + str(target))
    shell = 'test -n "$MYSQL_ROOT_PASSWORD" || exit 3; export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"; exec mysqldump --user=root --single-transaction --skip-lock-tables --skip-add-locks --no-tablespaces --set-gtid-purged=OFF --hex-blob "$@"'
    arguments = [docker_cli, 'exec', scope['sourceContainer'], 'sh', '-c', shell, 'dcc-readonly-dump', scope['sourceDatabase'], *options]
    stderr_path = backup_dir / ('dcc-' + kind + '.stderr.txt')
    with stderr_path.open('wb') as errors:
        process = subprocess.Popen(arguments, stdout=subprocess.PIPE, stderr=errors)
        with gzip.open(target, 'xb', compresslevel=6) as output:
            shutil.copyfileobj(process.stdout, output)
        process.stdout.close()
        if process.wait() != 0:
            raise RuntimeError('Dump failed; inspect protected backup stderr file')
    uncompressed_bytes = 0
    with gzip.open(target, 'rb') as checked:
        while chunk := checked.read(1024 * 1024):
            uncompressed_bytes += len(chunk)
    if uncompressed_bytes == 0:
        raise RuntimeError('Empty dump')
    digest = hashlib.sha256()
    with target.open('rb') as payload:
        while chunk := payload.read(1024 * 1024):
            digest.update(chunk)
    receipt.append({'kind': kind, 'path': str(target), 'bytes': target.stat().st_size, 'uncompressedBytes': uncompressed_bytes, 'sha256': digest.hexdigest(), 'gzipIntegrity': 'PASS', 'dumpExitCode': 0})
result = {'status': 'read_only_backup_verified_not_restored', 'database': scope['sourceDatabase'], 'tableCount': scope['tableCount'], 'artifacts': receipt, 'credentialsExported': False, 'databaseWrites': False}
(task_dir / 'g18-backup-receipt.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
print(json.dumps(result, indent=2))
