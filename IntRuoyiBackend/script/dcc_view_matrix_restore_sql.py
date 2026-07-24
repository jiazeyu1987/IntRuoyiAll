from __future__ import annotations

import argparse
import ast
import re
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_SEED = ROOT / "sql" / "mysql" / "20260613_dcc_file_view_matrix_seed.sql"
DEFAULT_TENANT_ID = 1
RESTORE_ACTOR = "dcc_view_matrix_restore_20260622"
RESTORE_PROCEDURE = "apply_dcc_view_matrix_restore_20260622"
MATRIX_DIRECTORY_REASON = "文件查阅矩阵：允许查询与预览，不自动开放下载"
WENKONG_DIRECTORY_REASONS = [
    "文控角色受控浏览预览：允许查询与预览，不开放下载",
    "文控下载角色继承文控目录范围并开放下载",
]


def sql_quote(value: object) -> str:
    return "'" + str(value).replace("\\", "\\\\").replace("'", "''") + "'"


def sql_string_expr(value: object) -> str:
    text = str(value)
    if text.isascii():
        return sql_quote(text)
    return f"CONVERT(UNHEX('{text.encode('utf-8').hex().upper()}') USING utf8mb4) COLLATE utf8mb4_unicode_ci"


def parse_values_block(sql: str, table_name: str) -> list[tuple]:
    pattern = (
        r"INSERT\s+INTO\s+`?"
        + re.escape(table_name)
        + r"`?\s*\([^)]*\)\s*VALUES\s*(?P<values>.*?);"
    )
    match = re.search(pattern, sql, re.IGNORECASE | re.DOTALL)
    if not match:
        raise ValueError(f"seed sql missing VALUES block: {table_name}")
    rows = []
    for row_text in re.findall(r"\((.*?)\)(?:,\s*|$)", match.group("values"), re.DOTALL):
        rows.append(ast.literal_eval("(" + row_text + ")"))
    if not rows:
        raise ValueError(f"seed sql has empty VALUES block: {table_name}")
    return rows


def sql_values(rows: list[tuple]) -> str:
    return ",\n".join(
        "(" + ", ".join(sql_string_expr(value) if isinstance(value, str) else str(value) for value in row) + ")"
        for row in rows
    )


def load_seed(seed_path: Path) -> dict[str, list[tuple]]:
    if not seed_path.exists():
        raise FileNotFoundError(f"seed sql does not exist: {seed_path}")
    seed = seed_path.read_text(encoding="utf-8")
    parsed = {
        "categories": parse_values_block(seed, "tmp_dcc_file_view_matrix_category"),
        "departments": parse_values_block(seed, "tmp_dcc_file_view_matrix_department"),
        "roles": parse_values_block(seed, "tmp_dcc_file_view_matrix_role"),
        "grants": parse_values_block(seed, "tmp_dcc_file_view_matrix_grant"),
    }
    if len(parsed["categories"]) != 59:
        raise ValueError(f"expected 59 matrix categories, got {len(parsed['categories'])}")
    if len(parsed["grants"]) != 231:
        raise ValueError(f"expected 231 matrix grants, got {len(parsed['grants'])}")
    return parsed


def build_restore_sql(seed_rows: dict[str, list[tuple]], tenant_id: int) -> str:
    categories = sql_values(seed_rows["categories"])
    departments = sql_values(seed_rows["departments"])
    roles = sql_values(seed_rows["roles"])
    grants = sql_values(seed_rows["grants"])
    matrix_reason = sql_string_expr(MATRIX_DIRECTORY_REASON)
    wenkong_reasons = ", ".join(sql_string_expr(reason) for reason in WENKONG_DIRECTORY_REASONS)

    return f"""-- DCC file view matrix restore SQL.
-- Source: {DEFAULT_SEED.as_posix()}
-- Scope: tenant {tenant_id}; restores Excel file-view matrix mode for current active directories.
SET @dcc_fvm_restore_tenant_id := {tenant_id};
SET @dcc_fvm_restore_actor := {sql_string_expr(RESTORE_ACTOR)};

DROP PROCEDURE IF EXISTS {RESTORE_PROCEDURE};
DELIMITER $$
CREATE PROCEDURE {RESTORE_PROCEDURE}()
BEGIN
  DECLARE v_missing_text TEXT DEFAULT NULL;
  DECLARE v_expected_matrix_view_rules BIGINT DEFAULT 0;
  DECLARE v_active_matrix_view_rules BIGINT DEFAULT 0;
  DECLARE v_unexpected_matrix_active_rules BIGINT DEFAULT 0;
  DECLARE v_restored_matrix_directory_rules BIGINT DEFAULT 0;
  DECLARE v_active_wenkong_directory_rules BIGINT DEFAULT 0;
  DECLARE v_tenant122_matrix_view_rules BIGINT DEFAULT 0;

  START TRANSACTION;

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_fvm_restore_category;
  CREATE TEMPORARY TABLE tmp_dcc_fvm_restore_category (
    matrix_group varchar(8) NOT NULL,
    matrix_sort int NOT NULL,
    file_number_pattern varchar(128) NOT NULL,
    file_name varchar(128) NOT NULL,
    category_code varchar(64) NOT NULL,
    PRIMARY KEY (category_code)
  ) ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

  INSERT INTO tmp_dcc_fvm_restore_category
    (matrix_group, matrix_sort, file_number_pattern, file_name, category_code)
  VALUES
{categories};

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_fvm_restore_department;
  CREATE TEMPORARY TABLE tmp_dcc_fvm_restore_department (
    matrix_department varchar(64) NOT NULL,
    dept_name varchar(64) NOT NULL,
    parent_dept_name varchar(64) NOT NULL,
    subject_lookup_name varchar(128) NOT NULL,
    PRIMARY KEY (matrix_department)
  ) ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

  INSERT INTO tmp_dcc_fvm_restore_department
    (matrix_department, dept_name, parent_dept_name, subject_lookup_name)
  VALUES
{departments};

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_fvm_restore_role;
  CREATE TEMPORARY TABLE tmp_dcc_fvm_restore_role (
    matrix_department varchar(64) NOT NULL,
    role_name varchar(30) NOT NULL,
    role_code varchar(100) NOT NULL,
    role_remark varchar(500) NOT NULL,
    PRIMARY KEY (role_code)
  ) ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

  INSERT INTO tmp_dcc_fvm_restore_role
    (matrix_department, role_name, role_code, role_remark)
  VALUES
{roles};

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_fvm_restore_grant;
  CREATE TEMPORARY TABLE tmp_dcc_fvm_restore_grant (
    category_code varchar(64) NOT NULL,
    matrix_department varchar(64) NOT NULL,
    marker varchar(4) NOT NULL,
    action_type varchar(32) NOT NULL,
    subject_type varchar(32) NOT NULL,
    subject_lookup_name varchar(128) NOT NULL,
    access_note varchar(128) NOT NULL,
    PRIMARY KEY (category_code, matrix_department)
  ) ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

  INSERT INTO tmp_dcc_fvm_restore_grant
    (category_code, matrix_department, marker, action_type, subject_type, subject_lookup_name, access_note)
  VALUES
{grants};

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_fvm_restore_resolved_dept;
  CREATE TEMPORARY TABLE tmp_dcc_fvm_restore_resolved_dept AS
  SELECT mapping.matrix_department,
         mapping.subject_lookup_name,
         dept.id AS subject_id
  FROM tmp_dcc_fvm_restore_department mapping
  JOIN system_dept dept
    ON dept.tenant_id = @dcc_fvm_restore_tenant_id
   AND dept.deleted = b'0'
   AND dept.status = 0
   AND dept.name = mapping.dept_name
  JOIN system_dept parent_dept
    ON parent_dept.tenant_id = @dcc_fvm_restore_tenant_id
   AND parent_dept.deleted = b'0'
   AND parent_dept.id = dept.parent_id
   AND parent_dept.name = mapping.parent_dept_name;

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_fvm_restore_missing_dept;
  CREATE TEMPORARY TABLE tmp_dcc_fvm_restore_missing_dept AS
  SELECT mapping.matrix_department, mapping.subject_lookup_name, COALESCE(resolved.resolved_count, 0) AS resolved_count
  FROM tmp_dcc_fvm_restore_department mapping
  LEFT JOIN (
    SELECT matrix_department, COUNT(*) AS resolved_count
    FROM tmp_dcc_fvm_restore_resolved_dept
    GROUP BY matrix_department
  ) resolved ON resolved.matrix_department = mapping.matrix_department
  WHERE COALESCE(resolved.resolved_count, 0) <> 1;

  IF (SELECT COUNT(*) FROM tmp_dcc_fvm_restore_missing_dept) > 0 THEN
    SELECT GROUP_CONCAT(CONCAT(matrix_department, '=>', subject_lookup_name, '#', resolved_count) SEPARATOR '; ')
      INTO v_missing_text
    FROM tmp_dcc_fvm_restore_missing_dept;
    SET v_missing_text = CONCAT('DCC_VIEW_MATRIX_RESTORE_SUBJECT_PRECHECK_FAILED: dept=', v_missing_text);
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_missing_text;
  END IF;

  INSERT INTO `system_role`
    (name, code, sort, data_scope, data_scope_dept_ids, status, type, remark,
     tenant_id, creator, create_time, updater, update_time, deleted)
  SELECT role.role_name, role.role_code, 6900, 1, '', 0, 2,
         CONCAT('DCC 文件查阅矩阵主管级角色：', role.role_remark),
         @dcc_fvm_restore_tenant_id, @dcc_fvm_restore_actor, NOW(), @dcc_fvm_restore_actor, NOW(), b'0'
  FROM tmp_dcc_fvm_restore_role role
  WHERE NOT EXISTS (
    SELECT 1 FROM system_role existing
    WHERE existing.tenant_id = @dcc_fvm_restore_tenant_id
      AND existing.deleted = b'0'
      AND existing.code = role.role_code
  );

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_fvm_restore_resolved_role;
  CREATE TEMPORARY TABLE tmp_dcc_fvm_restore_resolved_role AS
  SELECT role.role_code,
         role.role_name,
         system_role.id AS subject_id,
         role.role_name AS subject_lookup_name
  FROM tmp_dcc_fvm_restore_role role
  JOIN system_role
    ON system_role.tenant_id = @dcc_fvm_restore_tenant_id
   AND system_role.deleted = b'0'
   AND system_role.code = role.role_code;

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_fvm_restore_missing_role;
  CREATE TEMPORARY TABLE tmp_dcc_fvm_restore_missing_role AS
  SELECT role.role_code, role.role_name, COALESCE(resolved.resolved_count, 0) AS resolved_count
  FROM tmp_dcc_fvm_restore_role role
  LEFT JOIN (
    SELECT role_code, COUNT(*) AS resolved_count
    FROM tmp_dcc_fvm_restore_resolved_role
    GROUP BY role_code
  ) resolved ON resolved.role_code = role.role_code
  WHERE COALESCE(resolved.resolved_count, 0) <> 1;

  IF (SELECT COUNT(*) FROM tmp_dcc_fvm_restore_missing_role) > 0 THEN
    SELECT GROUP_CONCAT(CONCAT(role_code, '=>', role_name, '#', resolved_count) SEPARATOR '; ')
      INTO v_missing_text
    FROM tmp_dcc_fvm_restore_missing_role;
    SET v_missing_text = CONCAT('DCC_VIEW_MATRIX_RESTORE_SUBJECT_PRECHECK_FAILED: role=', v_missing_text);
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_missing_text;
  END IF;

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_fvm_restore_category_ambiguous;
  CREATE TEMPORARY TABLE tmp_dcc_fvm_restore_category_ambiguous AS
  SELECT matrix_category.category_code, matrix_category.file_name, COUNT(category.id) AS resolved_count
  FROM tmp_dcc_fvm_restore_category matrix_category
  LEFT JOIN dcc_file_category category
    ON category.tenant_id = @dcc_fvm_restore_tenant_id
   AND category.deleted = 0
   AND (category.code = matrix_category.category_code OR category.name = matrix_category.file_name)
  GROUP BY matrix_category.category_code, matrix_category.file_name
  HAVING COUNT(category.id) > 1;

  IF (SELECT COUNT(*) FROM tmp_dcc_fvm_restore_category_ambiguous) > 0 THEN
    SELECT GROUP_CONCAT(CONCAT(category_code, '=>', file_name, '#', resolved_count) SEPARATOR '; ')
      INTO v_missing_text
    FROM tmp_dcc_fvm_restore_category_ambiguous;
    SET v_missing_text = CONCAT('DCC_VIEW_MATRIX_RESTORE_CATEGORY_PRECHECK_FAILED: ', v_missing_text);
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_missing_text;
  END IF;

  INSERT INTO `dcc_file_category`
    (code, name, parent_id, active, sort, source, remark, description,
     distribution_required, training_required, tenant_id, create_time, update_time, creator, updater, deleted)
  SELECT matrix_category.category_code,
         matrix_category.file_name,
         NULL,
         1,
         CASE matrix_category.matrix_group WHEN 'DHF' THEN matrix_category.matrix_sort ELSE 100 + matrix_category.matrix_sort END,
         'VIEW_MATRIX',
         CONCAT('文件查阅矩阵 ', matrix_category.matrix_group, '-', LPAD(matrix_category.matrix_sort, 3, '0')),
         CONCAT('编号模板：', COALESCE(NULLIF(matrix_category.file_number_pattern, ''), '/')),
         0,
         0,
         @dcc_fvm_restore_tenant_id,
         NOW(),
         NOW(),
         @dcc_fvm_restore_actor,
         @dcc_fvm_restore_actor,
         0
  FROM tmp_dcc_fvm_restore_category matrix_category
  WHERE NOT EXISTS (
    SELECT 1 FROM dcc_file_category existing
    WHERE existing.tenant_id = @dcc_fvm_restore_tenant_id
      AND existing.deleted = 0
      AND (existing.code = matrix_category.category_code OR existing.name = matrix_category.file_name)
  );

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_fvm_restore_category_resolved;
  CREATE TEMPORARY TABLE tmp_dcc_fvm_restore_category_resolved AS
  SELECT matrix_category.category_code,
         matrix_category.matrix_group,
         matrix_category.matrix_sort,
         matrix_category.file_name,
         category.id AS category_id
  FROM tmp_dcc_fvm_restore_category matrix_category
  JOIN dcc_file_category category
    ON category.tenant_id = @dcc_fvm_restore_tenant_id
   AND category.deleted = 0
   AND (category.code = matrix_category.category_code OR category.name = matrix_category.file_name);

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_fvm_restore_category_missing;
  CREATE TEMPORARY TABLE tmp_dcc_fvm_restore_category_missing AS
  SELECT matrix_category.category_code, matrix_category.file_name, COALESCE(resolved.resolved_count, 0) AS resolved_count
  FROM tmp_dcc_fvm_restore_category matrix_category
  LEFT JOIN (
    SELECT category_code, COUNT(*) AS resolved_count
    FROM tmp_dcc_fvm_restore_category_resolved
    GROUP BY category_code
  ) resolved ON resolved.category_code = matrix_category.category_code
  WHERE COALESCE(resolved.resolved_count, 0) <> 1;

  IF (SELECT COUNT(*) FROM tmp_dcc_fvm_restore_category_missing) > 0 THEN
    SELECT GROUP_CONCAT(CONCAT(category_code, '=>', file_name, '#', resolved_count) SEPARATOR '; ')
      INTO v_missing_text
    FROM tmp_dcc_fvm_restore_category_missing;
    SET v_missing_text = CONCAT('DCC_VIEW_MATRIX_RESTORE_CATEGORY_PRECHECK_FAILED: ', v_missing_text);
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_missing_text;
  END IF;

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_fvm_restore_resolved_subject;
  CREATE TEMPORARY TABLE tmp_dcc_fvm_restore_resolved_subject (
    category_code varchar(64) NOT NULL,
    matrix_department varchar(64) NOT NULL,
    marker varchar(4) NOT NULL,
    action_type varchar(32) NOT NULL,
    subject_type varchar(32) NOT NULL,
    subject_id bigint NOT NULL,
    subject_lookup_name varchar(128) NOT NULL,
    access_note varchar(128) NOT NULL,
    scope_type varchar(32) NOT NULL
  ) ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

  INSERT INTO tmp_dcc_fvm_restore_resolved_subject
    (category_code, matrix_department, marker, action_type, subject_type, subject_id, subject_lookup_name, access_note, scope_type)
  SELECT grant_rule.category_code,
         grant_rule.matrix_department,
         grant_rule.marker,
         grant_rule.action_type,
         grant_rule.subject_type,
         resolved_dept.subject_id,
         grant_rule.subject_lookup_name,
         grant_rule.access_note,
         CASE
           WHEN grant_rule.matrix_department = '新品开发部'
             AND grant_rule.subject_type = 'DEPT'
             AND grant_rule.marker = '●'
           THEN 'PRODUCT_GROUP'
           ELSE 'GLOBAL'
         END AS scope_type
  FROM tmp_dcc_fvm_restore_grant grant_rule
  JOIN tmp_dcc_fvm_restore_resolved_dept resolved_dept
    ON grant_rule.subject_type = 'DEPT'
   AND resolved_dept.matrix_department = grant_rule.matrix_department;

  INSERT INTO tmp_dcc_fvm_restore_resolved_subject
    (category_code, matrix_department, marker, action_type, subject_type, subject_id, subject_lookup_name, access_note, scope_type)
  SELECT grant_rule.category_code,
         grant_rule.matrix_department,
         grant_rule.marker,
         grant_rule.action_type,
         grant_rule.subject_type,
         resolved_role.subject_id,
         grant_rule.subject_lookup_name,
         grant_rule.access_note,
         'GLOBAL' AS scope_type
  FROM tmp_dcc_fvm_restore_grant grant_rule
  JOIN tmp_dcc_fvm_restore_resolved_role resolved_role
    ON grant_rule.subject_type = 'ROLE'
   AND resolved_role.subject_lookup_name = grant_rule.subject_lookup_name;

  INSERT INTO `dcc_file_category_permission_rule`
    (category_id, action_type, subject_type, subject_id, scope_type, active, remark,
     tenant_id, create_time, update_time, creator, updater, deleted)
  SELECT category_resolved.category_id,
         subject_resolved.action_type,
         subject_resolved.subject_type,
         subject_resolved.subject_id,
         subject_resolved.scope_type,
         1,
         CONCAT('文件查阅矩阵 ', subject_resolved.matrix_department, ' ', subject_resolved.marker, ' ', subject_resolved.access_note),
         @dcc_fvm_restore_tenant_id,
         NOW(),
         NOW(),
         @dcc_fvm_restore_actor,
         @dcc_fvm_restore_actor,
         0
  FROM tmp_dcc_fvm_restore_resolved_subject subject_resolved
  JOIN tmp_dcc_fvm_restore_category_resolved category_resolved
    ON category_resolved.category_code = subject_resolved.category_code
  ON DUPLICATE KEY UPDATE
    active = VALUES(active),
    scope_type = VALUES(scope_type),
    remark = VALUES(remark),
    update_time = VALUES(update_time),
    updater = VALUES(updater),
    deleted = VALUES(deleted);

  UPDATE `dcc_file_category_permission_rule` legacy_rule
  JOIN tmp_dcc_fvm_restore_category_resolved category_resolved
    ON category_resolved.category_id = legacy_rule.category_id
  LEFT JOIN tmp_dcc_fvm_restore_resolved_subject subject_resolved
    ON subject_resolved.category_code = category_resolved.category_code
   AND subject_resolved.action_type = legacy_rule.action_type
   AND subject_resolved.subject_type = legacy_rule.subject_type
   AND subject_resolved.subject_id = legacy_rule.subject_id
   AND subject_resolved.scope_type = legacy_rule.scope_type
  SET legacy_rule.active = 0,
      legacy_rule.deleted = 1,
      legacy_rule.remark = LEFT(CONCAT('DCC_VIEW_MATRIX_RESTORE_LEGACY_PERMISSION_DISABLED: ', COALESCE(legacy_rule.remark, '')), 255),
      legacy_rule.update_time = NOW(),
      legacy_rule.updater = @dcc_fvm_restore_actor
  WHERE legacy_rule.tenant_id = @dcc_fvm_restore_tenant_id
    AND legacy_rule.deleted = 0
    AND legacy_rule.active = 1
    AND (
      legacy_rule.action_type = 'DOWNLOAD'
      OR legacy_rule.subject_type = 'USER'
      OR (
        legacy_rule.action_type = 'VIEW'
        AND subject_resolved.category_code IS NULL
      )
    );

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_fvm_restore_directory_subject;
  CREATE TEMPORARY TABLE tmp_dcc_fvm_restore_directory_subject AS
  SELECT DISTINCT subject_type, subject_id
  FROM tmp_dcc_fvm_restore_resolved_subject;

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_fvm_restore_missing_directory;
  CREATE TEMPORARY TABLE tmp_dcc_fvm_restore_missing_directory AS
  SELECT 'NO_ACTIVE_DCC_DIRECTORY' AS missing_reason
  WHERE NOT EXISTS (
    SELECT 1
    FROM dcc_file_directory directory_record
    WHERE directory_record.tenant_id = @dcc_fvm_restore_tenant_id
      AND directory_record.deleted = 0
      AND directory_record.active = 1
  );

  IF (SELECT COUNT(*) FROM tmp_dcc_fvm_restore_missing_directory) > 0 THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'DCC_VIEW_MATRIX_RESTORE_DIRECTORY_PRECHECK_FAILED';
  END IF;

  UPDATE dcc_directory_access_rule access_rule
  JOIN dcc_file_directory directory_record
    ON directory_record.id = access_rule.directory_id
   AND directory_record.tenant_id = @dcc_fvm_restore_tenant_id
   AND directory_record.deleted = 0
   AND directory_record.active = 1
  JOIN tmp_dcc_fvm_restore_directory_subject directory_subject
    ON directory_subject.subject_type = access_rule.subject_type
   AND directory_subject.subject_id = access_rule.subject_id
  SET access_rule.can_query = 1,
      access_rule.can_preview = 1,
      access_rule.can_download = 0,
      access_rule.active = 1,
      access_rule.change_reason = {matrix_reason},
      access_rule.update_time = NOW(),
      access_rule.updater = @dcc_fvm_restore_actor,
      access_rule.deleted = 0
  WHERE access_rule.tenant_id = @dcc_fvm_restore_tenant_id;

  INSERT INTO dcc_directory_access_rule
    (directory_id, subject_type, subject_id, can_query, can_preview, can_download, active, change_reason,
     tenant_id, create_time, update_time, creator, updater, deleted)
  SELECT directory_record.id,
         directory_subject.subject_type,
         directory_subject.subject_id,
         1,
         1,
         0,
         1,
         {matrix_reason},
         @dcc_fvm_restore_tenant_id,
         NOW(),
         NOW(),
         @dcc_fvm_restore_actor,
         @dcc_fvm_restore_actor,
         0
  FROM dcc_file_directory directory_record
  CROSS JOIN tmp_dcc_fvm_restore_directory_subject directory_subject
  WHERE directory_record.tenant_id = @dcc_fvm_restore_tenant_id
    AND directory_record.deleted = 0
    AND directory_record.active = 1
    AND NOT EXISTS (
      SELECT 1
      FROM dcc_directory_access_rule existing
      WHERE existing.tenant_id = @dcc_fvm_restore_tenant_id
        AND existing.deleted = 0
        AND existing.directory_id = directory_record.id
        AND existing.subject_type = directory_subject.subject_type
        AND existing.subject_id = directory_subject.subject_id
    );

  UPDATE dcc_directory_access_rule access_rule
  JOIN system_role role
    ON role.id = access_rule.subject_id
   AND role.tenant_id = access_rule.tenant_id
   AND role.deleted = b'0'
  SET access_rule.can_query = 0,
      access_rule.can_preview = 0,
      access_rule.can_download = 0,
      access_rule.active = 0,
      access_rule.deleted = 1,
      access_rule.change_reason = LEFT(CONCAT('DCC_VIEW_MATRIX_RESTORE_WENKONG_DIRECTORY_DISABLED: ', COALESCE(access_rule.change_reason, '')), 255),
      access_rule.update_time = NOW(),
      access_rule.updater = @dcc_fvm_restore_actor
  WHERE access_rule.tenant_id = @dcc_fvm_restore_tenant_id
    AND access_rule.deleted = 0
    AND access_rule.active = 1
    AND access_rule.subject_type = 'ROLE'
    AND role.code IN ('wenkong', 'wenkong_download')
    AND access_rule.change_reason IN ({wenkong_reasons});

  SELECT COUNT(*) INTO v_expected_matrix_view_rules
  FROM tmp_dcc_fvm_restore_resolved_subject
  WHERE action_type = 'VIEW';

  SELECT COUNT(*) INTO v_active_matrix_view_rules
  FROM dcc_file_category_permission_rule rule
  JOIN tmp_dcc_fvm_restore_category_resolved category_resolved
    ON category_resolved.category_id = rule.category_id
  WHERE rule.tenant_id = @dcc_fvm_restore_tenant_id
    AND rule.deleted = 0
    AND rule.active = 1
    AND rule.action_type = 'VIEW';

  SELECT COUNT(*) INTO v_unexpected_matrix_active_rules
  FROM dcc_file_category_permission_rule rule
  JOIN tmp_dcc_fvm_restore_category_resolved category_resolved
    ON category_resolved.category_id = rule.category_id
  LEFT JOIN tmp_dcc_fvm_restore_resolved_subject subject_resolved
    ON subject_resolved.category_code = category_resolved.category_code
   AND subject_resolved.action_type = rule.action_type
   AND subject_resolved.subject_type = rule.subject_type
   AND subject_resolved.subject_id = rule.subject_id
   AND subject_resolved.scope_type = rule.scope_type
  WHERE rule.tenant_id = @dcc_fvm_restore_tenant_id
    AND rule.deleted = 0
    AND rule.active = 1
    AND (
      rule.action_type = 'DOWNLOAD'
      OR rule.subject_type = 'USER'
      OR (rule.action_type = 'VIEW' AND subject_resolved.category_code IS NULL)
    );

  SELECT COUNT(*) INTO v_restored_matrix_directory_rules
  FROM dcc_directory_access_rule access_rule
  JOIN dcc_file_directory directory_record
    ON directory_record.id = access_rule.directory_id
   AND directory_record.tenant_id = @dcc_fvm_restore_tenant_id
   AND directory_record.deleted = 0
   AND directory_record.active = 1
  JOIN tmp_dcc_fvm_restore_directory_subject directory_subject
    ON directory_subject.subject_type = access_rule.subject_type
   AND directory_subject.subject_id = access_rule.subject_id
  WHERE access_rule.tenant_id = @dcc_fvm_restore_tenant_id
    AND access_rule.deleted = 0
    AND access_rule.active = 1
    AND access_rule.can_query = 1
    AND access_rule.can_preview = 1
    AND access_rule.can_download = 0
    AND access_rule.change_reason = {matrix_reason};

  SELECT COUNT(*) INTO v_active_wenkong_directory_rules
  FROM dcc_directory_access_rule access_rule
  JOIN system_role role
    ON role.id = access_rule.subject_id
   AND role.tenant_id = access_rule.tenant_id
   AND role.deleted = b'0'
  WHERE access_rule.tenant_id = @dcc_fvm_restore_tenant_id
    AND access_rule.deleted = 0
    AND access_rule.active = 1
    AND access_rule.subject_type = 'ROLE'
    AND role.code IN ('wenkong', 'wenkong_download')
    AND access_rule.change_reason IN ({wenkong_reasons});

  SELECT COUNT(*) INTO v_tenant122_matrix_view_rules
  FROM dcc_file_category_permission_rule rule
  JOIN dcc_file_category category
    ON category.id = rule.category_id
   AND category.tenant_id = rule.tenant_id
   AND category.deleted = 0
  WHERE rule.tenant_id = 122
    AND rule.deleted = 0
    AND rule.active = 1
    AND rule.action_type = 'VIEW'
    AND category.code LIKE 'DCC_FVM_%';

  COMMIT;

  SELECT 'DCC_VIEW_MATRIX_RESTORE_APPLIED' AS audit_code,
         v_expected_matrix_view_rules AS expected_matrix_view_rules,
         v_active_matrix_view_rules AS active_matrix_view_rules,
         v_unexpected_matrix_active_rules AS unexpected_matrix_active_rules,
         v_restored_matrix_directory_rules AS restored_matrix_directory_rules,
         v_active_wenkong_directory_rules AS active_wenkong_directory_rules,
         v_tenant122_matrix_view_rules AS tenant122_matrix_view_rules;
END$$
DELIMITER ;

CALL {RESTORE_PROCEDURE}();
DROP PROCEDURE IF EXISTS {RESTORE_PROCEDURE};
"""


def default_mysql_command() -> list[str]:
    return [
        "docker",
        "exec",
        "-i",
        "int-ruoyi-mysql",
        "mysql",
        "-uroot",
        "-p123456",
        "--default-character-set=utf8mb4",
        "--batch",
        "--raw",
        "ruoyi-vue-pro",
    ]


def run_mysql(mysql_command: list[str], sql: str) -> str:
    completed = subprocess.run(
        mysql_command,
        input=sql.encode("utf-8"),
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
    )
    stdout = completed.stdout.decode("utf-8", errors="replace")
    stderr = completed.stderr.decode("utf-8", errors="replace")
    if completed.returncode != 0:
        raise RuntimeError((stderr or stdout).strip())
    if stderr:
        sys.stderr.write(stderr)
    return stdout


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--tenant-id", type=int, default=DEFAULT_TENANT_ID)
    parser.add_argument("--seed-sql", default=str(DEFAULT_SEED))
    parser.add_argument("--print-sql", action="store_true")
    parser.add_argument("--apply-local-mysql", action="store_true")
    parser.add_argument("--mysql-command", nargs="+")
    args = parser.parse_args()

    try:
        seed_rows = load_seed(Path(args.seed_sql))
        sql = build_restore_sql(seed_rows, args.tenant_id)
        if args.print_sql:
            print(sql)
            return 0
        if args.apply_local_mysql and args.mysql_command:
            raise ValueError("--apply-local-mysql and --mysql-command cannot be used together")
        if args.apply_local_mysql:
            print(run_mysql(default_mysql_command(), sql), end="")
            return 0
        if args.mysql_command:
            print(run_mysql(args.mysql_command, sql), end="")
            return 0
        raise ValueError("use --print-sql, --apply-local-mysql, or --mysql-command")
    except Exception as exc:
        print(str(exc), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
