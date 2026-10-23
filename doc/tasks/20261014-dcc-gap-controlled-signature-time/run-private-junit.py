"""Scoped private javac/JUnit. No shared target writes; not a whole-current-source reactor gate."""
from pathlib import Path
import hashlib
import json
import subprocess
import sys

TASK = Path(__file__).resolve().parent
ROOT = TASK.parents[2]
RUN = TASK / "private-junit" / sys.argv[1]
if len(sys.argv) < 3 or RUN.exists():
    raise SystemExit("Pass a fresh run id and one or more G02 class names.")
RUN.mkdir(parents=True)
CLASSES = RUN / "classes"
CLASSES.mkdir()
cp = (ROOT / "doc/tasks/20261014-dcc-gap-delayed-effective/private-classpath.txt").read_text(encoding="utf-8").strip()
cp += ";C:/Users/D01020/.m2/repository/org/junit/platform/junit-platform-launcher/1.12.2/junit-platform-launcher-1.12.2.jar"
java_root = ROOT / "IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc"
paths = [
    "dal/dataobject/file/DccControlledFileDO.java",
    "dal/dataobject/file/DccControlledFileSignatureDO.java",
    "service/file/DccApplicationEffectiveSelection.java",
    "service/file/DccWorkflowDatePolicy.java",
    "service/file/DccWorkflowDefinitionCatalog.java",
    "service/file/DccBoundWorkflowContractResolver.java",
    "service/file/DccApplicationExecutionContractService.java",
    "service/file/DccControlledFileApprovalRouteAssigneeResolver.java",
    "service/file/DccFrozenApprovalSignatures.java",
    "service/file/DccWorkflowFileStateAudit.java",
    "service/file/DccControlledContentAdapter.java",
    "service/file/DccLifecycleEffectiveContract.java",
    "service/file/DccLifecycleEffectiveContractResolver.java",
    "service/file/DccDelayedEffectiveConfirmationEvidence.java",
    "service/file/DccDelayedEffectiveConfirmationService.java",
    "service/file/DccSignatureVerificationService.java",
    "service/file/DccSignatureVerificationServiceImpl.java",
    "service/file/DccControlledFileSignatureSubjectAdapter.java",
    "controller/admin/file/vo/DccControlledFileDelayedEffectiveConfirmReqVO.java",
    "controller/admin/file/vo/DccControlledFileDelayedEffectiveConfirmRespVO.java",
    "service/file/DccControlledFileSignatureEvidenceCreateReq.java",
    "service/file/DccControlledFileSignatureEvidence.java",
    "service/file/DccControlledFileSignatureEvidenceService.java",
    "service/file/DccControlledFileSignatureEvidenceServiceImpl.java",
    "service/file/DccElectronicSignatureManagementServiceImpl.java",
    "service/file/DccElectronicSignatureAuthorizationAuditService.java",
    "service/file/DccElectronicSignatureAuthorizationServiceImpl.java",
    "service/route/DccActionApprovalRoutePolicy.java",
    "service/route/DccFixedApprovalRoutePolicy.java",
    "service/file/listener/DccControlledFileStatusListener.java",
    "service/file/DccControlledFileSignatureBindingService.java",
    "service/file/DccControlledFileLifecycleService.java",
    "service/file/DccControlledFileFinalizationServiceImpl.java",
]
sources = [java_root / name for name in paths]
sources.extend([ROOT / rel for rel in [
    "IntRuoyiBackend/yudao-module-signature/src/main/java/cn/iocoder/yudao/module/signature/api/ElectronicSignatureSubjectAdapter.java",
    "IntRuoyiBackend/yudao-module-signature/src/main/java/cn/iocoder/yudao/module/signature/service/ElectronicSignatureServiceImpl.java",
    "IntRuoyiBackend/yudao-module-signature/src/main/java/cn/iocoder/yudao/module/signature/service/ElectronicSignatureQueryServiceImpl.java",
    "IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/service/gxpaudit/GxpAuditServiceImpl.java",
    "IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/service/task/BpmTaskServiceImpl.java",
    "IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/service/task/BpmProcessInstanceServiceImpl.java",
    "IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/service/task/DccSignedTaskActionGuard.java",
    "IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/framework/flowable/core/listener/BpmProcessInstanceEventListener.java",
    "IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/framework/flowable/core/event/BpmProcessInstanceEventPublisher.java",
    "IntRuoyiBackend/yudao-framework/yudao-spring-boot-starter-mybatis/src/main/java/cn/iocoder/yudao/framework/mybatis/core/handler/DefaultDBFieldHandler.java",
]])
sources.append(TASK / "PrivateJUnitRunner.java")
for name in sys.argv[2:]:
    clsname=name.split("#",1)[0]
    if clsname not in {"DccG02ControlledSignatureTimeTransactionTest", "DccG02DelayedFinalizationTransactionTest"}:
        raise SystemExit("Only the authorized G02 tests may run here.")
    sources.append(ROOT / "IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file" / (clsname + ".java"))
sources.append(ROOT / "IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccG02ControlledSignatureTimeTransactionTest.java")
sources=list(dict.fromkeys(sources))
for path in sources:
    if not path.is_file():
        raise SystemExit("Source dependency absent: " + str(path))
def hashes():
    return {p.relative_to(ROOT).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest() for p in sources}
before = hashes()
manifest = {"sources": before, "boundary": "Private scoped compilation of the listed current DCC source only. Existing module classpath supports unchanged interfaces/ports; not full current DCC/BPM/signature source validation. Official -am reactor still required.", "classpath": cp}
(RUN / "source-manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
def quote(arg):
    return '"' + str(arg).replace("\\", "/").replace('"', '\\"') + '"'
compile_args = ["--release", "17", "-encoding", "UTF-8", "-cp", cp, "-d", CLASSES, *sources]
(RUN / "javac.args").write_text("\n".join(quote(x) for x in compile_args) + "\n", encoding="utf-8")
compiler = ROOT / ".runtime/tools/jdk-17/bin/javac.exe"
java = ROOT / ".runtime/tools/jdk-17/bin/java.exe"
result = subprocess.run([str(compiler), "-J-Dfile.encoding=UTF-8", "@" + str(RUN / "javac.args")], cwd=ROOT, capture_output=True, encoding="utf-8", errors="strict")
(RUN / "compile.log").write_text(result.stdout + result.stderr, encoding="utf-8")
print(result.stdout + result.stderr)
after_compile=hashes()
if before != after_compile:
    (RUN / "source-drift.json").write_text(json.dumps({"stage":"compile","files":[{"path":p,"before":before[p],"after":after_compile[p]} for p in before if before[p]!=after_compile[p]]},indent=2)+"\n",encoding="utf-8")
    raise SystemExit("SOURCE_DRIFT during compile: no behavior claim.")
if result.returncode:
    print("JAVAC_BLOCKED_BEFORE_BEHAVIOR exit=" + str(result.returncode))
    raise SystemExit(result.returncode)
run_args = ["-Dfile.encoding=UTF-8", "-cp", str(CLASSES) + ";" + cp, "PrivateJUnitRunner",
            *["cn.iocoder.yudao.module.dcc.service.file." + name for name in sys.argv[2:]]]
(RUN / "java.args").write_text("\n".join(quote(x) for x in run_args) + "\n", encoding="utf-8")
result = subprocess.run([str(java), "@" + str(RUN / "java.args")], cwd=ROOT, capture_output=True, encoding="utf-8", errors="strict")
(RUN / "result.log").write_text(result.stdout + result.stderr, encoding="utf-8")
combined=result.stdout+result.stderr
summary_start=combined.find("Test run finished")
print(combined[summary_start:] if summary_start>=0 else combined[-5000:])
after_tests=hashes()
stable = before == after_tests
if not stable:
    (RUN / "source-drift.json").write_text(json.dumps({"stage":"tests","files":[{"path":p,"before":before[p],"after":after_tests[p]} for p in before if before[p]!=after_tests[p]]},indent=2)+"\n",encoding="utf-8")
(RUN / "exit.json").write_text(json.dumps({"compile": 0, "junit": result.returncode, "source_stable": stable}) + "\n", encoding="utf-8")
if not stable:
    raise SystemExit("SOURCE_DRIFT during tests: no source-stable behavior claim.")
print("PRIVATE_JUNIT_EXIT=" + str(result.returncode) + " SOURCE_STABLE")
raise SystemExit(result.returncode)
