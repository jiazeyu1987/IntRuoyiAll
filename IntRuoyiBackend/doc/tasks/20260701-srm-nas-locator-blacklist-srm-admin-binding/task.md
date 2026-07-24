# 任务：SRM管理员 绑定 NAS定位黑名单权限（后端 / SQL）

- Task ID: `20260701-srm-nas-locator-blacklist-srm-admin-binding`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-07-01`
- Current Status: `completed`

## Task Goal

修正 `20260701_srm_t6_nas_locator_blacklist_config.sql` 的菜单 ID 冲突，并把 `NAS定位黑名单 / srm:nas-locator:config` 正式绑定到 `srm_admin` 角色。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260701-srm-nas-locator-blacklist-button-missing\task.md`
- 状态：`completed`
- 处理说明：上一任务已定位根因，不阻塞本轮 SQL 正式修复。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - SQL 读写与数据库回查统一显式 UTF-8；执行后必须做只读验证。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。修复菜单主键冲突并按角色/套餐正式落权限，不手工只补某个账号。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 黑名单菜单不用冲突 ID -> Given 991104 已被 DCC 菜单占用 / When 执行 NAS 黑名单修复 SQL / Then 新菜单使用未冲突的新 ID，且 permission 为 `srm:nas-locator:config`。`
- `BDD: srm_admin 获得黑名单菜单绑定 -> Given system_role 中存在 `srm_admin` / When 执行修复 SQL / Then 各租户 `srm_admin` 都获得新黑名单菜单绑定。`
- `BDD: 持有 srm_admin 的测试租户账号可见按钮 -> Given `aoteman` 持有测试租户 `srm_admin` / When 前端加载权限菜单 / Then `get-permission-info` 中包含 `srm:nas-locator:config`。`

## Milestones

1. M1：建立后端任务台账并确认冲突现状。`completed`
2. M2：补 RED 合同。`completed`
3. M3：实现 SQL 修复并应用本机库。`completed`
4. M4：GREEN 验证与证据回填。`completed`

## Expected Verification

- `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_srm_d7_d10_sql_contract.py -k nas_locator -q`
- `docker exec int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 -D ruoyi-vue-pro -e "<readonly verification sql>"`

## Current Blockers

- 暂无。

## Final Verification Result

- `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_srm_d7_d10_sql_contract.py -k nas_locator -q` -> `PASS`
- 本机库只读回查：
  - `system_menu` 已新增 `991105 / srm:nas-locator:config`
  - `srm_admin` 在租户 `1` 和 `122` 都已绑定 `991105`
  - 测试租户套餐 `113` 已包含 `991105`
- 结论：
  - `SRM管理员` 角色已正式拥有 NAS 黑名单权限，不再依赖冲突的 `991104`

## Current Status

completed
