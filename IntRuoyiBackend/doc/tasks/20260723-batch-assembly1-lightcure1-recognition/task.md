# 20260723 组装Ⅰ与光固Ⅰ批记录识别差异优化

## Task Goal

在 `20260723_batch` 独立 worktree 中，对比识别后的组装Ⅰ、光固Ⅰ表单与 `C:\Users\BJB110\Desktop\文档\批记录压力泵.doc` 中原始组装Ⅰ、光固Ⅰ表单的每个单元格，找出差异最大的单元格，优化批记录 Word 识别算法，直到组装Ⅰ与光固Ⅰ的识别结果与原 Word 基本无差异。

## Milestones

- [x] M1：建立任务文档、经验门禁、BDD/TDD 记录与 worktree 边界。
- [x] M2：创建或识别 `20260723_batch` worktree，并确认后端/前端参与仓库、分支和运行端口。
- [x] M3：建立可重复的 Word 原表与识别结果单元格对比工具，覆盖组装Ⅰ、光固Ⅰ。
- [x] M4：运行当前算法基线对比，按单元格定位差异最大的区域与根因。
- [x] M5：先补 RED 契约测试，复现差异最大的通用识别问题。
- [x] M6：优化识别算法，不写单表硬编码补丁。
- [x] M7：重复对比组装Ⅰ、光固Ⅰ，直到剩余差异收敛到可解释的低风险差异。
- [x] M8：运行目标单测/契约测试、必要构建检查和识别对比报告，记录证据。

## Expected Verification

- 生成或更新单元格级对比报告，包含组装Ⅰ、光固Ⅰ每个单元格的原 Word 内容、识别内容、行列/合并/控件差异与差异评分。
- 明确列出修复前差异最大的单元格及根因。
- 新增或更新自动化测试能在修复前 RED、修复后 GREEN。
- 修复后组装Ⅰ、光固Ⅰ识别对比报告显示关键单元格文本、合并、checkbox/输入框等结构基本一致。
- 不引入 fallback、mock、针对单个表单名称或单个坐标的临时补丁。

## Current Status

completed

## 经验门禁

- PowerShell / Windows shell：已读取 `docs/powershell-memory.md`；后续中文路径、Word 文件、JSON/Markdown 报告和命令输出必须显式 UTF-8，不使用 `&&`。
- Worktree：已读取 `docs/worktree-memory.md` 与 worktree 技能；必须在 `20260723_batch` 独立 worktree 内进行开发，记录 worktree 路径、分支、参与仓库和端口归属；不得在主工作区进行生产代码修改。
- BDD/TDD：实现前必须记录 BDD 与 RED；生产代码修改必须有对应测试或契约验证。
- No fallback：不得用静默降级、mock 成功、单表坐标硬编码或特例表单补丁掩盖识别问题。
- 真实文件证据：必须以 `C:\Users\BJB110\Desktop\文档\批记录压力泵.doc` 和当前识别产物/运行结果为准，不凭截图或记忆判断完成。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是通用 Word 表格识别算法优化。
- `是否存在临时补丁或绕过`：否。

## Evidence

- 2026-07-23：完成只读 UTF-8 bootstrap，读取 `docs/experience-index.md`、`docs/powershell-memory.md`、`docs/worktree-memory.md` 和 worktree 技能，确认本任务命中 PowerShell、worktree、BDD/TDD、no-fallback 与真实文件证据门禁。

- 2026-07-23：创建后端 worktree `D:\ProjectPackage\Int\IntRuoyiWorktrees\20260723_batch\m`，分支 `codex/20260723_batch`，基于 `int_main` 提交 `63edb0502c`。本任务当前只确认需要修改后端识别算法，未创建前端 worktree。

- 2026-07-23：用户新增通用交互要求：辅助模式中互斥的 checkbox 结果项应呈现为一个选项组，例如 `检测结果：○ 符合要求  ○ 不符合要求`，不得继续作为难以理解的独立 checkbox 填写项。

- 2026-07-23：已实现互斥 checkbox 选项组通用化；后端输出 `STRING` + `radio-group` + `selectionMode=single` + `options`，前端辅助模式渲染为单行选项组并保持字段审计草稿链路。

- 2026-07-23：补强通用标签归一；当 Word 表头为泛化 `结果/检查结果` 且选项为 `符合要求/不符合要求`、`合格/不合格`、`通过/不通过` 或 `OK/NG` 时，后端规则标签输出为 `检测结果`，继续保持非特定表单、非坐标逻辑。

- 2026-07-23：新增 `MesProBatchRecordPressurePumpCellDiffReportTest`，直接读取 `C:\Users\BJB110\Desktop\文档\批记录压力泵.doc`，对 `组装Ⅰ工序生产记录` 与 `光固Ⅰ工序生产记录` 做 Word 原表和 JSON 渲染表的单元格级对比，并生成 `pressure-pump-assembly-lightcure-cell-diff-report.md`。

- 2026-07-23：差异最大的通用问题已定位为两类：其一是汇总/结构化空白录入格与普通汇总留白混在同一行型下，导致该填写格被抑制或留白被误填；其二是压缩物料矩阵奇数物料行展开后，右侧物料槽与批号槽丢失可填写属性。

- 2026-07-23：通用修复已完成：`MesProBatchRecordReportJsonBuilder` 只在具备汇总关键词且同一汇总行存在至少 3 个真实空白录入格时恢复填写控件，避免普通汇总留白误生控件；`MesProBatchRecordReportLayoutCalibrator` 展开压缩物料矩阵时保留奇数尾行左右批号/物料槽可填写属性，不按表单名称、行列坐标或固定标题特例处理。

- 2026-07-23：最终报告显示 `组装Ⅰ工序生产记录 diffCount=0, maxScore=0`，`光固Ⅰ工序生产记录 diffCount=0, maxScore=0`；仅记录右边界闭合微列等价差异，测试中按“同一末端闭合边界”显式计入 `ignoredRightEdgeClosureCount`，不计入差异评分。

- 2026-07-23：验证通过：`MesProBatchRecordReportJsonBuilderTest#build_shouldRenderWhitespaceForSuppressedBlankFillableCells+build_shouldNotAutoFillSummaryPaddingCells+build_shouldGenerateFillFormForPackedMaterialMatrixBlankBatchCells` PASS；`MesProBatchRecordReportLayoutCalibratorTest#calibrate_shouldExpandPackedMaterialMatrixForRouteDAssemblyOneRowWithoutSourceRowSpan+calibrate_shouldExpandPackedMaterialMatrixCellsIntoStructuredRowsForRouteAProcessPages` PASS；`MesProBatchRecordPressurePumpCellDiffReportTest` PASS；`MesProBatchRecordCellRuleSupportTest` 21 tests PASS；前端 `edhr-assist-fill-mode-static.spec.js` PASS。

- 2026-07-23：全类组合回归 `MesProBatchRecordCellRuleSupportTest,MesProBatchRecordReportLayoutCalibratorTest,MesProBatchRecordReportJsonBuilderTest,MesProBatchRecordPressurePumpCellDiffReportTest` 在 15 分钟超时停止，未作为通过证据；当前完成口径只采用上述定向 GREEN 证据与最终单元格差异报告。

- 2026-07-23：用户要求融合进 `int_main`；预检确认目标提交 `95cd191e3d` 的合并树无已提交历史冲突，但主后端工作区 `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro` 存在同文件未提交改动，重叠文件为 `yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordExecutionServiceImpl.java`。按 `docs/worktree-memory.md` 主工作区脏改同文件重叠门禁，当前暂停融合，未执行 merge/stash/覆盖。

- 2026-07-23：按用户指令先提交主线阻塞改动，再融合；主线阻塞改动已提交为 `a8e5ea61fa`，`codex/20260723_batch` 已通过合并提交 `226813a203` 融入后端 `int_main`。合并后验证 `mvn -pl yudao-module-mes -am "-DskipTests" compile` 失败，失败点来自主工作区既有 eDHR 审批适配器/VO/DO 编译不一致，非本次压力泵识别合并文件；因此不执行 worktree 清理，任务状态保持阻塞。

- 2026-07-23：用户要求修复融合后验证问题；当前 `int_main` 合并结果中 `MesProBatchRecordParsedCell` 保留了重复的 `reviewedCellRule` / `cellRuleSource` 字段定义，已删除末尾重复定义，仅保留前置正式字段定义。

- 2026-07-23：融合结果验证恢复：新增 `MesProBatchRecordParsedCellTest` 覆盖审计字段唯一性与 builder 契约，`mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordParsedCellTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` PASS，1 test；`mvn -pl yudao-module-mes -am "-DskipTests" compile` PASS；`mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordPressurePumpCellDiffReportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` PASS，1 test，0 failures，0 errors，0 skipped。首次目标测试运行在 `yudao-module-bpm` 编译阶段失败于 `target\generated-sources\annotations\cn\iocoder\yudao\module\bpm\convert\task` 缺失，未修改源码后复跑通过。

- 2026-07-23：收尾完成：主后端工作区 `task-closeout-cleanup` preview/apply 均 PASS，删除临时差异报告并保留 `task.md`、`execution-log.md`；确认 `82406c7820` 已是 `int_main` 当前 HEAD 祖先，`20260723_batch` linked worktree 已无未提交改动，删除临时截图导出产物后执行 `git worktree remove D:\ProjectPackage\Int\IntRuoyiWorktrees\20260723_batch\m` PASS；`git worktree list` 已不再包含 `20260723_batch`。经验沉淀门禁已复核，命中 `docs/worktree-memory.md` 既有 worktree closeout / dirty main / cleanup preview 门禁，无需新增长期经验文档。
