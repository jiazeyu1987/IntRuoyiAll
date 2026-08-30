# Execution Log

## User Intent

- 根据当前代码实现最小 PDF/A 归档链路。
- 在独立 worktree 中完成开发和验证，完成后融合到 `int_main`。

## Worktree Baseline

- Worktree: `D:\IntRuoyiWorktree\20260831-edhr-pdfa-archive-minimal`
- Branch: `codex/20260831-edhr-pdfa-archive-minimal`
- Base: `int_main@58479242435efa1f7eafd6e0a17e36bd9c811e5f`
- Trigger rules read: `docs/worktree-restrictions.md`, `docs/task-closeout-rules.md`, `docs/powershell-memory.md`, `docs/backend-development.md`, `docs/database-rules.md`, `docs/frontend-development.md`.

## BDD Scenarios

BDD: 完整批次生成 PDF/A-1b 归档 -> Given 已关闭批次具有完整模板、填写、签名和附件清单，且受保护存储可用 / When 归档责任人生成最终归档 / Then 系统生成通过独立校验的 PDF/A-1b，保存原文件和 SHA-256，完成归档任务并将批次标记已归档。

BDD: 模板布局缺失阻止归档 -> Given 已关闭批次中的正式批记录缺少可打印模板布局 / When 归档责任人生成最终归档 / Then 系统明确拒绝生成，批次保持已关闭且归档任务保持未完成。

BDD: PDF/A 校验失败阻止归档 -> Given 渲染结果不符合 PDF/A-1b / When 系统执行归档校验 / Then 系统拒绝封存，不创建成功归档且不推进批次状态。

BDD: 受保护存储失败阻止归档 -> Given PDF/A 文件有效但受保护存储未返回完整保留证据 / When 系统封存归档 / Then 系统明确失败，批次保持已关闭且归档任务保持未完成。

BDD: 下载返回封存原文件 -> Given 批次已有有效且封存的 PDF/A 归档 / When 有权限用户下载归档 / Then 系统读取封存原文件、复核 SHA-256 并返回与生成时相同的字节；哈希不一致时拒绝下载。

BDD: 历史普通 PDF 不冒充 PDF/A -> Given 历史归档没有 PDF/A 校验记录 / When 用户查看历史追溯 / Then 页面不显示 PDF/A 合规标识，并仍按原有只读规则处理历史数据。

## TDD Sequence

1. RED：新增 PDF/A 渲染及校验测试，证明当前普通 PDF 不通过 PDF/A-1b 合同。
2. GREEN：实现 PDF/A 元数据、输出色彩配置、字体嵌入和独立校验。
3. RED：新增归档服务测试，证明当前实现未保存原文件且下载会重新生成。
4. GREEN：新增持久化字段并复用受保护存储，下载读取原文件并校验哈希。
5. RED：新增前端合同，证明历史页未展示 PDF/A profile/status。
6. GREEN：实现最小历史页状态、加载态与错误展示。
7. REGRESSION：运行相关后端、迁移、前端、PDF 格式和视觉验证。

## M2 RED Evidence

RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchArchivePdfAComplianceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 2 tests / 2 failures。当前 PDF 被 Preflight 判定缺少 `DestOutputProfile` 和 XMP metadata；缺失模板布局时也没有抛出异常。

RED: `mvn -pl yudao-module-mes -am -rf :yudao-module-mes "-Dtest=MesProEdhrBatchArchivePdfAComplianceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL at testCompile, expected reason: `MesProEdhrPdfAValidator` 尚不存在，归档服务没有可注入的独立 PDF/A 校验边界。

GREEN: `mvn -pl yudao-module-mes -am -rf :yudao-module-mes "-Dtest=MesProEdhrBatchArchivePdfAComplianceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 3 tests / 0 failures。生成文件通过 Preflight PDF/A-1b，缺布局失败，普通 PDF 被独立校验器拒绝。

RED: `mvn -pl yudao-module-mes -am -rf :yudao-module-mes "-Dtest=MesEdhrBatchArchivePdfASchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 2 tests / 1 failure / 1 error。正式迁移不存在且 H2 归档表缺少 `file_id` 等 PDF/A 证据字段。

GREEN: `mvn -pl yudao-module-mes -am -rf :yudao-module-mes "-Dtest=MesEdhrBatchArchivePdfASchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 2 tests / 0 failures。迁移为五个可空增量字段且无历史回填，H2 测试表同步。

RED: `mvn -pl yudao-module-mes -am -rf :yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#generateArchive_requiresClosedBatch+generateArchive_pdfAValidationFailure_keepsBatchClosedAndTaskPending+generateArchive_incompleteStorageEvidence_keepsBatchClosedAndTaskPending+downloadArchive_checksumMismatch_rejectsStoredContent" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 4 tests / 4 failures。现有服务未调用 PDF/A 校验和受保护存储、未保存 profile、下载篡改未被拒绝。首次运行曾被主线新增的不合格评审服务缺少测试 mock 阻断，补齐正式测试依赖后取得上述业务 RED。

## Milestone Status

- `M1`: completed - 已冻结 PDF/A-1b、不可变原文件、失败不推进状态、历史普通 PDF 不冒充合规文件的验收范围，并写入首轮渲染 RED 测试。
- `M2`: completed - 生成文件包含 PDF/A-1b XMP、sRGB OutputIntent、内嵌字体，并由独立 Preflight 校验器 fail-fast。
- `M3`: completed - 新归档保存受保护对象版本和保留证据，下载读取原文件并校验 SHA-256；迁移为可空增量且不回填历史。
- `M4`: completed - 批次详情归档抽屉和正式表单追溯抽屉显示真实 PDF/A 状态，生成动作有同步加载态。
- `M5`: completed - 定向后端、迁移、前端、PDF 格式和视觉验证通过；未执行需要受保护 S3 与任务自有关闭批次的真实写入 E2E。
- `M6`: pending

## Final Verification Evidence

GREEN: `mvn -pl yudao-module-mes -am -rf :yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#<12 archive methods>" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 12 tests / 0 failures / 0 errors。

GREEN: `MesProEdhrBatchArchivePdfAComplianceTest`、`MesEdhrBatchArchivePdfASchemaTest`、`ExecutionArchiveRendererTest` -> PASS, 16 tests / 0 failures / 0 errors。

GREEN: `node tests/e2e/edhr-batch-pdfa-archive-static.spec.js`、`edhr-batch-history-static.spec.js`、`edhr-batch-history-evidence-layout-static.spec.js` -> PASS。

GREEN: `pnpm ts:check` -> PASS。

GREEN: `pnpm build:local` -> PASS，Vite 输出 `Build successful. Please see dist directory`。

GREEN: release migration policy gate with `20260612_mes_edhr_void_reopen_supplement.sql` and `20260831_mes_edhr_batch_archive_pdfa.sql` -> PASS。

GREEN: backend/database/frontend skill evidence validators and their self-tests -> PASS。

GREEN: Poppler `pdfinfo` and `pdftoppm` -> sample is unencrypted PDF 1.4, A4, 3 pages, metadata stream present；逐页 PNG 检查无空白、裁切、重叠或黑块。

REGRESSION: 完整 `MesProEdhrBatchExecutionServiceTest` 运行 177 tests，归档相关用例通过，但两个非归档方法失败：`previewTask_generatesDynamicRouteFormSignatureMarkersFromSignatureRules`、`openExistingBatch_shouldRecoverMissingRouteProcessTasks`。本任务未修改其生产路径，不将该全类命令记录为通过。

REGRESSION: `edhr-final-archive-work-task-static.spec.js` 命中既有 `workTaskId: number` 断言与当前 `EdhrRouteId` 合同不一致；本任务未修改 workTaskId 身份类型，不将该相邻合同记录为通过。

## Experience Consolidation

- 已调用 `project-experience-consolidation`。
- 未新增或修改长期经验文档：PDF/A 实现细节与一次性验证归任务证据；受保护存储、worktree、Maven、E2E 通用门禁已有正式文档，避免重复；同时不触碰 `int_main` 上用户正在修改的 `docs/experience-index.md` 和 `docs/backend-development.md`。

## Commit And Closeout

- Runtime slot: `int_main slot 58`, frontend `8313`, backend `48313`；未启动服务。
- Branch runtime port guard: PASS before implementation commit.
- Implementation commit: `0dc0ff392` (`feat: 完成 eDHR 批次 PDF/A 归档闭环`), 16 task-owned production/test/migration files.
- First closeout preview correctly kept `task.md`、`execution-log.md`、`verification-report.md` and selected evidence/temp outputs for deletion, but automatic merge was blocked because `E:\IntRuoyi` contains unrelated user changes.
- Cleanup apply deleted the three skill evidence files and `dist`, then twice hit a Windows disappearing-file race inside `node_modules`; direct PowerShell removal was blocked by the environment safety policy。`node_modules` 不再单独清理，将随任务 worktree 删除；剩余 `tmp/pdfs` 继续由 cleanup skill 处理。
- Final cleanup preview (`--worktree-closeout off`) kept only `task.md`、`execution-log.md`、`verification-report.md` and selected `tmp/pdfs` for deletion；apply status `applied`，`tmp/pdfs` 已删除。`node_modules` 随 worktree 删除。
- Task documentation commit: `a96fa28eb` (`docs: 记录 eDHR PDF/A 归档验证结果`).
- `int_main` advanced during development from `584792424` to `d9fe88557`; direct ff was no longer possible.
- Merged current `int_main@d9fe88557` into the task worktree without conflict, producing integration commit `580ed03ca`.
- After integration, `int_main` is an ancestor of task HEAD; task incoming path count is 19 and exact intersection with the 40 dirty/untracked paths in `E:\IntRuoyi` is 0.

## Post-Integration Verification

GREEN: PDF/A compliance + schema + renderer tests after integrating current `int_main` -> PASS, 16/16.

GREEN: all archive service methods after integrating current `int_main` -> PASS, 12/12.

GREEN: `edhr-batch-pdfa-archive-static.spec.js`、`edhr-batch-history-static.spec.js`、`edhr-batch-history-evidence-layout-static.spec.js` -> PASS.

GREEN: `pnpm ts:check` -> PASS after integration.

GREEN: `pnpm build:local` -> PASS after integration.

GREEN: release migration policy gate with declared dependency closure -> PASS after integration.
