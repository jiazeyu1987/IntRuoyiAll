# DCC 日志/历史查询 ID 精度修复

## Task Goal

修复 DCC 文控日志及其历史查询入口对 19 位 `controlledFileId` 使用 JavaScript `number` 导致精度截断的问题。请求、路由查询参数及日志查询 VO 的受控文件 ID 统一按字符串传输；数据库查询边界再显式解析为后端所需的数值类型。不得修改数据库、业务数据或 Git。

## BDD

### BDD-01: 19 位受控文件 ID 保持完整

Given URL 中存在 `controlledFileId=2054545668044084022`
When 打开 DCC 文控日志页面并发起列表请求
Then 请求参数必须仍为完整字符串 `2054545668044084022`，不得变为 `2054545668044084000`。

### BDD-02: URL 关键字和筛选值可回放

Given URL 中存在 `keyword`、日志类型及受控文件 ID 筛选参数
When 页面初始化日志查询
Then 页面查询模型和请求必须保留这些原始字符串值，且不因 `Number()` 转换而丢失精度。

### BDD-03: 后端日志查询结果不回归

Given 日志查询服务收到完整字符串受控文件 ID
When 服务执行筛选
Then 仅在服务边界把合法字符串解析为数据库查询所需的 `Long`，既有日志/历史筛选结果保持不变；非法 ID 必须明确报错。

## Milestones

1. 记录旧实现的 RED 复现证据。
2. 完成日志 API、页面、请求 VO 和服务边界的最小 string 修复。
3. 运行日志、历史入口、审批时间线相关静态测试及 DCC 日志后端单测。
4. 记录 GREEN、回归结果和未执行项。

## Expected Verification

- RED：新增精度与 URL 查询回放测试在旧实现上失败。
- GREEN：前端 DCC 日志静态合同通过。
- GREEN：DCC 日志查询服务及 Controller 单测通过。
- GREEN：相关历史/生命周期/审批时间线静态合同通过。
- `git diff --check` 通过；不执行数据库写入、业务数据写入或 Git 操作。

## Design Constraints

- 只允许修改 DCC logs 前端 API/页面、日志相关后端 VO/服务边界，以及相关 static/unit tests 和本任务文档。
- 不改数据库 schema、迁移、业务数据、无关模块或 Git。
- 前端不得对 `controlledFileId` 调用 `Number()`；URL 筛选值按字符串读取。
- 后端数据库 mapper 仍使用已有 `Long` ID 类型，解析失败必须显式抛出参数错误，不得 fallback 或吞异常。

## Current Status

in_progress

## Cleanup Keep

- doc/tasks/20260927-dcc-log-id-precision/task.md
- doc/tasks/20260927-dcc-log-id-precision/execution-log.md
- doc/tasks/20260927-dcc-log-id-precision/verification-report.md
