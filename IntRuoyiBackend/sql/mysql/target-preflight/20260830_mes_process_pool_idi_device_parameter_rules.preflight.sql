-- release-target-preflight: migrationId=20260830_mes_process_pool_idi_device_parameter_rules; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*)
   FROM information_schema.tables
   WHERE table_schema = DATABASE()
     AND LOWER(table_name) IN (
       'dcc_project_code',
       'mes_pro_process_pool_device_parameter_rule',
       'mes_pro_process_pool_team_device',
       'mes_pro_process_pool_team_process_device',
       'mes_pro_route',
       'mes_pro_route_dcc_project_binding',
       'mes_pro_route_process',
       'system_users'
     )) = 8
  AND (SELECT COUNT(*)
       FROM dcc_project_code
       WHERE tenant_id = 1
         AND project_code = 'IDI'
         AND project_name = _utf8mb4 0xe68c89e58e8be5bc8fe79083e59b8ae689a9e58585e58e8be58a9be6b3b5
         AND status = 'ENABLE'
         AND deleted = 0) = 1
  AND (SELECT COUNT(*)
       FROM dcc_project_code project
       JOIN mes_pro_route_dcc_project_binding binding
         ON binding.dcc_project_code_id = project.id
        AND binding.tenant_id = project.tenant_id
        AND binding.deleted = b'0'
        AND binding.active_route_id = binding.route_id
       JOIN mes_pro_route target_route
         ON target_route.id = binding.route_id
        AND target_route.tenant_id = project.tenant_id
        AND target_route.deleted = b'0'
        AND target_route.code = 'RT000028-IDI'
       WHERE project.tenant_id = 1
         AND project.project_code = 'IDI'
         AND project.project_name = _utf8mb4 0xe68c89e58e8be5bc8fe79083e59b8ae689a9e58585e58e8be58a9be6b3b5
         AND project.status = 'ENABLE'
         AND project.deleted = 0) = 1
  AND (SELECT COUNT(*)
       FROM dcc_project_code project
       JOIN mes_pro_route_dcc_project_binding binding
         ON binding.dcc_project_code_id = project.id
        AND binding.tenant_id = project.tenant_id
        AND binding.deleted = b'0'
        AND binding.active_route_id = binding.route_id
       JOIN mes_pro_route target_route
         ON target_route.id = binding.route_id
        AND target_route.tenant_id = project.tenant_id
        AND target_route.deleted = b'0'
        AND target_route.code = 'RT000028-IDI'
       JOIN mes_pro_route_process target_route_process
         ON target_route_process.route_id = target_route.id
        AND target_route_process.tenant_id = target_route.tenant_id
        AND target_route_process.deleted = b'0'
       JOIN mes_pro_process_pool_team_process_device target_binding
         ON target_binding.tenant_id = target_route_process.tenant_id
        AND target_binding.process_id = target_route_process.process_id
        AND target_binding.enabled = b'1'
        AND target_binding.deleted = b'0'
       JOIN system_users leader
         ON leader.id = target_binding.leader_user_id
        AND leader.tenant_id = target_binding.tenant_id
        AND leader.username = 'admin'
        AND leader.deleted = b'0'
       JOIN mes_pro_process_pool_team_device target_device
         ON target_device.id = target_binding.device_id
        AND target_device.leader_user_id = target_binding.leader_user_id
        AND target_device.tenant_id = target_binding.tenant_id
        AND target_device.device_status = 'ENABLED'
        AND target_device.enabled = b'1'
        AND target_device.deleted = b'0'
       WHERE project.tenant_id = 1
         AND project.project_code = 'IDI'
         AND project.project_name = _utf8mb4 0xe68c89e58e8be5bc8fe79083e59b8ae689a9e58585e58e8be58a9be6b3b5
         AND project.status = 'ENABLE'
         AND project.deleted = 0) > 0
  AND (SELECT COUNT(*)
       FROM dcc_project_code project
       JOIN mes_pro_route_dcc_project_binding binding
         ON binding.dcc_project_code_id = project.id
        AND binding.tenant_id = project.tenant_id
        AND binding.deleted = b'0'
        AND binding.active_route_id = binding.route_id
       JOIN mes_pro_route target_route
         ON target_route.id = binding.route_id
        AND target_route.tenant_id = project.tenant_id
        AND target_route.deleted = b'0'
        AND target_route.code = 'RT000028-IDI'
       JOIN mes_pro_route_process target_route_process
         ON target_route_process.route_id = target_route.id
        AND target_route_process.tenant_id = target_route.tenant_id
        AND target_route_process.deleted = b'0'
       JOIN mes_pro_process_pool_team_process_device target_binding
         ON target_binding.tenant_id = target_route_process.tenant_id
        AND target_binding.process_id = target_route_process.process_id
        AND target_binding.enabled = b'1'
        AND target_binding.deleted = b'0'
       JOIN mes_pro_process_pool_team_device target_device
         ON target_device.id = target_binding.device_id
        AND target_device.leader_user_id = target_binding.leader_user_id
        AND target_device.tenant_id = target_binding.tenant_id
        AND target_device.device_status = 'ENABLED'
        AND target_device.enabled = b'1'
        AND target_device.deleted = b'0'
       JOIN mes_pro_process_pool_device_parameter_rule target_rule
         ON target_rule.tenant_id = target_route_process.tenant_id
        AND target_rule.leader_user_id = target_binding.leader_user_id
        AND target_rule.route_process_id = target_route_process.id
        AND target_rule.process_id = target_route_process.process_id
        AND target_rule.device_id = target_device.id
        AND target_rule.enabled = b'1'
        AND target_rule.deleted = b'0'
       WHERE project.tenant_id = 1
         AND project.project_code = 'IDI'
         AND project.project_name = _utf8mb4 0xe68c89e58e8be5bc8fe79083e59b8ae689a9e58585e58e8be58a9be6b3b5
         AND project.status = 'ENABLE'
         AND project.deleted = 0) >= 37
  AND (SELECT COUNT(DISTINCT target_rule.parameter_code)
       FROM dcc_project_code project
       JOIN mes_pro_route_dcc_project_binding binding
         ON binding.dcc_project_code_id = project.id
        AND binding.tenant_id = project.tenant_id
        AND binding.deleted = b'0'
        AND binding.active_route_id = binding.route_id
       JOIN mes_pro_route target_route
         ON target_route.id = binding.route_id
        AND target_route.tenant_id = project.tenant_id
        AND target_route.deleted = b'0'
        AND target_route.code = 'RT000028-IDI'
       JOIN mes_pro_route_process target_route_process
         ON target_route_process.route_id = target_route.id
        AND target_route_process.tenant_id = target_route.tenant_id
        AND target_route_process.deleted = b'0'
        AND target_route_process.sort = 1
       JOIN mes_pro_process_pool_team_process_device target_binding
         ON target_binding.tenant_id = target_route_process.tenant_id
        AND target_binding.process_id = target_route_process.process_id
        AND target_binding.enabled = b'1'
        AND target_binding.deleted = b'0'
       JOIN mes_pro_process_pool_team_device target_device
         ON target_device.id = target_binding.device_id
        AND target_device.device_code = 'B09393'
        AND target_device.leader_user_id = target_binding.leader_user_id
        AND target_device.tenant_id = target_binding.tenant_id
        AND target_device.device_status = 'ENABLED'
        AND target_device.enabled = b'1'
        AND target_device.deleted = b'0'
       JOIN mes_pro_process_pool_device_parameter_rule target_rule
         ON target_rule.tenant_id = target_route_process.tenant_id
        AND target_rule.leader_user_id = target_binding.leader_user_id
        AND target_rule.route_process_id = target_route_process.id
        AND target_rule.process_id = target_route_process.process_id
        AND target_rule.device_id = target_device.id
        AND target_rule.enabled = b'1'
        AND target_rule.deleted = b'0'
        AND target_rule.parameter_code IN (
          'IDIJSON_01_B09393_01',
          'IDIJSON_01_B09393_02',
          'IDIJSON_01_B09393_03',
          'IDIJSON_01_B09393_04',
          'IDIJSON_01_B09393_05'
        )
       WHERE project.tenant_id = 1
         AND project.project_code = 'IDI'
         AND project.project_name = _utf8mb4 0xe68c89e58e8be5bc8fe79083e59b8ae689a9e58585e58e8be58a9be6b3b5
         AND project.status = 'ENABLE'
         AND project.deleted = 0) = 5
THEN 'TARGET_PREFLIGHT_PASS:20260830_mes_process_pool_idi_device_parameter_rules'
ELSE 'TARGET_PREFLIGHT_BLOCKED:20260830_mes_process_pool_idi_device_parameter_rules'
END;
