# Execution Log

## User Intent

- 用户要求：文控审批点击“打开”应打开用户上传页面的信息，不需要看所有追溯信息，只需要上传页面的信息，包括文件预览。

## BDD

- `BDD: DCC approval open shows upload information -> Given` 审批中心 DCC 待办行具备 `PROCESS_IN_MODULE` 能力，`When` 用户点击“打开”，`Then` 详情页进入 `handling=approval&from=approval-center` 上传审批视角，页面显示上传提交相关信息和文件预览。
- `BDD: DCC approval open hides traceability blocks -> Given` 用户位于 DCC 上传审批视角，`When` 页面加载完成，`Then` 关键记录时间线、路线快照、版本历史、分发状态、受控打印、培训状态、签核追溯和签名留痕等全量追溯区块不可见。
- `BDD: DCC approval processing remains actionable -> Given` 当前用户是审批任务处理人，`When` 进入上传审批视角，`Then` 当前审批处理区和审批/驳回/转办/加签等正式处理动作仍按原规则显示。

## Command Log

- Pending.

## RED/GREEN Evidence

- Pending.

## Blockers

- 当前工作区存在与本任务无关的脏改动和 ahead 提交；本任务只选择性修改 DCC 审批相关文件和本任务文档，不触碰并行改动。
