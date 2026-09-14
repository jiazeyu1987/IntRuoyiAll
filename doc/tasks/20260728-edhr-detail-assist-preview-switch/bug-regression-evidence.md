# Bug Regression Evidence: eDHR 详情页 blocker 修复

## Bug

收尾报告中仍有前后端阻塞：填写页辅助模式静态合同与后端快照读取合同口径冲突，且全量前端类型检查曾在并行改动中失败。

## Expected

- 填写页辅助模式“填写人”切换必须从执行详情 `assistSwitchTasks` 快照读取候选人，不得在弹框打开时调用全量 `getEdhrBatchExecution`。
- `assistSwitchTasks` 必须按当前 `routeProcessId` 汇总当前工序各表单任务 `fillableUsers`，并保留主生产表与附加动态表单槽位。
- 前端 `pnpm ts:check` 必须通过，不再出现 `assistPreviewRows` 或 DCC 上传页类型错误。

## Reproduction

- RED: `node src/test/js/mes-edhr-assist-filler-switch-snapshot-static.spec.cjs` -> FAIL，发现 `loadAssistFillerSwitchItems` 调用了全量批次详情接口。
- RED: `mvn -o -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，旧测试仍断言不落 `taskId/workstationId`，且 active 查询无法复用匹配上下文的执行记录。

## Root Cause

一次 blocker 修复把填写人候选来源从执行详情快照改成了全量批次详情，违反 `docs/backend-development.md#切换填写人快照读取边界`；同时执行记录 `openOrCreateByContext` 创建时未完整保存请求上下文，导致新批次任务可能复用错误 active 执行详情。

## Fix

- `ExecutionPage.vue` 的 `loadAssistFillerSwitchItems` 改回从 `execution.value?.assistSwitchTasks` 读取，并按当前 `routeProcessId` 过滤 `fillableUsers`。
- `edhr-assist-fill-mode-static.spec.js` 改为锁定执行详情快照来源，并显式断言不得调用 `getEdhrBatchExecution`。
- `MesProBatchRecordExecutionServiceImpl` 在 open/create 路径保存 `taskId/workstationId`，active 查询按 `batchExecutionId + taskId + workstationId + workOrderId + routeProcessId + batchRecordReportId + batchCode` 匹配。
- `MesProBatchRecordExecutionServiceImplTest` 更新旧断言，验证按任务/工位上下文隔离和复用 active 执行记录。

## GREEN

- GREEN: `node tests/e2e/edhr-assist-fill-mode-static.spec.js` -> PASS。
- GREEN: `node src/test/js/mes-edhr-assist-filler-switch-snapshot-static.spec.cjs` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `pnpm build:local` -> PASS，长耗时构建最终成功。
- GREEN: `mvn -o -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，97 个测试通过。

## Verification

- 已复跑详情页辅助 Switch、动态表单预览、旧红框隐藏、填写页辅助模式、损耗单打开动作五条前端静态合同。
- 已复跑 MES 执行记录服务测试类、eDHR preview 目标 JUnit、MES reactor compile 和前端 build/type check。

## Blockers

- 当前无可复现前后端编译、类型或静态合同错误。
- 工作区仍存在大量并行改动和未跟踪文件；提交前必须做任务文件隔离，不能直接宽泛 `git add -A`。

## Disabled Hint Visibility Regression

- Bug: 详情页右侧辅助模式 Switch 在无辅助配置时把“未配置辅助模式”与“原表模式 / Switch / 辅助模式”挤在同一行，窄右侧栏中提示文字换行后被蓝框区域裁切，用户无法完整阅读。
- Expected: 无辅助配置时 Switch 保留禁用状态，且“未配置辅助模式”必须完整显示，不影响右侧卡片列表和中间预览模式逻辑。
- Reproduction: `node tests/e2e/edhr-batch-detail-assist-preview-switch-static.spec.js` -> FAIL，新增布局合同后失败于 Switch 容器未使用三列 grid。
- Root Cause: Switch 容器使用单行 flex 且所有标签默认可收缩，右侧栏宽度不足时禁用提示被挤压换行，原 `line-height: 1` 又放大了视觉裁切问题。
- Fix: Switch 主行改为三列 grid；“未配置辅助模式”提示独占第二行、右对齐，并对主标签和禁用提示增加 `white-space: nowrap`。
- GREEN: `node tests/e2e/edhr-batch-detail-assist-preview-switch-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。

## Disabled Hint Contrast Regression

- Bug: “未配置辅助模式”虽然不再换行裁切，但使用 `#98a2b3` 浅灰禁用色，白底小字号下视觉上几乎不可见。
- Expected: 无辅助配置时提示必须清晰可读，禁用状态不能靠过浅文字表达。
- Reproduction: `node tests/e2e/edhr-batch-detail-assist-preview-switch-static.spec.js` -> FAIL，新增清晰状态条合同后当前样式未满足 `background: #f8fafc`、`color: #475467`、`font-weight: 600`。
- Root Cause: 上次修复只处理布局和换行，没有把禁用提示从普通浅灰小字提升为可读状态提示。
- Fix: 将禁用提示改为撑满第二行的中性状态条，增加浅背景、边框、居中对齐、较深文字和半粗字重。
- GREEN: `node tests/e2e/edhr-batch-detail-assist-preview-switch-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
