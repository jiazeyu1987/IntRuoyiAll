# Task: 批记录测试黄框区域隐藏

## Goal

按截图要求隐藏批记录测试页黄框区域：顶部说明页头不显示；列表工具栏右侧 Runner 状态、刷新状态和“显示字段”入口不显示；保留页签、测试租户下拉和表格行操作。

## Milestones

- [x] M1：定位截图黄框对应的页面模板与标准列表工具栏来源。
- [x] M2：补充 RED 静态合同，锁定黄框区域不可见。
- [x] M3：最小实现隐藏页头、Runner 状态/刷新和显示字段入口。
- [x] M4：运行目标静态合同、TypeScript 和差异检查。
- [x] M5：完成证据、cleanup 与收尾。

## Expected Verification

- `node IntRuoyiFronted\tests\e2e\edhr-batch-record-test-tab-static.spec.cjs`
- `pnpm ts:check` from `IntRuoyiFronted`
- `git diff --check -- IntRuoyiFronted\src\views\mes\pro\edhr-batch\BatchRecordTestPage.vue IntRuoyiFronted\tests\e2e\edhr-batch-record-test-tab-static.spec.cjs doc\tasks\20260808-batch-record-test-hide-yellow-ui`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260808-batch-record-test-hide-yellow-ui/frontend-feature-evidence.md`

## Applicable Gates

- 前端截图按钮统一静态契约门禁：黄框内按钮/区域不显示必须由目标静态合同先 RED 再 GREEN。
- 统一列表复合工具栏布局门禁：同页三张 `UnifiedListTemplate` 均需显式处理，不得只改当前可见页签。
- 前端列表跨账号默认列布局统一门禁：隐藏“显示字段”需通过 `showColumnSettings` 调用方显式关闭，不通过清缓存或个人配置处理。
- Strict No-Fallback：不新增 fallback、不吞异常、不改变测试执行 API 契约。

## Current Status

completed：实现、目标静态合同、TypeScript、diff check、负向扫描、frontend-feature evidence 校验、cleanup preview/apply 和经验沉淀判断均已完成；未执行 Git 提交，符合当前项目默认不提交策略。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，计划在目标页面调用方显式隐藏黄框区域。
- `是否存在临时补丁或绕过`：否。
