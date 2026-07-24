# 执行日志：排产员工作台 admin 权限恢复

BDD: 排产员工作台操作权限菜单在本机运行库中完整存在 -> Given 本机运行库已经应用工作台权限拆分迁移 / When 查询 system_menu 与 system_role_menu / Then 应存在 query、update、smoke-test 三类权限菜单及对应角色绑定。
BDD: 本机 admin 登录排产员工作台时可看到允许的操作按钮 -> Given admin 已拥有当前系统设计要求的菜单权限 / When 打开排产员工作台 / Then 前端基于 checkPermi 能显示保存与冒烟测试操作入口。
GREEN: previous-task-check -> PASS, 最近同仓后端任务 task.md 为 COMPLETED。
GREEN: experience-preflight -> PASS, 本轮仅操作本机运行库、后端运行时门禁和本机 admin 登录验证，不访问测试服或正式服。
RED: python -X utf8 (docker exec int-ruoyi-mysql mysql ... 查询 system_menu/system_role_menu/system_user_role) -> FAIL, 本机库仅存在 5590=mes:pro-scheduler-workbench:query；900170/900171 缺失，admin 也未拥有 mes_scheduler 角色。
GREEN: python -X utf8 (docker exec int-ruoyi-mysql mysql < sql/mysql/20260624_mes_scheduler_workbench_permission_split.sql) -> PASS, 本机库已补入 900170=mes:pro-scheduler-workbench:update 与 900171=mes:pro-scheduler-workbench:smoke-test。
GREEN: python -X utf8 (docker exec int-ruoyi-mysql mysql ... INSERT system_user_role user_id=1 role_id=910233 tenant_id=1) -> PASS, admin 已追加排产员角色且未重复写入。
GREEN: python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_restart_int_ruoyi_local_schema.py -k scheduler_workbench_permission_split -> PASS
GREEN: node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 芋道源码 --username admin --password <redacted> --target-path /mes/pro/scheduler-workbench --target-text 排产设置 --timeout 90000 -> PASS
GREEN: python -X utf8 (POST /admin-api/system/auth/login with tenant-id=1, then GET /admin-api/system/auth/get-permission-info) -> PASS, admin 当前同时拥有 mes:pro-scheduler-workbench:query / update / smoke-test，角色列表包含 mes_scheduler。
