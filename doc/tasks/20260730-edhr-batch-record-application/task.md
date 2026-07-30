# EDHR 批记录电子化系统立项书文档生成

## Task Goal

为用户生成《批记录电子化系统》毕业实践项目立项申请 Word 文档，内容基于当前 eDHR 批记录系统业务场景，控制在 1000 字以内，便于直接提交。

## Milestones

- [x] 创建任务目录并读取项目任务、编码、经验门禁
- [x] 生成 Word 文档到 `output/doc/`
- [x] 验证文档可读取、中文无乱码、核心字段完整
- [x] 更新任务状态与验证记录

## Expected Verification

- 使用 `python-docx` 重新读取生成的 `.docx` 文本。
- 验证项目名称、负责人、组员、业务痛点、项目目标、解决方案、实施计划、预期成果与价值均存在。
- 记录文档输出路径和验证结果。

## Applicable Gates

### PowerShell / 中文编码门禁

- Trigger: PowerShell 中生成或读取中文文档。
- Preflight check: 使用 UTF-8 读取规则文档；中文文本写入使用 `apply_patch` 或显式 UTF-8 API。
- Blocker: 出现乱码、写入失败、路径不明确或无法验证生成文件时停止。
- Verification: 写入后使用 Python UTF-8 / `python-docx` 重新读取并记录摘要。
- Forbidden action: 禁止使用默认编码 `Set-Content` / `Out-File` 写中文，禁止吞掉错误。

### Git 脏工作区边界

- Trigger: 当前仓库已有未跟踪任务目录。
- Preflight check: 仅修改本任务目录与输出文档目录，不触碰既有未跟踪任务目录。
- Blocker: 若提交或推送需要混入既有未跟踪目录，停止并报告。
- Verification: `git status --short --branch` 显示本任务新增文件边界清晰。
- Forbidden action: 禁止删除、回滚或提交不属于本任务的并行任务文件。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；按项目规则生成可追溯任务记录与可提交文档。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

## Cleanup Candidates

- tmp/docs/edhr-batch-record-application/

## Final Verification Result

- Word 文件已生成并保留：`output/doc/批记录电子化系统-毕业实践项目立项申请.docx`。
- `python-docx` 文本读取校验通过，核心字段和章节完整，正文约 754 字。
- task-closeout-cleanup preview/apply 已执行，删除本任务临时脚本目录，保留任务记录和最终 Word 文件。
