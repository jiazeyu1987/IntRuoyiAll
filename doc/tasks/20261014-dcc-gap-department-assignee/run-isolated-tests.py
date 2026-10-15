"""G05 targeted javac/JUnit; read dependency outputs, write only this task's directory."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import sys

sys.stdout.reconfigure(encoding='utf-8')
sys.stderr.reconfigure(encoding='utf-8')

TASK = Path(__file__).resolve().parent
ROOT = TASK.parents[2]
BACKEND = ROOT / 'IntRuoyiBackend'
DCC = BACKEND / 'yudao-module-dcc'
PACKAGE = 'cn.iocoder.yudao.module.dcc.service.file.'
RUNNER = '''import java.io.PrintWriter;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
public class G05JUnitRunner {
  public static void main(String[] args) {
    var request = LauncherDiscoveryRequestBuilder.request();
    for (String name : args) request.selectors(selectClass(name));
    var listener = new SummaryGeneratingListener();
    var launcher = LauncherFactory.create(); launcher.registerTestExecutionListeners(listener);
    launcher.execute(request.build());
    var summary = listener.getSummary();
    summary.printTo(new PrintWriter(System.out, true));
    summary.printFailuresTo(new PrintWriter(System.out, true));
    if (summary.getTestsFoundCount() == 0 || summary.getTestsFailedCount() != 0
        || summary.getTestsSkippedCount() != 0 || summary.getTestsAbortedCount() != 0
        || summary.getContainersFailedCount() != 0) System.exit(1);
  }
}'''


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--phase', required=True)
    parser.add_argument('--regression', action='store_true')
    args = parser.parse_args()
    output = TASK / ('isolated-' + args.phase)
    output.mkdir(exist_ok=False)
    classes = output / 'classes'
    classes.mkdir()
    runner = output / 'G05JUnitRunner.java'
    runner.write_text(RUNNER, encoding='utf-8')
    dependencies = (DCC / 'target/dcc-test.classpath').read_text(encoding='utf-8-sig').strip().split(';')
    launcher = Path.home() / '.m2/repository/org/junit/platform/junit-platform-launcher/1.12.2/junit-platform-launcher-1.12.2.jar'
    targets = [DCC / 'target/classes'] + sorted(BACKEND.glob('*/target/classes')) + sorted(BACKEND.glob('yudao-framework/*/target/classes'))
    dependencies = list(dict.fromkeys(str(path) for path in [*targets, launcher, *dependencies] if str(path)))
    missing = [entry for entry in dependencies if not Path(entry).exists()]
    if missing:
        raise FileNotFoundError('Dependency classpath is incomplete: ' + repr(missing))
    main_root = DCC / 'src/main/java/cn/iocoder/yudao/module/dcc'
    test_root = DCC / 'src/test/java/cn/iocoder/yudao/module/dcc/service/file'
    sources = [main_root / 'service/file/DccWorkflowSignoffAssignmentService.java',
               main_root / 'service/file/DccSignoffAssignmentContext.java',
               main_root / 'controller/admin/file/vo/DccSignoffAssignmentReqVO.java',
               test_root / 'DccWorkflowSignoffAssignmentTest.java']
    tests = ['DccWorkflowSignoffAssignmentTest']
    if args.regression:
        tests += ['DccRelationStageSnapshotServiceTest', 'DccWorkflowRelationStageIntegrationTest']
        sources += [test_root / (name + '.java') for name in tests[1:]]
        sources += [test_root / 'DccRelationStageH2Schema.java']
    manifest = {str(path.relative_to(ROOT)): hashlib.sha256(path.read_bytes()).hexdigest() for path in sources}
    (output / 'source-hashes.json').write_text(json.dumps(manifest, indent=2), encoding='utf-8')
    cp = ';'.join(dependencies)
    compile_args = ['-encoding', 'UTF-8', '-cp', cp, '-sourcepath', '', '-d', str(classes), str(runner), *map(str, sources)]
    argfile = output / 'compile.args'
    argfile.write_text('\n'.join('"' + value.replace('\\', '/') + '"' for value in compile_args), encoding='utf-8')
    compile_result = subprocess.run([shutil.which('javac'), '-J-Dfile.encoding=UTF-8', '@' + str(argfile)], cwd=ROOT, capture_output=True)
    (output / 'compile.log').write_bytes(compile_result.stdout + compile_result.stderr)
    print('javac exit=' + str(compile_result.returncode), flush=True)
    if compile_result.returncode:
        print((compile_result.stdout + compile_result.stderr).decode('utf-8', errors='replace'))
        return compile_result.returncode
    run_args = ['-Dfile.encoding=UTF-8', '-cp', str(classes) + ';' + cp, 'G05JUnitRunner', *[PACKAGE + name for name in tests]]
    runfile = output / 'run.args'
    runfile.write_text('\n'.join('"' + value.replace('\\', '/') + '"' for value in run_args), encoding='utf-8')
    result = subprocess.run([shutil.which('java'), '@' + str(runfile)], cwd=DCC, capture_output=True)
    (output / 'run.log').write_bytes(result.stdout + result.stderr)
    text = (result.stdout + result.stderr).decode('utf-8', errors='replace')
    print(text[-20000:])
    print('JUnit exit=' + str(result.returncode))
    return result.returncode


if __name__ == '__main__':
    sys.exit(main())
