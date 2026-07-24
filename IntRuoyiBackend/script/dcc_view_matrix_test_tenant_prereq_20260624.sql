-- Local test-tenant prerequisite data for the DCC Excel view matrix seed.
-- This script is intentionally limited to tenant_id=122.
-- It prepares real persistent test departments, users, and department leaders
-- so sql/mysql/20260624_dcc_view_matrix_independent_seed.sql can be verified
-- without writing the protected 芋道源码 tenant.

SET @dcc_view_matrix_test_tenant_id := 122;
SET @dcc_view_matrix_test_prereq_actor := 'dcc_view_matrix_test_prereq_20260624';

DROP PROCEDURE IF EXISTS apply_dcc_view_matrix_test_prereq_20260624;
DELIMITER $$
CREATE PROCEDURE apply_dcc_view_matrix_test_prereq_20260624()
BEGIN
  DECLARE v_template_password varchar(100) DEFAULT NULL;
  DECLARE v_root_dept_id bigint DEFAULT NULL;

  IF @dcc_view_matrix_test_tenant_id IS NULL OR @dcc_view_matrix_test_tenant_id <> 122 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'VIEW_MATRIX_TEST_TENANT_ONLY';
  END IF;

  SELECT MAX(password)
    INTO v_template_password
  FROM system_users
  WHERE tenant_id = @dcc_view_matrix_test_tenant_id
    AND username = 'aoteman'
    AND deleted = b'0'
    AND status = 0;

  IF v_template_password IS NULL OR v_template_password = '' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'VIEW_MATRIX_TEST_PREREQ_AOTEMAN_MISSING';
  END IF;

  SELECT MAX(id)
    INTO v_root_dept_id
  FROM system_dept
  WHERE tenant_id = @dcc_view_matrix_test_tenant_id
    AND name = '顶级部门'
    AND parent_id = 0
    AND deleted = b'0'
    AND status = 0;

  IF v_root_dept_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'VIEW_MATRIX_TEST_PREREQ_ROOT_DEPT_MISSING';
  END IF;

  START TRANSACTION;

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_view_matrix_test_dept_plan;
  CREATE TEMPORARY TABLE tmp_dcc_view_matrix_test_dept_plan (
    dept_key varchar(64) NOT NULL,
    parent_key varchar(64) NOT NULL,
    parent_name varchar(30) NOT NULL,
    name varchar(30) NOT NULL,
    sort int NOT NULL,
    PRIMARY KEY (dept_key)
  ) ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

  INSERT INTO tmp_dcc_view_matrix_test_dept_plan (dept_key, parent_key, parent_name, name, sort) VALUES
    ('ytyl', 'ROOT', '顶级部门', '瑛泰医疗', 1000),
    ('quality', 'ROOT', '顶级部门', '质量体系中心', 1010),
    ('rd', 'ROOT', '顶级部门', '研发创新中心', 1020),
    ('supply', 'ROOT', '顶级部门', '供应链中心', 1030),
    ('regsvc', 'ytyl', '瑛泰医疗', '注册服务中心', 1040),
    ('inspection', 'ytyl', '瑛泰医疗', '检测中心', 1050),
    ('production', 'ytyl', '瑛泰医疗', '生产制造中心', 1060),
    ('marketing', 'ytyl', '瑛泰医疗', '市场营销中心', 1070),
    ('qa', 'quality', '质量体系中心', 'QA', 1110),
    ('qc', 'quality', '质量体系中心', 'QC', 1120),
    ('qms', 'quality', '质量体系中心', 'QMS', 1130),
    ('newproduct', 'rd', '研发创新中心', '新品开发部', 1210),
    ('equipment', 'rd', '研发创新中心', '设备开发部', 1220),
    ('packaging', 'supply', '供应链中心', '包装设计组', 1310),
    ('plan', 'supply', '供应链中心', '生产计划', 1320),
    ('purchase', 'supply', '供应链中心', '生产采购', 1330),
    ('regdept', 'regsvc', '注册服务中心', '注册部', 1410);

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_view_matrix_test_parent_plan;
  CREATE TEMPORARY TABLE tmp_dcc_view_matrix_test_parent_plan AS
  SELECT * FROM tmp_dcc_view_matrix_test_dept_plan;

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_view_matrix_test_grand_plan;
  CREATE TEMPORARY TABLE tmp_dcc_view_matrix_test_grand_plan AS
  SELECT * FROM tmp_dcc_view_matrix_test_dept_plan;

  INSERT INTO system_dept
    (name, parent_id, sort, leader_user_id, status, creator, create_time, updater, update_time, deleted, tenant_id)
  SELECT plan.name, v_root_dept_id, plan.sort, NULL, 0,
         @dcc_view_matrix_test_prereq_actor, NOW(), @dcc_view_matrix_test_prereq_actor, NOW(), b'0',
         @dcc_view_matrix_test_tenant_id
  FROM tmp_dcc_view_matrix_test_dept_plan plan
  WHERE plan.parent_key = 'ROOT'
    AND NOT EXISTS (
      SELECT 1
      FROM system_dept existing
      WHERE existing.tenant_id = @dcc_view_matrix_test_tenant_id
        AND existing.deleted = b'0'
        AND existing.status = 0
        AND existing.name = plan.name
        AND existing.parent_id = v_root_dept_id
    );

  INSERT INTO system_dept
    (name, parent_id, sort, leader_user_id, status, creator, create_time, updater, update_time, deleted, tenant_id)
  SELECT plan.name, parent_dept.id, plan.sort, NULL, 0,
         @dcc_view_matrix_test_prereq_actor, NOW(), @dcc_view_matrix_test_prereq_actor, NOW(), b'0',
         @dcc_view_matrix_test_tenant_id
  FROM tmp_dcc_view_matrix_test_dept_plan plan
  JOIN system_dept parent_dept
    ON parent_dept.tenant_id = @dcc_view_matrix_test_tenant_id
   AND parent_dept.deleted = b'0'
   AND parent_dept.status = 0
   AND parent_dept.name = plan.parent_name
   AND parent_dept.parent_id = v_root_dept_id
  WHERE plan.parent_key = 'ytyl'
    AND NOT EXISTS (
      SELECT 1
      FROM system_dept existing
      WHERE existing.tenant_id = @dcc_view_matrix_test_tenant_id
        AND existing.deleted = b'0'
        AND existing.status = 0
        AND existing.name = plan.name
        AND existing.parent_id = parent_dept.id
    );

  INSERT INTO system_dept
    (name, parent_id, sort, leader_user_id, status, creator, create_time, updater, update_time, deleted, tenant_id)
  SELECT plan.name, parent_dept.id, plan.sort, NULL, 0,
         @dcc_view_matrix_test_prereq_actor, NOW(), @dcc_view_matrix_test_prereq_actor, NOW(), b'0',
         @dcc_view_matrix_test_tenant_id
  FROM tmp_dcc_view_matrix_test_dept_plan plan
  JOIN tmp_dcc_view_matrix_test_parent_plan parent_plan
    ON parent_plan.dept_key = plan.parent_key
  LEFT JOIN tmp_dcc_view_matrix_test_grand_plan grand_plan
    ON grand_plan.dept_key = parent_plan.parent_key
  LEFT JOIN system_dept grand_dept
    ON grand_dept.tenant_id = @dcc_view_matrix_test_tenant_id
   AND grand_dept.deleted = b'0'
   AND grand_dept.status = 0
   AND grand_dept.name = grand_plan.name
   AND grand_dept.parent_id = v_root_dept_id
  JOIN system_dept parent_dept
    ON parent_dept.tenant_id = @dcc_view_matrix_test_tenant_id
   AND parent_dept.deleted = b'0'
   AND parent_dept.status = 0
   AND parent_dept.name = parent_plan.name
   AND parent_dept.parent_id = CASE
     WHEN parent_plan.parent_key = 'ROOT' THEN v_root_dept_id
     ELSE grand_dept.id
   END
  WHERE plan.parent_key NOT IN ('ROOT', 'ytyl')
    AND NOT EXISTS (
      SELECT 1
      FROM system_dept existing
      WHERE existing.tenant_id = @dcc_view_matrix_test_tenant_id
        AND existing.deleted = b'0'
        AND existing.status = 0
        AND existing.name = plan.name
        AND existing.parent_id = parent_dept.id
    );

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_view_matrix_test_parent_resolved;
  CREATE TEMPORARY TABLE tmp_dcc_view_matrix_test_parent_resolved AS
  SELECT plan.dept_key,
         CASE
           WHEN plan.parent_key = 'ROOT' THEN 1
           ELSE COUNT(parent_dept.id)
         END AS resolved_count,
         CASE
           WHEN plan.parent_key = 'ROOT' THEN v_root_dept_id
           ELSE MAX(parent_dept.id)
         END AS dept_id
  FROM tmp_dcc_view_matrix_test_dept_plan plan
  LEFT JOIN tmp_dcc_view_matrix_test_parent_plan parent_plan
    ON parent_plan.dept_key = plan.parent_key
  LEFT JOIN tmp_dcc_view_matrix_test_grand_plan grand_plan
    ON grand_plan.dept_key = parent_plan.parent_key
  LEFT JOIN system_dept grand_dept
    ON grand_dept.tenant_id = @dcc_view_matrix_test_tenant_id
   AND grand_dept.deleted = b'0'
   AND grand_dept.status = 0
   AND grand_dept.name = grand_plan.name
   AND grand_dept.parent_id = v_root_dept_id
  LEFT JOIN system_dept parent_dept
    ON parent_dept.tenant_id = @dcc_view_matrix_test_tenant_id
   AND parent_dept.deleted = b'0'
   AND parent_dept.status = 0
   AND parent_dept.name = parent_plan.name
   AND parent_dept.parent_id = CASE
     WHEN parent_plan.parent_key = 'ROOT' THEN v_root_dept_id
     ELSE grand_dept.id
   END
  GROUP BY plan.dept_key, plan.parent_key;

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_view_matrix_test_dept_resolved;
  CREATE TEMPORARY TABLE tmp_dcc_view_matrix_test_dept_resolved AS
  SELECT plan.dept_key,
         plan.name,
         COUNT(dept.id) AS resolved_count,
         MAX(dept.id) AS dept_id,
         parent_resolved.dept_id AS parent_dept_id
  FROM tmp_dcc_view_matrix_test_dept_plan plan
  LEFT JOIN tmp_dcc_view_matrix_test_parent_resolved parent_resolved
    ON parent_resolved.dept_key = plan.dept_key
   AND parent_resolved.resolved_count = 1
  LEFT JOIN system_dept dept
    ON dept.tenant_id = @dcc_view_matrix_test_tenant_id
   AND dept.deleted = b'0'
   AND dept.status = 0
   AND dept.name = plan.name
   AND dept.parent_id = parent_resolved.dept_id
  GROUP BY plan.dept_key, plan.name, parent_resolved.dept_id;

  IF EXISTS (SELECT 1 FROM tmp_dcc_view_matrix_test_dept_resolved WHERE resolved_count <> 1) THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'VIEW_MATRIX_TEST_PREREQ_DEPT_RESOLUTION_FAILED';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_view_matrix_test_user_plan;
  CREATE TEMPORARY TABLE tmp_dcc_view_matrix_test_user_plan (
    username varchar(30) NOT NULL,
    nickname varchar(30) NOT NULL,
    dept_key varchar(64) NOT NULL,
    PRIMARY KEY (username)
  ) ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

  INSERT INTO tmp_dcc_view_matrix_test_user_plan (username, nickname, dept_key) VALUES
    ('dccmatrixqa', 'DCC矩阵-QA', 'qa'),
    ('dccmatrixqc', 'DCC矩阵-QC', 'qc'),
    ('dccmatrixqms', 'DCC矩阵-QMS', 'qms'),
    ('dccmatrixpackaging', 'DCC矩阵-包装设计', 'packaging'),
    ('dccmatrixmarket', 'DCC矩阵-市场营销', 'marketing'),
    ('dccmatrixregsvc', 'DCC矩阵-注册服务', 'regsvc'),
    ('dccmatrixnewproduct', 'DCC矩阵-新品开发', 'newproduct'),
    ('dccmatrixinspection', 'DCC矩阵-检测中心', 'inspection'),
    ('dccmatrixregdept', 'DCC矩阵-注册部', 'regdept'),
    ('dccmatrixproduction', 'DCC矩阵-生产中心', 'production'),
    ('dccmatrixplan', 'DCC矩阵-生产计划', 'plan'),
    ('dccmatrixpurchase', 'DCC矩阵-生产采购', 'purchase'),
    ('dccmatrixequipment', 'DCC矩阵-设备开发', 'equipment'),
    ('dccmatrixqualitylead', 'DCC矩阵-质量主管', 'quality'),
    ('dccmatrixrdlead', 'DCC矩阵-研发主管', 'rd'),
    ('dccmatrixsupplylead', 'DCC矩阵-供应链主管', 'supply');

  INSERT INTO system_users
    (username, password, password_update_time, nickname, remark, dept_id, post_ids, email, mobile, sex, avatar,
     status, login_ip, login_date, creator, create_time, updater, update_time, deleted, tenant_id)
  SELECT plan.username, v_template_password, NOW(), plan.nickname,
         'DCC view matrix test tenant prerequisite user',
         dept.dept_id, NULL, '', '', 0, '', 0, '', NULL,
         @dcc_view_matrix_test_prereq_actor, NOW(), @dcc_view_matrix_test_prereq_actor, NOW(), b'0',
         @dcc_view_matrix_test_tenant_id
  FROM tmp_dcc_view_matrix_test_user_plan plan
  JOIN tmp_dcc_view_matrix_test_dept_resolved dept
    ON dept.dept_key = plan.dept_key
  WHERE NOT EXISTS (
    SELECT 1
    FROM system_users existing
    WHERE existing.tenant_id = @dcc_view_matrix_test_tenant_id
      AND existing.deleted = b'0'
      AND existing.username = plan.username
  );

  UPDATE system_users user_table
  JOIN tmp_dcc_view_matrix_test_user_plan plan
    ON plan.username = user_table.username
  JOIN tmp_dcc_view_matrix_test_dept_resolved dept
    ON dept.dept_key = plan.dept_key
  SET user_table.nickname = plan.nickname,
      user_table.dept_id = dept.dept_id,
      user_table.status = 0,
      user_table.password = IF(user_table.password IS NULL OR user_table.password = '', v_template_password, user_table.password),
      user_table.remark = 'DCC view matrix test tenant prerequisite user',
      user_table.updater = @dcc_view_matrix_test_prereq_actor,
      user_table.update_time = NOW()
  WHERE user_table.tenant_id = @dcc_view_matrix_test_tenant_id
    AND user_table.deleted = b'0';

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_view_matrix_test_user_resolved;
  CREATE TEMPORARY TABLE tmp_dcc_view_matrix_test_user_resolved AS
  SELECT plan.username,
         COUNT(user_table.id) AS resolved_count,
         MAX(user_table.id) AS user_id
  FROM tmp_dcc_view_matrix_test_user_plan plan
  LEFT JOIN system_users user_table
    ON user_table.tenant_id = @dcc_view_matrix_test_tenant_id
   AND user_table.deleted = b'0'
   AND user_table.status = 0
   AND user_table.username = plan.username
  GROUP BY plan.username;

  IF EXISTS (SELECT 1 FROM tmp_dcc_view_matrix_test_user_resolved WHERE resolved_count <> 1) THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'VIEW_MATRIX_TEST_PREREQ_USER_RESOLUTION_FAILED';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_view_matrix_test_leader_plan;
  CREATE TEMPORARY TABLE tmp_dcc_view_matrix_test_leader_plan (
    dept_key varchar(64) NOT NULL,
    username varchar(30) NOT NULL,
    PRIMARY KEY (dept_key)
  ) ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

  INSERT INTO tmp_dcc_view_matrix_test_leader_plan (dept_key, username) VALUES
    ('quality', 'dccmatrixqualitylead'),
    ('rd', 'dccmatrixrdlead'),
    ('supply', 'dccmatrixsupplylead'),
    ('qc', 'dccmatrixqc'),
    ('packaging', 'dccmatrixpackaging'),
    ('newproduct', 'dccmatrixnewproduct'),
    ('production', 'dccmatrixproduction'),
    ('purchase', 'dccmatrixpurchase');

  UPDATE system_dept dept_table
  JOIN tmp_dcc_view_matrix_test_dept_resolved dept
    ON dept.dept_id = dept_table.id
  JOIN tmp_dcc_view_matrix_test_leader_plan leader
    ON leader.dept_key = dept.dept_key
  JOIN tmp_dcc_view_matrix_test_user_resolved user_resolved
    ON user_resolved.username = leader.username
  SET dept_table.leader_user_id = user_resolved.user_id,
      dept_table.updater = @dcc_view_matrix_test_prereq_actor,
      dept_table.update_time = NOW()
  WHERE dept_table.tenant_id = @dcc_view_matrix_test_tenant_id
    AND dept_table.deleted = b'0'
    AND dept_table.status = 0;

  IF EXISTS (
    SELECT 1
    FROM tmp_dcc_view_matrix_test_leader_plan leader
    JOIN tmp_dcc_view_matrix_test_dept_resolved dept
      ON dept.dept_key = leader.dept_key
    JOIN tmp_dcc_view_matrix_test_user_resolved user_resolved
      ON user_resolved.username = leader.username
    JOIN system_dept dept_table
      ON dept_table.id = dept.dept_id
     AND dept_table.tenant_id = @dcc_view_matrix_test_tenant_id
    WHERE dept_table.leader_user_id <> user_resolved.user_id
  ) THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'VIEW_MATRIX_TEST_PREREQ_LEADER_RESOLUTION_FAILED';
  END IF;

  COMMIT;

  SELECT @dcc_view_matrix_test_tenant_id AS tenant_id,
         (SELECT COUNT(*) FROM tmp_dcc_view_matrix_test_dept_resolved) AS prepared_department_count,
         (SELECT COUNT(*) FROM tmp_dcc_view_matrix_test_user_resolved) AS prepared_user_count,
         (SELECT COUNT(*) FROM tmp_dcc_view_matrix_test_leader_plan) AS prepared_leader_count;
END$$
DELIMITER ;

CALL apply_dcc_view_matrix_test_prereq_20260624();
DROP PROCEDURE IF EXISTS apply_dcc_view_matrix_test_prereq_20260624;
