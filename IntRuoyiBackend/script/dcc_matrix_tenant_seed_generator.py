import argparse
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_SEED = ROOT / "sql/mysql/20260613_dcc_file_view_matrix_seed.sql"


def sql_quote(value):
    return "'" + str(value).replace("\\", "\\\\").replace("'", "''") + "'"


def sql_string_expr(value):
    text = str(value)
    if text.isascii():
        return sql_quote(text)
    return f"CONVERT(UNHEX('{text.encode('utf-8').hex().upper()}') USING utf8mb4) COLLATE utf8mb4_unicode_ci"


def insert_block(seed_text, table, next_marker):
    start = seed_text.find(f"INSERT INTO {table}")
    end = seed_text.find(next_marker, start)
    if start < 0 or end < 0:
        raise RuntimeError(f"Cannot locate insert block for {table}")
    return seed_text[start:end].split("VALUES", 1)[1].rsplit(";", 1)[0]


def load_categories(seed_text):
    return [
        {
            "matrix_group": m.group(1),
            "matrix_sort": int(m.group(2)),
            "file_number_pattern": m.group(3),
            "matrix_file_name": m.group(4),
            "category_code": m.group(5),
        }
        for m in re.finditer(
            r"\('([^']*)',\s*(\d+),\s*'([^']*)',\s*'([^']*)',\s*'([^']*)'\)",
            insert_block(
                seed_text,
                "tmp_dcc_file_view_matrix_category",
                "DROP TEMPORARY TABLE IF EXISTS tmp_dcc_file_view_matrix_department",
            ),
        )
    ]


def load_roles(seed_text):
    return [
        {
            "matrix_department": m.group(1),
            "role_name": m.group(2),
            "role_code": m.group(3),
            "role_remark": m.group(4),
        }
        for m in re.finditer(
            r"\('([^']*)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)'\)",
            insert_block(
                seed_text,
                "tmp_dcc_file_view_matrix_role",
                "DROP TEMPORARY TABLE IF EXISTS tmp_dcc_file_view_matrix_grant",
            ),
        )
    ]


def build_sql(tenant_id, categories, roles):
    category_values = ",\n".join(
        "("
        f"{sql_string_expr(item['category_code'])}, "
        f"{sql_string_expr(item['matrix_file_name'])}, "
        "NULL, 1, "
        f"{item['matrix_sort']}, "
        f"{sql_string_expr('VIEW_MATRIX')}, "
        f"{sql_string_expr(item['matrix_group'] + ' / ' + item['file_number_pattern'])}, "
        f"{sql_string_expr('DCC view matrix test tenant seed')}, "
        "0, 0, "
        f"{tenant_id}, NOW(), NOW(), "
        f"{sql_string_expr('dcc_matrix_tenant_seed_generator')}, {sql_string_expr('dcc_matrix_tenant_seed_generator')}, 0"
        ")"
        for item in categories
    )
    role_values = ",\n".join(
        "("
        f"{sql_string_expr(item['role_name'])}, "
        f"{sql_string_expr(item['role_code'])}, "
        f"{item_index * 10}, "
        "1, '', 0, 2, "
        f"{sql_string_expr(item['matrix_department'] + ' - ' + item['role_remark'])}, "
        f"{sql_string_expr('dcc_matrix_tenant_seed_generator')}, NOW(), {sql_string_expr('dcc_matrix_tenant_seed_generator')}, NOW(), b'0', "
        f"{tenant_id}"
        ")"
        for item_index, item in enumerate(roles, start=1)
    )
    return f"""-- Generated DCC matrix category and role seed for tenant {tenant_id}.
-- Scope: local/test tenant preparation only. Idempotent by tenant/category code and tenant/role code.
START TRANSACTION;

CREATE TEMPORARY TABLE tmp_dcc_matrix_seed_category (
  code varchar(64) NOT NULL PRIMARY KEY,
  name varchar(128) NOT NULL,
  parent_id bigint NULL,
  active tinyint NOT NULL,
  sort int NOT NULL,
  source varchar(32) NOT NULL,
  remark varchar(255) NULL,
  description varchar(255) NULL,
  distribution_required tinyint NOT NULL,
  training_required tinyint NOT NULL,
  tenant_id bigint NOT NULL,
  create_time datetime NULL,
  update_time datetime NULL,
  creator varchar(64) NULL,
  updater varchar(64) NULL,
  deleted tinyint NOT NULL
) ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO tmp_dcc_matrix_seed_category
  (code, name, parent_id, active, sort, source, remark, description, distribution_required, training_required,
   tenant_id, create_time, update_time, creator, updater, deleted)
VALUES
{category_values};

INSERT INTO dcc_file_category
  (code, name, parent_id, active, sort, source, remark, description, distribution_required, training_required,
   tenant_id, create_time, update_time, creator, updater, deleted)
SELECT tmp.code, tmp.name, tmp.parent_id, tmp.active, tmp.sort, tmp.source, tmp.remark, tmp.description,
       tmp.distribution_required, tmp.training_required, tmp.tenant_id, tmp.create_time, tmp.update_time,
       tmp.creator, tmp.updater, tmp.deleted
FROM tmp_dcc_matrix_seed_category tmp
WHERE NOT EXISTS (
  SELECT 1 FROM dcc_file_category existing
  WHERE existing.tenant_id = tmp.tenant_id AND existing.code = tmp.code
);

UPDATE dcc_file_category existing
JOIN tmp_dcc_matrix_seed_category tmp
  ON existing.tenant_id = tmp.tenant_id AND existing.code = tmp.code
SET existing.name = tmp.name,
    existing.active = tmp.active,
    existing.sort = tmp.sort,
    existing.source = tmp.source,
    existing.remark = tmp.remark,
    existing.description = tmp.description,
    existing.distribution_required = tmp.distribution_required,
    existing.training_required = tmp.training_required,
    existing.updater = tmp.updater,
    existing.update_time = tmp.update_time,
    existing.deleted = tmp.deleted
WHERE existing.deleted = 0;

CREATE TEMPORARY TABLE tmp_dcc_matrix_seed_role (
  name varchar(30) NOT NULL,
  code varchar(100) NOT NULL,
  sort int NOT NULL,
  data_scope tinyint NOT NULL,
  data_scope_dept_ids varchar(500) NOT NULL,
  status tinyint NOT NULL,
  type tinyint NOT NULL,
  remark varchar(500) NULL,
  creator varchar(64) NULL,
  create_time datetime NOT NULL,
  updater varchar(64) NULL,
  update_time datetime NOT NULL,
  deleted bit(1) NOT NULL,
  tenant_id bigint NOT NULL
) ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO tmp_dcc_matrix_seed_role
  (name, code, sort, data_scope, data_scope_dept_ids, status, type, remark,
   creator, create_time, updater, update_time, deleted, tenant_id)
VALUES
{role_values};

INSERT INTO system_role
  (name, code, sort, data_scope, data_scope_dept_ids, status, type, remark,
   creator, create_time, updater, update_time, deleted, tenant_id)
SELECT tmp.name, tmp.code, tmp.sort, tmp.data_scope, tmp.data_scope_dept_ids, tmp.status, tmp.type, tmp.remark,
       tmp.creator, tmp.create_time, tmp.updater, tmp.update_time, tmp.deleted, tmp.tenant_id
FROM tmp_dcc_matrix_seed_role tmp
WHERE NOT EXISTS (
  SELECT 1 FROM system_role existing
  WHERE existing.tenant_id = tmp.tenant_id AND existing.code = tmp.code AND existing.deleted = b'0'
);

UPDATE system_role existing
JOIN tmp_dcc_matrix_seed_role tmp
  ON existing.tenant_id = tmp.tenant_id AND existing.code = tmp.code
SET existing.name = tmp.name,
    existing.sort = tmp.sort,
    existing.data_scope = tmp.data_scope,
    existing.data_scope_dept_ids = tmp.data_scope_dept_ids,
    existing.status = tmp.status,
    existing.type = tmp.type,
    existing.remark = tmp.remark,
    existing.updater = tmp.updater,
    existing.update_time = tmp.update_time,
    existing.deleted = tmp.deleted
WHERE existing.deleted = b'0';

COMMIT;
"""


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--tenant-id", type=int, required=True)
    parser.add_argument("--output-sql", required=True)
    parser.add_argument("--seed-sql", default=str(DEFAULT_SEED))
    args = parser.parse_args()

    seed_text = Path(args.seed_sql).read_text(encoding="utf-8")
    categories = load_categories(seed_text)
    roles = load_roles(seed_text)
    if len(categories) != 59:
        raise ValueError(f"Expected 59 categories, got {len(categories)}")
    if not roles:
        raise ValueError("No matrix roles found")
    output = Path(args.output_sql)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(build_sql(args.tenant_id, categories, roles), encoding="utf-8")
    print(f"categories={len(categories)}")
    print(f"roles={len(roles)}")


if __name__ == "__main__":
    raise SystemExit(main())
