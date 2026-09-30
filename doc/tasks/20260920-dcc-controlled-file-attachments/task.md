# DCC Controlled File Attachments

## Goal

为 DCC 受控文件提交链路新增普通附件集合：除可编辑源文件和不可编辑/图纸 PDF 文件外，用户可上传若干个附件；查看受控文件时可以查看这些附件。

## Milestones

1. 梳理现有 DCC 上传、提交、详情/浏览、文件存储和预览下载边界。
2. 设计附件持久化与接口合同，避免与源文件、图纸 PDF、关联受控文件混淆。
3. 以 RED/GREEN 补后端与前端静态/单元合同。
4. 验证提交载荷、详情回显和查看入口。

## Expected Verification

- 后端定向测试覆盖：提交时绑定多个附件；详情/版本响应返回附件列表；源文件/图纸 PDF 与普通附件角色隔离。
- 前端静态/单元合同覆盖：上传页可选择多个普通附件并提交附件票据；查看页展示附件并提供查看入口。
- `git diff --check`。
- 用户明确要求时，使用 Playwright 通过真实前端页面、真实账号和任务自有数据验证附件上传与查看闭环。

## Current Status

completed

## Design Constraints Check

- 已读取 `docs/backend-development.md`、`docs/frontend-development.md`、`docs/database-rules.md`、`docs/task-closeout-rules.md`。
- 不能把普通附件混入 `sourceUploadTicket`、`drawingPdfUploadTicket` 或 `relatedControlledFileIds`。
- 不能用历史源文件、图纸 PDF 或关联文件自动补齐附件。
- 附件必须是显式上传和显式返回的独立集合。
- 不执行 E2E，除非用户当轮明确要求。
