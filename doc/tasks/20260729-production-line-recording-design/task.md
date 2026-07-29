# 20260729 production line recording design

## Task Goal

将本次线程中关于“生产一线简化填报、资源池、FIFO 订单消耗、审核副本、原始记录修改日志、固定 UI 模板”的业务讨论整理并保存到项目文档中，仅记录本次线程对话，不汇总其它任务或历史对话。

## Milestones

- [x] 建立任务目录和任务记录
- [x] 读取项目任务收尾、PowerShell UTF-8、项目构想文档技能要求
- [x] 明确记录范围为本次线程对话
- [x] 写入业务简述文档
- [x] 写入证据清单与本线程原始对话摘录
- [x] 执行文档结构与 UTF-8 读取验证
- [x] 更新验证报告
- [x] cleanup preview/apply
- [x] 补充现有系统结合方案
- [ ] Git closeout

## Expected Verification

- `docs/inception/project-brief.md` 包含项目构想文档要求的固定章节。
- `docs/inception/evidence-inventory.md` 包含证据清单固定章节，并保存本次线程原始对话摘录。
- 文档明确只记录本次线程，不包含其它历史任务或项目全量对话。
- 使用 UTF-8 方式读取新增文档，确认中文内容可读。
- 运行 `project-inception-docs` 技能提供的结构校验脚本。

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，本任务仅做需求事实沉淀，不用其它历史信息替代本次线程证据。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- 已读取 `docs/task-closeout-rules.md`：本任务修改文件前建立 `doc/tasks/<task-id>/`，收尾前需要记录验证结果。
- 已读取 `docs/powershell-encoding.md`：中文 Markdown 写入使用 `apply_patch`，读取验证使用 UTF-8。
- 已读取 `docs/experience-index.md`：命中“批记录/批记录表单/formBindings/工序开始三类配置不得混用”门禁，本次文档必须保持术语边界。
- 已读取 `project-inception-docs` 技能：本次输出项目构想文档和证据清单，不编造未确认事实。
- 已执行 `project-experience-consolidation` 技能评估：本次新增的是当前线程业务需求证据，不是可复用工程事故或通用流程门禁；未创建新的长期经验文档。

## Closeout Notes

- 文档实现与结构验证已完成。
- 已补充“现有系统结合方案”，记录复用 eDHR 记录本、报工、资源池、排产订单、字段审计和工序任务候选的方向。
- `task-closeout-cleanup` preview/apply 已通过，未删除任何文件。
- Git closeout 需先处理当前工作区既有脏改动；本任务不主动提交或暂存无关文件。
