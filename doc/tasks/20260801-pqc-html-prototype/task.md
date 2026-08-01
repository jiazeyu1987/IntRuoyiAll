# 20260801 PQC HTML Prototype

## Task Goal

Create an independent static HTML prototype for a frontline PQC inspection entry page based on the provided paper inspection record image.

## Milestones

- [x] Establish task documentation and constraints.
- [x] Build a standalone HTML prototype under `output/`.
- [x] Verify the HTML file exists, is UTF-8 readable, and contains the required PQC entry sections.
- [x] Record final verification and closeout status.

## Expected Verification

- Structural check confirms the HTML contains the header, basic production information, inspection table, pass/fail decision controls, inspector/signature area, and operator actions.
- No backend, API, routing, database, or runtime service changes are made.
- UTF-8 readback confirms Chinese labels are preserved.

## Current Status

completed

## Design Constraints Check

- `是否引入 fallback/降级/吞异常`: 否。
- `是否从根因和长期维护角度解决`: 是，先交付独立静态原型，后续可按正式前端路由和数据契约接入。
- `是否存在临时补丁或绕过`: 否。

## Experience Gate

`docs/experience-index.md` exists and was reviewed. Applicable style gate: frontend page/table/style work should follow the IntPP operations-console style from `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`; this prototype uses the requested paper-form visual while keeping the project blue/white/gray dense operations style.

## Cleanup Keep

- output/pqc-frontline-inspection-record.html
- output/playwright/20260801-pqc-html-prototype/pqc-frontline-inspection-record.png
