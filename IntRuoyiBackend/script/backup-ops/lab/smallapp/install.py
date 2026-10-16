"""W05 installation only: empty task DB + actual login identity, zero business rows."""
from pathlib import Path
import json
from core import Store, TABLES, AppError, config, exact, load_fd, need, password_hash


def install(envelope):
    exact(envelope, ('config', 'secrets', 'bootstrap'))
    settings = config(envelope['config'])
    need(settings['runtimeRole'] == 'SOURCE', 'TARGET_BOOTSTRAP_REFUSED')
    bootstrap = exact(envelope['bootstrap'], ('username', 'tenantId', 'password'))
    need(bootstrap['username'] == 'aoteman' and bootstrap['tenantId'] == 122, 'BOOTSTRAP_SCOPE_MISMATCH')
    encoded = password_hash(bootstrap['password'])
    store = Store(settings, envelope['secrets'])
    store.environment(empty=True)
    # MySQL DDL commits independently. Partial schema is retained and rejected on
    # the next install; no IF NOT EXISTS, auto-drop, retry, or seed bypass.
    ddl = (Path(__file__).parent / 'schema.sql').read_text(encoding='utf-8')
    statements = [statement.strip() for statement in ddl.split(';') if statement.strip()]
    need(len(statements) == len(TABLES), 'INSTALL_SCHEMA_COUNT_MISMATCH')
    with store.transaction() as cur:
        for statement in statements:
            cur.execute(statement)
        for table in TABLES:
            cur.execute('DESCRIBE `' + table + '`')
            need(bool(cur.fetchall()), 'INSTALL_SCHEMA_MISSING')
        cur.execute('INSERT INTO smallapp_tenants(tenant_id,name) VALUES(%s,%s)', (122, '测试租户'))
        cur.execute('INSERT INTO smallapp_users(id,tenant_id,username,password_hash,active) VALUES(%s,%s,%s,%s,%s)',
                    (1, 122, 'aoteman', encoded, 1))
        for table in ('smallapp_records', 'smallapp_objects', 'smallapp_attachments', 'smallapp_sessions'):
            cur.execute('SELECT COUNT(*) AS n FROM `' + table + '`')
            need(cur.fetchone()['n'] == 0, 'BUSINESS_ROWS_MUST_START_EMPTY')
    store.environment()
    print(json.dumps({'schemaVersion': 'SmallAppInstallResult/v1', 'tenantId': 122,
                      'username': 'aoteman', 'businessRows': 0, 'tables': list(TABLES)}))


if __name__ == '__main__':
    try:
        install(load_fd())
    except Exception as exc:
        # Never echo MySQL/S3 errors, connection details, private file paths or passwords.
        print(json.dumps({'state': 'FAILED', 'errorCode': exc.code if isinstance(exc, AppError) else 'INSTALL_DEPENDENCY_FAILED',
                          'exceptionClass': type(exc).__name__}))
        raise SystemExit(1) from None
