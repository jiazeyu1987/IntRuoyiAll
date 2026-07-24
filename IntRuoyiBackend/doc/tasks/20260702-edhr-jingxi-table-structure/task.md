# 20260702-edhr-jingxi-table-structure

## 任务目标

在 `edhr_table_jingxi` worktree 中修复 eDHR 批记录 Word 表格识别链路中“右侧多出空单元格、局部列对齐错位”的问题。以真实文件 `C:\Users\BJB110\Desktop\2\2\RE-PP-ID-01（A 1）球囊扩张压力泵生产记录(1).doc` 中的 `精洗工序生产记录` 表格为验收锚点，要求识别输出与 Word 原表在单元格数量、排布、合并关系和比例上完全一致。

## 里程碑

- [x] M1 建立真实 DOC 结构一致性验证：抽取 Word 原始表格作为 expected，抽取当前识别/JSON 输出作为 actual，并输出结构 diff。
- [x] M2 复现 RED：证明当前实现仍存在结构不一致、右侧空列或局部错位问题。
- [x] M3 从通用解析/校准/JSON 构建规则修复根因，不写标题、文件名或单表特判。
- [x] M4 跑 GREEN 与回归测试，确认真实 DOC `精洗工序生产记录` 结构 diff 为 0，且既有右侧空列回归不复发。
- [x] M5 使用 worktree 专属真实前端入口导入/预览该 DOC 并截图验证；同时记录旧存量模板仍保留历史 26 列布局，不作为新链路通过证据。
- [x] M6 任务收口：更新文档、提交仅本任务相关文件。

## 预期验证

- `mvn -pl yudao-module-mes '-Dtest=MesProBatchRecordJingxiTableStructureVerificationTest' test`
- `mvn -pl yudao-module-mes '-Dtest=MesProBatchRecordJingxiTableStructureVerificationTest,MesProBatchRecordReportJsonBuilderTest,MesProBatchRecordCodexCliImageParserTest' test`
- 如运行态具备：Playwright 真实页面导入/预览验收锚点 DOC。

## 当前状态

- 状态：后端真实 DOC 结构 diff、回归测试、worktree 专属运行态、真实页面上传/预览 E2E 均已完成；新上传批记录预览无右侧多余空列。
- worktree：`D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_table_jingxi`
- 后端分支：`codex/edhr_table_jingxi`
- 前端分支：`codex/edhr_table_jingxi`
- FE 端口：`8143`，已由标准 worktree 端口台账登记并完成 HTTP 200 运行态验证。
- BE 端口：`48143`，已由标准 worktree 端口台账登记并完成 HTTP 200 运行态验证。
- DB/Redis/File：当前 worktree 前后端已启动并通过真实页面 E2E；Playwright 使用前端仓 `node_modules` 中 1.60.0 与系统 Chrome。

## 经验门禁

- PowerShell / Windows shell：已读取 `docs/powershell-memory.md`；中文路径、here-string、管道、文件读写均使用显式 UTF-8。
- worktree / 分支 / 清理：已读取 `docs/worktree-memory.md`；开发在 `D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_table_jingxi` 隔离 worktree 内完成。
- 真实 E2E：执行前必须先读取 `docs/login-access.md`、登记 FE/BE 运行态、启动目标 worktree 前后端并记录 `experience-preflight`；已启动并验证当前 worktree FE `8143` / BE `48143` 运行态；真实页面 E2E 使用测试租户 `aoteman` 登录，不复用默认 `8081/48081`。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；修复范围为 Word visual grid / segment column widths / preserveSourceGrid / JSON fixed width / row height rhythm 的通用规则。
- 是否存在临时补丁或绕过：否；未按文件名、表名或 `精洗工序生产记录` 单表硬编码生产逻辑。

## 前一任务检查

- 本 worktree 为隔离任务分支；未继承未完成任务文档作为当前任务状态。
- 主工作区前后端创建 worktree 前均为干净状态；仅存在 detached release worktree，不作为本任务开发环境。

## 完成内容

- 新增真实 DOC 结构一致性测试 `MesProBatchRecordJingxiTableStructureVerificationTest`，从 Word 原表抽取 `精洗工序生产记录` 的行列、合并、宽度和行高结构作为 ground truth。
- 修复 `MesProBatchRecordDocParser`，按 Word 视觉列宽继承和分段宽度解析构建 parsed table，避免分段表右侧被推断出不存在的空列。
- 扩展 `MesProBatchRecordParsedTable.preserveSourceGrid`，让校准和 JSON 构建能够区分“必须保留 Word 源网格”的表格。
- 修复 `MesProBatchRecordReportLayoutCalibrator`，在源网格保真表中跳过会改写列网格的语义重排，同时对宽矩阵工艺页保留横向预算。
- 修复 `MesProBatchRecordReportJsonBuilder`，固定源列宽、跳过溢出的空白视觉格、保护源行高节奏，并对低列数完整工艺页保留最低视觉高度。
- 清理临时诊断测试文件 `MesProBatchRecordDebugProbeTest`、`MesProBatchRecordHeightDebugProbeTest`，未保留调试产物。

## 验证结果

- GREEN: `mvn -pl yudao-module-mes '-Dtest=MesProBatchRecordJingxiTableStructureVerificationTest,MesProBatchRecordReportJsonBuilderTest#build_shouldPreferSourceRowHeightsForFixedRouteANonPagedProcessRows+build_routeAT13_shouldUseLandscapeA4ForFixedWideTemplate+build_shouldNotOverCompressFixedRouteADetectionPageIntoLargeBottomWhitespace+build_shouldFollowSourceHeightForLiveLikeMediumProcessPages+build_shouldPreferSourceRowHeightsForDenseRowsWithVisualBlanksAndShortWrappedCells' test` -> PASS, 6 tests, 0 failures, finished 2026-07-03T02:40:54+08:00.
- REGRESSION: `mvn -pl yudao-module-mes '-Dtest=MesProBatchRecordJingxiTableStructureVerificationTest,MesProBatchRecordReportJsonBuilderTest,MesProBatchRecordCodexCliImageParserTest' test` -> PASS, 81 tests, 0 failures, finished 2026-07-03T02:44:53+08:00.

## 页面 E2E 结果

- RED / 存量数据定位：读取已有批记录 EDHR-TABLE-VERIFY-20260630-1 的 精洗工序生产记录，页面 DOM 检测仍为 26 列，右侧发现 20 个可疑窄空白单元格；证据文件：artifacts/edhr-jingxi-template-preview-e2e-report.json。该证据用于证明旧模板数据仍携带历史布局，不能作为新链路验收通过证据。
- GREEN / 当前生成链路：通过页面 导入 Word 上传真实 DOC，生成新批记录 EDHR-JINGXI-E2E-20260703035210，再选择 精洗工序生产记录 预览；页面 DOM 检测为 19 列，右侧可疑窄空白单元格 0 个，截图：artifacts/edhr-jingxi-template-preview-upload-e2e.png，报告：artifacts/edhr-jingxi-template-preview-upload-e2e-report.json。

## 未完成 / 阻塞

- 无当前代码链路阻塞。注意：旧批记录 EDHR-TABLE-VERIFY-20260630-1 是修复前生成的存量模板，仍保留历史 26 列布局；若业务需要旧模板同步修复，需要另起数据迁移/重生成任务，不应混入本次解析链路修复提交。
## 合并结果

- 后端：codex/edhr_table_jingxi 已融合进 int_main，merge commit 2214893e2e。
- 前端：任务分支相对 int_main 无新增提交，本次融合 no-op。
- 合并后验证：int_main 上关键后端回归 PASS，6 tests / 0 failures，完成于 2026-07-03T08:22:40+08:00。