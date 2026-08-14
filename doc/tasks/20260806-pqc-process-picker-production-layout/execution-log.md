# Execution Log

## User Intent

- 用户要求一线 PQC 点击“工序”的弹框大小布局、内部子卡片大小布局，与一线生产点击“工序”的弹框一致。

## BDD Scenarios

- BDD: PQC 工序弹框复用生产布局 -> Given 一线 PQC 页面打开 When 点击顶部“工序” Then 工序弹框容器、选项网格、选项卡片、标题和返回按钮尺寸布局与一线生产工序弹框一致。
- BDD: 数据来源不变 -> Given PQC 工序弹框布局调整 When 用户选择工序 Then 工序候选仍来自所选活跃订单对应工艺路线，不改接口或数据来源。

## RED / GREEN Evidence

- RED: `node tests/e2e/mes-frontline-pqc-process-picker-production-layout-static.spec.cjs` -> FAIL, expected reason: `PQC process picker template must include: data-pqc-process-picker`.
- GREEN: `node tests/e2e/mes-frontline-pqc-process-picker-production-layout-static.spec.cjs` -> PASS, output: `PASS: PQC process picker uses production picker layout`.
- GREEN: `node tests/e2e/mes-frontline-pqc-order-picker-production-layout-static.spec.cjs` -> PASS, output: `PASS: PQC production-order picker matches production process picker layout`.
- GREEN: `node tests/e2e/edhr-frontline-production-fullscreen-toggle-static.spec.cjs` -> PASS, output: `PASS: eDHR frontline production strict canvas scale-to-fit static contract`.
- GREEN: `node tests/e2e/mes-frontline-pqc-login-employee-lock-static.spec.cjs` -> PASS.
- GREEN: `node tests/e2e/mes-frontline-pqc-active-order-switching-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/edhr-frontline-fill-tabs-static.spec.cjs` -> PASS, output: `PASS: eDHR frontline fill tabs static contract`.
- GREEN: `node tests/e2e/edhr-frontline-production-pixel-parity-static.spec.cjs` -> PASS, output: `PASS: eDHR frontline production pixel parity static contract`.
- GREEN: `git diff --check` -> PASS with CRLF warnings only; no whitespace error lines were reported.

## Milestone Updates

- M1 completed: 新增 `mes-frontline-pqc-process-picker-production-layout-static.spec.cjs`，并先跑出缺少 PQC 工序布局标记的 RED。
- M2 completed: `FrontlineFixedTemplatePanel.vue` 中 PQC picker 增加 `frontline-picker--production-process`，工序弹框使用与一线生产相同的 `1770px / 1920:1080 / 6 列 / 16:9 子卡片` 布局 token；同步将前台颜色变量提升到 panel 层，保证 PQC 外置弹框变量可用。
- M3 completed: 目标合同、PQC 订单弹框合同、生产弹框合同、PQC 员工锁定合同、活跃订单切换合同、一线填写页签合同和 `git diff --check` 已通过。
- M4 completed: 已更新前端 evidence 和验证报告；真实浏览器截图未在本轮执行。

## Blockers

- 当前工作区已有其他任务改动；本任务只修改 PQC 工序弹框布局相关文件和任务记录。
- 当前仍未执行提交/推送；任务状态为 `ready_for_closeout`，等待按项目规则处理现有脏工作区基线、提交和推送。
