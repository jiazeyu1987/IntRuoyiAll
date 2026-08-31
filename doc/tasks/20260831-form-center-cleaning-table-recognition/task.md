# 表单中心清洗工序 Word 识别优化

## Task Goal

在独立 worktree 中优化表单中心的通用 Word 表格识别方案，使整份按压式球囊扩张压力泵生产记录中的“清洗工序生产记录”能够被独立选中并生成与用户提供的三张原始截图一致的表格结构；禁止按文件名、产品名或“清洗工序”名称硬编码。

## Milestones

- [x] M1：建立真实 DOCX 回归样本与当前错误行为的 RED 证据。
- [x] M2：实现通用的多表格候选识别、同表多工序切分和目标候选选择。
- [x] M3：保留清洗工序的视觉网格、纵横合并、行列尺寸、边框与斜线格，并生成可填写单元格规则。
- [x] M4：完成定向测试、相邻回归和结构验收，形成验证报告。
- [x] M5：执行任务收尾预检，保留正式任务记录并说明 worktree 状态。
- [ ] M6：将已验证实现安全融合到当前 `int_main`，保留主工作区并行改动并完成融合后回归。

## Expected Verification

- 真实文件中的清洗工序被识别为独立候选，不再固定使用文档第一张表。
- 清洗工序结构包含标题、生产前检查、设备信息、8 个物料清洗记录块、生产自检、批量汇总和生产后清场记录。
- 输出保持 45 列基础网格，并保留关键横向合并、纵向合并、列宽、行高和无效填写格斜线。
- 重复表头不按文字去重，空白实际值、数量、人员和日期单元格生成独立填写规则。
- 源文档“螺纹块”区域的错误标题保持可追溯，不在识别器中按工序名静默纠正。
- 受影响 BPM 模块定向测试和相邻表单中心识别回归通过。

## Current Status

ready_for_closeout

用户已确认识别结果正确，正在执行任务分支提交、`int_main` 融合和融合后验证。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；多候选无法唯一匹配时必须明确失败，不继续静默取第一张表。
- `是否从根因和长期维护角度解决`：是；按 Word 结构识别逻辑表单边界并保留原始视觉网格，不绑定单个文件或工序名称。
- `是否存在临时补丁或绕过`：否；真实文件只作为回归样本和验收证据。

## 适用经验门禁

- `docs/backend-development.md#eDHR-批记录-Word-表格解析门禁`：真实源 DOCX 与最小合成表格必须同时覆盖；先定位共享 parser/builder 规则，禁止用表单名、工序名、文件名或压力泵模板名硬编码。
- `docs/backend-development.md#Form-Center-导入必须同时持久化源表格布局`：识别结果必须保存完整 `sheetLayoutJson` 与 `cellRules`，不得用纵向字段列表替代源表格结构。
- `docs/backend-development.md#过程检验源-Word-必须先命中-profile-再拆输入框`：目标逻辑表格的选择与归一化必须基于通用结构 profile；不得绕过候选过滤或静默进入错误表格。
- `docs/backend-development.md#Jimu-fillForm-组件类型语义优先边界`：签名、日期、普通输入和叙述格应按单元格语义生成，正式化阶段不得降级重算。
- 多候选选择必须 fail fast：无法根据正式导入上下文唯一匹配时返回可理解的识别失败，不继续固定选择第一张表。

## Cleanup Keep

- doc/tasks/20260831-form-center-cleaning-table-recognition/task.md
- doc/tasks/20260831-form-center-cleaning-table-recognition/execution-log.md
- doc/tasks/20260831-form-center-cleaning-table-recognition/verification-report.md
- doc/tasks/20260831-form-center-cleaning-table-recognition/cleaning-recognition-result-01-top.png
- doc/tasks/20260831-form-center-cleaning-table-recognition/cleaning-recognition-result-02-middle-a.png
- doc/tasks/20260831-form-center-cleaning-table-recognition/cleaning-recognition-result-03-middle-b.png
- doc/tasks/20260831-form-center-cleaning-table-recognition/cleaning-recognition-result-04-bottom.png
