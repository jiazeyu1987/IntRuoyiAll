"""Verify the capacity migration in a task-owned disposable local MySQL schema.

Uses the local int-ruoyi-mysql container; never connects to a remote database.
This is a storage regression check, not frontend E2E.
"""
import json
from pathlib import Path
import subprocess
import unittest
import uuid


ROOT = Path(__file__).resolve().parents[2]
MIGRATION = ROOT / 'IntRuoyiBackend/sql/mysql/20261122_mes_route_version_snapshot_longtext.sql'


class RouteSnapshotCapacityCheck(unittest.TestCase):
    @classmethod
    def mysql(cls, sql, *, expect_failure=False):
        result = subprocess.run(
            ['docker', 'exec', '-i', 'int-ruoyi-mysql', 'sh', '-c',
             'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot --batch --raw --skip-column-names --default-character-set=utf8mb4'],
            input="SET SESSION sql_mode='STRICT_ALL_TABLES';\n" + sql,
            text=True, encoding='utf-8', capture_output=True, timeout=60)
        if expect_failure:
            if result.returncode == 0:
                raise AssertionError('Expected MySQL to reject the oversized snapshot')
            return result.stderr
        if result.returncode:
            raise AssertionError(result.stderr)
        return result.stdout.strip()

    @classmethod
    def setUpClass(cls):
        cls.schema = 'route_capacity_test_' + uuid.uuid4().hex
        cls.mysql(f'CREATE DATABASE `{cls.schema}` CHARACTER SET utf8mb4;')
        cls.addClassCleanup(lambda: cls.mysql(f'DROP DATABASE `{cls.schema}`;'))

    def execute(self, sql, **kwargs):
        return self.mysql(f'USE `{self.schema}`;\n' + sql, **kwargs)

    @staticmethod
    def literal(value):
        return "CONVERT(0x" + value.encode('utf-8').hex() + " USING utf8mb4)"

    def test_large_products_and_leaders_preserve_existing_snapshot(self):
        self.execute('CREATE TABLE mes_pro_route_version ('
                     'id BIGINT PRIMARY KEY, route_snapshot_json TEXT CHARACTER SET utf8mb4 '
                     "COLLATE utf8mb4_unicode_ci NULL COMMENT '路线版本快照', "
                     'route_snapshot_sha256 CHAR(64), lifecycle_status VARCHAR(32)) ENGINE=InnoDB;')
        products = {str(2123251871439744800 + n): {
            'routeId': 2123251871343222810, 'itemId': 2123251871439744800 + n,
            'quantity': None, 'productionTime': None, 'timeUnitType': None, 'remark': None
        } for n in range(211)}
        snapshot = {'routeName': '球囊扩张压力泵工艺流程',
                    'configSnapshots': {'products': products}, 'otherConfig': ''}
        encode = lambda: json.dumps(snapshot, ensure_ascii=False, separators=(',', ':'))
        # Match the observed production byte count, without copying production data.
        snapshot['otherConfig'] = 'x' * (65458 - len(encode().encode('utf-8')))
        original = encode()
        self.assertEqual(65458, len(original.encode('utf-8')))
        self.execute('INSERT INTO mes_pro_route_version VALUES '
                     f"(1,{self.literal(original)},SHA2({self.literal(original)},256),'DRAFT'),"
                     "(2,NULL,NULL,'LEGACY_INCOMPLETE');")
        fingerprint_sql = ('SELECT id,SHA2(route_snapshot_json,256),route_snapshot_sha256,'
                           'lifecycle_status FROM mes_pro_route_version ORDER BY id;')
        before = self.execute(fingerprint_sql)
        products['2123251871439745062'] = {**next(iter(products.values())),
                                         'itemId': 2123251871439745062}
        larger = encode()
        failure = self.execute('UPDATE mes_pro_route_version SET route_snapshot_json='
                               + self.literal(larger) + ' WHERE id=1;', expect_failure=True)
        self.assertIn("Data too long for column 'route_snapshot_json'", failure)
        del products['2123251871439745062']
        snapshot['configSnapshots']['routeStartProductionLeaders'] = [{
            'productionLineId': 2123251871343222810, 'candidateSourceType': 'USERS',
            'candidateSourceIds': [123], 'candidateSourceNames': ['测试生产组长']}]
        leaders = encode()
        failure = self.execute('UPDATE mes_pro_route_version SET route_snapshot_json='
                               + self.literal(leaders) + ' WHERE id=1;', expect_failure=True)
        self.assertIn("Data too long for column 'route_snapshot_json'", failure)
        for _ in range(2):
            self.execute(MIGRATION.read_text(encoding='utf-8'))
            self.assertEqual(before, self.execute(fingerprint_sql))
        self.assertEqual('longtext\tutf8mb4_unicode_ci\tYES', self.execute(
            'SELECT DATA_TYPE,COLLATION_NAME,IS_NULLABLE FROM information_schema.COLUMNS '
            "WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='mes_pro_route_version' "
            "AND COLUMN_NAME='route_snapshot_json';"))
        for updated, expected_count in [(larger, 212), (leaders, 211)]:
            self.assertGreater(len(updated.encode('utf-8')), 65535)
            self.execute('UPDATE mes_pro_route_version SET route_snapshot_json='
                         + self.literal(updated) + ',route_snapshot_sha256=SHA2('
                         + self.literal(updated) + ',256) WHERE id=1;')
            self.assertEqual(f'{expected_count}\t1\t1\t1', self.execute(
                "SELECT JSON_LENGTH(JSON_EXTRACT(route_snapshot_json,'$.configSnapshots.products')),"
                'JSON_VALID(route_snapshot_json),route_snapshot_sha256=SHA2(route_snapshot_json,256),'
                'BINARY route_snapshot_json=BINARY ' + self.literal(updated) + ' '
                'FROM mes_pro_route_version WHERE id=1;'))
        self.execute(MIGRATION.read_text(encoding='utf-8'))
        self.assertEqual('1', self.execute('SELECT BINARY route_snapshot_json=BINARY '
                                          + self.literal(leaders)
                                          + ' FROM mes_pro_route_version WHERE id=1;'))


if __name__ == '__main__':
    unittest.main()
