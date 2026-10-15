"""A23 formal source gate plus real H2 test schema contract; no app compilation."""
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
DDL='IntRuoyiBackend/sql/mysql/20261015_dcc_application_custom_matrix.sql'
DO='IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/dataobject/file/DccControlledFileDO.java'
COLUMNS={'draft_approval_source_mode':'VARCHAR(32)','draft_application_custom_matrix_json':'LONGTEXT','draft_application_context_key':'VARCHAR(128)'}


def main():
    parser=argparse.ArgumentParser();parser.add_argument('--phase',required=True);args=parser.parse_args()
    out=TASK/('a23-'+args.phase);out.mkdir(exist_ok=False)
    schema=(ROOT/SCHEMA).read_text(encoding='utf-8');table=re.search(r'CREATE TABLE IF NOT EXISTS `dcc_controlled_file`\s*\(.*?\n\);',schema,re.S)
    if not table:raise ValueError('actual file CREATE TABLE required')
    (out/'file.sql').write_text(table[0],encoding='utf-8')
    cp=(ROOT/'IntRuoyiBackend/yudao-module-dcc/target/dcc-test.classpath').read_text(encoding='utf-8-sig').split(';')
    h2=[x for x in cp if re.search(r'[\\/]com[\\/]h2database[\\/]h2[\\/]',x)]
    if len(h2)!=1:raise ValueError('one exact H2 jar required')
    run=subprocess.run(['java','-Dfile.encoding=UTF-8','--source','17','-cp',h2[0],str(TASK/'A23DraftSchema.java'),str(out/'file.sql')],cwd=ROOT,capture_output=True)
    text=(run.stdout+run.stderr).decode('utf-8',errors='replace');print(text)
    exists=(ROOT/DDL).exists();ddl=(ROOT/DDL).read_text(encoding='utf-8') if exists else '';java=(ROOT/DO).read_text(encoding='utf-8')
    formal_columns=dict(re.findall(r"CALL ensure_dcc_application_custom_column\('([^']+)','([^']+)'\);",ddl))
    dependency_match=re.search(r'dependsOn=([^;]+);',ddl)
    dependencies=dependency_match[1].split(',') if dependency_match else []
    ready=(exists
        and formal_columns=={name:kind.lower() for name,kind in COLUMNS.items()}
        and "IS_NULLABLE='YES'" in ddl and 'COLUMN_DEFAULT IS NULL' in ddl
        and not re.search(r'(?im)^\s*(UPDATE|INSERT|DELETE)\s',ddl)
        and bool(dependencies) and all((ROOT/'IntRuoyiBackend/sql/mysql'/(name+'.sql')).exists() for name in dependencies)
        and all(re.search(r'private\s+String\s+'+name+r'\s*;',java) for name in ['draftApprovalSourceMode','draftApplicationCustomMatrixJson','draftApplicationContextKey']))
    print('A23 formal actualDDL/DO gate:','READY' if ready else 'DEPENDENCY_NOT_READY; not business RED')
    declarations={name: bool(re.search(r'`'+name+r'` '+re.escape(kind)+r' NULL,',table[0])) for name,kind in COLUMNS.items()}
    print('A23 explicit nullable/no-default schema declarations:',declarations)
    result={'phase':args.phase,'h2_exit':run.returncode,'h2_output':text,'formal_sources_ready':ready,'formal_columns':formal_columns,'direct_dependency_files_exist':dependencies,'schema_declarations':declarations,'source_sha256':{name:hashlib.sha256((ROOT/name).read_bytes()).hexdigest() for name in [SCHEMA,DO]+([DDL] if exists else [])}}
    (out/'result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    return 1 if run.returncode or not all(declarations.values()) else (0 if ready else 2)


if __name__=='__main__':sys.exit(main())
