# Bug Regression Evidence

## Bug Summary And Expected Behavior

一线生产填写页点击入口后曾默认进入全屏；修正默认全屏后，普通页仍因固定 1920px 画布横向超出页面，右侧填设备区域和最大化按钮显示不全；随后用户澄清不应使用整块 1920×1080 画布缩放，而应让工序选择、员工选择按 1920:1080 比例做局部 grid。预期行为应与一线 PQC 一致：页面首次进入保持普通页面流，右上按钮初始显示“最大化”，只有用户点击后才通过浏览器 Fullscreen API 进入全屏，进入后按钮显示“主页”并用于退出全屏；普通页中的工序/员工选择卡片按 16:9 比例排布，不产生横向裁切。

## Reproduction Command Or Path

- Path: 打开一线生产填写页 `/mes/pro/feedback/edhr-batch-production-fill`
- RED command: `node tests\e2e\edhr-frontline-production-fullscreen-toggle-static.spec.cjs`
- RED result: FAIL，生产页缺少 `data-production-fullscreen-toggle`，按钮仍是静态 `@click="handleHome"` + `主页`，且 production carrier 仍默认 fixed/inset 全屏。
- Additional RED result: FAIL，生产页缺少 `frontline-production-stage` / `productionStageStyle`，固定 1920px 画布会横向撑出普通页面。
- Additional RED result: FAIL，用户澄清后当前生产页仍存在 `frontline-production-stage` / `productionStageStyle` 整块 1920×1080 stage 缩放，工序/员工卡片没有生产专用 1920:1080 局部 grid。

## Root Cause

一线 PQC 使用 `isPqcFullscreen`、`pqcFullscreenActionText`、`handlePqcFullscreenToggle` 和 `frontlinePanelRef.value.requestFullscreen()`，由用户显式点击后进入浏览器全屏。一线生产此前没有同等显式状态，而是通过 `.frontline-operator-panel.is-production-mode` 的 `position: fixed; inset: 0; z-index: 2000; min-height: 100vh;` 默认覆盖视口，造成进入页面即全屏。移除默认全屏承载后，内部 1920x1080 参考画布仍直接参与普通页面布局，页面容器不足 1920px 时横向溢出。后续用户澄清后确认，整块 stage 缩放本身不是正确方案；残留的 `productionViewportScale`、`ResizeObserver`、`.frontline-production-stage` 和固定 `1920px × 1080px` screen 会继续造成普通页横向超出范围，并使按钮字号补偿无法稳定作用到真实可见结构。最终方案是删除整块 stage 缩放链路，改为响应式 screen 和局部 16:9 selection grid。

## Regression Test Added Or Updated

- `IntRuoyiFronted/tests/e2e/edhr-frontline-production-fullscreen-toggle-static.spec.cjs`
- `IntRuoyiFronted/tests/e2e/edhr-frontline-production-pixel-parity-static.spec.cjs`
- `IntRuoyiFronted/tests/e2e/edhr-frontline-production-prototype-parity-static.spec.cjs`
- `IntRuoyiFronted/tests/e2e/edhr-frontline-fill-tabs-static.spec.cjs`
- `IntRuoyiFronted/tests/e2e/edhr-frontline-production-fullscreen-toggle-real.e2e.cjs`

## RED: Command And Expected Failure

- `node tests\e2e\edhr-frontline-production-fullscreen-toggle-static.spec.cjs` -> FAIL
- Expected reason: 当前生产页没有显式 fullscreen toggle，仍由默认 fixed carrier 冒充全屏；追加截图回归中，当前生产页没有生产专用 stage，固定 1920px 画布横向溢出页面；追加局部 16:9 回归中，普通页不得残留整块 stage 缩放、ResizeObserver 缩放或固定 1920×1080 screen，且工序/员工/最大化卡片必须位于 1920:1080 比例的局部 grid。

## GREEN: Command And Passing Result

- `node tests\e2e\edhr-frontline-production-fullscreen-toggle-static.spec.cjs` -> PASS
- `node tests\e2e\edhr-frontline-production-pixel-parity-static.spec.cjs` -> PASS
- `node tests\e2e\edhr-frontline-production-prototype-parity-static.spec.cjs` -> PASS
- `node tests\e2e\edhr-frontline-fill-tabs-static.spec.cjs` -> PASS
- `node --check tests\e2e\edhr-frontline-production-fullscreen-toggle-real.e2e.cjs` -> PASS
- `pnpm ts:check` -> PASS

## Verification And Regression Scope

风险集中在一线生产与一线 PQC 共用的 `frontlinePanelRef` 和 fullscreen 样式。已通过静态契约锁定：PQC fullscreen 状态仍由 `syncPqcFullscreenState` 监听 `fullscreenchange`，生产页 only 在 production mode 且 panel 成为 fullscreenElement 时更新 `isProductionFullscreen`；生产 1920 画布尺寸、footer 300px + 1fr、picker 原型样式仍通过相邻契约保护。追加用户澄清后已移除 `frontline-production-stage` 整块缩放链路，普通页 `.frontline-operator-screen` 改为响应式页面流；`data-frontline-production-selection-grid` 内的工序/员工卡片通过 `aspect-ratio: 1920 / 1080` 锁定局部 16:9 比例，真实 E2E 脚本同步断言不再存在 stage wrapper。

## Blockers And Follow-Up Actions

- 工作区已有大量非本任务脏改动，且 `int_main` 落后 `origin/int_main`；本任务按提交推送门禁未提交、未推送。
