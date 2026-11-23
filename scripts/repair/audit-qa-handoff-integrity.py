"""Read-only QA handoff source audit; exit 2 on missing/deleted source records.

Requires QA_AUDIT_DB_HOST/PORT/NAME/USER/PASSWORD and --tenant-id.
Never restores evidence, guesses decisions, or edits task status.
"""
import argparse
import json
import os

import pymysql


def connect():
    return pymysql.connect(
        host=os.environ['QA_AUDIT_DB_HOST'], port=int(os.environ['QA_AUDIT_DB_PORT']),
        database=os.environ['QA_AUDIT_DB_NAME'], user=os.environ['QA_AUDIT_DB_USER'],
        password=os.environ['QA_AUDIT_DB_PASSWORD'], charset='utf8mb4',
        cursorclass=pymysql.cursors.DictCursor,
    )


def audit(connection, tenant_id):
    if tenant_id <= 0:
        raise ValueError('tenant_id must be positive')
    with connection.cursor() as q:
        q.execute('SET TRANSACTION READ ONLY')
        q.execute('START TRANSACTION WITH CONSISTENT SNAPSHOT')
        q.execute("""
            SELECT h.id AS task_id, h.source_id AS review_id, h.task_type, h.status,
                   h.active_order_id, h.work_order_id,
                   CASE WHEN r.id IS NULL THEN 'SOURCE_MISSING' ELSE 'SOURCE_DELETED' END AS issue
            FROM mes_active_order_handoff_task h
            LEFT JOIN mes_pro_edhr_nonconformance_review r
              ON r.id=h.source_id AND r.tenant_id=h.tenant_id
            WHERE h.tenant_id=%s AND h.deleted=0 AND h.source_type='NONCONFORMANCE_REVIEW'
              AND (r.id IS NULL OR r.deleted<>0)
            ORDER BY h.source_id,h.id
        """, (tenant_id,))
        rows = list(q.fetchall())
    connection.rollback()
    return {'tenantId': tenant_id, 'status': 'FAILED' if rows else 'PASSED',
            'scope': 'QA handoff source existence only; signature integrity is checked by the application',
            'affectedTaskCount': len(rows),
            'affectedReviewCount': len({r['review_id'] for r in rows}), 'issues': rows}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--tenant-id', type=int, required=True)
    args = parser.parse_args()
    connection = connect()
    try:
        report = audit(connection, args.tenant_id)
    finally:
        connection.close()
    print(json.dumps(report, ensure_ascii=False))
    return 2 if report['issues'] else 0


if __name__ == '__main__':
    raise SystemExit(main())
