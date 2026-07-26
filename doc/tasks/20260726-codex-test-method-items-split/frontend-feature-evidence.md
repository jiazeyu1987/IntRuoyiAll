# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: 将新增/修改测试项弹窗中的测试方法项改为逐项录入，支持 1、2、3、4 等方法项分开维护。
- Non-goal: 不修改后端接口、数据库字段、Runner 执行契约或测试目标项数据结构。

## Requirements And Acceptance IDs

- REQ-1: 测试方法项必须像测试目标项一样逐项展示。
- REQ-2: 用户可以新增和删除测试方法项。
- REQ-3: 保存时继续使用既有 `methodText` 换行文本提交，保持后端契约不变。

## UI Entry Points, Routes, Components, And Owned Files

- Entry: 系统管理 > 测试管理 > 新增测试项 / 修改测试项。
- Component: `IntRuoyiFronted/src/views/system/codex-test-management/index.vue`。
- Test: `IntRuoyiFronted/tests/e2e/system-codex-test-management-static.spec.js`。

## API Contracts And Data States

- API contract unchanged: `CodexTestCaseVO.methodText` remains a string.
- Frontend state: local `methodItems` array maps UI rows to newline-delimited `methodText`.
- Edit state: existing `methodText` is split by line into method rows.
- Save state: method rows are sorted by `sort`, trimmed, filtered, and joined with `\n`.

## BDD Scenarios

- `BDD: 测试方法项逐项录入 -> Given 打开新增测试项弹窗 When 用户维护测试方法项 Then 页面应显示可新增/删除的方法项行，每行包含序号和方法内容，并在保存时按序号合并为既有 methodText 换行文本`

## RED Command And Expected Failure

- RED: `pnpm e2e:system:codex-test-management:static` -> FAIL，预期失败在“测试方法项表单必须从单个 textarea 改为逐项录入容器”。

## GREEN Command And Passing Result

- GREEN: `pnpm e2e:system:codex-test-management:static` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。

## Responsive, Accessibility, Loading, Empty, Error, And Permission Checks

- Responsive/layout: 方法项行使用专用 CSS grid，序号列、内容列和删除按钮列固定职责。
- Accessibility/usability: 保留表单 label 和按钮文字，新增/删除入口为可见按钮。
- Loading: 不涉及加载态变更。
- Empty: 新增表单默认保留 1 条空方法项，保存前要求至少 1 条非空方法。
- Error: 空方法项继续通过 `测试方法项不能为空` 显式提示。
- Permission: 保存按钮权限未变，仍使用既有 `system:codex-test:create/update`。

## E2E Or Component Verification Path

- 静态合同：`pnpm e2e:system:codex-test-management:static`。
- 类型检查：`pnpm ts:check`。

## Blockers And Follow-Up Skills

- Blocker: 工作区存在大量非本任务脏改动，本任务不执行 commit/push，避免混入并行任务。
- Follow-up skills: 不需要新增长期经验文档，本次经验已被现有静态合同和 Element Plus 布局门禁覆盖。
