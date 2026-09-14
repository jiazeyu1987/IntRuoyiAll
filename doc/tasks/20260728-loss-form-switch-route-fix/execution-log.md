# Execution Log

## User Intent

- 用户截图反馈：“切换填写人”选择红框里的 `张可莹`，应该切换到 `损耗单`，当前弹窗报错 `eDHR 批次缺少唯一批记录路线`。
- 用户授权在 worktree 中修复，然后融合进 `int_main`。

## Worktree Evidence

- Worktree: `D:\IntRuoyiWorktree\loss-form-switch-fix`。
- Branch: `codex/20260728-loss-form-switch-fix`。
- Runtime profile: `int_main`。
- Reserved slot: `9`，frontend `8090`，backend `48090`。
- Source HEAD at creation: `9ca306d7 chore: baseline form template assist docs`。

## BDD

- BDD: 表单槽位候选切换到损耗单 -> Given eDHR 填写页切换填写人弹窗展示 `张可莹` 且载体为 `工艺路线表单槽位 · 损耗单` When 用户选择该候选 Then 系统应打开该候选对应的损耗单表单槽位任务，不得报 `eDHR 批次缺少唯一批记录路线`。
- BDD: 批记录和表单槽位边界保持独立 -> Given 同一工序存在 `MAIN` 批记录表单和 `LOSS_REPORT` 表单槽位 When 选择表单槽位候选 Then 打开请求必须携带并使用 `formBindingKey/formTemplateId/formCenterInstanceId` 等表单槽位上下文，不得用批记录报表唯一路线解析替代。
- BDD: 后端错误不被吞掉 -> Given 所选任务缺正式表单槽位上下文 When 后端无法打开 Then 前端展示真实错误；不得默认成功、空表单或静默切回当前填写人。

## RED/GREEN

- RED: PowerShell source-contract check against `git show HEAD:IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java` -> FAIL as expected，旧实现缺少 `resolveDynamicRouteFormVisibleAssistRows` 和 `isDynamicRouteFormTask(task)` 分流，会继续读取批记录 execution snapshot，导致动态损耗单候选触发 `eDHR 批次缺少唯一批记录路线`。
- GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-assist-filler-switch-snapshot-static.spec.cjs` -> PASS，静态合同锁定动态路线表单损耗单必须从 FormCenter 模板解析辅助行，不得读取批记录 execution 快照。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#openTask_dynamicRouteFormFillerSwitchUsesTemplateAssistRowsWithoutExecutionRoute" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 1, Failures: 0, Errors: 0。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#openTask_dynamicRouteFormFillerSwitchUsesTemplateAssistRowsWithoutExecutionRoute+openTask_returnsDynamicRouteFormContextWithoutBatchReportExecution+previewTask_returnsDynamicRouteFormTemplatePreviewWithoutBatchReportSource" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 2, Failures: 0, Errors: 0。
- GREEN: `mvn -pl yudao-module-mes -am "-DskipTests" compile` -> PASS，BUILD SUCCESS。

## Milestone Updates

- M1 in progress: 已读取 worktree、端口、前端、后端、E2E、数据库、登录、本地运行态、PowerShell 编码/Git、bug regression、frontend/backend delivery 和经验索引门禁。
- M1 completed: 已确认 `loss-form-switch-fix` worktree 为 `int_main` profile slot 9，端口登记 `8090/48090`，本次未启动服务。
- M2 completed: RED 证明旧 `resolveVisibleAssistRows` 对动态路线表单任务仍落入批记录 execution snapshot 路径；截图中的 `张可莹 / 工艺路线表单槽位 · 损耗单` 因无传统批记录 execution 路线而报 `eDHR 批次缺少唯一批记录路线`。
- M3 completed: 后端在 `resolveVisibleAssistRows` 中优先分流 `isDynamicRouteFormTask(task)`，通过任务冻结 `formTemplateVersionId` 读取 FormCenter 模板版本的 `edhrAssistRows`，并按所选填写人的正式 `responsibilityScopeJson` 过滤辅助行。
- M4 completed: 静态合同、定向 JUnit 和 MES 模块 reactor compile 均通过；本次只改后端打开链路和后端静态合同，未修改前端运行代码。
- M5 partial: 已按 `project-experience-consolidation` 将动态路线表单损耗单切换填写人不得回落到传统批记录 execution snapshot 的经验并入 `docs/backend-development.md#切换填写人快照读取边界`，并更新 `docs/experience-index.md` 关键词。
- M5 partial: 任务状态已推进为 `ready_for_closeout`，准备执行 cleanup preview/apply 与提交/融合。

## Blockers

- Pending merge/closeout: 仍需提交本 worktree 变更、沉淀经验、融合回 `int_main`，并处理主工作区当前 ahead/behind 与既有脏改动边界。

## Verification Evidence

- Root cause: 动态路线表单任务已有完整 `formBindingKey/formTemplateId/formTemplateVersionId/formCenterInstanceId`，但辅助填写行解析仍按传统批记录任务读取 `task.executionId` 和 execution snapshot；表单槽位损耗单没有唯一批记录路线时触发错误。
- Fix scope: `MesProEdhrBatchExecutionServiceImpl#resolveVisibleAssistRows` 与 `MesProEdhrBatchExecutionServiceTest#openTask_dynamicRouteFormFillerSwitchUsesTemplateAssistRowsWithoutExecutionRoute`。
- Static regression: `mes-edhr-assist-filler-switch-snapshot-static.spec.cjs` 新增动态路线表单分流和模板版本读取断言。
