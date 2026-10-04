import copy
import unittest
from g21_mysql_support import compare_original_rows, identifier, LocalMysql


class HistoricalProofTests(unittest.TestCase):
    def setUp(self):
        self.before = {'dcc_controlled_file': {'columns': ['id', 'version_no', 'body_hash'], 'rows': {'31': 'a' * 64}, 'count': 1, 'aggregateSha256': 'b' * 64},
                       'act_re_procdef': {'columns': ['ID_', 'VERSION_'], 'rows': {'41': 'c' * 64}, 'count': 1, 'aggregateSha256': 'd' * 64}}

    def test_old_payloads_and_exact_permitted_configuration_deltas(self):
        after = copy.deepcopy(self.before); after['act_re_procdef']['rows']['42'] = 'e' * 64
        result = compare_original_rows(self.before, after, {'act_re_procdef': 1})
        self.assertTrue(result['dcc_controlled_file']['oldRowsUnchanged'])
        self.assertEqual(result['act_re_procdef']['newRowCount'], 1)

    def test_equal_counts_do_not_hide_changed_historical_signature_or_body(self):
        after = copy.deepcopy(self.before); after['dcc_controlled_file']['rows']['31'] = 'f' * 64
        with self.assertRaises(ValueError):
            compare_original_rows(self.before, after, {})

    def test_deleted_original_and_extra_unauthorized_row_reject(self):
        for kind in ['deleted', 'added']:
            after = copy.deepcopy(self.before)
            if kind == 'deleted': del after['dcc_controlled_file']['rows']['31']
            else: after['dcc_controlled_file']['rows']['32'] = 'f' * 64
            with self.assertRaises(ValueError):
                compare_original_rows(self.before, after, {})

    def test_new_columns_cannot_replace_the_frozen_original_projection(self):
        after = copy.deepcopy(self.before); after['dcc_controlled_file']['columns'].append('file_owner_user_id')
        with self.assertRaises(ValueError):
            compare_original_rows(self.before, after, {})

    def test_sql_identifiers_reject_injection_and_foreign_database_never_initializes_transport(self):
        self.assertEqual(identifier('dcc_controlled_file'), '`dcc_controlled_file`')
        for value in ['t` DROP TABLE t', 'a.b', '../other']:
            with self.assertRaises(ValueError): identifier(value)
        with self.assertRaises(ValueError): LocalMysql('other_database')


if __name__ == '__main__': unittest.main(verbosity=2)
