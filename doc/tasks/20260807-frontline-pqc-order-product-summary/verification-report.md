# Verification Report

## Verification Summary

一线 PQC 活跃订单响应、顶部订单摘要和响应式布局已实现并通过静态合同、TypeScript、真实接口与三视口 Playwright 验收。真实页面选择订单后完整显示订单号、产品名称和去尾零数量，订单切换沿用同一选中对象同步更新，未选择状态不生成产品或数量占位。

## Automated Evidence

- Backend static contract: PASS。
- Frontend product-summary static contract: PASS。
- Existing PQC active-order search, picker, switching, employee-lock and fullscreen static regressions: PASS。
- `pnpm ts:check`: PASS。
- Backend focused Maven/Surefire: BLOCKED by concurrent writes to the shared MES `target`; no JUnit PASS is claimed.
- Backend evidence validator: PASS，`Backend API evidence is valid.`。
- Frontend evidence validator: PASS，`Frontend feature evidence is valid.`。

## Real Path Evidence

- URL: `http://127.0.0.1:8081/mes/pro/feedback/edhr-batch-pqc-fill`。
- Identity: `芋道源码/admin`。
- Active-order API count: 11。
- Selected order: `PQC-E2E-FS-20260804` / `球囊扩张压力泵` / `100`。
- Viewports: `1440x900`、`1920x1080`、PQC fullscreen，全部 PASS。
- Layout assertions: 顶部信息栏位于视口内；卡片无重叠；各值位于父卡片内；允许换行；无省略号；顶部值字号不超过 26px。
- Write safety: `pqcSubmitRequestCount=0`。
- Evidence: `output/playwright/20260807-frontline-pqc-order-product-summary/result.json` 及三张截图。

## Residual Risks And Blockers

- 新增后端 JUnit 已编写，但共享 MES 编译目录持续被并发任务写入，未获得独立 Surefire PASS。后端静态合同、运行字节码和真实接口覆盖了契约及正向路径，异常分支仍以源码合同留证。
- 当前样例订单的下游工序缺少已发布 QA 检验规程，系统明确报出上下文错误；该数据缺口不属于本任务，也未使用 fallback 隐藏。

## Runtime

- Frontend: `127.0.0.1:8081`，监听中。
- Backend: `127.0.0.1:48081`，最终健康检查 `UP`，PID `40088`。当前共享运行包由并发 ERP 任务更新，但内嵌 MES 字节码确认包含本任务的 `quantity`、`setQuantity` 与 `validateActiveOrderSummary` 标记。
- Preserved artifact: 本任务已验收运行包 `output/runtime/int_main/backend-runtime-control-20260807-frontline-pqc-order-product-summary.jar`，SHA-256 `974F8BB0F65AC3D26F173B8DD874EEA9E110846E42426BB5BE6E031A7132CA3D`。

## Final Result

PASS_WITH_MAVEN_ENVIRONMENT_GAP - 用户可见功能和真实只读路径通过；聚焦 Maven 缺少可归因的独立完成证据，已明确保留为环境验证缺口。

## Closeout Result

- Project experience: 已合并到 `docs/e2e-rules.md#顶部固定信息栏真实视口边界门禁` 并更新经验索引。
- Cleanup: `task-closeout-cleanup` 最终状态 `applied`；任务自有临时脚本、技能 evidence 和 `output/tmp` 隔离产物已删除，保留三份核心任务文档、三视口截图、结果 JSON 和当前运行包。
- Git: 未提交、未合并、未推送。
