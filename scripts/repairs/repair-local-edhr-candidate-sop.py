"""One-off local test1 correction; dry run unless --apply is explicitly supplied.

Only the pinned ACTIVE snapshots below may change. No application fallback.
Each missing SOP must be proven unbound by an unchanged, older formal process.
Original rows and the exact transaction are saved before any database write.
"""
import argparse
import copy
from decimal import Decimal
import hashlib
import json
from pathlib import Path
import subprocess

PINS = {
    2121453182031057239: '4003d46a079f58efb7a707225fb8fe94d1de2534df8ee5c6430494204bc979c9',
    2121453182031057241: '11156904a5fe93f8f987ac4b33fc38cd9734feef3168bb2eb1e057ccfc3b0694',
    2121453182031057242: 'd5e2185d456240cbb027e1848b819982d2701850f6d1a697e43af647e7bf9a4d',
    2121453182031057240: '40133da15717ec41a5c878a2d6a706f84d48bb0cdcf6d32929afe99c6fa64052',
    2121453182031057202: 'f8d159185b4c635afc63aa27d2132f68a96ead1bcb91a27992eb51cbf75bad93',
    2121453182031057203: 'f2069351cfc6909783e41e4541a6cb60ea78ced8e169ec436953e8fda5f0d379',
    2121453182031057207: 'cca4f6aed2c9b7302cf451435832eeef224727128c83e02d33abc80a7f08e751',
    2121453182031057205: 'e0809df3cde8ba34ce9a7a0536cf80e164795f21bf270f89c0162fa4d5c13eba',
    2121453182031057208: '477ac53733821659c207f027191a0407fd6800742ff0ef7b5b849c78dd04797f',
    2121453182031057209: '9717b8490d15260a56981990f7892ee16db68f00f03310634cfc276be68af393',
    2121453182031057210: 'd7eaad54a5330f991eff0505b65c4b38b75c8d5f968db2f885caf080dfb8fd2d',
    2121453182031057226: 'ba3a433bf9fa9306bec74852478a374d36237a94735d713df797317b14ce5f1a',
}
TABLE = 'mes_pro_route_version'


def require(ok, message):
    if not ok:
        raise ValueError(message)


def canonical(value):
    """MES_ROUTE_SNAPSHOT_CANONICAL_V1: retain integers and decimal precision."""
    if isinstance(value, dict):
        return '{' + ','.join(canonical(k) + ':' + canonical(value[k]) for k in sorted(value)) + '}'
    if isinstance(value, list):
        return '[' + ','.join(map(canonical, value)) + ']'
    if isinstance(value, Decimal):
        return '0' if value == 0 else format(value, 'f').rstrip('0').rstrip('.') if '.' in format(value, 'f') else format(value, 'f')
    return json.dumps(value, ensure_ascii=False, separators=(',', ':'), allow_nan=False)


def sha(text):
    return hashlib.sha256(text.encode('utf8')).hexdigest()


def mysql(sql):
    result = subprocess.run([
        'docker', 'exec', '-i', 'int-ruoyi-mysql', 'sh', '-c',
        'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot --batch --raw '
        '--skip-column-names --default-character-set=utf8mb4 --database=ruoyi-vue-pro',
    ], input=sql, text=True, encoding='utf8', capture_output=True, timeout=60)
    require(result.returncode == 0, result.stderr)
    return result.stdout.strip()


def rows(table, where):
    columns = mysql("SELECT CONCAT(column_name,':',data_type) FROM information_schema.columns WHERE "
                    f"table_schema=DATABASE() AND table_name='{table}' ORDER BY ordinal_position").splitlines()
    require(columns, 'missing table ' + table)
    fields = ','.join(f"'{name}'," + (f'CAST(`{name}` AS UNSIGNED)' if kind == 'bit' else f'`{name}`')
                      for name, kind in (c.split(':') for c in columns))
    return [json.loads(line) for line in mysql(
        f'SELECT JSON_OBJECT({fields}) FROM `{table}` WHERE {where} ORDER BY id').splitlines()]


def correct(row, processes):
    require(row['id'] in PINS and row['tenant_id'] == 186 and row['deleted'] == 0
            and row['lifecycle_status'] == 'ACTIVE' and row['active'] == 1, 'unexpected version identity')
    require(row['route_snapshot_format_version'] == 'MES_ROUTE_SNAPSHOT_CANONICAL_V1', 'unexpected format')
    original = json.loads(row['route_snapshot_json'], parse_float=Decimal)
    require(sha(canonical(original)) == row['route_snapshot_sha256'] == PINS[row['id']], 'snapshot hash changed')
    fixed = copy.deepcopy(original)
    nodes = fixed['configSnapshots']['flowGraph']['nodes']
    require(nodes, 'missing process graph')
    for node in nodes:
        require('sopFileUrl' not in node, 'expected exactly the diagnosed missing-field shape')
        process = processes[node['processId']]
        require(process['tenant_id'] == 186 and process['deleted'] == 0
                and process['sop_file_url'] is None and process['update_time'] <= row['create_time']
                and process['create_time'] <= row['create_time'], 'unproven historical unbound SOP')
        node['sopFileUrl'] = None
    return canonical(fixed), len(nodes)


def literal(text):
    return 'CONVERT(0x' + text.encode('utf8').hex() + ' USING utf8mb4)'


def protected_rows():
    tables = ['mes_pro_work_order', 'mes_pro_process_pool_active_order',
              'mes_pro_process_pool_active_order_process_snapshot',
              'mes_pqc_inspection_task', 'mes_pro_edhr_nonconformance_review',
              'mes_pro_edhr_batch_execution']
    result = {}
    for table in tables:
        values = rows(table, 'tenant_id=186')
        result[table] = {'count': len(values), 'sha256': sha(canonical(values))}
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--apply', action='store_true')
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=False)
    versions = rows(TABLE, 'id IN (' + ','.join(map(str, PINS)) + ')')
    require(len(versions) == len(PINS), 'missing pinned versions')
    process_ids = sorted({n['processId'] for v in versions for n in json.loads(
        v['route_snapshot_json'])['configSnapshots']['flowGraph']['nodes']})
    processes = rows('mes_pro_process', 'id IN (' + ','.join(map(str, process_ids)) + ')')
    by_id = {p['id']: p for p in processes}
    before = protected_rows()
    (args.output / 'original.json').write_text(json.dumps(
        {'versions': versions, 'processes': processes, 'protectedRows': before},
        ensure_ascii=False, indent=2), encoding='utf8')
    statements = ['START TRANSACTION;',
                  'CREATE TEMPORARY TABLE sop_repair_guard (ok INT NOT NULL CHECK(ok=1));',
                  'SELECT id FROM mes_pro_route_version WHERE id IN (' + ','.join(map(str, PINS)) + ') FOR UPDATE;',
                  'SELECT id FROM mes_pro_process WHERE id IN (' + ','.join(map(str, process_ids)) + ') FOR UPDATE;']
    # A failing guard terminates mysql; disconnect rolls back the transaction.
    for p in processes:
        statements.append('INSERT INTO sop_repair_guard SELECT COUNT(*) FROM mes_pro_process '
                          f"WHERE id={p['id']} AND tenant_id=186 AND deleted=0 AND sop_file_url IS NULL "
                          f"AND update_time={literal(p['update_time'])};")
    summary = []
    for version in versions:
        fixed, count = correct(version, by_id)
        digest = sha(fixed)
        guard = (f"id={version['id']} AND tenant_id=186 AND deleted=0 AND active=1 "
                 "AND lifecycle_status='ACTIVE' "
                 f"AND route_snapshot_sha256='{PINS[version['id']]}' "
                 f"AND SHA2(route_snapshot_json,256)='{sha(version['route_snapshot_json'])}' "
                 f"AND update_time={literal(version['update_time'])}")
        statements += [f'INSERT INTO sop_repair_guard SELECT COUNT(*) FROM {TABLE} WHERE {guard};',
                       f"UPDATE {TABLE} SET route_snapshot_json={literal(fixed)}, "
                       f"route_snapshot_sha256='{digest}',update_time=CURRENT_TIMESTAMP WHERE {guard};",
                       'INSERT INTO sop_repair_guard VALUES (ROW_COUNT());']
        summary.append({'id': version['id'], 'nodes': count, 'beforeHash': PINS[version['id']], 'afterHash': digest})
    statements.append('COMMIT;')
    sql = '\n'.join(statements)
    (args.output / 'repair.sql').write_text(sql, encoding='utf8')
    (args.output / 'plan.json').write_text(json.dumps(summary, indent=2), encoding='utf8')
    if args.apply:
        mysql(sql)
        after = rows(TABLE, 'id IN (' + ','.join(map(str, PINS)) + ')')
        expected = {p['id']: p['afterHash'] for p in summary}
        for row in after:
            require(row['route_snapshot_sha256'] == expected[row['id']]
                    == sha(canonical(json.loads(row['route_snapshot_json'], parse_float=Decimal))), 'readback failed')
        require(before == protected_rows(), 'protected order/QA rows changed; investigate concurrent activity')
        (args.output / 'verified.json').write_text(json.dumps(after, ensure_ascii=False, indent=2), encoding='utf8')
    print(json.dumps({'applied': args.apply, 'versions': len(summary), 'nodes': sum(p['nodes'] for p in summary),
                      'protectedTables': before}, ensure_ascii=False))


if __name__ == '__main__':
    main()
