"""Isolated MySQL regression; source business schema is read-only."""
from pathlib import Path
from urllib.parse import urlparse
import uuid
import pymysql
import yaml

ROOT = Path(__file__).resolve().parents[2]


def main():
    docs = yaml.safe_load_all((ROOT / 'IntRuoyiBackend/yudao-server/src/main/resources/application-local.yaml').read_text(encoding='utf-8'))
    master = next(d['spring']['datasource']['dynamic']['datasource']['master'] for d in docs if d and 'datasource' in d.get('spring', {}))
    jdbc = urlparse(master['url'].removeprefix('jdbc:'))
    if jdbc.scheme != 'mysql' or jdbc.hostname not in ('127.0.0.1', 'localhost', '::1'):
        raise ValueError('Regression requires a loopback MySQL JDBC URL')
    connection = pymysql.connect(host=jdbc.hostname, port=jdbc.port or 3306, user=master['username'], password=str(master['password']), database=jdbc.path.lstrip('/'), autocommit=True)
    schema = 'task_access_reason_' + uuid.uuid4().hex
    sql = (ROOT / 'IntRuoyiBackend/sql/mysql/20261126_dcc_project_access_reason_check_fix.sql').read_text(encoding='utf-8-sig')
    with connection:
        with connection.cursor() as cursor:
            cursor.execute('SHOW CREATE TABLE dcc_project_access_rule')
            ddl = cursor.fetchone()[1]
            cursor.execute(f'CREATE DATABASE `{schema}`')
            try:
                cursor.execute(f'USE `{schema}`')
                def migrate():
                    delimiter, pending = ';', ''
                    for line in sql.splitlines():
                        if line.startswith('DELIMITER '):
                            delimiter = line.split()[1]
                            continue
                        pending += line + '\n'
                        if pending.rstrip().endswith(delimiter):
                            cursor.execute(pending.rstrip()[:-len(delimiter)])
                            pending = ''
                def rejected(action, code):
                    try:
                        action()
                    except pymysql.MySQLError as error:
                        assert error.args[0] == code, error
                    else:
                        raise AssertionError('Expected database rejection')
                rejected(migrate, 1644)
                cursor.execute(ddl)
                def checks():
                    cursor.execute('SELECT tc.CONSTRAINT_NAME,cc.CHECK_CLAUSE FROM information_schema.TABLE_CONSTRAINTS tc JOIN information_schema.CHECK_CONSTRAINTS cc USING (CONSTRAINT_SCHEMA,CONSTRAINT_NAME) WHERE tc.CONSTRAINT_SCHEMA=%s AND tc.TABLE_NAME=%s', (schema, 'dcc_project_access_rule'))
                    return dict(cursor.fetchall())
                if 'ck_dcc_project_access_reason' in checks():
                    cursor.execute('ALTER TABLE dcc_project_access_rule DROP CHECK ck_dcc_project_access_reason')
                cursor.execute('ALTER TABLE dcc_project_access_rule ADD CONSTRAINT ck_dcc_project_access_reason CHECK (char_length(trim(change_reason)) > 0)')
                before = checks()
                before.pop('ck_dcc_project_access_reason')
                insert = 'INSERT INTO dcc_project_access_rule (dcc_project_code_id,subject_type,subject_id,access_level,change_reason) VALUES (1,%s,%s,%s,%s)'
                rejected(lambda: cursor.execute(insert, ('USER', 1, 'VIEW', '')), 3819)
                migrate()
                migrate()
                assert checks() == before, 'Other check expressions changed'
                for i, reason in enumerate((None, '', '   ', 'authorized'), 1):
                    cursor.execute(insert, ('USER', i, 'VIEW', reason))
                rejected(lambda: cursor.execute(insert, ('INVALID', 10, 'VIEW', '')), 3819)
                rejected(lambda: cursor.execute(insert, ('USER', 10, 'INVALID', '')), 3819)
                rejected(lambda: cursor.execute(insert, ('USER', 1, 'VIEW', '')), 1062)
                cursor.execute('ALTER TABLE dcc_project_access_rule ADD CONSTRAINT ck_dcc_project_access_reason CHECK (char_length(change_reason) < 513)')
                different = checks()
                rejected(migrate, 1644)
                assert checks() == different, 'Unexpected expression was removed or changed'
                print('PASS: missing table fails; blank rejection reproduced; optional reasons accepted; other checks unchanged; uniqueness preserved; rerun passed')
                print('PASS: same-name different-expression check rejected and preserved unchanged')
            finally:
                cursor.execute(f'DROP DATABASE `{schema}`')


if __name__ == '__main__':
    main()
