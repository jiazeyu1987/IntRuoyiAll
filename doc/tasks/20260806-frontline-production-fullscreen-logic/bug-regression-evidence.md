# Bug Regression Evidence

## Bug Summary And Expected Behavior

一线生产填写页此前经历默认 fixed 全屏、普通页溢出和 picker 弹框比例不符合最新反馈等回归。最新预期是：首次进入不默认浏览器全屏，右上按钮初始为“最大化”；内部生产填写 canvas 保持参考 HTML 的 `1920px × 1080px` 和原始布局/字号；普通页面通过外层 stage 对整张 canvas 等比例缩放避免横向溢出；生产 picker 弹框和每个选项卡按 `1920:1080` / 16:9 比例显示。

## Reproduction Command Or Path

- Path: 打开一线生产填写页 `/mes/pro/feedback/edhr-batch-production-fill`
- RED command: `node tests\e2e\edhr-frontline-production-fullscreen-toggle-static.spec.cjs`
- RED result: FAIL，当前实现缺少 `frontline-production-stage` / `productionStageStyle`，并且生产 screen 仍是响应式 `width: min(100%, 1600px)` 与局部 selection grid，不能证明与参考 HTML 严格一致。

## Root Cause

为了修复普通页横向溢出，旧实现把参考 HTML 内部 canvas 改造成响应式页面流，并让顶部工序/员工/最大化区域使用局部 16:9 grid。这会改变参考 HTML 的核心布局 token：`1920px × 1080px`、`130px 1fr 126px`、顶部 `1fr 1fr 240px` 等不再成立，按钮和 picker 的视觉大小也随局部重排偏离目标。正确边界是：内部 canvas 不重排，只在外层 stage 根据容器宽度做等比例缩放。

## Regression Test Added Or Updated

- `IntRuoyiFronted/tests/e2e/edhr-frontline-production-fullscreen-toggle-static.spec.cjs`
- `IntRuoyiFronted/tests/e2e/edhr-frontline-production-pixel-parity-static.spec.cjs`
- `IntRuoyiFronted/tests/e2e/edhr-frontline-production-prototype-parity-static.spec.cjs`
- `IntRuoyiFronted/tests/e2e/edhr-frontline-fill-tabs-static.spec.cjs`
- `IntRuoyiFronted/tests/e2e/edhr-frontline-production-fullscreen-toggle-real.e2e.cjs`

## RED: Command And Expected Failure

- `node tests\e2e\edhr-frontline-production-fullscreen-toggle-static.spec.cjs` -> FAIL
- Expected reason: 当前生产页未使用外层 `frontline-production-stage` 缩放整张参考 canvas，或内部 `.frontline-operator-screen` 未保持 `width: 1920px; height: 1080px; grid-template-rows: 130px 1fr 126px;`。

## GREEN: Command And Passing Result

- `node tests\e2e\edhr-frontline-production-fullscreen-toggle-static.spec.cjs` -> PASS
- `node tests\e2e\edhr-frontline-production-pixel-parity-static.spec.cjs` -> PASS
- `node tests\e2e\edhr-frontline-production-prototype-parity-static.spec.cjs` -> PASS
- `node tests\e2e\edhr-frontline-fill-tabs-static.spec.cjs` -> PASS
- `node --check tests\e2e\edhr-frontline-production-fullscreen-toggle-real.e2e.cjs` -> PASS
- `pnpm ts:check` -> PASS
- `git diff --check -- <本任务文件>` -> PASS

## Verification And Regression Scope

风险集中在一线生产与一线 PQC 共用的 `frontlinePanelRef`、fullscreen 状态和生产 CSS。已通过合同锁定：一线生产默认不 fixed 全屏；右上按钮按 `isProductionFullscreen` 在“最大化/主页”之间切换；生产内部 canvas 保持参考 HTML 尺寸和 grid；生产 picker card 使用 `width: min(92%, 1180px)` 与 `aspect-ratio: 1920 / 1080`，选项卡也使用 `aspect-ratio: 1920 / 1080`；真实 E2E 脚本同步断言 stage 不超出 viewport、内部 canvas computed width/height 仍为 1920px/1080px，并覆盖 picker/option 实际 bounding box 比例。PQC 专用 `.is-pqc` 样式仍由相邻合同覆盖。

## Blockers And Follow-Up Actions

- 工作区已有非本任务脏改动/未跟踪文件，且 `int_main` 当前 ahead；本任务按提交推送门禁未提交、未推送。