# Bug Regression Evidence

## Bug Summary And Expected Behavior

一线生产填写页此前经历默认 fixed 全屏、普通页溢出、picker 弹框比例不符合最新反馈、stage 缩放后三个关键按钮可见字号偏小、以及选择工序后弹框等待异步链路才关闭等回归。最新预期是：首次进入不默认浏览器全屏，右上按钮初始为“最大化”；内部生产填写 canvas 保持参考 HTML 的 `1920px × 1080px` 和原始布局；普通页面通过外层 stage 对整张 canvas 等比例缩放避免横向溢出；生产 picker 弹框和每个选项卡按 `1920:1080` / 16:9 比例显示；“最大化 / 重填 / 提交”在缩放后的可见字号仍分别对齐参考顶部 42px 和底部 54px；一线生产点击工序后 picker 立即关闭，PQC 仍保留校验成功后关闭。

## Reproduction Command Or Path

- Path: 打开一线生产填写页 `/mes/pro/feedback/edhr-batch-production-fill`
- RED command: `node tests\e2e\edhr-frontline-production-fullscreen-toggle-static.spec.cjs`
- RED result: FAIL，`handleSelectProcess` 中生产模式先等待 `selectFrontlineProcess` 和默认员工切换，最后才关闭 picker，无法满足点击工序即收起弹框。

## Root Cause

`handleSelectProcess` 同时服务一线生产和一线 PQC。旧实现把 `closePicker()` 放在 `selectFrontlineProcess` / `selectFrontlinePqcProcess`、上下文应用、默认员工查找和 `handleSelectEmployee` 之后；生产模式的运行配置加载和默认员工切换都是异步链路，因此用户点击后会先看到 option active，但 picker 仍停留到异步链路完成。PQC 需要保留登录人和候选校验成功后关闭，不能简单把所有模式统一提前关闭。

## Regression Test Added Or Updated

- `IntRuoyiFronted/tests/e2e/edhr-frontline-production-fullscreen-toggle-static.spec.cjs`
- `IntRuoyiFronted/tests/e2e/edhr-frontline-production-pixel-parity-static.spec.cjs`
- `IntRuoyiFronted/tests/e2e/edhr-frontline-production-prototype-parity-static.spec.cjs`
- `IntRuoyiFronted/tests/e2e/edhr-frontline-fill-tabs-static.spec.cjs`
- `IntRuoyiFronted/tests/e2e/edhr-frontline-production-fullscreen-toggle-real.e2e.cjs`

## RED: Command And Expected Failure

- `node tests\e2e\edhr-frontline-production-fullscreen-toggle-static.spec.cjs` -> FAIL
- Expected reason: `handleSelectProcess` 未在生产模式等待 `selectFrontlineProcess` 前执行 `closePicker()`，且未保留 PQC 成功校验后关闭分支。

## GREEN: Command And Passing Result

- `node tests\e2e\edhr-frontline-production-fullscreen-toggle-static.spec.cjs` -> PASS
- `node tests\e2e\edhr-frontline-production-pixel-parity-static.spec.cjs` -> PASS
- `node tests\e2e\edhr-frontline-production-prototype-parity-static.spec.cjs` -> PASS
- `node tests\e2e\edhr-frontline-fill-tabs-static.spec.cjs` -> PASS
- `node --check tests\e2e\edhr-frontline-production-fullscreen-toggle-real.e2e.cjs` -> PASS
- `pnpm ts:check` -> PASS
- `git diff --check -- <本任务文件>` -> PASS

## Verification And Regression Scope

风险集中在一线生产与一线 PQC 共用的 `frontlinePanelRef`、fullscreen 状态、生产 CSS 和 `handleSelectProcess` 选择流程。已通过合同锁定：一线生产默认不 fixed 全屏；右上按钮按 `isProductionFullscreen` 在“最大化/主页”之间切换；生产内部 canvas 保持参考 HTML 尺寸和 grid；生产 picker card 使用 `width: min(96%, 1770px)` 与 `aspect-ratio: 1920 / 1080`，options 使用 6 列布局，选项卡使用 `aspect-ratio: 1920 / 1080`、`font-size: 30px` 和居中显示；`productionStageStyle` 提供顶部/底部按钮字号补偿变量，生产 stage 内“最大化 / 重填 / 提交”使用补偿变量；生产模式在等待 `selectFrontlineProcess` 前先 `closePicker()`，PQC 模式仍通过 `if (!shouldClosePickerImmediately) { closePicker() }` 保留校验成功后关闭；真实 E2E 脚本同步断言 stage 不超出 viewport、内部 canvas computed width/height 仍为 1920px/1080px，并覆盖 picker/option 实际 bounding box 比例。PQC 专用 `.is-pqc` 样式仍由相邻合同覆盖。

## Blockers And Follow-Up Actions

- 工作区已有非本任务脏改动/未跟踪文件，且 `int_main` 当前 ahead；本任务按提交推送门禁未提交、未推送。
