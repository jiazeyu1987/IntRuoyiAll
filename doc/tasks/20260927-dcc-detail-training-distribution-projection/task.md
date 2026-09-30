# DCC 详情页三流程培训与分发投影修复

## Goal

修复 upload/revision 三流程完成培训记录上传和手工分发后，DCC 详情页仍显示空培训/分发记录的问题。投影必须读取真实的 `trainingRecordFileId` 与三流程状态机事实，不创建虚假的通用培训或分发表数据；普通培训/分发流程保持原有表投影。

## BDD

- BDD: 三流程培训证据 -> Given upload/revision 的 `needTraining=true` 且 `trainingRecordFileId` 指向存在的文件，When 查询详情，Then 返回培训证据可用及真实文件名，前端显示已上传证据。
- BDD: 三流程跳过培训 -> Given upload/revision 的 `needTraining=false`，When 查询详情，Then 不显示培训证据已上传状态。
- BDD: 三流程分发完成 -> Given upload/revision 已从 `PENDING_MANUAL_DISTRIBUTION` 推进到文控审核或终态，When 查询详情，Then 返回明确的分发完成状态；待分发时不得显示完成。
- BDD: 普通流程兼容 -> Given非三流程存在通用培训/分发记录，When 查询详情，Then 原有 `trainingStatuses` 与 `distributionStatuses` 投影不变。

## Expected Verification

- RED：三流程 `needTraining=true` + `trainingRecordFileId` 和已完成分发状态的详情查询，在修复前不能得到证据/完成投影。
- GREEN：`DccControlledFileQueryServiceTest` 覆盖三流程培训证据、跳过培训、待分发/已分发状态及普通流程兼容。
- REGRESSION：运行 DCC QueryService 定向测试及必要前端类型检查。

## Design Constraints

- 仅修改 DCC 详情查询投影、必要响应字段、相关单元测试和前端详情显示。
- 不修改数据库、流程写入、业务数据、共享服务或 Git。
- 不用 synthetic training/distribution 表行、默认成功值或 mock 业务结果。

## Current Status

in_progress

## Cleanup Keep

- doc/tasks/20260927-dcc-detail-training-distribution-projection/task.md
- doc/tasks/20260927-dcc-detail-training-distribution-projection/execution-log.md
- doc/tasks/20260927-dcc-detail-training-distribution-projection/verification-report.md
