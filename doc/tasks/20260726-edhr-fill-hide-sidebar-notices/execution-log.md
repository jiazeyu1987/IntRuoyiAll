# Execution Log

## 2026-07-26

- User intent: 用户要求 eDHR 填写页截图红框内的提示不显示。
- Skill: `bug-regression-fix-loop`。
- Trigger docs read: `docs/frontend-development.md`、`docs/task-closeout-rules.md`、`docs/powershell-memory.md`、`docs/powershell-encoding.md`。
- Experience gate: `docs/experience-index.md` 已读取；使用前端聚焦静态合同门禁。
- Git preflight: 根仓库为 `E:\IntRuoyi`，当前分支 `int_main`，remote 为 `origin`。
- Existing dirty state: `doc/tasks/20260726-edhr-new-business-latest-published-form/task.md` 与 `execution-log.md` 在本任务开始前已处于删除状态，属于既有脏改动，将单独保存为基线提交。
- BASELINE: `7b2a02ac` (`chore: preserve pre-task dirty workspace baseline`) -> 仅保存上述两项既有删除，不包含本任务文件。
- CONCURRENT: 基线提交后出现 `doc/tasks/20260726-hide-word-import-form-type/` 的并行任务更新；本任务不修改、不暂存、不提交。
- BDD: 左侧说明性提示不显示 -> Given 用户进入可编辑的 eDHR 填写工作台且具备金手指权限或关闭前修改状态 / When 左侧操作栏渲染 / Then 不显示“关闭前可修改”和“金手指测试权限”说明，同时保留错误、锁定告警和操作按钮。
- RED: `node tests/e2e/edhr-fill-workspace-hide-side-panels-static.spec.js` -> FAIL，首个失败为左侧栏仍包含 `v-if="preReleaseEditNotice"`。
- Root cause: `ExecutionPage.vue` 左侧栏显式渲染 `preReleaseEditNotice` 与 `goldenFingerNotice` 两个说明性 `el-alert`，既有隐藏红框合同未覆盖这两个节点。
- CHANGE: 移除左侧栏两个说明性 `el-alert` 及仅用于该展示的计算属性；保留 `revisionLockNotice`、`fieldAuditOpenGateError`、`fieldAuditSaveError` 和全部操作按钮。
- CHANGE: 更新金手指权限静态合同，继续锁定权限和绕过规则，但不再要求侧栏可见提示。
- GREEN: `node tests/e2e/edhr-fill-workspace-hide-side-panels-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-golden-finger-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-execution-fill-workspace-submit-static.spec.js` -> PASS。
- GREEN: `git diff --check` -> PASS。
