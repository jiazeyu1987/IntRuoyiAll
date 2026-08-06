# Execution Log

## 2026-08-06 13:37 +08:00

User intent: 用户反馈“一线 PQC 正常，一线生产默认就全屏，逻辑是不是不对”，要求查看一线 PQC 逻辑并修正一线生产默认全屏行为。

Preflight:
- 读取 `bug-regression-fix-loop` 技能与 bug evidence contract。
- 读取 `docs/frontend-development.md`、`docs/e2e-rules.md`、`docs/task-closeout-rules.md`、`docs/powershell-memory.md`、`docs/experience-index.md`。
- `git status --short --branch`：当前 `int_main...origin/int_main [behind 4]`，工作区已有大量非本任务脏改动；本任务只触碰一线生产全屏逻辑相关文件，不回滚、不暂存、不提交其它改动。

BDD: 一线生产默认不全屏 -> Given 用户从菜单进入一线生产填写页；When 页面首次渲染；Then `.frontline-operator-panel.is-production-mode` 不能使用 `position: fixed` / `inset: 0` / `z-index: 2000` 这种默认全屏承载，并且浏览器 `document.fullscreenElement` 应为空。

BDD: 一线生产按 PQC 显式最大化 -> Given 用户在一线生产填写页看到右上按钮；When 按钮初始显示“最大化”并被点击；Then 才调用同一 panel 的 `requestFullscreen()`，按钮文案切换为“主页”；When 再点击“主页”；Then 调用 `document.exitFullscreen()` 并恢复“最大化”。

Root cause:
- PQC 逻辑是 `isPqcFullscreen` + `pqcFullscreenActionText`，点击 `handlePqcFullscreenToggle` 后通过 `frontlinePanelRef.value.requestFullscreen()` 显式进入全屏，`fullscreenchange` 同步状态。
- 一线生产没有同等显式状态，而是通过 `.frontline-operator-panel.is-production-mode { position: fixed; inset: 0; z-index: 2000; min-height: 100vh; }` 默认覆盖整屏，因此点击菜单进入后看起来已经全屏。

RED: `node tests\e2e\edhr-frontline-production-fullscreen-toggle-static.spec.cjs` -> FAIL, expected reason: 当前生产页缺少 `data-production-fullscreen-toggle` / 显式 fullscreen 状态，仍是静态“主页”按钮与默认 fixed carrier。

Implementation:
- `FrontlineFixedTemplatePanel.vue`：新增 `isProductionFullscreen`、`productionFullscreenActionText`、`enterProductionFullscreen`、`exitProductionFullscreen`、`handleProductionFullscreenToggle`。
- `FrontlineFixedTemplatePanel.vue`：生产页右上按钮保留 `frontline-home-button home-btn` 原型样式类，改为 `data-production-fullscreen-toggle` 和 `handleProductionFullscreenToggle`，文案为“最大化/主页”计算状态。
- `FrontlineFixedTemplatePanel.vue`：移除生产默认 fixed/inset/z-index 全屏承载，新增 `.is-production-fullscreen` / `:fullscreen` 显式全屏样式；PQC fullscreen 的通用 top-card/home-button 样式改为只作用于 `.frontline-operator-screen.is-pqc`，避免影响生产 1920 原型。
- 更新生产页 fullscreen、pixel parity、prototype parity、fill tabs 静态契约；更新真实 E2E 脚本断言为“默认非 fixed，初始最大化，点击后才进入 fullscreen”。

GREEN: `node tests\e2e\edhr-frontline-production-fullscreen-toggle-static.spec.cjs` -> PASS
GREEN: `node tests\e2e\edhr-frontline-production-pixel-parity-static.spec.cjs` -> PASS
GREEN: `node tests\e2e\edhr-frontline-production-prototype-parity-static.spec.cjs` -> PASS
GREEN: `node tests\e2e\edhr-frontline-fill-tabs-static.spec.cjs` -> PASS
GREEN: `node --check tests\e2e\edhr-frontline-production-fullscreen-toggle-real.e2e.cjs` -> PASS
GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260806-frontline-production-fullscreen-logic\bug-regression-evidence.md` -> PASS
GREEN: `git diff --check -- <本任务文件>` -> PASS
GREEN: `node -e <task file trailing whitespace check>` -> PASS

REGRESSION: `pnpm ts:check` -> FAIL, unrelated blocker: `src/views/mes/pro/processpool/QaRegulationPage.vue(236,28/242,26/243,28)` 缺少 `finalInspectionRequired`、`finalInspectionNotApplicableReason` 实例属性。该文件已有非本任务脏改动，未在本任务中修改。

Experience consolidation:
- 已读取 `project-experience-consolidation` 技能。
- 本次经验属于前端局部 fullscreen 行为，现有 `docs/frontend-development.md#Element Plus 全屏弹框挂载门禁` 已覆盖 Fullscreen API / `:fullscreen` 相关门禁；本次不新建长期经验文档。
- 当前 `docs/frontend-development.md` 与 `docs/experience-index.md` 已存在非本任务脏改动，为避免混入并行任务，本次仅在任务本地证据中记录经验边界。

Closeout boundary:
- 本任务实现和定向回归已完成。
- 因全量类型检查被无关 QA 页面阻塞、工作区已有大量非本任务脏改动且分支落后 `origin/int_main`，按项目门禁不标记 completed、不提交、不推送。

## 2026-08-06 追加：一线生产普通页显示完整

User intent: 用户提供截图说明图 1 一线 PQC 页面普通模式完整显示，图 2 一线生产页面普通模式右侧内容超出页面范围，要求一线生产也显示完整。

Boundary:
- 只改 `FrontlineFixedTemplatePanel.vue` 的展示层缩放与相关回归契约。
- 不改 API、数据源、后端、路由权限、生产/PQC 业务字段或提交 payload。

BDD: 一线生产普通页宽度内完整显示 -> Given 用户进入一线生产普通页面；When 页面容器宽度小于 1920 参考画布；Then 生产画布应通过生产专用 stage 按容器宽度缩放，右上最大化按钮和右侧填设备区域不得被横向裁切。

RED: `node tests\e2e\edhr-frontline-production-fullscreen-toggle-static.spec.cjs` -> FAIL, expected reason: 当前生产页没有 `frontline-production-stage` / `productionStageStyle`，固定 1920px 画布会横向撑出页面。

Implementation:
- `FrontlineFixedTemplatePanel.vue`：生产模式增加 `frontline-production-stage`，外层舞台按 `productionStageStyle` 设置缩放后的宽高。
- `FrontlineFixedTemplatePanel.vue`：保留内部 `.frontline-operator-screen` 为 1920x1080 参考画布，用 `transform: scale(var(--frontline-production-scale))` 视觉缩放，不改内部原型尺寸和业务布局。
- `FrontlineFixedTemplatePanel.vue`：通过 `ResizeObserver`、`window.resize` 和 `fullscreenchange` 计算 `availableWidth / 1920`，普通页容器不足 1920 时自动缩小，防止右侧设备区和最大化按钮被横向裁切。
- `edhr-frontline-production-fullscreen-toggle-real.e2e.cjs`：真实路径断言从“普通模式必须 1920 宽”更新为“普通模式 stage/screen 不超出 viewport，内部 computed width/height 仍为 1920x1080”。

GREEN: `node tests\e2e\edhr-frontline-production-fullscreen-toggle-static.spec.cjs` -> PASS
GREEN: `node tests\e2e\edhr-frontline-production-pixel-parity-static.spec.cjs` -> PASS
GREEN: `node tests\e2e\edhr-frontline-production-prototype-parity-static.spec.cjs` -> PASS
GREEN: `node tests\e2e\edhr-frontline-fill-tabs-static.spec.cjs` -> PASS
GREEN: `node --check tests\e2e\edhr-frontline-production-fullscreen-toggle-real.e2e.cjs` -> PASS
GREEN: `pnpm ts:check` -> PASS
GREEN: `git diff --check -- <本任务文件>` -> PASS
GREEN: `node -e <task file trailing whitespace check>` -> PASS

Closeout boundary update:
- 本任务实现和定向验证已完成。
- 当前 `git status --short --branch -- <本任务文件>` 显示 `int_main...origin/int_main [behind 7]`，且本任务目标文件仍处于 modified/untracked；全仓存在大量非本任务脏改动。为避免混入并行任务，未执行提交和推送，任务状态保持 blocked。

## 2026-08-06 15:01 +08:00

User intent: 用户追加反馈一线生产普通页缩放后，“最大化/主页”“重填”“提交”几个按钮的文字大小不对，要求严格与目标页面一致。

Boundary:
- 只改 `FrontlineFixedTemplatePanel.vue` 的生产页缩放展示层字号补偿，以及已存在的一线生产 fullscreen 静态合同。
- 不改 API、后端、路由、数据字段、提交 payload 或一线 PQC 行为。

BDD: 一线生产缩放视图按钮字号保持原型视觉大小 -> Given 一线生产普通页用 stage 缩放完整显示 1920x1080 原型画布；When 页面宽度小于 1920 导致画布按比例缩小；Then “最大化/主页”按钮文字可见大小仍对齐原型 42px，“重填/提交”按钮文字可见大小仍对齐原型 54px，而不是被整体缩放后变小。

RED: `node tests\e2e\edhr-frontline-production-fullscreen-toggle-static.spec.cjs` -> FAIL, expected reason: `productionStageStyle` 尚未提供 `--frontline-production-top-action-font-size` / `--frontline-production-footer-action-font-size`，CSS 也未让生产 stage 内的最大化/主页、重填、提交按钮使用补偿字号。

Implementation:
- `FrontlineFixedTemplatePanel.vue`：在 `productionStageStyle` 中新增 `--frontline-production-top-action-font-size: ${42 / scale}px` 和 `--frontline-production-footer-action-font-size: ${54 / scale}px`。
- `FrontlineFixedTemplatePanel.vue`：新增生产 stage 作用域 CSS，让 `.frontline-production-fullscreen-toggle` 使用补偿后的顶部按钮字号，让 `.frontline-production-reset-button` / `.frontline-production-submit-button` 使用补偿后的底部按钮字号。

GREEN: `node tests\e2e\edhr-frontline-production-fullscreen-toggle-static.spec.cjs` -> PASS
GREEN: `node tests\e2e\edhr-frontline-production-pixel-parity-static.spec.cjs` -> PASS
GREEN: `node tests\e2e\edhr-frontline-production-prototype-parity-static.spec.cjs` -> PASS
GREEN: `node tests\e2e\edhr-frontline-fill-tabs-static.spec.cjs` -> PASS
GREEN: `node --check tests\e2e\edhr-frontline-production-fullscreen-toggle-real.e2e.cjs` -> PASS
GREEN: `pnpm ts:check` -> PASS
GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260806-frontline-production-fullscreen-logic\bug-regression-evidence.md` -> PASS
GREEN: `git diff --check -- <本任务文件>` -> PASS

Experience consolidation:
- 读取 `project-experience-consolidation` 技能。
- 将缩放 stage 内关键按钮可见字号补偿经验合并到 `docs/frontend-development.md#前端截图字号调整静态契约门禁`。
- 更新 `docs/experience-index.md`，加入 `缩放画布`、`transform scale`、`stage 字号补偿`、`可见字号` 等关键词路由。

Closeout boundary update:
- 本次追加字号修复验证已完成。
- 当前工作区仍存在大量非本任务脏改动/未跟踪文件，且分支落后 `origin/int_main`；本任务继续不提交、不推送、不标记 completed。

## 2026-08-06 追加：局部 16:9 选择 Grid

User intent: 用户澄清“一线生产”不是放在 1920×1080 固定画布里，也不是整块 1920 画布缩放；工序选择和员工选择区域可以约占页面 2/3，但 grid 的长宽比例应按 1920:1080 设计，每个 grid/card 也应按这个比例。

Boundary:
- 只改 `FrontlineFixedTemplatePanel.vue` 的生产页展示层和相关回归契约。
- 不改 API、后端、路由、权限、数据来源、生产/PQC 业务字段或提交 payload。

BDD: 一线生产局部选择区按 1920:1080 比例排布 -> Given 用户进入一线生产普通页面；When 页面在正常后台内容区域中渲染；Then 普通页不得通过 `frontline-production-stage` 整块缩放 1920×1080 画布，工序选择和员工选择应落在生产专用 selection grid 内，selection card 使用 `aspect-ratio: 1920 / 1080`，且页面不产生横向溢出。

RED: 待运行 `node tests\e2e\edhr-frontline-production-fullscreen-toggle-static.spec.cjs` -> expected FAIL: 当前实现仍存在 `frontline-production-stage`、`productionStageStyle`、`PRODUCTION_CANVAS_WIDTH/HEIGHT`、`ResizeObserver` 整画布缩放，并且工序/员工卡片没有生产专用 1920:1080 局部 grid。

## 2026-08-06 15:49 +08:00

User intent: 用户反馈“又变成之前的老问题”，一线生产页面仍然超出页面范围且字体没有变化，要求继续修复。

Root cause:
- 模板层已经改为直接渲染 `.frontline-operator-screen screen`，但脚本和样式仍残留 `productionViewportScale`、`ResizeObserver`、`.frontline-production-stage`、`transform: scale(...)` 和固定 `width: 1920px; height: 1080px;`。
- 残留旧缩放链路与固定 1920 screen 会让普通页面继续横向溢出；由于页面不再包实际 stage，字号补偿变量也不会稳定作用到可见按钮。

Implementation:
- `FrontlineFixedTemplatePanel.vue`：移除生产普通页的 `productionViewportScale` / `ResizeObserver` / resize listener / stage 缩放更新函数。
- `FrontlineFixedTemplatePanel.vue`：删除 `.frontline-production-stage` 和 stage 内按钮字号补偿 CSS，避免整块画布 transform 缩放。
- `FrontlineFixedTemplatePanel.vue`：将普通生产 `.frontline-operator-screen` 改为 `width: min(100%, 1600px)`、`min-height: min(1080px, calc(100vh - 144px))`、`grid-template-rows: auto minmax(0, 1fr) 126px`，保证不超过页面宽度。
- `FrontlineFixedTemplatePanel.vue`：新增 `.frontline-operator-top.is-production` 局部 selection grid，宽度 `min(100%, 68vw)`、最大 `1280px`，工序/员工/最大化卡片使用 `aspect-ratio: 1920 / 1080`。
- 更新一线生产 fullscreen、pixel parity、prototype parity 和真实 E2E 语法脚本，锁定无 stage、无 fixed 1920 普通 screen、无横向裁切。

GREEN: `node tests\e2e\edhr-frontline-production-fullscreen-toggle-static.spec.cjs` -> PASS
GREEN: `node tests\e2e\edhr-frontline-production-pixel-parity-static.spec.cjs` -> PASS
GREEN: `node tests\e2e\edhr-frontline-production-prototype-parity-static.spec.cjs` -> PASS
GREEN: `node tests\e2e\edhr-frontline-fill-tabs-static.spec.cjs` -> PASS
GREEN: `node --check tests\e2e\edhr-frontline-production-fullscreen-toggle-real.e2e.cjs` -> PASS
GREEN: `pnpm ts:check` -> PASS
GREEN: `git diff --check -- <本任务文件>` -> PASS

Closeout boundary update:
- 本轮回归修复和定向验证已完成。
- 当前工作区仍存在大量非本任务脏改动/未跟踪文件，且分支落后 `origin/int_main`；本任务继续不提交、不推送、不标记 completed。

Implementation update:
- `FrontlineFixedTemplatePanel.vue`：移除 `frontline-production-stage`、`productionStageStyle`、`PRODUCTION_CANVAS_WIDTH/HEIGHT`、`productionViewportScale`、`ResizeObserver` 整块画布缩放链路。
- `FrontlineFixedTemplatePanel.vue`：生产普通页直接渲染 `.frontline-operator-screen screen`，顶部增加 `data-frontline-production-selection-grid`，工序/员工卡片增加 `frontline-production-selection-card` 和 `data-frontline-production-selection-card`。
- `FrontlineFixedTemplatePanel.vue`：`.frontline-operator-screen` 改为 `width: min(100%, 1600px)`、响应式 min-height；`.frontline-operator-top.is-production` 使用 `width: min(100%, 68vw)`、`max-width: 1280px` 和局部三列 grid；工序、员工、最大化按钮单元使用 `aspect-ratio: 1920 / 1080`。
- `edhr-frontline-production-fullscreen-toggle-real.e2e.cjs`：真实路径断言普通页不再存在 `data-frontline-production-stage`，工序/员工卡片接近 16:9 比例，且初始仍非 fullscreen。

GREEN: `node tests\e2e\edhr-frontline-production-fullscreen-toggle-static.spec.cjs` -> PASS
GREEN: `node tests\e2e\edhr-frontline-production-pixel-parity-static.spec.cjs` -> PASS
GREEN: `node tests\e2e\edhr-frontline-production-prototype-parity-static.spec.cjs` -> PASS
GREEN: `node tests\e2e\edhr-frontline-fill-tabs-static.spec.cjs` -> PASS
GREEN: `node --check tests\e2e\edhr-frontline-production-fullscreen-toggle-real.e2e.cjs` -> PASS
GREEN: `pnpm ts:check` -> PASS
GREEN: `git diff --check -- <本任务文件>` -> PASS

Closeout boundary update:
- 本次用户澄清的局部 16:9 grid 修复和定向验证已完成。
- 当前 `git status --short --branch -- <本任务文件>` 显示 `int_main...origin/int_main [behind 11]`，且全仓仍有大量非本任务脏改动/未跟踪文件。为避免混入并行任务，未执行提交和推送，任务状态保持 blocked。

## 2026-08-06 18:52 +08:00

User intent: 用户反馈一线生产普通页已不再溢出，但排版、大小、布局又与 `C:\Users\BJB110\Desktop\3\frontline-production-operator-1920.html` 不一致，要求严格完全一致。

Boundary:
- 只改一线生产填写组件展示层和对应回归合同/真实 E2E 脚本断言。
- 不改 API、后端、路由、权限、业务字段、提交 payload 或一线 PQC 数据逻辑。

BDD: 一线生产严格参考画布且不横向溢出 -> Given 用户进入一线生产填写页；When 普通后台页面容器宽度不足 1920px；Then 内部 `.frontline-operator-screen` 仍保持参考 HTML 的 1920×1080、顶部 `1fr 1fr 240px`、主区 `1050px 1fr`、底部 `300px 1fr` 和原始字号，只有外层 `.frontline-production-stage` 等比例缩放以适配页面宽度。

RED: `node tests\e2e\edhr-frontline-production-fullscreen-toggle-static.spec.cjs` -> FAIL, expected reason: 当前实现缺少 scale-to-fit stage 或内部 screen 仍使用响应式 `1600px` / 局部 16:9 grid，无法证明与参考 HTML 严格一致。

Implementation:
- `FrontlineFixedTemplatePanel.vue`：生产模式保留 `frontline-production-stage` 外层，内部 `.frontline-operator-screen screen` 保持 `1920px × 1080px` 与 `130px 1fr 126px`。
- `FrontlineFixedTemplatePanel.vue`：移除生产 top 的 `68vw` 局部 grid 覆盖，恢复参考顶部 `1fr 1fr 240px`。
- `FrontlineFixedTemplatePanel.vue`：恢复生产 picker 为参考 `760px` 卡片、双列 options、`112px` option 高度，避免 picker 也被局部 16:9 重排。
- `edhr-frontline-production-fullscreen-toggle-real.e2e.cjs`：真实路径断言改为 stage fit viewport + inner canvas computed `1920px × 1080px`。
- 静态合同恢复为“严格参考 canvas + 外层缩放”口径，撤销上一轮局部响应式 grid 合同。

GREEN: `node tests\e2e\edhr-frontline-production-fullscreen-toggle-static.spec.cjs` -> PASS
GREEN: `node tests\e2e\edhr-frontline-production-pixel-parity-static.spec.cjs` -> PASS
GREEN: `node tests\e2e\edhr-frontline-production-prototype-parity-static.spec.cjs` -> PASS
GREEN: `node tests\e2e\edhr-frontline-fill-tabs-static.spec.cjs` -> PASS
GREEN: `node --check tests\e2e\edhr-frontline-production-fullscreen-toggle-real.e2e.cjs` -> PASS
GREEN: `pnpm ts:check` -> PASS
GREEN: `git diff --check -- <本任务文件>` -> PASS

Experience consolidation:
- 已读取 `project-experience-consolidation` 技能。
- 本次可复用经验是：用户要求与参考 HTML/截图“严格完全一致”时，不得用局部响应式重排替代目标画布；应把防溢出适配放在外层缩放或外层容器。
- 因 `docs/frontend-development.md` 当前已有非本任务脏改动，本轮不混入长期文档，只在任务证据中记录。

Closeout boundary update:
- 本轮实现和定向验证已完成。
- 当前工作区存在非本任务脏改动/未跟踪文件且分支 ahead；本任务不提交、不推送，状态保持 blocked。
## 2026-08-06 追加：生产 picker 弹框 16:9

User intent: 用户截图反馈一线生产“选工序”弹框比例不是按 1920×1080 来的，要求弹框自身和选项 grid/card 都按 1920:1080 / 16:9 比例。

BDD: 一线生产 picker 弹框按 16:9 显示 -> Given 用户点击一线生产顶部工序或员工选择卡；When picker 弹框打开；Then 弹框 card 使用 `aspect-ratio: 1920 / 1080`，内部 options 区域在 card 内滚动，每个 option 也使用 `aspect-ratio: 1920 / 1080`。

RED: `node tests\e2e\edhr-frontline-production-pixel-parity-static.spec.cjs` -> FAIL, expected reason: 旧实现仍要求/使用 picker card `width: 760px` 和 option `height: 112px`，不能满足 1920:1080 弹框比例。
RED: `node tests\e2e\edhr-frontline-production-fullscreen-toggle-static.spec.cjs` -> FAIL, expected reason: 旧 fullscreen 合同仍把 production picker 断言为固定 760px/112px。

Implementation:
- `FrontlineFixedTemplatePanel.vue`：生产 picker card 改为 `width: min(92%, 1180px)`、`aspect-ratio: 1920 / 1080`、`grid-template-rows: auto minmax(0, 1fr) auto` 和 `padding: 32px`。
- `FrontlineFixedTemplatePanel.vue`：生产 picker options 增加 `align-content: start`、`min-height: 0`、`max-height: none`、`overflow: auto`，滚动限定在 options 区。
- `FrontlineFixedTemplatePanel.vue`：生产 picker option 改为 `height: auto`、`aspect-ratio: 1920 / 1080`、`min-height: 0`。
- 静态合同和真实 E2E 脚本同步锁定 picker card / option 的 16:9 比例。

GREEN: `node tests\e2e\edhr-frontline-production-pixel-parity-static.spec.cjs` -> PASS
GREEN: `node tests\e2e\edhr-frontline-production-fullscreen-toggle-static.spec.cjs` -> PASS
GREEN: `node --check tests\e2e\edhr-frontline-production-fullscreen-toggle-real.e2e.cjs` -> PASS
GREEN: `node tests\e2e\edhr-frontline-production-prototype-parity-static.spec.cjs` -> PASS
GREEN: `node tests\e2e\edhr-frontline-fill-tabs-static.spec.cjs` -> PASS
GREEN: `pnpm ts:check` -> PASS
GREEN: `git diff --check -- <本任务文件>` -> PASS

Closeout boundary update:
- 本轮 picker 比例修复和定向验证已完成。
- 当前 `int_main` 工作区仍有非本任务脏改动/未跟踪文件；本任务未提交、未推送。
