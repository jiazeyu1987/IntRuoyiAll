"""Require an explicit migration decision for menu/permission development.

Uses the cumulative diff since adoption, so a later release cannot hide an
unmigrated change by comparing only the last commit. No database is contacted.
"""
from __future__ import annotations

import argparse
from functools import lru_cache
import hashlib
import json
from pathlib import Path
import re
import subprocess
import sys
import tempfile
import time

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


def read_git_objects(root: Path, expressions: list[str]) -> dict[str, str]:
    """Read blobs in one process; NUL framing also supports newlines in paths."""
    expressions = list(dict.fromkeys(expressions))
    if not expressions:
        return {}
    result = subprocess.run(["git", "-C", str(root), "cat-file", "--batch", "-Z"],
                            input="".join(value + "\0" for value in expressions).encode("utf-8"),
                            capture_output=True)
    if result.returncode:
        raise GateError(result.stderr.decode("utf-8", errors="replace").strip())
    contents, offset = {}, 0
    for expression in expressions:
        end = result.stdout.find(b"\0", offset)
        if end < 0:
            raise GateError(f"Incomplete Git batch header: {expression}")
        header = result.stdout[offset:end].split()
        if len(header) != 3 or header[1] != b"blob" or not header[2].isdigit():
            raise GateError(f"Cannot read Git blob {expression}: {result.stdout[offset:end]!r}")
        start, size = end + 1, int(header[2])
        offset = start + size
        if result.stdout[offset:offset + 1] != b"\0":
            raise GateError(f"Incomplete Git batch content: {expression}")
        contents[expression] = result.stdout[start:offset].decode("utf-8").removeprefix("\ufeff").replace("\r\n", "\n")
        offset += 1
    if offset != len(result.stdout):
        raise GateError("Unexpected trailing Git batch output")
    return contents


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
    started = time.perf_counter()
    snapshot = "index" if staged else revision or "worktree"
    print(f"MENU-MIGRATION: checking {snapshot}; collecting cumulative changes...", file=sys.stderr, flush=True)
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

    sources = sorted(path for path in changed - {""}
                     if path.startswith(("IntRuoyiFronted/src/", "IntRuoyiBackend/"))
                     and Path(path).suffix in {".ts", ".js", ".vue", ".tsx", ".java"})
    expressions = [baseline + ":" + path for path in sources if path in old_paths]
    if staged or revision:
        expressions.extend((revision or "") + ":" + path for path in [*sources, CONTRACT] if path in current_paths)
    print(f"MENU-MIGRATION: reading {len(sources)} source files ({len(expressions)} Git blobs in batch)...",
          file=sys.stderr, flush=True)
    blobs = read_git_objects(root, expressions)

    @lru_cache(maxsize=None)
    def read(path: str, old: bool = False) -> str:
        if path not in (old_paths if old else current_paths):
            return ""
        if old or staged or revision:
            expression = (baseline if old else revision or "") + ":" + path
            if expression not in blobs:
                blobs.update(read_git_objects(root, [expression]))
            return blobs[expression]
        file = root / path
        return file.read_text(encoding="utf-8-sig") if file.is_file() else ""

    findings = []
    for path in sources:
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
    print(f"MENU-MIGRATION: validating {len(findings)} menu/permission changes...", file=sys.stderr, flush=True)
    errors, required = [], set()
    migration_cache = {}
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
            cache_key = tuple(migrations)
            if cache_key not in migration_cache:
                migration_cache[cache_key] = validate_migrations(root, read, current_paths, migrations)
            closure, sql = migration_cache[cache_key]
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
    print(f"MENU-MIGRATION: passed in {time.perf_counter() - started:.2f}s", file=sys.stderr, flush=True)
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
    catalog_path = SQL_ROOT + "release-sources.json"
    if catalog_path in paths:
        catalog = json.loads(read(catalog_path))
        if catalog.get("schemaVersion") != "release-sql-sources/v2":
            raise GateError("Unsupported release source catalog")
        for entry in catalog.get("historicalMigrations", []):
            name, original = entry["file"], entry["original"]
            if Path(name).name != name or original != "historical/" + name:
                raise GateError("Invalid historical migration source: " + name)
            expected = {SQL_ROOT + name, SQL_ROOT + original}
            if set(available.get(Path(name).stem, [])) != expected:
                raise GateError("Historical migration files missing or ambiguous: " + name)
            # The release package executes the declared immutable original.
            # The reviewed root file is retained as source evidence, not a second migration.
            available[Path(name).stem] = [SQL_ROOT + original]
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
