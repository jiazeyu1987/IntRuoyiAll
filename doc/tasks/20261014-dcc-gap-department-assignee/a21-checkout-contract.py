"""Targeted A21 fixture contract; official DDL availability is a separate gate from H2 RED/GREEN."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import sys

sys.stdout.reconfigure(encoding='utf-8')
TASK=Path(__file__).resolve().parent
ROOT=TASK.parents[2]
SCHEMA='IntRuoyiBackend/yudao-module-dcc/src/test/resources/sql/create_tables.sql'
DDL='IntRuoyiBackend/sql/mysql/20261015_dcc_checkin_payload_hash.sql'
DO='IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/dataobject/file/DccControlledFileCheckoutDO.java'
MAPPER='IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/file/DccControlledFileCheckoutMapper.java'


def main():
    parser=argparse.ArgumentParser();parser.add_argument('--phase',required=True);args=parser.parse_args()
    output=TASK/('a21-'+args.phase);output.mkdir(exist_ok=False)
    schema=(ROOT/SCHEMA).read_text(encoding='utf-8');table=re.search(r'CREATE TABLE IF NOT EXISTS `dcc_controlled_file_checkout`\s*\(.*?\n\);',schema,re.S)
    if not table:raise ValueError('exact checkout CREATE TABLE required')
    target=output/'checkout.sql';target.write_text(table[0],encoding='utf-8')
    paths=(ROOT/'IntRuoyiBackend/yudao-module-dcc/target/dcc-test.classpath').read_text(encoding='utf-8-sig').split(';')
    h2=[x for x in paths if re.search(r'[\\/]com[\\/]h2database[\\/]h2[\\/]',x)]
    if len(h2)!=1:raise ValueError('one real H2 jar required')
    h2run=subprocess.run(['java','-Dfile.encoding=UTF-8','--source','17','-cp',h2[0],str(TASK/'A21CheckoutSchema.java'),str(target)],cwd=ROOT,capture_output=True)
    text=(h2run.stdout+h2run.stderr).decode('utf-8',errors='replace');print(text)
    exists=(ROOT/DDL).exists()
    ddl=(ROOT/DDL).read_text(encoding='utf-8') if exists else ''
    java=(ROOT/DO).read_text(encoding='utf-8')
    mapper=(ROOT/MAPPER).read_text(encoding='utf-8')
    sources_ready=(exists
        and bool(re.search(r'ADD COLUMN checkin_payload_hash CHAR\(64\) NULL',ddl))
        and "IS_NULLABLE='YES'" in ddl and 'COLUMN_DEFAULT IS NULL' in ddl
        and not re.search(r'(?im)^\s*(UPDATE|INSERT|DELETE)\s',ddl)
        and bool(re.search(r'private\s+String\s+checkinPayloadHash\s*;',java))
        and 'checkin_payload_hash = #{payloadHash}' in mapper
        and '@Param("payloadHash") String payloadHash' in mapper)
    print('Official DDL/DO source gate:', 'READY' if sources_ready else 'DEPENDENCY_NOT_READY (not counted as business RED)')
    print('Schema explicit CHAR(64) NULL no-default:',bool(re.search(r'`checkin_payload_hash` CHAR\(64\) NULL,',table[0])))
    result={'phase':args.phase,'h2_exit':h2run.returncode,'h2_output':text,'official_sources_ready':sources_ready,
            'hashes':{name:hashlib.sha256((ROOT/name).read_bytes()).hexdigest() for name in [SCHEMA,DO,MAPPER]+([DDL] if exists else [])}}
    (output/'result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    return 1 if h2run.returncode else (2 if not sources_ready else 0)


if __name__=='__main__':sys.exit(main())
