"""Repair missing display variables of one explicitly selected FINISHED download.

No runtime fallback and no approval/state/payload changes. Defaults to read-only.
--apply requires exclusive evidence output; only absent variables are inserted.
Recovery: delete exactly the insertedVariableIds in the evidence after checking
their process, name and value still match. Never delete by request ID alone.
Database settings come from REGCERT_REPAIR_DB_{HOST,PORT,NAME,USER,PASSWORD}.
"""
import argparse
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import uuid

import pymysql


FIELDS = ('ownerCompanyName', 'certificateName')


def text(value, label):
    if not isinstance(value, str) or not value.strip() or len(value) > 4000:
        raise ValueError('Missing or invalid display fact: ' + label)
    return value.strip()


def plan_variables(submission, company, file_names, variables):
    """Use submitted facts when recorded; otherwise the explicit reference facts.

    Reference-derived names are repair-time display metadata, not a claim that
    they were in the original submitted payload. Original payload is never changed.
    """
    result = {}
    origins = {}
    for key in FIELDS:
        if key in submission:
            value = text(submission[key], key)
            origin = 'submitted detail_json'
        elif key == 'ownerCompanyName':
            value = text(company, key)
            origin = 'request owner company reference at repair time'
        else:
            if not file_names:
                raise ValueError('Requested file snapshots are missing')
            names = list(dict.fromkeys(text(name, 'requested file snapshot') for name in file_names))
            value = text('；'.join(names), key)
            origin = 'requested file version snapshots at request time'
        existing = [row for row in variables if row['NAME_'] == key]
        if len(existing) > 1:
            raise ValueError('Duplicate root variable: ' + key)
        if existing:
            if text(existing[0]['TEXT_'], key) != value:
                raise ValueError('Existing root variable conflicts with formal display fact: ' + key)
        else:
            result[key] = value
            origins[key] = origin
    return result, origins


def repair(connection, tenant_id, request_id, process_id, *, apply=False, evidence=None):
    with connection.cursor() as cur:
        if not apply:
            cur.execute('SET TRANSACTION READ ONLY')
        cur.execute('START TRANSACTION')
        lock = ' FOR UPDATE' if apply else ''
        cur.execute('SELECT * FROM dcc_registration_certificate_access_request '
                    'WHERE tenant_id=%s AND id=%s AND deleted=0' + lock, (tenant_id, request_id))
        request = cur.fetchone()
        if not request or request['request_type'] != 'DOWNLOAD_FILE' or request['bpm_process_instance_id'] != process_id:
            raise ValueError('Selected request is not this tenant/process download')
        cur.execute('SELECT p.PROC_INST_ID_,p.TENANT_ID_,p.BUSINESS_KEY_,p.END_TIME_,d.KEY_,d.TENANT_ID_ AS definitionTenant '
                    'FROM ACT_HI_PROCINST p JOIN ACT_RE_PROCDEF d ON d.ID_=p.PROC_DEF_ID_ '
                    'WHERE CAST(p.PROC_INST_ID_ AS BINARY)=CAST(%s AS BINARY)' + lock, (process_id,))
        processes = cur.fetchall()
        if len(processes) != 1:
            raise ValueError('Exact historical process is missing or ambiguous')
        process = processes[0]
        if (process['TENANT_ID_'] != str(tenant_id) or process['definitionTenant'] != str(tenant_id)
                or process['KEY_'] != 'dcc-registration-certificate-access' or not process['END_TIME_']
                or request['status'] not in ('APPROVED', 'REJECTED', 'WITHDRAWN')):
            raise ValueError('Only a finished, matching registration download can be repaired')
        cur.execute('SELECT business_key,bpm_process_instance_id FROM dcc_registration_certificate_bpm_binding '
                    'WHERE tenant_id=%s AND request_id=%s AND deleted=0' + lock, (tenant_id, request_id))
        bindings = cur.fetchall()
        if (len(bindings) != 1 or bindings[0]['business_key'] != process['BUSINESS_KEY_']
                or bindings[0]['bpm_process_instance_id'] != process_id):
            raise ValueError('Formal process binding mismatch')
        cur.execute('SELECT ID_ FROM ACT_RU_EXECUTION WHERE PROC_INST_ID_=%s' + lock, (process_id,))
        if cur.fetchone():
            raise ValueError('Process still has runtime executions; stop this repair')
        cur.execute('SELECT * FROM ACT_HI_VARINST WHERE PROC_INST_ID_=%s AND EXECUTION_ID_=%s '
                    'AND TASK_ID_ IS NULL AND NAME_ IN (%s,%s)' + lock, (process_id, process_id, *FIELDS))
        original_variables = cur.fetchall()
        submission = json.loads(request['detail_json'])
        if not isinstance(submission, dict) or submission.get('requestType') != 'DOWNLOAD_FILE':
            raise ValueError('Download submission identity is missing')
        company = None
        if 'ownerCompanyName' not in submission:
            cur.execute("SELECT name FROM mdm_enterprise WHERE tenant_id=%s AND id=%s "
                        "AND type='OWNED_COMPANY' AND deleted=0" + lock, (tenant_id, request['owner_company_id']))
            companies = cur.fetchall()
            if len(companies) != 1:
                raise ValueError('Exact owner company reference is missing')
            company = companies[0]['name']
        # Only the requested files and their exact historical version are eligible.
        # A file kind whose owner cannot be resolved explicitly fails; no current-version substitution.
        cur.execute("""
            SELECT rf.business_file_id,f.owner_type,v.id AS version_id
            FROM dcc_registration_certificate_access_request_file rf
            LEFT JOIN dcc_registration_certificate_file f
              ON f.tenant_id=rf.tenant_id AND f.id=rf.business_file_id AND f.deleted=0
            LEFT JOIN dcc_registration_certificate_version v ON v.tenant_id=f.tenant_id AND v.deleted=0
              AND v.id=CASE f.owner_type
                WHEN 'VERSION' THEN f.owner_id
                WHEN 'CHANGE' THEN (SELECT c.source_version_id FROM dcc_registration_certificate_change c
                  WHERE c.tenant_id=f.tenant_id AND c.id=f.owner_id AND c.certificate_id=%s AND c.deleted=0)
                WHEN 'SUPPORTING_DOCUMENT' THEN (SELECT sd.version_id FROM dcc_registration_certificate_supporting_document sd
                  WHERE sd.tenant_id=f.tenant_id AND sd.id=f.owner_id AND sd.business_file_id=f.id
                    AND sd.certificate_id=%s AND sd.deleted=0) END
              AND v.certificate_id=%s
            WHERE rf.tenant_id=%s AND rf.request_id=%s AND rf.deleted=0 ORDER BY rf.id
            """ + lock, (request['certificate_id'], request['certificate_id'], request['certificate_id'],
                         tenant_id, request_id))
        files = cur.fetchall()
        if (not files or any(not row['version_id'] for row in files)
                or sorted(submission.get('businessFileIds', [])) != sorted(row['business_file_id'] for row in files)):
            raise ValueError('Requested file set or historical snapshot is incomplete')
        for row in files:
            cur.execute('SELECT id AS snapshot_id,product_name FROM dcc_registration_certificate_snapshot '
                        'WHERE tenant_id=%s AND version_id=%s AND deleted=0 AND create_time<=%s '
                        'ORDER BY revision_no DESC,id DESC LIMIT 1' + lock,
                        (tenant_id, row['version_id'], request['requested_at']))
            snapshot = cur.fetchone()
            if not snapshot:
                raise ValueError('Requested file historical snapshot is missing')
            row.update(snapshot)
        values, origins = plan_variables(submission, company, [f['product_name'] for f in files], original_variables)
        plan = dict(tenantId=tenant_id, requestId=request_id, processInstanceId=process_id,
                    insertVariables=values, origins=origins, files=files)
        if not apply or not values:
            connection.rollback()
            return plan
        if evidence is None:
            raise ValueError('--evidence is required when applying')
        ids = {key: 'regsum-' + uuid.uuid4().hex for key in values}
        evidence_data = dict(plan, originalRequest=request, originalVariables=original_variables,
                             insertedVariableIds=ids, preparedAt=datetime.now(timezone.utc).isoformat(),
                             status='prepared', recovery='Delete only these exact inserted variable IDs after verifying process/name/value.')
        with evidence.open('x', encoding='utf-8') as out:
            json.dump(evidence_data, out, ensure_ascii=False, default=str, indent=2)
        for key, value in values.items():
            cur.execute('INSERT INTO ACT_HI_VARINST '
                        '(ID_,REV_,PROC_INST_ID_,EXECUTION_ID_,TASK_ID_,NAME_,VAR_TYPE_,TEXT_,CREATE_TIME_,LAST_UPDATED_TIME_) '
                        'VALUES(%s,1,%s,%s,NULL,%s,\'string\',%s,NOW(3),NOW(3))',
                        (ids[key], process_id, process_id, key, value))
            if cur.rowcount != 1:
                raise ValueError('Historical display variable insert was not exact')
        connection.commit()
        evidence_data['status'] = 'committed'
        evidence.write_text(json.dumps(evidence_data, ensure_ascii=False, default=str, indent=2), encoding='utf-8')
        return plan


def connect():
    names = ('HOST', 'PORT', 'NAME', 'USER', 'PASSWORD')
    settings = {key: os.environ['REGCERT_REPAIR_DB_' + key] for key in names}
    return pymysql.connect(host=settings['HOST'], port=int(settings['PORT']), database=settings['NAME'],
                           user=settings['USER'], password=settings['PASSWORD'], charset='utf8mb4',
                           cursorclass=pymysql.cursors.DictCursor, autocommit=False)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--tenant-id', type=int, required=True)
    parser.add_argument('--request-id', type=int, required=True)
    parser.add_argument('--process-id', required=True)
    parser.add_argument('--apply', action='store_true')
    parser.add_argument('--evidence', type=Path)
    args = parser.parse_args()
    if min(args.tenant_id, args.request_id) <= 0 or not args.process_id.strip():
        parser.error('Positive tenant/request and exact process identity required')
    if args.apply and args.evidence is None:
        parser.error('--evidence is required with --apply')
    connection = connect()
    try:
        result = repair(connection, args.tenant_id, args.request_id, args.process_id,
                        apply=args.apply, evidence=args.evidence)
        print(json.dumps(result, ensure_ascii=False, default=str, indent=2))
    finally:
        connection.rollback()
        connection.close()


if __name__ == '__main__':
    main()
