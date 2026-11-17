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


if __name__ == '__main__':
    unittest.main()
