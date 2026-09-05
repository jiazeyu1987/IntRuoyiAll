# Execution Log

BDD: 活跃订单详情以独立页面打开 -> Given 用户在活跃订单池列表看到目标订单 When 点击该行“详情” Then 系统跳转到独立详情页面并加载该订单提交详情，不显示详情弹框。
BDD: Stage1 生成订单详情以独立页面打开 -> Given 用户对来源订单点击 Stage1 模拟生成新活跃订单 When 模拟成功自动打开详情 Then 独立页面显示来源订单与生成订单的关系，并加载生成订单的提交详情。
BDD: 列表行详情不串单 -> Given 源订单存在 Stage1 生成测试单 When 用户手工点击源订单行“详情” Then 详情页面展示源订单本身，而不是跳到 Stage1 生成测试单。

RED: node IntRuoyiFronted\tests\e2e\active-order-detail-standalone-page-static.spec.cjs -> FAIL, expected reason: must provide standalone active order submission detail page
GREEN: node IntRuoyiFronted\tests\e2e\active-order-detail-standalone-page-static.spec.cjs -> PASS
GREEN: node IntRuoyiFronted\tests\e2e\production-leader-active-order-process-submission-detail-static.spec.cjs -> PASS
GREEN: node IntRuoyiFronted\tests\e2e\team-leader-active-order-release-application-static.spec.js -> PASS
GREEN: node -c IntRuoyiFronted\tests\e2e\active-order-submission-overview-real.e2e.cjs -> PASS
GREEN: node -c IntRuoyiFronted\tests\e2e\active-order-version-upgrade-final-state-real.e2e.cjs -> PASS
GREEN: node -c IntRuoyiFronted\tests\e2e\frontline-active-order-submit-allocation-real.e2e.js -> PASS
GREEN: pnpm --dir IntRuoyiFronted ts:check -> PASS

## Completed Work

- Added standalone route `MesProcessPoolActiveOrderSubmissionDetail` for active order submission detail.
- Added `ActiveOrderSubmissionDetailPage.vue` to load detail by `activeOrderId` route param.
- Added reusable `ActiveOrderSubmissionDetailPanel.vue` and preserved production/PQC/material tabs, PQC item aggregation, and production device display.
- Changed workbench row “详情” to navigate to the current row active order, not a dialog.
- Changed Stage1 post-simulation auto-open to navigate to the newly generated test active order with `sourceWorkOrderCode` query for the source/target提示.
- Updated existing E2E/static scripts that previously asserted the old dialog behavior.

## Remaining Blockers

None for static/type verification. Full browser E2E was not run in this turn because the user requested the UI behavior change, not explicit E2E execution.

BDD: 详情页表格一屏内展示 -> Given 用户进入活跃订单工序提交详情页 When 页面展示生产提交、PQC提交、领料单表格 Then 页面主体不产生横向溢出，表格使用标准列表壳层和固定布局在当前页面宽度内显示。
RED: node IntRuoyiFronted\tests\e2e\active-order-detail-layout-static.spec.cjs -> pending, expected reason: detail tables are not wrapped in standard list shells and fixed layout contract is absent.
GREEN: node IntRuoyiFronted\tests\e2e\active-order-detail-layout-static.spec.cjs -> PASS
GREEN: pnpm --dir IntRuoyiFronted ts:check -> PASS after detail table fixed-layout update
GREEN: node IntRuoyiFronted\tests\e2e\active-order-detail-standalone-page-static.spec.cjs -> PASS，收尾复跑通过。
GREEN: node IntRuoyiFronted\tests\e2e\active-order-detail-layout-static.spec.cjs -> PASS，收尾复跑通过。
GREEN: node IntRuoyiFronted\tests\e2e\production-leader-active-order-process-submission-detail-static.spec.cjs -> PASS，收尾复跑通过。
GREEN: node IntRuoyiFronted\tests\e2e\team-leader-active-order-release-application-static.spec.js -> PASS，收尾复跑通过。
GREEN: node -c IntRuoyiFronted\tests\e2e\active-order-submission-overview-real.e2e.cjs -> PASS，收尾复跑通过。
GREEN: node -c IntRuoyiFronted\tests\e2e\active-order-version-upgrade-final-state-real.e2e.cjs -> PASS，收尾复跑通过。
GREEN: node -c IntRuoyiFronted\tests\e2e\frontline-active-order-submit-allocation-real.e2e.js -> PASS，收尾复跑通过。
GREEN: python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260905-active-order-detail-standalone-page/frontend-feature-evidence.md -> PASS。
GREEN: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260905-active-order-detail-standalone-page --mode preview -> PASS，keep task/execution/verification，delete frontend-feature-evidence，blocked/warnings 均无。
FIX: 收尾复跑发现旧静态合同仍读取 `TeamLeaderWorkbenchPage.vue` 的弹框结构；已将相关合同锚点更新到 `ActiveOrderSubmissionDetailPanel.vue` 独立详情组件。
FIX: `production-leader-active-order-process-submission-detail-static.spec.cjs` 改为基于测试文件位置解析前端根目录，避免从仓库根运行时误读 `E:\IntRuoyi\src`。
GREEN: git diff --check -> PASS，仅剩 Windows LF/CRLF 提示。
COMMIT: `5c969a5ff` -> `fix: show active order submission material details`，包含活跃订单详情独立页、表格布局合同、旧静态合同锚点修正和长期经验文档。
GREEN: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260905-active-order-detail-standalone-page --mode apply -> PASS，删除 frontend-feature-evidence，保留 task/execution/verification。
