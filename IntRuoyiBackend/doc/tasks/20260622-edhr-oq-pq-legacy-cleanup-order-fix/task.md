# 任务: 修复 eDHR OQ/PQ legacy cleanup 顺序缺陷

## 任务目标

修复 `sql/mysql/20260618_mes_edhr_oq_pq_execution_deviation.sql` 在真实页面 `deploy-test(v8)` 阶段暴露的新根因：测试服当前真实库在执行到 line 399 时抛出 `Missing eDHR OQ/PQ system_menu rows; cannot merge tenant package menu_ids`。只读现场已证明当前 SQL 会先因旧 OQ/PQ `path/permission` 残留跳过新 `900332-900334` 插入，再删除 legacy `900290-900292`，最终把页面/查询/创建三条一起删空，只剩 `900335-900337`，从而导致测试租户菜单合并门禁失败。

本任务只修改业务仓正式 SQL 发布输入与对应契约测试，不手工改测试服库，不绕过维护仓页面发布流程，不靠维护仓脚本临时补丁掩盖错误。

## 上一任务检查

- 上一后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260622-edhr-oq-pq-menu-definition-fix\task.md`
- 状态：`COMPLETED`
- 处理：上一任务已把 OQ/PQ 正式菜单号段从冲突的 `900290-900295` 切到 `900332-900337`，并加入 legacy map；维护仓真实页面 `build-release(v8)` 也已证明该修复进入 clean 发布包。当前新的阻塞不是号段冲突，而是 SQL 在 legacy cleanup 与新菜单插入的顺序上仍有缺陷，需作为新任务继续处理。

## 用户要求与执行边界

- 用户要求：
  - 必须按真实页面点击流程重新走完整构建、测试服、正式服、备份服发布链路
  - 遇到问题先记录，再修复，再回页面重走
  - 禁止接口替代页面动作
- 本任务边界：
  - 只修复 `20260618_mes_edhr_oq_pq_execution_deviation.sql` 的 legacy cleanup / 新菜单插入顺序问题及其测试契约
  - 如需同步更新与该缺陷直接相关的测试文件，可一并最小修改
  - 不手工插入、删除或修正测试服真实 `system_menu` 作为绕过
  - 修复会改变发布包输入，必须由维护仓重新构建新 `releaseTag` 并从页面重走全链路

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中文档：
    - `D:\ProjectPackage\Int\IntRuoyi\docs\release-backup-restore.md`
    - `D:\ProjectPackage\Int\IntRuoyi\docs\server-access.md`
- 本任务强制门禁摘录：
  - 页面发布链路暴露出的 SQL 阻塞必须修到正式发布输入中，不能用手工改库、跳过 SQL 或重置测试数据掩盖。
  - 远端只读核对可以用于证明真实库状态，但任何测试服写入都必须通过维护仓真实页面重新发布触发。
  - 会改变发布包输入的 SQL 修复必须先 RED 后 GREEN，再回维护仓重新构建新包重走页面。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。必须通过调整正式 SQL 的 legacy cleanup / 新菜单插入顺序，并补齐回归测试来收口。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: legacy OQ/PQ 残留存在时也必须补齐新菜单 -> Given 测试服真实库已残留旧 OQ/PQ 页面/查询/创建菜单并且新 SQL 需要迁移到 900332-900337 / When 执行 20260618_mes_edhr_oq_pq_execution_deviation.sql / Then SQL 必须先吸收 legacy 残留再补齐新菜单，最终 900332-900337 六条都存在。`
- `BDD: 真实页面 deploy-test 在 legacy cleanup 后不得把 OQ/PQ 页面菜单删空 -> Given 维护仓真实页面 deploy-test(v8) 已证明测试库当前只剩 900335-900337 / When 修复后的 SQL 再次参与发布 / Then 不得再因 Missing eDHR OQ/PQ system_menu rows 中断测试服发布链路。`

## 里程碑

1. 建立任务文档并固化维护仓 `deploy-test(v8)` 失败证据。`DONE`
2. 只读核对测试服真实 `system_menu` 与 SQL 顺序，确认根因。`DONE`
3. 先补 RED 契约测试，再最小修复 SQL 顺序。`DONE`
4. 运行目标 pytest、迁移策略与缺陷证据回归。`DONE`
5. 回填维护仓主任务，等待重新构建新包并从页面重走。`TODO`

## 预期验证

- `python -X utf8 -m pytest script\\tests\\test_edhr_oq_pq_schema_sql.py -q` 先 RED 后 GREEN
- `python -X utf8 script\\release\\run-release-migration-policy-gate.py --sql-root sql\\mysql` 通过
- `python -X utf8 C:\\Users\\BJB110\\.codex\\skills\\bug-regression-fix-loop\\scripts\\validate_bug_regression.py --evidence D:\\ProjectPackage\\Int\\IntRuoyi\\ruoyi-vue-pro\\doc\\tasks\\20260622-edhr-oq-pq-legacy-cleanup-order-fix\\bug-regression-evidence.md` 通过
- 维护仓主任务文档明确记录“cleanup 顺序修复改变发布包输入，必须重新 build-release(v9) 并从页面重走测试服/正式服/备份服”

## 当前状态

COMPLETED：维护仓真实页面 `deploy-test(v9)` 已证明本任务修复进入 clean 发布包并成功跨过此前阻塞的 `20260618_mes_edhr_oq_pq_execution_deviation.sql`。当前 SQL 已把 legacy cleanup 前移到 `900332-900334` 新菜单插入之前，`python -X utf8 -m pytest ...test_edhr_oq_pq_schema_sql.py -q`、`python -X utf8 script\\release\\run-release-migration-policy-gate.py --sql-root sql\\mysql` 与 `python -X utf8 ...validate_bug_regression.py --evidence ...bug-regression-evidence.md` 均已 PASS；维护仓新的测试服阻塞已切换到后续 `20260618_mes_edhr_print_policy_reissue_void.sql`，因此本任务目标已实际完成。
