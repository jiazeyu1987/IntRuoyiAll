import json
import unittest
from pathlib import Path


class ReleaseSourceRegistrationTest(unittest.TestCase):
    def test_every_forward_sql_has_one_explicit_disposition(self):
        root = Path(__file__).resolve().parents[2] / 'IntRuoyiBackend/sql/mysql'
        catalog = json.loads((root / 'release-sources.json').read_text(encoding='utf-8'))
        groups = [set(catalog['migrations'])]
        for key in ('sourceReferences', 'excludedSources', 'historicalMigrations'):
            groups.append({item['file'] for item in catalog[key]})
        self.assertEqual(sum(map(len, groups)), len(set.union(*groups)), 'SQL has conflicting release dispositions')
        actual = {p.name for p in root.glob('20*.sql')}
        self.assertEqual(actual, set.union(*groups), 'New SQL must be explicitly reviewed for release')

    def test_current_database_contracts_have_their_own_catalog_not_migration_ledger(self):
        root = Path(__file__).resolve().parents[2] / 'IntRuoyiBackend/sql/mysql'
        contract_root = root / 'database-contracts'
        catalog = json.loads((contract_root / 'catalog.json').read_text(encoding='utf-8'))
        self.assertEqual('ApplicationDatabaseContract/v1', catalog['schemaVersion'])
        entries = catalog['contracts']
        self.assertTrue(entries)
        self.assertEqual(len(entries), len({entry['id'] for entry in entries}))
        self.assertEqual(len(entries), len({entry['file'] for entry in entries}))
        registered = {entry['file'] for entry in entries}
        self.assertEqual({p.name for p in contract_root.glob('*.sql')}, registered,
                         'Every current contract SQL requires explicit catalog ownership')
        for name in registered:
            self.assertEqual(name, Path(name).name)
            self.assertFalse(name.startswith('20'), 'Current assertions must not enter forward migration scans')
            self.assertNotIn('release-migration:', (contract_root / name).read_text(encoding='utf-8'))


if __name__ == '__main__':
    unittest.main()
