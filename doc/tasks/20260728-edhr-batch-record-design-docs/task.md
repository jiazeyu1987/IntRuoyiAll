# 20260728 EDHR 批记录系统设计文档交付

## Task Goal

基于 EDHR 批记录系统建设范围，整理并输出 Word 版《需求设计》《概要设计》《详细设计》文档。文档体系保持“需求设计 -> 概要设计 -> 详细设计”的逐层衍生关系，并满足正式项目交付口径。

## Milestones

- [x] 建立任务记录并完成经验门禁读取
- [x] 盘点 EDHR 批记录相关现有实现证据
- [x] 反向生成需求设计、概要设计、详细设计内容
- [x] 生成 Word 文档并完成结构验证；渲染验证因本机缺少 soffice 阻塞
- [x] 将合并版设计文档拆分为需求设计、概要设计、详细设计三份独立 Word 文件
- [x] 按正式交付口径修订文档，移除内部化、生成方式、源文件、倒推等不适合交付的表述
- [x] 更新任务记录、验证报告并完成收尾

## Expected Verification

- 文档内容满足正式项目交付口径，不出现内部生成方式、源文件、工作区、倒推、AI 等不适合交付的表述。
- 三份设计文档之间保持需求、概要、详细逐层衍生的一致性。
- Word 文件可正常打开，文档结构、标题层级、表格和中文内容完整。
- 若 LibreOffice 渲染可用，完成 DOCX -> PNG 渲染检查；若不可用，记录阻塞并执行结构验证。

## Current Status

completed

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，文档按正式需求、概要、详细设计交付结构整理，不用臆测替代缺失依据。
- 是否存在临时补丁或绕过：否。

## 经验门禁

- EDHR 术语门禁：批记录表单、表单槽位、工序开始必须作为三条独立链路表达，禁止互相 fallback。
- 后端 EDHR 门禁：批次任务配置来源、批记录版本治理、单元格链接落库、字段审计链和作废终态必须 fail-fast。
- 前端 EDHR 门禁：表单模板与批记录表单不得混为同一数据域，批记录列表辅助查询失败不得污染主列表成功状态。
- E2E 门禁：真实写入验收必须走真实前端路径、授权租户账号和任务自有数据；本文档生成任务不执行写入型 E2E。
- Word 文档门禁：已生成 DOCX 并完成结构检查；LibreOffice/`soffice` 缺失导致 DOCX -> PNG 视觉渲染不可执行，按文档技能规则记录该限制。

## Cleanup Keep

- doc/tasks/20260728-edhr-batch-record-design-docs/output/EDHR_Batch_Record_System_Design.docx
- doc/tasks/20260728-edhr-batch-record-design-docs/output/EDHR_Batch_Record_Requirement_Design.docx
- doc/tasks/20260728-edhr-batch-record-design-docs/output/EDHR_Batch_Record_Outline_Design.docx
- doc/tasks/20260728-edhr-batch-record-design-docs/output/EDHR_Batch_Record_Detailed_Design.docx
