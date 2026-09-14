import argparse
import hashlib
import json
import shlex
import subprocess
from pathlib import Path


TASK_DIR = Path(__file__).resolve().parent
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
LOCK_OPERATION_ID = "test-tenant1-user-role-binding-sync-20260808T001"
RELEASE_TAG = "manual-test-tenant1-user-role-binding-sync-20260808"
SNAPSHOT_TABLES = [
    "system_users",
    "system_role",
    "system_user_role",
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


def write_json(path: Path, payload: object) -> None:
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(payload, ensure_ascii=False, indent=2))


def run_ssh(script: str) -> subprocess.CompletedProcess[bytes]:
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


def run_remote_mysql(sql: str) -> subprocess.CompletedProcess[bytes]:
    script = f"""set -eu
test -d {sql_string(REMOTE_APP_DIR)}
docker ps --format '{{{{.Names}}}}' | grep -Fx {sql_string(MYSQL_CONTAINER)} >/dev/null
docker exec -i -e MYSQL_DATABASE={sql_string(DB_NAME)} {sql_string(MYSQL_CONTAINER)} sh -lc 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot --default-character-set=utf8mb4 --batch --raw "$MYSQL_DATABASE"' <<'SQL'
{sql}
SQL
"""
    return run_ssh(script)


def remote_mysql_json(sql: str) -> dict:
    result = run_remote_mysql(sql)
    stdout = result.stdout.decode("utf-8", errors="replace")
    stderr = result.stderr.decode("utf-8", errors="replace")
    if result.returncode != 0:
        raise SystemExit(f"remote mysql failed with exit {result.returncode}\nSTDOUT:\n{stdout}\nSTDERR:\n{stderr}")
    lines = [line for line in stdout.splitlines() if line.strip()]
    if not lines:
        raise SystemExit(f"remote mysql returned no output\nSTDERR:\n{stderr}")
    return json.loads(lines[-1])


def dump(args: argparse.Namespace) -> None:
    tables = " ".join(shlex.quote(table) for table in SNAPSHOT_TABLES)
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
        "target": SSH_TARGET,
        "database": DB_NAME,
        "output": str(output),
        "sha256": sha256_file(output),
        "bytes": output.stat().st_size,
        "tables": SNAPSHOT_TABLES,
        "stderr": stderr.strip(),
    }
    write_json(Path(args.manifest), payload)


def lock_sql(operation_id: str) -> str:
    return f"""
INSERT INTO infra_release_operation_lock
  (target_environment, operation_id, release_tag, status, started_at, finished_at, error_message, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 'test', {sql_string(operation_id)}, {sql_string(RELEASE_TAG)}, 'RUNNING', NOW(), NULL, NULL, 'codex', NOW(), 'codex', NOW(), b'0', 0
WHERE NOT EXISTS (
  SELECT 1
  FROM infra_release_operation_lock
  WHERE target_environment = 'test'
    AND status = 'RUNNING'
    AND deleted = b'0'
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
WHERE target_environment = 'test'
  AND deleted = b'0';
"""


def release_lock_sql(operation_id: str, status: str, error: str) -> str:
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
  'releaseTag', release_tag,
  'updatedRows', ROW_COUNT()
)
FROM infra_release_operation_lock
WHERE target_environment = 'test'
  AND operation_id = {sql_string(operation_id)}
  AND deleted = b'0';
"""


def acquire_lock(args: argparse.Namespace) -> None:
    payload = remote_mysql_json(lock_sql(args.operation_id))
    if not payload.get("acquired"):
        raise SystemExit(f"test release/data lock was not acquired: {json.dumps(payload, ensure_ascii=False)}")
    write_json(Path(args.output), payload)


def release_lock(args: argparse.Namespace) -> None:
    payload = remote_mysql_json(release_lock_sql(args.operation_id, args.status, args.error))
    write_json(Path(args.output), payload)


def run_sql_file(args: argparse.Namespace) -> None:
    sql_path = Path(args.sql)
    sql = sql_path.read_text(encoding="utf-8")
    result = run_remote_mysql(sql)
    stdout = result.stdout.decode("utf-8", errors="replace")
    stderr = result.stderr.decode("utf-8", errors="replace")
    output = {
        "sql": str(sql_path),
        "sqlSha256": sha256_file(sql_path),
        "returncode": result.returncode,
        "stdout": stdout.splitlines(),
        "stderr": stderr.splitlines(),
    }
    write_json(Path(args.output), output)
    if result.returncode != 0:
        raise SystemExit(f"remote sql file failed with exit {result.returncode}")


def build_user_id_sql(usernames: list[str]) -> str:
    values = ",\n".join(f"({sql_string(username)})" for username in usernames)
    return f"""
DROP TEMPORARY TABLE IF EXISTS tmp_codex_affected_user;
CREATE TEMPORARY TABLE tmp_codex_affected_user (
  username varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (username)
);
INSERT INTO tmp_codex_affected_user (username) VALUES
{values};
SELECT JSON_ARRAYAGG(JSON_OBJECT('username', u.username, 'id', u.id))
FROM system_users u
JOIN tmp_codex_affected_user a ON a.username = u.username
WHERE u.tenant_id = 1
  AND u.deleted = b'0'
  AND u.status = 0
ORDER BY u.username;
"""


def affected_user_ids(args: argparse.Namespace) -> None:
    affected = json.loads(Path(args.input).read_text(encoding="utf-8"))
    usernames = affected.get("affectedUsers") or []
    payload = remote_mysql_json(build_user_id_sql(usernames))
    write_json(Path(args.output), payload)


def redis_scan(args: argparse.Namespace) -> None:
    user_ids = [str(row["id"]) for row in json.loads(Path(args.user_ids).read_text(encoding="utf-8"))]
    patterns = []
    for user_id in user_ids:
        patterns.append(f"*user_role_ids*{user_id}*")
    quoted_patterns = " ".join(shlex.quote(pattern) for pattern in patterns)
    script = f"""set -eu
docker ps --format '{{{{.Names}}}}' | grep -Fx {sql_string(REDIS_CONTAINER)} >/dev/null
docker exec {sql_string(REDIS_CONTAINER)} sh -lc '
for db in 0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15; do
  for pattern in {quoted_patterns}; do
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
    allowed_suffixes = {f":{user_id}" for user_id in user_ids} | {f"::{user_id}" for user_id in user_ids}
    for line in stdout.splitlines():
        parts = line.split("\t", 2)
        if len(parts) != 3:
            continue
        db, pattern, key = parts
        if not (key.endswith(tuple(allowed_suffixes)) and "user_role_ids" in key):
            continue
        entries.append({"db": int(db), "pattern": pattern, "key": key})
    write_json(Path(args.output), {"keyCount": len(entries), "keys": entries})


def redis_delete(args: argparse.Namespace) -> None:
    scan = json.loads(Path(args.input).read_text(encoding="utf-8"))
    grouped: dict[int, list[str]] = {}
    for entry in scan.get("keys", []):
        key = str(entry["key"])
        if "user_role_ids" not in key:
            raise SystemExit(f"refusing to delete non-user-role key: {key}")
        grouped.setdefault(int(entry["db"]), []).append(key)
    commands = []
    for db, keys in sorted(grouped.items()):
        quoted = " ".join(shlex.quote(key) for key in sorted(set(keys)))
        commands.append(f"redis-cli -n {db} DEL {quoted}")
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
        "stdout": [line for line in stdout.splitlines() if line.strip()],
        "stderr": stderr.splitlines(),
    }
    write_json(Path(args.output), payload)


def main() -> None:
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(dest="command", required=True)

    dump_parser = subparsers.add_parser("dump")
    dump_parser.add_argument("--output", required=True)
    dump_parser.add_argument("--manifest", required=True)
    dump_parser.set_defaults(func=dump)

    lock_parser = subparsers.add_parser("acquire-lock")
    lock_parser.add_argument("--operation-id", default=LOCK_OPERATION_ID)
    lock_parser.add_argument("--output", required=True)
    lock_parser.set_defaults(func=acquire_lock)

    release_parser = subparsers.add_parser("release-lock")
    release_parser.add_argument("--operation-id", default=LOCK_OPERATION_ID)
    release_parser.add_argument("--status", choices=["APPLIED", "FAILED"], required=True)
    release_parser.add_argument("--error", default="")
    release_parser.add_argument("--output", required=True)
    release_parser.set_defaults(func=release_lock)

    run_parser = subparsers.add_parser("run-sql-file")
    run_parser.add_argument("--sql", required=True)
    run_parser.add_argument("--output", required=True)
    run_parser.set_defaults(func=run_sql_file)

    affected_parser = subparsers.add_parser("affected-user-ids")
    affected_parser.add_argument("--input", required=True)
    affected_parser.add_argument("--output", required=True)
    affected_parser.set_defaults(func=affected_user_ids)

    redis_scan_parser = subparsers.add_parser("redis-scan")
    redis_scan_parser.add_argument("--user-ids", required=True)
    redis_scan_parser.add_argument("--output", required=True)
    redis_scan_parser.set_defaults(func=redis_scan)

    redis_delete_parser = subparsers.add_parser("redis-delete")
    redis_delete_parser.add_argument("--input", required=True)
    redis_delete_parser.add_argument("--output", required=True)
    redis_delete_parser.set_defaults(func=redis_delete)

    args = parser.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
