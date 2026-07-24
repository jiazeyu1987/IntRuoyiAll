import argparse
import csv
import json
import subprocess
from datetime import datetime
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
DEFAULT_TASK_DIR = ROOT / "doc/tasks/20260614-dcc-product-group-preflight"

OUTPUT_FIELDS = [
    "tenant_id",
    "group_code",
    "group_name",
    "dept_id",
    "dept_name",
    "user_id",
    "username",
    "nickname",
    "product_master_id",
    "product_code",
    "dcc_product_code",
    "product_name",
    "candidate_source",
    "manual_confirm",
    "confirm_note",
]


def qjson(full_sql):
    cmd = [
        "docker", "exec", "int-ruoyi-mysql", "mysql", "--default-character-set=utf8mb4",
        "-uroot", "-p123456", "--batch", "--raw", "--skip-column-names", "ruoyi-vue-pro",
        "-e", full_sql,
    ]
    cp = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=False)
    if cp.returncode != 0:
        raise RuntimeError(cp.stderr.decode("utf-8", errors="replace"))
    return [json.loads(line) for line in cp.stdout.decode("utf-8", errors="replace").splitlines() if line]


def write_csv(path, rows, fields):
    with Path(path).open("w", encoding="utf-8-sig", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fields)
        writer.writeheader()
        writer.writerows(rows)


def build_candidate_rows(users, products):
    rows = []
    for user in users:
        for product in products:
            if user["tenant_id"] != product["tenant_id"]:
                continue
            rows.append({
                "tenant_id": user["tenant_id"],
                "group_code": "",
                "group_name": "",
                "dept_id": user["dept_id"],
                "dept_name": user["dept_name"],
                "user_id": user["user_id"],
                "username": user["username"],
                "nickname": user["nickname"],
                "product_master_id": product["product_master_id"],
                "product_code": product["product_code"],
                "dcc_product_code": product["dcc_product_code"],
                "product_name": product["product_name"],
                "candidate_source": product["source"],
                "manual_confirm": "",
                "confirm_note": "",
            })
    return rows


def load_new_product_users(tenant_id):
    return qjson(f"""
SELECT JSON_OBJECT(
  'tenant_id', u.tenant_id,
  'user_id', u.id,
  'username', u.username,
  'nickname', IFNULL(u.nickname, ''),
  'dept_id', u.dept_id,
  'dept_name', d.name
)
FROM system_users u
JOIN system_dept d
  ON d.id = u.dept_id
 AND d.tenant_id = u.tenant_id
 AND d.deleted = b'0'
WHERE u.tenant_id = {tenant_id}
  AND u.deleted = b'0'
  AND u.status = 0
  AND d.name = '新品开发部'
ORDER BY u.id
""")


def load_products(tenant_id):
    return qjson(f"""
SELECT JSON_OBJECT(
  'tenant_id', p.tenant_id,
  'product_master_id', p.id,
  'product_code', IFNULL(p.product_code, ''),
  'dcc_product_code', IFNULL(p.dcc_product_code, ''),
  'product_name', IFNULL(p.name_cn, ''),
  'source', 'mdm_product'
)
FROM mdm_product p
WHERE p.tenant_id = {tenant_id}
  AND p.deleted = b'0'
  AND (p.status IS NULL OR p.status = '0' OR p.status = 'ACTIVE' OR p.status = 'ENABLE')
ORDER BY p.product_code, p.id
""")


def load_dcc_referenced_products(tenant_id):
    return qjson(f"""
SELECT JSON_OBJECT(
  'tenant_id', ref.tenant_id,
  'product_master_id', ref.product_master_id,
  'product_code', IFNULL(ref.product_code, ''),
  'dcc_product_code', IFNULL(p.dcc_product_code, ''),
  'product_name', COALESCE(NULLIF(ref.product_name, ''), p.name_cn, ''),
  'source', 'dcc_controlled_file'
)
FROM (
  SELECT tenant_id,
         product_master_id,
         MAX(IFNULL(product_code, '')) AS product_code,
         MAX(IFNULL(product_name, '')) AS product_name
  FROM dcc_controlled_file
  WHERE tenant_id = {tenant_id}
    AND product_master_id IS NOT NULL
  GROUP BY tenant_id, product_master_id
) ref
LEFT JOIN mdm_product p
  ON p.id = ref.product_master_id
 AND p.tenant_id = ref.tenant_id
 AND p.deleted = b'0'
ORDER BY ref.product_master_id
""")


def merge_products(primary, referenced):
    by_key = {}
    for product in primary + referenced:
        key = (product["tenant_id"], product["product_master_id"])
        if key not in by_key or by_key[key]["source"] != "dcc_controlled_file":
            by_key[key] = product
    return list(by_key.values())


def write_summary(path, rows, users, products):
    lines = [
        "# DCC 产品组绑定预检摘要",
        "",
        f"- Generated at: {datetime.now().isoformat(timespec='seconds')}",
        f"- Candidate users: {len(users)}",
        f"- Candidate products: {len(products)}",
        f"- Candidate rows: {len(rows)}",
        "",
        "## 关键结论",
        "",
        "- 本预检只读生成候选，不写入产品组、成员或产品绑定。",
        "- `group_code`、`group_name`、`manual_confirm` 默认留空，必须由业务确认后再生成 SQL。",
        "- 若候选行过多，应先按实际项目/产品组筛选，再填写确认列。",
    ]
    Path(path).write_text("\n".join(lines) + "\n", encoding="utf-8")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--tenant-id", type=int, default=1)
    parser.add_argument("--output-csv", default=str(DEFAULT_TASK_DIR / "dcc-product-group-candidates.csv"))
    parser.add_argument("--summary", default=str(DEFAULT_TASK_DIR / "dcc-product-group-preflight-summary.md"))
    args = parser.parse_args()

    output_csv = Path(args.output_csv)
    output_csv.parent.mkdir(parents=True, exist_ok=True)
    users = load_new_product_users(args.tenant_id)
    products = merge_products(load_products(args.tenant_id), load_dcc_referenced_products(args.tenant_id))
    rows = build_candidate_rows(users, products)
    write_csv(output_csv, rows, OUTPUT_FIELDS)
    write_summary(args.summary, rows, users, products)
    print(f"candidate_users={len(users)}")
    print(f"candidate_products={len(products)}")
    print(f"candidate_rows={len(rows)}")


if __name__ == "__main__":
    main()
