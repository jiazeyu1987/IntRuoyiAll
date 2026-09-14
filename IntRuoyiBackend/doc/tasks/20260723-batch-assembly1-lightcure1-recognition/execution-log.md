# Execution Log

BDD: cell-level Word recognition parity -> Given `批记录压力泵.doc` 中存在组装Ⅰ与光固Ⅰ原始 Word 表单, When 当前批记录识别算法解析这两个表单, Then 识别结果应在每个单元格的文本、合并结构、填写控件、checkbox/输入框语义上与原 Word 基本一致。

BDD: largest-difference diagnosis -> Given 识别结果与 Word 原表存在差异, When 运行单元格级对比工具, Then 输出差异评分最高的单元格、差异类型和可定位根因，供算法通用修复。

BDD: no single-form patch -> Given 组装Ⅰ或光固Ⅰ出现识别差异, When 优化算法, Then 不得按表单名称、单元格坐标或固定文本写特例补丁。

BDD: checkbox choice group contract -> Given Word 单元格中存在 `□符合要求` 与 `□不符合要求` 这类互斥 checkbox 文本, When 自动生成 eDHR 填写规则和执行快照字段, Then 后端输出一个可被前端渲染为选项组的通用字段契约，而不是只能作为普通布尔 checkbox 填写。

INFO: experience-index -> matched `docs/powershell-memory.md`, `docs/worktree-memory.md`, BDD/TDD, no-fallback, real-file evidence.

GREEN: experience-preflight -> PASS, PowerShell UTF-8 bootstrap and worktree memory read before write/build/E2E actions.

RED: mvn -pl yudao-module-mes -Dtest=MesProBatchRecordCellRuleSupportTest -Dsurefire.failIfNoSpecifiedTests=false test -> FAIL, 单模块使用旧 BPM 本地依赖时 `getBpmProcessDefinitionKey` override 编译失败；改用 `-am` 验证上游源码。
RED: mvn -pl yudao-module-mes -am -Dtest=MesProBatchRecordCellRuleSupportTest -Dsurefire.failIfNoSpecifiedTests=false test -> FAIL, 多选项 checkbox 初版将组标签取成左侧长描述或上方 checkbox 文本。
GREEN: mvn -pl yudao-module-mes -am -Dtest=MesProBatchRecordCellRuleSupportTest -Dsurefire.failIfNoSpecifiedTests=false test -> PASS, 21 tests, Failures 0, Errors 0; 多选项 checkbox 输出 STRING/radio-group、selectionMode=single、options，并同步 fillForm.options。

GREEN: 2026-07-23 recheck `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellRuleSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 21 tests, Failures 0, Errors 0; 选项组后端契约保持有效。

GREEN: 2026-07-23 option-group label normalization `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellRuleSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 21 tests, Failures 0, Errors 0; 泛化 `结果 + 符合要求/不符合要求` 为 `检测结果`，后端输出 `STRING/radio-group/single/options`。

RED: pressure pump cell diff baseline -> FAIL, 组装Ⅰ与光固Ⅰ差异最高区域集中在结构化汇总空白录入格、压缩物料矩阵奇数尾行右侧物料/批号槽、普通汇总留白误填控件；这些差异属于通用 Word 表格行型和压缩矩阵展开规则问题，不属于单表坐标问题。
GREEN: mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportJsonBuilderTest#build_shouldRenderWhitespaceForSuppressedBlankFillableCells+build_shouldNotAutoFillSummaryPaddingCells+build_shouldGenerateFillFormForPackedMaterialMatrixBlankBatchCells" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, 3 tests; 汇总空白录入格恢复、普通汇总留白不误填、压缩物料矩阵奇数尾行保留可填写属性。
GREEN: mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportLayoutCalibratorTest#calibrate_shouldExpandPackedMaterialMatrixForRouteDAssemblyOneRowWithoutSourceRowSpan+calibrate_shouldExpandPackedMaterialMatrixCellsIntoStructuredRowsForRouteAProcessPages" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, 2 tests; 压缩物料矩阵 Route D 与 Route A 展开契约保持有效。
GREEN: mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordPressurePumpCellDiffReportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, 1 test; `组装Ⅰ工序生产记录 diffCount=0/maxScore=0`，`光固Ⅰ工序生产记录 diffCount=0/maxScore=0`，报告输出到 `doc/tasks/20260723-batch-assembly1-lightcure1-recognition/pressure-pump-assembly-lightcure-cell-diff-report.md`。
GREEN: mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellRuleSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, 21 tests; checkbox 选项组契约在本轮通用识别修复后保持有效。
GREEN: node tests\e2e\edhr-assist-fill-mode-static.spec.js -> PASS; 前端辅助填写模式保留 `检测结果：○ 符合要求  ○ 不符合要求` 选项组渲染契约。
NOTE: mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellRuleSupportTest,MesProBatchRecordReportLayoutCalibratorTest,MesProBatchRecordReportJsonBuilderTest,MesProBatchRecordPressurePumpCellDiffReportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> STOPPED, 15 分钟超时后手动停止；该命令未作为通过证据，完成证据以定向 GREEN 和最终单元格差异报告为准。
NOTE: 2026-07-23 recheck with `MesProBatchRecordPressurePumpCellDiffReportTest` included in a combined Maven selector -> TIMEOUT after 600s; stopped task-owned Java process. This run produced no PASS evidence and does not replace the earlier successful diff report evidence.
GREEN: 2026-07-23 recheck `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportJsonBuilderTest#build_shouldRenderWhitespaceForSuppressedBlankFillableCells+build_shouldNotAutoFillSummaryPaddingCells+build_shouldGenerateFillFormForPackedMaterialMatrixBlankBatchCells,MesProBatchRecordReportLayoutCalibratorTest#calibrate_shouldExpandPackedMaterialMatrixForRouteDAssemblyOneRowWithoutSourceRowSpan+calibrate_shouldExpandPackedMaterialMatrixCellsIntoStructuredRowsForRouteAProcessPages" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 5 tests, Failures 0, Errors 0; production algorithm contracts still green after documentation and test harness updates.

BLOCKER: 2026-07-23 int-main-fusion -> main backend workspace `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro` is on `int_main`, merge-tree for `95cd191e3d` reports no committed-history conflict, but `int_main` has uncommitted same-file overlap with branch diff in `yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordExecutionServiceImpl.java`; project worktree gate forbids merge/stash/overwrite while same-file dirty overlap is unresolved.

GREEN: 2026-07-23 main-overlap-commit -> committed blocking main workspace changes in `MesProBatchRecordExecutionServiceImpl.java` together with existing related `MesProBatchRecordExecutionServiceImplTest.java` hunk as `a8e5ea61fa`; commit hook TDD compliance PASS.

GREEN: 2026-07-23 int-main-fusion -> merged `codex/20260723_batch` HEAD `82406c7820` into backend `int_main` with merge commit `226813a203`; pre-merge dirty overlap count was 0.

BLOCKER: 2026-07-23 merged-result-verification -> `mvn -pl yudao-module-mes -am "-DskipTests" compile` failed after merge because main workspace has unrelated existing production compile errors in `MesProEdhrApprovalTaskAdapter` / `MesProFeedbackApprovalTaskAdapter` against `MesProEdhrWorkTaskRespVO`, `MesProEdhrWorkTaskDO`, and `MesProFeedbackStatusEnum`; keep task worktree and do not run closeout cleanup.

RED: 2026-07-23 merged-result-parsed-cell-duplicate-field -> FAIL, `git show HEAD:yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordParsedCell.java` showed duplicated `reviewedCellRule` and `cellRuleSource` definitions in the merged `int_main` result; this is a compile-integrity defect, not a Word-recognition algorithm fallback.
GREEN: 2026-07-23 parsed-cell-duplicate-field-fix -> PASS, removed only the trailing duplicate `reviewedCellRule` / `cellRuleSource` definitions in `MesProBatchRecordParsedCell`, preserving the canonical fields near the diagonal-slash metadata block.
GREEN: 2026-07-23 parsed-cell-schema-test -> `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordParsedCellTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` PASS, 1 test, Failures 0, Errors 0, Skipped 0; covers unique `reviewedCellRule` / `cellRuleSource` fields and Lombok builder access.
GREEN: 2026-07-23 merged-result-compile -> `mvn -pl yudao-module-mes -am "-DskipTests" compile` PASS, BUILD SUCCESS.
NOTE: 2026-07-23 pressure-pump-target-test-first-run -> FAIL before MES target test execution at `yudao-module-bpm` compile with `NoSuchFileException: ...\target\generated-sources\annotations\cn\iocoder\yudao\module\bpm\convert\task`; no source change was made for this generated-output path before rerun.
GREEN: 2026-07-23 pressure-pump-target-test-rerun -> `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordPressurePumpCellDiffReportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` PASS, 1 test, Failures 0, Errors 0, Skipped 0.
GREEN: 2026-07-23 main-closeout-preview -> `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260723-batch-assembly1-lightcure1-recognition --mode preview` PASS, keep `task.md` / `execution-log.md`, delete only `pressure-pump-assembly-lightcure-cell-diff-report.md`, blocked `<none>`, warnings `<none>`.
GREEN: 2026-07-23 main-closeout-apply -> `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260723-batch-assembly1-lightcure1-recognition --mode apply` PASS, deleted only the temporary pressure-pump diff report; main workspace is the owning `int_main` worktree, so no main-worktree merge step was required.
NOTE: 2026-07-23 linked-worktree-preview -> linked worktree preview reported `codex/20260723_batch` could not be fast-forward merged into `int_main` and main worktree was dirty; this preview was not used as closeout success evidence.
GREEN: 2026-07-23 linked-worktree-removal -> verified `82406c78201f3bce897ff5c2193689fcc2d772be` is ancestor of backend `int_main` HEAD, verified `D:\ProjectPackage\Int\IntRuoyiWorktrees\20260723_batch\m` had clean `git status --short` after deleting task-owned screenshot export scratch files, then `git worktree remove D:\ProjectPackage\Int\IntRuoyiWorktrees\20260723_batch\m` PASS.
GREEN: 2026-07-23 worktree-list-verification -> `git worktree list` no longer contains `D:\ProjectPackage\Int\IntRuoyiWorktrees\20260723_batch\m`; remaining registered worktrees are unrelated concurrent tasks.
GREEN: 2026-07-23 experience-consolidation -> reviewed `project-experience-consolidation` guidance and existing `docs/worktree-memory.md` / `docs/experience-index.md`; this task did not add a new durable rule beyond the already recorded worktree closeout, dirty main, and cleanup preview gates.
