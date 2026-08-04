# QA 规程页签配置

## Task Goal

在生产组长工作台补充 `QA 规程` 页签，让 QA 可以定义给 PQC 执行的过程检验规则，并用压力泵 PDF 规程信息初始化示例内容。该功能不接入 DCC、不做 DCC 文件分类、不作为受控文件上传入口。

## Milestones

- [x] 识别现有生产/PQC 工作台入口和 QA/PQC 需求边界。
- [x] 增加 QA 规程页签静态契约，先证明现有页面缺少该能力。
- [x] 实现 QA 规程配置 UI，包含规程元数据、适用范围、首检/巡检/末检规则、检验项目和发布完整性检查。
- [x] 运行定向静态契约和相邻 QA/PQC 回归验证。
- [x] 记录验证报告和剩余阻塞。
- [x] 增加检验项目原文依据摘录，让 QA 能看到每条解析标准对应的扫描 PDF 相关原文。

## Expected Verification

- RED: `node IntRuoyiFronted/tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` must fail before implementation because the existing workbench has no `QA 规程` tab.
- GREEN: `node IntRuoyiFronted/tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` must pass after implementation.
- REGRESSION: `pnpm --dir IntRuoyiFronted run e2e:role-matrix-qa-regulation:static` must pass to protect the existing QA regulation schema/dynamic item contract.
- REGRESSION: `pnpm --dir IntRuoyiFronted run e2e:role-matrix-pqc-dynamic-form:static` must pass to protect PQC dynamic rendering from published QA regulation data.
- REGRESSION: `pnpm --dir IntRuoyiFronted ts:check` must pass to prove the Vue/TypeScript changes are type-safe.
- EVIDENCE: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260804-qa-regulation-tab/frontend-feature-evidence.md` must pass.
- RED/GREEN: `node IntRuoyiFronted/tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` must require each QA inspection item to expose relevant original-source fields and short excerpts.
- REGRESSION: local browser E2E should still open `QA 规程`, show source excerpts, and send no backend write request while formal save/publish API is absent.

## BDD/TDD Acceptance Matrix

| Scenario ID | BDD Given / When / Then | RED Expected Failure | GREEN / Regression Evidence |
| --- | --- | --- | --- |
| BDD-QA-001 | Given QA opens the production/PQC workbench, When QA selects `QA 规程`, Then the page shows regulation metadata, scope, first/patrol/final rules, and item configuration. | Static contract fails because no `QA 规程` tab or stable QA selectors exist. | New static contract passes and `ts:check` passes. |
| BDD-QA-002 | Given the pressure-pump PDF is the source reference, When the QA tab loads, Then it shows `PQC-IDI-001`, `B/0`, `2026-01-04`, and `按压式球囊扩充压力泵组装过程检验规程`. | Static contract fails because pressure-pump metadata is absent from the workbench. | New static contract passes with source metadata assertions. |
| BDD-QA-003 | Given PQC only executes QA rules, When QA defines rules, Then the QA tab contains no DCC file classification, controlled-file upload, or document-control workflow semantics. | Static contract fails if QA block contains DCC/file-classification/controlled-file terms. | New static contract passes with negative DCC-coupling assertions. |
| BDD-QA-004 | Given formal save/publish API is not wired, When QA previews or runs publish checks, Then the page exposes missing publishing prerequisites and does not fake backend success. | Static contract fails because the workbench does not show API-not-wired and no-backend-write messaging. | New static contract passes and frontend evidence validator passes. |
| BDD-QA-005 | Given QA reviews a parsed inspection item, When QA checks its判定标准, Then the item shows only the relevant original PDF excerpt, source page, source item, and method excerpt so QA can compare parsed text with the source. | Static contract fails because item rows only have parsed standard/source notes and no original excerpt fields or UI. | Static contract verifies original-source excerpts are visible in the item model/UI; local browser E2E is currently blocked by a non-QA backend startup failure. |

## Test Data

- PDF source: `C:/Users/BJB110/Desktop/文档/1/PQC-IDI-001（B 0）按压式球囊扩充压力泵组装过程检验规程--2026.01.04生效.pdf`.
- Extracted reliable metadata: `PQC-IDI-001`, `B/0`, `2026-01-04`, `按压式球囊扩充压力泵组装过程检验规程`.
- QA draft product: `按压式球囊扩充压力泵`.
- Example order quantity: `301`, used to prove patrol sampling can display `301 × 5%` and round up to `16`.
- Inspection item seed rows are editable QA draft defaults, not authoritative OCR extraction.
- Source excerpts are manually transcribed from the rendered scanned PDF pages because direct PDF text extraction returned empty text. Excerpts are short, item-specific, and remain QA-reviewable rather than full-page OCR.

## E2E / User Path Plan

- Current verified path is a static frontend contract because no local runtime was started and no formal QA save/publish API is exposed in this slice.
- Future real E2E path: login as QA, open the production/PQC workbench, select `QA 规程`, edit rules, run publish precheck, verify no backend write is sent until the formal API exists.
- Future PQC integration path: publish a QA regulation through the formal API, login as PQC, verify generated tasks use the published version snapshot and not hardcoded demo items.

## Current Status

blocked

Item-level original-source excerpts are implemented and covered by static QA/PQC contracts plus `ts:check`. The required local browser E2E refresh for the new excerpt column is blocked because `E:\IntRuoyi` backend startup on `48081` fails before health with `@Resource annotation requires a single-arg method` on non-QA `MesProRouteFlowConfigServiceImpl.getRouteFlowProcessConfigList(Long,String)`. No mock login, API-only substitute, random port, or frontend-only success was used. Previous worktree runtime/browser E2E also remains blocked because `scripts\runtime\reserve-worktree-slot.ps1` reported no available `int_main` slot in `1..19`; commit/push closeout remains blocked by the missing worktree registry entry and broader dirty/ahead repository state.

## Design Constraints

- QA 是给 PQC 制定规则的角色，PQC 按 QA 发布规程执行。
- QA 规程页签不得出现 DCC、文件分类、受控文件上传或文控审批含义。
- 页面可以用压力泵 PDF 元数据初始化示例，但不得宣称已完成 DCC 识别或受控文件归档。
- 每个解析后的检验项目必须能看到与该项目相关的短原文摘录，不展示整页 OCR，也不把看不清或未定位的内容伪装成已确认原文。
- 如果正式保存/发布 API 未接入，页面必须明确停留在前端配置/发布前检查表达，不得伪造持久化成功。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按 QA 规程 -> PQC 执行的业务边界设计页面入口和静态契约。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- 前端静态契约隔离门禁：新增任务专用静态契约覆盖 QA 页签行为，避免被无关大契约失败掩盖。
- 任务验证脚本保留门禁：若任务专用脚本需要作为长期证据，必须在收尾前记录保留原因。
- 技能证据文件清理前归档门禁：`frontend-feature-evidence.md` 通过 validator 后，把核心结论复制到 `verification-report.md`。

## Worktree Verification

- Worktree: `D:\IntRuoyiWorktree\2020804_qa`.
- Branch: `codex/2020804_qa`.
- Dependency prerequisite: `pnpm --dir IntRuoyiFronted install --frozen-lockfile` -> PASS, `node_modules` created from the worktree lock file.
- Runtime prerequisite: `powershell -ExecutionPolicy Bypass -File scripts\runtime\reserve-worktree-slot.ps1 -Name 2020804_qa -Path D:\IntRuoyiWorktree\2020804_qa -Branch codex/2020804_qa -Profile int_main -AsJson` -> BLOCKED, no available runtime slot for profile `int_main` in range `1..19`.
- Commit prerequisite: `powershell -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1` -> BLOCKED, no worktree port registry entry is registered for `D:\IntRuoyiWorktree\2020804_qa`.

## Int Main Sync Verification

- Workspace: `E:\IntRuoyi`.
- Branch: `int_main`.
- Source sync: `IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue` matches `2020804_qa` when ignoring EOL-only differences.
- GREEN: local browser E2E on `http://127.0.0.1:8081/mes/pro/process-pool/team-leader` -> PASS; verified login, `QA 规程` tab, pressure-pump metadata, inspection rules/items, completeness checks, PQC task preview, local draft add, precheck messages, no DCC terms, no backend write requests, no console/page errors.
- Evidence screenshot: `doc/tasks/20260804-qa-regulation-tab/qa-regulation-live-e2e.png`.
- GREEN: `node IntRuoyiFronted\tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS.
- GREEN: `pnpm --dir IntRuoyiFronted run e2e:role-matrix-qa-regulation:static` -> PASS.
- GREEN: `pnpm --dir IntRuoyiFronted run e2e:role-matrix-pqc-dynamic-form:static` -> PASS.
- GREEN: `pnpm --dir IntRuoyiFronted run ts:check` -> PASS.
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260804-qa-regulation-tab/frontend-feature-evidence.md` -> PASS.
- GREEN: `E:\IntRuoyi` `node IntRuoyiFronted\tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS after adding original-source excerpt UI and fields.
- GREEN: `E:\IntRuoyi\IntRuoyiFronted` `pnpm run e2e:role-matrix-qa-regulation:static` -> PASS after adding original-source excerpt UI and fields.
- GREEN: `E:\IntRuoyi\IntRuoyiFronted` `pnpm run e2e:role-matrix-pqc-dynamic-form:static` -> PASS after adding original-source excerpt UI and fields.
- GREEN: `E:\IntRuoyi\IntRuoyiFronted` `pnpm run ts:check` -> PASS after adding original-source excerpt UI and fields.
- BLOCKED: local browser E2E refresh for source excerpts -> backend `48081` failed to start; latest log shows `BeanCreationException` for non-QA `mesProRouteFlowConfigController`, caused by `@Resource annotation requires a single-arg method` on `MesProRouteFlowConfigServiceImpl.getRouteFlowProcessConfigList(Long,String)`.

## Cleanup Keep

- doc/tasks/20260804-qa-regulation-tab/frontend-feature-evidence.md
- doc/tasks/20260804-qa-regulation-tab/qa-regulation-live-e2e.png
