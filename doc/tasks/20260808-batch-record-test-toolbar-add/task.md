# 批记录测试页工具栏单行与新增按钮

## Task Goal

- 将“批记录测试”页面黄框内的筛选控件与测试租户控件显示在同一行。
- 在同一行增加“新增”按钮，并提供可保存的新测试任务入口，避免只显示空按钮。

## Milestones

- [x] M1: 建立任务文档、BDD 场景和预期验证。
- [x] M2: 先补充静态合同，锁定工具栏单行布局和新增按钮行为。
- [x] M3: 实现页面布局和新增任务弹框/写入内存列表逻辑。
- [x] M4: 运行目标静态合同、类型检查和差异检查。
- [x] M5: 收尾清理并记录最终验证结果。

## Expected Verification

- `node IntRuoyiFronted\tests\e2e\edhr-batch-record-test-tab-static.spec.cjs`
- `pnpm ts:check` in `IntRuoyiFronted`
- `git diff --check -- IntRuoyiFronted\src\views\mes\pro\edhr-batch\BatchRecordTestPage.vue IntRuoyiFronted\tests\e2e\edhr-batch-record-test-tab-static.spec.cjs doc\tasks\20260808-batch-record-test-toolbar-add`

## Current Status

completed

## Applicable Experience Gates

- `docs/frontend-development.md#前端按钮文案与行为一致性门禁`: “新增”必须绑定正式新增入口，不能只改文案或复用刷新/查询动作。
- `docs/frontend-development.md#Vue Scoped Slot 静态合同门禁`: 静态合同需聚焦 `UnifiedListTemplate` 的 `#actions` slot，避免宽泛正则误判。
- `docs/frontend-development.md#复合输入控件交互保留门禁`: 保留测试租户 `el-select` 的可选择能力，不用文本或只读输入替换。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，通过标准列表模板的 `singleLineToolbar` 能力和正式新增弹框解决布局与动作入口。
- `是否存在临时补丁或绕过`：否。
