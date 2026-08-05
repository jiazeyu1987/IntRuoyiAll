# Frontend Feature Evidence

## Feature Goal

把 QA 规程检验项目表格的“显示字段”入口移动到卡片标题栏，并保持列配置自动保存能力。

## Non-Goals

- 不修改 QA 规程数据、接口、权限、路由或后端行为。
- 不改变列配置结构、持久化 table key 或“新增检验方法”行为。
- 不引入重复按钮、重置列入口、fallback 或 mock 数据。

## Requirements And Acceptance

- AC-1：标题栏右侧按“显示字段”“新增检验方法”的顺序展示两个按钮。
- AC-2：表格工具栏不再渲染内置“显示字段”入口。
- AC-3：移动后的“显示字段”继续绑定 `qaItemsColumns`、`qaItemsColumnSaving` 和 `saveQaItemsColumnConfig`。

## Entry Point And Owned Files

- 页面：QA 规程配置 > 检验项目。
- 组件：`IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue`。
- 测试：`IntRuoyiFronted/tests/e2e/qa-regulation-display-fields-titlebar-static.spec.js`。

## API Contracts And Data States

- 无 API 合同变化。
- loading、empty、error 和 permission 状态保持现有实现。

## BDD

- BDD: 显示字段按钮位于检验项目标题栏 -> Given 用户已进入 QA 规程检验项目页签，When “工序检验方法与抽样方案”卡片完成渲染，Then 标题栏显示“显示字段”和“新增检验方法”，且表格工具栏没有重复“显示字段”入口。

## TDD Evidence

- RED: `node tests/e2e/qa-regulation-display-fields-titlebar-static.spec.js` -> FAIL，首个断言证明标题栏尚未直接导入显示字段组件。
- GREEN: `node tests/e2e/qa-regulation-display-fields-titlebar-static.spec.js` -> PASS，证明标题栏动作顺序、列配置绑定、旧入口关闭和空工具栏移除均满足契约。

## Responsive And Accessibility

- 复用现有标题栏 flex 布局和 Element Plus 按钮，不增加固定宽度。
- 保留“显示字段”按钮可访问名称和原组件交互。

## Verification Path

- 任务专用静态契约：PASS。
- 相邻 QA 规程静态契约：PASS。
- TypeScript 检查：PASS。
- 真实页面 Playwright 只读核对：本机运行态可用，但现有浏览器会话登录超时，未执行凭据提交。

## Blockers And Follow-Up

- 布局实现无业务前置缺口。
- 若需要补充真实截图证据，需要先提供或恢复有效的本机登录会话。
