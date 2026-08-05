# PQC 红框区域 UI 原型设计执行日志

## User Intent

用户指出 PQC 页面截图红框内 UI 风格与主风格不搭配，要求先思考合适设计并进行 HTML 原型设计。

2026-08-05 追加反馈：用户基于新截图要求“增大红色区域，缩小黄色区域”，即扩大左侧检验内容区、压缩右侧填检验区。

2026-08-05 二次确认：用户同意继续按“改布局为主、局部辅助文字微调为辅”的建议推进，要求继续优化内容显示完整性。

2026-08-05 三次反馈：用户询问“长度、外观是否可以放在 tab 里，不是这样列表的形式”，要求左侧检验项从纵向列表改为页签式切换。

2026-08-05 四次反馈：用户基于截图要求黄框里的内容不要显示，即移除顶部原型说明条、左侧“检验内容”大标题和右侧“填检验”大标题。

2026-08-05 五次反馈：用户要求当前检验项详情区域不要再用一张外层卡片包裹，直接融入后面的左侧面板背景。

## Preflight

- 读取 `docs/frontend-development.md`、`docs/task-closeout-rules.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`。
- 读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，确认通用前端样式偏向操作台一致性；本页截图与源码显示 PQC 填写属于一线触控操作端，应优先保持当前 PQC 主体的绿色、大字号、粗边框风格。
- 读取技能：`design-system-delivery`、`design-taste-frontend`。
- Git 状态：`int_main...origin/int_main [ahead 13]`，且已有大量非本任务脏改动；本轮仅新增 `doc/tasks/20260805-pqc-redbox-ui-prototype/` 下任务文件，避免触碰并发改动。

## BDD

- BDD: PQC 红框区域风格统一 -> Given PQC 填写页主界面使用大字号、粗边框、圆角和深绿操作按钮 When 用户查看检验内容卡片中的设备、标准、方法区域 Then 该区域应使用同等触控面积、边框、字号和信息层级，而不是原生小 select/button。

## Milestone Evidence

- 定位源码：PQC 填写路由为 `IntRuoyiFronted/src/router/modules/remaining.ts` 中 `/mes/pro/feedback/edhr-batch-pqc-fill`，页面实现位于 `IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue`。
- 定位红框结构：`FrontlineFixedTemplatePanel.vue` 中 `.frontline-pqc-equipment-controls` 当前使用两个原生 `select`，`.frontline-pqc-fact-actions` 当前使用普通按钮；与周围 `.frontline-pqc-choice-actions`、`.frontline-pqc-type-tabs` 的大触控按钮不一致。
- 原型输出：`doc/tasks/20260805-pqc-redbox-ui-prototype/pqc-redbox-ui-prototype.html`。

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

## Remaining Blockers

- 正式 Vue 页面尚未修改；需用户确认原型方向后再进入 BDD/TDD 实现。
- Git 收尾阻塞：`git status --short --branch --untracked-files=no` 显示 `int_main...origin/int_main [ahead 13]` 且存在大量非本任务脏改动；本任务只新增 `doc/tasks/20260805-pqc-redbox-ui-prototype/`，未提交、未推送，避免把并发任务改动混入当前原型任务。
