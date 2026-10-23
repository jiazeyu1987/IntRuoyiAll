"""A33 private javac + real Spring/H2 PageDB; never runs Maven or writes shared target."""
from pathlib import Path
import xml.etree.ElementTree as ET
import hashlib, subprocess, sys

task=Path(__file__).resolve().parent
repo=task.parents[2]
module=repo/'IntRuoyiBackend/yudao-module-dcc'
phase=sys.argv[1]
if phase not in {'red','green','regression'}: raise ValueError('red/green/regression required')
xml=repo/'doc/tasks/20261014-dcc-gap-fix-coordination/m09-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccReferenceUsagePageDatabaseTest.xml'
props={p.attrib['name']:p.attrib['value'] for p in ET.parse(xml).findall('./properties/property')}
paths=props['java.class.path'].split(';')
engine=next(Path(p) for p in paths if Path(p).name.startswith('junit-platform-engine-') and p.endswith('.jar'))
launcher=engine.parent.parent.parent/'junit-platform-launcher'/engine.parent.name/('junit-platform-launcher-'+engine.parent.name+'.jar')
if str(launcher) not in paths: paths.append(str(launcher))
for path in paths:
    if not Path(path).exists(): raise FileNotFoundError(path)
out=task/('private-page-'+phase)
out.mkdir(exist_ok=True)
base=module/'src/main/java/cn/iocoder/yudao/module/dcc'
sources=[base/('service/file/relations/'+name+'.java') for name in ['DccRelationContracts','DccRelationStore','DccProjectReferenceService']]
sources += [base/'service/file/DccControlledFileQueryServiceImpl.java',base/'service/file/DccControlledFileDetailAuthorizationGuard.java',base/'controller/admin/file/DccProjectReferenceController.java']
sources += [module/'src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccReferenceUsagePageDatabaseTest.java',task/'PageReviewJUnit.java']
for source in sources: print('CURRENT_SOURCE',source.relative_to(repo),hashlib.sha256(source.read_bytes()).hexdigest(),flush=True)
quote=lambda value:'"'+str(value).replace('\\','/')+'"'
cp=';'.join(paths)
compile_args=out/'compile.args'
compile_args.write_text('-encoding UTF-8\n-parameters\n-proc:none\n-d\n'+quote(out)+'\n-cp\n'+quote(cp)+'\n'+'\n'.join(quote(source) for source in sources),encoding='utf-8')
jdk=repo/'.runtime/tools/jdk-17/bin'
result=subprocess.run([str(jdk/'javac.exe'),'@'+str(compile_args)],cwd=module)
if result.returncode: sys.exit(result.returncode)
run_args=out/'run.args'
run_args.write_text('-Dfile.encoding=UTF-8\n-cp\n'+quote(str(out)+';'+cp)+'\nPageReviewJUnit\n'+('regression\n' if phase=='regression' else ''),encoding='utf-8')
result=subprocess.run([str(jdk/'java.exe'),'@'+str(run_args)],cwd=module)
sys.exit(result.returncode)
