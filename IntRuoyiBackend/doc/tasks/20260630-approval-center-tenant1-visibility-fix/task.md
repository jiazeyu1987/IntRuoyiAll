# 任务：审批中心 tenant=1 入口权限与审批管理员全量可见修复（后端）

- Task ID: `20260630-approval-center-tenant1-visibility-fix`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-30`
- Current Status: `completed`

## Task Goal

在 `ruoyi-vue-pro` 后端中交付一套正式 SQL 与统一审批中心权限收口方案：为 `tenant_id=1` 新增 `approval_center_entry` 与 `approval_admin` 角色，批量为当前启用用户补齐审批中心入口权限，并让审批中心各 provider 仅在 `approval_admin` 场景放开全量数据与轨迹可见性。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260630-showroom-product-import-create-missing\task.md`
- 状态：`blocked`
- 处理说明：该任务已因用户优先级切换阻塞，不影响本次审批中心权限修复继续推进。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 本次命中 `docs\powershell-memory.md`、`docs\login-access.md`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - SQL、任务文档、执行日志与命令输出统一显式 UTF-8；PowerShell 5.1 不使用 `&&`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
  - 本机真实审批中心登录验证必须先跑 `login-preflight.mjs`；`芋道源码/admin` 仅做最终只读核验，不得旁路登录。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。通过统一 `globalView` 权限计算与 provider 收口，避免在各模块散落硬编码角色判断或前端绕过。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 审批中心入口角色与绑定由正式 SQL 维护 -> Given tenant_id=1 需要新增 approval_center_entry 与 approval_admin / When 执行正式 SQL / Then 脚本幂等创建角色、仅绑定 1200/1221，并按要求绑定用户角色。`
- `BDD: 普通用户只获得入口不获得全量可见 -> Given 普通用户拥有 approval_center_entry 但没有 approval_admin / When 查询审批中心页签和轨迹 / Then provider 继续按当前登录人过滤相关数据。`
- `BDD: approval_admin 在审批中心统一触发全量可见 -> Given admin 拥有 approval_admin / When 查询 BPM、DCC、Showroom、MES_FEEDBACK、SRM、eDHR 的审批中心数据与轨迹 / Then 保留页签语义但去掉当前登录人过滤。`

## Milestones

1. M1：建立后端任务文档并锁定 SQL/权限/验证边界。`completed`
2. M2：补 RED 契约测试，锁定角色、菜单、入口绑定与 provider/globalView 行为。`completed`
3. M3：实现 SQL、统一权限 helper 与 provider/eDHR 收口逻辑。`completed`
4. M4：完成定向测试、本机 SQL 应用与真实验收证据。`in_progress`
4. M4：完成定向测试、本机 SQL 应用与真实验收证据。`completed`

## Expected Verification

- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_approval_center_role_visibility_sql.py -q`
- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-bpm,yudao-module-dcc,yudao-module-showroom,yudao-module-mes,yudao-module-srm,yudao-module-system "-Dtest=ApprovalCenterServiceImplTest,ApprovalCenterTimelineContractTest,BpmNativeApprovalTaskProviderTest,BpmTaskServiceImplApprovalFilterTest,DccApprovalTaskAdapterTest,ShowroomApprovalTaskAdapterTest,MesProFeedbackApprovalTaskAdapterTest,MesProEdhrApprovalTaskAdapterTest,MesProEdhrWorkTaskServiceImplTest,SrmSupplierPortalApprovalTaskAdapterTest,PermissionServiceTest,AuthControllerTest,PermissionCacheStartupRefreshRunnerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql\20260630_approval_center_role_visibility.sql | docker exec -i int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 ruoyi-vue-pro`
- `powershell -NoProfile -ExecutionPolicy Bypass -File D:\ProjectPackage\Int\IntRuoyi\script\deploy\restart-ruoyi-local-component.ps1 -Component backend`

## Final Verification

- `zhaojie`
  - `login-preflight.mjs` 真实登录进入 `/approval-center/todo` 成功。
  - `get-permission-info` 返回 `roles=["approval_center_entry","mes_scheduler"]`，`permissions` 包含 `bpm:task:query`。
  - `/admin-api/approval-center/tasks/page?viewType=TODO&pageNo=1&pageSize=10` 返回 `200/code=0`。
- `admin`
  - `login-preflight.mjs` 真实登录进入 `/approval-center/todo` 成功。
  - `get-permission-info` 返回 `roles` 包含 `approval_center_entry`、`approval_admin`，`permissions` 包含 `bpm:task:query`。
  - `/admin-api/approval-center/tasks/page?viewType=TODO&pageNo=1&pageSize=10` 返回 `200/code=0`，本机样例总数 `11`。
- 数据库回查
  - `system_user_role` 中 `admin` 已绑定 `910295` 与 `910296`；`zhaojie` 仅绑定 `910295`。
  - `tenant_id=1` 启用用户数 `2124`，入口角色绑定启用用户数 `2124`。
- `system_role_menu` 中 `910295/910296` 均未激活 `980104`，仅保留 `1200/1221` 最小菜单集。

## Current Status

completed

## Current Blockers

- 无。
