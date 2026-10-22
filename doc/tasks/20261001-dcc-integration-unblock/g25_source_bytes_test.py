import copy
import unittest
from g25_source_bytes import expectations, reader_payload, verified_results


def fixture():
    facts = [{'kind': 'runtime', 'database': 'ruoyi-vue-pro', 'serverUuid': 'uuid', 'tenantId': '1'}]
    fresh = [{'kind': 'runtime', 'database': 'ruoyi-vue-pro', 'serverUuid': 'uuid'},
             {'kind': 'config', 'id': '28', 'storage': 20, 'deleted': 0,
              'config': {'endpoint': 'http://127.0.0.1:9000', 'bucket': 'test', 'region': 'local',
                         'accessKey': 'test-key', 'accessSecret': 'test-secret', 'enablePathStyleAccess': True}}]
    for n in range(39):
        ident = str(9007199254740993 + n)
        facts += [{'kind': 'version', 'id': str(n+1), 'tenantId': '1', 'sourceFileId': ident,
                   'sourceSha256': 'ab'*32, 'deleted': 0},
                  {'kind': 'storage', 'id': ident, 'configId': '28', 'name': 'A.docx',
                   'nameHex': 'A.docx'.encode().hex().upper(), 'size': '12', 'deleted': 0}]
        fresh.append({'kind': 'storage', 'id': ident, 'configId': '28', 'nameHex': 'A.docx'.encode().hex().upper(),
                      'size': '12', 'key': 'temporary/'+ident, 'deleted': 0})
        fresh.append({'kind': 'version', 'id': str(n+1), 'tenantId': '1', 'sourceFileId': ident,
                      'sourceSha256': 'ab'*32, 'deleted': 0})
    return facts, fresh


class SourceBytesContract(unittest.TestCase):
    def test_exact_large_ids_and_no_secret_in_evidence(self):
        f, r = fixture()
        e, identity = expectations(f)
        payload = reader_payload(e, identity, r)
        self.assertEqual('9007199254740993', payload['files'][0]['id'])
        self.assertEqual(39, len(payload['files']))
        self.assertEqual('test-secret', payload['config']['accessSecret'])
        self.assertNotIn('accessSecret', str(e))

    def test_missing_or_duplicate_source_metadata_rejected(self):
        for change in ('missing', 'duplicate'):
            f, _ = fixture()
            if change == 'missing': f.pop()
            else: f.append(copy.deepcopy(f[-1]))
            with self.assertRaises(ValueError): expectations(f)

    def test_source_sha_and_tenant_required(self):
        for field, value in [('sourceSha256', None), ('sourceSha256', 'fake'), ('tenantId', '2')]:
            f, _ = fixture()
            f[1][field] = value
            with self.assertRaises(ValueError): expectations(f)

    def test_fresh_metadata_drift_and_foreign_runtime_fail(self):
        for field, value in [('configId', '29'), ('nameHex', 'AA'), ('size', '13'), ('deleted', 1)]:
            f, r = fixture(); e, identity = expectations(f)
            next(x for x in r if x['kind']=='storage')[field] = value
            with self.assertRaises(ValueError): reader_payload(e, identity, r)
        f, r = fixture(); e, identity = expectations(f)
        r[0]['serverUuid'] = 'foreign'
        with self.assertRaises(ValueError): reader_payload(e, identity, r)

    def test_fresh_version_binding_or_sha_drift_refused(self):
        for field, value in [('sourceFileId', '3'), ('sourceSha256', 'cd'*32), ('tenantId', '2'),
                             ('id', '88'), ('deleted', 1)]:
            f, r = fixture(); e, identity = expectations(f)
            next(x for x in r if x['kind']=='version')[field] = value
            with self.assertRaises(ValueError): reader_payload(e, identity, r)

    def test_remote_or_implicit_config_refused(self):
        for field, value in [('endpoint', 'https://example.com'), ('endpoint', 'http://localhost:9000/foreign'),
                             ('endpoint', 'http://secret@localhost:9000'), ('region', ''),
                             ('accessSecret', ''), ('enablePathStyleAccess', False)]:
            f, r = fixture(); e, identity = expectations(f)
            r[1]['config'][field] = value
            with self.assertRaises(ValueError): reader_payload(e, identity, r)

    def test_result_sha_length_status_and_exit_are_all_verified(self):
        f, _ = fixture(); e, _ = expectations(f)
        rows = [{'id': i, 'status': 'MATCH', 'actualSha256': 'ab'*32, 'actualLength': 12,
                 'httpStatus': 200, 'errorCode': None} for i in e]
        self.assertTrue(verified_results(e, rows, 0))
        for field, value in [('id', '2'), ('actualSha256', 'cd'*32), ('actualLength', 11),
                             ('httpStatus', 201), ('errorCode', 'FAKE'), ('extra', 'secret')]:
            bad = copy.deepcopy(rows); bad[0][field] = value
            with self.assertRaises(ValueError): verified_results(e, bad, 0)
        with self.assertRaises(ValueError): verified_results(e, rows, 1)
        with self.assertRaises(ValueError): verified_results(e, rows[:-1], 0)
        bad = copy.deepcopy(rows); bad[0].update(status='HTTP_ERROR', actualSha256=None,
                                              actualLength=None, httpStatus=404, errorCode='NoSuchKey')
        self.assertFalse(verified_results(e, bad, 1))
        with self.assertRaises(ValueError): verified_results(e, bad, 0)

    def test_failed_result_never_persists_arbitrary_strings(self):
        f, _ = fixture(); e, _ = expectations(f)
        rows = [{'id': i, 'status': 'MATCH', 'actualSha256': 'ab'*32, 'actualLength': 12,
                 'httpStatus': 200, 'errorCode': None} for i in e]
        for field in ('errorCode', 'actualSha256', 'actualLength', 'httpStatus'):
            bad = copy.deepcopy(rows)
            bad[0].update(status='READ_ERROR', actualSha256=None, actualLength=None,
                          httpStatus=None, errorCode='IO_ERROR')
            bad[0][field] = 'test-secret'
            with self.assertRaises(ValueError): verified_results(e, bad, 1)


if __name__ == '__main__': unittest.main()
