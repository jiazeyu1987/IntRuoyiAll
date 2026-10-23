"""A26 actual H2 shape + actual public source negative guards. Targeted private output only."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import sys
sys.stdout.reconfigure(encoding='utf-8')
TASK=Path(__file__).resolve().parent;ROOT=TASK.parents[2];BE=ROOT/'IntRuoyiBackend';DCC=BE/'yudao-module-dcc'
SCHEMA='IntRuoyiBackend/yudao-module-dcc/src/test/resources/sql/create_tables.sql'


def main():
    parser=argparse.ArgumentParser();parser.add_argument('--phase',required=True);args=parser.parse_args();out=TASK/('a26-'+args.phase);out.mkdir(exist_ok=False)
    text=(ROOT/SCHEMA).read_text(encoding='utf-8');table=re.search(r'CREATE TABLE IF NOT EXISTS `dcc_controlled_file_route_snapshot`\s*\(.*?\n\);',text,re.S)
    if not table:raise ValueError('current route snapshot table required')
    ddl=out/'route.sql';ddl.write_text(table[0],encoding='utf-8')
    entries=(DCC/'target/dcc-test.classpath').read_text(encoding='utf-8-sig').split(';');h2=[x for x in entries if re.search(r'[\\/]com[\\/]h2database[\\/]h2[\\/]',x)]
    if len(h2)!=1:raise ValueError('exact H2 dependency required')
    shape=subprocess.run(['java','-Dfile.encoding=UTF-8','--source','17','-cp',h2[0],str(TASK/'A26RouteShape.java'),str(ddl)],cwd=ROOT,capture_output=True)
    shape_text=(shape.stdout+shape.stderr).decode('utf-8',errors='replace');print(shape_text)
    classes=out/'classes';classes.mkdir()
    main=DCC/'src/main/java/cn/iocoder/yudao/module/dcc/service/file'
    sources=[main/'DccApprovalMatrixCanonicalizer.java',main/'DccApprovalMatrixSourceService.java',DCC/'src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccApprovalMatrixBaselineVO.java']
    cp=';'.join(dict.fromkeys([str(DCC/'target/classes'),*[str(p) for p in sorted(BE.glob('*/target/classes'))],*[str(p) for p in sorted(BE.glob('yudao-framework/*/target/classes'))],*entries]))
    compile_args=['-encoding','UTF-8','-sourcepath','','-cp',cp,'-d',str(classes),*[str(p) for p in sources],str(TASK/'A26PublicSourceGate.java')]
    argfile=out/'compile.args';argfile.write_text('\n'.join('"'+x.replace('\\','/')+'"' for x in compile_args),encoding='utf-8')
    compile=subprocess.run(['javac','-J-Dfile.encoding=UTF-8','@'+str(argfile)],cwd=ROOT,capture_output=True)
    print('A26 targeted3-source javac exit=',compile.returncode)
    public_exit=None;public_text=''
    if compile.returncode:public_text=(compile.stdout+compile.stderr).decode('utf-8',errors='replace');print(public_text)
    else:
        runfile=out/'run.args';runfile.write_text('\n'.join('"'+x.replace('\\','/')+'"' for x in ['-Dfile.encoding=UTF-8','-cp',str(classes)+';'+cp,'A26PublicSourceGate']),encoding='utf-8')
        public=subprocess.run(['java','@'+str(runfile)],cwd=ROOT,capture_output=True);public_exit=public.returncode;public_text=(public.stdout+public.stderr).decode('utf-8',errors='replace');print(public_text)
    result={'phase':args.phase,'h2_exit':shape.returncode,'h2_output':shape_text,'javac_exit':compile.returncode,'public_gate_exit':public_exit,'public_gate_output':public_text,'source_sha256':{str(p.relative_to(ROOT)):hashlib.sha256(p.read_bytes()).hexdigest() for p in [ROOT/SCHEMA,*sources]}}
    (out/'result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    return 1 if shape.returncode or compile.returncode or public_exit!=0 else 0


if __name__=='__main__':sys.exit(main())
