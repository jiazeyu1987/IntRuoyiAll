# Execution Log

## User Intent

用户反馈：在个人工作台点击 eDHR 待办“进入/处理”时报“当前 eDHR 批次状态不允许该操作”，并明确要求新业务只能使用最新已发布的批记录表单。

## BDD

- `BDD: new eDHR business freezes latest published batch record form -> Given` 同一批记录定义存在多个已发布版本且工艺路线或历史绑定仍指向旧版本，`When` 创建新批次、返工或其它新业务，`Then` 系统只冻结最新已发布版本及其报表上下文。
- `BDD: historical eDHR business keeps frozen form version -> Given` 历史批次已冻结旧批记录版本，`When` 后续发布新版本并重新打开历史业务，`Then` 历史批次仍使用原冻结版本，不自动升级。
- `BDD: actionable workbench task opens normally -> Given` 个人工作台展示属于可处理批次的新业务待办，`When` 当前责任人点击“进入/处理”，`Then` 正式打开填写页面且不提示“当前 eDHR 批次状态不允许该操作”。
- `BDD: terminal batch remains blocked and hidden -> Given` 批次已关闭、归档、驳回或作废，`When` 查询个人待办或尝试打开任务，`Then` 待办不进入可处理列表且打开接口继续 fail-fast。

## Milestone Updates

- in_progress: 已创建任务记录并读取匹配经验门禁，开始定位新业务版本选择与个人工作台打开链路。

## TDD Evidence

- RED: pending
- GREEN: pending

## Blockers

- 当前根工作区已有其它任务未提交的 `doc/tasks/20260726-merge-worktrees-into-int-main/` 文档改动；本任务不修改这些文件，提交前按项目 Git 门禁处理。

