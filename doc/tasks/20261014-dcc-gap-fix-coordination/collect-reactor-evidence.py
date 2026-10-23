"""Archive fresh, explicitly selected Surefire reports; never execute tests or alter source."""
import datetime
import hashlib
import json
import pathlib
import re
import shutil
import sys
import xml.etree.ElementTree as ET

ticket = sys.argv[1].lower()
if not re.fullmatch(r"m[0-9]+", ticket):
    raise ValueError("An explicit Maven ticket such as m11 is required")
task_root = pathlib.Path(__file__).resolve().parent
repo = task_root.parents[2]
if not (repo / "AGENTS.md").is_file():
    raise ValueError("The collector must remain under the recorded repository task directory")
receipt = json.loads((task_root / f"{ticket}-reactor-receipt.json").read_text(encoding="utf-8-sig"))
start = datetime.datetime.fromisoformat(receipt["started"]).timestamp()
end = datetime.datetime.fromisoformat(receipt["ended"]).timestamp()
archive = task_root / f"{ticket}-junit"
archive.mkdir(exist_ok=True)
classes = list(dict.fromkeys(item.split("#", 1)[0] for item in receipt["scope"].split(",")))
reports = []
for name in classes:
    matches = list((repo / "IntRuoyiBackend").glob(f"*/target/surefire-reports/TEST-*.{name}.xml"))
    if len(matches) != 1:
        raise ValueError(f"Expected one actual report for {name}: {matches}")
    report = matches[0]
    if not start <= report.stat().st_mtime <= end + 2:
        raise ValueError(f"Report is outside the actual run window: {report}")
    suite = ET.parse(report).getroot()
    item = {"class": name, **{key: int(suite.get(key, 0)) for key in ("tests", "failures", "errors", "skipped")},
            "sha256": hashlib.sha256(report.read_bytes()).hexdigest(), "problems": []}
    if item["tests"] <= 0:
        raise ValueError(f"The selected class did not execute: {name}")
    for case in suite.findall("testcase"):
        for kind in ("failure", "error"):
            problem = case.find(kind)
            if problem is not None:
                item["problems"].append({"name": case.get("name"), "kind": kind,
                                         "message": problem.get("message", ""), "detail": (problem.text or "")[:4000]})
    shutil.copy2(report, archive / report.name)
    reports.append(item)
before = json.loads((task_root / f"{ticket}-source-before.json").read_text(encoding="utf-8-sig"))
drift = []
for source in before:
    path = repo / source["path"]
    current = hashlib.sha256(path.read_bytes()).hexdigest() if path.is_file() else None
    if current != source["sha256"]:
        drift.append({"path": source["path"], "before": source["sha256"], "after": current})
totals = {key: sum(report[key] for report in reports) for key in ("tests", "failures", "errors", "skipped")}
result = {"ticket": ticket.upper(), "exitCode": receipt["exitCode"], "reports": reports, "totals": totals,
          "sourceManifestCount": len(before), "sourceDriftCount": len(drift),
          "boundary": "Fresh selected reports and recorded source paths only; no deployment, E2E, or implicit complete-scope assertion"}
for suffix, data in (("source-drift", drift), ("verification", result)):
    (task_root / f"{ticket}-{suffix}.json").write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({"ticket": ticket.upper(), "exitCode": receipt["exitCode"], "totals": totals,
                  "sourceDriftCount": len(drift), "failedClasses": [r["class"] for r in reports if r["problems"]]}, ensure_ascii=False))
