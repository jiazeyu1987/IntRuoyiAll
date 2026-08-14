# Execution Log

## User Intent

- 用户确认：截图中 `ID / 球囊扩张压力泵 / 112` 产品应对应 `C:\Users\BJB110\Desktop\文档\1\PQC-ID-001 (G 0) （椎体）球囊扩张压力泵组装过程检验规程.pdf`，需要理解并按该 PDF 处理 QA 规程配置。

## BDD

- BDD: ID 产品使用独立 PQC-ID-001 规程 -> Given 用户在 QA 规程配置选择 `ID / 球囊扩张压力泵 / 112`, When 页面初始化或复制该产品 QA 检验项目, Then 系统应使用 `PQC-ID-001 (G 0)` 的检验项目、标准、方法、器具和抽样方案，而不是 `PQC-IDI-001` 的按压式压力泵模板。
- BDD: ID 与 IDI 模板互不串用 -> Given 系统同时支持 `ID` 和 `IDI` 两个压力泵类产品, When 用户分别选择两个产品, Then 两个产品的 QA 检验项目来源 PDF、产品代码和逐页项目合同应可区分并独立验证。

## Command Log

- 2026-08-06: Read `pdf` skill, `frontend-feature-delivery` skill, `docs/task-closeout-rules.md`, `docs/frontend-development.md`, and `docs/powershell-encoding.md`.
- 2026-08-06: Created task directory `doc/tasks/20260806-qa-id-balloon-pressure-pump-pdf-items/`.
- 2026-08-06: Confirmed source PDF exists at `C:\Users\BJB110\Desktop\文档\1\PQC-ID-001 (G 0) （椎体）球囊扩张压力泵组装过程检验规程.pdf`; `pypdf` text extraction was blank because the PDF is image/scanned.
- 2026-08-06: Rendered all 14 PDF pages with Poppler `pdftoppm`; inspected rendered pages and identified `5.1 检验内容` on document pages 4-7, containing 17 inspection rows.
- 2026-08-06: Added `IntRuoyiFronted/tests/e2e/qa-regulation-id-balloon-pressure-pump-pdf-items-static.spec.cjs` to lock `ID / PQC-ID-001` template content and prevent `IDI / PQC-IDI-001` item reuse.
- 2026-08-06: Updated `IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue` with `BALLOON_PRESSURE_PUMP_PROJECT_CODE = 'ID'`, independent `PQC-ID-001` draft metadata, 17 PDF rows, separate product-id binding cache, and product-id based loader branch.
- 2026-08-06: Updated existing `IDI` static-contract slice boundaries so the older 22-row contract continues to validate only `createPressurePumpQaRegulationItems`.
- 2026-08-06: Validated `frontend-feature-evidence.md` with `validate_frontend_feature.py`.
- 2026-08-06: Ran `task_closeout.py --mode preview` and `--mode apply`; deleted only `tmp/pdfs/qa-id-balloon-pressure-pump/` and kept `task.md`, `execution-log.md`, `verification-report.md`, and `frontend-feature-evidence.md`.
- 2026-08-06: Consolidated reusable static-contract boundary experience into `docs/frontend-development.md#前端静态契约隔离门禁` and updated `docs/experience-index.md`.
- 2026-08-06: Did not commit or push because the shared `int_main` workspace has extensive unrelated dirty changes, and `docs/frontend-development.md` / `docs/experience-index.md` already contained non-task changes before this task; committing from this state risks mixing concurrent work.

## TDD Evidence

- RED: `node tests/e2e/qa-regulation-id-balloon-pressure-pump-pdf-items-static.spec.cjs` -> FAIL, expected reason `ID balloon pressure-pump draft template must exist.`
- GREEN: `node tests/e2e/qa-regulation-id-balloon-pressure-pump-pdf-items-static.spec.cjs` -> PASS
- GREEN: `node tests/e2e/qa-regulation-pressure-pump-complete-pdf-items-static.spec.cjs` -> PASS
- GREEN: `node tests/e2e/qa-regulation-pressure-pump-pdf-field-alignment-static.spec.cjs` -> PASS
- GREEN: `node tests/e2e/qa-regulation-product-specific-rules-static.spec.cjs` -> PASS
- GREEN: `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS
- GREEN: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue IntRuoyiFronted/tests/e2e/qa-regulation-id-balloon-pressure-pump-pdf-items-static.spec.cjs IntRuoyiFronted/tests/e2e/qa-regulation-pressure-pump-complete-pdf-items-static.spec.cjs IntRuoyiFronted/tests/e2e/qa-regulation-pressure-pump-pdf-field-alignment-static.spec.cjs IntRuoyiFronted/tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs doc/tasks/20260806-qa-id-balloon-pressure-pump-pdf-items` -> PASS
- GREEN: `node node_modules/vue-tsc/bin/vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> PASS
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260806-qa-id-balloon-pressure-pump-pdf-items\frontend-feature-evidence.md` -> PASS
- BLOCKED: `pnpm ts:check` through bundled wrapper -> FAIL before typecheck with `[ERR_PNPM_ABORTED_REMOVE_MODULES_DIR_NO_TTY]`; direct project script command above completed type checking without reinstalling or purging dependencies.

## Notes

- 当前工作区存在大量并发脏改动；本任务将只修改 QA 规程页面、目标静态合同和本任务文档，避免回滚或覆盖并发任务内容。
- 本任务不改变后端接口、DCC 项目代码下拉交互或 `IDI / PQC-IDI-001` 既有 22 行模板内容。
- Closeout blocker: implementation and verification are complete, but repository commit/push is intentionally not performed from the dirty shared workspace to avoid committing unrelated concurrent task changes.
