# DCC-STATIC-021 Execution Log

## Scope

- Only DCC-STATIC-021 from `docs/bugs/20260912-dcc-90-step-static-audit.md`.
- No E2E, service start/restart, database write, remote operation, Git commit, or Git push in this task unless separately authorized.

## BDD

- BDD: 当前图纸版本预览使用当前配套 PDF -> Given 当前 WORKING 或待审批图纸版本保存了 CAD/SolidWorks 源件和与该源件明确绑定的 `drawingPdfFileId`; When 申请人或当前审批人通过受控预览读取正文; Then 后端选择该版本当前绑定的 PDF 二进制作为预览载体，并保留源件 ID 用于签名证据。
- BDD: 替换图纸不得复用旧配套 PDF -> Given A/1 图纸有源件 S1/PDF P1 且检入 A/2 替换为源件 S2; When A/2 未携带与 S2 绑定的当前 `drawingPdfFileId` 就进入 WORKING 或审批预览; Then 后端明确拒绝缺少当前配套 PDF，不选择 S2 的 CAD 源件，也不复用 P1。

## RED

- RED: `node IntRuoyiBackend/yudao-module-dcc/src/test/js/dcc-static-021-drawing-preview-pdf-contract.spec.cjs` -> FAIL, expected reason: static contract could not find `resolveCurrentRevisionPreviewFileId`; old `resolveBinaryFileId` still returned `sourceFileId` directly for WORKING preview.
- RED: `mvn -pl yudao-module-dcc -am "-Dtest=cn.iocoder.yudao.module.dcc.service.file.DccControlledFileQueryServiceTest#readPreviewFile_workingDrawingRequesterReadsCurrentDrawingPdfBinary+getPreviewMetadata_workingDrawingWithoutCurrentPdfRejectsBeforeSourcePreview+checkinDrawingSourceWithoutCurrentPdfRejectsBeforeCopyingOldPdf" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: 3/3 targeted regressions failed; WORKING drawing preview returned `a2-drawing.dwg` instead of `a2-drawing.pdf`, missing PDF metadata did not throw, and drawing check-in without current PDF returned the old check-in error instead of `CONTROLLED_FILE_DRAWING_PDF_REQUIRED`.

## Work Log

- 已读取 `bug-regression-fix-loop` 技能及 `references/bug-contract.md`。
- 已读取项目 `AGENTS.md`、`docs/task-closeout-rules.md`、`docs/backend-development.md`、`docs/powershell-encoding.md`、`docs/worktree-restrictions.md`、`docs/branch-runtime-ports.md`。
- 已识别旧 DCC-STATIC-021 worktree 存在未提交改动，因此创建新的 clean worktree：`D:\IntRuoyiWorktree\20260913-dcc-static-021-drawing-preview-pdf-clean`。
- 已预约 `int_main` runtime slot `9`，frontend `8090`，backend `48090`；本任务不启动服务。
- 已补充 `DccControlledFileQueryServiceTest` 三个回归断言和 `dcc-static-021-drawing-preview-pdf-contract.spec.cjs` 静态合同。
- 已修改 `DccControlledFileQueryServiceImpl`：WORKING/待审批当前版本预览改为解析当前源件，图纸源件返回当前 `drawingPdfFileId`；替换图纸源件检入时若无当前配套 PDF 则拒绝，不复制旧 PDF。
- Root Cause: `resolveBinaryFileId` 对 WORKING/待审批版本直接返回 `sourceFileId`，导致 DWG/SolidWorks 源件进入预览链路；检入复制下一小版本时也会无条件沿用旧 `drawingPdfFileId`，存在源件 S2 误配旧 PDF P1 的风险。
- 2026-09-14 用户授权“融合进 int_main”；本任务仅执行本地任务提交和本地 `int_main` 融合，不执行远程推送。
- 已从本地 `int_main` `cd376a2a2` fast-forward 到 `b80fd2655`，post-merge branch runtime port guard PASS。
- 已运行 `task_closeout.py --mode apply --worktree-closeout off`，结果 PASS；delete `<none>`，保留任务记录与 `bug-regression-evidence.md`。
- 已使用 `project-experience-consolidation` 规则复核，本次复用既有 `docs/worktree-memory.md` 经验，无需新增长期经验文档。
- 主工作区既有未提交 dirty 通过 stash 暂存后恢复，未纳入本任务提交；远程 push 仍未授权未执行。

## Verification Evidence

- GREEN: `node IntRuoyiBackend\yudao-module-dcc\src\test\js\dcc-static-021-drawing-preview-pdf-contract.spec.cjs` -> PASS, `DCC-STATIC-021 drawing preview PDF contract passed`。
- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=cn.iocoder.yudao.module.dcc.service.file.DccControlledFileQueryServiceTest#readPreviewFile_workingDrawingRequesterReadsCurrentDrawingPdfBinary+getPreviewMetadata_workingDrawingWithoutCurrentPdfRejectsBeforeSourcePreview+checkinDrawingSourceWithoutCurrentPdfRejectsBeforeCopyingOldPdf" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`; reactor `BUILD SUCCESS`。
- GREEN: post-merge `node IntRuoyiBackend\yudao-module-dcc\src\test\js\dcc-static-021-drawing-preview-pdf-contract.spec.cjs` -> PASS。
- GREEN: post-merge `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260913-dcc-static-021-drawing-preview-pdf\bug-regression-evidence.md` -> PASS。
- GREEN: post-merge Maven targeted regression on local `int_main` -> PASS, `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`; reactor `BUILD SUCCESS`。
- CLEANUP_APPLY: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260913-dcc-static-021-drawing-preview-pdf --mode apply --worktree-closeout off` -> PASS, delete `<none>`。
- Verification scope: 静态合同 + DCC 查询服务定向单测/编译链路；未执行 E2E、服务启动/重启、数据库写入、远程操作。

## Blockers

- 暂无本任务阻塞；远程 push 未授权，未执行。
