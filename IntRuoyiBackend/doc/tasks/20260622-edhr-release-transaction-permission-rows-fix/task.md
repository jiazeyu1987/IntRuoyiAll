# 任务: 修复 eDHR 放行事务 permission 行缺失导致的正式服上线失败

## 任务目标

修复维护仓真实页面 `上线正式服(v12)` 首次暴露的新根因：正式服执行 `sql/mysql/20260618_mes_edhr_release_transaction_lifecycle.sql` 到 line `341` 时抛出 `Missing eDHR release transaction permission rows; cannot merge tenant package menu_ids`。修复必须落在正式发布输入与对应回归测试中，不允许手工改正式服库、不允许跳过 SQL、也不允许通过维护仓脚本增加临时绕过。

## 上一任务检查

- 上一后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260622-edhr-dhr-template-menu-id-collision-fix\task.md`
- 状态：`COMPLETED_WAITING_MAIN`
- 处理：上一后端任务已完成 DHR 模板菜单主键冲突修复，且维护仓真实页面 `上线正式服(v12)` 日志已证明 `20260618_mes_edhr_dhr_template_lifecycle.sql` 成功跨过。当前新的阻塞已切换到后续 `20260618_mes_edhr_release_transaction_lifecycle.sql` 的 permission 行缺失，因此需作为新任务继续处理。

## 用户要求与执行边界

- 用户要求：
  - 主程序合并后必须重新走一遍“构建发布包 -> 部署测试服 -> 标记测试通过 -> 上线正式服 -> 上线备份服”的真实页面链路
  - 遇到问题先记录，再修复，再回页面重走
  - 禁止接口替代页面动作
- 本任务边界：
  - 只修复 `20260618_mes_edhr_release_precheck_engine.sql`、`20260618_mes_edhr_release_transaction_lifecycle.sql` 及其直接相关的回归测试/证据
  - 允许做正式服失败日志与业务 SQL 的只读核对，不允许手工写正式服 `system_menu` 或 `system_role_menu`
  - 本任务任何修复都会改变发布包输入；维护仓必须使用新的 `releaseTag` 从真实页面重新 `build-release -> deploy-test -> mark-tested -> prod -> backup`

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中文档：
    - `D:\ProjectPackage\Int\IntRuoyi\docs\release-backup-restore.md`
    - `D:\ProjectPackage\Int\IntRuoyi\docs\server-access.md`
    - `D:\ProjectPackage\Int\IntRuoyi\docs\worktree-memory.md`
- 本任务强制门禁摘录：
  - 发布链路暴露出的正式 SQL 问题必须回到业务发布输入中修复，不能靠手工改正式库、跳过 SQL 或测试数据回填掩盖。
  - 对正式服的现场核对只能做只读证据收集；任何写入必须回到维护仓真实页面发布流程触发。
  - 修复发布输入前必须先有 RED 契约，再做最小 SQL 修改，并在维护仓重新构建新包验证。
  - clean release worktree 的 cherry-pick/验证必须保持成对路径和分支一致；未在合并结果上验证前，不得视为完成或清理 worktree。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。必须让 release 预检切片能纠正 legacy `900263/900264` permission 漂移，避免后续 lifecycle SQL 再因同一 drift 失败。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: legacy submit/approve 菜单 permission 漂移不能让 lifecycle 在正式服失败 -> Given 正式库中 900263/900264 旧菜单 id 可能已存在但 permission 与当前发布契约不一致 / When 执行 20260618_mes_edhr_release_precheck_engine.sql 与 20260618_mes_edhr_release_transaction_lifecycle.sql / Then SQL 必须先把 900263/900264 纠正到 submit/approve 正式权限，再继续 lifecycle 的 permission 合并门禁。`
- `BDD: 修复后真实页面 promote-prod 不得再因 release transaction permission 缺失中断 -> Given 维护仓真实页面 prod(v12) 已证明当前失败点是 release_transaction_lifecycle SQL / When 修复后的 SQL 重新进入 clean 发布包并参与真实页面发布 / Then 正式服发布链路不得再在该 SQL 阶段因 Missing eDHR release transaction permission rows 失败。`
- `BDD: 放行事务按钮不得复用 traveler 菜单号段 -> Given traveler SQL 已把 900266/900267/900268 固定为 eDHR流转单菜单 / When 执行 release transaction lifecycle SQL / Then reject、withdraw、event-query 必须切到独立号段并同步更新 package/menu 与 role_menu 合并逻辑。`

## 里程碑

1. 建立任务文档并固化维护仓 `prod(v12)` 失败证据。`DONE`
2. 只读核对 `precheck_engine` / `lifecycle` SQL 与正式服失败证据，确认根因。`DONE`
3. 先补 RED 契约测试，再最小修复 SQL。`DONE`
4. 运行目标 pytest、迁移策略与缺陷证据回归。`DONE`
5. 回填维护仓主任务并准备 clean release worktree 更新。`DONE`

## 预期验证

- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyiWorktrees\ruoyi-vue-pro-mainmerge-release-clean\script\tests\test_edhr_release_precheck_schema_sql.py -q` 通过
- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyiWorktrees\ruoyi-vue-pro-mainmerge-release-clean\script\tests\test_edhr_release_transaction_schema_sql.py -q` 先 RED 后 GREEN
- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyiWorktrees\ruoyi-vue-pro-mainmerge-release-clean\script\tests\test_edhr_traveler_schema_sql.py -q` 通过
- `python -X utf8 D:\ProjectPackage\Int\IntRuoyiWorktrees\ruoyi-vue-pro-mainmerge-release-clean\script\release\run-release-migration-policy-gate.py --sql-root D:\ProjectPackage\Int\IntRuoyiWorktrees\ruoyi-vue-pro-mainmerge-release-clean\sql\mysql` 通过
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyiWorktrees\ruoyi-vue-pro-mainmerge-release-clean\doc\tasks\20260622-edhr-release-transaction-permission-rows-fix\bug-regression-evidence.md` 通过
- 维护仓主任务明确记录“release transaction 修复改变发布包输入，必须用新的 releaseTag 从真实页面重走测试服/正式服/备份服”

## 当前状态

COMPLETED_WAITING_MAIN：维护仓真实页面 `上线正式服(v14)` 暴露的 release transaction 第二阶段根因已在 clean release backend worktree 收口。当前只读证据已证明正式库 `900266/900267/900268` 仍被 traveler 菜单占用，而旧 lifecycle SQL 仍错误复用这组三号段；正式修复已把 `reject / withdraw / event-query` 切换到独立的 `900353/900354/900355`，并同步更新 `system_menu` 校验、package menu merge 与 `system_role_menu` 合并逻辑。验证已通过 `python -X utf8 -m pytest ...test_edhr_release_transaction_schema_sql.py -q`、`python -X utf8 -m pytest ...test_edhr_traveler_schema_sql.py -q`、`python -X utf8 -m pytest ...test_edhr_release_precheck_schema_sql.py -q`、`python -X utf8 ...run-release-migration-policy-gate.py --sql-root ...` 与 `validate_bug_regression.py --evidence ...bug-regression-evidence.md`。下一步必须回维护仓改用新的 `releaseTag`，从真实页面重新执行 `build-release -> deploy-test -> mark-tested -> prod -> backup`。
