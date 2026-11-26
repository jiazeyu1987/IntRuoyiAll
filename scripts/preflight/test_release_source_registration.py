import json
import hashlib
import unittest
from pathlib import Path


class ReleaseSourceRegistrationTest(unittest.TestCase):
    def test_product_binding_migration_requires_formal_admission_without_rewriting_history(self):
        root = Path(__file__).resolve().parents[2] / 'IntRuoyiBackend/sql/mysql'
        catalog = json.loads((root / 'release-sources.json').read_text(encoding='utf-8'))
        original = '20261125_dcc_registration_certificate_products.sql'
        admission = '20261127_dcc_registration_certificate_products_admission'
        edges = [item for item in catalog['migrationDependencies'] if item['file'] == original]
        self.assertEqual(1, len(edges))
        self.assertEqual('053edc551e2a9ebc87c47fda9e1c0e095245b6c4b09b754d90cc066be13b0123', edges[0]['sha256'])
        # The release checkout explicitly pins SQL to CRLF on every host.
        packaged_original = (root / original).read_bytes().replace(b'\r\n', b'\n').replace(b'\n', b'\r\n')
        self.assertEqual(edges[0]['sha256'], hashlib.sha256(packaged_original).hexdigest())
        self.assertIn(admission, edges[0]['dependsOn'])
        self.assertIn(admission + '.sql', catalog['migrations'])
        header = (root / (admission + '.sql')).read_text('utf-8').splitlines()[0]
        self.assertIn('type=schema', header)
        self.assertIn('requiresTargetPreflight=true', header)
        self.assertIn('applyOrder=1', header)
        self.assertNotIn('20261125_dcc_registration_certificate_products', header)
        check = (root / 'target-preflight' / (admission + '.preflight.sql')).read_text('utf-8')
        self.assertIn('migrationId=' + admission + ';', check)
        self.assertEqual(1, check.count('TARGET_PREFLIGHT_PASS:' + admission))

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
