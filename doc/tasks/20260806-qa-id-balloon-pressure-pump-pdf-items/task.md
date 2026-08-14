# QA 球囊扩张压力泵 PDF 检验项匹配

## Task Goal

将 `ID / 球囊扩张压力泵 / 112` 产品的 QA 规程配置来源切换/补齐为正式 PDF：`PQC-ID-001 (G 0) （椎体）球囊扩张压力泵组装过程检验规程.pdf`，确保该产品不再沿用 `IDI / 按压式球囊扩充压力泵` 的检验项目。

## Milestones

- [x] M1 建立任务文档、读取适用经验门禁并确认 PDF 可读取/可渲染。
- [x] M2 从 `PQC-ID-001` PDF 逐页提取/核对检验项目、接受标准、检验方法、器具和抽样方案。
- [x] M3 为 `ID / 球囊扩张压力泵 / 112` 建立独立 QA 模板或映射，避免与 `IDI` 模板混用。
- [x] M4 新增 RED/GREEN 静态合同并运行相邻 QA 回归。
- [x] M5 更新验证报告、执行 cleanup，并记录提交/推送状态。

## Expected Verification

- `node tests/e2e/qa-regulation-id-balloon-pressure-pump-pdf-items-static.spec.cjs`
- `node tests/e2e/qa-regulation-pressure-pump-complete-pdf-items-static.spec.cjs`
- `node tests/e2e/qa-regulation-pressure-pump-pdf-field-alignment-static.spec.cjs`
- `node tests/e2e/qa-regulation-product-specific-rules-static.spec.cjs`
- `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs`
- `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue IntRuoyiFronted/tests/e2e/qa-regulation-id-balloon-pressure-pump-pdf-items-static.spec.cjs IntRuoyiFronted/tests/e2e/qa-regulation-pressure-pump-complete-pdf-items-static.spec.cjs IntRuoyiFronted/tests/e2e/qa-regulation-pressure-pump-pdf-field-alignment-static.spec.cjs IntRuoyiFronted/tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs doc/tasks/20260806-qa-id-balloon-pressure-pump-pdf-items`
- `node node_modules/vue-tsc/bin/vue-tsc.js --noEmit -p tsconfig.relaxed.json`

## Applicable Gates

- `docs/frontend-development.md#前端静态契约隔离门禁`：新增最小静态合同先 RED，再实现 GREEN。
- `docs/frontend-development.md#复合输入控件交互保留门禁`：保留 QA 规程 DCC 项目代码下拉/复制交互，仅扩展产品模板映射。
- `docs/backend-development.md#mes-pqc-项目级检验快照门禁`：QA 检验规则、PQC 填写与项目级检验快照必须跟随正式产品，不同产品不得串用检验项目。
- `docs/task-closeout-rules.md#技能证据文件清理前归档门禁`：保留前端证据与验证报告。

## Design Constraint Check

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，按产品代码 `ID` 与正式 PDF 独立建模，不复用 `IDI` 样例模板。
- 是否存在临时补丁或绕过：否。

## Cleanup Candidates

- `tmp/pdfs/qa-id-balloon-pressure-pump/`

## Cleanup Keep

- `doc/tasks/20260806-qa-id-balloon-pressure-pump-pdf-items/frontend-feature-evidence.md`

## Current Status

ready_for_closeout

已完成 `ID / 球囊扩张压力泵 / 112` 的独立 `PQC-ID-001 (G/0)` QA 模板、17 条 PDF 5.1 检验内容、产品 ID 映射、静态合同回归、cleanup apply 和经验沉淀。提交/推送未执行：当前 `int_main` 工作区存在大量并发脏改动，且 `docs/frontend-development.md`、`docs/experience-index.md` 在本任务开始前已包含非本任务变更；为避免混入他人改动，本任务停留在 `ready_for_closeout`。
