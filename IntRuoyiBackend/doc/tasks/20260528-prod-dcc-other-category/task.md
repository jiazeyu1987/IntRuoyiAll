# 任务：修复正式服 NAS 转移缺少 DCC“其他”类别

## 目标

修复正式服务器 NAS 管理点击“转移”时报错 `DCC 模板类别缺少启用的“其他”` 的问题，确认根因是正式服 DCC 文件类别基础数据缺失，补齐可重复执行的“其他”模板类别及其审批/权限治理数据。

## 里程碑

- [x] M1：定位前端提示来源和既有 DCC 种子 SQL。
- [x] M2：只读查询正式服与测试服 DCC 文件类别现状。
- [x] M3：在测试服和正式服执行安全、可重复的 DCC“其他”模板类别种子 SQL。
- [x] M4：验证正式服已存在唯一启用的“其他”类别和审批路线。
- [x] M5：记录证据并收尾提交。

## BDD 场景

BDD: 正式服 NAS 转移可找到 DCC 其他类别 -> Given 正式服 DCC 文件类别存在唯一启用的“其他” / When NAS 管理发起转移 / Then 前端不再因缺少模板类别而阻断转移。

BDD: DCC 其他类别治理数据完整 -> Given “其他”类别由“产品技术要求”模板复制 / When 转移生成 DCC 文件 / Then 审批路线、权限、分发和培训规则按模板存在。

## 预期验证

- 正式服 SQL 只读查询：启用的 `其他` 类别数量为 1。
- 正式服 SQL 只读查询：`其他` 类别存在启用审批路线和路线节点。
- 种子 SQL 可重复执行，无重复启用类别。
- `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc\tasks\20260528-prod-dcc-other-category\database-schema-evidence.md`

## 当前状态

completed

## Current Status

completed

## 验证结果

- RED：正式服 `dcc_file_category` 中真实租户 `tenant_id=1` 缺少启用的“其他”，前端 NAS 转移因此阻断。
- GREEN：正式服 `tenant_id=1` 已有唯一启用“其他”类别 `id=906104`，审批路线 1 条、路线节点 4 个。
- GREEN：测试服同样补齐并验证，保持测试/正式 DCC 类别基线一致。
- GREEN：正式服重复执行 `sql/mysql/20260526_dcc_other_template_category.sql` 后仍只有 1 个启用“其他”，证明种子可重复执行。

## Cleanup Keep

- doc/tasks/20260528-prod-dcc-other-category/task.md
- doc/tasks/20260528-prod-dcc-other-category/execution-log.md
- doc/tasks/20260528-prod-dcc-other-category/database-schema-evidence.md
- doc/tasks/20260528-prod-dcc-other-category/bug-regression-evidence.md
