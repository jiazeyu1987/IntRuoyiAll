# Execution Log

- User intent: 截图中黄色框内的放行预检错误提示，在显示错误后 5 秒自动消失。
- Scope: `IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue` 与对应静态契约测试；不修改后端、不修改接口契约、不新增 fallback。
- Baseline: 任务开始前工作区已有脏改动和 `int_main` ahead 1；按项目规则已创建独立脏工作区基线提交 `9d064ae0 chore: 保存任务前脏工作区基线`。
- Baseline files: `git show --name-status --oneline -1` 记录 26 个既有改动文件，未发现明显 secret/token/key/.env 文件名，最大文件约 215 KB。
- Note: 基线提交后并发任务证据 `doc/tasks/20260725-full-e2e-admin-validation/artifacts/full-chain-admin/run-config.json` 又更新了 `runId`，不属于本任务范围，后续提交前需重新隔离或阻塞记录。

BDD: 放行预检错误 5 秒后自动消失 -> Given 用户在批次详情页执行放行预检且后端返回错误 / When 页面展示 `releaseActionError` 错误提示 / Then 该错误提示先可见，并在 5 秒后由前端状态自动清空。

BDD: 后续错误不得被旧定时器误清除 -> Given 用户连续触发两个不同放行错误 / When 第一个错误的 5 秒定时器到期 / Then 若当前错误已变更，页面必须保留新的错误提示，只清除同一次展示的错误。

BDD: 成功或刷新应立即清除旧错误 -> Given 页面正在重新执行放行预检或加载放行检查项 / When 逻辑明确进入新请求或成功路径 / Then 旧错误立即消失，不等待 5 秒。

