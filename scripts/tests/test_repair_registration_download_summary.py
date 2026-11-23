import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location('reg_download_repair', Path(__file__).parents[1] / 'repair' /
                                            'repair-registration-download-summary.py')
repair = importlib.util.module_from_spec(spec)
spec.loader.exec_module(repair)


class RegistrationDownloadSummaryRepairTest(unittest.TestCase):
    def test_legacy_request_uses_explicit_references_and_keeps_payload_unchanged(self):
        submission = {'requestType': 'DOWNLOAD_FILE', 'payloadHash': 'original'}
        values, origins = repair.plan_variables(submission, '所属公司', ['旧证名称', '旧证名称'], [])
        self.assertEqual({'ownerCompanyName': '所属公司', 'certificateName': '旧证名称'}, values)
        self.assertEqual('request owner company reference at repair time', origins['ownerCompanyName'])
        self.assertEqual({'requestType': 'DOWNLOAD_FILE', 'payloadHash': 'original'}, submission)

    def test_frozen_names_win_over_renamed_current_references(self):
        frozen = {'ownerCompanyName': '申请时公司', 'certificateName': '申请时证名'}
        values, origins = repair.plan_variables(frozen, '改名后的公司', ['后续名称'], [])
        self.assertEqual(frozen, values)
        self.assertEqual({'submitted detail_json'}, set(origins.values()))
        existing = [{'NAME_': key, 'TEXT_': value} for key, value in values.items()]
        self.assertEqual(({}, {}), repair.plan_variables(frozen, '改名', [], existing))

    def test_never_hides_incomplete_files_or_corrupt_existing_variable(self):
        for names in ([], ['旧证', None], ['旧证', ' ']):
            with self.assertRaises(ValueError):
                repair.plan_variables({}, '公司', names, [])
        for existing in ([{'NAME_': 'ownerCompanyName', 'TEXT_': ''}],
                         [{'NAME_': 'ownerCompanyName', 'TEXT_': '另一公司'}],
                         [{'NAME_': 'ownerCompanyName', 'TEXT_': '公司'}] * 2):
            with self.assertRaises(ValueError):
                repair.plan_variables({}, '公司', ['证名'], existing)
        for invalid in (None, '', ' ', 123):
            with self.assertRaises(ValueError):
                repair.plan_variables({'certificateName': invalid}, '公司', ['可用参考'], [])


if __name__ == '__main__':
    unittest.main()
