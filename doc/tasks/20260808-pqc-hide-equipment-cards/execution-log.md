# Execution Log

## Intent

用户反馈：如果一个检验方法没有设备，不显示截图红框内的“检验设备”和“设备编号”内容。

Follow-up intent: 用户进一步明确“有设备的也不显示红框里的内容了”，因此一线 PQC 检验方法详情区无论是否有正式设备选项，都隐藏“检验设备”和“设备编号”两张卡片。

Final correction intent: 用户再次明确“有设备的需要显示红框里的内容，现在是不显示的”，最终口径为有正式设备选项时显示“检验设备”和“设备编号”，无正式设备选项时隐藏这两张卡片。

E2E follow-up intent: 用户要求追加真实 E2E 验证，覆盖最终口径是否在本机真实一线 PQC 页面生效。

## BDD

- `BDD: 无设备检验方法隐藏设备卡 -> Given 当前检验方法没有检验设备配置 When 一线 PQC 页面展示该方法 Then 不渲染“检验设备”和“设备编号”两张卡片`
- `BDD: 有设备检验方法显示设备卡 -> Given 当前检验方法存在检验设备配置 When 一线 PQC 页面展示该方法 Then 渲染“检验设备”和“设备编号”两张卡片`

## Milestone Updates

- M1 completed：定位到 `IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue` 的 `pqc-utility-strip`，原逻辑始终渲染 `data-pqc-equipment-card` 和 `data-pqc-equipment-number-card`，仅把文案切成“无需检验设备/无需设备编号”。
- M2 completed：更新 `IntRuoyiFronted/tests/e2e/pqc-item-equipment-standard-method-static.spec.js`，用静态合同锁定设备卡存在且必须由 `hasPqcEquipmentOptions(activePqcTabItem)` 控制，禁止无设备占位文案。
- M3 completed：恢复 `FrontlineFixedTemplatePanel.vue` 中“检验设备”和“设备编号”两段卡片模板，并新增 `v-if="hasPqcEquipmentOptions(activePqcTabItem)"`；恢复仅服务这些卡片的选择/格式化函数。
- M4 completed：定向静态合同、格式检查和 `pnpm ts:check` 均通过。
- M5 completed：已把“有设备显示设备卡、无设备隐藏设备卡”合并到现有前端经验门禁和经验索引。
- M6 completed：新增任务自有真实 Playwright 脚本 `doc/tasks/20260808-pqc-hide-equipment-cards/pqc-equipment-cards-real.e2e.cjs`，通过本机 `http://127.0.0.1:8081` 登录 `芋道源码/admin`，读取页面自身 `active-orders` 与 `active-order/processes` 响应作为正式数据源，并完成有设备/无设备两类检验项目 UI 断言。

## TDD Evidence

- RED: `node tests\e2e\pqc-item-equipment-standard-method-static.spec.js` -> FAIL, expected reason: current implementation did not expose `data-pqc-equipment-select` / equipment cards for equipment-backed items.
- GREEN: `node tests\e2e\pqc-item-equipment-standard-method-static.spec.js` -> PASS
- REGRESSION: `pnpm ts:check` -> PASS
- REGRESSION: `git diff --check -- IntRuoyiFronted\src\views\mes\pro\feedback\FrontlineFixedTemplatePanel.vue IntRuoyiFronted\tests\e2e\pqc-item-equipment-standard-method-static.spec.js docs\frontend-development.md docs\experience-index.md doc\tasks\20260808-pqc-hide-equipment-cards` -> PASS
- REGRESSION: `rg -n 'v-if="hasPqcEquipmentOptions\(activePqcTabItem\)"|data-pqc-equipment-card|data-pqc-equipment-number-card' IntRuoyiFronted\src\views\mes\pro\feedback\FrontlineFixedTemplatePanel.vue` -> PASS
- REGRESSION: `rg -n '无需检验设备|无需设备编号' IntRuoyiFronted\src\views\mes\pro\feedback\FrontlineFixedTemplatePanel.vue` -> PASS by no matches.
- REGRESSION: `rg -n "有设备显示设备卡|无设备隐藏设备卡|20260808-pqc-hide-equipment-cards" docs\experience-index.md docs\frontend-development.md` -> PASS
- E2E DISCOVERY: `node tests\e2e\edhr-frontline-pqc-menu-real.e2e.js` -> FAIL, expected non-target reason: existing menu visibility script has stale read-only assertion and treats page initialization `POST /admin-api/mes/pro/feedback/frontline/device-account/pqc/switch-employee` as forbidden. Target page entry, menu and DOM diagnostics were still collected, so a task-owned target E2E was created for the equipment-card behavior.
- E2E CHECK: `node --check doc\tasks\20260808-pqc-hide-equipment-cards\pqc-equipment-cards-real.e2e.cjs` -> PASS
- E2E GREEN: `node doc\tasks\20260808-pqc-hide-equipment-cards\pqc-equipment-cards-real.e2e.cjs` -> PASS, real page data source activeOrderCount=7, selected workOrderCode `881MO090889`, processCount=14, inspectionItemCount=96.
- E2E GREEN: 有设备样本 `RRM-PPV21-QA-001-RP928609 / 清洗-外观-抽检` equipmentOptionCount=1 -> 页面显示 `data-pqc-equipment-card` 与 `data-pqc-equipment-number-card`，并显示“检验设备/设备编号”。
- E2E GREEN: 无设备样本 `RRM-PPV21-QA-FIRST-01-RP928609 / 粗洗-默认首检规则` equipmentOptionCount=0 -> 页面不渲染 `data-pqc-equipment-card` 与 `data-pqc-equipment-number-card`，且无“无需检验设备/无需设备编号”占位。
- E2E GREEN: 结果文件 `output\playwright\20260808-pqc-hide-equipment-cards\pqc-equipment-cards-real-e2e.json`，截图 `withEquipment-881MO090889-RRM-PPV21-QA-001-RP928609.png` 与 `withoutEquipment-881MO090889-RRM-PPV21-QA-FIRST-01-RP928609.png`。
- REGRESSION: `node tests\e2e\pqc-item-equipment-standard-method-static.spec.js` -> PASS
- REGRESSION: `git diff --check -- IntRuoyiFronted\src\views\mes\pro\feedback\FrontlineFixedTemplatePanel.vue IntRuoyiFronted\tests\e2e\pqc-item-equipment-standard-method-static.spec.js docs\frontend-development.md docs\experience-index.md doc\tasks\20260808-pqc-hide-equipment-cards` -> PASS, only LF/CRLF warnings.

## Experience Consolidation

- 合并到 `docs/frontend-development.md#前端提交前严格验证与草稿态计算隔离门禁`：一线 PQC 检验方法详情区有正式设备选项时显示“检验设备/设备编号”卡片，无正式设备选项时隐藏。
- 更新 `docs/experience-index.md` 关键词：`有设备显示设备卡`、`无设备隐藏设备卡`、`显示检验设备`、`显示设备编号`、`data-pqc-equipment`。
- 本轮追加 E2E 后复核经验沉淀：已有 `docs/e2e-rules.md` 覆盖真实页面路径、目标接口监听、非目标请求归因；已有前端经验门禁覆盖本次 PQC 设备卡口径，因此不新建长期经验文档。

## Cleanup

- PREVIEW: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260808-pqc-hide-equipment-cards --mode preview` -> PASS, keep `task.md` / `execution-log.md` / `verification-report.md`, delete none, blocked none.
- APPLY: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260808-pqc-hide-equipment-cards --mode apply` -> PASS, deleted none, linked worktree false.
- POST-E2E PREVIEW: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260808-pqc-hide-equipment-cards --mode preview` -> PASS, keep `task.md` / `execution-log.md` / `verification-report.md` / `pqc-equipment-cards-real.e2e.cjs`, delete none, blocked none, warnings none.
- POST-E2E APPLY: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260808-pqc-hide-equipment-cards --mode apply` -> PASS, deleted none, linked worktree false, warnings none.

## Blockers

- None. 真实 E2E、静态合同和 diff 检查均已通过；未执行 PQC 提交，页面初始化仅观察到 `pqc/switch-employee` 员工锁定请求。
