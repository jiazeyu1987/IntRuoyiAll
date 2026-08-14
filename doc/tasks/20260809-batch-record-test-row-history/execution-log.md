# Execution Log

- Task ID: `20260809-batch-record-test-row-history`
- Created: `2026-08-09`

## BDD Scenarios

- BDD: 当前行重新测试先清空历史 -> Given 行 A 已有一次 Codex CLI 回复，When 再次点击行 A 的测试，Then 只清空行 A 的旧回复，历史按钮立即变灰且不可查看，行 B 的历史保持不变。
- BDD: 终态回复归属当前行 -> Given 行 A 启动后得到 executionId，When 正式结果接口返回终态，Then 仅当 `caseName`、executionId 和轮询 token 同时匹配时写入行 A，历史按钮变绿。
- BDD: 不同行回复隔离 -> Given 行 A 和行 B 先后执行，When 分别点击两行历史，Then 各自弹窗展示自己的执行编号、任务标题和 Codex CLI 回复，不得串行或被过期轮询覆盖。
- BDD: 执行异常不伪造历史 -> Given 启动或读取结果失败，When 页面处理错误，Then 当前行历史保持灰色且错误明确提示，不生成默认成功或其它行回复。

## Root Cause

- 当前页面使用单一全局 `testResult` 保存 executionId、rowTitle、loading、error 和 data；点击测试时立即打开同一弹窗。
- 页面没有按行保存结果的状态，也没有历史按钮，因此用户无法区分“本行无回复、执行中、已有终态回复”。
- 当前轮询只校验全局 `testResult.executionId`；需要进一步把稳定 `caseName` 与 executionId 一起冻结，避免后续行或过期异步返回写入错误历史。

## TDD Evidence

- RED: `node .\tests\e2e\edhr-batch-record-test-row-history-static.spec.cjs` -> FAIL，四张列表均缺少逐行历史按钮，actual=0、expected=4。
- GREEN: `node .\tests\e2e\edhr-batch-record-test-row-history-static.spec.cjs` -> PASS。
- RED: `node .\tests\e2e\edhr-batch-record-test-row-history-static.spec.cjs` -> FAIL，处理函数缺少同步执行中门禁，快速连点可能在 Vue 禁用状态刷新前创建多个执行批次。
- GREEN: `node .\tests\e2e\edhr-batch-record-test-row-history-static.spec.cjs` -> PASS，`handleTestRow` 在调用启动接口前同步阻止重复执行。
- REGRESSION: `edhr-batch-record-test-tab-static.spec.cjs`、`edhr-batch-record-test-description-wrap-static.spec.cjs`、`edhr-batch-record-test-order-allocation-static.spec.cjs` -> PASS。
- GREEN: `pnpm ts:check` -> PASS，退出码 0。
- Implementation: 四张列表均增加历史按钮；初始/执行中为 disabled info，终态正式结果后为 success；测试启动不再自动打开结果弹窗。
- Isolation: `rowTestHistories` 按唯一 `caseName` 保存；启动冻结 run token，轮询写入同时校验 historyKey、executionId 和 poll token；结果弹窗只复制所点击行的终态快照。
- Lifecycle: 重新测试先清空当前行；描述成功修改或删除行时丢弃该行旧历史；启动/读取失败保持灰色并显示正式错误。
- Concurrency: `handleTestRow` 在异步启动前同步检查全局执行中状态，防止快速连点绕过按钮禁用刷新并产生多个批次。
- E2E attempt: 首次登录后从真实菜单进入目标页时遇到 Vite HMR 旧 setup 状态；全新 reload 随后发现 `48081` 监听消失。第二次 fresh 登录被后端租户接口 500 阻塞，复核时 `48081` 已无监听。
- Runtime blocker evidence: 并行后端任务的 `active-order-manual-sort` 启动日志报 `NoClassDefFoundError: MesReportAllocationSnapshot`，启动失败后共享 `int_main` 后端未恢复。本任务未修改后端，也未抢占或覆盖并行任务运行态。
- Runtime recovery: `48081` 恢复监听、`8081` 返回 200 后，使用 fresh Playwright 从 `MES 系统 -> eDHR批记录 -> 批记录测试` 进入真实页面；租户下拉明确存在“测试租户”，因此满足写入型 E2E 门禁。
- E2E: 1440x900 下首屏历史按钮均为 disabled/info，documentWidth=viewportWidth=1440，首行“测试/历史/修改/删除”无重叠。
- E2E: 第一次验证期间同一组件被其它任务热更新，终态历史随组件内存重建而清空；待源码写入稳定后在一个连续会话重新执行两行，未将热更新清空误判为产品缺陷。
- E2E: 行 A 执行 `132`，运行时 A/B 历史均灰；A 终态后仅 A 绿。随后行 B 执行 `133`，B 运行时 A 保持绿、B 灰；B 终态后两行均绿。
- E2E: 行 A 弹窗标题匹配“工艺路线生产组长配置”、行 B 匹配“批记录解析与工序配置”，两者均显示 Codex CLI 回复；再次打开 A 仍为 executionId `132`，与 B 的 `133` 隔离。
- E2E evidence: `output/playwright/batch-record-test-row-history/browser-verification.json`、`history-dialog-row-a.png`、`history-buttons-two-rows-green.png`。
- Final regression: 在并入并发任务最新源码后重跑历史合同、Tab 合同、描述换行合同、订单分配合同及 `pnpm ts:check`，全部 PASS。
- Evidence validators: `validate_frontend_feature.py`、`validate_bug_regression.py` 和 `browser-verification.json` JSON 解析均 PASS。
- Experience: 已合并到 `docs/frontend-development.md#前端行级异步结果归属门禁`，并更新 `docs/experience-index.md`。
- Closeout: `task_closeout.py --mode preview` 无 blocked/warnings，范围仅包含两份临时技能 evidence 和本任务 `.playwright-cli`；`--mode apply` 成功删除三项，保留 task/execution-log/verification-report、两张脱敏截图和 browser-verification.json。

## Blockers

- 已解除：共享后端和前端恢复后完成真实两行验证。共享 Playwright 随后被其它任务关闭为 `about:blank`，不影响已保存的本任务终态证据。
