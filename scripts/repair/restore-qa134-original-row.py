"""Restore an explicitly selected missing tenant186 signed QA row from binlog.

No approval, task or production-state transition is replayed. --apply requires
an exclusive evidence file. Any reversal requires a separately reviewed maintenance
operation: retained closed rows are protected by database triggers and must not be
deleted automatically. Original signatures and downstream tasks remain untouched.
"""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path
import struct
import zlib
import base64

spec = importlib.util.spec_from_file_location('db', Path(__file__).with_name('repair-registration-download-summary.py'))
db = importlib.util.module_from_spec(spec)
spec.loader.exec_module(db)
TABLE = 'mes_pro_edhr_nonconformance_review'
ALLOWED_IDS = set(range(109,118)) | set(range(122,135))


def original_row(evidence, review_id=134):
    if review_id not in ALLOWED_IDS:
        raise ValueError('Review is outside the explicitly audited repair scope')
    events = [e for e in evidence['matches'] if any(isinstance(v,dict) and v.get('id') == review_id for v in e['row'].values())]
    # Later maintenance INSERTs are separate events; restore the first deleted lifecycle.
    deletions = [i for i,e in enumerate(events) if e['eventType']=='DeleteRowsEvent']
    if deletions:
        events = events[:deletions[0]+1]
    if (review_id not in evidence.get('targetIds', [evidence.get('targetId')]) or evidence['table'] != TABLE or len(events) != 3
            or [e['eventType'] for e in events] != ['WriteRowsEvent', 'UpdateRowsEvent', 'DeleteRowsEvent']):
        raise ValueError('Expected exact original creation, disposition and deletion chain')
    for e in events:
        raw = base64.b64decode(e['rawEventBase64'], validate=True)
        if (zlib.crc32(raw[:-4]) != struct.unpack('<I', raw[-4:])[0]
                or struct.unpack_from('<I', raw, 13)[0] != e['nextPosition']):
            raise ValueError('Original event integrity/position mismatch')
    row = events[2]['row']['values']
    if (events[0]['row']['values'] != events[1]['row']['before_values']
            or events[1]['row']['after_values'] != row or set(evidence['columnNames']) != set(row)
            or row['id'] != review_id or row['tenant_id'] != 186 or row['review_status'] != 'closed'
            or row['disposition'] not in ('void','rework','concession_release') or row['deleted'] != '0'
            or not row['active_order_id'] or not row['closed_at']):
        raise ValueError('Original full-row chain or selected identity conflicts')
    trace = json.loads(row['trace_snapshot_json'])
    signed = trace['qaSignatureSnapshotJson']
    fields = dict(reviewId='id', reviewCode='review_code', sourceType='source_type', sourceId='source_id',
                  batchExecutionId='batch_execution_id', workOrderId='work_order_id', disposition='disposition',
                  reviewMaterialUrl='review_material_url', reviewMaterialFileId='review_material_file_id',
                  reviewMaterialsJson='review_materials_json', reviewOpinion='review_opinion', qaUserId='qa_user_id')
    payload = {k: row[v] for k, v in fields.items() if row[v] is not None}
    digest = hashlib.sha256(json.dumps(payload, ensure_ascii=False, separators=(',', ':')).encode()).hexdigest()
    if (digest != signed['aggregateHash'] or not signed['signatureId'] or signed['reviewId'] != review_id
            or signed['qaUserId'] != row['qa_user_id'] or signed['disposition'] != row['disposition']):
        raise ValueError('Restored disposition does not match original signed aggregate')
    return row


def normalize(row, columns):
    result = {}
    for col in columns:
        key = col['Field']
        value = row[key]
        if value is not None:
            if col['Type'].startswith('bit'):
                value = str(int.from_bytes(value, 'big')) if isinstance(value, bytes) else str(value)
            elif col['Type'].startswith(('datetime', 'timestamp')):
                value = str(value)
        result[key] = value
    return result


def repair(c, source, *, apply=False, evidence=None, review_id=134):
    original = original_row(json.loads(source.read_text(encoding='utf-8')), review_id)
    with c.cursor() as q:
        if not apply:
            q.execute('SET TRANSACTION READ ONLY')
        q.execute('START TRANSACTION')
        lock = ' FOR UPDATE' if apply else ''
        q.execute('SHOW COLUMNS FROM ' + TABLE)
        columns = list(q.fetchall())
        if set(original) != {col['Field'] for col in columns}:
            raise ValueError('Current schema differs from original full row')
        q.execute('SELECT * FROM ' + TABLE + ' WHERE id=%s' + lock, (review_id,))
        existing = q.fetchone()
        if existing and normalize(existing, columns) != original:
            raise ValueError('Existing QA134 conflicts; refusing overwrite')
        q.execute("SELECT * FROM mes_active_order_handoff_task WHERE tenant_id=186 AND source_type='NONCONFORMANCE_REVIEW' AND source_id=%s AND task_type='QA_REVIEW' AND deleted=0" + lock, (review_id,))
        tasks = q.fetchall()
        task = tasks[0] if len(tasks)==1 else None
        if (not task or task['tenant_id'] != 186 or task['task_type'] != 'QA_REVIEW'
                or task['source_id'] != review_id or task['completion_source_id'] != review_id or task['status'] != 'DONE'
                or task['completed_by'] != original['qa_user_id'] or task['active_order_id'] != original['active_order_id']
                or task['work_order_id'] != original['work_order_id']):
            raise ValueError('Exact completed handoff lineage is missing or conflicts')
        signed = json.loads(original['trace_snapshot_json'])['qaSignatureSnapshotJson']
        q.execute('SELECT * FROM system_electronic_signature WHERE id=%s' + lock, (signed['signatureId'],))
        sig = q.fetchone()
        if (not sig or sig['tenant_id'] != 186 or sig['actor_id'] != original['qa_user_id']
                or sig['verification_status'] != 'VALID' or sig['module_code'] != 'MES'
                or sig['action_code'] != 'QA_DISPOSITION' or sig['reason'] != original['review_opinion']
                or signed['aggregateHash'] not in base64.urlsafe_b64decode(sig['subject_id'] + '=' * (-len(sig['subject_id']) % 4)).decode()
                or json.loads(sig['canonical_content_json']).get('cellValuesHash') != signed['aggregateHash']):
            raise ValueError('Original electronic signature reference is missing or conflicts')
        canonical = json.dumps(json.loads(sig['canonical_content_json']), sort_keys=True, ensure_ascii=False, separators=(',', ':'))
        if hashlib.sha256(canonical.encode()).hexdigest() != sig['content_hash']:
            raise ValueError('Original signature canonical content hash mismatch')
        if (sig['evidence_format_version'] is not None or sig['command_hash_version'] is not None
                or sig['algorithm']!='SHA-256' or sig['key_version']!='system-local-v1'
                or not sig['time_evidence_id'].startswith('SERVER_CLOCK:')):
            raise ValueError('Unsupported original signature evidence protocol')
        canonical_optional = lambda value: '' if value is None else json.dumps(json.loads(value), sort_keys=True, ensure_ascii=False, separators=(',', ':'))
        parts = [str(sig[k]) for k in ('tenant_id','actor_id','module_code','action_code','subject_type','subject_id','subject_version','meaning_code','meaning_label','reason')]
        parts += [sig['time_evidence_id'].split(':',1)[1],sig['time_evidence_id'],sig['authentication_method'],sig['content_hash'],sig['before_content_hash'] or '',sig['after_content_hash'] or '']
        parts += [canonical_optional(sig[k]) for k in ('before_content_json','after_content_json','field_diff_json')]
        parts += ['SHA-256',sig['key_version'],sig['policy_version'],sig['verification_status']]
        if hashlib.sha256('|'.join(parts).encode()).hexdigest()!=sig['evidence_hash']:
            raise ValueError('Original signature full evidence hash mismatch')
        plan = dict(tenantId=186, reviewId=review_id, taskId=task['id'], needsInsert=not bool(existing),
                    sourceSha256=hashlib.sha256(source.read_bytes()).hexdigest(), restoredRow=original,
                    originalSignature=sig, originalHandoff=task)
        if not apply or existing:
            c.rollback()
            return plan
        if evidence is None:
            raise ValueError('Exclusive evidence file is required')
        plan['status'] = 'prepared'
        with evidence.open('x', encoding='utf-8') as f:
            json.dump(plan, f, ensure_ascii=False, indent=2, default=str)
        names = [col['Field'] for col in columns]
        values = [int(original[k]) if original[k] is not None and col['Type'].startswith('bit') else original[k]
                  for k, col in zip(names, columns)]
        q.execute('INSERT INTO ' + TABLE + ' (' + ','.join('`' + k + '`' for k in names) + ') VALUES ('
                  + ','.join(['%s'] * len(names)) + ')', values)
        if q.rowcount != 1:
            raise ValueError('Exact original row insert failed')
        q.execute('SELECT * FROM ' + TABLE + ' WHERE id=%s', (review_id,))
        if normalize(q.fetchone(), columns) != original:
            raise ValueError('Restored full row differs from original deleted image')
        c.commit()
        plan['status'] = 'committed'
        evidence.write_text(json.dumps(plan, ensure_ascii=False, indent=2, default=str), encoding='utf-8')
        return plan


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--source', type=Path, required=True)
    p.add_argument('--review-id', type=int, default=134)
    p.add_argument('--apply', action='store_true')
    p.add_argument('--evidence', type=Path)
    args = p.parse_args()
    c = db.connect()
    try:
        plan = repair(c, args.source, apply=args.apply, evidence=args.evidence, review_id=args.review_id)
        print(json.dumps({k: plan[k] for k in ('tenantId','reviewId','taskId','needsInsert','sourceSha256')}))
    finally:
        c.rollback()
        c.close()


if __name__ == '__main__':
    main()
