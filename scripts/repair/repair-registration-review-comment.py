"""Restore a missing business review comment from the exact finished BPM task.

Defaults to read-only. No formatted-comment parsing, approval replay or runtime
fallback. --apply requires an exclusive evidence file containing the original row.
Recovery: restore originalBinding.detail_json only if the current value still
equals repairedDetailJson and tenant/request/process/binding identities match.
"""
import argparse
from datetime import datetime, timezone
import importlib.util
import json
from pathlib import Path

spec = importlib.util.spec_from_file_location('reg_summary', Path(__file__).with_name('repair-registration-download-summary.py'))
summary = importlib.util.module_from_spec(spec)
spec.loader.exec_module(summary)


def plan_comment(request, binding, process, tasks, variables, comments):
    tenant = request['tenant_id']
    proc = request['bpm_process_instance_id']
    if (request['status'] != 'APPROVED' or binding['tenant_id'] != tenant
            or binding['request_id'] != request['id'] or binding['bpm_process_instance_id'] != proc
            or binding['business_key'] != 'DCC_REG_CERT_ACCESS:' + str(request['id'])
            or process['TENANT_ID_'] != str(tenant) or process['definitionTenant'] != str(tenant)
            or process['KEY_'] != 'dcc-registration-certificate-access'
            or process['BUSINESS_KEY_'] != binding['business_key'] or not process['END_TIME_']):
        raise ValueError('Approved request, tenant, process or formal binding mismatch')
    if len(tasks) != 1:
        raise ValueError('Expected exactly one original approval task')
    task = tasks[0]
    detail = json.loads(binding['detail_json'])
    if (not isinstance(detail, dict) or task['TASK_DEF_KEY_'] != 'REG_CERT_ACCESS_APPROVAL'
            or task.get('TENANT_ID_') != str(tenant)
            or not task['END_TIME_'] or str(detail.get('reviewerUserId')) != task['ASSIGNEE_']):
        raise ValueError('Original completed approver does not match business evidence')
    reviewed = datetime.fromisoformat(detail['reviewedAt'])
    if abs((reviewed - task['END_TIME_']).total_seconds()) > 1:
        raise ValueError('Review timestamp conflicts with original completed task')
    def only(name, task_id):
        found = [v for v in variables if v['NAME_'] == name and v['TASK_ID_'] == task_id]
        if len(found) != 1:
            raise ValueError('Missing or ambiguous original variable: ' + name)
        return found[0]
    if only('TASK_STATUS', task['ID_'])['LONG_'] != 2 or only('PROCESS_STATUS', None)['LONG_'] != 2:
        raise ValueError('Original task/process is not approved')
    reason = only('TASK_REASON', task['ID_'])
    value = reason['TEXT_']
    if reason['VAR_TYPE_'] != 'string' or not isinstance(value, str) or not value.strip():
        raise ValueError('Exact original task reason is missing')
    matching = [x for x in comments if x['TASK_ID_'] == task['ID_'] and x['TYPE_'] == '1']
    if (len(matching) != 1 or matching[0]['USER_ID_'] != task['ASSIGNEE_']
            or matching[0]['MESSAGE_'] != '审批通过，原因是：' + value):
        raise ValueError('Original approval comment does not confirm the task reason')
    old = detail.get('reviewComment')
    if old is not None and old != value:
        raise ValueError('Existing business review comment conflicts; refusing overwrite')
    detail['reviewComment'] = value
    return dict(tenantId=tenant, requestId=request['id'], processInstanceId=proc,
                bindingId=binding['id'], taskId=task['ID_'], reasonVariableId=reason['ID_'],
                needsUpdate=old is None, repairedDetailJson=json.dumps(detail, ensure_ascii=False))


def repair(connection, tenant_id, request_id, process_id, *, apply=False, evidence=None):
    with connection.cursor() as q:
        if not apply:
            q.execute('SET TRANSACTION READ ONLY')
        q.execute('START TRANSACTION')
        lock = ' FOR UPDATE' if apply else ''
        def one(sql, args):
            q.execute(sql + lock, args)
            rows = q.fetchall()
            if len(rows) != 1:
                raise ValueError('Required exact historical row missing or ambiguous')
            return rows[0]
        request = one('SELECT * FROM dcc_registration_certificate_access_request '
                      'WHERE tenant_id=%s AND id=%s AND deleted=0', (tenant_id, request_id))
        if request['bpm_process_instance_id'] != process_id:
            raise ValueError('Explicit process differs from selected request')
        binding = one('SELECT * FROM dcc_registration_certificate_bpm_binding '
                      'WHERE tenant_id=%s AND request_id=%s AND deleted=0', (tenant_id, request_id))
        process = one('SELECT p.*,d.KEY_,d.TENANT_ID_ AS definitionTenant FROM ACT_HI_PROCINST p '
                      'JOIN ACT_RE_PROCDEF d ON d.ID_=p.PROC_DEF_ID_ '
                      'WHERE CAST(p.PROC_INST_ID_ AS BINARY)=CAST(%s AS BINARY)', (process_id,))
        q.execute('SELECT ID_ FROM ACT_RU_EXECUTION WHERE PROC_INST_ID_=%s' + lock, (process_id,))
        if q.fetchone():
            raise ValueError('Process still has runtime executions')
        groups = []
        for table in ('ACT_HI_TASKINST', 'ACT_HI_VARINST', 'ACT_HI_COMMENT'):
            q.execute('SELECT * FROM ' + table + ' WHERE PROC_INST_ID_=%s' + lock, (process_id,))
            groups.append(q.fetchall())
        plan = plan_comment(request, binding, process, *groups)
        if not apply or not plan['needsUpdate']:
            connection.rollback()
            return plan
        if evidence is None:
            raise ValueError('Exclusive --evidence is required with --apply')
        data = dict(plan, originalBinding=binding, originalRequest=request,
                    sourceEvidence=dict(process=process, tasks=groups[0], variables=groups[1], comments=groups[2]),
                    preparedAt=datetime.now(timezone.utc).isoformat(), status='prepared')
        with evidence.open('x', encoding='utf-8') as f:
            json.dump(data, f, ensure_ascii=False, default=str, indent=2)
        q.execute('UPDATE dcc_registration_certificate_bpm_binding SET detail_json=%s '
                  'WHERE id=%s AND tenant_id=%s AND request_id=%s AND bpm_process_instance_id=%s '
                  'AND deleted=0 AND CAST(detail_json AS BINARY)=CAST(%s AS BINARY)',
                  (plan['repairedDetailJson'], binding['id'], tenant_id, request_id, process_id, binding['detail_json']))
        if q.rowcount != 1:
            raise ValueError('Exact original business evidence changed; repair rejected')
        q.execute('SELECT detail_json FROM dcc_registration_certificate_bpm_binding WHERE id=%s', (binding['id'],))
        if json.loads(q.fetchone()['detail_json']) != json.loads(plan['repairedDetailJson']):
            raise ValueError('Written business review did not match repair plan')
        connection.commit()
        data['status'] = 'committed'
        evidence.write_text(json.dumps(data, ensure_ascii=False, default=str, indent=2), encoding='utf-8')
        return plan


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--tenant-id', type=int, required=True)
    p.add_argument('--request-id', type=int, required=True)
    p.add_argument('--process-id', required=True)
    p.add_argument('--apply', action='store_true')
    p.add_argument('--evidence', type=Path)
    args = p.parse_args()
    if min(args.tenant_id, args.request_id) <= 0 or not args.process_id.strip() or (args.apply and args.evidence is None):
        p.error('Positive identities and exclusive evidence for writes are required')
    c = summary.connect()
    try:
        print(json.dumps(repair(c, args.tenant_id, args.request_id, args.process_id,
                               apply=args.apply, evidence=args.evidence), ensure_ascii=False))
    finally:
        c.rollback()
        c.close()


if __name__ == '__main__':
    main()
