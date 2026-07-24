import argparse
import csv
import sys
from pathlib import Path


TRUE_VALUES = {"1", "true", "yes", "y", "是", "确认", "confirmed"}


def read_csv(path):
    with Path(path).open(encoding="utf-8-sig", newline="") as f:
        return list(csv.DictReader(f))


def sql_quote(value):
    return "'" + str(value).replace("\\", "\\\\").replace("'", "''") + "'"


def normalize_bool(value):
    return str(value or "").strip().lower() in TRUE_VALUES


def require_int(value, field_name):
    text = str(value or "").strip()
    if not text.isdigit():
        raise ValueError(f"{field_name} must be a positive integer: {value!r}")
    return int(text)


def collect_confirmed_files(rows):
    confirmed = []
    for index, row in enumerate(rows, start=2):
        category_code = str(row.get("manual_confirm_category_code") or "").strip()
        if not category_code:
            continue
        file_id = require_int(row.get("file_id"), f"classification row {index} file_id")
        tenant_id = require_int(row.get("tenant_id"), f"classification row {index} tenant_id")
        deleted = str(row.get("deleted") or "").strip()
        if deleted not in {"0", "False", "false", "b'0'"}:
            raise ValueError(f"classification row {index} selected file_id={file_id} has deleted!=0")
        if not category_code.startswith("DCC_FVM_"):
            raise ValueError(f"classification row {index} has invalid matrix category code: {category_code}")
        confirmed.append({"file_id": file_id, "tenant_id": tenant_id, "category_code": category_code})
    return confirmed


def collect_confirmed_roles(rows):
    confirmed = []
    for index, row in enumerate(rows, start=2):
        if not normalize_bool(row.get("manual_confirm")):
            continue
        role_code = str(row.get("role_code") or "").strip()
        user_id = require_int(row.get("candidate_user_id"), f"role row {index} candidate_user_id")
        if not role_code.startswith("dcc_matrix_"):
            raise ValueError(f"role row {index} has invalid matrix role code: {role_code}")
        confirmed.append({"role_code": role_code, "user_id": user_id})
    return confirmed


def build_sql(files, roles):
    lines = [
        "-- Generated from manually confirmed DCC matrix preflight CSV files.",
        "-- Safety: fail-fast prechecks, no download permission changes.",
        "START TRANSACTION;",
        "SET @dcc_matrix_actor := 'dcc_matrix_confirmed_sql_generator';",
        "",
    ]
    if files:
        lines += [
            "DROP TEMPORARY TABLE IF EXISTS tmp_dcc_confirmed_file_category;",
            "CREATE TEMPORARY TABLE tmp_dcc_confirmed_file_category (",
            "  file_id bigint NOT NULL PRIMARY KEY,",
            "  tenant_id bigint NOT NULL,",
            "  category_code varchar(64) NOT NULL",
            ") ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;",
            "INSERT INTO tmp_dcc_confirmed_file_category (file_id, tenant_id, category_code) VALUES",
        ]
        lines.append(",\n".join(
            f"({item['file_id']}, {item['tenant_id']}, {sql_quote(item['category_code'])})"
            for item in files
        ) + ";")
        lines += [
            "",
            "DROP TEMPORARY TABLE IF EXISTS tmp_dcc_confirmed_precheck_error;",
            "CREATE TEMPORARY TABLE tmp_dcc_confirmed_precheck_error (",
            "  id int NOT NULL PRIMARY KEY",
            ") ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;",
            "INSERT INTO tmp_dcc_confirmed_precheck_error (id)",
            "SELECT 1",
            "FROM (",
            "  SELECT 1",
            "  FROM tmp_dcc_confirmed_file_category tmp",
            "  LEFT JOIN dcc_controlled_file f ON f.id = tmp.file_id AND f.tenant_id = tmp.tenant_id",
            "  WHERE f.id IS NULL OR f.deleted <> 0",
            "  LIMIT 1",
            ") bad_file_precheck;",
            "INSERT INTO tmp_dcc_confirmed_precheck_error (id) VALUES (1);",
            "",
            "TRUNCATE TABLE tmp_dcc_confirmed_precheck_error;",
            "INSERT INTO tmp_dcc_confirmed_precheck_error (id)",
            "SELECT 1",
            "FROM (",
            "  SELECT 1",
            "  FROM tmp_dcc_confirmed_file_category tmp",
            "  LEFT JOIN dcc_file_category c ON c.tenant_id = tmp.tenant_id AND c.code = tmp.category_code AND c.deleted = 0",
            "  WHERE c.id IS NULL",
            "  LIMIT 1",
            ") bad_category_precheck;",
            "INSERT INTO tmp_dcc_confirmed_precheck_error (id) VALUES (1);",
            "",
            "UPDATE dcc_controlled_file f",
            "JOIN tmp_dcc_confirmed_file_category tmp ON tmp.file_id = f.id AND tmp.tenant_id = f.tenant_id",
            "JOIN dcc_file_category c ON c.tenant_id = tmp.tenant_id AND c.code = tmp.category_code AND c.deleted = 0",
            "SET f.category_id = c.id,",
            "    f.updater = @dcc_matrix_actor,",
            "    f.update_time = NOW()",
            "WHERE f.deleted = 0;",
            "",
        ]
    if roles:
        lines += [
            "DROP TEMPORARY TABLE IF EXISTS tmp_dcc_confirmed_role_member;",
            "CREATE TEMPORARY TABLE tmp_dcc_confirmed_role_member (",
            "  role_code varchar(100) NOT NULL,",
            "  user_id bigint NOT NULL,",
            "  PRIMARY KEY (role_code, user_id)",
            ") ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;",
            "INSERT INTO tmp_dcc_confirmed_role_member (role_code, user_id) VALUES",
        ]
        lines.append(",\n".join(
            f"({sql_quote(item['role_code'])}, {item['user_id']})"
            for item in roles
        ) + ";")
        lines += [
            "",
            "DROP TEMPORARY TABLE IF EXISTS tmp_dcc_confirmed_precheck_error;",
            "CREATE TEMPORARY TABLE tmp_dcc_confirmed_precheck_error (",
            "  id int NOT NULL PRIMARY KEY",
            ") ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;",
            "INSERT INTO tmp_dcc_confirmed_precheck_error (id)",
            "SELECT 1",
            "FROM (",
            "  SELECT 1",
            "  FROM tmp_dcc_confirmed_role_member tmp",
            "  LEFT JOIN system_role r ON r.code = tmp.role_code AND r.deleted = b'0'",
            "  LEFT JOIN system_users u ON u.id = tmp.user_id AND u.deleted = b'0'",
            "  WHERE r.id IS NULL OR u.id IS NULL OR r.tenant_id <> u.tenant_id",
            "  LIMIT 1",
            ") bad_role_member_precheck;",
            "INSERT INTO tmp_dcc_confirmed_precheck_error (id) VALUES (1);",
            "",
            "INSERT INTO system_user_role (user_id, role_id, creator, create_time, updater, update_time, deleted, tenant_id)",
            "SELECT tmp.user_id, r.id, @dcc_matrix_actor, NOW(), @dcc_matrix_actor, NOW(), b'0', r.tenant_id",
            "FROM tmp_dcc_confirmed_role_member tmp",
            "JOIN system_role r ON r.code = tmp.role_code AND r.deleted = b'0'",
            "JOIN system_users u ON u.id = tmp.user_id AND u.deleted = b'0' AND u.tenant_id = r.tenant_id",
            "WHERE NOT EXISTS (",
            "  SELECT 1 FROM system_user_role existing",
            "  WHERE existing.user_id = tmp.user_id AND existing.role_id = r.id",
            "    AND existing.tenant_id = r.tenant_id AND existing.deleted = b'0'",
            ");",
            "",
        ]
    lines += [
        "COMMIT;",
        "",
    ]
    return "\n".join(lines)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--classification-csv", required=True)
    parser.add_argument("--role-csv", required=True)
    parser.add_argument("--output-sql", required=True)
    args = parser.parse_args()

    try:
        files = collect_confirmed_files(read_csv(args.classification_csv))
        roles = collect_confirmed_roles(read_csv(args.role_csv))
        if not files and not roles:
            raise ValueError("No confirmed rows found in classification or role CSV files")
        Path(args.output_sql).write_text(build_sql(files, roles), encoding="utf-8")
    except Exception as exc:
        print(str(exc), file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
