# Execution Log: eDHR 详情页辅助模式预览 Switch

## User Intent

用户要求在 eDHR 批次详情页右侧红框位置增加“辅助模式”Switch，切换后中间预览区可以在原表模式和辅助模式之间切换；Switch 只影响中间预览，无辅助配置时显示禁用。

## Initial State

- PRECHECK: 工作区 `E:\IntRuoyi` 已存在大量非本任务修改/未跟踪文件；本任务只触碰 eDHR 详情页辅助预览 Switch 相关文件。
- PRECHECK: 已读取 `frontend-feature-delivery`、`backend-api-delivery`、`docs/frontend-development.md`、`docs/backend-development.md`、`docs/e2e-rules.md`、`docs/task-closeout-rules.md`、`docs/powershell-encoding.md`。

## BDD Scenarios

- BDD: 详情页原表/辅助预览切换 -> Given eDHR 批次详情页当前选中右侧某个主生产表卡片且该表单配置了辅助模式, When 用户打开右侧栏顶部“辅助模式”Switch, Then 中间预览区从原表只读预览切换为辅助字段只读列表，不触发保存、提交、打开表单或写请求。
- BDD: 无辅助配置禁用 Switch -> Given 当前选中表单没有 `assistRows`, When 用户查看右侧栏顶部 Switch, Then Switch 保留但禁用，并显示“未配置辅助模式”提示，中间区域仍展示原表模式。
- BDD: 未打开主生产表预览包含辅助快照 -> Given 当前主生产表任务尚未生成执行记录但正式报表配置了辅助行, When 详情页调用 `/task/preview`, Then 响应中的 `formViewModel.executionSnapshotJson` 包含 `assistRows`，供前端只读辅助预览使用。
- BDD: 动态表单来源不混用 -> Given 当前选中的是动态表单卡片, When 用户切换辅助模式或查看表单, Then 动态表单仍按 FormCenter 预览来源处理，不使用批记录报表快照生成辅助行。

## RED Evidence

- RED: `node tests/e2e/edhr-batch-detail-assist-preview-switch-static.spec.js` -> FAIL，缺少 `edhr-batch-detail__preview-mode-switch`，详情页右侧栏未提供辅助模式 Switch。
- RED: `mvn -o -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#previewTask_returnsUnopenedBatchRecordWithExecutionSnapshotAssistRows" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，未打开主生产表 `previewTask` 返回的 `formViewModel.executionSnapshotJson` 为空，缺少正式 `assistRows`。

## Implementation

- 前端：`BatchExecutionDetailPage.vue` 右侧非放行工序栏新增 `el-switch`，绑定 `detailPreviewAssistMode`，文案包含“原表模式/辅助模式”；无 `assistRows` 时禁用并提示“未配置辅助模式”。
- 前端：中间预览区新增 `effectiveDetailPreviewAssistMode` 分支，辅助模式只渲染字段名、说明、位置、当前值、必填、签名、完成状态，不提供保存、提交、签名、上传或打开载体动作。
- 后端：提炼 `MesProBatchRecordRuntimeSnapshotSupport` 复用正式 runtime snapshot 生成逻辑；未打开主生产表 `previewTask` 使用正式批记录报表来源填充 `sheetLayoutJson`、`metaJson`、`executionSnapshotJson`。
- 后端：动态表单预览仍走 FormCenter 预览链路，未使用批记录报表 runtime snapshot。

## GREEN Evidence

- GREEN: `node tests/e2e/edhr-batch-detail-assist-preview-switch-static.spec.js` -> PASS，详情页 Switch、只读辅助预览和不改打开载体静态合同通过。
- GREEN: `mvn -o -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#previewTask_returnsUnopenedBatchRecordWithExecutionSnapshotAssistRows" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，1 个目标 JUnit 通过，未打开主生产表预览包含 `executionSnapshotJson.assistRows`，无辅助配置返回空数组。
- GREEN: `mvn -o -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionServiceImplTest#openOrCreateByContext_shouldNormalizeExecutionSnapshotJsonStructure+openOrCreateByContext_freezesAssistRowsInExecutionSnapshot" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，2 个既有 runtime snapshot 回归通过。

## Regression Evidence

- GREEN: `node tests/e2e/edhr-dynamic-form-card-preview-static.spec.js` -> PASS，动态表单卡片仍按 FormCenter 只读预览。
- GREEN: `node tests/e2e/edhr-batch-detail-hide-red-box-static.spec.js` -> PASS，详情页右侧旧红框填写元信息仍隐藏。
- BLOCKER: `node tests/e2e/edhr-assist-fill-mode-static.spec.js` -> FAIL，失败点在 `ExecutionPage.vue` 既有填写页合同，期望 `loadAssistFillerSwitchItems` 调用 `getEdhrBatchExecution(batchExecutionId)` 汇总当前 `routeProcessId` 的 `fillableUsers`；本任务按用户约束不修改填写页行为。
- GREEN: `node tests/e2e/edhr-loss-form-open-action-static.spec.js` -> PASS，损耗单打开动作未把必填路线表单当可跳过。
- BLOCKER: `pnpm ts:check` -> FAIL，已越过本任务详情页，当前首个错误为 `src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue(117,21)` 和 `(121,36)`：`assistPreviewRows` 不存在于组件实例类型，属于非本任务文件。

## Experience Consolidation

- GREEN: `project-experience-consolidation` -> PASS，已将 PowerShell 分号串联测试可能掩盖中间失败的经验合并到 `docs/powershell-memory.md`，并在 `docs/experience-index.md` 增加关键词路由。

## Cleanup Preview

- PREVIEW: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260728-edhr-detail-assist-preview-switch --mode preview` -> READY，首次预览会删除 `frontend-feature-evidence.md` 和 `backend-api-evidence.md`。
- ACTION: 已在 `task.md` 增加 `Cleanup Keep`，保留两个 evidence 文件作为正式交付证据；未执行 apply 删除。
- PREVIEW: 同命令复跑 -> READY，keep 包含 `task.md`、`execution-log.md`、`verification-report.md`、两个 evidence 文件；delete/blocked/warnings 均为 `<none>`。

## Blocker Fix Reopen

- USER: 要求解决 verification report 中 blocked/失败内容。
- BDD: 填写页辅助模式填写人按当前批次详情解析 -> Given 用户在 eDHR 填写页辅助模式点击“填写人”切换, When 页面加载候选填写人, Then 必须调用当前批次详情接口并按当前 `routeProcessId` 汇总该工序各表单任务的 `fillableUsers`，不得使用陈旧执行快照或跨工序待办。
- BDD: 批记录规则弹框类型检查通过 -> Given 前端执行 `pnpm ts:check`, When 检查批记录规则弹框辅助表单预览, Then `assistPreviewRows` 必须是脚本中声明的响应式计算值，不再触发组件实例类型缺失。
- RED: `node tests/e2e/edhr-assist-fill-mode-static.spec.js` -> FAIL，`loadAssistFillerSwitchItems` 未匹配 `getEdhrBatchExecution(batchExecutionId)` + `routeProcessId` + `fillableUsers` 的合同。
- RECHECK: `pnpm ts:check` -> PASS，原 `BatchRecordCellRulesConfirmDialog.vue assistPreviewRows` 类型 blocker 当前已由并行改动消除，保留复验结果作为 blocker 解除证据。
- FIX: `ExecutionPage.vue` 的 `loadAssistFillerSwitchItems` 改为打开填写人切换弹框时调用 `getEdhrBatchExecution(batchExecutionId)`，再按当前批次任务 `routeProcessId` 汇总当前工序任务 `fillableUsers`。
- GREEN: `node tests/e2e/edhr-assist-fill-mode-static.spec.js` -> PASS，填写人来源静态合同通过。
- GREEN: `pnpm ts:check` -> PASS，全量前端类型检查通过，`BatchRecordCellRulesConfirmDialog.vue assistPreviewRows` 类型 blocker 已解除。
- REGRESSION: `node tests/e2e/edhr-batch-detail-assist-preview-switch-static.spec.js` -> PASS。
- REGRESSION: `node tests/e2e/edhr-dynamic-form-card-preview-static.spec.js` -> PASS。
- REGRESSION: `node tests/e2e/edhr-batch-detail-hide-red-box-static.spec.js` -> PASS。
- REGRESSION: `node tests/e2e/edhr-loss-form-open-action-static.spec.js` -> PASS。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260728-edhr-detail-assist-preview-switch/bug-regression-evidence.md` -> PASS。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260728-edhr-detail-assist-preview-switch/frontend-feature-evidence.md` -> PASS。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260728-edhr-detail-assist-preview-switch/backend-api-evidence.md` -> PASS。
- PREVIEW: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260728-edhr-detail-assist-preview-switch --mode preview` -> READY，keep 包含 task、execution-log、verification-report、frontend/backend/bug evidence；delete/blocked/warnings 均为 `<none>`。

## Final Blocker Correction

- RECHECK: `node src/test/js/mes-edhr-assist-filler-switch-snapshot-static.spec.cjs` -> FAIL，发现上一轮 blocker 修复把填写人切换改成了 `getEdhrBatchExecution(batchExecutionId)` 全量详情加载，违反 `docs/backend-development.md#切换填写人快照读取边界`。
- FIX: `ExecutionPage.vue` 的 `loadAssistFillerSwitchItems` 改回从 `execution.value?.assistSwitchTasks` 执行详情快照读取候选人；`edhr-assist-fill-mode-static.spec.js` 同步锁定不得调用全量批次详情接口。
- FIX: `MesProBatchRecordExecutionServiceImpl` 的 open/create active 查询与新建执行记录保存 `taskId/workstationId` 上下文，避免不同批次任务或工位复用旧执行详情；`MesProBatchRecordExecutionServiceImplTest` 更新旧断言，验证正式隔离口径。
- GREEN: `node tests/e2e/edhr-assist-fill-mode-static.spec.js` -> PASS。
- GREEN: `node src/test/js/mes-edhr-assist-filler-switch-snapshot-static.spec.cjs` -> PASS。
- GREEN: `mvn -o -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，97 个测试通过。
- GREEN: `mvn -o -pl yudao-module-mes -am "-DskipTests" compile` -> PASS，MES reactor compile 通过。
- GREEN: `mvn -o -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#previewTask_returnsUnopenedBatchRecordWithExecutionSnapshotAssistRows" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- RECHECK: `pnpm build:local` 首次 10 分钟超时，确认本次构建残留进程后停止；`.progress` 历史目录清理命令被本地策略拦截。
- GREEN: `pnpm build:local` -> PASS，使用更长执行窗口后构建成功。
- REGRESSION: `node tests/e2e/edhr-batch-detail-assist-preview-switch-static.spec.js` -> PASS。
- REGRESSION: `node tests/e2e/edhr-dynamic-form-card-preview-static.spec.js` -> PASS。
- REGRESSION: `node tests/e2e/edhr-batch-detail-hide-red-box-static.spec.js` -> PASS。
- REGRESSION: `node tests/e2e/edhr-loss-form-open-action-static.spec.js` -> PASS。
- FINAL: 当前前端类型检查、前端本地构建、eDHR 静态合同、MES compile、MES 目标 JUnit 和执行记录服务 JUnit 均已通过；工作区仍有大量并行改动，未提交。

## Disabled Hint Visibility Reopen

- USER: 截图反馈右侧辅助模式 Switch 蓝框内“未配置辅助模式”文字不完整显示。
- BDD: 无辅助配置提示完整可见 -> Given eDHR 批次详情页右侧栏当前表单无辅助配置, When Switch 禁用并显示“未配置辅助模式”, Then “原表模式/辅助模式/未配置辅助模式”三段文字必须完整可读，不被窄右侧栏挤压、换行截断或裁切。
- RED: `node tests/e2e/edhr-batch-detail-assist-preview-switch-static.spec.js` -> FAIL，新增布局合同后当前实现仍使用 flex 单行挤压禁用提示，断言失败于 Switch 区域未使用三列栅格承载主标签。
- FIX: `BatchExecutionDetailPage.vue` 将 `.edhr-batch-detail__preview-mode-switch` 改为三列 grid，只承载“原表模式 / Switch / 辅助模式”；禁用提示 `.edhr-batch-detail__preview-mode-disabled` 独占第二行、右对齐并 `white-space: nowrap`，避免蓝框文字截断。
- GREEN: `node tests/e2e/edhr-batch-detail-assist-preview-switch-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260728-edhr-detail-assist-preview-switch/bug-regression-evidence.md` -> PASS。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260728-edhr-detail-assist-preview-switch/frontend-feature-evidence.md` -> PASS。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260728-edhr-detail-assist-preview-switch --mode preview` -> READY，keep 包含正式任务文档和 evidence，delete/blocked/warnings 均为 `<none>`。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260728-edhr-detail-assist-preview-switch --mode apply` -> APPLIED，无删除项。
- EXPERIENCE: 已将窄栏 `el-switch` 状态提示完整可见规则合并到 `docs/e2e-rules.md#Element Plus 选择框显示门禁`，并在 `docs/experience-index.md` 增加 `Element Plus el-switch 状态标签`、`未配置辅助模式`、`white-space nowrap` 等关键词。
- GREEN: `rg -n "Switch 主标签|Switch 状态提示|el-switch|换行后被裁切" docs/e2e-rules.md docs/experience-index.md` -> PASS，经验索引可定位。
- GREEN: `git diff --check -- <本任务相关文件>` -> PASS，仅输出 Windows LF/CRLF 工作区提示，无 whitespace error。

## Disabled Hint Contrast Reopen

- USER: 截图反馈蓝框内“未配置辅助模式”仍然看不清。
- BDD: 禁用提示高对比可读 -> Given eDHR 批次详情页当前表单未配置辅助模式, When 右侧栏显示禁用 Switch 提示, Then “未配置辅助模式”必须使用清晰状态条样式和足够文字对比度，不使用过浅禁用灰色造成视觉不可见。
- RED: `node tests/e2e/edhr-batch-detail-assist-preview-switch-static.spec.js` -> FAIL，新增清晰度合同后失败于 `.edhr-batch-detail__preview-mode-disabled` 仍使用 `#98a2b3` 浅灰文字且无状态条背景。
- FIX: `.edhr-batch-detail__preview-mode-disabled` 改为独占第二行的中性状态条，使用 `justify-self: stretch`、`background: #f8fafc`、`color: #475467`、`font-weight: 600`，并保持不换行。
- GREEN: `node tests/e2e/edhr-batch-detail-assist-preview-switch-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` 首次 124 秒超时后确认任务自有 `ts:check` 进程已退出；使用更长窗口复跑 -> PASS。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260728-edhr-detail-assist-preview-switch/bug-regression-evidence.md` -> PASS。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260728-edhr-detail-assist-preview-switch/frontend-feature-evidence.md` -> PASS。
- GREEN: `git diff --check -- <本任务相关文件>` -> PASS，仅输出 Windows LF/CRLF 工作区提示，无 whitespace error。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260728-edhr-detail-assist-preview-switch --mode preview` -> READY，delete/blocked/warnings 均为 `<none>`。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260728-edhr-detail-assist-preview-switch --mode apply` -> APPLIED，无删除项。
