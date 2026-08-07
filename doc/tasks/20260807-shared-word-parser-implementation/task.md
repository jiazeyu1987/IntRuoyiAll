# 共享 Word 解析器实施任务

## Task Goal

将 MES 批记录现有 Word 结构解析能力下沉为 BPM 与 MES 共同依赖的共享解析模块，使表单模板与批记录表单使用同一套 `.doc` / `.docx` 结构解析算法，同时保持各自 API、权限、版本、审批、路线和报表业务语义不变。

## Milestones

- M1：完成现状分析、PRD、任务分解、测试计划和依赖边界设计。
- M2：按 BDD + 严格 TDD 建立共享解析模块及 canonical profile。
- M3：迁移 MES 解析适配器并验证旧新结构等价。
- M4：迁移表单中心识别器并验证字段识别与错误语义。
- M5：完成独立回归、经验沉淀、提交推送和任务清理。

## Expected Verification

- 共享模块依赖方向静态合同。
- 共享解析器 `.doc` / `.docx`、空文件、损坏文件、无内容和结构解析单元测试。
- MES 旧新解析结构等价测试及批记录路线/报表定向回归。
- BPM 表单模板字段识别、失败语义和运行时合同测试。
- 前端三个既有导入 API URL 静态合同。
- `git diff --check`、backend evidence validator、独立测试报告和最终推送状态。

## Applicable Experience Gate

- 待读取 `docs/experience-index.md` 后补充匹配经验摘要。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；通过下沉共享解析模块消除两套 POI 解析实现分叉。
- `是否存在临时补丁或绕过`：否。

## Current Status

in_progress：正在执行任务启动、规划门禁和脏工作区基线提交。

## Cleanup Keep

- doc/tasks/20260807-shared-word-parser-implementation/task.md
- doc/tasks/20260807-shared-word-parser-implementation/execution-log.md
- doc/tasks/20260807-shared-word-parser-implementation/verification-report.md
