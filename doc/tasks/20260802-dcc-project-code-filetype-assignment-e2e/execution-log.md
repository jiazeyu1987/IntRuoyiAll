# 20260802 DCC 项目代码文件类型归属 E2E 验证 Execution Log

## User Intent

- 用户要求验证另一条链路：修改已有受控文件的 5 个文件类型以及归属的 DCC 项目代码后，DCC 项目代码 item 详情中的文件类型也发生对应变化。
- 本任务必须验证真实页面链路，不使用 API-only、SQL 直改或 mock 数据冒充通过。

## BDD

- BDD: 已有文件归属到 DCC 项目代码 -> Given 测试租户中存在一个已有受控文件和目标 DCC 项目代码，When 用户在真实页面修改文件基础信息并保存目标 DCC 项目代码，Then 目标 DCC 项目代码详情的关联文档包含该文件。
- BDD: 五个文件类型同步 -> Given 目标 DCC 项目代码详情按正式 DCC 文件分类树展示阶段和文件类型，When 用户依次把同一个已有文件修改为 5 个不同文件类型，Then 每次进入目标 DCC 项目代码详情都能在对应文件类型下看到该文件。
- BDD: 非目标归属不污染 -> Given 文件从原 DCC 项目代码移动到目标 DCC 项目代码，When 修改保存成功，Then 原项目代码详情不再把该文件计入当前归属，目标项目代码详情按新文件类型展示。

## Verification Evidence

- GREEN: task bootstrap -> PASS, task directory created and applicable E2E/database/frontend gates recorded.

## Blockers

- None yet.
