# 任务：修复 MES 智能排产角色范围 SQL 的测试服发布兼容性

## 任务目标

- 修复 `sql/mysql/20260629_mes_smart_scheduling_role_scope.sql` 与 `sql/mysql/20260629_mes_smart_scheduling_role_assignment.sql` 在测试服真实升级路径上的发布阻塞。
- 保持正式幂等 SQL 交付，不使用手工改库或发布阶段绕过。
- 让测试服 `build-release -> publish-test` 能在 tenant `1` 缺少 `车间主任 / mes_workshop_director` 历史基线时按目标角色范围自动收口。

## 上一任务检查

- 上一个相关后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-mes-smart-scheduling-role-scope-check\task.md`
- 状态：`COMPLETED`
- 处理说明：上一任务已完成本机运行库角色范围与账号绑定收口，但其 SQL 合同尚未覆盖测试服真实历史基线差异，本次作为独立发布兼容修复继续处理。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\release-backup-restore.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\server-access.md`
- 适用强制门禁：
  - PowerShell、SSH、MySQL 与中文日志/SQL 交互必须显式 UTF-8，复杂远端 SQL 优先用 UTF-8 感知运行时而不是多层 here-string 直传。
  - 发布链路问题优先按“契约层 -> 脚本层 -> 环境层”排查；本次先核 required SQL 契约与测试服真实数据。
  - 影响发布链路的 SQL 修改必须补契约测试或门禁验证，不得只做本机人工跑通。
  - 涉及服务器只读核验前必须明确目标主机、目标库和授权范围；本次只允许测试服 `172.30.30.58`，数据库 `ruoyi-vue-pro`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。通过正式幂等 SQL 覆盖测试服缺失历史角色基线的升级路径，而不是发布时手工补数据。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 测试服缺少车间主任历史角色时仍可升级 -> Given tenant 1 已存在排产员目标用户与智能排产菜单基线但缺少车间主任角色 / When 执行角色范围迁移 SQL / Then SQL 应补齐正式车间主任角色并继续收敛菜单权限，而不是直接因缺角色失败。`
- `BDD: 角色分配迁移复用角色范围迁移结果 -> Given 角色范围迁移已确保排产员/车间主任/班组长角色存在 / When 执行账号角色分配 SQL / Then zhaojie 绑定排产员，guliya/wuxiaolei/zhangjiayi 绑定车间主任，并保持幂等。`
- `BDD: 发布链路只接受已提交修复 -> Given 测试服 publish-test 真实执行 required SQL / When 重新构建并部署同一 releaseTag 闭环 / Then required SQL 不再因缺少 mes_workshop_director 历史基线而阻塞。`

## 里程碑

1. M1：建立发布兼容修复任务台账并记录真实阻塞证据。`COMPLETED`
2. M2：补 RED 测试覆盖测试服缺失车间主任历史角色的升级路径。`COMPLETED`
3. M3：最小修改角色范围/角色分配 SQL 并完成 GREEN 验证。`COMPLETED`
4. M4：提交后端修复并回写维护仓发布输入。`IN_PROGRESS`
5. M5：重新构建并发布测试服验证通过。`PENDING`

## 预期验证

- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_scope_sql.py -q`
- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_scope_related_tabs_sql.py -q`
- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_assignment_sql.py -q`
- 只读核验测试服 `172.30.30.58` / `ruoyi-vue-pro` 的 `system_role`、`system_users`、`system_user_role`
- 后续由维护仓重跑真实 `build-release -> publish-test`

## 当前状态

IN_PROGRESS：维护仓真实 `publish-test(v9)` 已继续跨过 `20260629_mes_smart_scheduling_role_scope.sql`，但失败点后移到 `20260629_mes_smart_scheduling_role_assignment.sql`，错误为：

- `ERROR 1644 (45000) at line 121: Missing enabled MES workshop director role in tenant 1 for assignment`

当前根因已进一步收敛：

- `role_scope.sql` 本轮已被真实标记 `APPLIED`
- 但测试服历史上若已存在同名或同 code 的 `车间主任 / 班组长` 角色，且其状态为禁用或软删，当前 `role_scope.sql` 只会在“完全不存在”时新建角色，无法保证后续 `role_assignment.sql` 一定能拿到“启用且未删除”的目标角色

当前已进入第四轮发布兼容修复：在 `20260629_mes_smart_scheduling_role_scope.sql` 中先恢复 `车间主任 / 班组长` 历史角色为启用未删除，再在缺失时补建，并显式解析单一可用角色 ID 供后续角色菜单/角色分配复用；对应契约测试已补齐并通过，随后继续最小提交、重建干净 release worktree，并由维护仓重跑真实 `build-release -> publish-test`。
