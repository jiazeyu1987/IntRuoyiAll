# Frontend Feature Evidence

## Feature Goal

- eDHR 批次详情页放行预检错误提示显示后 5 秒自动消失。

## Non-goals

- 不调整后端放行预检接口。
- 不改变 Element Plus 全局消息行为。
- 不隐藏、吞掉或降级真实错误。

## Requirements And Acceptance

- AC1：`releaseActionError` 设置为错误文案后，页面错误提示立即可见。
- AC2：同一条错误展示满 5 秒后，`releaseActionError` 自动清空。
- AC3：若 5 秒内出现新的错误，旧定时器不得清除新错误。
- AC4：成功路径和刷新路径继续立即清除旧错误。

## UI Entry Points

- 页面：eDHR 批次详情页。
- 组件：`src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue`。
- 用户入口：放行预检工作区的“预检”按钮，以及同页放行/追溯相关错误提示区域。

## API Contracts And Data States

- 沿用 `precheckEdhrRelease`、`getEdhrReleaseCheckItemPage` 现有接口。
- 错误来源仍为 `resolveErrorMessage(error, ...)`。
- 前端仅增加错误状态的显示生命周期，不更改请求参数或响应解析。

## BDD Scenarios

- `BDD: 放行预检错误 5 秒后自动消失 -> Given 用户在批次详情页执行放行预检且后端返回错误 / When 页面展示 releaseActionError 错误提示 / Then 该错误提示先可见，并在 5 秒后由前端状态自动清空。`
- `BDD: 后续错误不得被旧定时器误清除 -> Given 用户连续触发两个不同放行错误 / When 第一个错误的 5 秒定时器到期 / Then 若当前错误已变更，页面必须保留新的错误提示，只清除同一次展示的错误。`
- `BDD: 成功或刷新应立即清除旧错误 -> Given 页面正在重新执行放行预检或加载放行检查项 / When 逻辑明确进入新请求或成功路径 / Then 旧错误立即消失，不等待 5 秒。`

## Verification Evidence

- RED: `node tests/e2e/edhr-release-action-error-autohide-static.spec.js` -> FAIL, 缺少 `RELEASE_ACTION_ERROR_AUTO_HIDE_DELAY_MS = 5000`。
- GREEN: `node tests/e2e/edhr-release-action-error-autohide-static.spec.js` -> PASS.
- REGRESSION：`pnpm ts:check` 失败于既有 `src/views/system/codex-test-management/index.vue` 字段缺失，当前输出未包含本任务文件错误。

## Blockers

- `pnpm ts:check` 仍被既有 `src/views/system/codex-test-management/index.vue` 字段缺失阻塞，无法作为本任务全量回归通过证据；本任务目标静态契约已通过。

## Responsive / A11y / Loading / Empty / Error / Permission

- Responsive: 不改变布局，仅改变错误提示生命周期。
- Accessibility: 保持 `el-alert` 语义。
- Loading: 预检 loading 状态不变。
- Empty: 不改变空检查项展示。
- Error: 真实错误仍显示并同步触发 `message.error`。
- Permission: `ensureViewedReleaseStageWritable` 行为不变。
