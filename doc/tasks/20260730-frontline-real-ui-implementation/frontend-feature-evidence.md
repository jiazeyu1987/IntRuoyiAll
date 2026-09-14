# Frontend Feature Evidence

## Feature

Real frontend simplified operator surfaces for production frontline reporting and PQC frontline inspection.

## Scope

- Frontend component: `IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue`
- Static contract: `IntRuoyiFronted/src/views/mes/pro/feedback/frontline-template-render.spec.cjs`
- Protected files not changed: backend controllers/services, API wrapper contracts, DTO/schema, database, mock/seed data.

## Acceptance

- Production operator surface shows only 工序 / 员工 / 主页 in the top area, does not expose work-order fields, and keeps only required production inputs.
- Production device area supports no-device state and caps visible device parameter cards to three devices.
- PQC operator surface shows 生产订单 / 工序 / 员工 / 主页 in the same top area pattern.
- PQC inspection content is editable for 长度、外观、密封、压力.
- PQC fill panel keeps 首检 / 巡检 / 末检, patrol round, 检验数量 and 损耗数量.
- PQC UI removes success/failure result buttons, inspection method row, patrol summary text and statistics.

## BDD:

- Production operator -> Given a frontline production worker enters the feedback page, When the page renders, Then only process, employee, home, required quantity inputs and device parameter inputs are visible.
- PQC operator -> Given a PQC worker enters a PQC template context, When the page renders, Then production order, process, employee, home, editable inspection content and required inspection quantity fields are visible.

## RED:

- `node src\views\mes\pro\feedback\frontline-template-render.spec.cjs` -> FAIL, expected reason: old fixed-template panel did not expose `data-frontline-production-operator` and still showed the technical template form.

## GREEN:

- `node src\views\mes\pro\feedback\frontline-template-render.spec.cjs` -> PASS.
- `node src\views\mes\pro\feedback\frontline-template-switch.spec.cjs` -> PASS.
- `pnpm ts:check` -> PASS.
- `git diff --check -- IntRuoyiFronted\src\views\mes\pro\feedback\FrontlineFixedTemplatePanel.vue IntRuoyiFronted\src\views\mes\pro\feedback\frontline-template-render.spec.cjs doc\tasks\20260730-frontline-real-ui-implementation` -> PASS with CRLF normalization warnings only.

## Verification

- Static contracts and TypeScript checks passed.
- Main review confirmed no backend/API/schema/data contract files were changed.

## Blockers

- Sub-agent dispatch was attempted multiple times, but the local `spawn_agent` tool rejected calls because its schema treated empty `message` and `items` fields as mutually provided. Main thread continued with the same two-surface split and recorded this blocker.
- PQC detailed inspection fields are front-end design fields only at this stage. The existing formal backend template still accepts only the old `PQC_RESULT` field, so PQC submit fails fast instead of fabricating a success payload.
