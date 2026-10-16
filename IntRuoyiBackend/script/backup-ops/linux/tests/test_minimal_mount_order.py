"""Replay pinned W05 inspect observations locally; never invoke Docker or a live DB.

The frontend was already running in the read-only evidence. Fence-path tests
explicitly set both replay writers stopped to model that call's required stage.
Only local replay state changes on start; these tests prove no remote recovery.
"""
import copy
import json
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from formal_snapshot_linux import FormalSnapshotError
from minimal_capture_support import Budget
from minimal_capture_worker import Runtime, canonical_mount_records


FIXTURE = json.loads((Path(__file__).parent/'fixtures'/'w05_mount_order_inspects_20261015.json').read_text(encoding='utf-8'))
DATABASE_UUID = '01234567-1234-1234-1234-0123456789ab'
FENCE_SQL = 'SELECT @@server_uuid,@@global.read_only,@@global.super_read_only;'


class DatabaseReplay:
    def __init__(self):
        self.uuid = DATABASE_UUID; self.flags = ['1', '1']; self.queries = []

    def query(self, sql):
        self.queries.append(sql)
        if sql == FENCE_SQL: return self.uuid+'\t'+'\t'.join(self.flags)
        if sql == 'SELECT @@server_uuid;': return self.uuid
        if sql == 'SET GLOBAL super_read_only=OFF; SET GLOBAL read_only=0; SET GLOBAL super_read_only=0;':
            self.flags = ['0', '0']; return ''
        raise AssertionError('Unexpected query in local replay')


class InspectReplay(Runtime):
    """Retain real Runtime.inspect identity checks; replace only command I/O."""
    def __init__(self, config, budget, root, rows):
        super().__init__(config, budget, root)
        self.rows = copy.deepcopy(rows); self.commands = []; self.start_succeeds = True

    def command(self, arguments, *, cleanup=False):
        self.commands.append((arguments, cleanup))
        if arguments[0] == 'inspect':
            assert arguments[1] == '--format' and arguments[-2] == '--'
            return json.dumps(self.rows[arguments[-1]]).encode('utf-8')
        if arguments == ['start', '--', arguments[-1]]:
            assert cleanup is True
            if self.start_succeeds: self.rows[arguments[-1]]['running'] = True
            return b''
        raise AssertionError('Unexpected command in local replay')

    @property
    def starts(self): return [args[-1] for args, _ in self.commands if args[0] == 'start']


class MountOrderRegressionTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix='w02-mount-order-unit-')
        self.addCleanup(self.temp.cleanup); self.root = Path(self.temp.name)
        self.writers = copy.deepcopy(FIXTURE['writerInspects'])
        self.variants = {r['id']: r['orderedMountVariants'] for r in FIXTURE['observations']}

    def runtime(self):
        limits = dict(workSeconds=30, cleanupSeconds=3, querySeconds=1,
            maxWrittenBytes=1024*1024, minimumFreeBytes=1)
        source = dict(writerContainerIds=[r['id'] for r in self.writers],
            minioContainerId=FIXTURE['minioInspect']['id'], databaseUuid=DATABASE_UUID,
            sourceIdentity='local-replay-only')
        rows = {r['id']: copy.deepcopy(r) for r in [*self.writers, FIXTURE['minioInspect']]}
        runtime = InspectReplay(dict(source=source, limits=limits), Budget(self.root, limits), self.root, rows)
        runtime.initial = copy.deepcopy(rows); runtime.mysql = DatabaseReplay()
        for writer in self.writers: runtime.rows[writer['id']]['running'] = False
        return runtime

    def expect_code(self, code, fn):
        with self.assertRaises(FormalSnapshotError) as error: fn()
        self.assertEqual(code, error.exception.code)

    def reject_mounts(self, container, mounts, initial=None):
        for path, code in [('protected', 'MINIMAL_WRITER_RESUMED'),
                ('restore_container', 'MINIMAL_SOURCE_CONTAINER_REPLACED')]:
            with self.subTest(path=path):
                runtime = self.runtime()
                if initial is not None: runtime.initial[container]['mounts'] = copy.deepcopy(initial)
                runtime.rows[container]['mounts'] = copy.deepcopy(mounts)
                action = (lambda: runtime.protected(force=True)) if path == 'protected' else (
                    lambda: runtime.restore_container(container))
                self.expect_code(code, action); self.assertEqual([], runtime.starts)

    def test_recorded_variants_exhibit_the_reported_order_only_difference(self):
        self.assertEqual(2, len(FIXTURE['observations']))
        for observation in FIXTURE['observations']:
            self.assertEqual(12, observation['samples'])
            self.assertEqual(12, sum(v['count'] for v in observation['orderedMountVariants']))
            self.assertEqual(1, observation['identityCount'])
            first, second = [v['mounts'] for v in observation['orderedMountVariants']]
            self.assertNotEqual(first, second)
            self.assertEqual(first, list(reversed(second)))
            recorded = next(r['mounts'] for r in self.writers if r['id'] == observation['id'])
            self.assertEqual(recorded, first)

    def test_protected_accepts_each_recorded_before_after_order_pair(self):
        for writer in self.writers:
            container = writer['id']
            for before in self.variants[container]:
                for after in self.variants[container]:
                    with self.subTest(container=container, before=before['count'], after=after['count']):
                        runtime = self.runtime()
                        runtime.initial[container]['mounts'] = copy.deepcopy(before['mounts'])
                        runtime.rows[container]['mounts'] = copy.deepcopy(after['mounts'])
                        runtime.protected(force=True)
                        self.assertEqual([FENCE_SQL], runtime.mysql.queries)
                        self.assertEqual([], runtime.starts)

    def test_restore_accepts_each_order_pair_for_stopped_and_running_writers(self):
        for writer in self.writers:
            container = writer['id']
            for before in self.variants[container]:
                for after in self.variants[container]:
                    for running in (False, True):
                        with self.subTest(container=container, before=before['count'], after=after['count'], running=running):
                            runtime = self.runtime()
                            runtime.initial[container]['mounts'] = copy.deepcopy(before['mounts'])
                            runtime.rows[container].update(mounts=copy.deepcopy(after['mounts']), running=running)
                            runtime.restore_container(container)
                            self.assertEqual([] if running else [container], runtime.starts)
                            self.assertTrue(all(cleanup for _, cleanup in runtime.commands))

    def test_every_recorded_mount_field_value_change_is_rejected(self):
        for writer in self.writers:
            for index, mount in enumerate(writer['mounts']):
                for key, value in mount.items():
                    with self.subTest(container=writer['id'], record=index, field=key):
                        mounts = copy.deepcopy(writer['mounts'])
                        mounts[index][key] = not value if type(value) is bool else value+'-changed'
                        self.reject_mounts(writer['id'], mounts)

    def test_every_recorded_mount_field_removal_is_rejected(self):
        for writer in self.writers:
            for index, mount in enumerate(writer['mounts']):
                for key in mount:
                    with self.subTest(container=writer['id'], record=index, field=key):
                        mounts = copy.deepcopy(writer['mounts']); del mounts[index][key]
                        self.reject_mounts(writer['id'], mounts)

    def test_new_mount_fields_are_not_discarded(self):
        for writer in self.writers:
            mounts = copy.deepcopy(writer['mounts']); mounts[0]['NewDockerField'] = {'enabled': True}
            self.reject_mounts(writer['id'], mounts)

    def test_mount_add_remove_and_same_length_duplicate_substitution_are_rejected(self):
        for writer in self.writers:
            mounts = writer['mounts']
            for change in [mounts[:-1], mounts+[mounts[0]], [mounts[0], mounts[0]]]:
                with self.subTest(container=writer['id'], mounts=change):
                    self.reject_mounts(writer['id'], change)

    def test_duplicate_records_are_retained_in_both_comparisons(self):
        container = self.writers[0]['id']; a, b = self.writers[0]['mounts']
        initial = [a, a, b]; reordered = [b, a, a]
        runtime = self.runtime(); runtime.initial[container]['mounts'] = copy.deepcopy(initial)
        runtime.rows[container]['mounts'] = copy.deepcopy(reordered)
        runtime.protected(force=True); runtime.restore_container(container)
        self.assertEqual([container], runtime.starts)
        self.reject_mounts(container, [b, a], initial)
        self.reject_mounts(container, [b, b, a], initial)

    def test_nested_unknown_fields_keep_values_and_array_order(self):
        container = self.writers[0]['id']; initial = copy.deepcopy(self.writers[0]['mounts'])
        initial[0]['Future'] = {'options': ['one', 'two'], 'enabled': True}
        runtime = self.runtime(); runtime.initial[container]['mounts'] = copy.deepcopy(initial)
        runtime.rows[container]['mounts'] = copy.deepcopy(list(reversed(initial)))
        runtime.protected(force=True); runtime.restore_container(container)
        for future in ({'options': ['two', 'one'], 'enabled': True},
                {'options': ['one', 'two'], 'enabled': False}):
            changed = copy.deepcopy(initial); changed[0]['Future'] = future
            self.reject_mounts(container, changed, initial)

    def test_image_and_inspect_identity_changes_are_still_rejected(self):
        for writer in self.writers:
            for key, replacement in [('image', 'sha256:'+'f'*64), ('id', 'f'*64)]:
                for path in ('protected', 'restore_container'):
                    with self.subTest(container=writer['id'], key=key, path=path):
                        runtime = self.runtime(); runtime.rows[writer['id']][key] = replacement
                        code = 'MINIMAL_CONTAINER_CHANGED' if key == 'id' else (
                            'MINIMAL_WRITER_RESUMED' if path == 'protected' else 'MINIMAL_SOURCE_CONTAINER_REPLACED')
                        action = (lambda: runtime.protected(force=True)) if path == 'protected' else (
                            lambda: runtime.restore_container(writer['id']))
                        self.expect_code(code, action); self.assertEqual([], runtime.starts)

    def test_writer_state_and_minio_state_fence_remain_enforced(self):
        for key in ('running', 'paused'):
            runtime = self.runtime(); runtime.rows[self.writers[0]['id']][key] = True
            self.expect_code('MINIMAL_WRITER_RESUMED', lambda: runtime.protected(force=True))
        for minio_stopped in (False, True):
            runtime = self.runtime(); runtime.minio_stopped = minio_stopped
            runtime.rows[runtime.source['minioContainerId']]['running'] = minio_stopped
            self.expect_code('MINIMAL_MINIO_STATE_CHANGED', lambda: runtime.protected(force=True))

    def test_database_uuid_and_read_only_fence_remain_enforced(self):
        for uuid, flags in [('changed-uuid', ['1', '1']), (DATABASE_UUID, ['0', '1']),
                (DATABASE_UUID, ['1', '0'])]:
            runtime = self.runtime(); runtime.mysql.uuid = uuid; runtime.mysql.flags = flags
            self.expect_code('MINIMAL_DATABASE_FENCE_LOST', lambda: runtime.protected(force=True))

    def test_start_without_observed_running_state_is_not_success(self):
        runtime = self.runtime(); runtime.start_succeeds = False
        self.expect_code('MINIMAL_SOURCE_START_UNPROVEN', lambda: runtime.restore_container(self.writers[0]['id']))

    def test_restore_flow_accepts_reordering_after_database_flags_are_verified(self):
        runtime = self.runtime(); runtime.flags = ['0', '0']; runtime.fence_attempted = True
        runtime.touched = [r['id'] for r in self.writers]
        for writer in self.writers: runtime.rows[writer['id']]['mounts'].reverse()
        reference = runtime.restore()
        self.assertEqual(list(reversed(runtime.touched)), runtime.starts)
        self.assertEqual(['0', '0'], runtime.mysql.flags)
        self.assertTrue(json.loads(Path(reference['path']).read_text(encoding='utf-8'))['restored'])

    def test_data_restore_failure_retains_writers_stopped_and_restored_false(self):
        runtime = self.runtime(); runtime.flags = ['0', '0']; runtime.fence_attempted = True
        runtime.touched = [r['id'] for r in self.writers]; runtime.mysql.uuid = 'changed-uuid'
        self.expect_code('MINIMAL_SOURCE_RESTORATION_FAILED', runtime.restore)
        result = json.loads((self.root/'source-restoration.json').read_text(encoding='utf-8'))
        self.assertIs(False, result['restored']); self.assertEqual([], runtime.starts)
        self.assertEqual([dict(step='DATABASE_FLAGS_RESTORED', failureCode='MINIMAL_DATABASE_UUID_CHANGED')], result['errors'])

    def test_malformed_mount_collections_fail_explicitly(self):
        for mounts in (None, {}, 'invalid', [None], [['not-a-record']]):
            self.expect_code('MINIMAL_SOURCE_MOUNTS_INVALID', lambda: canonical_mount_records(mounts))


if __name__ == '__main__': unittest.main()
