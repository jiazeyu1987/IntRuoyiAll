# Execution Log

## 2026-07-31 Bootstrap

- User intent: PQC 检验 30 个产品时，检查长度、外观等检验项目要弹出 30 条检验列表，让检验员逐件填写。
- Scope: `output/frontline-pqc-operator-1920.html`、本任务文档和任务专用浏览器验证脚本。
- Rules read: `AGENTS.md`、`docs/frontend-development.md`、`docs/e2e-rules.md`、`docs/powershell-encoding.md`、`docs/task-closeout-rules.md`。
- Experience index: `docs/experience-index.md` 已读取；适用门禁为最小范围 RED/GREEN、真实页面 Playwright、UTF-8 和无 fallback。
- BDD: 数值项目逐件填写 -> Given 当前检验数量为 30 / When PQC 点击“长度” / Then 页面弹出 30 行带序号和厘米输入框的列表，保存后主页面显示已填数量。
- BDD: 判断项目逐件填写 -> Given 当前检验数量为 30 / When PQC 点击“外观” / Then 页面弹出 30 行合格/不合格选择，且每件结果独立保存。
- BDD: 数量联动 -> Given PQC 把检验数量改为 5 / When 再打开任一检验项目 / Then 弹窗只显示 5 行，既有范围内结果保留，超出部分不显示。

## Current Milestone

- 2026-07-31 实现和最终验证已完成，任务进入收尾。

## 2026-07-31 Default Value And Grid Requirement

- User intent: 所有需要填写的数值都有默认值；默认值旁可用加减号调整，也可手工输入；弹框用 M×N grid 利用横向空间。
- Prototype defaults: 长度 `32.5 cm`、步长 `0.1`；压力 `50 MPa`、步长 `1`。
- BDD: 默认值 -> Given 检验数量为 30 / When 打开长度逐件检验 / Then 30 件均预填 32.5，主页面显示已填 30/30。
- BDD: 加减与手填 -> Given 某件长度默认 32.5 / When 点击加号、减号或直接输入 / Then 值分别按 0.1 调整或采用手工输入。
- BDD: 网格利用空间 -> Given 检验数量为 30 / When 打开任一逐件检验弹框 / Then 30 个检验格按 5 列 6 行展示，无需单列滚动查看。

## 2026-07-31 Default Value And Grid RED

- RED: `node doc/tasks/20260731-frontline-pqc-piece-inspection/pqc-piece-list.e2e.cjs` -> FAIL。
- Expected reason: 长度主页面仍显示 `已填 0/30`，未自动生成默认值。
- Visual RED: 首轮 5×6 数值网格把序号、减号、输入、加号、单位挤在同一行，默认值输入框宽度不足 80px，`32.5` 无法清晰显示。

## 2026-07-31 Default Value And Grid Implementation

- 长度模板增加默认值 `32.5` 和步长 `0.1`；压力模板增加默认值 `50` 和步长 `1`。
- 数值逐件状态首次生成或检验数量增加时，新增格自动采用对应模板默认值。
- 每个数值格包含减号、可手工输入的数值框、加号和单位。
- 数值加减按模板步长精确计算；长度保留 1 位小数，压力按整数步长调整。
- 弹框宽度扩大到 1580px，逐件区域固定 5 列；30 件自然形成 5×6。
- 每格改为上方序号、下方操作控件，确保默认值输入框宽度不小于 80px。
- 判断项目逐件弹框同步改为 5×6 网格，批量合格/不良和逐件修改逻辑不变。

## 2026-07-31 Default Value And Grid GREEN

- GREEN: `node doc/tasks/20260731-frontline-pqc-piece-inspection/pqc-piece-list.e2e.cjs` -> PASS。
- Asserted: 长度和压力主页面首次显示 `已填 30/30`。
- Asserted: 30 个长度输入均默认 32.5，每件带两个步进按钮。
- Asserted: 长度首件 `32.5 → 32.6 → 32.5`，并可手工输入 32.1。
- Asserted: 压力首件 `50 → 51 → 50`，并可手工输入 49。
- Asserted: 数值网格和判断网格均为 5 列 6 行，30 件无需纵向滚动。
- Asserted: 默认值输入框宽度至少 80px，32.5 完整可见。
- Screenshot: `output/playwright/frontline-pqc-operator-1920-numeric-grid.png`。
- Screenshot: `output/playwright/frontline-pqc-operator-1920-bulk-choice-list.png`。

## 2026-07-31 Separate Choice Title Requirement

- User intent: 外观、密封标题不要放在“全部合格”按钮中，标题单独列出来更合适。
- BDD: 标题与操作分离 -> Given 判断项目为外观 / When 页面展示该项目 / Then 外观作为独立标题位于上方，下方只显示全部合格、全部不良、逐件选择三个按钮。
- BDD: 一屏显示 -> Given 外观和密封都改为上下两层 / When 页面以 1920×1080 展示 / Then 左侧四个检验项目仍全部可见，文字和按钮不溢出。

## 2026-07-31 Separate Choice Title RED

- RED: `node doc/tasks/20260731-frontline-pqc-piece-inspection/pqc-piece-list.e2e.cjs` -> FAIL。
- Expected reason: 当前外观标题仍嵌在“全部合格”按钮内，`:scope > .choice-item-title` 数量为 `0`，预期为 `1`。

## 2026-07-31 Separate Choice Title Implementation

- 外观、密封判断项目改为上下两层结构。
- 上层只显示项目名称，不响应点击；下层独立显示全部合格、全部不良、逐件选择三个按钮。
- 移除批量按钮内部的项目名称，避免标题与操作语义混淆。
- 判断项目高度调整为 142px，左侧长度、外观、密封、压力仍能在 1920×1080 一屏内完整显示。

## 2026-07-31 Separate Choice Title GREEN

- GREEN: `node doc/tasks/20260731-frontline-pqc-piece-inspection/pqc-piece-list.e2e.cjs` -> PASS。
- Asserted: 外观存在独立直接子标题 `.choice-item-title`，文本为“外观”。
- Asserted: 标题下方 `.choice-actions` 恰好包含三个按钮。
- Asserted: 操作按钮内部不再存在 `.choice-item-name`。
- Asserted: 批量同步、逐件修改、巡检次数隔离、列表滚动和 1920×1080 布局回归继续通过。

## 2026-07-31 Bulk Choice Requirement

- User intent: 如截图红框所示，外观、密封等合格/不合格项目分成三个操作区；第一个全部合格，第二个全部不良，第三个才进入逐件选择。
- BDD: 全部合格 -> Given 检验数量为 30 且外观未填写 / When 点击“全部合格” / Then 外观显示已填 30/30，打开逐件选择后 30 件全部为合格。
- BDD: 全部不良 -> Given 检验数量为 30 / When 点击“全部不良” / Then 打开逐件选择后 30 件全部为不合格。
- BDD: 批量后逐件修改 -> Given 已执行全部不良 / When 在逐件列表把其中一件改为合格并完成 / Then 批量按钮不再显示全选状态，逐件结果保持混合状态。

## 2026-07-31 Bulk Choice RED

- RED: `node doc/tasks/20260731-frontline-pqc-piece-inspection/pqc-piece-list.e2e.cjs` -> FAIL。
- Expected reason: 当前页面不存在 `[data-inspection-group="appearance"]`，错误为“外观应显示三段操作”，实际数量 `0`，预期数量 `1`。

## 2026-07-31 Bulk Choice Implementation

- 数值项目长度、压力继续保持原来的整行逐件入口。
- 判断项目外观、密封改为三个操作区：第一段显示项目名和“全部合格”，第二段显示“全部不良”，第三段显示“逐件选择”和已填数量。
- 点击全部合格，把当前检验数量内的结果全部写为 `合格`；点击全部不良，把当前范围全部写为 `不合格`。
- 批量操作直接更新逐件数据，因此进入逐件选择后，每一行会显示对应的已选结果。
- 当逐件结果全部一致时，高亮对应的批量按钮；手工改成混合结果后，取消两个批量高亮并高亮逐件选择。

## 2026-07-31 Bulk Choice GREEN

- GREEN: `node doc/tasks/20260731-frontline-pqc-piece-inspection/pqc-piece-list.e2e.cjs` -> PASS。
- Asserted: 外观存在全部合格、全部不良、逐件选择三个操作。
- Asserted: 点击全部合格后显示 `已填 30/30`，逐件列表 30 个合格按钮全部选中。
- Asserted: 点击全部不良后，逐件列表 30 个不合格按钮全部选中。
- Asserted: 把其中一件改为合格后，两个批量按钮均取消高亮，逐件选择进入混合状态。
- Asserted: 第 1 次和第 2 次巡检的批量结果分别保存，不互相覆盖。
- Asserted: 两个三段操作组及内部按钮均无横向或纵向文字溢出。
- Screenshot: `output/playwright/frontline-pqc-operator-1920-bulk-choice-main.png`。
- Screenshot: `output/playwright/frontline-pqc-operator-1920-bulk-choice-list.png`。

## 2026-07-31 RED

- RED: `node doc/tasks/20260731-frontline-pqc-piece-inspection/pqc-piece-list.e2e.cjs` -> FAIL。
- Expected reason: 当前页面不存在 `[data-inspection-key="length"]`，错误为“长度检验项目应可打开逐件列表”，实际数量 `0`，预期数量 `1`。

## 2026-07-31 Implementation

- 将左侧长度、外观、密封、压力从整批单值控件改为逐件检验入口，只显示项目名称和 `已填 x/总数`。
- 新增逐件检验弹窗：标题显示检验项目和当前件数；明细区显示序号和逐件填写结果；底部只保留“返回/完成”。
- 数值项目生成逐件数值输入和单位；判断项目生成逐件“合格/不合格”，默认不自动选中。
- 逐件数据按 `工序 + 首检/巡检/末检 + 检验次数 + 检验项目` 隔离，切换上下文不会覆盖。
- 修改检验数量后，弹窗行数同步；减少数量只隐藏超出行，不删除已填数据，恢复数量后继续保留。
- 补齐检验数量和损耗数量的 `+/-` 按钮行为；“重填”清空当前检验上下文的逐件数据。
- 独立只读评审确认三个重点风险：禁止默认全部合格、避免不同工序/检验次数数据串位、处理检验数量变化。本实现覆盖前两项，并通过“隐藏但不删除”处理数量减少，不增加额外确认弹窗。

## 2026-07-31 GREEN

- GREEN: `node doc/tasks/20260731-frontline-pqc-piece-inspection/pqc-piece-list.e2e.cjs` -> PASS，输出 `PQC piece inspection E2E PASS`。
- Asserted: 长度 30 行数值输入；外观 30 行和 60 个判断按钮；保存后主页面显示 `已填 2/30`。
- Asserted: 保存后重新打开，首件 `32.1`、末件 `32.9` 保留。
- Asserted: 第 2 次巡检显示 `0/25`，填写后为 `1/25`；切回第 1 次仍为 `2/30`。
- Asserted: 检验数量改为 5 后，压力弹窗只生成 5 行。
- Asserted: `bodyScrollWidth=1920`；弹窗完整位于 1920×1080 视口内；明细列表可滚动；底部操作区位于弹窗内。
- Screenshot: `output/playwright/frontline-pqc-operator-1920-piece-list-main.png`。
- Screenshot: `output/playwright/frontline-pqc-operator-1920-piece-list.png`。
- Evidence validator: `validate_frontend_feature.py --evidence .../frontend-feature-evidence.md` -> PASS，输出 `Frontend feature evidence is valid.`。
- Evidence validator self-test: `validate_frontend_feature.py --self-test` -> PASS。
- UTF-8 document check -> PASS，任务文档和线程讨论记录未发现替换字符。

## Thread Record

- 本线程此前和本次需求已整理到 `doc/tasks/20260730-frontline-ui-prototypes/thread-discussion-record.md`。

## 2026-07-31 Final Verification

- `node --check doc/tasks/20260731-frontline-pqc-piece-inspection/pqc-piece-list.e2e.cjs` -> PASS。
- `node doc/tasks/20260731-frontline-pqc-piece-inspection/pqc-piece-list.e2e.cjs` -> PASS，输出 `PQC piece inspection E2E PASS`。
- 压力首件已覆盖 `50 -> 51 -> 50` 和手工输入 `49`，确认整数步长与手工输入均生效。
- `validate_frontend_feature.py --evidence .../frontend-feature-evidence.md` -> PASS，输出 `Frontend feature evidence is valid.`。
- UTF-8 replacement-character check -> PASS。
- 重新目检数值 5x6 网格、判断 5x6 网格和主页面截图：30 件完整显示，标题、数值、单位、按钮均无重叠或截断。
- Project experience consolidation: 已检查现有长期经验文档；本次新增内容属于当前 PQC 原型的具体交互规格，没有发现需要写入长期工程门禁的新经验，因此未修改长期经验文档。

## 2026-07-31 Closeout

- Cleanup preview -> PASS：保留 `task.md`、`execution-log.md`、`verification-report.md` 和任务专用 E2E 脚本；仅删除已归档结论的 `frontend-feature-evidence.md`。
- Cleanup apply -> PASS：按 preview 删除 evidence 文件，未触碰其它任务产物。
- Git closeout -> BLOCKED：`.git/index.lock` 为非空文件，同时存在持续运行的 `git status --short --branch` 进程。按 Git 锁恢复门禁未强杀进程、未删除锁文件，因此本任务保持 `ready_for_closeout`，尚未提交和推送。
