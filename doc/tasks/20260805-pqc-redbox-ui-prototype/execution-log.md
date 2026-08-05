# PQC 红框区域 UI 原型设计执行日志

## User Intent

用户指出 PQC 页面截图红框内 UI 风格与主风格不搭配，要求先思考合适设计并进行 HTML 原型设计。

2026-08-05 追加反馈：用户基于新截图要求“增大红色区域，缩小黄色区域”，即扩大左侧检验内容区、压缩右侧填检验区。

2026-08-05 二次确认：用户同意继续按“改布局为主、局部辅助文字微调为辅”的建议推进，要求继续优化内容显示完整性。

2026-08-05 三次反馈：用户询问“长度、外观是否可以放在 tab 里，不是这样列表的形式”，要求左侧检验项从纵向列表改为页签式切换。

2026-08-05 四次反馈：用户基于截图要求黄框里的内容不要显示，即移除顶部原型说明条、左侧“检验内容”大标题和右侧“填检验”大标题。

2026-08-05 五次反馈：用户要求当前检验项详情区域不要再用一张外层卡片包裹，直接融入后面的左侧面板背景。

2026-08-05 六次反馈：用户指出 tab 卡片需要和其它普通卡片做区分，否则不知道它是 tab。

2026-08-05 七次反馈：用户基于截图要求“改成红色范围，黄色里是 tab 的 tab 样式”，即红框作为整条 tab 区域，黄色框对应单个 tab 标签本体。

2026-08-05 八次反馈：用户要求考虑最多可以完整显示 10 个 tab，因此需要重新评估 tab 大小和排版，不能只按 4 个 tab 设计。

2026-08-05 九次反馈：用户确认“继续”，要求将已确认的 10-tab 原型方向继续推进到正式页面实现。

2026-08-05 十次反馈：用户基于截图要求“不显示黄框里的绿条，选中的用背景黄色显示”，即选中 tab 不再使用绿色顶部状态条，改用黄色背景作为当前项反馈。

2026-08-05 十一次反馈：用户要求“把当前系统的PQC填写改成与更新后的HTML预览的效果一致”，即正式 Vue 页面必须与最新 HTML 预览的 tab 视觉和完整显示规则保持一致。

## Preflight

- 读取 `docs/frontend-development.md`、`docs/task-closeout-rules.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`。
- 读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，确认通用前端样式偏向操作台一致性；本页截图与源码显示 PQC 填写属于一线触控操作端，应优先保持当前 PQC 主体的绿色、大字号、粗边框风格。
- 读取技能：`design-system-delivery`、`design-taste-frontend`。
- Git 状态：`int_main...origin/int_main [ahead 13]`，且已有大量非本任务脏改动；本轮仅新增 `doc/tasks/20260805-pqc-redbox-ui-prototype/` 下任务文件，避免触碰并发改动。

## BDD

- BDD: PQC 红框区域风格统一 -> Given PQC 填写页主界面使用大字号、粗边框、圆角和深绿操作按钮 When 用户查看检验内容卡片中的设备、标准、方法区域 Then 该区域应使用同等触控面积、边框、字号和信息层级，而不是原生小 select/button。
- BDD: PQC 选中 tab 黄色高亮且无绿条 -> Given PQC 检验项以 tab 形式展示 When 操作员选中一个检验项 Then 该 tab 使用黄色背景表达选中态，并且不显示旧的绿色顶部状态条。
- BDD: 正式 PQC tab 与 HTML 预览一致 -> Given HTML 预览中 10 个 tab 的“要求”和“已填进度”完整展示 When 当前系统 PQC 填写页渲染正式 tab Then 正式页应保留同样的黄底选中态、无绿色条和完整字段显示规则。

## Milestone Evidence

- 定位源码：PQC 填写路由为 `IntRuoyiFronted/src/router/modules/remaining.ts` 中 `/mes/pro/feedback/edhr-batch-pqc-fill`，页面实现位于 `IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue`。
- 定位红框结构：`FrontlineFixedTemplatePanel.vue` 中 `.frontline-pqc-equipment-controls` 当前使用两个原生 `select`，`.frontline-pqc-fact-actions` 当前使用普通按钮；与周围 `.frontline-pqc-choice-actions`、`.frontline-pqc-type-tabs` 的大触控按钮不一致。
- 原型输出：`doc/tasks/20260805-pqc-redbox-ui-prototype/pqc-redbox-ui-prototype.html`。
- 正式实现输出：`IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue` 已使用 `data-pqc-active-inspection-panel` 单一当前项详情、`data-pqc-inspection-tabs` 检验项 tab、`repeat(5, minmax(0, 1fr))` 10-tab 网格和触控式设备/编号信息卡。
- 最新选中态输出：`.pqc-item-tab.active` 已改为黄色背景 `#fff4bf`，active 伪元素 `&::before` 使用 `display: none` 且 `background: transparent`，不再显示黄框中的绿色条。
- 正式静态契约输出：`IntRuoyiFronted/tests/e2e/pqc-inspection-tabs-layout-static.spec.js`。
- 前端技能证据输出：`doc/tasks/20260805-pqc-redbox-ui-prototype/frontend-feature-evidence.md`。

## Verification Evidence

- GREEN: `python -X utf8` 静态读取与关键结构断言 -> PASS。
- GREEN: 原型文件包含 `.pqc-utility-strip`、`.pqc-select-card`、`.pqc-fact-card`、`接收标准`、`检验方法`、`逐件选择` -> PASS。
- GREEN: Edge headless 生成原型预览截图 `doc/tasks/20260805-pqc-redbox-ui-prototype/pqc-redbox-ui-prototype.png` -> PASS，文件大小 274565 bytes。
- GREEN: `git diff --check -- doc/tasks/20260805-pqc-redbox-ui-prototype` -> PASS。
- GREEN: `task_closeout.py --task-id 20260805-pqc-redbox-ui-prototype --mode preview` -> PASS，HTML 与 PNG 均在 keep，delete/blocked/warnings 均为 none。
- GREEN: `task_closeout.py --task-id 20260805-pqc-redbox-ui-prototype --mode apply` -> PASS，deleted_paths 为 none。
- Project experience consolidation: 已搜索 `docs/*memory*.md`、`docs/frontend-development.md` 与 `docs/experience-index.md`；现有截图/红框/前端样式门禁已覆盖本轮可复用经验，本次不新增长期经验文档。
- GREEN: 根据用户追加反馈调整 HTML 原型 CSS，两栏布局从固定 `770px + 右侧自适应` 改为 `minmax(760px, 1.72fr) + minmax(390px, 0.78fr)`，右侧数量控件同步压缩为 `128px 58px minmax(54px, 1fr) 58px 42px` -> PASS。
- GREEN: 重新生成预览图 `pqc-redbox-ui-prototype.png` -> PASS，文件大小 278343 bytes。
- GREEN: `python -X utf8` 断言调整后的左右比例 CSS 与 PNG 存在 -> PASS。
- GREEN: 将红框内质检信息条从“一行四格”改为“两行两列”，设备/编号在第一行，接收标准/检验方法在第二行；保留主操作按钮字号，轻压标题、卡片高度与间距 -> PASS。
- GREEN: 重新生成预览图 `pqc-redbox-ui-prototype.png` -> PASS，文件大小 273408 bytes。
- GREEN: `python -X utf8` 断言两行两列结构、完整 `接收标准` / `检验方法` 文案和紧凑高度 CSS -> PASS。
- GREEN: 将左侧检验项从列表改为 `pqc-item-tabs` 页签结构，仅展开当前“长度”详情，“外观/密封/压力”作为可切换 tab 展示状态摘要 -> PASS。
- GREEN: 重新生成预览图 `pqc-redbox-ui-prototype.png` -> PASS，文件大小 279507 bytes。
- GREEN: `python -X utf8` 断言页签结构、当前项 active、旧列表卡片移除、PNG 存在 -> PASS。
- GREEN: 移除顶部原型说明条、左侧 `检验内容` 标题、右侧 `填检验` 标题；保留核心操作 UI 和页签结构 -> PASS。
- GREEN: 重新生成预览图 `pqc-redbox-ui-prototype.png` -> PASS，文件大小 266956 bytes。
- GREEN: `python -X utf8` 断言黄框内容不再存在、核心结构仍存在、PNG 存在 -> PASS。
- GREEN: 去除当前检验项详情的外层整卡包裹，将 `.frontline-pqc-content-item` 改为透明背景、无外层边框，并取消 `.pqc-utility-strip` 分隔底线；内部设备、标准、方法和判定操作仍保留独立触控控件 -> PASS。
- GREEN: 重新生成预览图 `pqc-redbox-ui-prototype.png` -> PASS，文件大小 267141 bytes。
- GREEN: `python -X utf8` 断言当前项详情已融入面板背景、页签结构仍存在、PNG 存在 -> PASS。
- GREEN: 增强 `pqc-item-tabs` 页签识别：先新增浅绿轨道背景、分隔线、active 内嵌边框和顶部绿色指示条，使 tab 与普通卡片形成视觉区分 -> PASS。
- GREEN: 重新生成预览图 `pqc-redbox-ui-prototype.png` -> PASS，文件大小 266315 bytes。
- GREEN: `python -X utf8` 断言 tab 轨道背景、active 顶部指示条、active 内嵌边框和 PNG 存在 -> PASS。
- GREEN: 根据用户新截图将 tab 改为红框范围内的底部贴边页签样式：去掉整条轨道卡片感，保留底部连接线、上圆角 tab、active 顶部绿色条和轻微上浮 -> PASS。
- GREEN: 重新生成预览图 `pqc-redbox-ui-prototype.png` -> PASS，文件大小 264915 bytes。
- GREEN: `python -X utf8` 断言底部贴边页签圆角、底线、active 顶部指示条、active 上浮和 PNG 存在 -> PASS。
- GREEN: 根据用户红黄框标注，将检验项 tab 改为红框连接线下挂式页签：红框区域承担整条 tab 带，黄色区域对应单个底部圆角 tab 本体，active tab 轻微下移并保留绿色状态条 -> PASS。
- GREEN: 重新生成预览图 `pqc-redbox-ui-prototype.png` -> PASS，文件大小 263805 bytes。
- GREEN: `python -X utf8` 断言下挂式 tab 容器顶线、单个 tab 去顶边框、底部圆角、active 下移和 PNG 存在 -> PASS。
- GREEN: 将 tab 区调整为 2 行 x 5 列固定网格，示例扩展至 10 个检验项：长度、外观、密封、压力、内径、宽度、厚度、重量、颜色、硬度 -> PASS。
- GREEN: 将每个 tab 的底部信息拆成“要求”和“已填进度”两个字段，避免 10 个 tab 情况下出现省略号隐藏关键状态 -> PASS。
- GREEN: 重新生成预览图 `pqc-redbox-ui-prototype.png` -> PASS，文件大小 265297 bytes。
- GREEN: `python -X utf8` 断言 10 个 tab、5 列网格、10 个 `已填 0/30` 字段、无 tab 进度省略逻辑和 PNG 存在 -> PASS。
- RED: `node tests/e2e/pqc-inspection-tabs-layout-static.spec.js` -> FAIL，预期原因：旧正式页面未渲染 `data-pqc-active-inspection-panel`，仍是纵向展开列表。
- GREEN: `node tests/e2e/pqc-inspection-tabs-layout-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/pqc-item-equipment-standard-method-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS；首次 120s 超时无诊断，延长至 300s 后 134.9s 通过。
- GREEN: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue IntRuoyiFronted/tests/e2e/pqc-inspection-tabs-layout-static.spec.js doc/tasks/20260805-pqc-redbox-ui-prototype` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260805-pqc-redbox-ui-prototype/frontend-feature-evidence.md` -> PASS，输出 `Frontend feature evidence is valid.`
- Project experience consolidation: 已读取 `project-experience-consolidation` 技能并检查 `docs/*memory*.md`、`docs/frontend-development.md` 与 `docs/experience-index.md`；本次经验属于既有“前端静态契约隔离门禁”和“截图局部 UI 静态契约”覆盖范围，不新增长期经验文档。
- Closeout preview/apply blocker: 当前环境未找到 `task_closeout.py` / `task-closeout-cleanup` 命令，`scripts` 浅层目录也无 closeout 脚本；因此正式实现阶段未能重新执行 cleanup preview/apply。
- RED: `node tests/e2e/pqc-inspection-tabs-layout-static.spec.js` -> FAIL，预期原因：最新截图要求选中 tab 必须使用黄色背景，旧 active 选中态仍是白底绿字并带绿色顶部状态条。
- GREEN: `node tests/e2e/pqc-inspection-tabs-layout-static.spec.js` -> PASS，已验证 active tab 使用 `#fff4bf` 黄色背景，且 active `&::before` 被 `display: none` 隐藏并保持透明。
- GREEN: `node tests/e2e/pqc-item-equipment-standard-method-static.spec.js` -> PASS，设备、编号、标准和方法相邻契约未被破坏。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue IntRuoyiFronted/tests/e2e/pqc-inspection-tabs-layout-static.spec.js doc/tasks/20260805-pqc-redbox-ui-prototype` -> PASS。
- Project experience consolidation: 将“截图样式块静态契约必须抽取目标状态块，避免过宽正则跨块误命中”的通用经验合并到 `docs/frontend-development.md#前端截图样式块静态契约门禁`，并更新 `docs/experience-index.md` 关键词索引。
- RED: `node tests/e2e/pqc-inspection-tabs-layout-static.spec.js` -> FAIL，预期原因：正式 Vue 页 tab 的 `small` 字段块缺少更新后 HTML 预览中的 `overflow: visible` 和子字段完整显示规则。
- GREEN: `node tests/e2e/pqc-inspection-tabs-layout-static.spec.js` -> PASS，正式页已与更新后 HTML 预览对齐：10-tab 网格、黄色 active 背景、active 无绿色条、要求/已填字段完整显示。
- GREEN: `node tests/e2e/pqc-item-equipment-standard-method-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue IntRuoyiFronted/tests/e2e/pqc-inspection-tabs-layout-static.spec.js doc/tasks/20260805-pqc-redbox-ui-prototype docs/frontend-development.md docs/experience-index.md` -> PASS。

## Remaining Blockers

- 正式 Vue 页面已完成，本轮不再存在实现阻塞。
- Cleanup/Git 收尾阻塞：当前环境缺少可调用的 closeout cleanup 命令；`git status --short --branch` 显示当前分支未标记 ahead，但存在多项非本任务脏改动。为避免混入并发任务改动，本轮未提交或推送。
