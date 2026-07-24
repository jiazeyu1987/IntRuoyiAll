# 任务：电子批记录报表视觉保真优化 Round 5

## 任务目标

- 在新的前后端 paired worktree 中接续 Automation `automation-2`。
- 先通过真实前端点击 `清除电子批记录报表`，再点击 `A 直接 doc`，重新生成 Route A Jimu 报表。
- 以源 Word `D:\ProjectPackage\Int\IntRuoyi\resource\批记录模板.doc` 为准，继续优化 Jimu 报表布局、分页节奏、列宽、行高、块结构、页头页脚、留白和跨行跨列关系。
- 所有代码修改必须落在共享规则层，不得按报表标题、工序名或表编号硬编码。

## 旧任务接续说明

- 前一同主题任务：`doc/tasks/20260525-automation-2-ebr-visual-fidelity/`。
- 前一任务状态：Round 4 代码、测试、打包和真实生成验证已完成；自动 cleanup/合并曾因主 worktree 状态阻塞。
- 当前基线：后端 `int_main` 已包含 Round 4 结果；本任务从 `int_main` 新建分支 `task/20260526-automation-2-ebr-visual-fidelity-round5` 接续。
- 影响：旧任务的自动收尾阻塞不影响本轮继续开发，但本轮必须重新生成并重新对比，不能复用旧结论代替验证。

## BDD 场景

- BDD: 清除后重新生成最新报表 -> Given 测试租户存在电子批记录报表真实入口和源 Word 模板 / When 用户通过真实前端点击 `清除电子批记录报表` 后再点击 `A 直接 doc` / Then 系统必须生成最新 Jimu 报表，且不能沿用旧报表或静默跳过失败。
- BDD: 源文档优先对比 -> Given 源 Word 与最新生成的 Jimu 报表 / When 对比页头、页脚、分页节奏、表头层级、明细块结构、列宽比例、行高块高、跨行跨列、空白格、斜线空格、汇总区和清场区 / Then 差异必须按源 Word 为准记录。
- BDD: 通用规则修复 -> Given 某个差异可由页型识别、行类型识别、版式求解、JSON 构建或视觉样式规则解释 / When 子 agent 修改 / Then 必须先有失败测试，再最小实现，且不得包含单报表硬编码。
- BDD: 主 agent 放行 review -> Given 子 agent 返回修改 / When 主 agent 审查通用性、风险和测试证据 / Then 只有通用、风险可控、测试完整且未回退他人改动的修改可以保留。

## 里程碑

- [x] M1：创建新的 paired worktree、任务文档和状态基线。
- [x] M2：由 planner/decomposer 子 agent 生成并通过主 agent 审核 PRD、开发计划和测试计划。
- [x] M3：通过真实前端清除并 `A 直接 doc` 重新生成，采集最新 Jimu 对比对象。
- [x] M4：按源 Word 优先原则列出覆盖十项维度的布局差异。
- [x] M5：将可由通用规则修复的差异拆给子 agent 执行 RED -> GREEN。
- [x] M6：主 agent review 子 agent 结果并只保留通过的修改。
- [x] M7：回归测试、生产打包、真实重新生成与源 Word 再对比。
- [x] M8：task-closeout-cleanup 预览、按当前任务范围提交。

## 预期验证

- RED/GREEN：后端 `yudao-module-mes` 中 DOC 解析、行类型、版式求解、Jimu JSON 构建等定向测试。
- 回归：相邻后端报表测试和必要打包命令。
- 前端：真实 Playwright 路径验证，必须使用测试租户真实点击清除和生成；如果前端代码被修改，补充对应 RED/GREEN。
- 真实生成：清除后 `A 直接 doc` 重新生成 Route A 报表，并记录生成时间、数量、Jimu JSON 结构摘要与源 Word 差异。

## 当前状态

- 状态：completed。
- 已完成：新 paired worktree 创建、任务文档基线创建；planner/decomposer 审核通过；真实前端清除和 `A 直接 doc` 生成 15 份 Route A Jimu 报表；按源 Word 优先输出十项维度差异；Maxwell 子 agent 勾选框规则通过 review；Lagrange 子 agent 源网格分页诊断通过 review；Godel reviewer 阻塞项已修复；权威源列宽、源文档页头、源网格分页/合并、显式源行高、源 `☑` 勾选状态均已完成 RED/GREEN。
- 当前阻塞：无。
- 当前门禁结论：`MesProBatchRecordReportJsonBuilderTest` 61 项回归通过，`yudao-server` 打包通过，真实前端 `清除电子批记录报表` + `A 直接 doc` 重新生成通过；最新源 Word vs Jimu 指标级对比显示 15 张表 rows/cols/width/height/merges、汇总区、清场区、页脚和勾选框均一致；生产代码已清除 reviewer 发现的源文档标题/记录号/版本号专用 dead detector。

## Current Status

Completed - verification passed, cleanup preview is clean, and the remaining repository changes are scoped to this task.

## Cleanup Keep

- `doc/tasks/20260526-automation-2-ebr-visual-fidelity-round5/verification-report.md`
