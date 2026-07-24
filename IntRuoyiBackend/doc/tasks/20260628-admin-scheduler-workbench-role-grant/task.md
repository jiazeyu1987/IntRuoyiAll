# 任务：排产员工作台 admin 权限恢复

## 任务目标

- 修复本机 `tenant_id=1 / 芋道源码 / admin` 无法操作 `排产员工作台` 的权限链路。
- 确认并补齐本机运行时缺失的正式 SQL 权限迁移。
- 让后续本机后端重启自动保持该权限修复结果。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260628-approval-center-provider-error-fix\task.md`
- 状态：`COMPLETED`
- 处理说明：上一后端任务已完成，本次单独处理排产员工作台权限恢复。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- 适用强制门禁：
  - 本机 `admin` 租户数据写入已获得用户明确授权，但仍必须限制在最小必要的角色/菜单权限范围。
  - 真实登录验证前，先记录 `GREEN: experience-preflight -> PASS`。
  - PowerShell 与 SQL 文件读写必须显式 UTF-8。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是补齐正式迁移门禁并修正本机运行库缺失权限。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 排产员工作台操作权限菜单在本机运行库中完整存在 -> Given 本机运行库已经应用工作台权限拆分迁移 / When 查询 system_menu 与 system_role_menu / Then 应存在 query、update、smoke-test 三类权限菜单及对应角色绑定。`
- `BDD: 本机 admin 登录排产员工作台时可看到允许的操作按钮 -> Given admin 已拥有当前系统设计要求的菜单权限 / When 打开排产员工作台 / Then 前端基于 checkPermi 能显示保存与冒烟测试操作入口。`

## 里程碑

1. M1：建立任务文档与执行日志。`COMPLETED`
2. M2：定位 DB 根因与运行时门禁缺口。`COMPLETED`
3. M3：补充脚本/测试并执行本机权限修复。`COMPLETED`
4. M4：完成真实验证与证据回写。`COMPLETED`

## 预期验证

- 运行时脚本/测试验证 `20260624_mes_scheduler_workbench_permission_split.sql` 已纳入本机迁移门禁。
- 本机数据库验证 `900170/900171` 菜单和必要角色绑定存在。
- `admin` 真实登录到 `/mes/pro/scheduler-workbench` 后，工作台操作入口恢复。

## 最终验证结果

- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_restart_int_ruoyi_local_schema.py -k scheduler_workbench_permission_split` -> PASS
- `node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 芋道源码 --username admin --password <redacted> --target-path /mes/pro/scheduler-workbench --target-text 排产设置 --timeout 90000` -> PASS
- `python -X utf8 (POST /admin-api/system/auth/login + GET /admin-api/system/auth/get-permission-info)` -> PASS，返回 `mes:pro-scheduler-workbench:query/update/smoke-test` 三个权限。

## 完成记录

- 根因确认：本机运行库漏应用正式迁移 `20260624_mes_scheduler_workbench_permission_split.sql`，导致 `900170/900171` 工作台操作权限菜单缺失。
- 已按正式 SQL 把 `900170/900171` 补入本机库，并为 `super_admin(role_id=1)` 恢复对应菜单绑定。
- 已按用户要求给 `admin(user_id=1)` 追加 `排产员(role_id=910233, code=mes_scheduler)` 角色。
- 已将 `20260624_mes_scheduler_workbench_permission_split.sql` 纳入本机重启脚本 `script/deploy/restart-int-ruoyi-local.ps1` 的必检迁移，并补充对应脚本测试，避免后续本机重启回退。

## 当前阻塞

- 无。

## Current Status

completed
