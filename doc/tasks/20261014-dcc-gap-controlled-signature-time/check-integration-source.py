"""Read-only source availability checkpoint. This is not a G02 behavior test."""
from pathlib import Path
from datetime import datetime, timezone
import hashlib
import json
import re
import sys

TASK = Path(__file__).resolve().parent
ROOT = TASK.parents[2]
BASELINE = json.loads((TASK / "a07-integration-baseline.json").read_text(encoding="utf-8"))
OUT = TASK / sys.argv[1] if len(sys.argv) == 2 else None
if OUT is None or OUT.parent.resolve() != TASK or OUT.exists():
    raise SystemExit("Specify a new JSON checkpoint filename inside the G02 task directory.")
entries = []
for previous in BASELINE["files"]:
    path = ROOT / previous["path"]
    item = {"path": previous["path"], "owner": previous["owner"]}
    if path.is_file():
        raw = path.read_bytes()
        source = raw.decode("utf-8").replace("\r\n", "\n").replace("\r", "\n")
        item.update(state="PRESENT", raw_sha256=hashlib.sha256(raw).hexdigest(),
                    sha256=hashlib.sha256(source.encode("utf-8")).hexdigest())
        item["changed_since_a07"] = item["sha256"] != previous.get("sha256")
        if path.name == "DccDelayedEffectiveConfirmationEvidence.java":
            item["public_declarations_for_review"] = [
                {"line": n, "text": line.strip()}
                for n, line in enumerate(source.splitlines(), 1)
                if re.search(r"\bpublic\b|\bVerifiedConfirmation\b", line)
            ]
    else:
        item.update(state="ABSENT", changed_since_a07=previous["state"] != "ABSENT")
    entries.append(item)
result = {
    "phase": "A11", "collected_at_utc": datetime.now(timezone.utc).isoformat(),
    "normalization": BASELINE["normalization"],
    "boundary": "Availability and source identity only. Presence/declarations do not prove completion, signature verification or behavior. No Java compilation or execution.",
    "files": entries,
}
OUT.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
present_changes = [e["path"] for e in entries if e["state"] == "PRESENT" and e["changed_since_a07"]]
dependencies_absent = [e["path"] for e in entries if e["state"] == "ABSENT" and "G01" in e["owner"]]
print(json.dumps({"changed_present_sources_since_a07": present_changes,
                  "absent_g01_dependencies": dependencies_absent,
                  "checkpoint": OUT.relative_to(ROOT).as_posix()}, ensure_ascii=False, indent=2))
print("Behavior RED/GREEN: NOT_RUN. Source availability is not a completion proof.")
