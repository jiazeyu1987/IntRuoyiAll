# DCC-STATIC-021 Drawing Preview PDF Fix

## Task Goal

修复 `docs/bugs/20260912-dcc-90-step-static-audit.md` 中 DCC-STATIC-021：工程图纸当前版本预览必须选择与当前源件明确绑定的 `drawingPdfFileId`，签名证据保留源件关联；替换图纸后不得误用旧配套 PDF，缺少当前配套件要明确拒绝。

## Milestones

- [x] 创建干净 worktree，隔离既有未提交 diff。
- [x] 读取并遵守 `docs/task-closeout-rules.md`、`docs/backend-development.md`、`docs/powershell-encoding.md`、`docs/worktree-restrictions.md`。
- [x] 记录 DCC-STATIC-021 的 Given/When/Then 与 RED 失败证据。
- [x] 最小化修复图纸类 WORKING/待审批预览二进制选择逻辑。
- [x] 执行定向单元/静态合同/编译验证，不执行 E2E、服务启动、数据库写入或远程操作。
- [x] 更新本项 bug 状态/证据与任务验证报告。
- [x] 按 2026-09-14 用户授权，将本任务实现融合进本地 `int_main`；远程推送仍未授权。

## Expected Verification

- RED：新增 DCC-STATIC-021 静态合同或定向测试在修复前失败，证明 WORKING/待审批图纸预览仍选择 CAD `sourceFileId` 或可能误用旧 PDF。
- GREEN：同一测试在修复后通过，证明当前图纸源件只使用当前绑定 `drawingPdfFileId` 作为预览二进制。
- REGRESSION：运行 DCC 相关定向 Maven 测试/编译验证；如测试范围受限，记录命令和边界。
- 静态检查：确认签名证据仍使用源件关联，不开放未发布 CAD 源件下载，不修改其它 DCC-STATIC 项。

## Current Status

completed

实现、定向验证、cleanup apply 和本地 `int_main` fast-forward 融合已完成；远程推送未授权未执行。

## Design Constraints Check

- 禁止 fallback、降级、吞异常、模拟成功或旧 PDF 补齐。
- 图纸当前版本预览必须使用当前源件明确绑定的 `drawingPdfFileId`。
- 缺少当前配套 PDF 时必须明确拒绝预览，而不是选择 CAD 源件或历史 PDF。
- 签名证据继续保留 `sourceFileId` 关联，预览载体和签名源件证据不得混淆。
- 本轮不执行 E2E、不启动/重启服务、不写数据库、不操作远程。
- 2026-09-14 用户授权“融合进 int_main”；仅允许本地任务提交和本地 `int_main` 融合，远程推送仍未授权。

## Worktree Evidence

- Source project: `E:\IntRuoyi`
- Clean worktree: `D:\IntRuoyiWorktree\20260913-dcc-static-021-drawing-preview-pdf-clean`
- Branch: `codex/20260913-dcc-static-021-drawing-preview-pdf-clean`
- Base HEAD: `f773eab07ff86df182ef404e8279040468608dec`
- Runtime slot reservation: `int_main` slot `9`, frontend `8090`, backend `48090`; services will not be started.
- Clean worktree implementation commit: `328323cc3`
- Latest int_main fusion worktree: `D:\IntRuoyiWorktree\20260914-dcc-static-021-int-main-fusion-v2`
- Local int_main FF merge: `cd376a2a2..b80fd2655`
- Post-merge branch runtime port guard: PASS for `int_main/int_main`, frontend `8081`, backend `48081`
- Closeout cleanup apply: PASS, delete `<none>`, keep task records and `bug-regression-evidence.md`
- Pre-existing main-worktree dirty changes were preserved outside this task; they were not staged or committed by DCC-STATIC-021.
- Clean implementation worktree removed: `D:\IntRuoyiWorktree\20260913-dcc-static-021-drawing-preview-pdf-clean`; Git registration absent and runtime slot `9` marked inactive.
- Retained worktree pending explicit discard authorization: `D:\IntRuoyiWorktree\20260914-dcc-static-021-int-main-fusion-v2`; it contains staged task-document drafts superseded by local `int_main` closeout commit.

## Cleanup Keep

- doc/tasks/20260913-dcc-static-021-drawing-preview-pdf/bug-regression-evidence.md
