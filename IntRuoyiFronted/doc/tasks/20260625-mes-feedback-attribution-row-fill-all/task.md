# 任务：确认归属弹窗行内全部按钮迁移

## 任务目标

将 `src/views/mes/pro/feedback/ImportAttributionDialog.vue` 顶部数量区的全局 `全部` 按钮迁移到候选订单工序表格行内右侧空白区，改为“每行一个 `全部` 按钮”；保持现有归属请求结构、分配校验、默认数量规则和成功事件载荷不变。

## 里程碑

- [x] M1：创建任务文档并记录经验门禁、设计约束检查与 BDD 场景。
- [x] M2：先修改静态契约测试，锁定“顶部移除 + 行内新增”的 RED 失败。
- [x] M3：改造确认归属弹窗布局与行内 `全部` 按钮交互。
- [x] M4：运行定向静态验证并补齐执行证据。

## 预期验证

- `node tests/e2e/mes-feedback-simulated-import-static.spec.js`
- `node tests/e2e/mes-feedback-attribution-process-picker-static.spec.js`

## 当前状态

已完成。确认归属弹窗顶部全局 `全部` 按钮已迁移为表格每行一个 `全部` 按钮，相关静态合同已通过。

## 前一任务检查

- 前端前一最近任务 `20260624-unified-electronic-signature-primary-tab` 已标记完成，允许继续本任务。
- 当前前端仓库存在其他未归属脏改动；本任务只修改确认归属弹窗、相关静态测试和本任务文档，不覆盖其他改动。

## 经验门禁

- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：弹窗表格、按钮尺寸、状态与间距保持密集操作台风格，不新增装饰性结构。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。按钮迁移后继续暴露真实分配与提交错误，不新增兜底路径。
- `是否从根因和长期维护角度解决`：是。通过行内操作位承接单行填满逻辑，避免顶部全局按钮与行级分配语义错位。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 行内全部按钮迁移 -> Given 用户打开确认归属弹窗 When 查看订单工序列表 Then 顶部不再显示全局全部按钮，且每行右侧操作位显示对应全部按钮。`
- `BDD: 行内全部按钮复用原分配逻辑 -> Given 某行存在可分配数量 When 用户点击该行全部 Then 该行分配数量按现有规则自动填满，并保持勾选联动和提交校验不变。`

## Cleanup Keep

- `doc/tasks/20260625-mes-feedback-attribution-row-fill-all/task.md`
- `doc/tasks/20260625-mes-feedback-attribution-row-fill-all/execution-log.md`
- `doc/tasks/20260625-mes-feedback-attribution-row-fill-all/frontend-feature-evidence.md`
