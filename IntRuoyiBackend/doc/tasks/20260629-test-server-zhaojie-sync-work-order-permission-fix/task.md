# 任务：排产员同步工单权限正式修复并落测试服

- Task ID: `20260629-test-server-zhaojie-sync-work-order-permission-fix`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-29`
- Current Status: `in_progress`

## Task Goal

从正式角色范围 SQL 根因修复 `tenant_id=1` 下 `排产员` 点击 `同步工单` 时缺少 `admission-diff / create-from-work-orders` 权限的问题，并在本地通过 TDD 验证后，将同一正式 SQL 受控应用到测试服务器。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-scheduler-workbench-full-config-package\task.md`
- 状态：`blocked`
- 处理说明：已因用户优先切换到测试服真实权限故障而显式阻塞，不与本次权限修复混合交付。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 本次命中 `docs/powershell-memory.md`、`docs/server-access.md`、`docs/release-backup-restore.md`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - PowerShell 与 SQL/任务文档读写统一显式 UTF-8。
- `D:\ProjectPackage\Int\IntRuoyi\docs\server-access.md`
  - 测试服务器只允许执行当前授权范围内的最小 SQL 修复与只读核验。
- `D:\ProjectPackage\Int\IntRuoyi\docs\release-backup-restore.md`
  - 涉及测试服真实库写入前，必须确认目标主机、目标库与恢复阻断语义。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。直接修正式角色范围 SQL 白名单，并补对应回归测试，避免后续再次被角色收敛误删。
- `是否存在临时补丁或绕过`：否。禁止以手工临时加菜单绑定代替最终 SQL 交付。

## BDD 场景

- `BDD: 排产员保留同步工单诊断与创建权限 -> Given 排产员拥有排产工单页签 / When 角色范围 SQL 收敛智能排产菜单 / Then 排产员仍保留 5581=query、5582=create、5584=admission-diff、5585=preflight。`
- `BDD: 车间主任保留排产工单最小查询权限 -> Given 车间主任保留排产工单页签 / When 角色范围 SQL 收敛智能排产菜单 / Then 车间主任至少保留 5581=query，避免页面本身不可读。`
- `BDD: 本地与测试服应用同一正式 SQL 后行为一致 -> Given 本地库与测试服库都执行同一份幂等迁移 / When 回查角色菜单绑定 / Then 排产员角色在两端都具备同步工单所需关键菜单绑定。`

## Milestones

1. M1：建立后端任务文档并确认高风险环境门禁。`completed`
2. M2：补 RED 测试锁定角色范围 SQL 缺口。`completed`
3. M3：修改正式 SQL 并完成本地 GREEN 验证。`completed`
4. M4：应用本地运行库并只读核验菜单绑定。`completed`
5. M5：应用测试服务器并完成远端只读核验。`completed`

## Expected Verification

- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_scope_sql.py -q`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql\20260629_mes_smart_scheduling_role_scope.sql | docker exec -i int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 ruoyi-vue-pro`
- 测试服只读回查 `system_role_menu`：`role_id=910216` 应拥有 `5581/5582/5584/5585`。

## Final Verification Result

- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_scope_sql.py -q` -> PASS，13 passed。
- `git show HEAD:sql/mysql/20260629_mes_smart_scheduling_role_scope.sql` + Python 断言 -> RED FAIL，旧版 `scheduler` 白名单缺少 `5581`，连带缺 `5582/5584/5585`。
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql\20260629_mes_smart_scheduling_role_scope.sql | docker exec -i int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 ruoyi-vue-pro` -> PASS。
- 本地只读回查 `排产员(role_id=910233)` -> PASS，当前拥有 `5581/5582/5584/5585/5590/900170`，未拥有 `900171`。
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql\20260629_mes_smart_scheduling_role_scope.sql | ssh root@172.30.30.58 "docker exec -i intruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 ruoyi-vue-pro"` -> PASS。
- 测试服只读回查 `排产员(role_id=910216)` -> PASS，当前拥有 `5581/5582/5584/5585/5590/900170`，未拥有 `900171`。

## Completion Result

- 正式 SQL `20260629_mes_smart_scheduling_role_scope.sql` 已补齐 `排产员` 的排产工单同步链路权限：`5581=query`、`5582=create`、`5584=admission-diff`、`5585=preflight`。
- 同时为 `车间主任` 保留最小 `5581=query`，避免保留页签却无查询权限的断链问题。
- 同一份正式 SQL 已在本地运行库与测试服务器应用并通过数据库回查。

## Current Blockers

- 无。

## Current Status

completed
