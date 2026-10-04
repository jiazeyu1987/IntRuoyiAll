"""Local task transport and immutable-row proofs; caller owns explicit write authorization."""
from __future__ import annotations

import gzip
import hashlib
import json
from pathlib import Path
import re
import shutil
import subprocess

DATABASES = {'ruoyi-vue-pro', 'dcc_intqms_g18_rehearsal'}


def identifier(value: str) -> str:
    if not isinstance(value, str) or not re.fullmatch(r'[A-Za-z0-9_]+', value):
        raise ValueError('Unsafe table/column identifier')
    return '`' + value + '`'


class LocalMysql:
    def __init__(self, database: str):
        if database not in DATABASES:
            raise ValueError('Database is outside the approved local task scope')
        self.database = database
        self.docker = shutil.which('docker.exe')
        if not self.docker:
            raise RuntimeError('The existing Docker CLI is required')

    def command(self) -> list[str]:
        shell = 'test -n "$MYSQL_ROOT_PASSWORD" || exit 3; export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"; exec mysql --user=root --default-character-set=utf8mb4 --batch --raw --skip-column-names "$@"'
        return [self.docker, 'exec', '-i', 'int-ruoyi-mysql', 'sh', '-c', shell, 'dcc-local-task-mysql', self.database]

    def read(self, sql: str) -> str:
        statements = [part.strip() for part in re.sub(r'^\s*--.*$', '', sql, flags=re.MULTILINE).split(';') if part.strip()]
        if not statements or any(not re.match(r'^(SELECT|SHOW)\b', part, re.IGNORECASE) for part in statements):
            raise ValueError('Read transport only accepts SELECT/SHOW')
        result = subprocess.run(self.command(), input=sql.encode('utf-8'), capture_output=True)
        if result.returncode:
            # MySQL messages may contain data values. Preserve privately at the caller, never print payloads.
            raise MysqlFailure(result.returncode, result.stderr)
        return result.stdout.decode('utf-8')

    def run_authorized_sql(self, sql: bytes, *, private_directory: Path, stem: str) -> dict:
        if not stem or re.fullmatch(r'[A-Za-z0-9_-]+', stem) is None:
            raise ValueError('Invalid task receipt filename')
        private_directory.mkdir(parents=True, exist_ok=True)
        stdout = private_directory / (stem + '.stdout.txt')
        stderr = private_directory / (stem + '.stderr.txt')
        if stdout.exists() or stderr.exists():
            raise RuntimeError('Execution receipt exists; refusing an ambiguous retry')
        with stdout.open('xb') as out, stderr.open('xb') as error:
            result = subprocess.run(self.command(), input=sql, stdout=out, stderr=error)
        receipt = {'database': self.database, 'sqlSha256': hashlib.sha256(sql).hexdigest(),
                   'exitCode': result.returncode, 'stdout': str(stdout), 'stderr': str(stderr)}
        if result.returncode:
            raise RuntimeError('MySQL stopped at an error; inspect protected step receipt: ' + str(stderr))
        return receipt

    def restore_gzip(self, path: Path, *, private_directory: Path, stem: str) -> dict:
        # Validate database routing first without replacing/rewriting dump bytes or suppressing errors.
        with gzip.open(path, 'rt', encoding='utf-8') as check:
            for line in check:
                if re.match(r'\s*(USE\s|(?:CREATE|DROP)\s+(?:DATABASE|SCHEMA)\b|SOURCE\s|\\!)', line, re.IGNORECASE):
                    raise ValueError('Restore dump contains forbidden database routing or shell directives')
        private_directory.mkdir(parents=True, exist_ok=True)
        stdout = private_directory / (stem + '.stdout.txt')
        stderr = private_directory / (stem + '.stderr.txt')
        if stdout.exists() or stderr.exists():
            raise RuntimeError('Restore receipt exists; refusing duplicate restore')
        with stdout.open('xb') as out, stderr.open('xb') as error, gzip.open(path, 'rb') as source:
            process = subprocess.Popen(self.command(), stdin=subprocess.PIPE, stdout=out, stderr=error)
            try:
                shutil.copyfileobj(source, process.stdin)
                process.stdin.close()
            except BaseException:
                if process.poll() is None:
                    process.terminate()
                process.wait()
                raise
            code = process.wait()
        if code:
            raise RuntimeError('MySQL restore failed; inspect protected receipt: ' + str(stderr))
        return {'database': self.database, 'dumpSha256': sha256_file(path), 'exitCode': code,
                'stdout': str(stdout), 'stderr': str(stderr)}


class MysqlFailure(RuntimeError):
    def __init__(self, code: int, private_error: bytes):
        self.exit_code = code
        self.private_error = private_error
        super().__init__('Read-only MySQL command failed; exact error retained privately')


def sha256_file(path: Path) -> str:
    with path.open('rb') as source:
        return hashlib.file_digest(source, 'sha256').hexdigest()


def snapshot_original_rows(mysql: LocalMysql, tables: list[str], existing_columns: dict[str, list[str]] | None = None) -> dict:
    snapshot = {}
    for table in tables:
        quoted = identifier(table)
        if existing_columns is None:
            columns_sql = "SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='" + table + "' ORDER BY ORDINAL_POSITION;"
            columns = mysql.read(columns_sql).splitlines()
        else:
            columns = existing_columns[table]
        if not columns or 'id' not in columns and 'ID_' not in columns:
            raise ValueError('Original table needs explicit primary identity and columns: ' + table)
        primary = 'id' if 'id' in columns else 'ID_'
        # Encode NULL and every exact original column value separately. Generated new columns are excluded.
        terms = ["CASE WHEN " + identifier(column) + " IS NULL THEN 'N' ELSE CONCAT('V',HEX(CAST(" + identifier(column) + " AS BINARY))) END" for column in columns]
        statement = "SELECT HEX(CAST(" + identifier(primary) + " AS BINARY)),SHA2(CONCAT_WS('|'," + ','.join(terms) + "),256) FROM " + quoted + ';'
        rows = {}
        for line in mysql.read(statement).splitlines():
            identity, digest = line.split('\t')
            if identity in rows or not re.fullmatch(r'[0-9a-f]{64}', digest):
                raise ValueError('Duplicate/invalid original row identity digest: ' + table)
            rows[identity] = digest
        aggregate = hashlib.sha256(json.dumps(rows, sort_keys=True, separators=(',', ':')).encode()).hexdigest()
        snapshot[table] = {'columns': columns, 'rows': rows, 'count': len(rows), 'aggregateSha256': aggregate}
    return snapshot


def compare_original_rows(before: dict, after: dict, permitted_additions: dict[str, int]) -> dict:
    outcomes = {}
    if set(before) != set(after):
        raise ValueError('Original-table snapshot scope changed')
    for table, original in before.items():
        current = after[table]
        if original['columns'] != current['columns']:
            raise ValueError('Original protected columns differ: ' + table)
        for key, digest in original['rows'].items():
            if current['rows'].get(key) != digest:
                raise ValueError('Original row was deleted or changed: ' + table)
        added = set(current['rows']) - set(original['rows'])
        if len(added) != permitted_additions.get(table, 0):
            raise ValueError('Unexpected new row delta: ' + table)
        outcomes[table] = {'originalCount': original['count'], 'originalPayloadSha256': original['aggregateSha256'],
                           'oldRowsUnchanged': True, 'newRowCount': len(added)}
    return outcomes
