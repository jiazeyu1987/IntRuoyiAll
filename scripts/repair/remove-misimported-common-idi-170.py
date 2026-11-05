"""Remove tenant 170's mistaken common IDI entry; preserve all published versions.

Dry-run by default. This deliberately targets only parent 83 / version 100;
the genuine PQC parents 79 and 80 and every snapshot must remain unchanged.
"""
import argparse
import importlib.util
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location(
    'mixed_common_repair', Path(__file__).with_name('repair-mixed-common-regulations.py'))
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


def capture(cursor, lock=''):
    cursor.execute('SELECT * FROM mes_qa_inspection_regulation WHERE tenant_id=170 '
                   'AND id IN (78,79,80,82,83,84) ORDER BY id' + lock)
    parents = cursor.fetchall()
    cursor.execute('SELECT * FROM mes_qa_inspection_regulation_version WHERE tenant_id=170 '
                   'AND id IN (99,100,101,102,103,104,105) ORDER BY id' + lock)
    return dict(parents=parents, versions=cursor.fetchall())


def run(connection, apply, backup):
    with connection.cursor() as cursor:
        if not apply:
            cursor.execute('SET TRANSACTION READ ONLY')
        cursor.execute('START TRANSACTION')
        before = capture(cursor, ' FOR UPDATE' if apply else '')
        parents = {row['id']: row for row in before['parents']}
        versions = {row['id']: row for row in before['versions']}
        wrong = parents[83]
        if (wrong['deleted'] != b'\x00' or wrong['owner_module'] != 'MES_QA_COMMON'
                or wrong['regulation_code'] != 'PQC-IDI-001'
                or wrong['current_version_id'] != 100
                or versions[100]['regulation_id'] != 83):
            raise ValueError('Unexpected common record; no changes made')
        for parent_id, version_id in ((79, 101), (80, 102)):
            parent, version = parents[parent_id], versions[version_id]
            if (parent['deleted'] != b'\x00' or parent['owner_module'] != 'MES_QA'
                    or parent['regulation_code'] != 'PQC-IDI-001'
                    or parent['current_version_id'] != version_id
                    or version['deleted'] != b'\x00' or version['regulation_id'] != parent_id
                    or version['lifecycle_status'] != 'PUBLISHED'):
                raise ValueError('Expected published PQC counterpart is missing')
        cursor.execute("SELECT TABLE_NAME,COLUMN_NAME FROM information_schema.COLUMNS "
                       "WHERE TABLE_SCHEMA=DATABASE() AND COLUMN_NAME IN "
                       "('regulation_id','common_regulation_id','qa_regulation_id',"
                       "'common_regulation_version_id','qa_regulation_version_id',"
                       "'regulation_version_id')")
        for column in cursor.fetchall():
            table, key = column['TABLE_NAME'], column['COLUMN_NAME']
            if table in ('mes_qa_inspection_regulation_version',
                         'mes_qa_inspection_regulation_process',
                         'mes_qa_inspection_regulation_item',
                         'mes_qa_inspection_regulation_item_equipment'):
                continue
            cursor.execute(f'SELECT COUNT(*) AS n FROM `{table}` WHERE `{key}`=%s',
                           (83 if key.endswith('regulation_id') else 100,))
            if cursor.fetchone()['n']:
                raise ValueError(f'Downstream reference prevents removal: {table}.{key}')
        if not apply:
            connection.rollback()
            return {'applied': False, 'tenant': 170, 'remove_common_parent': 83,
                    'preserve_pqc_parents': [79, 80], 'preserve_version_ids': list(versions)}
        if backup is None:
            raise ValueError('--backup is required for --apply')
        with backup.open('x', encoding='utf-8') as file:
            json.dump(before, file, ensure_ascii=False, default=str, indent=2)
        # Logical deletion of the mistaken parent excludes it from the common list.
        # Versions and snapshots remain intact for audit and reversible restoration.
        cursor.execute("UPDATE mes_qa_inspection_regulation SET deleted=1,"
                       "updater='common-idi-classification-fix' "
                       "WHERE tenant_id=170 AND id=83 AND deleted=0 "
                       "AND owner_module='MES_QA_COMMON' AND current_version_id=100")
        if cursor.rowcount != 1:
            raise ValueError('Selected common parent changed during repair')
        after = capture(cursor)
        if after['versions'] != before['versions']:
            raise ValueError('Published versions changed; rolling back')
        for old, new in zip(before['parents'], after['parents']):
            if old['id'] != new['id']:
                raise ValueError('Parent inventory changed; rolling back')
            if old['id'] == 83:
                if new['deleted'] != b'\x01':
                    raise ValueError('Logical deletion failed')
                new = dict(new, deleted=old['deleted'], updater=old['updater'],
                           update_time=old['update_time'])
            if new != old:
                raise ValueError('Unexpected parent changes; rolling back')
        connection.commit()
        return {'applied': True, 'removed_common_parent': 83,
                'published_versions_unchanged': True, 'pqc_parents_unchanged': [79, 80],
                'backup': str(backup)}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apply', action='store_true')
    parser.add_argument('--backup', type=Path)
    args = parser.parse_args()
    connection = module.connect(ROOT / 'IntRuoyiBackend/yudao-server/src/main/resources/application-local.yaml')
    try:
        print(json.dumps(run(connection, args.apply, args.backup), ensure_ascii=False))
    except Exception:
        connection.rollback()
        raise
    finally:
        connection.close()
