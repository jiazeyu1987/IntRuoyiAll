# 批记录电子化系统立项书内容优化

## Task Goal

根据用户要求，优化现有《批记录电子化系统-毕业实践项目立项申请.docx》正文：业务痛点聚焦纸质批记录痛点，项目目标明确为把纸质批记录电子化，并同步优化解决方案、实施计划、预期成果与价值。

## Milestones

- [x] 创建任务目录并记录用户意图
- [x] 读取经验索引并补充适用门禁
- [x] 更新 Word 文档正文
- [x] 校验文档可读取、核心表述正确且 1000 字以内
- [x] 清理临时脚本并记录完成状态

## Expected Verification

- 使用 `python-docx` 读取目标 `.docx`。
- 验证业务痛点包含纸质批记录、手工填写、流转、归档、追溯等痛点。
- 验证项目目标包含把纸质批记录电子化。
- 验证实施计划仍包含 `2026年10月15日`、`2026年11月30日`、`2026年12月31日`、`2027年1月31日`。
- 验证全文仍低于 1000 字。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；围绕纸质批记录电子化重新组织立项书核心内容。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

## Applicable Gates

- PowerShell / 中文编码门禁：中文文档读写使用 UTF-8 路径，禁止默认编码写入和吞掉错误。
- Git 脏工作区边界：当前存在其它并行前端/后端改动，本任务不触碰、不提交、不清理无关文件。
- 任务验证脚本清理门禁：临时更新脚本位于 `tmp/docs/edhr-batch-record-content-refine/`，收尾时作为 cleanup candidate 删除。

## Cleanup Candidates

- tmp/docs/edhr-batch-record-content-refine/

## Final Verification Result

- Word 文件已更新并保留：`output/doc/批记录电子化系统-毕业实践项目立项申请.docx`。
- `python-docx` 校验通过，业务痛点和项目目标已按用户要求改写。
- task-closeout-cleanup preview/apply 已执行，删除本任务临时脚本目录。
