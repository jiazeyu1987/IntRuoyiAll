"""Compile only authorized current Reference source/test into this task's private output; no Maven/shared target writes."""
import hashlib
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

TASK = Path(__file__).resolve().parent
REPO = TASK.parents[2]
MODULE = REPO / 'IntRuoyiBackend/yudao-module-dcc'
PHASE = sys.argv[1]
SELECTION = sys.argv[2] if len(sys.argv) > 2 else ('regression' if PHASE == 'regression' else '')
if SELECTION not in {'', 'regression', 'summary-review'}:
    raise ValueError('selection must be regression/summary-review or omitted')
if PHASE not in {'red', 'green', 'regression'}:
    raise ValueError('phase must be red/green/regression')
REPORT = MODULE / 'target/surefire-reports/TEST-cn.iocoder.yudao.module.dcc.service.file.DccReferenceUsagePageDatabaseTest.xml'
properties = {prop.attrib['name']: prop.attrib['value'] for prop in ET.parse(REPORT).findall('./properties/property')}
classpath = properties['java.class.path']
entries = [Path(entry) for entry in classpath.split(';')]
if any(not path.exists() for path in entries):
    raise FileNotFoundError('read-only existing dependency classpath has missing entries')
out = TASK / ('private-reference-' + PHASE)
out.mkdir(exist_ok=True)
main = MODULE / 'src/main/java/cn/iocoder/yudao/module/dcc'
tests = MODULE / 'src/test/java/cn/iocoder/yudao/module/dcc/service/file'
sources = [main / ('service/file/relations/' + file + '.java') for file in ['DccRelationContracts', 'DccRelationStore', 'DccProjectReferenceService']]
sources += [tests / 'DccExactVersionReferenceUsageTest.java']
sources += [tests / 'DccProjectReferenceTransactionTest.java']
sources += [tests / 'DccReferenceUsagePageDatabaseTest.java']
sources += [main / 'controller/admin/file/DccProjectReferenceController.java']
for source in sources:
    print('SOURCE', source.relative_to(REPO), hashlib.sha256(source.read_bytes()).hexdigest(), flush=True)
quote = lambda value: '"' + str(value).replace('\\', '/') + '"'
compile_args = out / 'javac.args'
compile_args.write_text('-J-Dfile.encoding=UTF-8\n-encoding UTF-8\n-proc:none\n-d\n' + quote(out) + '\n-cp\n' + quote(classpath) + '\n' + '\n'.join(quote(source) for source in sources), encoding='utf-8')
jdk = REPO / '.runtime/tools/jdk-17/bin'
compile_result = subprocess.run([str(jdk / 'javac.exe'), '@' + str(compile_args)], cwd=MODULE)
if compile_result.returncode:
    sys.exit(compile_result.returncode)
run_args = out / 'java.args'
run_args.write_text('-cp\n' + quote(str(out) + ';' + classpath) + '\ncn.iocoder.yudao.module.dcc.service.file.DccExactVersionReferenceUsageTest\n' + (SELECTION+'\n' if SELECTION else ''), encoding='utf-8')
result = subprocess.run([str(jdk / 'java.exe'), '@' + str(run_args)], cwd=MODULE)
sys.exit(result.returncode)
