# 任务：排产员工作台参数编辑权限修复并落测试服

- Task ID: `20260629-test-server-zhaojie-scheduler-update-permission-fix`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-29`
- Current Status: `in_progress`

## Task Goal

从正式 SQL 根因修复 `tenant_id=1` 下排产员角色缺失 `mes:pro-scheduler-workbench:update` 的问题，先在本地运行库完成 TDD 修复与验证，再把同一正式 SQL 受控应用到测试服务器，使 `zhaojie` 可编辑排产员参数。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-smart-scheduling-smoke-one-shot-package\task.md`
- 状态：`completed`
- 处理说明：上一任务已完成，不阻塞本次权限修复。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 本次需命中 `docs/powershell-memory.md`、`docs/server-access.md`、`docs/release-backup-restore.md`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - PowerShell 与 SQL/任务文档读写统一显式 UTF-8。
- `D:\ProjectPackage\Int\IntRuoyi\docs\server-access.md`
  - 测试服务器只允许在当前授权范围内执行最小 SQL 修复与只读核验。
- `D:\ProjectPackage\Int\IntRuoyi\docs\release-backup-restore.md`
  - 涉及测试服真实库写入前，必须遵守环境确认、目标库确认、失败即阻断与证据记录门禁。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。直接修正式角色范围 SQL，让排产员角色白名单与“参数可编辑”的权限设计一致。
- `是否存在临时补丁或绕过`：否。禁止手工临时改库替代正式幂等 SQL。

## BDD 场景

- `BDD: 排产员角色保留工作台设置保存权限 -> Given 排产员角色拥有排产员工作台查询权限 / When 角色范围 SQL 收敛智能排产菜单 / Then 排产员仍保留 900170=mes:pro-scheduler-workbench:update，可编辑工作台参数。`
- `BDD: 排产员角色不额外获得冒烟测试权限 -> Given 本次只修复参数编辑问题 / When 执行角色范围 SQL / Then 排产员默认不新增 900171=mes:pro-scheduler-workbench:smoke-test。`
- `BDD: 本地与测试服应用同一正式 SQL 后行为一致 -> Given 本地库与测试服库都执行同一份幂等迁移 / When 回查角色菜单绑定 / Then 排产员角色在两端都拥有 5590 与 900170。`

## Milestones

1. M1：建立任务文档并确认高风险环境门禁。`completed`
2. M2：补 RED 测试锁定排产员白名单缺口。`in_progress`
3. M3：修改正式 SQL 并完成本地 GREEN 验证。`completed`
4. M4：应用本地运行库并只读核验菜单绑定。`completed`
5. M5：应用测试服务器并完成远端只读核验。`completed`

## Expected Verification

- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_scope_sql.py -q`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql\20260629_mes_smart_scheduling_role_scope.sql | docker exec -i int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 ruoyi-vue-pro`
- 本地只读回查 `system_role_menu`：排产员角色应包含 `5590` 与 `900170`。
- 测试服只读回查 `system_role_menu`：排产员角色应包含 `5590` 与 `900170`。

## Current Blockers

- 无。

## Final Verification Result

- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_scope_sql.py -q` -> PASS
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql\20260629_mes_smart_scheduling_role_scope.sql | docker exec -i int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 ruoyi-vue-pro` -> PASS
- 本地只读回查 `system_role_menu` -> PASS，`排产员(role_id=910233)` 当前拥有 `5590` 与 `900170`，未拥有 `900171`。
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql\20260629_mes_smart_scheduling_role_scope.sql | ssh root@172.30.30.58 "docker exec -i intruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 ruoyi-vue-pro"` -> PASS
- 测试服只读回查 `system_role_menu` -> PASS，`排产员(role_id=910216)` 当前拥有 `5590` 与 `900170`，未拥有 `900171`。

## Completion Result

- 已从正式 SQL 根因修复：`20260629_mes_smart_scheduling_role_scope.sql` 的排产员白名单补入 `900170=mes:pro-scheduler-workbench:update`。
- 修复范围保持最小化：本次没有给排产员额外加 `900171=mes:pro-scheduler-workbench:smoke-test`。
- 已在本地运行库和测试服务器应用同一份正式幂等 SQL，并完成数据库侧菜单绑定回查。
- 数据库层面 `zhaojie` 所属的 `排产员` 角色现在已经具备参数编辑所需权限；前端重新登录后应可编辑排产员参数。

## Current Status

completed
