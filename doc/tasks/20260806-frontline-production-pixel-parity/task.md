# 一线生产填写页像素级一致

## Task Goal

将“一线生产”填写页严格对齐参考文件 `C:\Users\BJB110\Desktop\3\frontline-production-operator-1920.html`，覆盖画布尺寸、主体排版、弹窗尺寸、按钮/输入框字号、间距和静态结构；只修改真实前端展示层，不修改 API、后端、请求参数、响应结构、数据源或 mock。

## Milestones

- [x] M1 建立严格视觉合同，证明现有实现仍存在尺寸/排版差异。
- [x] M2 修正 Vue 生产模式模板与样式，使主体和选择弹窗静态视觉规格与参考 HTML 一致。
- [x] M3 运行聚焦静态合同、相邻回归、真实浏览器布局比对和类型检查。
- [x] M4 完成验证报告、清理和收尾。

## Expected Verification

- `node tests/e2e/edhr-frontline-production-pixel-parity-static.spec.cjs`
- `node tests/e2e/edhr-frontline-production-prototype-parity-static.spec.cjs`
- `node tests/e2e/edhr-frontline-fill-tabs-static.spec.cjs`
- `node tests/e2e/edhr-frontline-production-fullscreen-toggle-static.spec.cjs`
- `node --check tests/e2e/edhr-frontline-production-fullscreen-toggle-real.e2e.cjs`
- Playwright 真实路由布局比对：`runtime-layout-compare.json` 中 `diffCount=0`
- Playwright 真实路由截图：`runtime-production-page.png` 为 `1920x1080`
- `pnpm ts:check`
- `git diff --check -- <本任务文件>`

## Applicable Gates

- 前端静态契约隔离门禁：当前任务使用专用最小静态合同覆盖像素级视觉差异，不把无关全量 `ts:check` 历史失败作为本任务 GREEN 证据。
- 前端截图样式块静态契约门禁：合同必须锁定目标选择器和目标样式块，不使用跨块宽泛正则冒充视觉一致。
- 前端截图字号调整静态契约门禁：字号、按钮高度、输入框高度、网格列宽等必须由静态合同明确锁定。
- Replicate Frontend UI：只复刻真实当前页面的展示层，保护 API/后端/数据源/DTO/mock。

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；以参考 HTML 的静态视觉规格作为生产模式样式合同。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- doc/tasks/20260806-frontline-production-pixel-parity/runtime-production-page.png
- doc/tasks/20260806-frontline-production-pixel-parity/runtime-layout-compare.json

## Current Status

completed：生产填写页已完成严格像素规格修正，静态合同、真实浏览器布局比对、截图尺寸、类型检查、格式检查和 cleanup apply 均已通过。
