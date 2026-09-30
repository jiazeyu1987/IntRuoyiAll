# DCC 文控日志精确查询性能修复

## Task Goal

修复 DCC 文控日志页面在携带 19 位 `controlledFileId` 和 `keyword` 时超过前端 30/60 秒的问题。精确文件查询必须按文件 ID 限定各日志来源的数据库读取，避免先无界扫描全部日志再在 Java 内存中过滤；保留完整字符串传输、非法 ID 明确报错和无文件 ID 全局日志页语义。

## BDD

### BDD-01: 精确文件查询不扫描全量日志

Given 请求包含合法的 19 位 `controlledFileId`
When 日志查询服务构造候选记录
Then 每个日志源只读取该文件相关记录，不能调用无条件全表 `selectList()` 扫描后再过滤。

### BDD-02: 关键字筛选保持完整语义

Given 请求同时包含 `controlledFileId` 和 `keyword`
When 页面等待日志列表请求完整返回
Then 请求中的 ID 和关键字保持原始字符串，返回结果仍按现有候选文本执行关键字过滤和分页。

### BDD-03: 非法 ID 明确失败

Given `controlledFileId` 不是合法整数
When 服务开始查询
Then 明确抛出参数错误，不 fallback 到全局日志查询，也不吞异常。

### BDD-04: 无 ID 全局查询不改变

Given 请求不包含 `controlledFileId`
When 查询日志列表
Then 保留现有全局多来源日志查询、排序和分页行为。

## Milestones

1. RED：确认当前精确文件查询仍从多个日志源执行无条件全量读取，且真实页面请求被前端超时中止。
2. GREEN：新增精确文件 ID 查询路径和各来源限定 Mapper 查询，不修改数据库结构或数据。
3. 回归：运行 DCC 日志单测、控制器单测、相关静态合同、TypeScript 检查和真实 Playwright 页面完整响应验证。

## Expected Verification

- DCC 日志查询/控制器单测通过，覆盖 19 位 ID、关键字、非法 ID、空结果和精确查询候选范围。
- DCC 日志相关静态合同和 `pnpm ts:check` 通过。
- 真实 Playwright 页面等待 `/admin-api/dcc/controlled-file-logs/page` 完整响应并确认 HTTP 200、DOM 结束加载、无 timeout/abort/503；只读，不通过 API 或数据库代替页面业务动作。
- `git diff --check` 通过。

## Design Constraints

- 只修改 DCC 日志查询服务、该服务使用的 DCC 日志来源 Mapper、相关单测/静态检查和本任务文档；前端只允许必要的请求配置调整，不能用增大 timeout 掩盖慢查询。
- 不修改数据库 schema、迁移、业务数据、Git 或共享服务。
- `controlledFileId` 在前端和请求 VO 中保持 String，服务边界一次解析为 Long；解析失败必须明确报错。
- 精确查询优先按索引列限定来源；关键字过滤继续使用现有候选文本语义，不能 fallback 为全局扫描。

## Current Status

blocked

Implementation and verification are complete. Git closeout is blocked because this task explicitly forbids Git operations.

## Cleanup Keep

- doc/tasks/20260927-dcc-log-query-performance/task.md
- doc/tasks/20260927-dcc-log-query-performance/execution-log.md
- doc/tasks/20260927-dcc-log-query-performance/verification-report.md
