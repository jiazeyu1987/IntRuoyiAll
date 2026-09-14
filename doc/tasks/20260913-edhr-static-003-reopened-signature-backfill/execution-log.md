# Execution Log

## Preflight

- 2026-09-13：读取 `C:\Users\BJB110\.codex\worktrees\ac42\IntRuoyi\AGENTS.md`。
- 2026-09-13：读取 `docs/task-closeout-rules.md`、`docs/backend-development.md` 相关 eDHR/签名/分配门禁、`docs/powershell-encoding.md`、`docs/test-release-preflight.md`、`docs/adr/ADR-0003-unified-electronic-signature-kernel.md`。
- 2026-09-13：当前 worktree 缺少 `docs/bugs/20260912-edhr-90-step-static-audit.md` 与 `doc/tasks/20260913-edhr-fix-independent-audit/verification-report.md`，已按项目主目录 `E:\IntRuoyi` 读取两份证据；本任务改动仍限定当前 worktree。
- 2026-09-13：读取 `bug-regression-fix-loop` 技能和 `references/bug-contract.md`。

## BDD / TDD Evidence

- BDD: EDHR-STATIC-003 legacy approved review signature backfill -> Given 旧 CURRENT 分配已引用 APPROVED 复核且数量等于本次期望数量，但复核缺少 `reviewSignatureId`、`reviewSignatureUserId` 或 `reviewSignatureSnapshotJson`; When 生产组长提供本人签名密码并保持原数量重新确认; Then 系统补齐原复核签名证据、保持原分配 `reviewId` 引用、同步确认时间，并让完工读取的反馈批准人与复核签名证据一致。
- BDD: EDHR-STATIC-003 duplicate confirmation is idempotent -> Given 上述旧复核已补齐签名证据; When 生产组长再次以相同数量确认; Then 不重复创建复核、不重复调用签名、不改写已完整签名快照。

## Work Log

- 2026-09-13：继续当前任务时工作区已有任务实现草稿；未回滚 task-owned 改动来重放最早的预实现 RED，避免改写已有工作区状态。
- 2026-09-13：补齐 `MesReportAllocationCommandServiceTest.unchangedAllocationWithLegacyUnsignedReviewMustBackfillReviewSignatureEvidence`，覆盖旧 CURRENT 分配引用 APPROVED 复核但签名字段缺失、数量不变并本人重新签名确认的路径。
- 2026-09-13：最小修改 `MesReportAllocationCommandService.save` / `requireReview`：数量不变时检查当前分配的 `reviewId` 证据；缺签 APPROVED 复核通过本人签名补齐原复核；缺 `reviewId` 的 CURRENT 行绑定同一复核；非 APPROVED 复核不静默升级。
- 2026-09-13：新增 `MesProcessPoolReportAllocationMapper.attachReviewToCurrentRowsByEventId` 与 `refreshReviewEvidenceForCurrentRowsByReviewId`，仅更新当前事件 CURRENT 分配行的复核引用、确认人和确认时间。
- 2026-09-13：补齐已签 APPROVED 历史复核测试夹具，证明重复相同数量确认不会新建复核、不会重复签名、不会改写完整签名快照。
- 2026-09-13：调用 `project-experience-consolidation` 技能做收尾前经验检查；本轮 Maven `-Dtest` 参数引号问题已被 `docs/powershell-preflight-lessons.md#2026-08-23 Maven 回归参数、内存与证据基线门禁` 覆盖，未新增长期经验文档。
- 2026-09-13：静态核对发现新增错误码 `1_040_760_384` 与 `PRO_PROCESS_POOL_ACTIVE_ORDER_VERSION_UPGRADE_CONFIRM_REQUIRED` 冲突，已改为当前段空位 `1_040_760_330` 并复查唯一性。
- 2026-09-13：`python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260913-edhr-static-003-reopened-signature-backfill/execution-log.md` -> PASS。
- 2026-09-13：`python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260913-edhr-static-003-reopened-signature-backfill --mode preview --worktree-closeout off --json` -> PASS；keep=task.md/execution-log.md/verification-report.md，delete=[]，blocked=[]，warnings=[]。
- 2026-09-13：`python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260913-edhr-static-003-reopened-signature-backfill --mode apply --worktree-closeout off --json` -> PASS；deleted_paths=[]。按当前任务范围未执行 worktree 合并/删除或 Git 提交/推送。
- 2026-09-13：用户明确回复“授权”，允许提交并推送本任务改动；当前 Codex worktree 为 detached HEAD，提交后推送到远端任务分支 `codex/20260913-edhr-static-003-reopened-signature-backfill`。
- 2026-09-13：detached HEAD 下直接 `git commit` 被 `branch-runtime-port-guard.ps1` 阻断；改到 `D:\IntRuoyiWorktree\20260913-edhr-static-003-reopened-signature-backfill` 的真实任务分支提交，未使用 `--no-verify`。
- 2026-09-13：任务分支 commit hook 首次因端口登记缺失阻断；按项目脚本执行 `scripts\runtime\reserve-worktree-slot.ps1 -Name 20260913-edhr-static-003-reopened-signature-backfill -Path D:\IntRuoyiWorktree\20260913-edhr-static-003-reopened-signature-backfill -Branch codex/20260913-edhr-static-003-reopened-signature-backfill -Profile int_main -AsJson` -> PASS，登记 slot=48，frontendPort=8263，backendPort=48263，未启动服务。
- 2026-09-13：实现提交 `06de7883e4610f9d9729ad9eb22c53f6df327310`，message=`fix: backfill EDHR reopened review signatures`，文件清单：`MesProcessPoolReportAllocationMapper.java`、`ErrorCodeConstants.java`、`MesReportAllocationCommandService.java`、`MesReportAllocationCommandServiceTest.java`。
- 2026-09-13：用户追问“融合进int_main了吗”后继续回复“授权”，允许执行 `int_main` 融合与推送。
- 2026-09-13：`git merge-base --is-ancestor 88effd0a77a8464bd11fa6da986f57aaf421000a int_main` / `origin/int_main` -> no，确认授权前尚未融合进主干。
- 2026-09-13：`E:\IntRuoyi` 主干存在 3 个 DCC 收尾文档脏改动；按收尾规则先提交为 `1b25f3df6`，message=`docs: complete DCC finalization retry closeout`。
- 2026-09-13：执行 `git merge --no-ff codex/20260913-edhr-static-003-reopened-signature-backfill -m "merge: integrate EDHR static 003 signature backfill"`，遇到 4 个预期冲突文件：`MesProcessPoolReportAllocationMapper.java`、`ErrorCodeConstants.java`、`MesReportAllocationCommandService.java`、`MesReportAllocationCommandServiceTest.java`。
- 2026-09-13：冲突解决策略：保留 `int_main` 现有初始分配与发布锁回归，合入 EDHR-STATIC-003 复核签名补齐逻辑；错误码使用 `1_040_760_330`，避免与 `PRO_PROCESS_POOL_ACTIVE_ORDER_VERSION_UPGRADE_CONFIRM_REQUIRED=1_040_760_384` 冲突。
- 2026-09-13：`mvn -pl yudao-module-mes -am "-Dtest=cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesReportAllocationCommandServiceTest" test` -> FAIL，expected reason：`-am` 上游模块无匹配测试触发 Surefire fail-if-no-specified-tests 门禁；按项目 PowerShell/Maven 规则补 `"-Dsurefire.failIfNoSpecifiedTests=false"` 后重跑。
- 2026-09-13：`mvn -pl yudao-module-mes -am "-Dtest=cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesReportAllocationCommandServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，28 tests / 0 failures / 0 errors / 0 skipped。
- 2026-09-13：`mvn -pl yudao-module-mes -am "-Dtest=MesReportAllocationCommandServiceTest,MesTeamLeaderBatchRecordBackfillServiceTest,MesTeamLeaderTraceServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，47 tests / 0 failures / 0 errors / 0 skipped。
- 2026-09-13：主干融合后同步修正 `mes-edhr-static-findings-fix-static.spec.cjs` 对 `reviewEvidenceRequirement` / `requireReview(event, command, reviewToBackfill)` 的静态合同断言。
- 2026-09-13：`node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-findings-fix-static.spec.cjs` -> PASS，eDHR static findings fix contract。
- 2026-09-13：融合后 `git diff --check` -> PASS，仅有 LF/CRLF 工作区提示，无 whitespace error。
- 2026-09-13：融合后 `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260913-edhr-static-003-reopened-signature-backfill/execution-log.md` -> PASS。
- 2026-09-13：融合后 `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260913-edhr-static-003-reopened-signature-backfill --mode preview --worktree-closeout off --json` -> READY，keep=3，delete=[]，blocked=[]，warnings=[]。
- 2026-09-13：融合后 `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260913-edhr-static-003-reopened-signature-backfill --mode apply --worktree-closeout off --json` -> APPLIED，deleted_paths=[]。

## Bug Regression Evidence

### Bug

EDHR-STATIC-003 重开路径中，旧 CURRENT 分配已有 `reviewId`，但对应 APPROVED 复核缺少 `reviewSignatureId` / `reviewSignatureUserId` / `reviewSignatureSnapshotJson` 时，保持原分配数量重新确认没有补齐正式复核签名证据。

### Expected

生产组长必须提供本人电子签名密码；系统补齐同一个 APPROVED 复核的签名证据，保持原分配 `reviewId` 不变，并让完工读取链路可读到同一复核签名证据。

### Reproduction

- RED: `mvn -pl yudao-module-mes "-Dtest=cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesReportAllocationCommandServiceTest" test` -> FAIL, 22 tests / 5 errors；expected reason：延续实现草稿未完整满足正式复核签名证据合同，旧空复核夹具和数量不变分支触发 `PRO_PROCESS_POOL_SUBMISSION_REVIEW_SIGNATURE_REQUIRED` / version conflict。

### Root Cause

`save` 的数量不变分支只做幂等快照和完工回算，没有检查当前分配关联的 APPROVED 复核是否具备正式签名字段；同时既有 `requireReview` 允许复用不完整复核，导致下游批记录/追溯读取无法稳定拿到复核签名证据。

### Verification

- GREEN: `mvn -pl yudao-module-mes "-Dtest=cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesReportAllocationCommandServiceTest" test` -> PASS, 22 tests / 0 failures / 0 errors / 0 skipped。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesReportAllocationCommandServiceTest,MesTeamLeaderBatchRecordBackfillServiceTest,MesTeamLeaderTraceServiceTest" test` -> PASS, 41 tests / 0 failures / 0 errors / 0 skipped。
- GREEN: `git diff --check` -> PASS；仅输出 Git LF/CRLF 工作区提示，无 whitespace error。
- GREEN: `rg -n "1_040_760_330|1_040_760_384|PRO_PROCESS_POOL_SUBMISSION_REVIEW_SIGNATURE_REQUIRED|PRO_PROCESS_POOL_ACTIVE_ORDER_VERSION_UPGRADE_CONFIRM_REQUIRED" IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/enums/ErrorCodeConstants.java` -> PASS；新增签名必填错误码为 `1_040_760_330`，原版本升级确认错误码保持 `1_040_760_384`。
- GREEN: 错误码修复后重跑 `mvn -pl yudao-module-mes "-Dtest=MesReportAllocationCommandServiceTest,MesTeamLeaderBatchRecordBackfillServiceTest,MesTeamLeaderTraceServiceTest" test` -> PASS, 41 tests / 0 failures / 0 errors / 0 skipped，BUILD SUCCESS。
- GREEN: 融合进 `int_main` 后重跑 `mvn -pl yudao-module-mes -am "-Dtest=MesReportAllocationCommandServiceTest,MesTeamLeaderBatchRecordBackfillServiceTest,MesTeamLeaderTraceServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 47 tests / 0 failures / 0 errors / 0 skipped，BUILD SUCCESS。
- GREEN: 融合进 `int_main` 后重跑 `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-findings-fix-static.spec.cjs` -> PASS。
- GREEN: 融合后重跑 `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-findings-fix-static.spec.cjs` -> PASS；静态合同已更新为当前 `reviewEvidenceRequirement`/三参 `requireReview` 实现边界。

### Blockers

无产品代码阻塞。按当前任务范围未执行 E2E、未启动服务、未写数据库；Git 提交/推送已由用户在 2026-09-13 单独授权。
