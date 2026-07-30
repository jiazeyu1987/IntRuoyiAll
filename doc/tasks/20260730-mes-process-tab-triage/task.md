# 20260730 MES process tab triage

## Task Goal

评估在“工序设置”和“工艺流程”之间新增“MES工序”页签的业务与技术影响，明确 MES 工序、设备和现有工序设置工序之间的关系，记录本次线程需求，不实施生产代码。

## Milestones

- [x] 核对现有工序设置与工艺流程菜单位置
- [x] 核对现有工序、路线工序、工作站和设备关系
- [x] 识别新 MES 工序与现有 `mes_pro_process` 的命名和职责冲突
- [x] 形成变更评估、建议列表字段和待确认问题
- [x] 验证变更文档结构和 UTF-8
- [x] 完成任务收尾

## Expected Verification

- 变更文档包含 Request、Baseline、Classification、Impact、Decision、Downstream 和 Blockers。
- 文档明确新“MES工序”是一线报工工序层，不是当前“工序设置”的改名。
- 文档明确复用现有设备主数据和正式工序，不复制批记录绑定。
- 文档列出关系基数、适用范围、设备使用方式、产能归属和历史变更规则等待确认项。
- 运行 `change-request-triage` 校验脚本。
- 使用 UTF-8 重新读取任务文档与变更文档。

## Current Status

completed

## Closeout Notes

- `task-closeout-cleanup` preview/apply 均通过。
- 保留三个核心任务文档，删除项和阻塞项为空。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；先拆清一线 MES 工序和正式工序两个领域对象，避免继续把不同业务名称写入同一工序主表。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- 已读取 `AGENTS.md`：正式批记录表单仍由工序设置中的逐工序绑定提供，新 MES 工序不得用表单槽位或工序开始配置替代正式批记录绑定。
- 已读取 `docs/powershell-encoding.md`：中文 Markdown 使用 `apply_patch` 写入并以 UTF-8 验证。
- 已读取 `docs/task-closeout-rules.md`：本任务为文档化变更评估，需要任务记录、验证报告和 cleanup。
- 已读取 `change-request-triage` 技能及变更证据契约：当前只做接受/阻塞决策和影响分析，不提前实施代码。
