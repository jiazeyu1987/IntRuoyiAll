import copy
import hashlib
import importlib.util
import json
from pathlib import Path
import unittest

HERE = Path(__file__).resolve().parent


class LegacyMappingTests(unittest.TestCase):
    def setUp(self):
        path = HERE / 'g23-legacy-name-mapping.py'
        self.assertTrue(path.exists(), 'Exact legacy source metadata mapping validator is missing')
        spec = importlib.util.spec_from_file_location('legacy_name_mapping', path)
        self.module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(self.module)
        self.contract = json.loads((HERE / 'g23-legacy-name-mapping-contract.json').read_text(encoding='utf-8'))
        self.rows = self.module.offline_fixture(self.contract)

    def run_mapping(self, rows=None):
        return self.module.validate(self.contract, rows or self.rows)

    def row(self, kind):
        return next(x for x in self.rows if x['kind'] == kind)

    def test_complete_exact_metadata_proposal_is_not_write_or_bytes_approval(self):
        result = self.run_mapping()
        self.assertEqual('PROPOSED_VERIFIED_METADATA_MAPPING', result['status'])
        self.assertEqual(25, len(result['mappings']))
        self.assertFalse(result['source_bytes_verified'])
        self.assertFalse(result['write_authorized'])
        self.assertEqual('9223372036854700000', result['mappings'][0]['claimId'])

    def test_wrong_tenant_unsafe_id_count_drift_and_truncated_capture_fail(self):
        for kind, field, value in [('claim', 'tenantId', '122'), ('claim', 'id', 9007199254740993), ('runtime', 'activeClaimCount', 26)]:
            changed = copy.deepcopy(self.rows)
            next(x for x in changed if x['kind'] == kind)[field] = value
            self.assertEqual('INVALID_CAPTURE', self.run_mapping(changed)['status'])
        self.assertEqual('INVALID_CAPTURE', self.run_mapping(self.rows[:-1])['status'])

    def test_missing_master_source_metadata_or_sha_stays_unconfirmed(self):
        for kind, field, value in [('version', 'sourceFileId', None), ('version', 'sourceSha256', None), ('storage', 'name', None), ('master', 'deleted', 1)]:
            changed = copy.deepcopy(self.rows)
            next(x for x in changed if x['kind'] == kind)[field] = value
            result = self.run_mapping(changed)
            self.assertNotEqual('PROPOSED_VERIFIED_METADATA_MAPPING', result['status'])

    def test_normalized_title_and_original_pdf_do_not_replace_source_name(self):
        self.row('claim')['normalizedName'] = 'DO_NOT_USE_TEMPLATE'
        self.row('claim')['nameHex'] = 'DO_NOT_USE_TEMPLATE'.encode().hex().upper()
        self.row('master')['fileName'] = 'DO_NOT_USE_TITLE'
        self.row('version')['originalFileId'] = '777'
        self.assertEqual('Source 0.DOCX ', self.run_mapping()['mappings'][0]['proposedOriginalName'])
        self.row('version')['sourceFileId'] = None
        self.assertIsNone(self.run_mapping()['mappings'][0]['proposedOriginalName'])

    def test_multiversion_different_name_or_same_number_keeps_exact_identity(self):
        version = copy.deepcopy(self.row('version')); version['id'] = '77777'; version['versionNo'] = 'A/1'; version['sourceFileId'] = '77778'
        storage = copy.deepcopy(self.row('storage')); storage['id'] = '77778'; storage['name'] = 'Different original.DOCX'; storage['nameHex'] = storage['name'].encode().hex().upper()
        self.rows += [version, storage]
        self.module.refresh_fixture_counts(self.rows)
        result = self.run_mapping()
        self.assertEqual('UNCONFIRMED', result['status'])
        self.assertIn('MULTIPLE_EXACT_SOURCE_NAMES', result['mappings'][0]['reasons'])
        self.assertIn('DUPLICATE_VERSION_LABEL', result['mappings'][0]['reasons'])

    def test_name_case_extension_and_trailing_spaces_are_binary_exact(self):
        self.row('storage')['nameHex'] = 'WRONG'
        self.assertEqual('INVALID_CAPTURE', self.run_mapping()['status'])

    def test_source_hash_conflict_ownership_wrong_tenant_and_ticket_wrong_name_reject(self):
        for kind, field, value in [('ownership', 'sourceSha256', 'b' * 64), ('ownership', 'tenantId', '122'), ('ticket', 'originalName', 'WRONG.docx')]:
            changed = copy.deepcopy(self.rows); next(x for x in changed if x['kind'] == kind)[field] = value
            if kind == 'ticket': next(x for x in changed if x['kind'] == kind)['nameHex'] = value.encode().hex().upper()
            self.assertEqual('UNCONFIRMED', self.run_mapping(changed)['status'])

    def test_cross_tenant_source_reference_duplicate_exact_name_and_stale_master_pointer(self):
        for field, value in [('tenantId', '122'), ('masterId', '789')]:
            changed = copy.deepcopy(self.rows); next(x for x in changed if x['kind'] == 'reference')[field] = value
            self.assertEqual('UNCONFIRMED', self.run_mapping(changed)['status'])
        self.row('master')['currentActiveFileId'] = '765'
        self.assertEqual('UNCONFIRMED', self.run_mapping()['status'])

    def test_sql_is_single_select_all_versions_no_credentials_no_fallback(self):
        sql = (HERE / 'g23-legacy-name-mapping.sql').read_text(encoding='utf-8')
        statements = [s.strip() for s in sql.split(';') if s.strip()]
        self.assertEqual(1, len(statements)); self.assertTrue(statements[0].startswith('SELECT '))
        for forbidden in ['upload_ticket', 'session_id', 'url', 'path', 'COALESCE', 'LIMIT 25', 'UPDATE ', 'INSERT ', 'DELETE ']:
            self.assertNotIn(forbidden, sql)
        self.assertIn('source_file_id', sql); self.assertIn('HEX(f.name)', sql)

    def test_missing_master_and_no_versions_are_explicit_unconfirmed(self):
        changed = [r for r in self.rows if r != self.row('master')]
        self.module.refresh_fixture_counts(changed)
        result = self.run_mapping(changed)
        self.assertEqual('UNCONFIRMED', result['status'])
        self.assertIn('MASTER_MISSING', result['mappings'][0]['reasons'])
        changed = [r for r in self.rows if r != self.row('version')]
        self.module.refresh_fixture_counts(changed)
        self.assertIn('VERSION_CHAIN_MISSING', self.run_mapping(changed)['mappings'][0]['reasons'])

    def test_source_name_collision_between_claims_is_not_silently_deduplicated(self):
        storage = [r for r in self.rows if r['kind'] == 'storage']
        tickets = [r for r in self.rows if r['kind'] == 'ticket']
        storage[1]['name'] = storage[0]['name']; storage[1]['nameHex'] = storage[0]['nameHex']
        tickets[1]['originalName'] = storage[0]['name']; tickets[1]['nameHex'] = storage[0]['nameHex']
        result = self.run_mapping()
        self.assertEqual('UNCONFIRMED', result['status'])
        self.assertTrue(all('EXACT_SOURCE_NAME_COLLISION_BETWEEN_CLAIMS' in r['reasons'] for r in result['mappings'][:2]))
        self.assertTrue(all(r['proposedOriginalName'] is None for r in result['mappings'][:2]))

    def test_original_id_and_missing_independent_hash_never_supply_fallback(self):
        self.row('version')['sourceSha256'] = None
        result = self.run_mapping()
        self.assertEqual('UNCONFIRMED', result['status'])
        self.assertIn('SOURCE_SHA256_MISSING_OR_INVALID', result['mappings'][0]['reasons'])
        self.assertIsNone(result['mappings'][0]['proposedOriginalName'])

    def test_foreign_ownership_record_on_same_storage_is_preserved_and_rejected(self):
        owner = copy.deepcopy(self.row('ownership')); owner['id'] = '9999'; owner['controlledFileId'] = '9998'; owner['tenantId'] = '122'
        self.rows.append(owner); self.module.refresh_fixture_counts(self.rows)
        self.assertIn('SOURCE_OWNED_BY_OTHER_VERSION_OR_TENANT', self.run_mapping()['mappings'][0]['reasons'])

    def test_duplicate_rows_and_missing_field_never_produce_mapping(self):
        duplicate = copy.deepcopy(self.rows); duplicate.append(copy.deepcopy(self.row('version')))
        self.module.refresh_fixture_counts(duplicate)
        self.assertEqual('INVALID_CAPTURE', self.run_mapping(duplicate)['status'])
        self.row('storage').pop('configId')
        self.assertEqual('INVALID_CAPTURE', self.run_mapping()['status'])

    def test_contract_hash_is_exact_written_query_bytes_including_windows_newlines(self):
        self.assertEqual(self.contract['querySha256'], hashlib.sha256((HERE / 'g23-legacy-name-mapping.sql').read_bytes()).hexdigest())

    def test_unconfirmed_multiname_master_still_reserves_all_observed_names_for_collision_review(self):
        versions = [r for r in self.rows if r['kind'] == 'version']
        storage = [r for r in self.rows if r['kind'] == 'storage']
        extra = copy.deepcopy(versions[1]); extra.update(id='8001', sourceFileId='8002', versionNo='A/2')
        meta = copy.deepcopy(storage[0]); meta['id'] = '8002'
        self.rows.extend([extra, meta, {'kind': 'reference', 'id': '8001', 'tenantId': extra['tenantId'], 'masterId': extra['masterId'], 'sourceFileId': '8002', 'sourceSha256': extra['sourceSha256'], 'deleted': 0}])
        self.module.refresh_fixture_counts(self.rows)
        result = self.run_mapping()
        self.assertIn('MULTIPLE_EXACT_SOURCE_NAMES', result['mappings'][1]['reasons'])
        self.assertIn('EXACT_SOURCE_NAME_COLLISION_BETWEEN_CLAIMS', result['mappings'][0]['reasons'])
        self.assertIsNone(result['mappings'][0]['proposedOriginalName'])


if __name__ == '__main__':
    unittest.main(verbosity=2)
