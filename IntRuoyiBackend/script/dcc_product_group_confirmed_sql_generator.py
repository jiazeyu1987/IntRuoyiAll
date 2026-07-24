import argparse
import csv
import re
import sys
from pathlib import Path


TRUE_VALUES = {"1", "true", "yes", "y", "是", "确认", "confirmed"}
GROUP_CODE_PATTERN = re.compile(r"^[A-Za-z0-9][A-Za-z0-9_-]{1,63}$")


def read_csv(path):
    with Path(path).open(encoding="utf-8-sig", newline="") as f:
        return list(csv.DictReader(f))


def sql_quote(value):
    return "'" + str(value).replace("\\", "\\\\").replace("'", "''") + "'"


def normalize_bool(value):
    return str(value or "").strip().lower() in TRUE_VALUES


def require_int(value, field_name):
    text = str(value or "").strip()
    if not text.isdigit() or int(text) <= 0:
        raise ValueError(f"{field_name} must be a positive integer: {value!r}")
    return int(text)


def require_text(value, field_name):
    text = str(value or "").strip()
    if not text:
        raise ValueError(f"{field_name} is required")
    return text


def collect_confirmed_rows(rows):
    confirmed = []
    seen = set()
    for index, row in enumerate(rows, start=2):
        if not normalize_bool(row.get("manual_confirm")):
            continue
        tenant_id = require_int(row.get("tenant_id"), f"row {index} tenant_id")
        dept_id = require_int(row.get("dept_id"), f"row {index} dept_id")
        user_id = require_int(row.get("user_id"), f"row {index} user_id")
        product_master_id = require_int(row.get("product_master_id"), f"row {index} product_master_id")
        group_code = require_text(row.get("group_code"), f"row {index} group_code")
        group_name = require_text(row.get("group_name"), f"row {index} group_name")
        if not GROUP_CODE_PATTERN.fullmatch(group_code):
            raise ValueError(f"row {index} group_code is invalid: {group_code!r}")
        key = (tenant_id, group_code, user_id, product_master_id)
        if key in seen:
            continue
        seen.add(key)
        confirmed.append({
            "tenant_id": tenant_id,
            "group_code": group_code,
            "group_name": group_name,
            "dept_id": dept_id,
            "user_id": user_id,
            "product_master_id": product_master_id,
        })
    return confirmed


def build_sql(rows):
    values = ",\n".join(
        "("
        f"{item['tenant_id']}, "
        f"{sql_quote(item['group_code'])}, "
        f"{sql_quote(item['group_name'])}, "
        f"{item['dept_id']}, "
        f"{item['user_id']}, "
        f"{item['product_master_id']}"
        ")"
        for item in rows
    )
    return f"""-- Generated from manually confirmed DCC product group CSV.
-- Safety: fail-fast prechecks, no category or role permission changes.
START TRANSACTION;
SET @dcc_product_group_actor := 'dcc_product_group_confirmed_sql_generator';

DROP TEMPORARY TABLE IF EXISTS tmp_dcc_confirmed_product_group;
CREATE TEMPORARY TABLE tmp_dcc_confirmed_product_group (
  tenant_id bigint NOT NULL,
  group_code varchar(64) NOT NULL,
  group_name varchar(128) NOT NULL,
  dept_id bigint NOT NULL,
  user_id bigint NOT NULL,
  product_master_id bigint NOT NULL,
  PRIMARY KEY (tenant_id, group_code, user_id, product_master_id)
) ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO tmp_dcc_confirmed_product_group
  (tenant_id, group_code, group_name, dept_id, user_id, product_master_id)
VALUES
{values};

DROP TEMPORARY TABLE IF EXISTS tmp_dcc_product_group_precheck_error;
CREATE TEMPORARY TABLE tmp_dcc_product_group_precheck_error (
  id int NOT NULL PRIMARY KEY
) ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO tmp_dcc_product_group_precheck_error (id)
SELECT 1
FROM (
  SELECT 1
  FROM tmp_dcc_confirmed_product_group tmp
  LEFT JOIN system_dept dept
    ON dept.id = tmp.dept_id
   AND dept.tenant_id = tmp.tenant_id
   AND dept.deleted = b'0'
  LEFT JOIN system_users user_record
    ON user_record.id = tmp.user_id
   AND user_record.tenant_id = tmp.tenant_id
   AND user_record.deleted = b'0'
  LEFT JOIN mdm_product product
    ON product.id = tmp.product_master_id
   AND product.tenant_id = tmp.tenant_id
   AND product.deleted = b'0'
  WHERE dept.id IS NULL
     OR user_record.id IS NULL
     OR product.id IS NULL
  LIMIT 1
) missing_precheck;
INSERT INTO tmp_dcc_product_group_precheck_error (id) VALUES (1);

TRUNCATE TABLE tmp_dcc_product_group_precheck_error;
INSERT INTO tmp_dcc_product_group_precheck_error (id)
SELECT 1
FROM (
  SELECT 1
  FROM tmp_dcc_confirmed_product_group tmp
  JOIN system_users user_record
    ON user_record.id = tmp.user_id
    AND user_record.tenant_id = tmp.tenant_id
    AND user_record.deleted = b'0'
  WHERE user_record.dept_id <> tmp.dept_id
  LIMIT 1
) dept_mismatch_precheck;
INSERT INTO tmp_dcc_product_group_precheck_error (id) VALUES (1);

INSERT INTO dcc_product_visibility_group
  (tenant_id, dept_id, name, active, remark, creator, create_time, updater, update_time, deleted)
SELECT DISTINCT tmp.tenant_id,
       tmp.dept_id,
       tmp.group_name,
       b'1',
       CONCAT('DCC产品组确认导入：', tmp.group_code),
       @dcc_product_group_actor,
       NOW(),
       @dcc_product_group_actor,
       NOW(),
       b'0'
FROM tmp_dcc_confirmed_product_group tmp
WHERE NOT EXISTS (
  SELECT 1
  FROM dcc_product_visibility_group existing
  WHERE existing.tenant_id = tmp.tenant_id
    AND existing.name = tmp.group_name
    AND existing.dept_id = tmp.dept_id
    AND existing.deleted = b'0'
);

DROP TEMPORARY TABLE IF EXISTS tmp_dcc_confirmed_product_group_resolved;
CREATE TEMPORARY TABLE tmp_dcc_confirmed_product_group_resolved AS
SELECT tmp.tenant_id,
       tmp.group_code,
       tmp.group_name,
       tmp.dept_id,
       tmp.user_id,
       tmp.product_master_id,
       product_group.id AS group_id
FROM tmp_dcc_confirmed_product_group tmp
JOIN dcc_product_visibility_group product_group
  ON product_group.tenant_id = tmp.tenant_id
 AND product_group.dept_id = tmp.dept_id
 AND product_group.name = tmp.group_name
 AND product_group.deleted = b'0';

TRUNCATE TABLE tmp_dcc_product_group_precheck_error;
INSERT INTO tmp_dcc_product_group_precheck_error (id)
SELECT 1
FROM (
  SELECT 1
  FROM tmp_dcc_confirmed_product_group_resolved
  GROUP BY tenant_id, group_code
  HAVING COUNT(DISTINCT group_id) <> 1
  LIMIT 1
) group_resolve_precheck;
INSERT INTO tmp_dcc_product_group_precheck_error (id) VALUES (1);

INSERT INTO dcc_product_visibility_group_member
  (tenant_id, group_id, user_id, active, remark, creator, create_time, updater, update_time, deleted)
SELECT DISTINCT resolved.tenant_id,
       resolved.group_id,
       resolved.user_id,
       b'1',
       CONCAT('DCC产品组确认导入：', resolved.group_code),
       @dcc_product_group_actor,
       NOW(),
       @dcc_product_group_actor,
       NOW(),
       b'0'
FROM tmp_dcc_confirmed_product_group_resolved resolved
WHERE NOT EXISTS (
  SELECT 1
  FROM dcc_product_visibility_group_member existing
  WHERE existing.tenant_id = resolved.tenant_id
    AND existing.group_id = resolved.group_id
    AND existing.user_id = resolved.user_id
    AND existing.deleted = b'0'
);

INSERT INTO dcc_product_visibility_group_product
  (tenant_id, group_id, product_master_id, active, remark, creator, create_time, updater, update_time, deleted)
SELECT DISTINCT resolved.tenant_id,
       resolved.group_id,
       resolved.product_master_id,
       b'1',
       CONCAT('DCC产品组确认导入：', resolved.group_code),
       @dcc_product_group_actor,
       NOW(),
       @dcc_product_group_actor,
       NOW(),
       b'0'
FROM tmp_dcc_confirmed_product_group_resolved resolved
WHERE NOT EXISTS (
  SELECT 1
  FROM dcc_product_visibility_group_product existing
  WHERE existing.tenant_id = resolved.tenant_id
    AND existing.group_id = resolved.group_id
    AND existing.product_master_id = resolved.product_master_id
    AND existing.deleted = b'0'
);

COMMIT;
"""


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--input-csv", required=True)
    parser.add_argument("--output-sql", required=True)
    args = parser.parse_args()

    try:
        rows = collect_confirmed_rows(read_csv(args.input_csv))
        if not rows:
            raise ValueError("No confirmed product group rows found")
        Path(args.output_sql).write_text(build_sql(rows), encoding="utf-8")
    except Exception as exc:
        print(str(exc), file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
