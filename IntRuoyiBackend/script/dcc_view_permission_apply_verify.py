import argparse
import csv
import json
import subprocess
import sys
from pathlib import Path

from dcc_view_permission_sql_bundle_verify import verify_bundle


def read_csv(path):
    with Path(path).open(encoding="utf-8-sig", newline="") as f:
        return list(csv.DictReader(f))


def sql_quote(value):
    return "'" + str(value).replace("\\", "\\\\").replace("'", "''") + "'"


def build_values(rows, fields):
    if not rows:
        return "SELECT NULL WHERE FALSE"
    return "\nUNION ALL\n".join(
        "SELECT " + ", ".join(f"{sql_quote(row.get(field, ''))} AS {field}" for field in fields)
        for row in rows
    )


def build_verify_sql(manifest):
    input_files = manifest["inputFiles"]
    classification_rows = [row for row in read_csv(input_files["matrixClassificationCsv"]) if row.get("manual_confirm_category_code")]
    role_rows = [row for row in read_csv(input_files["matrixRoleCsv"]) if str(row.get("manual_confirm") or "").strip()]
    product_rows = [row for row in read_csv(input_files["productGroupCsv"]) if str(row.get("manual_confirm") or "").strip()]
    classification_values = build_values(classification_rows, ["file_id", "tenant_id", "manual_confirm_category_code"])
    role_values = build_values(role_rows, ["role_code", "candidate_user_id"])
    product_values = build_values(product_rows, ["tenant_id", "group_name", "dept_id", "user_id", "product_master_id"])
    return f"""WITH
confirmed_files AS (
{classification_values}
),
missing_files AS (
  SELECT cf.file_id
  FROM confirmed_files cf
  LEFT JOIN dcc_controlled_file f
    ON f.id = CAST(cf.file_id AS UNSIGNED)
   AND f.tenant_id = CAST(cf.tenant_id AS UNSIGNED)
   AND f.deleted = 0
  LEFT JOIN dcc_file_category c
    ON c.id = f.category_id
   AND c.tenant_id = f.tenant_id
   AND c.code = cf.manual_confirm_category_code
   AND c.deleted = 0
  WHERE cf.file_id IS NOT NULL AND c.id IS NULL
),
confirmed_roles AS (
{role_values}
),
missing_roles AS (
  SELECT cr.role_code, cr.candidate_user_id
  FROM confirmed_roles cr
  LEFT JOIN system_role r
    ON r.code = cr.role_code
   AND r.deleted = b'0'
  LEFT JOIN system_user_role ur
    ON ur.role_id = r.id
   AND ur.user_id = CAST(cr.candidate_user_id AS UNSIGNED)
   AND ur.tenant_id = r.tenant_id
   AND ur.deleted = b'0'
  WHERE cr.role_code IS NOT NULL AND ur.id IS NULL
),
confirmed_products AS (
{product_values}
),
missing_product_members AS (
  SELECT cp.group_name, cp.user_id
  FROM confirmed_products cp
  LEFT JOIN dcc_product_visibility_group g
    ON g.tenant_id = CAST(cp.tenant_id AS UNSIGNED)
   AND g.dept_id = CAST(cp.dept_id AS UNSIGNED)
   AND g.name = cp.group_name
   AND g.deleted = b'0'
  LEFT JOIN dcc_product_visibility_group_member gm
    ON gm.group_id = g.id
   AND gm.tenant_id = g.tenant_id
   AND gm.user_id = CAST(cp.user_id AS UNSIGNED)
   AND gm.deleted = b'0'
  WHERE cp.group_name IS NOT NULL AND gm.id IS NULL
),
missing_product_bindings AS (
  SELECT cp.group_name, cp.product_master_id
  FROM confirmed_products cp
  LEFT JOIN dcc_product_visibility_group g
    ON g.tenant_id = CAST(cp.tenant_id AS UNSIGNED)
   AND g.dept_id = CAST(cp.dept_id AS UNSIGNED)
   AND g.name = cp.group_name
   AND g.deleted = b'0'
  LEFT JOIN dcc_product_visibility_group_product gp
    ON gp.group_id = g.id
   AND gp.tenant_id = g.tenant_id
   AND gp.product_master_id = CAST(cp.product_master_id AS UNSIGNED)
   AND gp.deleted = b'0'
  WHERE cp.group_name IS NOT NULL AND gp.id IS NULL
)
SELECT 'missing_files' AS check_name, COUNT(*) AS missing_count FROM missing_files
UNION ALL SELECT 'missing_roles', COUNT(*) FROM missing_roles
UNION ALL SELECT 'missing_product_members', COUNT(*) FROM missing_product_members
UNION ALL SELECT 'missing_product_bindings', COUNT(*) FROM missing_product_bindings;
"""


def parse_mysql_counts(output):
    counts = {}
    for line in output.splitlines():
        parts = line.strip().split()
        if len(parts) == 2 and parts[1].isdigit() and parts[0] != "check_name":
            counts[parts[0]] = int(parts[1])
    if not counts:
        raise ValueError("mysql output did not contain verification counts")
    return counts


def run_mysql(mysql_command, sql):
    cp = subprocess.run(
        mysql_command,
        input=sql,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
    )
    if cp.returncode != 0:
        raise RuntimeError(cp.stderr.strip() or cp.stdout.strip())
    return parse_mysql_counts(cp.stdout)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--bundle-dir", required=True)
    parser.add_argument("--print-sql", action="store_true")
    parser.add_argument("--mysql-command", nargs="+")
    args = parser.parse_args()

    try:
        verify_bundle(args.bundle_dir)
        manifest = json.loads((Path(args.bundle_dir) / "manifest.json").read_text(encoding="utf-8"))
        sql = build_verify_sql(manifest)
        if args.print_sql:
            print(sql)
            return 0
        if not args.mysql_command:
            raise ValueError("--mysql-command is required unless --print-sql is used")
        counts = run_mysql(args.mysql_command, sql)
        failed = {key: value for key, value in counts.items() if value != 0}
        if failed:
            raise ValueError(f"DCC apply verification failed: {failed}")
        print(json.dumps({"ready": True, "counts": counts}, ensure_ascii=False, indent=2))
    except Exception as exc:
        print(str(exc), file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
