import json
from pathlib import Path


TASK_DIR = Path(__file__).resolve().parent
ACTOR = "codex-20260808-user-role-bindings-sync"
TENANT_ID = 1


def sql_string(value):
    text = "" if value is None else str(value)
    return "'" + text.replace("\\", "\\\\").replace("'", "''") + "'"


def values(rows):
    return ",\n".join("(" + ", ".join(sql_string(item) for item in row) + ")" for row in rows)


def load(name):
    return json.loads((TASK_DIR / name).read_text(encoding="utf-8"))


def active_maps(payload):
    users = {row["username"]: row for row in payload["activeUsers"]}
    roles = {row["code"]: row for row in payload["activeRoles"]}
    pairs = {(row["username"], row["roleCode"]) for row in payload["activeUserRoles"]}
    return users, roles, pairs


def create_temp_tables(scope_users, source_pairs, table_prefix="tmp_codex_ur"):
    return f"""
DROP TEMPORARY TABLE IF EXISTS {table_prefix}_scope_user;
CREATE TEMPORARY TABLE {table_prefix}_scope_user (
  username varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (username)
);
INSERT INTO {table_prefix}_scope_user (username) VALUES
{values((username,) for username in scope_users)};

DROP TEMPORARY TABLE IF EXISTS {table_prefix}_source_pair;
CREATE TEMPORARY TABLE {table_prefix}_source_pair (
  username varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  role_code varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (username, role_code)
);
INSERT INTO {table_prefix}_source_pair (username, role_code) VALUES
{values(source_pairs)};
"""


def main():
    local = load("source-user-role-audit-local.json")
    target = load("target-user-role-audit-test.json")
    local_users, local_roles, local_pairs = active_maps(local)
    target_users, target_roles, target_pairs = active_maps(target)

    scope_users = sorted(set(local_users) & set(target_users))
    source_pairs = sorted(pair for pair in local_pairs if pair[0] in target_users and pair[1] in target_roles)
    unresolved_pairs = sorted(pair for pair in local_pairs if pair not in source_pairs)
    target_effective_pairs_for_scope = sorted(pair for pair in target_pairs if pair[0] in scope_users)
    to_insert_or_reactivate = sorted(set(source_pairs) - set(target_effective_pairs_for_scope))
    to_soft_delete = sorted(set(target_effective_pairs_for_scope) - set(source_pairs))
    affected_users = sorted({username for username, _ in to_insert_or_reactivate} | {username for username, _ in to_soft_delete})
    dangerous_roles = sorted({
        role_code
        for _, role_code in source_pairs
        if role_code in {"super_admin", "wenkong", "wenkong_download", "doc_control"}
    })

    plan = {
        "tenantId": TENANT_ID,
        "scopeUserCount": len(scope_users),
        "sourcePairCount": len(source_pairs),
        "unresolvedSourcePairCount": len(unresolved_pairs),
        "unresolvedSourcePairs": unresolved_pairs,
        "targetEffectivePairForScopeCount": len(target_effective_pairs_for_scope),
        "toInsertOrReactivateCount": len(to_insert_or_reactivate),
        "toInsertOrReactivate": to_insert_or_reactivate,
        "toSoftDeleteCount": len(to_soft_delete),
        "toSoftDelete": to_soft_delete,
        "affectedUserCount": len(affected_users),
        "affectedUsers": affected_users,
        "dangerousRoleCodesInSource": dangerous_roles,
        "missingUsersInTest": sorted(set(local_users) - set(target_users)),
        "extraUsersInTest": sorted(set(target_users) - set(local_users)),
        "missingRolesInTest": sorted(set(local_roles) - set(target_roles)),
        "extraRolesInTest": sorted(set(target_roles) - set(local_roles)),
        "preTargetOtherTenantUserRoleHash": target["otherTenantUserRoleHash"],
        "preTargetTenantUserRoleHash": target["tenantUserRoleHash"],
    }
    (TASK_DIR / "sync-plan.json").write_text(json.dumps(plan, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (TASK_DIR / "affected-users.json").write_text(json.dumps({"affectedUsers": affected_users}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    temp_sql = create_temp_tables(scope_users, source_pairs)

    change_sql = f"""DROP PROCEDURE IF EXISTS codex_20260808_sync_user_role_bindings;
DELIMITER $$
CREATE PROCEDURE codex_20260808_sync_user_role_bindings()
proc: BEGIN
    DECLARE v_actor VARCHAR(64) DEFAULT '{ACTOR}';
    DECLARE v_scope_count INT DEFAULT 0;
    DECLARE v_pair_count INT DEFAULT 0;
    DECLARE v_bad_count INT DEFAULT 0;
    DECLARE v_expected_scope_count INT DEFAULT {len(scope_users)};
    DECLARE v_expected_pair_count INT DEFAULT {len(source_pairs)};
    DECLARE v_expected_change_count INT DEFAULT {len(to_insert_or_reactivate) + len(to_soft_delete)};
    DECLARE v_expected_soft_delete_count INT DEFAULT {len(to_soft_delete)};
    DECLARE v_soft_delete_count INT DEFAULT 0;
    DECLARE v_reactivate_count INT DEFAULT 0;
    DECLARE v_insert_count INT DEFAULT 0;
    DECLARE v_missing_count INT DEFAULT 0;
    DECLARE v_extra_count INT DEFAULT 0;
    DECLARE v_other_tenant_hash_before VARCHAR(64);
    DECLARE v_other_tenant_hash_after VARCHAR(64);
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;
    SET SESSION group_concat_max_len = 100000000;

{temp_sql}

    SELECT COUNT(*) INTO v_scope_count FROM tmp_codex_ur_scope_user;
    IF v_scope_count <> v_expected_scope_count THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'scope user count mismatch';
    END IF;

    SELECT COUNT(*) INTO v_pair_count FROM tmp_codex_ur_source_pair;
    IF v_pair_count <> v_expected_pair_count THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'source pair count mismatch';
    END IF;

    SELECT COUNT(*) INTO v_bad_count
    FROM (
      SELECT u.username
      FROM system_users u
      JOIN tmp_codex_ur_scope_user s ON s.username = u.username
      WHERE u.tenant_id = {TENANT_ID} AND u.deleted = b'0' AND u.status = 0
      GROUP BY u.username
      HAVING COUNT(*) <> 1
    ) duplicate_or_missing_user;
    IF v_bad_count <> 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'target enabled user is not uniquely resolvable';
    END IF;

    SELECT COUNT(*) INTO v_bad_count
    FROM (
      SELECT r.code
      FROM system_role r
      JOIN (SELECT DISTINCT role_code FROM tmp_codex_ur_source_pair) src ON src.role_code = r.code
      WHERE r.tenant_id = {TENANT_ID} AND r.deleted = b'0' AND r.status = 0
      GROUP BY r.code
      HAVING COUNT(*) <> 1
    ) duplicate_or_missing_role;
    IF v_bad_count <> 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'target enabled role is not uniquely resolvable';
    END IF;

    DROP TEMPORARY TABLE IF EXISTS tmp_codex_ur_target_pair;
    CREATE TEMPORARY TABLE tmp_codex_ur_target_pair AS
    SELECT u.id AS user_id,
           u.username,
           r.id AS role_id,
           r.code AS role_code
    FROM tmp_codex_ur_source_pair src
    JOIN system_users u
      ON u.username = src.username
     AND u.tenant_id = {TENANT_ID}
     AND u.deleted = b'0'
     AND u.status = 0
    JOIN system_role r
      ON r.code = src.role_code
     AND r.tenant_id = {TENANT_ID}
     AND r.deleted = b'0'
     AND r.status = 0;

    SELECT COUNT(*) INTO v_pair_count FROM tmp_codex_ur_target_pair;
    IF v_pair_count <> v_expected_pair_count THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'resolved target pair count mismatch';
    END IF;

    SELECT COUNT(*) INTO v_bad_count
    FROM (
      SELECT ur.tenant_id, ur.user_id, ur.role_id, COUNT(*) AS row_count
      FROM system_user_role ur
      JOIN system_users u ON u.id = ur.user_id AND u.tenant_id = ur.tenant_id
      JOIN tmp_codex_ur_scope_user scope_user ON scope_user.username = u.username
      WHERE ur.tenant_id = {TENANT_ID}
      GROUP BY ur.tenant_id, ur.user_id, ur.role_id
      HAVING COUNT(*) > 1
    ) duplicate_user_role;
    IF v_bad_count <> 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'target system_user_role contains duplicate scoped pairs';
    END IF;

    SELECT SHA2(COALESCE(GROUP_CONCAT(CONCAT_WS(':', tenant_id, user_id, role_id, HEX(deleted)) ORDER BY tenant_id, user_id, role_id, HEX(deleted) SEPARATOR '|'), ''), 256)
      INTO v_other_tenant_hash_before
      FROM system_user_role
     WHERE tenant_id <> {TENANT_ID};

    UPDATE system_user_role ur
    JOIN system_users u
      ON u.id = ur.user_id
     AND u.tenant_id = ur.tenant_id
     AND u.deleted = b'0'
     AND u.status = 0
    JOIN system_role r
      ON r.id = ur.role_id
     AND r.tenant_id = ur.tenant_id
     AND r.deleted = b'0'
     AND r.status = 0
    JOIN tmp_codex_ur_scope_user scope_user
      ON scope_user.username = u.username
    LEFT JOIN tmp_codex_ur_source_pair src
      ON src.username = u.username
     AND src.role_code = r.code
       SET ur.deleted = b'1',
           ur.updater = v_actor,
           ur.update_time = NOW()
     WHERE ur.tenant_id = {TENANT_ID}
       AND ur.deleted = b'0'
       AND src.username IS NULL;
    SET v_soft_delete_count = ROW_COUNT();

    IF v_soft_delete_count <> v_expected_soft_delete_count THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'soft delete count mismatch';
    END IF;

    UPDATE system_user_role ur
    JOIN tmp_codex_ur_target_pair target_pair
      ON target_pair.user_id = ur.user_id
     AND target_pair.role_id = ur.role_id
       SET ur.deleted = b'0',
           ur.updater = v_actor,
           ur.update_time = NOW()
     WHERE ur.tenant_id = {TENANT_ID}
       AND ur.deleted <> b'0';
    SET v_reactivate_count = ROW_COUNT();

    INSERT INTO system_user_role (
        user_id, role_id, creator, create_time, updater, update_time, deleted, tenant_id
    )
    SELECT target_pair.user_id,
           target_pair.role_id,
           v_actor,
           NOW(),
           v_actor,
           NOW(),
           b'0',
           {TENANT_ID}
    FROM tmp_codex_ur_target_pair target_pair
    WHERE NOT EXISTS (
      SELECT 1
      FROM system_user_role existing
      WHERE existing.tenant_id = {TENANT_ID}
        AND existing.user_id = target_pair.user_id
        AND existing.role_id = target_pair.role_id
    );
    SET v_insert_count = ROW_COUNT();

    IF v_soft_delete_count + v_reactivate_count + v_insert_count <> v_expected_change_count THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'total change count mismatch';
    END IF;

    SELECT COUNT(*) INTO v_missing_count
    FROM tmp_codex_ur_target_pair target_pair
    WHERE NOT EXISTS (
      SELECT 1
      FROM system_user_role ur
      WHERE ur.tenant_id = {TENANT_ID}
        AND ur.user_id = target_pair.user_id
        AND ur.role_id = target_pair.role_id
        AND ur.deleted = b'0'
    );
    IF v_missing_count <> 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'post-sync missing active pair count mismatch';
    END IF;

    SELECT COUNT(*) INTO v_extra_count
    FROM system_user_role ur
    JOIN system_users u
      ON u.id = ur.user_id
     AND u.tenant_id = ur.tenant_id
     AND u.deleted = b'0'
     AND u.status = 0
    JOIN system_role r
      ON r.id = ur.role_id
     AND r.tenant_id = ur.tenant_id
     AND r.deleted = b'0'
     AND r.status = 0
    JOIN tmp_codex_ur_scope_user scope_user
      ON scope_user.username = u.username
    LEFT JOIN tmp_codex_ur_source_pair src
      ON src.username = u.username
     AND src.role_code = r.code
    WHERE ur.tenant_id = {TENANT_ID}
      AND ur.deleted = b'0'
      AND src.username IS NULL;
    IF v_extra_count <> 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'post-sync extra active pair count mismatch';
    END IF;

    SELECT SHA2(COALESCE(GROUP_CONCAT(CONCAT_WS(':', tenant_id, user_id, role_id, HEX(deleted)) ORDER BY tenant_id, user_id, role_id, HEX(deleted) SEPARATOR '|'), ''), 256)
      INTO v_other_tenant_hash_after
      FROM system_user_role
     WHERE tenant_id <> {TENANT_ID};
    IF v_other_tenant_hash_after <> v_other_tenant_hash_before THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'other tenant system_user_role hash changed';
    END IF;

    COMMIT;
    SELECT 'COMMITTED' AS tx_result,
           v_expected_scope_count AS scoped_user_count,
           v_expected_pair_count AS source_pair_count,
           v_soft_delete_count AS soft_deleted_count,
           v_reactivate_count AS reactivated_count,
           v_insert_count AS inserted_count,
           v_other_tenant_hash_after AS other_tenant_user_role_hash;
END$$
DELIMITER ;
CALL codex_20260808_sync_user_role_bindings();
DROP PROCEDURE IF EXISTS codex_20260808_sync_user_role_bindings;
"""

    rollback_temp = create_temp_tables(scope_users, target_effective_pairs_for_scope, "tmp_codex_ur_rb")
    rollback_sql = f"""DROP PROCEDURE IF EXISTS codex_20260808_rollback_user_role_bindings;
DELIMITER $$
CREATE PROCEDURE codex_20260808_rollback_user_role_bindings()
proc: BEGIN
    DECLARE v_actor VARCHAR(64) DEFAULT '{ACTOR}-rollback';
    DECLARE v_scope_count INT DEFAULT 0;
    DECLARE v_pair_count INT DEFAULT 0;
    DECLARE v_expected_scope_count INT DEFAULT {len(scope_users)};
    DECLARE v_expected_pair_count INT DEFAULT {len(target_effective_pairs_for_scope)};
    DECLARE v_soft_delete_count INT DEFAULT 0;
    DECLARE v_reactivate_count INT DEFAULT 0;
    DECLARE v_insert_count INT DEFAULT 0;
    DECLARE v_missing_count INT DEFAULT 0;
    DECLARE v_extra_count INT DEFAULT 0;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;
    SET SESSION group_concat_max_len = 100000000;

{rollback_temp}

    SELECT COUNT(*) INTO v_scope_count FROM tmp_codex_ur_rb_scope_user;
    IF v_scope_count <> v_expected_scope_count THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'rollback scope count mismatch';
    END IF;
    SELECT COUNT(*) INTO v_pair_count FROM tmp_codex_ur_rb_source_pair;
    IF v_pair_count <> v_expected_pair_count THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'rollback pre-state pair count mismatch';
    END IF;

    DROP TEMPORARY TABLE IF EXISTS tmp_codex_ur_rb_target_pair;
    CREATE TEMPORARY TABLE tmp_codex_ur_rb_target_pair AS
    SELECT u.id AS user_id,
           u.username,
           r.id AS role_id,
           r.code AS role_code
    FROM tmp_codex_ur_rb_source_pair src
    JOIN system_users u
      ON u.username = src.username
     AND u.tenant_id = {TENANT_ID}
     AND u.deleted = b'0'
     AND u.status = 0
    JOIN system_role r
      ON r.code = src.role_code
     AND r.tenant_id = {TENANT_ID}
     AND r.deleted = b'0'
     AND r.status = 0;

    UPDATE system_user_role ur
    JOIN system_users u
      ON u.id = ur.user_id
     AND u.tenant_id = ur.tenant_id
     AND u.deleted = b'0'
     AND u.status = 0
    JOIN system_role r
      ON r.id = ur.role_id
     AND r.tenant_id = ur.tenant_id
     AND r.deleted = b'0'
     AND r.status = 0
    JOIN tmp_codex_ur_rb_scope_user scope_user
      ON scope_user.username = u.username
    LEFT JOIN tmp_codex_ur_rb_source_pair src
      ON src.username = u.username
     AND src.role_code = r.code
       SET ur.deleted = b'1',
           ur.updater = v_actor,
           ur.update_time = NOW()
     WHERE ur.tenant_id = {TENANT_ID}
       AND ur.deleted = b'0'
       AND src.username IS NULL;
    SET v_soft_delete_count = ROW_COUNT();

    UPDATE system_user_role ur
    JOIN tmp_codex_ur_rb_target_pair target_pair
      ON target_pair.user_id = ur.user_id
     AND target_pair.role_id = ur.role_id
       SET ur.deleted = b'0',
           ur.updater = v_actor,
           ur.update_time = NOW()
     WHERE ur.tenant_id = {TENANT_ID}
       AND ur.deleted <> b'0';
    SET v_reactivate_count = ROW_COUNT();

    INSERT INTO system_user_role (
        user_id, role_id, creator, create_time, updater, update_time, deleted, tenant_id
    )
    SELECT target_pair.user_id,
           target_pair.role_id,
           v_actor,
           NOW(),
           v_actor,
           NOW(),
           b'0',
           {TENANT_ID}
    FROM tmp_codex_ur_rb_target_pair target_pair
    WHERE NOT EXISTS (
      SELECT 1
      FROM system_user_role existing
      WHERE existing.tenant_id = {TENANT_ID}
        AND existing.user_id = target_pair.user_id
        AND existing.role_id = target_pair.role_id
    );
    SET v_insert_count = ROW_COUNT();

    SELECT COUNT(*) INTO v_missing_count
    FROM tmp_codex_ur_rb_target_pair target_pair
    WHERE NOT EXISTS (
      SELECT 1
      FROM system_user_role ur
      WHERE ur.tenant_id = {TENANT_ID}
        AND ur.user_id = target_pair.user_id
        AND ur.role_id = target_pair.role_id
        AND ur.deleted = b'0'
    );
    IF v_missing_count <> 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'rollback missing active pair count mismatch';
    END IF;

    SELECT COUNT(*) INTO v_extra_count
    FROM system_user_role ur
    JOIN system_users u
      ON u.id = ur.user_id
     AND u.tenant_id = ur.tenant_id
     AND u.deleted = b'0'
     AND u.status = 0
    JOIN system_role r
      ON r.id = ur.role_id
     AND r.tenant_id = ur.tenant_id
     AND r.deleted = b'0'
     AND r.status = 0
    JOIN tmp_codex_ur_rb_scope_user scope_user
      ON scope_user.username = u.username
    LEFT JOIN tmp_codex_ur_rb_source_pair src
      ON src.username = u.username
     AND src.role_code = r.code
    WHERE ur.tenant_id = {TENANT_ID}
      AND ur.deleted = b'0'
      AND src.username IS NULL;
    IF v_extra_count <> 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'rollback extra active pair count mismatch';
    END IF;

    COMMIT;
    SELECT 'ROLLED_BACK' AS tx_result,
           v_expected_scope_count AS scoped_user_count,
           v_expected_pair_count AS restored_pair_count,
           v_soft_delete_count AS soft_deleted_count,
           v_reactivate_count AS reactivated_count,
           v_insert_count AS inserted_count;
END$$
DELIMITER ;
CALL codex_20260808_rollback_user_role_bindings();
DROP PROCEDURE IF EXISTS codex_20260808_rollback_user_role_bindings;
"""

    verify_sql = f"""SET SESSION group_concat_max_len = 100000000;
{temp_sql}

DROP TEMPORARY TABLE IF EXISTS tmp_codex_ur_verify_target_pair;
CREATE TEMPORARY TABLE tmp_codex_ur_verify_target_pair AS
SELECT u.id AS user_id,
       u.username,
       r.id AS role_id,
       r.code AS role_code
FROM tmp_codex_ur_source_pair src
JOIN system_users u
  ON u.username = src.username
 AND u.tenant_id = {TENANT_ID}
 AND u.deleted = b'0'
 AND u.status = 0
JOIN system_role r
  ON r.code = src.role_code
 AND r.tenant_id = {TENANT_ID}
 AND r.deleted = b'0'
 AND r.status = 0;

SELECT CONCAT('VERIFY_COUNTS', CHAR(9),
              (SELECT COUNT(*) FROM tmp_codex_ur_scope_user), CHAR(9),
              (SELECT COUNT(*) FROM tmp_codex_ur_source_pair), CHAR(9),
              (SELECT COUNT(*) FROM tmp_codex_ur_verify_target_pair)) AS rowdata;

SELECT CONCAT('VERIFY_MISSING', CHAR(9), COUNT(*)) AS rowdata
FROM tmp_codex_ur_verify_target_pair target_pair
WHERE NOT EXISTS (
  SELECT 1
  FROM system_user_role ur
  WHERE ur.tenant_id = {TENANT_ID}
    AND ur.user_id = target_pair.user_id
    AND ur.role_id = target_pair.role_id
    AND ur.deleted = b'0'
);

SELECT CONCAT('VERIFY_EXTRA', CHAR(9), COUNT(*)) AS rowdata
FROM system_user_role ur
JOIN system_users u
  ON u.id = ur.user_id
 AND u.tenant_id = ur.tenant_id
 AND u.deleted = b'0'
 AND u.status = 0
JOIN system_role r
  ON r.id = ur.role_id
 AND r.tenant_id = ur.tenant_id
 AND r.deleted = b'0'
 AND r.status = 0
JOIN tmp_codex_ur_scope_user scope_user
  ON scope_user.username = u.username
LEFT JOIN tmp_codex_ur_source_pair src
  ON src.username = u.username
 AND src.role_code = r.code
WHERE ur.tenant_id = {TENANT_ID}
  AND ur.deleted = b'0'
  AND src.username IS NULL;

SELECT CONCAT('VERIFY_WANGSIYU_ROLES', CHAR(9), COALESCE(GROUP_CONCAT(r.code ORDER BY r.code SEPARATOR ','), '')) AS rowdata
FROM system_user_role ur
JOIN system_users u ON u.id = ur.user_id AND u.tenant_id = ur.tenant_id
JOIN system_role r ON r.id = ur.role_id AND r.tenant_id = ur.tenant_id AND r.deleted = b'0' AND r.status = 0
WHERE ur.tenant_id = {TENANT_ID}
  AND ur.deleted = b'0'
  AND u.username = 'wangsiyu';

SELECT CONCAT('VERIFY_DCC_DANGEROUS_GRANTS', CHAR(9), COUNT(DISTINCT CONCAT(u.username, ':', r.code, ':', m.permission))) AS rowdata
FROM system_user_role ur
JOIN system_users u ON u.id = ur.user_id AND u.tenant_id = ur.tenant_id AND u.deleted = b'0' AND u.status = 0
JOIN system_role r ON r.id = ur.role_id AND r.tenant_id = ur.tenant_id AND r.deleted = b'0' AND r.status = 0
JOIN system_role_menu rm ON rm.role_id = r.id AND rm.tenant_id = r.tenant_id AND rm.deleted = b'0'
JOIN system_menu m ON m.id = rm.menu_id AND m.deleted = b'0' AND m.status = 0
WHERE ur.tenant_id = {TENANT_ID}
  AND ur.deleted = b'0'
  AND u.username IN ('wangsiyu', 'zhaohaichen')
  AND m.permission IN ('dcc:controlled-file:directory:manage','dcc:controlled-file:category:manage','dcc:controlled-file:download');

SELECT CONCAT('VERIFY_OTHER_TENANT_HASH', CHAR(9),
              SHA2(COALESCE(GROUP_CONCAT(CONCAT_WS(':', tenant_id, user_id, role_id, HEX(deleted)) ORDER BY tenant_id, user_id, role_id, HEX(deleted) SEPARATOR '|'), ''), 256)) AS rowdata
FROM system_user_role
WHERE tenant_id <> {TENANT_ID};
"""

    (TASK_DIR / "change.sql").write_text(change_sql, encoding="utf-8")
    (TASK_DIR / "rollback.sql").write_text(rollback_sql, encoding="utf-8")
    (TASK_DIR / "verify.sql").write_text(verify_sql, encoding="utf-8")
    print(json.dumps(plan, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
