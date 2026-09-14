# Execution Log

## User Intent

用户要求审批中心每行只保留一个“查看”、一个“审核”和一个“流程”按钮。

## Experience Consolidation

已检查现有 `docs/frontend-development.md` 与前端按钮文案/行为一致性门禁；本次经验已有合适归宿，无需新增长期经验文档。

## BDD Scenarios

BDD: 审批中心操作列统一 -> Given 审批中心加载任意待办、已办、我发起或抄送任务；When 用户查看某行操作列；Then 页面最多显示“查看、审核、流程”三个入口，且不再并列显示“处理、打开、详情、轨迹”。

BDD: 查看入口保持业务详情 -> Given 任务存在正式业务详情路由；When 用户点击“查看”；Then 页面沿用该任务正式详情路由并保持原查询参数和只读/处理上下文。

BDD: 审核入口按能力可用 -> Given 待办任务同时具备审批决定能力；When 用户点击“审核”；Then 继续打开现有审核确认流程；Given 任务不具备直接审核能力；Then 不显示“审核”入口。

BDD: 流程入口保持过程追踪 -> Given 任务声明统一轨迹能力；When 用户点击“流程”；Then 打开现有审批轨迹并保留错误可见性；Given 任务未声明该能力；Then “流程”不可用。

## RED

RED: `node tests/e2e/approval-center-actions-consolidation-static.spec.js` -> 预期 FAIL，现有操作列仍渲染“处理、打开、详情、轨迹”，且没有统一查看/审核/流程入口。

## GREEN

GREEN: `node tests/e2e/approval-center-actions-consolidation-static.spec.js` -> PASS。
GREEN: `node tests/e2e/approval-center-review-action-static.spec.js` -> PASS。
GREEN: `node tests/e2e/approval-center-module-review-action-static.spec.js` -> PASS。
GREEN: `node tests/e2e/approval-center-upload-quick-review-static.spec.js` -> PASS。
GREEN: `node tests/e2e/approval-center-bpm-detail-clickable-static.spec.js` -> PASS。
GREEN: `node tests/e2e/dcc-approval-task-summary-static.spec.js` -> PASS。
GREEN: `node tests/e2e/approval-center-fill-list-area-static.spec.js` -> PASS。
GREEN: `node tests/e2e/approval-center-redbox-controls-static.spec.js` -> PASS。
GREEN: `node tests/e2e/approval-center-phase3-static.spec.mjs` -> PASS。
GREEN: `node tests/e2e/approval-center-phase4-static.spec.mjs` -> PASS。

## Regression

REGRESSION: `git diff --check -- <task-owned files>` -> PASS（仅提示工作区 LF/CRLF 转换警告）。
REGRESSION: `pnpm ts:check` -> BLOCKED，工作区既有 `src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue` 缺失 `openProductionReject` 和 `router`。
REGRESSION: `node scripts/approval-center-page-contract.test.mjs` -> PASS，合同与现行签名待处理并入待办规则一致。

## Blockers

- `pnpm ts:check` 受工作区无关的 TeamLeaderWorkbenchPage.vue 历史类型错误阻断。
- 真实 Playwright E2E 缺少本任务确认的登录租户和运行服务，未将其宣称为通过。
- task-closeout-cleanup preview 已执行，但工具将当前主工作区误判为 linked worktree，并因当前分支为 `int_main` 阻止自动收尾；任务状态保持 `ready_for_closeout`，未执行删除或合并。
- E2E: `$env:APPROVAL_CENTER_TASK_DIR='E:\IntRuoyi\IntRuoyiFronted\doc\tasks\20260826-approval-center-action-consolidation\e2e-artifacts'; node tests/e2e/approval-center-phase2-real.e2e.mjs` -> PASS；真实租户 `测试租户`、账号 `aoteman`，任务 `MES_FEEDBACK / FB-000287`，流程记录 2 条，查看已进入 `/mes/pro/feedback`。
- E2E: `node tests/e2e/approval-center-phase8-mes-feedback-real.e2e.mjs` -> BLOCKED；页面真实返回 MES 任务，但脚本选择器硬编码要求 `requiresSignature=false`，实际首批任务均为 `requiresSignature=true`，属于测试数据/脚本前置不匹配，非页面按钮跳转失败。
