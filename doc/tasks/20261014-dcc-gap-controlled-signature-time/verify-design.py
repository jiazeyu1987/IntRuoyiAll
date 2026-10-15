"""Task-owned documentation checks only; never a G02 behavior test."""
from pathlib import Path
import hashlib
import json
import re

TASK = Path(__file__).resolve().parent
ROOT = TASK.parents[2]
required = {
    "task.md": ["## Task Goal", "## Milestones", "## Expected Verification", "## Current Status", "## 设计约束检查", "## BDD", "## Cleanup Keep"],
    "execution-log.md": ["DEPENDENCY_ONLY", "int_qms"],
    "design.md": ["## 2. 当前完整调用链", "## 4. G01 -> G02", "## 5. 真实 RED/GREEN/REGRESSION", "均未重跑"],
    "integration-notes.md": ["请求下一阶段", "共享依赖", "必要最小差异", "验收影响"],
    "changed-files.md": ["A13 当前授权四文件", "verify-design.py"],
    "review-ready.md": ["尚未实现/验证", "软件范围正式通过", "文件归还", "工程收尾 blocked", "21/21 PASS"],
    "verification-report.md": ["NOT_RUN", "唯一 Maven", "未执行"],
    "c01-end-event-test-design.md": ["WRITE_GRANTED_FROZEN", "C02", "实际 S 保持", "LAST_APPROVER", "DccG02ControlledSignatureTimeTransactionTest", "DccG02DelayedFinalizationTransactionTest", "M02", "未写 Java"],
    "a07-interface-composition.md": ["WRITE_GRANTED", "ABSENT", "唯一 Evidence", "Lifecycle 逐方法", "Finalization 逐方法", "私有 javac/JUnit", "原精度", "两专属真实测试"],
    "a13-implementation-evidence.md": ["A13", "14/14PASS", "a13-red-time4", "SOURCE_STABLE", "C05", "MICROS", "不能扩大的证据边界"],
    "public-command-verification.md": ["2/2 PASS", "public-command-11", "46", "BpmTaskServiceImpl.approveTask", "DefaultDBFieldHandler", "剩余门禁"],
    "public-guards-verification.md": ["public-guards-02", "3/3PASS", "SOURCE_DRIFT", "producer", "M05"],
    "public-training-verification.md": ["public-training-01", "2/2PASS", "SOURCE_STABLE", "培训记录上传公开入口", "下一步"],
}
for name, markers in required.items():
    source = (TASK / name).read_text(encoding="utf-8")
    assert source and "\ufffd" not in source, name
    for marker in markers:
        assert marker in source, (name, marker)
task = (TASK / "task.md").read_text(encoding="utf-8")
status = task.split("## Current Status", 1)[1].strip().splitlines()[0]
assert status.startswith("blocked"), status
assert "软件" in status and "冻结交还" in status
for n in range(1, 14):
    assert f"BDD G02-{n:02d}" in task
keep = task.split("## Cleanup Keep", 1)[1]
for asset in TASK.iterdir():
    if asset.is_file():
        assert f"- {asset.relative_to(ROOT).as_posix()}" in keep, asset.name
changed = (TASK / "changed-files.md").read_text(encoding="utf-8")
for asset in TASK.iterdir():
    if asset.is_file():
        assert asset.relative_to(ROOT).as_posix() in changed, asset.name
baseline = json.loads((TASK / "baseline-fingerprints.json").read_text(encoding="utf-8"))
followup = json.loads((TASK / "c01-followup-baseline.json").read_text(encoding="utf-8"))
a07 = json.loads((TASK / "a07-integration-baseline.json").read_text(encoding="utf-8"))
checkpoint = json.loads((TASK / "a11-source-checkpoint.json").read_text(encoding="utf-8"))
assert baseline["branch"] == "int_qms"
assert followup["branch"] == "int_qms"
assert a07["branch"] == "int_qms" and a07["phase"] == "A07"
assert len(a07["files"]) == 16
for item in a07["files"]:
    assert item["state"] in {"PRESENT", "ABSENT"}, item
    if item["state"] == "PRESENT":
        assert re.fullmatch(r"[a-f0-9]{64}", item["sha256"])
        assert re.fullmatch(r"[a-f0-9]{64}", item["raw_sha256"])
assert baseline["normalization"] == "UTF-8 decoded text, CRLF/CR normalized to LF, SHA-256 of UTF-8 bytes"
# Previous phase source snapshots are retained historical evidence. G01 source writes
# are authorized and must not be reported as product failure or unchanged source PASS.
assert checkpoint["phase"] == "A11" and len(checkpoint["files"]) == 16
for item in checkpoint["files"]:
    if item["state"] == "PRESENT":
        assert re.fullmatch(r"[a-f0-9]{64}", item["sha256"])
        assert re.fullmatch(r"[a-f0-9]{64}", item["raw_sha256"])
    else:
        assert item["state"] == "ABSENT"
for item in baseline["files"] + followup["files"]:
    path = ROOT / item["path"]
    assert path.is_file(), item["path"]
    path.read_text(encoding="utf-8")
    assert re.fullmatch(r"[a-f0-9]{64}", item["sha256"])
    assert all(anchor in item["anchor_lines"] for anchor in item["anchors"])
current = json.loads((TASK / "public-training-ready.json").read_text(encoding="utf-8"))
assert len(current["four_authorized_sources"]) == 4
for item in current["four_authorized_sources"]:
    assert hashlib.sha256((ROOT / item["path"]).read_bytes()).hexdigest() == item["raw_sha256"], item["path"]
    if "/main/" in item["path"]:
        assert item["changed_since_a13"] is False
assert len(current["compiled_current_sources"]) == 46
result = json.loads((TASK / "private-junit/a13-final14/exit.json").read_text(encoding="utf-8"))
assert result == {"compile": 0, "junit": 0, "source_stable": True}
output = (TASK / "private-junit/a13-final14/result.log").read_text(encoding="utf-8")
assert re.search(r"\[\s*14 tests successful", output)
assert re.search(r"\[\s*0 tests failed", output)
public_result=json.loads((TASK / "private-junit/public-command-11/exit.json").read_text(encoding="utf-8"))
assert public_result == {"compile": 0, "junit": 0, "source_stable": True}
public_output=(TASK / "private-junit/public-command-11/result.log").read_text(encoding="utf-8")
assert re.search(r"\[\s*2 tests successful",public_output) and re.search(r"\[\s*0 tests failed",public_output)
for phase,count in [("public-guards-02",3),("public-training-01",2)]:
    actual_exit=json.loads((TASK / "private-junit" / phase / "exit.json").read_text(encoding="utf-8"))
    assert actual_exit=={"compile":0,"junit":0,"source_stable":True}
    actual_log=(TASK / "private-junit" / phase / "result.log").read_text(encoding="utf-8")
    assert re.search(r"\[\s*"+str(count)+r" tests successful",actual_log) and re.search(r"\[\s*0 tests failed",actual_log)
assert json.loads((TASK / "private-junit/public-guards-01/exit.json").read_text())["source_stable"] is False
pre_m07=json.loads((TASK / "pre-m07-training-source.json").read_text(encoding="utf-8"))
assert len(pre_m07["files"])==46 and not pre_m07["drift"]
assert all(item["unchanged"] for item in pre_m07["files"])
formal=json.loads((TASK / "m07-formal-g02-evidence.json").read_text(encoding="utf-8"))
handback=json.loads((TASK / "g02-software-handback.json").read_text(encoding="utf-8"))
assert handback["software_status"]=="ROOT_REVIEW_ACCEPTED_FROZEN" and handback["g02_total"]==21
assert formal["g02_total"]==21 and formal["root_receipt"]["exitCode"]==1
for test in formal["g02_tests"]:
    assert test["counts"]["failures"]==test["counts"]["errors"]==test["counts"]["skipped"]==0
    assert hashlib.sha256((ROOT / test["root_archived_xml"]).read_bytes()).hexdigest()==test["raw_xml_sha256"]
for source in handback["files"]:
    assert hashlib.sha256((ROOT / source["path"]).read_bytes()).hexdigest()==source["raw_sha256"]
joint=json.loads((TASK / "m08-formal-joint-evidence.json").read_text(encoding="utf-8"))
assert joint["root_receipt"]["exitCode"]==0 and joint["total_tests"]==55 and joint["g02_tests"]==21
assert joint["g02_own_source_drift"]==0 and len(joint["classes"])==8
for suite in joint["classes"]:
    assert suite["counts"]["failures"]==suite["counts"]["errors"]==suite["counts"]["skipped"]==0
    assert hashlib.sha256((ROOT / suite["archive"]).read_bytes()).hexdigest()==suite["raw_xml_sha256"]
print(f"PASS: {len(required)} UTF-8 task documents, 13 BDD scenarios, asset scope/keep; four current authorized source hashes; independent original14/public2/guards3/training2 stored provenance; first drift retained.")
print("Formal latest M08: eight classes55 PASS/0F/E/skip, BUILD SUCCESS; G02 two classes21 PASS, four sources frozen0drift. M07 history retained; no runtime/whole12/final-project-freeze/Git completion claim.")
