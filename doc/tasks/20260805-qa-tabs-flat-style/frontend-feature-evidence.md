# Feature

QA 规程配置页下方 `总览 / 检验规则 / 检验项目 / 发布检查` tab 统一为与上方模块 tab 一致的紧凑 flat underline 样式。

## Acceptance

- QA tab 使用稳定的 `qa-regulation-page__tabs--flat` 类名。
- QA tab active 文案和 active bar 使用 `#00a896`。
- QA tab inactive 文案使用 `#172033` 且 `font-weight: 600`。
- QA tabs wrapper 收紧顶部 padding 到 `12px`，底部 padding 为 `0`，不渲染空的 Element Plus tab content。
- 项目选择器不显示左侧 `DCC 项目代码` 表单标签，但通过 `aria-label` 保留可访问名称。
- 页面根布局 `gap` 为 `0`，删除项目卡片与 tab、tab 与内容之间的空白。
- 项目选择器位于 QA 标题与生命周期状态标签之间；桌面保持同排，窄屏换行后占满一行。
- 不修改 API、后端、权限、数据库或真实数据来源。

## BDD:

Given 用户进入 QA 规程配置页并选择 DCC 项目代码, When 页面展示 QA 下方四个 tab, Then tab 与上方模块 tab 保持一致的紧凑下划线视觉且不保留空白内容带。

Given 用户进入 QA 规程配置页, When 项目选择器、QA tab 和当前内容依次显示, Then 页面不显示项目选择器左侧标签且三段区域之间无根布局空白。

Given 用户进入 QA 规程配置页, When 顶部标题区域渲染, Then 项目选择器显示在标题与生命周期状态标签之间，窄屏时自动换到下一行并保持全宽。

## RED:

`workdir=IntRuoyiFronted; node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> FAIL，旧 QA tab 未声明 `qa-regulation-page__tabs--flat`。

`workdir=IntRuoyiFronted; node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> FAIL，旧项目选择器仍显示可见标签且页面根布局为 `gap: 8px`。

## GREEN:

`workdir=IntRuoyiFronted; node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS。

## Verification

- `workdir=IntRuoyiFronted; pnpm ts:check` -> PASS。
- `workdir=E:\IntRuoyi; git diff --check -- IntRuoyiFronted\src\views\mes\pro\processpool\QaRegulationPage.vue IntRuoyiFronted\tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs doc\tasks\20260805-qa-tabs-flat-style` -> PASS，只有 CRLF normalization warnings。
- `workdir=IntRuoyiFronted; node tests\e2e\pqc-leader-module-tabs-static.spec.js` -> NON-GATE FAIL，PQC 自身默认状态类型断言已过期，与 QA 改动无关。

## Blockers

- `QaRegulationPage.vue` 和目标 QA 静态契约中包含开始本任务前的并行 hunks，无法安全独立提交本任务修改。
- PQC 相邻契约存在与本任务无关的过期断言；当前 QA Expected Verification 不依赖该契约。
