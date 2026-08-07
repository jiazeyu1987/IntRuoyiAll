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

- `docs/backend-development.md#eDHR-批记录-Word-表格解析门禁`：使用真实源 DOC 与最小合成表格双重验证；缺 fixture 或 RED 不稳定时阻塞；禁止按表单名、工序名、文件名或产品模板写特例。
- `docs/powershell-memory.md#PowerShell-Maven--D-参数引号门禁`：所有 Maven `-D...` 参数整体加引号。
- `docs/powershell-memory.md#Maven-单模块陈旧依赖门禁`：BPM/MES 目标测试使用 `-pl ... -am`，确保兄弟模块和共享模块进入 reactor。
- `docs/powershell-memory.md#Maven-目标目录文件系统异常门禁`：不得与并发 Maven 叠加写同一 `target`；未到达 Surefire 不得记录为业务通过。
- `docs/powershell-memory.md#共享分支并发基线提交门禁`：每个阶段复核最近提交、状态和本任务 diff，防止并发基线吞入本任务实现。
- `docs/task-closeout-rules.md#技能证据文件清理前归档门禁`：backend evidence validator 通过后，将关键结论归档到保留报告，再执行 cleanup。

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
