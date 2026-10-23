"""Prepare a read-only DCC migration dependency review package; never connect to a DB."""
from pathlib import Path
import argparse
import hashlib
import json
import re
import sys

parser = argparse.ArgumentParser()
parser.add_argument("--integration-root", type=Path, required=True)
parser.add_argument("--output", type=Path, required=True)
parser.add_argument("--additional-root", action="append", default=[])
args = parser.parse_args()
backend = args.integration_root.resolve() / "IntRuoyiBackend"
sys.path.insert(0, str(backend))
from script.release.release_migration_manifest import _parse_metadata, build_migration_manifest
from script.release.release_migration_policy_gate import run_migration_policy_gate

sql_root = backend / "sql/mysql"
paths = {}
for path in sql_root.rglob("20*.sql"):
    if path.stem in paths:
        raise RuntimeError(f"Ambiguous migration identity: {path.stem}")
    paths[path.stem] = path
roots = sorted(set(key for key in paths if re.match(r"2026(?:0930|100[12])_dcc_", key)) | set(args.additional_root))
if not roots:
    raise RuntimeError("No current DCC migration roots found")
order, visited, active = [], set(), set()
def visit(key):
    if key in active:
        raise RuntimeError(f"Dependency cycle: {key}")
    if key in visited:
        return
    if key not in paths:
        raise RuntimeError(f"Missing migration dependency: {key}")
    active.add(key)
    for dependency in _parse_metadata(paths[key])["dependsOn"]:
        visit(dependency)
    active.remove(key)
    visited.add(key)
    order.append(key)
for key in roots:
    visit(key)
selected = [paths[key] for key in order]
gate = run_migration_policy_gate(sql_root, sql_paths=selected)
entries = {entry["migrationId"]: entry for entry in build_migration_manifest(sql_root, sql_paths=selected)}
package = {
    "status": "prepared_not_executed",
    "databaseConnected": False,
    "runtimeSchemaVerified": False,
    "rootMigrationCount": len(roots),
    "rootMigrationIds": roots,
    "closureCount": len(order),
    "policyGate": gate["status"],
    "executionOrder": [entries[key] for key in order],
    "reviewedSql": [
        {"migrationId": key, "sha256": hashlib.sha256(paths[key].read_bytes()).hexdigest(),
         "statementsRequiringReview": [line.strip() for line in paths[key].read_text(encoding="utf-8").splitlines()
             if re.match(r"\s*(UPDATE|DELETE\s+FROM|INSERT\s+INTO|ALTER\s+TABLE|CREATE\s+TABLE|DROP\s+TABLE)", line, re.I)]}
        for key in roots
    ],
}
args.output.write_text(json.dumps(package, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(json.dumps({key: package[key] for key in (
    "status", "rootMigrationCount", "closureCount", "policyGate", "databaseConnected", "runtimeSchemaVerified")}, ensure_ascii=False))
