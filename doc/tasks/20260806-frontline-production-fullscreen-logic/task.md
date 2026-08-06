# 一线生产默认全屏逻辑修正

## Task Goal

修正一线生产入口默认全屏、普通页显示不完整、以及选择区比例不符合要求的问题，使其进入页面时与一线 PQC 一样保持普通页面布局；普通页不再使用整页 1920×1080 固定画布或整块画布缩放，而是将工序选择、员工选择按 1920:1080 比例的局部 16:9 grid 排布，只在用户点击“最大化”后进入浏览器全屏，退出后按钮恢复为“最大化”。

## Milestones

- [x] 复核一线 PQC 最大化/主页切换逻辑与一线生产当前默认全屏根因
- [x] 补充静态回归契约，先证明当前生产页默认全屏逻辑失败
- [x] 用最小前端改动让一线生产复用显式全屏切换语义
- [x] 让一线生产普通页按容器宽度缩放完整显示
- [x] 补偿缩放后最大化/主页、重填、提交按钮文字大小
- [x] 按用户澄清改为局部 16:9 选择 grid，移除普通页整块 1920 画布缩放
- [x] 运行定向静态契约、相邻契约和类型检查
- [x] 记录验证结果并完成收尾边界说明

## Expected Verification

- `node tests/e2e/edhr-frontline-production-fullscreen-toggle-static.spec.cjs`
- `node tests/e2e/edhr-frontline-production-pixel-parity-static.spec.cjs`
- `node tests/e2e/edhr-frontline-production-prototype-parity-static.spec.cjs`
- `node tests/e2e/edhr-frontline-fill-tabs-static.spec.cjs`
- `node --check tests/e2e/edhr-frontline-production-fullscreen-toggle-real.e2e.cjs`
- `pnpm ts:check`
- `git diff --check -- IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue IntRuoyiFronted/tests/e2e/edhr-frontline-production-fullscreen-toggle-static.spec.cjs IntRuoyiFronted/tests/e2e/edhr-frontline-production-pixel-parity-static.spec.cjs IntRuoyiFronted/tests/e2e/edhr-frontline-production-prototype-parity-static.spec.cjs IntRuoyiFronted/tests/e2e/edhr-frontline-fill-tabs-static.spec.cjs IntRuoyiFronted/tests/e2e/edhr-frontline-production-fullscreen-toggle-real.e2e.cjs doc/tasks/20260806-frontline-production-fullscreen-logic`

## Current Status

blocked

实现与定向验证已通过：一线生产普通页已移除整块 1920 stage/ResizeObserver 缩放和固定 1920×1080 screen，改为响应式 screen + 局部 16:9 选择 grid；但当前 int_main 落后 origin/int_main 且工作区仍有大量非本任务脏改动/未跟踪文件，本任务按项目提交推送门禁不标记 completed、不提交、不推送。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按 PQC 的显式浏览器 fullscreen 状态建模，移除生产页默认 fixed 全屏承载，并将普通模式选择区改为局部 16:9 grid 而非全画布缩放。
- `是否存在临时补丁或绕过`：否。

## Applicable Experience Gates

- 前端静态契约隔离门禁：本任务用一线生产最大化专用静态契约先 RED 后 GREEN，避免依赖无关大契约。
- 前端截图字号调整静态契约门禁：普通页不要用整块 stage 缩放掩盖显示不全；关键按钮和选择卡应在响应式布局内保留可见字号与比例。
- Element Plus 全屏弹框挂载门禁：局部页面全屏应由浏览器 `requestFullscreen()` 显式触发，不能用固定定位和 z-index 冒充默认全屏。
