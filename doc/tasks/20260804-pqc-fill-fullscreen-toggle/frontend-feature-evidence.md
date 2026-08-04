# Feature

PQC填写页顶部操作按钮改为全屏切换：默认显示“最大化”，点击后请求浏览器全屏并把按钮切换为“主页”，再次点击退出全屏并恢复普通布局。最大化样式按用户截图压缩为 114px 顶部、左右主面板、底部重填/提交栏。

Non-goals: 不改变 PQC 检验事实来源、QA 规程快照、`itemResults[]`、提交 payload、设备/标准/方法绑定、权限、路由或后端接口。

Acceptance

- PQC填写首次进入时不再显示硬编码“主页”按钮，顶部操作渲染 `pqcFullscreenActionText`，初始值为“最大化”。
- 点击“最大化”调用外层 `frontlinePanelRef.requestFullscreen()`，让选择订单/工序/员工、逐件检验、标准/方法弹层都保留在 fullscreen 子树内。
- fullscreen 状态下按钮文字为“主页”；点击后调用 `document.exitFullscreen()` 并恢复普通布局。
- 浏览器 ESC 或系统退出 fullscreen 时通过 `fullscreenchange` 同步状态。

UI entry points, routes, components, owned files

- Route: `/mes/pro/feedback/edhr-batch-pqc-fill`
- Wrapper: `IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchPqcFillPage.vue`
- Component: `IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue`
- Static contract: `IntRuoyiFronted/tests/e2e/edhr-frontline-pqc-fullscreen-toggle-static.spec.cjs`
- Adjacent contract update: `IntRuoyiFronted/tests/e2e/edhr-frontline-fill-tabs-static.spec.cjs`

API contracts and data states

- No API contract changes.
- No PQC payload changes.
- No fallback, mock data, or default-success path added.

BDD:

- BDD: PQC填写默认最大化入口 -> Given PQC账号进入 PQC填写页面, When 页面首次渲染顶部操作区, Then 操作按钮显示“最大化”且不显示“主页”作为默认入口。
- BDD: PQC填写进入全屏 -> Given PQC填写页面显示“最大化”, When 点击最大化按钮, Then 页面请求浏览器全屏并应用最大化样式，按钮文案切换为“主页”。
- BDD: PQC填写退出全屏 -> Given PQC填写页面处于最大化状态且按钮显示“主页”, When 点击主页按钮, Then 页面退出浏览器全屏并恢复普通布局，按钮文案切回“最大化”。

RED:

- `workdir=IntRuoyiFronted; node tests\e2e\edhr-frontline-pqc-fullscreen-toggle-static.spec.cjs` -> FAIL, expected reason: current PQC template lacks `frontlinePanelRef`, `isPqcFullscreen`, fullscreen API lifecycle, and still has the old hard-coded `@click="handleHome">主页</button>`.

GREEN:

- `workdir=IntRuoyiFronted; node tests\e2e\edhr-frontline-pqc-fullscreen-toggle-static.spec.cjs` -> PASS.
- `workdir=IntRuoyiFronted; node tests\e2e\edhr-frontline-pqc-html-alignment-static.spec.cjs` -> PASS.

Verification

- `git diff --check -- IntRuoyiFronted\src\views\mes\pro\feedback\FrontlineFixedTemplatePanel.vue IntRuoyiFronted\tests\e2e\edhr-frontline-pqc-fullscreen-toggle-static.spec.cjs IntRuoyiFronted\tests\e2e\edhr-frontline-fill-tabs-static.spec.cjs doc\tasks\20260804-pqc-fill-fullscreen-toggle\task.md doc\tasks\20260804-pqc-fill-fullscreen-toggle\execution-log.md` -> PASS with only CRLF normalization warnings.
- `workdir=IntRuoyiFronted; node tests\e2e\edhr-frontline-fill-tabs-static.spec.cjs` -> BLOCKED by unrelated pre-existing tab assertion: `eDHR batch tabs must include 历史批记录`.

Responsive, accessibility, loading, empty, error, and permission checks

- Responsive: fullscreen CSS fills `100vw/100vh`; static contract locks screenshot-like header/main/footer proportions.
- Accessibility: fullscreen button exposes `aria-label` and `aria-pressed` from live state.
- Loading/empty/error: unchanged; API loading and missing PQC snapshot fail-fast behavior remains intact.
- Permission: unchanged; route and API permission contracts were not modified.

Blockers

- Formal closeout is blocked by unrelated dirty workspace and branch state: `int_main` was already ahead of `origin/int_main`, and many unrelated files were dirty before this scoped edit.
- Adjacent fill-tabs static regression is blocked before PQC assertions because current workspace `EdhrBatchRecordTabs.vue` omits “历史批记录”; this task did not alter that file.
