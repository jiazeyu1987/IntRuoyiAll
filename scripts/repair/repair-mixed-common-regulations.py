"""Split an accidentally merged common regulation. Dry-run unless --apply is supplied.

Only an explicitly selected tenant and regulation are eligible. Published content,
version IDs and inspection items are preserved; snapshot parent identity follows the split.
Referenced records require a separate
reviewed repair; this tool refuses to modify them.
"""
import argparse
import json
import os
from pathlib import Path
import re

import pymysql
import yaml


def remap_snapshot_identity(raw, source_id, target_id):
    snapshot = json.loads(raw)
    identity = snapshot.get('regulationId')
    if identity is not None and str(identity) != str(source_id):
        raise ValueError('Snapshot does not belong to the selected regulation')
    if identity is None or source_id == target_id:
        return raw
    snapshot['regulationId'] = target_id
    return json.dumps(snapshot, ensure_ascii=False, separators=(',', ':'))


def plan_repair(regulation, versions):
    if regulation['owner_module'] != 'MES_QA_COMMON':
        raise ValueError('Only common regulations can be repaired')
    groups = {}
    for version in versions:
        if version['tenant_id'] != regulation['tenant_id'] or version['regulation_id'] != regulation['id']:
            raise ValueError('Version does not belong to the selected tenant and regulation')
        if version['lifecycle_status'] not in ('PUBLISHED', 'RETIRED') or not version['published_at']:
            raise ValueError('Unpublished versions require a separate reviewed repair')
        remap_snapshot_identity(version['snapshot_json'], regulation['id'], regulation['id'])
        snapshot = json.loads(version['snapshot_json'])
        code = snapshot.get('regulationCode', '').strip()
        name = snapshot.get('regulationName', '').strip()
        if not code or not name:
            raise ValueError('Published snapshot is missing the document identity')
        groups.setdefault(code, []).append((version, name))
    if len(groups) < 2:
        raise ValueError('Selected regulation does not contain mixed document codes')
    if regulation['regulation_code'] not in groups:
        raise ValueError('Current document code is not present in the published snapshots')
    result = []
    for code, members in groups.items():
        members.sort(key=lambda pair: (pair[0]['published_at'], pair[0]['id']))
        latest, name = members[-1]
        if len({v['version_no'] for v, _ in members}) != len(members):
            raise ValueError('Duplicate version numbers within one document')
        result.append(dict(code=code, name=name, latest_version_id=latest['id'],
                           version_ids=[v['id'] for v, _ in members],
                           retain_parent=code == regulation['regulation_code']))
    return result


def connect(config_path):
    documents = list(yaml.safe_load_all(config_path.read_text(encoding='utf-8')))
    datasource = next(d['spring']['datasource']['dynamic']['datasource']['master']
                      for d in documents if d and 'datasource' in d.get('spring', {}))

    def resolve(value):
        match = re.fullmatch(r'\$\{([^:}]+)(?::(.*))?\}', str(value))
        if match:
            value = os.environ.get(match[1], match[2])
            if value is None:
                raise ValueError('Missing database setting: ' + match[1])
        return str(value)

    match = re.fullmatch(r'jdbc:mysql://([^:/]+)(?::(\d+))?/([^?]+)(?:\?.*)?', resolve(datasource['url']))
    if not match:
        raise ValueError('Expected a MySQL JDBC connection')
    return pymysql.connect(host=match[1], port=int(match[2] or 3306), database=match[3],
                           user=resolve(datasource['username']), password=resolve(datasource['password']),
                           cursorclass=pymysql.cursors.DictCursor, charset='utf8mb4', autocommit=False,
                           client_flag=pymysql.constants.CLIENT.FOUND_ROWS)


def repair(connection, tenant_id, regulation_id, *, apply=False, backup_path=None):
    with connection.cursor() as cur:
        if not apply:
            cur.execute('SET TRANSACTION READ ONLY')
        cur.execute('START TRANSACTION')
        lock = ' FOR UPDATE' if apply else ''
        cur.execute('SELECT * FROM mes_qa_inspection_regulation '
                    'WHERE tenant_id=%s AND id=%s AND deleted=0' + lock, (tenant_id, regulation_id))
        regulation = cur.fetchone()
        if not regulation:
            raise ValueError('Selected common regulation does not exist')
        cur.execute('SELECT * FROM mes_qa_inspection_regulation_version '
                    'WHERE tenant_id=%s AND regulation_id=%s AND deleted=0 ORDER BY id' + lock,
                    (tenant_id, regulation_id))
        versions = cur.fetchall()
        plan = plan_repair(regulation, versions)
        version_ids = [v['id'] for v in versions]
        placeholders = ','.join(['%s'] * len(version_ids))
        # Discover every direct identity reference; inspection children keep the same
        # version IDs and need no update. Unknown identity references fail explicitly.
        cur.execute("SELECT TABLE_NAME,COLUMN_NAME FROM information_schema.COLUMNS "
                    "WHERE TABLE_SCHEMA=DATABASE() AND COLUMN_NAME IN "
                    "('regulation_id','common_regulation_id','qa_regulation_id',"
                    "'common_regulation_version_id','qa_regulation_version_id','regulation_version_id')")
        for column in cur.fetchall():
            table, key = column['TABLE_NAME'], column['COLUMN_NAME']
            if table in ('mes_qa_inspection_regulation_version', 'mes_qa_inspection_regulation_process',
                         'mes_qa_inspection_regulation_item', 'mes_qa_inspection_regulation_item_equipment'):
                continue
            if key.endswith('regulation_id'):
                query, args = f'`{key}`=%s', (regulation_id,)
            else:
                query, args = f'`{key}` IN ({placeholders})', version_ids
            cur.execute(f'SELECT COUNT(*) AS count FROM `{table}` WHERE {query}', args)
            if cur.fetchone()['count']:
                raise ValueError(f'Referenced data requires separate review: {table}.{key}')
        # Do not create a second parent when a document is already represented elsewhere.
        for group in plan:
            cur.execute('SELECT id FROM mes_qa_inspection_regulation WHERE tenant_id=%s '
                        'AND owner_module=%s AND regulation_code=%s AND id<>%s AND deleted=0',
                        (tenant_id, 'MES_QA_COMMON', group['code'], regulation_id))
            if cur.fetchone():
                raise ValueError('Document already has another parent: ' + group['code'])
        if not apply:
            connection.rollback()
            return plan
        if backup_path is None:
            raise ValueError('--backup is required when applying the repair')
        backup = dict(tenant_id=tenant_id, regulation=regulation,
                      versions=[{key: v[key] for key in
                                 ('id', 'regulation_id', 'snapshot_json', 'lifecycle_status', 'retired_at', 'update_time', 'updater')}
                                for v in versions], created_regulation_ids=[], status='prepared')
        original_snapshots = {v['id']: v['snapshot_json'] for v in versions}
        expected_snapshots = {}
        # Exclusive creation prevents overwriting previous recovery evidence.
        with backup_path.open('x', encoding='utf-8') as file:
            json.dump(backup, file, ensure_ascii=False, default=str, indent=2)
        for group in plan:
            parent_id = regulation_id
            if not group['retain_parent']:
                cur.execute('INSERT INTO mes_qa_inspection_regulation '
                            '(dcc_project_code_id,owner_module,regulation_code,regulation_name,'
                            'lifecycle_status,current_version_id,tenant_id,creator,updater) '
                            'VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s)',
                            (regulation['dcc_project_code_id'], 'MES_QA_COMMON', group['code'], group['name'],
                             'PUBLISHED', group['latest_version_id'], tenant_id, 'common-identity-repair',
                             'common-identity-repair'))
                parent_id = cur.lastrowid
                backup['created_regulation_ids'].append(parent_id)
            cur.execute('UPDATE mes_qa_inspection_regulation SET regulation_name=%s,'
                        'lifecycle_status=%s,current_version_id=%s WHERE tenant_id=%s AND id=%s',
                        (group['name'], 'PUBLISHED', group['latest_version_id'], tenant_id, parent_id))
            for version_id in group['version_ids']:
                latest = version_id == group['latest_version_id']
                expected_snapshots[version_id] = remap_snapshot_identity(
                    original_snapshots[version_id], regulation_id, parent_id)
                cur.execute('UPDATE mes_qa_inspection_regulation_version SET regulation_id=%s,'
                            'snapshot_json=%s,lifecycle_status=%s,retired_at=IF(%s,NULL,retired_at) '
                            'WHERE tenant_id=%s AND id=%s AND regulation_id=%s',
                            (parent_id, expected_snapshots[version_id], 'PUBLISHED' if latest else 'RETIRED', latest,
                             tenant_id, version_id, regulation_id))
                if cur.rowcount != 1:
                    raise ValueError('Version changed during the repair')
            group['regulation_id'] = parent_id
        cur.execute(f'SELECT id,regulation_id,lifecycle_status,retired_at,snapshot_json '
                    f'FROM mes_qa_inspection_regulation_version WHERE tenant_id=%s '
                    f'AND id IN ({placeholders})', [tenant_id, *version_ids])
        repaired_versions = {v['id']: v for v in cur.fetchall()}
        if len(repaired_versions) != len(version_ids):
            raise ValueError('Version count changed during the repair')
        for group in plan:
            for version_id in group['version_ids']:
                actual = repaired_versions[version_id]
                latest = version_id == group['latest_version_id']
                if (actual['regulation_id'] != group['regulation_id']
                        or actual['lifecycle_status'] != ('PUBLISHED' if latest else 'RETIRED')
                        or json.loads(actual['snapshot_json']) != json.loads(expected_snapshots[version_id])
                        or (latest and actual['retired_at'] is not None)):
                    raise ValueError('Post-repair identity, status or content verification failed')
        backup['status'] = 'ready-to-commit'
        backup_path.write_text(json.dumps(backup, ensure_ascii=False, default=str, indent=2), encoding='utf-8')
        connection.commit()
        backup['status'] = 'committed'
        backup_path.write_text(json.dumps(backup, ensure_ascii=False, default=str, indent=2), encoding='utf-8')
        return plan


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--config', type=Path, default=Path('IntRuoyiBackend/yudao-server/src/main/resources/application-local.yaml'))
    parser.add_argument('--tenant-id', type=int, required=True)
    parser.add_argument('--regulation-id', type=int, required=True)
    parser.add_argument('--apply', action='store_true')
    parser.add_argument('--backup', type=Path)
    args = parser.parse_args()
    connection = connect(args.config)
    try:
        plan = repair(connection, args.tenant_id, args.regulation_id, apply=args.apply, backup_path=args.backup)
        print(json.dumps(dict(applied=args.apply, tenant_id=args.tenant_id, plan=plan), ensure_ascii=True))
    except Exception:
        connection.rollback()
        raise
    finally:
        connection.close()


if __name__ == '__main__':
    main()
