"""Inspect actual MySQL 8.0 ROW events before exporting executable binlog SQL.

SHOW BINLOG EVENTS is read in bounded pages over the exact retained positions.
No event is filtered or skipped. Statement DDL, account/privilege SQL, compressed
transaction payloads and unknown event types require a separate scope proof and
are refused here. A current database listing is not historical scope evidence.
"""
import hashlib
import math
import re
import time

import formal_snapshot_linux as snapshot


def require(value, code='FORMAL_CAPTURE_BINLOG_SCOPE_UNPROVEN'):
    snapshot.require(value, code)


class RowEventScope:
    def __init__(self, database):
        require(type(database) is str and
                re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_-]{0,63}', database))
        self.database = database
        self.tables = {}
        self.transaction = self.gtid = False
        self.row_count = self.event_count = 0

    def observe(self, event_type, info):
        require(type(info) is str and '\0' not in info)
        self.event_count += 1
        if event_type in {'Format_desc', 'Previous_gtids', 'Rotate', 'Stop'}:
            require(not self.transaction and not self.gtid)
        elif event_type == 'Gtid':
            require(not self.transaction and not self.gtid)
            self.gtid = True
        elif event_type == 'Query':
            # Query_log_event::pack_info prepends its original default DB.
            match = re.fullmatch(r'(?:use `([A-Za-z0-9_-]+)`; )?'
                                 r'(BEGIN|COMMIT|ROLLBACK)', info)
            require(match is not None, 'FORMAL_CAPTURE_BINLOG_QUERY_SCOPE_UNPROVEN')
            require(match[1] in (None, self.database),
                    'FORMAL_CAPTURE_BINLOG_DATABASE_OUT_OF_SCOPE')
            if match[2] == 'BEGIN':
                require(self.gtid and not self.transaction)
                self.transaction = True
            else:
                require(self.gtid and self.transaction)
                self.transaction = self.gtid = False
                self.tables.clear()
        elif event_type == 'Table_map':
            require(self.gtid and self.transaction)
            match = re.fullmatch(r'table_id: ([0-9]+) '
                                 r'\(([A-Za-z0-9_-]+)\.([A-Za-z0-9_$-]+)\)', info)
            require(match is not None)
            require(match[2] == self.database,
                    'FORMAL_CAPTURE_BINLOG_DATABASE_OUT_OF_SCOPE')
            self.tables[int(match[1])] = (match[2], match[3])
        elif event_type in {'Write_rows', 'Update_rows', 'Delete_rows'}:
            require(self.gtid and self.transaction)
            match = re.fullmatch(r'table_id: ([0-9]+)(?: flags: STMT_END_F)?', info)
            require(match is not None and int(match[1]) in self.tables)
            self.row_count += 1
        elif event_type == 'Rows_query':
            # This annotation is not executable SQL; the actual table/row events
            # still have to prove every affected database independently.
            require(self.gtid and self.transaction)
        elif event_type == 'Xid':
            require(self.gtid and self.transaction)
            require(re.fullmatch(r'COMMIT /\* xid=[0-9]+ \*/', info) is not None)
            self.transaction = self.gtid = False
            self.tables.clear()
        else:
            # Includes statement DML helpers, load-data, anonymous GTIDs and
            # Transaction_payload (whose inner events are not exposed here).
            require(False, 'FORMAL_CAPTURE_BINLOG_EVENT_SCOPE_UNPROVEN')

    def complete(self):
        require(not self.transaction and not self.gtid,
                'FORMAL_CAPTURE_BINLOG_TRANSACTION_INCOMPLETE')


def inspect_binlog_scope(mysql, segments, database, *, guard, deadline):
    """Retain a digest of all original event rows, never log their SQL text."""
    require(callable(guard) and type(deadline) in (int, float) and math.isfinite(deadline))
    require(type(segments) is list)
    RowEventScope(database)
    proof = []
    for segment in segments:
        require(type(segment) is dict and set(segment) ==
                {'file', 'startPosition', 'stopPosition'} and
                type(segment['file']) is str and
                re.fullmatch(r'[A-Za-z0-9_-]+\.[0-9]{6}', segment['file']) and
                type(segment['startPosition']) is int and
                type(segment['stopPosition']) is int and
                4 <= segment['startPosition'] < segment['stopPosition'])
        cursor = segment['startPosition']
        scope = RowEventScope(database)
        checksum = hashlib.sha256()
        while cursor < segment['stopPosition']:
            guard()
            require(time.monotonic() < deadline, 'FORMAL_CAPTURE_BINLOG_SCOPE_DEADLINE')
            # Identifiers/positions are strictly validated before SQL assembly.
            rows = mysql.query("SHOW BINLOG EVENTS IN '" + segment['file'] +
                               "' FROM " + str(cursor) + " LIMIT 256;")
            require(type(rows) is str and 0 < len(rows.encode('utf-8')) <= 1048576)
            page = rows.splitlines()
            require(0 < len(page) <= 256)
            for row in page:
                if cursor == segment['stopPosition']:
                    break
                guard()
                require(time.monotonic() < deadline, 'FORMAL_CAPTURE_BINLOG_SCOPE_DEADLINE')
                values = row.split('\t', 5)
                require(len(values) == 6 and values[0] == segment['file'] and
                        all(re.fullmatch(r'[0-9]+', values[i]) for i in (1, 3, 4)))
                event_position, end_position = int(values[1]), int(values[4])
                require(event_position == cursor and
                        cursor < end_position <= segment['stopPosition'],
                        'FORMAL_CAPTURE_BINLOG_EVENT_RANGE_CHANGED')
                scope.observe(values[2], values[5])
                checksum.update(snapshot._canonical_bytes(values))
                cursor = end_position
        scope.complete()
        proof.append(dict(segment=segment, eventRowsSha256=checksum.hexdigest(),
                          eventCount=scope.event_count, rowEventCount=scope.row_count))
    guard()
    require(time.monotonic() < deadline, 'FORMAL_CAPTURE_BINLOG_SCOPE_DEADLINE')
    return dict(schemaVersion='backup-binlog-event-scope/v1', databaseScope=[database],
                policy='MYSQL80_ROW_SCOPED_TRANSACTIONS_ONLY', segments=proof)
