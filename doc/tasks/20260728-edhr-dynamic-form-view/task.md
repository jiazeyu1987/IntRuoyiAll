# Task: eDHR 动态表单查看入口修复

## Task Goal

修复 eDHR 批次详情右侧动态表单卡片“查看表单”无法像主生产表一样打开查看的问题；主生产表查看链路保持不变，动态表单必须按 `formBindings` / `formCenterInstanceId` 等动态表单上下文打开，只读查看时不得误走批记录表单来源。

## Milestones

1. [x] 定位批次详情右侧卡片“查看表单”前端链路和现有静态契约。
2. [x] 补充 RED 静态回归，证明动态表单查看入口目前未加载动态表单预览。
3. [x] 实施最小正式修复，不引入 fallback、默认成功或吞异常。
4. [x] 运行目标 GREEN 与相邻 eDHR 静态回归。
5. [x] 更新验证报告、经验记录和收尾状态。
6. [x] 跟进修复点击后报“eDHR 审批快照无效”的动态表单模板解析回归。
7. [x] 跟进修复右侧红框表单卡片选中态，使当前选中表单与左侧面板一致显示黄色背景。

## Expected Verification

- `node tests/e2e/mes/<target-static-contract>.spec.js` 覆盖动态表单查看入口。
- 相邻 eDHR 批次详情/右侧卡片静态合同通过。
- 若运行态前置齐备，再通过真实页面只读路径验证动态表单“查看表单”可打开。

## 经验门禁

### 工艺路线三类配置术语契约

- Trigger: 批记录、批记录表单、动态表单、表单槽位、`formBindings`、工序开始相关页面或接口。
- Preflight check: 明确“主生产表/批记录表单”按工序设置逐工序批记录绑定读取；“动态表单/表单槽位”只按 `formBindings` 和动态表单链路读取。
- Blocker: 接口或前端需要用 `formBindings` 推断批记录表单，或用批记录表单字段替代动态表单来源时必须停止。
- Verification: 测试分别覆盖主生产表查看链路和动态表单查看链路，互不替代。
- Forbidden action: 禁止用空值、默认 `MAIN`、表单槽位或特殊节点配置掩盖正式来源缺失。
- Evidence: `AGENTS.md#工艺路线三类配置术语契约`。

### eDHR 路线表单跳过口径门禁

- Trigger: eDHR 动态表单、查看表单、`routeFormReadonly`、`OPEN_FORM`、`formCenterInstanceId`。
- Preflight check: 查看入口必须保留真实动态表单上下文；只读查看不得依赖批记录预览存在。
- Blocker: 动态表单卡片点击后仍显示“当前节点没有可预览的批记录表单”，或只用批记录表单状态证明动态表单可查看时必须停止。
- Verification: 静态合同覆盖动态表单查看入口能选择动态表单预览，并保持主生产表查看不退化。
- Forbidden action: 禁止 API-only、隐藏提示、默认成功或用主生产表替代动态表单查看。
- Evidence: `docs/e2e-rules.md#edhr-路线表单跳过口径门禁`。

### 前端静态契约隔离门禁

- Trigger: 现有全量静态检查或 `pnpm ts:check` 因无关历史问题失败。
- Preflight check: 先运行最接近的目标静态合同；若无关失败阻塞，新增本任务专用最小静态合同。
- Blocker: 无法证明失败点与当前任务无关，或无法形成稳定 RED/GREEN 时不得宣称完成。
- Verification: `execution-log.md` 记录目标 RED/GREEN 和剩余阻塞摘要。
- Forbidden action: 禁止修改无关大契约绕过历史失败。
- Evidence: `docs/frontend-development.md#前端静态契约隔离门禁`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是修复动态表单查看入口的真实上下文选择。
- `是否存在临时补丁或绕过`：否。

## Current Status

ready_for_closeout

## Verification Evidence

- RED: `node tests/e2e/edhr-dynamic-form-card-preview-static.spec.js` -> FAIL，当前 `shouldLoadTaskPreview` 排除 `formTemplateId/formCenterInstanceId` 动态表单。
- GREEN: `node tests/e2e/edhr-dynamic-form-card-preview-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-loss-form-open-action-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-admin-preview-runtime-fix-static.spec.js` -> PASS。
- FOLLOW-UP: 用户反馈红框内哪个表单被选中不明显；右侧当前选中表单卡片需与左侧工序面板一致显示黄色背景。
- RED: `node tests/e2e/edhr-dynamic-form-card-preview-static.spec.js` -> FAIL，右侧当前选中表单卡片仍为蓝色背景。
- GREEN: `node tests/e2e/edhr-dynamic-form-card-preview-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-detail-hide-red-box-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-admin-filler-visibility-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-loss-form-open-action-static.spec.js` -> PASS。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#previewTask_returnsDynamicRouteFormTemplatePreviewWithoutBatchReportSource" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> BUILD SUCCESS，1 test passed.
- CHECK: `git diff --check -- <task files>` -> no whitespace errors; Git reported line-ending normalization warnings only.
- CLEANUP PREVIEW/APPLY: `task_closeout.py --task-id 20260728-edhr-dynamic-form-view --mode preview/apply` -> keep core task evidence, delete none, blocked none, warnings none.
- CLOSEOUT BLOCKER: 工作区存在大量非本任务已修改/未跟踪文件；未执行提交/推送，避免把无关并行任务改动混入本任务提交。
- FOLLOW-UP: 用户点击动态表单后出现“eDHR 审批快照无效”，需要补充后端回归覆盖已发布动态模板只有 FormCenter 识别字段/旧正式布局但无 `sheetLayoutJson` 包装时的只读预览。
- RED: `mvn -o -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#previewTask_returnsDynamicRouteFormRecognizedFieldsPreviewWithoutJimuSchema" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，后端抛 `eDHR 审批快照无效`。
- GREEN: `mvn -o -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#previewTask_returnsDynamicRouteFormRecognizedFieldsPreviewWithoutJimuSchema" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> BUILD SUCCESS，1 test passed.
- GREEN: `mvn -o -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#previewTask_returnsDynamicRouteFormTemplatePreviewWithoutBatchReportSource" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> BUILD SUCCESS，1 test passed.
- GREEN: `node tests/e2e/edhr-dynamic-form-card-preview-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-loss-form-open-action-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-admin-preview-runtime-fix-static.spec.js` -> PASS。

## Cleanup Keep

- doc/tasks/20260728-edhr-dynamic-form-view/bug-regression-evidence.md
- doc/tasks/20260728-edhr-dynamic-form-view/frontend-feature-evidence.md
