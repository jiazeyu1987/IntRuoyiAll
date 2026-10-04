"""Offline metadata proposal only. No database/client/network/write-back capability."""
from __future__ import annotations

import argparse
import copy
import gzip
import hashlib
import json
from pathlib import Path
import re

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
SCHEMA = Path('C:/IntRuoyiBackups/20261003-dcc-integration/dcc-schema.sql.gz')
SCHEMA_SHA = 'cf6a94375b95720637d7b75a471e508420ffa9cc6b399c4438851237d4fc66fa'
KINDS = ('claim', 'master', 'version', 'storage', 'ownership', 'ticket', 'reference')
MAX_LONG = 9223372036854775807


def require(condition, reason):
    if not condition:
        raise ValueError(reason)


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def write_json(path, value):
    Path(path).write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def literal(value):
    return "'" + value.replace("'", "''") + "'"


def make_queries():
    claims = 'SELECT c.id FROM dcc_controlled_file_name_claim c WHERE c.tenant_id=1 AND c.deleted=0'
    masters = 'SELECT c.master_id FROM dcc_controlled_file_name_claim c WHERE c.tenant_id=1 AND c.deleted=0'
    versions = 'SELECT v.id FROM dcc_controlled_file v WHERE v.master_id IN (' + masters + ')'
    sources = 'SELECT v.source_file_id FROM dcc_controlled_file v WHERE v.id IN (' + versions + ')'
    origins = 'SELECT o.origin_source_file_id FROM dcc_controlled_file_source_ownership o WHERE o.controlled_file_id IN (' + versions + ') OR o.source_file_id IN (' + sources + ')'
    definitions = {
        'claim': ('dcc_controlled_file_name_claim', 'c', 'c.id IN (' + claims + ')', {
            'id': 'id', 'tenantId': 'tenant_id', 'masterId': 'master_id', 'normalizedName': 'normalized_name', 'nameHex': '@HEX(c.normalized_name)', 'deleted': 'deleted', 'activeFlag': 'active_unique_flag'}),
        'master': ('dcc_controlled_file_master', 'm', 'm.id IN (' + masters + ')', {
            'id': 'id', 'tenantId': 'tenant_id', 'fileName': 'file_name', 'projectId': 'dcc_project_code_id', 'leafId': 'file_type_taxonomy_leaf_id', 'normalizedNumber': 'normalized_file_number', 'fileNumber': 'file_number', 'currentActiveFileId': 'current_active_controlled_file_id', 'status': 'status', 'deleted': 'deleted'}),
        'version': ('dcc_controlled_file', 'v', 'v.id IN (' + versions + ')', {
            'id': 'id', 'tenantId': 'tenant_id', 'masterId': 'master_id', 'sourceFileId': 'source_file_id', 'originalFileId': 'original_file_id', 'sourceSha256': 'source_sha256', 'projectId': 'dcc_project_code_id', 'leafId': 'file_type_taxonomy_id', 'fileNumber': 'file_number', 'versionNo': 'version_no', 'status': 'status', 'processInstanceId': 'process_instance_id', 'predecessorId': 'predecessor_controlled_file_id', 'deleted': 'deleted'}),
        'storage': ('infra_file', 'f', 'f.id IN (' + sources + ') OR f.id IN (' + origins + ')', {
            'id': 'id', 'configId': 'config_id', 'name': 'name', 'nameHex': '@HEX(f.name)', 'mime': 'type', 'size': 'size', 'deleted': 'deleted'}),
        'ownership': ('dcc_controlled_file_source_ownership', 'o', 'o.controlled_file_id IN (' + versions + ') OR o.source_file_id IN (' + sources + ')', {
            'id': 'id', 'tenantId': 'tenant_id', 'controlledFileId': 'controlled_file_id', 'sourceFileId': 'source_file_id', 'originSourceFileId': 'origin_source_file_id', 'sourceSha256': 'source_sha256', 'ownershipType': 'ownership_type', 'deleted': 'deleted'}),
        'ticket': ('dcc_controlled_file_temporary_file', 't', 't.bound_controlled_file_id IN (' + versions + ') OR t.storage_file_id IN (' + sources + ') OR t.storage_file_id IN (' + origins + ')', {
            'id': 'id', 'tenantId': 'tenant_id', 'controlledFileId': 'bound_controlled_file_id', 'storageFileId': 'storage_file_id', 'originalName': 'original_file_name', 'nameHex': '@HEX(t.original_file_name)', 'fileSha256': 'file_sha256', 'size': 'file_size', 'purpose': 'purpose', 'status': 'status', 'cleanupStatus': 'cleanup_status', 'deleted': 'deleted'}),
        'reference': ('dcc_controlled_file', 'r', 'r.source_file_id IN (' + sources + ')', {
            'id': 'id', 'tenantId': 'tenant_id', 'masterId': 'master_id', 'sourceFileId': 'source_file_id', 'sourceSha256': 'source_sha256', 'deleted': 'deleted'})}
    parts = []
    counts = []
    required_columns = {}
    fields = {}
    for kind, (table, alias, where, mapping) in definitions.items():
        fields[kind] = list(mapping)
        expressions = []
        for key, column in mapping.items():
            if column.startswith('@'):
                expr = column[1:]
            else:
                required_columns.setdefault(table, set()).add(column)
                expr = alias + '.' + column
                if key == 'id' or key.endswith('Id') or key == 'size':
                    expr = 'CAST(' + expr + ' AS CHAR)'
                elif key in ('deleted', 'activeFlag'):
                    expr = '(' + expr + '+0)'
            expressions.extend((literal(key), expr))
        relation = table + ' ' + alias + ' WHERE ' + where
        parts.append("SELECT " + literal(kind) + " AS fact_kind,CAST(" + alias + ".id AS CHAR) AS sort_id,JSON_OBJECT('kind'," + literal(kind) + ',' + ','.join(expressions) + ') AS fact FROM ' + relation)
        counts.extend((literal(kind), '(SELECT COUNT(*) FROM ' + relation + ')'))
    runtime = "SELECT 'runtime' AS fact_kind,'' AS sort_id,JSON_OBJECT('kind','runtime','database',DATABASE(),'serverUuid',@@server_uuid,'mysqlVersion',@@version,'capturedAt',CAST(UTC_TIMESTAMP(6) AS CHAR),'tenantId','1','activeClaimCount',(" + claims.replace('SELECT c.id', 'SELECT COUNT(*)') + "),'counts',JSON_OBJECT(" + ','.join(counts) + ')) AS fact'
    sql = 'SELECT fact FROM (\n' + '\nUNION ALL\n'.join([runtime] + parts) + '\n) captured_facts ORDER BY fact_kind,sort_id;\n'
    return sql, {table: sorted(cols) for table, cols in required_columns.items()}, fields


def prepare():
    require(sha(SCHEMA) == SCHEMA_SHA, 'Protected schema checksum changed')
    schema = gzip.open(SCHEMA, 'rt', encoding='utf-8').read()
    sql, columns, fields = make_queries()
    shapes = {}
    for table, names in columns.items():
        match = re.search(r'CREATE TABLE `' + table + r'` \((.*?)\n\)', schema, re.S)
        require(match is not None, 'Required actual table absent: ' + table)
        shapes[table] = {}
        for name in names:
            declaration = re.search(r'^\s*`' + name + r'`\s+(.+)$', match[1], re.M)
            require(declaration is not None, 'Required actual column absent: ' + table + '.' + name)
            shapes[table][name] = declaration[1].split(' COMMENT ')[0].rstrip(',')
    contract = {'status': 'PREPARED_READ_ONLY_METADATA_PROPOSAL', 'database': 'ruoyi-vue-pro',
                'serverUuid': '92ca05d0-aec8-11f1-a944-02b4e226a5ef', 'mysqlVersion': '8.0.40', 'tenantId': '1', 'expectedActiveClaimCount': 25,
                'protectedSchemaSha256': SCHEMA_SHA,
                'fields': fields, 'requiredExistingColumns': shapes, 'infraFileTenantFieldExists': False,
                'infraFileBodyHashFieldExists': False, 'source_bytes_verified': False, 'write_authorized': False,
                'schemaSemantics': 'Query only pre-existing source columns; no reference to C newly-added names. Single SELECT snapshot, all scoped versions including deleted.'}
    (HERE / 'g23-legacy-name-mapping.sql').write_text(sql, encoding='utf-8')
    contract['querySha256'] = sha(HERE / 'g23-legacy-name-mapping.sql')
    write_json(HERE / 'g23-legacy-name-mapping-contract.json', contract)
    return contract


def exact_id(value, nullable=False):
    if value is None and nullable:
        return
    require(isinstance(value, str) and re.fullmatch(r'[1-9][0-9]*', value) and int(value) <= MAX_LONG, 'Identity must be exact positive signed Long string')


def valid_hash(value):
    return isinstance(value, str) and re.fullmatch(r'[0-9a-fA-F]{64}', value) is not None


def valid_name(value):
    return isinstance(value, str) and bool(value.strip()) and len(value) <= 256 and not any(c in value for c in ('/', '\\', '\x00'))


def validate(contract, rows):
    result = {'status': 'INVALID_CAPTURE', 'source_bytes_verified': False, 'write_authorized': False, 'mappings': [], 'errors': [],
              'meaning': 'Read-only exact metadata proposal, never approval to populate names or proof of current object bytes.'}
    try:
        require(isinstance(rows, list), 'Expected JSONL rows')
        runtime = [row for row in rows if isinstance(row, dict) and row.get('kind') == 'runtime']
        require(len(runtime) == 1, 'One runtime envelope required')
        envelope = runtime[0]
        for key in ('database', 'serverUuid', 'mysqlVersion', 'tenantId'):
            require(envelope.get(key) == contract[key], 'Unexpected capture ' + key)
        require(envelope.get('activeClaimCount') == contract['expectedActiveClaimCount'], 'Fresh active claim count changed; no truncation permitted')
        groups = {kind: {} for kind in KINDS}
        for row in rows:
            require(isinstance(row, dict) and row.get('kind') in (*KINDS, 'runtime'), 'Unknown fact row')
            kind = row['kind']
            if kind == 'runtime':
                continue
            require(set(row) == set(contract['fields'][kind]) | {'kind'}, 'Missing/extra fact fields: ' + kind)
            exact_id(row['id'])
            require(row['id'] not in groups[kind], 'Duplicate fact identity: ' + kind)
            for key, value in row.items():
                if key.endswith('Id') and key != 'processInstanceId':
                    exact_id(value, nullable=True)
            require(type(row['deleted']) is int and row['deleted'] in (0, 1), 'Invalid deleted flag')
            if kind in ('claim', 'storage', 'ticket'):
                name_key = {'claim': 'normalizedName', 'storage': 'name', 'ticket': 'originalName'}[kind]
                name = row[name_key]
                if name is not None:
                    require(isinstance(name, str) and row['nameHex'] == name.encode('utf-8').hex().upper(), 'Exact name UTF-8 bytes mismatch')
            if kind == 'claim':
                require(row['tenantId'] == contract['tenantId'] and row['deleted'] == 0 and row['activeFlag'] == 1, 'Claim is outside exact active tenant scope')
            groups[kind][row['id']] = row
        require(set(envelope.get('counts', {})) == set(KINDS), 'Missing exact row counts')
        for kind in KINDS:
            require(type(envelope['counts'][kind]) is int and envelope['counts'][kind] == len(groups[kind]), 'Truncated/extra fact group: ' + kind)
        require(len(groups['claim']) == contract['expectedActiveClaimCount'], 'Claim row count changed')
        claims = sorted(groups['claim'].values(), key=lambda x: int(x['id']))
        for claim in claims:
            reasons = set()
            master = groups['master'].get(claim['masterId'])
            versions = sorted([v for v in groups['version'].values() if v['masterId'] == claim['masterId']], key=lambda x: int(x['id']))
            observations = []
            if master is None:
                reasons.add('MASTER_MISSING')
            elif master['tenantId'] != claim['tenantId'] or master['deleted'] != 0:
                reasons.add('MASTER_TENANT_OR_DELETED_MISMATCH')
            if not versions:
                reasons.add('VERSION_CHAIN_MISSING')
            if master and master['currentActiveFileId'] is not None and master['currentActiveFileId'] not in {v['id'] for v in versions if not v['deleted']}:
                reasons.add('MASTER_ACTIVE_POINTER_NOT_IN_CHAIN')
            labels = [v['versionNo'] for v in versions]
            if len(labels) != len(set(labels)):
                reasons.add('DUPLICATE_VERSION_LABEL')
            if sum(c['masterId'] == claim['masterId'] for c in claims) != 1:
                reasons.add('MULTIPLE_ACTIVE_CLAIMS_FOR_MASTER')
            for version in versions:
                vr = set()
                if version['tenantId'] != claim['tenantId']:
                    vr.add('VERSION_TENANT_MISMATCH')
                if not isinstance(version['versionNo'], str) or not version['versionNo'].strip():
                    vr.add('VERSION_LABEL_MISSING')
                if version['deleted']:
                    vr.add('DELETED_VERSION_REQUIRES_REVIEW')
                if not valid_hash(version['sourceSha256']):
                    vr.add('SOURCE_SHA256_MISSING_OR_INVALID')
                if version['sourceFileId'] is None:
                    vr.add('SOURCE_FILE_ID_MISSING')
                storage = groups['storage'].get(version['sourceFileId'])
                if storage is None:
                    vr.add('SOURCE_METADATA_MISSING')
                else:
                    if storage['deleted'] or storage['configId'] is None:
                        vr.add('SOURCE_METADATA_DELETED_OR_UNCONFIGURED')
                    if not valid_name(storage['name']):
                        vr.add('SOURCE_NAME_MISSING_OR_INVALID')
                    if not isinstance(storage['size'], str) or not re.fullmatch(r'[0-9]+', storage['size']):
                        vr.add('SOURCE_SIZE_INVALID')
                owned = [o for o in groups['ownership'].values() if o['controlledFileId'] == version['id'] and not o['deleted']]
                if len(owned) > 1:
                    vr.add('DUPLICATE_SOURCE_OWNERSHIP')
                for owner in owned:
                    if owner['tenantId'] != claim['tenantId'] or owner['sourceFileId'] != version['sourceFileId']:
                        vr.add('SOURCE_OWNERSHIP_IDENTITY_MISMATCH')
                    if not valid_hash(owner['sourceSha256']) or not valid_hash(version['sourceSha256']) or owner['sourceSha256'].lower() != version['sourceSha256'].lower():
                        vr.add('SOURCE_OWNERSHIP_SHA256_MISMATCH')
                    origin = groups['storage'].get(owner['originSourceFileId'])
                    if origin is None or origin['deleted'] or not valid_name(origin['name']):
                        vr.add('ORIGIN_SOURCE_METADATA_MISSING_OR_DELETED')
                    elif storage and (origin['nameHex'] != storage['nameHex'] or origin['size'] != storage['size']):
                        vr.add('ORIGIN_SOURCE_NAME_OR_SIZE_MISMATCH')
                if any(o['sourceFileId'] == version['sourceFileId'] and not o['deleted'] and (o['tenantId'] != claim['tenantId'] or o['controlledFileId'] != version['id']) for o in groups['ownership'].values()):
                    vr.add('SOURCE_OWNED_BY_OTHER_VERSION_OR_TENANT')
                related_ids = {version['sourceFileId']} | {o['originSourceFileId'] for o in owned}
                tickets = [t for t in groups['ticket'].values() if not t['deleted'] and t['purpose'] == 'SOURCE' and (t['controlledFileId'] == version['id'] or t['storageFileId'] in related_ids)]
                for ticket in tickets:
                    if ticket['tenantId'] != claim['tenantId'] or ticket['controlledFileId'] != version['id'] or ticket['storageFileId'] not in related_ids or ticket['status'] != 'BOUND' or ticket['cleanupStatus'] != 'BOUND':
                        vr.add('SOURCE_TICKET_BINDING_MISMATCH')
                    if not storage or ticket['nameHex'] != storage['nameHex'] or ticket['size'] != storage['size']:
                        vr.add('SOURCE_TICKET_NAME_OR_SIZE_MISMATCH')
                    if not valid_hash(ticket['fileSha256']) or not valid_hash(version['sourceSha256']) or ticket['fileSha256'].lower() != version['sourceSha256'].lower():
                        vr.add('SOURCE_TICKET_SHA256_MISMATCH')
                refs = [r for r in groups['reference'].values() if r['sourceFileId'] == version['sourceFileId']]
                if version['sourceFileId'] is not None and not any(r['id'] == version['id'] for r in refs):
                    vr.add('SOURCE_SELF_REFERENCE_MISSING')
                for reference in refs:
                    if reference['tenantId'] != claim['tenantId'] or reference['masterId'] != claim['masterId']:
                        vr.add('SOURCE_SHARED_WITH_FOREIGN_CHAIN_OR_TENANT')
                    if reference['sourceSha256'] != version['sourceSha256']:
                        vr.add('SAME_SOURCE_REFERENCE_SHA256_MISMATCH')
                reasons |= vr
                observations.append({'controlledFileId': version['id'], 'versionNo': version['versionNo'], 'processInstanceId': version['processInstanceId'], 'status': version['status'], 'deleted': version['deleted'],
                                     'sourceFileId': version['sourceFileId'], 'originalFileIdDiagnosticOnly': version['originalFileId'], 'sourceName': storage['name'] if storage else None,
                                     'sourceNameHex': storage['nameHex'] if storage else None, 'sourceSha256': version['sourceSha256'], 'ownershipIds': [o['id'] for o in owned], 'ticketEvidenceIds': [t['id'] for t in tickets], 'reasons': sorted(vr)})
            names = {o['sourceNameHex']: o['sourceName'] for o in observations if valid_name(o['sourceName'])}
            if len(names) != 1:
                reasons.add('MULTIPLE_EXACT_SOURCE_NAMES' if len(names) > 1 else 'EXACT_SOURCE_NAME_NOT_ESTABLISHED')
            formal = []
            if master:
                if master['projectId'] is None or master['leafId'] is None or not master['normalizedNumber']:
                    formal.append('MASTER_FORMAL_NUMBER_IDENTITY_INCOMPLETE')
                if any(v['projectId'] != master['projectId'] or v['leafId'] != master['leafId'] or v['fileNumber'] != master['normalizedNumber'] for v in versions):
                    formal.append('VERSION_FORMAL_NUMBER_IDENTITY_DIFFERS')
            result['mappings'].append({'claimId': claim['id'], 'tenantId': claim['tenantId'], 'masterId': claim['masterId'], 'status': 'UNCONFIRMED' if reasons else 'PROPOSED_VERIFIED_METADATA_MAPPING',
                                       'proposedOriginalName': next(iter(names.values())) if not reasons else None, 'proposedOriginalNameHex': next(iter(names)) if not reasons else None,
                                       'source_bytes_verified': False, 'write_authorized': False, 'reasons': sorted(reasons), 'formalIdentityReview': formal, 'versions': observations})
        by_name = {}
        for mapping in result['mappings']:
            for name in {v['sourceNameHex'] for v in mapping['versions'] if valid_name(v['sourceName'])}:
                by_name.setdefault(name, []).append(mapping)
        for mappings in by_name.values():
            if len(mappings) > 1:
                for mapping in mappings:
                    mapping['status'] = 'UNCONFIRMED'
                    if 'EXACT_SOURCE_NAME_COLLISION_BETWEEN_CLAIMS' not in mapping['reasons']:
                        mapping['reasons'].append('EXACT_SOURCE_NAME_COLLISION_BETWEEN_CLAIMS')
                    mapping['proposedOriginalName'] = None; mapping['proposedOriginalNameHex'] = None
        result.update(status='UNCONFIRMED' if any(x['status'] == 'UNCONFIRMED' for x in result['mappings']) else 'PROPOSED_VERIFIED_METADATA_MAPPING',
                      database=envelope['database'], serverUuid=envelope['serverUuid'], capturedAt=envelope['capturedAt'], activeClaimCount=len(claims),
                      captureSha256=hashlib.sha256(json.dumps(rows, sort_keys=True, ensure_ascii=False, separators=(',', ':')).encode()).hexdigest())
    except (ValueError, KeyError, TypeError, UnicodeError) as exc:
        result['status'] = 'INVALID_CAPTURE'; result['errors'].append(str(exc))
    return result


def refresh_fixture_counts(rows):
    envelope = next(row for row in rows if row['kind'] == 'runtime')
    envelope['counts'] = {kind: sum(row['kind'] == kind for row in rows) for kind in KINDS}
    envelope['activeClaimCount'] = envelope['counts']['claim']


def offline_fixture(contract):
    rows = [{'kind': 'runtime', **{key: contract[key] for key in ('database', 'serverUuid', 'mysqlVersion', 'tenantId')}, 'capturedAt': 'OFFLINE_UNIT_FIXTURE_NOT_MYSQL'}]
    for n in range(25):
        cid, mid, vid, fid = [str(9223372036854700000 + n * 10 + offset) for offset in range(4)]
        name = 'Source ' + str(n) + '.DOCX '; hx = name.encode().hex().upper(); digest = 'a' * 64
        rows.extend([
            {'kind': 'claim', 'id': cid, 'tenantId': '1', 'masterId': mid, 'normalizedName': name, 'nameHex': hx, 'deleted': 0, 'activeFlag': 1},
            {'kind': 'master', 'id': mid, 'tenantId': '1', 'fileName': 'template', 'projectId': '12', 'leafId': '13', 'normalizedNumber': 'DCC-'+str(n), 'fileNumber': 'DCC-'+str(n), 'currentActiveFileId': vid, 'status': 'ACTIVE', 'deleted': 0},
            {'kind': 'version', 'id': vid, 'tenantId': '1', 'masterId': mid, 'sourceFileId': fid, 'originalFileId': '999', 'sourceSha256': digest, 'projectId': '12', 'leafId': '13', 'fileNumber': 'DCC-'+str(n), 'versionNo': 'A/1', 'status': 'ACTIVE', 'processInstanceId': 'exact-bpm-'+str(n), 'predecessorId': None, 'deleted': 0},
            {'kind': 'storage', 'id': fid, 'configId': '15', 'name': name, 'nameHex': hx, 'mime': 'application/octet-stream', 'size': '100', 'deleted': 0},
            {'kind': 'ownership', 'id': str(n+1), 'tenantId': '1', 'controlledFileId': vid, 'sourceFileId': fid, 'originSourceFileId': fid, 'sourceSha256': digest, 'ownershipType': 'NEW', 'deleted': 0},
            {'kind': 'ticket', 'id': str(n+1), 'tenantId': '1', 'controlledFileId': vid, 'storageFileId': fid, 'originalName': name, 'nameHex': hx, 'fileSha256': digest, 'size': '100', 'purpose': 'SOURCE', 'status': 'BOUND', 'cleanupStatus': 'BOUND', 'deleted': 0},
            {'kind': 'reference', 'id': vid, 'tenantId': '1', 'masterId': mid, 'sourceFileId': fid, 'sourceSha256': digest, 'deleted': 0}])
    refresh_fixture_counts(rows)
    return rows


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest='mode', required=True)
    commands.add_parser('prepare')
    command = commands.add_parser('validate'); command.add_argument('--facts', required=True); command.add_argument('--result', required=True)
    args = parser.parse_args()
    if args.mode == 'prepare':
        contract = prepare(); print(json.dumps({'status': contract['status'], 'expectedActiveClaimCount': 25, 'write_authorized': False})); return
    try:
        contract = json.loads((HERE / 'g23-legacy-name-mapping-contract.json').read_text(encoding='utf-8'))
        require(sha(HERE / 'g23-legacy-name-mapping.sql') == contract['querySha256'], 'Prepared SELECT checksum drift')
        rows = [json.loads(line) for line in Path(args.facts).read_text(encoding='utf-8-sig').splitlines() if line.strip()]
        result = validate(contract, rows)
    except (ValueError, KeyError, TypeError, OSError) as exc:
        result = {'status': 'INVALID_CAPTURE', 'source_bytes_verified': False, 'write_authorized': False, 'errors': [str(exc)], 'mappings': []}
    write_json(args.result, result)
    counts = {status: sum(m['status'] == status for m in result['mappings']) for status in ('PROPOSED_VERIFIED_METADATA_MAPPING', 'UNCONFIRMED')}
    print(json.dumps({'status': result['status'], 'counts': counts, 'source_bytes_verified': False, 'write_authorized': False}))
    raise SystemExit(0 if result['status'] == 'PROPOSED_VERIFIED_METADATA_MAPPING' else 2 if result['status'] == 'UNCONFIRMED' else 1)


if __name__ == '__main__':
    main()
