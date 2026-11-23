"""Read-only extraction of the 22 audited QA row images from local MySQL binlogs.

Uses pinned mysql-replication installed only in output/registration-binlog-tools.
Reads files through docker exec cat; never replays SQL or changes the database.
CRC32 and exact schema/column count must match. Output contains only scoped rows.
"""
import base64
import argparse
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import struct
import subprocess
import sys
import zlib

root = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(root / 'output/registration-binlog-tools'))
from pymysql.protocol import MysqlPacket
from pymysqlreplication.packet import BinLogPacketWrapper
from pymysqlreplication.row_event import TableMapEvent, WriteRowsEvent, UpdateRowsEvent, DeleteRowsEvent

spec = importlib.util.spec_from_file_location('db', Path(__file__).with_name('repair-registration-download-summary.py'))
db = importlib.util.module_from_spec(spec)
spec.loader.exec_module(db)
table = 'mes_pro_edhr_nonconformance_review'


def run():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--logs', nargs='+', default=['binlog.%06d' % i for i in range(294,304)])
    parser.add_argument('--output', type=Path, default=root / 'workspace/qa-missing-original-binlog-evidence-20261122.json')
    args = parser.parse_args()
    if any(not __import__('re').fullmatch(r'binlog\.\d{6}', log) for log in args.logs):
        raise ValueError('Only exact local binlog names allowed')
    c = db.connect()
    c._get_dbms = lambda: 'mysql'
    try:
        with c.cursor() as q:
            q.execute('SET TRANSACTION READ ONLY')
            q.execute('START TRANSACTION')
            q.execute('SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=%s AND TABLE_NAME=%s ORDER BY ORDINAL_POSITION', (os.environ['REGCERT_REPAIR_DB_NAME'], table))
            names = [r['COLUMN_NAME'] for r in q.fetchall()]
        assert names and names[0] == 'id'
        ids = [109,110,111,112,113,114,115,116,117,122,123,124,125,126,127,128,129,130,131,132,133,134]
        result = dict(table=table, targetIds=ids, columnNames=names, files=[], matches=[])
        output = args.output
        if output.exists():
            raise FileExistsError(output)
        for log in args.logs:
            maps = {}
            count = 0
            selected = 0
            h = hashlib.sha256()
            p = subprocess.Popen(['docker', 'exec', 'int-ruoyi-mysql', 'cat', '/var/lib/mysql/' + log], stdout=subprocess.PIPE)
            def read(n):
                data = p.stdout.read(n)
                if len(data) != n:
                    raise ValueError('Truncated binlog ' + log)
                h.update(data)
                return data
            assert read(4) == b'\xfebin'
            position = 4
            try:
                while True:
                    header = p.stdout.read(19)
                    if not header:
                        break
                    if len(header) != 19:
                        raise ValueError('Partial event header')
                    h.update(header)
                    ts, kind, server, size, nextpos, flags = struct.unpack('<IBIIIH', header)
                    if size < 23 or size > 2**30:
                        raise ValueError('Invalid event size')
                    payload = read(size - 19)
                    raw = header + payload
                    checksum_data = bytearray(raw[:-4])
                    # MySQL excludes the mutable BINLOG_IN_USE flag from FDE CRC32.
                    if kind == 15:
                        checksum_data[17] &= 0xfe
                    if zlib.crc32(checksum_data) != struct.unpack('<I', raw[-4:])[0]:
                        raise ValueError('Event CRC32 mismatch')
                    count += 1
                    relevant = False
                    if kind == 19:
                        tid = int.from_bytes(payload[:6], 'little')
                        dblen = payload[8]
                        schema = payload[9:9+dblen].decode()
                        off = 10 + dblen
                        tablen = payload[off]
                        tab = payload[off+1:off+1+tablen].decode()
                        maps.pop(tid, None)
                        relevant = tab == table and schema == os.environ['REGCERT_REPAIR_DB_NAME']
                    elif kind in (23, 24, 25, 30, 31, 32, 39):
                        relevant = int.from_bytes(payload[:6], 'little') in maps
                    if relevant:
                        wrapped = BinLogPacketWrapper(MysqlPacket(b'\0' + raw, 'utf8'), maps, c, (8, 0, 39), True,
                            {TableMapEvent, WriteRowsEvent, UpdateRowsEvent, DeleteRowsEvent}, [table], None,
                            [os.environ['REGCERT_REPAIR_DB_NAME']], None, False, False, True, True, False,
                            use_column_name_cache=True)
                        event = wrapped.event
                        if kind == 19:
                            if event.column_count != len(names) or [col.name for col in event.columns] != names:
                                raise ValueError('Historical row schema differs from verified column layout')
                        else:
                            selected += 1
                            for row in event.rows:
                                images = [row[k] for k in ('values','before_values','after_values') if k in row]
                                if any(image.get('id') in ids for image in images):
                                    result['matches'].append(dict(log=log, position=position, nextPosition=nextpos,
                                        timestamp=ts, eventType=type(event).__name__, row=row,
                                        rawEventBase64=base64.b64encode(raw).decode()))
                                    print(json.dumps(dict(log=log, position=position, type=type(event).__name__, ids=[image.get('id') for image in images])), flush=True)
                    position += size
                    if count % 1000000 == 0:
                        print(json.dumps(dict(log=log, bytes=position, events=count)), flush=True)
                if p.wait() != 0:
                    raise RuntimeError('Binlog file read failed')
            finally:
                if p.poll() is None:
                    p.terminate()
                p.stdout.close()
            result['files'].append(dict(log=log, bytes=position, sha256=h.hexdigest(), events=count, selectedEvents=selected))
            print(json.dumps(result['files'][-1]), flush=True)
        with output.open('x', encoding='utf-8') as f:
            json.dump(result, f, ensure_ascii=False, indent=2, default=lambda x: {'binaryBase64':base64.b64encode(x).decode()} if isinstance(x,bytes) else str(x))
        print('Extraction complete; matches:', len(result['matches']), flush=True)
    finally:
        c.rollback()
        c.close()


if __name__ == '__main__':
    run()
