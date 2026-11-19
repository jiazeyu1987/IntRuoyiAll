"""Staged ownership migration preserves unresolved records and refuses conflicting evidence."""
import unittest
import test_tenant_file_ownership_mysql as harness
import subprocess
import time
import uuid
ROOT = harness.ROOT


class TenantFileOwnershipPrepareMySqlTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.container = 'file-prepare-test-' + uuid.uuid4().hex[:12]
        subprocess.run(['docker', 'run', '--pull=never', '--rm', '-d', '--network=none', '--name', cls.container,
                        '-e', 'MYSQL_ALLOW_EMPTY_PASSWORD=yes', 'mysql:8.0.40'], check=True, capture_output=True)
        cls.addClassCleanup(lambda: subprocess.run(['docker', 'rm', '-f', cls.container], check=True, capture_output=True))
        deadline = time.monotonic() + 180
        while cls.sql('SELECT 1', check=False).returncode:
            if time.monotonic() > deadline:
                raise RuntimeError('Task-owned MySQL did not become ready')
            time.sleep(1)
    sql = classmethod(harness.TenantFileOwnershipMySqlTest.sql.__func__)
    query = harness.TenantFileOwnershipMySqlTest.query
    setUp = harness.TenantFileOwnershipMySqlTest.setUp

    def migrate(self, check=True):
        return self.query((ROOT/'sql/mysql/20261117_tenant_file_ownership_prepare.sql').read_text(encoding='utf-8'), check)

    def test_only_verified_files_become_visible_and_bytes_stay_unchanged(self):
        self.migrate()
        self.query("INSERT INTO infra_file_tenant_owner VALUES(1,11,'business record 1');")
        self.migrate()
        self.migrate()
        self.assertEqual('1\t11\n2\tNULL\n', self.query('SELECT id,tenant_id FROM infra_file ORDER BY id').stdout)
        self.assertEqual('11\t41\nNULL\t42\n', self.query('SELECT tenant_id,HEX(content) FROM infra_file_content ORDER BY id').stdout)
        self.assertEqual('0\n', self.query('SELECT COUNT(*) FROM infra_file WHERE tenant_id=22').stdout)
        self.assertEqual('0\n', self.query("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='infra_file_tenant_migration'").stdout)

    def test_shared_key_requires_every_row_to_have_same_verified_owner(self):
        self.migrate()
        self.query("INSERT INTO infra_file_tenant_owner VALUES(1,11,'business 1'); UPDATE infra_file SET path='a.txt' WHERE id=2;")
        self.assertIn('TENANT_FILE_SHARED_OBJECT_REQUIRES_VERIFICATION', self.migrate(False).stderr)
        self.assertEqual('2\n', self.query('SELECT COUNT(*) FROM infra_file WHERE tenant_id IS NULL').stdout)

    def test_existing_owner_and_invalid_tenant_are_not_overwritten(self):
        self.migrate()
        self.query("INSERT INTO infra_file_tenant_owner VALUES(1,11,'business 1'); UPDATE infra_file SET tenant_id=22 WHERE id=1;")
        self.assertIn('TENANT_FILE_EXISTING_OWNER_CONFLICT', self.migrate(False).stderr)
        self.query('UPDATE infra_file_tenant_owner SET tenant_id=999 WHERE file_id=1')
        self.assertIn('TENANT_FILE_VERIFIED_OWNER_REQUIRED', self.migrate(False).stderr)


if __name__ == '__main__':
    unittest.main()
