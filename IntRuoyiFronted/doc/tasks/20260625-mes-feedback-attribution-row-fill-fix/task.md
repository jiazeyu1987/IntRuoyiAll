# 任务：修复归属弹窗行内全部按钮与无限大显示

## 任务目标

修复 `src/views/mes/pro/feedback/ImportAttributionDialog.vue` 中候选订单工序表格的行内 `全部` 按钮点击无效问题，并将红框内的 `999999` 哨兵值按业务语义显示为“无限大”；保持现有归属请求结构、勾选联动、提交校验和无 fallback 基线不变。

## 里程碑

- [x] M1：创建任务文档，记录经验门禁、设计约束检查与 BDD 场景。
- [x] M2：先补 RED 静态回归，锁定“行内全部按计划/剩余取值”和“999999 显示无限大”。
- [x] M3：最小修改归属弹窗数量规则与显示逻辑。
- [x] M4：运行定向静态验证与类型检查，补齐执行证据。

## 预期验证

- `node tests/e2e/mes-feedback-attribution-row-fill-static.spec.js`
- `node tests/e2e/mes-feedback-simulated-import-static.spec.js`
- `pnpm ts:check`

## 当前状态

已完成

## 前一任务检查

- 前端最近任务 `20260625-mes-feedback-attribution-row-fill-all` 已标记完成，允许继续本任务。
- 本任务只修改确认归属弹窗、定向静态测试与任务文档，不覆盖其他未归属改动。

## 经验门禁

- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：保持密集操作台风格，不新增装饰性结构，行内按钮仍使用紧凑操作尺寸。
- `docs/experience-index.md`：本任务仅做本机源码与静态验证，不执行真实写入型 E2E，因此暂不触发 `experience-preflight` 高风险门禁。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。数量规则与显示异常直接按正式业务语义修正，不增加兼容分支。
- `是否从根因和长期维护角度解决`：是。把“全部”按钮的填充规则与“无限大”哨兵显示收口到显式函数，避免 UI 按钮存在但业务取值错误。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 当前订单点击全部按计划上限填充 -> Given 某候选订单工序计划 99、实际 3412 / When 用户点击该行全部 / Then 分配数量显示 99。`
- `BDD: 当前订单点击全部按剩余上限填充 -> Given 某候选订单工序计划 99、剩余 43 / When 用户点击该行全部 / Then 分配数量显示 43。`
- `BDD: 其他订单计划哨兵显示无限大 -> Given 其他订单候选的 plannedQuantity 为 999999 / When 用户查看数量列 / Then 计划显示“无限大”而不是数字或短横线。`
- `BDD: 行内全部保持勾选联动 -> Given 某候选行未勾选且可分配 / When 用户点击该行全部 / Then 该行自动勾选并写入目标分配数量。`

## Cleanup Keep

- `doc/tasks/20260625-mes-feedback-attribution-row-fill-fix/task.md`
- `doc/tasks/20260625-mes-feedback-attribution-row-fill-fix/execution-log.md`
- `doc/tasks/20260625-mes-feedback-attribution-row-fill-fix/frontend-feature-evidence.md`

## 最终验证结果

- `node tests/e2e/mes-feedback-attribution-row-fill-static.spec.js`：PASS
- `node tests/e2e/mes-feedback-simulated-import-static.spec.js`：PASS
- `pnpm ts:check`：BLOCKED，存在与本任务无关的前端仓库既有类型错误，详见 `execution-log.md`
