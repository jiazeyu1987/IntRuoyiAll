# 任务：修复 NAS 管理页签菜单乱码

## Goal

修复当前本地系统管理菜单中 `NAS管理` 页签显示乱码的问题，并补上防回归检查，确保后续再次执行 NAS 菜单 SQL 时不会把中文菜单名写成乱码。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql\20260520_system_nas_management_menu.sql`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_system_nas_menu_sql.py`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-system-nas-menu-garbled-fix\**`

## Non-Scope

- 不调整 NAS 连接逻辑本身。
- 不改动前端组件结构。
- 不处理与当前菜单乱码无关的其他菜单或数据库问题。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-system-nas-management-local-verify\task.md`
- Status before this task: `completed`
- Impact: 本地已验证 `NAS管理` 菜单、接口和页面可用；本任务只修复菜单名乱码及其回归防护。

## Milestones

- [x] M1: 创建任务文档并记录乱码复现依据。
- [x] M2: 增加 RED 回归检查，锁定菜单 SQL 的字符集声明要求。
- [x] M3: 修复 SQL 文件并修正当前本地数据库中的乱码菜单记录。
- [x] M4: 运行回归验证并完成收尾。

## Expected Verification

- `python -m pytest ruoyi-vue-pro\script\tests\test_system_nas_menu_sql.py -q`
- 真实 `GET http://127.0.0.1:48081/admin-api/system/auth/get-permission-info` 返回 `NAS管理`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260521-system-nas-menu-garbled-fix --mode preview`

## Current Status

Completed on 2026-05-21. 已补齐 SQL 字符集声明，并修正本地库中 `5900-5903` 的乱码菜单记录；运行中的权限菜单已恢复正确中文名称。

## Blockers And Impact

- Blocker: none.
- Impact:
  - 后续再次执行 `20260520_system_nas_management_menu.sql` 时，将以 `utf8mb4` 会话写入中文菜单名。
  - 当前本地系统管理菜单中的 `NAS管理` 已恢复正常显示。

## Final Verification Result

- RED 复现：
  - 真实 `GET /admin-api/system/auth/get-permission-info` 中，`component=system/nas/index` 的菜单名曾返回乱码 `NASç®¡ç†`。
  - `20260520_system_nas_management_menu.sql` 在修复前缺少 `SET NAMES utf8mb4;`。
- 修复动作：
  - 在 `sql/mysql/20260520_system_nas_management_menu.sql` 顶部加入 `SET NAMES utf8mb4;`
  - 通过本地 SQL 修正 `system_menu.id in (5900,5901,5902,5903)` 的中文名称
- GREEN 验证：
  - `python -m pytest ruoyi-vue-pro\script\tests\test_system_nas_menu_sql.py -q` -> PASS
  - 本地数据库 `system_menu` 查询：`5900-5903` 分别为 `NAS管理 / NAS配置查询 / NAS配置保存 / NAS连接测试`
  - 重新登录后 `GET http://127.0.0.1:48081/admin-api/system/auth/get-permission-info` -> PASS，`component=system/nas/index` 对应 `name=NAS管理`
