from decimal import Decimal
import importlib.util
from pathlib import Path
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('repair', Path(__file__).parents[1] / 'repairs/repair-local-edhr-candidate-sop.py')
repair = importlib.util.module_from_spec(spec)
spec.loader.exec_module(repair)


class LocalSopCorrectionTest(unittest.TestCase):
    def fixture(self):
        snapshot = {'routeId': 10, 'configSnapshots': {'flowGraph': {
            'nodes': [{'processId': 20, 'routeProcessId': 30}]},
            'batchUseConfigs': [{'formBindings': [{'formTemplateId': 99}]}]}}
        text = repair.canonical(snapshot)
        row = dict(id=1, route_id=10, tenant_id=186, active=1, deleted=0,
                   lifecycle_status='ACTIVE', create_time='2026-11-20',
                   route_snapshot_json=text, route_snapshot_sha256=repair.sha(text),
                   route_snapshot_format_version='MES_ROUTE_SNAPSHOT_CANONICAL_V1')
        self.enterContext(patch.dict(repair.PINS, {1: repair.sha(text)}))
        processes = {20: dict(id=20, tenant_id=186, deleted=0, sop_file_url=None,
                             create_time='2026-11-12', update_time='2026-11-12')}
        return row, processes

    def test_preserves_entire_snapshot_except_proven_missing_sop(self):
        import json
        row, processes = self.fixture()
        before = json.loads(row['route_snapshot_json'], parse_float=Decimal)
        fixed, count = repair.correct(row, processes)
        after = json.loads(fixed, parse_float=Decimal)
        self.assertEqual(len(before['configSnapshots']['flowGraph']['nodes']), count)
        for node in after['configSnapshots']['flowGraph']['nodes']:
            self.assertIsNone(node.pop('sopFileUrl'))
        self.assertEqual(before, after)
        self.assertNotEqual(row['route_snapshot_sha256'], repair.sha(fixed))

    def test_refuses_changed_identity_or_unproven_historical_sop(self):
        for change in ['hash', 'tenant', 'nonempty', 'newer', 'missing_process']:
            with self.subTest(change=change):
                row, processes = self.fixture()
                pid = __import__('json').loads(row['route_snapshot_json'])['configSnapshots']['flowGraph']['nodes'][0]['processId']
                if change == 'hash': row['route_snapshot_sha256'] = '0' * 64
                if change == 'tenant': row['tenant_id'] = 163
                if change == 'nonempty': processes[pid]['sop_file_url'] = '/real/sop.pdf'
                if change == 'newer': processes[pid]['update_time'] = '2099-01-01'
                if change == 'missing_process': del processes[pid]
                with self.assertRaises((ValueError, KeyError)):
                    repair.correct(row, processes)

    def test_decimal_canonicalization_does_not_lose_precision(self):
        self.assertEqual('{"a":2121453182031057241,"b":0,"c":1.23}', repair.canonical(
            {'c': Decimal('1.2300'), 'a': 2121453182031057241, 'b': Decimal('-0.00')}))


if __name__ == '__main__':
    unittest.main()
