# Task: 清理旧批记录模板并重导入

## Goal

清理当前数据库中旧的 5 条历史批记录模板，并基于
`C:\Users\BJB110\Desktop\2\2\RE-PP-ID-01（A 1）球囊扩张压力泵生产记录(1).doc`
重新导入，落成新的 10 条模板。

## Scope

- 仅处理运行库中的旧模板数据与重导入动作
- 不修改前后端代码
- 使用当前 clean backend 接口与数据库执行

## Milestones

- [x] M1: 确认当前旧模板数量与样本最新解析结果。
- [x] M2: 清理旧模板数据。
- [x] M3: 重导入样本并提交 10 条新模板。
- [x] M4: 验证模板列表数量与标题。

## Current Status

Completed.

## Final Verification

- 删除旧模板后，模板列表不再保留原先的 5 条历史模板
- 使用 `importId = 11` 提交 `selectedTableIndexes = [1..10]` 后，系统成功创建 10 条新模板
- `GET /admin-api/mes/pro/batch-record-template/page?pageNo=1&pageSize=30` 返回 `total = 10`
