# QA 压力泵 PDF 检验项完整识别

## Task Goal

按用户提供的 5 页截图逐页核对 `PQC-IDI-001（B 0）按压式球囊扩充压力泵组装过程检验规程--2026.01.04生效.pdf` 的检验项目清单，补齐/纠正 QA 规程配置页压力泵模板中的缺失或错误归属检验项。

## Milestones

- [x] M1 建立 5 页截图对应的完整 22 条静态合同并记录 RED。
- [x] M2 修正压力泵 QA 检验项模板，使每条检验项的工序、项目、页码、标准、方法、器具和抽样方案与截图一致。
- [x] M3 运行目标合同、相邻 QA 合同和 diff 检查。
- [x] M4 完成验证报告、cleanup 和提交/推送状态记录。
- [x] M5 按用户要求逐页复核 5 张截图，并把逐页行数、行级字段和验证结论补入静态合同与报告。

## Expected Verification

- `node tests/e2e/qa-regulation-pressure-pump-complete-pdf-items-static.spec.cjs`
- `node tests/e2e/qa-regulation-pressure-pump-pdf-field-alignment-static.spec.cjs`
- `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs`
- `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue IntRuoyiFronted/tests/e2e/qa-regulation-pressure-pump-complete-pdf-items-static.spec.cjs doc/tasks/20260806-qa-pressure-pump-complete-pdf-items/task.md doc/tasks/20260806-qa-pressure-pump-complete-pdf-items/execution-log.md doc/tasks/20260806-qa-pressure-pump-complete-pdf-items/verification-report.md`
- `pnpm ts:check`

## Applicable Gates

- PDF visual review: `pypdf` 无法抽取该扫描 PDF 文本，按用户提供的 5 页截图人工核对，静态合同锁定每页关键行。
- 前端静态契约隔离门禁：用任务专用静态合同覆盖当前 QA 模板完整性，避免被其它大合同历史问题干扰。
- PQC 检验项目事实门禁：检验项目、方法、器具、标准和抽样方案必须来自发布规程/PDF，不得用旧演示项或固定四项替代。

## Design Constraint Check

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，通过 5 页完整合同锁定模板数据来源和行级归属。
- 是否存在临时补丁或绕过：否。

## Cleanup Candidates

- doc/tasks/20260806-qa-pressure-pump-complete-pdf-items/frontend-feature-evidence.md

## Current Status

blocked

- 实现、验证和 cleanup 已完成。
- 用户要求的逐页验证已完成：5 张截图对应 PDF 第 3-7 页，逐页行数为 4/5/5/5/3，共 22 条，目标和相邻 QA 静态合同均 PASS。
- 提交/推送被阻塞：当前 `int_main` 工作区存在大量并发无关脏改动，且本地分支最新观测落后 `origin/int_main` 11 个提交；为避免混入其它任务，未执行提交或推送。
