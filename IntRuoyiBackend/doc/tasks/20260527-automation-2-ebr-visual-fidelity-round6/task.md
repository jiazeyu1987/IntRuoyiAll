# 任务：电子批记录报表视觉保真优化 Round 6

## 任务目标

- 在新的后端、前端成对 worktree 中接续 Automation `automation-2`。
- 通过真实前端先点击 `清除电子批记录报表`，再点击 `A 直接 doc`，重新生成最新 Route A Jimu 报表。
- 以源 Word `D:\ProjectPackage\Int\IntRuoyi\resource\批记录模板.doc` 为准，继续对比并优化 Jimu 报表在布局、分页节奏、列宽、行高、块结构、页头页脚、留白、跨行跨列关系上的视觉保真度。
- 仅允许采用共享规则层修改，例如页型识别、行类型识别、版式求解、JSON 构建、视觉渲染或样式规则；不得按某个报表标题、工序名或表编号硬编码。

## Worktree

- 后端：`D:\ProjectPackage\Int\IntRuoyi\worktrees\20260527-automation-2-ebr-visual-fidelity-round6\ruoyi-vue-pro`
- 前端：`D:\ProjectPackage\Int\IntRuoyi\worktrees\20260527-automation-2-ebr-visual-fidelity-round6\yudao-ui-admin-vue3`
- 分支：`codex/20260527-automation-2-ebr-visual-fidelity-round6`
- 本地端口：前端 `8109`，后端 `48109`

## 前序任务检查

- 前一同主题后端任务：`doc/tasks/20260526-automation-2-ebr-visual-fidelity-round5/`。
- 前一任务状态：`completed`。
- 前一任务结论：Round 5 已在 `int_main` 合入源列宽、源页头、源网格分页、显式源行高与勾选框通用规则；指标级对比 rows/cols/width/height/merges 已一致。
- 本轮接续原则：不得复用上一轮对比结论替代验证；必须重新清除、重新生成、重新对比。
- 自动化记忆：`$CODEX_HOME/automations/automation-2/memory.md` 在当前进程未解析到有效文件，且 `C:\Users\BJB110\.codex\automations\automation-2\memory.md` 与项目 `.codex` 路径均不存在。影响：不能读取自动化持久 memory；本轮以用户交接内容、前序任务文档、源 Word 和真实重新生成结果作为权威输入。

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
- 回归：`MesProBatchRecordReportJsonBuilderTest`、相邻 batch record report 测试和必要的 `yudao-server` 打包命令。
- 前端：真实 Playwright 路径验证，必须使用测试租户真实点击清除和生成；如果前端代码被修改，补充对应 RED/GREEN。
- 真实生成：清除后 `A 直接 doc` 重新生成 Route A 报表，并记录生成时间、数量、Jimu JSON 结构摘要与源 Word 差异。
- 对比维度必须覆盖：页头、页脚、分页节奏、表头层级、明细块结构、列宽比例、行高与块高、跨行/跨列合并、空白列/空白格/斜线空格、汇总区和清场区。

## 当前状态

- 状态：completed。
- 当前阶段：已完成 JSON builder 通用规则修复、回归、打包、真实前端重新生成、最终源优先十项维度对比和 cleanup preview/apply；等待 scoped commit。
- 当前阻塞：无；自动化 memory 文件缺失已记录，不能作为本轮对比证据来源。
- 当前对比对象：源 Word `D:\ProjectPackage\Int\IntRuoyi\resource\批记录模板.doc` vs 2026-05-27 19:45:52 真实重新生成的 15 张 Route A Jimu 报表。
- 当前差异：无。最终对比中页头、页脚、分页节奏、表头层级、明细块结构、列宽比例、行高块高、跨行/跨列合并、空白格/斜线空格、汇总区和清场区均 PASS。

## Current Status

completed

## Cleanup Keep

- `doc/tasks/20260527-automation-2-ebr-visual-fidelity-round6/request-analysis.md`
- `doc/tasks/20260527-automation-2-ebr-visual-fidelity-round6/prd.md`
- `doc/tasks/20260527-automation-2-ebr-visual-fidelity-round6/dev-plan.md`
- `doc/tasks/20260527-automation-2-ebr-visual-fidelity-round6/test-plan.md`
- `doc/tasks/20260527-automation-2-ebr-visual-fidelity-round6/task-state.json`
- `doc/tasks/20260527-automation-2-ebr-visual-fidelity-round6/execution-log.md`
- `doc/tasks/20260527-automation-2-ebr-visual-fidelity-round6/test-report.md`
- `doc/tasks/20260527-automation-2-ebr-visual-fidelity-round6/verification-report.md`
- `doc/tasks/20260527-automation-2-ebr-visual-fidelity-round6/task.md`
