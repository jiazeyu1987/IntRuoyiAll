"""Require an explicit migration decision for menu/permission development.

Uses the cumulative diff since adoption, so a later release cannot hide an
unmigrated change by comparing only the last commit. No database is contacted.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import sys
import tempfile

BASELINE = "32ff79dec7bee877df7a65d896c6143ae6d53523"
CONTRACT = "config/menu-migrations.json"
SQL_ROOT = "IntRuoyiBackend/sql/mysql/"
PERMISSION = re.compile(r"['\"]([\w-]+(?::[\w-]+){2,})['\"]")
# Keep quoted strings intact while removing comments (including URLs in strings).
LEXEMES = re.compile(r"'(?:\\.|[^'\\])*'|\"(?:\\.|[^\"\\])*\"|`(?:\\.|[^`\\])*`|/\*[\s\S]*?\*/|//[^\n]*|<!--[^>]*-->")


class GateError(RuntimeError):
    pass


def git(root: Path, *args: str) -> str:
    result = subprocess.run(["git", "-C", str(root), *args], capture_output=True)
    if result.returncode:
        raise GateError(result.stderr.decode("utf-8", errors="replace").strip())
    return result.stdout.decode("utf-8")


def uncomment(source: str) -> str:
    return LEXEMES.sub(lambda m: "" if m[0].startswith(("//", "/*", "<!--")) else m[0], source)


def signals(path: str, before: str, after: str) -> list[dict[str, str]]:
    if not after:
        return []
    frontend = path.startswith("IntRuoyiFronted/src/") and Path(path).suffix in {".ts", ".js", ".vue", ".tsx"}
    backend = path.startswith("IntRuoyiBackend/") and "/src/main/java/" in path and path.endswith(".java")
    if not (frontend or backend):
        return []
    old, new = uncomment(before), uncomment(after)
    found = [{"source": path, "kind": "permission", "value": value}
             for value in sorted(set(PERMISSION.findall(new)) - set(PERMISSION.findall(old)))]
    if frontend and path.startswith("IntRuoyiFronted/src/router/") and old.strip() != new.strip():
        # ponytail: fingerprint the route file instead of maintaining a TypeScript parser.
        # Every route edit needs a fresh decision; detail-only routes use a reason.
        found.append({"source": path, "kind": "router", "value": hashlib.sha256(new.encode()).hexdigest()})
    if not before and path.startswith("IntRuoyiFronted/src/views/") and path.endswith("/index.vue"):
        found.append({"source": path, "kind": "page", "value": path.split("/src/views/", 1)[1][:-4]})
    return found


def check(root: Path, *, staged: bool = False, revision: str | None = None, baseline: str = BASELINE,
          packaged_sql: set[str] | None = None) -> dict:
    git(root, "cat-file", "-e", baseline + "^{commit}")
    old_paths = set(git(root, "ls-tree", "-r", "--name-only", "-z", baseline).split("\0"))
    current_paths = set(git(root, "ls-tree", "-r", "--name-only", "-z", revision).split("\0")) if revision else set(git(root, "ls-files", "--cached", "-z").split("\0"))
    diff_args = ["diff", "--name-only", "--no-renames", "-z"]
    if staged:
        diff_args.append("--cached")
    changed = set(git(root, *diff_args, baseline, *([revision] if revision else []), "--").split("\0"))
    if not staged and not revision:
        untracked = set(git(root, "ls-files", "--others", "--exclude-standard", "-z").split("\0"))
        current_paths |= untracked
        changed |= untracked

    def read(path: str, old: bool = False) -> str:
        if path not in (old_paths if old else current_paths):
            return ""
        if old or staged or revision:
            return git(root, "show", (baseline if old else revision or "") + ":" + path).removeprefix("\ufeff").replace("\r\n", "\n")
        file = root / path
        return file.read_text(encoding="utf-8-sig") if file.is_file() else ""

    findings = []
    for path in sorted(changed - {""}):
        if path.startswith(("IntRuoyiFronted/src/", "IntRuoyiBackend/")) and Path(path).suffix in {".ts", ".js", ".vue", ".tsx", ".java"}:
            findings.extend(signals(path, read(path, True), read(path)))
    contract_text = read(CONTRACT)
    if not contract_text:
        raise GateError(f"Missing {CONTRACT}; stage it together with the source changes.")
    decisions = json.loads(contract_text)["changes"]
    indexed = {}
    for item in decisions:
        key = tuple(item[name] for name in ("source", "kind", "value"))
        if key in indexed:
            raise GateError(f"Duplicate menu migration decision: {key}")
        indexed[key] = item
    errors, required = [], set()
    for finding in findings:
        key = tuple(finding[name] for name in ("source", "kind", "value"))
        decision = indexed.get(key)
        if decision is None:
            errors.append("Missing migration decision: " + json.dumps(finding, ensure_ascii=False))
            continue
        migrations, reason = decision.get("migrations", []), decision.get("reason", "").strip()
        if bool(migrations) == bool(reason):
            errors.append(f"{key}: supply migrations OR a concrete no-migration reason")
            continue
        if reason:
            print(f"MENU-MIGRATION EXEMPT: {key}: {reason}")
            continue
        try:
            closure, sql = validate_migrations(root, read, current_paths, migrations)
            if any(read(path, True) for path in migrations):
                raise GateError("Link a new migration, not an unchanged historical SQL file")
            if finding["kind"] != "router" and not re.search(
                    r"['\"]" + re.escape(finding["value"]) + r"['\"]", sql):
                raise GateError(f"SQL does not contain the exact {finding['kind']} value {finding['value']}")
            for table in ("system_menu", "system_role_menu", "system_tenant_package"):
                if not re.search(r"\b(?:INSERT\s+(?:IGNORE\s+)?INTO|REPLACE\s+INTO|UPDATE)\s+`?" + table + r"`?\b", sql, re.I):
                    raise GateError(f"SQL must write {table}; otherwise explain reuse with a reason")
            if packaged_sql is not None and not closure <= packaged_sql:
                raise GateError("Migrations omitted from release package: " + ", ".join(sorted(closure - packaged_sql)))
            required |= closure
        except GateError as exc:
            errors.append(f"{key}: {exc}")
    if errors:
        raise GateError("\n".join(errors))
    return {"status": "passed", "signalCount": len(findings), "requiredSql": sorted(required)}


def validate_migrations(root: Path, read, paths: set[str], migrations: list[str]) -> tuple[set[str], str]:
    # Reuse the existing release metadata/dependency validator, including for index snapshots.
    sys.path.insert(0, str(root / "IntRuoyiBackend"))
    from script.release.release_migration_manifest import METADATA_PATTERN, _parse_metadata
    from script.release.release_migration_policy_gate import run_migration_policy_gate, MigrationPolicyError
    available = {}
    for path in paths:
        if path.startswith(SQL_ROOT) and path.endswith(".sql"):
            available.setdefault(Path(path).stem, []).append(path)
    closure, visiting = set(), set()
    with tempfile.TemporaryDirectory(prefix="menu-migration-") as temporary:
        sql_root = Path(temporary)

        def collect(path: str) -> None:
            if path in visiting:
                raise GateError(f"Migration dependency cycle: {path}")
            if path in closure:
                return
            if path not in paths or not path.startswith(SQL_ROOT) or ".." in Path(path).parts:
                raise GateError(f"Migration missing or outside {SQL_ROOT}: {path}")
            source = read(path)
            if not METADATA_PATTERN.search(source):
                raise GateError(f"Missing release-migration metadata: {path}")
            target = sql_root / path[len(SQL_ROOT):]
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(source, encoding="utf-8")
            metadata = _parse_metadata(target)
            if set(metadata["allowedEnvironments"]) != {"test", "prod", "backup"}:
                raise GateError(f"Menu migration must support test,prod,backup: {path}")
            visiting.add(path)
            for dependency in metadata["dependsOn"]:
                matches = available.get(dependency, [])
                if len(matches) != 1:
                    raise GateError(f"Dependency missing or ambiguous: {dependency}")
                collect(matches[0])
            visiting.remove(path)
            closure.add(path)

        try:
            for path in migrations:
                collect(path)
            result = run_migration_policy_gate(sql_root, sql_paths=[sql_root / p[len(SQL_ROOT):] for p in closure])
            if any(item["type"] not in {"menu", "permission", "schema", "data", "config", "seed"}
                   for item in result["migrations"]):
                raise GateError("Evidence-only SQL cannot satisfy menu migration coverage")
        except (MigrationPolicyError, ValueError, RuntimeError) as exc:
            raise GateError(str(exc)) from exc
    sql = "\n".join(read(path) for path in sorted(closure))
    # SQL comment text is not evidence of an executable menu or grant change.
    sql = re.sub(r"'(?:''|\\.|[^'\\])*'|`[^`]*`|/\*[\s\S]*?\*/|--[^\n]*|#[^\n]*",
                 lambda m: "" if m[0].startswith(("--", "#", "/*")) else m[0], sql)
    return closure, sql


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", type=Path, default=Path(__file__).resolve().parents[2])
    snapshot = parser.add_mutually_exclusive_group()
    snapshot.add_argument("--staged", action="store_true", help="Read source, decisions and SQL from Git index")
    snapshot.add_argument("--revision", help="Read a committed snapshot (used by pre-push and CI)")
    parser.add_argument("--packaged-sql", type=Path, help="JSON array of actual package SQL paths, relative to backend root")
    args = parser.parse_args()
    try:
        packaged = None if args.packaged_sql is None else {
            "IntRuoyiBackend/" + path for path in json.loads(args.packaged_sql.read_text(encoding="utf-8-sig"))}
        print(json.dumps(check(args.repo.resolve(), staged=args.staged, revision=args.revision, packaged_sql=packaged), ensure_ascii=False))
        return 0
    except (GateError, ValueError, KeyError, TypeError, OSError, ImportError) as exc:
        print(f"MENU-MIGRATION FAILED: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
