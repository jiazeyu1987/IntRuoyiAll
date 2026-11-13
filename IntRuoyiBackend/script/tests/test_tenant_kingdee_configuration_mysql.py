"""Migration verification against a task-owned, network-isolated MySQL container."""
from pathlib import Path
import subprocess
import time
import unittest
import uuid

MIGRATION = Path(__file__).resolve().parents[2] / 'sql/mysql/20261112_tenant_kingdee_configuration.sql'


class TenantKingdeeConfigurationMySqlTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.container = 'tenant-kingdee-config-test-' + uuid.uuid4().hex[:12]
        subprocess.run(['docker', 'run', '--pull=never', '--rm', '-d', '--network=none',
                        '--name', cls.container, '-e', 'MYSQL_ALLOW_EMPTY_PASSWORD=yes',
                        'mysql:8.0.39'], check=True, capture_output=True)
        cls.addClassCleanup(lambda: subprocess.run(['docker', 'rm', '-f', cls.container],
                                                   check=True, capture_output=True))
        deadline = time.monotonic() + 60
        while cls.sql('SELECT 1', check=False).returncode:
            if time.monotonic() > deadline:
                raise RuntimeError('Task-owned MySQL did not become ready')
            time.sleep(1)

    @classmethod
    def sql(cls, query, check=True):
        result = subprocess.run(['docker', 'exec', '-i', cls.container, 'mysql', '-uroot',
                                 '--protocol=TCP', '-h127.0.0.1', '-BN', '--default-character-set=utf8mb4'],
                                input=query, capture_output=True, encoding='utf-8')
        if check and result.returncode:
            raise AssertionError(result.stderr)
        return result

    def setUp(self):
        self.database = 'test_' + uuid.uuid4().hex
        self.sql(f'CREATE DATABASE {self.database};')
        self.addCleanup(lambda: self.sql(f'DROP DATABASE {self.database};'))
        self.query('''CREATE TABLE system_tenant (id bigint PRIMARY KEY, deleted bit DEFAULT 0);
          INSERT INTO system_tenant (id) VALUES (11),(22);
          CREATE TABLE infra_config (id bigint AUTO_INCREMENT PRIMARY KEY, category varchar(50),
            type tinyint, name varchar(100), config_key varchar(100), value longtext, visible bit,
            remark varchar(500), creator varchar(64), updater varchar(64), deleted bit DEFAULT 0);
        ''')
        for key, value in [('config', '{}'), ('connection.production', '{}'),
                           ('connection.active', 'PRODUCTION'), ('external-write-enabled', 'false')]:
            self.query(f"INSERT INTO infra_config (category,type,name,config_key,value,visible) "
                       f"VALUES ('erp',2,'fixture','yudao.erp.kingdee.{key}','{value}',0);")

    def query(self, query, check=True):
        return self.sql(f'USE {self.database};\n' + query, check)

    def migrate(self, check=True):
        return self.query(MIGRATION.read_text(encoding='utf-8'), check)

    def test_snapshots_are_independent_and_rerun_preserves_tenant_edits(self):
        before = self.query('SELECT id,config_key,value FROM infra_config ORDER BY id;').stdout
        self.migrate()
        self.assertEqual('8\n', self.query('SELECT COUNT(*) FROM infra_tenant_config;').stdout)
        self.query("UPDATE infra_tenant_config SET value='TEST' WHERE tenant_id=11 "
                   "AND config_key='yudao.erp.kingdee.connection.active';")
        self.migrate()
        self.assertEqual('11\tTEST\n22\tPRODUCTION\n', self.query(
            "SELECT tenant_id,value FROM infra_tenant_config "
            "WHERE config_key='yudao.erp.kingdee.connection.active' ORDER BY tenant_id;").stdout)
        self.assertEqual(before, self.query('SELECT id,config_key,value FROM infra_config ORDER BY id;').stdout)

    def test_business_configuration_snapshot_excludes_platform_and_preserves_tenant_changes(self):
        self.migrate()
        self.query("INSERT INTO infra_config (category,type,name,config_key,value,visible) VALUES "
                   "('system',2,'idle','system.login.idle-timeout-minutes','15',1),"
                   "('system',1,'audit','system.gxp.audit-mode','FORMAL',1),"
                   "('runtime',1,'runtime','runtime-control.release-package.backend-runtime-base-mode','offline-tar',0);")
        business = MIGRATION.with_name('20261112_tenant_scoped_business_configuration.sql').read_text(encoding='utf-8')
        self.query(business)
        self.assertEqual('10\n', self.query('SELECT COUNT(*) FROM infra_tenant_config;').stdout)
        self.query("UPDATE infra_tenant_config SET value='30' WHERE tenant_id=11 "
                   "AND config_key='system.login.idle-timeout-minutes';")
        self.query(business)
        self.assertEqual('11\t30\n22\t15\n', self.query(
            "SELECT tenant_id,value FROM infra_tenant_config WHERE config_key='system.login.idle-timeout-minutes' ORDER BY tenant_id;").stdout)
        self.assertEqual('0\n', self.query("SELECT COUNT(*) FROM infra_tenant_config WHERE config_key='system.gxp.audit-mode' "
                                          "OR config_key LIKE 'runtime-control.%';").stdout)

    def test_rerun_does_not_restore_a_deleted_tenant_setting(self):
        self.migrate()
        self.query("UPDATE infra_tenant_config SET deleted=1 WHERE tenant_id=11 "
                   "AND config_key='yudao.erp.kingdee.connection.active';")
        self.migrate()
        self.assertEqual('0\n', self.query("SELECT COUNT(*) FROM infra_tenant_config WHERE tenant_id=11 "
                                          "AND config_key='yudao.erp.kingdee.connection.active' AND deleted=0;").stdout)

    def test_missing_source_config_fails_without_partial_tenant_configuration(self):
        self.query("DELETE FROM infra_config WHERE config_key='yudao.erp.kingdee.connection.active';")
        result = self.migrate(check=False)
        self.assertNotEqual(0, result.returncode)
        self.assertIn('TENANT_KINGDEE_EXPLICIT_SOURCE_CONFIGURATION_REQUIRED', result.stderr)
        self.assertEqual('0\n', self.query('SELECT COUNT(*) FROM infra_tenant_config;').stdout)


if __name__ == '__main__':
    unittest.main()
