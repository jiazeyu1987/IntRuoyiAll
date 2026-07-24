# 任务: 修复 eDHR OQ/PQ 菜单定义冲突阻塞

## 任务目标

修复 `sql/mysql/20260618_mes_edhr_oq_pq_execution_deviation.sql` 在真实页面“部署测试服（v5）”阶段暴露的菜单定义冲突根因：当前脚本在真实测试库执行到 line 357 时抛出 `Invalid eDHR OQ/PQ button menu definition; cannot merge tenant package menu_ids`，导致页面发布链路在测试服 required SQL 阶段 fail fast。

本任务只修改业务仓正式 SQL 发布输入与对应契约测试，不手工改测试服库，不绕过页面发布流程，不靠维护仓脚本临时补丁掩盖错误。

## 上一任务检查

- 上一后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260622-edhr-label-print-menu-id-collision-fix\task.md`
- 状态：`COMPLETED`
- 处理：上一任务修复的 `20260618_mes_edhr_label_print_queue.sql` 已在维护仓真实页面 `deploy-test(v5)` 日志中确认 `APPLIED`，说明 label print 根因已被真实发布链路跨过；当前新的阻塞已切换为 `20260618_mes_edhr_oq_pq_execution_deviation.sql` 的 OQ/PQ 菜单定义冲突，可作为新任务继续处理。

## 用户要求与执行边界

- 用户要求：
  - 必须按真实页面点击流程重新走完整构建、测试服、正式服、备份服发布链路
  - 遇到问题先记录，再修复，再回页面重走
  - 禁止接口替代页面动作
- 本任务边界：
  - 只修复 `20260618_mes_edhr_oq_pq_execution_deviation.sql` 及其测试契约
  - 如需同步更新与 OQ/PQ 菜单唯一性直接相关的测试文件，可一并最小修改
  - 不手工删除或修改测试服真实库菜单作为绕过
  - 修复会改变发布包输入，必须由维护仓重新构建新 `releaseTag` 并从页面重走全链路

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中文档：
    - `D:\ProjectPackage\Int\IntRuoyi\docs\release-backup-restore.md`
    - `D:\ProjectPackage\Int\IntRuoyi\docs\server-access.md`
- 本任务强制门禁摘录：
  - 页面发布链路暴露出的 SQL 阻塞必须修到正式发布输入中，不能用手工改库或跳过门禁掩盖。
  - 菜单类迁移在真实库执行前，必须先用只读证据核对目标 ID 段、父菜单和已存在菜单定义，不能凭记忆挑号。
  - 任何会改变发布包内容的修复，都必须先 RED 后 GREEN，再回维护仓重新构建新包重走页面。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。必须通过修正正式 SQL 的 OQ/PQ 菜单定义和补齐冲突回归测试来收口，而不是靠手工改库或跳过按钮菜单校验。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: OQ/PQ 菜单定义不得与既有 eDHR 菜单冲突 -> Given 真实测试库已存在既有 eDHR 菜单集且当前发布链路在 OQ/PQ SQL 校验按钮菜单定义时失败 / When 执行 20260618_mes_edhr_oq_pq_execution_deviation.sql / Then 迁移必须使用独立且正确的 OQ/PQ 页面与按钮菜单定义，不得复用或污染其他 eDHR 菜单。`
- `BDD: 真实页面 deploy-test 在跨过 label print SQL 后仍必须能继续执行 OQ/PQ SQL -> Given 20260618_mes_edhr_label_print_queue.sql 已在真实测试库成功 APPLIED / When 页面 deploy-test 继续执行 20260618_mes_edhr_oq_pq_execution_deviation.sql / Then 不得再因 OQ/PQ system_menu 定义冲突中断整个测试服发布链路。`

## 里程碑

1. 建立任务文档并固化真实页面 `deploy-test(v5)` 失败证据。`DONE`
2. 只读核对冲突 SQL、现有测试与真实菜单占用，确认根因。`DONE`
3. 先补 RED 契约测试，再最小修复 SQL 菜单定义。`DONE`
4. 运行目标 pytest、迁移策略与缺陷证据回归。`DONE`
5. 回填维护仓主任务，等待重新构建新包并从页面重走。`DONE`

## 预期验证

- `python -X utf8 -m pytest script\tests\test_edhr_oq_pq_schema_sql.py -q` 先 RED 后 GREEN
- `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql` 通过
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260622-edhr-oq-pq-menu-definition-fix\bug-regression-evidence.md` 通过
- 维护仓主任务文档明确记录“OQ/PQ SQL 修复改变发布包输入，必须重新 build-release 并从页面重走测试服/正式服/备份服”

## 当前状态

COMPLETED：业务仓已按严格 TDD 完成 `20260618_mes_edhr_oq_pq_execution_deviation.sql` 的菜单定义冲突修复。只读源码与测试服 `system_menu` 现场共同证明：旧号段 `900290-900295` 中，`900290-900292` 与其他 eDHR SQL 存在直接复用，`900293-900295` 已被 `eDHR统一变更` 占用，因此 OQ/PQ 菜单整体切换到独立未占用的 `900332-900337`，并补入 `tmp_mes_edhr_oq_pq_legacy_menu_map` 用于吸收测试库已落下的旧 OQ/PQ 页面/按钮残留。当前已通过 `test_edhr_oq_pq_schema_sql.py`、`run-release-migration-policy-gate.py --sql-root sql\mysql` 与 bug regression validator；维护仓主任务也应回填“必须以新的 `releaseTag` 重新 build-release，再从真实页面重走测试服/正式服/备份服”的要求。
