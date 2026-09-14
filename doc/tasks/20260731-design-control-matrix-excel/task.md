# 截图转中文设计控制矩阵 Excel

## Task Goal

将用户提供的“Design Control Matrix”截图按原有表格层级整理为中文 Excel，保留标题、7 列表头、分区、合并单元格、底色、边框、换行与文档编号。

## Milestones

- [x] M1：识别截图结构、可见文字和翻译边界
- [x] M2：生成中文 Excel 工作簿
- [x] M3：检查关键单元格内容、公式错误和视觉版式
- [x] M4：完成任务记录与清理

## Expected Verification

- 工作簿可成功导出为 `.xlsx`。
- 工作表包含 7 列中文表头及“生物学要求”“灭菌要求”两个分区。
- 文档编号保持截图原值，截图不可见的 ISO 标准编号不做猜测补充。
- 关键范围检查无缺项，公式错误扫描无错误。
- 全工作表渲染后无明显截断、重叠或不可读内容。
- `task-closeout-cleanup` preview/apply 通过。
- 按用户明确要求，本任务不执行 Git 基线、提交或推送。

## Applicable Experience Gates

- 未命中现有项目经验索引中的专用 Excel 制作经验。
- 中文任务记录与工作簿内容必须使用 UTF-8 安全链路。
- 用户已明确取消本任务 Git 保存门禁，不执行脏工作区基线、提交或推送。
- 收尾顺序必须为 `ready_for_closeout`、cleanup preview/apply、`completed`。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；直接生成结构化、可编辑的中文工作簿，不以图片或不可编辑对象替代表格。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- outputs/019fb812-d0e3-7f20-8895-31a209f54b2e/设计控制矩阵.xlsx

## Cleanup Candidates

- outputs/019fb812-d0e3-7f20-8895-31a209f54b2e/设计控制矩阵.xlsx.inspect.ndjson
