import importlib.util
import json
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location(
    'common_repair', Path(__file__).parents[1] / 'repair' / 'repair-mixed-common-regulations.py')
repair = importlib.util.module_from_spec(spec)
spec.loader.exec_module(repair)


class CommonIdentityRepairTest(unittest.TestCase):
    def test_split_remaps_snapshot_parent_without_changing_published_content(self):
        original = dict(regulationId=78, regulationCode='INITIAL', versionNo='A/8',
                        processes=[dict(processCode='PACK', items=[dict(itemCode='APPEARANCE')])])
        raw = json.dumps(original)
        actual = json.loads(repair.remap_snapshot_identity(raw, 78, 82))
        self.assertEqual(original | dict(regulationId=82), actual)
        self.assertEqual(78, json.loads(raw)['regulationId'])
        self.assertEqual(raw, repair.remap_snapshot_identity(raw, 78, 78))
        self.assertEqual('{"regulationCode":"INITIAL"}',
                         repair.remap_snapshot_identity('{"regulationCode":"INITIAL"}', 78, 82))
        with self.assertRaises(ValueError):
            repair.remap_snapshot_identity(raw, 79, 82)

    def parent(self):
        return dict(id=78, tenant_id=170, owner_module='MES_QA_COMMON', regulation_code='PACK')

    def version(self, id, code, date, status='RETIRED', version_no='A/1'):
        return dict(id=id, tenant_id=170, regulation_id=78, lifecycle_status=status,
                    version_no=version_no, published_at=date,
                    snapshot_json=json.dumps(dict(regulationCode=code, regulationName=code + ' name')))

    def test_split_by_snapshot_code_preserves_every_version_and_parent_of_current_document(self):
        versions = [self.version(99, 'INITIAL', '2026-11-01'),
                    self.version(100, 'DEVICE', '2026-11-02'),
                    self.version(103, 'MEILIAN', '2026-11-03'),
                    self.version(104, 'INITIAL', '2026-11-03', version_no='A/2'),
                    self.version(105, 'PACK', '2026-11-03', status='PUBLISHED')]
        result = repair.plan_repair(self.parent(), versions)
        self.assertEqual([104, 100, 103, 105], [g['latest_version_id'] for g in result])
        self.assertEqual([99, 104], result[0]['version_ids'])
        self.assertEqual([False, False, False, True], [g['retain_parent'] for g in result])
        self.assertEqual(5, sum(len(g['version_ids']) for g in result))
        self.assertEqual('RETIRED', versions[0]['lifecycle_status'])

    def test_rejects_drafts_and_cross_tenant_records(self):
        for changes in (dict(lifecycle_status='DRAFT'), dict(tenant_id=171), dict(regulation_id=79)):
            version = self.version(99, 'INITIAL', '2026-11-01') | changes
            with self.assertRaises(ValueError):
                repair.plan_repair(self.parent(), [version])

    def test_rejects_missing_identity_duplicate_versions_and_nonmixed_parent(self):
        with self.assertRaises(ValueError):
            repair.plan_repair(self.parent(), [self.version(99, 'PACK', '2026-11-01')])
        with self.assertRaises(ValueError):
            repair.plan_repair(self.parent(), [self.version(99, '', '2026-11-01')])
        with self.assertRaises(ValueError):
            repair.plan_repair(self.parent(), [self.version(99, 'INITIAL', '2026-11-01'),
                                              self.version(100, 'INITIAL', '2026-11-02'),
                                              self.version(105, 'PACK', '2026-11-03')])


if __name__ == '__main__':
    unittest.main()
