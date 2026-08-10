import argparse
import hashlib
import json
import subprocess
from pathlib import Path


TASK_DIR = Path(__file__).resolve().parent
REPO_ROOT = TASK_DIR.parents[2]
SOURCE_ROLES_PATH = TASK_DIR / "all-role-source-definitions.json"
LOCAL_AUDIT_PATH = TASK_DIR / "all-role-audit-local.json"

SSH_TARGET = "root@172.30.30.58"
SSH_OPTIONS = [
    "-o",
    "BatchMode=yes",
    "-o",
    "ConnectTimeout=10",
    "-o",
    "ConnectionAttempts=1",
    "-o",
    "ServerAliveInterval=10",
    "-o",
    "ServerAliveCountMax=3",
    "-o",
    "StrictHostKeyChecking=no",
]

DB_NAME = "ruoyi-vue-pro"
MYSQL_CONTAINER = "intruoyi-mysql"
REDIS_CONTAINER = "intruoyi-redis"
REMOTE_APP_DIR = "/opt/intruoyi/runtime"
LOCK_ID = "test-tenant1-all-role-permission-sync-20260807T2008"
RELEASE_TAG = "manual-test-tenant1-all-role-permission-sync-20260807"

SNAPSHOT_TABLES = [
    "system_role_category",
    "system_role",
    "system_menu",
    "system_role_menu",
    "system_user_role",
    "system_tenant_package",
    "infra_release_migration",
    "infra_release_operation_lock",
]


def sql_string(value: object) -> str:
    text = "" if value is None else str(value)
    return "'" + text.replace("\\", "\\\\").replace("'", "''") + "'"


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def source_role_codes() -> list[str]:
    roles = json.loads(SOURCE_ROLES_PATH.read_text(encoding="utf-8"))
    return sorted({str(role["code"]) for role in roles})


def desired_role_permissions() -> list[tuple[str, str]]:
    local = json.loads(LOCAL_AUDIT_PATH.read_text(encoding="utf-8"))
    pairs = {
        (str(row["roleCode"]), str(row["permission"]))
        for row in local["roleMenus"]
        if int(row["menuStatus"]) == 0 and str(row.get("permission") or "").strip()
    }
    return sorted(pairs)


def run_ssh(script: str, *, capture_binary: bool = False) -> subprocess.CompletedProcess:
    if "\r" in script:
        raise ValueError("remote script must use LF-only newlines")
    command = ["ssh", *SSH_OPTIONS, SSH_TARGET, "bash", "-s"]
    return subprocess.run(
        command,
        input=script.encode("utf-8"),
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
    )


def remote_mysql_script(sql: str) -> str:
    return f"""set -eu
test -d {sql_string(REMOTE_APP_DIR)}
docker ps --format '{{{{.Names}}}}' | grep -Fx {sql_string(MYSQL_CONTAINER)} >/dev/null
docker exec -i -e MYSQL_DATABASE={sql_string(DB_NAME)} {sql_string(MYSQL_CONTAINER)} sh -lc 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot --default-character-set=utf8mb4 --batch --raw --skip-column-names "$MYSQL_DATABASE"' <<'SQL'
{sql}
SQL
"""


def run_mysql_json(sql: str) -> dict:
    result = run_ssh(remote_mysql_script(sql))
    stdout = result.stdout.decode("utf-8", errors="replace")
    stderr = result.stderr.decode("utf-8", errors="replace")
    if result.returncode != 0:
        raise SystemExit(f"remote mysql failed with exit {result.returncode}\nSTDOUT:\n{stdout}\nSTDERR:\n{stderr}")
    lines = [line for line in stdout.splitlines() if line.strip()]
    if not lines:
        raise SystemExit(f"remote mysql returned no output\nSTDERR:\n{stderr}")
    return json.loads(lines[-1])


def values_clause(rows: list[tuple[str, ...]]) -> str:
    return ",\n".join("(" + ", ".join(sql_string(value) for value in row) + ")" for row in rows)


def build_validation_sql() -> str:
    role_codes = source_role_codes()
    desired_pairs = desired_role_permissions()
    role_values = values_clause([(code,) for code in role_codes])
    pair_values = values_clause(desired_pairs)
    return f"""
SET SESSION group_concat_max_len = 100000000;

DROP TEMPORARY TABLE IF EXISTS tmp_sync_source_roles;
CREATE TEMPORARY TABLE tmp_sync_source_roles (
  code varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (code)
);
INSERT INTO tmp_sync_source_roles (code) VALUES
{role_values};

DROP TEMPORARY TABLE IF EXISTS tmp_sync_source_permissions;
CREATE TEMPORARY TABLE tmp_sync_source_permissions (
  role_code varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  permission varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (role_code, permission)
);
INSERT INTO tmp_sync_source_permissions (role_code, permission) VALUES
{pair_values};

SELECT DATE_FORMAT(NOW(), '%Y-%m-%dT%H:%i:%s') INTO @captured_at;
SELECT COUNT(*) INTO @source_role_count FROM tmp_sync_source_roles;
SELECT COUNT(*) INTO @source_permission_pair_count FROM tmp_sync_source_permissions;
SELECT COUNT(*) INTO @target_resolved_source_role_count
FROM system_role r
JOIN tmp_sync_source_roles s ON s.code = r.code
WHERE r.tenant_id = 1 AND r.deleted = b'0';
SELECT COUNT(*) INTO @duplicate_target_source_role_codes
FROM (
  SELECT r.code
  FROM system_role r
  JOIN tmp_sync_source_roles s ON s.code = r.code
  WHERE r.tenant_id = 1 AND r.deleted = b'0'
  GROUP BY r.code
  HAVING COUNT(*) > 1
) duplicate_role;
SELECT COUNT(*) INTO @tenant1_active_role_count
FROM system_role
WHERE tenant_id = 1 AND deleted = b'0';
SELECT COUNT(*) INTO @target_only_active_role_count
FROM system_role r
LEFT JOIN tmp_sync_source_roles s ON s.code = r.code
WHERE r.tenant_id = 1 AND r.deleted = b'0' AND s.code IS NULL;
SELECT COUNT(*) INTO @missing_source_permission_count
FROM tmp_sync_source_permissions desired
JOIN system_role r
  ON r.code = desired.role_code
 AND r.tenant_id = 1
 AND r.deleted = b'0'
WHERE NOT EXISTS (
  SELECT 1
  FROM system_role_menu rm
  JOIN system_menu m
    ON m.id = rm.menu_id
   AND m.deleted = b'0'
   AND m.status = 0
  WHERE rm.role_id = r.id
    AND rm.tenant_id = 1
    AND rm.deleted = b'0'
    AND m.permission = desired.permission
);
SELECT COUNT(*) INTO @extra_source_role_permission_count
FROM system_role r
JOIN tmp_sync_source_roles s ON s.code = r.code
JOIN system_role_menu rm
  ON rm.role_id = r.id
 AND rm.tenant_id = r.tenant_id
 AND rm.deleted = b'0'
JOIN system_menu m
  ON m.id = rm.menu_id
 AND m.deleted = b'0'
 AND m.status = 0
LEFT JOIN tmp_sync_source_permissions desired
  ON desired.role_code = r.code
 AND desired.permission = m.permission
WHERE r.tenant_id = 1
  AND r.deleted = b'0'
  AND m.permission <> ''
  AND desired.permission IS NULL;
SELECT COUNT(*) INTO @tenant1_user_role_count
FROM system_user_role
WHERE tenant_id = 1;
SELECT COUNT(*) INTO @tenant1_active_user_role_count
FROM system_user_role
WHERE tenant_id = 1 AND deleted = b'0';
SELECT SHA2(COALESCE(GROUP_CONCAT(CONCAT_WS(':', user_id, role_id, HEX(deleted), tenant_id) ORDER BY user_id, role_id, HEX(deleted) SEPARATOR '|'), ''), 256)
INTO @tenant1_user_role_hash
FROM system_user_role
WHERE tenant_id = 1;
SELECT SHA2(COALESCE(GROUP_CONCAT(CONCAT_WS(':', tenant_id, role_id, menu_id, HEX(deleted)) ORDER BY tenant_id, role_id, menu_id, HEX(deleted) SEPARATOR '|'), ''), 256)
INTO @other_tenant_role_menu_hash
FROM system_role_menu
WHERE tenant_id <> 1;
SELECT SHA2(COALESCE(GROUP_CONCAT(CONCAT_WS(':', r.code, m.permission) ORDER BY r.code, m.permission SEPARATOR '|'), ''), 256)
INTO @target_only_permission_hash
FROM system_role r
LEFT JOIN tmp_sync_source_roles s ON s.code = r.code
JOIN system_role_menu rm
  ON rm.role_id = r.id
 AND rm.tenant_id = r.tenant_id
 AND rm.deleted = b'0'
JOIN system_menu m
  ON m.id = rm.menu_id
 AND m.deleted = b'0'
 AND m.status = 0
WHERE r.tenant_id = 1
  AND r.deleted = b'0'
  AND s.code IS NULL
  AND m.permission <> '';
SELECT COUNT(*) INTO @running_test_lock_count
FROM infra_release_operation_lock
WHERE target_environment = 'test'
  AND status = 'RUNNING'
  AND deleted = b'0';
SELECT COUNT(*) INTO @new_migration_applied_count
FROM infra_release_migration
WHERE migration_id = '20260807_test_tenant1_all_role_permission_sync'
  AND status = 'APPLIED'
  AND deleted = b'0';

SELECT JSON_OBJECT(
  'capturedAt', @captured_at,
  'sourceRoleCount', @source_role_count,
  'sourcePermissionPairCount', @source_permission_pair_count,
  'targetResolvedSourceRoleCount', @target_resolved_source_role_count,
  'duplicateTargetSourceRoleCodes', @duplicate_target_source_role_codes,
  'tenant1ActiveRoleCount', @tenant1_active_role_count,
  'targetOnlyActiveRoleCount', @target_only_active_role_count,
  'missingSourcePermissionCount', @missing_source_permission_count,
  'extraSourceRolePermissionCount', @extra_source_role_permission_count,
  'tenant1UserRoleCount', @tenant1_user_role_count,
  'tenant1ActiveUserRoleCount', @tenant1_active_user_role_count,
  'tenant1UserRoleHash', @tenant1_user_role_hash,
  'otherTenantRoleMenuHash', @other_tenant_role_menu_hash,
  'targetOnlyPermissionHash', @target_only_permission_hash,
  'runningTestLockCount', @running_test_lock_count,
  'newMigrationAppliedCount', @new_migration_applied_count
);
"""


def write_json(path: Path, payload: dict) -> None:
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(payload, ensure_ascii=False, indent=2))


def snapshot(args: argparse.Namespace) -> None:
    payload = run_mysql_json(build_validation_sql())
    output = Path(args.output)
    write_json(output, payload)


def dump(args: argparse.Namespace) -> None:
    tables = " ".join(SNAPSHOT_TABLES)
    script = f"""set -eu
test -d {sql_string(REMOTE_APP_DIR)}
docker ps --format '{{{{.Names}}}}' | grep -Fx {sql_string(MYSQL_CONTAINER)} >/dev/null
docker exec -i -e MYSQL_DATABASE={sql_string(DB_NAME)} {sql_string(MYSQL_CONTAINER)} sh -lc 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -uroot --default-character-set=utf8mb4 --single-transaction --skip-lock-tables --no-tablespaces "$MYSQL_DATABASE" {tables}'
"""
    result = run_ssh(script)
    stderr = result.stderr.decode("utf-8", errors="replace")
    if result.returncode != 0:
        stdout = result.stdout.decode("utf-8", errors="replace")
        raise SystemExit(f"remote dump failed with exit {result.returncode}\nSTDOUT:\n{stdout}\nSTDERR:\n{stderr}")
    output = Path(args.output)
    output.write_bytes(result.stdout)
    payload = {
        "output": str(output),
        "sha256": sha256_file(output),
        "bytes": output.stat().st_size,
        "tables": SNAPSHOT_TABLES,
    }
    write_json(Path(args.manifest), payload)


def lock_sql(operation_id: str) -> str:
    return f"""
INSERT INTO infra_release_operation_lock
  (target_environment, operation_id, release_tag, status, started_at, finished_at, error_message, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 'test', {sql_string(operation_id)}, {sql_string(RELEASE_TAG)}, 'RUNNING', NOW(), NULL, NULL, 'codex', NOW(), 'codex', NOW(), b'0', 0
WHERE NOT EXISTS (
  SELECT 1 FROM infra_release_operation_lock
  WHERE target_environment = 'test' AND status = 'RUNNING' AND deleted = b'0'
)
ON DUPLICATE KEY UPDATE
  operation_id = IF(status = 'RUNNING', operation_id, VALUES(operation_id)),
  release_tag = IF(status = 'RUNNING', release_tag, VALUES(release_tag)),
  started_at = IF(status = 'RUNNING', started_at, VALUES(started_at)),
  finished_at = IF(status = 'RUNNING', finished_at, NULL),
  error_message = IF(status = 'RUNNING', error_message, NULL),
  status = IF(status = 'RUNNING', status, 'RUNNING'),
  updater = 'codex',
  update_time = NOW();
SELECT JSON_OBJECT(
  'operationId', operation_id,
  'status', status,
  'releaseTag', release_tag,
  'acquired', IF(operation_id = {sql_string(operation_id)} AND status = 'RUNNING', true, false)
)
FROM infra_release_operation_lock
WHERE target_environment = 'test' AND deleted = b'0';
"""


def release_lock_sql(operation_id: str, status: str, error: str = "") -> str:
    return f"""
UPDATE infra_release_operation_lock
SET status = {sql_string(status)},
    finished_at = NOW(),
    error_message = NULLIF({sql_string(error)}, ''),
    updater = 'codex',
    update_time = NOW()
WHERE target_environment = 'test'
  AND operation_id = {sql_string(operation_id)}
  AND status = 'RUNNING'
  AND deleted = b'0';
SELECT JSON_OBJECT(
  'operationId', operation_id,
  'status', status,
  'releaseTag', release_tag
)
FROM infra_release_operation_lock
WHERE target_environment = 'test'
  AND operation_id = {sql_string(operation_id)}
  AND deleted = b'0';
"""


def acquire_lock(args: argparse.Namespace) -> None:
    payload = run_mysql_json(lock_sql(args.operation_id))
    if not payload.get("acquired"):
        raise SystemExit(f"test release lock was not acquired: {json.dumps(payload, ensure_ascii=False)}")
    write_json(Path(args.output), payload)


def release_lock(args: argparse.Namespace) -> None:
    payload = run_mysql_json(release_lock_sql(args.operation_id, args.status, args.error))
    write_json(Path(args.output), payload)


def redis_scan(args: argparse.Namespace) -> None:
    script = f"""set -eu
docker ps --format '{{{{.Names}}}}' | grep -Fx {sql_string(REDIS_CONTAINER)} >/dev/null
docker exec {sql_string(REDIS_CONTAINER)} sh -lc '
for db in 0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15; do
  for pattern in "*user_role_ids*" "*menu_role_ids*" "*permission_menu_ids*"; do
    redis-cli -n "$db" --scan --pattern "$pattern" | while IFS= read -r key; do
      [ -n "$key" ] && printf "%s\\t%s\\t%s\\n" "$db" "$pattern" "$key"
    done
  done
done
'
"""
    result = run_ssh(script)
    stdout = result.stdout.decode("utf-8", errors="replace")
    stderr = result.stderr.decode("utf-8", errors="replace")
    if result.returncode != 0:
        raise SystemExit(f"remote redis scan failed with exit {result.returncode}\nSTDOUT:\n{stdout}\nSTDERR:\n{stderr}")
    entries = []
    for line in stdout.splitlines():
        parts = line.split("\t", 2)
        if len(parts) == 3:
            entries.append({"db": int(parts[0]), "pattern": parts[1], "key": parts[2]})
    payload = {"keyCount": len(entries), "keys": entries}
    write_json(Path(args.output), payload)


def redis_delete_from_scan(args: argparse.Namespace) -> None:
    scan = json.loads(Path(args.input).read_text(encoding="utf-8"))
    prefixes = tuple(args.prefix)
    grouped: dict[int, list[str]] = {}
    for entry in scan.get("keys", []):
        key = str(entry["key"])
        if not key.startswith(prefixes):
            continue
        grouped.setdefault(int(entry["db"]), []).append(key)
    commands = []
    for db, keys in sorted(grouped.items()):
        quoted_keys = " ".join(sql_string(key) for key in sorted(set(keys)))
        commands.append(f"redis-cli -n {db} DEL {quoted_keys}")
    script_body = "\n".join(commands) if commands else "true"
    script = f"""set -eu
docker ps --format '{{{{.Names}}}}' | grep -Fx {sql_string(REDIS_CONTAINER)} >/dev/null
docker exec {sql_string(REDIS_CONTAINER)} sh -lc {sql_string(script_body)}
"""
    result = run_ssh(script)
    stdout = result.stdout.decode("utf-8", errors="replace")
    stderr = result.stderr.decode("utf-8", errors="replace")
    if result.returncode != 0:
        raise SystemExit(f"remote redis delete failed with exit {result.returncode}\nSTDOUT:\n{stdout}\nSTDERR:\n{stderr}")
    payload = {
        "deletedKeyCountRequested": sum(len(set(keys)) for keys in grouped.values()),
        "prefixes": list(prefixes),
        "stdout": [line for line in stdout.splitlines() if line.strip()],
    }
    write_json(Path(args.output), payload)


def main() -> None:
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(dest="command", required=True)

    snapshot_parser = subparsers.add_parser("snapshot")
    snapshot_parser.add_argument("--output", required=True)
    snapshot_parser.set_defaults(func=snapshot)

    dump_parser = subparsers.add_parser("dump")
    dump_parser.add_argument("--output", required=True)
    dump_parser.add_argument("--manifest", required=True)
    dump_parser.set_defaults(func=dump)

    lock_parser = subparsers.add_parser("acquire-lock")
    lock_parser.add_argument("--operation-id", default=LOCK_ID)
    lock_parser.add_argument("--output", required=True)
    lock_parser.set_defaults(func=acquire_lock)

    release_parser = subparsers.add_parser("release-lock")
    release_parser.add_argument("--operation-id", default=LOCK_ID)
    release_parser.add_argument("--status", choices=["APPLIED", "FAILED"], required=True)
    release_parser.add_argument("--error", default="")
    release_parser.add_argument("--output", required=True)
    release_parser.set_defaults(func=release_lock)

    redis_scan_parser = subparsers.add_parser("redis-scan")
    redis_scan_parser.add_argument("--output", required=True)
    redis_scan_parser.set_defaults(func=redis_scan)

    redis_delete_parser = subparsers.add_parser("redis-delete-from-scan")
    redis_delete_parser.add_argument("--input", required=True)
    redis_delete_parser.add_argument("--output", required=True)
    redis_delete_parser.add_argument("--prefix", action="append", required=True)
    redis_delete_parser.set_defaults(func=redis_delete_from_scan)

    args = parser.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
