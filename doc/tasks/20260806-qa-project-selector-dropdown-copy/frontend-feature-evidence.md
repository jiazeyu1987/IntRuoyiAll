# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: QA 规程配置页头部项目代码字段在支持复制后，仍明确支持下拉选择和远程搜索。
- Non-goals: 不改 DCC 项目代码 API、不改 QA 发布按钮、不改 QA 内容页签。

## Requirements And Acceptance IDs

- AC-QA-DROPDOWN-1: 项目代码字段必须保留 `el-select` 下拉候选，不得替换成纯 `el-input`。
- AC-QA-DROPDOWN-2: 字段聚焦时支持自动展开，下拉箭头在远程搜索状态仍可见。
- AC-QA-DROPDOWN-3: 复制按钮不影响项目代码远程搜索和选择。

## UI Entry Points, Routes, Components, And Owned Files

- Entry: QA 规程配置页头部 DCC 项目代码控件。
- Component: `IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue`
- Test: `IntRuoyiFronted/tests/e2e/qa-regulation-project-last-copy-static.spec.cjs`

## API Contracts And Data States

- 保留 `getProjectCodePage` 作为远程候选加载方法。
- 保留 `getProjectCode` 作为上次选择 ID 恢复校验。
- 不新增后端接口或本地数据兜底。

## BDD Scenarios

- BDD: 项目代码仍可下拉选择 -> Given QA 页头部项目代码字段显示上次选择内容 When 用户点击或聚焦字段 Then 字段仍打开正式 DCC 项目下拉候选并支持选择。
- BDD: 项目代码仍可远程搜索 -> Given 候选很多 When 用户输入项目代码或项目名称关键字 Then 控件继续调用正式远程候选加载方法。
- BDD: 复制不替代选择 -> Given 用户需要复制当前项目文本 When 复制按钮存在 Then 复制按钮不把项目代码控件改成纯输入框，用户仍可用下拉改变项目。

## RED Command And Expected Failure

- RED: `node tests/e2e/qa-regulation-project-last-copy-static.spec.cjs` -> FAIL, expected because the existing selector did not expose an explicit dropdown marker or lock `automatic-dropdown` / `remote-show-suffix`.

## GREEN Command And Passing Result

- GREEN: `node tests/e2e/qa-regulation-project-last-copy-static.spec.cjs` -> PASS.
- GREEN: `node tests/e2e/qa-regulation-version-publish-header-static.spec.cjs` -> PASS.
- GREEN: `node tests/e2e/qa-regulation-publish-tab-hidden-static.spec.cjs` -> PASS.
- GREEN: `node tests/e2e/qa-regulation-display-fields-titlebar-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS.
- GREEN: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue IntRuoyiFronted/tests/e2e/qa-regulation-project-last-copy-static.spec.cjs doc/tasks/20260806-qa-project-selector-dropdown-copy` -> PASS.

## Responsive, Accessibility, Loading, Empty, Error, And Permission Checks

- Responsive: 未改变现有头部布局，只增强 `el-select` 属性。
- Accessibility: 保留 `aria-label="DCC 项目代码"` 和复制按钮 `aria-label`。
- Loading: 保留 `dccProjectCodeOptionsLoading`。
- Empty: 保留清空逻辑和上次选择清除。
- Error: 保留正式候选加载与恢复错误展示。
- Permission: 未改变路由或权限。

## E2E Or Component Verification Path

- 使用 QA 专用静态合同验证控件结构与行为契约。
- `pnpm ts:check` 被无关的 `TeamLeaderWorkbenchPage.vue` 脏改动阻塞，未作为本任务 GREEN。

## Blockers And Follow-Up Skills

- Blocker: 当前共享工作区 `TeamLeaderWorkbenchPage.vue` 存在非本任务脏改动导致 `pnpm ts:check` 报缺少 `searchActiveOrderCandidates`、`activeOrderCandidateLoading`、`activeOrderCandidateOptions`、`activeOrderCandidateError`。
- Blocker: 当前分支已有 6 个未推送的非本任务提交；为避免推送无关提交，本任务暂不推送。
